# SmartHub detailed workflow test specification

Status: planned assertions informed by source inspection on 2026-09-11; none of these checkboxes is evidence of a passing test. This expands the comprehensive testing plan. It does not replace the source inventory or its outstanding reconciliation work.

## What was inspected

The generated [source inventory](source-inventory.md) lists backend mapping declarations, their query inputs, request/filter DTO fields, frontend routes, filter/query type fields, and background entry points. Its companion [JSON manifest](source-inventory.json) also includes UI control candidates, filter/search/sort state, test-file names, and working-tree status. The source scanner reads both repositories and does not execute application code or contact providers.

The scanner found 75 controllers with 870 HTTP mapping declarations, 573 query-input declarations, 1,390 request/filter DTO field declarations, 137 frontend route declarations, and 29 scheduled/listener annotation entry points. These are source declaration counts, not distinct business features, unique filters, or test counts. Aliases and repeated controls exist. The frontend had existing scheduling changes during discovery; they were read but not modified.

Do not call coverage complete until UI controls, API operations, backend rules, and background effects have been reconciled. Dynamic/inherited behavior requires inspection beyond the scanner. Read expected behavior from service rules and product requirements; do not make tests that simply reproduce whatever the current implementation happens to do.

## Required coverage by actor

Every applicable workflow must be exercised separately for platform administrator, organization administrator, therapist, staff/custom role, and client portal. Also exercise anonymous, expired-session, deactivated-account, wrong-organization, wrong-client, and unassigned-user access where relevant. Shared components do not prove identical role behavior.

- [ ] ROLE-01: Resolve host/tenant and identity, activate accounts, login, MFA, password recovery, refresh, logout, idle timeout, known devices and session revocation.
- [ ] ROLE-02: Verify navigation, nested tabs, direct deep links, API permission enforcement, read-only permissions, caseload restrictions and role changes during a session.
- [ ] ROLE-03: Test plan/feature enablement and usage limits independently from user permissions; hide/disable the UI appropriately and enforce the same rules in the backend.
- [ ] ROLE-04: Verify therapist dashboard, upcoming/previous/overdue sessions, assigned clients, billing visibility, tasks, profile, availability and security.
- [ ] ROLE-05: Verify staff route guards, index redirect, no-access page, custom roles, and permitted client/content/scheduling actions.
- [ ] ROLE-06: Verify client activation, profile/avatar, timezone, appointment browsing/booking/detail/cancel/reschedule, session history/rating, invoices/payments/receipts, documents, forms, assessments, consents and notifications. Assess API-only versus UI-supported operations explicitly.
- [ ] ROLE-07: Verify organization administration and platform administration independently, including impersonation entry/exit, audit attribution, time limits and isolation.

Evidence anchors: frontend `src/routes/index.tsx`, layouts and `StaffRouteGuard`; backend `ClientPortalController`, `UserController`, authorization/caseload services and subscription gates.

## Zoom and remote-session lifecycle

- [ ] ZOOM-01: Own therapist credential configuration, status, test and removal; admin visibility/test permissions; missing/incomplete/invalid credentials; secrets absent from responses and logs.
- [ ] ZOOM-02: Meeting creation from staff scheduling and separately from client portal booking; correct therapist account, date, timezone, duration, service and session association.
- [ ] ZOOM-03: Expired access token refresh, provider denial/timeouts/rate limiting, and failure during session creation. Define and verify whether the local session persists or rolls back.
- [ ] ZOOM-04: Repeated submit/retry does not unintentionally create extra meetings or sessions; verify persisted meeting ID, join URL and password.
- [ ] ZOOM-05: Client and therapist see their permitted meeting information. Start/join labels and actions match actual provider URLs; a join link alone is not proof of host capabilities.
- [ ] ZOOM-06: Missing URL, Zoom disabled, in-person session, cancelled/no-show and other supported statuses produce the intended join visibility and direct endpoint behavior.
- [ ] ZOOM-07: Reschedule, recurring edits, therapist reassignment, cancellation and Zoom disablement reconcile local and provider state according to documented behavior. Inspect and flag missing synchronization rather than assuming it exists.
- [ ] ZOOM-08: Integration health, therapist availability, service eligibility and provider-test options return correctly scoped results and do not expose credentials.
- [ ] ZOOM-09: Isolated two-account provider smoke: intended therapist and client can enter the same meeting, while unauthorized users cannot retrieve its protected application details. Use provider sandbox/test accounts.
- [ ] ZOOM-10: Independently verify whether both speakers in a remote meeting are captured by the application's recording setup. Test headphones versus speakers and echo cancellation; record unsupported capture configurations as gaps.

