import React, { useEffect } from "react";
import { ArrowLeft } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "../ui/button";
import { Form } from "../ui/form";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import CustomTextarea from "../form/CustomTextarea";
import type { NotificationTemplate } from "@/types/notification";
import {
  notificationTemplateSchema,
  type NotificationTemplateSchema,
} from "@/schemas/notification.schema";

interface CreateTemplateSidePanelProps {
  isOpen: boolean;
  onClose: () => void;
  initialData?: NotificationTemplate | null;
  isSubmitting?: boolean;
  onSubmitTemplate: (data: NotificationTemplateSchema) => void | Promise<void>;
}

const TEMPLATE_NAME_MAX_LENGTH = 100;
const TEMPLATE_SUBJECT_MAX_LENGTH = 200;
const TEMPLATE_MESSAGE_MAX_LENGTH = 5000;

const CreateTemplateSidePanel: React.FC<CreateTemplateSidePanelProps> = ({
  isOpen,
  onClose,
  initialData,
  isSubmitting = false,
  onSubmitTemplate,
}) => {
  const form = useForm<NotificationTemplateSchema>({
    resolver: zodResolver(notificationTemplateSchema),
    defaultValues: {
      templateName: initialData?.title || "",
      subject: initialData?.subject || "",
      message: initialData?.description || "",
      status: initialData?.isActive === false ? "inactive" : "active",
    },
  });

  useEffect(() => {
    form.reset({
      templateName: initialData?.title || "",
      subject: initialData?.subject || "",
      message: initialData?.description || "",
      status: initialData?.isActive === false ? "inactive" : "active",
    });
  }, [form, initialData]);

  if (!isOpen) return null;

  const onSubmit = async (data: NotificationTemplateSchema) => {
    await onSubmitTemplate(data);
  };

  return (
    <Form {...form}>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/20 z-50 transition-opacity animate-in fade-in"
        onClick={onClose}
      />

      {/* Side Panel */}
      <div className="fixed top-0 right-0 h-full w-full md:w-170 bg-white shadow-2xl z-50 animate-in slide-in-from-right duration-300 flex flex-col">
        {/* Header */}
        <div className="px-6 py-5 border-b border-(--neutral-100) flex items-center gap-3">
          <button
            onClick={onClose}
            type="button"
            className="text-(--text-neutral-600) hover:bg-(--neutral-100) rounded-full p-1 transition-colors cursor-pointer"
          >
            <ArrowLeft size={20} />
          </button>
          <h2 className="text-(--text-primary-dark) font-semibold text-lg">
            {initialData
              ? "Edit Notification Template"
              : "Create Notification Template"}
          </h2>
        </div>

        {/* Content */}
        <form
          id="template-form"
          onSubmit={form.handleSubmit(onSubmit)}
          className="flex-1 overflow-y-auto p-6 space-y-6"
        >
          <CustomInput
            control={form.control}
            name="templateName"
            label="Template Name"
            required
            disabled={Boolean(initialData)}
            maxLength={TEMPLATE_NAME_MAX_LENGTH}
            hint={`${form.watch("templateName")?.length ?? 0}/${TEMPLATE_NAME_MAX_LENGTH}`}
          />

          <CustomInput
            control={form.control}
            name="subject"
            label="Notification Subject"
            maxLength={TEMPLATE_SUBJECT_MAX_LENGTH}
            hint={`${form.watch("subject")?.length ?? 0}/${TEMPLATE_SUBJECT_MAX_LENGTH}`}
          />

          <CustomTextarea
            control={form.control}
            name="message"
            label="Message"
            className="min-h-40"
            maxLength={TEMPLATE_MESSAGE_MAX_LENGTH}
            hint={`${form.watch("message")?.length ?? 0}/${TEMPLATE_MESSAGE_MAX_LENGTH}`}
          />

          <CustomSelect
            control={form.control}
            name="status"
            label="Status"
            options={[
              { label: "Active", value: "active" },
              { label: "Inactive", value: "inactive" },
            ]}
            isSearch={false}
          />
        </form>

        {/* Footer */}
        <div className="p-6 pt-2 flex justify-end gap-3">
          <Button
            variant="outline"
            onClick={onClose}
            type="button"
            className="rounded-full px-6 h-12 border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--neutral-50) hover:text-(--text-primary-dark) cursor-pointer"
          >
            Cancel
          </Button>
          <Button
            type="submit"
            form="template-form"
            className="rounded-full px-6 h-12 bg-(--bg-primary-dark) text-white hover:bg-(--bg-primary-dark)/90 shadow-none cursor-pointer"
            disabled={!form.watch("templateName") || isSubmitting}
            loading={isSubmitting}
            loadingLabel="Saving..."
          >
            {initialData ? "Update Template" : "Create Template"}
          </Button>
        </div>
      </div>
    </Form>
  );
};

export default CreateTemplateSidePanel;
