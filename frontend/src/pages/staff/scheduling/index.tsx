import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
import AddSessionModal from "@/components/scheduling-sections/add-session-modal";
import type { Appointment } from "@/types/scheduling";
import AddSessionSuccessModal from "@/components/scheduling-sections/AddSessionSuccessModal";
import AdminDayScheduling from "@/components/admin-scheduling-sections/AdminDayScheduling";
import AdminWeekScheduling from "@/components/admin-scheduling-sections/AdminWeekScheduling";
import AdminMonthScheduling from "@/components/admin-scheduling-sections/AdminMonthScheduling";
import AdminAllSessions from "@/components/admin-scheduling-sections/AdminAllSessions";
import {
  type SchedulingFormValues,
  type SchedulingSuccessData,
} from "@/schemas/scheduling.schema";
import Toast from "@/components/shared/Toast";
import { useStaffPortalRestrictions } from "@/hooks/useStaffPortalRestrictions";

const StaffScheduling = () => {
  const [selectedType, setSelectedType] = useState("Month");
  const [refreshKey, setRefreshKey] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [isSuccessOpen, setIsSuccessOpen] = useState(false);
  const [isEditSchedule, setIsEditSchedule] = useState(false);
  const [editingSessionId, setEditingSessionId] = useState<number | null>(null);
  const [initialData, setInitialData] = useState<Partial<SchedulingFormValues>>({});
  const [submittedData, setSubmittedData] =
    useState<SchedulingSuccessData | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const {
    supervisorStaffMode,
    hideSchedulingEdits,
    canCreateSessions,
    canManageSessions,
  } = useStaffPortalRestrictions();

  const handleTypesTabChange = (type: string) => {
    setSelectedType(type);
  };

  const handleSchedule = (data: SchedulingSuccessData) => {
    setSubmittedData(data);
    setIsOpen(false);
    setRefreshKey((previous) => previous + 1);
    setIsSuccessOpen(true);
  };

  const handleEditScheduleClick = (appointment: Appointment) => {
    if (hideSchedulingEdits) return;
    setIsEditSchedule(true);
    setEditingSessionId(Number.parseInt(appointment.id, 10));
    setInitialData({});
    setIsOpen(true);
  };

  const tabs = [
    { id: "Month", label: "Month" },
    { id: "Week", label: "Week" },
    { id: "Day", label: "Day" },
    { id: "All", label: "All Sessions" },
  ];

  const schedulingProps = {
    staffMode: supervisorStaffMode,
    hideEditActions: hideSchedulingEdits,
    handleEditScheduleClick: hideSchedulingEdits ? undefined : handleEditScheduleClick,
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-4">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="flex shrink-0 md:flex-row gap-4 flex-col-reverse md:items-center justify-between">
        <div className="bg-(--neutral-100) p-1 rounded-full flex items-center gap-2">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => handleTypesTabChange(tab.id)}
              aria-pressed={selectedType === tab.id}
              className={`rounded-full px-4 py-2 text-sm font-medium transition cursor-pointer duration-300 ${
                selectedType === tab.id
                  ? "bg-white text-(--text-primary-dark)"
                  : "text-(--text-neutral-600)"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {canCreateSessions ? (
          <Button
            onClick={() => {
              setIsEditSchedule(false);
              setEditingSessionId(null);
              setInitialData({});
              setIsOpen(true);
            }}
            className="min-w-36 h-10 px-4 font-semibold w-fit cursor-pointer text-sm bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full flex items-center gap-2 transition duration-300"
          >
            <Plus size={16} />
            New Session
          </Button>
        ) : null}
      </div>

      <div
        className={
          selectedType === "Day"
            ? undefined
            : "flex min-h-0 flex-1 flex-col"
        }
      >
        {selectedType === "Day" && (
          <AdminDayScheduling key={`staff-day-${refreshKey}`} {...schedulingProps} />
        )}
        {selectedType === "Week" && (
          <AdminWeekScheduling key={`staff-week-${refreshKey}`} {...schedulingProps} />
        )}
        {selectedType === "Month" && (
          <AdminMonthScheduling key={`staff-month-${refreshKey}`} {...schedulingProps} />
        )}
        {selectedType === "All" && (
          <AdminAllSessions key={`staff-all-${refreshKey}`} {...schedulingProps} />
        )}
      </div>

      {canManageSessions ? (
        <>
          <AddSessionModal
            isOpen={isOpen}
            onClose={() => setIsOpen(false)}
            initialData={initialData}
            onSchedule={handleSchedule}
            onToast={(message, type) => {
              setToastType(type);
              setToastMessage(message);
            }}
            isEditSchedule={isEditSchedule}
            isAdmin={true}
            sessionId={editingSessionId}
          />

          <AddSessionSuccessModal
            isOpen={isSuccessOpen}
            onClose={() => setIsSuccessOpen(false)}
            data={submittedData}
          />
        </>
      ) : null}
    </div>
  );
};

export default StaffScheduling;
