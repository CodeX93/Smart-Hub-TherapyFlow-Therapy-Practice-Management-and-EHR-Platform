# SmartHub E2E coverage audit — 2026-09-11

> Follow-up — clinical/document access checks completed locally: seven new real-JWT/API/database cases verify portal view/download before and after sharing revocation, wrong-client portal denial, other-therapist document/note denial, finalized-note edit/delete rejection with unchanged saved content/timestamps, and report supporting-file ownership filtering. **40 tests across six suites passed**, zero failures/errors/skips; source inputs unchanged and disposable database removed. [Results](/Users/apple/IdeaProjects/smarthub/qa-results/tenant-20260911-184004-d8a559e5/summary.json). [New tests](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/ClinicalDocumentAccessApiTest.java). Rerun with Java 17: `python3 scripts/test-tenant-fixtures.py --suites qa/backend-clinical-access-suites.txt`. Foreign report supporting-file IDs are silently excluded by the current service; this verifies source selection, not a generated AI report or an HTTP rejection for generation. No browser, live-provider, migration, or complete-family verification is claimed. Production code was not changed.

> Follow-up — payment edge cases completed locally: six real-PostgreSQL regressions now cover cumulative split-payment replay and deltas, discount removal/reapply, removing a full outstanding discount, concurrent duplicate submissions, simultaneous client/insurance legs, and stale-edit conflict handling. **63 tests across five suites passed**, zero failures/errors/skips; source inputs stayed unchanged and the disposable database was removed. [Result](/Users/apple/IdeaProjects/smarthub/qa-results/tenant-20260911-182817-96e14974/summary.json). Rerun with Java 17: `python3 scripts/test-tenant-fixtures.py --suites qa/backend-payment-edge-suites.txt`. These are committed-transaction service/database checks, not browser or live-provider tests; invoice pricing is mocked to $100 and schemas are Hibernate-generated. This narrows MONEY-07/09/10 gaps without completing those families. Production code was not changed.

> Follow-up — Stripe placeholder repair completed locally: the three empty tests were replaced with checks for linked payment/transaction entries and partial/full/replayed payments, checkout without payment, and Stripe transaction-history fields. **42 unit tests across seven suites passed**, zero failures/errors/skips, Maven exit 0. [Focused result](/Users/apple/IdeaProjects/smarthub/qa-results/stripe-placeholder-repair/summary.json). [Changed test](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java:414). Production code was not changed or deployed. This resolves the empty-test item only; the original audit and 1,178-test snapshot below remain historical, and the full application suite has not been rerun.

All **73 existing scenario families were reviewed**, with assertion-level evidence and a concrete remaining-work decision for each below. None can yet be marked fully verified. The main gap is the breadth and depth of connected user journeys, not the existence of a test runner.

## Evidence and boundaries

Audit captured at `2026-09-11T13:08:26.242535+00:00`. Backend HEAD `424c7111ffca5a32f17bedb9e323f006319d76ffc96d850fabff76d24483be45`; frontend HEAD `8126f0354a5fd10d269dde94e1e4c477be6322da`. Stage 6 has concurrent working-tree edits, so HEAD alone does not identify its state. This audit reads current test sources and saved results; it does not run tests or certify the current build. Source SHA-256 values and exact historical test names are in [machine-readable evidence](/Users/apple/IdeaProjects/smarthub/docs/testing/e2e-coverage-audit-2026-09-11.json).

- Backend saved JUnit XML: **1,178 reported passes across 234 XML suites**, zero failures/errors/skips. Some suites are nested test classes; this is not 234 independent user journeys. [Backend summary](/Users/apple/IdeaProjects/smarthub/qa-results/tenant-20260911-171827-e777f967/summary.json).
- The earlier **47 passing Playwright checks comprise 7 actual UI journeys, 31 real-HTTP API checks, and 9 frontend helper checks**. No failed/skipped/flaky results in that saved run. These checks and the backend run are separate snapshots; their totals must not be combined into a fresh whole-app pass. [Playwright results](/Users/apple/IdeaProjects/smarthub/qa-results/2026-09-11T08-08-46-895Z-1d123d/playwright.json).
- The browser snapshot records backend revision `f2eb9e809ffa96c6bf234b134e82e733a17aab9e91438af3286733fd5006c66e`, frontend revision `8126f0354a5fd10d269dde94e1e4c477be6322da`, and a stable working-tree fingerprint. It predates Stage 6. [Snapshot metadata](/Users/apple/IdeaProjects/smarthub/qa-results/2026-09-11T08-08-46-895Z-1d123d/summary.json).
- The fixture uses real password/JWT login and a disposable database, but automatic maintenance/outbound email are mocked and schema creation uses Hibernate rather than the production Flyway chain. Provider unit tests use doubles. [Runner limitations](/Users/apple/IdeaProjects/smarthub/qa/README.md:56).
- Stage 6 component tests mount real frontend components with controlled/mocked network responses. They are useful lifecycle regressions, but do not establish real-backend payment, clinical or authentication journeys. Their files/results remain in progress and are not promoted to family completion here. [frontend-lifecycle.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-lifecycle.spec.mjs).

## Findings that change how the green reports should be interpreted

1. **Three Stripe tests pass without executing a test body.** In [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java:411), `shouldUpdatePaymentStatusWithStripeFields`, `shouldCreateSessionBillingWithStripeFields`, and `shouldMapAllStripeFieldsInResponse` contain only comments. They still have `@Test`; the saved XML counts each as passed, not skipped. They provide no assertion of the named Stripe behavior (shared setup still runs). Replace them with assertions on the current payment entities, or explicitly retire obsolete cases with replacement evidence. Do not describe 1,178 as 1,178 meaningful behavior assertions. This does not invalidate the other passing tests, and no adjusted coverage percentage is justified. The separate empty `ApplicationTests.contextLoads` is an intentional Spring startup smoke test; it should not be treated as another Stripe-style defect.
2. **Some workflow names overstate their assertions.** [SessionWorkflowE2ETest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/e2e/SessionWorkflowE2ETest.java:106) has a “booking to completion” case that creates and lists a future session; it never marks it Completed. Its other case creates/updates/cancels. [BillingWorkflowE2ETest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/e2e/BillingWorkflowE2ETest.java:54) creates/reads/lists a bill but does not record payment. Both are MockMvc/database tests, not browser journeys. They are repaired and present in the latest backend report; older notes calling BillingWorkflowE2ETest obsolete are superseded for this snapshot.
3. **The form happy path does not validate required-field rejection.** [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs:45) creates a required field and supplies a value, then checks the saved JSON contains it. Signature, review, missing-answer rejection, and old-version integrity need separate assertions.
4. **The ledger underlinks existing evidence.** Its role-family class lists omit AuthorizationSecurityTest and controller contracts; money-family lists omit Stripe/payment/subscription suites; filter lists are empty despite real API/helper checks. Use the per-family source links below to reconcile it. Do not simply replace “partial” with “passed”.
5. **Backup success is a capability gap.** [SuperAdminOperationsBackupTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminOperationsBackupTest.java:68) asserts `501 / AZURE_BACKUP_UNSUPPORTED` and that queued placeholder jobs fail without a fake storage location. Current [backup implementation](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:166) agrees. Passing these tests proves honest failure behavior, not backup/restore availability.
6. **A local timezone gap has already been addressed in unit tests.** [InvoicePolicyServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/InvoicePolicyServiceTest.java:178) and [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java:314) assert a Toronto local date when UTC is already the following day. What remains is persisted competing-policy precedence, effective-date/DST boundaries, and full UI/billing date agreement; do not re-list the existing unit assertion as absent.
7. **The 73 families are not an exhaustive action matrix.** The discovery artifact includes routes, request fields, jobs and controls that still require reconciliation. There is no dedicated scheduling family in scope.json: scheduling is spread across roles, people, money and Zoom. Add explicit scheduling scenarios (working hours, room conflict, recurrence, reassignment, time boundaries, history) during reconciliation. The discovery counts are an older source snapshot, not current test coverage. [Source inventory](/Users/apple/IdeaProjects/smarthub/docs/testing/source-inventory.md).

