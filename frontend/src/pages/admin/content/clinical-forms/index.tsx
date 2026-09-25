
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useCallback, useMemo, useEffect } from "react";

import ClinicalFormsToolbar from "../../../../components/admin-clinical-sections/ClinicalFormsToolbar";
import ClinicalFormsTable from "../../../../components/admin-clinical-sections/ClinicalFormsTable";
import CreateFormTemplateModal from "../../../../components/admin-clinical-sections/CreateFormTemplateModal";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { type FormEntry } from "../content.static";
import { useNavigate } from "react-router-dom";
import type { CreateFormTemplateValues } from "@/schemas/admin-clinical-schemas";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import {
  useCreateFormTemplateMutation,
  useDeleteFormTemplateMutation,
  useGetFormTemplatesQuery,
  useLazyGetFormTemplateByIdQuery,
  useUpdateFormTemplateMutation,
  type AdminFormTemplate,
} from "@/store/api/admin/clients.api";
import { getApiErrorMessage } from "@/utils/apiError";

const ITEMS_PER_PAGE = 20;
const FORM_CATEGORY_OPTIONS = [
  "consent",
  "intake",
  "release",
  "agreement",
  "safety",
  "discharge",
  "custom",
];

function normalizeTemplateCategory(value?: string): string {
  const normalized = value?.trim().toLowerCase() || "consent";
  return FORM_CATEGORY_OPTIONS.includes(normalized) ? normalized : "consent";
}

