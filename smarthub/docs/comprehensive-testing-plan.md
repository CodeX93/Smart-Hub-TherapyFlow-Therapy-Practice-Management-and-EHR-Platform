# SmartHub comprehensive testing plan

Status: reusable automation implemented for an initial subset; full application coverage remains incomplete. See [runner guide](../qa/README.md) for commands and limitations.
Created: 2026-09-11.

Expanded source discovery on 2026-09-11: see [detailed workflow test specification](testing/detailed-workflow-test-spec.md) and [source-discovered inventory](testing/source-inventory.md). Discovery covers both repositories and explicitly includes role-specific screens, Zoom, microphone/live/chunk transcription, speaker diarization, discount and invoice-policy rules, individual filters, request fields, and background entry points. These artifacts are not test results; Phase 1 reconciliation and execution remain outstanding.

## Objective and boundaries

Establish repeatable evidence that SmartHub's supported features work through the UI, API, database, and external integrations, with correct permissions and failure handling.

“Every function” means every supported user action, API operation, and important business-rule branch has a documented verification decision. This does not promise exhaustive testing of every possible input combination. Line and branch coverage supplement workflow evidence.

Scope includes the backend in `/Users/apple/IdeaProjects/smarthub` and frontend in `/Users/apple/IdeaProjects/trappy-flow-frontend`. Inventory supported roles, plans, feature flags, and configurations from current code before finalizing scenarios.

This document authorizes no production execution, external messages, purchases, deployments, or customer-data changes. Implementation should use synthetic fixtures, disposable databases, and sandbox integrations. Keep credentials and patient data out of source, reports, screenshots, and traces.

## Current baseline

Verified by repository inspection, not a fresh test run:

- Backend has unit, API, security, repository/service integration, workflow, architecture, migration, and performance test files. Their existence does not establish that they currently pass or assert complete workflows.
- Maven targets Java 17. Surefire excludes `*IntegrationTest`; Failsafe includes `*IntegrationTest` and `*IT`. `./mvnw clean verify` is the broad lifecycle entry point once isolated dependencies are configured.
- JaCoCo is configured with package line coverage of 80%, package branch coverage of 75%, and class line coverage of 80%, subject to configured exclusions. Verify actual results and exclusion effectiveness before using these as release evidence.
- The frontend package exposes build and lint scripts, but no configured browser-test runner.
- `scripts/test-billing-e2e.sh` provisions disposable PostgreSQL and selects billing integration/policy tests. `scripts/test-billing-ui-policy.mjs` runs real frontend helpers; it is not browser E2E coverage.
- `src/test/resources/application-test.yaml` uses Hibernate `create-drop` with Flyway disabled. Its tests do not prove production migration compatibility.
- `docs/billing-e2e-qa-2026-09-10.md` records billing results and limitations. Reproduce results against the tested revision; do not carry forward historical passes as current evidence.

## Coverage tracking

Create `docs/testing/feature-coverage.csv` during Phase 1. One row represents a scenario, with these columns:

`scenario_id, module, feature, ui_route, api_method_path, role, tenant_scope, plan_or_flag, preconditions, action, expected_result, priority, test_layer, test_file_or_manual_steps, status, evidence, tested_revision`

Use statuses: `unmapped`, `missing`, `implemented-not-run`, `passed`, `failed`, `blocked`, `not-applicable`. Record the reason for blocked/not-applicable scenarios. Track backend and frontend revisions in evidence. Never label skipped tests as passed.

For each feature, consider success, validation, permission denial, tenant isolation, empty states, boundaries, repeat submissions, concurrency, dependency failure, and persistence after refresh. Mark irrelevant dimensions explicitly instead of generating mechanical tests for every combination.

Priority definitions:

- **P0:** access control, tenant isolation, authentication, financial correctness, data loss, and core client-to-session-to-invoice workflow.
- **P1:** remaining supported workflows, failure recovery, important UI behavior, and integration contracts.
- **P2:** secondary combinations, presentation variations, and lower-risk edge cases. Performance cases become P0/P1 when they threaten core availability.

## Phase 1 — Inventory and assess existing coverage

