import { X } from "lucide-react";
import type { DuplicateRecord } from "@/types/duplicate-detection.types";

interface FullRecordModalProps {
  isOpen: boolean;
  record: DuplicateRecord | null;
  onClose: () => void;
}

const renderValue = (value: unknown): string => {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "Yes" : "No";
  return String(value);
};

const FullRecordModal = ({ isOpen, record, onClose }: FullRecordModalProps) => {
  if (!isOpen || !record) return null;

  const details = record.details ?? {};
  const fields: Array<{ label: string; value: unknown }> = [
    { label: "Client ID", value: record.clientId },
    { label: "Full Name", value: record.name },
    { label: "Email", value: record.email },
    { label: "Phone", value: record.phone },
    { label: "Date of Birth", value: record.dob },
    { label: "Status", value: record.status },
    { label: "Stage", value: details.stage },
    {
      label: "Assigned Therapist",
      value: details.assignedTherapistName || details.assignedTherapistId,
    },
    { label: "Preferred Language", value: details.preferredLanguage },
    { label: "Pronouns", value: details.pronouns },
    { label: "Gender", value: details.gender },
    { label: "Marital Status", value: details.maritalStatus },
    { label: "Client Type", value: details.clientType },
    { label: "Service Type", value: details.serviceType },
    { label: "Service Frequency", value: details.serviceFrequency },
    { label: "Street Address 1", value: details.streetAddress1 },
    { label: "Street Address 2", value: details.streetAddress2 },
    { label: "City", value: details.city },
    { label: "Province", value: details.province },
    { label: "Postal Code", value: details.postalCode },
    { label: "Country", value: details.country },
    { label: "Emergency Contact Name", value: details.emergencyContactName },
    { label: "Emergency Contact Phone", value: details.emergencyContactPhone },
    {
      label: "Emergency Contact Relationship",
      value: details.emergencyContactRelationship,
    },
    { label: "Insurance Provider", value: details.insuranceProvider },
    { label: "Policy Number", value: details.policyNumber },
    { label: "Group Number", value: details.groupNumber },
    { label: "Insurance Phone", value: details.insurancePhone },
    { label: "Copay Amount", value: details.copayAmount },
    { label: "Deductible", value: details.deductible },
    { label: "Referrer Name", value: details.referrerName },
    { label: "Referral Date", value: details.referralDate },
    { label: "Reference Number", value: details.referenceNumber },
    { label: "Client Source", value: details.clientSource },
    { label: "Has Portal Access", value: details.hasPortalAccess },
    { label: "Portal Email", value: details.portalEmail },
    { label: "Email Notifications", value: details.emailNotifications },
    { label: "Notes", value: details.notes },
    { label: "Created At", value: record.created },
    { label: "Updated At", value: details.updatedAt },
    { label: "Last Session Date", value: details.lastSessionDate },
    { label: "Next Appointment Date", value: details.nextAppointmentDate },
  ];

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 backdrop-blur-sm">
      <div className="relative w-full max-w-4xl bg-white rounded-2xl shadow-2xl mx-4 overflow-hidden">
        <div className="p-6 border-b border-(--neutral-100) flex items-center justify-between">
          <h3 className="text-xl font-semibold text-(--text-primary-dark)">
            Full Client Record
          </h3>
          <button
            onClick={onClose}
            className="p-1.5 hover:bg-gray-100 rounded-full transition-colors cursor-pointer"
          >
            <X size={22} className="text-(--text-neutral-500)" />
          </button>
        </div>

        <div className="p-6 max-h-[70vh] overflow-y-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-3">
            {fields.map((field) => (
              <div key={field.label} className="flex justify-between gap-4 py-1">
                <span className="text-sm text-(--text-neutral-600)">{field.label}</span>
                <span className="text-sm font-medium text-(--text-primary-dark) text-right break-words">
                  {renderValue(field.value)}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default FullRecordModal;
