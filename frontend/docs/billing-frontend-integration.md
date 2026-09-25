# Billing — Frontend Integration Guide

Base URL: `/api/v1`

All staff/admin endpoints require:

- Header: `Authorization: Bearer <access_token>`
- Tenant context resolved from the authenticated user (organisation schema)

Most billing endpoints also require the **Billing module** plan feature. If disabled, the API returns **403**:

```json
{
  "message": "Billing is not included in your plan. Please upgrade to access billing and invoicing."
}
```

---

## Permissions (staff app)

| Permission | Typical use |
|------------|-------------|
| `BILLING_VIEW` | Read lists, stats, previews |
| `BILLING_CREATE` | Create session billing, services (admin) |
| `BILLING_EDIT` | Record payments, discounts, status changes |
| `BILLING_DELETE` | Delete services (admin) |
| `BILLING_EXPORT` | Download invoice PDF |
| `BILLING_MANAGE` | Invoice policies, Stripe Connect, subscription pay |

Roles: `ADMIN`, `BILLING_SPECIALIST`, `SUPERVISOR`, `THERAPIST` (scoped to their clients/sessions where noted).

---

## 1. Invoice policies

Base path: `/api/v1/billing/invoice-policies`

Dynamic pricing rules by **client type** + **session status** (+ optional service).

### 1.1 List policies

```
GET /api/v1/billing/invoice-policies
```

**Response `200`** — array of:

```json
[
  {
    "id": 7,
    "clientTypeKey": "refugee",
    "clientTypeLabel": "Refugee",
    "appointmentStatusKey": "completed",
    "appointmentStatusLabel": "Completed",
    "enabled": true,
    "priceType": "FIXED",
    "invoicePrice": 50.00,
    "policyName": "Refugee completed fixed",
    "serviceId": 5,
    "effectiveFrom": "2026-01-01",
    "effectiveTo": null,
    "priority": 20,
    "createdAt": "2026-03-01T10:00:00Z",
    "updatedAt": "2026-03-01T10:00:00Z"
  }
]
```

`priceType`: `FIXED` | `PERCENTAGE` (enum name in JSON).

> **Note:** Field names say `appointmentStatus*` but values come from the **`session_status`** system-options catalog (`completed`, `no_show`, `cancelled`, `rescheduled`, etc.). Legacy policies may still store `show-up`; backend matching accepts both.

---

### 1.2 Get policy

```
GET /api/v1/billing/invoice-policies/{id}
```

**Response `200`:** same object shape as list item.

---

### 1.3 Create policy

```
POST /api/v1/billing/invoice-policies
```

**Request body:**

