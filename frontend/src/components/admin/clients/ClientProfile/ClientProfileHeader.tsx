import { TrashIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { Archive } from "@solar-icons/react-perf/category/notes/Linear/Archive";
import { CheckCircle } from "@solar-icons/react-perf/category/ui/Linear/CheckCircle";
import { FolderOpen } from "@solar-icons/react-perf/category/folders/Linear/FolderOpen";
import { Pen } from "@solar-icons/react-perf/category/messages/Linear/Pen";
import { ShieldCheck } from "@solar-icons/react-perf/category/security/Linear/ShieldCheck";
import { SquareTopDown } from "@solar-icons/react-perf/category/arrows-action/Linear/SquareTopDown";
import { UserHeartRounded } from "@solar-icons/react-perf/category/users/Linear/UserHeartRounded";
import { UserRounded } from "@solar-icons/react-perf/category/users/Linear/UserRounded";
import { Avatar, AvatarFallback } from "../../../ui/avatar";
import { Badge } from "../../../ui/badge";
import ConfirmationModal from "../../../shared/ConfirmationModal";
import type { Client } from "../../../../types/client.type";
import { isClientInactive } from "@/utils/clientStatus";
import { matchesOptionKey } from "@/utils/systemOptions";
import { useClientOverviewLabels } from "@/hooks/useSystemOptionCatalog";

interface ClientProfileHeaderProps {
  client: Client;
  readOnly?: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
  onCloseFile?: () => Promise<void> | void;
  onActivateFile?: () => Promise<void> | void;
  isClosingFile?: boolean;
  isActivatingFile?: boolean;
}

const ClientProfileHeader = ({
  client,
  readOnly = false,
  onEdit,
  onDelete,
  onCloseFile,
  onActivateFile,
  isClosingFile = false,
  isActivatingFile = false,
}: ClientProfileHeaderProps) => {
  const [isCloseFileModalOpen, setIsCloseFileModalOpen] = useState(false);
  const [isActivateFileModalOpen, setIsActivateFileModalOpen] = useState(false);
  const showOpenFile = isClientInactive(client);
  const labels = useClientOverviewLabels({
    insuranceProvider: client.insuranceProvider || client.insurance,
    insuranceType: client.insuranceType,
    clientStatus: client.clientStatus,
  });
  const insuranceLabel =
    (labels.insuranceProvider && labels.insuranceProvider !== "—"
      ? labels.insuranceProvider
      : null) ||
    (labels.insuranceType && labels.insuranceType !== "—"
      ? labels.insuranceType
      : null) ||
    client.insurance ||
    "N/A";

  const getInitials = (name: string): string => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  const handleCloseFile = async () => {
    if (!onCloseFile) return;
    await onCloseFile();
    setIsCloseFileModalOpen(false);
  };

  const handleActivateFile = async () => {
    if (!onActivateFile) return;
    await onActivateFile();
    setIsActivateFileModalOpen(false);
  };

  return (
    <>
      <div className="min-w-0 overflow-hidden bg-white px-5 pt-5 pb-1.5">
        {/* Client Header */}
        <div className="mb-5 flex min-w-0 items-start gap-4">
          <Avatar className="h-16 w-16 shrink-0">
            <AvatarFallback className="bg-(--bg-primary-dark) text-xl font-semibold leading-7 text-white">
              {getInitials(client.name)}
            </AvatarFallback>
          </Avatar>

          <div className="flex-1 min-w-0">
            <div className="mb-1 flex min-w-0 flex-wrap items-center gap-3">
              <h2
                className="min-w-0 break-words text-xl font-semibold leading-7 text-(--text-primary-dark) [overflow-wrap:anywhere]"
                title={client.name}
              >
                {client.name}
              </h2>
              {client.serviceType && (
                <Badge className="shrink-0 border-0 bg-[#D0FBE3] px-2 py-0.5 text-xs font-normal text-[#007C54]">
                  {client.serviceType}
                </Badge>
              )}
              {client.clientStatus && (
                <Badge
                  className={`shrink-0 border-0 px-2 py-0.5 text-xs font-normal ${
                    isClientInactive(client)
                      ? "bg-[#FEE4E2] text-[#B42318]"
                      : matchesOptionKey(client.clientStatus, "pending")
                        ? "bg-[#FFFAEB] text-[#B54708]"
                        : "bg-[#EFF8FF] text-[#175CD3]"
                  }`}
                >
                  {labels.clientStatus && labels.clientStatus !== "—"
                    ? labels.clientStatus
                    : client.clientStatus}
                </Badge>
              )}
            </div>
            <div className="flex flex-wrap items-center gap-2 text-sm leading-[1.375rem] text-(--text-neutral-600)">
              {client.age && <span className="border-r border-(--text-neutral-400) pr-1.5">Age: {client.age}</span>}
              {!client.isMVAClient && (
                <span className="border-r border-(--text-neutral-400) pr-1.5">MVA Client</span>
              )}
              {client.lastSession && (
                <span>Last session: {client.lastSession}</span>
              )}
            </div>
          </div>

          {/* Action Icons */}
          <div className="flex shrink-0 items-center gap-3">
            {!readOnly && onEdit && (
              <button
                onClick={onEdit}
                className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg border border-(--neutral-100) transition-colors hover:bg-(--neutral-50)"
                title="Edit"
              >
                <Pen size={20} color="#1B1C20" />
              </button>
            )}
            {onCloseFile || onActivateFile ? (
              showOpenFile ? (
                <button
                  onClick={() => setIsActivateFileModalOpen(true)}
                  disabled={isActivatingFile || !onActivateFile}
                  className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg border border-(--neutral-100) transition-colors hover:bg-[#EBFEF4]"
                  title="Open File"
                >
                  <FolderOpen size={20} color="#007C54" />
                </button>
              ) : (
                <button
                  onClick={() => setIsCloseFileModalOpen(true)}
                  disabled={isClosingFile || !onCloseFile}
                  className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg border border-(--neutral-100) transition-colors hover:bg-(--neutral-50)"
                  title="Close File"
                >
                  <Archive size={20} color="#1B1C20" />
                </button>
              )
            ) : null}
            {onDelete ? (
              <button
                onClick={onDelete}
                className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg border border-(--neutral-100) transition-colors hover:bg-red-50"
                title="Delete"
              >
                <TrashIcon size={20} color="#EF4444" />
              </button>
            ) : null}
          </div>
        </div>

        {/* Key Identifiers - Cleaner Card Design */}
        <div className="grid grid-cols-4 gap-4">
          {/* Client ID Card */}
          <div className="flex items-center gap-3 rounded-xl border border-(--neutral-100) bg-white p-3">
            <div className="flex h-[2.375rem] w-[2.375rem] shrink-0 items-center justify-center rounded-lg bg-(--bg-primary-50)">
              <UserRounded size={20} color="#3C4D58" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="mb-0.5 text-xs leading-[1.125rem] text-(--text-neutral-600)">Client ID</p>
              <p className="truncate text-base font-semibold leading-6 text-(--text-primary-dark)">
                {client.clientId || client.referenceNumber}
              </p>
            </div>
          </div>

          {/* Assigned Therapist Card */}
          <div className="flex items-center gap-3 rounded-xl border border-(--neutral-100) bg-white p-3">
            <div className="flex h-[2.375rem] w-[2.375rem] shrink-0 items-center justify-center rounded-lg bg-(--bg-primary-50)">
              <UserHeartRounded size={20} color="#3C4D58" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="mb-0.5 text-xs leading-[1.125rem] text-(--text-neutral-600)">Assigned Therapist</p>
              <p
                className="truncate text-base font-semibold leading-6 text-(--text-primary-dark)"
                title={client.therapist || "Unassigned"}
              >
                {client.therapist || "Unassigned"}
              </p>
            </div>
          </div>

          {/* Insurance Card */}
          <div className="flex items-center gap-3 rounded-xl border border-(--neutral-100) bg-white p-3">
            <div className="flex h-[2.375rem] w-[2.375rem] shrink-0 items-center justify-center rounded-lg bg-(--bg-primary-50)">
              <ShieldCheck size={20} color="#3C4D58" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="mb-0.5 text-xs leading-[1.125rem] text-(--text-neutral-600)">Insurance</p>
              <p
                className="truncate text-base font-semibold leading-6 text-(--text-primary-dark)"
                title={insuranceLabel}
              >
                {insuranceLabel}
              </p>
            </div>
          </div>

          {/* Client Portal Card with External Link */}
          <div className="flex items-center gap-3 rounded-xl border border-(--neutral-100) bg-white p-3">
            <div className="flex h-[2.375rem] w-[2.375rem] shrink-0 items-center justify-center rounded-lg bg-[#EBFEF4]">
              <CheckCircle size={20} color="#0ABF7C" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="mb-0.5 text-xs leading-[1.125rem] text-(--text-neutral-600)">Client Portal</p>
              <p className="text-base font-semibold leading-6 text-(--text-primary-dark)">
                {client.portalAccessEnabled ? "Enabled" : "Disabled"}
              </p>
            </div>
            {client.portalAccessEnabled && (
              <button
                type="button"
                className="p-1 hover:bg-gray-100 rounded transition-colors cursor-pointer"
                title="Open portal"
                aria-label="Open client portal"
              >
                <SquareTopDown
                  size={20}
                  color="#1B1C20"
                />
              </button>
            )}
          </div>
        </div>
      </div>

      <ConfirmationModal
        type="close"
        isOpen={isCloseFileModalOpen}
        onClose={() => setIsCloseFileModalOpen(false)}
        onConfirm={() => {
          void handleCloseFile();
        }}
        confirmButtonLoading={isClosingFile}
        confirmButtonLoadingText="Closing..."
      />

      <ConfirmationModal
        type="activate"
        isOpen={isActivateFileModalOpen}
        onClose={() => setIsActivateFileModalOpen(false)}
        onConfirm={() => {
          void handleActivateFile();
        }}
        confirmButtonLoading={isActivatingFile}
        confirmButtonLoadingText="Opening..."
      />
    </>
  );
};

export default ClientProfileHeader;
