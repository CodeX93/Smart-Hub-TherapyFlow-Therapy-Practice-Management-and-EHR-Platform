
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useCallback, useEffect, useMemo, useRef } from "react";
import { cn } from "@/lib/utils";
import { Plus, Search, History } from "lucide-react";
import { useNavigate } from "react-router-dom";
import TaskSummaryCards from "@/components/shared/TaskSummaryCards";
import TaskCard from "../../../components/admin-task-sections/TaskCard";
import CreateTaskModal from "../../../components/admin-task-sections/CreateTaskModal";
import EditTaskModal from "../../../components/admin-task-sections/EditTaskModal";
import TaskCommentsModal from "@/components/therapist/tasks/TaskCommentsModal";
import TaskDetailsModal from "@/components/admin-task-sections/TaskDetailsModal";
import ConfirmationModal from "../../../components/shared/ConfirmationModal";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { Button } from "../../../components/ui/button";
import type { Task, Comment } from "@/pages/therapist/tasks/tasks.static";
import AdminTaskFilters, {
  type AdminTaskFiltersState,
} from "../../../components/admin-task-sections/AdminTaskFilters";
import CustomInput from "@/components/form/CustomInput";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useCreateTaskMutation,
  useCreateTaskCommentMutation,
  useDeleteTaskCommentMutation,
  useLazyGetTaskByIdQuery,
  useLazyGetTaskCommentsQuery,
  useGetTasksQuery,
  useDeleteTaskMutation,
  useReplaceTaskMutation,
  useUpdateTaskCommentMutation,
  type TaskCommentItem,
} from "@/store/api/admin/tasks.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { mapApiTaskToUiTask } from "@/utils/tasks/mapApiTaskToUiTask";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import EmptyTasksState from "@/components/shared/EmptyTasksState";
import {
  buildTaskFilterChips,
  EMPTY_TASK_FILTERS,
  removeTaskFilterChip,
} from "@/utils/appliedFilterChips";
import { useTaskSummaryCounts } from "@/hooks/useTaskSummaryCounts";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";
import TaskTimeRangeFilter from "@/components/shared/TaskTimeRangeFilter";
import {
  getTaskTimeRangeDateField,
  getTaskTimeRangeDates,
  type TaskTimeRange,
} from "@/utils/taskTimeRange";

// Filter values are system-option keys; the backend resolves them against the catalogue.
function mapStatusFilterToApiStatus(status: string | null): string | undefined {
  return status?.trim() || undefined;
}

function mapPriorityFilterToApiPriority(priority: string | null): string | undefined {
  return priority?.trim() || undefined;
}

/** Catalogue-key form of a status ("In Progress" and "in_progress" both become "in_progress"). */
function taskStatusKey(status: string | null | undefined): string {
  return String(status ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, "_");
}

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

