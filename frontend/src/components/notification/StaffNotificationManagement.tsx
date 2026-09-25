
import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AlertCircle, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomSelect from "@/components/form/CustomSelect";
import NotificationListing from "@/components/notification/NotificationListing";
import ExpandableText from "@/components/shared/ExpandableText";
import DeleteConfirmationModal from "@/components/settings/DeleteConfirmationModal";
import { getApiErrorMessage } from "@/utils/apiError";
import { useLazyGetAdminClientsQuery } from "@/store/api/admin/clients.api";
import {
  useCreateStaffNotificationMutation,
  useBroadcastStaffNotificationMutation,
  useCreateStaffNotificationTemplateMutation,
  useCreateStaffNotificationTriggerMutation,
  useDeleteStaffNotificationMutation,
  useDeleteStaffNotificationTemplateMutation,
  useDeleteStaffNotificationTriggerMutation,
  useGetStaffNotificationPreferencesQuery,
  useGetStaffNotificationActionMetadataQuery,
  useGetStaffNotificationStatsQuery,
  useGetStaffNotificationUnreadCountQuery,
  useGetStaffNotificationEventCatalogQuery,
  useGetStaffNotificationTemplatesQuery,
  useGetStaffNotificationTriggersQuery,
  useGetStaffNotificationsPaginatedQuery,
  useMarkAllStaffNotificationsReadMutation,
  useReadStaffNotificationMutation,
  useUpdateStaffNotificationPreferenceMutation,
  useUpdateStaffNotificationTemplateMutation,
  useUpdateStaffNotificationTriggerMutation,
  type StaffNotificationItem,
  type StaffNotificationPreference,
  type StaffNotificationTemplate,
  type StaffNotificationTemplatePayload,
  type StaffNotificationTrigger,
  type StaffNotificationTriggerPayload,
} from "@/store/api/admin/notifications.api";
import {
  formatNotificationMessage,
  formatNotificationTimestamp,
} from "@/utils/notificationDisplay";
import {
  resolveNotificationTemplateChannelType,
  resolveNotificationTemplateEventType,
} from "@/utils/notificationTemplates";
import type { Notification } from "@/types/notification";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  NOTIFICATION_TRIGGER_LIMITS,
  notificationTriggerSchema,
} from "@/schemas/notification.schema";
import {
  ENTITY_TYPE_OPTIONS,
  EVENT_TYPE_FALLBACK_KEYS,
  EVENT_TYPE_OPTIONS,
} from "./notification.static";

type TabId =
  | "notifications"
  | "event-catalog"
  | "triggers"
  | "templates"
  | "preferences";

type TriggerFormState = {
  name: string;
  description: string;
  eventType: string;
  entityType: string;
  conditionRules: string;
  recipientRules: string;
  priority: string;
  isScheduled: boolean;
  scheduleOffsetMinutes: string;
  batchWindowMinutes: string;
  maxBatchSize: string;
  isActive: boolean;
};

type TemplateFormState = {
  name: string;
  type: string;
  eventType: string;
  subject: string;
  bodyTemplate: string;
  isSystem: boolean;
  isActive: boolean;
};

const baseTabs: { id: TabId; label: string }[] = [
  { id: "notifications", label: "Notifications" },
  { id: "event-catalog", label: "Event Catalog" },
  { id: "triggers", label: "Triggers" },
  { id: "templates", label: "Templates" },
  { id: "preferences", label: "Preferences" },
];

const notificationTypeOptions = [
  "APPOINTMENT_REMINDER",
  "APPOINTMENT_CONFIRMED",
  "APPOINTMENT_CANCELLED",
  "APPOINTMENT_RESCHEDULED",
  "APPOINTMENT_24H_REMINDER",
  "APPOINTMENT_1H_REMINDER",
  "FORM_ASSIGNED",
  "FORM_DUE_SOON",
  "FORM_OVERDUE",
  "FORM_SUBMITTED",
  "FORM_REVIEWED",
  "DOCUMENT_SHARED",
  "DOCUMENT_UPDATED",
  "SESSION_NOTES_AVAILABLE",
  "PROGRESS_REPORT_AVAILABLE",
  "PAYMENT_DUE",
  "PAYMENT_OVERDUE",
  "PAYMENT_RECEIVED",
  "PAYMENT_FAILED",
  "INVOICE_GENERATED",
  "PORTAL_ACCESS_GRANTED",
  "PASSWORD_RESET_REQUESTED",
  "PASSWORD_CHANGED",
  "ACCOUNT_LOCKED",
  "ACCOUNT_UNLOCKED",
  "NEW_MESSAGE",
  "MESSAGE_REPLY",
  "CRISIS_RESOURCES_SHARED",
  "EMERGENCY_CONTACT_UPDATED",
  "SYSTEM_MAINTENANCE",
  "SYSTEM_UPGRADE",
  "POLICY_UPDATE",
  "INSURANCE_VERIFICATION_NEEDED",
  "INSURANCE_AUTHORIZATION_EXPIRING",
  "INSURANCE_CLAIM_PROCESSED",
] as const;

const notificationCategoryOptions = [
  "APPOINTMENT",
  "FORM",
  "DOCUMENT",
  "SESSION",
  "BILLING",
  "ACCOUNT",
  "SECURITY",
  "MESSAGE",
  "EMERGENCY",
  "SYSTEM",
  "INSURANCE",
] as const;

const notificationPriorityOptions = ["LOW", "MEDIUM", "HIGH", "URGENT"] as const;
const notificationTargetTypeOptions = ["BOTH", "USERS", "CLIENTS"] as const;

function normalizeTriggerPriorityForForm(priority?: string | null): string {
  const normalized = (priority ?? "").trim().toUpperCase();
  return normalized || "HIGH";
}

function buildTriggerPriorityOptions(currentPriority?: string): CustomSelectOption[] {
  const base = notificationPriorityOptions.map((priority) => ({
    label: priority,
    value: priority,
  }));
  const normalizedCurrent = normalizeTriggerPriorityForForm(currentPriority);
  if (base.some((option) => option.value === normalizedCurrent)) {
    return base;
  }

  return [
    ...base,
    {
      label: normalizedCurrent,
      value: normalizedCurrent,
    },
  ];
}

const emptyTriggerForm: TriggerFormState = {
  name: "",
  description: "",
  eventType: "",
  entityType: "CLIENT",
  conditionRules: "{}",
  recipientRules: "{}",
  priority: "HIGH",
  isScheduled: false,
  scheduleOffsetMinutes: "0",
  batchWindowMinutes: "5",
  maxBatchSize: "10",
  isActive: true,
};

const emptyTemplateForm: TemplateFormState = {
  name: "",
  type: "IN_APP",
  eventType: "",
  subject: "",
  bodyTemplate: "",
  isSystem: false,
  isActive: true,
};

function formatTimestamp(value: string): string {
  return formatNotificationTimestamp(value);
}

function humanizeNotificationLabel(value: string): string {
  return value
    .split("_")
    .filter(Boolean)
    .map((part) => {
      const lower = part.toLowerCase();
      if (lower === "sms") return "SMS";
      return lower.charAt(0).toUpperCase() + lower.slice(1);
    })
    .join(" ");
}

function formatNotificationDisplayLabel(value: string): string {
  if (!value) return value;
  const trimmed = value.trim();
  const isBackendKey = trimmed.includes("_") && !trimmed.includes(" ");
  return isBackendKey ? humanizeNotificationLabel(trimmed) : trimmed;
}

function parseDateTimeLocalToIso(value?: string): string | undefined {
  if (!value) return undefined;
  if (isPastDateTimeLocal(value)) {
    throw new Error("Expiration date and time must be in the future.");
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    throw new Error("Please enter a valid expiration date and time.");
  }
  return parsed.toISOString();
}

