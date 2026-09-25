
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from "react";

import DetectionHeader from "@/components/duplicate-detection/DetectionHeader";
import DuplicateGroupCard from "@/components/duplicate-detection/DuplicateGroupCard";
import DuplicateModal from "@/components/duplicate-detection/DuplicateModal";
import FullRecordModal from "@/components/duplicate-detection/FullRecordModal";
import Toast from "@/components/shared/Toast";
import {
  useGetDuplicateClientsQuery,
  useMarkClientAsDuplicateMutation,
} from "@/store/api/admin/clients.api";
import { getApiErrorMessage } from "@/utils/apiError";

import type {
  DuplicateGroup,
  DuplicateRecord,
} from "@/types/duplicate-detection.types";
import type { AdminDuplicateGroup } from "@/store/api/admin/clients.api";

function formatDate(value?: string): string {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  }).format(date);
}

function normalizeStatus(value?: string): DuplicateRecord["status"] {
  const normalized = value?.trim().toLowerCase();
  if (normalized === "active") return "Active";
  if (normalized === "inactive") return "Inactive";
  return "Pending";
}

function mapDuplicateGroups(apiGroups: AdminDuplicateGroup[]): DuplicateGroup[] {
  return apiGroups.map((group, groupIndex) => {
    const keepId = group.recommendation?.keepClientId;
    const records: DuplicateRecord[] = group.clients.map((client) => ({
      id: String(client.id),
      clientId: client.clientId || String(client.id),
      name: client.fullName || "Unknown Client",
      phone: client.phone || "-",
      email: client.email || "-",
      dob: formatDate(client.dateOfBirth),
      status: normalizeStatus(client.status),
      created: formatDate(client.createdAt),
      stats: {
        sessions: 0,
        documents: client.documentCount ?? 0,
        billing: 0,
      },
      isRecommendedToKeep: keepId ? client.id === keepId : groupIndex === 0,
      details: {
        stage: client.stage,
        assignedTherapistName: client.assignedTherapistName,
        assignedTherapistId: client.assignedTherapistId,
        preferredLanguage: client.preferredLanguage,
        pronouns: client.pronouns,
        gender: client.gender,
        maritalStatus: client.maritalStatus,
        clientType: client.clientType,
        serviceType: client.serviceType,
        serviceFrequency: client.serviceFrequency,
        streetAddress1: client.streetAddress1,
        streetAddress2: client.streetAddress2,
        city: client.city,
        province: client.province,
        postalCode: client.postalCode,
        country: client.country,
        emergencyContactName: client.emergencyContactName,
        emergencyContactPhone: client.emergencyContactPhone,
        emergencyContactRelationship: client.emergencyContactRelationship,
        insuranceProvider: client.insuranceProvider,
        policyNumber: client.policyNumber,
        groupNumber: client.groupNumber,
        insurancePhone: client.insurancePhone,
        copayAmount: client.copayAmount,
        deductible: client.deductible,
        referrerName: client.referrerName,
        referralDate: client.referralDate,
        referenceNumber: client.referenceNumber,
        clientSource: client.clientSource,
        hasPortalAccess: client.hasPortalAccess,
        portalEmail: client.portalEmail,
        emailNotifications: client.emailNotifications,
        notes: client.notes,
        updatedAt: formatDate(client.updatedAt),
        lastSessionDate: formatDate(client.lastSessionDate),
        nextAppointmentDate: formatDate(client.nextAppointmentDate),
      },
    }));

    const keepRecord = records.find((record) => record.isRecommendedToKeep) || records[0];
    const reasons = group.recommendation?.reasons?.length
      ? group.recommendation.reasons
      : [
          group.matchType || "Potential duplicate",
          group.confidenceLevel ? `Confidence ${group.confidenceLevel}` : "",
        ].filter(Boolean);

    return {
      id: `dup-${groupIndex + 1}`,
      groupNumber: groupIndex + 1,
      matchReasons: reasons,
      recommendation: {
        keepName: keepRecord?.name || "Unknown Client",
        keepId: keepRecord?.clientId || "-",
        reason: reasons[0] || "Review recommended record.",
      },
      records,
    };
  });
}

