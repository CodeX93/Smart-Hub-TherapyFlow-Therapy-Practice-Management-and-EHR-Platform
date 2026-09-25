# Forms lifecycle regression checks — 2026-09-11

## Scope

`FormLifecycleApiTest` uses the existing `BaseTenantApiTest`, real HTTP controllers through MockMvc, JWT authentication and a run-owned local PostgreSQL database. Templates, assignments, responses, signatures and reviews go through the application APIs. Staff/client identities and the test admin's template permission are synthetic repository fixtures. Outbound email and scheduled maintenance use the existing fixture mocks.

This is backend API coverage, not browser E2E, a production-data test, or Flyway migration validation. Local implementation fixes added after the initial audit are recorded below.

## Repeatable command

```sh
JAVA_HOME=/Users/apple/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home \
  python3 scripts/test-tenant-fixtures.py --suites qa/backend-form-lifecycle-suites.txt
```

The suite includes `FormLifecycleApiTest`, `FormServiceTest`, `FormServiceVersioningPatchTest`, `FormSubmissionValidatorTest` and `ClientPortalServiceTest`. Rejection assertions intentionally require the expected safeguard; they must not be weakened to accept an unsafe success response just to make the suite pass.

## Checks

- Template revision preserves old assignment version, instructions, field snapshot and saved answer; a new assignment receives the new field definition.
- Submitting another assignment's field ID is rejected and transaction rollback preserves the existing answer.
- Blank signature data, signer name or signer role is rejected without completing the form.
- A valid answer and signature persist completion timestamps. Review metadata is tested independently on an assignment whose completed status is seeded through the repository.
- Signing with a missing required answer is rejected.
- Signing with declined terms is rejected.
- Review preserves assignment status while saving review metadata, including for an ASSIGNED form. This matches the verified ClientHub implementation.
- A template revision cannot remove the signature requirement of an existing assignment in the portal.

## Execution evidence

Initial run `tenant-20260911-185613-53be8186`: 22 existing unit tests passed; all 10 API cases stopped during fixture setup because the synthetic admin lacked `FORM_TEMPLATE_MANAGE`. This run provides no lifecycle outcome evidence. Its database was removed.

The permission was added only to this test fixture. Run `tenant-20260911-190104-83426b6b` then executed all cases: 32 tests, 2 failures, 0 errors/skips. Its sources stayed unchanged and its database was removed. It exposed two additional test-quality issues: required-answer/terms assertions accepted any HTTP 400, including an unrelated signature database constraint error; the review rejection request lacked its required assignment ID. Those assertions/payloads were corrected before the final run, and a separate completed-review case was added. Do not count the intermediate passing rejection cases as verified lifecycle guards.

Final run `tenant-20260911-190535-e9168c30`: **33 tests, 28 passed, 5 failed, 0 errors, 0 skipped; Maven exit 1.** All three selected suites ran, source fingerprints stayed unchanged, and the run-owned database was removed. Evidence: `qa-results/tenant-20260911-190535-e9168c30/summary.json`, `cleanup.json`, and `build/surefire-reports/TEST-com.smart.therapy.flow.api.FormLifecycleApiTest.xml`.

The 22 existing unit tests passed. Of the 11 new API cases, six passed: historical field/answer preservation, foreign-field rejection with rollback, three blank-signature-metadata checks, and completed-assignment review metadata persistence.

## Issues found in the initial audit

| Failing check | Observed result | Implementation follow-up |
| --- | --- | --- |
| Valid signature completion | HTTP 400: `signature type is required and cannot be empty.` | `FormService.submitSignature` does not populate the non-null `FormSignature.signatureType`. The separate portal signature implementation already assigns `SignatureType.DRAWN`. |
| Required answer before signing | Reaches the same signature-type constraint instead of required-answer validation | Validate required assignment-field snapshots before completion. A generic 400 does not prove this rule. |
| Declined terms before signing | Reaches the same signature-type constraint instead of terms validation | Require accepted terms before persisting a signature. These tests do not claim that invalid signatures were successfully saved. |
| Review before completion | HTTP 200 | `reviewAssignment` permits review of an ASSIGNED form. Confirm whether draft review is intended before restricting statuses. |
| Historical signature requirement in portal | Before revision: HTTP 400 with `Signature required before submission`; after revision: HTTP 200 on the same assignment/token without a signature | `ClientPortalService.submitForm` reads the current template flag instead of the assigned version's signature requirement. |

