import { useEffect, useMemo, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { X } from "lucide-react";
import { Button } from "../ui/button";
import { Form } from "@/components/ui/form";
import CustomSelect from "@/components/form/CustomSelect";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import Toast from "@/components/shared/Toast";
import { useLazyGetAdminClientsQuery } from "@/store/api/admin/clients.api";
import { useLazyGetAdminUsersQuery } from "@/store/api/admin/users.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { useTaskSystemOptions } from "@/hooks/useSystemOptionCatalog";
import { toSelectOptions } from "@/utils/systemOptions";
import {
  buildTaskTitlePayload,
  CUSTOM_TASK_TITLE_VALUE,
} from "@/utils/taskForm";
import {
  TaskLockedClientField,
  TaskModalClientHeading,
} from "@/components/shared/TaskModalClientDisplay";

interface CreateTaskData {
  title: string;
  titleKey?: string;
  taskType?: string;
  clientId: number;
  assignedToId?: number;
  priority: string;
  status: string;
  description?: string;
  dueDate?: string;
}

const CUSTOM_TASK_TITLE_MAX_LENGTH = 80;
const TASK_DESCRIPTION_MAX_LENGTH = 300;
const TASK_DUE_DATE_MIN_YEAR = 2010;

const createTaskSchema = z
  .object({
    titleType: z.string().min(1, "Please select a title type"),
    customTitle: z
      .string()
      .max(
        CUSTOM_TASK_TITLE_MAX_LENGTH,
        `Custom title cannot exceed ${CUSTOM_TASK_TITLE_MAX_LENGTH} characters`,
      )
      .optional(),
    clientId: z.string().min(1, "Please select a client"),
    assigneeId: z.string().optional(),
    taskType: z.string().optional(),
    priority: z.string().min(1, "Please select a priority"),
    status: z.string().min(1, "Please select a status"),
    description: z
      .string()
      .max(
        TASK_DESCRIPTION_MAX_LENGTH,
        `Description must be ${TASK_DESCRIPTION_MAX_LENGTH} characters or less`,
      )
      .optional(),
    dueDate: z.string().optional(),
  })
  .refine(
    (data) => {
      if (
        data.titleType === CUSTOM_TASK_TITLE_VALUE &&
        (!data.customTitle || data.customTitle.trim() === "")
      ) {
        return false;
      }
      return true;
    },
    {
      message: "Custom title is required",
      path: ["customTitle"],
    },
  )
  .refine(
    (data) => {
      if (!data.dueDate) return true;
      const parsed = new Date(data.dueDate);
      if (Number.isNaN(parsed.getTime())) return false;
      const endOfDay = new Date(parsed);
      endOfDay.setHours(23, 59, 59, 999);
      return endOfDay.getTime() > Date.now();
    },
    {
      message: "Due date must be in the future",
      path: ["dueDate"],
    },
  );

type CreateTaskFormValues = z.infer<typeof createTaskSchema>;

interface CreateTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (taskData: CreateTaskData) => Promise<void> | void;
  isSubmitting?: boolean;
  client?: {
    id: string;
    name: string;
    clientId?: string;
    assignedTherapistId?: string;
  };
}

