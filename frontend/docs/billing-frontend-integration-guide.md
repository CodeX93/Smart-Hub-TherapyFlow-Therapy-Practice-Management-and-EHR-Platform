# Billing & Invoice Policy — Frontend Integration Guide

**Audience:** `trappy-flow-frontend` team  
**Backend:** `smarthub` (`billing-feature` branch)  
**API base:** Tenant subdomain URL (see §1)  
**Swagger:** `{tenant-api}/swagger-ui.html`

---

## 1. Prerequisites

### 1.1 Tenant URL & auth (existing pattern)

Staff and portal calls use the same RTK Query setup in `src/store/api/baseApi.ts`:

- **Authorization:** `Bearer {accessToken}` from `getAuthSession()`
- **Tenant routing:** `buildTenantApiBase(session.tenantSubdomain)` → `{subdomain}.{api-host}/api/v1/...`

No `X-Tenant-Subdomain` header is required when using subdomain URLs.

### 1.2 Plan features (must be enabled per org)

| Feature code | Required for |
|---|---|
| `BILLING_MODULE` | All billing & invoice-policy endpoints |
| `STRIPE_PAYMENTS` | Client portal “Pay invoice” (Stripe Checkout) |

If missing, API returns **403** with a message like *“Billing is not included in your plan…”*.

### 1.3 Permissions (staff UI)

From `src/utils/staffPermissions.ts`:

| Permission | UI capability |
|---|---|
| `BILLING_VIEW` | Read billing list, stats, history, invoice preview |
| `BILLING_EDIT` | Record payment, change status, apply discount |
| `BILLING_MANAGE` | Invoice policies CRUD, Stripe Connect, services admin |

`manageInvoicePolicies` and `manageBillingServices` already map to `BILLING_MANAGE`.

---

## 2. Architecture overview

```
┌─────────────────┐     POST /sessions/{id}/billing      ┌──────────────────┐
│ Staff billing   │ ───────────────────────────────────► │ Session invoice  │
│ (policy rate)   │     (auto-applies invoice policy)  │ billingStatus    │
└─────────────────┘                                      └────────┬─────────┘
                                                                  │
┌─────────────────┐     POST /portal/invoices/{id}/pay           │
│ Client portal   │ ───────────────────────────────────────────►│ Stripe Checkout
└─────────────────┘     returns checkoutUrl                     │
                                                                  ▼
                                                         Webhook → status PAID
```

**Invoice policies** are configured by admins; **rates are applied server-side** when staff creates session billing (`POST /billing/sessions/{sessionId}/billing`). The frontend does not calculate policy prices—it displays `invoicePolicyId` and amounts from the API.

---

## 3. Staff billing APIs (mostly integrated)

Existing slice: `src/store/api/admin/billing.api.ts`  
Pages: `src/pages/staff/billings`, admin/therapist billing tabs.

### 3.1 Already wired in frontend

| Action | Method | Path |
|---|---|---|
| Dashboard stats | `GET` | `/api/v1/billing/statistics` |
| Invoice list (history) | `GET` | `/api/v1/billing/history` |
| Record payment | `POST` | `/api/v1/billing/billing/{id}/record-payment` |
| Apply discount | `PATCH` | `/api/v1/billing/billing/{id}/discount` |
| Change status | `PATCH` | `/api/v1/billing/billing/{id}/status` |
| Email invoice | `POST` | `/api/v1/billing/billing/{id}/send-invoice-email` |
| Preview / download | `GET` | `/api/v1/billing/billing/{id}/invoice-preview` / `invoice-download` |

### 3.2 New / extend for full billing module

| Action | Method | Path | Notes |
|---|---|---|---|
| Create invoice for session | `POST` | `/api/v1/billing/sessions/{sessionId}/billing` | Body: optional `serviceId`, `units`, discounts |
| Get session invoice | `GET` | `/api/v1/billing/sessions/{sessionId}/billing` | |
| Paginated billing list | `GET` | `/api/v1/billing/billing` | Filters: `status`, `clientId`, dates, etc. |
| Payment transactions | `GET` | `/api/v1/billing/billing/{id}/transactions` | |
| Void transaction | `POST` | `/api/v1/billing/billing/{id}/transactions/{txId}/void` | |

### 3.3 Key response fields (`SessionBillingResponse` / history)

| Field | Use in UI |
|---|---|
| `billingStatus` | `pending`, `billed`, `paid`, `denied`, `follow_up`, `cancelled` |
| `amountDue` | After discount |
| `remainingDue` | Show “balance due”; hide Pay when `0` |
| `invoicePolicyId` | Optional badge “Policy applied” |
| `stripeCheckoutSessionId` | Set after client starts Stripe pay |

### 3.4 Record payment body

```json
{
  "paymentAmount": 150.00,
  "paymentMethod": "credit_card",
  "referenceNumber": "optional",
  "notes": "optional"
}
```

