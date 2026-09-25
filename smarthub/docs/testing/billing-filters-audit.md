# Billing filter audit and repair

Scope: the shared Admin, Staff and Therapist billing page. Tests use synthetic records and a disposable PostgreSQL database; no production billing data was changed. The reference-link changes were committed and pushed first (backend `280ba7f`, frontend `ad7149b`). Deployment was not verified.

## Original audit: browser checks

All 25 dropdown options were selected through the real controls on each role's page (75 selections): four Billing Status choices, five Payment Status choices, eight Payment Methods, five Client Types and three Session Types. Their values reach the billing endpoint and reset pagination to page zero. This proves transport, not that the server honors the filter.

The browser also verifies default month dates, rejection of reversed dates, same-day ranges, removing only the start-date chip, cancelling an unapplied change and clearing filters.

Confirmed failure in all three roles: choose Billing Status=Pending and Payment Status=Paid. Both chips remain visible, but only `status=pending` is sent. Payment Status is silently discarded by `billingStatus || paymentStatus`; the API also has only the unified `status` parameter. The original audit kept these assertions failing to expose the regression.

Evidence: `qa-results/frontend-lifecycle-2026-09-12T21-07-58-448Z/summary.json` (three option-matrix tests passed; three combined-status tests failed only at the missing Payment Status assertion).

## Original audit: backend coverage

`BillingFiltersIntegrationTest` exercises the real specification against encrypted clients, sessions, billing rows and payments in PostgreSQL. It checks every dropdown value plus API client ID, therapist ID, service code, minimum/maximum amounts, session-date boundaries in America/Toronto, combined constraints, reset and client-search inputs. Date filtering uses the actual session date, not the displayed Billing date; this matches the existing documented API behavior.

The public service rejects `status=failed`: its parser accepts only pending, partial, paid, denied and refunded. The UI nevertheless offers Failed under Payment Status. This is an API/UI contract mismatch.

The statistics cards send date bounds only. Status, method, client type and session type therefore narrow the table without narrowing the cards. Treat the cards as date-scoped totals until product behavior is clarified.

## Repeat

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) python3 scripts/test-tenant-fixtures.py --suites qa/backend-billing-filter-suites.txt
node scripts/test-frontend-lifecycle.mjs --grep 'billing filters'
```

These tests are discovered by the full test runner. Audit tests and documentation remain local; billing application logic was not modified by this audit.

## Original confirmed database defects

- **Client Type — Individual, Couple, Family:** the predicate compares the encrypted string `Client.clientType` against a `ClientType` enum. Hibernate rejects the parameter with a type-conversion error. Merely changing the enum to a string would still require checking encrypted-field search behavior.
- **Client Type — Refugee, MVA:** the enum does not recognize these values. The catch block drops the criterion; the query returns the matching fixture and an unrelated control client.
- **Session Type — In Person:** the predicate compares string `Session.sessionType` against a `SessionType` enum and Hibernate rejects it. The canonical API value `online` has the same problem.
- **Session Type — Virtual, Telehealth:** these dropdown values are unrecognized by `SessionType.fromValue`; the criterion is silently dropped and unrelated session modes are returned.

Repair order: align Client Type storage/search and option values; align Session Type values and predicate types; separate payment-status filtering from billing-status filtering and support/reject offered options consistently; then rerun the failing matrix and full regression suite. Invalid filter values should produce a clear validation error rather than silently broadening results.

## Original audit results

Production-style `blind_only` run: `qa-results/tenant-20260913-021559-5f9cda68/summary.json` — 40 tests, 30 passed, 4 assertion failures, 6 query/parser errors, no skips; source fingerprint unchanged and run-owned database removed. The ten unsuccessful cases are Failed status, all five Client Type options, all three Session Type options, and canonical API mode `online`. Client name/prefix, DOB, MRN, full email and full phone searches all passed. So did all eight payment methods, the five accepted statuses, client/therapist/service constraints, inclusive amount limits, date boundaries, combined constraints, reset and inherited tenant guards.

These results are isolated predicate/database and browser-component evidence. They do not establish live production performance, provider behavior, every possible filter combination, or role authorization beyond existing inherited tenant safeguards. At audit time, these regression cases failed on the confirmed defects; the repair results below supersede that state.


## Repair behavior

The billing list now sends `status` (billing) and `paymentStatus` independently and combines their predicates with AND. Payment Pending/Partial/Paid follow the same paid-amount/discounted-total rules as response presentation, excluding denied/cancelled bills. Failed and Refunded match recorded payment events. A failed attempt can coexist with an unpaid balance; those filters describe different facts.

Client Type selects only active client IDs and encrypted type values, then compares decrypted values without hydrating profile graphs. This narrow tenant scan happens once while building the specification, before content/count evaluation. It remains a scan, not an indexed lookup. All six displayed types (Individual, Couple, Family, Group, Refugee, MVA) are supported. Other values return a validation error.

The Session Type dropdown now offers In Person and Online, matching the stored modes. Legacy `in_person`, `virtual` and `telehealth` query aliases remain supported; both virtual aliases mean Online because there is no separate stored Telehealth mode. Invalid modes, payment methods/statuses, reversed dates and invalid amount ranges return validation errors.

The existing duplicate-service unit fixture now includes a service name so it reaches the intended duplicate-code check. No billing/payment mutation rules were changed.

Repair browser evidence: `qa-results/frontend-lifecycle-2026-09-12T21-23-05-661Z/summary.json` — all 15 billing browser regressions passed. Changed-file ESLint and the frontend production build passed; the existing bundle-size warning remains. The backend gate now includes billing service, rules, payments, guards, HTTP API and client-search tests in addition to the database filter matrix.


## Final repair verification

Backend evidence: `qa-results/tenant-20260913-022956-d2cbaa38/summary.json` — **100 tests passed**, zero failures/errors/skips, all requested suites present, source fingerprint unchanged, disposable database removed. This includes 45 database filter checks and authenticated HTTP tests proving independent status filtering and HTTP 400 for invalid values/ranges. Browser: **15 billing tests passed**, including every updated dropdown option across all three roles. Frontend production build and changed-file lint passed. Diff whitespace checks passed in both repositories.

All billing gates run for this repair are green. This is not a claim that the entire application's full suite was rerun. Repair changes are local and have not been committed, pushed or deployed.
