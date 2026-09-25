
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useMemo, useCallback, useEffect, useRef } from "react";

import AssessmentTabs from "@/components/admin-assessment-sections/AssessmentTabs";
import AssessmentFilters from "@/components/admin-assessment-sections/AssessmentFilters";
import TemplateCard from "@/components/admin-assessment-sections/TemplateCard";
import AssignmentCard from "@/components/admin-assessment-sections/AssignmentCard";
import CreateTemplateModal from "@/components/admin-assessment-sections/CreateTemplateModal";
import AssignTemplateModal from "@/components/admin-assessment-sections/AssignTemplateModal";
import DeleteAssessmentModal from "@/components/admin-assessment-sections/DeleteAssessmentModal";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  ASSESSMENT_TABS,
  type AssessmentTemplate,
  type ActiveAssignment,
} from "../content.static";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useCreateAssessmentTemplateMutation,
  useDeleteAssessmentTemplateMutation,
  useGetAssessmentTemplatesQuery,
  useGetAssessmentAssignmentsQuery,
  useLazyGetAssessmentTemplateByIdQuery,
  useUpdateAssessmentTemplateMutation,
  useAssignClientAssessmentMutation,
  useUpdateAssessmentAssignmentStatusMutation,
  type AdminAssessmentTemplate,
  type AdminClientAssessment,
} from "@/store/api/admin/clients.api";
import { getApiErrorMessage } from "@/utils/apiError";
import type { CreateTemplateFormValues } from "@/schemas/admin-assessment.schemas";
import type { AssignTemplateFormValues } from "@/schemas/admin-assessment.schemas";
import EmptyAssessmentsState from "@/components/assessments/EmptyAssessmentsState";

const ITEMS_PER_PAGE = 20;

export type AssessmentAccessControl = {
  canView: boolean;
  canManageTemplates: boolean;
  canAssign: boolean;
  canChangeAssignmentStatus: boolean;
  buildAssessmentPath: string;
};

const DEFAULT_ASSESSMENT_ACCESS: AssessmentAccessControl = {
  canView: true,
  canManageTemplates: true,
  canAssign: true,
  canChangeAssignmentStatus: true,
  buildAssessmentPath: "/admin/content/assessment/create-assessment",
};

const STAFF_ASSESSMENT_ACCESS: AssessmentAccessControl = {
  canView: true,
  canManageTemplates: false,
  canAssign: false,
  canChangeAssignmentStatus: false,
  buildAssessmentPath: "/staff/content/assessment/create-assessment",
};

type AdminAssessmentProps = {
  staffMode?: boolean;
  assessmentAccess?: AssessmentAccessControl;
};