const CreateTaskModal = ({
  isOpen,
  onClose,
  onCreate,
  isSubmitting = false,
  client,
}: CreateTaskModalProps) => {
  const maxDueDate = useMemo(() => {
    const today = new Date();
    return new Date(
      today.getFullYear() + 10,
      today.getMonth(),
      today.getDate(),
    );
  }, []);

  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [triggerGetClients, { isFetching: isFetchingClients }] = useLazyGetAdminClientsQuery();
  const [triggerGetUsers, { isFetching: isFetchingUsers }] = useLazyGetAdminUsersQuery();
  const taskOptions = useTaskSystemOptions(isOpen);
  const resolvedClientId = client?.id ?? "";
  const [clientOptions, setClientOptions] = useState<CustomSelectOption[]>([]);
  const [assigneeOptions, setAssigneeOptions] = useState<CustomSelectOption[]>([]);
  const [clientsPage, setClientsPage] = useState(0);
  const [clientsTotalPages, setClientsTotalPages] = useState(1);
  const [assigneesPage, setAssigneesPage] = useState(0);
  const [assigneesTotalPages, setAssigneesTotalPages] = useState(1);
  const [clientTherapistMap, setClientTherapistMap] = useState<Record<string, string>>({});

  const defaultValues = useMemo<Partial<CreateTaskFormValues>>(
    () => ({
      titleType: "",
      customTitle: "",
      clientId: resolvedClientId,
      assigneeId: client?.assignedTherapistId ?? "",
      taskType: "",
      priority: taskOptions.getDefaultPriority(),
      status: taskOptions.getDefaultStatus(),
      description: "",
    }),
    [
      resolvedClientId,
      client?.assignedTherapistId,
      taskOptions.priorityOptions,
      taskOptions.statusOptions,
    ],
  );

  const form = useForm<CreateTaskFormValues>({
    resolver: zodResolver(createTaskSchema),
    defaultValues,
    mode: "onChange",
  });

  const { watch } = form;
  const titleType = watch("titleType");
  const selectedClientId = watch("clientId");
  
  useEffect(() => {
    if (selectedClientId) {
      if (selectedClientId === client?.id && client?.assignedTherapistId) {
        form.setValue("assigneeId", client.assignedTherapistId);
      } else if (clientTherapistMap[selectedClientId]) {
        form.setValue("assigneeId", clientTherapistMap[selectedClientId]);
      }
    }
  }, [selectedClientId, clientTherapistMap, client?.id, client?.assignedTherapistId, form]);

  const hasMoreClients = clientsPage < clientsTotalPages;
  const hasMoreAssignees = assigneesPage < assigneesTotalPages;
  const wasOpenRef = useRef(false);
  const clientsInitialLoadDoneRef = useRef(false);
  const assigneesInitialLoadDoneRef = useRef(false);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    if (!isOpen) {
      if (wasOpenRef.current) {
        wasOpenRef.current = false;
        setClientOptions([]);
        setAssigneeOptions([]);
        setClientsPage(0);
        setClientsTotalPages(1);
        setAssigneesPage(0);
        setAssigneesTotalPages(1);
        clientsInitialLoadDoneRef.current = false;
        assigneesInitialLoadDoneRef.current = false;
      }
      return;
    }

    wasOpenRef.current = true;
    form.reset(defaultValues);
  }, [defaultValues, form, isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    if (
      !resolvedClientId &&
      !clientsInitialLoadDoneRef.current &&
      !isFetchingClients
    ) {
      clientsInitialLoadDoneRef.current = true;
      void loadClients(1);
    }
    if (!assigneesInitialLoadDoneRef.current && !isFetchingUsers) {
      assigneesInitialLoadDoneRef.current = true;
      void loadAssignees(1);
    }
  }, [isOpen, resolvedClientId, isFetchingClients, isFetchingUsers]);

  const taskTitleOptions = useMemo(
    () => [
      ...toSelectOptions(taskOptions.titleOptions),
      { value: CUSTOM_TASK_TITLE_VALUE, label: "Other (Custom Title)" },
    ],
    [taskOptions.titleOptions],
  );

  const taskTypeOptions = useMemo(
    () => toSelectOptions(taskOptions.taskTypeOptions),
    [taskOptions.taskTypeOptions],
  );

  const priorityOptions = useMemo(
    () => toSelectOptions(taskOptions.priorityOptions),
    [taskOptions.priorityOptions],
  );

  const statusOptions = useMemo(
    () => toSelectOptions(taskOptions.statusOptions),
    [taskOptions.statusOptions],
  );

  const loadClients = async (page: number) => {
    try {
      const response = await triggerGetClients({
        page,
        pageSize: 25,
        sortBy: "fullName",
        sortOrder: "asc",
      }).unwrap();

      const nextOptions = response.items.map((client) => ({
        value: String(client.id),
        label: `${client.fullName}${client.clientId ? ` (${client.clientId})` : ""}`,
      }));

      setClientTherapistMap((prev) => {
        const next = { ...prev };
        response.items.forEach((c) => {
          if (c.assignedTherapistId) {
            next[String(c.id)] = String(c.assignedTherapistId);
          }
        });
        return next;
      });

      setClientOptions((prev) => {
        const existing = new Set(prev.map((entry) => entry.value));
        const merged = [...prev];
        nextOptions.forEach((entry) => {
          if (!existing.has(entry.value)) merged.push(entry);
        });
        return merged;
      });
      setClientsPage(response.page);
      setClientsTotalPages(response.totalPages || 1);
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const loadAssignees = async (page: number) => {
    try {
      const response = await triggerGetUsers({
        page,
        pageSize: 25,
      }).unwrap();

      const nextOptions = response.items
        .filter((user) =>
          user.roles.some((role) => role === "THERAPIST" || role === "SUPERVISOR"),
        )
        .map((user) => ({
          value: String(user.id),
          label: user.fullName?.trim() || user.email || user.username,
        }));

      setAssigneeOptions((prev) => {
        const existing = new Set(prev.map((entry) => entry.value));
        const merged = [...prev];
        nextOptions.forEach((entry) => {
          if (!existing.has(entry.value)) merged.push(entry);
        });
        return merged;
      });
      setAssigneesPage(response.page);
      setAssigneesTotalPages(response.totalPages || 1);
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const onSubmit = async (data: CreateTaskFormValues) => {
    const titlePayload = buildTaskTitlePayload(
      data.titleType,
      data.customTitle,
      taskOptions.titleOptions,
    );
    const newTask: CreateTaskData = {
      ...titlePayload,
      taskType: data.taskType?.trim() || undefined,
      clientId: Number(data.clientId),
      assignedToId: data.assigneeId ? Number(data.assigneeId) : undefined,
      priority: data.priority,
      status: data.status,
      description: data.description,
      dueDate: data.dueDate ? new Date(data.dueDate).toISOString() : undefined,
    };
    await onCreate(newTask);
  };

  if (!isOpen) return null;

  return (
    <div className="app-modal-overlay fixed inset-0 z-[60] flex items-center justify-center p-4">
      <div className="fixed inset-0" onClick={onClose} />
      <div className="app-modal-surface relative flex max-h-[calc(100dvh-2rem)] w-full max-w-[36.6875rem] flex-col overflow-hidden rounded-3xl">
        {toastMessage ? (
          <Toast
            message={toastMessage}
            type="error"
            onClose={() => setToastMessage(null)}
          />
        ) : null}
        {/* Header */}
        <div className="flex shrink-0 items-start justify-between gap-4 px-6 py-5">
          <div className="min-w-0 flex-1 pr-2">
            <TaskModalClientHeading action="Create" name={client?.name} />
            <p className="mt-2 text-sm leading-[1.375rem] text-[#5B616E]">
              Create a new task and assign it to a client and therapist
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            aria-label="Close create task"
            className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-full text-[#1B1C20] transition-colors hover:bg-[#F6F6F6] disabled:cursor-not-allowed disabled:opacity-50"
          >
            <X size={24} />
          </button>
        </div>

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            className="flex min-h-0 flex-1 flex-col overflow-hidden"
          >
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-6 pt-3 pb-5">
              <div className="space-y-4">
              {/* Title Section */}
              <div className="space-y-4">
                <CustomSelect
                  control={form.control}
                  name="titleType"
                  label="Task Title"
                  required
                  options={taskTitleOptions}
                  isSearch
                  placeholder="Select Title"
                  className="rounded-2xl border-[#D8DBDF] shadow-none"
                />

                {titleType === CUSTOM_TASK_TITLE_VALUE && (
                  <>
                    <CustomInput
                      control={form.control}
                      name="customTitle"
                      label="Custom title"
                      required
                      maxLength={CUSTOM_TASK_TITLE_MAX_LENGTH}
                      hint="Used when the task title is not available in the list."
                      className="rounded-2xl border-[#D8DBDF] shadow-none"
                    />
                  </>
                )}
              </div>

              {/* Description */}
              <CustomTextarea
                control={form.control}
                name="description"
                label="Description"
                characterLimit={TASK_DESCRIPTION_MAX_LENGTH}
                className="min-h-[8.125rem] rounded-2xl border-[#D8DBDF] shadow-none"
              />

              <CustomSelect
                control={form.control}
                name="taskType"
                label="Task Type"
                options={taskTypeOptions}
                placeholder="Select type (optional)"
                isSearch
                className="rounded-2xl border-[#D8DBDF] shadow-none"
              />

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {/* Client */}
                {resolvedClientId ? (
                  <TaskLockedClientField
                    name={client?.name}
                    clientId={client?.clientId}
                    className="rounded-2xl border-[#D8DBDF]"
                  />
                ) : (
                  <CustomSelect
                    control={form.control}
                    name="clientId"
                    label="Client"
                    required
                    options={clientOptions}
                    isSearch
                    placeholder="Select Client"
                    wrapSelectedLabel
                    onMenuScrollToEnd={() => {
                      if (hasMoreClients && !isFetchingClients) {
                        void loadClients(clientsPage + 1);
                      }
                    }}
                    hasMore={hasMoreClients}
                    isLoadingMore={isFetchingClients && clientOptions.length > 0}
                    loadingMoreLabel="Loading more clients..."
                    className="rounded-2xl border-[#D8DBDF] shadow-none"
                  />
                )}

                {/* Assignee */}
                <CustomSelect
                  control={form.control}
                  name="assigneeId"
                  label="Assigned to"
                  options={assigneeOptions}
                  isSearch
                  placeholder="Select"
                  disabled={true}
                  onMenuScrollToEnd={() => {
                    if (hasMoreAssignees && !isFetchingUsers) {
                      void loadAssignees(assigneesPage + 1);
                    }
                  }}
                  hasMore={hasMoreAssignees}
                  isLoadingMore={isFetchingUsers && assigneeOptions.length > 0}
                  loadingMoreLabel="Loading more assignees..."
                  className="rounded-2xl border-[#D8DBDF] shadow-none"
                />
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                {/* Priority */}
                <CustomSelect
                  control={form.control}
                  name="priority"
                  label="Priority"
                  options={priorityOptions}
                  isSearch={false}
                  className="rounded-2xl border-[#D8DBDF] shadow-none"
                />

                {/* Status */}
                <CustomSelect
                  control={form.control}
                  name="status"
                  label="Status"
                  options={statusOptions}
                  isSearch={false}
                  className="rounded-2xl border-[#D8DBDF] shadow-none"
                />

                {/* Due Date */}
                <CustomDatePicker
                  control={form.control}
                  name="dueDate"
                  label="Due Date"
                  disablePast
                  minYear={TASK_DUE_DATE_MIN_YEAR}
                  maxYear={maxDueDate.getFullYear()}
                  maxDate={maxDueDate}
                  className="rounded-2xl border-[#D8DBDF] shadow-none"
                />
              </div>

              </div>
            </div>

            {/* Footer Actions */}
            <div className="flex shrink-0 items-center justify-end gap-3 px-6 py-5">
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={onClose}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="lg"
                disabled={isSubmitting || !form.formState.isValid}
                loading={isSubmitting}
                loadingLabel="Creating..."
              >
                Create task
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default CreateTaskModal;
