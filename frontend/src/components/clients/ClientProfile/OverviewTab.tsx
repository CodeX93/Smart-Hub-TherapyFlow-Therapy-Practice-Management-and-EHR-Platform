import { useState } from "react";
import CollapsibleSection from "../../shared/CollapsibleSection";
import PortalAccessSection from "./PortalAccessSection";
import type { Client } from "../../../types/client.type";
import { useClientOverviewLabels } from "@/hooks/useSystemOptionCatalog";

interface OverviewTabProps {
  client: Client;
}

const OverviewTab = ({ client }: OverviewTabProps) => {
  const [expandedSection, setExpandedSection] = useState<string>(
    "General Information",
  );
  const labels = useClientOverviewLabels(client);

  const display = (value?: string, resolved?: string) =>
    resolved && resolved !== "—" ? resolved : value;

  return (
    <div className="p-6 space-y-0">
      {/* General Information */}
      <CollapsibleSection
        title="General Information"
        isExpanded={expandedSection === "General Information"}
        onToggle={() =>
          setExpandedSection(
            expandedSection === "General Information"
              ? ""
              : "General Information",
          )
        }
      >
        <div className="pt-4 space-y-6">
          {/* Personal Info */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-4">
              Personal Info
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-x-6 gap-y-4">
              {client.dateOfBirth && (
                <div>
                  <span className="text-xs text-gray-500 block mb-1">DOB</span>
                  <p className="text-sm font-semibold text-gray-900">
                    {client.dateOfBirth}
                  </p>
                </div>
              )}
              {client.gender && (
                <div>
                  <span className="text-xs text-gray-500 block mb-1">
                    Gender
                  </span>
                  <p className="text-sm font-semibold text-gray-900">
                    {display(client.gender, labels.gender)}
                  </p>
                </div>
              )}
              {client.maritalStatus && (
                <div>
                  <span className="text-xs text-gray-500 block mb-1">
                    Marital Status
                  </span>
                  <p className="text-sm font-semibold text-gray-900">
                    {display(client.maritalStatus, labels.maritalStatus)}
                  </p>
                </div>
              )}
              {client.preferredLanguage && (
                <div>
                  <span className="text-xs text-gray-500 block mb-1">
                    Preferred Language
                  </span>
                  <p className="text-sm font-semibold text-gray-900">
                    {display(client.preferredLanguage, labels.preferredLanguage)}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Contact Info */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-4">
              Contact Info
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-x-6 gap-y-4">
              {client.phone && (
                <div>
                  <span className="text-xs text-gray-500 block mb-1">
                    Phone
                  </span>
                  <p className="text-sm font-semibold text-gray-900">
                    {client.phone}
                  </p>
                </div>
              )}
              {client.email && (
                <div>
                  <span className="text-xs text-gray-500 block mb-1">
                    Email
                  </span>
                  <p className="text-sm font-semibold text-gray-900">
                    {client.email}
                  </p>
                </div>
              )}
              {client.address && (
                <div className="col-span-2 md:col-span-2">
                  <span className="text-xs text-gray-500 block mb-1">
                    Address
                  </span>
                  <p className="text-sm font-semibold text-gray-900">
                    {client.address}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Emergency Contact - Separate Box */}
          {client.emergencyContact && (
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <span className="text-xs text-gray-500 block mb-1">
                Emergency Contact
              </span>
              <p className="text-sm font-semibold text-gray-900">
                {client.emergencyContact.name} | {client.emergencyContact.phone}
              </p>
            </div>
          )}
        </div>
      </CollapsibleSection>

      {/* Clinical Status */}
      <CollapsibleSection
        title="Clinical Status"
        isExpanded={expandedSection === "Clinical Status"}
        onToggle={() =>
          setExpandedSection(
            expandedSection === "Clinical Status" ? "" : "Clinical Status",
          )
        }
      >
        <div className="pt-4 space-y-4">
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {client.clientStatus && (
              <div>
                <span className="text-xs text-gray-500 block mb-1">
                  Status
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(client.clientStatus, labels.clientStatus)}
                </p>
              </div>
            )}
            {client.clientStage && (
              <div>
                <span className="text-xs text-gray-500 block mb-1">
                  Treatment Stage
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(client.clientStage, labels.clientStage)}
                </p>
              </div>
            )}
            {client.clientType && (
              <div>
                <span className="text-xs text-gray-500 block mb-1">
                  Client Type
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(client.clientType, labels.clientType)}
                </p>
              </div>
            )}
            {client.serviceType && (
              <div>
                <span className="text-xs text-gray-500 block mb-1">
                  Service Type
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(client.serviceType, labels.serviceType)}
                </p>
              </div>
            )}
            {client.serviceFrequency && (
              <div>
                <span className="text-xs text-gray-500 block mb-1">
                  Frequency
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(client.serviceFrequency, labels.serviceFrequency)}
                </p>
              </div>
            )}
            {client.treatmentModality && (
              <div>
                <span className="text-xs text-gray-500 block mb-1">
                  Treatment Modality
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(client.treatmentModality, labels.treatmentModality)}
                </p>
              </div>
            )}
            {(client.insuranceProvider || client.insurance) && (
              <div>
                <span className="text-sm text-gray-500 block mb-1">
                  Insurance Provider
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(
                    client.insuranceProvider || client.insurance,
                    labels.insuranceProvider,
                  )}
                </p>
              </div>
            )}
            {client.insuranceType && (
              <div>
                <span className="text-sm text-gray-500 block mb-1">
                  Insurance Type
                </span>
                <p className="text-sm text-gray-900 font-medium">
                  {display(client.insuranceType, labels.insuranceType)}
                </p>
              </div>
            )}
          </div>

          {/* Session Progress - Separate Box */}
          {client.completedSessions !== undefined && (
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <span className="text-xs text-gray-500 block mb-1">
                Session Progress
              </span>
              <p className="text-sm font-semibold text-gray-900">
                {client.completedSessions} completed Sessions
              </p>
            </div>
          )}
        </div>
      </CollapsibleSection>

      {/* Referral Information */}
      <CollapsibleSection
        title="Referral Information"
        isExpanded={expandedSection === "Referral Information"}
        onToggle={() =>
          setExpandedSection(
            expandedSection === "Referral Information"
              ? ""
              : "Referral Information",
          )
        }
      >
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 pt-4">
          {client.referrerName && (
            <div>
              <span className="text-xs text-gray-500 block mb-1">
                Referred By
              </span>
              <p className="text-sm text-gray-900 font-medium">
                {client.referrerName}
              </p>
            </div>
          )}
          {client.referralNumber && (
            <div>
              <span className="text-xs text-gray-500 block mb-1">
                Reference Number
              </span>
              <p className="text-sm text-gray-900 font-medium">
                {client.referralNumber}
              </p>
            </div>
          )}
          {client.referralDate && (
            <div>
              <span className="text-xs text-gray-500 block mb-1">
                Referral Date
              </span>
              <p className="text-sm text-gray-900 font-medium">
                {client.referralDate}
              </p>
            </div>
          )}
          {client.referralSource && (
            <div>
              <span className="text-xs text-gray-500 block mb-1">Source</span>
              <p className="text-sm text-gray-900 font-medium">
                {display(client.referralSource, labels.referralSource)}
              </p>
            </div>
          )}
        </div>
      </CollapsibleSection>

      {/* Portal Access Management */}
      <CollapsibleSection
        title="Portal Access Management"
        showStatus={client.portalAccessEnabled ? "Enabled" : "Disabled"}
        isExpanded={expandedSection === "Portal Access Management"}
        onToggle={() =>
          setExpandedSection(
            expandedSection === "Portal Access Management"
              ? ""
              : "Portal Access Management",
          )
        }
      >
        <PortalAccessSection
          client={client}
          isEnabled={client.portalAccessEnabled || false}
        />
      </CollapsibleSection>
    </div>
  );
};

export default OverviewTab;
