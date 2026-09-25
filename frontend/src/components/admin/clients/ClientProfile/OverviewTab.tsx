import { useEffect, useState } from "react";
import CollapsibleSection from "../../../shared/CollapsibleSection";
import PortalAccessSection from "./PortalAccessSection";
import type { Client } from "@/types/client.type.ts";
import { useClientOverviewLabels } from "@/hooks/useSystemOptionCatalog";
import { useGetAdminClientSessionSummaryQuery } from "@/store/api/admin/clients.api";

interface OverviewTabProps {
  client: Client;
  readOnly?: boolean;
  isPortalAccessUpdating?: boolean;
  isPortalActivationSending?: boolean;
  onUpdatePortalAccess?: (enable: boolean) => void;
  onResendPortalActivation?: () => void;
}

function formatLastLoginMeta(date?: string | null): string {
  if (!date) return "Last Login: Not logged in yet";

  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return "Last Login: Not logged in yet";

  return `Last Login: ${new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  }).format(parsed)}`;
}

const overviewValueClassName =
  "break-words text-base font-medium leading-6 text-(--text-primary-dark) [overflow-wrap:anywhere]";
const overviewMediumValueClassName =
  "break-words text-base font-medium leading-6 text-(--text-primary-dark) [overflow-wrap:anywhere]";
const overviewLabelClassName =
  "mb-1 block text-sm leading-[1.375rem] text-(--text-neutral-600)";
const profileSectionClassName =
  "rounded-xl border-(--neutral-100) shadow-[0_2px_2px_0_rgba(30,40,46,0.04)]";
const profileSectionHeaderClassName = "h-14 bg-white px-4 hover:bg-white";
const profileSectionContentClassName = "px-4 pb-4";
const profileSectionTitleClassName =
  "text-base leading-6 text-(--text-primary-dark)";

const OVERVIEW_EXPANDED_SECTIONS_STORAGE_KEY =
  "therapyflow:client-overview:expanded-sections";
const DEFAULT_EXPANDED_SECTIONS = ["General Information"];

const getInitialExpandedSections = (): Set<string> => {
  if (typeof window === "undefined") {
    return new Set(DEFAULT_EXPANDED_SECTIONS);
  }

  try {
    const storedSections = window.localStorage.getItem(
      OVERVIEW_EXPANDED_SECTIONS_STORAGE_KEY,
    );
    if (storedSections === null) {
      return new Set(DEFAULT_EXPANDED_SECTIONS);
    }

    const parsedSections: unknown = JSON.parse(storedSections);
    if (
      Array.isArray(parsedSections) &&
      parsedSections.every((section) => typeof section === "string")
    ) {
      return new Set(parsedSections);
    }
  } catch {
    // Ignore unavailable or malformed browser storage and use the default state.
  }

  return new Set(DEFAULT_EXPANDED_SECTIONS);
};

