import type {
  SubscriptionNextAction,
  TenantSubscriptionResponse,
} from "@/types/sessionBilling.type";

export const SUBSCRIPTION_SETTINGS_PATH =
  "/admin/payments-and-subscription?tab=subscription";
export const PAYMENT_INTEGRATION_PATH =
  "/admin/payments-and-subscription?tab=payment-integration";
export const PAYMENTS_AND_SUBSCRIPTION_PATH = "/admin/payments-and-subscription";
export const SUBSCRIPTION_PAGE_PATH = "/billing/subscription";
export const SUBSCRIPTION_SUCCESS_PATH = "/billing/subscription/success";

export function formatSubscriptionDate(value?: string | null): string {
  if (!value) return "—";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function getSubscribeButtonLabel(subscription?: TenantSubscriptionResponse | null): string {
  if (!subscription) return "Subscribe";

  const status = subscription.subscriptionStatus?.toLowerCase() ?? "";
  const isTrialing = subscription.trialing || status === "trialing";

  if (isTrialing) {
    return "Add payment method";
  }

  if (
    status.includes("cancel") ||
    status.includes("expir") ||
    status.includes("past_due") ||
    status.includes("inactive")
  ) {
    return "Renew subscription";
  }

  return subscription.subscriptionId ? "Renew subscription" : "Subscribe";
}

export function getTrialCheckoutHelperText(
  subscription?: TenantSubscriptionResponse | null,
): string | null {
  if (!subscription) return null;
  const status = subscription.subscriptionStatus?.toLowerCase() ?? "";
  const isTrialing = subscription.trialing || status === "trialing";
  if (!isTrialing) return null;
  return "Add your card now. You won't be charged until your trial ends.";
}

export function getSelfServeUnavailableMessage(
  subscription?: TenantSubscriptionResponse | null,
): string | null {
  if (!subscription || subscription.canSelfServeRenew !== false) return null;
  if (subscription.nextAction && subscription.nextAction !== "none") return null;

  if (subscription.billingMode === "manual_or_unconfigured") {
    return "Online subscription billing is not configured for this plan yet. Contact support to add a payment method.";
  }

  return "Self-serve subscription billing is not available for this organisation. Contact support for help.";
}

export function getNextActionButtonLabel(
  nextAction: SubscriptionNextAction | null | undefined,
  subscription?: TenantSubscriptionResponse | null,
): string | null {
  switch (nextAction) {
    case "subscribe":
      return getSubscribeButtonLabel(subscription);
    case "pay_invoice":
      return "Pay now";
    case "update_payment_method":
      return "Manage billing";
    default:
      return null;
  }
}

export function isSubscriptionCheckoutComplete(
  subscription: TenantSubscriptionResponse,
): boolean {
  if (subscription.accessRestricted) return false;
  if (subscription.providerBillingConfigured !== true) return false;

  const status = subscription.subscriptionStatus?.toLowerCase() ?? "";
  const isTrialing = subscription.trialing || status === "trialing";
  return status === "active" || isTrialing;
}
