import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  useGetAdminSessionsListQuery,
  useLazyGetAdminSessionsListQuery,
  type AdminDashboardSession,
  type AdminSessionsListParams,
} from "@/store/api/admin/dashboard.api";

/** Prefer backend max; hook still walks remaining pages if totalCount exceeds one page. */
export const CALENDAR_SESSIONS_PAGE_SIZE = 500;

type CalendarSessionsArgs = Omit<AdminSessionsListParams, "page">;

/**
 * Loads every session in a calendar range (month/week/day chips).
 * Page 1 comes from the cached query; extra pages are fetched when totalCount
 * exceeds the effective page size (production may clamp below the request).
 */
export function useCalendarSessions(
  baseArgs: CalendarSessionsArgs | undefined,
  options?: { skip?: boolean },
) {
  const skip = Boolean(options?.skip) || !baseArgs;
  const pageSize = Math.max(1, baseArgs?.pageSize ?? CALENDAR_SESSIONS_PAGE_SIZE);

  // Serialize so parent re-creating Date/arg objects with the same values
  // does not retrigger page walks every render.
  const scopeKey = baseArgs
    ? JSON.stringify({
        ...baseArgs,
        page: 1,
        pageSize,
        view: baseArgs.view ?? "calendar",
      })
    : "";

  const firstPageArgs = useMemo((): AdminSessionsListParams | undefined => {
    if (!scopeKey) return undefined;
    return JSON.parse(scopeKey) as AdminSessionsListParams;
  }, [scopeKey]);

  const {
    data: firstPage,
    isFetching: isFirstFetching,
    isLoading: isFirstLoading,
    refetch: refetchFirst,
  } = useGetAdminSessionsListQuery(firstPageArgs!, {
    skip: skip || !firstPageArgs,
  });

  const [trigger] = useLazyGetAdminSessionsListQuery();
  const [extraItems, setExtraItems] = useState<AdminDashboardSession[]>([]);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const fetchGen = useRef(0);

  const totalCount = firstPage?.totalCount ?? 0;
  const effectivePageSize = Math.max(1, firstPage?.pageSize ?? pageSize);
  const pagesNeeded = Math.max(
    1,
    firstPage?.totalPages || Math.ceil(totalCount / effectivePageSize) || 1,
  );

  useEffect(() => {
    setExtraItems([]);
  }, [scopeKey]);

  useEffect(() => {
    if (skip || !firstPageArgs || pagesNeeded <= 1 || totalCount <= 0) {
      setIsLoadingMore(false);
      return;
    }

    const gen = ++fetchGen.current;
    let cancelled = false;

    const loadRest = async () => {
      setIsLoadingMore(true);
      const collected: AdminDashboardSession[] = [];
      try {
        for (let page = 2; page <= pagesNeeded; page += 1) {
          if (cancelled || fetchGen.current !== gen) return;
          const result = await trigger({
            ...firstPageArgs,
            page,
            pageSize: effectivePageSize,
          }).unwrap();
          collected.push(...(result.items ?? []));
        }
        if (!cancelled && fetchGen.current === gen) {
          setExtraItems(collected);
        }
      } catch {
        if (!cancelled && fetchGen.current === gen) {
          setExtraItems([]);
        }
      } finally {
        if (!cancelled && fetchGen.current === gen) {
          setIsLoadingMore(false);
        }
      }
    };

    void loadRest();
    return () => {
      cancelled = true;
    };
  }, [
    skip,
    scopeKey,
    firstPageArgs,
    pagesNeeded,
    totalCount,
    effectivePageSize,
    trigger,
  ]);

  const items = useMemo(() => {
    const first = firstPage?.items ?? [];
    if (extraItems.length === 0) return first;
    const byId = new Map<number, AdminDashboardSession>();
    for (const session of first) byId.set(session.id, session);
    for (const session of extraItems) byId.set(session.id, session);
    return Array.from(byId.values());
  }, [firstPage?.items, extraItems]);

  const refetch = useCallback(async () => {
    setExtraItems([]);
    await refetchFirst();
  }, [refetchFirst]);

  return {
    items,
    totalCount,
    isFetching: isFirstFetching || isLoadingMore,
    isLoading: isFirstLoading && items.length === 0,
    refetch,
  };
}
