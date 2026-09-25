
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import platform from "@/assets/figma/tasks-empty/platform.svg";
import clipboardBody from "@/assets/figma/tasks-empty/clipboard-body.svg";
import clipboardClip from "@/assets/figma/tasks-empty/clipboard-clip.svg";
import clipboardLines from "@/assets/figma/tasks-empty/clipboard-lines.svg";
import shadow from "@/assets/figma/tasks-empty/shadow.svg";
import ClientTaskCard from "./ClientTaskCard";
import CreateTaskModal from "@/components/therapist/tasks/CreateTaskModal";
import EditTaskModal from "@/components/therapist/tasks/EditTaskModal";
import TaskCommentsModal from "@/components/therapist/tasks/TaskCommentsModal";
import TaskDetailsModal from "@/components/therapist/tasks/TaskDetailsModal";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import Toast from "@/components/shared/Toast";
import type { Task, Comment } from "@/pages/therapist/tasks/tasks.static";
import type { Client } from "@/types/client.type";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  useCreateTaskMutation,
  useCreateTaskCommentMutation,
  useDeleteTaskCommentMutation,
  useDeleteTaskMutation,
  useLazyGetTaskCommentsQuery,
  useReplaceTaskMutation,
  useUpdateTaskCommentMutation,
  type TaskCommentItem,
} from "@/store/api/admin/tasks.api";
import { useGetAuthMeQuery } from "@/store/api/authApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { useClientTaskPages } from "@/hooks/useClientTaskPages";

interface TasksTabProps {
  client: Client;
  openCreateTrigger?: number;
  readOnly?: boolean;
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

const ClientTasks = ({ client, openCreateTrigger = 0, readOnly = false }: TasksTabProps) => {
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isCommentsModalOpen, setIsCommentsModalOpen] = useState(false);
  const [isTaskDetailsModalOpen, setIsTaskDetailsModalOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);

  const { data: authMeData } = useGetAuthMeQuery();
  const loggedInTherapistId = authMeData?.user?.id;

  const {
    tasks, hasMore, handleLoadMore, refreshTasks,
    isLoading: isLoadingTasks, isFetching, isError, error,
  } = useClientTaskPages(String(client.id), loggedInTherapistId, !loggedInTherapistId);

  const [createTask, { isLoading: isCreatingTask }] = useCreateTaskMutation();
  const [replaceTask, { isLoading: isUpdatingTask }] = useReplaceTaskMutation();
  const [deleteTask, { isLoading: isDeletingTask }] = useDeleteTaskMutation();
  const [triggerGetTaskComments, { isFetching: isFetchingComments }] = useLazyGetTaskCommentsQuery();
  const [createComment, { isLoading: isCreatingComment }] = useCreateTaskCommentMutation();
  const [updateComment, { isLoading: isUpdatingComment }] = useUpdateTaskCommentMutation();
  const [deleteComment, { isLoading: isDeletingComment }] = useDeleteTaskCommentMutation();

  const [handledCreateTrigger, setHandledCreateTrigger] = useState(0);

  if (openCreateTrigger && openCreateTrigger !== handledCreateTrigger) {
    setHandledCreateTrigger(openCreateTrigger);
    setIsEditModalOpen(false);
    setSelectedTask(null);
    setIsCreateModalOpen(true);
  }

