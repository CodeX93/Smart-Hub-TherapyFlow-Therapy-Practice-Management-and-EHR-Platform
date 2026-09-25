import { useCallback } from "react";
import React, { forwardRef, useImperativeHandle, useRef, useState } from "react";
import { useForm, useFieldArray, FormProvider } from "react-hook-form";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomInput from "../form/CustomInput";
import { Form } from "../ui/form";
import {
  scheduleSchema,
  USER_ACCESS_PROFILE_LIMITS,
  type ScheduleFormValues,
} from "@/schemas/user-access-profiles.schema";
import { initialDays } from "@/pages/admin/user-access/profiles/user-access.static";
import WorkingDayItem from "./WorkingDayItem";
import CustomSelect from "../form/CustomSelect";
import { TIME_ZONE_OPTIONS } from "@/utils/functions/timezone";
import type { AdminUserProfessionalProfile } from "@/store/api/admin/users.api";
import {
  formatApiTimeTo12h,
  resolveScheduleFormTimezone,
} from "@/utils/therapistTimezone";
import ScheduleTimezoneNote from "@/components/shared/ScheduleTimezoneNote";
import { Check, ChevronDown } from "lucide-react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import { cn } from "@/lib/utils";
import { useCloseOnScroll } from "@/hooks/useCloseOnScroll";
import type { ScheduleSectionHandle } from "./profileSectionHandle";
type RoomMultiSelectOption = { id: string; label: string };

interface ScheduleSectionProps {
  profileData?: AdminUserProfessionalProfile | null;
  isProfileMissing?: boolean;
  roomOptions?: RoomMultiSelectOption[];
}

const ScheduleSection = forwardRef<
  ScheduleSectionHandle<ScheduleFormValues>,
  ScheduleSectionProps
