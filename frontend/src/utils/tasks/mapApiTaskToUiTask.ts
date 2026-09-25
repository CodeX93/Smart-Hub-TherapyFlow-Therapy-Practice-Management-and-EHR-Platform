import type { TaskItem } from "@/store/api/admin/tasks.api";
import type { Task } from "@/pages/therapist/tasks/tasks.static";

function normalizeOptionKey(value?: string | null): string {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, "_");
}

export function mapApiTaskToUiTask(item: TaskItem): Task {
  const priorityKey = normalizeOptionKey(item.priority) || "medium";
  const statusKey = normalizeOptionKey(item.status) || "pending";

  return {
    id: String(item.id),
    title: item.title || "Untitled task",
    titleKey: item.titleKey ?? null,
    taskType: item.taskType ?? null,
    clientName: item.clientName || "Unknown Client",
    clientId: item.clientId != null ? String(item.clientId) : "",
    assignee: item.assignedToName || "",
    assigneeId: item.assignedToId != null ? String(item.assignedToId) : "",
    priority: priorityKey === "urgen" || priorityKey === "urgency" ? "urgent" : priorityKey,
    status: statusKey === "inprogress" ? "in_progress" : statusKey,
    createdDate: item.createdAt
      ? new Date(item.createdAt).toLocaleDateString("en-US", {
          month: "short",
          day: "numeric",
          year: "numeric",
        })
      : "",
    dueDate: item.dueDate
      ? new Date(item.dueDate).toLocaleDateString("en-US", {
          month: "short",
          day: "numeric",
          year: "numeric",
        })
      : "---",
    rawDueDate: item.dueDate,
    description: item.description,
    commentsCount: item.commentCount ?? 0,
    comments: [],
  };
}
