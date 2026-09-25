
import { ContentLoader } from "@/components/shared/ContentLoader";
import React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { Button } from "../ui/button";
import CustomSelect from "../form/CustomSelect";
import CustomTextarea from "../form/CustomTextarea";
import { cn } from "@/lib/utils";
import { Form } from "../ui/form";
import {
  assignSupervisorSchema,
  type AssignSupervisorFormValues,
} from "@/schemas/user-access-profiles.schema";
import CustomDatePicker from "../form/CustomDatePicker";
import type { CustomSelectOption } from "../form/CustomSelect";

interface AssignSupervisorModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAssign: (data: AssignSupervisorFormValues) => void;
  title?: string;
  submitLabel?: string;
  isSubmitting?: boolean;
  isLoadingDetails?: boolean;
  supervisorOptions: CustomSelectOption[];
  therapistOptions: CustomSelectOption[];
  assignmentTypeOptions: CustomSelectOption[];
  frequencyOptions: CustomSelectOption[];
  initialValues?: Partial<AssignSupervisorFormValues> | null;
  isEdit?: boolean;
  initialTherapistName?: string;
}

const emptyFormValues: AssignSupervisorFormValues = {
  supervisorId: "",
  therapistId: "",
  assignmentType: "PRIMARY",
  startDate: "",
  endDate: "",
  frequency: "WEEKLY",
  notes: "",
};

const AssignSupervisorModal: React.FC<AssignSupervisorModalProps> = ({
  isOpen,
  onClose,
  onAssign,
  title = "Assign Supervisor",
  submitLabel = "Assign",
  isSubmitting = false,
  isLoadingDetails = false,
  supervisorOptions,
  therapistOptions,
  assignmentTypeOptions,
  frequencyOptions,
  initialValues,
  isEdit = false,
}) => {
  const minSupervisorYear = 2010;
  const maxSupervisorYear = new Date().getFullYear() + 10;
  const form = useForm<AssignSupervisorFormValues>({
    resolver: zodResolver(assignSupervisorSchema),
    defaultValues: emptyFormValues,
    mode: "onChange",
  });
  const selectedStartDate = form.watch("startDate");

  const lastSeedKeyRef = React.useRef<string | null>(null);
  const hasEditSeedData = Boolean(
    initialValues?.supervisorId && initialValues?.therapistId,
  );
  const showDetailsLoader = isEdit && (isLoadingDetails || !hasEditSeedData);

  React.useEffect(() => {
    if (!isOpen) {
      lastSeedKeyRef.current = null;
      form.reset(emptyFormValues);
      return;
    }

    // Edit mode: wait for fetched assignment details before seeding.
    if (isEdit) {
      if (isLoadingDetails || !hasEditSeedData) return;
    }

    const nextSeedKey = isEdit
      ? [
          initialValues?.supervisorId ?? "",
          initialValues?.therapistId ?? "",
          initialValues?.assignmentType ?? "",
          initialValues?.startDate ?? "",
          initialValues?.endDate ?? "",
          initialValues?.frequency ?? "",
          initialValues?.notes ?? "",
        ].join("|")
      : "create";

    if (lastSeedKeyRef.current === nextSeedKey) return;

    form.reset({
      supervisorId: initialValues?.supervisorId ?? "",
      therapistId: initialValues?.therapistId ?? "",
      assignmentType: initialValues?.assignmentType ?? "PRIMARY",
      startDate: initialValues?.startDate ?? "",
      endDate: initialValues?.endDate ?? "",
      frequency: initialValues?.frequency ?? "WEEKLY",
      notes: initialValues?.notes ?? "",
    });
    lastSeedKeyRef.current = nextSeedKey;
  }, [form, hasEditSeedData, initialValues, isEdit, isLoadingDetails, isOpen]);

  if (!isOpen) return null;

  const onSubmit = (values: AssignSupervisorFormValues) => {
    onAssign(values);
  };

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/50 p-2 md:p-4">
      <div className="w-full max-w-150 max-h-[95vh] bg-white rounded-[1.5rem] shadow-xl overflow-hidden relative animate-in fade-in zoom-in duration-200 flex flex-col">
        <div className="sticky top-0 z-10 shrink-0 rounded-t-[1.5rem] bg-white px-5 pt-5 pb-2 md:px-8 md:pt-6">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <h3 className="text-xl font-semibold text-(--text-primary-dark)">
                {title}
              </h3>
              <p className="mt-1 text-(--text-neutral-600) text-sm font-normal">
                Link a supervisor to a therapist for clinical oversight.
              </p>
            </div>
            <button
              onClick={onClose}
              className="shrink-0 text-(--text-neutral-600) hover:text-(--neutral-950) transition-colors cursor-pointer p-1 hover:bg-(--bg-primary-50) rounded-full"
            >
              <X size={24} />
            </button>
          </div>
        </div>

        <div className="p-5 pt-3 md:px-8 md:pb-8 md:pt-3 overflow-y-auto custom-scrollbar flex-1">

          {showDetailsLoader ? (
            <ContentLoader className="min-h-48 py-16" />
          ) : (
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit(onSubmit)}
                className="flex flex-col gap-5"
              >
                <CustomSelect
                  control={form.control}
                  name="supervisorId"
                  label="Supervisor"
                  required
                  options={supervisorOptions}
                  disabled={isSubmitting}
                />

                <CustomSelect
                  control={form.control}
                  name="therapistId"
                  label="Therapist"
                  required
                  options={therapistOptions}
                  disabled={isSubmitting || isEdit}
                />

                <CustomSelect
                  control={form.control}
                  name="assignmentType"
                  label="Assignment Type"
                  options={assignmentTypeOptions}
                  required
                  disabled={isSubmitting}
                />

                <div className="grid grid-cols-2 gap-5">
                  <CustomDatePicker
                    control={form.control}
                    name="startDate"
                    label="Start Date"
                    disabled={isSubmitting}
                    disablePast
                    minYear={minSupervisorYear}
                    maxYear={maxSupervisorYear}
                  />
                  <CustomDatePicker
                    control={form.control}
                    name="endDate"
                    label="End Date"
                    disabled={isSubmitting}
                    disablePast
                    minYear={minSupervisorYear}
                    maxYear={maxSupervisorYear}
                    minDate={selectedStartDate || null}
                  />
                </div>

                <CustomSelect
                  control={form.control}
                  name="frequency"
                  label="Meeting Frequency"
                  options={frequencyOptions}
                  required
                  disabled={isSubmitting}
                />

                <CustomTextarea
                  control={form.control}
                  name="notes"
                  label="Notes"
                  hint="Additional supervisor notes"
                />
              </form>
            </Form>
          )}

        </div>

        <div className="shrink-0 border-t border-(--neutral-100) bg-white p-5 md:px-8 md:py-5">
          <div className="flex justify-end gap-3">
            <Button
              variant="outline"
              className="rounded-full px-8 h-11.5 border-(--neutral-200) text-(--neutral-950) hover:bg-(--neutral-50) font-semibold transition-all cursor-pointer"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              className={cn(
                "rounded-full px-8 h-11.5 font-semibold transition-all cursor-pointer border-none",
                !form.formState.isValid || isSubmitting || showDetailsLoader
                  ? "bg-(--neutral-100) text-(--text-neutral-400)"
                  : "bg-(--bg-primary-dark) text-white hover:opacity-90",
              )}
              onClick={form.handleSubmit(onSubmit)}
              disabled={!form.formState.isValid || isSubmitting || showDetailsLoader}
              loading={isSubmitting}
              loadingLabel={`${submitLabel}...`}
            >
              {submitLabel}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AssignSupervisorModal;
