# Insurance Reconciliation — Frontend Development Guide

Tenant-scoped remittance → session billing bridge.

**Base path:** `/api/v1/billing/insurance`  
**Auth:** same tenant session as other billing APIs + `BILLING_*`  
**Headers:** tenant context (`X-Tenant-Schema` / org slug) as for `/api/v1/billing/**`  
**Gate:** org must have billing module feature enabled

> Do **not** point local FE/API at production DB while developing this — see “Safe testing / shared DB” below.

---

## What to build (screens)

### 1. Statements tab
- Upload control: PDF / Excel / CSV
- Optional therapist picker on upload
- Table: file, payer, check #, therapist, status, line counts, posted total, uploaded at
- Actions: open detail, delete (only if `DRAFT`/`VOIDED`; if `POSTED` show “void first”)
- Handle upload response `{ duplicate: true, existingStatementId }` → confirm dialog → resubmit with `force=true`

### 2. Statement detail
- Header: payer, check #, statement date, `totalPaid`, status badges
- Tie-out strip (optional Phase 3): `totalPaid` vs `sumLinePaid` vs `sumLedgerPosted` using `tieOutHeaderVsLines` / `tieOutLinesVsLedger`
- Actions by status:
  - `DRAFT` / `POSTED` leftovers: Rematch, Post
  - `POSTED`: Void (reason required)
  - `VOIDED`: Reopen, Delete
- Line table: date, name (editable), CPT, billed/allowed/paid/patient resp, remark, match status, confidence, session/billing link
- Per-line: Confirm / Unconfirm / Skip / Clear / Link / Reverse (when `POSTED`)
- Denial hint: `denialHeuristic === true` (paid ≤ 0 + remark code)

### 3. Transactions tab
- Flat line list: `GET /transactions`
- Filters: search `q`, `therapistId`, `matchStatus`, `needsAttention=true`, optional `statementIds`
- Bulk Rematch / Post scoped to **visible** `statementIds` only (never silent full-org when therapist filter active)

### 4. Billing Dashboard / Client Detail (small hooks)
- Manual insurance pay: handle `422 DUPLICATE_INSURANCE_PAYMENT` → advisory modal; allow override with `overrideReason`
- Void payment: if `409 STATEMENT_SOURCED_PAYMENT`, deep-link to Insurance Reconciliation instead of voiding

---

## Status enums (send/receive uppercase)

| Kind | Values |
|------|--------|
| Statement | `DRAFT`, `POSTED`, `VOIDED` |
| Line match | `UNMATCHED`, `SUGGESTED`, `CONFIRMED`, `POSTED`, `SKIPPED`, `REVERSED` |
| Confidence | `HIGH`, `MEDIUM`, `LOW`, `PARTIAL` |

**Money rule:** auto-match only creates `SUGGESTED`. Only **Post** of `CONFIRMED` lines moves money. `postedAmount` = net shortfall added (not always full remittance paid).

---

## API map

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/statements` | multipart `file`, optional `therapistId`, `force` |
| `GET` | `/statements` | `status`, `therapistId`, `page`, `pageSize` |
| `GET` | `/statements/{id}` | detail + lines + tie-out |
| `PATCH` | `/statements/{id}/therapist` | `{ "therapistId": 12 \| null }` |
| `POST` | `/statements/{id}/rematch` | |
| `POST` | `/statements/{id}/post` | |
| `POST` | `/statements/{id}/void` | `{ "reason": "..." }` min 3 chars |
| `POST` | `/statements/{id}/reopen` | |
| `DELETE` | `/statements/{id}` | 204; draft/voided only |
| `GET` | `/transactions` | work queue |
| `POST` | `/rematch-all` | `{ "statementIds": [...] }` optional |
| `POST` | `/post-all` | same |
| `PATCH` | `/lines/{id}` | actions below |
| `PATCH` | `/lines/{id}/fields` | OCR fixes + rematch |
| `POST` | `/lines/{id}/reverse` | optional `{ "reason" }` |
| `POST` | `/lines/{id}/create-billing-and-confirm` | back-bill then confirm |

### Upload example

```http
POST /api/v1/billing/insurance/statements
Content-Type: multipart/form-data