- [ ] Build an action-level inventory, not just a module list. A passing task-creation test cannot mark task comments, task history, or overdue processing covered.
- [ ] Reconcile the inventory in both directions: every UI action maps to behavior/tests, and every endpoint, background trigger, and configuration effect maps to scenarios even when it has no visible button.
- [ ] Compare the completed inventory with every role's navigation and nested tabs/dialogs. Record UI-only and API-only features explicitly; investigate mismatches.
- [ ] Enumerate frontend routes, menus, forms, dialogs, actions, and role-specific screens.
- [ ] Enumerate backend endpoints, authorization rules, service operations, scheduled jobs, event handlers, webhooks, and websocket flows.
- [ ] Map roles, custom permissions, organization scoping, caseload restrictions, subscriptions, and feature flags to affected actions.
- [ ] Read existing test assertions and fixtures; distinguish genuine persisted workflows from mocked behavior and list-only checks.
- [ ] Populate the coverage matrix and assign P0/P1/P2 priorities.
- [ ] Record obsolete fixtures, disabled tests, missing assertions, nondeterministic tests, and environment blockers without silently excluding them.
- [ ] Identify where unit tests are sufficient and where database/API/browser or manual evidence is required.

Deliverables: feature coverage matrix and prioritized gap list.

Exit criterion: every inventoried feature has a scenario or an explicit verification decision; all P0 scenarios have expected outcomes and an assigned test layer.

### Mandatory detailed workflow inventory

The following actions are required inventory targets, based on the named features and current controller routes inspected on 2026-09-11. They are planned scenarios, not verified behavior or an exhaustive application inventory. Read service rules and frontend flows to establish exact allowed states, recipients, permissions, and expected side effects before implementing assertions. Apply the same level of detail to all other modules.

| Area | Separate actions and behavior to map and test |
| --- | --- |
| Tasks | Create, view, edit, delete, assign/reassign, due dates, supported status transitions, overdue processing, client-specific lists, filtering, history, recent/upcoming lists, pending counts and statistics |
| Task comments | List, add, edit, delete; author and permitted-role rules; wrong task/comment ID combinations; empty/oversized content validation; safe text rendering; ordering; persistence on reopen; access after reassignment/deletion; notification effects if defined by the product |
| Checklists | Template create/edit/delete; item create/edit/delete and ordering if supported; assign to a client; bulk assign; update assigned items; filter assignments; compliance reporting; effect of template changes on already assigned clients |
| Notifications | Each configured event and trigger; intended recipients and excluded users; in-app list, unread counts, mark one/all read, delete, preferences; channel selection/suppression; delivery failure/retry and duplicate-event handling; staff and client-portal scope |
| Notification administration | Trigger and template CRUD; placeholders and invalid configuration; setup health/coverage/events; action metadata and destination links; seed/sync behavior; broadcasts and cleanup permissions. Use local capture/test doubles for automated delivery |
| System settings | Practice configuration read/update; each editable field's validation, authorization, persistence and downstream effect; timezone effects; category and option CRUD; usage checks; option ordering; behavior when referenced options change or are deleted; organization isolation |
| Forms | Template/field configuration supported by the UI/API; versioning; individual, bulk and multi-template assignment; assignment update/delete/status changes; submit/read/update responses; required-field rules; signatures; review; permissions and effects of later template edits on existing responses |
| Document workflow | Upload → list/detail → preview/view/download → share/access check → review queue/action → updated review summary/dashboard; deletion and missing files; invalid type/size/content; client-portal visibility and permission revocation |
| Session notes | Create/list/detail/edit/delete; session and client linkage; finalize; amendment create/history; finalization editing rules; PDF content; transcription and audio reprocessing success/failure; AI templates; cross-client/session denial; persistence and duplicate submission |
| General notes and clinical records | Inventory general notes separately from session notes, then assessments, reports, report sources and supporting files; follow each record through its supported lifecycle and permissions |

For each settings field, the assertion must check its actual effect where consumed, not merely a successful save response. For every notification-producing action, map the originating action, trigger/configuration, recipient, channel, payload/link, delivery outcome, and read state. Do not assume every action produces a notification.

Required connected journeys, where supported by current product rules:

- [ ] Create and assign task → add comment → authorized recipient sees the task/comment → verify configured notification behavior → edit/delete comment → reopen task and verify persisted state and counts.
- [ ] Create checklist template/items → assign to client → complete assigned items → verify compliance report → change template and verify documented behavior of existing assignments.
- [ ] Create/version form template → assign → enter responses → validate → sign → review → verify the expected final state and visibility from each supported role.
- [ ] Upload client document → review → share → verify portal preview/download → revoke sharing → verify direct file access is denied.
- [ ] Create session note → edit draft → finalize → exercise allowed/denied edits → append amendment → inspect history and PDF.
- [ ] Change practice setting or system option → reload → open every affected workflow → verify its new behavior and organization scope.
- [ ] Trigger a configured notification → verify eligible recipient/channel → change preference/configuration → trigger again → verify suppression/routing rules and unread counts.

Completion rule: a module remains partially covered while any inventoried action lacks a passing test or an explicit manual/blocked/not-applicable decision. Report automated and manual coverage separately. Prioritization controls execution order; it must not silently remove smaller features from scope.

