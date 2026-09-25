
import { ContentLoader } from "@/components/shared/ContentLoader";
import type { ConsentRecord } from "@/pages/admin/compliance/compliance.static";
// import Pagination from "../shared/Pagination"; // Removed
import { useState, useCallback } from "react";
import { StatusBadge } from "@/utils/functions/adminConsentTable";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";

interface ConsentTableProps {
  data: ConsentRecord[];
}

const ConsentTable = ({ data }: ConsentTableProps) => {
  // const [currentPage, setCurrentPage] = useState(1);
  const [displayedItems, setDisplayedItems] = useState(20);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const itemsPerPage = 20;

  const handleLoadMore = useCallback(() => {
    setIsLoadingMore(true);
    setDisplayedItems((prev) => prev + itemsPerPage);
    setIsLoadingMore(false);
  }, []);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: displayedItems < data.length,
    isLoading: isLoadingMore,
  });

  const currentData = data.slice(0, displayedItems);

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-[0px_2px_2px_0px_var(--shadow)]">
        <div className="min-h-0 flex-1 overflow-auto overscroll-contain">
          <table className="w-full table-fixed">
            <thead className="sticky top-0 z-10 bg-(--bg-primary-50)">
              <tr className="h-11.5 border-b border-(--neutral-100)">
              <th className="text-left px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Client ID
              </th>
              <th className="text-left px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Full Name
              </th>
              <th className="text-left px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Email
              </th>
              <th className="text-center px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Portal Access
              </th>
              <th className="text-center px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                AI Processing
              </th>
              <th className="text-center px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Data Sharing
              </th>
              <th className="text-center px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Research
              </th>
              <th className="text-center px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Marketing
              </th>
            </tr>
          </thead>
          <tbody>
            {currentData.length > 0 ? (
              currentData.map((record) => (
                <tr
                  key={record.id}
                  className="h-18 border-t border-(--neutral-100) transition-colors hover:bg-(--neutral-50)/30"
                >
                  <td className="max-w-0 overflow-hidden px-4 py-3 text-sm text-(--text-neutral-600)">
                    <span className="block truncate" title={record.clientId}>
                      {record.clientId}
                    </span>
                  </td>
                  <td className="max-w-0 overflow-hidden px-4 py-3 text-sm text-(--text-primary-dark)">
                    <span className="block truncate" title={record.fullName}>
                      {record.fullName}
                    </span>
                  </td>
                  <td className="max-w-0 overflow-hidden px-4 py-3 text-sm text-(--text-neutral-600)">
                    <span className="block truncate" title={record.email || "----"}>
                      {record.email ? record.email : "----"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex justify-center">
                      <StatusBadge status={record.portalAccess} />
                    </div>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex justify-center">
                      <StatusBadge status={record.aiProcessing} />
                    </div>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex justify-center">
                      <StatusBadge status={record.dataSharing} />
                    </div>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex justify-center">
                      <StatusBadge status={record.research} />
                    </div>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex justify-center">
                      <StatusBadge status={record.marketing} />
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr className="border-t border-(--neutral-100)">
                <td
                  colSpan={8}
                  className="px-4 py-10 text-center text-sm text-(--text-neutral-400)"
                >
                  No data found.
                </td>
              </tr>
            )}
          </tbody>
          </table>

          <div
            ref={observerTarget}
            className="flex h-10 w-full items-center justify-center"
          >
            {isLoadingMore && (
              <ContentLoader variant="inline" size="md" />
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConsentTable;
