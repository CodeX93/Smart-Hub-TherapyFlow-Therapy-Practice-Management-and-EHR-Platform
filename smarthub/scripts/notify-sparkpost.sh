#!/usr/bin/env bash
# Send deployment notifications via SparkPost Transmissions API.
set -euo pipefail

send_sparkpost_notification() {
  local status="$1"
  local environment="${2:-unknown}"
  local message="${3:-}"
  local details="${4:-}"

  if [[ -z "${SPARKPOST_API_KEY:-}" ]]; then
    echo "WARN: SPARKPOST_API_KEY not set — skipping deploy notification"
    return 0
  fi

  if [[ -z "${DEPLOY_NOTIFY_EMAIL:-}" ]]; then
    echo "WARN: DEPLOY_NOTIFY_EMAIL not set — skipping deploy notification"
    return 0
  fi

  local base_url="${SPARKPOST_BASE_URL:-https://api.sparkpost.com/api/v1}"
  local from_email="${EMAIL_FROM:-noreply@mail.resiliencecrm.com}"
  local from_name="${EMAIL_FROM_NAME:-TherapyFlow}"
  local commit="${CI_COMMIT_SHORT_SHA:-local}"
  local branch="${CI_COMMIT_REF_NAME:-local}"
  local pipeline="${CI_PIPELINE_URL:-}"
  local subject
  local color
  local status_label

  if [[ "$status" == "success" ]]; then
    subject="✅ TherapyFlow ${environment} deployment succeeded"
    color="#22c55e"
    status_label="SUCCESS"
  else
    subject="❌ TherapyFlow ${environment} deployment failed"
    color="#ef4444"
    status_label="FAILED"
  fi

  local html
  html=$(cat <<EOF
<html><body style="font-family: sans-serif; color: #1f2937;">
  <h2 style="color: ${color};">${status_label}: ${environment} deployment</h2>
  <p><strong>Environment:</strong> ${environment}</p>
  <p><strong>Branch:</strong> ${branch}</p>
  <p><strong>Commit:</strong> ${commit}</p>
  <p><strong>Image tag:</strong> ${IMAGE_TAG:-n/a}</p>
  <p><strong>Container app:</strong> ${AZURE_CONTAINER_APP:-n/a}</p>
  <p><strong>Message:</strong> ${message}</p>
  ${details:+<pre style="background:#f3f4f6;padding:12px;border-radius:6px;white-space:pre-wrap;">${details}</pre>}
  ${pipeline:+<p><a href="${pipeline}">View pipeline</a></p>}
</body></html>
EOF
)

  local payload
  payload=$(jq -n \
    --arg from_email "$from_email" \
    --arg from_name "$from_name" \
    --arg subject "$subject" \
    --arg html "$html" \
    --arg to "$DEPLOY_NOTIFY_EMAIL" \
    '{
      content: {
        from: {email: $from_email, name: $from_name},
        subject: $subject,
        html: $html
      },
      recipients: [{address: {email: $to}}]
    }')

  local http_code
  http_code=$(curl -s -o /tmp/sparkpost-response.json -w "%{http_code}" \
    -X POST "${base_url}/transmissions" \
    -H "Authorization: ${SPARKPOST_API_KEY}" \
    -H "Content-Type: application/json" \
    -d "$payload")

  if [[ "$http_code" =~ ^2 ]]; then
    echo "SparkPost notification sent (${status}) to ${DEPLOY_NOTIFY_EMAIL}"
  else
    echo "WARN: SparkPost notification failed (HTTP ${http_code})"
    cat /tmp/sparkpost-response.json 2>/dev/null || true
  fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  send_sparkpost_notification "${1:-failure}" "${2:-unknown}" "${3:-}" "${4:-}"
fi
