#!/usr/bin/env bash
# Validate release evidence before production deployment and write the final manifest after it.
set -euo pipefail

MODE="${1:-}"
EVIDENCE_DIR="${RELEASE_EVIDENCE_DIR:-release-evidence}"
IMAGE_PROVENANCE="${EVIDENCE_DIR}/image-provenance.json"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_status() {
  local file="$1"
  [[ -s "$file" ]] || fail "Required evidence report is missing or empty: ${file}"
  grep -q '^status=passed$' "$file" || fail "Evidence report did not pass: ${file}"
}

require_common_evidence() {
  require_status "${EVIDENCE_DIR}/test-status.txt"
  require_status "${EVIDENCE_DIR}/dependency-scan.status"
  require_status "${EVIDENCE_DIR}/secret-scan.status"
  require_status "${EVIDENCE_DIR}/migration-status.txt"

  [[ -s "$IMAGE_PROVENANCE" ]] || fail "Image provenance report is missing or empty: ${IMAGE_PROVENANCE}"
  command -v jq >/dev/null 2>&1 || fail "jq is required to validate image provenance"
  jq -e '(.image | type == "string" and length > 0) and (.digest | type == "string" and length > 0)' \
    "$IMAGE_PROVENANCE" >/dev/null \
    || fail "Image provenance must contain a non-empty image and digest"
}

image_reference() {
  if [[ -n "${IMAGE_REFERENCE:-}" ]]; then
    printf '%s\n' "$IMAGE_REFERENCE"
  else
    jq -r '.image' "$IMAGE_PROVENANCE"
  fi
}

image_digest() {
  if [[ -n "${IMAGE_DIGEST:-}" ]]; then
    printf '%s\n' "$IMAGE_DIGEST"
  else
    jq -r '.digest' "$IMAGE_PROVENANCE"
  fi
}

pre_deploy() {
  require_common_evidence
  echo "Production release evidence preflight passed; deployment may proceed."
}

post_deploy() {
  : "${CI_COMMIT_SHA:?CI_COMMIT_SHA is required for release metadata}"
  require_common_evidence
  require_status "${EVIDENCE_DIR}/health.status"
  require_status "${EVIDENCE_DIR}/traffic.status"

  local image_ref image_digest_value
  image_ref="$(image_reference)"
  image_digest_value="$(image_digest)"

  jq -n \
    --arg commit "$CI_COMMIT_SHA" \
    --arg ref "$image_ref" \
    --arg digest "$image_digest_value" \
    --arg test_status "passed" \
    --arg dependency_status "passed" \
    --arg secret_status "passed" \
    --arg migration_status "passed" \
    --arg health_status "passed" \
    --arg traffic_status "passed" \
    --arg test_evidence "${EVIDENCE_DIR}/test-status.txt" \
    --arg dependency_evidence "${EVIDENCE_DIR}/dependency-scan.status" \
    --arg secret_evidence "${EVIDENCE_DIR}/secret-scan.status" \
    --arg migration_evidence "${EVIDENCE_DIR}/migration-status.txt" \
    --arg health_evidence "${EVIDENCE_DIR}/health.status" \
    --arg traffic_evidence "${EVIDENCE_DIR}/traffic.status" \
    '{
      schemaVersion: 1,
      commit: {sha: $commit},
      image: {reference: $ref, digest: $digest},
      tests: {status: $test_status, evidence: $test_evidence},
      scans: {dependency: $dependency_status, secrets: $secret_status,
              dependencyEvidence: $dependency_evidence, secretEvidence: $secret_evidence},
      migration: {status: $migration_status, evidence: $migration_evidence},
      health: {status: $health_status, evidence: $health_evidence},
      traffic: {status: $traffic_status, evidence: $traffic_evidence}
    }' > "${EVIDENCE_DIR}/release-metadata.json"

  test -s "${EVIDENCE_DIR}/release-metadata.json" || fail "Release metadata was not written"
  echo "Release metadata written to ${EVIDENCE_DIR}/release-metadata.json"
}

case "$MODE" in
  pre-deploy) pre_deploy ;;
  post-deploy) post_deploy ;;
  *) fail "Usage: $0 {pre-deploy|post-deploy}" ;;
esac
