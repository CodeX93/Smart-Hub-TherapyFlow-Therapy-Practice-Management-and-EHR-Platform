
import { ContentLoader } from "@/components/shared/ContentLoader";
import { Plus } from "lucide-react";
import { Button } from "../ui/button";
import { useMemo, useState } from "react";
import TemplateListing from "./TemplateListing";
import CreateTemplateSidePanel from "./CreateTemplateSidePanel";
import Toast from "../shared/Toast";
import ConfirmationModal from "../shared/ConfirmationModal";
import type { NotificationTemplate } from "../../types/notification";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  useDeleteSuperAdminNotificationTemplateByKeyMutation,
  useGetSuperAdminNotificationTemplatesQuery,
  useUpdateSuperAdminNotificationTemplateByKeyMutation,
} from "@/store/api/superAdminApi";
import type { NotificationTemplateSchema } from "@/schemas/notification.schema";

function toDisplayDate(value?: string): string {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleString("en-US", {
      month: "short",
      day: "2-digit",
      year: "numeric",
    });
  } catch {
    return value;
  }
}

function toTemplateKey(raw: string): string {
  return raw
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

const TemplateTab = () => {
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [selectedTemplate, setSelectedTemplate] =
    useState<NotificationTemplate | null>(null);
  const [templateToDelete, setTemplateToDelete] =
    useState<NotificationTemplate | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");
  const { data, isLoading, isError, error, refetch } =
    useGetSuperAdminNotificationTemplatesQuery();
  const [updateTemplate, { isLoading: isSaving }] =
    useUpdateSuperAdminNotificationTemplateByKeyMutation();
  const [deleteTemplate, { isLoading: isDeleting }] =
    useDeleteSuperAdminNotificationTemplateByKeyMutation();

  const templates = useMemo(
    () =>
      (data ?? []).map((template) => ({
        id: String(template.id),
        templateKey: template.templateKey,
        title: template.templateKey,
        tag: template.isActive ? "Active" : "Inactive",
        description: template.bodyTemplate || "",
        createdDate: toDisplayDate(template.updatedAt || template.createdAt),
        subject: template.subjectTemplate || "",
        isActive: template.isActive,
      })),
    [data]
  );

  const handleCreate = () => {
    setSelectedTemplate(null);
    setIsPanelOpen(true);
  };

  const handleEdit = (template: NotificationTemplate) => {
    setSelectedTemplate(template);
    setIsPanelOpen(true);
  };

  const handleDelete = (template: NotificationTemplate) => {
    setTemplateToDelete(template);
  };

  const handleConfirmDelete = async () => {
    const templateKey = templateToDelete?.templateKey?.trim();
    if (!templateKey) return;

    try {
      await deleteTemplate(templateKey).unwrap();
      setToastType("success");
      setToastMessage("Template deleted successfully.");
      await refetch();
      setTemplateToDelete(null);
    } catch (deleteError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(deleteError));
      setTemplateToDelete(null);
    }
  };

  const handleSaveTemplate = async (formData: NotificationTemplateSchema) => {
    const nextTemplateKey = selectedTemplate?.templateKey || toTemplateKey(formData.templateName);
    if (!nextTemplateKey) return;

    try {
      await updateTemplate({
        templateKey: nextTemplateKey,
        body: {
          subjectTemplate: formData.subject || "",
          bodyTemplate: formData.message || "",
          active: formData.status !== "inactive",
        },
      }).unwrap();
      setToastType("success");
      setToastMessage(
        selectedTemplate ? "Template updated successfully." : "Template created successfully."
      );
      await refetch();
      setIsPanelOpen(false);
    } catch (saveError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(saveError));
      throw saveError;
    }
  };

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto px-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-(--text-primary-dark) font-semibold">
            Notification Templates
          </h1>
          <Button
            onClick={handleCreate}
            className="bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full px-4 h-9 text-sm font-medium flex items-center gap-2 cursor-pointer transition-colors"
          >
            <Plus size={16} />
            Add Template
          </Button>
        </div>

        <div className="space-y-4 pb-20">
          {isLoading ? (
            <ContentLoader />
          ) : isError ? (
            <div className="rounded-xl border border-[#f3d4d4] bg-[#fff5f5] p-4 text-sm text-(--status-denied)">
              {getApiErrorMessage(error)}
            </div>
          ) : templates.length === 0 ? (
            <div className="rounded-xl border border-[#e3ebf3] bg-white p-4 text-sm text-[#667483]">
              No templates available.
            </div>
          ) : (
            templates.map((template) => (
              <TemplateListing
                key={template.id}
                template={template}
                onEdit={handleEdit}
                onDelete={handleDelete}
              />
            ))
          )}
        </div>
      </div>

      {isPanelOpen && (
        <CreateTemplateSidePanel
          isOpen={isPanelOpen}
          onClose={() => setIsPanelOpen(false)}
          initialData={selectedTemplate}
          onSubmitTemplate={handleSaveTemplate}
          isSubmitting={isSaving}
        />
      )}

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(templateToDelete)}
        onClose={() => setTemplateToDelete(null)}
        onConfirm={() => void handleConfirmDelete()}
        title={
          templateToDelete
            ? `Delete Template "${templateToDelete.title}"`
            : "Delete Template"
        }
        description="Are you sure you want to delete this notification template? This action cannot be undone."
        confirmButtonText="Delete template"
        confirmButtonLoading={isDeleting}
        confirmButtonLoadingText="Deleting..."
      />
    </div>
  );
};

export default TemplateTab;
