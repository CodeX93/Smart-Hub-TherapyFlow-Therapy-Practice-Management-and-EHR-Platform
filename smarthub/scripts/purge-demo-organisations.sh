#!/usr/bin/env bash
# Hard-purge every organisation except KEEP_ID, one transaction per organisation.
#
# Connection comes from standard libpq env vars -- nothing is hardcoded and no
# credential is read from or written to this file. For the Azure database:
#
#   export PGHOST=client-hub.postgres.database.azure.com
#   export PGPORT=5432
#   export PGDATABASE=therapyflow
#   export PGUSER=...            # your admin user
#   export PGSSLMODE=require
#   read -rs PGPASSWORD && export PGPASSWORD    # typed, not echoed, not stored
#
# Then:
#   ./purge-demo-organisations.sh --dry-run   # list what would be purged
#   ./purge-demo-organisations.sh --confirm   # actually purge
set -euo pipefail

KEEP_ID="${KEEP_ID:-50}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL="$HERE/purge-one-organisation.sql"
MODE="${1:---dry-run}"

psql_q() { psql -v ON_ERROR_STOP=1 -qAt -c "$1"; }

if [[ ! -f "$SQL" ]]; then
    echo "missing $SQL" >&2; exit 1
fi

# The org to keep must exist, or we are pointed at the wrong database.
if [[ "$(psql_q "select count(*) from public.organisations where id = $KEEP_ID;")" != "1" ]]; then
    echo "ABORT: organisation $KEEP_ID not found in ${PGDATABASE:-?} on ${PGHOST:-?}." >&2
    echo "       Wrong database, or the org to keep is already gone." >&2
    exit 1
fi

# Pre-flight: the purge must be able to suspend the platform_audit_logs append-only
# trigger for one DELETE. That needs ownership of the table -- on Azure Flexible Server
# membership of azure_pg_admin is NOT sufficient. Check once, up front, instead of
# discovering it 26 failures in.
owner="$(psql_q "select pg_catalog.pg_get_userbyid(c.relowner)
                 from pg_class c where c.oid = 'public.platform_audit_logs'::regclass;")"
can_alter="$(psql_q "select pg_has_role(current_user, c.relowner, 'USAGE')
                     from pg_class c where c.oid = 'public.platform_audit_logs'::regclass;")"

echo "Database : ${PGUSER:-?}@${PGHOST:-?}:${PGPORT:-5432}/${PGDATABASE:-?}"
echo "Audit tbl: public.platform_audit_logs owned by '$owner'; can suspend trigger: $can_alter"

if [[ "$can_alter" != "t" ]]; then
    echo >&2
    echo "ABORT: $(psql_q 'select current_user;') cannot ALTER TABLE public.platform_audit_logs," >&2
    echo "       so the append-only trigger cannot be suspended and every org would fail." >&2
    echo "       Run the purge as '$owner', or grant ownership to your user:" >&2
    echo "         GRANT $owner TO <your_user>;" >&2
    exit 1
fi

echo "Keeping  : $(psql_q "select id || ' - ' || name || ' (' || schema_name || ')' from public.organisations where id = $KEEP_ID;")"
echo

# Portable across bash 3.2 (macOS) -- no mapfile.
TARGETS=()
while IFS= read -r id; do
    [[ -n "$id" ]] && TARGETS+=("$id")
done < <(psql_q "select id from public.organisations where id <> $KEEP_ID order by id;")

if [[ ${#TARGETS[@]} -eq 0 ]]; then
    echo "Nothing to purge."; exit 0
fi

echo "Will purge ${#TARGETS[@]} organisation(s):"
psql -qA -F ' | ' -c "select id, name, schema_name, status from public.organisations where id <> $KEEP_ID order by id;"
echo

if [[ "$MODE" != "--confirm" ]]; then
    echo "Dry run. Re-run with --confirm to purge. THIS IS IRREVERSIBLE."
    exit 0
fi

# The purge suspends the platform_audit_logs append-only trigger for the duration of one
# DELETE, inside the per-org transaction. Verify it is on before we start and after each
# org, and stop immediately if it is ever found off -- an audit guard must never be left
# disabled.
audit_trigger_state() {
    psql_q "select t.tgenabled from pg_trigger t
            where t.tgrelid = 'public.platform_audit_logs'::regclass
              and t.tgname = 'trg_platform_audit_logs_reject_mutation';"
}

if [[ "$(audit_trigger_state)" != "O" ]]; then
    echo "ABORT: platform_audit_logs append-only trigger is not enabled to begin with." >&2
    exit 1
fi

ok=0; failed=0
for org in "${TARGETS[@]}"; do
    if psql -v ON_ERROR_STOP=1 -q -v org_id="$org" -v keep_id="$KEEP_ID" -f "$SQL"; then
        ok=$((ok + 1))
    else
        failed=$((failed + 1))
        echo "!! org $org FAILED and was rolled back; continuing" >&2
    fi

    state="$(audit_trigger_state)"
    if [[ "$state" != "O" ]]; then
        echo "ABORT: append-only trigger left in state '$state' after org $org." >&2
        echo "       Re-enable before continuing:" >&2
        echo "       ALTER TABLE public.platform_audit_logs ENABLE TRIGGER trg_platform_audit_logs_reject_mutation;" >&2
        exit 1
    fi
done

echo
echo "purged=$ok failed=$failed"

# Post-conditions.
remaining="$(psql_q "select count(*) from public.organisations;")"
orphans="$(psql_q "select coalesce(string_agg(n.nspname, ', '), '') from pg_namespace n
                   where n.nspname like 'tenant\\_%'
                     and not exists (select 1 from public.organisations o where o.schema_name = n.nspname);")"

echo "organisations remaining: $remaining"
[[ -n "$orphans" ]] && echo "WARNING orphaned tenant schemas: $orphans"
psql -qA -F ' | ' -c "select id, name, slug, subdomain, schema_name, status from public.organisations order by id;"

echo
echo "Audit archived to public.tenant_audit_archive:"
psql -qA -F ' | ' -c "select tenant_schema, count(*) as archives, sum(row_count) as rows_preserved
                      from public.tenant_audit_archive group by tenant_schema order by 1;"

[[ "$failed" -eq 0 && "$remaining" -eq 1 && -z "$orphans" ]] || exit 1
echo "OK: only organisation $KEEP_ID remains, no orphaned tenant schemas."
