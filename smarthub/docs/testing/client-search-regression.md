# Client search regression

The Admin, Staff and Therapist client lists debounce normalized search input for 300 ms, show an explicit loading status while a request is pending, and show an empty result only after the current query completes. Responses for older query arguments must not populate the new list. Changing search resets pagination; whitespace-only changes retain the current results.

MRN matching supports full numbers and fragments, including `CL-2026`, `2026-03`, and `0395`. Email/phone search keeps its existing matching behavior; numeric phone numbers of 10–15 digits still follow phone search. MRNs and referral numbers remain encrypted at rest.

The fallback search selects only IDs, MRNs and referral numbers instead of hydrating client profiles and associations. The client-list specification resolves those candidates once, sharing the result between page and count queries. Existing tenant and client-scope predicates remain in place. Partial encrypted search still scans these narrow values within the tenant; this is not an indexed substring search and no production latency target is claimed.

## Run

From the backend repository, with Java 17, local Docker and the sibling frontend dependencies installed:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) python3 scripts/test-tenant-fixtures.py --suites qa/backend-client-search-suites.txt
node scripts/test-frontend-lifecycle.mjs --grep 'client search'
```

The backend runner creates and removes a disposable PostgreSQL database. The mounted browser tests use synthetic HTTP responses and real client-page components, with no production access.

## Covered cases

- One request for rapid MRN typing rather than a request per keystroke.
- No premature empty result during a delayed search, followed by the true empty result when complete.
- Clearing the query restores clients.
- An older response cannot overwrite a newer partial-MRN result.
- Search starts from page one, and whitespace does not clear unchanged results.
- Full/prefix/fragment MRN normalization and encrypted referral-number matching.
- Narrow database scan executes one query with no client entity or collection hydration.
- Reusing the specification for counts does not repeat the candidate scan; an additional client restriction still narrows results.
- Deleted clients are excluded from the fallback candidate scan.

The pre-fix browser reproduction issued twelve requests for a twelve-character MRN. The pre-fix unit run failed the numeric-fragment, substring-match and narrow-candidate cases. These tests supplement, rather than replace, live latency measurement and the broader client authorization suite.

## Verified repair run

- Backend: 29 tests passed, zero failures/errors/skips. Evidence: `qa-results/tenant-20260913-011721-544f6054/summary.json`; disposable database cleanup confirmed in `cleanup.json`.
- Browser: all six client-search tests passed, covering Admin, Staff and Therapist pages. Evidence: `qa-results/frontend-lifecycle-2026-09-12T20-19-21-846Z/summary.json`.
- Frontend TypeScript, changed-file ESLint and production build passed. Vite retained its large-bundle warning.
- No production deployment or live latency measurement was performed.

## Shared-field format audit

The format audit uses `blind_only`, matching the inspected production setting, and encrypted synthetic clients with full-name, name-token, contact and DOB indexes. It separates supported inputs from explicit known-gap assertions: a passing known-gap test means that input currently returns no match, not that the feature works.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) python3 scripts/test-tenant-fixtures.py --suites qa/backend-client-search-field-suites.txt
node scripts/test-frontend-lifecycle.mjs --grep 'client search field submits'
```

The browser tests verify that all three real client pages submit names, email, phone, DOB and MRN through the same `search` query parameter without corrupting plus signs or formatting. Matching itself is checked separately against PostgreSQL; browser responses are synthetic, not live production searches.

### Format audit results — 13 September 2026

36 database checks passed: 19 positive matching formats, 10 assertions documenting unsupported inputs, 5 no-match/deleted-contact checks and 2 inherited tenant-schema safeguards. Three browser transport checks passed (Admin, Staff, Therapist). Passing the known-gap assertions confirms lack of support; it does not certify those inputs as working.

| Input | Observed result |
|---|---|
| Full name, first name, last name, reordered full-name tokens | Matches; case and whitespace normalized |
| Full email | Matches; case and surrounding whitespace normalized |
| Full national/international/formatted phone and work phone | Matches |
| Full MRN, case/spacing variants, prefix and numeric fragment | Matches |
| DOB ISO, MM/DD/YYYY, DD/MM/YYYY | No match despite matching DOB stored and indexed |
| Partial first/last name | No match |
| Partial email/local part/domain | No match |
| Last four phone digits | Interpreted as an MRN fragment; does not match phone |
| Combined name and email | No match |
| Removed email contact | Correctly excluded |

Backend evidence: `qa-results/tenant-20260913-013414-0a07ace7/summary.json`; cleanup confirmed. Browser evidence: `qa-results/frontend-lifecycle-2026-09-12T20-34-51-443Z/summary.json`. Production search mode was inspected as `blind_only`; no production patient records were queried, and existing production index completeness was not assessed. No application behavior or deployment was changed during this audit.

## DOB and name-prefix enhancement

The shared field now accepts strict DOB values in `YYYY-MM-DD` and `MM/DD/YYYY` formats. Slash dates always mean month/day/year; invalid dates do not roll into another calendar date. DOB uses the existing tenant-bound DOB digest. Names match complete words or word prefixes of at least two characters, including multiple prefixes such as `Ja Do`. Full email and phone matching remains unchanged.

Name-prefix digests share the existing name-token index table; plaintext prefixes are not stored. Both normal client writes and the scheduler use the same term generator. A separate `NAME_PREFIX_DIGESTS` job starts from zero for existing tenants, even when their older name-token job is complete. Existing records gain prefix matching as that background rebuild progresses after deployment. This adds index rows proportional to name length; it does not add a profile-decryption scan to name searches.

The Admin, Staff and Therapist search boxes increase from 20.75rem to 24.9rem (20%) in the full client-list view. The constrained half-panel view keeps its existing sizing. The placeholder includes DOB and the field help states both accepted date formats.

Browser enhancement verification: all nine client-search checks passed in `qa-results/frontend-lifecycle-2026-09-12T20-47-04-471Z/summary.json`. Changed-page ESLint and the frontend production build passed; Vite retains its existing bundle-size warning. This enhancement has not been deployed.

Final backend enhancement verification: 52 tests passed with zero failures, errors or skips in `qa-results/tenant-20260913-014805-5fa3e43c/summary.json`; disposable database cleanup confirmed. Coverage includes positive shared-field formats, explicitly unsupported formats, no-match/deleted-contact cases, tenant guards, index row reuse, renaming, month-first slash dates, leap dates, seeding a new prefix job when older jobs are complete, and repeatable rebuilding of encrypted existing records. The initial enhancement red run had five expected database failures and three expected width failures before implementation.
