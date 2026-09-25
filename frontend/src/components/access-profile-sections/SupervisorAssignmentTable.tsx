import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { Badge } from "../ui/badge";
import { Pencil, Search } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import DeleteAssignmentModal from "./DeleteAssignmentModal";
import { FREQUENCY_OPTIONS } from "@/pages/admin/user-access/profiles/user-access.static";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import AssignSupervisorModal from "./AssignSupervisorModal";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import Toast from "@/components/shared/Toast";
import {
  useCreateSupervisorAssignmentMutation,
  useDeleteSupervisorAssignmentMutation,
  useGetSupervisorAssignmentsQuery,
  useLazyGetSupervisorAssignmentByIdQuery,
  useUpdateSupervisorAssignmentMutation,
} from "@/store/api/admin/supervisorAssignments.api";
import { useLazyGetAdminUsersQuery } from "@/store/api/admin/users.api";
import type { SupervisorAssignmentSummary } from "@/store/api/admin/supervisorAssignments.api";
import type { AssignSupervisorFormValues } from "@/schemas/user-access-profiles.schema";
import type { MeetingFrequency, SupervisorAssignment } from "@/types/user-access-profiles.type";
import type { CustomSelectOption } from "../form/CustomSelect";
import { getApiErrorMessage } from "@/utils/apiError";

interface SupervisorAssignmentTableProps {
  isAssignModalOpen?: boolean;
  setSupervisorCounts?: (count: number) => void;
  setIsAssignModalOpen?: (value: boolean) => void;
  canManageAssignments?: boolean;
}

const assignmentTypeOptions: CustomSelectOption[] = [
  { value: "PRIMARY", label: "Primary" },
  { value: "SECONDARY", label: "Secondary" },
  { value: "CLINICAL", label: "Clinical" },
];

const frequencyOptions: CustomSelectOption[] = [
  { value: "DAILY", label: "Daily" },
  { value: "WEEKLY", label: "Weekly" },
  { value: "BIWEEKLY", label: "Biweekly" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "YEARLY", label: "Yearly" },
];