Evidence anchors: `UserController`, `AdminIntegrationHealthController`, `SessionController.getZoomMeetingDetails`, `ClientPortalService` online booking, both Zoom service implementations, frontend `ZoomIntegrationSection`, `ZoomMeetingJoinBlock`, `utils/zoomMeeting.ts`.

## Voice, live transcription, saved transcripts and clinical note generation

There are distinct paths: microphone recording, a live websocket preview, uploaded audio chunks/finalization, speaker diarization, and note/assessment transcription. Do not credit one path's success to the others. `RecordSessionModal` uses `getUserMedia` microphone input; the inspected capture path does not establish direct Zoom remote/system-audio capture.

- [ ] AUDIO-01: Microphone enumeration/selection, permission grant/deny, missing or disconnected device, unsupported browser/codec, empty input and microphone-level display.
- [ ] AUDIO-02: Start, pause, resume, stop/save, close/dismiss and repeated clicks; duration accounting; microphone/media/socket cleanup after success, failure and navigation.
- [ ] AUDIO-03: Use synthetic two-speaker fixtures with known dialogue, silences, interruptions and language changes. Verify captured content, order, timestamps and completeness rather than only HTTP success.
- [ ] AUDIO-04: Language selection and translation-to-English options flow through the UI/request/provider/result correctly.
- [ ] AUDIO-05: Consent and permissions at upload start, chunk processing, finalization, retrieval, diarization and smart fill; check consent withdrawal between steps and cross-session/upload-ID access.
- [ ] AUDIO-06: Websocket tickets bound to upload IDs, expiration, missing/wrong ticket, disallowed origins, organization context, and cleanup of context after callbacks.
- [ ] AUDIO-07: Live connection establishment, queued frames, partial/final messages, network loss, upstream disconnect, timeout, retry/reconnect behavior and useful UI errors without losing saved-upload state.
- [ ] AUDIO-08: Chunk index/size/duration validation, silent chunks, duplicate and out-of-order chunks, provider errors, rate limiting, local blob recovery and retry limits.
- [ ] AUDIO-09: Finalization with complete, missing, failed and silent chunks; expected-count limits; ordered stitching and visible gap markers; repeated finalize; prevent saving an apparently complete transcript when accounting is incomplete.
- [ ] AUDIO-10: Saved transcript status/detail/list, download, copy, reopen and delete; client/session scoping and cross-tab consistency.
- [ ] AUDIO-11: Speaker diarization only on eligible transcripts; preserve original text; cache/idempotency; invalid/empty/refusal output; do not silently treat uncertain patient/clinician attribution as reliable.
- [ ] AUDIO-12: Transcript smart fill and review map to the intended note fields, preserve clinician edits, handle missing fields and require the supported review/save step.
- [ ] AUDIO-13: Session-note voice upload/reprocess and assessment audio transcription have independent regression cases.
- [ ] AUDIO-14: Retention expiry, transcript/chunk soft deletion, audio cleanup, failed-upload remnants and absence of transcript content in operational logs.
- [ ] AUDIO-15: Run deterministic provider-stub regression checks automatically; evaluate actual recognition/speaker accuracy with known fixtures separately. Clinical correctness and physical audio capture need explicit quality evidence beyond a green API test.

Evidence anchors: frontend `components/sessions/recording/*` and `utils/recording/*`; backend `SessionTranscriptService`, `SessionNoteController`, `AssessmentController`, `ConsentPolicyService`, `transcription/ws/*`, `AudioCleanupScheduler`, `TranscriptRetentionScheduler`.

## Pricing, invoice policy, discounts and payments

Current source distinguishes invoice-policy pricing from a discount applied to an existing outstanding balance. Establish approved expected amounts and test both independently.

