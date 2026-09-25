import { useCallback, useMemo } from "react";
import { useGetTasksQuery } from "@/store/api/admin/tasks.api";
import { mapApiTaskToUiTask } from "@/utils/tasks/mapApiTaskToUiTask";
import { useAccumulatedPages } from "./useAccumulatedPages";
import { useScopedPage } from "./useScopedPage";

export function useClientTaskPages(clientId: string, assignedToId?: number, skip = false) {
  const scopeKey = JSON.stringify({ clientId, assignedToId });
  const [page, setPage] = useScopedPage(scopeKey);
  const query = useGetTasksQuery({
    page, pageSize: 10, clientId: Number(clientId), assignedToId,
    sortBy: "createdAt", sortOrder: "desc",
  }, { skip: !clientId || skip, refetchOnMountOrArgChange: true });
  const items = useMemo(() => query.currentData?.items.map(mapApiTaskToUiTask), [query.currentData]);
  const { pages, clearPages, isReadyToLoadMore } = useAccumulatedPages(items, page, scopeKey);
  const tasks = useMemo(() => {
    const rows = new Map<string, ReturnType<typeof mapApiTaskToUiTask>>();
    for (let number = 1; number <= page; number += 1) {
      for (const task of pages[number] ?? []) rows.set(task.id, task);
    }
    return [...rows.values()];
  }, [pages, page]);
  const totalTasks = query.currentData?.totalCount ?? 0;
  const hasMore = isReadyToLoadMore && page * 10 < totalTasks;
  const handleLoadMore = useCallback(() => {
    if (hasMore && !query.isFetching) setPage((previous) => previous + 1);
  }, [hasMore, query.isFetching, setPage]);
  const { refetch } = query;
  const refreshTasks = async () => {
    clearPages();
    if (page === 1) await refetch().unwrap();
    else setPage(1);
  };
  return { ...query, tasks, hasMore, handleLoadMore, refreshTasks };
}
