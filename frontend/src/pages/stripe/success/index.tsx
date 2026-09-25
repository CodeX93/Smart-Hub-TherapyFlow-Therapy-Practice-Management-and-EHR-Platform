
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { getAuthSession } from "@/utils/authStorage";
import {
  useLazyGetStripeConnectStatusQuery,
  type OrgStripeConnectStatusResponse,
} from "@/store/api/admin/stripeConnect.api";

type PageState = "loading" | "success" | "error";

const STATUS_VERIFY_ERROR =
  "Could not verify Stripe connection. Please try again from Billing settings.";

function resolveStripeConnectOutcome(status: OrgStripeConnectStatusResponse): {
  state: "success" | "error";
  message: string;
  warning?: string;
} {
  if (status.onboardingStatus === "CONNECTED" && status.chargesEnabled) {
    const hasOpenRequirements =
      (status.currentlyDueRequirements?.length ?? 0) > 0 ||
      (status.pastDueRequirements?.length ?? 0) > 0;

    return {
      state: "success",
      message: "Stripe connected successfully. You can accept client portal payments.",
      warning: hasOpenRequirements
        ? "Additional Stripe verification may be required."
        : undefined,
    };
  }

  if (status.onboardingStatus === "RESTRICTED") {
    return {
      state: "error",
      message: status.disabledReason?.trim() || "Stripe Connect could not be completed.",
    };
  }

  if (status.onboardingStatus === "PENDING" || !status.chargesEnabled) {
    return {
      state: "error",
      message:
        "Stripe setup is incomplete. Finish onboarding in Stripe or try again from Billing settings.",
    };
  }

  if (status.onboardingStatus === "NOT_CONNECTED") {
    return {
      state: "error",
      message: "Stripe was not connected. Please try again.",
    };
  }

  return {
    state: "error",
    message:
      "Stripe setup is incomplete. Finish onboarding in Stripe or try again from Billing settings.",
  };
}

const StripeConnectSuccessPage = () => {
  const [hasSession] = useState(() => Boolean(getAuthSession()?.accessToken));
  const [pageState, setPageState] = useState<PageState>(hasSession ? "loading" : "error");
  const [message, setMessage] = useState(hasSession ? "" : STATUS_VERIFY_ERROR);
  const [warning, setWarning] = useState<string | null>(null);
  const [triggerGetStatus] = useLazyGetStripeConnectStatusQuery();

  useEffect(() => {
    if (!hasSession) return;

    void triggerGetStatus()
      .unwrap()
      .then((status) => {
        const outcome = resolveStripeConnectOutcome(status);
        setPageState(outcome.state);
        setMessage(outcome.message);
        setWarning(outcome.warning ?? null);
      })
      .catch(() => {
        setPageState("error");
        setMessage(STATUS_VERIFY_ERROR);
      });
  }, [hasSession, triggerGetStatus]);

  const handleContinue = () => {
    window.location.href = window.location.origin;
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#fafafb] px-4 py-10">
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow-md">
        {pageState === "loading" ? (
          <div className="flex items-center justify-center gap-2 py-6 text-sm text-(--text-neutral-600)">
            <ContentLoader variant="inline" size="md" />
            Verifying Stripe connection…
          </div>
        ) : (
          <div className="space-y-3 text-center">
            <p
              className={
                pageState === "success"
                  ? "text-base font-medium text-(--text-primary-dark)"
                  : "text-base font-medium text-red-600"
              }
            >
              {message}
            </p>
            {warning ? (
              <p className="text-sm text-(--text-neutral-600)">{warning}</p>
            ) : null}
          </div>
        )}

        <Button
          type="button"
          onClick={handleContinue}
          disabled={pageState === "loading"}
          className="mt-8 h-11 w-full rounded-full bg-(--bg-primary-dark) text-sm font-semibold text-white hover:bg-(--bg-primary-dark)/90"
        >
          Continue
        </Button>
      </div>
    </div>
  );
};

export default StripeConnectSuccessPage;