- [ ] MONEY-01: Service code/rate CRUD, activation, therapist/portal visibility, defaults, duration and service-specific scheduling/billing effects.
- [ ] MONEY-02: Policy CRUD, activate/deactivate/delete, required scopes, duplicate scope, fixed/percentage pricing, invalid/negative/>100 percentage and invalid effective date ranges.
- [ ] MONEY-03: Policy matching for client type, appointment status, service, wildcard `all`, start/end dates and practice timezone. Verify precedence: service specificity, client type specificity, status specificity, priority, then updated time as currently implemented; test ambiguous ties explicitly.
- [ ] MONEY-04: Completed/no-show/manual billing eligibility, future and cancelled sessions, duplicate billing, transaction rollback and response mapping after commit.
- [ ] MONEY-05: Discount none/fixed/percentage, aliases and invalid type; positive values, configured maximum percentage, rounding, fixed amount exceeding outstanding and a fully paid invoice.
- [ ] MONEY-06: Partial-payment example: $100 total, $40 paid, then 10% discount must follow the reviewed rule. Current implementation computes $6 discount against $60 outstanding, leaving $54 outstanding. Verify API, UI, persisted values, PDF and receipt agree.
- [ ] MONEY-07: Existing nonzero discount cannot be replaced without removal under current service rule; verify remove/reapply, duplicate clicks and concurrent requests.
- [ ] MONEY-08: `ADVANCED_BILLING` feature and `BILLING_MANAGE` permission gates; denied attempts must not change balance, status or audit history as a successful mutation.
- [ ] MONEY-09: Discount-induced status changes, zero outstanding, discount removal after payment and reconciliation of amount due, paid and outstanding; never manufacture a payment from a discount.
- [ ] MONEY-10: Insurance/copay snapshots, client/insurance split payment, cumulative versus delta amounts, repeated payment requests, stale/concurrent edits and ledger consistency.
- [ ] MONEY-11: Stripe checkout/payment outcome, abandoned payment, signed webhook validation, duplicate/out-of-order webhook events, refunds and retry paths where supported.
- [ ] MONEY-12: Billing history, client portal invoices, payment status filters, totals/statistics, PDF/HTML receipts and exports all agree with persisted records and selected scope.
- [ ] MONEY-13: Organization subscription invoices are separate from patient billing: plan/add-ons, limits, subscription checkout, renewal/failure, dunning, cancellation, refund/dispute and grace/access transitions.

Evidence anchors: `BillingService.applyDiscount`, `applyDiscountValues`, `InvoicePolicyService`, `InvoicePolicyRepository`, `Stripe*Service`, `OrgStripeConnectService`, frontend `ApplyDiscountModal`, `InvoicePolicyModal`, payment/subscription screens.

## Each filter, search, sort and pagination control

Use [source-inventory.md](source-inventory.md) for exact frontend type fields and endpoint query-input names. The 573 backend query declarations include pagination, upload parameters and other inputs; do not mislabel all of them as filters. Inherited types and inline query objects must be reconciled as well.

