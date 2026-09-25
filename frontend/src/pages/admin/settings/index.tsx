import { useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import SettingsTabs from "@/components/settings/SettingsTabs";
import SystemOptions from "@/components/settings/SystemOptions";
import ServicePrices from "@/components/settings/ServicePrices";
import InvoicePolicies from "@/components/settings/InvoicePolicies";
import TherapyRooms from "@/components/settings/TherapyRooms";
import Administration from "@/components/settings/Administration";
import LibraryCategories from "@/components/settings/LibraryCategories";
import PublicSite from "@/components/settings/PublicSite";

const MOVED_TABS: Record<string, string> = {
  "stripe-connect": "/admin/payments-and-subscription?tab=payment-integration",
  "payment-integration": "/admin/payments-and-subscription?tab=payment-integration",
  "subscription-billing": "/admin/payments-and-subscription?tab=subscription",
  subscription: "/admin/payments-and-subscription?tab=subscription",
};

/** Old settings tab ids → merged Services tab */
const LEGACY_TAB_ALIASES: Record<string, string> = {
  "service-prices": "services",
  "service-visibility": "services",
};

const Settings = () => {
  const [searchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") ?? "system-options";
  const movedTo = MOVED_TABS[initialTab];
  const resolvedTab = LEGACY_TAB_ALIASES[initialTab] ?? initialTab;
  const [activeTab, setActiveTab] = useState(
    movedTo ? "system-options" : resolvedTab,
  );

  const resetKey = initialTab;
  const [previousResetKey, setPreviousResetKey] = useState(resetKey);
  if (previousResetKey !== resetKey) {
    setPreviousResetKey(resetKey);
    if (!movedTo) setActiveTab(LEGACY_TAB_ALIASES[initialTab] ?? initialTab);
  }

  if (movedTo) {
    return <Navigate to={movedTo} replace />;
  }

  const renderActiveTab = () => {
    switch (activeTab) {
      case "system-options":
        return <SystemOptions />;
      case "services":
      case "service-prices":
      case "service-visibility":
        return <ServicePrices />;
      case "public-site":
        return <PublicSite />;
      case "invoice-policies":
        return <InvoicePolicies />;
      case "therapy-rooms":
        return <TherapyRooms />;
      case "library-categories":
        return <LibraryCategories />;
      case "administration":
        return <Administration />;
      default:
        return <SystemOptions />;
    }
  };

  return (
    <div className="flex h-[calc(100vh-10.5rem)] min-h-0 flex-col overflow-hidden">
      <div className="shrink-0 pb-4">
        <SettingsTabs activeTab={activeTab} onTabChange={setActiveTab} />
      </div>
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        {renderActiveTab()}
      </div>
    </div>
  );
};

export default Settings;
