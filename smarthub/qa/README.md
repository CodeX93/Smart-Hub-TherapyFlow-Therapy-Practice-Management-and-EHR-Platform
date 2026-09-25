# Repeatable SmartHub QA

Run from the backend checkout. Backend modes require Java 17, Node/npm, a **local** Docker daemon and the frontend checkout. The focused frontend component runner below requires only Node/npm, frontend dependencies and installed Chromium. The runner is tested on macOS and intended for Linux shell runners too. Windows native execution is not currently supported.

## Install once

```sh
npm ci
npm run test:app:install
npm --prefix ../trappy-flow-frontend ci
```

On Linux install Chromium's OS libraries too (`npx playwright install --with-deps chromium`, using the appropriate system privileges). Set `JAVA_HOME` to JDK 17; macOS selects installed Java 17 automatically.

## Run later

```sh
./scripts/test-app.sh smoke
./scripts/test-app.sh full
./scripts/test-app.sh browser
./scripts/test-app.sh integrations
```

For a different frontend checkout:

```sh
QA_FRONTEND_ROOT=/absolute/path/to/trappy-flow-frontend ./scripts/test-app.sh full
```

Equivalent npm commands: `npm run test:app`, `npm run test:app:full`, `npm run test:app:browser`, and `npm run test:app:integrations`. `npm test` verifies the report generator itself, not the application.

The full backend stage has a 30-minute budget for clean compilation, Surefire and Failsafe. Other stage budgets and the load tests' response-time limits are unchanged. A timeout or failed assertion still fails the report.

Run timing checks without competing builds or desktop indexing. On macOS, the backend stage records `pmset -g therm` every 30 seconds in `host-capacity.log`; a reduced `CPU_Speed_Limit` helps identify a machine-constrained run. These diagnostics never bypass a failed assertion. Let the machine recover before repeating a timing failure. For a large local verification checkout, a temporary `.noindex` directory outside the IDE project avoids indexing generated builds.

The browser backend is compiled in a separate, reported stage before its four-minute readiness deadline starts. Its dedicated Surefire invocation runs the launcher without the test-phase JaCoCo report, so the 20-second shutdown gate measures application/JVM exit. Full backend coverage is still collected by the normal Maven verification and coverage stages. The report records the browser backend's shutdown duration.

