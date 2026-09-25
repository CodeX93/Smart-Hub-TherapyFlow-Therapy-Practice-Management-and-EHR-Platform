import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";

import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Search } from "lucide-react";
import { Checkbox } from "@/components/ui/checkbox";
import CustomInput from "@/components/form/CustomInput";
import { cn } from "@/lib/utils";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  useGetLibraryEntriesQuery,
  useLazySearchLibraryEntriesQuery,
} from "@/store/api/admin/libraryEntries.api";
import { getApiErrorMessage } from "@/utils/apiError";

interface SmartConnectSectionProps {
  title: string;
  selectedIds: string[];
  onChange: (ids: string[]) => void;
  categories: { id: string; name: string }[];
  onError?: (message: string) => void;
}

const PAGE_SIZE = 20;

const SmartConnectSection = ({
  title,
  selectedIds,
  onChange,
  categories,
  onError,
}: SmartConnectSectionProps) => {
  const [activeTab, setActiveTab] = useState("All");
  const [smartConnectSearch, setSmartConnectSearch] = useState("");
  const listScope = JSON.stringify([title, activeTab, smartConnectSearch]);
  const [page, setPage] = useScopedPage(listScope);


  const selectedCategory = useMemo(
    () => categories.find((category) => category.name === activeTab) ?? null,
    [activeTab, categories],
  );

  const {
    currentData: entriesResponse,
    isFetching: isFetchingEntries,
    isError: isEntriesError,
    error: entriesError,
  } = useGetLibraryEntriesQuery(
    title
      ? {
          page,
          limit: PAGE_SIZE,
          categoryId: selectedCategory ? Number(selectedCategory.id) : undefined,
        }
      : undefined,
    {
      skip: !title.trim() || smartConnectSearch.trim().length > 0,
      refetchOnMountOrArgChange: true,
    },
  );

  const [triggerSearch, { isFetching: isSearching }] = useLazySearchLibraryEntriesQuery();

  useEffect(() => {
    if (isEntriesError && entriesError) {
      onError?.(getApiErrorMessage(entriesError));
    }
  }, [isEntriesError, entriesError, onError]);



  const mappedEntries = useMemo(() => entriesResponse?.map(entry => ({
    id: String(entry.id), title: entry.title, category: entry.categoryName || "Other",
    categoryId: entry.categoryId, description: entry.content || "No description available",
  })), [entriesResponse]);
  const { items: pagedEntries } = usePagedItems(mappedEntries, page, listScope);
  const [searchResult, setSearchResult] = useState<{ scope: string; items: typeof pagedEntries } | null>(null);
  const allEntries = smartConnectSearch.trim() ? (searchResult?.scope === listScope ? searchResult.items : []) : pagedEntries;

  useEffect(() => {
    if (!title.trim() || !smartConnectSearch.trim()) return;
    let cancelled = false;
    const timer = window.setTimeout(async () => {
      try {
        const response = await triggerSearch({
          q: smartConnectSearch.trim(),
          categoryId: selectedCategory ? Number(selectedCategory.id) : undefined,
        }).unwrap();
        const next = response.map((entry) => ({
          id: String(entry.id),
          title: entry.title,
          category: entry.categoryName || "Other",
          categoryId: entry.categoryId,
          description: entry.content || "No description available",
        }));
        if (!cancelled) setSearchResult({ scope: listScope, items: next });
      } catch (error) {
        if (!cancelled) onError?.(getApiErrorMessage(error));
      }
    }, 350);

    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [onError, selectedCategory, smartConnectSearch, title, triggerSearch, listScope]);

  const filteredConnections = allEntries;

  const isAllVisibleSelected =
    filteredConnections.length > 0 &&
    filteredConnections.every((entry) => selectedIds.includes(entry.id));

  const handleToggle = (id: string, checked: boolean) => {
    if (checked) {
      onChange([...selectedIds, id]);
    } else {
      onChange(selectedIds.filter((entryId) => entryId !== id));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    const visibleIds = filteredConnections.map((entry) => entry.id);
    if (checked) {
      const next = visibleIds.filter((id) => !selectedIds.includes(id));
      onChange([...selectedIds, ...next]);
      return;
    }
    onChange(selectedIds.filter((id) => !visibleIds.includes(id)));
  };

  const hasMore =
    smartConnectSearch.trim().length === 0 &&
    filteredConnections.length >= page * PAGE_SIZE &&
    title.trim().length > 0;
  const handleLoadMore = useCallback(() => {
    if (!hasMore || isFetchingEntries) return;
    setPage((prev) => prev + 1);
  }, [hasMore, isFetchingEntries, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore,
    isLoading: isFetchingEntries,
  });

  const tabs = ["All", ...categories.map((category) => category.name)];

  if (!title.trim()) {
    return (
      <div className="bg-(--bg-primary-50) rounded-lg p-5 flex flex-col gap-4 justify-center items-center">
        <img src="/assets/add-entry-frame.png" alt="frame" />
        <p className="text-(--text-neutral-600) text-sm">
          Enter a title to see Smart Connect suggestions
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <img src="/assets/smart-connect.png" alt="smart-connect" />
        Smart Connect
      </div>

      <div className="w-full bg-(--neutral-100) p-1 rounded-full flex items-center gap-1 overflow-x-auto no-scrollbar">
        {tabs.map((tab) => (
          <button
            type="button"
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={cn(
              "flex-1 py-1.5 px-3 min-w-fit text-xs font-medium rounded-full transition-all duration-300 text-center cursor-pointer hover:bg-(--neutral-200)",
              activeTab === tab
                ? "bg-white text-(--neutral-950) shadow-sm hover:bg-white"
                : "text-(--text-neutral-600) hover:text-(--neutral-950)",
            )}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className="my-1">
        <CustomInput
          placeholder="Search..."
          value={smartConnectSearch}
          onChange={(e) => setSmartConnectSearch(e.target.value)}
          icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
          className="rounded-full min-h-10 pb-0 pt-1.75 shadow"
        />
      </div>

      <div className="my-1 flex flex-wrap items-center justify-between gap-3 text-sm text-(--text-neutral-600)">
        <span className="font-bold text-(--neutral-950)">
          Other Entries ({filteredConnections.length})
        </span>
        <div className="flex flex-wrap items-center gap-2">
          <span>{selectedIds.length} Selected</span>
          <Checkbox
            id="select-all-visible"
            checked={isAllVisibleSelected}
            onCheckedChange={handleSelectAll}
          />
          <label
            htmlFor="select-all-visible"
            className="cursor-pointer text-(--neutral-950)"
          >
            Select All
          </label>
        </div>
      </div>

      <div className="space-y-3 mt-1 border border-(--neutral-100) rounded-xl p-4">
        {filteredConnections.length > 0 ? (
          filteredConnections.map((item) => (
            <div
              key={item.id}
              className="flex items-start gap-3 border-b border-(--neutral-100) last:border-none pb-3"
            >
              <Checkbox
                id={`smart-conn-${item.id}`}
                checked={selectedIds.includes(item.id)}
                onCheckedChange={(checked) => handleToggle(item.id, checked)}
                className="mt-1"
              />
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-3">
                  <label
                    htmlFor={`smart-conn-${item.id}`}
                    className="block min-w-0 flex-1 cursor-pointer text-sm font-semibold text-(--neutral-950) truncate"
                    title={item.title}
                  >
                    {item.title}
                  </label>
                  <span className="shrink-0 rounded-full bg-(--light-blue) px-2 py-0.5 text-[0.625rem] text-(--status-billed)">
                    {item.category}
                  </span>
                </div>
                <p
                  className="mt-0.5 truncate text-xs text-(--text-neutral-600)"
                  title={item.description || "No description available"}
                >
                  {item.description || "No description available"}
                </p>
              </div>
            </div>
          ))
        ) : (
          <div className="text-center py-4 text-xs text-(--text-neutral-400)">
            No connections found.
          </div>
        )}

        <div ref={observerTarget} className="h-8 flex items-center justify-center">
          {(isFetchingEntries || isSearching) && (
            <ContentLoader variant="inline" size="sm" />
          )}
        </div>
      </div>
    </div>
  );
};

export default SmartConnectSection;