file: remittance.xlsx
therapistId: 12
force: false
```

Duplicate without force:

```json
{ "duplicate": true, "existingStatementId": 9, "message": "..." }
```

### Patch line actions

```json
{ "action": "confirm" }
{ "action": "unconfirm" }
{ "action": "skip", "skipReason": "Denial" }
{ "action": "clear" }
{ "action": "link", "sessionBillingId": 55 }
{ "action": "link", "sessionId": 100 }
```

### Errors worth handling in UI

| Code | HTTP | UX |
|------|------|-----|
| `NO_PAYMENT_LINES` | 422 | Show parse failure |
| `PARSE_FAILED` / `AI_EXTRACT_FAILED` | 422 / 503 | Offer Excel/CSV fallback for PDF |
| `NO_CONFIRMED_LINES` | 422 | Need confirms before Post |
| `STATEMENT_VOIDED` / `STATEMENT_POSTED` | 409 | Disable illegal actions |
| `STATEMENT_SOURCED_PAYMENT` | 409 | From normal void API → redirect here |
| `DUPLICATE_INSURANCE_PAYMENT` | 422 | Advisory + override |

Payment txn payloads now include nullable: `sourceStatementId`, `sourceStatementLineId`, `adoptedByLineId`.

---

## How to test the flow (happy path)

Prereqs: local API on **separate** DB (not shared prod), tenant with billing module, admin/billing user, at least one **completed** session + client name that will appear on the remittance.

1. **Seed match target**  
   Create client (e.g. “Jane Doe”), complete a session on a known service date (practice timezone). Optionally create billing, or let confirm back-bill.

2. **Prepare file**  
   CSV/Excel columns roughly: Patient/Client, Date of Service, CPT, Paid, Allowed, Billed. One row matching Jane + that date + paid amount.

3. **Upload**  
   `POST /statements` → expect `DRAFT`, lines `SUGGESTED` or `UNMATCHED`.

4. **Detail**  
   Confirm suggested lines (`PATCH .../lines/{id}` `{ "action":"confirm" }`).  
   If no billing yet: `POST .../create-billing-and-confirm` or confirm (server creates billing).

5. **Post**  
   `POST .../statements/{id}/post` → line `POSTED`, `postedAmount` ≥ 0.  
   Check session billing `insurancePaidAmount` increased by shortfall only.

6. **Guards**  
   Try void that payment from normal billing UI → `409 STATEMENT_SOURCED_PAYMENT`.  
   Try manual insurance pay same amount → `422 DUPLICATE_INSURANCE_PAYMENT`.

7. **Undo**  
   Reverse one line or void whole statement with reason → reopen → re-post.

8. **Transactions tab**  
   `GET /transactions?needsAttention=true` after leaving unmatched lines.

9. **PDF path** (optional)  
   Needs OpenAI configured; otherwise use Excel/CSV.

---

## Safe testing / shared DB (read this first)

If local `DB_URL` is the **same Azure Postgres as production**:

- Public Flyway on startup can change **public** schema.
- Tenant migration job (`tenant.jobs.migration.enabled`, default on) will apply **V47 to every tenant behind on version** (including prod orgs).

**Recommended:** develop against a **local Postgres** or dedicated **staging** database. Do not run this branch against prod DSN.

Safer options if you must keep shared Azure temporarily:

1. Point local profile to a **cloned** Azure database (separate server/DB name), not prod.
2. Or set locally (and never on prod app settings):
   - `spring.flyway.enabled=false`
   - `tenant.jobs.migration.enabled=false`  
   …but then insurance tables won’t exist unless you manually create them on a **non-prod** test schema only — easy to get wrong; prefer option 1.
3. Do **not** enable `tenant.migration.run-on-startup=true` against shared Azure.
4. Keep V47 + insurance APIs off production deploy until you intentionally cut over staging/prod.

Additive V47 (nullable columns + new tables) is *backward compatible* for an old prod JAR **if** somehow applied — but you still should not apply it from a laptop against live prod. Treat schema cutover as a deliberate deploy.
