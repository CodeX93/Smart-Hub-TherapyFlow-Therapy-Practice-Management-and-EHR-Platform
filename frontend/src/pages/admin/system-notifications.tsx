import { useState } from "react";
import StaffNotificationManagement from "@/components/notification/StaffNotificationManagement";
import AdminSmsNotificationLogs from "@/components/notification/AdminSmsNotificationLogs";

type AdminNotificationsTab = "notifications" | "sms";

const tabs: Array<{ id: AdminNotificationsTab; label: string }> = [
  { id: "notifications", label: "Notifications" },
  { id: "sms", label: "SMS Notifications" },
];

const AdminSystemNotifications = () => {
  const [activeTab, setActiveTab] = useState<AdminNotificationsTab>("notifications");

  return (
    <div className="space-y-4">
      <div className="inline-flex items-center gap-2 rounded-full bg-(--neutral-100) p-1">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveTab(tab.id)}
            className={`rounded-full px-4 py-2 text-sm font-medium cursor-pointer transition-colors ${
              activeTab === tab.id
                ? "bg-white text-(--text-primary-dark)"
                : "text-(--text-neutral-600)"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "notifications" ? (
        <StaffNotificationManagement isTherapistView={false} />
      ) : (
        <AdminSmsNotificationLogs />
      )}
    </div>
  );
};

export default AdminSystemNotifications;
