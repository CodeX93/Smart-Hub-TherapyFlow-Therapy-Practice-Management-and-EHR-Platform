
import { ContentLoader } from "@/components/shared/ContentLoader";
import { FileText, Plus } from "lucide-react";
import { useState } from "react";
import Toast from "@/components/shared/Toast";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import UploadTemplateModal from "@/components/admin-report-templates/UploadTemplateModal";
import EditTemplateModal from "@/components/admin-report-templates/EditTemplateModal";
import {
  useCreateReportTemplateMutation,
  useDeleteReportTemplateMutation,
  useGetReportTemplatesQuery,
  useUpdateReportTemplateMutation,
  type ReportTemplate,
} from "@/store/api/admin/reportTemplates.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { truncateForDisplay } from "@/utils/reportTemplateValidation";

const AdminReportTemplates = () => {
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<ReportTemplate | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ReportTemplate | null>(null);
  const [togglingTemplateId, setTogglingTemplateId] = useState<number | null>(null);
  const [toast, setToast] = useState<{ message: string; type: "success" | "error" } | null>(null);

  const { data: templates = [], isLoading, refetch } = useGetReportTemplatesQuery(true);
  const [createTemplate, { isLoading: isCreating }] = useCreateReportTemplateMutation();
  const [updateTemplate, { isLoading: isUpdating }] = useUpdateReportTemplateMutation();
  const [deleteTemplate, { isLoading: isDeleting }] = useDeleteReportTemplateMutation();

  const showToast = (message: string, type: "success" | "error") => setToast({ message, type });

  const handleToggleActive = async (template: ReportTemplate, nextActive: boolean) => {
    if (togglingTemplateId === template.id) return;

    try {
      setTogglingTemplateId(template.id);
      await updateTemplate({
        id: template.id,
        isActive: nextActive,
      }).unwrap();
      showToast(nextActive ? "Template activated" : "Template deactivated", "success");
      refetch();
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to update template", "error");
    } finally {
      setTogglingTemplateId(null);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteTemplate(deleteTarget.id).unwrap();
      showToast("Template deleted", "success");
      setDeleteTarget(null);
      refetch();
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to delete template", "error");
    }
  };

  const deleteDescription = deleteTarget
    ? `Are you sure you want to delete "${truncateForDisplay(deleteTarget.name, 72)}"? Existing client reports will keep their snapshot name.`
    : "";

  return (
    <div className="flex h-full min-h-0 flex-col gap-4">
      <div className="flex flex-col gap-3 rounded-2xl border border-(--neutral-100) bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-(--bg-primary-50) text-(--text-primary-dark)">
            <FileText size={18} />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-(--text-primary-dark)">
              Upload a report layout
            </p>
            <p className="text-xs text-(--text-neutral-600)">
              Supported formats: DOCX and PDF
            </p>
          </div>
        </div>
        <Button
          type="button"
          onClick={() => setIsUploadOpen(true)}
          className="h-10 w-full cursor-pointer gap-2 rounded-full px-4 font-medium sm:w-auto"
        >
          <Plus size={16} />
          Upload Template
        </Button>
      </div>

      {isLoading ? (
        <ContentLoader />
      ) : templates.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-(--neutral-200) bg-white p-10 text-center text-(--text-neutral-600)">
          No report templates yet. Upload your first template to get started.
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {templates.map((template) => (
            <div
              key={template.id}
              className="flex min-w-0 flex-col overflow-hidden rounded-2xl border border-(--neutral-200) bg-white p-5 shadow-sm"
            >
              <div className="mb-3 flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex min-w-0 items-center gap-2">
                    <FileText size={18} className="shrink-0 text-(--text-neutral-600)" />
                    <h2
                      className="truncate text-lg font-medium text-(--neutral-950)"
                      title={template.name}
                    >
                      {template.name}
                    </h2>
                  </div>
                  {template.originalName ? (
                    <p
                      className="mt-1 truncate text-xs text-(--text-neutral-500)"
                      title={template.originalName}
                    >
                      {template.originalName}
                    </p>
                  ) : null}
                </div>
                <div className="flex shrink-0 items-center">
                  {togglingTemplateId === template.id ? (
                    <ContentLoader variant="inline" size="md" />
                  ) : (
                    <Switch
                      checked={Boolean(template.isActive)}
                      onCheckedChange={(checked) => void handleToggleActive(template, checked)}
                      aria-label={
                        template.isActive ? `Deactivate ${template.name}` : `Activate ${template.name}`
                      }
                    />
                  )}
                </div>
              </div>

              {template.description ? (
                <p
                  className="mb-3 line-clamp-2 break-words text-sm text-(--text-neutral-600)"
                  title={template.description}
                >
                  {template.description}
                </p>
              ) : null}

              <div className="mb-4 flex flex-wrap gap-2 text-xs text-(--text-neutral-600)">
                <span
                  className={
                    template.isActive
                      ? "rounded-full bg-(--bg-primary-50) px-2 py-1 text-(--text-primary-dark)"
                      : "rounded-full bg-(--neutral-100) px-2 py-1"
                  }
                >
                  {template.isActive ? "Active" : "Inactive"}
                </span>
                {template.supportingFilesExpected ? (
                  <span className="rounded-full bg-amber-50 px-2 py-1 text-amber-700">
                    Supporting files expected
                  </span>
                ) : null}
              </div>

              <div className="mt-auto flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setEditingTemplate(template)}
                  className="h-9 rounded-full border-(--neutral-200) px-3 text-sm text-(--neutral-950)"
                >
                  Edit
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setDeleteTarget(template)}
                  className="h-9 rounded-full border-red-200 px-3 text-sm text-red-600 hover:bg-red-50"
                >
                  Delete
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <UploadTemplateModal
        isOpen={isUploadOpen}
        isSubmitting={isCreating}
        onClose={() => setIsUploadOpen(false)}
        onSubmit={async (payload) => {
          try {
            await createTemplate(payload).unwrap();
            showToast("Template uploaded", "success");
            setIsUploadOpen(false);
            refetch();
          } catch (error) {
            showToast(getApiErrorMessage(error) || "Failed to upload template", "error");
          }
        }}
      />

      <EditTemplateModal
        template={editingTemplate}
        isSubmitting={isUpdating}
        onClose={() => setEditingTemplate(null)}
        onSubmit={async (payload) => {
          if (!editingTemplate) return;
          try {
            await updateTemplate({ id: editingTemplate.id, ...payload }).unwrap();
            showToast("Template updated", "success");
            setEditingTemplate(null);
            refetch();
          } catch (error) {
            showToast(getApiErrorMessage(error) || "Failed to update template", "error");
          }
        }}
      />

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(deleteTarget)}
        title="Delete template?"
        description={deleteDescription}
        items={[]}
        confirmButtonText="Delete"
        confirmButtonLoading={isDeleting}
        onConfirm={handleDelete}
        onClose={() => setDeleteTarget(null)}
      />

      {toast ? (
        <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />
      ) : null}
    </div>
  );
};

export default AdminReportTemplates;