## Recommended implementation order

Priorities below order verification work, not confirmed product defects. P0 protects access, money, clinical integrity and recovery; P1 covers remaining supported behavior. Each item should have independent persisted-state expectations and a named evidence result.

| Order | Work | Concrete acceptance condition | Existing files to extend |
| --- | --- | --- | --- |
| 1 | Repair misleading evidence | Replace the 3 empty Stripe cases with current payment-entity assertions; rename or complete the booking-to-completion test. Ensure the recorded test names describe actual checks. | [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java); [BillingServiceRulesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceRulesTest.java); [SessionWorkflowE2ETest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/e2e/SessionWorkflowE2ETest.java) |
| 2 | Connected client → session → note → invoice → payment → portal | Admin creates/assigns client and session; therapist completes eligible session and finalizes note; one bill appears; partial/full payment reconciles; portal sees only its permitted records; verify reload and independent API/DB reads. | [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [BillingSessionTriggerIntegrationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/integration/billing/BillingSessionTriggerIntegrationTest.java) |
| 3 | Authentication and access lifecycle | Browser MFA/recovery and logout/reset/revocation; old tokens fail; same-tenant wrong-client and other-tenant direct IDs fail on clinical/document/billing mutations and downloads. | [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [AuthorizationSecurityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/AuthorizationSecurityTest.java); [MfaServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/auth/service/MfaServiceTest.java) |
| 4 | Financial correctness and replay | Competing policies, split payments, concurrent duplicate outcome/payment/discount, discount removal, real signed webhook replay and exact ledger balances. Inspect receipt/PDF content, not just PDF header. | [InvoicePolicyServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/InvoicePolicyServiceTest.java); [BillingSessionTriggerIntegrationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/integration/billing/BillingSessionTriggerIntegrationTest.java); [BillingServiceRulesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceRulesTest.java); [StripeWebhookApplicationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/StripeWebhookApplicationServiceTest.java) |
| 5 | Clinical/document integrity | Required-answer rejection; signature/review/version retention; assessments produce expected scores; finalized notes resist edits; wrong-client report sources and revoked document URLs fail. | [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [documents.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/documents.spec.mjs); [FormServiceVersioningPatchTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/FormServiceVersioningPatchTest.java); [AssessmentServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/AssessmentServiceTest.java); [ClientReportServiceAuditTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientReportServiceAuditTest.java) |
| 6 | Task/checklist/event chains | Non-author comment mutation and wrong task/comment pair rejected; overdue and template-change behavior; real source event creates exactly intended durable notification, with preference/retry suppression. | [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [TaskServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/TaskServiceTest.java); [ChecklistServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ChecklistServiceTest.java); [ClientCreatedEffectsIntegrationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/integration/client/ClientCreatedEffectsIntegrationTest.java); [NotificationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/NotificationServiceTest.java) |
| 7 | Audio/Zoom failure recovery | Deterministic chunk/websocket consent, ordering, retry and loss recovery; recording controls save complete output; Zoom request and persisted association match. Actual two-person capture/accuracy stays a separate fixture/manual track. | [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java); [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java) |
| 8 | Remaining modules and filters | Follow every family decision below; compare exact expected IDs/counts across all filters/sorts/exports, settings effects, client/profile/bulk and platform/public workflows. | [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs); [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs) |
| 9 | Operational verification | Real forward migrations on fresh/upgrade QA schemas; isolated retention/purge, recovery drill and deployment-sized load. Decide/implement backup capability before claiming a successful backup workflow. | [MigrationGuardrailTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/migration/MigrationGuardrailTest.java); [SuperAdminOperationsBackupTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminOperationsBackupTest.java); [LoadTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/performance/LoadTest.java) |
| 10 | Consolidated run and CI | After Stage 6 settles, run full frontend/backend/browser lifecycle and coverage report on stable source inputs; publish exact failures/skips and both revisions. Configure dedicated CI runner and both repository checkouts. | [test-app.sh](/Users/apple/IdeaProjects/smarthub/scripts/test-app.sh); [QA guide](/Users/apple/IdeaProjects/smarthub/qa/README.md) |

For browser audio/Zoom or websocket/retention/real-Flyway work, add focused new suites alongside the listed existing files; the adjacent service test is an extension point, not evidence that the missing layer already exists. This audit does not change those runners or introduce tests.

## All 73 family decisions

“No direct scenario found” means no direct assertion establishing that scenario was identified in this audit; it is not proof that the feature is broken. “Unit/slice” means collaborators or HTTP security filters may be mocked/disabled as declared by the fixture. API entries from Playwright use real HTTP; Java MockMvc entries exercise application/API logic in the test process. Every family remains **partial-or-unverified**. Source links are current extension points; the JSON lists historical class results separately and never treats them as whole-family evidence.

### ROLE

#### ROLE-01 — P0

Resolve host/tenant and identity, activate accounts, login, MFA, password recovery, refresh, logout, idle timeout, known devices and session revocation.

**Evidence level:** UI/API/unit subset.

**Assertions present:** Admin and portal password login/reload, wrong-password rejection; backend refresh, MFA recovery-code reuse/lockout and session expiry rules.

**Still needed:** Add real browser MFA enrollment/challenge/recovery, activation/reset, logout/revocation, idle timeout and known-device lifecycle, including old-token denial.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [AuthenticationControllerApiTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/AuthenticationControllerApiTest.java); [MfaServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/auth/service/MfaServiceTest.java); [AuthSessionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/AuthSessionServiceTest.java); [AuthKnownDeviceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/auth/service/AuthKnownDeviceServiceTest.java).

#### ROLE-02 — P0

Verify navigation, nested tabs, direct deep links, API permission enforcement, read-only permissions, caseload restrictions and role changes during a session.

**Evidence level:** UI/API/unit subset.

**Assertions present:** Anonymous redirect; restricted task denial; tenant client lists; backend therapist/client scope and wrong-tenant token denial.

**Still needed:** Exercise direct object reads/writes/downloads for every sensitive module, read-only roles, permission changes mid-session, nested navigation and option-list leaks.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [AuthorizationSecurityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/AuthorizationSecurityTest.java); [RolePermissionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/RolePermissionServiceTest.java).

#### ROLE-03 — P0

Test plan/feature enablement and usage limits independently from user permissions; hide/disable the UI appropriately and enforce the same rules in the backend.

**Evidence level:** Unit/slice subset.

**Assertions present:** Disabled-limit versus numeric-cap resolution; client/user limit checks; selected controller permission contracts.

**Still needed:** Test feature enabled/disabled separately from permitted/denied roles in real HTTP and UI; verify rejected writes leave data unchanged.

**Existing files to extend:** [SubscriptionFeatureServiceLimitEnablementTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SubscriptionFeatureServiceLimitEnablementTest.java); [ClientServiceSubscriptionLimitTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientServiceSubscriptionLimitTest.java); [UserServiceSubscriptionLimitTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/UserServiceSubscriptionLimitTest.java); [BillingCustomRoleSecurityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/BillingCustomRoleSecurityTest.java).

#### ROLE-04 — P1

Verify therapist dashboard, upcoming/previous/overdue sessions, assigned clients, billing visibility, tasks, profile, availability and security.

**Evidence level:** UI subset.

**Assertions present:** Therapist client list includes assigned client and excludes unassigned/other-tenant clients.

**Still needed:** Add dashboard counts, upcoming/previous/overdue sessions, tasks, billing, profile and availability journeys with persisted outcomes.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [SessionHistoryScopeServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/session/SessionHistoryScopeServiceTest.java); [SessionOverviewTimezoneCountTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/session/SessionOverviewTimezoneCountTest.java).

#### ROLE-05 — P0

Verify staff route guards, index redirect, no-access page, custom roles, and permitted client/content/scheduling actions.

**Evidence level:** API/unit subset.

**Assertions present:** Restricted staff cannot list tasks; backend authorization and staff expression tests cover selected rules.

**Still needed:** Add staff login/index/no-access/deep-link browser cases and permitted creation/editing in each granted module; test custom-role changes.

**Existing files to extend:** [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [AuthorizationSecurityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/AuthorizationSecurityTest.java); [StaffAuthorizationExpressionsTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/security/StaffAuthorizationExpressionsTest.java).

#### ROLE-06 — P0

Verify client activation, profile/avatar, timezone, appointment browsing/booking/detail/cancel/reschedule, session history/rating, invoices/payments/receipts, documents, forms, assessments, consents and notifications. Assess API-only versus UI-supported operations explicitly.

**Evidence level:** UI/API/unit subset.

**Assertions present:** Portal login and previous appointments survive reload; form answer and document sharing APIs; portal invoice ownership unit denial.

**Still needed:** Complete portal activation, booking/reschedule/cancel, invoice/payment/receipt, consent, assessment/form/signature and document UI journeys with cross-client denials.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [documents.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/documents.spec.mjs); [ClientPortalServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientPortalServiceTest.java); [PortalInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/PortalInvoiceServiceTest.java); [PortalInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/client/portal/PortalInvoiceServiceTest.java).

#### ROLE-07 — P0

Verify organization administration and platform administration independently, including impersonation entry/exit, audit attribution, time limits and isolation.

**Evidence level:** Unit/slice subset.

**Assertions present:** Super-admin controller ACL/validation; ending impersonation revokes issued auth sessions.

**Still needed:** Test full impersonation entry/exit/expiry and audit attribution through real authentication, tenant switching and admin/platform UI separation.

**Existing files to extend:** [SuperAdminImpersonationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminImpersonationServiceTest.java); [SuperAdminMainControllerSecurityValidationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/SuperAdminMainControllerSecurityValidationTest.java); [SuperAdminExtendedControllerSecurityValidationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/SuperAdminExtendedControllerSecurityValidationTest.java).

### ZOOM

#### ZOOM-01 — P1

Own therapist credential configuration, status, test and removal; admin visibility/test permissions; missing/incomplete/invalid credentials; secrets absent from responses and logs.

**Evidence level:** API/unit subset.

**Assertions present:** Fresh therapist status reports unconfigured without secret/token fields; null/missing credentials rejected in services.

**Still needed:** Add configure/read/test/rotate/remove workflow, invalid credentials and admin/other-therapist denial; inspect all response/log secret fields.

**Existing files to extend:** [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [ZoomServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomServiceTest.java); [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java).

#### ZOOM-02 — P1

Meeting creation from staff scheduling and separately from client portal booking; correct therapist account, date, timezone, duration, service and session association.

**Evidence level:** Unit subset.

**Assertions present:** Mocked provider response maps meeting ID, join URL and password.

**Still needed:** Add staff and portal booking against controlled provider HTTP; assert therapist account, request date/duration/timezone and persisted session integration.

**Existing files to extend:** [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java); [SessionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionServiceTest.java).

#### ZOOM-03 — P1

Expired access token refresh, provider denial/timeouts/rate limiting, and failure during session creation. Define and verify whether the local session persists or rolls back.

**Evidence level:** Indirect only.

**Assertions present:** Missing-credential validation exists; no direct refresh/rate-limit/timeout recovery workflow identified.

**Still needed:** Add token expiry/refresh, denial, timeout and 429 tests with explicit local transaction outcome and retry policy.

**Existing files to extend:** [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java); [ZoomServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomServiceTest.java).

#### ZOOM-04 — P0

Repeated submit/retry does not unintentionally create extra meetings or sessions; verify persisted meeting ID, join URL and password.

**Evidence level:** No direct scenario found.

**Assertions present:** Meeting response mapping exists; it does not assert scheduling idempotency.

**Still needed:** Submit booking twice and retry after ambiguous provider response; assert one intended session/meeting association and stable identifiers.

**Existing files to extend:** [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java); [SessionControllerApiTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionControllerApiTest.java).

#### ZOOM-05 — P1

Client and therapist see their permitted meeting information. Start/join labels and actions match actual provider URLs; a join link alone is not proof of host capabilities.

**Evidence level:** Indirect helper only.

**Assertions present:** Join-eligibility helper accepts configured online meeting; no two-role meeting-detail UI journey.

**Still needed:** Test host/start versus participant/join links and permitted meeting details from therapist/client screens; reject unrelated users.

**Existing files to extend:** [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs); [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java).

#### ZOOM-06 — P1

Missing URL, Zoom disabled, in-person session, cancelled/no-show and other supported statuses produce the intended join visibility and direct endpoint behavior.

**Evidence level:** Helper subset.

**Assertions present:** Join helper covers cancelled/no-show spellings, in-person, blank URL and virtual mode.

**Still needed:** Add rendered button/deep-link/endpoint cases for Zoom disabled, supported status set and unauthorized users.

**Existing files to extend:** [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs).

#### ZOOM-07 — P1

Reschedule, recurring edits, therapist reassignment, cancellation and Zoom disablement reconcile local and provider state according to documented behavior. Inspect and flag missing synchronization rather than assuming it exists.

**Evidence level:** No direct scenario found.

**Assertions present:** Local recurrence/reschedule tests do not establish provider synchronization.

**Still needed:** Trace supported synchronization and test reschedule/reassign/cancel/series edits against provider state; document unsupported operations explicitly.

**Existing files to extend:** [RecurringSessionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/RecurringSessionServiceTest.java); [SessionWorkflowE2ETest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/e2e/SessionWorkflowE2ETest.java); [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java).

#### ZOOM-08 — P1

Integration health, therapist availability, service eligibility and provider-test options return correctly scoped results and do not expose credentials.

**Evidence level:** Unit/API subset.

**Assertions present:** Therapist configured checks and fresh-credential status endpoint.

**Still needed:** Verify integration health/test and eligible therapist/service lists with exact scope, denied access and secret-free outputs.

**Existing files to extend:** [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java); [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs).

#### ZOOM-09 — P1

Isolated two-account provider smoke: intended therapist and client can enter the same meeting, while unauthorized users cannot retrieve its protected application details. Use provider sandbox/test accounts.

**Evidence level:** External verification missing.

**Assertions present:** Provider calls are mocked; no real two-account meeting evidence.

**Still needed:** Run isolated provider-account join smoke and protected-detail denial; retain outcome evidence without credentials.

**Existing files to extend:** [ZoomApiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ZoomApiServiceTest.java).

#### ZOOM-10 — P1

Independently verify whether both speakers in a remote meeting are captured by the application's recording setup. Test headphones versus speakers and echo cancellation; record unsupported capture configurations as gaps.

**Evidence level:** Physical verification missing.

**Assertions present:** No two-person physical audio capture evidence identified.

**Still needed:** Run known-dialogue headphones/speakers and echo-cancellation matrix; record whether each remote/local speaker is actually captured.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java).

### AUDIO

#### AUDIO-01 — P1

Microphone enumeration/selection, permission grant/deny, missing or disconnected device, unsupported browser/codec, empty input and microphone-level display.

**Evidence level:** No direct scenario found.

**Assertions present:** Backend MIME/error checks do not exercise microphone devices or permission dialogs.

**Still needed:** Add browser permission grant/deny, device change/disconnect, absent microphone and unsupported codec cases using synthetic media.

**Existing files to extend:** [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java); [TranscriptionErrorMapperTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/ai/TranscriptionErrorMapperTest.java).

#### AUDIO-02 — P0

Start, pause, resume, stop/save, close/dismiss and repeated clicks; duration accounting; microphone/media/socket cleanup after success, failure and navigation.

**Evidence level:** No direct scenario found.

**Assertions present:** Service start/finalize/delete tests do not exercise recorder controls or resource cleanup.

**Still needed:** Test start/pause/resume/stop/save/repeated-click/navigation flows and assert duration, saved content and media/socket cleanup.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java).

#### AUDIO-03 — P1

Use synthetic two-speaker fixtures with known dialogue, silences, interruptions and language changes. Verify captured content, order, timestamps and completeness rather than only HTTP success.

**Evidence level:** Unit subset.

**Assertions present:** Stubbed chunk text and silence markers asserted; no end-to-end known two-speaker recording.

**Still needed:** Feed deterministic two-speaker fixture and assert complete ordered words/timestamps through capture, upload and saved output.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java).

#### AUDIO-04 — P1

Language selection and translation-to-English options flow through the UI/request/provider/result correctly.

**Evidence level:** Unit/contract subset.

**Assertions present:** Start saves language; unsupported start/chunk language is rejected; HTTP unsupported-language mapping.

**Still needed:** Test selected language and translation option through browser/request/provider call/result for supported combinations.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java).

#### AUDIO-05 — P0

Consent and permissions at upload start, chunk processing, finalization, retrieval, diarization and smart fill; check consent withdrawal between steps and cross-session/upload-ID access.

**Evidence level:** Unit subset.

**Assertions present:** Consent blocks upload/diarization; chunk wrong-session, inaccessible-session and non-owner denials prevent provider calls.

**Still needed:** Add real HTTP consent withdrawal between upload/chunk/finalize/read/smart-fill steps; cross-client/tenant/upload-ID access matrix and no-mutation assertions.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java); [ConsentCommandServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ConsentCommandServiceTest.java).

#### AUDIO-06 — P0

Websocket tickets bound to upload IDs, expiration, missing/wrong ticket, disallowed origins, organization context, and cleanup of context after callbacks.

**Evidence level:** Unit/contract subset.

**Assertions present:** JWT upload-bound ticket accepts correct upload, rejects another upload/access token; HTTP returns mocked ticket and TTL.

**Still needed:** Exercise real websocket handshake/callbacks with expired/missing/wrong tickets, disallowed origins and tenant-context cleanup.

**Existing files to extend:** [JwtTokenProviderTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/security/JwtTokenProviderTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java).

#### AUDIO-07 — P0

Live connection establishment, queued frames, partial/final messages, network loss, upstream disconnect, timeout, retry/reconnect behavior and useful UI errors without losing saved-upload state.

**Evidence level:** No direct scenario found.

**Assertions present:** Chunk HTTP error mapping is not live websocket reconnect evidence.

**Still needed:** Add websocket frame queue, partial/final results, disconnect/timeout/reconnect and visible recovery without losing durable upload state.

**Existing files to extend:** [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java); [TranscriptionErrorMapperTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/ai/TranscriptionErrorMapperTest.java).

#### AUDIO-08 — P0

Chunk index/size/duration validation, silent chunks, duplicate and out-of-order chunks, provider errors, rate limiting, local blob recovery and retry limits.

**Evidence level:** Unit/contract subset.

**Assertions present:** Successful/silent chunks, invalid upload/language/status, throttling, provider errors and non-audio HTTP rejection.

**Still needed:** Add duplicate/out-of-order chunk accounting, size/index/duration bounds, retry exhaustion and client blob recovery against real storage/database.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java); [TranscriptChunkRateLimiterTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/TranscriptChunkRateLimiterTest.java).

