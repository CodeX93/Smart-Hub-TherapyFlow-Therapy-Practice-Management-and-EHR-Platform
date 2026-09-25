import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";

import { ContentLoader } from "@/components/shared/ContentLoader";
import { Search } from "lucide-react";
import NotificationListing from "./NotificationListing";
import { useCallback, useMemo, useRef, useState } from "react";
import type { Notification } from "../../types/notification";
import CustomSelect from "../form/CustomSelect";
import { Button } from "../ui/button";
import CustomInput from "../form/CustomInput";
import Toast from "@/components/shared/Toast";
import {
  useGetSuperAdminNotificationHistoryQuery,
  useReadSuperAdminNotificationMutation,
  useReadAllSuperAdminNotificationsMutation
} from "@/store/api/superAdminApi";
import {
  useGetPortalNotificationUnreadCountQuery,
  useGetPortalNotificationsQuery,
  useMarkAllPortalNotificationsReadMutation,
  useMarkPortalNotificationReadMutation,
  PORTAL_NOTIFICATIONS_PAGE_SIZE
} from "@/store/api/portalNotificationsApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { mapPortalNotificationsToUi } from "@/utils/portalNotificationDisplay";
import {
  formatNotificationMessage,
  formatNotificationTimestamp,
} from "@/utils/notificationDisplay";

type FilterType = "All" | "Pending" | "Sent" | "Failed";

interface NotificationTabProps {
  notifications: Notification[];
  onMarkAllRead?: () => void;
  useApi?: boolean;
  usePortalApi?: boolean;
  onNotificationAction?: (notification: Notification) => void;
}

