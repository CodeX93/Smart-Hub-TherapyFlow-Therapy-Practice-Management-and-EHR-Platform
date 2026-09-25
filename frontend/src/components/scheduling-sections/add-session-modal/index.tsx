import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "../../ui/button";
import { useSessionSystemOptions } from "@/hooks/useSystemOptionCatalog";
import { toSelectOptions, resolveOptionKey } from "@/utils/systemOptions";
import { shouldDisableClientForScheduling } from "@/utils/clientStatus";
import SessionModeSelector from "./SessionModeSelector";
import TimeSlots from "./TimeSlots";
import RecurrenceFields from "./RecurrenceFields";
import RecurrencePreviewTable from "./RecurrencePreviewTable";
import CustomSelect from "@/components/form/CustomSelect";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import CustomTextarea from "@/components/form/CustomTextarea";
import { Form } from "@/components/ui/form";
import {
  schedulingSchema,
  type SchedulingFormValues,
  type SchedulingSuccessData,
} from "@/schemas/scheduling.schema";
import {
  type CreateSessionPayload,
  useCreateAdminSessionMutation,
  useGetSessionByIdQuery,
  useUpdateSessionMutation,
} from "@/store/api/admin/dashboard.api";
import { useLazyGetAvailableRoomsQuery, useGetAdminRoomsQuery } from "@/store/api/admin/rooms.api";
import { useGetAdminBillingServicesQuery } from "@/store/api/admin/services.api";
import { useGetTherapistAvailabilitySlotsQuery } from "@/store/api/admin/therapistAvailability.api";
import {
  useGetAdminClientsQuery,
  useLazyGetAdminClientsQuery,
} from "@/store/api/admin/clients.api";
import {
  useLazyGetAdminUsersQuery,
  useLazyGetAdminUserByIdQuery,
  useLazyGetAdminUserProfileQuery,
} from "@/store/api/admin/users.api";
import {
  usePreviewRecurringSessionsMutation,
  useCreateRecurringSessionsMutation,
  useUpdateRecurringFutureSessionsMutation,
} from "@/store/api/admin/recurringSessions.api";
import { useGetAuthMeQuery } from "@/store/api/authApi";
import {
  useGetMyProfileQuery,
  useGetUserZoomStatusQuery,
  useGetZoomStatusQuery,
} from "@/store/api/userProfile.api";
import { openTherapistProfileSection } from "@/utils/therapistProfileModal";
import {
  useLazyGetSupervisorAssignmentsQuery,
  type SupervisorAssignmentSummary,
} from "@/store/api/admin/supervisorAssignments.api";
import { isSupervisorApiRole } from "@/utils/staffPermissions";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  calendarDateParts,
  formatTime12hInTimezone,
  formatTimezoneDisplayLabel,
  instantToCalendarDateInTimezone,
  resolveTherapistTimezone,
  type TherapistAvailabilityProfile,
} from "@/utils/therapistTimezone";
import { toSessionIsoDateTime } from "@/utils/sessionDateTime";
import {
  DEFAULT_RECURRENCE_FORM,
  type RecurrenceFormState,
  type RecurrencePreviewResponse,
} from "@/types/recurringSessions";
import {
  buildRecurrenceRuleRequest,
  canPreviewRecurrence,
  getDefaultWeeklyDay,
  isRecurringSeriesSession,
} from "@/utils/recurringSessions";
import { cn } from "@/lib/utils";

interface AddSessionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSchedule: (data: SchedulingSuccessData) => void;
  onToast?: (message: string, type: "success" | "error" | "info") => void;
  initialData?: Partial<SchedulingFormValues>;
  isEditSchedule?: boolean;
  isAdmin?: boolean;
  sessionId?: number | null;
  lockClientSelection?: boolean;
  /** Display label when client is locked (e.g. scheduling from a client profile). */
  lockedClientLabel?: string;
}

interface ClientSelectOption extends CustomSelectOption {
  assignedTherapistId?: number | null;
}

type SchedulePlanType = "one-time" | "recurring";