```json
{
  "clientTypeKey": "refugee",
  "clientTypeLabel": "Refugee",
  "appointmentStatusKey": "completed",
  "appointmentStatusLabel": "Completed",
  "enabled": true,
  "priceType": "FIXED",
  "invoicePrice": 50.00,
  "policyName": "Refugee completed fixed",
  "serviceId": 5,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null,
  "priority": 20
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `clientTypeKey` / `clientTypeLabel` | Yes | Must exist in `client_type` system options |
| `appointmentStatusKey` / `appointmentStatusLabel` | Yes | Must exist in `session_status` system options |
| `enabled` | Yes | |
| `priceType` | Yes | `FIXED` or `PERCENTAGE` |
| `invoicePrice` | Yes | ≥ 0; max 100 when `PERCENTAGE` |
| `policyName` | No | Display name |
| `serviceId` | No | `null` = all services |
| `effectiveFrom` / `effectiveTo` | No | ISO date `yyyy-MM-dd` |
| `priority` | No | Higher wins when multiple rules match |

**Response `201`:** `InvoicePolicyResponse` (same as GET).

**Errors:** `400` duplicate scope, invalid option keys, validation.

---

### 1.4 Update policy

```
PUT /api/v1/billing/invoice-policies/{id}
```

**Request body:** same as create.

**Response `200`:** `InvoicePolicyResponse`.

---

### 1.5 Activate / deactivate

```
PATCH /api/v1/billing/invoice-policies/{id}/activate
PATCH /api/v1/billing/invoice-policies/{id}/deactivate
```

No body.

**Response `200`:** `InvoicePolicyResponse`.

---

### 1.6 Delete policy

```
DELETE /api/v1/billing/invoice-policies/{id}
```

**Response `204`** — no body.

---

### 1.7 Dropdown options

#### Client types

```
GET /api/v1/billing/invoice-policies/options/client-types
```

**Response `200`:**

```json
[
  {
    "id": 101,
    "categoryId": 10,
    "categoryKey": "client_type",
    "categoryName": "Client Type",
    "optionKey": "refugee",
    "optionLabel": "Refugee",
    "sortOrder": 1,
    "isDefault": false,
    "isSystem": true,
    "isActive": true,
    "price": 0.00,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-01T00:00:00Z"
  }
]
```

Use `optionKey` + `optionLabel` when saving a policy.

#### Session statuses (labeled “appointment status” in UI)

```
GET /api/v1/billing/invoice-policies/options/appointment-statuses
```

Returns **`session_status`** catalog options (fallback when no separate `appointment_status` category exists).

Typical keys: `scheduled`, `completed`, `confirmed`, `cancelled`, `no_show`, `rescheduled`.

**Billable statuses** (auto-billing on session status change): `completed`, `cancelled`, `no-show`/`no_show`, `rescheduling`/`rescheduled`.

---

## 2. Billing services (rate catalog)

Base path: `/api/v1/billing/services`

### 2.1 List services

```
GET /api/v1/billing/services?activeOnly=true&therapistVisible=true&clientPortalVisible=false
```

All query params optional.

**Response `200`:**

```json
[
  {
    "id": 5,
    "serviceCode": "PSY-60",
    "serviceName": "Psychotherapy Session - 60 minutes",
    "description": "Standard 60-minute session",
    "durationInMinutes": 60,
    "baseRate": 150.00,
    "isActive": true,
    "therapistVisible": true,
    "clientPortalVisible": false,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-01T00:00:00Z"
  }
]
```

---

### 2.2 Get service

```
GET /api/v1/billing/services/{id}
```

**Response `200`:** single `ServiceResponse`.

---

### 2.3 Create service (ADMIN + BILLING_CREATE)

```
POST /api/v1/billing/services
```

**Request:**

```json
{
  "serviceCode": "PSY-60",
  "serviceName": "Psychotherapy Session - 60 minutes",
  "description": "Standard 60-minute psychotherapy session",
  "durationInMinutes": 60,
  "baseRate": 150.00,
  "isActive": true,
  "therapistVisible": true,
  "clientPortalVisible": false
}
```

**Response `201`:** `ServiceResponse`.

---

### 2.4 Update service (ADMIN + BILLING_EDIT)

```
PUT /api/v1/billing/services/{id}
```

**Request:** all fields optional (partial update).

**Response `200`:** `ServiceResponse`.

---

### 2.5 Delete service (ADMIN + BILLING_DELETE)

```
DELETE /api/v1/billing/services/{id}
```

**Response `204`**. **400** if service is referenced by sessions/billing.

---

### 2.6 Bulk therapist visibility (ADMIN + BILLING_EDIT)

```
POST /api/v1/billing/services/visibility/therapists/show-all
POST /api/v1/billing/services/visibility/therapists/hide-all
```

**Response `204`**.

---

## 3. Session billing records

### 3.1 Create billing for a session

```
POST /api/v1/billing/sessions/{sessionId}/billing
```

**Request** (`sessionId` in URL overrides body):

```json
{
  "sessionId": 456,
  "serviceId": 5,
  "serviceCode": "PSY-60",
  "unitRate": 150.00,
  "units": 1,
  "insuranceCovered": false,
  "billingDate": "2026-03-15T00:00:00Z",
  "copayAmount": null,
  "discountType": null,
  "discountValue": null,
  "discountAmount": null
}
```

Invoice policy rate is applied automatically when a matching policy exists.

**Response `201`:** `SessionBillingResponse` (see §3.3).

> Billing is also auto-created when a session status changes to `completed`, `cancelled`, `no-show`, or `rescheduling`.

---

### 3.2 Get billing for a session

```
GET /api/v1/billing/sessions/{sessionId}/billing
```

**Response `200`:** `SessionBillingResponse`.

---

### 3.3 SessionBillingResponse shape

Used across create, update, list, and payment endpoints:

```json
{
  "id": 123,
  "sessionId": 456,
  "serviceId": 5,
  "serviceCode": "PSY-60",
  "serviceName": "Psychotherapy Session - 60 minutes",
  "unitRate": 150.00,
  "units": 1,
  "totalAmount": 150.00,
  "insuranceCovered": false,
  "billingStatus": "pending",
  "billingDate": "2026-03-15T00:00:00Z",
  "copayAmount": null,
  "discountType": "percentage",
  "discountValue": 15.00,
  "discountAmount": 22.50,
  "amountDue": 127.50,
  "remainingDue": 127.50,
  "creditAmount": 0.00,
  "clientPaidAmount": 0.00,
  "insurancePaidAmount": 0.00,
  "invoicePolicyId": 7,
  "stripeCheckoutSessionId": null,
  "stripePaymentIntentId": null,
  "createdAt": "2026-03-15T10:00:00Z",
  "updatedAt": "2026-03-15T10:00:00Z"
}
```

**billingStatus values:** `pending` | `billed` | `paid` | `denied` | `follow_up` | `cancelled`

**discountType values:** `percentage` | `fixed` | `none`

---

### 3.4 List billing records (paginated)

```
GET /api/v1/billing/billing
```

**Query parameters (all optional except pagination defaults):**

| Param | Example | Description |
|-------|---------|-------------|
| `page` | `0` | 0-based page |
| `size` | `25` | Page size (max 200) |
| `sort` | `createdAt` | Sort field |
| `direction` | `desc` | `asc` or `desc` |
| `clientId` | `1` | |
| `clientSearch` | `sarah` | Partial name match |
| `therapistId` | `2` | |
| `status` | `pending` | Unified filter: `pending`, `billed`, `paid`, `denied`, `refunded`, `follow_up` |
| `serviceCode` | `PSY-60` | |
| `clientType` | `Refugee` | |
| `sessionType` | `online` | Session mode |
| `paymentMethod` | `credit_card` | |
| `startDate` / `endDate` | `2026-01-01` | Billing date range |
| `minAmount` / `maxAmount` | `100.00` | |

**Response `200`** — Spring `Page` JSON:

```json
{
  "content": [ { /* SessionBillingResponse */ } ],
  "totalElements": 42,
  "totalPages": 2,
  "size": 25,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

---

### 3.5 Change billing status

```
PATCH /api/v1/billing/billing/{id}/status
```

**Request:**

```json
{
  "billingStatus": "billed",
  "notes": "Invoice sent to client"
}
```

**Response `200`:** `SessionBillingResponse`.

---

### 3.6 Apply discount

```
PATCH /api/v1/billing/billing/{id}/discount
```

**Request (one of):**

```json
{ "discountType": "percentage", "discountValue": 15.00 }
```

```json
{ "discountType": "fixed", "discountAmount": 50.00 }
```

```json
{ "discountType": "none" }
```

**Response `200`:** `SessionBillingResponse`.

---

### 3.7 Update payment status (Stripe webhook / manual sync)

```
PATCH /api/v1/billing/billing/{id}/payment-status?paymentStatus=paid&stripePaymentIntentId=pi_xxx
```

Query params:

- `paymentStatus` (required)
- `stripePaymentIntentId` (optional)

**Response `200`:** `SessionBillingResponse`.

---

## 4. Payments

### 4.1 Payment guidance (before recording)

```
GET /api/v1/billing/billing/{id}/payment-guidance
```

**Response `200`:**

```json
{
  "billingId": 123,
  "amountAfterDiscount": 127.50,
  "expectedClientPortion": 127.50,
  "expectedInsurancePortion": 0.00,
  "clientAlreadyPaid": 0.00,
  "insuranceAlreadyPaid": 0.00,
  "clientRemaining": 127.50,
  "insuranceRemaining": 0.00,
  "totalAlreadyPaid": 0.00,
  "totalRemainingDue": 127.50,
  "overpayDelta": 0.00,
  "insuranceCovered": false
}
```

---

### 4.2 Record payment

```
POST /api/v1/billing/billing/{id}/record-payment
```

**Request:**

```json
{
  "clientId": 1,
  "paymentAmount": 127.50,
  "paymentMethod": "credit_card",
  "referenceNumber": "CHK-12345",
  "notes": "Paid in full",
  "paymentDate": "2026-03-20T14:30:00Z",
  "paymentSide": "client",
  "allowZeroBillOverpayment": false,
  "overrideReason": null
}
```

**paymentMethod** (JSON string value, not enum name):

`cash` | `check` | `credit_card` | `debit_card` | `insurance` | `bank_transfer` | `online_payment` | `credit_balance`

**paymentSide:** `client` | `insurance` (optional hint)

**Response `200`:** `SessionBillingResponse`.

---

### 4.3 Record split payment (client + insurance)

```
POST /api/v1/billing/billing/{id}/record-split-payment
```

**Request:**

```json
{
  "clientLeg": {
    "amount": 40.00,
    "paymentMethod": "credit_card",
    "paymentDate": "2026-03-20T14:30:00Z",
    "referenceNumber": "TXN-001"
  },
  "insuranceLeg": {
    "amount": 87.50,
    "paymentMethod": "insurance",
    "referenceNumber": "EOB-123"
  },
  "notes": "Split payment",
  "allowZeroBillOverpayment": false,
  "overrideReason": null
}
```

**Response `200`:** `SessionBillingResponse`.

---

### 4.4 Edit payment

```
PATCH /api/v1/billing/billing/{id}/payments/{paymentId}
```

**Request** (all optional):

```json
{
  "amount": 100.00,
  "paymentMethod": "check",
  "paymentDate": "2026-03-21T10:00:00Z",
  "reference": "CHK-999",
  "notes": "Corrected amount"
}
```

**Response `200`:** `SessionBillingResponse`.

---

### 4.5 Refund

```
POST /api/v1/billing/billing/{id}/refund-payment
```

**Request:**

```json
{
  "refundAmount": 50.00,
  "referenceNumber": "REF-12345",
  "paymentMethod": "bank_transfer",
  "notes": "Partial refund"
}
```

**Response `200`:** `SessionBillingResponse`.

---

### 4.6 Refund history

```
GET /api/v1/billing/billing/{id}/refunds
```

**Response `200`:**

```json
{
  "billingId": 123,
  "refundCount": 1,
  "totalRefunded": 50.00,
  "refunds": [
    {
      "id": 10,
      "sessionBillingId": 123,
      "amount": 50.00,
      "paymentMethod": "bank_transfer",
      "paymentSource": "manual",
      "status": "succeeded",
      "paymentDate": "2026-03-22T10:00:00Z",
      "reference": "REF-12345",
      "notes": "Partial refund",
      "createdAt": "2026-03-22T10:00:00Z",
      "updatedAt": "2026-03-22T10:00:00Z",
      "transactions": []
    }
  ]
}
```

---

### 4.7 Payment transactions

```
GET /api/v1/billing/billing/{id}/transactions
```

**Response `200`:** array of:

```json
[
  {
    "id": 987,
    "provider": "stripe",
    "transactionType": "charge",
    "amount": 127.50,
    "providerIntentId": "pi_1234567890",
    "providerChargeId": "ch_1234567890",
    "providerCustomerId": "cus_1234567890",
    "providerPaymentMethodId": "pm_1234567890",
    "status": "succeeded",
    "failureReason": null,
    "voided": false,
    "voidReason": null,
    "voidedAt": null,
    "createdAt": "2026-03-20T14:30:00Z"
  }
]
```

---

### 4.8 Void transaction

```
POST /api/v1/billing/billing/{id}/transactions/{transactionId}/void
```

**Request:**

```json
{
  "voidReason": "Duplicate manual entry"
}
```

**Response `200`:** `SessionBillingResponse`.

---

## 5. Invoices (staff)

### 5.1 Send invoice email

```
POST /api/v1/billing/billing/{id}/send-invoice-email
```

**Response `200`** — empty body. Sets status to `billed` if currently `pending`.

---

### 5.2 Preview invoice HTML

```
GET /api/v1/billing/billing/{id}/invoice-preview
```

**Response `200`:**

```json
{
  "html": "<html>...</html>"
}
```

---

### 5.3 Download invoice HTML

```
GET /api/v1/billing/billing/{id}/invoice-download
```

**Response `200`:** `text/html` attachment.

---

### 5.4 Download invoice PDF

```
GET /api/v1/billing/billing/{id}/invoice
```

**Response `200`:** `application/pdf` bytes (`Content-Disposition: attachment; filename=invoice-{id}.pdf`).

Requires `BILLING_EXPORT`.

---

## 6. Statistics & history

### 6.1 Dashboard statistics

```
GET /api/v1/billing/statistics
```

**Response `200`:**

```json
{
  "outstandingBalance": 5000.00,
  "creditBalance": 138.00,
  "totalCollected": 25000.00,
  "activeClients": 45,
  "totalBillingRecords": 120,
  "pendingRecords": 15,
  "paidRecords": 85,
  "deniedRecords": 5,
  "followUpRecords": 10
}
```

---

### 6.2 Client billing stats

```
GET /api/v1/billing/clients/{clientId}/stats
```

**Response `200`:**

```json
{
  "clientId": 123,
  "totalInvoices": 24,
  "pendingInvoices": 2,
  "billedInvoices": 5,
  "paidInvoices": 15,
  "deniedInvoices": 1,
  "followUpInvoices": 1,
  "cancelledInvoices": 0,
  "totalBilledAmount": 3400.00,
  "totalPaidAmount": 2800.00,
  "dueAmount": 600.00,
  "creditAmount": 0.00
}
```

---

### 6.3 Billing history

```
GET /api/v1/billing/history?clientId=1&page=1&limit=10
```

**Query params:** `clientId`, `therapistId`, `paymentStatus`, `billingStatus`, `startDate`, `endDate`, `page` (1-based), `limit`.

**Response `200`:**

```json
{
  "items": [
    {
      "billingId": 123,
      "clientId": 456,
      "clientName": "John Doe",
      "sessionId": 789,
      "sessionDate": "2026-01-15T10:00:00Z",
      "serviceCode": "PSY-60",
      "serviceName": "Psychotherapy Session - 60 minutes",
      "totalAmount": 150.00,
      "discountAmount": 22.50,
      "amountDue": 127.50,
      "paymentStatus": "paid",
      "billingStatus": "billed",
      "paymentMethod": "credit_card",
      "paymentAmount": 127.50,
      "paymentDate": "2026-01-20T14:30:00Z",
      "billingDate": "2026-01-15T00:00:00Z",
      "createdAt": "2026-01-15T08:00:00Z"
    }
  ],
  "totalCount": 42,
  "page": 1,
  "pageSize": 10,
  "totalPages": 5
}
```

---

## 7. Tenant subscription billing (org pays TherapyFlow)

These are **SaaS subscription invoices**, not client session invoices.

### 7.1 Current subscription

```
GET /api/v1/billing/subscription/me
```

**Response `200`:**

```json
{
  "organisationId": 1,
  "subscriptionId": 10,
  "planCode": "professional",
  "planName": "Professional",
  "planStatus": "active",
  "subscriptionStatus": "active",
  "billingCycle": "monthly",
  "priceAtTime": 99.00,
  "startAt": "2026-01-01T00:00:00Z",
  "endAt": null,
  "trialEndAt": null,
  "trialing": false,
  "usagePeriod": "2026-03",
  "features": [
    {
      "featureCode": "BILLING_MODULE",
      "featureName": "Billing Module",
      "description": "...",
      "enabled": true,
      "usageLimit": null,
      "currentUsage": null,
      "coreFeature": false,
      "valueType": "boolean"
    }
  ],
  "auditExportEnabled": true,
  "auditExportLimit": 100,
  "auditExportUsage": 12
}
```

---

### 7.2 List subscription invoices

```
GET /api/v1/billing/subscription/invoices?status=all&page=0&size=25
```

`status`: `all` | `pending` | `paid` | etc.

**Response `200`** — Spring `Page`:

```json
{
  "content": [
    {
      "invoiceId": 55,
      "subscriptionId": 10,
      "status": "pending",
      "amount": 99.00,
      "outstandingBalance": 99.00,
      "totalPaid": 0.00,
      "refundedAmount": 0.00,
      "dueDate": "2026-04-01",
      "billingPeriodStart": "2026-03-01T00:00:00Z",
      "billingPeriodEnd": "2026-03-31T23:59:59Z",
      "paidAt": null,
      "providerInvoiceId": "in_xxx",
      "createdAt": "2026-03-01T00:00:00Z"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "size": 25,
  "number": 0
}
```

---

### 7.3 Pay subscription invoice (Stripe hosted URL)

```
POST /api/v1/billing/subscription/invoices/{invoiceId}/pay
```

**Response `200`:**

```json
{
  "invoiceId": 55,
  "providerInvoiceId": "in_xxx",
  "providerStatus": "open",
  "paymentUrl": "https://invoice.stripe.com/i/..."
}
```

Redirect user to `paymentUrl`.

---

## 8. Stripe Connect (admin setup)

Base path: `/api/v1/admin/stripe-connect`

Requires `BILLING_MANAGE`.

### 8.1 Status

```
GET /api/v1/admin/stripe-connect/status
```

**Response `200`:**

```json
{
  "organisationId": 1,
  "connectAccountId": "acct_xxx",
  "onboardingStatus": "CONNECTED",
  "chargesEnabled": true,
  "payoutsEnabled": true,
  "detailsSubmitted": true,
  "country": "CA",
  "defaultCurrency": "cad",
  "lastSyncedAt": "2026-03-01T12:00:00Z",
  "disabledReason": null,
  "pastDueRequirements": [],
  "currentlyDueRequirements": []
}
```

`onboardingStatus`: `NOT_CONNECTED` | `PENDING` | `CONNECTED` | `RESTRICTED`

---

### 8.2 Start OAuth

```
POST /api/v1/admin/stripe-connect/oauth/start
```

**Response `200`:**

```json
{
  "authorizeUrl": "https://connect.stripe.com/oauth/authorize?...",
  "stateExpiresAt": "2026-03-01T13:00:00Z"
}
```

Redirect admin to `authorizeUrl`.

---

### 8.3 OAuth callback (browser redirect)

```
GET /api/v1/admin/stripe-connect/oauth/callback?state=...&code=...
```

**Response `200`:** `OrgStripeConnectStatusResponse`.

---

### 8.4 Refresh / disconnect

```
POST /api/v1/admin/stripe-connect/refresh
POST /api/v1/admin/stripe-connect/disconnect
```

**Response `200`:** `OrgStripeConnectStatusResponse`.

---

### 8.5 Tenant Stripe config

```
GET /api/v1/admin/stripe-connect/config
PUT /api/v1/admin/stripe-connect/config
```

**PUT request:**

```json
{
  "publishableKey": "pk_live_...",
  "secretKey": "sk_live_...",
  "webhookEndpointUrl": "https://api.example.com/api/v1/stripe/webhook/tenant/my-org",
  "webhookSecret": "whsec_..."
}
```

**GET response:**

```json
{
  "organisationId": 1,
  "publishableKey": "pk_live_...",
  "secretKeyConfigured": true,
  "webhookEndpointUrl": "https://...",
  "webhookSecretConfigured": true,
  "lastUpdatedAt": "2026-03-01T00:00:00Z"
}
```

Secret values are never returned on GET.

---

## 9. Stripe client invoice payment

### 9.1 Staff/API initiate checkout (also used internally)

```
POST /api/v1/stripe/invoices/{invoiceId}/pay
```

`invoiceId` = **session billing record ID** (`SessionBilling.id`).

Optional cookie: `portalSessionToken` (for portal flows).

**Response `200`:**

```json
{
  "sessionId": "cs_test_xxx",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/..."
}
```

Redirect to `checkoutUrl`.

---

## 10. Client portal invoices

Base path: `/api/v1/portal`

Auth: client JWT with `CLIENT_PORTAL_ACCESS`.

### 10.1 Invoice stats

```
GET /api/v1/portal/invoices/stats
```

**Response `200`:**

```json
{
  "totalInvoices": 12,
  "totalBilled": 1800.00,
  "totalPaid": 1500.00
}
```

---

### 10.2 List invoices

```
GET /api/v1/portal/invoices?page=1&pageSize=20&paymentStatus=pending&insuranceCovered=false&startDate=2026-01-01&endDate=2026-12-31&search=PSY
```

**Response `200`:**

```json
{
  "items": [
    {
      "id": 123,
      "sessionId": 456,
      "serviceCode": "PSY-60",
      "serviceName": "Psychotherapy Session - 60 minutes",
      "sessionType": "individual",
      "sessionMode": "in_person",
      "sessionDate": "2026-03-10T15:00:00Z",
      "units": 1,
      "ratePerUnit": 150.00,
      "totalAmount": 150.00,
      "insuranceCovered": false,
      "copayAmount": null,
      "billingDate": "2026-03-10",
      "paymentStatus": "pending",
      "billingStatus": "billed",
      "paymentAmount": 0.00,
      "outstandingAmount": 150.00,
      "paymentDate": null,
      "paymentMethod": null,
      "discountType": null,
      "discountValue": null,
      "discountAmount": null,
      "createdAt": "2026-03-10T16:00:00Z"
    }
  ],
  "totalCount": 12,
  "page": 1,
  "pageSize": 20,
  "totalPages": 1
}
```

---

### 10.3 Pay invoice (Stripe checkout)

```
POST /api/v1/portal/invoices/{invoiceId}/pay
```

**Response `200`:**

```json
{
  "sessionId": "cs_test_xxx",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/..."
}
```

---

### 10.4 Download receipt PDF

```
GET /api/v1/portal/invoices/{invoiceId}/receipt
```

**Response `200`:** PDF bytes (paid or partially paid only).

---

## 11. Recommended frontend flows

### Invoice policy admin screen

1. `GET /billing/invoice-policies/options/client-types`
2. `GET /billing/invoice-policies/options/appointment-statuses` → populate status dropdown from `session_status`
3. `GET /billing/services` → optional service scope
4. `GET /billing/invoice-policies` → list
5. `POST` / `PUT` / `PATCH` / `DELETE` as needed

### Billing workspace

1. `GET /billing/statistics` → dashboard cards
2. `GET /billing/billing?...` → main table (`content` array)
3. Row actions: payment guidance → record payment → transactions/refunds
4. Invoice: preview → send email → PDF download

### Client portal billing tab

1. `GET /portal/invoices/stats`
2. `GET /portal/invoices`
3. Pay: `POST /portal/invoices/{id}/pay` → redirect to `checkoutUrl`
4. Receipt: `GET /portal/invoices/{id}/receipt`

### Stripe Connect onboarding (admin settings)

1. `GET /admin/stripe-connect/status`
2. If not connected: `POST /admin/stripe-connect/oauth/start` → redirect
3. After callback: `GET /admin/stripe-connect/status` again
4. Optional: `PUT /admin/stripe-connect/config` for tenant-specific keys

---

## 12. Common error responses

```json
{
  "timestamp": "2026-03-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invoice policy for this client type, appointment status, and service already exists",
  "path": "/api/v1/billing/invoice-policies"
}
```

| Code | Meaning |
|------|---------|
| `400` | Validation, duplicate policy, business rule |
| `401` | Missing/invalid token |
| `403` | Missing permission or billing module disabled |
| `404` | Resource not found |
| `204` | Success, no body (delete) |

---

## 13. Webhooks (backend-only — do not call from frontend)

```
POST /api/v1/stripe/webhook/platform
POST /api/v1/stripe/webhook/connect
POST /api/v1/stripe/webhook/tenant/{tenantKey}
```

Header: `Stripe-Signature`. Used by Stripe servers only.