const AdminClinicalForms = ({ staffMode = false }: { staffMode?: boolean }) => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [category, setCategory] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [displayedForms, setDisplayedForms] = useState<FormEntry[]>([]);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<"create" | "edit">("create");
  const [editingTemplateId, setEditingTemplateId] = useState<number | null>(null);
  const [editingTemplateData, setEditingTemplateData] =
    useState<Partial<CreateFormTemplateValues> | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [templateToDeleteId, setTemplateToDeleteId] = useState<number | null>(null);
  const [updatingStatusId, setUpdatingStatusId] = useState<string | null>(null);
  const [sortConfig, setSortConfig] = useState<{
    key: string;
    direction: "asc" | "desc";
  }>({
    key: "createdAt",
    direction: "desc",
  });
  const [createFormTemplate, { isLoading: isCreatingTemplate }] =
    useCreateFormTemplateMutation();
  const [updateFormTemplate, { isLoading: isUpdatingTemplate }] =
    useUpdateFormTemplateMutation();
  const [deleteFormTemplate, { isLoading: isDeletingTemplate }] =
    useDeleteFormTemplateMutation();
  const [triggerGetFormTemplateById, { isFetching: isFetchingTemplateById }] =
    useLazyGetFormTemplateByIdQuery();

  const activeCategoryFilter = category === "all" ? undefined : category;
  const activeSearchQuery = searchQuery.trim() || undefined;

  const {
    data: formsResponse,
    isLoading: isFormsLoading,
    isFetching: isFormsFetching,
    isError: isFormsError,
    error: formsError,
    refetch: refetchForms,
  } = useGetFormTemplatesQuery(
    {
      page: currentPage,
      pageSize: ITEMS_PER_PAGE,
      search: activeSearchQuery,
      category: activeCategoryFilter,
    },
    { refetchOnMountOrArgChange: true },
  );

  const mappedFormsPage = useMemo(() => {
    const items = formsResponse?.items ?? [];
    return items.map((form: AdminFormTemplate) => ({
      id: String(form.id),
      name: form.name || "Untitled Form",
      category: form.category || "uncategorized",
      signature: form.requiresSignature ? "Required" : "Optional",
      status: form.isActive ? "Active" : "Inactive",
      createdAt: form.createdAt,
    })) as FormEntry[];
  }, [formsResponse]);

  useEffect(() => {
    if (currentPage === 1) {
      setDisplayedForms(mappedFormsPage);
      return;
    }

    setDisplayedForms((prev) => {
      const existingIds = new Set(prev.map((form) => form.id));
      const uniqueIncoming = mappedFormsPage.filter((form) => !existingIds.has(form.id));
      return [...prev, ...uniqueIncoming];
    });
  }, [currentPage, mappedFormsPage]);

  useEffect(() => {
    if (isFormsError && formsError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(formsError));
    }
  }, [isFormsError, formsError]);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const handleSort = (key: string) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === "asc" ? "desc" : "asc",
    }));
  };

  const handleToggleStatus = async (id: string) => {
    const target = displayedForms.find((form) => form.id === id);
    if (!target) return;

    const templateId = Number(id);
    if (!templateId) return;

    setUpdatingStatusId(id);
    try {
      await updateFormTemplate({
        id: templateId,
        body: {
          name: target.name.trim(),
          category: normalizeTemplateCategory(target.category),
          requiresSignature: target.signature === "Required",
          isActive: target.status !== "Active",
        },
      }).unwrap();

      setToastType("success");
      setToastMessage(
        target.status === "Active"
          ? "Form template disabled successfully."
          : "Form template enabled successfully.",
      );
      setCurrentPage(1);
      await refetchForms();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setUpdatingStatusId(null);
    }
  };

  const handleDelete = (id: string) => {
    const templateId = Number(id);
    if (!templateId) return;
    setTemplateToDeleteId(templateId);
    setIsDeleteModalOpen(true);
  };

  const handleEdit = async (id: string) => {
    const templateId = Number(id);
    if (!templateId) return;

    setModalMode("edit");
    setEditingTemplateId(templateId);
    setEditingTemplateData(null);
    setIsCreateModalOpen(true);

    try {
      const details = await triggerGetFormTemplateById(templateId).unwrap();
      setEditingTemplateData({
        formName: details.name || "",
        category: normalizeTemplateCategory(details.category),
        description: details.description || "",
        instructions: details.instructions || "",
        requiresSignature: Boolean(details.requiresSignature),
        active: details.isActive !== false,
        isSystemTemplate: Boolean(details.isSystemTemplate),
        sortOrder: details.sortOrder ? String(details.sortOrder) : "",
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setIsCreateModalOpen(false);
      setModalMode("create");
      setEditingTemplateId(null);
      setEditingTemplateData(null);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!templateToDeleteId) return;
    try {
      await deleteFormTemplate(templateToDeleteId).unwrap();
      setToastType("success");
      setToastMessage("Form template deleted successfully.");
      setIsDeleteModalOpen(false);
      setTemplateToDeleteId(null);
      setCurrentPage(1);
      await refetchForms();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleNewTemplate = () => {
    setModalMode("create");
    setEditingTemplateId(null);
    setEditingTemplateData(null);
    setIsCreateModalOpen(true);
  };

  const handleCreateTemplate = async (data: CreateFormTemplateValues) => {
    try {
      const basePayload = {
        name: data.formName.trim(),
        category: data.category.trim(),
        requiresSignature: data.requiresSignature,
        description: data.description?.trim() || undefined,
        instructions: data.instructions?.trim() || undefined,
        isActive: data.active,
        isSystemTemplate: data.isSystemTemplate,
        sortOrder: data.sortOrder?.trim()
          ? Number.parseInt(data.sortOrder.trim(), 10)
          : undefined,
      };

      if (modalMode === "edit" && editingTemplateId) {
        await updateFormTemplate({
          id: editingTemplateId,
          body: basePayload,
        }).unwrap();
        setToastType("success");
        setToastMessage("Form template updated successfully.");
      } else {
        await createFormTemplate(basePayload).unwrap();
        setToastType("success");
        setToastMessage("Form template created successfully.");
      }

      setIsCreateModalOpen(false);
      setModalMode("create");
      setEditingTemplateId(null);
      setEditingTemplateData(null);
      setCurrentPage(1);
      await refetchForms();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleConsent = (id: string) => {
    const basePath = staffMode
      ? "/staff/content/clinical-forms/consent"
      : "/admin/content/clinical-forms/consent";
    navigate(`${basePath}/${id}`);
  };

  const handleLoadMore = useCallback(() => {
    const totalPages = formsResponse?.totalPages ?? 1;
    if (isFormsFetching || currentPage >= totalPages) return;
    setCurrentPage((prev) => prev + 1);
  }, [currentPage, formsResponse?.totalPages, isFormsFetching, setCurrentPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: currentPage < (formsResponse?.totalPages ?? 1),
    isLoading: isFormsFetching,
  });

  useEffect(() => {
    setCurrentPage(1);
  }, [activeSearchQuery, activeCategoryFilter, setCurrentPage]);

  const sortedForms = useMemo(() => {
    const data = [...displayedForms];
    const direction = sortConfig.direction === "asc" ? 1 : -1;
    data.sort((a, b) => {
      if (sortConfig.key === "createdAt") {
        const aTime = a.createdAt ? new Date(a.createdAt).getTime() : Number(a.id);
        const bTime = b.createdAt ? new Date(b.createdAt).getTime() : Number(b.id);
        return (aTime - bTime) * direction;
      }
      if (sortConfig.key === "name") {
        return a.name.localeCompare(b.name) * direction;
      }
      if (sortConfig.key === "signature") {
        return a.signature.localeCompare(b.signature) * direction;
      }
      if (sortConfig.key === "status") {
        return a.status.localeCompare(b.status) * direction;
      }
      return 0;
    });
    return data;
  }, [displayedForms, sortConfig]);

  return (
    <div className="flex h-[calc(100vh-10.5rem)] min-h-0 flex-col overflow-hidden">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <ScrollToTopButton />
      <ClinicalFormsToolbar
        searchQuery={searchQuery}
        setSearchQuery={(query) => {
          setSearchQuery(query);
        }}
        category={category}
        setCategory={(cat) => {
          setCategory(cat);
        }}
        showNewTemplate={!staffMode}
        onNewTemplate={staffMode ? undefined : handleNewTemplate}
      />

      <div className="min-h-0 flex-1 overflow-y-auto pr-1">
        <ClinicalFormsTable
          forms={sortedForms}
          isLoading={isFormsLoading && currentPage === 1}
          updatingStatusId={updatingStatusId}
          readOnly={staffMode}
          onEdit={staffMode ? undefined : handleEdit}
          onDelete={staffMode ? undefined : handleDelete}
          onToggleStatus={staffMode ? undefined : handleToggleStatus}
          onSort={handleSort}
          sortConfig={sortConfig}
          onConsent={staffMode ? undefined : handleConsent}
        />

        <div
          ref={observerTarget}
          className="mt-4 flex h-10 w-full items-center justify-center"
        >
          {isFormsFetching && currentPage > 1 && (
            <ContentLoader variant="inline" size="md" />
          )}
        </div>
      </div>

      {!staffMode ? (
        <>
          <CreateFormTemplateModal
            isOpen={isCreateModalOpen}
            onClose={() => {
              if (isCreatingTemplate || isUpdatingTemplate || isFetchingTemplateById) return;
              setIsCreateModalOpen(false);
              setModalMode("create");
              setEditingTemplateId(null);
              setEditingTemplateData(null);
            }}
            onCreate={handleCreateTemplate}
            isSubmitting={isCreatingTemplate || isUpdatingTemplate}
            mode={modalMode}
            isFetching={isFetchingTemplateById && !editingTemplateData}
            initialValues={editingTemplateData}
          />

          <ConfirmationModal
            type="delete"
            isOpen={isDeleteModalOpen}
            onClose={() => {
              if (isDeletingTemplate) return;
              setIsDeleteModalOpen(false);
              setTemplateToDeleteId(null);
            }}
            onConfirm={() => {
              void handleDeleteConfirm();
            }}
            title="Delete form template"
            description="Are you sure you want to delete this form template? This action cannot be undone."
            items={[]}
            confirmButtonText="Delete template"
            confirmButtonLoading={isDeletingTemplate}
            confirmButtonLoadingText="Deleting..."
          />
        </>
      ) : null}
    </div>
  );
};

export default AdminClinicalForms;
