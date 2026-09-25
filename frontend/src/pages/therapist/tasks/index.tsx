
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useCallback, useEffect, useMemo, useRef } from "react";
import { Plus, Search, History } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import TaskSummaryCards from "@/components/shared/TaskSummaryCards";
import TaskCard from "../../../components/therapist/tasks/TaskCard";
import TaskFilters, {
  type TaskFiltersState,
} from "../../../components/therapist/tasks/TaskFilters";
import CreateTaskModal from "../../../components/therapist/tasks/CreateTaskModal";
import EditTaskModal from "../../../components/therapist/tasks/EditTaskModal";
import TaskCommentsModal from "../../../components/therapist/tasks/TaskCommentsModal";
import TaskDetailsModal from "../../../components/therapist/tasks/TaskDetailsModal";
import ConfirmationModal from "../../../components/shared/ConfirmationModal";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { Button } from "../../../components/ui/button";
import type { Task, Comment } from "@/pages/therapist/tasks/tasks.static";
import CustomInput from "@/components/form/CustomInput";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import { cn } from "@/lib/utils";
import {
  useCreateTaskMutation,
  useCreateTaskCommentMutation,
  useDeleteTaskCommentMutation,
  useDeleteTaskMutation,
  useGetTasksQuery,
  useLazyGetTaskByIdQuery,
  useLazyGetTaskCommentsQuery,
  useReplaceTaskMutation,
  useUpdateTaskCommentMutation,
  type TaskCommentItem,
} from "@/store/api/admin/tasks.api";
import { useGetAuthMeQuery } from "@/store/api/authApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { mapApiTaskToUiTask } from "@/utils/tasks/mapApiTaskToUiTask";
import { useTaskSummaryCounts } from "@/hooks/useTaskSummaryCounts";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import EmptyTasksState from "@/components/shared/EmptyTasksState";
import {
  buildTaskFilterChips,
  EMPTY_TASK_FILTERS,
  removeTaskFilterChip,
} from "@/utils/appliedFilterChips";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";
import TaskTimeRangeFilter from "@/components/shared/TaskTimeRangeFilter";
import {
  getTaskTimeRangeDateField,
  getTaskTimeRangeDates,
  type TaskTimeRange,
} from "@/utils/taskTimeRange";

function mapApiCommentToUiComment(comment: TaskCommentItem): Comment {
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
    createdAt: comment.createdAt,
    timestamp: comment.createdAt
      ? new Date(comment.createdAt).toLocaleString("en-US", {
          month: "short",
          day: "numeric",
          hour: "numeric",
          minute: "2-digit",
        })
      : "",
    isInternal: comment.isInternal,
  };
}

const TasksPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [page, setPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [isSearchExpanded, setIsSearchExpanded] = useState(false);
  const [activeTab, setActiveTab] = useState<"all" | "active">("active");
  const [filters, setFilters] = useState<TaskFiltersState>(EMPTY_TASK_FILTERS);
  const [timeRange, setTimeRange] = useState<TaskTimeRange | null>("week");
  const presetDates = useMemo(() => getTaskTimeRangeDates(timeRange), [timeRange]);
  const presetDateField = useMemo(
    () => getTaskTimeRangeDateField(timeRange),
    [timeRange],
  );
  const effectiveFromDate =
    presetDates.fromDate ??
    (filters.startDate
      ? new Date(filters.startDate).toISOString().split("T")[0]
      : undefined);
  const effectiveToDate =
    presetDates.toDate ??
    (filters.endDate
      ? new Date(filters.endDate).toISOString().split("T")[0]
      : undefined);
  const effectiveDateField: "dueDate" | "createdAt" | "updatedAt" | undefined =
    activeTab === "active"
      ? undefined
      : presetDateField ??
        (effectiveFromDate || effectiveToDate ? "dueDate" : undefined);

  const { statusLabel, priorityLabel } = useTaskOptionPresentation();
  const appliedFilterChips = useMemo(
    () => buildTaskFilterChips(filters, { statusLabel, priorityLabel }),
    [filters, statusLabel, priorityLabel],
  );

  const handleRemoveFilterChip = useCallback((chipId: string) => {
    setFilters((currentFilters) => removeTaskFilterChip(currentFilters, chipId));
  }, []);

  const handleClearAllFilters = useCallback(() => {
    setFilters(EMPTY_TASK_FILTERS);
    setTimeRange("week");
  }, []);

  const handleApplyFilters = useCallback((nextFilters: TaskFiltersState) => {
    setFilters(nextFilters);
    if (nextFilters.startDate || nextFilters.endDate) {
      setTimeRange(null);
    }
  }, []);

  const handleTimeRangeChange = useCallback((nextRange: TaskTimeRange) => {
    setTimeRange(nextRange);
    setFilters((currentFilters) => ({
      ...currentFilters,
      startDate: null,
      endDate: null,
    }));
  }, []);

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [taskDetailsModalOpen, setTaskDetailsModalOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isLoadingTaskDetails, setIsLoadingTaskDetails] = useState(false);

  const { data: authMeData } = useGetAuthMeQuery();
  const loggedInTherapistId = authMeData?.user?.id;
  const taskSummaryCounts = useTaskSummaryCounts({
    assignedToId: loggedInTherapistId,
    fromDate: activeTab === "active" ? undefined : effectiveFromDate,
    toDate: activeTab === "active" ? undefined : effectiveToDate,
    dateField: effectiveDateField,
    skip: !loggedInTherapistId,
  });

  // Stable filter key (excludes page) to detect filter changes
  const filterKey = useMemo(
    () =>
      JSON.stringify({
        searchQuery,
        activeTab,
        loggedInTherapistId,
        effectiveFromDate,
        effectiveToDate,
        ...(activeTab === "all" ? { filters } : {}),
      }),
    [
      searchQuery,
      activeTab,
      effectiveFromDate,
      effectiveToDate,
      filters,
      loggedInTherapistId,
    ],
  );
  const accumulatorRef = useRef<{ key: string; tasks: Task[] }>({ key: "", tasks: [] });
  const taskCommentsChangedRef = useRef(false);
  const [isSilentTasksRefetch, setIsSilentTasksRefetch] = useState(false);

  // Reset accumulator and page when filters change
  useEffect(() => {
    setPage(1);
    accumulatorRef.current = { key: filterKey, tasks: [] };
  }, [filterKey, setPage]);

  const queryParams = useMemo(
    () => ({
      page,
      pageSize: 10,
      search: searchQuery || undefined,
      status: activeTab === "active" ? "active" : filters.status || undefined,
      priority: activeTab === "active" ? undefined : filters.priority || undefined,
      assignedToId: loggedInTherapistId,
      fromDate: activeTab === "active" ? undefined : effectiveFromDate,
      toDate: activeTab === "active" ? undefined : effectiveToDate,
      dateField: effectiveDateField,
      sortBy: "createdAt",
      sortOrder: "desc",
    }),
    [
      activeTab,
      effectiveDateField,
      effectiveFromDate,
      effectiveToDate,
      filters,
      loggedInTherapistId,
      page,
      searchQuery,
    ],
  );

  const {
    data: tasksResponse,
    isLoading: isLoadingTasks,
    isError: isTasksError,
    error: tasksError,
    isFetching,
    refetch,
  } = useGetTasksQuery(queryParams, {
    skip: !loggedInTherapistId,
    refetchOnMountOrArgChange: true,
  });

  useEffect(() => {
    if (location.pathname !== "/therapist/tasks") return;
    setPage(1);
    accumulatorRef.current = { key: filterKey, tasks: [] };
    if (loggedInTherapistId) {
      void refetch();
    }
    // Only refetch when entering the tasks route, not when filters change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname, loggedInTherapistId, setPage]);

  const [createTask, { isLoading: isCreatingTask }] = useCreateTaskMutation();
  const [replaceTask, { isLoading: isUpdatingTask }] = useReplaceTaskMutation();
  const [deleteTask, { isLoading: isDeletingTask }] = useDeleteTaskMutation();
  const [triggerGetTaskById] =
    useLazyGetTaskByIdQuery();
  const [triggerGetTaskComments, { isFetching: isFetchingComments }] =
    useLazyGetTaskCommentsQuery();
  const [createComment, { isLoading: isCreatingComment }] =
    useCreateTaskCommentMutation();
  const [updateComment, { isLoading: isUpdatingComment }] =
    useUpdateTaskCommentMutation();
  const [deleteComment, { isLoading: isDeletingComment }] =
    useDeleteTaskCommentMutation();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    if (isTasksError && tasksError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(tasksError));
    }
  }, [isTasksError, tasksError]);

  // Derive displayed tasks from accumulator ref
  const allTasks = useMemo(() => {
    if (!tasksResponse) return accumulatorRef.current.tasks;
    const items = (tasksResponse.items || []).map(mapApiTaskToUiTask);
    if (accumulatorRef.current.key !== filterKey) {
      accumulatorRef.current = { key: filterKey, tasks: items };
    } else {
      const merged = [...accumulatorRef.current.tasks];

      items.forEach((task) => {
        const existingIndex = merged.findIndex((existingTask) => existingTask.id === task.id);
        if (existingIndex >= 0) {
          merged[existingIndex] = task;
          return;
        }
        merged.push(task);
      });

      accumulatorRef.current = { key: filterKey, tasks: merged };
    }
    return accumulatorRef.current.tasks;
  }, [tasksResponse, filterKey]);

  const refreshTasksListSilently = useCallback(async () => {
    setIsSilentTasksRefetch(true);
    try {
      await refetch();
    } finally {
      setIsSilentTasksRefetch(false);
    }
  }, [refetch]);

  const handleCloseDetailsModal = useCallback(() => {
    const shouldRefresh = taskCommentsChangedRef.current;
    taskCommentsChangedRef.current = false;

    setDetailsModalOpen(false);
    setTaskDetailsModalOpen(false);
    setSelectedTask(null);

    if (shouldRefresh) {
      void refreshTasksListSilently();
    }
  }, [refreshTasksListSilently]);

  const totalTasks = tasksResponse?.totalCount ?? 0;

  const handleEditTask = async (task: Task) => {
    if (taskCommentsChangedRef.current) {
      taskCommentsChangedRef.current = false;
      void refreshTasksListSilently();
    }
    setSelectedTask(null);
    setDetailsModalOpen(false);
    setTaskDetailsModalOpen(false);
    setEditModalOpen(true);
    setIsLoadingTaskDetails(true);
    try {
      const [taskPayload, commentsPayload] = await Promise.all([
        triggerGetTaskById(Number(task.id)).unwrap(),
        triggerGetTaskComments(Number(task.id)).unwrap(),
      ]);
      const mapped = mapApiTaskToUiTask(taskPayload);
      mapped.comments = commentsPayload.map((entry) => mapApiCommentToUiComment(entry));
      mapped.commentsCount = mapped.comments.length;
      setSelectedTask(mapped);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setSelectedTask(task);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const handleDeleteClick = (task: Task) => {
    setTaskDetailsModalOpen(false);
    setSelectedTask(task);
    setDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!selectedTask) return;
    try {
      await deleteTask(Number(selectedTask.id)).unwrap();
      accumulatorRef.current = {
        key: accumulatorRef.current.key,
        tasks: accumulatorRef.current.tasks.filter((entry) => entry.id !== selectedTask.id),
      };
      await refetch();
      setDeleteModalOpen(false);
      setSelectedTask(null);
      setToastType("success");
      setToastMessage("Task deleted successfully");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleCreateTask = async (newTaskData: {
    title: string;
    titleKey?: string;
    taskType?: string;
    clientId: number;
    priority: string;
    status: string;
    description?: string;
    dueDate?: string;
  }) => {
    if (!loggedInTherapistId) {
      setToastType("error");
      setToastMessage("Unable to resolve therapist profile.");
      return;
    }
    try {
      const created = await createTask({
        title: newTaskData.title,
        titleKey: newTaskData.titleKey,
        taskType: newTaskData.taskType,
        description: newTaskData.description?.trim() || undefined,
        priority: newTaskData.priority,
        status: newTaskData.status,
        clientId: newTaskData.clientId,
        assignedToId: loggedInTherapistId,
        dueDate: newTaskData.dueDate,
      }).unwrap();
      const mapped = mapApiTaskToUiTask(created);
      accumulatorRef.current = {
        key: accumulatorRef.current.key,
        tasks: [mapped, ...accumulatorRef.current.tasks],
      };
      await refetch();
      setCreateModalOpen(false);
      setToastType("success");
      setToastMessage("Task created successfully");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleViewDetails = async (task: Task) => {
    taskCommentsChangedRef.current = false;
    setSelectedTask(task);
    setDetailsModalOpen(false);
    setTaskDetailsModalOpen(true);
    setIsLoadingTaskDetails(true);
    try {
      const commentsPayload = await triggerGetTaskComments(Number(task.id)).unwrap();
      setSelectedTask((current) =>
        current?.id === task.id
          ? {
              ...current,
              comments: commentsPayload.map((entry) => mapApiCommentToUiComment(entry)),
              commentsCount: commentsPayload.length,
            }
          : current,
      );
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setTaskDetailsModalOpen(false);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const handleViewComments = async (task: Task) => {
    taskCommentsChangedRef.current = false;
    setSelectedTask(task);
    setTaskDetailsModalOpen(false);
    setDetailsModalOpen(true);
    setIsLoadingTaskDetails(true);
    try {
      const commentsPayload = await triggerGetTaskComments(Number(task.id)).unwrap();
      setSelectedTask((current) =>
        current?.id === task.id
          ? {
              ...current,
              comments: commentsPayload.map((entry) => mapApiCommentToUiComment(entry)),
              commentsCount: commentsPayload.length,
            }
          : current,
      );
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setDetailsModalOpen(false);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const handleUpdateTask = async (updatedTask: Task) => {
    if (!loggedInTherapistId) {
      setToastType("error");
      setToastMessage("Unable to resolve therapist profile.");
      return;
    }
    try {
      const updated = await replaceTask({
        id: Number(updatedTask.id),
        body: {
          title: updatedTask.title,
        titleKey: updatedTask.titleKey,
        taskType: updatedTask.taskType,
          description: updatedTask.description?.trim() || undefined,
          status: updatedTask.status,
          priority: updatedTask.priority,
          clientId: updatedTask.clientId ? Number(updatedTask.clientId) : undefined,
          assignedToId: loggedInTherapistId,
          dueDate: updatedTask.rawDueDate
            ? (() => {
                const parsed = new Date(updatedTask.rawDueDate);
                return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString();
              })()
            : undefined,
        },
      }).unwrap();

      const mapped = mapApiTaskToUiTask(updated);
      mapped.comments = updatedTask.comments || [];
      mapped.commentsCount = updatedTask.commentsCount || 0;
      accumulatorRef.current = {
        key: accumulatorRef.current.key,
        tasks: accumulatorRef.current.tasks.map((entry) => (entry.id === mapped.id ? mapped : entry)),
      };
      await refetch();
      setSelectedTask(mapped);
      setToastType("success");
      setToastMessage("Task updated successfully");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleAddComment = async (taskId: string, content: string, isInternal: boolean) => {
    try {
      const created = await createComment({
        taskId: Number(taskId),
        content,
        isInternal,
      }).unwrap();
      taskCommentsChangedRef.current = true;
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = [...(prev.comments || []), mapApiCommentToUiComment(created)];
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
      taskCommentsChangedRef.current = true;
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).map((entry) =>
          entry.id === commentId ? mapApiCommentToUiComment(updated) : entry,
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
      taskCommentsChangedRef.current = true;
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).filter((entry) => entry.id !== commentId);
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleLoadMore = useCallback(() => {
    if (page * 10 < totalTasks && !isFetching) {
      setPage((prev) => prev + 1);
    }
  }, [isFetching, page, totalTasks, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: page * 10 < totalTasks,
    isLoading: isFetching,
  });

  const hasTasks = allTasks.length > 0;
  const isTasksListLoading =
    !hasTasks &&
    !isSilentTasksRefetch &&
    (isLoadingTasks || isFetching) &&
    Boolean(loggedInTherapistId);

  return (
    <div className="relative flex h-full min-h-0 w-full flex-col overflow-hidden gap-6">
      <ScrollToTopButton />
      <div className="flex shrink-0 flex-col gap-6">
        <TaskSummaryCards counts={taskSummaryCounts} />

        <div className="flex flex-col lg:flex-row justify-between items-center gap-4">
          <div className="flex w-full flex-wrap items-center gap-4">
            <div className="bg-(--neutral-100) p-1 rounded-full flex h-10 items-center">
              <button
                onClick={() => setActiveTab("active")}
                className={`px-4 h-8 rounded-full text-sm font-medium transition-all cursor-pointer flex items-center ${
                  activeTab === "active"
                    ? "bg-white text-(--text-primary-dark) shadow-sm"
                    : "text-(--text-neutral-600) hover:text-(--text-primary-dark)"
                }`}
              >
                Active tasks
              </button>
              <button
                onClick={() => setActiveTab("all")}
                className={`px-4 h-8 rounded-full text-sm font-medium transition-all cursor-pointer flex items-center ${
                  activeTab === "all"
                    ? "bg-white text-(--text-primary-dark) shadow-sm"
                    : "text-(--text-neutral-600) hover:text-(--text-primary-dark)"
                }`}
              >
                All tasks
              </button>
            </div>

            <div
              className="flex items-center relative"
              onMouseEnter={() => setIsSearchExpanded(true)}
              onMouseLeave={() => !searchQuery && setIsSearchExpanded(false)}
            >
              <div
                className={cn(
                  "bg-white py-2 px-4 rounded-full border border-(--neutral-100) cursor-pointer transition-all duration-300 flex items-center justify-center whitespace-nowrap",
                  isSearchExpanded || searchQuery
                    ? "w-0 opacity-0 p-0 border-0 overflow-hidden"
                    : "opacity-100",
                )}
              >
                <Search className="size-4.5 text-(--text-neutral-600)" />
              </div>
              <div
                className={cn(
                  "transition-all duration-300 overflow-hidden",
                  isSearchExpanded || searchQuery ? "w-60 opacity-100 ml-0" : "w-0 opacity-0",
                )}
              >
                <CustomInput
                  placeholder="Search tasks, clients..."
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
                  className="rounded-full min-h-10 w-60 pb-0 pt-1.75"
                  onFocus={() => setIsSearchExpanded(true)}
                  onBlur={() => !searchQuery && setIsSearchExpanded(false)}
                />
              </div>
            </div>

            {activeTab === "all" && (
              <div className="flex items-center gap-2">
                <TaskTimeRangeFilter value={timeRange} onChange={handleTimeRangeChange} />
                <TaskFilters
                  onApplyFilters={handleApplyFilters}
                  appliedFilters={filters}
                />
              </div>
            )}
          </div>

          <div className="flex items-center gap-3 w-full lg:w-auto ml-auto">
            <Button
              onClick={() => navigate("history")}
              variant="outline"
              className="px-4 h-10 font-bold text-sm gap-2 rounded-full border border-(--neutral-200) bg-transparent text-(--bg-primary-dark) cursor-pointer"
            >
              <History size={16} />
              View History
            </Button>
            <Button
              className="px-4 h-10 font-bold text-sm gap-2 rounded-full bg-(--bg-primary-dark) text-white hover:bg-(--bg-primary-dark)/90 cursor-pointer shadow-none"
              onClick={() => setCreateModalOpen(true)}
            >
              <Plus size={16} />
              Add task
            </Button>
          </div>
        </div>

        {activeTab === "all" ? (
          <AppliedFiltersBar
            chips={appliedFilterChips}
            onRemove={handleRemoveFilterChip}
            onClearAll={handleClearAllFilters}
            className="w-full"
          />
        ) : null}
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain pb-4">
        {isTasksListLoading ? (
          <ContentLoader />
        ) : hasTasks ? (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {allTasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onEdit={(entry) => {
                  void handleEditTask(entry);
                }}
                onDelete={handleDeleteClick}
                onViewDetails={(entry) => {
                  void handleViewDetails(entry);
                }}
                onViewComments={(entry) => {
                  void handleViewComments(entry);
                }}
              />
            ))}
          </div>
        ) : (
          <EmptyTasksState />
        )}

        <div ref={observerTarget} className="mt-4 flex h-10 w-full items-center justify-center">
          {isFetching && allTasks.length > 0 && !isSilentTasksRefetch && (
            <ContentLoader variant="inline" size="md" />
          )}
        </div>
      </div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <ConfirmationModal
        type="delete"
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={() => void handleConfirmDelete()}
        title="Delete task?"
        description="Are you sure you want to delete this task? This action can't be undone"
        items={[]}
        confirmButtonText={isDeletingTask ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingTask}
      />

      <CreateTaskModal
        isOpen={createModalOpen}
        onClose={() => {
          if (!isCreatingTask && !isUpdatingTask) setCreateModalOpen(false);
        }}
        onCreate={(taskData) => void handleCreateTask(taskData)}
        isSubmitting={isCreatingTask || isUpdatingTask}
      />

      <EditTaskModal
        isOpen={editModalOpen}
        onClose={() => {
          if (!isUpdatingTask) {
            setEditModalOpen(false);
            setSelectedTask(null);
          }
        }}
        taskToEdit={selectedTask}
        onUpdate={async (taskData) => {
          if (selectedTask) {
            const updatedTask: Task = {
              ...selectedTask,
              title: taskData.title,
              titleKey: taskData.titleKey,
              taskType: taskData.taskType,
              priority: taskData.priority,
              status: taskData.status,
              description: taskData.description,
              clientId: String(taskData.clientId),
              rawDueDate: taskData.dueDate,
            };
            await handleUpdateTask(updatedTask);
            setEditModalOpen(false);
            setSelectedTask(null);
          }
        }}
        isSubmitting={isUpdatingTask}
      />

      <TaskCommentsModal
        isOpen={detailsModalOpen}
        task={selectedTask}
        isLoading={isLoadingTaskDetails || isFetchingComments}
        onClose={handleCloseDetailsModal}
        onAddComment={handleAddComment}
        onEditComment={handleEditComment}
        onDeleteComment={handleDeleteComment}
        isCommentMutating={isCreatingComment || isUpdatingComment || isDeletingComment}
      />

      <TaskDetailsModal
        isOpen={taskDetailsModalOpen}
        task={selectedTask}
        isLoadingTask={isLoadingTaskDetails}
        isLoadingComments={isFetchingComments}
        onClose={handleCloseDetailsModal}
        onEdit={(task) => {
          void handleEditTask(task);
        }}
        onDelete={(taskId) => {
          const task =
            selectedTask?.id === taskId
              ? selectedTask
              : allTasks.find((entry) => entry.id === taskId) || null;
          if (task) handleDeleteClick(task);
        }}
        onUpdateTask={(updatedTask) => {
          setSelectedTask(updatedTask);
        }}
        onAddComment={handleAddComment}
        onEditComment={handleEditComment}
        onDeleteComment={handleDeleteComment}
        isCommentMutating={isCreatingComment || isUpdatingComment || isDeletingComment}
      />
    </div>
  );
};

export default TasksPage;
