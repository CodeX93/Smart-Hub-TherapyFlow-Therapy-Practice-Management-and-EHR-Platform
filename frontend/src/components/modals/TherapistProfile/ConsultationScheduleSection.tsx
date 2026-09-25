import { ContentLoader } from "@/components/shared/ContentLoader";
import React, { useEffect, useMemo } from "react";
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
import {
  useGetMyProfileQuery,
  useUpdateMyProfileMutation,
  type UserProfileResponse,
} from "@/store/api/userProfile.api";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  format12hToApiTime,
  formatApiTimeTo12h,
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

function parseConsultationHours(
  profile: UserProfileResponse,
): ScheduleFormValues["workingHours"] {
  let parsedEntries: WorkingHoursApiEntry[] = [];
  const raw = profile.consultationWorkingHours;
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        parsedEntries = parsed.filter(
          (entry): entry is WorkingHoursApiEntry =>
            typeof entry === "object" &&
            entry !== null &&
            typeof entry.day === "string",
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
        type:
          entry.mode === "in-person"
            ? ("in-person" as const)
            : ("virtual" as const),
        roomIds: [] as string[],
      }));

    return {
      id: day.id,
      label: DAY_LABELS[dayKey] ?? day.label,
      active: slots.length > 0,
      slots,
    };
  });

  if (!mappedDays.some((day) => day.active && day.slots.length > 0)) {
    return getEmptyWorkingDays();
  }
  return mappedDays;
}

interface ConsultationScheduleSectionProps {
  onNotify?: (message: string, type: "success" | "error") => void;
  onProfileSaved?: () => Promise<void> | void;
}

const ConsultationScheduleSection: React.FC<
  ConsultationScheduleSectionProps
> = ({ onNotify, onProfileSaved }) => {
  const form = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      timezone: "",
      physicalRoomIds: [],
      workingHours: initialDays,
    },
  });
  const {
    data: myProfileResponse,
    isLoading: isLoadingMyProfile,
    error: myProfileError,
  } = useGetMyProfileQuery();
  const [updateMyProfile, { isLoading: isSaving }] =
    useUpdateMyProfileMutation();

  useEffect(() => {
    if (myProfileError) {
      form.reset({
        timezone: "",
        physicalRoomIds: [],
        workingHours: getEmptyWorkingDays(),
      });
      onNotify?.("Unable to load consultation schedule.", "error");
      return;
    }
    if (!myProfileResponse) return;
    form.reset({
      timezone: myProfileResponse.timezone || "America/Toronto",
      physicalRoomIds: [],
      workingHours: parseConsultationHours(myProfileResponse),
    });
  }, [form, myProfileError, myProfileResponse, onNotify]);

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
          id: createSlotId(),
          startTime: "09:00 AM",
          endTime: "12:00 PM",
          type: "virtual",
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
        id: createSlotId(),
        startTime: "09:00 AM",
        endTime: "12:00 PM",
        type: "virtual",
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
        sourceSlots.map((s) => ({ ...s, id: createSlotId() })),
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

    try {
      await updateMyProfile({
        consultationWorkingHours: JSON.stringify(workingHoursPayload),
      }).unwrap();
      await onProfileSaved?.();
      onNotify?.("Consultation schedule updated successfully.", "success");
    } catch (error) {
      onNotify?.(getApiErrorMessage(error), "error");
    }
  };

  const hasHours = useMemo(
    () =>
      (form.watch("workingHours") ?? []).some(
        (d) => d.active && d.slots.length > 0,
      ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [form.watch("workingHours")],
  );

  return (
    <div className="flex min-w-0 flex-col gap-6 pt-6">
      <p className="text-sm text-(--text-neutral-600)">
        Set hours used only for public Consultation bookings. Your regular
        Schedule tab (All Services) is unchanged.
      </p>
      <ScheduleTimezoneNote />
      <FormProvider {...form}>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(handleSave)}
            className="relative flex h-full min-w-0 flex-col gap-4"
          >
            {isLoadingMyProfile ? (
              <div className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-white/80">
                <ContentLoader variant="inline" size="md" />
              </div>
            ) : null}

            <div className="flex flex-col gap-3">
              {workingHours.map((day, index) => (
                <WorkingDayItem
                  key={day.id}
                  idPrefix="consultation"
                  dayIndex={index}
                  handleDayToggle={handleDayToggle}
                  addSlot={addSlot}
                  removeSlot={removeSlot}
                  copyToAll={copyToAll}
                />
              ))}
            </div>

            <div className="flex justify-end pt-2">
              <Button
                type="submit"
                disabled={isSaving || isLoadingMyProfile}
                className="rounded-full bg-(--bg-primary-dark) px-6 text-white"
              >
                {isSaving ? "Saving…" : hasHours ? "Save Consultation Schedule" : "Clear Consultation Schedule"}
              </Button>
            </div>
          </form>
        </Form>
      </FormProvider>
    </div>
  );
};

export default ConsultationScheduleSection;
