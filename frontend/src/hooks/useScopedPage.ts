import { useCallback, useState, type SetStateAction } from "react";

/** Reset before querying a different client/filter, so no request uses its old page. */
export function useScopedPage(scopeKey: string, initialPage = 1) {
  const [pagination, setPagination] = useState({ scopeKey, page: initialPage });
  const page = pagination.scopeKey === scopeKey ? pagination.page : initialPage;
  if (pagination.scopeKey !== scopeKey) setPagination({ scopeKey, page: initialPage });

  const setPage = useCallback((next: SetStateAction<number>) => {
    setPagination((previous) => {
      const currentPage = previous.scopeKey === scopeKey ? previous.page : initialPage;
      return { scopeKey, page: typeof next === "function" ? next(currentPage) : next };
    });
  }, [scopeKey, initialPage]);

  return [page, setPage] as const;
}
