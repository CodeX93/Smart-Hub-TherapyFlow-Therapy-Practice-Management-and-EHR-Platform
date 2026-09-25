
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Search } from "lucide-react";
import TaskCard from "@/components/admin-task-sections/TaskCard";
import TaskCommentsModal from "@/components/therapist/tasks/TaskCommentsModal";
import TaskDetailsModal from "@/components/admin-task-sections/TaskDetailsModal";
import ConfirmationModal from "../../../components/shared/ConfirmationModal";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import type { Task, Comment } from "@/pages/therapist/tasks/tasks.static";
import type { AdminTaskFiltersState } from "@/components/admin-task-sections/AdminTaskFilters";
import AdminTaskFilters from "@/components/admin-task-sections/AdminTaskFilters";
import CreateTaskModal from "@/components/admin-task-sections/CreateTaskModal";
import EditTaskModal from "@/components/admin-task-sections/EditTaskModal";
import CustomInput from "@/components/form/CustomInput";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useCreateTaskMutation,
  useCreateTaskCommentMutation,
  useDeleteTaskCommentMutation,
  useDeleteTaskMutation,
  useGetTaskHistoryQuery,
  useLazyGetTaskByIdQuery,
  useLazyGetTaskCommentsQuery,
  useReplaceTaskMutation,
  useUpdateTaskCommentMutation,
  type TaskCommentItem,
} from "@/store/api/admin/tasks.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { mapApiTaskToUiTask } from "@/utils/tasks/mapApiTaskToUiTask";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import {
  buildTaskFilterChips,
  EMPTY_TASK_FILTERS,
  removeTaskFilterChip,
} from "@/utils/appliedFilterChips";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";

type TabType = "all" | "completed" | "overdue" | "recent";

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