function formatDateTimeLocal(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function getMinExpiresAt(): string {
  const now = new Date();
  now.setSeconds(0, 0);
  return formatDateTimeLocal(now);
}

function isPastDateTimeLocal(value: string): boolean {
  if (!value) return false;
  return value < getMinExpiresAt();
}

function parseJsonOrFallback(raw: string): string {
  if (!raw.trim()) return "{}";
  try {
    return JSON.stringify(JSON.parse(raw));
  } catch {
    return raw;
  }
}

interface StaffNotificationManagementProps {
  isTherapistView?: boolean;
  isStaffView?: boolean;
}

const StaffNotificationManagement = ({
  isTherapistView = false,
  isStaffView = false,
}: StaffNotificationManagementProps) => {
  const inboxOnlyView = isTherapistView || isStaffView;
  const expiresAtInputRef = useRef<HTMLInputElement | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("notifications");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [notificationsPage, setNotificationsPage] = useState(1);
  const [allNotifications, setAllNotifications] = useState<StaffNotificationItem[]>([]);

  const [showTriggerEditor, setShowTriggerEditor] = useState(false);
  const [editingTrigger, setEditingTrigger] = useState<StaffNotificationTrigger | null>(null);
  const [triggerForm, setTriggerForm] = useState<TriggerFormState>(emptyTriggerForm);
  const [triggerFormErrors, setTriggerFormErrors] = useState<Record<string, string>>({});

  const [showTemplateEditor, setShowTemplateEditor] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<StaffNotificationTemplate | null>(null);
  const [templateToDelete, setTemplateToDelete] =
    useState<StaffNotificationTemplate | null>(null);
  const [isDeleteTemplateModalOpen, setIsDeleteTemplateModalOpen] = useState(false);
  const [templateForm, setTemplateForm] = useState<TemplateFormState>(emptyTemplateForm);

  const [showNotificationCreate, setShowNotificationCreate] = useState(false);
  const [markingReadNotificationId, setMarkingReadNotificationId] = useState<string | null>(null);
  const [deletingNotificationId, setDeletingNotificationId] = useState<string | null>(null);
  const [canLoadMoreNotifications, setCanLoadMoreNotifications] = useState(false);
  const [newNotification, setNewNotification] = useState({
    type: "",
    category: "",
    targetType: "BOTH",
    title: "",
    message: "",
    priority: "",
    relatedEntityType: "",
    expiresAt: "",
    userId: "",
    clientId: "",
  });
  const [clientOptions, setClientOptions] = useState<CustomSelectOption[]>([]);
  const [clientsPage, setClientsPage] = useState(0);
  const [clientsTotalPages, setClientsTotalPages] = useState(1);
  const [preferenceDrafts, setPreferenceDrafts] = useState<
    Record<number, StaffNotificationPreference>
  >({});
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastVariant, setToastVariant] = useState<"success" | "error">("success");

  const notificationsQuery = useGetStaffNotificationsPaginatedQuery({
    unreadOnly,
    page: notificationsPage,
    pageSize: 20,
  });
  const statsQuery = useGetStaffNotificationStatsQuery();
  const unreadCountQuery = useGetStaffNotificationUnreadCountQuery(undefined, {
    skip: activeTab !== "notifications",
  });
  const eventCatalogQuery = useGetStaffNotificationEventCatalogQuery(undefined, {
    skip:
      inboxOnlyView ||
      (activeTab !== "event-catalog" &&
        activeTab !== "templates" &&
        !showTriggerEditor &&
        !showTemplateEditor),
  });
  const tabs = useMemo(
    () =>
      inboxOnlyView
        ? baseTabs.filter((tab) => tab.id === "notifications")
        : baseTabs,
    [inboxOnlyView],
  );
  const triggersQuery = useGetStaffNotificationTriggersQuery(undefined, {
    skip: activeTab !== "triggers",
  });
  const templatesQuery = useGetStaffNotificationTemplatesQuery(undefined, {
    skip: activeTab !== "templates",
  });
  const preferencesQuery = useGetStaffNotificationPreferencesQuery(undefined, {
    skip: activeTab !== "preferences",
  });

  const [createNotification, createNotificationState] = useCreateStaffNotificationMutation();
  const [broadcastNotification, broadcastNotificationState] =
    useBroadcastStaffNotificationMutation();
  const [deleteNotification] = useDeleteStaffNotificationMutation();
  const [readNotification] = useReadStaffNotificationMutation();
  const [markAllRead, { isLoading: isMarkingAllRead }] =
    useMarkAllStaffNotificationsReadMutation();

  const [createTrigger, createTriggerState] = useCreateStaffNotificationTriggerMutation();
  const [updateTrigger, updateTriggerState] = useUpdateStaffNotificationTriggerMutation();
  const [deleteTrigger] = useDeleteStaffNotificationTriggerMutation();

  const [createTemplate, createTemplateState] = useCreateStaffNotificationTemplateMutation();
  const [updateTemplate, updateTemplateState] = useUpdateStaffNotificationTemplateMutation();
  const [deleteTemplate, { isLoading: isDeletingTemplate }] =
    useDeleteStaffNotificationTemplateMutation();

  const [updatePreference, updatePreferenceState] = useUpdateStaffNotificationPreferenceMutation();
  const [triggerGetClients, { isFetching: isFetchingClients }] = useLazyGetAdminClientsQuery();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2800);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    if (activeTab === "notifications") {
      void notificationsQuery.refetch();
      void statsQuery.refetch();
      void unreadCountQuery.refetch();
    }
    if (activeTab === "event-catalog" && !isTherapistView) {
      void eventCatalogQuery.refetch();
    }
    if (activeTab === "triggers") {
      void triggersQuery.refetch();
    }
    if (activeTab === "templates") {
      void templatesQuery.refetch();
    }
    if (activeTab === "preferences") {
      void preferencesQuery.refetch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, isTherapistView]);

  useEffect(() => {
    if (!preferencesQuery.data) return;
    const mapped = preferencesQuery.data.reduce<Record<number, StaffNotificationPreference>>(
      (acc, item) => {
        acc[item.id] = { ...item };
        return acc;
      },
      {},
    );
    setPreferenceDrafts(mapped);
  }, [preferencesQuery.data]);

  useEffect(() => {
    if (!showNotificationCreate) return;
    if (isTherapistView && clientOptions.length === 0 && !isFetchingClients) {
      void loadClients(1);
    }
  }, [
    showNotificationCreate,
    isTherapistView,
    clientOptions.length,
    isFetchingClients,
  ]);

  useEffect(() => {
    setNotificationsPage(1);
    setAllNotifications([]);
    setCanLoadMoreNotifications(false);
  }, [unreadOnly]);

  useEffect(() => {
    if (activeTab !== "notifications") return;

    const handleScroll = () => {
      setCanLoadMoreNotifications(true);
    };

    window.addEventListener("scroll", handleScroll, { passive: true });

    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, [activeTab]);

  useEffect(() => {
    const pageItems = notificationsQuery.data?.items ?? [];
    if (!pageItems.length) {
      if (
        notificationsPage === 1 &&
        !notificationsQuery.isLoading &&
        !notificationsQuery.isFetching
      ) {
        setAllNotifications([]);
      }
      return;
    }
    setAllNotifications((previous) => {
      if (notificationsPage === 1) return pageItems;
      const existingIds = new Set(previous.map((item) => item.id));
      const merged = [...previous];
      pageItems.forEach((item) => {
        if (!existingIds.has(item.id)) merged.push(item);
      });
      return merged;
    });
  }, [
    notificationsQuery.data?.items,
    notificationsPage,
    notificationsQuery.isLoading,
    notificationsQuery.isFetching,
  ]);

  const refreshNotificationsList = useCallback(async () => {
    setCanLoadMoreNotifications(false);
    if (notificationsPage !== 1) {
      setNotificationsPage(1);
      return;
    }
    await Promise.all([
      notificationsQuery.refetch(),
      statsQuery.refetch(),
      unreadCountQuery.refetch(),
    ]);
  }, [notificationsPage, notificationsQuery, statsQuery, unreadCountQuery]);

  const mappedNotifications = useMemo<Notification[]>(
    () =>
      allNotifications.map((row: StaffNotificationItem) => ({
        id: String(row.id),
        title: row.title || row.type || "Notification",
        description: formatNotificationMessage(row.message),
        timestamp: formatTimestamp(row.createdAt),
        isRead: Boolean(row.isRead) || Boolean(row.readAt),
      })),
    [allNotifications],
  );
  const notificationTotalCount = notificationsQuery.data?.totalCount ?? 0;
  const hasUnreadNotifications = useMemo(() => {
    if (mappedNotifications.some((notification) => !notification.isRead)) {
      return true;
    }

    const loadedCount = mappedNotifications.length;
    if (
      loadedCount > 0 &&
      (notificationTotalCount === 0 || loadedCount >= notificationTotalCount)
    ) {
      return false;
    }

    const unreadFromStats = statsQuery.data?.unread;
    if (typeof unreadFromStats === "number") {
      return unreadFromStats > 0;
    }

    return (unreadCountQuery.data ?? 0) > 0;
  }, [
    mappedNotifications,
    notificationTotalCount,
    statsQuery.data?.unread,
    unreadCountQuery.data,
  ]);
  const notificationsTotalPages = notificationsQuery.data?.totalPages ?? 1;
  const hasMoreNotifications = notificationsPage < notificationsTotalPages;
  const isLoadingMoreNotifications =
    notificationsQuery.isFetching && notificationsPage > 1;
  const loadMoreNotifications = useCallback(() => {
    if (notificationsQuery.isFetching || !hasMoreNotifications || !canLoadMoreNotifications) return;
    setCanLoadMoreNotifications(false);
    setNotificationsPage((previous) => previous + 1);
  }, [canLoadMoreNotifications, hasMoreNotifications, notificationsQuery.isFetching]);
  const { observerTarget: notificationsObserverTarget } = useInfiniteScroll({
    onLoadMore: loadMoreNotifications,
    hasMore: hasMoreNotifications,
    isLoading: notificationsQuery.isFetching,
    rootMargin: "0px 0px 120px 0px",
    threshold: 0.8,
  });
  const hasMoreClients = clientsPage < clientsTotalPages;
  const formattedTypeOptions = useMemo(
    () =>
      notificationTypeOptions.map((value) => ({
        value,
        label: value.replaceAll("_", " "),
      })),
    [],
  );
  const formattedCategoryOptions = useMemo(
    () =>
      notificationCategoryOptions.map((value) => ({
        value,
        label: value,
      })),
    [],
  );
  const formattedPriorityOptions = useMemo(
    () =>
      notificationPriorityOptions.map((value) => ({
        value,
        label: value,
      })),
    [],
  );
  const formattedTargetTypeOptions = useMemo(
    () =>
      notificationTargetTypeOptions.map((value) => ({
        value,
        label: value,
      })),
    [],
  );

  const resetNotificationForm = () => {
    setNewNotification({
      type: "",
      category: "",
      targetType: "BOTH",
      title: "",
      message: "",
      priority: "",
      relatedEntityType: "",
      expiresAt: "",
      userId: "",
      clientId: "",
    });
  };

  const closeTriggerEditor = () => {
    setShowTriggerEditor(false);
    setEditingTrigger(null);
    setTriggerForm(emptyTriggerForm);
    setTriggerFormErrors({});
    createTriggerState.reset();
    updateTriggerState.reset();
  };

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
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleCreateNotification = async () => {
    if (
      !newNotification.type ||
      !newNotification.category ||
      !newNotification.priority ||
      !newNotification.title.trim() ||
      !newNotification.message.trim()
    ) {
      setToastVariant("error");
      setToastMessage("Please fill all required fields.");
      return;
    }
    if (isTherapistView && !newNotification.clientId) {
      setToastVariant("error");
      setToastMessage("Please select a client.");
      return;
    }
    if (!newNotification.relatedEntityType) {
      setToastVariant("error");
      setToastMessage("Please select an entity type.");
      return;
    }
    const selectedEntity = (actionMetadataQuery.data?.entities ?? []).find(
      (entity) => entity.relatedEntityType === newNotification.relatedEntityType,
    );
    const resolvedRelatedEntityId =
      newNotification.relatedEntityType === "client"
        ? Number(newNotification.clientId)
        : newNotification.relatedEntityType === "user" && !isTherapistView
          ? Number(newNotification.userId)
          : null;
    const resolvedActionUrl = selectedEntity
      ? resolvedRelatedEntityId
        ? selectedEntity.actionUrlTemplate.replace("{id}", String(resolvedRelatedEntityId))
        : selectedEntity.exampleActionUrl || selectedEntity.actionUrlTemplate
      : undefined;
    const resolvedActionLabel = selectedEntity?.defaultActionLabel || undefined;
    try {
      const expiresAtIso = parseDateTimeLocalToIso(newNotification.expiresAt);
      if (isTherapistView) {
        await createNotification({
          type: newNotification.type,
          category: newNotification.category,
          title: newNotification.title.trim(),
          message: newNotification.message.trim(),
          priority: newNotification.priority,
          clientId: Number(newNotification.clientId),
          actionUrl: resolvedActionUrl,
          actionLabel: resolvedActionLabel,
          relatedEntityType: newNotification.relatedEntityType,
          relatedEntityId: resolvedRelatedEntityId ?? undefined,
          expiresAt: expiresAtIso,
        }).unwrap();
      } else {
        await broadcastNotification({
          targetType: newNotification.targetType as "BOTH" | "USERS" | "CLIENTS",
          type: newNotification.type,
          category: newNotification.category,
          title: newNotification.title.trim(),
          message: newNotification.message.trim(),
          priority: newNotification.priority,
          actionUrl: resolvedActionUrl,
          actionLabel: resolvedActionLabel,
          relatedEntityType: newNotification.relatedEntityType,
          relatedEntityId: resolvedRelatedEntityId,
          expiresAt: expiresAtIso,
        }).unwrap();
      }
      setShowNotificationCreate(false);
      resetNotificationForm();
      await refreshNotificationsList();
      setToastVariant("success");
      setToastMessage("Notification created successfully.");
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const openCreateTrigger = () => {
    createTriggerState.reset();
    updateTriggerState.reset();
    setEditingTrigger(null);
    setTriggerForm(emptyTriggerForm);
    setTriggerFormErrors({});
    setShowTriggerEditor(true);
  };

  const openEditTrigger = (trigger: StaffNotificationTrigger) => {
    createTriggerState.reset();
    updateTriggerState.reset();
    setEditingTrigger(trigger);
    setTriggerForm({
      name: trigger.name,
      description: trigger.description || "",
      eventType: trigger.eventType || "",
      entityType: trigger.entityType || "CLIENT",
      conditionRules: trigger.conditionRules || "{}",
      recipientRules: trigger.recipientRules || "{}",
      priority: normalizeTriggerPriorityForForm(trigger.priority),
      isScheduled: trigger.isScheduled,
      scheduleOffsetMinutes: String(trigger.scheduleOffsetMinutes ?? 0),
      batchWindowMinutes: String(trigger.batchWindowMinutes ?? 0),
      maxBatchSize: String(trigger.maxBatchSize ?? 0),
      isActive: trigger.isActive,
    });
    setTriggerFormErrors({});
    setShowTriggerEditor(true);
  };

  const saveTrigger = async () => {
    const validation = notificationTriggerSchema.safeParse(triggerForm);
    const nextErrors: Record<string, string> = {};
    if (!validation.success) {
      validation.error.issues.forEach((issue) => {
        const field = issue.path[0];
        if (typeof field === "string" && !nextErrors[field]) {
          nextErrors[field] = issue.message;
        }
      });
    }
    if (
      triggerForm.eventType &&
      !triggerEventTypeOptions.some((option) => option.value === triggerForm.eventType)
    ) {
      nextErrors.eventType = "Select a valid event type from the list";
    }
    if (Object.keys(nextErrors).length > 0) {
      setTriggerFormErrors(nextErrors);
      setToastVariant("error");
      setToastMessage("Please fix the highlighted trigger fields before saving.");
      return;
    }

    setTriggerFormErrors({});
    const payload: StaffNotificationTriggerPayload = {
      name: triggerForm.name.trim(),
      description: triggerForm.description.trim(),
      eventType: triggerForm.eventType.trim(),
      entityType: triggerForm.entityType.trim(),
      conditionRules: parseJsonOrFallback(triggerForm.conditionRules),
      recipientRules: parseJsonOrFallback(triggerForm.recipientRules),
      priority: triggerForm.priority.trim().toUpperCase(),
      isScheduled: triggerForm.isScheduled,
      scheduleOffsetMinutes: Number(triggerForm.scheduleOffsetMinutes || "0"),
      batchWindowMinutes: Number(triggerForm.batchWindowMinutes || "0"),
      maxBatchSize: Number(triggerForm.maxBatchSize || "0"),
      isActive: triggerForm.isActive,
    };

    try {
      if (editingTrigger) {
        await updateTrigger({ id: editingTrigger.id, body: payload }).unwrap();
        setToastVariant("success");
        setToastMessage("Trigger updated successfully.");
      } else {
        await createTrigger(payload).unwrap();
        setToastVariant("success");
        setToastMessage("Trigger created successfully.");
      }
      closeTriggerEditor();
      await triggersQuery.refetch();
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const openCreateTemplate = () => {
    createTemplateState.reset();
    updateTemplateState.reset();
    setEditingTemplate(null);
    setTemplateForm(emptyTemplateForm);
    setShowTemplateEditor(true);
  };

  const openEditTemplate = (template: StaffNotificationTemplate) => {
    createTemplateState.reset();
    updateTemplateState.reset();
    setEditingTemplate(template);
    setTemplateForm({
      name: template.name,
      type: resolveNotificationTemplateChannelType({
        name: template.name,
        type: template.type,
        eventType: template.eventType,
      }),
      eventType: resolveNotificationTemplateEventType({
        name: template.name,
        type: template.type,
        eventType: template.eventType,
        validEventTypes: getValidEventTypes(),
      }),
      subject: template.subject,
      bodyTemplate: template.bodyTemplate,
      isSystem: template.isSystem,
      isActive: template.isActive,
    });
    setShowTemplateEditor(true);
  };

  const closeTemplateEditor = () => {
    setShowTemplateEditor(false);
    setEditingTemplate(null);
    setTemplateForm(emptyTemplateForm);
    createTemplateState.reset();
    updateTemplateState.reset();
  };

  const handleDeleteTemplateConfirm = async () => {
    if (!templateToDelete) return;

    try {
      await deleteTemplate(templateToDelete.id).unwrap();
      await templatesQuery.refetch();
      setToastVariant("success");
      setToastMessage("Template deleted successfully.");
      setIsDeleteTemplateModalOpen(false);
      setTemplateToDelete(null);
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const saveTemplate = async () => {
    const payload: StaffNotificationTemplatePayload = {
      ...templateForm,
    };

    try {
      if (editingTemplate) {
        await updateTemplate({ id: editingTemplate.id, body: payload }).unwrap();
        setToastVariant("success");
        setToastMessage("Template updated successfully.");
      } else {
        await createTemplate(payload).unwrap();
        setToastVariant("success");
        setToastMessage("Template created successfully.");
      }
      closeTemplateEditor();
      await templatesQuery.refetch();
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const savePreference = async (preference: StaffNotificationPreference) => {
    try {
      await updatePreference({
        triggerType: preference.notificationType,
        body: {
          notificationType: preference.notificationType,
          emailEnabled: preference.emailEnabled,
          smsEnabled: preference.smsEnabled,
          pushEnabled: preference.pushEnabled,
          inAppEnabled: preference.inAppEnabled,
          timing: preference.timing,
          quietHoursStart: preference.quietHoursStart,
          quietHoursEnd: preference.quietHoursEnd,
          weekendsEnabled: preference.weekendsEnabled,
        },
      }).unwrap();
      await preferencesQuery.refetch();
      setToastVariant("success");
      setToastMessage("Preference updated successfully.");
    } catch (error) {
      setToastVariant("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const renderNotificationsTab = () => {
    const isNotificationsListLoading =
      notificationsQuery.isLoading ||
      (notificationsQuery.isFetching &&
        mappedNotifications.length === 0 &&
        notificationsPage === 1);

    if (isNotificationsListLoading) {
      return (
        <ContentLoader />
      );
    }

    if (notificationsQuery.isError) {
      return (
        <div className="px-1 py-5 text-sm text-(--status-denied)">
          {getApiErrorMessage(notificationsQuery.error)}
        </div>
      );
    }

    return (
      <div className="space-y-4">
        <div className="rounded-xl border border-(--neutral-100)">
          {mappedNotifications.length > 0 ? (
            <>
              {mappedNotifications.map((notification) => (
                <div key={notification.id}>
                  <NotificationListing
                    notification={notification}
                    isMarkingRead={markingReadNotificationId === notification.id}
                    onMarkRead={(id) => {
                      setMarkingReadNotificationId(id);
                      void readNotification(Number(id))
                        .unwrap()
                        .then(async () => {
                          await refreshNotificationsList();
                          setToastVariant("success");
                          setToastMessage("Notification marked as read.");
                        })
                        .catch((error) => {
                          setToastVariant("error");
                          setToastMessage(getApiErrorMessage(error));
                        })
                        .finally(() => {
                          setMarkingReadNotificationId((current) =>
                            current === id ? null : current,
                          );
                        });
                    }}
                    endActions={
                      <button
                        type="button"
                        onClick={() => {
                          setDeletingNotificationId(notification.id);
                          void deleteNotification(Number(notification.id))
                            .unwrap()
                            .then(async () => {
                              await refreshNotificationsList();
                              setToastVariant("success");
                              setToastMessage("Notification deleted successfully.");
                            })
                            .catch((error) => {
                              setToastVariant("error");
                              setToastMessage(getApiErrorMessage(error));
                            })
                            .finally(() => {
                              setDeletingNotificationId((current) =>
                                current === notification.id ? null : current,
                              );
                            });
                        }}
                        disabled={deletingNotificationId === notification.id}
                        aria-label="Delete notification"
                        title="Delete"
                        className="inline-flex h-7 w-7 items-center justify-center rounded-full text-(--status-denied) hover:bg-(--neutral-100) disabled:opacity-70 cursor-pointer"
                      >
                        {deletingNotificationId === notification.id ? (
                          <ContentLoader variant="inline" size="sm" />
                        ) : (
                          <TrashIcon size={18} />
                        )}
                      </button>
                    }
                  />
                </div>
              ))}
              <div ref={notificationsObserverTarget} className="h-1" />
              {isLoadingMoreNotifications ? (
                <ContentLoader size="sm" className="py-3 text-(--text-neutral-600)" />
              ) : null}
            </>
          ) : (
            <div className="px-5 py-6 text-sm text-(--text-neutral-600)">No notifications found.</div>
          )}
        </div>

      </div>
    );
  };

  const renderEventCatalogTab = () => {
    if (eventCatalogQuery.isLoading || eventCatalogQuery.isFetching) {
      return (
        <ContentLoader />
      );
    }
    if (eventCatalogQuery.isError) {
      return (
        <div className="px-1 py-5 text-sm text-(--status-denied)">
          {getApiErrorMessage(eventCatalogQuery.error)}
        </div>
      );
    }
    if ((eventCatalogQuery.data?.events?.length ?? 0) === 0) {
      return <div className="px-1 py-5 text-sm text-(--text-neutral-600)">No data found.</div>;
    }
    return (
      <div className="space-y-2">
        {eventCatalogQuery.data?.events.map((event) => (
          <div key={event.eventType} className="rounded-lg border border-(--neutral-100) p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-medium text-(--text-primary-dark)">
                {humanizeNotificationLabel(event.eventType)}
              </p>
              <div className="flex flex-wrap gap-1">
                {event.defaultChannels.map((channel) => (
                  <span
                    key={channel}
                    className="rounded-full bg-(--neutral-100) px-2 py-0.5 text-xs text-(--text-neutral-600)"
                  >
                    {humanizeNotificationLabel(channel)}
                  </span>
                ))}
              </div>
            </div>
            <p className="mt-1 text-xs text-(--text-neutral-600)">
              Required: {event.required ? "Yes" : "No"} | Session health event:{" "}
              {event.sessionHealthEvent ? "Yes" : "No"}
            </p>
          </div>
        ))}
      </div>
    );
  };

  const actionMetadataQuery = useGetStaffNotificationActionMetadataQuery(undefined, {
    skip: activeTab !== "notifications" && !showNotificationCreate,
  });

  const relatedEntityTypeOptions = useMemo(
    () =>
      (actionMetadataQuery.data?.entities ?? []).map((entity) => ({
        value: entity.relatedEntityType,
        label: entity.relatedEntityType,
      })),
    [actionMetadataQuery.data?.entities],
  );

  const triggerEventTypeOptions = useMemo<CustomSelectOption[]>(() => {
    const catalogTypes =
      eventCatalogQuery.data?.events
        .map((event) => event.eventType)
        .filter((eventType): eventType is string => Boolean(eventType)) ?? [];
    const values = [...new Set([...catalogTypes, ...EVENT_TYPE_FALLBACK_KEYS])];
    return values.map((value) => {
      const fallback = EVENT_TYPE_OPTIONS.find((option) => option.value === value);
      return {
        value,
        label: fallback?.label ?? humanizeNotificationLabel(value),
        group: fallback?.group,
      };
    });
  }, [eventCatalogQuery.data?.events]);

  const getValidEventTypes = useCallback(
    () => triggerEventTypeOptions.map((option) => option.value),
    [triggerEventTypeOptions],
  );

  const templateEventTypeOptions = useMemo<CustomSelectOption[]>(() => {
    const current = templateForm.eventType?.trim();
    if (!current || triggerEventTypeOptions.some((option) => option.value === current)) {
      return triggerEventTypeOptions;
    }

    const fallback = EVENT_TYPE_OPTIONS.find((option) => option.value === current);
    return [
      ...triggerEventTypeOptions,
      {
        value: current,
        label: fallback?.label ?? humanizeNotificationLabel(current),
        group: fallback?.group ?? "Other Events",
      },
    ];
  }, [triggerEventTypeOptions, templateForm.eventType]);

  const triggerPriorityOptions = useMemo<CustomSelectOption[]>(
    () => buildTriggerPriorityOptions(triggerForm.priority),
    [triggerForm.priority],
  );

  const renderTriggersTab = () => {
    if (triggersQuery.isLoading) {
      return (
        <ContentLoader />
      );
    }
    if (triggersQuery.isError) {
      return (
        <div className="px-1 py-5 text-sm text-(--status-denied)">
          {getApiErrorMessage(triggersQuery.error)}
        </div>
      );
    }

    return (
      <div className="space-y-3">
          {(triggersQuery.data ?? []).length === 0 ? (
            <div className="rounded-xl border border-(--neutral-100) bg-white p-4 text-sm text-(--text-neutral-600)">
              No data found.
            </div>
          ) : (triggersQuery.data ?? []).map((trigger) => (
            <div key={trigger.id} className="overflow-hidden rounded-xl border border-(--neutral-100) bg-white p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <h3
                    className="truncate font-semibold text-(--text-primary-dark)"
                    title={trigger.name || "Untitled Trigger"}
                  >
                    {trigger.name || "Untitled Trigger"}
                  </h3>
                  <ExpandableText
                    text={trigger.description}
                    emptyText="-"
                    className="mt-1 text-sm text-(--text-neutral-600)"
                    collapseThreshold={100}
                  />
                </div>
                <span className="shrink-0 rounded-full bg-(--neutral-100) px-3 py-1 text-xs text-(--text-neutral-600)">
                  {trigger.isActive ? "Active" : "Inactive"}
                </span>
              </div>
              <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                <div className="flex min-w-0 flex-wrap gap-2 text-xs text-(--text-neutral-600)">
                  <span className="rounded-full bg-(--neutral-50) px-2 py-1">{trigger.eventType}</span>
                  <span className="rounded-full bg-(--neutral-50) px-2 py-1">{trigger.entityType}</span>
                  <span className="rounded-full bg-(--neutral-50) px-2 py-1">{trigger.priority}</span>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <Button
                    onClick={() => openEditTrigger(trigger)}
                    className="h-8 rounded-full bg-transparent px-3 text-xs text-(--text-primary-500) hover:bg-(--neutral-100)"
                  >
                    Edit
                  </Button>
                  <Button
                    onClick={() =>
                      void deleteTrigger(trigger.id)
                        .unwrap()
                        .then(async () => {
                          await triggersQuery.refetch();
                          setToastVariant("success");
                          setToastMessage("Trigger deleted successfully.");
                        })
                        .catch((error) => {
                          setToastVariant("error");
                          setToastMessage(getApiErrorMessage(error));
                        })
                    }
                    className="h-8 rounded-full bg-transparent px-3 text-xs text-(--status-denied) hover:bg-(--neutral-100)"
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </div>
          ))}
      </div>
    );
  };

  const renderTemplatesTab = () => {
    if (templatesQuery.isLoading) {
      return (
        <ContentLoader />
      );
    }
    if (templatesQuery.isError) {
      return (
        <div className="px-1 py-5 text-sm text-(--status-denied)">
          {getApiErrorMessage(templatesQuery.error)}
        </div>
      );
    }

    return (
      <div className="space-y-3">
          {(templatesQuery.data ?? []).length === 0 ? (
            <div className="rounded-xl border border-(--neutral-100) bg-white p-4 text-sm text-(--text-neutral-600)">
              No data found.
            </div>
          ) : (templatesQuery.data ?? []).map((template) => (
            <div key={template.id} className="rounded-xl border border-(--neutral-100) bg-white p-4 overflow-hidden">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <h3
                    className="truncate font-semibold text-(--text-primary-dark) break-words [overflow-wrap:anywhere]"
                    title={formatNotificationDisplayLabel(template.name)}
                  >
                    {formatNotificationDisplayLabel(template.name)}
                  </h3>
                  <p
                    className="truncate text-sm text-(--text-neutral-600) break-words [overflow-wrap:anywhere]"
                    title={template.subject || "-"}
                  >
                    {template.subject || "-"}
                  </p>
                </div>
                <span className="shrink-0 rounded-full bg-(--neutral-100) px-3 py-1 text-xs text-(--text-neutral-600)">
                  {template.isActive ? "Active" : "Inactive"}
                </span>
              </div>
              <p
                className="mt-2 line-clamp-3 text-sm text-(--text-neutral-600) break-words [overflow-wrap:anywhere]"
                title={template.bodyTemplate || "-"}
              >
                {template.bodyTemplate || "-"}
              </p>
              <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                <div className="flex min-w-0 flex-wrap gap-2 text-xs text-(--text-neutral-600)">
                  <span className="rounded-full bg-(--neutral-50) px-2 py-1">
                    {formatNotificationDisplayLabel(template.type)}
                  </span>
                  <span className="rounded-full bg-(--neutral-50) px-2 py-1">
                    {formatNotificationDisplayLabel(template.eventType)}
                  </span>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <Button
                    onClick={() => openEditTemplate(template)}
                    className="h-8 rounded-full bg-transparent px-3 text-xs text-(--text-primary-500) hover:bg-(--neutral-100)"
                  >
                    Edit
                  </Button>
                  <Button
                    onClick={() => {
                      setTemplateToDelete(template);
                      setIsDeleteTemplateModalOpen(true);
                    }}
                    className="h-8 rounded-full bg-transparent px-3 text-xs text-(--status-denied) hover:bg-(--neutral-100)"
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </div>
          ))}
      </div>
    );
  };

  const renderPreferencesTab = () => {
    if (preferencesQuery.isLoading) {
      return (
        <ContentLoader />
      );
    }
    if (preferencesQuery.isError) {
      return (
        <div className="px-1 py-5 text-sm text-(--status-denied)">
          {getApiErrorMessage(preferencesQuery.error)}
        </div>
      );
    }

    return (
      <div className="space-y-3">
        {(preferencesQuery.data ?? []).length === 0 ? (
          <div className="rounded-xl border border-(--neutral-100) bg-white p-4 text-sm text-(--text-neutral-600)">
            No data found.
          </div>
        ) : (preferencesQuery.data ?? []).map((item) => {
          const preference = preferenceDrafts[item.id] ?? item;
          return (
          <div key={preference.id} className="rounded-xl border border-(--neutral-100) bg-white p-4">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-(--text-primary-dark)">{preference.notificationType}</h3>
              <Button
                onClick={() => void savePreference(preference)}
                className="h-8 rounded-full bg-(--bg-primary-dark) px-3 text-xs text-white hover:bg-(--bg-primary-dark)/90"
                disabled={updatePreferenceState.isLoading}
                loading={updatePreferenceState.isLoading}
                loadingLabel="Saving..."
              >
                Save
              </Button>
            </div>
            <div className="mt-3 grid grid-cols-2 gap-3 md:grid-cols-5">
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <Switch
                  checked={preference.emailEnabled}
                  onCheckedChange={(checked) => {
                    setPreferenceDrafts((prev) => ({
                      ...prev,
                      [preference.id]: { ...preference, emailEnabled: checked },
                    }));
                  }}
                />
                Email
              </label>
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <Switch
                  checked={preference.smsEnabled}
                  onCheckedChange={(checked) => {
                    setPreferenceDrafts((prev) => ({
                      ...prev,
                      [preference.id]: { ...preference, smsEnabled: checked },
                    }));
                  }}
                />
                SMS
              </label>
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <Switch
                  checked={preference.pushEnabled}
                  onCheckedChange={(checked) => {
                    setPreferenceDrafts((prev) => ({
                      ...prev,
                      [preference.id]: { ...preference, pushEnabled: checked },
                    }));
                  }}
                />
                Push
              </label>
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <Switch
                  checked={preference.inAppEnabled}
                  onCheckedChange={(checked) => {
                    setPreferenceDrafts((prev) => ({
                      ...prev,
                      [preference.id]: { ...preference, inAppEnabled: checked },
                    }));
                  }}
                />
                In-App
              </label>
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <Switch
                  checked={preference.weekendsEnabled}
                  onCheckedChange={(checked) => {
                    setPreferenceDrafts((prev) => ({
                      ...prev,
                      [preference.id]: { ...preference, weekendsEnabled: checked },
                    }));
                  }}
                />
                Weekends
              </label>
            </div>
          </div>
        )})}
      </div>
    );
  };

  const renderTabToolbar = () => {
    if (activeTab === "notifications") {
      return (
        <div className="shrink-0 border-b border-(--neutral-100) bg-white px-5 py-3">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Switch checked={unreadOnly} onCheckedChange={setUnreadOnly} />
              <span className="text-sm text-(--text-neutral-600)">Unread only</span>
            </div>
            <div className="flex justify-end gap-2">
            {!inboxOnlyView ? (
              <Button
                onClick={() => {
                  resetNotificationForm();
                  setShowNotificationCreate(true);
                }}
                className="h-9 rounded-full bg-(--bg-primary-dark) px-4 text-white hover:bg-(--bg-primary-dark)/90"
              >
                Create Notification
              </Button>
            ) : null}
            <Button
              type="button"
              onClick={() => {
                if (!hasUnreadNotifications || isMarkingAllRead) return;
                void markAllRead()
                  .unwrap()
                  .then(async () => {
                    if (unreadOnly) {
                      await refreshNotificationsList();
                    } else {
                      setAllNotifications((previous) =>
                        previous.map((item) => ({ ...item, isRead: true })),
                      );
                      await Promise.all([
                        notificationsQuery.refetch(),
                        statsQuery.refetch(),
                        unreadCountQuery.refetch(),
                      ]);
                    }
                    setToastVariant("success");
                    setToastMessage("All notifications marked as read.");
                  })
                  .catch((error) => {
                    setToastVariant("error");
                    setToastMessage(getApiErrorMessage(error));
                  });
              }}
              disabled={!hasUnreadNotifications || isMarkingAllRead}
              aria-disabled={!hasUnreadNotifications || isMarkingAllRead}
              className="h-9 rounded-full bg-transparent text-(--text-primary-500) hover:bg-(--neutral-100) disabled:pointer-events-none disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-transparent"
            >
              Mark all read
            </Button>
            </div>
          </div>
        </div>
      );
    }

    if (activeTab === "triggers") {
      return (
        <div className="shrink-0 border-b border-(--neutral-100) bg-white px-5 py-3">
          <div className="flex justify-end">
            <Button
              onClick={openCreateTrigger}
              className="h-9 rounded-full bg-(--bg-primary-dark) px-4 text-white hover:bg-(--bg-primary-dark)/90"
            >
              Add Trigger
            </Button>
          </div>
        </div>
      );
    }

    if (activeTab === "templates") {
      return (
        <div className="shrink-0 border-b border-(--neutral-100) bg-white px-5 py-3">
          <div className="flex justify-end">
            <Button
              onClick={openCreateTemplate}
              className="h-9 rounded-full bg-(--bg-primary-dark) px-4 text-white hover:bg-(--bg-primary-dark)/90"
            >
              Add Template
            </Button>
          </div>
        </div>
      );
    }

    return null;
  };

  return (
    <div className="flex h-[calc(100vh-10.5rem)] min-h-0 flex-col overflow-hidden rounded-2xl border border-(--neutral-100) bg-white">
      <div className="shrink-0 border-b border-(--neutral-100) bg-white px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-(--text-primary-dark)">
            {activeTab === "notifications"
              ? inboxOnlyView
                ? "Notifications"
                : "Notification Management"
              : tabs.find((tab) => tab.id === activeTab)?.label}
          </h2>
          {!inboxOnlyView ? (
            <div className="flex items-center gap-2 rounded-full bg-(--neutral-100) p-1">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`rounded-full px-4 py-2 text-sm font-medium cursor-pointer transition-colors ${
                    activeTab === tab.id
                      ? "bg-white text-(--text-primary-dark)"
                      : "text-(--text-neutral-600)"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          ) : null}
        </div>
      </div>

      {renderTabToolbar()}

      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-5">
        {activeTab === "notifications" && renderNotificationsTab()}
        {activeTab === "event-catalog" && !inboxOnlyView && renderEventCatalogTab()}
        {activeTab === "triggers" && renderTriggersTab()}
        {activeTab === "templates" && renderTemplatesTab()}
        {activeTab === "preferences" && renderPreferencesTab()}
      </div>

      {showTriggerEditor ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/25 p-4">
          <div className="flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl bg-white">
            <div className="shrink-0 border-b border-(--neutral-100) px-5 py-4">
              <h3 className="text-lg font-semibold text-(--text-primary-dark)">
                {editingTrigger ? "Edit Trigger" : "Create Trigger"}
              </h3>
            </div>
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 py-4">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <div>
                <CustomInput
                  label="Name"
                  required
                  value={triggerForm.name}
                  maxLength={NOTIFICATION_TRIGGER_LIMITS.name}
                  hint={`${triggerForm.name.length}/${NOTIFICATION_TRIGGER_LIMITS.name}`}
                  onChange={(e) => setTriggerForm((p) => ({ ...p, name: e.target.value }))}
                />
                {triggerFormErrors.name ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.name}</p>
                ) : null}
              </div>
              <div>
                <CustomSelect
                  label="Event Type"
                  required
                  value={triggerForm.eventType}
                  onChange={(value) => setTriggerForm((p) => ({ ...p, eventType: value }))}
                  options={triggerEventTypeOptions}
                  isGrouped
                  placeholder="Select event type..."
                />
                {triggerFormErrors.eventType ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.eventType}</p>
                ) : null}
              </div>
              <div>
                <CustomSelect
                  label="Entity Type"
                  required
                  value={triggerForm.entityType}
                  onChange={(value) => setTriggerForm((p) => ({ ...p, entityType: value }))}
                  options={ENTITY_TYPE_OPTIONS}
                  placeholder="Select entity type..."
                />
                {triggerFormErrors.entityType ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.entityType}</p>
                ) : null}
              </div>
              <div>
                <CustomSelect
                  label="Priority"
                  required
                  value={triggerForm.priority}
                  onChange={(value) => setTriggerForm((p) => ({ ...p, priority: value }))}
                  options={triggerPriorityOptions}
                  isSearch={false}
                  placeholder="Select priority..."
                />
                {triggerFormErrors.priority ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.priority}</p>
                ) : null}
              </div>
              <div>
                <CustomInput
                  label="Schedule Offset Minutes"
                  value={triggerForm.scheduleOffsetMinutes}
                  inputMode="numeric"
                  digitsOnly
                  maxLength={5}
                  onChange={(e) =>
                    setTriggerForm((p) => ({ ...p, scheduleOffsetMinutes: e.target.value }))
                  }
                />
                {triggerFormErrors.scheduleOffsetMinutes ? (
                  <p className="mt-1 text-sm text-(--status-denied)">
                    {triggerFormErrors.scheduleOffsetMinutes}
                  </p>
                ) : null}
              </div>
              <div>
                <CustomInput
                  label="Batch Window Minutes"
                  value={triggerForm.batchWindowMinutes}
                  inputMode="numeric"
                  digitsOnly
                  maxLength={5}
                  onChange={(e) =>
                    setTriggerForm((p) => ({ ...p, batchWindowMinutes: e.target.value }))
                  }
                />
                {triggerFormErrors.batchWindowMinutes ? (
                  <p className="mt-1 text-sm text-(--status-denied)">
                    {triggerFormErrors.batchWindowMinutes}
                  </p>
                ) : null}
              </div>
              <div>
                <CustomInput
                  label="Max Batch Size"
                  value={triggerForm.maxBatchSize}
                  inputMode="numeric"
                  digitsOnly
                  maxLength={5}
                  onChange={(e) => setTriggerForm((p) => ({ ...p, maxBatchSize: e.target.value }))}
                />
                {triggerFormErrors.maxBatchSize ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.maxBatchSize}</p>
                ) : null}
              </div>
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)"><Switch checked={triggerForm.isScheduled} onCheckedChange={(v) => setTriggerForm((p) => ({ ...p, isScheduled: v }))} /> Scheduled</label>
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)"><Switch checked={triggerForm.isActive} onCheckedChange={(v) => setTriggerForm((p) => ({ ...p, isActive: v }))} /> Active</label>
            </div>
            <div className="mt-3 space-y-3">
              <div>
                <CustomTextarea
                  label="Description"
                  value={triggerForm.description}
                  maxLength={NOTIFICATION_TRIGGER_LIMITS.description}
                  hint={`${triggerForm.description.length}/${NOTIFICATION_TRIGGER_LIMITS.description}`}
                  onChange={(e) => setTriggerForm((p) => ({ ...p, description: e.target.value }))}
                />
                {triggerFormErrors.description ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.description}</p>
                ) : null}
              </div>
              <div>
                <CustomTextarea
                  label="Condition Rules JSON"
                  required
                  value={triggerForm.conditionRules}
                  className="min-h-40 font-mono text-sm"
                  maxLength={NOTIFICATION_TRIGGER_LIMITS.jsonRules}
                  hint={`${triggerForm.conditionRules.length}/${NOTIFICATION_TRIGGER_LIMITS.jsonRules}`}
                  onChange={(e) =>
                    setTriggerForm((p) => ({ ...p, conditionRules: e.target.value }))
                  }
                />
                {triggerFormErrors.conditionRules ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.conditionRules}</p>
                ) : null}
              </div>
              <div>
                <CustomTextarea
                  label="Recipient Rules JSON"
                  required
                  value={triggerForm.recipientRules}
                  className="min-h-40 font-mono text-sm"
                  maxLength={NOTIFICATION_TRIGGER_LIMITS.jsonRules}
                  hint={`${triggerForm.recipientRules.length}/${NOTIFICATION_TRIGGER_LIMITS.jsonRules}`}
                  onChange={(e) =>
                    setTriggerForm((p) => ({ ...p, recipientRules: e.target.value }))
                  }
                />
                {triggerFormErrors.recipientRules ? (
                  <p className="mt-1 text-sm text-(--status-denied)">{triggerFormErrors.recipientRules}</p>
                ) : null}
              </div>
            </div>
            {(createTriggerState.error || updateTriggerState.error) ? (
              <p className="mt-3 text-sm text-(--status-denied)">
                {getApiErrorMessage(createTriggerState.error ?? updateTriggerState.error)}
              </p>
            ) : null}
            </div>
            <div className="shrink-0 border-t border-(--neutral-100) px-5 py-4 flex justify-end gap-2">
              <Button onClick={closeTriggerEditor} className="h-9 rounded-full bg-transparent px-4 text-(--text-neutral-600) hover:bg-(--neutral-100)">Cancel</Button>
              <Button onClick={() => void saveTrigger()} disabled={createTriggerState.isLoading || updateTriggerState.isLoading} loading={createTriggerState.isLoading || updateTriggerState.isLoading} loadingLabel="Saving..." className="h-9 rounded-full bg-(--bg-primary-dark) px-4 text-white hover:bg-(--bg-primary-dark)/90">
                Save
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      <DeleteConfirmationModal
        isOpen={isDeleteTemplateModalOpen}
        onClose={() => {
          if (!isDeletingTemplate) {
            setIsDeleteTemplateModalOpen(false);
            setTemplateToDelete(null);
          }
        }}
        onConfirm={() => void handleDeleteTemplateConfirm()}
        title={`Delete "${formatNotificationDisplayLabel(templateToDelete?.name || "Template")}"`}
        description="Are you sure you want to delete this template? This action cannot be undone."
        isDeleting={isDeletingTemplate}
      />

      {showTemplateEditor ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/25 p-4">
          <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-5">
            <h3 className="mb-4 text-lg font-semibold text-(--text-primary-dark)">
              {editingTemplate ? "Edit Template" : "Create Template"}
            </h3>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <CustomInput label="Name" value={templateForm.name} onChange={(e) => setTemplateForm((p) => ({ ...p, name: e.target.value }))} />
              <CustomSelect
                label="Type"
                value={templateForm.type}
                onChange={(value) => setTemplateForm((p) => ({ ...p, type: value }))}
                options={[
                  { label: humanizeNotificationLabel("IN_APP"), value: "IN_APP" },
                  { label: humanizeNotificationLabel("EMAIL"), value: "EMAIL" },
                  { label: humanizeNotificationLabel("SMS"), value: "SMS" },
                ]}
                isSearch={false}
              />              <CustomSelect
                label="Event Type"
                value={templateForm.eventType}
                onChange={(value) => setTemplateForm((p) => ({ ...p, eventType: value }))}
                options={templateEventTypeOptions}
                isGrouped
                placeholder="Select event type..."
              />
              <CustomInput label="Subject" value={templateForm.subject} onChange={(e) => setTemplateForm((p) => ({ ...p, subject: e.target.value }))} />
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)"><Switch checked={templateForm.isSystem} onCheckedChange={(v) => setTemplateForm((p) => ({ ...p, isSystem: v }))} /> System</label>
              <label className="flex items-center gap-2 text-sm text-(--text-neutral-600)"><Switch checked={templateForm.isActive} onCheckedChange={(v) => setTemplateForm((p) => ({ ...p, isActive: v }))} /> Active</label>
            </div>
            <div className="mt-3">
              <CustomTextarea label="Body Template" value={templateForm.bodyTemplate} onChange={(e) => setTemplateForm((p) => ({ ...p, bodyTemplate: e.target.value }))} className="min-h-40" />
            </div>
            {(createTemplateState.error || updateTemplateState.error) ? (
              <p className="mt-3 text-sm text-(--status-denied)">
                {getApiErrorMessage(createTemplateState.error ?? updateTemplateState.error)}
              </p>
            ) : null}
            <div className="mt-4 flex justify-end gap-2">
              <Button onClick={closeTemplateEditor} className="h-9 rounded-full bg-transparent px-4 text-(--text-neutral-600) hover:bg-(--neutral-100)">Cancel</Button>
              <Button onClick={() => void saveTemplate()} disabled={createTemplateState.isLoading || updateTemplateState.isLoading} loading={createTemplateState.isLoading || updateTemplateState.isLoading} loadingLabel="Saving..." className="h-9 rounded-full bg-(--bg-primary-dark) px-4 text-white hover:bg-(--bg-primary-dark)/90">
                Save
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {showNotificationCreate ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/25 p-4">
          <div className="w-full max-w-2xl rounded-2xl bg-white p-5">
            <h3 className="mb-4 text-lg font-semibold text-(--text-primary-dark)">Create Notification</h3>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <CustomSelect
                label="Type"
                value={newNotification.type}
                onChange={(value) => setNewNotification((p) => ({ ...p, type: value }))}
                options={formattedTypeOptions}
                isSearch
                required
              />
              <CustomSelect
                label="Category"
                value={newNotification.category}
                onChange={(value) => setNewNotification((p) => ({ ...p, category: value }))}
                options={formattedCategoryOptions}
                isSearch
                required
              />
              <CustomInput
                label="Title"
                value={newNotification.title}
                onChange={(e) => setNewNotification((p) => ({ ...p, title: e.target.value }))}
                required
              />
              <CustomSelect
                label="Priority"
                value={newNotification.priority}
                onChange={(value) => setNewNotification((p) => ({ ...p, priority: value }))}
                options={formattedPriorityOptions}
                isSearch={false}
                required
              />
              {!isTherapistView ? (
                <CustomSelect
                  label="Target Type"
                  value={newNotification.targetType}
                  onChange={(value) => setNewNotification((p) => ({ ...p, targetType: value }))}
                  options={formattedTargetTypeOptions}
                  isSearch={false}
                />
              ) : null}
              {!isTherapistView ? (
                <></>
              ) : (
                <CustomSelect
                  label="Client"
                  value={newNotification.clientId}
                  onChange={(value) => setNewNotification((p) => ({ ...p, clientId: value }))}
                  options={clientOptions}
                  isSearch
                  required
                  onMenuScrollToEnd={() => {
                    if (hasMoreClients && !isFetchingClients) {
                      void loadClients(clientsPage + 1);
                    }
                  }}
                  hasMore={hasMoreClients}
                  isLoadingMore={isFetchingClients && clientOptions.length > 0}
                  loadingMoreLabel="Loading more clients..."
                />
              )}
              <CustomSelect
                label="Entity Type"
                value={newNotification.relatedEntityType}
                onChange={(value) =>
                  setNewNotification((p) => ({ ...p, relatedEntityType: value }))
                }
                options={relatedEntityTypeOptions}
                isSearch
                required
              />
              <label className="relative group w-full md:col-span-2">
                <div
                  className="w-full min-h-15 rounded-xl border border-(--neutral-100) shadow-xs pt-7 pb-2 px-3 transition-colors hover:border-(--neutral-600) bg-white cursor-text"
                  onClick={() => {
                    const input = expiresAtInputRef.current;
                    if (!input) return;
                    input.focus();
                    if ("showPicker" in input && typeof input.showPicker === "function") {
                      input.showPicker();
                    }
                  }}
                >
                  <Input
                    ref={expiresAtInputRef}
                    type="datetime-local"
                    step="60"
                    min={getMinExpiresAt()}
                    value={newNotification.expiresAt}
                    onChange={(e) => {
                      const value = e.target.value;
                      if (isPastDateTimeLocal(value)) return;
                      setNewNotification((p) => ({ ...p, expiresAt: value }));
                    }}
                    className="h-6 p-0 border-none focus-visible:ring-0 focus-visible:ring-offset-0 shadow-none text-base text-(--neutral-950) bg-transparent"
                    placeholder=" "
                  />
                  <span className="absolute left-3 top-3.5 text-[0.6875rem] text-(--text-neutral-400) pointer-events-none">
                    Expires At
                  </span>
                </div>
              </label>
            </div>
            <div className="mt-3">
              <CustomTextarea label="Message" value={newNotification.message} onChange={(e) => setNewNotification((p) => ({ ...p, message: e.target.value }))} className="min-h-30" required />
            </div>
            {createNotificationState.error ? (
              <p className="mt-3 text-sm text-(--status-denied)">{getApiErrorMessage(createNotificationState.error)}</p>
            ) : null}
            <div className="mt-4 flex justify-end gap-2">
              <Button onClick={() => setShowNotificationCreate(false)} className="h-9 rounded-full bg-transparent px-4 text-(--text-neutral-600) hover:bg-(--neutral-100)">Cancel</Button>
              <Button
                onClick={() => void handleCreateNotification()}
                disabled={createNotificationState.isLoading || broadcastNotificationState.isLoading}
                loading={createNotificationState.isLoading || broadcastNotificationState.isLoading}
                loadingLabel="Creating..."
                className="h-9 rounded-full bg-(--bg-primary-dark) px-4 text-white hover:bg-(--bg-primary-dark)/90"
              >
                Create
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {(createNotificationState.isLoading ||
        broadcastNotificationState.isLoading ||
        createTriggerState.isLoading ||
        updateTriggerState.isLoading ||
        createTemplateState.isLoading ||
        updateTemplateState.isLoading ||
        updatePreferenceState.isLoading) ? (
        <div className="fixed inset-0 z-[60] grid place-items-center bg-black/15">
          <div className="rounded-xl bg-white px-4 py-3 text-sm text-(--text-primary-dark) shadow-lg">
            Processing request...
          </div>
        </div>
      ) : null}

      {toastMessage ? (
        <div
          className={`fixed right-6 top-6 z-[70] rounded-[0.75rem] border px-4 py-3 text-sm font-medium shadow-[0_12px_24px_rgba(15,23,42,0.10)] flex items-center gap-2 ${
            toastVariant === "error"
              ? "border-[#f3d4d4] bg-[#fff5f5] text-[#b42318]"
              : "border-[#d1fadf] bg-[#ecfdf3] text-[#027a48]"
          }`}
        >
          {toastVariant === "error" ? <AlertCircle size={16} /> : <CheckCircle2 size={16} />}
          {toastMessage}
        </div>
      ) : null}
    </div>
  );
};

export default StaffNotificationManagement;