const NotificationTab = ({
  notifications,
  onMarkAllRead,
  useApi = false,
  usePortalApi = false,
  onNotificationAction,
}: NotificationTabProps) => {
  const listScrollRef = useRef<HTMLDivElement>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<FilterType>("All");
  const [page, setPage] = useScopedPage(String(useApi), 0);
  const pageSize = 25;

  const [portalPage, setPortalPage] = useScopedPage(String(usePortalApi));


  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const { currentData: data, isLoading, isFetching, isError, error } =
    useGetSuperAdminNotificationHistoryQuery(
      useApi
        ? {
            page,
            size: pageSize,
          }
        : undefined,
      {
        skip: !useApi,
        refetchOnMountOrArgChange: true,
      },
    );

  const [readNotification] = useReadSuperAdminNotificationMutation();
  const [readAllNotifications, { isLoading: isMarkingAllAdminRead }] =
    useReadAllSuperAdminNotificationsMutation();

  const portalListQueryArgs = useMemo(
    () => ({
      page: portalPage,
      pageSize: PORTAL_NOTIFICATIONS_PAGE_SIZE,
    }),
    [portalPage],
  );

  const {
    currentData: portalNotificationsPage,
    isLoading: isPortalLoading,
    isFetching: isPortalFetching,
    isError: isPortalError,
    error: portalError,
  } = useGetPortalNotificationsQuery(portalListQueryArgs, {
    skip: !usePortalApi,
    refetchOnMountOrArgChange: true,
  });
  const { data: portalUnreadCount = 0 } = useGetPortalNotificationUnreadCountQuery(
    undefined,
    { skip: !usePortalApi },
  );
  const [markPortalNotificationRead] = useMarkPortalNotificationReadMutation();
  const [markAllPortalNotificationsRead, { isLoading: isMarkingAllPortalRead }] =
    useMarkAllPortalNotificationsReadMutation();



  const { items: portalRows, isReadyToLoadMore: portalReady } = usePagedItems(portalNotificationsPage?.items, portalPage, String(usePortalApi));
  const { items: adminRows, isReadyToLoadMore: adminReady } = usePagedItems(data, page, String(useApi));
  const totalPortalPages = portalNotificationsPage?.totalPages ?? 1;
  const [confirmedReadIds, setConfirmedReadIds] = useState<Set<string>>(() => new Set());
  const readScope = `${useApi}:${usePortalApi}`;
  const [previousReadScope, setPreviousReadScope] = useState(readScope);
  if (previousReadScope !== readScope) {
    setPreviousReadScope(readScope);
    setConfirmedReadIds(new Set());
  }
  const allRows = useMemo(() => adminRows.map(row => confirmedReadIds.has(String(row.id)) ? { ...row, isRead: true } : row), [adminRows, confirmedReadIds]);
  const accumulatedPortalNotifications = useMemo(() => portalRows.map(row => confirmedReadIds.has(String(row.id)) ? { ...row, isRead: true } : row), [portalRows, confirmedReadIds]);

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (usePortalApi && isPortalError && portalError !== reportedError) {
    setReportedError(portalError);
    setToastMessage(getApiErrorMessage(portalError));
  }

  const handleMarkRead = useCallback(async (id: string) => {
    if (!useApi && !usePortalApi) return;
    try {
      if (usePortalApi) await markPortalNotificationRead(Number(id)).unwrap();
      else await readNotification(Number(id)).unwrap();
      setConfirmedReadIds(previous => new Set([...previous, id]));
    } catch (readError) {
      setToastMessage(getApiErrorMessage(readError));
    }
  }, [useApi, usePortalApi, markPortalNotificationRead, readNotification]);

  const handleMarkAllRead = useCallback(async () => {
    if (isMarkingAllPortalRead || isMarkingAllAdminRead) return;
    try {
      if (usePortalApi) {
        if (portalUnreadCount === 0) return;
        await markAllPortalNotificationsRead().unwrap();
      } else if (useApi) {
        await readAllNotifications().unwrap();
      }
      const ids = (usePortalApi ? portalRows : adminRows).map(row => String(row.id));
      setConfirmedReadIds(previous => new Set([...previous, ...ids]));
      onMarkAllRead?.();
    } catch (readError) {
      setToastMessage(getApiErrorMessage(readError));
    }
  }, [useApi, usePortalApi, isMarkingAllPortalRead, isMarkingAllAdminRead, portalUnreadCount, markAllPortalNotificationsRead, readAllNotifications, portalRows, adminRows, onMarkAllRead]);

  const rows = data ?? [];
  const hasMoreAdmin = useApi && adminReady ? rows.length === pageSize : false;
  const hasMorePortal = usePortalApi && portalReady ? portalPage < totalPortalPages : false;
  const hasMore = usePortalApi ? hasMorePortal : hasMoreAdmin;





  const onLoadMore = useCallback(() => {
    if (usePortalApi) {
      if (!hasMorePortal || isPortalFetching) return;
      setPortalPage((currentPage) => currentPage + 1);
      return;
    }

    if (!useApi || !hasMoreAdmin || isFetching) return;
    setPage((previous) => previous + 1);
  }, [hasMoreAdmin, hasMorePortal, isFetching, isPortalFetching, useApi, usePortalApi, setPage, setPortalPage]);

  const isLoadingMore = usePortalApi
    ? isPortalFetching && portalPage > 1 && accumulatedPortalNotifications.length > 0
    : useApi && isFetching && page > 0 && allRows.length > 0;

  const { observerTarget } = useInfiniteScroll({
    onLoadMore,
    hasMore,
    isLoading: isLoadingMore,
    scrollRootRef: listScrollRef,
  });

  const mappedApiNotifications = useMemo<Notification[]>(() => {
    if (!useApi) return [];
    return allRows.map((item) => {
      const status = item.status.trim().toUpperCase();
      const isRead =
        item.isRead ??
        (status === "SENT" || status === "DELIVERED" || status === "SUCCESS");
      const timestampSource = item.sentAt || item.scheduledAt || item.createdAt;
      const timestamp = formatNotificationTimestamp(timestampSource);
      const title = item.title || item.jobType || "Notification";
      const description = formatNotificationMessage(
        item.message || item.errorMessage || item.targetJson,
      );
      return {
        id: String(item.id),
        title,
        description,
        timestamp,
        isRead,
      };
    });
  }, [allRows, useApi]);

  const mappedPortalNotifications = useMemo<Notification[]>(() => {
    if (!usePortalApi) return [];
    return mapPortalNotificationsToUi(accumulatedPortalNotifications);
  }, [accumulatedPortalNotifications, usePortalApi]);

  const sourceNotifications = useApi
    ? mappedApiNotifications
    : usePortalApi
      ? mappedPortalNotifications
      : notifications;

  const filteredNotifications = sourceNotifications.filter((notification) => {
    const matchesSearch =
      notification.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      notification.description
        .toLowerCase()
        .includes(searchQuery.toLowerCase());

    let matchesFilter = true;
    if (filter === "Pending") matchesFilter = !notification.isRead;
    if (filter === "Sent") matchesFilter = notification.isRead;
    if (filter === "Failed") {
      const plainText =
        `${notification.title} ${notification.description}`.toLowerCase();
      matchesFilter =
        plainText.includes("failed") ||
        plainText.includes("error") ||
        plainText.includes("unsent");
    }

    return matchesSearch && matchesFilter;
  });

  const isMarkingAllRead = usePortalApi
    ? isMarkingAllPortalRead
    : useApi
      ? isMarkingAllAdminRead
      : false;

  const isMarkAllReadDisabled =
    isMarkingAllRead || (usePortalApi && portalUnreadCount === 0);

  return (
    <div className="flex flex-col gap-5 overflow-auto">
      <div className="flex items-center justify-between gap-2 px-6">
        <div className="flex items-center gap-2">
          <CustomInput
            icon={<Search size={18} className="text-(--text-neutral-600)" />}
            type="text"
            placeholder="Search notifications..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="min-h-10 max-w-70 rounded-full pt-1.75 pb-0"
          />

          <CustomSelect
            options={[
              { label: "All", value: "All" },
              { label: "Pending", value: "Pending" },
              { label: "Sent", value: "Sent" },
              { label: "Failed", value: "Failed" },
            ]}
            value={filter}
            onChange={(value) => setFilter(value as FilterType)}
            className="max-h-10 w-33 rounded-full pb-0 pt-0"
            isSearch={false}
          />
        </div>
        <Button
          onClick={handleMarkAllRead}
          disabled={isMarkAllReadDisabled}
          loading={isMarkingAllRead}
          loadingLabel="Marking..."
          className="flex h-auto w-fit cursor-pointer items-center gap-2 bg-transparent p-0 text-sm font-medium text-(--text-primary-500) transition-colors hover:bg-transparent hover:text-(--text-primary-500)/80 disabled:cursor-not-allowed disabled:opacity-60"
        >
          Mark all as read
        </Button>
      </div>
      <div ref={listScrollRef} className="flex-1 overflow-y-auto">
        {useApi && isLoading && allRows.length === 0 ? (
          <ContentLoader />
        ) : usePortalApi &&
          (isPortalLoading || isPortalFetching) &&
          accumulatedPortalNotifications.length === 0 ? (
          <ContentLoader />
        ) : useApi && isError ? (
          <div className="px-6 py-4 text-sm text-(--status-denied)">
            {getApiErrorMessage(error)}
          </div>
        ) : filteredNotifications.length > 0 ? (
          filteredNotifications.map((notification) => (
            <NotificationListing
              key={notification.id}
              notification={notification}
              onMarkRead={useApi || usePortalApi ? handleMarkRead : undefined}
              onAction={usePortalApi ? undefined : onNotificationAction}
            />
          ))
        ) : (
          <div className="flex h-full flex-col items-center justify-center px-6 text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-(--neutral-100)">
              <Search className="size-8 text-(--text-neutral-600)" />
            </div>
            <p className="mb-1 text-lg font-medium text-(--text-title-dark)">
              No notifications found
            </p>
            <p className="text-sm text-(--text-neutral-600)">
              Try adjusting your filters or search query
            </p>
          </div>
        )}
        {(useApi || usePortalApi) && filteredNotifications.length > 0 ? (
          <div
            ref={observerTarget}
            className="flex h-10 w-full items-center justify-center"
          >
            {isLoadingMore ? (
              <ContentLoader variant="inline" size="sm" />
            ) : null}
          </div>
        ) : null}
      </div>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type="error"
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

export default NotificationTab;