#### AUDIO-09 — P0

Finalization with complete, missing, failed and silent chunks; expected-count limits; ordered stitching and visible gap markers; repeated finalize; prevent saving an apparently complete transcript when accounting is incomplete.

**Evidence level:** Unit/contract subset.

**Assertions present:** Finalize produces ready text; silence marker; existing-ready idempotency; missing chunks conflict; expired upload denial.

**Still needed:** Add persisted multi-chunk order, failed/gapped chunk accounting, expected-count boundaries and concurrent/repeated finalization; verify visible gap state.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java).

#### AUDIO-10 — P0

Saved transcript status/detail/list, download, copy, reopen and delete; client/session scoping and cross-tab consistency.

**Evidence level:** Unit/contract subset.

**Assertions present:** Recording/ready detail shapes, exact download text and audit calls; soft-delete status/chunk save; HTTP get/delete contract.

**Still needed:** Add transcript list/reopen/copy/download/delete browser journey with cross-session/tenant denial and persisted cross-tab state.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java); [SessionTranscriptServiceAuditTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceAuditTest.java).

#### AUDIO-11 — P1

Speaker diarization only on eligible transcripts; preserve original text; cache/idempotency; invalid/empty/refusal output; do not silently treat uncertain patient/clinician attribution as reliable.