The sequential client-read load check validates 100 fixed warm-up requests, then measures 100 more requests against the unchanged **average below 100 ms** limit. Both phases and the measured first/last ten, median and maximum timings are logged. This measures sustained local MockMvc/PostgreSQL reads; it does not certify cold-request latency or production network capacity. Explicit warm-up avoids relying on whichever API tests happened to execute earlier; JVM execution profiles can differ across benchmark runs ([OpenJDK JMH discussion](https://github.com/openjdk/jmh/blob/master/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_12_Forking.java)).

For the mounted frontend lifecycle regressions added in Step 6:

```sh
npm run test:app:frontend
# Optional focused rerun; Playwright CLI arguments are forwarded.
npm run test:app:frontend -- --grep 'task|billing'
```

This runs Chromium component tests using actual frontend hooks, RTK Query, forms and controls with synthetic API responses. It checks task/billing pagination, client/filter resets, edits/deletions on loaded pages, abandoned React renders and StrictMode, admin/therapist task dialogs, organisation input registration, toast timers, action menus and drawn signatures. It also checks confirmed notification reads and failed updates, report and AI-template drafts, microphone cleanup including delayed permission, assessment text/textarea/voice mic dictation fill/cancel/confirm, email formatting, provider-setting isolation and session-note edit/cancel/refetch behavior. It does not start a backend or contact providers. Results, traces on failure and source-consistency evidence are written under `qa-results/frontend-lifecycle-*/`. `QA_FRONTEND_ROOT` selects another checkout. These same tests are automatically included in the regular `smoke`, `browser` and `full` Playwright runs; the focused command does not replace full frontend lint/type/build checks or real-backend journeys.

The fixture entrypoint is served only by `qa/vite.config.mjs`; it is outside frontend application source and is not added to application routes. Browser requests to remote hosts are blocked. The standalone runner stops its Vite server and fails if source inputs change during the run.

| Mode | What actually runs |
| --- | --- |
| smoke | Selected backend rule/security/transcript/task/form/checklist/notification/Zoom unit/contract suites, database billing regression, frontend billing helpers, source discovery, frontend typecheck/build and implemented API/browser checks |
| full | All Maven Surefire and Failsafe tests through `clean verify`, JaCoCo report, frontend helpers/typecheck/lint/build, discovery and implemented API/browser checks |
| browser | Frontend helpers/discovery/typecheck/build and the real HTTP test application plus Playwright; useful for focused browser reruns |
| integrations | Existing local Zoom/Stripe/SMS/email/AI contract/unit tests plus frontend helper checks and discovery. **No live-provider delivery/payment/meeting checks** |

Full runs continue through test failures to collect independent results. Maven's `maven.test.failure.ignore` is used only to reach later lifecycle phases; parsed JUnit failures still fail the overall runner. Missing test evidence and Playwright retries/unexpected results cannot count as clean passes.

The September 11 Step 6 baseline passed all 1,306 executed backend/API/browser tests, but `full` still exits 1 because the existing JaCoCo gate requires 80% line and 75% branch coverage per package. Aggregate coverage was 29.36% lines and 21.98% branches. Coverage expansion is the next stage; the gate remains enforced. See `qa/triage-2026-09-11.md` for the verified repair scope and accepted deployment follow-up.

## Read the result

The terminal prints `qa-results/<run-id>/index.html`. Open that file for stage status, individual test evidence and the scope ledger. `qa-results/latest.json` points to the newest report. Each run retains:

- `summary.json` and `index.html`: combined result and coverage limitations.
- Stage logs and separately scoped Maven builds/reports, including JaCoCo for full runs.
- `playwright/index.html`, `playwright.json`, and failed-browser traces/screenshots/video when generated.
- `inventory/`: source inventory snapshot from both working trees.

Exit `0` means the **implemented checks selected by that mode** passed. It does not mean every SmartHub feature was tested. Exit `1` means a failed/blocked stage or test, `2` means the optional completeness gate was not met, and `130` means interruption.

```sh
./scripts/test-app.sh full --require-complete
```

The completeness gate currently fails deliberately: full application coverage is not implemented. `scope.json` preserves the detailed scenario families as partial/unverified. Related passing tests do not automatically complete a family. Use the generated inventory to map each remaining action and filter; do not turn scope entries green based on file names or one happy-path assertion.

## Isolation and reproducibility

The runner creates a labeled PostgreSQL 15 container on a random loopback port, generates disposable credentials, runs with a restricted environment, and removes its own database/runtime credentials at completion. It does not accept a production DB URL or existing backend URL. Browser requests to remote hosts are blocked; Vite ignores frontend `.env` files and proxies application requests to the local test backend.

The real backend runs through `QaApplicationHarnessTest` on the test classpath. It contains no production fixture endpoint, no authentication bypass, and is disabled unless explicitly launched by this runner. It verifies a run-owned database marker before Spring starts. Fixtures have two isolated tenant schemas and admin, therapist, restricted staff and client identities. Browser login uses real passwords/JWTs. The browser harness uses `blind_only` name search, matching the local application profile, and seeds name indexes through the real `BlindIndexService`; other search configurations require separate checks. Passwords are random and only available in the private runtime directory during the run; artifacts may contain disposable synthetic fixture data.

Automatic maintenance and outbound email are mocked/disabled in the browser harness. Provider responses in existing unit tests are mocked. The browser database is Hibernate-generated with cloned tenant tables; cloning does **not** reproduce all Flyway foreign keys, native migration objects or production schema history. This is not migration certification. Other existing tests retain their own documented configuration; failures are surfaced.

The runner records both git revisions, tracked working-tree changes, and a source fingerprint. A run fails its source-consistency check if tracked test/application inputs change during execution. Existing frontend changes are built and tested as they are; no reset or checkout is performed. A workspace lock prevents simultaneous runners from competing for frontend compiler caches. On an unclean crash inspect `qa-results/.runner-lock/owner.json` before removing a stale lock; run-owned Docker containers are labeled `smarthub.qa.run`.

## Extend coverage

1. Pick an unmapped action/filter from the inventory and a scenario ID from the detailed specification.
2. Add backend assertions in the appropriate existing test package, or browser/API tests under `qa/browser`.
3. Include `[SCENARIO-ID]` in the Playwright test title and update the scope entry's explicit evidence patterns for reviewed backend tests.
4. Use synthetic records and unique per-test names. Authenticate normally, verify exact persisted outcomes and denial cases, and avoid tests depending on another test's execution.
5. Run the relevant mode and inspect real evidence before expanding coverage claims.

Physical two-person audio capture, live provider connectivity, clinical correctness, performance budgets and restore drills remain separate verification tracks. The `integrations` mode does not silently substitute for those checks.

## CI

Use the same entrypoint on a dedicated Linux/macOS shell runner with Java 17, Node and local Docker. Check out both repositories at explicit revisions, install both lockfiles and Playwright, then run `./scripts/test-app.sh full`. Set `QA_FRONTEND_ROOT` to that job's frontend checkout. Publish `qa-results/` with `when: always`, retaining access controls appropriate for test artifacts. Do not share one workspace between concurrent jobs.

The existing GitLab deployment configuration is unchanged. A hosted job needs the frontend repository checkout/credentials and a suitable runner configured before this cross-repository suite can be wired into CI. No deployment or external notification is performed by this harness.


### Tenant/API fixture regression (Step 3)

With `JAVA_HOME` set to JDK 17, run:

```sh
python3 scripts/test-tenant-fixtures.py
```

The command runs every suite in `qa/backend-tenant-regression-suites.txt` on its own local PostgreSQL 15 container. Repository and API contexts use separate databases so Hibernate DDL cannot reset another context's public identity tables. It generates database credentials, does not inherit provider keys or application/database URLs, and removes only the container bearing its run label. Each run creates a fresh `qa-results/tenant-*/` directory with `tests.log`, `summary.json`, Surefire reports, and `cleanup.json`. A missing suite, failure, error, or skipped test makes the command fail.

This focused gate covers tenant repository/service/API fixtures, staff and client-portal authentication boundaries, client/session/billing workflows, and local MockMvc load/endurance checks, plus regressions for repairs found in those paths. Authentication and authorization are real; email delivery and scheduled maintenance are mocked. Asynchronous client-created listeners still run, but their audit/notification side effects are not asserted by this gate; see the triage follow-ups. The test schema uses Hibernate plus the migration-owned MRN counter, encrypted-address widths and V91 control-table SQL; this does not validate the Flyway migration chain. Load checks use an in-process HTTP harness and PostgreSQL, not production network capacity. The full runner also discovers these JUnit suites; Steps 4–6 and other application coverage remain separate.

The local server allows 250 connections and the tenant API pool allows 201 (two idle initially) for the existing 100-worker stress cases and nested audit transactions. The missing primary datasource previously routed requests through the ten-connection migration pool; this gate verifies fixtures and behavior at the stated test capacity, not production pool sizing. Production connection demand from nested audit transactions remains a capacity concern for the architecture/performance review. Both reusable runners apply the local server setting; production pool limits are unchanged; the primary datasource repair restores use of the existing Hikari settings.

### Controller context and architecture regression (Step 4)

With Java 17 selected, run:

```sh
./mvnw -Dtest="$(paste -sd, qa/backend-context-regression-suites.txt)" test
```

These controller slices keep method authorization and exception responses real while mocking service collaborators. The suite also checks organisation ID/slug resolution, controller/service architecture, and migration history. It does not connect to a production database or execute migrations.

`src/test/resources/migration-baseline.sha256` pins the 129 existing migration files from commit `65d9a38`. It records repository history, not proof of deployment. Do not regenerate this baseline to accept SQL edits: add a new higher-version migration with `-- PLATFORM` or `-- TENANT` as its first nonempty line in the corresponding folder. The guardrail rejects modified/deleted/moved history, duplicate versions, older new versions, nested SQL, missing directories and missing/wrong tags. Future migrations stay outside the baseline and are checked by the new-file rules; this guardrail does not execute or prove the semantic safety of their SQL.

### Business contracts and post-commit effects (Step 5)

With Java 17 and local Docker available:

```sh
python3 scripts/test-tenant-fixtures.py --suites qa/backend-business-regression-suites.txt
```

This gate covers billing totals, dunning transactions, usage limits, identity/profile updates, public-versus-tenant auth context, library links, notification migration dependencies, consent and provider contracts. The authenticated input tests check field-specific validation, SQL-search containment, Unicode round trips, JSON content type and opaque local upload paths. HTML escaping is checked at email rendering; these backend assertions do not prove browser XSS protection.

`ClientCreatedEffectsIntegrationTest` verifies committed history, a populated audit snapshot, the welcome-email invocation and the assigned therapist's durable in-app notification. It also checks that the event's contacts/therapist/referral can be read after the repository session closes, and status labels resolve without a caller transaction. Outbound delivery is mocked; notification rows and audit writes use real PostgreSQL. The event reader fetches its required relationships before side effects, so it does not hold a transaction open around email delivery.

The runner accepts a repository `qa/` suite list and retains the same isolated database, credentials, source-fingerprint and cleanup checks as the Step 3 command. A report only counts the selected tests, not every function in SmartHub.

Capacity review: the checked-in primary pool is 20 connections by default and 30 in the `prod` profile unless overridden. Requests can retain one connection while MRN reservation or immediate/after-commit auditing obtains another. Concurrent request workers must leave connection headroom for these nested transactions and background jobs; a pool size alone is not a throughput guarantee. The 100-worker regression uses 201 connections and a 250-connection local server. No production sizing or executor settings are changed by Step 5. A deployment-specific saturation/load test remains a separate performance task; this gate does not certify 1,000 concurrent production requests.

To rerun all backend JUnit test classes, including integration tests:

```sh
python3 scripts/test-tenant-fixtures.py --suites all
```

This explicitly selects `*Test`, `*Tests` and `*IT`, so Surefire also executes the integration classes normally reserved for Failsafe. The only excluded launcher is `QaApplicationHarnessTest`, which starts a persistent server for the separate browser runner. Skipped tests still fail this backend gate. Use `./scripts/test-app.sh full` for the combined frontend/backend/browser run; the backend-only command does not run frontend checks, browser flows or live providers.

### Client portal booking notification regression

Run `JAVA_HOME=$(/usr/libexec/java_home -v 17) python3 scripts/test-tenant-fixtures.py --suites qa/backend-portal-booking-suites.txt` for the focused booking, notification, and shared-caller unit checks. These JUnit classes are also discovered by the full Maven suite.

The regression covers a successful portal booking deferring its notification, notification failure in a separate transaction after commit, no notification after rollback, legacy equality-map conditions, canonical conditions, and invalid rules failing closed. It also covers one email and one in-app notification per recipient despite duplicate triggers, no separate direct booking email, complete service/time-zone/calendar details, and Zoom join details when available. Email and Zoom are mocked; this does not submit a production booking or verify provider delivery.

### Portal activation and MFA login regression

Run `JAVA_HOME=$(/usr/libexec/java_home -v 17) python3 scripts/test-tenant-fixtures.py --suites qa/backend-portal-activation-suites.txt` for activation persistence and authentication regressions. PostgreSQL tests verify activation survives a persistence-context clear and that activation followed by a fresh password login succeeds. Unit tests cover clinic context before MFA enrollment and login challenges; MFA, authentication and JWT suites cover the shared security path. All are discovered by the full Maven suite.

Public portal login regressions also exercise trusted-device and TOTP sign-in without tenant headers, assert the database transaction uses the clinic schema, fetch `/portal/me` using the issued session, and reject replay of a consumed MFA challenge. This catches public-session routing errors that schema-qualified fixture mappings alone can hide.

### Refresh-token reuse regression

Run `JAVA_HOME=$(/usr/libexec/java_home -v 17) python3 scripts/test-tenant-fixtures.py --suites qa/backend-refresh-token-reuse-suites.txt` for refresh-token rotation and theft response. The PostgreSQL API test logs in, rotates once, replays the old token (401) and then requires the newest token of the same family to be rejected too, which proves the family revocation commits even though the replayed request fails. Unit tests cover the reuse path going through its own transaction and expired tokens not triggering it.
