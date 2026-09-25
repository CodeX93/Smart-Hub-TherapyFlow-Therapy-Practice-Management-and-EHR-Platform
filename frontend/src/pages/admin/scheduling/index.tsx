import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Button } from "../../../components/ui/button";
import { Plus, Upload } from "lucide-react";
import AddSessionModal from "@/components/scheduling-sections/add-session-modal";
import type { Appointment } from "../../../types/scheduling";
import AddSessionSuccessModal from "@/components/scheduling-sections/AddSessionSuccessModal";
import AdminDayScheduling from "@/components/admin-scheduling-sections/AdminDayScheduling";
import AdminWeekScheduling from "@/components/admin-scheduling-sections/AdminWeekScheduling";
import AdminMonthScheduling from "@/components/admin-scheduling-sections/AdminMonthScheduling";
import AdminAllSessions from "@/components/admin-scheduling-sections/AdminAllSessions";
import {
  type SchedulingFormValues,
  type SchedulingSuccessData,
} from "@/schemas/scheduling.schema";
import BulkUploadSessionsModal from "@/components/admin-scheduling-sections/BulkUploadSessionsModal";
import Toast from "@/components/shared/Toast";
import type { SchedulingFilters } from "@/components/scheduling-sections/SchedulingFilterDropdown";

const AdminScheduling = () => {
  const [searchParams] = useSearchParams();
  const initialTab = searchParams.get("tab");
  const [selectedType, setSelectedType] = useState(
    initialTab === "All" ||
      initialTab === "Day" ||
      initialTab === "Week" ||
      initialTab === "Month"
      ? initialTab
      : "Month",
  );
  const [refreshKey, setRefreshKey] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [isSuccessOpen, setIsSuccessOpen] = useState(false);
  const [isEditSchedule, setIsEditSchedule] = useState(false);
  const [editingSessionId, setEditingSessionId] = useState<number | null>(null);
  const [initialData, setInitialData] = useState<Partial<SchedulingFormValues>>(
    {},
  );
  const [submittedData, setSubmittedData] =
    useState<SchedulingSuccessData | null>(null);
  const [isBulkUploadOpen, setIsBulkUploadOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">(
    "info",
  );

  const dashboardFilters = useMemo((): Partial<SchedulingFilters> => {
    const status = searchParams.get("status");
    const endDateRaw = searchParams.get("endDate");
    const startDateRaw = searchParams.get("startDate");
    const endDate = endDateRaw ? new Date(`${endDateRaw}T23:59:59`) : null;
    const startDate = startDateRaw ? new Date(`${startDateRaw}T00:00:00`) : null;
    return {
      status: status || null,
      endDate: endDate && !Number.isNaN(endDate.getTime()) ? endDate : null,
      startDate: startDate && !Number.isNaN(startDate.getTime()) ? startDate : null,
    };
  }, [searchParams]);

  const handleTypesTabChange = (type: string) => {
    setSelectedType(type);
  };

  const handleSchedule = (data: SchedulingSuccessData) => {
    setSubmittedData(data);
    setIsOpen(false);
    setRefreshKey((previous) => previous + 1);
    setIsSuccessOpen(true);
  };

  const tabs = [
    { id: "Month", label: "Month" },
    { id: "Week", label: "Week" },
    { id: "Day", label: "Day" },
    { id: "All", label: "All Sessions" },
  ];

  const handleEditScheduleClick = (appointment: Appointment) => {
    setIsEditSchedule(true);
    setEditingSessionId(Number.parseInt(appointment.id, 10));
    setInitialData({});
    setIsOpen(true);
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

        <div className="flex items-center gap-3">
          <Button
            onClick={() => setIsBulkUploadOpen(true)}
            variant="outline"
            className="min-w-28 h-10 px-4 font-semibold w-fit cursor-pointer text-sm bg-white border border-(--neutral-200) hover:border-(--neutral-300) hover:bg-white text-(--text-primary-dark) rounded-full flex items-center gap-2 transition duration-300"
          >
            <Upload size={16} />
            Import
          </Button>
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
        </div>
      </div>

      <div
        className={
          selectedType === "Day"
            ? undefined
            : "flex min-h-0 flex-1 flex-col"
        }
      >
        {selectedType === "Day" && (
          <AdminDayScheduling
            key={`admin-day-${refreshKey}`}
            handleEditScheduleClick={handleEditScheduleClick}
          />
        )}
        {selectedType === "Week" && (
          <AdminWeekScheduling
            key={`admin-week-${refreshKey}`}
            handleEditScheduleClick={handleEditScheduleClick}
          />
        )}
        {selectedType === "Month" && (
          <AdminMonthScheduling
            key={`admin-month-${refreshKey}`}
            handleEditScheduleClick={handleEditScheduleClick}
          />
        )}
        {selectedType === "All" && (
          <AdminAllSessions
            key={`admin-all-${refreshKey}-${searchParams.toString()}`}
            handleEditScheduleClick={handleEditScheduleClick}
            initialFilters={dashboardFilters}
          />
        )}
      </div>

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

      <BulkUploadSessionsModal
        isOpen={isBulkUploadOpen}
        onClose={() => setIsBulkUploadOpen(false)}
        onSuccess={() => setRefreshKey((previous) => previous + 1)}
        onToast={(message, type) => {
          setToastType(type);
          setToastMessage(message);
        }}
      />
    </div>
  );
};

export default AdminScheduling;
