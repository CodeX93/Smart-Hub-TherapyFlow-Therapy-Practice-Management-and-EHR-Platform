import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { DoubleAltArrowRight } from "@solar-icons/react-perf/category/arrows/Linear/DoubleAltArrowRight";
import { MaximizeSquareMinimalistic } from "@solar-icons/react-perf/category/arrows-action/Linear/MaximizeSquareMinimalistic";
import { MinimizeSquareMinimalistic } from "@solar-icons/react-perf/category/arrows-action/Linear/MinimizeSquareMinimalistic";
import { cn } from "@/lib/utils";
import ClientProfileHeader from "./ClientProfileHeader";
import ClientProfileTabs from "./ClientProfileTabs";
import OverviewTab from "@/components/admin/clients/ClientProfile/OverviewTab";
import SessionsTab from "@/components/admin/clients/ClientProfile/SessionsTab";
import type { Client } from "../../../types/client.type";

import AssessmentTab from "../../assessments/AssessmentTab";
import ReportsTab from "@/components/reports/ReportsTab";
import BillingTab from "@/components/admin/clients/ClientProfile/Billing/BillingTab";
import TasksTab from "./Tasks/TasksTab";
import ChecklistsTab from "@/components/admin/clients/ClientProfile/Checklists/ChecklistsTab";
import HistoryTab from "@/components/admin/clients/ClientProfile/History/HistoryTab";
import AdminFormsAndDocsTab from "@/components/admin/clients/ClientProfile/FormsAndDocs/FormsAndDocsTab";
import { isClientInactive } from "@/utils/clientStatus";
import ClosedFileNoticeModal from "@/components/shared/ClosedFileNoticeModal";
import { useClosedFileNotice } from "@/hooks/useClosedFileNotice";

interface ClientProfileProps {
  client: Client | null;
  isOpen: boolean;
  viewMode?: "closed" | "half" | "full";
  onClose?: () => void;
  onToggleFullView?: () => void;
  onEdit?: (clientId: string) => void;
  onDelete?: (clientId: string) => void;
  isPortalAccessUpdating?: boolean;
  isPortalActivationSending?: boolean;
  onUpdatePortalAccess?: (enable: boolean) => void;
  onResendPortalActivation?: () => void;
  requestedAction?: "checklist" | "scheduled-session" | "create-task" | null;
  requestedActionNonce?: number;
  /** When null/undefined, keep the currently open tab (e.g. while switching clients). */
  initialTab?: string | null;
}

const getInitialMountedTabs = (tab: string): Set<string> => {
  const next = new Set(["Overview"]);
  if (tab !== "Overview") next.add(tab);
  return next;
};