const AddSessionModal: React.FC<AddSessionModalProps> = ({
  isOpen,
  onClose,
  onSchedule,
  onToast,
  initialData,
  isEditSchedule = false,
  isAdmin = false,
  sessionId = null,
  lockClientSelection = false,
  lockedClientLabel,
}) => {
  const sessionCatalog = useSessionSystemOptions(isOpen);
  const sessionModeOptions = useMemo(() => {
    const source =
      sessionCatalog.sessionModeOptions.length > 0
        ? sessionCatalog.sessionModeOptions
        : [
            { optionKey: "in_person", optionLabel: "In Person" },
            { optionKey: "online", optionLabel: "Virtual" },
          ];

    // Hide phone/hybrid from the session modal UI (keep in_person + online/virtual only).
    return source.filter((option) => {
      const key = option.optionKey.trim().toLowerCase().replace(/[\s-]+/g, "_");
      return key !== "phone" && key !== "hybrid";
    });
  }, [sessionCatalog.sessionModeOptions]);
  const defaultSessionModeKey = useMemo(() => {
    const key = sessionCatalog.getDefaultSessionModeKey();
    return sessionModeOptions.some((option) => option.optionKey === key)
      ? key
      : sessionModeOptions[0]?.optionKey ?? "in_person";
  }, [sessionCatalog, sessionModeOptions]);
  const isInPersonSessionMode = sessionCatalog.isInPersonMode;

  const { data: authMeData } = useGetAuthMeQuery(undefined, { skip: !isOpen });
  const isSupervisor = isSupervisorApiRole(authMeData?.roles ?? []);
  const needsTherapistPicker = isAdmin || isSupervisor;

  const form = useForm<SchedulingFormValues>({
    resolver: zodResolver(schedulingSchema(needsTherapistPicker ? "admin" : "therapist")),
    defaultValues: {
      sessionType: "",
      client: "",
      service: "",
      therapist: "",
      sessionMode: defaultSessionModeKey,
      date: null,
      selectedTimeSlot: "",
      room: "",
      notes: "",
      ...initialData,
    },
  });
  const [submissionError, setSubmissionError] = useState<string | null>(null);
  const [clientOptions, setClientOptions] = useState<ClientSelectOption[]>([]);
  const [therapistOptions, setTherapistOptions] = useState<CustomSelectOption[]>([]);
  const [selectedTherapistProfile, setSelectedTherapistProfile] =
    useState<TherapistAvailabilityProfile | null>(null);
  const selectedClientId = form.watch("client");
  const sessionMode = form.watch("sessionMode");
  const dateValue = form.watch("date");
  const selectedTimeSlot = form.watch("selectedTimeSlot");
  const selectedServiceId = form.watch("service");
  const selectedTherapistId = form.watch("therapist");
  const { data: myProfileData } = useGetMyProfileQuery(undefined, {
    skip: !isOpen || needsTherapistPicker,
  });
  const [triggerGetSupervisorAssignments] = useLazyGetSupervisorAssignmentsQuery();
  const {
    data: sessionDetails,
    isFetching: isFetchingSessionDetails,
    refetch: refetchSessionDetails,
  } = useGetSessionByIdQuery(sessionId ?? 0, {
    skip: !isOpen || !isEditSchedule || !sessionId,
    refetchOnMountOrArgChange: true,
  });
  const [triggerGetClients] = useLazyGetAdminClientsQuery();
  const [isFetchingClients, setIsFetchingClients] = useState(false);
  const [clientsPage, setClientsPage] = useState(0);
  const [clientsTotalPages, setClientsTotalPages] = useState(1);

  const schedulingTherapistId = useMemo(() => {
    if (needsTherapistPicker) {
      const parsed = Number.parseInt(selectedTherapistId || "", 10);
      return Number.isFinite(parsed) ? parsed : undefined;
    }
    const selfId = authMeData?.user?.id;
    return typeof selfId === "number" && Number.isFinite(selfId) ? selfId : undefined;
  }, [authMeData?.user?.id, needsTherapistPicker, selectedTherapistId]);

  // An online session is a Zoom meeting created under the hosting therapist's credentials.
  // Without them the booking cannot produce a link, so the mode is not offered at all.
  const isSchedulingForSelf =
    !needsTherapistPicker ||
    (schedulingTherapistId != null && schedulingTherapistId === authMeData?.user?.id);
  const { data: ownZoomStatus } = useGetZoomStatusQuery(undefined, {
    skip: !isOpen || !isSchedulingForSelf,
  });
  const { data: therapistZoomStatus, isError: isTherapistZoomStatusError } =
    useGetUserZoomStatusQuery(schedulingTherapistId as number, {
      skip: !isOpen || isSchedulingForSelf || schedulingTherapistId == null,
    });

  const onlineSessionAvailable = useMemo(() => {
    if (isSchedulingForSelf) {
      // Undefined while the status is still loading: stay open rather than flicker the
      // option out from under someone who does have Zoom connected.
      return ownZoomStatus?.configured ?? true;
    }
    if (schedulingTherapistId == null) return true;
    // Reading another user needs USER_VIEW. A caller without it learns nothing, so the
    // option stays available and the server remains the authority.
    if (isTherapistZoomStatusError) return true;
    return therapistZoomStatus?.configured ?? true;
  }, [
    isSchedulingForSelf,
    isTherapistZoomStatusError,
    ownZoomStatus?.configured,
    schedulingTherapistId,
    therapistZoomStatus?.configured,
  ]);

  const onlineUnavailableReason = isSchedulingForSelf
    ? "Your Zoom account isn't connected, so no meeting link can be created for an online session."
    : "This therapist's Zoom account isn't connected, so no meeting link can be created for an online session.";

  // Picking a therapist who has no Zoom after choosing Online would otherwise leave a
  // selection that cannot be booked, so fall back to the first in-person mode.
  useEffect(() => {
    if (onlineSessionAvailable) return;
    if (isInPersonSessionMode(sessionMode)) return;
    const inPersonOption = sessionModeOptions.find((option) =>
      isInPersonSessionMode(option.optionKey),
    );
    if (!inPersonOption) return;
    form.setValue("sessionMode", inPersonOption.optionKey, { shouldValidate: false });
  }, [form, isInPersonSessionMode, onlineSessionAvailable, sessionMode, sessionModeOptions]);

  const clientListParams = useMemo(
    () => ({
      page: 1,
      pageSize: 25,
      sortBy: "fullName" as const,
      sortOrder: "asc" as const,
      status: "active",
      includeUnassigned: true,
      ...(schedulingTherapistId != null
        ? { therapistId: schedulingTherapistId }
        : {}),
    }),
    [schedulingTherapistId],
  );

  const skipClientListQuery =
    !isOpen || (!needsTherapistPicker && authMeData == null);

  const {
    data: initialClientsPage,
    isFetching: isFetchingInitialClients,
    isError: isInitialClientsError,
    error: initialClientsError,
  } = useGetAdminClientsQuery(clientListParams, {
    skip: skipClientListQuery,
    refetchOnMountOrArgChange: true,
  });
  const [triggerGetUsers] = useLazyGetAdminUsersQuery();
  const [triggerGetUserById] = useLazyGetAdminUserByIdQuery();
  const [triggerGetUserProfile] = useLazyGetAdminUserProfileQuery();
  const { data: servicesData = [] } = useGetAdminBillingServicesQuery(undefined, {
    skip: !isOpen,
  });
  const { data: roomsData = [] } = useGetAdminRoomsQuery(undefined, {
    skip: !isOpen,
  });
  const [createSession, { isLoading: isCreatingSession }] =
    useCreateAdminSessionMutation();
  const [updateSession, { isLoading: isUpdatingSession }] =
    useUpdateSessionMutation();
  const [triggerGetAvailableRooms, { isFetching: isFetchingAvailableRooms }] =
    useLazyGetAvailableRoomsQuery();
  const [availableRoomOptions, setAvailableRoomOptions] = useState<CustomSelectOption[]>(
    [],
  );
  const [recurrenceForm, setRecurrenceForm] =
    useState<RecurrenceFormState>(DEFAULT_RECURRENCE_FORM);
  const [schedulePlanType, setSchedulePlanType] = useState<SchedulePlanType>("one-time");
  const [wizardStep, setWizardStep] = useState<1 | 2>(1);
  const [editScope, setEditScope] = useState<"single" | "future">("single");
  const [previewData, setPreviewData] = useState<RecurrencePreviewResponse | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [previewRecurringSessions, { isLoading: isPreviewLoading }] =
    usePreviewRecurringSessionsMutation();
  const [createRecurringSessions, { isLoading: isCreatingRecurring }] =
    useCreateRecurringSessionsMutation();
  const [updateRecurringFutureSessions, { isLoading: isUpdatingRecurringFuture }] =
    useUpdateRecurringFutureSessionsMutation();

  const recurrenceGroupId = sessionDetails?.recurrenceGroupId ?? null;
  const isSeriesSession = isRecurringSeriesSession(recurrenceGroupId);
  const sessionTimezone = useMemo(
    () =>
      resolveTherapistTimezone(
        selectedTherapistProfile?.timezone || myProfileData?.timezone,
      ),
    [myProfileData?.timezone, selectedTherapistProfile?.timezone],
  );
  const sessionTimezoneLabel = useMemo(
    () => formatTimezoneDisplayLabel(sessionTimezone),
    [sessionTimezone],
  );
  const selectedSessionType = form.watch("sessionType");
  const selectedRoom = form.watch("room");
  const isSessionDetailsComplete = useMemo(() => {
    const hasSessionType = Boolean(selectedSessionType?.trim());
    const hasClient = Boolean(selectedClientId?.trim());
    const hasService = Boolean(selectedServiceId?.trim());
    const hasTherapist = needsTherapistPicker ? Boolean(selectedTherapistId?.trim()) : true;
    const hasDate = dateValue !== null;
    const hasTimeSlot = Boolean(selectedTimeSlot?.trim());
    const hasRoom = !isInPersonSessionMode(sessionMode) || Boolean(selectedRoom?.trim());

    return (
      hasSessionType &&
      hasClient &&
      hasService &&
      hasTherapist &&
      hasDate &&
      hasTimeSlot &&
      hasRoom
    );
  }, [
    dateValue,
    needsTherapistPicker,
    selectedClientId,
    selectedRoom,
    selectedServiceId,
    selectedSessionType,
    selectedTherapistId,
    selectedTimeSlot,
    sessionMode,
  ]);

  const isRecurringStepComplete = useMemo(
    () =>
      canPreviewRecurrence(
        { ...recurrenceForm, isRecurring: true },
        dateValue,
        selectedTimeSlot,
      ),
    [dateValue, recurrenceForm, selectedTimeSlot],
  );

  const isRequiredFieldsFilled = useMemo(() => {
    if (!isSessionDetailsComplete) return false;
    if (isEditSchedule || schedulePlanType === "one-time") return true;
    return (
      isRecurringStepComplete &&
      Boolean(previewData) &&
      !isPreviewLoading
    );
  }, [
    isEditSchedule,
    isPreviewLoading,
    isRecurringStepComplete,
    isSessionDetailsComplete,
    previewData,
    schedulePlanType,
  ]);

  const isRecurringCreateFlow =
    !isEditSchedule && schedulePlanType === "recurring";
  const showSessionStep = isEditSchedule || !isRecurringCreateFlow || wizardStep === 1;
  const showRecurringStep = isRecurringCreateFlow && wizardStep === 2;

  const sessionTypeOptions = useMemo(() => {
    const catalogOptions = toSelectOptions(sessionCatalog.serviceTypeOptions);
    const normalizedSelectedSessionType = selectedSessionType?.trim();

    if (
      !normalizedSelectedSessionType ||
      catalogOptions.some((option) => option.value === normalizedSelectedSessionType)
    ) {
      return catalogOptions;
    }

    return [
      { value: normalizedSelectedSessionType, label: normalizedSelectedSessionType },
      ...catalogOptions,
    ];
  }, [selectedSessionType, sessionCatalog.serviceTypeOptions]);

  const isSessionFormBootstrapping =
    sessionCatalog.isLoading ||
    (isEditSchedule && isFetchingSessionDetails && !sessionDetails);
  const wasOpenRef = useRef(false);
  const previousSelectedClientIdRef = useRef<string | null>(null);
  const editHydrationKeyRef = useRef<string | null>(null);

  useEffect(() => {
    const didOpen = isOpen && !wasOpenRef.current;

    if (didOpen) {
      setSubmissionError(null);
      setRecurrenceForm(DEFAULT_RECURRENCE_FORM);
      setSchedulePlanType("one-time");
      setWizardStep(1);
      setEditScope("single");
      setPreviewData(null);
      setPreviewError(null);
      editHydrationKeyRef.current = null;
      form.reset({
        sessionType: "",
        client: "",
        service: "",
        therapist: "",
        sessionMode: defaultSessionModeKey,
        date: null,
        selectedTimeSlot: "",
        room: "",
        notes: "",
        ...initialData,
      });
    }

    wasOpenRef.current = isOpen;
  }, [defaultSessionModeKey, form, initialData, isOpen]);

  useEffect(() => {
    if (!isOpen || isEditSchedule) return;
    form.setValue("selectedTimeSlot", "");
  }, [
    form,
    isEditSchedule,
    isOpen,
    selectedTherapistId,
    selectedServiceId,
    sessionMode,
    dateValue,
    sessionTimezone,
  ]);

  useEffect(() => {
    if (!isOpen || !isEditSchedule || !sessionId) return;
    void refetchSessionDetails();
  }, [isOpen, isEditSchedule, sessionId, refetchSessionDetails]);

  useEffect(() => {
    if (!isOpen || !isEditSchedule || !sessionDetails) return;

    const editTimezone = resolveTherapistTimezone(
      selectedTherapistProfile?.timezone || myProfileData?.timezone,
    );
    const assignedRoom = sessionDetails.roomId
      ? roomsData.find((room) => room.id === sessionDetails.roomId)
      : undefined;
    // Migrated/online sessions sometimes store in-person mode with a virtual room.
    // Prefer the room type so edit mode matches the appointment's real setup.
    const inferredOnlineFromRoom = assignedRoom?.roomType === "VIRTUAL";
    const resolvedSessionMode = inferredOnlineFromRoom
      ? sessionCatalog.resolveSessionModeKey("virtual")
      : resolveOptionKey(
          sessionCatalog.sessionModeOptions,
          sessionDetails.sessionMode || "",
        ) || defaultSessionModeKey;
    const resolvedSessionType = sessionCatalog.isReady
      ? resolveOptionKey(
          sessionCatalog.serviceTypeOptions,
          sessionDetails.sessionType || "",
        ) || sessionDetails.sessionType || ""
      : sessionDetails.sessionType || "";
    const isInPerson = isInPersonSessionMode(resolvedSessionMode);
    const roomValue =
      isInPerson && sessionDetails.roomId ? String(sessionDetails.roomId) : "";

    const hydrationKey = [
      sessionDetails.id,
      sessionDetails.sessionDate,
      sessionDetails.clientId,
      sessionDetails.roomId,
      sessionDetails.serviceId,
      sessionDetails.therapistId,
      sessionDetails.sessionMode,
      assignedRoom?.roomType ?? "",
      resolvedSessionMode,
      resolvedSessionType,
      editTimezone,
      sessionCatalog.isReady ? "ready" : "pending",
    ].join(":");

    // Re-hydrate when session details, room type, or therapist timezone resolve.
    // Do not skip for isDirty — room-availability clears must not block prefill.
    if (editHydrationKeyRef.current === hydrationKey) return;

    const calendarDate = sessionDetails.sessionDate
      ? instantToCalendarDateInTimezone(sessionDetails.sessionDate, editTimezone)
      : null;

    form.reset({
      sessionType: resolvedSessionType,
      client: sessionDetails.clientId != null ? String(sessionDetails.clientId) : "",
      service: sessionDetails.serviceId != null ? String(sessionDetails.serviceId) : "",
      therapist:
        sessionDetails.therapistId != null ? String(sessionDetails.therapistId) : "",
      sessionMode: resolvedSessionMode,
      date: calendarDate,
      selectedTimeSlot: sessionDetails.sessionDate
        ? formatTime12hInTimezone(sessionDetails.sessionDate, editTimezone)
        : "",
      room: roomValue,
      notes: sessionDetails.notes || "",
    });
    editHydrationKeyRef.current = hydrationKey;
  }, [
    defaultSessionModeKey,
    form,
    isEditSchedule,
    isInPersonSessionMode,
    isOpen,
    myProfileData?.timezone,
    roomsData,
    selectedTherapistProfile?.timezone,
    sessionCatalog,
    sessionDetails,
  ]);

  useEffect(() => {
    if (
      !recurrenceForm.isRecurring ||
      recurrenceForm.recurrenceType !== "weekly" ||
      schedulePlanType !== "recurring"
    ) {
      return;
    }
    if (!dateValue) return;
    if (recurrenceForm.daysOfWeek.length > 0) return;
    setRecurrenceForm((previous) => ({
      ...previous,
      daysOfWeek: getDefaultWeeklyDay(dateValue, sessionTimezone),
    }));
  }, [
    dateValue,
    recurrenceForm.daysOfWeek.length,
    recurrenceForm.isRecurring,
    recurrenceForm.recurrenceType,
    schedulePlanType,
    sessionTimezone,
  ]);

  const fetchRecurringPreview = useCallback(
    (recurrenceState: RecurrenceFormState) => {
      const recurrence = { ...recurrenceState, isRecurring: true };

      if (!canPreviewRecurrence(recurrence, dateValue, selectedTimeSlot)) {
        setPreviewData(null);
        setPreviewError(null);
        return;
      }

      const therapistId = needsTherapistPicker
        ? Number.parseInt(selectedTherapistId || "", 10)
        : authMeData?.user?.id;
      const clientId = Number.parseInt(form.getValues("client") || "", 10);
      const serviceId = Number.parseInt(selectedServiceId || "", 10);
      const roomId = form.getValues("room")
        ? Number.parseInt(form.getValues("room") || "", 10)
        : undefined;

      if (!dateValue || !therapistId || !clientId || !selectedServiceId) return;

      const body = buildRecurrenceRuleRequest({
        clientId,
        therapistId,
        serviceId,
        roomId: isInPersonSessionMode(sessionMode) ? roomId : undefined,
        sessionMode,
        isInPersonMode: isInPersonSessionMode,
        sessionType: form.getValues("sessionType"),
        notes: form.getValues("notes"),
        sessionDate: dateValue,
        sessionTime12h: selectedTimeSlot,
        timezone: sessionTimezone,
        recurrence,
      });

      if (!body) return;

      void previewRecurringSessions(body)
        .unwrap()
        .then((response) => {
          setPreviewData(response);
          setPreviewError(null);
        })
        .catch((error) => {
          setPreviewData(null);
          setPreviewError(getApiErrorMessage(error));
        });
    },
    [
      authMeData?.user?.id,
      dateValue,
      form,
      needsTherapistPicker,
      previewRecurringSessions,
      selectedServiceId,
      selectedTherapistId,
      selectedTimeSlot,
      sessionMode,
      sessionTimezone,
    ],
  );

  useEffect(() => {
    if (!isOpen || isEditSchedule || schedulePlanType !== "recurring" || wizardStep !== 2) {
      setPreviewData(null);
      setPreviewError(null);
      return;
    }

    const timer = window.setTimeout(() => {
      fetchRecurringPreview(recurrenceForm);
    }, 400);

    return () => window.clearTimeout(timer);
  }, [
    fetchRecurringPreview,
    isEditSchedule,
    isOpen,
    recurrenceForm,
    schedulePlanType,
    wizardStep,
  ]);

  const mapClientToOption = useCallback(
    (client: {
      id: number;
      fullName: string;
      clientId?: string;
      stage?: string;
      status?: string;
      assignedTherapistId?: number | null;
    }): ClientSelectOption | null => {
      // Only active/on-hold clients are schedulable; hide the rest instead of disabling.
      if (
        shouldDisableClientForScheduling({
          clientStatus: client.status,
          clientStage: client.stage,
        })
      ) {
        return null;
      }

      if (schedulingTherapistId != null) {
        const assignedId = client.assignedTherapistId ?? null;
        if (assignedId != null && assignedId !== schedulingTherapistId) {
          return null;
        }
      }

      return {
        value: String(client.id),
        label: `${client.fullName}${client.clientId ? ` (${client.clientId})` : ""}`,
        assignedTherapistId: client.assignedTherapistId ?? null,
      };
    },
    [schedulingTherapistId],
  );

  useEffect(() => {
    if (!isOpen) {
      setClientOptions([]);
      setClientsPage(0);
      setClientsTotalPages(1);
      return;
    }
    if (!initialClientsPage) return;

    setClientOptions(
      initialClientsPage.items
        .map(mapClientToOption)
        .filter((option): option is ClientSelectOption => option != null),
    );
    setClientsPage(initialClientsPage.page);
    setClientsTotalPages(Math.max(1, initialClientsPage.totalPages || 1));
  }, [initialClientsPage, isOpen, mapClientToOption]);

  useEffect(() => {
    if (!isOpen || !isInitialClientsError || !initialClientsError) return;
    setSubmissionError(getApiErrorMessage(initialClientsError));
  }, [initialClientsError, isInitialClientsError, isOpen]);

  const loadClientsPage = useCallback(
    async (page: number) => {
      if (page <= 1) return;
      setIsFetchingClients(true);
      try {
        const response = await triggerGetClients({
          ...clientListParams,
          page,
        }).unwrap();

        const nextOptions = response.items
          .map(mapClientToOption)
          .filter((option): option is ClientSelectOption => option != null);
        setClientOptions((prev) => {
          const existing = new Set(prev.map((entry) => entry.value));
          const merged = [...prev];
          nextOptions.forEach((entry) => {
            if (!existing.has(entry.value)) merged.push(entry);
          });
          return merged;
        });
        setClientsPage(response.page);
        setClientsTotalPages(Math.max(1, response.totalPages || 1));
      } catch (error) {
        setSubmissionError(getApiErrorMessage(error));
      } finally {
        setIsFetchingClients(false);
      }
    },
    [clientListParams, mapClientToOption, triggerGetClients],
  );

  const hasMoreClients = clientsPage < clientsTotalPages;
  const isLoadingClients = isFetchingInitialClients || isFetchingClients;
  const handleClientsMenuScrollToEnd = useCallback(() => {
    if (!hasMoreClients || isLoadingClients) return;
    void loadClientsPage(clientsPage + 1);
  }, [clientsPage, hasMoreClients, isLoadingClients, loadClientsPage]);

  const clientSelectOptions = useMemo(() => {
    const ensureClientOption = (
      value: string,
      label: string,
      assignedTherapistId?: number | null,
    ): ClientSelectOption[] => {
      if (clientOptions.some((option) => option.value === value)) {
        return clientOptions;
      }
      return [
        {
          value,
          label,
          assignedTherapistId: assignedTherapistId ?? null,
        },
        ...clientOptions,
      ];
    };

    if (isEditSchedule && sessionDetails?.clientId != null) {
      return ensureClientOption(
        String(sessionDetails.clientId),
        sessionDetails.clientName || `Client #${sessionDetails.clientId}`,
        null,
      );
    }

    if (lockClientSelection && initialData?.client) {
      const lockedValue = String(initialData.client);
      const assignedTherapistId = initialData.therapist
        ? Number.parseInt(initialData.therapist, 10)
        : null;
      return ensureClientOption(
        lockedValue,
        lockedClientLabel || `Client #${lockedValue}`,
        Number.isFinite(assignedTherapistId) ? assignedTherapistId : null,
      );
    }

    return clientOptions;
  }, [
    clientOptions,
    initialData?.client,
    initialData?.therapist,
    isEditSchedule,
    lockClientSelection,
    lockedClientLabel,
    sessionDetails?.clientId,
    sessionDetails?.clientName,
  ]);

  const selectedClientAssignedTherapistId = clientSelectOptions.find(
    (option) => option.value === selectedClientId,
  )?.assignedTherapistId;
  const therapistSelectLocked =
    !isEditSchedule &&
    selectedClientAssignedTherapistId != null &&
    Number.isFinite(Number(selectedClientAssignedTherapistId));

  useEffect(() => {
    if (!isOpen || !needsTherapistPicker || isEditSchedule) return;
    if (!selectedClientId) {
      previousSelectedClientIdRef.current = null;
      return;
    }
    if (clientSelectOptions.length === 0) return;

    const didClientChange = previousSelectedClientIdRef.current !== selectedClientId;

    if (!didClientChange) {
      return;
    }

    const matchedClient = clientSelectOptions.find(
      (option) => option.value === selectedClientId,
    );
    const assignedTherapistId = matchedClient?.assignedTherapistId;
    let therapistValue = "";
    if (assignedTherapistId) {
      therapistValue = String(assignedTherapistId);
    }
    form.setValue("therapist", therapistValue);
    previousSelectedClientIdRef.current = selectedClientId;
  }, [
    clientSelectOptions,
    form,
    needsTherapistPicker,
    isEditSchedule,
    isOpen,
    selectedClientId,
    therapistOptions,
  ]);

  useEffect(() => {
    if (!isOpen) return;
    // Staff scheduling passes isAdmin=true for supervisors too — wait for /me before
    // choosing admin users vs supervisor-assignments.
    if (needsTherapistPicker && !authMeData) return;

    if (isSupervisor) {
      void triggerGetSupervisorAssignments({ active: true })
        .unwrap()
        .then((assignments: SupervisorAssignmentSummary[]) => {
          const seen = new Set<string>();
          const options: CustomSelectOption[] = [];
          for (const assignment of assignments) {
            if (!assignment.isActive || !assignment.therapist?.id) continue;
            const value = String(assignment.therapist.id);
            if (seen.has(value)) continue;
            seen.add(value);
            options.push({
              value,
              label:
                assignment.therapist.fullName ||
                assignment.therapist.email ||
                assignment.therapist.username,
            });
          }
          setTherapistOptions(options);
        })
        .catch((error: unknown) => {
          setTherapistOptions([]);
          setSubmissionError(getApiErrorMessage(error));
        });
      return;
    }

    if (!isAdmin) return;

    void triggerGetUsers({
      page: 1,
      pageSize: 200,
      role: "THERAPIST",
    })
      .unwrap()
      .then((response) => {
        setTherapistOptions(
          response.items
            .filter((user) => user.roles.includes("THERAPIST"))
            .map((user) => ({
              value: String(user.id),
              label: user.fullName || user.email || user.username,
              disabled: !user.active,
            })),
        );
      })
      .catch((error) => {
        setTherapistOptions([]);
        setSubmissionError(getApiErrorMessage(error));
      });
  }, [
    authMeData,
    isAdmin,
    isOpen,
    isSupervisor,
    needsTherapistPicker,
    triggerGetSupervisorAssignments,
    triggerGetUsers,
  ]);

  useEffect(() => {
    if (!isOpen) return;
    if (!needsTherapistPicker) {
      setSelectedTherapistProfile({
        workingHours: myProfileData?.workingHours ?? null,
        sessionDuration: myProfileData?.sessionDuration ?? 60,
        timezone: myProfileData?.timezone ?? null,
        availablePhysicalRoomIds: myProfileData?.availablePhysicalRoomIds ?? [],
      });
      return;
    }

    if (!authMeData) return;

    if (!selectedTherapistId) {
      setSelectedTherapistProfile(null);
      return;
    }

    const therapistIdNum = Number.parseInt(selectedTherapistId, 10);
    if (!Number.isFinite(therapistIdNum)) {
      setSelectedTherapistProfile(null);
      return;
    }

    // Supervisors lack USER_VIEW / admin users API — load scheduling fields via profile endpoint.
    // Backend caseload check limits this to assigned therapists only.
    if (isSupervisor) {
      void triggerGetUserProfile(therapistIdNum)
        .unwrap()
        .then((profile) => {
          setSelectedTherapistProfile({
            workingHours: profile.workingHours ?? null,
            sessionDuration: profile.sessionDuration ?? 60,
            timezone: profile.timezone ?? null,
            availablePhysicalRoomIds: Array.isArray(profile.availablePhysicalRoomIds)
              ? profile.availablePhysicalRoomIds
                  .map((value) => Number.parseInt(String(value), 10))
                  .filter((value) => Number.isFinite(value))
              : [],
          });
        })
        .catch((error) => {
          setSelectedTherapistProfile(null);
          setSubmissionError(getApiErrorMessage(error));
        });
      return;
    }

    void triggerGetUserById(therapistIdNum)
      .unwrap()
      .then((user) => {
        const profile =
          user.profile && typeof user.profile === "object"
            ? (user.profile as Record<string, unknown>)
            : null;
        setSelectedTherapistProfile({
          workingHours:
            profile && typeof profile.workingHours === "string"
              ? profile.workingHours
              : null,
          sessionDuration:
            profile && typeof profile.sessionDuration === "number"
              ? profile.sessionDuration
              : 60,
          timezone:
            profile && typeof profile.timezone === "string" ? profile.timezone : null,
          availablePhysicalRoomIds:
            profile && Array.isArray(profile.availablePhysicalRoomIds)
              ? profile.availablePhysicalRoomIds
                  .map((value) => Number.parseInt(String(value), 10))
                  .filter((value) => Number.isFinite(value))
              : [],
        });
      })
      .catch((error) => {
        setSelectedTherapistProfile(null);
        setSubmissionError(getApiErrorMessage(error));
      });
  }, [
    authMeData,
    needsTherapistPicker,
    isOpen,
    isSupervisor,
    myProfileData?.availablePhysicalRoomIds,
    myProfileData?.sessionDuration,
    myProfileData?.timezone,
    myProfileData?.workingHours,
    selectedTherapistId,
    triggerGetUserById,
    triggerGetUserProfile,
  ]);

  const serviceOptions = useMemo(
    () =>
      servicesData
        .filter((service) => service.isActive)
        .map((service) => ({
          value: String(service.id),
          label: service.serviceName,
          duration: `${service.durationInMinutes ?? 60} min`,
          price: `$${service.baseRate.toFixed(2)}`,
        })),
    [servicesData],
  );

  const selectedService = useMemo(
    () => servicesData.find((service) => String(service.id) === selectedServiceId) ?? null,
    [selectedServiceId, servicesData],
  );

  const availabilityTherapistId = useMemo(() => {
    if (needsTherapistPicker) {
      const parsed = Number.parseInt(selectedTherapistId || "", 10);
      return Number.isFinite(parsed) ? parsed : null;
    }
    const selfId = authMeData?.user?.id;
    return typeof selfId === "number" && Number.isFinite(selfId) ? selfId : null;
  }, [authMeData?.user?.id, needsTherapistPicker, selectedTherapistId]);

  const availabilityDateIso = useMemo(() => {
    if (!dateValue) return null;
    const { year, month, day } = calendarDateParts(dateValue);
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }, [dateValue]);

  const availabilityServiceId = useMemo(() => {
    const parsed = Number.parseInt(selectedServiceId || "", 10);
    return Number.isFinite(parsed) ? parsed : null;
  }, [selectedServiceId]);

  const availabilitySessionType = isInPersonSessionMode(sessionMode)
    ? "in-person"
    : "online";

  const canFetchAvailabilitySlots = Boolean(
    isOpen &&
      availabilityTherapistId &&
      availabilityDateIso &&
      availabilityServiceId,
  );

  const {
    currentData: availabilitySlotsData,
    isFetching: isFetchingAvailabilitySlots,
    isError: isAvailabilitySlotsError,
  } = useGetTherapistAvailabilitySlotsQuery(
    {
      therapistId: availabilityTherapistId as number,
      date: availabilityDateIso as string,
      serviceId: availabilityServiceId as number,
      sessionType: availabilitySessionType,
    },
    {
      skip: !canFetchAvailabilitySlots,
      refetchOnMountOrArgChange: true,
    },
  );

  const availableTimeSlots = useMemo(() => {
    if (!canFetchAvailabilitySlots) return [];

    const slots = (availabilitySlotsData ?? [])
      .filter((slot) => slot.available)
      .map((slot) => slot.localTimeLabel)
      .filter(Boolean);

    const uniqueSlots = Array.from(new Set(slots));

    // Keep the currently scheduled time visible while editing even if the API
    // marks it busy (the existing session occupies that slot).
    if (
      isEditSchedule &&
      selectedTimeSlot &&
      !uniqueSlots.some(
        (slot) => slot.toLowerCase() === selectedTimeSlot.toLowerCase(),
      )
    ) {
      return [selectedTimeSlot, ...uniqueSlots];
    }

    return uniqueSlots;
  }, [
    availabilitySlotsData,
    canFetchAvailabilitySlots,
    isEditSchedule,
    selectedTimeSlot,
  ]);

  const timeSlotsEmptyMessage = !selectedServiceId
    ? "Select a service to see available times"
    : needsTherapistPicker && !selectedTherapistId
      ? "Select a therapist to see available times"
      : isAvailabilitySlotsError
        ? "Unable to load available times"
        : "No available time slots";

  const roomOptions = useMemo(() => {
    // Room selection is only used for in-person sessions.
    if (!isInPersonSessionMode(sessionMode)) {
      return [];
    }
    const availableRoomIds = new Set(selectedTherapistProfile?.availablePhysicalRoomIds ?? []);
    const profileRooms = roomsData
      .filter(
        (room) =>
          room.isActive &&
          room.roomType === "PHYSICAL" &&
          availableRoomIds.has(room.id),
      )
      .map((room) => ({
        value: String(room.id),
        label: `${room.roomNumber} - ${room.roomName}`,
      }));
    const baseOptions =
      availableRoomOptions.length > 0 ? availableRoomOptions : profileRooms;

    // Keep the currently booked room visible while editing even if availability
    // briefly excludes it (e.g. before excludeSessionId is applied).
    if (
      isEditSchedule &&
      sessionDetails?.roomId &&
      sessionDetails.roomName
    ) {
      const currentRoomValue = String(sessionDetails.roomId);
      if (!baseOptions.some((option) => option.value === currentRoomValue)) {
        const currentFromRooms = roomsData.find(
          (room) => room.id === sessionDetails.roomId,
        );
        return [
          {
            value: currentRoomValue,
            label: currentFromRooms
              ? `${currentFromRooms.roomNumber} - ${currentFromRooms.roomName}`
              : sessionDetails.roomName,
          },
          ...baseOptions,
        ];
      }
    }

    return baseOptions;
  }, [
    availableRoomOptions,
    isEditSchedule,
    isInPersonSessionMode,
    roomsData,
    selectedTherapistProfile?.availablePhysicalRoomIds,
    sessionDetails?.roomId,
    sessionDetails?.roomName,
    sessionMode,
  ]);

  useEffect(() => {
    if (!isOpen) return;
    // Online/virtual sessions never need a room — skip room loading entirely.
    if (!isInPersonSessionMode(sessionMode)) {
      setAvailableRoomOptions([]);
      if (form.getValues("room")) {
        form.setValue("room", "", { shouldDirty: false, shouldValidate: false });
      }
      return;
    }
    if (!dateValue || !selectedTimeSlot) {
      setAvailableRoomOptions([]);
      return;
    }

    const therapistId = needsTherapistPicker
      ? Number.parseInt(selectedTherapistId || "", 10)
      : authMeData?.user?.id;
    if (!therapistId || !Number.isFinite(therapistId)) {
      setAvailableRoomOptions([]);
      return;
    }

    const sessionDate = toSessionIsoDateTime(
      dateValue,
      selectedTimeSlot,
      sessionTimezone,
    );
    const serviceId = selectedServiceId ? Number.parseInt(selectedServiceId, 10) : undefined;
    const duration =
      selectedService?.durationInMinutes ??
      selectedTherapistProfile?.sessionDuration ??
      60;
    const editingSessionId =
      isEditSchedule && sessionId && Number.isFinite(sessionId) ? sessionId : undefined;
    const preserveRoomValue =
      isEditSchedule && sessionDetails?.roomId != null
        ? String(sessionDetails.roomId)
        : null;

    void triggerGetAvailableRooms({
      therapistId,
      sessionDate,
      sessionType: "in-person",
      serviceId: Number.isFinite(serviceId as number) ? serviceId : undefined,
      duration,
      excludeSessionId: editingSessionId,
    })
      .unwrap()
      .then((rooms) => {
        const options = rooms
          .filter((room) => room.isActive && room.roomType === "PHYSICAL")
          .map((room) => ({
            value: String(room.id),
            label: `${room.roomNumber} - ${room.roomName}`,
          }));
        setAvailableRoomOptions(options);
        const currentRoom = form.getValues("room");
        if (
          currentRoom &&
          !options.some((option) => option.value === currentRoom) &&
          currentRoom !== preserveRoomValue
        ) {
          form.setValue("room", "", { shouldDirty: false, shouldValidate: false });
        }
      })
      .catch(() => {
        // On failure keep any currently selected edit room; fall back to profile rooms.
        setAvailableRoomOptions([]);
        if (!isEditSchedule) {
          form.setValue("room", "", { shouldDirty: false, shouldValidate: false });
        }
      });
  }, [
    authMeData?.user?.id,
    dateValue,
    form,
    isEditSchedule,
    isInPersonSessionMode,
    needsTherapistPicker,
    isOpen,
    selectedService?.durationInMinutes,
    selectedServiceId,
    selectedTherapistId,
    selectedTherapistProfile?.sessionDuration,
    selectedTimeSlot,
    sessionDetails?.roomId,
    sessionId,
    sessionMode,
    sessionTimezone,
    triggerGetAvailableRooms,
  ]);

  const sessionSummary = useMemo(() => {
    if (!isSessionDetailsComplete || !dateValue) return null;

    const clientLabel =
      clientSelectOptions.find((option) => option.value === selectedClientId)?.label ||
      "Selected client";
    const serviceLabel =
      serviceOptions.find((option) => option.value === selectedServiceId)?.label ||
      "Selected service";
    const roomLabel =
      roomOptions.find((option) => option.value === selectedRoom)?.label || "";
    const modeLabel =
      sessionCatalog.resolveSessionModeLabel(sessionMode) ||
      (isInPersonSessionMode(sessionMode) ? "In-person" : "Virtual");

    return {
      clientLabel,
      serviceLabel,
      modeLabel,
      dateLabel: dateValue.toLocaleDateString(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric",
        year: "numeric",
      }),
      timeLabel: selectedTimeSlot,
      roomLabel,
    };
  }, [
    clientSelectOptions,
    dateValue,
    isSessionDetailsComplete,
    roomOptions,
    selectedClientId,
    selectedRoom,
    selectedServiceId,
    selectedTimeSlot,
    serviceOptions,
    sessionMode,
  ]);

  if (!isOpen) return null;

  const handleSchedulePlanTypeChange = (nextPlanType: SchedulePlanType) => {
    setSchedulePlanType(nextPlanType);
    setWizardStep(1);
    setSubmissionError(null);
    setPreviewData(null);
    setPreviewError(null);
    setRecurrenceForm({
      ...DEFAULT_RECURRENCE_FORM,
      isRecurring: nextPlanType === "recurring",
      daysOfWeek:
        nextPlanType === "recurring" && dateValue
          ? getDefaultWeeklyDay(dateValue, sessionTimezone)
          : [],
    });
  };

  const handleContinueToRecurringStep = () => {
    if (!isSessionDetailsComplete) return;
    setSubmissionError(null);
    setPreviewData(null);
    setPreviewError(null);

    const nextRecurrence: RecurrenceFormState = {
      ...recurrenceForm,
      isRecurring: true,
      daysOfWeek:
        recurrenceForm.daysOfWeek.length > 0 || !dateValue
          ? recurrenceForm.daysOfWeek
          : getDefaultWeeklyDay(dateValue, sessionTimezone),
    };

    setRecurrenceForm(nextRecurrence);
    setWizardStep(2);
    fetchRecurringPreview(nextRecurrence);
  };

  const buildSuccessData = (data: SchedulingFormValues): SchedulingSuccessData => {
    const clientLabel =
      clientSelectOptions.find((option) => option.value === data.client)?.label || data.client;
    const serviceLabel =
      serviceOptions.find((option) => option.value === data.service)?.label || data.service;
    const roomLabel =
      roomOptions.find((option) => option.value === data.room)?.label || data.room;
    const therapistLabel = needsTherapistPicker
      ? therapistOptions.find((option) => option.value === data.therapist)?.label || ""
      : authMeData?.user?.fullName || "";

    return {
      ...data,
      client: clientLabel,
      service: `${serviceLabel}${selectedService ? ` $${selectedService.baseRate.toFixed(2)}` : ""}`,
      therapist: therapistLabel,
      room: roomLabel,
    };
  };

  const onSubmit = (data: SchedulingFormValues) => {
    setSubmissionError(null);

    const therapistId = needsTherapistPicker
      ? Number.parseInt(data.therapist || "", 10)
      : authMeData?.user?.id;
    const clientId = Number.parseInt(data.client, 10);
    const serviceId = Number.parseInt(data.service, 10);
    const roomId = data.room ? Number.parseInt(data.room, 10) : undefined;

    if (!data.date || !therapistId || !clientId || !selectedTimeSlot) {
      setSubmissionError("Please complete all required session fields.");
      return;
    }

    const sessionModeKey = data.sessionMode;

    const payload: CreateSessionPayload = {
      clientId,
      therapistId,
      sessionDate: toSessionIsoDateTime(
        data.date,
        data.selectedTimeSlot,
        sessionTimezone,
      ),
      sessionMode: sessionModeKey,
      sessionType: data.sessionType || undefined,
      status: sessionDetails?.status || "scheduled",
      duration:
        selectedService?.durationInMinutes ??
        selectedTherapistProfile?.sessionDuration ??
        60,
      serviceId: Number.isFinite(serviceId) ? serviceId : undefined,
      roomId: isInPersonSessionMode(data.sessionMode) ? roomId : undefined,
      notes: data.notes?.trim() || undefined,
      zoomEnabled: !isInPersonSessionMode(data.sessionMode),
      timezone: sessionTimezone,
    };

    if (!isEditSchedule && schedulePlanType === "recurring") {
      if (wizardStep !== 2) {
        return;
      }

      const recurringBody = buildRecurrenceRuleRequest({
        clientId,
        therapistId,
        serviceId,
        roomId: isInPersonSessionMode(data.sessionMode) ? roomId : undefined,
        sessionMode: data.sessionMode,
        isInPersonMode: isInPersonSessionMode,
        sessionType: data.sessionType,
        notes: data.notes,
        sessionDate: data.date,
        sessionTime12h: data.selectedTimeSlot,
        timezone: sessionTimezone,
        recurrence: { ...recurrenceForm, isRecurring: true },
      });

      if (!recurringBody) {
        setSubmissionError("Please complete all recurring session fields.");
        return;
      }

      if (previewData && previewData.freeCount === 0) {
        setSubmissionError("All recurring dates conflict. Adjust the schedule and try again.");
        return;
      }

      if (previewData && previewData.conflictCount > 0) {
        const proceed = window.confirm(
          `${previewData.conflictCount} date${previewData.conflictCount === 1 ? "" : "s"} conflict and will be skipped. Create ${previewData.freeCount} session${previewData.freeCount === 1 ? "" : "s"} anyway?`,
        );
        if (!proceed) return;
      }

      void createRecurringSessions(recurringBody)
        .unwrap()
        .then((response) => {
          onSchedule({
            ...buildSuccessData(data),
            recurringSummary: {
              createdCount: response.createdCount,
              skippedCount: response.skippedCount,
              groupId: response.groupId,
            },
          });
          if (response.warning) {
            onToast?.(response.warning, "info");
          }
          onClose();
        })
        .catch((error) => {
          const message = getApiErrorMessage(error);
          setSubmissionError(null);
          onToast?.(message, "error");
        });
      return;
    }

    if (isEditSchedule && sessionId && editScope === "future" && isSeriesSession && recurrenceGroupId) {
      void updateRecurringFutureSessions({
        groupId: recurrenceGroupId,
        body: {
          anchorId: sessionId,
          sessionDate: toSessionIsoDateTime(
        data.date,
        data.selectedTimeSlot,
        sessionTimezone,
      ),
          roomId: isInPersonSessionMode(data.sessionMode) ? roomId : undefined,
          notes: data.notes?.trim() || undefined,
          serviceId: Number.isFinite(serviceId) ? serviceId : undefined,
          therapistId,
          sessionType: data.sessionType || undefined,
          sessionMode: sessionModeKey,
          zoomEnabled: !isInPersonSessionMode(data.sessionMode),
        },
      })
        .unwrap()
        .then(() => {
          onSchedule(buildSuccessData(data));
          onClose();
        })
        .catch((error) => {
          const message = getApiErrorMessage(error);
          setSubmissionError(null);
          onToast?.(message, "error");
        });
      return;
    }

    const mutationPromise =
      isEditSchedule && sessionId
        ? updateSession({
            id: sessionId,
            body: payload,
          }).unwrap()
        : createSession(payload).unwrap();

    void mutationPromise
      .then((response) => {
        onSchedule({
          ...buildSuccessData(data),
          zoomJoinUrl: response?.zoomJoinUrl || undefined,
          zoomPassword: response?.zoomPassword || undefined,
        });
        onClose();
      })
      .catch((error) => {
        const message = getApiErrorMessage(error);
        setSubmissionError(null);
        onToast?.(message, "error");
      });
  };

  const isSubmitting =
    isCreatingSession ||
    isUpdatingSession ||
    isCreatingRecurring ||
    isUpdatingRecurringFuture;

  return (
    <div className="fixed inset-0 z-9999 flex items-center justify-center bg-black/50 px-2 md:px-0">
      <div className="flex max-h-[90vh] w-150 flex-col overflow-hidden rounded-2xl border border-(--neutral-100) bg-white shadow-xl">
        <div className="flex items-center justify-between p-2 py-3 md:p-6 md:pb-4">
          <h2 className="text-xl font-semibold text-(--text-primary-dark)">
            {isEditSchedule ? "Edit Session" : "Schedule New Session"}
          </h2>
          <button
            onClick={onClose}
            className="rounded-full p-1 text-(--text-neutral-600) transition-colors duration-300 hover:bg-(--neutral-100) cursor-pointer"
          >
            <X size={24} />
          </button>
        </div>

        {!isEditSchedule ? (
          <div className="space-y-3 px-2 md:px-6">
            <div className="flex gap-2 rounded-full bg-(--neutral-100) p-1">
              <button
                type="button"
                onClick={() => handleSchedulePlanTypeChange("one-time")}
                className={cn(
                  "flex-1 rounded-full px-4 py-2 text-sm font-medium transition-all cursor-pointer text-center",
                  schedulePlanType === "one-time"
                    ? "bg-white text-(--text-primary-dark) shadow-sm"
                    : "text-(--text-neutral-400) hover:text-(--text-neutral-600)",
                )}
              >
                One-time session
              </button>
              <button
                type="button"
                onClick={() => handleSchedulePlanTypeChange("recurring")}
                className={cn(
                  "flex-1 rounded-full px-4 py-2 text-sm font-medium transition-all cursor-pointer text-center",
                  schedulePlanType === "recurring"
                    ? "bg-white text-(--text-primary-dark) shadow-sm"
                    : "text-(--text-neutral-400) hover:text-(--text-neutral-600)",
                )}
              >
                Recurring series
              </button>
            </div>

            {isRecurringCreateFlow ? (
              <div className="flex items-center gap-3 pb-1">
                <div className="flex items-center gap-2">
                  <span
                    className={cn(
                      "flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold",
                      wizardStep === 1
                        ? "bg-(--bg-primary-dark) text-white"
                        : "bg-(--neutral-100) text-(--text-neutral-600)",
                    )}
                  >
                    1
                  </span>
                  <span
                    className={cn(
                      "text-sm font-medium",
                      wizardStep === 1
                        ? "text-(--text-primary-dark)"
                        : "text-(--text-neutral-500)",
                    )}
                  >
                    Session
                  </span>
                </div>
                <div className="h-px flex-1 bg-(--neutral-200)" />
                <div className="flex items-center gap-2">
                  <span
                    className={cn(
                      "flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold",
                      wizardStep === 2
                        ? "bg-(--bg-primary-dark) text-white"
                        : "bg-(--neutral-100) text-(--text-neutral-600)",
                    )}
                  >
                    2
                  </span>
                  <span
                    className={cn(
                      "text-sm font-medium",
                      wizardStep === 2
                        ? "text-(--text-primary-dark)"
                        : "text-(--text-neutral-500)",
                    )}
                  >
                    Repeat
                  </span>
                </div>
              </div>
            ) : null}
          </div>
        ) : null}

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            onKeyDown={(event) => {
              if (
                event.key === "Enter" &&
                isRecurringCreateFlow &&
                wizardStep === 1
              ) {
                event.preventDefault();
              }
            }}
            className="flex min-h-0 flex-1 flex-col overflow-hidden"
          >
            <div className="custom-scrollbar min-h-0 flex-1 overflow-y-auto px-2 md:px-6">
              <div className="space-y-4 py-2">
                {submissionError ? (
                  <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm font-medium text-[#b42318]">
                    {submissionError}
                  </div>
                ) : null}
                {isSessionFormBootstrapping ? (
                  <div className="rounded-[0.75rem] border border-(--neutral-100) bg-(--bg-primary-50) px-4 py-3 text-sm font-medium text-(--text-neutral-600)">
                    Loading session details...
                  </div>
                ) : null}

                {showSessionStep && !isSessionFormBootstrapping ? (
                  <>
                <div className="grid grid-cols-2 gap-3">
                  <CustomSelect
                    control={form.control}
                    name="sessionType"
                    label="Session Type"
                    options={sessionTypeOptions}
                    placeholder="Select type"
                    required
                  />

                  <CustomSelect
                    control={form.control}
                    name="client"
                    label="Client"
                    options={clientSelectOptions}
                    placeholder="Search client"
                    isSearch={true}
                    required
                    disabled={isEditSchedule || lockClientSelection || (isEditSchedule && editScope === "future" && isSeriesSession)}
                    onMenuScrollToEnd={handleClientsMenuScrollToEnd}
                    hasMore={hasMoreClients}
                    isLoadingMore={isLoadingClients}
                    loadingMoreLabel={
                      clientSelectOptions.length === 0
                        ? "Loading clients..."
                        : "Loading more clients..."
                    }
                  />
                </div>

                <CustomSelect
                  control={form.control}
                  name="service"
                  label="Service"
                  options={serviceOptions}
                  placeholder="Select service"
                  contentClassName="max-h-52"
                  renderOption={(s) => (
                    <div className="flex min-w-0 w-full items-center justify-between gap-2">
                      <span
                        className="min-w-0 flex-1 truncate"
                        title={s.label}
                      >
                        {s.label}
                      </span>
                      <span className="shrink-0 whitespace-nowrap text-sm text-(--text-neutral-600)">
                        {s.duration} • {s.price}
                      </span>
                    </div>
                  )}
                  required
                />

                {needsTherapistPicker && (
                  <CustomSelect
                    control={form.control}
                    name="therapist"
                    label="Therapist"
                    options={therapistOptions}
                    placeholder="Select therapist"
                    isSearch={true}
                    required
                    disabled={isEditSchedule || therapistSelectLocked}
                  />
                )}

                <SessionModeSelector
                  sessionMode={sessionMode}
                  options={sessionModeOptions}
                  isInPersonMode={isInPersonSessionMode}
                  onlineAvailable={onlineSessionAvailable}
                  onlineUnavailableReason={onlineUnavailableReason}
                  onOpenZoomSettings={
                    isSchedulingForSelf
                      ? () => {
                          onClose();
                          openTherapistProfileSection("zoom");
                        }
                      : undefined
                  }
                  onChange={(v) => {
                    form.setValue("sessionMode", v);
                    if (!isInPersonSessionMode(v)) {
                      form.setValue("room", "", {
                        shouldDirty: false,
                        shouldValidate: false,
                      });
                      form.clearErrors("room");
                    }
                  }}
                />

                <CustomDatePicker
                  label="Date"
                  date={dateValue}
                  onDateChange={(d) => form.setValue("date", d)}
                  required
                  disablePast={true}
                />
                {!dateValue && form.formState.errors.date && (
                  <p className="text-red-500 text-sm">
                    {form.formState.errors.date.message}
                  </p>
                )}

                <TimeSlots
                  date={dateValue}
                  timeSlots={availableTimeSlots}
                  selected={selectedTimeSlot}
                  onSelect={(v) => form.setValue("selectedTimeSlot", v)}
                  timezoneLabel={sessionTimezoneLabel}
                  isLoading={
                    canFetchAvailabilitySlots &&
                    isFetchingAvailabilitySlots &&
                    availableTimeSlots.length === 0
                  }
                  emptyMessage={timeSlotsEmptyMessage}
                />
                {dateValue &&
                  !selectedTimeSlot &&
                  form.formState.errors.selectedTimeSlot && (
                    <p className="text-red-500 text-sm">
                      {form.formState.errors.selectedTimeSlot.message}
                    </p>
                  )}

                {dateValue && selectedTimeSlot && isInPersonSessionMode(sessionMode) && (
                  <CustomSelect
                    control={form.control}
                    name="room"
                    label="Room"
                    options={roomOptions}
                    placeholder="Select room"
                    disabled={isFetchingAvailableRooms}
                    required
                  />
                )}
                  </>
                ) : null}

                {showRecurringStep && sessionSummary ? (
                  <div className="rounded-2xl border border-(--neutral-100) bg-(--bg-primary-50) px-4 py-3">
                    <p className="text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      Session summary
                    </p>
                    <p className="mt-2 text-sm font-medium text-(--text-primary-dark)">
                      {sessionSummary.clientLabel} · {sessionSummary.serviceLabel}
                    </p>
                    <p className="mt-1 text-sm text-(--text-neutral-600)">
                      {sessionSummary.dateLabel} · {sessionSummary.timeLabel} ·{" "}
                      {sessionSummary.modeLabel}
                      {sessionSummary.roomLabel ? ` · ${sessionSummary.roomLabel}` : ""}
                    </p>
                  </div>
                ) : null}

                {isEditSchedule && isSeriesSession ? (
                  <div className="rounded-2xl border border-(--neutral-100) bg-(--neutral-50) p-4 space-y-3">
                    <p className="text-sm font-semibold text-(--text-primary-dark)">
                      Edit recurring session
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      <button
                        type="button"
                        onClick={() => setEditScope("single")}
                        className={cn(
                          "rounded-xl border px-3 py-3 text-left transition-colors",
                          editScope === "single"
                            ? "border-(--bg-primary-dark) bg-white"
                            : "border-(--neutral-200) bg-white/70",
                        )}
                      >
                        <p className="text-sm font-medium text-(--text-primary-dark)">
                          This session only
                        </p>
                        <p className="text-xs text-(--text-neutral-600) mt-1">
                          Update only this occurrence.
                        </p>
                      </button>
                      <button
                        type="button"
                        onClick={() => setEditScope("future")}
                        className={cn(
                          "rounded-xl border px-3 py-3 text-left transition-colors",
                          editScope === "future"
                            ? "border-(--bg-primary-dark) bg-white"
                            : "border-(--neutral-200) bg-white/70",
                        )}
                      >
                        <p className="text-sm font-medium text-(--text-primary-dark)">
                          This and all future
                        </p>
                        <p className="text-xs text-(--text-neutral-600) mt-1">
                          Shift upcoming sessions in this series.
                        </p>
                      </button>
                    </div>
                  </div>
                ) : null}

                {showRecurringStep ? (
                  <>
                    <RecurrenceFields
                      hideToggle
                      value={{ ...recurrenceForm, isRecurring: true }}
                      onChange={(next) =>
                        setRecurrenceForm({ ...next, isRecurring: true })
                      }
                      timezoneLabel={sessionTimezoneLabel}
                    />

                    <RecurrencePreviewTable
                      preview={previewData}
                      isLoading={isPreviewLoading}
                      error={previewError}
                    />

                    <CustomTextarea
                      control={form.control}
                      name="notes"
                      label="Session notes or special instructions"
                    />
                  </>
                ) : null}

                {!isEditSchedule && schedulePlanType === "one-time" ? (
                  <CustomTextarea
                    control={form.control}
                    name="notes"
                    label="Session notes or special instructions"
                  />
                ) : null}

                {isEditSchedule ? (
                  <CustomTextarea
                    control={form.control}
                    name="notes"
                    label="Session notes or special instructions"
                  />
                ) : null}
              </div>
            </div>

            <div className="flex gap-3 justify-end border-t border-(--neutral-100) bg-white p-6">
              {showRecurringStep ? (
                <Button
                  type="button"
                  onClick={() => {
                    setWizardStep(1);
                    setSubmissionError(null);
                  }}
                  className="mr-auto h-auto rounded-full border border-(--neutral-200) bg-transparent px-6 py-2.5 font-semibold text-(--text-primary-dark) hover:bg-(--neutral-50) cursor-pointer"
                >
                  Back
                </Button>
              ) : null}
              <Button
                type="button"
                onClick={onClose}
                className="h-auto rounded-full border border-(--neutral-200) bg-transparent px-6 py-2.5 font-semibold text-(--text-primary-dark) hover:bg-(--neutral-50) cursor-pointer"
              >
                Cancel
              </Button>
              {isRecurringCreateFlow && wizardStep === 1 ? (
                <Button
                  type="button"
                  onClick={handleContinueToRecurringStep}
                  disabled={!isSessionDetailsComplete || isFetchingSessionDetails}
                  className="h-auto rounded-full bg-(--bg-primary-dark) px-6 py-2.5 font-semibold text-white hover:bg-(--bg-primary-dark)/90 cursor-pointer disabled:bg-(--text-neutral-100) disabled:text-(--text-neutral-400) disabled:pointer-events-none"
                >
                  Continue
                </Button>
              ) : (
              <Button
                type="submit"
                className="h-auto rounded-full bg-(--bg-primary-dark) px-6 py-2.5 font-semibold text-white hover:bg-(--bg-primary-dark)/90 cursor-pointer disabled:bg-(--text-neutral-100) disabled:text-(--text-neutral-400) disabled:pointer-events-none"
                disabled={
                  !isRequiredFieldsFilled ||
                  isSubmitting ||
                  isFetchingSessionDetails
                }
                loading={isSubmitting}
                loadingLabel={isEditSchedule ? "Updating..." : "Scheduling..."}
              >
                {isEditSchedule
                  ? editScope === "future" && isSeriesSession
                    ? "Update Series"
                    : "Update Session"
                  : schedulePlanType === "recurring"
                    ? "Create Recurring Series"
                    : "Schedule Session"}
              </Button>
              )}
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default AddSessionModal;
