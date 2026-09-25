import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
} from "react";
import { useForm, useFieldArray, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "../ui/form";
import {
  consultationScheduleSchema,
  type ConsultationScheduleFormValues,
} from "@/schemas/user-access-profiles.schema";
import { initialDays } from "@/pages/admin/user-access/profiles/user-access.static";
import WorkingDayItem from "./WorkingDayItem";
import type { AdminUserProfessionalProfile } from "@/store/api/admin/users.api";
import { formatApiTimeTo12h } from "@/utils/therapistTimezone";
import type { ProfileSectionHandle } from "./profileSectionHandle";

interface ConsultationScheduleSectionProps {
  profileData?: AdminUserProfessionalProfile | null;
}

function createSlotId(): string {
  return Math.random().toString(36).slice(2, 11);
}

function parseConsultationWorkingHours(
  raw: string | undefined | null,
  emptyDays: ConsultationScheduleFormValues["workingHours"],
): ConsultationScheduleFormValues["workingHours"] {
  if (!raw?.trim()) {
    return emptyDays;
  }
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return emptyDays;
    }

    return emptyDays.map((baseDay) => {
      const dayKey = baseDay.id.toLowerCase();
      const dayEntries = parsed.filter((entry) => {
        if (!entry || typeof entry !== "object") return false;
        const record = entry as Record<string, unknown>;
        if (record.enabled === false) return false;
        return String(record.day || "").toLowerCase() === dayKey;
      });

      const slots = dayEntries.map((entry, index) => {
        const record = entry as Record<string, unknown>;
        const mode = String(
          record.mode || record.type || "virtual",
        ).toLowerCase();
        return {
          id: `${dayKey}-c-${index}-${createSlotId()}`,
          startTime: formatApiTimeTo12h(
            String(record.start || record.startTime || "09:00"),
          ),
          endTime: formatApiTimeTo12h(
            String(record.end || record.endTime || "17:00"),
          ),
          type: (mode === "in-person" ? "in-person" : "virtual") as
            | "in-person"
            | "virtual",
          roomIds: [] as string[],
        };
      });

      return {
        ...baseDay,
        active: slots.length > 0,
        slots,
      };
    });
  } catch {
    return emptyDays;
  }
}

const ConsultationScheduleSection = forwardRef<
  ProfileSectionHandle<ConsultationScheduleFormValues>,
  ConsultationScheduleSectionProps
>(function ConsultationScheduleSection({ profileData }, ref) {
  const getEmptyWorkingDays = useCallback(
    (): ConsultationScheduleFormValues["workingHours"] =>
      initialDays.map((day) => ({
        ...day,
        active: false,
        slots: [],
      })),
    [],
  );

  const form = useForm<ConsultationScheduleFormValues>({
    resolver: zodResolver(consultationScheduleSchema),
    defaultValues: {
      workingHours: getEmptyWorkingDays(),
    },
  });

  const { fields: workingHours } = useFieldArray({
    control: form.control,
    name: "workingHours",
  });

  const handleDayToggle = (index: number, active: boolean) => {
    const day = form.getValues(`workingHours.${index}`);
    form.setValue(`workingHours.${index}.active`, active, { shouldDirty: true });
    if (active && day.slots.length === 0) {
      form.setValue(
        `workingHours.${index}.slots`,
        [
          {
            id: createSlotId(),
            startTime: "09:00 AM",
            endTime: "05:00 PM",
            type: "virtual",
            roomIds: [],
          },
        ],
        { shouldDirty: true },
      );
    }
  };

  const addSlot = (dayIndex: number) => {
    const currentSlots = form.getValues(`workingHours.${dayIndex}.slots`);
    form.setValue(
      `workingHours.${dayIndex}.slots`,
      [
        ...currentSlots,
        {
          id: createSlotId(),
          startTime: "09:00 AM",
          endTime: "12:00 PM",
          type: "virtual",
          roomIds: [],
        },
      ],
      { shouldDirty: true },
    );
  };

  const removeSlot = (dayIndex: number, slotIndex: number) => {
    const currentSlots = form.getValues(`workingHours.${dayIndex}.slots`);
    form.setValue(
      `workingHours.${dayIndex}.slots`,
      currentSlots.filter((_, index) => index !== slotIndex),
      { shouldDirty: true },
    );
  };

  const copyToAll = (sourceDayIndex: number) => {
    const sourceSlots = form.getValues(`workingHours.${sourceDayIndex}.slots`);
    const allDays = form.getValues("workingHours");
    allDays.forEach((_, index) => {
      if (index === sourceDayIndex) return;
      form.setValue(`workingHours.${index}.active`, true, { shouldDirty: true });
      form.setValue(
        `workingHours.${index}.slots`,
        sourceSlots.map((slot) => ({ ...slot, id: createSlotId() })),
        { shouldDirty: true },
      );
    });
  };

  // Read during render, like every other profile section. react-hook-form only
  // keeps formState.isDirty current while something is subscribed to it, and a
  // subscription is what reading it creates. Reading it for the first time from
  // the isDirty() handle below is too late: Save Profile calls that handle after
  // the edit, and gets back the untouched initial false.
  const { isDirty } = form.formState;

  useImperativeHandle(
    ref,
    () => ({
      getValues: () => form.getValues(),
      isDirty: () => isDirty,
      trigger: () => form.trigger(),
      reset: (values) => form.reset(values),
      focusError: () => {
        const first = Object.keys(form.formState.errors)[0];
        if (first) {
          form.setFocus(first as keyof ConsultationScheduleFormValues);
        }
      },
    }),
    [form, isDirty],
  );

  // Re-hydrate when the API brings hours that differ from the ones this form was
  // last filled from. Resetting on every dependency change threw away an edit the
  // admin had not saved yet; gating on isDirty instead skipped mapping when the
  // form had merely been touched, so compare against what was actually hydrated.
  const hydratedFromRef = useRef<string | null>(null);

  useEffect(() => {
    const incoming = profileData?.consultationWorkingHours || "";
    if (hydratedFromRef.current === incoming) return;
    hydratedFromRef.current = incoming;

    form.reset({
      workingHours: parseConsultationWorkingHours(
        incoming,
        getEmptyWorkingDays(),
      ),
    });
  }, [
    form,
    getEmptyWorkingDays,
    profileData?.id,
    profileData?.consultationWorkingHours,
  ]);

  return (
    <FormProvider {...form}>
      <Form {...form}>
        <form className="flex flex-col gap-5 py-4">
          <div>
            <h3 className="text-(--neutral-950) text-[1.125rem] leading-6 font-semibold">
              Consultation / public site hours
            </h3>
            <p className="mt-1 text-sm text-(--text-neutral-600)">
              Separate from clinical Schedule. These hours are used only for
              public consultation booking on the marketing site.
            </p>
          </div>

          <div className="flex flex-col gap-5">
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
        </form>
      </Form>
    </FormProvider>
  );
});

export default ConsultationScheduleSection;
