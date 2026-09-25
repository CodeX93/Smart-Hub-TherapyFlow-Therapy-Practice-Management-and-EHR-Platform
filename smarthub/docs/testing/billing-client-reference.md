# Billing client reference navigation

Billing records and billing history expose `clientReferenceNumber` from the client's referral reference. It is distinct from the MRN and payment reference. The shared billing table displays a nonblank reference below the client name. The reference link opens the same client's Overview tab; the name link opens Billing. Both use the existing role-specific client route and support keyboard activation. Missing references render no label.

The regression is included automatically in the full Maven and Playwright discovery used by `./scripts/test-app.sh full`.

Focused verification:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw '-Dtest=BillingServiceTest#billingResponsesIncludeClientReferenceAndHandleMissingReferral' test
node scripts/test-frontend-lifecycle.mjs --grep 'billing reference'
```

The backend unit test covers both response mappings with and without a referral. The three browser component tests cover Admin, Staff and Therapist, both frontend response mappings, missing/blank references, keyboard activation and distinct Overview/Billing navigation state. They use synthetic API responses, not a live browser-to-database journey.

Browser evidence: `qa-results/frontend-lifecycle-2026-09-12T20-57-32-620Z/summary.json` (3 passed). Frontend production build and changed-file ESLint passed. The broader billing unit suite had a pre-existing `shouldThrowExceptionWhenServiceCodeExists` failure before the backend change: its fixture omits the required service name and hits name validation before duplicate-code validation.

Backend verification after the change: the new mapping test passed; the full `BillingServiceTest` run passed 17 of 18 tests, with only the same pre-existing duplicate-service fixture failure. No application commit, push or deployment was performed for this change.