const DuplicateDetection = () => {
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("error");
  const {
    data: duplicateResponse,
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useGetDuplicateClientsQuery();
  const [markClientAsDuplicate, { isLoading: isMarkingDuplicate }] =
    useMarkClientAsDuplicateMutation();
  const duplicateGroups = mapDuplicateGroups(
    duplicateResponse?.duplicateGroups ?? [],
  );
  const [resolvedGroupIds, setResolvedGroupIds] = useState<string[]>([]);
  const [modalState, setModalState] = useState<{
    isOpen: boolean;
    primaryRecord: DuplicateRecord | null;
    duplicateRecord: DuplicateRecord | null;
    groupId: string | null;
  }>({
    isOpen: false,
    primaryRecord: null,
    duplicateRecord: null,
    groupId: null,
  });
  const [fullRecordModalState, setFullRecordModalState] = useState<{
    isOpen: boolean;
    record: DuplicateRecord | null;
  }>({
    isOpen: false,
    record: null,
  });

  const handleRefreshScan = () => {
    void refetch()
      .unwrap()
      .then(() => {
        setResolvedGroupIds([]);
        setToastType("success");
        setToastMessage("Duplicate scan refreshed successfully.");
      })
      .catch((err) => {
        setToastType("error");
        setToastMessage(getApiErrorMessage(err));
      });
  };

  const openMarkAsDuplicateModal = (groupId: string, recordId: string) => {
    const group = duplicateGroups.find((g) => g.id === groupId);
    if (!group) return;

    const duplicateRecord = group.records.find((r) => r.id === recordId);
    const primaryRecord = group.records.find((r) => r.id !== recordId);

    if (duplicateRecord && primaryRecord) {
      setModalState({
        isOpen: true,
        primaryRecord,
        duplicateRecord,
        groupId,
      });
    }
  };

  const openFullRecordModal = (record: DuplicateRecord) => {
    setFullRecordModalState({
      isOpen: true,
      record,
    });
  };

  const handleConfirmDuplicate = async () => {
    if (!modalState.groupId || !modalState.duplicateRecord || !modalState.primaryRecord) return;

    const duplicateId = Number(modalState.duplicateRecord.id);
    const keepId = Number(modalState.primaryRecord.id);
    if (!Number.isFinite(duplicateId) || !Number.isFinite(keepId)) {
      setToastType("error");
      setToastMessage("Invalid client selection for duplicate action.");
      return;
    }

    try {
      await markClientAsDuplicate({
        id: duplicateId,
        duplicateOfClientId: keepId,
      }).unwrap();

      setToastType("success");
      setToastMessage("Client marked as duplicate successfully.");
      setResolvedGroupIds((prev) =>
        modalState.groupId ? [...prev, modalState.groupId] : prev,
      );

      setModalState({
        isOpen: false,
        primaryRecord: null,
        duplicateRecord: null,
        groupId: null,
      });

      await refetch().unwrap();
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const activeGroups = duplicateGroups.filter(
    (group) => !resolvedGroupIds.includes(group.id),
  );
  const affectedRecordsCount = activeGroups.reduce(
    (acc, group) => acc + group.records.length,
    0,
  );

  const queryErrorMessage = isError && error ? getApiErrorMessage(error) : null;

  return (
    <div className="flex flex-col w-full">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : queryErrorMessage ? (
        <Toast
          message={queryErrorMessage}
          type="error"
          onClose={() => {}}
        />
      ) : null}
      <DetectionHeader
        count={activeGroups.length}
        affectedRecords={affectedRecordsCount}
        onRefresh={handleRefreshScan}
      />

      <div className="flex flex-col">
        {isLoading || isFetching ? (
          <ContentLoader size="lg" className="py-20 bg-white rounded-3xl border border-(--neutral-100)" />
        ) : activeGroups.length > 0 ? (
          activeGroups.map((group) => (
            <DuplicateGroupCard
              key={group.id}
              group={group}
              onMarkAsDuplicate={openMarkAsDuplicateModal}
              onViewFullRecord={openFullRecordModal}
            />
          ))
        ) : (
          <div className="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-(--neutral-200)">
            <p className="text-(--text-neutral-500) font-medium">
              No more potential duplicates found.
            </p>
          </div>
        )}
      </div>

      <DuplicateModal
        isOpen={modalState.isOpen}
        onClose={() => setModalState((prev) => ({ ...prev, isOpen: false }))}
        onConfirm={handleConfirmDuplicate}
        primaryName={modalState.primaryRecord?.name || ""}
        duplicateName={modalState.duplicateRecord?.name || ""}
        isSubmitting={isMarkingDuplicate}
      />
      <FullRecordModal
        isOpen={fullRecordModalState.isOpen}
        record={fullRecordModalState.record}
        onClose={() =>
          setFullRecordModalState({
            isOpen: false,
            record: null,
          })
        }
      />
    </div>
  );
};

export default DuplicateDetection;
