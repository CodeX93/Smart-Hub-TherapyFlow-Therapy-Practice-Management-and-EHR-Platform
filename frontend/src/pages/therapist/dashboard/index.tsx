
import { ContentLoader } from "@/components/shared/ContentLoader";
import OverviewCard from "@/components/shared/OverviewCard";
import type { DashboardOverviewItem } from "@/types/therapist-dashboard.type";
import Sessions from "@/components/dashboard-sections/Sessions";
import UpcomingDeadlines from "@/components/dashboard-sections/UpcomingDeadlines";
import RecentTasks from "@/components/dashboard-sections/RecentTasks";
import AddSessionModal from "@/components/scheduling-sections/add-session-modal";
import EditTaskModal from "@/components/therapist/tasks/EditTaskModal";
import TaskDetailsModal from "@/components/therapist/tasks/TaskDetailsModal";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import { getIcon } from "@/utils/functions/dashboard";
import { AlertTriangle } from "lucide-react";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import type {
  DashboardSessionItem,
  DashboardSessions,
  DeadlineItem,
  RecentTaskItem,
} from "@/types/therapist-dashboard.type";
import {
  type TaskCommentItem,
  useCreateTaskCommentMutation,
  useDeleteTaskCommentMutation,
  useDeleteTaskMutation,
  useLazyGetTaskByIdQuery,
  useLazyGetTaskCommentsQuery,
  useReplaceTaskMutation,
  useUpdateTaskCommentMutation,
} from "@/store/api/admin/tasks.api";
import {
  useGetAdminSessionsOverdueQuery,
  useGetAdminSessionsPreviousQuery,
  useGetAdminSessionsUpcomingQuery,
  useGetAdminTasksRecentQuery,
  useGetAdminTasksUpcomingQuery,
  useGetTherapistDashboardSummaryQuery,
  useUpdateSessionStatusMutation,
} from "@/store/api/admin/dashboard.api";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import {
  isAllowedBackendSessionStatusTransition,
  type BackendSessionStatus,
} from "@/utils/sessionStatusTransitions";
import type { Comment, Task } from "@/pages/therapist/tasks/tasks.static";
import { mapApiTaskToUiTask } from "@/utils/tasks/mapApiTaskToUiTask";
import { useStaffNotificationUnreadBootstrap } from "@/hooks/useNotificationUnreadBootstrap";
import { getSessionStatusLabel } from "@/utils/sessionStatusPresentation";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import {
  formatDateInScheduleTimezone,
  formatDateTimeInScheduleTimezone,
  formatTimeInScheduleTimezone,
} from "@/utils/scheduleTimezone";
import {
  DEFAULT_SESSION_PERIOD,
  getSessionPeriodRange,
  type SessionPeriod,
} from "@/utils/sessionPeriod";

function getOverdueDaysLabel(value?: string): string | undefined {
  if (!value) return undefined;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return undefined;
  const dayMs = 1000 * 60 * 60 * 24;
  const diffDays = Math.max(0, Math.floor((Date.now() - parsed.getTime()) / dayMs));
  if (diffDays <= 1) return "1 day overdue";
  return `${diffDays} days overdue`;
}

function mapApiCommentToUiComment(
  comment: TaskCommentItem,
  timezone?: string | null,
): Comment {
  const initials = comment.authorName
    ? comment.authorName
        .split(" ")
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase() || "")
        .join("")
    : "NA";

  return {
    id: String(comment.id),
    userId: String(comment.authorId),
    userName: comment.authorName || "Unknown",
    userInitials: initials || "NA",
    text: comment.content,
    timestamp: formatDateTimeInScheduleTimezone(comment.createdAt, timezone),
    isInternal: comment.isInternal,
  };
}

function formatCurrency(amount: number, currencyCode: string): string {
  const normalized = Number.isFinite(amount) ? amount : 0;
  const currency = currencyCode || "USD";
  if (currency.toUpperCase() === "USD") {
    return `$${normalized.toLocaleString(undefined, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    })}`;
  }
  return `${normalized.toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })} ${currency}`;
}