## Phase 2 — Establish a reproducible QA environment and baseline

- [ ] Create a documented local QA setup using Java 17, disposable PostgreSQL, isolated storage, and Redis where required by the behavior under test.
- [ ] Isolate environment variables from production credentials and make destructive database operations refuse non-QA targets.
- [ ] Provide synthetic fixtures for at least two organizations, supported roles, a restricted custom role, assigned/unassigned clients, and relevant plan/flag states.
- [ ] Make fixture creation deterministic with unique run IDs and narrowly scoped cleanup that cannot delete unrelated data.
- [ ] Use controlled clocks for time-dependent unit/integration tests and deterministic dates for browser fixtures.
- [ ] Stub external services for routine regression tests; route email/SMS to local capture or test doubles.
- [ ] Run the existing billing scripts, then the full backend suite and frontend build/lint. Save actual exits, failures, skips, counts, and revision IDs.
- [ ] Diagnose baseline failures individually. Repair test fixtures or environment where appropriate; log product defects separately.
- [ ] Regenerate the coverage report after integration tests so the reported execution data includes the intended suite.

Initial commands, after installing dependencies and configuring isolated QA services:

```sh
cd /Users/apple/IdeaProjects/smarthub
bash scripts/test-billing-e2e.sh
node scripts/test-billing-ui-policy.mjs /Users/apple/IdeaProjects/trappy-flow-frontend

# Requires explicitly isolated DB_URL, DB_USERNAME, DB_PASSWORD and QA dependencies.
./mvnw clean verify
./mvnw jacoco:report

cd /Users/apple/IdeaProjects/trappy-flow-frontend
npm run build
npm run lint
```

Deliverables: QA setup instructions, fixture utilities, and baseline execution report.

Exit criterion: another developer can reproduce the baseline without production access; every failing or blocked baseline check has a recorded cause or investigation item.

## Phase 3 — Close P0 backend and permission gaps

- [ ] Verify login, MFA challenges/recovery, logout, token refresh/revocation, expired sessions, and disabled accounts with meaningful state assertions.
- [ ] Verify organization isolation and role/caseload restrictions on reads, writes, downloads, exports, searches, and direct object-ID access.
- [ ] Verify client creation/update, assignment, file closure/reopening, and deletion semantics against persisted data and dependent records.
- [ ] Verify scheduling conflicts, therapist working hours, room availability, recurring-session changes, cancellation, and status transitions.
- [ ] Verify Completed/No-show eligibility before, at, and after the allowed instant; cancellation rules; billing failure rollback; and response consistency after commit.
- [ ] Verify invoice rates, discounts, payer allocations, cumulative payment updates, partial/full/split payments, duplicates, and simultaneous requests.
- [ ] Verify clinical/document access and data persistence, including denied access through direct API and download requests.
- [ ] Exercise failure paths and concurrency using the real database where transaction behavior matters.

Deliverables: targeted regression tests and matrix evidence for P0 backend scenarios.

Exit criterion: all P0 backend scenarios pass or have an explicitly accepted release exception with owner, rationale, and expiry. Coverage thresholds alone cannot waive a failed P0 scenario.

## Phase 4 — Automate browser workflows

- [ ] Add a browser-test harness to the frontend, with isolated base URLs, role fixtures, stable selectors, and traces/screenshots on failure.
- [ ] Select and document the browser automation tool during implementation. Keep test setup separate from assertions of the actual UI behavior.
- [ ] Cover real password login and MFA with dedicated test accounts; authenticated fixture shortcuts may support other scenarios but do not count as login tests.
- [ ] Implement the core journey: admin creates therapist and availability → creates/assigns client → schedules session → therapist records outcome and note → invoice appears → payment is recorded → client portal shows permitted information.
- [ ] Verify the outcome across calendar, client profile, billing lists, and portal, including reload and a fresh browser context where relevant.
- [ ] Cover denied actions and deep links for restricted users, including a second organization.
- [ ] Verify validation feedback, empty/loading/error states, duplicate clicks, interrupted requests, and recovery without duplicate records.
- [ ] Add critical desktop/mobile viewport checks and keyboard navigation for essential forms/dialogs.
- [ ] Run a primary-browser smoke suite first, then expand supported-browser coverage based on actual product support requirements.

Deliverables: repeatable browser smoke suite, role-based workflow suite, and failure artifacts.

Exit criterion: core journeys pass against the real QA frontend/backend with persisted outcomes verified and without unexplained flaky retries.

## Phase 5 — Complete remaining feature and integration coverage

Use the inventory to expand these groups; this table is a starting scope, not a substitute for the endpoint/UI inventory.