**Evidence level:** Unit subset.

**Assertions present:** Original transcript preserved; cached diarization avoids AI; refusal cache retry; empty/not-ready/consent denials.

**Still needed:** Add persisted/browser eligibility and uncertain-speaker handling; independently evaluate speaker attribution accuracy with labelled fixtures.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [AiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/AiServiceTest.java).

#### AUDIO-12 — P0

Transcript smart fill and review map to the intended note fields, preserve clinician edits, handle missing fields and require the supported review/save step.

**Evidence level:** Unit subset.

**Assertions present:** Smart-fill maps named fields, returns empty-result message and rejects not-ready/missing text.

**Still needed:** Test UI review/save, partial/missing fields, retry and preservation of existing clinician edits; verify intended note and reload.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java).

#### AUDIO-13 — P1

Session-note voice upload/reprocess and assessment audio transcription have independent regression cases.

**Evidence level:** No direct workflow found.

**Assertions present:** Generic transcript and note DTO/service tests are not independent note/assessment audio workflows.

**Still needed:** Add separate note voice upload/reprocess and assessment audio cases for success, denial, failed provider and saved output.

**Existing files to extend:** [SessionNoteServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionNoteServiceTest.java); [SessionTranscriptControllerContractTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/SessionTranscriptControllerContractTest.java).

#### AUDIO-14 — P0

Retention expiry, transcript/chunk soft deletion, audio cleanup, failed-upload remnants and absence of transcript content in operational logs.

**Evidence level:** Unit subset.

**Assertions present:** Expired upload rejection, transcript soft-delete and chunk-save calls; general logging masking regressions.

**Still needed:** Run retention scheduler at expiry boundaries with real stored blobs/chunks; assert cleanup/retry and no clinical text in error logs.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [SessionTranscriptServiceAuditTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceAuditTest.java); [SensitiveDataMaskerTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/common/SensitiveDataMaskerTest.java).

#### AUDIO-15 — P1

