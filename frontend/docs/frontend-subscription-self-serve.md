# Frontend: Tenant subscription self-serve (pay / renew)

Handoff for the **staff app** only. Add the UI below; no changes are required to client invoicing, Stripe Connect, or super-admin onboarding.

---

## What this does NOT change

| Area | Impact |
|------|--------|
| **Client session invoices** (`/api/v1/billing/billing`, portal invoices, `POST /stripe/invoices/{id}/pay`) | **No change.** Same APIs, same flows. |
| **Stripe Connect** (`/api/v1/admin/stripe-connect/*`) | **No change.** Still for client payments on the org’s Connect account. |
| **Super-admin onboarding** (`POST /api/v1/super-admin/organisations/onboard`) | **No change.** Super-admin still creates orgs the same way. Stripe provisioning on onboard is optional backend-side; frontend onboarding screens stay as-is. |
| **Invoice policies, services, billing workspace** | **No change.** |

This feature is only **Settings → Billing → Subscription** (org pays TherapyFlow for the SaaS plan).

---

## New / updated routes (staff app)

| Route | Purpose |
|-------|---------|
| `/billing/subscription` | Subscription status + actions |
| `/billing/subscription/success` | Return URL after Stripe Checkout |

---

## APIs to call

Base: `/api/v1/billing`  
Auth: staff JWT  
Permission: `BILLING_READ` for read; `BILLING_MANAGE` for checkout / portal / pay

### 1. Load page — `GET /subscription/me`

Use on `/billing/subscription` mount and on the success page (poll).

Key fields for UI:

| Field | Use |
|-------|-----|
| `planName`, `subscriptionStatus`, `billingCycle`, `priceAtTime`, `trialEndAt`, `currentPeriodEnd` | Display |
| `billingMode` | `provider_managed` \| `manual_or_unconfigured` |
| `nextAction` | Drives which button to show (see table below) |
| `pendingInvoiceId` | Use with pay flow when `nextAction=pay_invoice` |
| `accessRestricted` | Show locked-org banner app-wide (allow billing routes) |
| `canSelfServeRenew` | Hide subscribe/pay CTAs when `false` |

### 2. Subscribe / renew — `POST /subscription/checkout`

When `nextAction === 'subscribe'`:

```ts
const { checkoutUrl } = await api.post('/api/v1/billing/subscription/checkout');
window.location.href = checkoutUrl;
```

No request body.

### 3. Manage payment method — `POST /subscription/portal`

When `providerBillingConfigured === true` (e.g. “Manage billing” link):

```ts
const { portalUrl } = await api.post('/api/v1/billing/subscription/portal');
window.location.href = portalUrl;
```

### 4. Pay open invoice — existing endpoints

When `nextAction === 'pay_invoice'`:

1. `GET /api/v1/billing/subscription/invoices?status=pending` (or use `pendingInvoiceId` from `/me`)
2. `POST /api/v1/billing/subscription/invoices/{invoiceId}/pay`
3. Redirect to `paymentUrl` from response

### 5. Success page — `/billing/subscription/success`

After Stripe Checkout redirect:

1. Poll `GET /subscription/me` every 2–3s (max ~30s)
2. Success when `subscriptionStatus === 'active'` and `accessRestricted === false`
3. Show message + **Continue** → dashboard (`/` or home route)

---

## `nextAction` → button mapping

| `nextAction` | Show | Action |
|--------------|------|--------|
| `none` | Status only | — |
| `subscribe` | **Subscribe** or **Renew** | `POST /subscription/checkout` → redirect |
| `pay_invoice` | **Pay now** | List invoice → `POST .../invoices/{id}/pay` → redirect |
| `update_payment_method` | **Manage billing** | `POST /subscription/portal` → redirect |
| `contact_support` | Info text + support link | No API call (manual-mode orgs) |

---

## Locked org banner

If `GET /subscription/me` returns `accessRestricted: true`:

- Show a persistent banner on all routes **except** `/billing/subscription` and `/billing/subscription/success`
- CTA: follow `nextAction` (`subscribe` or `pay_invoice` when available; otherwise support)

---

## Suggested component sketch

```tsx
function SubscriptionBillingPage() {
  const { data: sub, refetch } = useQuery(['subscription-me'], () =>
    api.get('/api/v1/billing/subscription/me').then(r => r.data)
  );

  if (!sub) return <Loading />;

  return (
    <div>
      <PlanCard plan={sub.planName} status={sub.subscriptionStatus} cycle={sub.billingCycle} price={sub.priceAtTime} />
      {sub.trialing && sub.trialEndAt && <TrialBanner endsAt={sub.trialEndAt} />}
      {sub.accessRestricted && <LockedBanner nextAction={sub.nextAction} />}

      {sub.nextAction === 'subscribe' && sub.canSelfServeRenew && (
        <Button onClick={() => checkout()}>Subscribe</Button>
      )}
      {sub.nextAction === 'pay_invoice' && sub.pendingInvoiceId && (
        <Button onClick={() => payInvoice(sub.pendingInvoiceId)}>Pay now</Button>
      )}
      {sub.nextAction === 'update_payment_method' && (
        <Button onClick={() => openPortal()}>Manage billing</Button>
      )}
      {sub.nextAction === 'contact_support' && (
        <p>Contact support to renew your subscription.</p>
      )}
      {sub.providerBillingConfigured && sub.nextAction === 'none' && (
        <Button variant="link" onClick={() => openPortal()}>Manage billing</Button>
      )}

      <SubscriptionInvoiceList />
    </div>
  );
}
```

---

## Env (already backend defaults)

| Variable | Default |
|----------|---------|
| `APP_FRONTEND_SUBSCRIPTION_SUCCESS_URL` | `https://app.therapyflow.pro/billing/subscription/success` |
| `APP_FRONTEND_SUBSCRIPTION_CANCEL_URL` | `https://app.therapyflow.pro/billing/subscription` |

Local: point these at `http://localhost:3000/billing/subscription/success` and `/billing/subscription`.

---

## Safe rollout

1. **Ship now (read-only):** `/billing/subscription` calling `GET /subscription/me` only — safe before Stripe is configured in prod.
2. **After backend + webhooks in prod:** Enable Subscribe / Pay now / Manage billing buttons using `nextAction`.

---

## Quick reference vs other billing

```
Org SaaS subscription (THIS DOC)     →  GET/POST /api/v1/billing/subscription/*
Client session invoices              →  GET /api/v1/billing/billing, portal /invoices
Stripe Connect (client payments)     →  /api/v1/admin/stripe-connect/*
Super-admin creates org              →  /api/v1/super-admin/organisations/onboard
```
