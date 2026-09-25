import React, { useState } from "react";
import { Bell } from "lucide-react";
import { Popover, PopoverContent, PopoverTrigger } from "../ui/popover";
import { cn } from "../../lib/utils";
import NotificationSidePanel from "./NotificationSidePanel";
import type { Notification } from "../../types/notification";
import { Button } from "../ui/button";
import NotificationListing from "./NotificationListing";
import Toast from "@/components/shared/Toast";
import { useNavigate } from "react-router-dom";
import {
  useGetStaffNotificationsQuery,
  useGetStaffNotificationUnreadCountQuery,
  useMarkAllStaffNotificationsReadMutation,
  useReadStaffNotificationMutation,
} from "@/store/api/admin/notifications.api";
import {
  useGetPortalNotificationUnreadCountQuery,
  useGetPortalNotificationsQuery,
  useMarkAllPortalNotificationsReadMutation,
  useMarkPortalNotificationReadMutation,
  PORTAL_NOTIFICATIONS_DROPDOWN_PAGE_SIZE,
} from "@/store/api/portalNotificationsApi";
import { getApiErrorMessage } from "@/utils/apiError";
import type { StaffNotificationItem } from "@/store/api/admin/notifications.api";
import { mapPortalNotificationsToUi } from "@/utils/portalNotificationDisplay";
import { resolveNotificationActionPath } from "@/utils/notificationActionUrl";
import {
  formatNotificationMessage,
  formatNotificationTimestamp,
} from "@/utils/notificationDisplay";
import { useAppSelector } from "@/store/hooks";

interface NotificationDropdownProps {
  notifications?: Notification[];
  onMarkAsRead?: () => void;
}

