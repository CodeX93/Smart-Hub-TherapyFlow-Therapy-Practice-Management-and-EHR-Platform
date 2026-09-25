
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { getAuthSession } from "@/utils/authStorage";
import { getRedirectPathByRole } from "@/utils/redirectPathByRole";
import { getStaffLandingPath } from "@/utils/staffPermissions";
import { useLazyGetTenantSubscriptionQuery } from "@/store/api/admin/billing.api";
import { isSubscriptionCheckoutComplete } from "@/utils/subscriptionSelfServe";

type PageState = "loading" | "success" | "error";

const POLL_INTERVAL_MS = 2500;
const MAX_POLL_ATTEMPTS = 12;

const SubscriptionSuccessPage = () => {
  const [hasSession] = useState(() => Boolean(getAuthSession()?.accessToken));
  const [pageState, setPageState] = useState<PageState>(hasSession ? "loading" : "error");
  const [message, setMessage] = useState(hasSession ? "Verifying your subscription…" : "Could not verify your subscription. Please sign in and try again.");
  const completedRef = useRef(false);
  const attemptsRef = useRef(0);
  const [triggerGetSubscription] = useLazyGetTenantSubscriptionQuery();

  useEffect(() => {
    if (!hasSession) return;

    let cancelled = false;

    const verifySubscription = async () => {
      if (completedRef.current) return;

      try {
        const subscription = await triggerGetSubscription().unwrap();
        if (cancelled || completedRef.current) return;

        if (isSubscriptionCheckoutComplete(subscription)) {
          completedRef.current = true;
          setPageState("success");
          setMessage("Your subscription is active. Thank you for your payment.");
          return;
        }

        attemptsRef.current += 1;
        if (attemptsRef.current >= MAX_POLL_ATTEMPTS) {
          completedRef.current = true;
          setPageState("error");
          setMessage(
            "Payment is still processing. Check Subscription settings in a few minutes.",
          );
        }
      } catch {
        if (cancelled || completedRef.current) return;
        attemptsRef.current += 1;
        if (attemptsRef.current >= MAX_POLL_ATTEMPTS) {
          completedRef.current = true;
          setPageState("error");
          setMessage(
            "Could not verify your subscription. Please try again from Subscription settings.",
          );
        }
      }
    };

    void verifySubscription();
    const timer = window.setInterval(() => {
      void verifySubscription();
    }, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [hasSession, triggerGetSubscription]);

  const handleContinue = () => {
    const session = getAuthSession();
    const role = session?.role;
    if (!role) {
      window.location.href = window.location.origin;
      return;
    }

    const path =
      role === "staff"
        ? getStaffLandingPath(session.permissions ?? [], session.apiRoles ?? [])
        : getRedirectPathByRole(role);

    window.location.href = `${window.location.origin}${path}`;
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#fafafb] px-4 py-10">
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow-md">
        {pageState === "loading" ? (
          <div className="flex items-center justify-center gap-2 py-6 text-sm text-(--text-neutral-600)">
            <ContentLoader variant="inline" size="md" />
            {message}
          </div>
        ) : (
          <p
            className={
              pageState === "success"
                ? "text-center text-base font-medium text-(--text-primary-dark)"
                : "text-center text-base font-medium text-red-600"
            }
          >
            {message}
          </p>
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

export default SubscriptionSuccessPage;
