import { useState } from "react";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import { cn } from "@/lib/utils";
import type { IntegrationKey } from "@/store/api/superAdminApi";
import { SUPER_ADMIN_INTEGRATIONS } from "./integration.config";
import IntegrationPanel from "./components/IntegrationPanel";
import ApiKeysPanel from "./components/ApiKeysPanel";

type IntegrationsTabKey = IntegrationKey | "api-keys";

const IntegrationsPage = () => {
  const [activeTab, setActiveTab] = useState<IntegrationsTabKey>("stripe");

  return (
    <SuperAdminPageShell
      title="Integrations"
      description="Configure platform-wide provider credentials and rotate API keys."
    >
      <div className="w-full overflow-x-auto pb-1">
        <div className="flex min-w-max flex-nowrap items-center gap-1 rounded-full bg-[#eceff3] p-1">
          {SUPER_ADMIN_INTEGRATIONS.map((integration) => (
            <button
              key={integration.key}
              type="button"
              onClick={() => setActiveTab(integration.key)}
              className={cn(
                "whitespace-nowrap rounded-full px-[1.125rem] py-[0.4375rem] text-[0.875rem] font-medium leading-[1.375rem] transition-colors",
                activeTab === integration.key
                  ? "bg-white text-[#2f3945] shadow-[0_1px_2px_rgba(15,23,42,0.05)]"
                  : "text-[#697584] hover:text-[#2f3945]",
              )}
            >
              {integration.label}
            </button>
          ))}
          <button
            type="button"
            onClick={() => setActiveTab("api-keys")}
            className={cn(
              "whitespace-nowrap rounded-full px-[1.125rem] py-[0.4375rem] text-[0.875rem] font-medium leading-[1.375rem] transition-colors",
              activeTab === "api-keys"
                ? "bg-white text-[#2f3945] shadow-[0_1px_2px_rgba(15,23,42,0.05)]"
                : "text-[#697584] hover:text-[#2f3945]",
            )}
          >
            API Keys
          </button>
        </div>
      </div>

      <section className="rounded-[1rem] border border-[#edf1f4] bg-white shadow-[0_1px_3px_rgba(15,23,42,0.04)]">
        {activeTab === "api-keys" ? (
          <ApiKeysPanel />
        ) : (
          <IntegrationPanel key={activeTab} integrationKey={activeTab} />
        )}
      </section>
    </SuperAdminPageShell>
  );
};

export default IntegrationsPage;