const ClientProfile = ({
  client,
  isOpen,
  viewMode = "half",
  onClose,
  onToggleFullView,
  onEdit,
  onDelete,
  isPortalAccessUpdating = false,
  isPortalActivationSending = false,
  onUpdatePortalAccess,
  onResendPortalActivation,
  requestedAction = null,
  requestedActionNonce = 0,
  initialTab = null,
}: ClientProfileProps) => {
  const [activeTab, setActiveTab] = useState(initialTab ?? "Overview");
  const [mountedTabs, setMountedTabs] = useState<Set<string>>(() =>
    getInitialMountedTabs(initialTab ?? "Overview"),
  );
  const [openCreateTaskTrigger, setOpenCreateTaskTrigger] = useState(0);
  const scrollRef = useRef<HTMLDivElement>(null);
  const pendingCreateTaskRef = useRef(false);
  const { isNoticeOpen, closeNotice } = useClosedFileNotice(isOpen, client);

  const tabs = [
    "Overview",
    "Sessions",
    "Assessments",
    "Reports",
    "Forms & Docs",
    "Billing",
    "Tasks",
    "Checklists",
    "History",
  ];

  useEffect(() => {
    setMountedTabs((prev) => {
      if (prev.has(activeTab)) return prev;
      const next = new Set(prev);
      next.add(activeTab);
      return next;
    });
  }, [activeTab]);

  useLayoutEffect(() => {
    // No forced tab (normal client list click) — keep whatever tab is already open.
    if (!initialTab) return;

    setActiveTab(initialTab);
    setMountedTabs((previous) => {
      const next = new Set(previous);
      next.add(initialTab);
      return next;
    });
    if (
      requestedAction === "create-task" &&
      initialTab === "Tasks" &&
      client &&
      !isClientInactive(client)
    ) {
      pendingCreateTaskRef.current = true;
    }
    // Omit `client` so switching clients keeps the currently open tab.
    // eslint-disable-next-line react-hooks/exhaustive-deps -- preserve tab across client changes
  }, [initialTab, requestedActionNonce, requestedAction]);

  useEffect(() => {
    if (!pendingCreateTaskRef.current || activeTab !== "Tasks" || !client) return;
    pendingCreateTaskRef.current = false;
    setOpenCreateTaskTrigger((previous) => previous + 1);
  }, [activeTab, client?.id, requestedActionNonce]);

  if (!isOpen || !client) return null;

  const readOnly = isClientInactive(client);

  const renderTabContent = (tab: string) => {
    if (!mountedTabs.has(tab)) return null;
    const className = activeTab === tab ? "block h-full" : "hidden h-full";

    switch (tab) {
      case "Overview":
        return (
          <div className={className}>
            <OverviewTab
              client={client}
              readOnly={readOnly}
              isPortalAccessUpdating={isPortalAccessUpdating}
              isPortalActivationSending={isPortalActivationSending}
              onUpdatePortalAccess={onUpdatePortalAccess}
              onResendPortalActivation={onResendPortalActivation}
            />
          </div>
        );
      case "Sessions":
        return (
          <div className={className}>
            <SessionsTab
              client={client}
              scrollRef={scrollRef}
              schedulingPath="/therapist/scheduling"
              isAdmin={false}
              isActive={activeTab === "Sessions"}
              readOnly={readOnly}
            />
          </div>
        );
      case "Assessments":
        return (
          <div className={className}>
            <AssessmentTab clientId={Number(client.id)} readOnly={readOnly} />
          </div>
        );
      case "Reports":
        return (
          <div className={className}>
            <ReportsTab clientId={Number(client.id)} readOnly={readOnly} />
          </div>
        );
      case "Forms & Docs":
        return (
          <div
            className={
              activeTab === tab
                ? "block h-full min-h-0 overflow-hidden"
                : "hidden"
            }
          >
            <AdminFormsAndDocsTab
              client={client}
              isTabActive={activeTab === "Forms & Docs"}
              readOnly={readOnly}
            />
          </div>
        );
      case "Billing":
        return (
          <div
            className={activeTab === tab ? "block h-full min-h-0" : "hidden"}
          >
            <BillingTab
              key={client.id}
              scrollRef={scrollRef}
              client={client}
              isActive={activeTab === "Billing"}
              readOnly={readOnly}
            />
          </div>
        );
      case "Tasks":
        return (
          <div className={className}>
            <TasksTab
              key={client.id}
              client={client}
              openCreateTrigger={readOnly ? 0 : openCreateTaskTrigger}
              readOnly={readOnly}
            />
          </div>
        );
      case "Checklists":
        return (
          <div className={className}>
            <ChecklistsTab client={client} readOnly={readOnly} />
          </div>
        );
      case "History":
        return (
          <div className={className}>
            <HistoryTab
              clientId={Number(client.id)}
              isActive={activeTab === "History"}
              readOnly={readOnly}
            />
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="bg-white flex flex-col h-full overflow-hidden">
      <ClosedFileNoticeModal
        isOpen={isNoticeOpen}
        onClose={closeNotice}
        clientName={client.name}
      />
      {/* Top Controls Row - Above Tabs */}
      <div className="flex items-center justify-between px-5 py-3">
        {/* Left: Control Buttons */}
        <div className="flex items-center">
          {viewMode === "half" && onClose && (
            <button
              onClick={onClose}
              className="flex h-7 w-7 cursor-pointer items-center justify-center rounded-lg transition-colors hover:bg-(--neutral-50)"
              title="Close"
            >
              <DoubleAltArrowRight size={20} color="#1B1C20" />
            </button>
          )}
          {viewMode === "half" && onToggleFullView && (
            <button
              onClick={onToggleFullView}
              className="flex h-7 w-7 cursor-pointer items-center justify-center rounded-lg transition-colors hover:bg-(--neutral-50)"
              title="Expand to full view"
            >
              <MaximizeSquareMinimalistic size={20} color="#1B1C20" />
            </button>
          )}
          {viewMode === "full" && onToggleFullView && (
            <button
              onClick={onToggleFullView}
              className="flex h-7 w-7 cursor-pointer items-center justify-center rounded-lg transition-colors hover:bg-(--neutral-50)"
              title="Return to half view"
            >
              <MinimizeSquareMinimalistic size={20} color="#1B1C20" />
            </button>
          )}
        </div>
      </div>

      {/* Tabs Row - With Gray Background */}
      <div className="px-5">
        <ClientProfileTabs
          tabs={tabs}
          activeTab={activeTab}
          onTabChange={setActiveTab}
        />
      </div>

      {/* Client Header - Only show on Overview tab */}
      {activeTab === "Overview" && (
        <ClientProfileHeader
          client={client}
          readOnly={readOnly}
          onEdit={() => onEdit?.(client.id)}
          onDelete={() => onDelete?.(client.id)}
        />
      )}

      {/* Content */}
      <div
        ref={scrollRef}
        className={cn(
          "flex-1 min-h-0 overflow-x-hidden",
          activeTab === "Forms & Docs" || activeTab === "Billing"
            ? "overflow-hidden"
            : "overflow-y-auto",
        )}
      >
        {tabs.map((tab) => (
          <div
            key={tab}
            className={cn(
              activeTab === "Forms & Docs" && activeTab === tab && "h-full min-h-0",
              activeTab === "Billing" && activeTab === tab && "h-full min-h-0",
            )}
          >
            {renderTabContent(tab)}
          </div>
        ))}
      </div>
    </div>
  );
};

export default ClientProfile;