| Feature group | Required scenario families |
| --- | --- |
| Clients | Search/filter/pagination, contacts/addresses, insurance, consent, duplicates, history, caseload changes |
| Scheduling | Working-hours persistence, room conflicts, recurrence edits, history ordering, timezone differences, midnight and daylight-saving boundaries |
| Clinical | Notes, assessments, forms and versions, templates, reports, supporting files, transcripts, AI success and failure |
| Documents | Upload validation, previews/downloads, review, library, access changes, missing/corrupt files |
| Tasks and notifications | Assignment, status changes, checklists, scheduled delivery, retries, inbound events, unread/read state |
| Portal | Identity, scope, documents, invoices, notifications, expired access, cross-client denial |
| Administration | User lifecycle, custom roles, practice options, services, integrations, dashboards, audit history |
| Platform administration | Tenant routing, subscriptions/add-ons, usage, limits, feature flags, impersonation, operations and access boundaries |
| Public site | Booking, consultation, published CMS content, validation, duplicate submissions, visibility rules |
| Integrations | Stripe, email, SMS, Zoom, AI and storage contracts; timeouts, malformed responses, retries, duplicate/out-of-order webhooks |

- [ ] Use controlled integration responses in routine CI and separate sandbox checks for actual provider connectivity.
- [ ] Verify asynchronous effects with bounded polling, explicit completion criteria, and correlation IDs rather than arbitrary sleeps.
- [ ] Exercise production-relevant configuration differences: rate limiting, Redis behavior, secure cookies, storage permissions, and websocket setup.
- [ ] Verify logs, audit entries, screenshots, and exported evidence do not disclose secrets or patient data.
- [ ] Complete browser/manual checks for workflows that cannot be meaningfully covered by backend tests alone.

Deliverables: P1/P2 regression suites, sandbox integration runbook, and updated matrix.

Exit criterion: all supported features have an implemented verification path; integration mocks and live sandbox evidence are clearly distinguished.

## Phase 6 — Validate migrations, recovery, and performance

- [ ] Run actual Flyway public/tenant migrations against a fresh disposable database and a representative prior schema with synthetic data.
- [ ] Verify schema constraints, indexes, tenant provisioning, and application startup/read/write compatibility after migration.
- [ ] Exercise ClientHub migration dry runs, execution, reruns, resume, reconciliation, and failure recovery using synthetic source fixtures; preserve text-only transcript scope and test document binary transfers separately.
- [ ] Validate backup restoration into an isolated environment and reconcile restored records/files.
- [ ] Define realistic data volumes, concurrent users, latency/error budgets, and background-job completion limits before performance execution.
- [ ] Inspect existing performance tests before reuse; verify that they model realistic requests and assert useful limits.
- [ ] Run focused load, stress, and endurance checks against isolated infrastructure; inspect database queries, resource use, timeouts, and recovery.

Deliverables: migration/recovery report and measured performance report against agreed budgets.

Exit criterion: migrations and restoration preserve expected data; performance meets recorded budgets or has explicitly documented release exceptions.

## Phase 7 — Enforce regression checks and release evidence

- [ ] Discover the actual backend/frontend build and deployment pipelines; integrate checks into the active CI platform.
- [ ] Pull requests: compile/build, lint, fast backend tests, security/tenant checks, and targeted regression tests. Include P0 database tests when practical.
- [ ] Main branch or scheduled QA: full backend integration suite, browser journeys, migration checks, and coverage report.
- [ ] Scheduled isolated runs: sandbox integrations, supported-browser matrix, performance, and recovery exercises at an agreed cadence.
- [ ] Publish test counts, failures, skips, coverage, browser artifacts, and exact tested revisions with restricted retention/access where needed.
- [ ] Fail the relevant gate on test failures, missing required suites, or unapproved coverage regressions. Quarantined tests remain visible gaps.
- [ ] Produce a release report showing scenario totals by priority/status, unresolved defects, exceptions, and environment limitations.

Exit criterion: required tests run reproducibly, failures block the relevant release gate, and the report allows a reviewer to distinguish passed, untested, and waived behavior.

## Order of work and completion

Start with Phase 1, then establish Phase 2 before trusting test execution. Close P0 backend gaps and core browser journeys next. Expand remaining features, validate migration/recovery/performance, and complete CI gates. Basic CI can be added earlier once suites are stable.

Estimate effort after the inventory and baseline run reveal the scenario count, failing fixtures, and integration dependencies. Track progress by verified scenarios rather than number of test files.

The plan is complete when every inventoried supported feature has a verification decision, required scenarios pass on identified backend/frontend revisions, unresolved failures and exceptions are explicit, and the test environment plus execution instructions are reproducible. A green build, passing billing harness, or coverage percentage alone does not satisfy this criterion.