| Surface | Explicit fields discovered; expand inherited and inline definitions during reconciliation |
| --- | --- |
| Client lists | search, status, stage, therapistId, clientType, hasPortalAccess, hasPendingTasks, hasNoSessions, needsFollowUp, unassigned, includeUnassigned, checklistTemplateId, reportTemplateId, sortBy, sortOrder, page/pageSize; resolve frontend aliases such as noSessions |
| Scheduling | startDate/endDate, therapistId, clientId/clientSearch, status, sessionType, serviceId/serviceCode, roomId, mySessionsOnly, includeHiddenServices, view, page/pageSize; UI therapist/status/service selection and calendar view |
| Availability and conflicts | date/sessionDate, therapistId, roomId, duration, serviceId, sessionType, excludeSessionId; portal date ranges; integration-health timezone/includeSlots/therapistIds/live-test options |
| Billing overview | clientId/clientSearch, therapistId, status, serviceCode, clientType, sessionType, paymentMethod, startDate/endDate, minAmount/maxAmount, sort/direction, page/size; map UI paymentStatus and billingStatus to actual request logic |
| Billing history | clientId, therapistId, paymentStatus, billingStatus, startDate/endDate, page/limit |
| Tasks and task history | status, priority, assignedToId, clientId, search, dateFilter, fromDate/toDate, dateField, sortBy/sortOrder, page/pageSize; stats filters must match their stated scope |
| Client appointments | startDate/endDate, status, serviceId, sessionMode; API appointment status/paging and session-history scope; verify client-side filtering does not omit records on unloaded pages |
| Client invoices | paymentStatus, insuranceCovered, startDate/endDate, search, page/pageSize; test `false` insurance filtering distinctly from omitted/all |
| Forms | clientId, templateId, versionStatus, assignmentStatus, assignmentDateFrom/To, dueDateFrom/To; template search/category/paging |
| Assigned checklists | clientId, templateId, category, isCompleted, completedDateFrom/To, dueDateFrom/To, createdDateFrom/To; template search/category/paging |
| Assessments | assignment search/templateId/clientId/status/from/to and paging; template category/search/state controls; analytics/export inputs |
| Consent management | consentType, status, search |
| Notifications | unreadOnly, page/pageSize; trigger/template selectors; platform history orgId/page/size; admin lists and statistics query inputs |
| Audit and HIPAA | startDate/endDate, riskLevel, hipaaOnly, action, username, clientId, resourceType; UI actionType/phiAccessOnly aliases; platform authId/mine/resourceId/q/createdFrom/createdTo/sort/order and paging |
| Users and supervision | search, role, active, paging; supervisorId, therapistId, active, search, requiredMeetingFrequency |
| Roles and permissions | search, page/pageSize, sortBy/sortDirection, module, permissionGroup |
| Organizations | search, status, plan, createdFrom/To, region, dataResidency, sort/order, page/pageSize, exportData |
| Platform organization users/invoices | organization ID, search/role/status, invoice status/sort, paging; billing invoice organization aliases and export status |
| Library, documents, reports, public content and remaining lists | Every controller query input plus each inline UI search/filter/tab/sort field in the inventory. These require continued wiring reconciliation; the named examples above do not waive them |

For **each** actual filter, implement the following applicable assertions with stable scenario IDs:

- [ ] FILTER-01: Default state and omitted value; each supported enum/option; populated matching and nonmatching records; invalid/stale option IDs; literal `false`/zero versus absent values.
- [ ] FILTER-02: Actual result IDs/counts match the independent expected set. Checking that a dropdown changes or a query parameter is sent is insufficient.
- [ ] FILTER-03: Apply, clear-one chip, clear-all, cancel/unapplied edits, close/reopen, reload and back navigation according to the intended persistence contract.
- [ ] FILTER-04: Date/numeric lower bound, upper bound, equal bounds, inverted bounds, open-ended ranges, timezone/DST boundaries and invalid values.
- [ ] FILTER-05: Search whitespace/case, supported exact versus partial fields, special characters, empty query, no matches, debounced requests and stale-response races.
- [ ] FILTER-06: Every supported sort key and direction, equal-value tie ordering, nulls, stable pagination, page reset after filter change and no duplicate/missing infinite-scroll records.
- [ ] FILTER-07: Each filter individually, pairwise combinations, all compatible filters together, contradictory combinations, and explicit high-risk multi-filter combinations. Exhaustive Cartesian products are not implied.
- [ ] FILTER-08: Role/tenant/caseload scoping still holds under filtering and direct API calls. Option lists must not leak inaccessible users/clients.
- [ ] FILTER-09: Exported rows, dashboard counts and result lists use the documented scope. If a summary intentionally has a different scope, assert and label that behavior.

## Additional workflows discovered beyond the initial examples