Run deterministic provider-stub regression checks automatically; evaluate actual recognition/speaker accuracy with known fixtures separately. Clinical correctness and physical audio capture need explicit quality evidence beyond a green API test.

**Evidence level:** Unit automation; quality missing.

**Assertions present:** Deterministic provider doubles cover selected transcription/diarization rules.

**Still needed:** Add reproducible known-audio recognition and speaker-quality evaluation; separate clinical review and physical capture from automated API status.

**Existing files to extend:** [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java); [AiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/AiServiceTest.java).

### MONEY

#### MONEY-01 — P1

Service code/rate CRUD, activation, therapist/portal visibility, defaults, duration and service-specific scheduling/billing effects.

**Evidence level:** Unit subset.

**Assertions present:** Service create/duplicate code/read/active-list and base billing behavior.

**Still needed:** Add update/deactivate/delete, defaults/duration and therapist/portal visibility; verify rate/service edits affect scheduling and future bills correctly.

**Existing files to extend:** [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java); [SessionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionServiceTest.java).

#### MONEY-02 — P0

Policy CRUD, activate/deactivate/delete, required scopes, duplicate scope, fixed/percentage pricing, invalid/negative/>100 percentage and invalid effective date ranges.

**Evidence level:** Unit subset.

**Assertions present:** Policy create, duplicate scope and wildcard percentage-policy creation; request patch/deserialization.

**Still needed:** Add update/disable/delete, required scope and negative/over-100 percentage/date-range rejection through API with persisted no-mutation checks.

**Existing files to extend:** [InvoicePolicyServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/InvoicePolicyServiceTest.java); [InvoicePolicyRequestDeserializationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/billing/InvoicePolicyRequestDeserializationTest.java).

#### MONEY-03 — P0

Policy matching for client type, appointment status, service, wildcard `all`, start/end dates and practice timezone. Verify precedence: service specificity, client type specificity, status specificity, priority, then updated time as currently implemented; test ambiguous ties explicitly.

**Evidence level:** Unit subset.

**Assertions present:** Fixed rate/fallback, normalized status/client keys, wildcard lookup and practice-local policy date; BillingServiceTest also verifies the practice-local date passed to policy resolution.

**Still needed:** Test competing persisted policies for every precedence tier/tie, inclusive effective boundaries and midnight/DST billing-to-UI agreement.

**Existing files to extend:** [InvoicePolicyServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/InvoicePolicyServiceTest.java); [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java).

#### MONEY-04 — P0

Completed/no-show/manual billing eligibility, future and cancelled sessions, duplicate billing, transaction rollback and response mapping after commit.

**Evidence level:** Database/API/unit subset.

**Assertions present:** Completed/no-show bill creation, repeat status no duplicate, future/cancelled rejection, rollback, billed/no-cancel, manual once and readable HTTP completion.

**Still needed:** Add exact eligibility instant, concurrent completion/manual billing and full multi-role UI journey through note/invoice/payment; verify all persisted balances.

**Existing files to extend:** [BillingSessionTriggerIntegrationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/integration/billing/BillingSessionTriggerIntegrationTest.java); [SessionStatusBillingTriggerTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/session/SessionStatusBillingTriggerTest.java); [BillingGuardSessionEligibilityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/billing/BillingGuardSessionEligibilityTest.java); [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs).

#### MONEY-05 — P0

Discount none/fixed/percentage, aliases and invalid type; positive values, configured maximum percentage, rounding, fixed amount exceeding outstanding and a fully paid invoice.

**Evidence level:** Unit/API subset.

**Assertions present:** Fixed/full/percentage discounts and over-maximum percentage rejection; partial-payment discount API.

**Still needed:** Add none/alias/invalid type, zero/negative/rounding limits, fixed amount over outstanding and fully-paid invoice denials.

**Existing files to extend:** [BillingServiceRulesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceRulesTest.java); [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java); [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs).

#### MONEY-06 — P0

Partial-payment example: $100 total, $40 paid, then 10% discount must follow the reviewed rule. Current implementation computes $6 discount against $60 outstanding, leaving $54 outstanding. Verify API, UI, persisted values, PDF and receipt agree.

**Evidence level:** API/unit subset.

**Assertions present:** HTTP asserts 100 total, 40 paid, 60 remaining, 10% gives 6 discount and 54 remaining; second discount rejected.

**Still needed:** Read invoice again in a separate request/context and compare persisted totals with billing UI, portal, PDF and receipt.

