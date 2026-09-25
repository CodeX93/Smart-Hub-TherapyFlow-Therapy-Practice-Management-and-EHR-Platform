import { useEffect, useMemo, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import CustomSelect from "@/components/form/CustomSelect";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import { useLazyGetAdminClientsQuery } from "@/store/api/admin/clients.api";
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
      if (data.titleType !== CUSTOM_TASK_TITLE_VALUE) return true;
      return Boolean(data.customTitle?.trim());
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
  const taskOptions = useTaskSystemOptions(isOpen);
  const resolvedClientId = client?.id ?? "";
  const [clientOptions, setClientOptions] = useState<CustomSelectOption[]>([]);
  const [clientsPage, setClientsPage] = useState(0);
  const [clientsTotalPages, setClientsTotalPages] = useState(1);

  const defaultValues = useMemo<Partial<CreateTaskFormValues>>(
    () => ({
      titleType: "",
      customTitle: "",
      clientId: resolvedClientId,
      taskType: "",
      priority: taskOptions.getDefaultPriority(),
      status: taskOptions.getDefaultStatus(),
      description: "",
      dueDate: "",
    }),
    [
      resolvedClientId,
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
  const hasMoreClients = clientsPage < clientsTotalPages;
  const wasOpenRef = useRef(false);
  const clientsInitialLoadDoneRef = useRef(false);

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
        setClientsPage(0);
        setClientsTotalPages(1);
        clientsInitialLoadDoneRef.current = false;
      }
      return;
    }

    wasOpenRef.current = true;
    form.reset(defaultValues);
  }, [defaultValues, form, isOpen]);

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

  useEffect(() => {
    if (!isOpen) return;
    if (resolvedClientId) return;
    if (clientsInitialLoadDoneRef.current || isFetchingClients) return;
    clientsInitialLoadDoneRef.current = true;
    void loadClients(1);
  }, [isFetchingClients, isOpen, resolvedClientId]);

  const onSubmit = async (data: CreateTaskFormValues) => {
    const titlePayload = buildTaskTitlePayload(
      data.titleType,
      data.customTitle,
      taskOptions.titleOptions,
    );

    await onCreate({
      ...titlePayload,
      taskType: data.taskType?.trim() || undefined,
      clientId: Number(data.clientId),
      priority: data.priority,
      status: data.status,
      description: data.description?.trim() || undefined,
      dueDate: data.dueDate ? new Date(data.dueDate).toISOString() : undefined,
    });
  };

  if (!isOpen) return null;

  return (
    <div className="app-modal-overlay fixed inset-0 z-[60] flex items-center justify-center p-4">
      <div className="fixed inset-0" onClick={onClose} />
      <div className="app-modal-surface relative flex max-h-[calc(100dvh-2rem)] w-full max-w-[36.6875rem] flex-col overflow-hidden rounded-3xl">
        {toastMessage ? (
          <div className="absolute right-4 top-4 z-[80] rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-3 py-2 text-xs font-medium text-[#b42318] shadow-[0_12px_24px_rgba(15,23,42,0.10)]">
            {toastMessage}
          </div>
        ) : null}
        <div className="flex shrink-0 items-start justify-between gap-4 px-6 py-5">
          <div className="min-w-0 flex-1 pr-2">
            <TaskModalClientHeading action="Create" name={client?.name} />
            <p className="mt-2 text-sm leading-[1.375rem] text-[#5B616E]">
              Create a new task for a client
            </p>
          </div>
          <button
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

                {titleType === CUSTOM_TASK_TITLE_VALUE ? (
                  <CustomInput
                    control={form.control}
                    name="customTitle"
                    label="Custom title"
                    required
                    maxLength={CUSTOM_TASK_TITLE_MAX_LENGTH}
                    hint="Used when the task title is not available in the list."
                    className="rounded-2xl border-[#D8DBDF] shadow-none"
                  />
                ) : null}
              </div>

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

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <CustomSelect
                  control={form.control}
                  name="priority"
                  label="Priority"
                  options={priorityOptions}
                  isSearch={false}
                  className="rounded-2xl border-[#D8DBDF] shadow-none"
                />

                <CustomSelect
                  control={form.control}
                  name="status"
                  label="Status"
                  options={statusOptions}
                  isSearch={false}
                  className="rounded-2xl border-[#D8DBDF] shadow-none"
                />

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