const AdminTasksPage = () => {
  const navigate = useNavigate();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const [page, setPage] = useState(1);

  const [searchQuery, setSearchQuery] = useState("");
  const [isSearchExpanded, setIsSearchExpanded] = useState(false);
  const [activeTab, setActiveTab] = useState<"all" | "active">("active");

  const [filters, setFilters] = useState<AdminTaskFiltersState>(EMPTY_TASK_FILTERS);
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

  const handleApplyFilters = useCallback((nextFilters: AdminTaskFiltersState) => {
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

  // Build query params
  // Active tab: status=active (pending + in_progress + overdue). Date/panel filters only on All.
  const queryParams = useMemo(() => ({
    page,
    pageSize: 10,
    search: searchQuery || undefined,
    status: activeTab === "active" ? "active" : mapStatusFilterToApiStatus(filters.status),
    priority: activeTab === "active" ? undefined : mapPriorityFilterToApiPriority(filters.priority),
    assignedToId:
      activeTab === "active"
        ? undefined
        : filters.assignee && /^\d+$/.test(filters.assignee)
          ? Number(filters.assignee)
          : undefined,
    fromDate: activeTab === "active" ? undefined : effectiveFromDate,
    toDate: activeTab === "active" ? undefined : effectiveToDate,
    dateField: effectiveDateField,
    sortBy: "createdAt",
    sortOrder: "desc",
  }), [page, searchQuery, activeTab, filters, effectiveFromDate, effectiveToDate, effectiveDateField]);

  // API calls
  const { data: tasksResponse, isLoading: isLoadingTasks, isError: isTasksError, error: tasksError, isFetching, refetch } = useGetTasksQuery(queryParams);
  const [createTask, { isLoading: isCreatingTask }] = useCreateTaskMutation();
  const [triggerGetTaskById] = useLazyGetTaskByIdQuery();
  const [triggerGetTaskComments, { isFetching: isFetchingComments }] = useLazyGetTaskCommentsQuery();
  const [deleteTask, { isLoading: isDeletingTask }] = useDeleteTaskMutation();
  const [replaceTask, { isLoading: isUpdatingTask }] = useReplaceTaskMutation();
  const [createComment, { isLoading: isCreatingComment }] = useCreateTaskCommentMutation();
  const [updateComment, { isLoading: isUpdatingComment }] = useUpdateTaskCommentMutation();
  const [deleteComment, { isLoading: isDeletingComment }] = useDeleteTaskCommentMutation();

  // Toast handling
  useEffect(() => {
    if (!toastMessage) return;
    const lower = toastMessage.toLowerCase();
    if (
      lower.includes("sending") ||
      lower.includes("preparing") ||
      lower.includes("started")
    ) {
      setToastType("info");
    } else if (lower.includes("success")) {
      setToastType("success");
    }
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    if (isTasksError && tasksError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(tasksError));
    }
  }, [isTasksError, tasksError]);

  // Clear tasks when filters change
  // (handled by accumulatorRef reset above)

  const totalTasks = tasksResponse?.totalCount ?? 0;
  const taskSummaryCounts = useTaskSummaryCounts({
    fromDate: activeTab === "active" ? undefined : effectiveFromDate,
    toDate: activeTab === "active" ? undefined : effectiveToDate,
    dateField: effectiveDateField,
  });

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [taskDetailsModalOpen, setTaskDetailsModalOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  // Stable filter key (excludes page) to detect filter/tab changes
  const filterKey = useMemo(
    () =>
      JSON.stringify({
        searchQuery,
        activeTab,
        effectiveFromDate,
        effectiveToDate,
        ...(activeTab === "all" ? { filters } : {}),
      }),
    [searchQuery, activeTab, effectiveFromDate, effectiveToDate, filters],
  );
  const accumulatorRef = useRef<{ key: string; tasks: Task[] }>({ key: "", tasks: [] });
  const taskCommentsChangedRef = useRef(false);
  const [isSilentTasksRefetch, setIsSilentTasksRefetch] = useState(false);

  // Reset accumulator when filters change
  useEffect(() => {
    setPage(1);
    accumulatorRef.current = { key: filterKey, tasks: [] };
  }, [filterKey, setPage]);

  // Derive tasks list from accumulator
  const allTasks = useMemo(() => {
    if (!tasksResponse) return accumulatorRef.current.tasks;
    const items = (tasksResponse.items || []).map((item) => mapApiTaskToUiTask(item));
    if (accumulatorRef.current.key !== filterKey) {
      accumulatorRef.current = { key: filterKey, tasks: items };
    } else {
      const existingById = new Map(
        accumulatorRef.current.tasks.map((task) => [task.id, task] as const),
      );
      const merged = [...accumulatorRef.current.tasks];

      items.forEach((task) => {
        const existingIndex = merged.findIndex((existingTask) => existingTask.id === task.id);
        if (existingIndex >= 0) {
          merged[existingIndex] = task;
          existingById.set(task.id, task);
          return;
        }
        merged.push(task);
      });

      accumulatorRef.current = { key: filterKey, tasks: merged };
    }
    return accumulatorRef.current.tasks;

  }, [tasksResponse, filterKey]);
  const [isLoadingTaskDetails, setIsLoadingTaskDetails] = useState(false);

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
      const latestTask = await triggerGetTaskById(Number(task.id)).unwrap();
      setSelectedTask(mapApiTaskToUiTask(latestTask));
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
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
    if (selectedTask) {
      try {
        await deleteTask(Number(selectedTask.id)).unwrap();
        accumulatorRef.current = {
          key: accumulatorRef.current.key,
          tasks: accumulatorRef.current.tasks.filter((t) => t.id !== selectedTask.id),
        };
        await refetch();
        setToastType("success");
        setToastMessage("Task deleted successfully");
      } catch (err) {
        setToastType("error");
        setToastMessage(getApiErrorMessage(err));
      }
      setDeleteModalOpen(false);
      setSelectedTask(null);
    }
  };

  const handleCreateTask = async (newTaskData: {
    title: string;
    titleKey?: string;
    taskType?: string;
    clientId: number;
    assignedToId?: number;
    priority: string;
    status: string;
    description?: string;
    dueDate?: string;
  }) => {
    try {
      const createdTask = await createTask({
        title: newTaskData.title,
        titleKey: newTaskData.titleKey,
        taskType: newTaskData.taskType,
        description: newTaskData.description?.trim() || undefined,
        priority: newTaskData.priority,
        status: newTaskData.status,
        clientId: newTaskData.clientId,
        assignedToId: newTaskData.assignedToId,
        dueDate: newTaskData.dueDate,
      }).unwrap();

      const mapped = mapApiTaskToUiTask(createdTask);
      accumulatorRef.current = {
        key: accumulatorRef.current.key,
        tasks: [mapped, ...accumulatorRef.current.tasks],
      };
      await refetch();
      setCreateModalOpen(false);
      setToastType("success");
      setToastMessage("Task created successfully");
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
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
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
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
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
      setDetailsModalOpen(false);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const handleUpdateTask = async (updatedTask: Task) => {
    try {
      await replaceTask({ id: Number(updatedTask.id), body: {
        title: updatedTask.title,
        titleKey: updatedTask.titleKey,
        taskType: updatedTask.taskType,
        status: updatedTask.status,
        priority: updatedTask.priority,
        description: updatedTask.description?.trim() || undefined,
        clientId: updatedTask.clientId ? Number(updatedTask.clientId) : undefined,
        assignedToId: updatedTask.assigneeId ? Number(updatedTask.assigneeId) : undefined,
        dueDate: updatedTask.rawDueDate ? new Date(updatedTask.rawDueDate).toISOString() : undefined,
      }}).unwrap();
      // Mirror the delete path's prune: the refetch merge only updates and appends,
      // so a task whose new status falls outside the current list (off the active
      // board, or out of the All-tab status filter) must be dropped here.
      const nextStatusKey = taskStatusKey(updatedTask.status);
      const filterStatusKey = activeTab === "all" ? taskStatusKey(filters.status) : "";
      const leavesCurrentList =
        activeTab === "active"
          ? nextStatusKey === "completed"
          : filterStatusKey !== "" && nextStatusKey !== filterStatusKey;
      accumulatorRef.current = {
        key: accumulatorRef.current.key,
        tasks: leavesCurrentList
          ? accumulatorRef.current.tasks.filter((t) => t.id !== updatedTask.id)
          : accumulatorRef.current.tasks.map((t) => (t.id === updatedTask.id ? updatedTask : t)),
      };
      await refetch();
      setSelectedTask(updatedTask);
      setToastType("success");
      setToastMessage("Task updated successfully");
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleAddComment = async (taskId: string, content: string, isInternal: boolean) => {
    try {
      const created = await createComment({ taskId: Number(taskId), content, isInternal }).unwrap();
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

  const handleEditComment = async (taskId: string, commentId: string, content: string, isInternal: boolean) => {
    try {
      const updated = await updateComment({ taskId: Number(taskId), commentId: Number(commentId), content, isInternal }).unwrap();
      taskCommentsChangedRef.current = true;
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).map((c) =>
          c.id === commentId ? mapApiCommentToUiComment(updated) : c,
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
        const nextComments = (prev.comments || []).filter((c) => c.id !== commentId);
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  // Infinite Scroll Logic
  const handleLoadMore = useCallback(() => {
    if (page * 10 < totalTasks && !isFetching) {
      setPage(prev => prev + 1);
    }
  }, [page, totalTasks, isFetching, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: page * 10 < totalTasks,
    isLoading: isFetching,
  });

  const visibleTasks = useMemo(
    () =>
      activeTab === "active"
        ? allTasks.filter((task) => taskStatusKey(task.status) !== "completed")
        : allTasks,
    [activeTab, allTasks],
  );
  const hasTasks = visibleTasks.length > 0;

  return (
    <div className="flex flex-col gap-0 w-full relative">
      <ScrollToTopButton />
      {/* Summary Cards and Controls - Fixed Section */}
      <div className="flex-none flex flex-col gap-6">
        {/* Summary Cards */}
        <TaskSummaryCards counts={taskSummaryCounts} />

        {/* Controls Bar */}
        <div className="flex flex-col lg:flex-row justify-between items-center gap-4">
          <div className="flex w-full flex-wrap items-center gap-2">
            {/* Tabs */}
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

            {/* Search */}
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
                  isSearchExpanded || searchQuery
                    ? "w-60 opacity-100 ml-0"
                    : "w-0 opacity-0",
                )}
              >
                <CustomInput
                  placeholder="Search tasks, clients..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  icon={
                    <Search className="size-4.5 text-(--text-neutral-600)" />
                  }
                  className="rounded-full min-h-10 w-60 pb-0 pt-1.75"
                  onFocus={() => setIsSearchExpanded(true)}
                  onBlur={() => !searchQuery && setIsSearchExpanded(false)}
                />
              </div>
            </div>

            {activeTab === "all" && (
              <div className="flex items-center gap-2">
                <TaskTimeRangeFilter value={timeRange} onChange={handleTimeRangeChange} />
                <AdminTaskFilters
                  onApplyFilters={handleApplyFilters}
                  appliedFilters={filters}
                  showAssigneeFilter
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

      {/* Tasks Grid */}
      <div className="pt-6">
        {isLoadingTasks && allTasks.length === 0 ? (
          <ContentLoader />
        ) : hasTasks ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pb-4">
            {visibleTasks.map((task) => (
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
      </div>

      {/* Loading Indicator */}
      <div
        ref={observerTarget}
        className="h-10 w-full flex items-center justify-center mt-4"
      >
        {isFetching && allTasks.length > 0 && !isSilentTasksRefetch && (
          <ContentLoader variant="inline" size="md" />
        )}
      </div>

      {/* Toast Message */}
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      {/* Modals */}
      <ConfirmationModal
        type="delete"
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Delete task?"
        description="Are you sure you want to delete this task? This action can't be undone"
        items={[]} // Empty items to match design specific modal
        confirmButtonText={isDeletingTask ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingTask}
      />

      <CreateTaskModal
        isOpen={createModalOpen}
        onClose={() => {
          if (!isCreatingTask) setCreateModalOpen(false);
        }}
        onCreate={(taskData) => void handleCreateTask(taskData)}
        isSubmitting={isCreatingTask}
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
              assigneeId: String(taskData.assignedToId),
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

export default AdminTasksPage;