The initial audit did not include fixes. These results describe the isolated backend fixture, not a verified production incident. Required-field variations (blank values, conditional fields, checkboxes), review approval/revision semantics and portal signature-image validation remain outside this batch.

## Priority fixes and ClientHub review parity

- `FormService.submitSignature` now assigns `SignatureType.DRAWN`, matching the existing portal signature implementation. The non-null signature type can be persisted and the valid-signature completion test passes.
- `ClientPortalService.submitForm` now reads `requiresSignature` from the assigned template version, falling back to the template only for legacy null version flags. A later template edit no longer removes the requirement from an existing assignment.
- ClientHub's `server/routes.ts:20658` review endpoint checks staff role, writes review metadata, and does not restrict or change assignment status. The earlier incomplete-review rejection expectation was a proposed policy, not a migration regression. It has been replaced by `reviewPreservesAssignedStatusAsInClientHub`, which verifies review metadata without completion or status changes. No review implementation change was made.

Run `tenant-20260911-191309-3a4fed62` verified both priority fixes: 33 tests, 30 passed, 3 failed, no errors/skips; unchanged sources and database cleanup confirmed. Its failures were the two outstanding validation gaps and the obsolete review rejection expectation. With signature persistence repaired, missing required answers and declined terms both produced HTTP 201, confirming that explicit validation is still absent; those reproduction checks remain enabled.

Final rerun after correcting the review expectation: `tenant-20260911-191814-9d08e044`: **33 tests, 31 passed, 2 failed, 0 errors/skips; Maven exit 1**. All suites ran, sources stayed unchanged, and the database was removed. Both priority fixes and the corrected review parity test passed. The two failures are `signatureRejectsMissingRequiredAnswer` and `signatureRejectsDeclinedTerms`, both observing HTTP 201 instead of rejection. The whole suite remains red until those separate validation gaps are addressed. Evidence: the run's `summary.json`, `cleanup.json`, and Surefire XML reports. No commit, push, deployment or production-data mutation has been performed.

## Required-answer and terms follow-up

ClientHub comparison: `server/routes.ts:19823` accepts portal signature data without a separate acceptance flag and saves new signatures with `agreedToTerms: true`. `client/src/pages/portal-form-completion.tsx:240` checks required answers on final submission, excludes signature/heading/info fields, and presents a certification statement above its signature pad (around line 774). There is no separate terms checkbox in that flow. This implementation preserves signing-as-acceptance for omitted portal flags and rejects an explicitly declined flag; no new UI checkbox is introduced.

The shared `FormSubmissionValidator` applies to staff signature submission, staff status transitions to SUBMITTED/COMPLETED/REVIEWED, and portal final submission. It checks active assigned-field snapshots against saved responses; blank/missing required answers block completion. It also requires signatures according to the assigned version (legacy null fallback retained), rejects saved signatures with declined terms, and excludes headings/info text from answer requirements. Signature fields and the existing portal signature metadata labels (Client Full Name, Date, Signatures) are satisfied by a signature rather than a separate text response.

Conditional rules support the documented equality format `{"showIf":{"fieldId":originalFieldId,"value":"yes"}}`, resolving the controller through the same assignment's field snapshots. Hidden required fields do not block submission. Malformed, unsupported, missing-reference and circular rules fail with a configuration error rather than silently bypassing validation. This does not implement a general rule engine or frontend conditional rendering.

Draft responses and portal signature save/clear remain independent of required-answer completion. Review metadata remains independent of submission status, matching ClientHub. No database migration or production-data access is needed.

Red evidence before validation: `tenant-20260911-192647-5c5a103a` ran 41 tests with 6 expected failures and no errors/skips. Expanded bypass coverage in `tenant-20260911-193246-6c2dc958` ran 47 tests with 12 expected failures and no errors/skips; both databases were removed. Failures reproduced missing/blank required answers, conditional required answers, explicit declined portal/staff terms, direct staff status bypasses and final submission with a declined stored signature.

Verification after implementation: `tenant-20260911-194125-95033ac9`: **68 tests passed, zero failures/errors/skips, Maven exit 0**. This includes 25 real PostgreSQL/MockMvc API cases and 43 unit cases. All five selected suites ran; source fingerprints stayed unchanged; the run-owned database was removed. Evidence is in that run's `summary.json`, `cleanup.json`, and `build/surefire-reports/`.

The earlier failing audit results above are historical; the final suite is green. No push, deployment or production-data mutation was performed. Validation covers presence/blankness of saved answers, not arbitrary field-format/scoring rules or a general conditional-expression language.