const Dashboard = () => {
  useStaffNotificationUnreadBootstrap();
  const navigate = useNavigate();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [updatingSessionId, setUpdatingSessionId] = useState<string | null>(null);
  const [isEditSessionModalOpen, setIsEditSessionModalOpen] = useState(false);
  const [editingSessionId, setEditingSessionId] = useState<number | null>(null);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isTaskDetailsModalOpen, setIsTaskDetailsModalOpen] = useState(false);
  const [isTaskEditModalOpen, setIsTaskEditModalOpen] = useState(false);
  const [isTaskDeleteModalOpen, setIsTaskDeleteModalOpen] = useState(false);
  const [isLoadingTaskDetails, setIsLoadingTaskDetails] = useState(false);
  const [sessionPeriod, setSessionPeriod] = useState<SessionPeriod>(DEFAULT_SESSION_PERIOD);
  const sessionPeriodRange = useMemo(
    () => getSessionPeriodRange(sessionPeriod),
    [sessionPeriod],
  );
  const [updateSessionStatus] = useUpdateSessionStatusMutation();
  const [triggerGetTaskById] = useLazyGetTaskByIdQuery();
  const [triggerGetTaskComments, { isFetching: isFetchingTaskComments }] =
    useLazyGetTaskCommentsQuery();
  const [replaceTask, { isLoading: isUpdatingTask }] = useReplaceTaskMutation();
  const [deleteTask, { isLoading: isDeletingTask }] = useDeleteTaskMutation();
  const [createComment, { isLoading: isCreatingComment }] = useCreateTaskCommentMutation();
  const [updateComment, { isLoading: isUpdatingComment }] = useUpdateTaskCommentMutation();
  const [deleteComment, { isLoading: isDeletingComment }] = useDeleteTaskCommentMutation();

  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const scheduleTimezone = practiceConfig?.timezone;

  const {
    data: dashboardSummary,
    isLoading: isLoadingDashboardSummary,
    isFetching: isFetchingDashboardSummary,
    isError: isDashboardSummaryError,
    error: dashboardSummaryError,
  } = useGetTherapistDashboardSummaryQuery();
  const {
    data: upcomingSessionsSlice,
    isLoading: isLoadingUpcomingSessions,
    isFetching: isFetchingUpcomingSessions,
    isError: isUpcomingSessionsError,
    error: upcomingSessionsError,
    refetch: refetchUpcomingSessions,
  } = useGetAdminSessionsUpcomingQuery({ limit: 5, ...sessionPeriodRange });
  const {
    data: previousSessionsSlice,
    isLoading: isLoadingPreviousSessions,
    isFetching: isFetchingPreviousSessions,
    isError: isPreviousSessionsError,
    error: previousSessionsError,
    refetch: refetchPreviousSessions,
  } = useGetAdminSessionsPreviousQuery({ limit: 5, ...sessionPeriodRange });
  const {
    data: overdueSessionsSlice,
    isLoading: isLoadingOverdueSessions,
    isFetching: isFetchingOverdueSessions,
    isError: isOverdueSessionsError,
    error: overdueSessionsError,
    refetch: refetchOverdueSessions,
  } = useGetAdminSessionsOverdueQuery({ limit: 5, ...sessionPeriodRange });
  const upcomingSessions = upcomingSessionsSlice?.items ?? [];
  const previousSessions = previousSessionsSlice?.items ?? [];
  const overdueSessions = overdueSessionsSlice?.items ?? [];
  const upcomingSessionsTotal = upcomingSessionsSlice?.totalCount ?? 0;
  const previousSessionsTotal = previousSessionsSlice?.totalCount ?? 0;
  const overdueSessionsTotal = overdueSessionsSlice?.totalCount ?? 0;
  const {
    data: recentTasks = [],
    isLoading: isLoadingRecentTasks,
    isFetching: isFetchingRecentTasks,
    isError: isRecentTasksError,
    error: recentTasksError,
    refetch: refetchRecentTasks,
  } = useGetAdminTasksRecentQuery({ limit: 10 });
  const {
    data: upcomingTasks = [],
    isLoading: isLoadingUpcomingTasks,
    isFetching: isFetchingUpcomingTasks,
    isError: isUpcomingTasksError,
    error: upcomingTasksError,
    refetch: refetchUpcomingTasks,
  } = useGetAdminTasksUpcomingQuery({ limit: 10 });

  const hasAnyData =
    upcomingSessions.length > 0 ||
    previousSessions.length > 0 ||
    overdueSessions.length > 0 ||
    recentTasks.length > 0 ||
    upcomingTasks.length > 0;

  const isLoadingDashboard =
    isLoadingDashboardSummary ||
    isLoadingUpcomingSessions ||
    isLoadingPreviousSessions ||
    isLoadingOverdueSessions ||
    isLoadingRecentTasks ||
    isLoadingUpcomingTasks ||
    isFetchingDashboardSummary ||
    isFetchingUpcomingSessions ||
    isFetchingPreviousSessions ||
    isFetchingOverdueSessions ||
    isFetchingRecentTasks ||
    isFetchingUpcomingTasks;

  const dashboardError = useMemo(() => {
    const firstError =
      (isDashboardSummaryError && dashboardSummaryError) ||
      (isUpcomingSessionsError && upcomingSessionsError) ||
      (isPreviousSessionsError && previousSessionsError) ||
      (isOverdueSessionsError && overdueSessionsError) ||
      (isRecentTasksError && recentTasksError) ||
      (isUpcomingTasksError && upcomingTasksError);

    return firstError ? getApiErrorMessage(firstError) : null;
  }, [
    isDashboardSummaryError,
    dashboardSummaryError,
    isUpcomingSessionsError,
    upcomingSessionsError,
    isPreviousSessionsError,
    previousSessionsError,
    isOverdueSessionsError,
    overdueSessionsError,
    isRecentTasksError,
    recentTasksError,
    isUpcomingTasksError,
    upcomingTasksError,
  ]);

  const overviewItems: DashboardOverviewItem[] = useMemo(() => {
    const activeClients = dashboardSummary?.client.active ?? 0;
    const totalClients = dashboardSummary?.client.total ?? 0;
    const scheduledToday = dashboardSummary?.session.scheduledToday ?? 0;
    const pendingTasks = dashboardSummary?.task.pending ?? 0;
    const urgentTasks = dashboardSummary?.task.urgent ?? 0;
    const totalTasks = dashboardSummary?.task.total ?? 0;
    const outstandingBalance = dashboardSummary?.billing.outstandingBalance ?? 0;
    const totalCollected = dashboardSummary?.billing.totalCollected ?? 0;

    return [
      {
        label: "Active Clients",
        value: activeClients.toLocaleString(),
        subtext: `of ${totalClients.toLocaleString()} total`,
        icon: "users",
      },
      {
        label: "Today's Sessions",
        value: scheduledToday.toLocaleString(),
        subtext: "scheduled for today",
        icon: "calendar",
      },
      {
        label: "Pending Tasks",
        value: pendingTasks.toLocaleString(),
        subtext: `of ${totalTasks.toLocaleString()} total tasks`,
        icon: "tasks",
        badge:
          urgentTasks > 0
            ? { text: `${urgentTasks} urgent`, tone: "urgent" }
            : undefined,
      },
      {
        label: "Billing Overview",
        value: formatCurrency(outstandingBalance, "USD"),
        subtext: `Collected ${formatCurrency(totalCollected, "USD")} this month`,
        icon: "billing",
      },
    ];
  }, [dashboardSummary]);

  const sessionsData: DashboardSessions = useMemo(() => {
    const previous: DashboardSessionItem[] = previousSessions.map((session) => ({
      id: `previous-${session.id}`,
      patientName: session.clientName || "Unknown Client",
      date: formatDateInScheduleTimezone(session.sessionDate, scheduleTimezone),
      time: formatTimeInScheduleTimezone(session.sessionDate, scheduleTimezone),
      sessionId: session.serviceName || `SESSION-${session.id}`,
      status: getSessionStatusLabel(session.status || "pending"),
      scheduledAt: session.sessionDate,
      hasInvoice: Boolean(session.billingId || session.hasInvoice),
    }));

    const upcoming: DashboardSessionItem[] = upcomingSessions.map((session) => ({
      id: `upcoming-${session.id}`,
      patientName: session.clientName || "Unknown Client",
      date: formatDateInScheduleTimezone(session.sessionDate, scheduleTimezone),
      time: formatTimeInScheduleTimezone(session.sessionDate, scheduleTimezone),
      sessionId: session.serviceName || `SESSION-${session.id}`,
      status: getSessionStatusLabel(session.status || "pending"),
      scheduledAt: session.sessionDate,
      hasInvoice: Boolean(session.billingId || session.hasInvoice),
    }));

    const overdue: DashboardSessionItem[] = overdueSessions.map((session) => ({
      id: `overdue-${session.id}`,
      patientName: session.clientName || "Unknown Client",
      date: formatDateInScheduleTimezone(session.sessionDate, scheduleTimezone),
      time: formatTimeInScheduleTimezone(session.sessionDate, scheduleTimezone),
      sessionId: session.serviceName || `SESSION-${session.id}`,
      status: getSessionStatusLabel(session.status || "overdue"),
      scheduledAt: session.sessionDate,
      hasInvoice: Boolean(session.billingId || session.hasInvoice),
      overdueDays: getOverdueDaysLabel(session.sessionDate),
    }));

    return { previous, upcoming, overdue, previousTotal: previousSessionsTotal, upcomingTotal: upcomingSessionsTotal, overdueTotal: overdueSessionsTotal };
  }, [
    previousSessions,
    upcomingSessions,
    overdueSessions,
    previousSessionsTotal,
    upcomingSessionsTotal,
    overdueSessionsTotal,
    scheduleTimezone,
  ]);

  const deadlineItems: DeadlineItem[] = useMemo(
    () =>
      upcomingTasks.map((task) => {
        const mappedTask = mapApiTaskToUiTask(task);
        return {
        id: String(task.id),
        title: task.title || "Untitled task",
        date: formatDateInScheduleTimezone(task.dueDate, scheduleTimezone),
        clientName: task.clientName || "Unknown Client",
        priority: mappedTask.priority,
        status: mappedTask.status,
      };
      }),
    [upcomingTasks, scheduleTimezone],
  );

  const recentTaskItems: RecentTaskItem[] = useMemo(
    () =>
      recentTasks.map((task) => {
        const mappedTask = mapApiTaskToUiTask(task);
        return {
        id: String(task.id),
        title: task.title || "Untitled task",
        priority: mappedTask.priority,
        clientName: task.clientName || "Unknown Client",
        time:
          formatTimeInScheduleTimezone(
            task.updatedAt || task.createdAt || task.dueDate,
            scheduleTimezone,
          ) || "-",
        status: mappedTask.status,
      };
      }),
    [recentTasks, scheduleTimezone],
  );

  const handleOverdueSessionAction = async (action: string, dashboardSessionId: string) => {
    const rawSessionId = Number(dashboardSessionId.split("-").pop());
    if (!rawSessionId) return;

    if (action === "edit") {
      setEditingSessionId(rawSessionId);
      setIsEditSessionModalOpen(true);
      return;
    }

    const actionToStatus: Record<
      string,
      | "SCHEDULED"
      | "CONFIRMED"
      | "IN_PROGRESS"
      | "COMPLETED"
      | "CANCELLED"
      | "RESCHEDULING"
      | "NO_SHOW"
      | "OVERDUE"
    > = {
      scheduled: "SCHEDULED",
      confirmed: "CONFIRMED",
      overdue: "OVERDUE",
      in_progress: "IN_PROGRESS",
      completed: "COMPLETED",
      cancelled: "CANCELLED",
      rescheduling: "RESCHEDULING",
      no_show: "NO_SHOW",
      complete: "COMPLETED",
      cancel: "CANCELLED",
      reschedule: "RESCHEDULING",
    };

    const mappedStatus = actionToStatus[action.trim().toLowerCase()];
    if (!mappedStatus) return;
    const currentSession = overdueSessions.find((session) => session.id === rawSessionId);
    if (
      currentSession &&
      !isAllowedBackendSessionStatusTransition(
        currentSession.status || "overdue",
        mappedStatus as BackendSessionStatus,
        {
          scheduledAt: currentSession.sessionDate,
          hasInvoice: Boolean(currentSession.billingId || currentSession.hasInvoice),
          practiceTimezone: scheduleTimezone,
        },
      )
    ) {
      return;
    }

    setUpdatingSessionId(dashboardSessionId);
    try {
      await updateSessionStatus({
        id: rawSessionId,
        body: { status: mappedStatus },
      }).unwrap();

      await Promise.all([
        refetchUpcomingSessions(),
        refetchPreviousSessions(),
        refetchOverdueSessions(),
      ]);

      setToastType("success");
      setToastMessage("Session status updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setUpdatingSessionId(null);
    }
  };

  const handleSessionEdited = async () => {
    try {
      await Promise.all([
        refetchUpcomingSessions(),
        refetchPreviousSessions(),
        refetchOverdueSessions(),
      ]);
      setToastType("success");
      setToastMessage("Session updated successfully.");
      setEditingSessionId(null);
      setIsEditSessionModalOpen(false);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const refetchDashboardTasks = async () => {
    await Promise.all([refetchRecentTasks(), refetchUpcomingTasks()]);
  };

  const openTaskDetails = async (taskId: string) => {
    setSelectedTask(null);
    setIsTaskDetailsModalOpen(true);
    setIsLoadingTaskDetails(true);
    try {
      const [taskPayload, commentsPayload] = await Promise.all([
        triggerGetTaskById(Number(taskId)).unwrap(),
        triggerGetTaskComments(Number(taskId)).unwrap(),
      ]);
      const mapped = mapApiTaskToUiTask(taskPayload);
      mapped.comments = commentsPayload.map((entry) =>
        mapApiCommentToUiComment(entry, scheduleTimezone),
      );
      mapped.commentsCount = mapped.comments.length;
      setSelectedTask(mapped);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setIsTaskDetailsModalOpen(false);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const openTaskEdit = async (taskId: string) => {
    setSelectedTask(null);
    setIsTaskEditModalOpen(true);
    setIsLoadingTaskDetails(true);
    try {
      const taskPayload = await triggerGetTaskById(Number(taskId)).unwrap();
      setSelectedTask(mapApiTaskToUiTask(taskPayload));
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setIsTaskEditModalOpen(false);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const handleTaskAction = async (action: "view" | "edit" | "delete", taskId: string) => {
    if (action === "view") {
      await openTaskDetails(taskId);
      return;
    }
    if (action === "edit") {
      await openTaskEdit(taskId);
      return;
    }
    try {
      const taskPayload = await triggerGetTaskById(Number(taskId)).unwrap();
      setSelectedTask(mapApiTaskToUiTask(taskPayload));
      setIsTaskDeleteModalOpen(true);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleTaskUpdate = async (updatedTask: {
    title: string;
    titleKey?: string | null;
    taskType?: string | null;
    clientId: number;
    priority: string;
    status: string;
    description?: string;
    dueDate?: string;
  }) => {
    if (!selectedTask) return;
    try {
      await replaceTask({
        id: Number(selectedTask.id),
        body: {
          title: updatedTask.title,
        titleKey: updatedTask.titleKey,
        taskType: updatedTask.taskType,
          status: updatedTask.status,
          priority: updatedTask.priority,
          description: updatedTask.description?.trim() || undefined,
          clientId: updatedTask.clientId,
          dueDate: updatedTask.dueDate,
        },
      }).unwrap();
      await refetchDashboardTasks();
      setToastType("success");
      setToastMessage("Task updated successfully");
      setIsTaskEditModalOpen(false);
      setSelectedTask(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleTaskDelete = async () => {
    if (!selectedTask) return;
    try {
      await deleteTask(Number(selectedTask.id)).unwrap();
      await refetchDashboardTasks();
      setToastType("success");
      setToastMessage("Task deleted successfully");
      setIsTaskDeleteModalOpen(false);
      setSelectedTask(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleAddComment = async (taskId: string, content: string, isInternal: boolean) => {
    try {
      const created = await createComment({ taskId: Number(taskId), content, isInternal }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = [
          ...(prev.comments || []),
          mapApiCommentToUiComment(created, scheduleTimezone),
        ];
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleEditComment = async (
    taskId: string,
    commentId: string,
    content: string,
    isInternal: boolean,
  ) => {
    try {
      const updated = await updateComment({
        taskId: Number(taskId),
        commentId: Number(commentId),
        content,
        isInternal,
      }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).map((comment) =>
          comment.id === commentId
            ? mapApiCommentToUiComment(updated, scheduleTimezone)
            : comment,
        );
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleDeleteComment = async (taskId: string, commentId: string) => {
    try {
      await deleteComment({ taskId: Number(taskId), commentId: Number(commentId) }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).filter((comment) => comment.id !== commentId);
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  if (!hasAnyData && isLoadingDashboard) {
    return <ContentLoader className="min-h-[23.75rem]" />;
  }

  return (
    <div>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      {dashboardError ? (
        <div className="mb-4 flex items-start gap-2 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-[#b42318]">
          <AlertTriangle size={16} className="mt-0.5 shrink-0" />
          <span>{dashboardError}</span>
        </div>
      ) : null}
      <div className="grid grid-cols-2 md:grid-cols-4 md:gap-4 gap-2 mb-4">
        {(isLoadingDashboardSummary ? Array(4).fill(null) : overviewItems).map(
          (item, index) => (
            <OverviewCard
              key={index}
              label={item?.label ?? ""}
              value={isLoadingDashboardSummary ? "" : item?.value}
              icon={getIcon(item?.icon ?? "dollar")}
              subtext={item?.subtext}
              badge={item?.badge}
              labelFirst={true}
            />
          ),
        )}
      </div>
      <div className="flex gap-4 xl:flex-row flex-col justify-between items-start w-full">
        <div className="flex flex-col gap-4 w-full xl:flex-1 min-w-0">
          <Sessions
            sessions={sessionsData}
            period={sessionPeriod}
            onPeriodChange={setSessionPeriod}
            onViewAll={(tab) => {
              const params = new URLSearchParams({ tab: "All" });
              if (tab === "Overdue") {
                params.set("status", "overdue");
                params.set(
                  "endDate",
                  (sessionPeriodRange.endDate ?? new Date().toISOString()).slice(0, 10),
                );
              } else if (tab === "Previous") {
                params.set("status", "completed");
              } else {
                params.set("status", "scheduled");
              }
              if (sessionPeriodRange.startDate) {
                params.set("startDate", sessionPeriodRange.startDate.slice(0, 10));
              }
              if (sessionPeriodRange.endDate && tab !== "Overdue") {
                params.set("endDate", sessionPeriodRange.endDate.slice(0, 10));
              }
              navigate(`/therapist/scheduling?${params.toString()}`);
            }}
            onSessionAction={(action, sessionId) => {
              void handleOverdueSessionAction(action, sessionId);
            }}
            onScheduleSession={() => navigate("/therapist/scheduling")}
            updatingSessionId={updatingSessionId}
          />
          <UpcomingDeadlines
            deadlines={deadlineItems}
            onViewAll={() => navigate("/therapist/tasks")}
            onTaskAction={(action, taskId) => {
              void handleTaskAction(action, taskId);
            }}
          />
        </div>
        <div className="w-full xl:w-[32%] xl:min-w-91 bg-white p-2 rounded-lg border border-(--neutral-100) shadow-(--shadow)">
          <RecentTasks
            recentTasks={recentTaskItems}
            onViewAll={() => navigate("/therapist/tasks/history")}
            onCreateTask={() => navigate("/therapist/tasks")}
            onTaskAction={(action, taskId) => {
              void handleTaskAction(action, taskId);
            }}
          />
        </div>
      </div>

      <AddSessionModal
        isOpen={isEditSessionModalOpen}
        onClose={() => {
          setIsEditSessionModalOpen(false);
          setEditingSessionId(null);
        }}
        initialData={{}}
        onSchedule={handleSessionEdited}
        onToast={(message, type) => {
          setToastType(type);
          setToastMessage(message);
        }}
        isEditSchedule={true}
        sessionId={editingSessionId}
      />

      <EditTaskModal
        isOpen={isTaskEditModalOpen}
        onClose={() => {
          setIsTaskEditModalOpen(false);
          setSelectedTask(null);
        }}
        taskToEdit={selectedTask}
        onUpdate={handleTaskUpdate}
        isSubmitting={isUpdatingTask || isLoadingTaskDetails}
      />

      <TaskDetailsModal
        isOpen={isTaskDetailsModalOpen}
        task={selectedTask}
        isLoadingTask={isLoadingTaskDetails}
        isLoadingComments={isFetchingTaskComments}
        onClose={() => {
          setIsTaskDetailsModalOpen(false);
          setSelectedTask(null);
        }}
        onEdit={(task) => {
          setIsTaskDetailsModalOpen(false);
          void openTaskEdit(task.id);
        }}
        onDelete={(taskId) => {
          setIsTaskDetailsModalOpen(false);
          void handleTaskAction("delete", taskId);
        }}
        onUpdateTask={(updatedTask) => {
          setSelectedTask(updatedTask);
        }}
        onAddComment={handleAddComment}
        onEditComment={handleEditComment}
        onDeleteComment={handleDeleteComment}
        isCommentMutating={isCreatingComment || isUpdatingComment || isDeletingComment}
      />

      <ConfirmationModal
        type="delete"
        isOpen={isTaskDeleteModalOpen}
        onClose={() => {
          if (isDeletingTask) return;
          setIsTaskDeleteModalOpen(false);
          setSelectedTask(null);
        }}
        onConfirm={() => {
          void handleTaskDelete();
        }}
        title={`Delete task "${selectedTask?.title || ""}"?`}
        description="Are you sure you want to delete this task? This action cannot be undone."
        items={[]}
        confirmButtonText="Delete task"
        confirmButtonLoading={isDeletingTask}
        confirmButtonLoadingText="Deleting..."
      />
    </div>
  );
};

export default Dashboard;
