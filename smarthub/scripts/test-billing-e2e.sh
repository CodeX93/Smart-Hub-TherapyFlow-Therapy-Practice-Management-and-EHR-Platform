#!/usr/bin/env bash
# Local-only QA: always provisions and removes its own disposable PostgreSQL container.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi
qa_container="smarthub-billing-e2e-$$"
qa_password="$(openssl rand -hex 24)"
cleanup() { docker stop "$qa_container" >/dev/null 2>&1 || true; }
trap cleanup EXIT
docker run --rm -d --name "$qa_container" \
  -e POSTGRES_DB=therapyflow_test -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD="$qa_password" -p 127.0.0.1::5432 postgres:15 >/dev/null
qa_ready=false
for attempt in {1..30}; do
  if docker exec "$qa_container" pg_isready -U postgres -d therapyflow_test >/dev/null; then
    qa_ready=true
    break
  fi
  sleep 1
done
[[ "$qa_ready" == true ]] || { echo 'QA PostgreSQL did not become ready' >&2; exit 1; }
qa_port="$(docker port "$qa_container" 5432/tcp | awk -F: '{print $NF}')"
export DB_URL="jdbc:postgresql://127.0.0.1:${qa_port}/therapyflow_test"
export DB_USERNAME=postgres DB_PASSWORD="$qa_password"
./mvnw -q -Dlogging.level.org.hibernate.SQL=OFF \
  -Dtest=BillingSessionTriggerIntegrationTest,SessionStatusBillingTriggerTest,BillingGuardSessionEligibilityTest test