`paymentMethod` values: `cash`, `check`, `credit_card`, `debit_card`, `insurance`, `bank_transfer`, `online_payment`, `credit_balance`.

---

## 4. Invoice policy APIs (**new frontend work**)

Base path: **`/api/v1/billing/invoice-policies`**

Not yet in RTK Query—add e.g. `src/store/api/admin/invoicePolicy.api.ts` following `billing.api.ts`.

### 4.1 Endpoints

| Method | Path | Permission | Description |
|---|---|---|---|
| `GET` | `/invoice-policies` | `BILLING_VIEW` | List all policies |
| `GET` | `/invoice-policies/{id}` | `BILLING_VIEW` | Get one |
| `POST` | `/invoice-policies` | `BILLING_MANAGE` | Create → **201** |
| `PUT` | `/invoice-policies/{id}` | `BILLING_MANAGE` | Update |
| `PATCH` | `/invoice-policies/{id}/activate` | `BILLING_MANAGE` | Enable rule |
| `PATCH` | `/invoice-policies/{id}/deactivate` | `BILLING_MANAGE` | Disable rule |
| `DELETE` | `/invoice-policies/{id}` | `BILLING_MANAGE` | Delete → **204** |
| `GET` | `/invoice-policies/options/client-types` | `BILLING_VIEW` | Dropdown options |
| `GET` | `/invoice-policies/options/appointment-statuses` | `BILLING_VIEW` | Dropdown options |

### 4.2 Request / response shape

**Create / update body (`InvoicePolicyRequest`):**

```json
{
  "clientTypeKey": "individual",
  "clientTypeLabel": "Individual",
  "appointmentStatusKey": "completed",
  "appointmentStatusLabel": "Completed",
  "enabled": true,
  "priceType": "FIXED",
  "invoicePrice": 150.00,
  "policyName": "Individual completed session",
  "serviceId": null,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null,
  "priority": 10
}
```

| Field | Rules |
|---|---|
| `priceType` | `FIXED` → `invoicePrice` is dollar amount; `PERCENTAGE` → `invoicePrice` is percent of service base rate |
| `serviceId` | `null` = all services; set to scope one service |
| `priority` | Higher wins when multiple rules match |
| `effectiveFrom` / `effectiveTo` | Optional date window (inclusive) |

**Response (`InvoicePolicyResponse`):** same fields plus `id`, `createdAt`, `updatedAt`.

### 4.3 Admin UI recommendations

- **Settings → Billing → Invoice policies** table (sort by client type + appointment status).
- Form uses **options endpoints** for client type and appointment status—not hardcoded lists.
- Show `enabled` toggle via activate/deactivate endpoints (or include `enabled` on PUT).
- Explain in UI: policies apply when staff **creates** session billing, not retroactively.

### 4.4 RTK Query sketch

```typescript
// invoicePolicy.api.ts — inject into baseApi
getInvoicePolicies: builder.query<InvoicePolicyResponse[], void>({
  query: () => ({ url: "/api/v1/billing/invoice-policies", method: "GET" }),
  providesTags: ["InvoicePolicies"],
}),
createInvoicePolicy: builder.mutation<InvoicePolicyResponse, InvoicePolicyRequest>({
  query: (body) => ({
    url: "/api/v1/billing/invoice-policies",
    method: "POST",
    body,
  }),
  invalidatesTags: ["InvoicePolicies"],
}),
// … update, activate, deactivate, delete, options
```

Add `"InvoicePolicies"` to `tagTypes` in `baseApi.ts`.

---

## 5. Stripe Connect (org admin)

Base: **`/api/v1/admin/stripe-connect`** — requires `BILLING_MANAGE`.

Documented in `trappy-flow-frontend/docs/ADMIN_FRONTEND_INTEGRATION_GUIDE (1).md`; summary:

| Step | API | UI action |
|---|---|---|
| 1 | `GET /status` | Show connection state |
| 2 | `POST /oauth/start` | Redirect to `authorizeUrl` |
| 3 | Stripe redirects to backend callback | No frontend handler on tenant subdomain for callback URL |
| 4 | `POST /refresh` | Poll until `CONNECTED` + `chargesEnabled` |
| 5 | Enable `STRIPE_PAYMENTS` feature (super admin) | Portal pay unlocked |

**`OrgStripeConnectStatusResponse` fields for UI:**

- `onboardingStatus`: `NOT_CONNECTED` | `PENDING` | `CONNECTED` | `RESTRICTED`
- `chargesEnabled`, `payoutsEnabled`, `detailsSubmitted`
- `pastDueRequirements`, `currentlyDueRequirements` — show as checklist if restricted

Gate client “Pay” button: org Connect **CONNECTED** + `chargesEnabled` + feature `STRIPE_PAYMENTS`.

---

## 6. Client portal — pay invoice (integrated)

Slice: `src/store/api/portalApi.ts`