  const [dismissedError, setDismissedError] = useState<unknown>();
  const queryErrorMessage = isError && error !== dismissedError ? getApiErrorMessage(error) : null;
  const displayedToast = toastMessage || queryErrorMessage;
  const dismissToast = () => {
    setToastMessage(null);
    setDismissedError(error);
  };

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore,
    isLoading: isFetching,
  });

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
      await createTask({
        title: newTaskData.title,
        titleKey: newTaskData.titleKey,
        taskType: newTaskData.taskType,
        description: newTaskData.description?.trim() || undefined,
        priority: newTaskData.priority,
        status: newTaskData.status,
        clientId: Number(client.id),
        assignedToId: loggedInTherapistId,
        dueDate: newTaskData.dueDate,
      }).unwrap();
      await refreshTasks();
      setIsCreateModalOpen(false);
      setToastType("success");
      setToastMessage("Task created successfully");
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleUpdateTask = async (updatedTask: Task) => {
    if (!loggedInTherapistId) {
      setToastType("error");
      setToastMessage("Unable to resolve therapist profile.");
      return;
    }

    try {
      await replaceTask({
        id: Number(updatedTask.id),
        body: {
          title: updatedTask.title,
        titleKey: updatedTask.titleKey,
        taskType: updatedTask.taskType,
          description: updatedTask.description?.trim() || undefined,
          status: updatedTask.status,
          priority: updatedTask.priority,
          clientId: Number(client.id),
          assignedToId: loggedInTherapistId,
          dueDate: updatedTask.rawDueDate ? new Date(updatedTask.rawDueDate).toISOString() : undefined,
        },
      }).unwrap();
      await refreshTasks();
      setSelectedTask(updatedTask);
      setToastType("success");
      setToastMessage("Task updated successfully");
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleDeleteTask = async () => {
    if (!selectedTask) return;
    try {
      await deleteTask(Number(selectedTask.id)).unwrap();
      await refreshTasks();
      setIsDeleteModalOpen(false);
      setSelectedTask(null);
      setToastType("success");
      setToastMessage("Task deleted successfully");
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleOpenComments = async (task: Task) => {
    setSelectedTask(task);
    setIsTaskDetailsModalOpen(false);
    setIsCommentsModalOpen(true);
    try {
      const commentsPayload = await triggerGetTaskComments(Number(task.id)).unwrap();
      setSelectedTask((current) =>
        current?.id === task.id
          ? {
              ...current,
              comments: commentsPayload.map(mapApiCommentToUiComment),
              commentsCount: commentsPayload.length,
            }
          : current,
      );
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
      setIsCommentsModalOpen(false);
    }
  };

  const handleOpenTaskDetails = async (task: Task) => {
    setSelectedTask(task);
    setIsCommentsModalOpen(false);
    setIsTaskDetailsModalOpen(true);
    try {
      const commentsPayload = await triggerGetTaskComments(Number(task.id)).unwrap();
      setSelectedTask((current) =>
        current?.id === task.id
          ? {
              ...current,
              comments: commentsPayload.map(mapApiCommentToUiComment),
              commentsCount: commentsPayload.length,
            }
          : current,
      );
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
      setIsTaskDetailsModalOpen(false);
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
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleEditComment = async (taskId: string, commentId: string, content: string, isInternal: boolean) => {
    try {
      const updated = await updateComment({ taskId: Number(taskId), commentId: Number(commentId), content, isInternal }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).map((entry) =>
          entry.id === commentId ? mapApiCommentToUiComment(updated) : entry,
        );
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleDeleteComment = async (taskId: string, commentId: string) => {
    try {
      await deleteComment({ taskId: Number(taskId), commentId: Number(commentId) }).unwrap();
      setSelectedTask((prev) => {
        if (!prev || prev.id !== taskId) return prev;
        const nextComments = (prev.comments || []).filter((entry) => entry.id !== commentId);
        return { ...prev, comments: nextComments, commentsCount: nextComments.length };
      });
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  return (
    <div className="flex h-full flex-col space-y-4 px-5 py-5 animate-in fade-in duration-300">
      <div className="flex min-h-10 items-center justify-between gap-4">
        <h2 className="text-base font-semibold leading-6 text-[#1B1C20]">Client Tasks</h2>
        <div className="flex items-center gap-3">
          {tasks.length > 0 && (
            <Button variant="outline" className="h-10 rounded-full border-[#D8DBDF] px-4 text-sm font-semibold text-[#3C4D58]" disabled>
              View all tasks
            </Button>
          )}
          {!readOnly && tasks.length > 0 ? (
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            className="h-10 gap-2 rounded-full bg-[#3C4D58] pr-4 pl-3 text-sm font-semibold shadow-none hover:bg-[#323E47]"
          >
            <Plus size={20} />
            Add task
          </Button>
          ) : null}
        </div>
      </div>

      {isLoadingTasks && tasks.length === 0 ? (
        <ContentLoader className="-1" />
      ) : tasks.length === 0 ? (
        <div className="flex min-h-[28rem] flex-1 flex-col items-center justify-center text-center">
          <div
            className="relative mb-4 inline-grid shrink-0 grid-cols-[max-content] grid-rows-[max-content] place-items-start leading-[0]"
            aria-hidden="true"
          >
            <img
              src={platform}
              alt=""
              className="col-start-1 row-start-1 mt-11 h-6 w-[7.5rem]"
            />
            <div className="relative col-start-1 row-start-1 ml-8 size-14 overflow-clip rounded-[0.729rem]">
              <div className="absolute inset-[16.67%_12.5%_8.34%_12.5%]">
                <img
                  src={clipboardBody}
                  alt=""
                  className="absolute inset-0 block size-full max-w-none"
                />
              </div>
              <div className="absolute bottom-3/4 left-1/3 right-1/3 top-[8.33%]">
                <div className="absolute inset-[-10.71%_-5.36%]">
                  <img
                    src={clipboardClip}
                    alt=""
                    className="absolute inset-0 block size-full max-w-none"
                  />
                </div>
              </div>
              <div className="absolute inset-[40.63%_26.04%_23.96%_26.04%]">
                <img
                  src={clipboardLines}
                  alt=""
                  className="absolute inset-0 block size-full max-w-none"
                />
              </div>
            </div>
            <div className="relative col-start-1 row-start-1 ml-7 mt-[3.1625rem] h-[0.6875rem] w-[4.0625rem]">
              <div className="absolute inset-[-42.64%_-7.22%]">
                <img
                  src={shadow}
                  alt=""
                  className="absolute inset-0 block size-full max-w-none"
                />
              </div>
            </div>
          </div>
          <h3 className="text-xl font-semibold leading-7 text-[#1B1C20]">No tasks yet</h3>
          <p className="mt-1 max-w-[19.375rem] text-base leading-6 text-[#5B616E]">
            Create the first task for this client to get started.
          </p>
          {!readOnly ? (
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            className="mt-6 h-[2.875rem] gap-2 rounded-full bg-[#3C4D58] px-6 text-sm font-semibold shadow-none hover:bg-[#323E47]"
          >
            <Plus size={24} />
            Create first task
          </Button>
          ) : null}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {tasks.map((task) => (
              <ClientTaskCard
                key={task.id}
                task={task}
                readOnly={readOnly}
                onDelete={(entry) => {
                  setIsTaskDetailsModalOpen(false);
                  setSelectedTask(entry);
                  setIsDeleteModalOpen(true);
                }}
                onEdit={(entry) => {
                  setIsTaskDetailsModalOpen(false);
                  setSelectedTask(entry);
                  setIsEditModalOpen(true);
                }}
                onViewDetails={(entry) => {
                  void handleOpenTaskDetails(entry);
                }}
                onViewComments={(entry) => {
                  void handleOpenComments(entry);
                }}
              />
            ))}
          </div>
          <div ref={observerTarget} className="h-10 w-full flex items-center justify-center mt-2">
            {isFetching && <ContentLoader variant="inline" size="md" />}
          </div>
        </>
      )}

      {displayedToast ? (
        <Toast
          message={displayedToast}
          type={toastMessage ? toastType : "error"}
          onClose={dismissToast}
        />
      ) : null}

      <CreateTaskModal
        isOpen={isCreateModalOpen}
        onClose={() => {
          if (!isCreatingTask) setIsCreateModalOpen(false);
        }}
        onCreate={(taskData) => void handleCreateTask(taskData)}
        isSubmitting={isCreatingTask}
        client={{
          id: String(client.id),
          name: client.name,
          clientId: client.clientId,
        }}
      />

      <EditTaskModal
        isOpen={isEditModalOpen}
        onClose={() => {
          if (!isUpdatingTask) {
            setIsEditModalOpen(false);
            setSelectedTask(null);
          }
        }}
        taskToEdit={selectedTask}
        isSubmitting={isUpdatingTask}
        client={{
          id: String(client.id),
          name: client.name,
          clientId: client.clientId,
        }}
        onUpdate={async (taskData) => {
          if (!selectedTask) return;
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
          } as Task;
          await handleUpdateTask(updatedTask);
          setIsEditModalOpen(false);
          setSelectedTask(null);
        }}
      />

      <TaskCommentsModal
        isOpen={isCommentsModalOpen}
        task={selectedTask}
        isLoading={isFetchingComments}
        onClose={() => {
          setIsCommentsModalOpen(false);
          setSelectedTask(null);
        }}
        onAddComment={readOnly ? undefined : handleAddComment}
        onEditComment={readOnly ? undefined : handleEditComment}
        onDeleteComment={readOnly ? undefined : handleDeleteComment}
        isCommentMutating={isCreatingComment || isUpdatingComment || isDeletingComment}
      />

      <TaskDetailsModal
        isOpen={isTaskDetailsModalOpen}
        task={selectedTask}
        isLoadingComments={isFetchingComments}
        onClose={() => {
          setIsTaskDetailsModalOpen(false);
          setSelectedTask(null);
        }}
        onEdit={
          readOnly
            ? undefined
            : (task) => {
                setIsTaskDetailsModalOpen(false);
                setSelectedTask(task);
                setIsEditModalOpen(true);
              }
        }
        onDelete={
          readOnly
            ? undefined
            : (taskId) => {
                const task =
                  selectedTask?.id === taskId
                    ? selectedTask
                    : tasks.find((entry) => entry.id === taskId) || null;
                if (!task) return;
                setIsTaskDetailsModalOpen(false);
                setSelectedTask(task);
                setIsDeleteModalOpen(true);
              }
        }
        onUpdateTask={(updatedTask) => {
          setSelectedTask(updatedTask);
        }}
        onAddComment={readOnly ? undefined : handleAddComment}
        onEditComment={readOnly ? undefined : handleEditComment}
        onDeleteComment={readOnly ? undefined : handleDeleteComment}
        isCommentMutating={isCreatingComment || isUpdatingComment || isDeletingComment}
        readOnly={readOnly}
      />

      <ConfirmationModal
        type="delete"
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={() => void handleDeleteTask()}
        title="Delete task?"
        description="Are you sure you want to delete this task? This action can't be undone"
        items={[]}
        confirmButtonText={isDeletingTask ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingTask}
      />
    </div>
  );
};

const TasksTab = (props: TasksTabProps) => <ClientTasks key={props.client.id} {...props} />;

export default TasksTab;
