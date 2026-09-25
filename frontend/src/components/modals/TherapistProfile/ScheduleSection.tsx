import { useCallback } from "react";

import { ContentLoader } from "@/components/shared/ContentLoader";
import React from "react";
import { useForm, useFieldArray, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "../../ui/button";
import { Form } from "../../ui/form";
import { initialDays } from "@/pages/admin/user-access/profiles/user-access.static";
import {
  scheduleSchema,
  type ScheduleFormValues,
} from "@/schemas/general-profile-modal-schemas";
import WorkingDayItem from "./WorkingDayItem";
import CustomSelect from "@/components/form/CustomSelect";
import {
  useGetMyProfileQuery,
  useGetMyTimezoneQuery,
  useGetMyTimezonesQuery,
  useUpdateMyProfileMutation,
  type UserProfileResponse,
} from "@/store/api/userProfile.api";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import { useGetAdminRoomsQuery } from "@/store/api/admin/rooms.api";
import { useEffect, useMemo, useRef, useState } from "react";
import { Check, ChevronDown } from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import { cn } from "@/lib/utils";
import { getApiErrorMessage } from "@/utils/apiError";
import { useCloseOnScroll } from "@/hooks/useCloseOnScroll";
import {
  format12hToApiTime,
  formatApiTimeTo12h,
  resolveScheduleFormTimezone,
} from "@/utils/therapistTimezone";
import ScheduleTimezoneNote from "@/components/shared/ScheduleTimezoneNote";

type WorkingHoursApiEntry = {
  day: string;
  enabled?: boolean;
  start: string;
  end: string;
  mode?: string;
};

const DAY_LABELS: Record<string, string> = {
  monday: "Monday",
  tuesday: "Tuesday",
  wednesday: "Wednesday",
  thursday: "Thursday",
  friday: "Friday",
  saturday: "Saturday",
  sunday: "Sunday",
};

function createSlotId(): string {
  return Math.random().toString(36).slice(2, 11);
}

function getEmptyWorkingDays(): ScheduleFormValues["workingHours"] {
  return initialDays.map((day) => ({
    ...day,
    active: false,
    slots: [],
  }));
}

const toDisplayTime = formatApiTimeTo12h;
const toApiTime = format12hToApiTime;

function parseWorkingHours(profile: UserProfileResponse): ScheduleFormValues["workingHours"] {
  let parsedEntries: WorkingHoursApiEntry[] = [];

  if (profile.workingHours) {
    try {
      const parsed = JSON.parse(profile.workingHours);
      if (Array.isArray(parsed)) {
        parsedEntries = parsed.filter(
          (entry): entry is WorkingHoursApiEntry =>
            typeof entry === "object" && entry !== null && typeof entry.day === "string",
        );
      }
    } catch {
      parsedEntries = [];
    }
  }

  const mappedDays = initialDays.map((day) => {
    const dayKey = day.id.toLowerCase();
    const slots = parsedEntries
      .filter((entry) => entry.day.toLowerCase() === dayKey)
      .map((entry) => ({
        id: createSlotId(),
        startTime: toDisplayTime(entry.start),
        endTime: toDisplayTime(entry.end),
        type: entry.mode === "in-person" ? ("in-person" as const) : ("virtual" as const),
        roomIds:
          entry.mode === "in-person"
            ? (profile.availablePhysicalRoomIds ?? []).map((roomId) => String(roomId))
            : [],
      }));

    const isActive =
      profile.workingDays?.some((workingDay) => workingDay.toLowerCase() === dayKey) ??
      Boolean(slots.length);

    return {
      id: day.id,
      label: DAY_LABELS[dayKey] ?? day.label,
      active: isActive,
      slots: slots.length > 0 ? slots : isActive ? day.slots : [],
    };
  });

  const hasAnyConfiguredDay = mappedDays.some((day) => day.active && day.slots.length > 0);
  const hasWorkingDays = (profile.workingDays?.length ?? 0) > 0;

  if (!hasAnyConfiguredDay && !hasWorkingDays) {
    return getEmptyWorkingDays();
  }

  return mappedDays;
}

function formatProfileError(error: unknown): string {
  if (
    typeof error === "object" &&
    error !== null &&
    "data" in error &&
    typeof (error as { data?: { message?: unknown } }).data?.message === "string"
  ) {
    return (error as { data?: { message?: string } }).data?.message ?? "Unable to load schedule.";
  }

  if (
    typeof error === "object" &&
    error !== null &&
    "error" in error &&
    typeof (error as { error?: unknown }).error === "string"
  ) {
    return (error as { error: string }).error;
  }

  return "Unable to load schedule.";
}

interface ScheduleSectionProps {
  onNotify?: (message: string, type: "success" | "error") => void;
  onProfileSaved?: () => Promise<void> | void;
}

const ScheduleSection: React.FC<ScheduleSectionProps> = ({
  onNotify,
  onProfileSaved,
}) => {
  const [roomsPopoverOpen, setRoomsPopoverOpen] = useState(false);
  const [portalContainer, setPortalContainer] = useState<HTMLElement | null>(null);
  const sectionRef = useRef<HTMLDivElement>(null);
  const roomsTriggerRef = useRef<HTMLButtonElement>(null);
  const scrollContainerRef = useRef<HTMLElement | null>(null);
  const form = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      timezone: "",
      physicalRoomIds: [],
      workingHours: initialDays,
    },
  });
  const { data: timezoneOptionsResponse, isLoading: isLoadingTimezones } =
    useGetMyTimezonesQuery();
  const {
    data: myTimezoneResponse,
    isLoading: isLoadingMyTimezone,
    error: myTimezoneError,
  } =
    useGetMyTimezoneQuery();
  const {
    data: myProfileResponse,
    isLoading: isLoadingMyProfile,
    error: myProfileError,
  } = useGetMyProfileQuery();
  const { data: roomsResponse = [], isLoading: isLoadingRooms } = useGetAdminRoomsQuery();
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const [updateMyProfile, { isLoading: isSaving }] = useUpdateMyProfileMutation();

  const timezoneOptions = useMemo(
    () =>
      (timezoneOptionsResponse?.timezones ?? []).map((timezone) => ({
        label: timezone,
        value: timezone,
      })),
    [timezoneOptionsResponse?.timezones],
  );
  const roomOptions = useMemo(
    () =>
      roomsResponse
        .filter((room) => room.isActive)
        .map((room) => ({
          label: `${room.roomNumber} - ${room.roomName}`,
          value: String(room.id),
        })),
    [roomsResponse],
  );

  const attachSection = useCallback((node: HTMLDivElement | null) => {
    sectionRef.current = node;
    scrollContainerRef.current = node?.closest(".overflow-y-auto") as HTMLElement | null;
    setPortalContainer(node?.closest("[data-therapist-profile-modal]") as HTMLElement | null);
  }, []);

  useCloseOnScroll(
    roomsPopoverOpen,
    () => setRoomsPopoverOpen(false),
    roomsTriggerRef,
    [scrollContainerRef],
  );

  useEffect(() => {
    if (myProfileError) {
      form.reset({
        timezone: myTimezoneResponse?.timezone || "",
        physicalRoomIds: [],
        workingHours: getEmptyWorkingDays(),
      });
      onNotify?.(formatProfileError(myProfileError), "error");
      return;
    }

    if (!myProfileResponse) return;

    const physicalIds = (myProfileResponse.availablePhysicalRoomIds ?? []).map(String);
    const virtualId = myProfileResponse.virtualRoomId ? String(myProfileResponse.virtualRoomId) : null;
    const allSelectedIds = virtualId && !physicalIds.includes(virtualId)
      ? [...physicalIds, virtualId]
      : physicalIds;

    form.reset({
      timezone: resolveScheduleFormTimezone(
        myTimezoneResponse?.timezone || myProfileResponse.timezone,
        practiceConfig?.timezone,
      ),
      physicalRoomIds: allSelectedIds,
      workingHours: parseWorkingHours(myProfileResponse),
    });
  }, [form, myProfileError, myProfileResponse, myTimezoneResponse?.timezone, practiceConfig?.timezone, onNotify]);

  useEffect(() => {
    if (!myTimezoneError) return;
    onNotify?.(formatProfileError(myTimezoneError), "error");
  }, [myTimezoneError, onNotify]);

  const { fields: workingHours } = useFieldArray({
    control: form.control,
    name: "workingHours",
  });

  const handleDayToggle = (index: number, active: boolean) => {
    const day = form.getValues(`workingHours.${index}`);
    form.setValue(`workingHours.${index}.active`, active);
    if (active && day.slots.length === 0) {
      form.setValue(`workingHours.${index}.slots`, [
        {
          id: Math.random().toString(36).substr(2, 9),
          startTime: "09:00 AM",
          endTime: "05:00 PM",
          type: "in-person",
          roomIds: [],
        },
      ]);
    }
  };

  const addSlot = (dayIndex: number) => {
    const currentSlots = form.getValues(`workingHours.${dayIndex}.slots`);
    form.setValue(`workingHours.${dayIndex}.slots`, [
      ...currentSlots,
      {
        id: Math.random().toString(36).substr(2, 9),
        startTime: "09:00 AM",
        endTime: "05:00 PM",
        type: "in-person",
        roomIds: [],
      },
    ]);
  };

  const removeSlot = (dayIndex: number, slotIndex: number) => {
    const currentSlots = form.getValues(`workingHours.${dayIndex}.slots`);
    form.setValue(
      `workingHours.${dayIndex}.slots`,
      currentSlots.filter((_, i) => i !== slotIndex),
    );
  };

  const copyToAll = (sourceDayIndex: number) => {
    const sourceSlots = form.getValues(`workingHours.${sourceDayIndex}.slots`);
    const allDays = form.getValues("workingHours");
    const weekdayIds = new Set([
      "monday",
      "tuesday",
      "wednesday",
      "thursday",
      "friday",
    ]);

    allDays.forEach((day, index) => {
      if (!weekdayIds.has(day.id.toLowerCase()) || index === sourceDayIndex) {
        return;
      }

      form.setValue(`workingHours.${index}.active`, true);
      form.setValue(
        `workingHours.${index}.slots`,
        sourceSlots.map((s) => ({
          ...s,
          id: Math.random().toString(36).substr(2, 9),
        })),
      );
    });
  };

  const handleSave = async (values: ScheduleFormValues) => {
    const activeDays = values.workingHours.filter((day) => day.active);
    const workingHoursPayload = activeDays.flatMap((day) =>
      day.slots.map((slot) => ({
        day: day.id.toLowerCase(),
        enabled: true,
        start: toApiTime(slot.startTime),
        end: toApiTime(slot.endTime),
        mode: slot.type,
      })),
    );

    const selectedRoomIds = (values.physicalRoomIds ?? [])
      .map((id) => Number.parseInt(id, 10))
      .filter((id) => Number.isFinite(id));

    const availablePhysicalRoomIds = selectedRoomIds.filter((id) => {
      const room = roomsResponse.find((r) => r.id === id);
      return room && room.roomType.toUpperCase() !== "VIRTUAL";
    });

    const virtualRoom = roomsResponse.find(
      (r) => selectedRoomIds.includes(r.id) && r.roomType.toUpperCase() === "VIRTUAL",
    );
    const virtualRoomId = virtualRoom?.id ?? null;

    try {
      await updateMyProfile({
        // Only send a timezone the therapist actually picked. The field always shows
        // an effective zone, so sending it on every save would pin today's clinic zone
        // onto the profile and stop it following the clinic from then on.
        ...(form.formState.dirtyFields.timezone
          ? { timezone: values.timezone }
          : {}),
        workingDays: activeDays.map((day) => day.id.toLowerCase()),
        workingHours: JSON.stringify(workingHoursPayload),
        availablePhysicalRoomIds,
        availabilityStatus: myProfileResponse?.availabilityStatus,
        sessionDuration: myProfileResponse?.sessionDuration,
        virtualRoomId,
      }).unwrap();

      await onProfileSaved?.();
      onNotify?.("Schedule updated successfully.", "success");
    } catch (error) {
      onNotify?.(getApiErrorMessage(error), "error");
    }
  };

  return (
    <div ref={attachSection} className="flex min-w-0 flex-col gap-6 pt-6">
      <FormProvider {...form}>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(handleSave)}
            className="relative flex h-full min-w-0 flex-col gap-4"
          >
            {isLoadingMyProfile || isLoadingMyTimezone ? (
              <div className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-white/80 backdrop-blur-[0.125rem]">
                <div className="flex items-center gap-2 rounded-full border border-(--neutral-100) bg-white px-4 py-3 text-sm font-medium text-(--text-neutral-500) shadow-(--shadow)">
                  <ContentLoader variant="inline" size="md" />
                </div>
              </div>
            ) : null}

            <CustomSelect
              control={form.control}
              name="timezone"
              label="Time Zone"
              options={timezoneOptions}
              isSearch={true}
              required
              disabled={isLoadingTimezones || isLoadingMyProfile}
              portalContainer={portalContainer}
              closeOnScroll
              scrollContainerRefs={[scrollContainerRef]}
            />
            <ScheduleTimezoneNote />

              <FormField
                control={form.control}
                name="physicalRoomIds"
                render={({ field }) => {
                  const selected: string[] = field.value ?? [];
                  const toggle = (val: string) => {
                    const next = selected.includes(val)
                      ? selected.filter((v) => v !== val)
                      : [...selected, val];
                    field.onChange(next);
                  };
                  const selectedLabels = selected
                    .map((v) => roomOptions.find((o) => o.value === v)?.label)
                    .filter(Boolean)
                    .join(", ");
                  return (
                    <FormItem>
                      <FormControl>
                        <Popover open={roomsPopoverOpen} onOpenChange={setRoomsPopoverOpen}>
                          <PopoverTrigger asChild>
                            <button
                              ref={roomsTriggerRef}
                              type="button"
                              disabled={isLoadingRooms || isLoadingMyProfile}
                              className={cn(
                                "group w-full h-15 rounded-xl flex justify-between gap-2 items-center border border-(--neutral-100) hover:border-(--neutral-600) focus:border-(--neutral-600) transition-colors shadow-(--shadow) pt-7 px-3 pb-2 cursor-pointer relative text-left bg-transparent focus-visible:outline-none focus-visible:ring-0 overflow-hidden",
                                (isLoadingRooms || isLoadingMyProfile) && "cursor-not-allowed opacity-60 hover:border-(--neutral-100)"
                              )}
                            >
                              <span className={cn(
                                "absolute left-3 transition-all duration-200 pointer-events-none text-(--text-secondary-light)",
                                selectedLabels || roomsPopoverOpen
                                  ? "top-3.5 -translate-y-1/2 text-xs"
                                  : "top-1/2 -translate-y-1/2"
                              )}>
                                Select Rooms <span className="text-red-500">*</span>
                              </span>
                              <div className="flex min-w-0 flex-1 items-center">
                                <span
                                  className="block min-w-0 truncate text-sm text-(--neutral-950)"
                                  title={selectedLabels}
                                >
                                  {selectedLabels || ""}
                                </span>
                              </div>
                              <ChevronDown
                                className={cn(
                                  "h-5 w-5 shrink-0 -translate-y-1/2 text-(--text-neutral-400) opacity-50 transition-transform",
                                  roomsPopoverOpen && "rotate-180",
                                )}
                              />
                            </button>
                          </PopoverTrigger>
                          <PopoverContent
                            container={portalContainer ?? undefined}
                            className="w-(--radix-popover-trigger-width) max-w-full max-h-[min(var(--radix-popover-content-available-height),15rem)] flex flex-col p-0 z-9999 rounded-xl overflow-hidden border-(--neutral-100) bg-white shadow-xl"
                            align="start"
                            side="bottom"
                            sideOffset={4}
                            collisionPadding={24}
                          >
                            <div className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden overscroll-contain">
                              {roomOptions.length === 0 ? (
                                <div className="p-4 text-sm text-center text-(--text-neutral-400)">No rooms available</div>
                              ) : (
                                roomOptions.map((opt) => (
                                  <div
                                    key={opt.value}
                                    onClick={() => toggle(opt.value)}
                                    className="px-3 py-2.5 hover:bg-(--bg-primary-light)/50 cursor-pointer border-b border-(--neutral-50) last:border-0 transition-colors text-(--text-primary-dark) font-medium text-sm flex items-center justify-between gap-2 min-w-0"
                                  >
                                    <span className="min-w-0 flex-1 truncate" title={opt.label}>
                                      {opt.label}
                                    </span>
                                    {selected.includes(opt.value) && (
                                      <Check size={16} className="text-(--text-primary-dark) shrink-0" />
                                    )}
                                  </div>
                                ))
                              )}
                            </div>
                          </PopoverContent>
                        </Popover>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  );
                }}
              />

            <h3 className="text-(--neutral-950) text-[1.125rem] leading-6 font-semibold">
              Working hours
            </h3>

            <div className="flex flex-col gap-5">
              {workingHours.map((day, index) => (
                <WorkingDayItem
                  key={day.id}
                  idPrefix="schedule"
                  dayIndex={index}
                  handleDayToggle={handleDayToggle}
                  addSlot={addSlot}
                  removeSlot={removeSlot}
                  copyToAll={copyToAll}
                />
              ))}
            </div>

            <div className="pt-4 flex justify-end pb-6">
              <Button
                type="submit"
                disabled={isSaving || isLoadingMyProfile || isLoadingMyTimezone}
                loading={isSaving}
                loadingLabel="Saving..."
                className="rounded-full px-10 py-3 h-14 bg-(--bg-primary-dark) text-white hover:opacity-90 font-semibold text-[1rem] leading-6 cursor-pointer"
              >
                Save changes
              </Button>
            </div>
          </form>
        </Form>
      </FormProvider>
    </div>
  );
};

export default ScheduleSection;