const OverviewTab = ({
  client,
  readOnly = false,
  isPortalAccessUpdating = false,
  isPortalActivationSending = false,
  onUpdatePortalAccess,
  onResendPortalActivation,
}: OverviewTabProps) => {
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    getInitialExpandedSections,
  );
  const labels = useClientOverviewLabels(client);
  const clientId = Number.parseInt(client.id, 10);
  const { data: sessionSummary } = useGetAdminClientSessionSummaryQuery(
    clientId,
    { skip: !Number.isFinite(clientId) },
  );
  const completedSessions =
    sessionSummary?.completed ?? client.completedSessions;

  useEffect(() => {
    try {
      window.localStorage.setItem(
        OVERVIEW_EXPANDED_SECTIONS_STORAGE_KEY,
        JSON.stringify([...expandedSections]),
      );
    } catch {
      // The accordion still works when browser storage is unavailable.
    }
  }, [expandedSections]);

  const display = (value?: string, resolved?: string) =>
    resolved && resolved !== "—" ? resolved : value;

  const toggleSection = (section: string) => {
    setExpandedSections((current) => {
      const next = new Set(current);
      if (next.has(section)) {
        next.delete(section);
      } else {
        next.add(section);
      }
      return next;
    });
  };

  return (
    <div className="min-w-0 space-y-0 p-5">
      {/* General Information */}
      <CollapsibleSection
        title="General Information"
        className={profileSectionClassName}
        headerClassName={profileSectionHeaderClassName}
        contentClassName={profileSectionContentClassName}
        titleClassName={profileSectionTitleClassName}
        isExpanded={expandedSections.has("General Information")}
        onToggle={() => toggleSection("General Information")}
      >
        <div className="pt-4 space-y-6">
          {/* Personal Info */}
          <div>
            <h4 className="mb-3 text-sm font-bold leading-[1.375rem] text-(--text-primary-dark)">
              Personal Info
            </h4>
            <div className="grid grid-cols-2 gap-x-9 gap-y-4 md:grid-cols-3 lg:grid-cols-4">
              {client.dateOfBirth && (
                <div className="min-w-0">
                  <span className={overviewLabelClassName}>DOB</span>
                  <p className={overviewValueClassName}>
                    {client.dateOfBirth}
                  </p>
                </div>
              )}
              {client.gender && (
                <div className="min-w-0">
                  <span className={overviewLabelClassName}>
                    Gender
                  </span>
                  <p className={overviewValueClassName}>
                    {display(client.gender, labels.gender)}
                  </p>
                </div>
              )}
              {client.maritalStatus && (
                <div className="min-w-0">
                  <span className={overviewLabelClassName}>
                    Marital Status
                  </span>
                  <p className={overviewValueClassName}>
                    {display(client.maritalStatus, labels.maritalStatus)}
                  </p>
                </div>
              )}
              {client.preferredLanguage && (
                <div className="min-w-0">
                  <span className={overviewLabelClassName}>
                    Preferred Language
                  </span>
                  <p className={overviewValueClassName}>
                    {display(client.preferredLanguage, labels.preferredLanguage)}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Contact Info */}
          <div>
            <h4 className="mb-3 text-sm font-bold leading-[1.375rem] text-(--text-primary-dark)">
              Contact Info
            </h4>
            <div className="grid grid-cols-1 gap-x-9 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
              {client.phone && (
                <div className="min-w-0">
                  <span className={overviewLabelClassName}>
                    Phone
                  </span>
                  <p className={overviewValueClassName}>
                    {client.phone}
                  </p>
                </div>
              )}
              {client.email && (
                <div className="min-w-0">
                  <span className={overviewLabelClassName}>
                    Email
                  </span>
                  <p
                    className={`${overviewValueClassName} truncate`}
                    title={client.email}
                  >
                    {client.email}
                  </p>
                </div>
              )}
              {client.address && (
                <div className="min-w-0 sm:col-span-2">
                  <span className={overviewLabelClassName}>
                    Address
                  </span>
                  <p className={overviewValueClassName}>
                    {client.address}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Emergency Contact - Separate Box */}
          {client.emergencyContact && (
            <div className="min-w-0 rounded-xl border border-(--neutral-100) bg-(--bg-primary-light) p-3">
              <span className="mb-0.5 block text-xs leading-[1.125rem] text-(--text-neutral-600)">
                Emergency Contact
              </span>
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 break-words text-base font-semibold leading-6 text-(--text-primary-dark) [overflow-wrap:anywhere]">
                <span>{client.emergencyContact.name}</span>
                <span className="text-gray-400">|</span>
                <span>{client.emergencyContact.phone}</span>
              </div>
            </div>
          )}
        </div>
      </CollapsibleSection>

      {/* Clinical Status */}
      <CollapsibleSection
        title="Clinical Status"
        className={profileSectionClassName}
        headerClassName={profileSectionHeaderClassName}
        contentClassName={profileSectionContentClassName}
        titleClassName={profileSectionTitleClassName}
        isExpanded={expandedSections.has("Clinical Status")}
        onToggle={() => toggleSection("Clinical Status")}
      >
        <div className="pt-4 space-y-4">
          <div className="grid grid-cols-2 gap-x-9 gap-y-4 md:grid-cols-3 lg:grid-cols-4">
            {client.clientStatus && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Status
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(client.clientStatus, labels.clientStatus)}
                </p>
              </div>
            )}
            {client.clientStage && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Treatment Stage
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(client.clientStage, labels.clientStage)}
                </p>
              </div>
            )}
            {client.clientType && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Client Type
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(client.clientType, labels.clientType)}
                </p>
              </div>
            )}
            {client.serviceType && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Service Type
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(client.serviceType, labels.serviceType)}
                </p>
              </div>
            )}
            {client.serviceFrequency && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Frequency
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(client.serviceFrequency, labels.serviceFrequency)}
                </p>
              </div>
            )}
            {client.treatmentModality && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Treatment Modality
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(client.treatmentModality, labels.treatmentModality)}
                </p>
              </div>
            )}
            {(client.insuranceProvider || client.insurance) && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Insurance Provider
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(
                    client.insuranceProvider || client.insurance,
                    labels.insuranceProvider,
                  )}
                </p>
              </div>
            )}
            {client.insuranceType && (
              <div className="min-w-0">
                <span className={overviewLabelClassName}>
                  Insurance Type
                </span>
                <p className={overviewMediumValueClassName}>
                  {display(client.insuranceType, labels.insuranceType)}
                </p>
              </div>
            )}
          </div>

          {/* Session Progress - Separate Box */}
          <div className="rounded-xl border border-(--neutral-100) bg-(--bg-primary-light) p-3">
            <span className="mb-0.5 block text-xs leading-[1.125rem] text-(--text-neutral-600)">
              Session Progress
            </span>
            <p className="text-base font-semibold leading-6 text-(--text-primary-dark)">
              {completedSessions ?? "—"} completed Sessions
            </p>
          </div>
        </div>
      </CollapsibleSection>

      {/* Referral Information */}
      <CollapsibleSection
        title="Referral Information"
        className={profileSectionClassName}
        headerClassName={profileSectionHeaderClassName}
        contentClassName={profileSectionContentClassName}
        titleClassName={profileSectionTitleClassName}
        isExpanded={expandedSections.has("Referral Information")}
        onToggle={() => toggleSection("Referral Information")}
      >
        <div className="grid grid-cols-1 gap-x-9 gap-y-4 pt-4 sm:grid-cols-2 lg:grid-cols-4">
          {client.referrerName && (
            <div className="min-w-0">
              <span className={overviewLabelClassName}>
                Referred By
              </span>
              <p className={overviewMediumValueClassName}>
                {client.referrerName}
              </p>
            </div>
          )}
          {client.referralNumber && (
            <div className="min-w-0">
              <span className={overviewLabelClassName}>
                Reference Number
              </span>
              <p className={overviewMediumValueClassName}>
                {client.referralNumber}
              </p>
            </div>
          )}
          {client.referralDate && (
            <div className="min-w-0">
              <span className={overviewLabelClassName}>
                Referral Date
              </span>
              <p className={overviewMediumValueClassName}>
                {client.referralDate}
              </p>
            </div>
          )}
          {client.referralSource && (
            <div className="min-w-0">
              <span className={overviewLabelClassName}>Source</span>
              <p className={overviewMediumValueClassName}>
                {display(client.referralSource, labels.referralSource)}
              </p>
            </div>
          )}
        </div>
      </CollapsibleSection>

      {/* Portal Access Management */}
      <CollapsibleSection
        title="Portal Access Management"
        className={profileSectionClassName}
        headerClassName={profileSectionHeaderClassName}
        contentClassName={profileSectionContentClassName}
        titleClassName={profileSectionTitleClassName}
        showStatus={client.portalAccessEnabled ? "Enabled" : "Disabled"}
        statusMetaText={formatLastLoginMeta(client.lastLogin)}
        isExpanded={expandedSections.has("Portal Access Management")}
        onToggle={() => toggleSection("Portal Access Management")}
      >
        <PortalAccessSection
          client={client}
          isEnabled={client.portalAccessEnabled || false}
          readOnly={readOnly}
          isSubmitPending={isPortalAccessUpdating}
          isResendPending={isPortalActivationSending}
          onUpdatePortalAccess={onUpdatePortalAccess}
          onResendActivationEmail={onResendPortalActivation}
        />
      </CollapsibleSection>
    </div>
  );
};

export default OverviewTab;
