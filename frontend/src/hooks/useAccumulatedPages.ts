import { useCallback, useState } from "react";

/** Retain loaded pages in React state. Items must belong to the current query
 * arguments (RTK Query's currentData) and keep their identity between responses.
 */
export function useAccumulatedPages<T>(
  items: T[] | undefined,
  page: number,
  scopeKey = "",
) {
  const [cache, setCache] = useState(() => ({
    scopeKey,
    page,
    items,
    pages: (items === undefined ? {} : { [page]: items }) as Record<number, T[]>,
  }));

  let current = cache;
  if (cache.scopeKey !== scopeKey || cache.page !== page || cache.items !== items) {
    const pages = cache.scopeKey !== scopeKey
      ? {}
      : Object.fromEntries(Object.entries(cache.pages).filter(([key]) => Number(key) <= page));
    current = { scopeKey, page, items, pages: items === undefined ? pages : { ...pages, [page]: items } };
    // Adjust only this component's state when query inputs change. React retries
    // its render before committing children; abandoned renders cannot mutate it.
    setCache(current);
  }

  const clearPages = useCallback(() => {
    setCache((previous) => ({
      ...previous,
      // Keep the current response usable even if a refetch is structurally equal.
      pages: previous.items === undefined ? {} : { [previous.page]: previous.items },
    }));
  }, []);

  const updatePages = useCallback((update: (items: T[]) => T[]) => {
    setCache(previous => ({ ...previous, pages: Object.fromEntries(Object.entries(previous.pages).map(([key, value]) => [key, update(value)])) }));
  }, []);

  return {
    updatePages,
    pages: current.pages,
    clearPages,
    isReadyToLoadMore: items !== undefined && Object.hasOwn(current.pages, page),
  };
}