const AdminAssessment = ({ staffMode = false, assessmentAccess }: AdminAssessmentProps) => {
  const access =
    assessmentAccess ?? (staffMode ? STAFF_ASSESSMENT_ACCESS : DEFAULT_ASSESSMENT_ACCESS);
  const [activeTab, setActiveTab] = useState(ASSESSMENT_TABS[0]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedFilter, setSelectedFilter] = useState("All Categories");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [templateModalMode, setTemplateModalMode] = useState<"create" | "edit">("create");
  const [editingTemplateId, setEditingTemplateId] = useState<number | null>(null);
  const [editingTemplateData, setEditingTemplateData] =
    useState<Partial<CreateTemplateFormValues> | null>(null);

  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<AssessmentTemplate | null>(null);
  const [templateToDelete, setTemplateToDelete] = useState<AssessmentTemplate | null>(null);

  const [displayedTemplates, setDisplayedTemplates] = useState<AssessmentTemplate[]>([]);
  const [currentPage, setCurrentPage] = useState(1);

  const [displayedAssignments, setDisplayedAssignments] = useState<ActiveAssignment[]>([]);
  const [assignmentsPage, setAssignmentsPage] = useState(1);
  const [updatingAssignmentId, setUpdatingAssignmentId] = useState<string | null>(null);
  const templatesScrollRef = useRef<HTMLDivElement>(null);
  const assignmentsScrollRef = useRef<HTMLDivElement>(null);
  const activeScrollRef =
    activeTab === ASSESSMENT_TABS[1] ? assignmentsScrollRef : templatesScrollRef;

  const {
    data: templatesResponse,
    isLoading: isTemplatesLoading,
    isFetching: isTemplatesFetching,
    isError: isTemplatesError,
    error: templatesError,
    refetch: refetchTemplates,
  } = useGetAssessmentTemplatesQuery(
    { page: currentPage, pageSize: ITEMS_PER_PAGE },
    { refetchOnMountOrArgChange: true },
  );

  const assignmentsStatusFilter = useMemo(
    () => {
      if (activeTab !== ASSESSMENT_TABS[1] || selectedFilter === "All Statuses") {
        return undefined;
      }
      if (selectedFilter === "In Progress") return "client_in_progress";
      return selectedFilter.toLowerCase();
    },
    [activeTab, selectedFilter],
  );

  const assignmentsSearchFilter = useMemo(
    () => (activeTab === ASSESSMENT_TABS[1] ? searchQuery.trim() || undefined : undefined),
    [activeTab, searchQuery],
  );

  const {
    data: assignmentsResponse,
    isLoading: isAssignmentsLoading,
    isFetching: isAssignmentsFetching,
    isError: isAssignmentsError,
    error: assignmentsError,
    refetch: refetchAssignments,
  } = useGetAssessmentAssignmentsQuery(
    {
      page: assignmentsPage,
      pageSize: ITEMS_PER_PAGE,
      status: assignmentsStatusFilter,
      search: assignmentsSearchFilter,
    },
    { refetchOnMountOrArgChange: true },
  );

  const [createAssessmentTemplate, { isLoading: isCreatingTemplate }] =
    useCreateAssessmentTemplateMutation();
  const [updateAssessmentTemplate, { isLoading: isUpdatingTemplate }] =
    useUpdateAssessmentTemplateMutation();
  const [deleteAssessmentTemplate, { isLoading: isDeletingTemplate }] =
    useDeleteAssessmentTemplateMutation();
  const [triggerGetTemplateById, { isFetching: isFetchingTemplateById }] =
    useLazyGetAssessmentTemplateByIdQuery();
  const [assignClientAssessment, { isLoading: isAssigningAssessment }] =
    useAssignClientAssessmentMutation();
  const [updateAssessmentAssignmentStatus] =
    useUpdateAssessmentAssignmentStatusMutation();

  const mappedTemplatesPage = useMemo(() => {
    const items = templatesResponse?.items ?? [];
    return items.map((template: AdminAssessmentTemplate) => ({
      id: String(template.id),
      title: template.name || template.title || "Untitled Assessment",
      category: template.category || "Uncategorized",
      sections: template.sectionsCount ?? 0,
      createdBy: "Admin",
      version: String(template.version ?? 1),
      type: template.isStandardized ? "Standard" : "Custom",
    })) satisfies AssessmentTemplate[];
  }, [templatesResponse]);

  const mappedAssignmentsPage = useMemo(() => {
    const items = assignmentsResponse?.items ?? [];
    return items.map((assignment: AdminClientAssessment) => ({
      // Normalize snake_case API statuses like client_in_progress.
      status: (() => {
        const normalized = assignment.status?.trim().toLowerCase().replace(/[_\s]+/g, " ");
        if (normalized === "completed") return "Completed";
        if (normalized === "in progress" || normalized === "client in progress") {
          return "In Progress";
        }
        return "Pending";
      })() as "Pending" | "In Progress" | "Completed",
      id: String(assignment.id),
      title: assignment.templateName || `Template #${assignment.templateId}`,
      assignedTo: {
        id: assignment.clientId ? `CL-${assignment.clientId.toString().padStart(4, "0")}` : "---",
        name: assignment.clientName || "Unknown Client",
      },
      assignedBy: assignment.assignedByName || "Admin",
      dateAssigned: assignment.assignedDate ? new Date(assignment.assignedDate).toLocaleDateString("en-US", { month: "short", day: "2-digit", year: "numeric" }) : "---",
      dueDate: assignment.dueDate ? new Date(assignment.dueDate).toLocaleDateString("en-US", { month: "short", day: "2-digit", year: "numeric" }) : "---",
    })) satisfies ActiveAssignment[];
  }, [assignmentsResponse]);

  useEffect(() => {
    if (assignmentsPage === 1) {
      setDisplayedAssignments(mappedAssignmentsPage);
      return;
    }

    setDisplayedAssignments((prev) => {
      const existingIds = new Set(prev.map((a) => a.id));
      const uniqueIncoming = mappedAssignmentsPage.filter((a) => !existingIds.has(a.id));
      return [...prev, ...uniqueIncoming];
    });
  }, [assignmentsPage, mappedAssignmentsPage]);

  useEffect(() => {
    if (activeTab !== ASSESSMENT_TABS[1]) return;
    setAssignmentsPage(1);
  }, [activeTab, searchQuery, selectedFilter]);

  const templateCategoryOptions = useMemo(() => {
    const categories = new Set<string>(["All Categories"]);
    displayedTemplates.forEach((template) => {
      const normalized = template.category?.trim();
      if (normalized) {
        categories.add(normalized);
      }
    });

    return Array.from(categories).map((category) => ({
      value: category,
      label: category,
    }));
  }, [displayedTemplates]);

  useEffect(() => {
    if (currentPage === 1) {
      setDisplayedTemplates(mappedTemplatesPage);
      return;
    }

    setDisplayedTemplates((prev) => {
      const existingIds = new Set(prev.map((template) => template.id));
      const uniqueIncoming = mappedTemplatesPage.filter((template) => !existingIds.has(template.id));
      return [...prev, ...uniqueIncoming];
    });
  }, [currentPage, mappedTemplatesPage]);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const filteredTemplates = useMemo(() => {
    return displayedTemplates.filter((template) => {
      const query = searchQuery.trim().toLowerCase();
      const matchesSearch =
        !query ||
        template.title.toLowerCase().includes(query) ||
        template.category.toLowerCase().includes(query);
      const matchesCategory =
        selectedFilter === "All Categories" || template.category === selectedFilter;
      return matchesSearch && matchesCategory;
    });
  }, [displayedTemplates, searchQuery, selectedFilter]);

  const filteredAssignments = useMemo(() => {
    return displayedAssignments;
  }, [displayedAssignments]);

  const currentData = useMemo(() => {
    return activeTab === "Templates" ? filteredTemplates : filteredAssignments;
  }, [activeTab, filteredTemplates, filteredAssignments]);

  const totalItems = currentData.length;

  const handleLoadMore = useCallback(() => {
    if (activeTab === "Templates") {
      const totalPages = templatesResponse?.totalPages ?? 1;
      if (isTemplatesFetching || currentPage >= totalPages) return;
      setCurrentPage((prev) => prev + 1);
    } else {
      const totalPages = assignmentsResponse?.totalPages ?? 1;
      if (isAssignmentsFetching || assignmentsPage >= totalPages) return;
      setAssignmentsPage((prev) => prev + 1);
    }
  }, [activeTab, currentPage, isTemplatesFetching, templatesResponse?.totalPages, assignmentsPage, isAssignmentsFetching, assignmentsResponse?.totalPages, setCurrentPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore:
      activeTab === "Templates"
        ? currentPage < (templatesResponse?.totalPages ?? 1)
        : assignmentsPage < (assignmentsResponse?.totalPages ?? 1),
    isLoading: activeTab === "Templates" ? isTemplatesFetching : isAssignmentsFetching,
  });

  const counts = {
    [ASSESSMENT_TABS[0]]: templatesResponse?.totalCount ?? displayedTemplates.length,
    [ASSESSMENT_TABS[1]]: assignmentsResponse?.totalCount ?? displayedAssignments.length,
  };

  const resetTemplateModalState = () => {
    setTemplateModalMode("create");
    setEditingTemplateId(null);
    setEditingTemplateData(null);
  };

  const handleOpenCreateModal = () => {
    resetTemplateModalState();
    setIsCreateModalOpen(true);
  };

  const handleAssign = (template: AssessmentTemplate) => {
    setSelectedTemplate(template);
    setIsAssignModalOpen(true);
  };

  const handleEdit = async (template: AssessmentTemplate) => {
    const templateId = Number(template.id);
    if (!templateId) return;

    setTemplateModalMode("edit");
    setEditingTemplateId(templateId);
    setEditingTemplateData(null);
    setIsCreateModalOpen(true);

    try {
      const details = await triggerGetTemplateById(templateId).unwrap();
      setEditingTemplateData({
        templateName: details.name || details.title || "",
        category: details.category || "Clinical",
        description: details.description || "",
        version: String(details.version ?? 1),
        isStandardized: Boolean(details.isStandardized),
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setIsCreateModalOpen(false);
      resetTemplateModalState();
    }
  };

  const handleDelete = (template: AssessmentTemplate) => {
    setTemplateToDelete(template);
    setIsDeleteModalOpen(true);
  };

  const handleTemplateSubmit = async (data: CreateTemplateFormValues) => {
    try {
      if (templateModalMode === "edit" && editingTemplateId) {
        await updateAssessmentTemplate({
          id: editingTemplateId,
          body: {
            name: data.templateName.trim(),
            description: data.description.trim() || undefined,
            category: data.category.trim() || undefined,
            isStandardized: data.isStandardized,
            version: Number.parseFloat(data.version) || 1,
          },
        }).unwrap();
        setToastType("success");
        setToastMessage("Assessment template updated successfully.");
      } else {
        await createAssessmentTemplate({
          name: data.templateName.trim(),
          description: data.description.trim() || undefined,
          category: data.category.trim() || undefined,
          isStandardized: data.isStandardized,
          version: Number.parseFloat(data.version) || 1,
        }).unwrap();
        setToastType("success");
        setToastMessage("Assessment template created successfully.");
      }

      setCurrentPage(1);
      await refetchTemplates();
      resetTemplateModalState();
      return true;
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      return false;
    }
  };

  const handleDeleteConfirm = async () => {
    const templateId = Number(templateToDelete?.id);
    if (!templateId) return;

    try {
      await deleteAssessmentTemplate(templateId).unwrap();
      setToastType("success");
      setToastMessage("Assessment template deleted successfully.");
      setIsDeleteModalOpen(false);
      setTemplateToDelete(null);
      setCurrentPage(1);
      await refetchTemplates();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleAssignSubmit = async (data: AssignTemplateFormValues & { templateId: string }) => {
    try {
      await assignClientAssessment({
        templateId: Number(data.templateId),
        clientId: Number(data.clientId),
        dueDate: new Date(data.dueDate).toISOString(),
        notes: data.notes || undefined,
      }).unwrap();
      
      // Close modal and clear selection first
      setIsAssignModalOpen(false);
      setSelectedTemplate(null);
      
      // Then show success toast
      setToastType("success");
      setToastMessage("Assessment assigned successfully.");
    } catch (error) {
      console.error("Assignment error:", error);
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleAssignmentStatusChange = async (
    assignmentId: string,
    status: ActiveAssignment["status"],
  ) => {
    const parsedId = Number(assignmentId);
    if (!parsedId) return;

    const statusMap: Record<ActiveAssignment["status"], string> = {
      Pending: "PENDING",
      "In Progress": "CLIENT_IN_PROGRESS",
      Completed: "COMPLETED",
    };

    setUpdatingAssignmentId(assignmentId);
    setToastType("info");
    setToastMessage("Changing assessment status...");
    try {
      await updateAssessmentAssignmentStatus({
        id: parsedId,
        body: {
          status: statusMap[status] ?? "PENDING",
          notes: `Status changed to ${status}`,
        },
      }).unwrap();
      await refetchAssignments();
      setToastType("success");
      setToastMessage("Assessment status updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setUpdatingAssignmentId(null);
    }
  };

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden gap-6">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <ScrollToTopButton />

      <div className="flex shrink-0 flex-col gap-6">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <AssessmentTabs
            activeTab={activeTab}
            onTabChange={(tab: string) => {
              setActiveTab(tab);
              setSearchQuery("");
              setSelectedFilter(tab === "Templates" ? "All Categories" : "All Statuses");
              if (tab === "Templates") {
                setCurrentPage(1);
              } else {
                setAssignmentsPage(1);
                void refetchAssignments();
              }
            }}
            counts={counts}
            showNewTemplate={access.canManageTemplates}
            onNewTemplate={access.canManageTemplates ? handleOpenCreateModal : undefined}
          />
        </div>

        <AssessmentFilters
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          selectedFilter={selectedFilter}
          onFilterChange={setSelectedFilter}
          categoryOptions={templateCategoryOptions}
          activeTab={activeTab}
          scrollContainerRef={activeScrollRef}
        />
      </div>

      {activeTab === ASSESSMENT_TABS[1] ? (
        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
          <div
            ref={assignmentsScrollRef}
            className="min-h-0 flex-1 overflow-y-auto overscroll-contain pb-4"
          >
            {isAssignmentsLoading && assignmentsPage === 1 ? (
              <div className="flex justify-center py-20">
                <ContentLoader variant="inline" size="md" />
              </div>
            ) : isAssignmentsError ? (
              <div className="py-20 text-center text-red-500">
                {getApiErrorMessage(assignmentsError)}
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
                  {(currentData as ActiveAssignment[]).map((assignment) => (
                    <AssignmentCard
                      key={assignment.id}
                      assignment={assignment}
                      scrollContainerRef={assignmentsScrollRef}
                      onStatusChange={
                        access.canChangeAssignmentStatus
                          ? (id, nextStatus) => {
                              void handleAssignmentStatusChange(id, nextStatus);
                            }
                          : undefined
                      }
                      isUpdating={updatingAssignmentId === assignment.id}
                    />
                  ))}
                </div>

                {totalItems === 0 && (
                  <EmptyAssessmentsState
                    title="No assessments found"
                    description="Assessment assignments will appear here"
                  />
                )}

                <div
                  ref={observerTarget}
                  className="mt-4 flex h-10 w-full items-center justify-center"
                >
                  {isAssignmentsFetching && assignmentsPage > 1 && (
                    <ContentLoader variant="inline" size="md" />
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      ) : (
        <div
          ref={templatesScrollRef}
          className="flex-1 overflow-y-auto pb-4 no-scrollbar"
        >
          {isTemplatesLoading && currentPage === 1 ? (
            <div className="py-20 flex justify-center">
              <ContentLoader variant="inline" size="md" />
            </div>
          ) : isTemplatesError ? (
            <div className="py-20 text-center text-red-500">
              {getApiErrorMessage(templatesError)}
            </div>
          ) : null}

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {(currentData as AssessmentTemplate[]).map((template) => (
              <TemplateCard
                key={template.id}
                template={template}
                scrollContainerRef={templatesScrollRef}
                readOnly={!access.canManageTemplates}
                buildAssessmentPath={access.buildAssessmentPath}
                onAssign={access.canAssign ? handleAssign : undefined}
                onEdit={access.canManageTemplates ? handleEdit : undefined}
                onDelete={access.canManageTemplates ? handleDelete : undefined}
              />
            ))}
          </div>

          {totalItems === 0 && (
            <EmptyAssessmentsState
              title="No assessments found"
              description="Assessment templates will appear here"
            />
          )}

          <div
            ref={observerTarget}
            className="h-10 w-full flex items-center justify-center mt-4"
          >
            {isTemplatesFetching && currentPage > 1 && (
              <ContentLoader variant="inline" size="md" />
            )}
          </div>
        </div>
      )}

      {access.canManageTemplates || access.canAssign ? (
        <>
          {access.canManageTemplates ? (
            <CreateTemplateModal
              isOpen={isCreateModalOpen}
              onClose={() => {
                if (isCreatingTemplate || isUpdatingTemplate || isFetchingTemplateById) return;
                setIsCreateModalOpen(false);
                resetTemplateModalState();
              }}
              onSubmit={handleTemplateSubmit}
              isSubmitting={isCreatingTemplate || isUpdatingTemplate}
              mode={templateModalMode}
              initialValues={editingTemplateData}
            />
          ) : null}

          {access.canAssign ? (
            <AssignTemplateModal
              isOpen={isAssignModalOpen}
              onClose={() => setIsAssignModalOpen(false)}
              template={selectedTemplate}
              onSubmit={handleAssignSubmit}
              isSubmitting={isAssigningAssessment}
            />
          ) : null}

          {access.canManageTemplates ? (
            <DeleteAssessmentModal
              isOpen={isDeleteModalOpen}
              onClose={() => {
                if (isDeletingTemplate) return;
                setIsDeleteModalOpen(false);
                setTemplateToDelete(null);
              }}
              templateTitle={templateToDelete?.title || ""}
              onConfirm={() => {
                void handleDeleteConfirm();
              }}
              type="Assessment"
              isSubmitting={isDeletingTemplate}
            />
          ) : null}
        </>
      ) : null}
    </div>
  );
};

export default AdminAssessment;
