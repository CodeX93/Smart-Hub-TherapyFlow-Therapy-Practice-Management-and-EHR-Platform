
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useRef, useState } from "react";
import ConfirmationModal from "../../../components/appointment-sections/ConfirmationModal";
import SuccessModal from "../../../components/appointment-sections/SuccessModal";
import Calendar from "../../../components/appointment-sections/Calendar";
import TimeSlots from "../../../components/appointment-sections/TimeSlots";
import {
  formatDateLong,
  formatDateShort,
} from "../../../utils/transformer/dates.transformer";
import CustomSelect from "../../../components/form/CustomSelect";
import CustomInput from "../../../components/form/CustomInput";
import { Button } from "../../../components/ui/button";
import UpcomingList from "../../../components/appointment-sections/UpcomingList";
import ClientAppointmentsFilterPanel from "../../../components/appointment-sections/ClientAppointmentsFilterPanel";
import { NotepadText, Search, Video } from "lucide-react";
import {
  useBookPortalAppointmentMutation,
  useGetPortalAvailableSlotsQuery,
  useGetPortalMeQuery,
  useGetPortalSessionHistoryQuery,
  useGetPortalServicesQuery,
  useRequestOnlineBookingMutation,
  type PortalSessionHistoryItem,
} from "@/store/api/portalApi";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import {
  formatPortalSlotStartTime,
  getBookingSessionTypeLabel,
  getSessionModalityLabel,
  isPortalSlotInPast,
} from "@/utils/portalSessionDisplay";
import { getSessionStatusLabel } from "@/utils/sessionStatusPresentation";
import {
  applyClientAppointmentFilters,
  DEFAULT_CLIENT_APPOINTMENT_FILTERS,
  type ClientAppointmentFilters,
} from "@/utils/clientAppointmentFilters";
import { usePortalNotificationUnreadBootstrap } from "@/hooks/useNotificationUnreadBootstrap";

function mapSessionHistoryToListItem(appointment: PortalSessionHistoryItem) {
  const parsedSessionDate = new Date(appointment.sessionDate);
  const dateLabel = Number.isNaN(parsedSessionDate.getTime())
    ? "-"
    : formatDateLong(parsedSessionDate);
  const timeLabel = Number.isNaN(parsedSessionDate.getTime())
    ? "-"
    : parsedSessionDate.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
        hour12: true,
      });

  return {
    id: appointment.id,
    date: dateLabel,
    time: timeLabel,
    type: appointment.serviceName || appointment.sessionType || "Session",
    therapistName: appointment.therapistName || undefined,
    modality: getSessionModalityLabel(appointment.sessionMode),
    status: (appointment.status || "").trim().toLowerCase(),
    sessionMode: appointment.sessionMode || undefined,
    zoomEnabled: appointment.zoomEnabled,
    zoomJoinUrl: appointment.zoomJoinUrl || undefined,
    zoomPassword: appointment.zoomPassword || undefined,
  };
}

function AppointmentsLoader({ label }: { label: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-8">
      <ContentLoader variant="inline" size="md" />
      <span className="text-sm text-(--text-neutral-600)">{label}</span>
    </div>
  );
}

/** Matches ONLINE_BOOKING_REQUEST_COOLDOWN in ClientPortalService. */
const ONLINE_BOOKING_REQUEST_COOLDOWN_DAYS = 7;

function isOnlineBookingRequestPending(requestedAt: string | null): boolean {
  if (!requestedAt) return false;
  const requested = new Date(requestedAt);
  if (Number.isNaN(requested.getTime())) return false;
  const cooldownEnds =
    requested.getTime() + ONLINE_BOOKING_REQUEST_COOLDOWN_DAYS * 24 * 60 * 60 * 1000;
  return cooldownEnds > Date.now();
}

