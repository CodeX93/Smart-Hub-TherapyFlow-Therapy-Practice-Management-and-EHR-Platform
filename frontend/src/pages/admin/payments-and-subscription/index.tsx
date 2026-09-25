import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { cn } from "@/lib/utils";
import StripeConnect from "@/components/settings/StripeConnect";
import SubscriptionBilling from "@/components/settings/SubscriptionBilling";

const tabs = [
  { id: "payment-integration", label: "Payment Integration" },
  { id: "subscription", label: "Subscription" },
] as const;

type TabId = (typeof tabs)[number]["id"];

/** Legacy query values from Settings tabs — map to the new page tabs. */
function resolveTab(raw: string | null): TabId {
  if (raw === "subscription" || raw === "subscription-billing") {
    return "subscription";
  }
  // Default and legacy: stripe-connect / payment-integration
  return "payment-integration";
}

const PaymentsAndSubscription = () => {
  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState<TabId>(() =>
    resolveTab(searchParams.get("tab")),
  );

  return (
    <div className="flex h-[calc(100vh-10.5rem)] min-h-0 flex-col overflow-hidden">
      <div className="shrink-0 pb-4">
        <div className="overflow-x-auto pb-1 custom-scrollbar">
          <div className="inline-flex min-w-full w-max items-center rounded-full border border-(--neutral-100) bg-(--neutral-100) p-1 shadow-xs">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={cn(
                  "shrink-0 whitespace-nowrap rounded-full px-4 py-2.5 text-sm font-medium transition-all duration-300 cursor-pointer sm:px-6 lg:px-8",
                  activeTab === tab.id
                    ? "bg-white text-(--text-primary-dark) shadow-xs"
                    : "text-(--text-neutral-600) hover:text-(--text-primary-dark)",
                )}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      </div>
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        {activeTab === "subscription" ? (
          <SubscriptionBilling />
        ) : (
          <StripeConnect />
        )}
      </div>
    </div>
  );
};

export default PaymentsAndSubscription;