function formatDate(value?: string | null): string {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "-";
  return parsed.toLocaleDateString("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  });
}

function formatFrequency(value?: string | null): MeetingFrequency {
  const normalized = value?.trim().toUpperCase();
  if (normalized === "DAILY") return "Daily";
  if (normalized === "WEEKLY") return "Weekly";
  if (normalized === "BIWEEKLY") return "Biweekly";
  if (normalized === "MONTHLY") return "Monthly";
  return "Yearly";
}

function formatDateForInput(value?: string | null): string {
  if (!value) return "";
  const match = value.match(/^(\d{4}-\d{2}-\d{2})/);
  if (match) return match[1];
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "";
  const year = parsed.getFullYear();
  const month = String(parsed.getMonth() + 1).padStart(2, "0");
  const day = String(parsed.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function upsertSelectOption(
  options: CustomSelectOption[],
  next: CustomSelectOption,
): CustomSelectOption[] {
  if (!next.value) return options;
  const exists = options.some((entry) => entry.value === next.value);
  if (exists) {
    return options.map((entry) => (entry.value === next.value ? next : entry));
  }
  return [next, ...options];
}

function mapApiAssignmentToRow(record: SupervisorAssignmentSummary): SupervisorAssignment {
  return {
    id: String(record.id),
    supervisor: record.supervisor.fullName || record.supervisor.email || record.supervisor.username,
    therapist: record.therapist.fullName || record.therapist.email || record.therapist.username,
    meetingFrequency: formatFrequency(record.requiredMeetingFrequency),
    assignmentType: record.assignmentType || "-",
    lastMeeting: formatDate(record.lastMeetingDate),
    nextMeeting: formatDate(record.nextMeetingDate),
    assignedDate: formatDate(record.assignedDate),
    startDate: formatDate(record.startDate),
    endDate: formatDate(record.endDate),
    notes: record.notes || "-",
    isActive: record.isActive,
  };
}

const SupervisorAssignmentTable = ({
  isAssignModalOpen,
  setIsAssignModalOpen,
  setSupervisorCounts,
  canManageAssignments = true,
}: SupervisorAssignmentTableProps) => {
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [assignmentToDelete, setAssignmentToDelete] = useState<number | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [appliedSearchQuery, setAppliedSearchQuery] = useState("");
  const [filter, setFilter] = useState("All");
  const [displayedItems, setDisplayedItems] = useState(20);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingAssignmentId, setEditingAssignmentId] = useState<number | null>(null);
  const [modalInitialValues, setModalInitialValues] =
    useState<Partial<AssignSupervisorFormValues> | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastVariant, setToastVariant] = useState<"success" | "error">("success");
  const [supervisorOptions, setSupervisorOptions] = useState<CustomSelectOption[]>([]);
  const [therapistOptions, setTherapistOptions] = useState<CustomSelectOption[]>([]);
  const itemsPerPage = 20;

  const [triggerGetUsers] = useLazyGetAdminUsersQuery();
  const [triggerGetAssignmentById, { isFetching: isFetchingAssignmentDetails }] =
    useLazyGetSupervisorAssignmentByIdQuery();
  const [createAssignment, { isLoading: isCreatingAssignment }] =
    useCreateSupervisorAssignmentMutation();
  const [updateAssignment, { isLoading: isUpdatingAssignment }] =
    useUpdateSupervisorAssignmentMutation();
  const [deleteAssignment] = useDeleteSupervisorAssignmentMutation();

  const {
    data: assignmentsResponse = [],
    isLoading,
    isFetching,
    refetch,
  } = useGetSupervisorAssignmentsQuery({
    search: appliedSearchQuery.trim() || undefined,
    requiredMeetingFrequency: filter === "All" ? undefined : filter.toUpperCase().replace("-", ""),
  });

  const data = useMemo(
    () => assignmentsResponse.map(mapApiAssignmentToRow),
    [assignmentsResponse],
  );

  useEffect(() => {
    setSupervisorCounts?.(assignmentsResponse.length);
  }, [assignmentsResponse.length, setSupervisorCounts]);

  useEffect(() => {
    void triggerGetUsers({ page: 1, pageSize: 200, role: "SUPERVISOR" })
      .unwrap()
      .then((response) => {
        setSupervisorOptions(
          response.items.map((user) => ({
            value: String(user.id),
            label: user.fullName?.trim() || user.email || user.username,
          })),
        );
      })
      .catch((error) => {
        setSupervisorOptions([]);
        setToastMessage(getApiErrorMessage(error));
      });

    void triggerGetUsers({ page: 1, pageSize: 200, role: "THERAPIST" })
      .unwrap()
      .then((response) => {
        setTherapistOptions(
          response.items.map((user) => ({
            value: String(user.id),
            label: user.fullName?.trim() || user.email || user.username,
          })),
        );
      })
      .catch((error) => {
        setTherapistOptions([]);
        setToastMessage(getApiErrorMessage(error));
      });
  }, [triggerGetUsers]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setAppliedSearchQuery(searchQuery);
      setDisplayedItems(20);
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [searchQuery]);

  const resetKey = JSON.stringify([filter, assignmentsResponse.length]);
  const [previousResetKey, setPreviousResetKey] = useState(resetKey);
  if (previousResetKey !== resetKey) {
    setPreviousResetKey(resetKey);
    setDisplayedItems(20);
  }

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const handleLoadMore = useCallback(() => {
    setIsLoadingMore(true);
    setDisplayedItems((prev) => prev + itemsPerPage);
    setIsLoadingMore(false);
  }, []);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: displayedItems < data.length,
    isLoading: isLoadingMore,
  });

  const currentData = data.slice(0, displayedItems);

  const openCreateModal = useMemo(() => Boolean(isAssignModalOpen), [isAssignModalOpen]);

  const handleDeleteClick = (id: string) => {
    setAssignmentToDelete(Number.parseInt(id, 10));
    setIsDeleteModalOpen(true);
  };

  const handleEditClick = async (id: string) => {
    const assignmentId = Number.parseInt(id, 10);
    setEditingAssignmentId(assignmentId);
    setModalInitialValues(null);
    setIsEditModalOpen(true);

    try {
      const assignment = await triggerGetAssignmentById(assignmentId).unwrap();
      const supervisorOption = {
        value: String(assignment.supervisor.id),
        label:
          assignment.supervisor.fullName ||
          assignment.supervisor.email ||
          assignment.supervisor.username ||
          `Supervisor #${assignment.supervisor.id}`,
      };
      const therapistOption = {
        value: String(assignment.therapist.id),
        label:
          assignment.therapist.fullName ||
          assignment.therapist.email ||
          assignment.therapist.username ||
          `Therapist #${assignment.therapist.id}`,
      };

      setSupervisorOptions((prev) => upsertSelectOption(prev, supervisorOption));
      setTherapistOptions((prev) => upsertSelectOption(prev, therapistOption));
      setModalInitialValues({
        supervisorId: supervisorOption.value,
        therapistId: therapistOption.value,
        assignmentType: (assignment.assignmentType || "PRIMARY").toUpperCase(),
        startDate: formatDateForInput(assignment.startDate),
        endDate: formatDateForInput(assignment.endDate),
        frequency: (assignment.requiredMeetingFrequency || "WEEKLY").toUpperCase(),
        notes: assignment.notes || "",
      });
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
      setIsEditModalOpen(false);
      setEditingAssignmentId(null);
      setModalInitialValues(null);
    }
  };

  const handleConfirmDelete = async () => {
    if (!assignmentToDelete) return;

    try {
      await deleteAssignment(assignmentToDelete).unwrap();
      setToastVariant("success");
      setToastMessage("Supervisor assignment deleted successfully.");
      setIsDeleteModalOpen(false);
      setAssignmentToDelete(null);
      void refetch();
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleCreateAssignment = async (values: AssignSupervisorFormValues) => {
    try {
      await createAssignment({
        supervisorId: Number.parseInt(values.supervisorId, 10),
        therapistId: Number.parseInt(values.therapistId, 10),
        assignmentType: values.assignmentType || "PRIMARY",
        startDate: values.startDate || undefined,
        endDate: values.endDate || null,
        requiredMeetingFrequency: values.frequency,
        notes: values.notes || undefined,
      }).unwrap();
      setToastVariant("success");
      setToastMessage("Supervisor assigned successfully.");
      setIsAssignModalOpen?.(false);
      setModalInitialValues(null);
      void refetch();
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleUpdateAssignment = async (values: AssignSupervisorFormValues) => {
    if (!editingAssignmentId) return;

    try {
      await updateAssignment({
        id: editingAssignmentId,
        body: {
          assignmentType: values.assignmentType || "PRIMARY",
          startDate: values.startDate || undefined,
          endDate: values.endDate || null,
          requiredMeetingFrequency: values.frequency || undefined,
          notes: values.notes || undefined,
        },
      }).unwrap();
      setToastVariant("success");
      setToastMessage("Supervisor assignment updated successfully.");
      setIsEditModalOpen(false);
      setEditingAssignmentId(null);
      setModalInitialValues(null);
      void refetch();
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const columnCount = canManageAssignments ? 5 : 4;

  return (
    <div className="flex h-full min-h-0 flex-col">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastVariant}
          onClose={() => setToastMessage(null)}
          duration={2500}
        />
      ) : null}

      <div className="mb-4 flex shrink-0 items-center gap-3">
        <div className="max-w-sm w-full">
          <CustomInput
            placeholder="Search users by name, username, or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="rounded-full min-h-10 w-full pb-0 pt-1.75 bg-white shadow-xs border-(--neutral-100)"
          />
        </div>

        <div>
          <CustomSelect
            options={FREQUENCY_OPTIONS}
            value={filter}
            onChange={(value) => setFilter(value as string)}
            placeholder="All frequencies"
            className="rounded-full max-h-10 pb-0 pt-0 bg-white shadow-xs border-(--neutral-100)"
            isSearch={false}
            contentClassName="min-w-fit"
          />
        </div>
      </div>
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-[0px_2px_2px_0px_var(--shadow)]">
        <div className="min-h-0 flex-1 overflow-auto overscroll-contain">
          <table className="w-full text-left border-collapse">
            <thead className="sticky top-0 z-10 bg-(--bg-primary-50)">
              <tr className="h-11.5 border-b border-(--neutral-100)">
              <th className="px-4 py-3 text-[0.875rem] font-semibold text-(--text-primary-dark)">
                Supervisor <span className="inline-block ml-1">↓</span>
              </th>
              <th className="px-4 py-3 text-[0.875rem] font-semibold text-(--text-primary-dark)">
                Therapist <span className="inline-block ml-1">↓</span>
              </th>
              <th className="px-4 py-3 text-center text-sm font-semibold text-(--text-primary-dark)">
                Meeting Frequency
              </th>
              <th className="px-4 py-3 text-center text-sm font-semibold text-(--text-primary-dark)">
                Assigned Date
              </th>
              {canManageAssignments ? (
                <th className="px-4 py-3 text-center text-sm font-semibold text-(--text-primary-dark)">
                  Action
                </th>
              ) : null}
            </tr>
          </thead>
          <tbody>
            {(isLoading || isFetching) && currentData.length === 0 ? (
              <tr>
                <td colSpan={columnCount} className="px-4 py-16">
                  <ContentLoader className="min-h-48" />
                </td>
              </tr>
            ) : null}
            {!isLoading && !isFetching && currentData.length === 0 ? (
              <tr>
                <td colSpan={columnCount} className="px-4 py-5 text-center text-sm font-medium text-(--text-neutral-600)">
                  No supervisor assignments found.
                </td>
              </tr>
            ) : null}
            {currentData.map((record) => (
              <tr
                key={record.id}
                className="h-18 border-t border-(--neutral-100) transition-colors hover:bg-(--neutral-50)/30"
              >
                <td className="px-4 py-3 text-sm font-medium text-(--text-primary-dark)">
                  {record.supervisor}
                </td>
                <td className="px-4 py-3 text-sm font-medium text-(--text-primary-dark)">
                  {record.therapist}
                </td>
                <td className="px-4 py-3 text-center">
                  <div className="flex justify-center">
                    <Badge
                      variant="outline"
                      className="w-fit h-6 px-3 text-[0.75rem] font-medium rounded-full shadow-none border bg-(--neutral-50) text-(--text-neutral-600) border-(--neutral-100)"
                    >
                      {record.meetingFrequency}
                    </Badge>
                  </div>
                </td>
                <td className="px-4 py-3 text-center text-sm font-medium text-(--text-primary-dark)">
                  {record.assignedDate}
                </td>
                {canManageAssignments ? (
                  <td className="px-4 py-3 text-center">
                    <div className="flex justify-center gap-1">
                      <button
                        onClick={() => void handleEditClick(record.id)}
                        className="p-2 text-(--text-primary-dark) hover:text-(--bg-primary-dark) transition-colors cursor-pointer"
                      >
                        <Pencil size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteClick(record.id)}
                        className="p-2 text-(--text-primary-dark) hover:text-(--status-denied) transition-colors cursor-pointer"
                      >
                        <TrashIcon size={18} />
                      </button>
                    </div>
                  </td>
                ) : null}
              </tr>
            ))}
          </tbody>
          </table>

          <div ref={observerTarget} className="flex h-10 w-full items-center justify-center">
            {isLoadingMore && <ContentLoader variant="inline" size="md" />}
          </div>
        </div>
      </div>

      <DeleteAssignmentModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={() => void handleConfirmDelete()}
      />

      <AssignSupervisorModal
        isOpen={openCreateModal}
        onClose={() => setIsAssignModalOpen?.(false)}
        onAssign={(values) => void handleCreateAssignment(values)}
        supervisorOptions={supervisorOptions}
        therapistOptions={therapistOptions}
        assignmentTypeOptions={assignmentTypeOptions}
        frequencyOptions={frequencyOptions}
        isSubmitting={isCreatingAssignment}
      />

      <AssignSupervisorModal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingAssignmentId(null);
          setModalInitialValues(null);
        }}
        onAssign={(values) => void handleUpdateAssignment(values)}
        title="Edit Supervisor Assignment"
        submitLabel="Update"
        supervisorOptions={supervisorOptions}
        therapistOptions={therapistOptions}
        assignmentTypeOptions={assignmentTypeOptions}
        frequencyOptions={frequencyOptions}
        initialValues={modalInitialValues}
        isEdit={true}
        isLoadingDetails={isFetchingAssignmentDetails}
        isSubmitting={isUpdatingAssignment}
      />
    </div>
  );
};

export default SupervisorAssignmentTable;