>(function ScheduleSection({ profileData, isProfileMissing, roomOptions }, ref) {
  const sectionRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLElement | null>(null);
  const roomsTriggerRef = useRef<HTMLButtonElement>(null);
  const [portalContainer, setPortalContainer] = useState<HTMLElement | null>(null);
  const [roomsPopoverOpen, setRoomsPopoverOpen] = useState(false);
  const { data: practiceConfig, isFetching: isFetchingPracticeConfig } =
    useGetPracticeConfigurationQuery();
  const practiceTimezone = practiceConfig?.timezone?.trim() || "";
  const isCreatingProfile = Boolean(isProfileMissing) || !profileData;

  const attachSection = useCallback((node: HTMLDivElement | null) => {
    sectionRef.current = node;
    scrollContainerRef.current = node?.closest(".overflow-y-auto") as HTMLElement | null;
    setPortalContainer(node?.closest("[data-admin-profile-modal]") as HTMLElement | null);
  }, []);

  useCloseOnScroll(
    roomsPopoverOpen,
    () => setRoomsPopoverOpen(false),
    roomsTriggerRef,
    [scrollContainerRef],
  );

  const getEmptyWorkingDays = React.useCallback(
    (): ScheduleFormValues["workingHours"] =>
      initialDays.map((day) => ({
        ...day,
        active: false,
        slots: [],
      })),
    [],
  );

  const formatTo12HourTime = formatApiTimeTo12h;

  const form = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      timezone: "",
      physicalRoomIds: [],
      maxClientsPerDay: "0",
      sessionDuration: "50",
      workingHours: getEmptyWorkingDays(),
    },
  });

  const { fields: workingHours } = useFieldArray({
    control: form.control,
    name: "workingHours",
  });

  const handleDayToggle = (index: number, active: boolean) => {
    const day = form.getValues(`workingHours.${index}`);
    form.setValue(`workingHours.${index}.active`, active, {
      shouldDirty: true,
      shouldValidate: true,
    });
    if (active && day.slots.length === 0) {
      form.setValue(`workingHours.${index}.slots`, [
        {
          id: Math.random().toString(36).substr(2, 9),
          startTime: "09:00 AM",
          endTime: "05:00 PM",
          type: "in-person",
          roomIds: [],
        },
      ], { shouldDirty: true, shouldValidate: true });
    }
  };

  const addSlot = (dayIndex: number) => {
    const currentSlots = form.getValues(`workingHours.${dayIndex}.slots`);
    form.setValue(
      `workingHours.${dayIndex}.slots`,
      [
        ...currentSlots,
        {
          id: Math.random().toString(36).substr(2, 9),
          startTime: "09:00 AM",
          endTime: "05:00 PM",
          type: "in-person",
          roomIds: [],
        },
      ],
      { shouldDirty: true, shouldValidate: true },
    );
  };

  const removeSlot = (dayIndex: number, slotIndex: number) => {
    const currentSlots = form.getValues(`workingHours.${dayIndex}.slots`);
    form.setValue(
      `workingHours.${dayIndex}.slots`,
      currentSlots.filter((_, i) => i !== slotIndex),
      { shouldDirty: true, shouldValidate: true },
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

      form.setValue(`workingHours.${index}.active`, true, {
        shouldDirty: true,
        shouldValidate: true,
      });
      form.setValue(
        `workingHours.${index}.slots`,
        sourceSlots.map((s) => ({
          ...s,
          id: Math.random().toString(36).substr(2, 9),
        })),
        { shouldDirty: true, shouldValidate: true },
      );
    });
  };

  const scrollToRoomsField = () => {
    const trigger = roomsTriggerRef.current;
    if (!trigger) return;
    trigger.scrollIntoView({ behavior: "smooth", block: "center" });
    // Focus after scroll so the required field (and its error) is visible.
    window.setTimeout(() => {
      trigger.focus({ preventScroll: true });
    }, 250);
  };

  const { isDirty, dirtyFields, errors } = form.formState;

  useImperativeHandle(
    ref,
    () => ({
      getValues: () => form.getValues(),
      trigger: () => form.trigger(),
      reset: (values) => form.reset(values ?? form.getValues()),
      isDirty: () => isDirty,
      // The picker shows the clinic zone until the admin picks one, so only a
      // touched field counts as a choice worth storing on the profile.
      isTimezoneExplicitlyChosen: () => Boolean(dirtyFields.timezone),
      focusError: () => {
        if (errors.physicalRoomIds) {
          scrollToRoomsField();
        }
      },
    }),
    [dirtyFields.timezone, errors.physicalRoomIds, form, isDirty],
  );

  React.useEffect(() => {
    // Creating a profile: seed the timezone from practice settings as the default.
    // Wait until practice config has resolved so we don't stick on an empty/stale default.
    if (isCreatingProfile) {
      if (isFetchingPracticeConfig && !practiceTimezone) return;
      // The admin picked a timezone for this therapist — never overwrite it.
      if (form.formState.dirtyFields.timezone) return;

      if (!form.formState.isDirty) {
        form.reset({
          timezone: practiceTimezone,
          physicalRoomIds: [],
          maxClientsPerDay: "0",
          sessionDuration: "50",
          workingHours: getEmptyWorkingDays(),
        });
        return;
      }

      // Other schedule fields were edited but the timezone was left alone, so it is
      // still the default — re-seed it in case practice config only just resolved.
      if (form.getValues("timezone") !== practiceTimezone) {
        form.setValue("timezone", practiceTimezone, {
          shouldDirty: false,
          shouldValidate: true,
        });
      }
      return;
    }

    if (!profileData) return;
    if (form.formState.isDirty) return;

    let parsedWorkingHours = getEmptyWorkingDays();
    if (profileData.workingHours) {
      try {
        const parsed = JSON.parse(profileData.workingHours);
        if (Array.isArray(parsed)) {
          // API can return flat entries [{ day, start, end, mode, enabled }]
          // or grouped day entries [{ day, slots: [...] }]. Support both.
          const hasFlatEntries =
            parsed.length > 0 &&
            typeof parsed[0] === "object" &&
            parsed[0] !== null &&
            ("day" in parsed[0] || "start" in parsed[0] || "mode" in parsed[0]);

          if (hasFlatEntries) {
            parsedWorkingHours = getEmptyWorkingDays().map((baseDay) => {
              const dayKey = baseDay.id.toLowerCase();
              const dayEntries = parsed.filter((entry) => {
                if (!entry || typeof entry !== "object") return false;
                const record = entry as Record<string, unknown>;
                return String(record.day || "").toLowerCase() === dayKey;
              });

              const slots = dayEntries.map((entry, index) => {
                const record = entry as Record<string, unknown>;
                const mode = String(record.mode || record.type || "virtual").toLowerCase();
                return {
                  id: `${dayKey}-${index}-${Math.random().toString(36).slice(2, 7)}`,
                  startTime: formatTo12HourTime(String(record.start || record.startTime || "09:00")),
                  endTime: formatTo12HourTime(String(record.end || record.endTime || "17:00")),
                    type: (mode === "in-person" ? "in-person" : "virtual") as
                      | "in-person"
                      | "virtual",
                  roomIds:
                    mode === "in-person"
                      ? (profileData.availablePhysicalRoomIds || []).map(String)
                      : [],
                };
              });

              return {
                ...baseDay,
                active: slots.length > 0,
                slots,
              };
            });
          } else {
            parsedWorkingHours = parsed.map((day, index) => ({
              id: String(day?.id || day?.day || initialDays[index]?.id || index),
              label: String(day?.label || day?.day || initialDays[index]?.label || `Day ${index + 1}`),
              active: Boolean(day?.enabled ?? day?.active ?? false),
              slots: Array.isArray(day?.slots)
                ? day.slots.map((slot: Record<string, unknown>) => ({
                    id: String(slot.id || Math.random().toString(36).slice(2, 9)),
                    startTime: formatTo12HourTime(
                      String(slot.startTime || slot.start || "09:00"),
                    ),
                    endTime: formatTo12HourTime(
                      String(slot.endTime || slot.end || "17:00"),
                    ),
                    type: (
                      String(slot.mode || slot.type || "virtual").toLowerCase() === "in-person"
                        ? "in-person"
                        : "virtual"
                    ) as "in-person" | "virtual",
                    roomIds: Array.isArray(slot.roomIds)
                      ? slot.roomIds.map((id) => String(id))
                      : [],
                  }))
                : [],
            }));
          }
        }
      } catch {
        parsedWorkingHours = initialDays;
      }
    } else if (Array.isArray(profileData.workingDays) && profileData.workingDays.length > 0) {
      const activeDays = new Set(profileData.workingDays.map((d) => d.toLowerCase()));
                parsedWorkingHours = getEmptyWorkingDays().map((day) => ({
        ...day,
        active: activeDays.has(day.id.toLowerCase()),
      }));
    }

    // The therapist's own saved timezone wins: a therapist may sit outside the
    // clinic's zone, and the Administration timezone is only the default a new
    // profile starts from. Wait for practice config only when we'd have to fall
    // back to it, so a saved value never flashes past an empty/stale default.
    if (isFetchingPracticeConfig && !practiceTimezone && !profileData.timezone) {
      return;
    }

    form.reset({
      timezone: resolveScheduleFormTimezone(profileData.timezone, practiceTimezone),
      physicalRoomIds: [
        ...(profileData.availablePhysicalRoomIds || []).map(String),
        ...(profileData.virtualRoomId ? [String(profileData.virtualRoomId)] : []),
      ],
      maxClientsPerDay: profileData.maxClientsPerDay
        ? String(profileData.maxClientsPerDay)
        : "0",
      sessionDuration: profileData.sessionDuration
        ? String(profileData.sessionDuration)
        : "50",
      workingHours: parsedWorkingHours,
    });
  }, [
    form,
    profileData,
    getEmptyWorkingDays,
    isCreatingProfile,
    isFetchingPracticeConfig,
    practiceTimezone,
  ]);

  return (
    <div ref={attachSection} className="flex flex-col gap-6 pt-6 pb-4">
      <FormProvider {...form}>
        <Form {...form}>
          <form
            onSubmit={(event) => event.preventDefault()}
            className="flex flex-1 flex-col gap-4"
          >
            <CustomSelect
              control={form.control}
              name="timezone"
              label="Time Zone"
              options={TIME_ZONE_OPTIONS}
              isSearch={true}
              required
              closeOnScroll
              portalContainer={portalContainer}
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
                  .map((v) => roomOptions?.find((o) => o.id === v)?.label)
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
                            className={cn(
                              "group w-full h-15 rounded-xl flex justify-between gap-2 items-center border border-(--neutral-100) hover:border-(--neutral-600) focus:border-(--neutral-600) transition-colors shadow-(--shadow) pt-7 px-3 pb-2 cursor-pointer relative text-left bg-transparent focus-visible:outline-none focus-visible:ring-0 overflow-hidden",
                            )}
                          >
                            <span
                              className={cn(
                                "absolute left-3 transition-all duration-200 pointer-events-none text-(--text-secondary-light)",
                                selectedLabels || roomsPopoverOpen
                                  ? "top-3.5 -translate-y-1/2 text-xs"
                                  : "top-1/2 -translate-y-1/2",
                              )}
                            >
                              Select Rooms <span className="text-red-500">*</span>
                            </span>
                            <span
                              className="block min-w-0 flex-1 truncate text-sm text-(--neutral-950)"
                              title={selectedLabels}
                            >
                              {selectedLabels || ""}
                            </span>
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
                            {(roomOptions || []).length === 0 ? (
                              <div className="p-4 text-sm text-center text-(--text-neutral-400)">
                                No rooms available
                              </div>
                            ) : (
                              (roomOptions || []).map((opt) => (
                                <div
                                  key={opt.id}
                                  onClick={() => toggle(opt.id)}
                                  className="px-3 py-2.5 hover:bg-(--bg-primary-light)/50 cursor-pointer border-b border-(--neutral-50) last:border-0 transition-colors text-(--text-primary-dark) font-medium text-sm flex items-center justify-between gap-2"
                                >
                                  <span>{opt.label}</span>
                                  {selected.includes(opt.id) ? (
                                    <Check
                                      size={16}
                                      className="text-(--text-primary-dark) shrink-0"
                                    />
                                  ) : null}
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

            <div className="grid w-full grid-cols-2 gap-4">
              <CustomInput
                control={form.control}
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                digitsOnly
                maxLength={USER_ACCESS_PROFILE_LIMITS.maxClientsPerDay}
                label="Max Clients / day"
                name="maxClientsPerDay"
                className="w-full"
              />
              <CustomInput
                control={form.control}
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                digitsOnly
                maxLength={USER_ACCESS_PROFILE_LIMITS.sessionDuration}
                label="Session Duration (min)"
                name="sessionDuration"
                className="w-full"
              />
            </div>

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
          </form>
        </Form>
      </FormProvider>
    </div>
  );
});

export default ScheduleSection;
