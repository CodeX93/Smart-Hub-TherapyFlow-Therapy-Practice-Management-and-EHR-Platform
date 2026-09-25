#!/usr/bin/env bash
# Refresh a Key Vault secret in an Azure Container App and deploy a new revision.
#
# Container Apps cache Key Vault secrets at revision startup. After updating a
# secret in Key Vault, run this script to pin the latest version and roll out
# a new revision that loads it.
#
# Usage:
#   ./scripts/refresh-keyvault-secret.sh
#   ./scripts/refresh-keyvault-secret.sh --value "sk-your-new-key"
#   KEY_VAULT_SECRET_VALUE="sk-your-new-key" ./scripts/refresh-keyvault-secret.sh
#   ./scripts/refresh-keyvault-secret.sh --value-file ./openai.key
#   ./scripts/refresh-keyvault-secret.sh --prompt
#   ./scripts/refresh-keyvault-secret.sh --secret ai-integrations-openai-api-key --dry-run
#
# Environment overrides:
#   AZURE_RESOURCE_GROUP, AZURE_CONTAINER_APP, AZURE_SUBSCRIPTION
#   AZURE_KEY_VAULT (default: therapyflowkv809)
#   KEY_VAULT_SECRET_NAME (default: ai-integrations-openai-api-key)
#   CONTAINER_APP_SECRET_NAME (default: same as KEY_VAULT_SECRET_NAME)
#   KEY_VAULT_SECRET_VALUE (optional new secret value; preferred over --value)
set -euo pipefail

RG="${AZURE_RESOURCE_GROUP:-Resilience}"
APP="${AZURE_CONTAINER_APP:-therapy-flow-api}"
SUBSCRIPTION="${AZURE_SUBSCRIPTION:-80920a1f-e61e-4c65-9ecf-bb5bc6115046}"
KEY_VAULT="${AZURE_KEY_VAULT:-therapyflowkv809}"
KV_SECRET_NAME="${KEY_VAULT_SECRET_NAME:-ai-integrations-openai-api-key}"
CONTAINER_SECRET_NAME="${CONTAINER_APP_SECRET_NAME:-$KV_SECRET_NAME}"
WAIT_ATTEMPTS="${REFRESH_WAIT_ATTEMPTS:-30}"
DRY_RUN=false
SECRET_VALUE="${KEY_VAULT_SECRET_VALUE:-}"
VALUE_FILE=""
PROMPT_FOR_VALUE=false

usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  --secret NAME           Key Vault secret name (default: ai-integrations-openai-api-key)
  --container-secret NAME Container App secret name (default: same as --secret)
  --key-vault NAME        Key Vault name (default: therapyflowkv809)
  --value VALUE           Write a new value to Key Vault before refreshing the container
  --value-file PATH       Read the new secret value from a file (recommended for CI)
  --prompt                Prompt securely for the new secret value
  --dry-run               Show planned changes without applying them
  -h, --help              Show this help message

Examples:
  $0
  KEY_VAULT_SECRET_VALUE="sk-..." $0
  $0 --value "sk-..."
  $0 --value-file ./openai.key
  $0 --prompt
  $0 --secret openai-whisper-api-key
EOF
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --secret) KV_SECRET_NAME="$2"; CONTAINER_SECRET_NAME="${CONTAINER_APP_SECRET_NAME:-$2}"; shift 2 ;;
    --container-secret) CONTAINER_SECRET_NAME="$2"; shift 2 ;;
    --key-vault) KEY_VAULT="$2"; shift 2 ;;
    --value) SECRET_VALUE="$2"; shift 2 ;;
    --value-file) VALUE_FILE="$2"; shift 2 ;;
    --prompt) PROMPT_FOR_VALUE=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    -h|--help) usage ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

if [[ -n "$VALUE_FILE" ]]; then
  if [[ ! -f "$VALUE_FILE" ]]; then
    echo "ERROR: Value file not found: $VALUE_FILE"
    exit 1
  fi
  SECRET_VALUE="$(tr -d '\r\n' < "$VALUE_FILE")"
fi

if $PROMPT_FOR_VALUE; then
  if [[ -n "$SECRET_VALUE" ]]; then
    echo "ERROR: Use only one of --value, --value-file, --prompt, or KEY_VAULT_SECRET_VALUE"
    exit 1
  fi
  read -r -s -p "Enter new secret value: " SECRET_VALUE
  echo ""
fi

if [[ -n "$SECRET_VALUE" && "$SECRET_VALUE" == *$'\n'* ]]; then
  echo "ERROR: Secret value must be a single line"
  exit 1
fi

azure_login() {
  echo "==> Authenticating with Azure"
  if [[ -n "${AZURE_CLIENT_ID:-}" && -n "${AZURE_CLIENT_SECRET:-}" && -n "${AZURE_TENANT_ID:-}" ]]; then
    az login --service-principal \
      -u "$AZURE_CLIENT_ID" \
      -p "$AZURE_CLIENT_SECRET" \
      --tenant "$AZURE_TENANT_ID" \
      --output none
  elif az account show &>/dev/null; then
    echo "Using existing Azure CLI session"
  else
    echo "ERROR: Azure credentials not configured. Run 'az login' or set AZURE_CLIENT_ID/AZURE_CLIENT_SECRET/AZURE_TENANT_ID"
    exit 1
  fi
  az account set --subscription "$SUBSCRIPTION"
}

get_secret_version() {
  az keyvault secret show \
    --vault-name "$KEY_VAULT" \
    --name "$KV_SECRET_NAME" \
    --query id -o tsv | awk -F/ '{print $NF}'
}