const AdminTaskHistory = () => {
  const [activeTab, setActiveTab] = useState<TabType>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [filters, setFilters] = useState<AdminTaskFiltersState>(EMPTY_TASK_FILTERS);

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
  }, []);

  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [taskDetailsModalOpen, setTaskDetailsModalOpen] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [isLoadingTaskDetails, setIsLoadingTaskDetails] = useState(false);
  const [page, setPage] = useState(1);

  // Stable filter key (excludes page) to detect filter/tab changes
  const filterKey = useMemo(
    () => JSON.stringify({ activeTab, filters, searchQuery }),
    [activeTab, filters, searchQuery],
  );
  const accumulatorRef = useRef<{ key: string; tasks: Task[] }>({ key: "", tasks: [] });

  const queryParams = useMemo(
    () => ({
      page,
      pageSize: 10,
      search: searchQuery || undefined,
      status:
        activeTab === "completed"
          ? "completed"
          : activeTab === "overdue"
            ? "overdue"
            : filters.status
              ? filters.status.toLowerCase().replace(/\s+/g, "_")
              : undefined,
      priority: filters.priority ? filters.priority.toLowerCase() : undefined,
      assignedToId: filters.assignee ? Number(filters.assignee) : undefined,
      fromDate: filters.startDate
        ? new Date(filters.startDate).toISOString().split("T")[0]
        : undefined,
      toDate: filters.endDate
        ? new Date(filters.endDate).toISOString().split("T")[0]
        : undefined,
      sortBy: activeTab === "recent" ? "createdAt" : "updatedAt",
      sortOrder: "desc",
    }),
    [activeTab, filters, page, searchQuery],
  );

  const {
    data: historyResponse,
    isLoading: isLoadingHistory,
    isFetching: isFetchingHistory,
    isError: isHistoryError,
    error: historyError,
    refetch,
  } = useGetTaskHistoryQuery(queryParams);

  const [triggerGetTaskById] = useLazyGetTaskByIdQuery();
  const [triggerGetTaskComments, { isFetching: isFetchingComments }] = useLazyGetTaskCommentsQuery();
  const [createTask, { isLoading: isCreatingTask }] = useCreateTaskMutation();
  const [replaceTask, { isLoading: isUpdatingTask }] = useReplaceTaskMutation();
  const [deleteTask, { isLoading: isDeletingTask }] = useDeleteTaskMutation();
  const [createComment, { isLoading: isCreatingComment }] = useCreateTaskCommentMutation();
  const [updateComment, { isLoading: isUpdatingComment }] = useUpdateTaskCommentMutation();
  const [deleteComment, { isLoading: isDeletingComment }] = useDeleteTaskCommentMutation();

  // Toast auto-dismiss
  useEffect(() => {
    if (!toastMessage) return;
    const lower = toastMessage.toLowerCase();
    setToastType(lower.includes("success") ? "success" : "error");
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  // Error toast
  useEffect(() => {
    if (isHistoryError && historyError) {
      setToastMessage(getApiErrorMessage(historyError));
    }
  }, [historyError, isHistoryError]);

  // Reset accumulator and page on filter change
  useEffect(() => {
    setPage(1);
    accumulatorRef.current = { key: filterKey, tasks: [] };
  }, [filterKey, setPage]);

  // Derive displayed tasks
  const allTasks = useMemo(() => {
    if (!historyResponse) return accumulatorRef.current.tasks;
    const items = (historyResponse.items || []).map(mapApiTaskToUiTask);
    if (accumulatorRef.current.key !== filterKey) {
      accumulatorRef.current = { key: filterKey, tasks: items };
    } else {
      const existingIds = new Set(accumulatorRef.current.tasks.map((t) => t.id));
      const newItems = items.filter((t) => !existingIds.has(t.id));
      if (newItems.length > 0) {
        accumulatorRef.current = { key: filterKey, tasks: [...accumulatorRef.current.tasks, ...newItems] };
      }
    }
    return accumulatorRef.current.tasks;

  }, [historyResponse, filterKey]);

  const totalTasks = historyResponse?.totalCount ?? 0;

  const handleLoadMore = useCallback(() => {
    if (page * 10 < totalTasks && !isFetchingHistory) {
      setPage((prev) => prev + 1);
    }
  }, [isFetchingHistory, page, totalTasks, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: page * 10 < totalTasks,
    isLoading: isFetchingHistory,
  });

  const handleViewDetails = async (task: Task) => {
    setSelectedTask(task);
    setDetailsModalOpen(false);
    setTaskDetailsModalOpen(true);
    setIsLoadingTaskDetails(true);
    try {
      const commentsPayload = await triggerGetTaskComments(Number(task.id)).unwrap();
      setSelectedTask((current) =>
        current?.id === task.id
          ? { ...current, comments: commentsPayload.map(mapApiCommentToUiComment), commentsCount: commentsPayload.length }
          : current,
      );
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
      setTaskDetailsModalOpen(false);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const handleViewComments = async (task: Task) => {
    setSelectedTask(task);
    setTaskDetailsModalOpen(false);
    setDetailsModalOpen(true);
    setIsLoadingTaskDetails(true);
    try {
      const commentsPayload = await triggerGetTaskComments(Number(task.id)).unwrap();
      setSelectedTask((current) =>
        current?.id === task.id
          ? { ...current, comments: commentsPayload.map(mapApiCommentToUiComment), commentsCount: commentsPayload.length }
          : current,
      );
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
      setDetailsModalOpen(false);
    } finally {
      setIsLoadingTaskDetails(false);
    }
  };

  const handleEditTask = async (task: Task) => {
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
      mapped.comments = commentsPayload.map(mapApiCommentToUiComment);
      mapped.commentsCount = mapped.comments.length;
      setSelectedTask(mapped);
    } catch (error) {
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

  const handleDeleteTask = async () => {
    if (!selectedTask) return;
    try {
      await deleteTask(Number(selectedTask.id)).unwrap();
      accumulatorRef.current = {
        key: accumulatorRef.current.key,
        tasks: accumulatorRef.current.tasks.filter((t) => t.id !== selectedTask.id),
      };
      await refetch();
      setDeleteModalOpen(false);
      setSelectedTask(null);
      setToastMessage("Task deleted successfully");
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
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
      const created = await createTask({
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
      const mapped = mapApiTaskToUiTask(created);
      accumulatorRef.current = {
        key: accumulatorRef.current.key,
        tasks: [mapped, ...accumulatorRef.current.tasks],
      };
      await refetch();
      setCreateModalOpen(false);
      setToastMessage("Task created successfully");
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleUpdateTask = async (updatedTask: Task) => {
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
        tasks: accumulatorRef.current.tasks.map((t) => (t.id === mapped.id ? mapped : t)),
      };
      await refetch();
      setSelectedTask(mapped);
      setToastMessage("Task updated successfully");
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleAddComment = async (taskId: string, content: string, isInternal: boolean) => {
    try {
      const created = await createComment({ taskId: Number(taskId), content, isInternal }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = [...(prev.comments || []), mapApiCommentToUiComment(created)];
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleEditComment = async (taskId: string, commentId: string, content: string, isInternal: boolean) => {
    try {
      const updated = await updateComment({ taskId: Number(taskId), commentId: Number(commentId), content, isInternal }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).map((c) =>
          c.id === commentId ? mapApiCommentToUiComment(updated) : c,
        );
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleDeleteComment = async (taskId: string, commentId: string) => {
    try {
      await deleteComment({ taskId: Number(taskId), commentId: Number(commentId) }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).filter((c) => c.id !== commentId);
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const tabs = [
    { id: "all" as TabType, label: "All tasks" },
    { id: "completed" as TabType, label: "Completed" },
    { id: "overdue" as TabType, label: "Overdue" },
    { id: "recent" as TabType, label: "Recent" },
  ];

  return (
    <div className="flex h-[calc(100vh-10.5rem)] min-h-0 flex-col overflow-hidden">
      <ScrollToTopButton />
      <div className="mb-4 flex flex-none flex-col justify-between gap-4 lg:flex-row lg:items-center">
        <div className="bg-(--neutral-100) p-1 rounded-full flex h-10 items-center">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 h-8 rounded-full text-sm font-medium transition-all cursor-pointer flex items-center ${
                activeTab === tab.id
                  ? "bg-white text-(--text-primary-dark) shadow-sm"
                  : "text-(--text-neutral-600) hover:text-(--text-primary-dark)"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3">
          <CustomInput
            placeholder="Search task history..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="rounded-full min-h-10 md:w-79 pb-0 pt-1.75"
          />
          <AdminTaskFilters
            onApplyFilters={setFilters}
            appliedFilters={filters}
            showAssigneeFilter
          />
        </div>
      </div>

      <AppliedFiltersBar
        chips={appliedFilterChips}
        onRemove={handleRemoveFilterChip}
        onClearAll={handleClearAllFilters}
        className="mb-4 shrink-0"
      />

      <div className="min-h-0 flex-1 overflow-y-auto pr-1">
        {isLoadingHistory && allTasks.length === 0 ? (
          <ContentLoader size="md" className="py-20" />
        ) : allTasks.length > 0 ? (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
            {allTasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onViewDetails={(entry) => { void handleViewDetails(entry); }}
                onViewComments={(entry) => { void handleViewComments(entry); }}
                onEdit={(entry) => { void handleEditTask(entry); }}
                onDelete={handleDeleteClick}
              />
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-gray-100">
              <Search className="h-8 w-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-[#101828] mb-1">No tasks found</h3>
            <p className="text-sm text-[#8E95A2]">Try adjusting your filters or search query</p>
          </div>
        )}

        <div ref={observerTarget} className="mt-4 flex h-10 w-full items-center justify-center">
          {isFetchingHistory && allTasks.length > 0 && (
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

      <TaskCommentsModal
        isOpen={detailsModalOpen}
        task={selectedTask}
        isLoading={isLoadingTaskDetails || isFetchingComments}
        onClose={() => setDetailsModalOpen(false)}
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
        onClose={() => {
          setTaskDetailsModalOpen(false);
          setSelectedTask(null);
        }}
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

      <ConfirmationModal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={() => void handleDeleteTask()}
        type="delete"
        title="Delete task?"
        description="Are you sure you want to delete this task? This action can't be undone"
        items={[]}
        confirmButtonText={isDeletingTask ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingTask}
      />
    </div>
  );
};

export default AdminTaskHistory;