export default function Appointments() {
  usePortalNotificationUnreadBootstrap();
  const bookingScrollRef = useRef<HTMLDivElement>(null);
  const { data: meData } = useGetPortalMeQuery();
  const {
    data: servicesData = [],
    isLoading: isServicesLoading,
    isFetching: isServicesFetching,
    isError: isServicesError,
    error: servicesError,
  } = useGetPortalServicesQuery();
  const {
    data: upcomingSessions = [],
    isLoading: isUpcomingSessionsLoading,
    isFetching: isUpcomingSessionsFetching,
    isError: isUpcomingSessionsError,
    error: upcomingSessionsError,
    refetch: refetchUpcomingSessions,
  } = useGetPortalSessionHistoryQuery({ scope: "upcoming" });
  const {
    data: pastSessions = [],
    isLoading: isPastSessionsLoading,
    isFetching: isPastSessionsFetching,
    isError: isPastSessionsError,
    error: pastSessionsError,
    refetch: refetchPastSessions,
  } = useGetPortalSessionHistoryQuery({ scope: "past" });
  const [bookAppointment, { isLoading: isBooking }] = useBookPortalAppointmentMutation();
  const [requestOnlineBooking, { isLoading: isRequestingOnlineBooking }] =
    useRequestOnlineBookingMutation();
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date().getDate());
  const [requestedType, setSelectedType] = useState("in-person");
  const [selectedFilter, setSelectedFilter] = useState<"upcoming" | "previous">(
    "upcoming",
  );
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [selectedService, setSelectedService] = useState<string | undefined>(
    undefined,
  );
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("error");
  const [appointmentFilters, setAppointmentFilters] =
    useState<ClientAppointmentFilters>(DEFAULT_CLIENT_APPOINTMENT_FILTERS);

  const fullDate = new Date(
    currentMonth.getFullYear(),
    currentMonth.getMonth(),
    selectedDate,
  );

  const selectedDateIso = `${fullDate.getFullYear()}-${String(
    fullDate.getMonth() + 1,
  ).padStart(2, "0")}-${String(fullDate.getDate()).padStart(2, "0")}`;

  // The therapist needs a connected Zoom account before online sessions can be booked at all:
  // /available-slots and /book-appointment both reject them server-side. Mirror that here so the
  // client is told up front instead of after picking a service and a date.
  const onlineBookingAvailable = meData?.onlineBookingAvailable ?? true;
  const onlineBookingRequestSent = isOnlineBookingRequestPending(
    meData?.onlineBookingRequestedAt ?? null,
  );
  // Derived rather than stored, so a therapist disconnecting Zoom while this page is open
  // falls back to in-person on the next render instead of leaving a dead selection behind.
  const selectedType =
    !onlineBookingAvailable && requestedType === "virtual" ? "in-person" : requestedType;

  const selectedSessionType = selectedType === "in-person" ? "in-person" : "online";

  const availableSlotsQueryArgs = useMemo(
    (): {
      startDate: string;
      endDate: string;
      sessionType: "online" | "in-person";
      serviceId: number;
    } | null => {
      if (!selectedService) return null;
      const serviceId = Number.parseInt(selectedService, 10);
      if (!Number.isFinite(serviceId)) return null;
      return {
        startDate: selectedDateIso,
        endDate: selectedDateIso,
        sessionType: selectedSessionType,
        serviceId,
      };
    },
    [selectedDateIso, selectedSessionType, selectedService],
  );

  const availableSlotsQueryKey = availableSlotsQueryArgs
    ? `${availableSlotsQueryArgs.startDate}|${availableSlotsQueryArgs.sessionType}|${availableSlotsQueryArgs.serviceId}`
    : "no-service";

  const {
    currentData: availableSlotsData,
    isLoading: isSlotsLoading,
    isFetching: isSlotsFetching,
    isSuccess: isSlotsSuccess,
    isError: isSlotsError,
    error: slotsError,
    refetch: refetchAvailableSlots,
  } = useGetPortalAvailableSlotsQuery(availableSlotsQueryArgs as {
    startDate: string;
    endDate: string;
    sessionType: "online" | "in-person";
    serviceId: number;
  }, {
    skip: !availableSlotsQueryArgs,
    refetchOnMountOrArgChange: true,
  });





  const serviceTypes = servicesData.map((service) => ({
    value: String(service.id),
    label: service.serviceName,
    duration: `${service.duration} Min`,
    price: `$${service.baseRate.toFixed(2)}`,
  }));

  const [nowMs, setNowMs] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNowMs(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);
  const timeSlots = useMemo(() => {
    if (!availableSlotsQueryArgs || !availableSlotsData?.length) return [];


    return availableSlotsData
      .filter((slot) => !isPortalSlotInPast(slot.date, slot.start, nowMs, slot.startUtc))
      .map((slot) =>
        formatPortalSlotStartTime(slot.date, slot.start, slot.startUtc, slot.timezone),
      );
  }, [availableSlotsData, availableSlotsQueryArgs, nowMs]);

  // The API tells us which zone these times belong to; label them so a late-evening
  // slot is never mistaken for the clinic's local time.
  const slotTimezone = availableSlotsData?.[0]?.timezone ?? null;

  const [selectedTimeScope, setSelectedTimeScope] = useState(availableSlotsQueryKey);
  if (selectedTimeScope !== availableSlotsQueryKey) {
    setSelectedTimeScope(availableSlotsQueryKey);
    setSelectedTime(null);
  }

  if (selectedTime && availableSlotsData && !timeSlots.includes(selectedTime)) setSelectedTime(null);

  const sessionsHistoryData =
    selectedFilter === "upcoming" ? upcomingSessions : pastSessions;
  const isSessionsHistoryLoading =
    selectedFilter === "upcoming" ? isUpcomingSessionsLoading : isPastSessionsLoading;
  const isSessionsHistoryFetching =
    selectedFilter === "upcoming" ? isUpcomingSessionsFetching : isPastSessionsFetching;
  const showSessionsHistoryLoader =
    isSessionsHistoryLoading || isSessionsHistoryFetching;
  const showServicesLoader = isServicesLoading || isServicesFetching;
  const showSlotsLoader =
    Boolean(availableSlotsQueryArgs) &&
    (isSlotsLoading ||
      (isSlotsFetching && availableSlotsData === undefined));

  const queryError = isServicesError ? servicesError : isUpcomingSessionsError ? upcomingSessionsError : isPastSessionsError ? pastSessionsError : (isSlotsError && availableSlotsQueryArgs ? slotsError : null);
  const [reportedError, setReportedError] = useState<unknown>(null);
  if (queryError && queryError !== reportedError) {
    setReportedError(queryError);
    setToastType("error");
    setToastMessage(getApiErrorMessage(queryError));
  }

  const upcomingAppointments = upcomingSessions.map(mapSessionHistoryToListItem);
  const pastAppointments = pastSessions.map(mapSessionHistoryToListItem);


    const options = new Map<string, string>();

    for (const service of servicesData) {
      options.set(String(service.id), service.serviceName);
    }

    for (const session of [...upcomingSessions, ...pastSessions]) {
      const serviceId = String(session.serviceId);
      if (!options.has(serviceId)) {
        options.set(
          serviceId,
          session.serviceName || `Service ${session.serviceId}`,
        );
      }
    }

    const filterServiceOptions = [
      { value: "", label: "All Services" },
      ...Array.from(options.entries()).map(([value, label]) => ({
        value,
        label,
      })),
    ];

  const panelFilteredSessions = applyClientAppointmentFilters(sessionsHistoryData, appointmentFilters);

  const mappedAppointments = panelFilteredSessions.map(mapSessionHistoryToListItem);

  const filteredAppointments = mappedAppointments.filter((appointment) => {
    const needle = searchQuery.trim().toLowerCase();
    if (!needle) return true;

    const statusLabel = getSessionStatusLabel(appointment.status).toLowerCase();

    return (
      appointment.date.toLowerCase().includes(needle) ||
      appointment.time.toLowerCase().includes(needle) ||
      appointment.type.toLowerCase().includes(needle) ||
      appointment.modality.toLowerCase().includes(needle) ||
      appointment.status.toLowerCase().includes(needle) ||
      statusLabel.includes(needle) ||
      appointment.therapistName?.toLowerCase().includes(needle)
    );
  });

  const selectedServiceDetails = serviceTypes.find(
    (s) => s.value === selectedService,
  );

  const formattedDate = formatDateLong(fullDate);

  const shortFormattedDate = formatDateShort(fullDate);

  /* Date Validation Logic */
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const isPast = fullDate < today;
  const isDateDisabled = isPast; // Sync with Calendar disablePast prop

  /* Duplicate Check Logic */
  const isDuplicate = upcomingAppointments.some(
    (appt) => appt.date === formattedDate && appt.time === selectedTime,
  );

  const disabled =
    !selectedDate ||
    !selectedTime ||
    !selectedService ||
    isDateDisabled ||
    isDuplicate;

  const handleTypesTabChange = (type: string) => {
    if (type === "virtual" && !onlineBookingAvailable) return;
    setSelectedType(type);
  };

  const handleRequestOnlineBooking = async () => {
    if (isRequestingOnlineBooking) return;
    try {
      setToastMessage(null);
      const result = await requestOnlineBooking().unwrap();
      setToastType("success");
      setToastMessage(
        result.onlineBookingAvailable
          ? "Your therapist has set up video meetings. Virtual visits are available now."
          : "We let your therapist know. They'll be in touch once video meetings are set up.",
      );
    } catch (requestError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(requestError));
    }
  };

  const handleFilterTabChange = (filter: "upcoming" | "previous") => {
    setSelectedFilter(filter);
  };

  function buildSessionStartUtc(date: Date, time12h: string): string {
    const [time, meridiemRaw] = time12h.split(" ");
    const [hourRaw, minuteRaw] = time.split(":");
    let hours = Number.parseInt(hourRaw, 10);
    const minutes = Number.parseInt(minuteRaw, 10);
    const meridiem = meridiemRaw.toUpperCase();

    if (meridiem === "PM" && hours < 12) hours += 12;
    if (meridiem === "AM" && hours === 12) hours = 0;

    const localDate = new Date(date);
    localDate.setHours(hours, minutes, 0, 0);
    return localDate.toISOString();
  }

  const handleConfirmBooking = async () => {
    if (!selectedTime || !selectedServiceDetails || isBooking) return;
    
    // Find the original slot to get its true UTC start time
    const originalSlot = availableSlotsData?.find(
      (s) =>
        formatPortalSlotStartTime(s.date, s.start, s.startUtc, s.timezone) ===
        selectedTime
    );
    const sessionStartUtc = originalSlot?.startUtc || buildSessionStartUtc(fullDate, selectedTime);

    try {
      setToastMessage(null);
      await bookAppointment({
        sessionStartUtc,
        serviceId: Number.parseInt(selectedServiceDetails.value, 10),
        sessionType: selectedSessionType,
        duration: Number.parseInt(selectedServiceDetails.duration, 10),
      }).unwrap();
      setIsModalOpen(false);
      setIsSuccessModalOpen(true);
      setSelectedService(undefined);
      setToastType("success");
      setToastMessage("Appointment booked successfully.");
      await Promise.all([refetchUpcomingSessions(), refetchPastSessions()]);
    } catch (bookingError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(bookingError));
    }
  };

  const handleSuccessModalClose = () => {
    setIsSuccessModalOpen(false);
    void refetchAvailableSlots();
  };

  return (
    <div>
      <div className="flex md:flex-row flex-col gap-4 md:gap-8">
        <div className="flex min-h-0 w-full flex-col md:max-h-[calc(100vh-10rem)]">
          <div className="mb-4 flex shrink-0 items-center justify-center gap-4 md:justify-start">
            <div className="bg-(--neutral-100) p-1 rounded-full flex items-center gap-2 w-full md:w-auto">
              <button
                onClick={() => handleTypesTabChange("in-person")}
                aria-pressed={selectedType === "in-person"}
                className={`rounded-full px-4 py-2 text-sm font-medium flex items-center gap-2 transition cursor-pointer w-full md:w-auto ${
                  selectedType === "in-person"
                    ? "bg-white text-(--text-primary-dark)"
                    : "text-(--text-neutral-600)"
                }`}
              >
                <NotepadText
                  className={`w-5 h-5 ${
                    selectedType === "in-person"
                      ? "text-(--text-primary-dark)"
                      : "text-(--text-neutral-600)"
                  } `}
                  strokeWidth={1.33}
                />
                In person
              </button>
              <button
                onClick={() => handleTypesTabChange("virtual")}
                aria-pressed={selectedType === "virtual"}
                disabled={!onlineBookingAvailable}
                title={
                  onlineBookingAvailable
                    ? undefined
                    : "Your therapist hasn't set up video meetings yet."
                }
                className={`rounded-full px-4 py-2 text-sm font-medium flex items-center gap-2 transition w-full md:w-auto ${
                  !onlineBookingAvailable
                    ? "cursor-not-allowed text-(--text-neutral-600) opacity-50"
                    : selectedType === "virtual"
                      ? "cursor-pointer bg-white text-(--text-primary-dark)"
                      : "cursor-pointer text-(--text-neutral-600)"
                }`}
              >
                <Video
                  className={`w-5 h-5 ${
                    selectedType === "virtual" && onlineBookingAvailable
                      ? "text-(--text-primary-dark)"
                      : "text-(--text-neutral-600)"
                  } `}
                  strokeWidth={1.33}
                />
                Virtual Visit
              </button>
            </div>
          </div>

          {!onlineBookingAvailable && (
            <div className="mb-4 shrink-0 rounded-lg border border-(--neutral-200) bg-(--neutral-50) p-3">
              <p className="text-sm text-(--text-neutral-600)">
                Your therapist hasn't set up video meetings yet, so only in-person
                appointments can be booked right now.
              </p>
              {onlineBookingRequestSent ? (
                <p className="mt-2 text-sm font-medium text-(--text-primary-dark)">
                  We've let your therapist know. They'll be in touch once video
                  meetings are set up.
                </p>
              ) : (
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-2"
                  onClick={handleRequestOnlineBooking}
                  loading={isRequestingOnlineBooking}
                >
                  Ask my therapist to enable video sessions
                </Button>
              )}
            </div>
          )}

          <div className="relative z-10 mt-4 shrink-0 rounded-lg bg-white p-4 shadow-xs md:mt-0 md:mb-4">
            <h3 className="text-sm font-semibold text-(--text-primary-dark) mb-3">
              Choose the type of service you need
            </h3>
            {showServicesLoader ? (
              <AppointmentsLoader label="Loading services..." />
            ) : (
              <CustomSelect
                label="Select a service"
                required
                value={selectedService || ""}
                onChange={(v) => setSelectedService(v)}
                options={serviceTypes}
                side="bottom"
                closeOnScroll
                scrollContainerRefs={[bookingScrollRef]}
                collisionPadding={{ top: 12, bottom: 88 }}
                contentClassName="max-h-52"
                renderOption={(opt: {
                  label: string;
                  duration?: string;
                  price?: string;
                }) => (
                  <div className="flex min-w-0 w-full items-center justify-between gap-2">
                    <span
                      className="min-w-0 flex-1 truncate text-sm font-medium"
                      title={opt.label}
                    >
                      {opt.label}
                    </span>
                    <span className="shrink-0 whitespace-nowrap text-sm text-(--text-neutral-400)">
                      {opt.duration ? `${opt.duration} • ` : ""}
                      {opt.price ? `${opt.price}` : ""}
                    </span>
                  </div>
                )}
              />
            )}
          </div>

          <div
            ref={bookingScrollRef}
            className="min-h-0 flex-1 overflow-y-auto overscroll-contain pr-1"
          >
            <div className="flex flex-col gap-4 md:gap-8">
              <div className="bg-white p-4 rounded-lg shadow-xs">
                <h3 className="text-sm font-semibold text-(--text-secondary-dark) mb-4">
                  Select Date & Time
                </h3>
                <div className="flex lg:flex-row flex-col lg:gap-8">
                  <div className="w-full">
                    <Calendar
                      currentMonth={currentMonth}
                      setCurrentMonth={setCurrentMonth}
                      selectedDate={selectedDate}
                      setSelectedDate={setSelectedDate}
                      disablePast
                      isDropdowns={false}
                    />
                  </div>
                  <div className="w-full">
                    <p className="text-(--text-primary-dark) font-medium mb-4 mt-4 md:mt-0">
                      {formattedDate}
                    </p>
                    {!selectedService ? (
                      <p className="text-sm text-(--text-neutral-500)">
                        Select a service to see available time slots.
                      </p>
                    ) : showSlotsLoader ? (
                      <AppointmentsLoader label="Loading available slots..." />
                    ) : (
                      <>
                        {slotTimezone && timeSlots.length > 0 ? (
                          <p className="mb-2 text-xs text-(--text-neutral-500)">
                            Times shown in {slotTimezone}
                          </p>
                        ) : null}
                        <TimeSlots
                          timeSlots={timeSlots}
                          selectedTime={selectedTime}
                          setSelectedTime={(t) => setSelectedTime(t)}
                          showEmptyState={isSlotsSuccess}
                        />
                      </>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="relative z-20 mt-4 shrink-0 rounded-full border border-(--border-success) bg-(--bg-success-light) p-0.5 md:mt-6 flex min-w-0 items-center justify-between gap-2">
            <div
              className="min-w-0 flex-1 truncate pl-6 text-sm font-medium text-(--text-primary-dark)"
              title={`${shortFormattedDate} • ${getBookingSessionTypeLabel(selectedSessionType)}`}
            >
              {shortFormattedDate} • {getBookingSessionTypeLabel(selectedSessionType)}
            </div>
            <Button
              className={` rounded-full px-7.5 py-5.5 ${
                disabled || isBooking ? "cursor-not-allowed" : "cursor-pointer"
              }`}
              onClick={() => !disabled && !isBooking && setIsModalOpen(true)}
              loading={isBooking}
              loadingLabel="Booking..."
            >
              Next
            </Button>
          </div>
        </div>

        <div className="md:min-w-91 flex min-h-0 min-w-0 w-full flex-col">
          <div className="bg-(--neutral-100) p-1 rounded-full flex items-center gap-2 justify-center mb-4 shrink-0">
            <button
              onClick={() => handleFilterTabChange("upcoming")}
              aria-pressed={selectedFilter === "upcoming"}
              className={`rounded-full px-4 py-2 text-sm font-medium w-full flex items-center gap-2 transition cursor-pointer ${
                selectedFilter === "upcoming"
                  ? "bg-white text-(--text-primary-dark)"
                  : "text-(--text-neutral-600)"
              }`}
            >
              Upcoming ({upcomingAppointments.length})
            </button>
            <button
              onClick={() => handleFilterTabChange("previous")}
              aria-pressed={selectedFilter === "previous"}
              className={`rounded-full px-4 py-2 text-sm font-medium w-full flex items-center gap-2 transition cursor-pointer ${
                selectedFilter === "previous"
                  ? "bg-white text-(--text-primary-dark)"
                  : "text-(--text-neutral-600)"
              }`}
            >
              Previous ({pastAppointments.length})
            </button>
          </div>

          <div className="mb-4 flex shrink-0 gap-2">
            <CustomInput
              placeholder="Search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="rounded-full min-h-10 pb-0 pt-1.75"
            />
            <ClientAppointmentsFilterPanel
              filters={appointmentFilters}
              setFilters={setAppointmentFilters}
              serviceOptions={filterServiceOptions}
            />
          </div>
          <div className="min-h-0 max-h-[calc(100vh-17rem)] overflow-y-auto overscroll-contain pr-1">
            {showSessionsHistoryLoader ? (
              <AppointmentsLoader label="Loading appointments..." />
            ) : (
              <UpcomingList upcoming={filteredAppointments} />
            )}
          </div>
        </div>
      </div>

      {/* Modal Integration */}
      <ConfirmationModal
        isOpen={isModalOpen}
        onClose={() => {
          if (!isBooking) {
            setIsModalOpen(false);
          }
        }}
        onConfirm={() => void handleConfirmBooking()}
        isBooking={isBooking}
        data={{
          name: meData?.fullName || "Client",
          date: formattedDate,
          time: selectedTime || "",
          type: getBookingSessionTypeLabel(selectedSessionType),
          serviceName: selectedServiceDetails?.label || "",
          price: selectedServiceDetails?.price || "",
        }}
      />

      {/* Success Modal */}
      <SuccessModal
        isOpen={isSuccessModalOpen}
        onClose={handleSuccessModalClose}
        data={{
          date: shortFormattedDate,
          time: selectedTime || "",
          type: getBookingSessionTypeLabel(selectedSessionType),
        }}
      />
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
}
