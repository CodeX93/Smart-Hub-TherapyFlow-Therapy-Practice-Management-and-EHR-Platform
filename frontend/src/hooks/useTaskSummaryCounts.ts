import { useGetTaskStatsQuery } from "@/store/api/admin/tasks.api";

interface UseTaskSummaryCountsOptions {
  assignedToId?: number;
  fromDate?: string;
  toDate?: string;
  dateField?: "dueDate" | "createdAt" | "updatedAt";
  skip?: boolean;
}

export function useTaskSummaryCounts({
  assignedToId,
  fromDate,
  toDate,
  dateField,
  skip = false,
}: UseTaskSummaryCountsOptions = {}) {
  const { data } = useGetTaskStatsQuery(
    {
      assignedToId,
      fromDate,
      toDate,
      dateField,
    },
    { skip },
  );

  return {
    total: data?.totalTasks ?? 0,
    pending: data?.pendingTasks ?? 0,
    inProgress: data?.inProgressTasks ?? 0,
    completed: data?.completedTasks ?? 0,
    needsAttention: data?.needsAttentionTasks ?? 0,
  };
}