update_keyvault_secret() {
  local value="$1"

  echo "==> Writing new value to Key Vault secret '${KV_SECRET_NAME}'"
  az keyvault secret set \
    --vault-name "$KEY_VAULT" \
    --name "$KV_SECRET_NAME" \
    --value "$value" \
    -o none
}

get_container_secret_url() {
  az containerapp secret list -n "$APP" -g "$RG" \
    --query "[?name=='${CONTAINER_SECRET_NAME}'].keyVaultUrl | [0]" -o tsv
}

wait_for_revision() {
  local revision="$1"
  local attempt=0

  echo "==> Waiting for revision ${revision} to become healthy"
  while [[ $attempt -lt $WAIT_ATTEMPTS ]]; do
    attempt=$((attempt + 1))

    local provisioning_state health_state running_status active_replicas
    provisioning_state="$(az containerapp revision show \
      -n "$APP" -g "$RG" --revision "$revision" \
      --query "properties.provisioningState" -o tsv 2>/dev/null || echo "Unknown")"
    health_state="$(az containerapp revision show \
      -n "$APP" -g "$RG" --revision "$revision" \
      --query "properties.healthState" -o tsv 2>/dev/null || echo "Unknown")"
    running_status="$(az containerapp show -n "$APP" -g "$RG" \
      --query "properties.runningStatus" -o tsv 2>/dev/null || echo "Unknown")"
    active_replicas="$(az containerapp replica list \
      -n "$APP" -g "$RG" --revision "$revision" \
      --query "length([?properties.runningState=='Running'])" -o tsv 2>/dev/null || echo "0")"

    echo "[${attempt}/${WAIT_ATTEMPTS}] provisioning=${provisioning_state} health=${health_state} running=${running_status} active_replicas=${active_replicas}"

    if [[ "$provisioning_state" == "Provisioned" && "$running_status" == "Running" && "${active_replicas:-0}" -gt 0 ]]; then
      if [[ "$health_state" == "Healthy" || "$health_state" == "None" || "$health_state" == "Unknown" ]]; then
        echo "==> Revision is running"
        return 0
      fi
    fi

    if [[ "$health_state" == "Unhealthy" ]]; then
      echo "ERROR: Revision reported unhealthy"
      return 1
    fi

    sleep 10
  done

  echo "ERROR: Revision did not become healthy within timeout"
  return 1
}

main() {
  local kv_base_url="https://${KEY_VAULT}.vault.azure.net/secrets/${KV_SECRET_NAME}"
  local revision_suffix="kv-$(date +%m%d%H%M)"

  echo "==> Refresh Key Vault secret in Container App"
  echo "    Subscription:     ${SUBSCRIPTION}"
  echo "    Resource group:   ${RG}"
  echo "    Container app:    ${APP}"
  echo "    Key Vault:        ${KEY_VAULT}"
  echo "    KV secret:        ${KV_SECRET_NAME}"
  echo "    Container secret: ${CONTAINER_SECRET_NAME}"

  azure_login

  if [[ -n "$SECRET_VALUE" ]]; then
    if $DRY_RUN; then
      echo ""
      echo "==> Would update Key Vault secret '${KV_SECRET_NAME}' with a new value (length: ${#SECRET_VALUE})"
    else
      update_keyvault_secret "$SECRET_VALUE"
    fi
  fi

  local secret_version secret_updated current_url new_url previous_revision
  secret_version="$(get_secret_version)"
  secret_updated="$(az keyvault secret show \
    --vault-name "$KEY_VAULT" \
    --name "$KV_SECRET_NAME" \
    --query attributes.updated -o tsv)"
  current_url="$(get_container_secret_url)"
  new_url="${kv_base_url}/${secret_version}"
  previous_revision="$(az containerapp show -n "$APP" -g "$RG" \
    --query properties.latestRevisionName -o tsv)"

  echo ""
  echo "==> Key Vault secret"
  echo "    Version:  ${secret_version}"
  echo "    Updated:  ${secret_updated}"
  echo ""
  echo "==> Container App secret reference"
  echo "    Before:   ${current_url:-<not configured>}"
  echo "    After:    ${new_url}"
  echo ""
  echo "==> Planned rollout"
  echo "    Current revision: ${previous_revision}"
  echo "    New suffix:       ${revision_suffix}"

  if $DRY_RUN; then
    echo ""
    if [[ -n "$SECRET_VALUE" ]]; then
      echo "==> Dry run complete (Key Vault value and container would be updated)"
    else
      echo "==> Dry run complete (no changes applied)"
    fi
    exit 0
  fi

  if [[ -z "$current_url" || "$current_url" == "None" ]]; then
    echo "ERROR: Container App secret '${CONTAINER_SECRET_NAME}' was not found"
    exit 1
  fi

  if [[ "$current_url" == "$new_url" ]]; then
    echo ""
    echo "==> Secret reference already points to the latest Key Vault version"
    echo "==> Creating a new revision anyway so running containers reload the value"
  fi

  echo ""
  echo "==> Updating Container App secret reference"
  az containerapp secret set -n "$APP" -g "$RG" \
    --secrets "${CONTAINER_SECRET_NAME}=keyvaultref:${new_url},identityref:system" \
    -o none

  echo "==> Deploying new revision"
  az containerapp update -n "$APP" -g "$RG" \
    --revision-suffix "$revision_suffix" \
    -o none

  local new_revision
  new_revision="$(az containerapp show -n "$APP" -g "$RG" \
    --query properties.latestRevisionName -o tsv)"

  wait_for_revision "$new_revision"

  echo ""
  echo "==> Refresh complete"
  az containerapp show -n "$APP" -g "$RG" \
    --query "{latestRevision:properties.latestRevisionName,runningStatus:properties.runningStatus,secretRef:properties.configuration.secrets[?name=='${CONTAINER_SECRET_NAME}']}" \
    -o json
}

main "$@"