| Action | Method | Path |
|---|---|---|
| Invoice stats | `GET` | `/api/v1/portal/invoices/stats` |
| Invoice list | `GET` | `/api/v1/portal/invoices` |
| Pay | `POST` | `/api/v1/portal/invoices/{invoiceId}/pay` |
| Receipt PDF | `GET` | `/api/v1/portal/invoices/{invoiceId}/receipt` |

### 6.1 Pay flow (frontend)

1. Client opens invoices; filter `paymentStatus=unpaid` if needed (`unpaid`, `paid`, `partial`, `denied`, `cancelled`).
2. Call `payPortalInvoice(invoiceId)`.
3. Response maps to:

```typescript
{ stripeSessionId: response.sessionId, checkoutUrl: response.checkoutUrl }
```

4. **`window.location.href = checkoutUrl`** (Stripe Hosted Checkout).
5. After payment, Stripe redirects to your success/cancel URL; invoice status updates via webhook (may take a few seconds).
6. Refetch invoices; expect `billingStatus: "paid"`, `outstandingAmount: 0`.

### 6.2 Portal invoice row (`PortalInvoiceResponse`)

Show: `serviceName`, `sessionDate`, `totalAmount`, `outstandingAmount`, `billingStatus`, `paymentStatus`.

Disable Pay when `outstandingAmount <= 0` or status is `paid` / `cancelled`.

---

## 7. End-to-end test checklist

Use org with subdomain (e.g. `northstar`), billing module + Stripe payments enabled.

### A. Invoice policy (admin)

1. Login as org admin (`BILLING_MANAGE`).
2. `GET /invoice-policies/options/client-types` and `…/appointment-statuses` — populate form.
3. `POST /invoice-policies` — e.g. FIXED $150 for client type + `completed` status.
4. Confirm `GET /invoice-policies` lists the rule with `enabled: true`.

### B. Staff billing

1. Complete a session (status `completed`) for a client matching the policy.
2. `POST /billing/sessions/{sessionId}/billing` with `{ "units": 1 }`.
3. Verify response: `invoicePolicyId` set, `totalAmount` / `amountDue` reflect policy rate, `billingStatus` typically `pending` or `billed`.
4. Staff: `PATCH /billing/billing/{id}/status` → `billed` if needed.
5. Optional: `POST …/record-payment` for manual cash/card (non-Stripe).

### C. Client portal Stripe pay

1. Login as client with portal access.
2. `GET /portal/invoices` — locate unpaid invoice.
3. `POST /portal/invoices/{id}/pay` → redirect to Stripe Checkout.
4. Pay with test card `4242 4242 4242 4242`.
5. Return to portal; refresh — invoice **paid**, `outstandingAmount: 0`.

### D. Stripe Connect (one-time per org)

1. Admin: Connect Stripe (`/admin/stripe-connect/oauth/start`).
2. Complete onboarding; `GET /status` → `CONNECTED`.
3. Ensure super admin enabled `STRIPE_PAYMENTS` for org.

### E. Regression on existing staff billings page

- Statistics cards load (`/billing/statistics`).
- History table + filters (`/billing/history`).
- Record payment modal still invalidates `BillingInvoices` + `BillingStatistics` tags.

---

## 8. Error handling & UX

| HTTP | Typical cause | UI |
|---|---|---|
| **403** | Missing plan feature or permission | Upgrade / contact admin message |
| **404** | Invoice or policy not found | Toast + redirect |
| **400** | Validation (duplicate policy, bad enum) | Show field errors from message |
| **409** / business rules | Pay already paid invoice | Disable Pay button proactively |

**Stripe pay:** After redirect back from Checkout, poll `GET /portal/invoices` for ~30s or show “Payment processing…” — webhook is async.

**Policy conflicts:** Backend picks highest `priority` matching rule; UI only needs to display `policyName` / `invoicePolicyId` on created invoices.

---

## 9. Frontend file checklist

| Task | Suggested location |
|---|---|
| Invoice policy RTK slice | `src/store/api/admin/invoicePolicy.api.ts` |
| Policy admin page | `src/pages/admin/settings/invoice-policies` (or under billings settings) |
| Tag type | `baseApi.ts` → `InvoicePolicies` |
| Permission gate | `can("manageInvoicePolicies")` from `staffPermissions.ts` |
| Stripe Connect settings | Admin integrations (see existing admin guide) |
| Portal pay | Already in `portalApi.ts` — wire Pay button + redirect |

---

## 10. Reference

- Backend controllers: `BillingController`, `InvoicePolicyController`, `OrgStripeConnectController`, `ClientPortalController`
- Existing frontend billing: `src/store/api/admin/billing.api.ts`, `src/pages/staff/billings`
- Portal pay: `src/store/api/portalApi.ts` → `payPortalInvoice`
- Supervisor access matrix: `trappy-flow-frontend/docs/supervisor-api.md` (§ Invoice policies, Stripe Connect)

For interactive exploration, use Swagger on a running tenant API after staff login.