**Existing files to extend:** [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [BillingServiceRulesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceRulesTest.java); [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java).

#### MONEY-07 — P0

Existing nonzero discount cannot be replaced without removal under current service rule; verify remove/reapply, duplicate clicks and concurrent requests.

**Evidence level:** Unit/API subset.

**Assertions present:** Second nonzero discount is rejected.

**Still needed:** Add remove/reapply, duplicate click and simultaneous apply/remove tests with durable totals/history and no duplicate side effects.

**Existing files to extend:** [BillingServiceRulesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceRulesTest.java); [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs).

#### MONEY-08 — P0

`ADVANCED_BILLING` feature and `BILLING_MANAGE` permission gates; denied attempts must not change balance, status or audit history as a successful mutation.

**Evidence level:** Unit/slice subset.

**Assertions present:** Custom BILLING_VIEW statistics permission slice; tenant/payment guards. This is not ADVANCED_BILLING plus BILLING_MANAGE mutation coverage.

**Still needed:** Run feature on/off crossed with manage/read-only/no-permission users on discount/payment APIs; assert no balance/status/success-audit mutation on denial.

**Existing files to extend:** [BillingCustomRoleSecurityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/BillingCustomRoleSecurityTest.java); [BillingGuardTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingGuardTest.java); [BillingControllerApiTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/BillingControllerApiTest.java).

#### MONEY-09 — P0

Discount-induced status changes, zero outstanding, discount removal after payment and reconciliation of amount due, paid and outstanding; never manufacture a payment from a discount.

**Evidence level:** Unit subset.

**Assertions present:** Full discount marks paid with zero remaining; discount recalculation and payment void recompute totals.

**Still needed:** Test discount removal after partial/full payment and ledger consistency in database; assert discounts create no payment transactions.

**Existing files to extend:** [BillingServiceRulesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceRulesTest.java); [BillingServicePaymentTransactionTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServicePaymentTransactionTest.java).

#### MONEY-10 — P0

Insurance/copay snapshots, client/insurance split payment, cumulative versus delta amounts, repeated payment requests, stale/concurrent edits and ledger consistency.

**Evidence level:** Database/unit subset.

**Assertions present:** Partial/full cumulative payment workflow avoids double-counting; stale expected prior source and ownership rejected; payment void recalculates.

**Still needed:** Add real client/insurance split-payment path, insurance snapshot validation, concurrent/stale/replayed requests and exact per-source ledger reconciliation.

**Existing files to extend:** [BillingSessionTriggerIntegrationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/integration/billing/BillingSessionTriggerIntegrationTest.java); [BillingServiceRulesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceRulesTest.java); [BillingServicePaymentTransactionTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServicePaymentTransactionTest.java).

#### MONEY-11 — P0

Stripe checkout/payment outcome, abandoned payment, signed webhook validation, duplicate/out-of-order webhook events, refunds and retry paths where supported.

**Evidence level:** Unit subset; three empty tests.

**Assertions present:** Signature missing/invalid rejection, checkout account routing, redirect-not-paid, webhook application and duplicate-event/payment-intent checks; three obsolete Stripe test bodies do nothing.

**Still needed:** Replace empty tests with current Payment/PaymentTransaction assertions. Add signed HTTP webhook replay/out-of-order/concurrency, abandoned checkout and supported refund/retry cases; separate sandbox smoke.

**Existing files to extend:** [StripeWebhookApplicationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/StripeWebhookApplicationServiceTest.java); [StripeServiceWebhookTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/StripeServiceWebhookTest.java); [StripeWebhookEventServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/StripeWebhookEventServiceTest.java); [StripeServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/StripeServiceTest.java); [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java).

#### MONEY-12 — P0

Billing history, client portal invoices, payment status filters, totals/statistics, PDF/HTML receipts and exports all agree with persisted records and selected scope.

**Evidence level:** Unit/API subset.

**Assertions present:** Bill creation/read/list; portal ownership/totals; invoice preview and PDF header/size; billing sort tie-break. BillingExportServiceTest covers organization subscription export tokens, not patient export content.

**Still needed:** Compare exact list/statistics/export/receipt/PDF contents against independent persisted balances and filters, after payment/discount/void and cross-client denial.

**Existing files to extend:** [BillingWorkflowE2ETest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/e2e/BillingWorkflowE2ETest.java); [PortalInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/PortalInvoiceServiceTest.java); [PortalInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/client/portal/PortalInvoiceServiceTest.java); [BillingExportServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingExportServiceTest.java); [BillingListSortTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/billing/BillingListSortTest.java); [BillingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingServiceTest.java).

#### MONEY-13 — P0

Organization subscription invoices are separate from patient billing: plan/add-ons, limits, subscription checkout, renewal/failure, dunning, cancellation, refund/dispute and grace/access transitions.

**Evidence level:** Unit/slice subset.

**Assertions present:** Subscription period/plan limits, grace/dunning locks, trial dedup/expiry, invoice upsert and subscription webhook dispatch.

**Still needed:** Test complete organization subscription checkout/renew/fail/suspend/cancel/reactivate and entitlement transitions with database state; refund/dispute where supported.

**Existing files to extend:** [SubscriptionLifecycleServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SubscriptionLifecycleServiceTest.java); [SuperAdminSubscriptionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminSubscriptionServiceTest.java); [StripeSubscriptionWebhookServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/StripeSubscriptionWebhookServiceTest.java); [PlatformSubscriptionInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/PlatformSubscriptionInvoiceServiceTest.java).

### FILTER

#### FILTER-01 — P1

Default state and omitted value; each supported enum/option; populated matching and nonmatching records; invalid/stale option IDs; literal `false`/zero versus absent values.

**Evidence level:** API/helper subset.

**Assertions present:** Client portal true/false filters select expected fixture; invoice helper distinguishes false from omitted coverage.

**Still needed:** Map defaults/omitted/false/zero and invalid or stale options on every supported filter endpoint/UI.

**Existing files to extend:** [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs).

#### FILTER-02 — P1

Actual result IDs/counts match the independent expected set. Checking that a dropdown changes or a query parameter is sent is insufficient.

**Evidence level:** API/helper subset.

**Assertions present:** Positive/negative fixture membership for 5 task, 4 client and 5 document filter cases; 5 appointment helper filters.

**Still needed:** Add remaining filter surfaces and exact independent full result IDs/counts; current contains/not-contains tests do not assert absence of every unexpected row.

**Existing files to extend:** [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [documents.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/documents.spec.mjs); [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs).

#### FILTER-03 — P1

Apply, clear-one chip, clear-all, cancel/unapplied edits, close/reopen, reload and back navigation according to the intended persistence contract.

**Evidence level:** UI/helper subset.

**Assertions present:** Client search clear restores a row; appointment helper defaults reset local results.

**Still needed:** Test apply/cancel, individual chips/clear-all, dialog reopen/reload/back and specified persistence across role-specific screens.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs).

#### FILTER-04 — P0

Date/numeric lower bound, upper bound, equal bounds, inverted bounds, open-ended ranges, timezone/DST boundaries and invalid values.

**Evidence level:** Helper/unit subset.

**Assertions present:** Invoice helper retains start/end/status query values; calendar/date/payment validation units exist.

**Still needed:** Assert actual returned records at inclusive/exclusive/open/equal/inverted bounds, DST/midnight and invalid values; query serialization alone is insufficient.

**Existing files to extend:** [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs); [CalendarDateBoundsTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/session/CalendarDateBoundsTest.java); [PaymentDateValidatorTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/billing/PaymentDateValidatorTest.java).

#### FILTER-05 — P1

Search whitespace/case, supported exact versus partial fields, special characters, empty query, no matches, debounced requests and stale-response races.

**Evidence level:** UI/unit subset.

**Assertions present:** Client search match/no-match/clear; name/MRN classification and normalization units.

**Still needed:** Add all supported fields, whitespace/case/special characters and delayed out-of-order search responses; assert final displayed and returned IDs.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [ClientSearchHelperTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/client/ClientSearchHelperTest.java).

#### FILTER-06 — P1

Every supported sort key and direction, equal-value tie ordering, nulls, stable pagination, page reset after filter change and no duplicate/missing infinite-scroll records.

**Evidence level:** API/unit subset; Stage 6 WIP.

**Assertions present:** Two one-row client pages have different IDs; billing sort ID tie-break; Stage 6 has component pagination/ref regressions in progress.

**Still needed:** Verify complete datasets for every sort/direction/null/tie, page reset and real backend infinite scroll; incorporate Stage 6 only after its final passing report.

**Existing files to extend:** [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [BillingListSortTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/billing/BillingListSortTest.java); [frontend-lifecycle.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-lifecycle.spec.mjs).

#### FILTER-07 — P1

Each filter individually, pairwise combinations, all compatible filters together, contradictory combinations, and explicit high-risk multi-filter combinations. Exhaustive Cartesian products are not implied.

**Evidence level:** Helper/unit subset.

**Assertions present:** Appointment helper compatible/contradictory/reset sets; form combined search/category normalization.

**Still needed:** Add pairwise/high-risk combinations through actual APIs/UI for other filter surfaces; do not assume helper results validate server queries.

**Existing files to extend:** [frontend-policies.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/frontend-policies.spec.mjs); [FormServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/FormServiceTest.java).

#### FILTER-08 — P0

Role/tenant/caseload scoping still holds under filtering and direct API calls. Option lists must not leak inaccessible users/clients.

**Evidence level:** API/UI/unit subset.

**Assertions present:** Tenant client lists, therapist assigned-client list and selected direct authorization cases.

**Still needed:** Combine each sensitive filter/search/export with tenant/role/caseload denial; test dropdown options contain only authorized identities.

**Existing files to extend:** [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [AuthorizationSecurityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/AuthorizationSecurityTest.java).

#### FILTER-09 — P0

Exported rows, dashboard counts and result lists use the documented scope. If a summary intentionally has a different scope, assert and label that behavior.

**Evidence level:** Unit subset.

**Assertions present:** Portal totals aggregation and dashboard timezone/totals units; organization subscription export token validation. No patient export-row reconciliation established.

**Still needed:** Verify exact export rows and dashboard/list totals from the same independently calculated fixture scope; document intentional scope differences.

**Existing files to extend:** [PortalInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/PortalInvoiceServiceTest.java); [PortalInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/client/portal/PortalInvoiceServiceTest.java); [BillingExportServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BillingExportServiceTest.java); [AdminDashboardServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/AdminDashboardServiceTest.java).

### CLIENT

#### CLIENT-01 — P0

Complete intake sections: personal, clinical, address, referral, employment, contacts, insurance and consent; field validation, edit/patch semantics and history.

**Evidence level:** Database/API/unit subset.

**Assertions present:** Client create/read/update/soft-delete workflows; selected contact/address/insurance patch preservation and validation.

**Still needed:** Build full intake across all sections, null versus omitted fields, invalid nested data and exact history/persistence from UI and API.

**Existing files to extend:** [ClientWorkflowE2ETest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/e2e/ClientWorkflowE2ETest.java); [ClientControllerApiTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/ClientControllerApiTest.java); [ClientServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientServiceTest.java); [ClientAddressServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientAddressServiceTest.java); [ClientContactServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientContactServiceTest.java); [ClientInsuranceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientInsuranceServiceTest.java).

#### CLIENT-02 — P0

Portal enable/disable/activation resend, client close/reopen/delete/restore, related-record effects, purge eligibility and consent withdrawal.

**Evidence level:** Database/unit subset.

**Assertions present:** Soft delete; close/reopen role rules; portal disabled/pending/reset revocation; restore-at-limit denial; consent regrant.

**Still needed:** Test full activate/disable/resend/close/reopen/delete/restore lifecycle and dependent records/access; isolated purge and mid-flow consent withdrawal.

**Existing files to extend:** [ClientFileCloseServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientFileCloseServiceTest.java); [ClientPortalServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientPortalServiceTest.java); [ClientDeleteCascadeSessionsTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientDeleteCascadeSessionsTest.java); [ClientServiceSubscriptionLimitTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientServiceSubscriptionLimitTest.java); [ConsentCommandServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ConsentCommandServiceTest.java).

#### CLIENT-03 — P0

Bulk upload, stage/status changes, reassignment, portal access bulk changes, export, duplicate detect/mark/unmark; partial failures and rerun semantics.

**Evidence level:** Unit subset.

**Assertions present:** Duplicate detect/mark role/self-denial; client service/limit rules.

**Still needed:** Add bulk upload/update/reassign/portal/export workflows, mixed valid-invalid rows and idempotent rerun; unmark and duplicate history.

**Existing files to extend:** [DuplicateDetectionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/DuplicateDetectionServiceTest.java); [ClientControllerApiTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/api/ClientControllerApiTest.java); [ClientServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientServiceTest.java).

#### CLIENT-04 — P1

Overview, sessions, forms/documents, assessments, reports, tasks, checklists, billing, notes/history, stage duration, email history and SMS log/export; independent role-specific implementations and persistence.

**Evidence level:** UI/API/unit subset.

**Assertions present:** Selected tasks/comments, forms, checklist, notes, documents and portal history; email-history unit behavior.

**Still needed:** Verify each client tab and role implementation, cross-tab counts and independent expansion/tab persistence after switching clients and reload.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [documents.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/documents.spec.mjs); [ClientEmailHistoryTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/client/ClientEmailHistoryTest.java).

### PEOPLE

#### PEOPLE-01 — P0

User/profile lifecycle, credentials, password change, avatars, timezone, working hours, education/other profile sections, supervisor assignments and schedule eligibility after profile edits.

**Evidence level:** Unit/API subset.

**Assertions present:** User create/update/identity uniqueness and working-hour overlap; blocked-time rules and edit-session conflict exclusion.

**Still needed:** Add full profile/password/avatar/timezone/supervision lifecycle and real scheduling after availability edits; permitted/denied roles and reload.

**Existing files to extend:** [UserServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/UserServiceTest.java); [TherapistAvailabilityServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/TherapistAvailabilityServiceTest.java); [AuthorizationSecurityTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/security/AuthorizationSecurityTest.java).

### FORMS

#### FORMS-01 — P0

Template sections/fields, field ordering, versions, activate/archive, single/bulk/multi-template assignments, responses, signatures and review from staff and client sides.

**Evidence level:** API/unit subset.

**Assertions present:** Template/assignment/answer round trip; metadata versus structural version patch; signature requirement service branches.

**Still needed:** Add required-field rejection, field ordering/types, bulk assignments, real signature/review and old assignment preservation across template versions.

**Existing files to extend:** [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [FormServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/FormServiceTest.java); [FormServiceVersioningPatchTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/FormServiceVersioningPatchTest.java).

### ASSESS

#### ASSESS-01 — P0

Template sections/questions/options and bulk edits, versions, assignment, single/batch responses, scoring/recalculation, reminders, analytics, CSV/Excel export, report draft/finalize/unfinalize and PDF/DOCX.

**Evidence level:** Unit subset.

**Assertions present:** Template read/create/admin denial; assignment, duplicate/missing rejection and assigned-date mapping.

**Still needed:** Add responses, scoring/recalculation, version edits, portal completion, reminders, analytics/export and report finalize/unfinalize/PDF/DOCX with exact scores/content.

**Existing files to extend:** [AssessmentServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/AssessmentServiceTest.java).

### LIB

#### LIB-01 — P1

Categories, entries, tags, entry connections, batch connections, connected suggestions, bulk create/delete/tag, search and usage counters; downstream note/template options after changes.

**Evidence level:** Unit subset.

**Assertions present:** Entry/category read/create, connections and bulk imports with duplicate skipping.

**Still needed:** Add update/delete/tag/batch connections and usage/suggestions with downstream template behavior; persisted API/UI and denial cases.

**Existing files to extend:** [LibraryServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/LibraryServiceTest.java).

### NOTE

#### NOTE-01 — P0

General notes versus session notes; note templates, draft/edit/finalize/amend, voice reprocess, PDF; historical record integrity and edit permissions.

**Evidence level:** API/unit subset.

**Assertions present:** Session draft/finalize/amend API; finalized edit/delete unit rejection; client mismatch; general-note author-only edit.

**Still needed:** Test full draft/finalize/amend history and PDF in UI, durable failed-edit invariants, general-note lifecycle and voice reprocess separately.

**Existing files to extend:** [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [SessionNoteServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionNoteServiceTest.java); [NoteServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/NoteServiceTest.java); [SessionNoteAiTemplateServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionNoteAiTemplateServiceTest.java).

### REPORT

#### REPORT-01 — P0

Report templates, selected source records, supporting files, generated/draft/final content, regeneration and downloads; wrong-client source rejection and empty-source behavior.

**Evidence level:** Unit subset.

**Assertions present:** Report view/supporting-download audit, empty-source handling and HTML/document extraction utilities.

**Still needed:** Add source selection and wrong-client rejection, attachments, generate/edit/finalize/regenerate, exact download contents and preservation of clinician edits.

**Existing files to extend:** [ClientReportServiceAuditTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ClientReportServiceAuditTest.java); [ReportSupportingFileServiceAuditTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ReportSupportingFileServiceAuditTest.java); [ClientReportEmptySourcesTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/report/ClientReportEmptySourcesTest.java); [ReportDocumentExtractionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/report/ReportDocumentExtractionServiceTest.java).

### DOC

#### DOC-01 — P0

Upload, share/revoke, review queue/summary/dashboard, file and DOCX viewer, download, delete, client portal upload/delete and permission checks.

**Evidence level:** API/unit subset.

**Assertions present:** Upload/share/unshare/list/review/delete; exact bytes for staff file/download; anonymous/restricted/other-tenant denial; unit unshared portal access rejection.

**Still needed:** Add portal direct-file denial after revoke, wrong-client same-tenant IDs, browser preview/DOCX, invalid/corrupt files and review dashboard counts.

**Existing files to extend:** [documents.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/documents.spec.mjs); [DocumentServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/DocumentServiceTest.java); [ClientPortalDocumentAccessTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/client/portal/ClientPortalDocumentAccessTest.java).

### TASK

#### TASK-01 — P0

Task CRUD, assignment/status/due dates, comments CRUD with author rules, history/stats/recent/upcoming, overdue processing and configured notification effects.

**Evidence level:** UI/API/unit subset.

**Assertions present:** Task create/delete and comment create/edit/delete; blank comment rejects without row; UI comment persists after reload; selected access denials.

**Still needed:** Add comment non-author edit/delete and wrong task/comment pair, reassignment, dates/status rules, overdue job, counts/history and actual notification chain.

**Existing files to extend:** [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [api-contracts.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/api-contracts.spec.mjs); [TaskServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/TaskServiceTest.java).

### CHECK

#### CHECK-01 — P1

Template/item CRUD, assignment/bulk assignment, assigned-item completion and compliance reporting; template-change effects on existing instances.

**Evidence level:** API/unit subset.

**Assertions present:** Template/item create, assign, complete and reread; duplicate/missing/role rules and due-offset mapping.

**Still needed:** Add item/template edits/deletes, bulk assignments, compliance report and impact of template changes on existing assignments through UI/API.

**Existing files to extend:** [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [ChecklistServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/ChecklistServiceTest.java).

### NOTIFY

#### NOTIFY-01 — P0

Every registered event, trigger, recipient/channel/preference, template placeholder, action link, unread/read/delete state; setup health/coverage/sync/seed, broadcasts, cleanup, inbound SMS and retries.

**Evidence level:** API/database/unit subset.

**Assertions present:** Direct in-app create/list/read/delete and recipient exclusion; client-created durable therapist notification/audit; trigger CRUD; SMS consent/STOP/START units.

**Still needed:** Map originating action to every event/recipient/channel/link; test preferences, duplicate/retry, scheduled dispatch, unread counts and notification administration. Direct creation is not event delivery.

**Existing files to extend:** [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [NotificationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/NotificationServiceTest.java); [ClientCreatedEffectsIntegrationTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/integration/client/ClientCreatedEffectsIntegrationTest.java); [SmsNotificationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SmsNotificationServiceTest.java); [SmsInboundServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SmsInboundServiceTest.java).

### SETTINGS

#### SETTINGS-01 — P1

System Options, Services, Public Site, Invoice Policies, Therapy Rooms, Library Categories and Administration tabs; URL/legacy-tab redirects; every configuration field's downstream effect.

**Evidence level:** API/UI/unit subset.

**Assertions present:** Option create/update/read and tenant category isolation; practice name save/read/restricted denial; rooms tab GET 200.

**Still needed:** Add every editable field's validation and downstream effect; services/policies/rooms/library/public-site/admin tabs, URL aliases and referenced-option changes.

**Existing files to extend:** [clinical-admin.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/clinical-admin.spec.mjs); [workflows.spec.mjs](/Users/apple/IdeaProjects/smarthub/qa/browser/workflows.spec.mjs); [SystemOptionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SystemOptionServiceTest.java); [InvoicePolicyServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/InvoicePolicyServiceTest.java).

### AI

#### AI-01 — P0

Assistant conversation, session-note/template generation, field options, connected suggestions, reports and regeneration; consent, feature limits, timeouts, invalid content and preservation of clinician edits.

**Evidence level:** Unit subset.

**Assertions present:** Template/assistant response calls; diarization refusal retry and PHI-safe prompt checks; template ownership; missing provider approval blocks clinical calls.

**Still needed:** Add real UI workflows with deterministic provider responses, consent/feature caps, invalid/timeout/partial outputs and clinician-edit preservation; separate quality evaluation.

**Existing files to extend:** [AiServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/AiServiceTest.java); [SessionNoteAiTemplateServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionNoteAiTemplateServiceTest.java); [OpenAiClientComplianceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/ai/service/OpenAiClientComplianceTest.java); [SessionTranscriptServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SessionTranscriptServiceTest.java).

### PLATFORM

#### PLATFORM-01 — P0

Organization onboarding/provisioning, tenant routing, roles/permissions, feature catalog and rollout, per-tenant overrides, usage/metrics, integrations/API key rotation, security and impersonation.

**Evidence level:** Unit/slice subset.

**Assertions present:** Tenant routing, roles, controller ACL, quotas, webhook-secret persistence and impersonation end revocation.

**Still needed:** Add organization provisioning through real schema/auth, feature rollout/override, API-key rotation and full impersonation/tenant isolation journeys.

**Existing files to extend:** [PlatformTenantRoutingServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/PlatformTenantRoutingServiceTest.java); [TenantResolutionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/TenantResolutionServiceTest.java); [RolePermissionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/RolePermissionServiceTest.java); [SuperAdminIntegrationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminIntegrationServiceTest.java); [SuperAdminImpersonationServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminImpersonationServiceTest.java).

#### PLATFORM-02 — P0

Plans/entitlements/add-ons, invoices, dunning, notifications, CMS landing/learning-hub editing, booking-request lifecycle, public consultation and public service visibility.

**Evidence level:** Unit/slice subset.

**Assertions present:** Plan/subscription limits, addon price/status and invoice/dunning contracts.

**Still needed:** Add plan/addon purchase and entitlement lifecycle; public CMS publish/unpublish, consultation/booking request and service visibility UI/API flows.

**Existing files to extend:** [SuperAdminSubscriptionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminSubscriptionServiceTest.java); [SuperAdminAddonServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminAddonServiceTest.java); [PlatformSubscriptionInvoiceServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/PlatformSubscriptionInvoiceServiceTest.java); [SubscriptionLifecycleServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SubscriptionLifecycleServiceTest.java).

### OPS

#### OPS-01 — P0

Suspend/reactivate/terminate and bulk operations, backup/jobs, restore verification, migration/schema integrity, encryption/blind-index backfills, refunds retry, audio/transcript retention and client purge, all in isolated fixtures.

**Evidence level:** Unit/local-load subset; backup unsupported.

**Assertions present:** Migration history guard; encryption/blind-index and migration planners; local load/stress; backup tests deliberately assert 501 unsupported and failed placeholder job.

**Still needed:** Add real forward-Flyway schema fixtures, restore/recovery, retention/purge/jobs and suspend/reactivate/terminate isolation. Backup success requires capability implementation or explicit unsupported decision; local load is not production capacity.

**Existing files to extend:** [MigrationGuardrailTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/migration/MigrationGuardrailTest.java); [SuperAdminOperationsBackupTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/SuperAdminOperationsBackupTest.java); [EncryptionServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/EncryptionServiceTest.java); [BlindIndexServiceTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/unit/service/BlindIndexServiceTest.java); [StressTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/performance/StressTest.java); [LoadTest.java](/Users/apple/IdeaProjects/smarthub/src/test/java/com/smart/therapy/flow/performance/LoadTest.java).

## Verification of this audit

- Parsed the backend XML reports independently and confirmed 1,178 tests, 234 suites, no reported failures/errors/skips. Verified the three empty Stripe cases are included as passes.
- Parsed the historical Playwright JSON and independently classified its 47 cases as 7 UI, 31 API, 9 helper checks.
- Checked that the ledger contains exactly the same 73 unique IDs as qa/scope.json and that every referenced existing test file resolves on disk.
- Product code, tests, qa/scope.json and Stage 6 files were not edited. Only this report and its separate JSON evidence ledger were created in the repository. The report and JSON evidence ledger are versioned alongside the regression follow-ups; execution artifacts under qa-results remain local. No tests, deployments, provider calls or production operations were run.

After this audit, Stage 6 may continue changing frontend sources and results. Reconcile its final report before implementing overlapping frontend checks. Historical passes remain tied to their own source snapshots.