- [ ] CLIENT-01: Complete intake sections: personal, clinical, address, referral, employment, contacts, insurance and consent; field validation, edit/patch semantics and history.
- [ ] CLIENT-02: Portal enable/disable/activation resend, client close/reopen/delete/restore, related-record effects, purge eligibility and consent withdrawal.
- [ ] CLIENT-03: Bulk upload, stage/status changes, reassignment, portal access bulk changes, export, duplicate detect/mark/unmark; partial failures and rerun semantics.
- [ ] CLIENT-04: Overview, sessions, forms/documents, assessments, reports, tasks, checklists, billing, notes/history, stage duration, email history and SMS log/export; independent role-specific implementations and persistence.
- [ ] PEOPLE-01: User/profile lifecycle, credentials, password change, avatars, timezone, working hours, education/other profile sections, supervisor assignments and schedule eligibility after profile edits.
- [ ] FORMS-01: Template sections/fields, field ordering, versions, activate/archive, single/bulk/multi-template assignments, responses, signatures and review from staff and client sides.
- [ ] ASSESS-01: Template sections/questions/options and bulk edits, versions, assignment, single/batch responses, scoring/recalculation, reminders, analytics, CSV/Excel export, report draft/finalize/unfinalize and PDF/DOCX.
- [ ] LIB-01: Categories, entries, tags, entry connections, batch connections, connected suggestions, bulk create/delete/tag, search and usage counters; downstream note/template options after changes.
- [ ] NOTE-01: General notes versus session notes; note templates, draft/edit/finalize/amend, voice reprocess, PDF; historical record integrity and edit permissions.
- [ ] REPORT-01: Report templates, selected source records, supporting files, generated/draft/final content, regeneration and downloads; wrong-client source rejection and empty-source behavior.
- [ ] DOC-01: Upload, share/revoke, review queue/summary/dashboard, file and DOCX viewer, download, delete, client portal upload/delete and permission checks.
- [ ] TASK-01: Task CRUD, assignment/status/due dates, comments CRUD with author rules, history/stats/recent/upcoming, overdue processing and configured notification effects.
- [ ] CHECK-01: Template/item CRUD, assignment/bulk assignment, assigned-item completion and compliance reporting; template-change effects on existing instances.
- [ ] NOTIFY-01: Every registered event, trigger, recipient/channel/preference, template placeholder, action link, unread/read/delete state; setup health/coverage/sync/seed, broadcasts, cleanup, inbound SMS and retries.
- [ ] SETTINGS-01: System Options, Services, Public Site, Invoice Policies, Therapy Rooms, Library Categories and Administration tabs; URL/legacy-tab redirects; every configuration field's downstream effect.
- [ ] AI-01: Assistant conversation, session-note/template generation, field options, connected suggestions, reports and regeneration; consent, feature limits, timeouts, invalid content and preservation of clinician edits.
- [ ] PLATFORM-01: Organization onboarding/provisioning, tenant routing, roles/permissions, feature catalog and rollout, per-tenant overrides, usage/metrics, integrations/API key rotation, security and impersonation.
- [ ] PLATFORM-02: Plans/entitlements/add-ons, invoices, dunning, notifications, CMS landing/learning-hub editing, booking-request lifecycle, public consultation and public service visibility.
- [ ] OPS-01: Suspend/reactivate/terminate and bulk operations, backup/jobs, restore verification, migration/schema integrity, encryption/blind-index backfills, refunds retry, audio/transcript retention and client purge, all in isolated fixtures.

## Automation implementation and evidence contract

For each scenario add role, fixtures, precise expected results, source pointers, test implementation, backend/frontend revision plus working-tree fingerprint, execution status, and evidence. Map existing test assertions before creating duplicates. No test-file name or scanner match is counted as verified functionality.

Automate deterministic API/database assertions, UI interactions and provider contract failures. Use synthetic audio for recording automation and a separate provider/physical-device validation track. Report real Zoom/Stripe/email/SMS/AI checks separately from stubs. A successful launch of an external app does not prove audio capture, provider delivery or clinical quality.

The coverage report must list all unmapped actions, filters, unresolved wiring, missing tests, failed tests, blocked provider checks and manual checks. Add a discovery-diff check so new UI/API/configuration entries cannot silently bypass scenario review. Existing compatibility aliases need explicit equivalence checks or a documented exclusion.

Next implementation step: reconcile this source inventory with existing tests and runtime UI/API behavior, assigning stable scenario IDs to each action and filter. This document and the inventory are planning/discovery artifacts; the comprehensive test suite is not yet implemented or executed.
