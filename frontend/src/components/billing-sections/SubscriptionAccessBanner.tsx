import { Link, useLocation } from "react-router-dom";
import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useGetTenantSubscriptionQuery } from "@/store/api/admin/billing.api";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import {
  getNextActionButtonLabel,
  PAYMENTS_AND_SUBSCRIPTION_PATH,
  SUBSCRIPTION_PAGE_PATH,
  SUBSCRIPTION_SETTINGS_PATH,
  SUBSCRIPTION_SUCCESS_PATH,
} from "@/utils/subscriptionSelfServe";

const SubscriptionAccessBanner = () => {
  const location = useLocation();
  const { hasAny } = useStaffAccess();
  const canReadSubscription = hasAny("BILLING_VIEW", "BILLING_EDIT", "BILLING_MANAGE");

  const isSubscriptionRoute =
    location.pathname.startsWith(SUBSCRIPTION_PAGE_PATH) ||
    location.pathname === SUBSCRIPTION_SUCCESS_PATH ||
    location.pathname === "/admin/billing/subscription" ||
    location.pathname === "/staff/billing/subscription" ||
    location.pathname === PAYMENTS_AND_SUBSCRIPTION_PATH ||
    (location.pathname === "/admin/payments-and-subscription" &&
      new URLSearchParams(location.search).get("tab") === "subscription");

  const { data: subscription } = useGetTenantSubscriptionQuery(undefined, {
    skip: !canReadSubscription || isSubscriptionRoute,
  });

  if (!canReadSubscription || isSubscriptionRoute || !subscription?.accessRestricted) {
    return null;
  }

  const nextAction = subscription.nextAction ?? "contact_support";
  const actionLabel = getNextActionButtonLabel(nextAction, subscription);

  return (
    <div className="mb-4 flex flex-col gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />
        <div>
          <p className="text-sm font-semibold text-amber-950">Subscription action required</p>
          <p className="text-sm text-amber-900">
            {nextAction === "contact_support"
              ? "Your organisation access is limited. Contact support to renew your subscription."
              : "Your organisation access is limited until subscription billing is resolved."}
          </p>
        </div>
      </div>
      {nextAction === "contact_support" ? (
        <a
          href="mailto:support@therapyflow.pro"
          className="text-sm font-medium text-amber-950 underline"
        >
          Contact support
        </a>
      ) : actionLabel && subscription.canSelfServeRenew !== false ? (
        <Button asChild className="h-9 shrink-0 rounded-full bg-amber-900 hover:bg-amber-950">
          <Link to={SUBSCRIPTION_SETTINGS_PATH}>{actionLabel}</Link>
        </Button>
      ) : null}
    </div>
  );
};

export default SubscriptionAccessBanner;
