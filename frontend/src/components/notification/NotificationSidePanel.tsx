import React, { useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import type { Notification } from "../../types/notification";
import NotificationTab from "./NotificationTab";
import TemplateTab from "./TemplateTab";

interface NotificationSidePanelProps {
  isOpen: boolean;
  onClose: () => void;
  notifications: Notification[];
  onMarkAllRead: () => void;
  isAdmin: boolean;
  isClientPortal?: boolean;
  totalNotificationsCount?: number;
  onNotificationAction?: (notification: Notification) => void;
}

const tabs = [
  { id: "Notifications", label: "Notifications" },
  { id: "Templates", label: "Templates" },
];

const NotificationSidePanel: React.FC<NotificationSidePanelProps> = ({
  isOpen,
  onClose,
  notifications,
  onMarkAllRead,
  isAdmin,
  isClientPortal = false,
  totalNotificationsCount,
  onNotificationAction,
}) => {
  const [selectedTabType, setSelectedTabType] = useState("Notifications");

  const handleTabTypeChange = (type: string) => {
    setSelectedTabType(type);
  };

  if (!isOpen || typeof document === "undefined") return null;

  return createPortal(
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-[10030] bg-black/20 transition-opacity animate-in fade-in"
        onClick={onClose}
      />

      {/* Side Panel */}
      <div className="fixed top-0 right-0 z-[10035] flex h-full w-full flex-col bg-white shadow-2xl animate-in slide-in-from-right duration-300 md:w-170">
        {/* Header */}
        <div className="flex flex-col gap-4 px-6 py-5">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              {isAdmin ? "Notification Center" : "Notifications"}
            </h2>
            <button
              onClick={onClose}
              className="cursor-pointer rounded-full p-1 text-(--text-neutral-600) transition-colors duration-300 hover:bg-(--neutral-100)"
            >
              <X size={20} />
            </button>
          </div>

          {isAdmin && (
            <div className="flex items-center gap-2 rounded-full bg-(--neutral-100) p-1">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => handleTabTypeChange(tab.id)}
                  aria-pressed={selectedTabType === tab.id}
                  className={`w-full cursor-pointer rounded-full px-4 py-2 text-sm font-medium transition-all duration-300 ease-in-out ${
                    selectedTabType === tab.id
                      ? "bg-white text-(--text-primary-dark)"
                      : "text-(--text-neutral-600)"
                  }`}
                >
                  {tab.label}
                  {tab.id === "Notifications" &&
                    ` (${isAdmin ? (totalNotificationsCount ?? 0) : notifications.filter((n) => !n.isRead).length})`}
                </button>
              ))}
            </div>
          )}
        </div>

        {selectedTabType === "Notifications" && (
          <NotificationTab
            notifications={notifications}
            onMarkAllRead={onMarkAllRead}
            useApi={isAdmin}
            usePortalApi={isClientPortal}
            onNotificationAction={onNotificationAction}
          />
        )}
        {selectedTabType === "Templates" && <TemplateTab />}
      </div>
    </>,
    document.body,
  );
};

export default NotificationSidePanel;