const NotificationDropdown: React.FC<NotificationDropdownProps> = ({
  onMarkAsRead,
}) => {
  const formatTimestamp = formatNotificationTimestamp;
  const [isOpen, setIsOpen] = useState(false);
  const [isSidePanelOpen, setIsSidePanelOpen] = useState(false);
  const [markingReadNotificationId, setMarkingReadNotificationId] = useState<
    string | null
  >(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const navigate = useNavigate();
  const authRole = useAppSelector((state) => state.auth.role);
  const isAdmin = authRole === "admin";
  const isTherapist = authRole === "therapist";
  const isStaffRole = authRole === "staff";
  const isClientPortal = authRole === "user";
  const isStaffPortal = isAdmin || isTherapist || isStaffRole;

  const {
    data: staffNotifications = [],
    isLoading: isStaffNotificationsLoading,
    isFetching: isStaffNotificationsFetching,
    isError: isStaffNotificationsError,
    error: staffNotificationsError,
    refetch: refetchStaffNotifications,
  } = useGetStaffNotificationsQuery(undefined, {
    skip: !isStaffPortal || !isOpen,
    refetchOnMountOrArgChange: true,
  });
  const { data: staffUnreadCount = 0 } = useGetStaffNotificationUnreadCountQuery(
    undefined,
    { skip: !isStaffPortal, refetchOnMountOrArgChange: true },
  );
  const [readNotification] = useReadStaffNotificationMutation();
  const [markAllStaffRead, { isLoading: isMarkingAllStaffRead }] =
    useMarkAllStaffNotificationsReadMutation();

  const {
    data: portalNotificationsPage,
    isLoading: isPortalNotificationsLoading,
    isFetching: isPortalNotificationsFetching,
    isError: isPortalNotificationsError,
    error: portalNotificationsError,
    refetch: refetchPortalNotifications,
  } = useGetPortalNotificationsQuery(
    {
      page: 1,
      pageSize: PORTAL_NOTIFICATIONS_DROPDOWN_PAGE_SIZE,
    },
    {
      skip: !isClientPortal || !isOpen,
      refetchOnMountOrArgChange: true,
    },
  );
  const [markPortalNotificationRead] = useMarkPortalNotificationReadMutation();
  const [markAllPortalRead, { isLoading: isMarkingAllPortalRead }] =
    useMarkAllPortalNotificationsReadMutation();
  const { data: portalUnreadCount = 0 } = useGetPortalNotificationUnreadCountQuery(
    undefined,
    { skip: !isClientPortal, refetchOnMountOrArgChange: true },
  );

  const mappedStaffNotifications = staffNotifications.map(
    (row: StaffNotificationItem) => ({
      id: String(row.id),
      title: row.title || row.type || "Notification",
      description: formatNotificationMessage(row.message),
      timestamp: formatTimestamp(row.createdAt),
      isRead: Boolean(row.isRead),
    }),
  );

  const portalDropdownNotifications = portalNotificationsPage?.items ?? [];

  const mappedPortalNotifications = mapPortalNotificationsToUi(
    portalDropdownNotifications,
  );

  const displayNotifications = isStaffPortal
    ? mappedStaffNotifications
    : isClientPortal
      ? mappedPortalNotifications
      : [];

  const isNotificationsLoading = isStaffPortal
    ? (isStaffNotificationsLoading || isStaffNotificationsFetching) &&
      staffNotifications.length === 0
    : (isPortalNotificationsLoading || isPortalNotificationsFetching) &&
      portalDropdownNotifications.length === 0;
  const isNotificationsError = isStaffPortal
    ? isStaffNotificationsError
    : isPortalNotificationsError;
  const notificationsError = isStaffPortal
    ? staffNotificationsError
    : portalNotificationsError;

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (isClientPortal && isNotificationsError && notificationsError !== reportedError) {
    setReportedError(notificationsError);
    setToastMessage(getApiErrorMessage(notificationsError));
  }

  const showUnreadBadge = isStaffPortal
    ? staffUnreadCount > 0
    : portalUnreadCount > 0;

  const isMarkingAllRead = isStaffPortal
    ? isMarkingAllStaffRead
    : isMarkingAllPortalRead;

  const isMarkAllReadDisabled =
    isMarkingAllRead ||
    (isClientPortal && portalUnreadCount === 0) ||
    (isStaffPortal && staffUnreadCount === 0);

  const refreshNotifications = () => {
    if (isStaffPortal) {
      void refetchStaffNotifications();
      return;
    }
    if (isClientPortal) {
      void refetchPortalNotifications();
    }
  };

  const handleOpenChange = (open: boolean) => {
    setIsOpen(open);
  };

  const handleMarkAllAsRead = () => {
    if (isMarkAllReadDisabled) return;

    if (isStaffPortal) {
      void markAllStaffRead()
        .unwrap()
        .then(() => {
          refreshNotifications();
          onMarkAsRead?.();
        })
        .catch(() => {
          // Keep dropdown lightweight; full error handling lives on the notifications screen.
        });
      return;
    }

    if (isClientPortal) {
      void markAllPortalRead()
        .unwrap()
        .then(() => {
          refreshNotifications();
          onMarkAsRead?.();
        })
        .catch((markAllError) => {
          setToastMessage(getApiErrorMessage(markAllError));
        });
    }
  };

  const handleMarkRead = (id: string) => {
    if (isStaffPortal) {
      setMarkingReadNotificationId(id);
      void readNotification(Number(id))
        .unwrap()
        .then(() => {
          refreshNotifications();
          onMarkAsRead?.();
        })
        .catch(() => {
          // Keep dropdown lightweight; full error handling lives on the notifications screen.
        })
        .finally(() => {
          setMarkingReadNotificationId((current) =>
            current === id ? null : current,
          );
        });
      return;
    }

    if (isClientPortal) {
      setMarkingReadNotificationId(id);
      void markPortalNotificationRead(Number(id))
        .unwrap()
        .then(() => {
          refreshNotifications();
          onMarkAsRead?.();
        })
        .catch((markReadError) => {
          setToastMessage(getApiErrorMessage(markReadError));
        })
        .finally(() => {
          setMarkingReadNotificationId((current) =>
            current === id ? null : current,
          );
        });
    }
  };

  const handleNotificationAction = (notification: Notification) => {
    if (!notification.actionUrl) return;

    const path = resolveNotificationActionPath(
      notification.actionUrl,
      window.location.origin,
    );
    if (!path) {
      setToastMessage("This notification's link can't be opened.");
      return;
    }

    navigate(path);
    setIsOpen(false);
    setIsSidePanelOpen(false);
  };

  return (
    <>
      <Popover open={isOpen} onOpenChange={handleOpenChange}>
        <PopoverTrigger asChild>
          <button className="relative rounded-full p-2 transition-colors outline-none hover:bg-(--neutral-100) cursor-pointer">
            <Bell size={22} className="text-(--text-neutral-600)" />
            {showUnreadBadge ? (
              <span className="absolute top-1.5 right-2.5 h-2 w-2 rounded-full bg-(--status-denied)" />
            ) : null}
          </button>
        </PopoverTrigger>
        <PopoverContent
          align="end"
          sideOffset={8}
          collisionPadding={16}
          className={cn(
            "md:w-114.25 w-[calc(100vw-2rem)] max-w-114.25 max-h-[min(80vh,42rem)] p-0 rounded-xl shadow-lg border border-(--neutral-100) bg-white flex flex-col overflow-hidden",
            "data-[state=open]:animate-in data-[state=closed]:animate-out",
            "data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0",
            "data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95",
            "data-[side=bottom]:slide-in-from-top-2",
          )}
        >
          <div className="flex items-center justify-between border-b border-(--neutral-100) px-3 py-4 md:px-5">
            <h2 className="text-lg font-semibold text-(--text-primary-dark)">
              Notifications
            </h2>

            <div className="flex items-center justify-center">
              <Button
                onClick={handleMarkAllAsRead}
                disabled={isMarkAllReadDisabled}
                loading={isMarkingAllRead}
                loadingLabel="Marking..."
                className="flex w-fit cursor-pointer items-center gap-2 bg-transparent px-0 py-2 text-center text-sm font-medium text-(--text-primary-500) transition-all hover:bg-transparent hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-60"
              >
                Mark all read
              </Button>
            </div>
          </div>

          <div className="relative min-h-0 flex-1 overflow-y-auto">
            {isNotificationsLoading ? (
              <div className="px-5 py-6 text-sm text-[#667483]">
                Loading notifications...
              </div>
            ) : displayNotifications.length > 0 ? (
              (isClientPortal
                ? mappedPortalNotifications
                : displayNotifications.slice(0, 5)
              ).map((notification: Notification) => (
                <NotificationListing
                  key={notification.id}
                  notification={notification}
                  isMarkingRead={markingReadNotificationId === notification.id}
                  onMarkRead={handleMarkRead}
                  onAction={isClientPortal ? undefined : handleNotificationAction}
                />
              ))
            ) : (
              <div className="px-5 py-6 text-sm text-(--text-neutral-600)">
                No notifications found.
              </div>
            )}
          </div>

          <div className="border-t border-(--neutral-100) px-3 py-2">
            <Button
              onClick={() => {
                setIsOpen(false);
                if (isAdmin) {
                  navigate("/admin/system/notifications");
                  return;
                }
                if (isStaffRole) {
                  navigate("/staff/system/notifications");
                  return;
                }
                if (isTherapist) {
                  navigate("/therapist/system/notifications");
                  return;
                }
                setIsSidePanelOpen(true);
              }}
              className="flex min-h-10 w-full cursor-pointer items-center justify-center whitespace-nowrap rounded-lg bg-transparent py-2 text-center text-sm font-medium text-(--text-primary-500) transition-all hover:bg-transparent hover:opacity-80"
            >
              View all
            </Button>
          </div>
        </PopoverContent>
      </Popover>

      <NotificationSidePanel
        isOpen={isSidePanelOpen}
        onClose={() => setIsSidePanelOpen(false)}
        notifications={displayNotifications}
        onMarkAllRead={handleMarkAllAsRead}
        isAdmin={isAdmin}
        isClientPortal={isClientPortal}
        onNotificationAction={
          isClientPortal ? undefined : handleNotificationAction
        }
      />
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type="error"
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </>
  );
};

export default NotificationDropdown;
