import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomSelect from "@/components/form/CustomSelect";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import { Form } from "@/components/ui/form";
import type { AssessmentTemplate } from "../../pages/admin/content/content.static";
import {
  type AssignTemplateFormValues,
  assignTemplateFormSchema,
} from "@/schemas/admin-assessment.schemas";
import { useGetAdminClientsQuery } from "@/store/api/admin/clients.api";

interface AssignTemplateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: AssignTemplateFormValues & { templateId: string }) => void;
  template: AssessmentTemplate | null;
  isSubmitting?: boolean;
}

const AssignTemplateModal = ({
  isOpen,
  onClose,
  onSubmit,
  template,
  isSubmitting = false,
}: AssignTemplateModalProps) => {
  const minAssessmentYear = 2010;
  const maxAssessmentYear = new Date().getFullYear() + 10;
  const {
    data: clientsResponse,
    isLoading: isClientsLoading,
    isFetching: isClientsFetching,
  } = useGetAdminClientsQuery(
    { page: 1, pageSize: 500 },
    { skip: !isOpen },
  );

  const clientOptions = (clientsResponse?.items ?? []).map((client) => {
    const isClosedStage = (client.stage ?? "").trim().toLowerCase() === "closed";
    return {
      value: String(client.id),
      label: client.fullName || client.clientId || `Client #${client.id}`,
      disabled: isClosedStage,
    };
  });

  const form = useForm<AssignTemplateFormValues>({
    resolver: zodResolver(assignTemplateFormSchema),
    mode: "onChange",
    defaultValues: {
      clientId: "",
      dueDate: undefined,
      notes: "",
    },
  });

  const handleFormSubmit = (data: AssignTemplateFormValues) => {
    if (template) {
      onSubmit({ ...data, templateId: template.id });
    }
  };

  if (!isOpen || !template) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 backdrop-blur-[0.125rem] transition-all duration-300">
      <div className="w-full max-w-135 max-h-[calc(100vh-2rem)] bg-white rounded-[1.5rem] shadow-2xl mx-4 overflow-hidden animate-in fade-in zoom-in-95 duration-200 flex flex-col">
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(handleFormSubmit)}
            className="flex flex-col min-h-0 h-full"
          >
            {/* Header */}
            <div className="flex items-start justify-between px-8 pt-8 pb-2 shrink-0">
              <div className="space-y-1">
                <h2 className="text-[1.375rem] font-bold text-(--neutral-950)">
                  Assign Assessment
                </h2>
                <p className="text-sm text-(--text-neutral-600)">
                  Assign an assessment template to a client for completion.
                </p>
              </div>
              <button
                onClick={onClose}
                type="button"
                className="p-1 text-(--text-neutral-400) hover:text-(--neutral-950) transition-colors cursor-pointer rounded-full hover:bg-(--neutral-50)"
              >
                <X size={24} />
              </button>
            </div>

            {/* Scrollable Content */}
            <div className="px-8 py-6 space-y-6 overflow-y-auto min-h-0">
              {/* Selected Template Preview */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-(--neutral-950)">
                  Selected Template
                </label>
                <div className="p-5 rounded-2xl border border-(--neutral-100) bg-(--neutral-50)/30 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-base font-bold text-(--neutral-950)">
                      {template.title}
                    </h3>
                    <span className="px-3 py-1 bg-(--neutral-100) text-(--text-neutral-600) text-xs font-medium rounded-full">
                      {template.category}
                    </span>
                  </div>
                  <p className="text-xs text-(--text-neutral-400) leading-relaxed">
                    Lorem ipsum dolor sit amet orem ipsum dolor sit amet Lorem ipsum
                    dolor sit amet orem ipsum dolor sit amet
                  </p>
                </div>
              </div>

              <CustomSelect
                label="Select Client"
                name="clientId"
                control={form.control}
                options={clientOptions}
                required
                isSearch={true}
                disabled={isClientsLoading || isClientsFetching || isSubmitting}
              />

              <CustomDatePicker
                label="Due Date"
                name="dueDate"
                control={form.control}
                required
                disablePast
                minYear={minAssessmentYear}
                maxYear={maxAssessmentYear}
              />

              <div className="space-y-1">
                <CustomTextarea
                  label="Notes"
                  name="notes"
                  control={form.control}
                  placeholder=" "
                  className="min-h-24"
                />
                <p className="text-[0.6875rem] text-(--text-neutral-400) px-1">
                  Add any additional notes or instructions
                </p>
              </div>
            </div>

            {/* Fixed Footer */}
            <div className="flex justify-end items-center gap-4 px-8 py-5 border-t border-(--neutral-100) shrink-0 bg-white">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                className="px-10 py-3 h-auto border-(--neutral-100) text-(--neutral-950) rounded-full cursor-pointer text-base font-semibold transition-all duration-300 hover:bg-(--neutral-50)"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={
                  !form.formState.isValid ||
                  isSubmitting ||
                  isClientsLoading ||
                  isClientsFetching
                }
                className="px-8 py-3 h-auto bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/95 text-white rounded-full cursor-pointer text-base font-semibold transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
                loading={isSubmitting}
                loadingLabel="Assigning..."
              >
                Assign
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default AssignTemplateModal;
