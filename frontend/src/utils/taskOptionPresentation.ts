function normalizeKey(value: string): string {
  return value.trim().toLowerCase().replace(/[\s-]+/g, "_");
}

export function isTaskStatusCompleted(status?: string | null): boolean {
  const key = normalizeKey(String(status ?? ""));
  return key === "completed" || key === "complete" || key === "done";
}

export function getTaskPriorityBadgeClass(priorityKey: string): string {
  switch (normalizeKey(priorityKey)) {
    case "urgent":
      return "border-transparent bg-[#E52427] text-white hover:bg-[#E52427]";
    case "high":
      return "border-transparent bg-[#FEE2E2] text-[#B91C1C] hover:bg-[#FEE2E2]";
    case "medium":
      return "border-transparent bg-[#FFF7C5] text-[#BB5D02] hover:bg-[#FFF7C5]";
    case "low":
      return "border-transparent bg-[#EBEFFF] text-[#5878F8] hover:bg-[#EBEFFF]";
    default:
      return "bg-gray-100 text-gray-700 hover:bg-gray-100 border-transparent";
  }
}

export function getTaskStatusBadgeClass(statusKey: string): string {
  switch (normalizeKey(statusKey)) {
    case "completed":
      return "border-transparent bg-[#D0FBE3] text-[#007C54] hover:bg-[#D0FBE3]";
    case "in_progress":
      return "border-transparent bg-[#EBEFFF] text-[#5878F8] hover:bg-[#EBEFFF]";
    case "overdue":
      return "border-transparent bg-[#FEE2E2] text-[#B91C1C] hover:bg-[#FEE2E2]";
    case "needs_attention":
      return "bg-orange-50 text-orange-600 hover:bg-orange-50 border-transparent";
    case "pending":
      return "border-transparent bg-[#FFF7C5] text-[#BB5D02] hover:bg-[#FFF7C5]";
    default:
      return "border-transparent bg-gray-100 text-gray-600 hover:bg-gray-100";
  }
}
