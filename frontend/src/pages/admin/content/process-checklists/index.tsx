
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useEffect, useCallback, useRef } from "react";
import ChecklistToolbar from "@/components/admin-process-checklists/ChecklistToolbar";
import ProcessChecklistCard from "@/components/admin-process-checklists/ProcessChecklistCard";
import CreateTemplatePanel from "@/components/admin-process-checklists/CreateTemplatePanel";
import ChecklistItemsPanel from "@/components/admin-process-checklists/ChecklistItemsPanel";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import Toast from "../../../../components/shared/Toast";
import {
  useGetChecklistTemplatesQuery,
  useCreateChecklistTemplateMutation,
  useUpdateChecklistTemplateMutation,
  useLazyGetChecklistTemplateByIdQuery,
  useDeleteChecklistTemplateMutation,
  type ChecklistTemplate,
  type CreateChecklistTemplatePayload,
} from "@/store/api/admin/checklists.api";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";

import { getApiErrorMessage } from "@/utils/apiError";
import {
  type ProcessChecklistTemplate,
} from "../content.static";
import { type CreateTemplateValues } from "@/schemas/admin-checklist.schemas";
import EmptyChecklistsState from "@/components/admin/clients/ClientProfile/Checklists/EmptyChecklistsState";

const AdminProcessCheckLists = ({ staffMode = false }: { staffMode?: boolean }) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All Categories");
  const [allChecklists, setAllChecklists] = useState<ProcessChecklistTemplate[]>(
    [],
  );
  const [page, setPage] = useState(1);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const {
    data: checklistsResponse,
    isFetching,
  } = useGetChecklistTemplatesQuery({
    page,
    pageSize: 15,
    search: searchQuery.trim() || undefined,
    category: selectedCategory,
  });

  const [createChecklistTemplate, { isLoading: isCreating }] =
    useCreateChecklistTemplateMutation();
  const [updateChecklistTemplate, { isLoading: isUpdating }] =
    useUpdateChecklistTemplateMutation();
  const [deleteChecklistTemplate, { isLoading: isDeletingTemplate }] =
    useDeleteChecklistTemplateMutation();
  const [getTemplateById, { isFetching: isFetchingTemplate }] =
    useLazyGetChecklistTemplateByIdQuery();

  const [editingTemplateId, setEditingTemplateId] = useState<number | null>(
    null,
  );
  const [editInitialData, setEditInitialData] =
    useState<CreateTemplateValues | null>(null);
  const [expandedChecklistId, setExpandedChecklistId] = useState<string | null>(
    null,
  );
  const [itemToDelete, setItemToDelete] = useState<{
    checklistId: string;
    itemId: string;
  } | null>(null);
  const [isDeleteItemModalOpen, setIsDeleteItemModalOpen] = useState(false);

  const [templateToDeleteId, setTemplateToDeleteId] = useState<string | null>(
    null,
  );
  const [isDeleteTemplateModalOpen, setIsDeleteTemplateModalOpen] =
    useState(false);
  const listScrollRef = useRef<HTMLDivElement>(null);

  const totalPages = checklistsResponse
    ? Math.ceil(checklistsResponse.totalCount / 15)
    : 0;
  const hasMore = page < totalPages;

  const mapApiToUi = useCallback(
    (template: ChecklistTemplate): ProcessChecklistTemplate => ({
      id: String(template.id),
      title: template.name,
      description: template.description || "",
      itemCount: template.itemCount ?? template.items?.length ?? 0,
      items: (template.items || []).map((item) => ({
        id: String(item.id),
        title: item.title,
        description: item.description,
        category: item.category as ProcessChecklistTemplate["items"][number]["category"],
        required: item.isRequired,
      })),
    }),
    [],
  );

  useEffect(() => {
    if (checklistsResponse?.items) {
      if (page === 1) {
        setAllChecklists(checklistsResponse.items.map(mapApiToUi));
      } else {
        setAllChecklists((prev) => [
          ...prev,
          ...checklistsResponse.items
            .map(mapApiToUi)
            .filter((item) => !prev.some((p) => p.id === item.id)),
        ]);
      }
    }
  }, [checklistsResponse, page, mapApiToUi]);

  useEffect(() => {
    setPage(1);
  }, [searchQuery, selectedCategory, setPage]);

  const { observerTarget } = useInfiniteScroll({
    hasMore,
    isLoading: isFetching,
    onLoadMore: () => setPage((prev) => prev + 1),
  });

  const [isCreatePanelOpen, setIsCreatePanelOpen] = useState(false);
  const [isChecklistItemsPanelOpen, setIsChecklistItemsPanelOpen] =
    useState(false);

  const handleCreateClick = () => {
    setEditingTemplateId(null);
    setEditInitialData(null);
    setIsCreatePanelOpen(true);
  };

  const handleEditChecklist = async (checklistId: string) => {
    const id = Number(checklistId);
    if (!id) return;

    setEditingTemplateId(id);
    setEditInitialData(null);
    setIsCreatePanelOpen(true);

    try {
      const data = await getTemplateById(id).unwrap();
      if (data) {
        const mapCategory = (cat: string): ProcessChecklistTemplate["items"][number]["category"] => {
          const c = cat.toUpperCase();
          if (c === "INTAKE") return "Intake";
          if (c === "ASSESSMENT") return "Assessment";
          if (c === "ONGOING") return "Ongoing";
          if (c === "DISCHARGE") return "Discharge";
          return "Intake";
        };

        setEditInitialData({
          templateName: data.name,
          description: data.description || "",
          customItems: (data.items || []).map((item) => ({
            id: String(item.id),
            title: item.title,
            category: mapCategory(item.category),
            description: item.description || "",
            required: item.isRequired,
          })),
        });
      }
    } catch (err) {
      setToastMessage(getApiErrorMessage(err));
      setIsCreatePanelOpen(false);
    }
  };

  const handleDuplicateChecklist = async (checklistId: string) => {
    const id = Number(checklistId);
    if (!id) return;

    try {
      setToastMessage("Duplicating template...");
      const data = await getTemplateById(id).unwrap();
      if (!data) throw new Error("Template not found");

      const clonedItems = (data.items || []).map((item, index) => ({
        title: item.title,
        category: item.category,
        isRequired: item.isRequired,
        itemOrder: index,
        daysFromStart: 0,
        sortOrder: index,
        description: item.description || "",
      }));

      const payload: CreateChecklistTemplatePayload = {
        name: data.name,
        description: data.description || "",
        clientType: data.clientType || "General",
        isActive: true,
        sortOrder: 0,
        items: clonedItems,
      };

      await createChecklistTemplate(payload).unwrap();
      setToastMessage("Template duplicated successfully.");
      
      // Reset page to 1 to refetch from start
      setPage(1);
    } catch (err) {
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleDeleteChecklist = (checklistId: string) => {
    setTemplateToDeleteId(checklistId);
    setIsDeleteTemplateModalOpen(true);
  };

  const handleDeleteTemplateConfirm = async () => {
    if (!templateToDeleteId) return;

    const id = Number(templateToDeleteId);
    if (!id) return;

    try {
      await deleteChecklistTemplate(id).unwrap();
      setToastMessage("Checklist template deleted successfully.");
      setIsDeleteTemplateModalOpen(false);
      setTemplateToDeleteId(null);
      
      // Reset page to 1 to refetch from start
      setPage(1);
    } catch (err) {
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleDeleteItemRequest = (checklistId: string, itemId: string) => {
    setItemToDelete({ checklistId, itemId });
    setIsDeleteItemModalOpen(true);
  };

  const handleDeleteItemConfirm = async () => {
    if (!itemToDelete) return;

    const { checklistId, itemId } = itemToDelete;
    const id = Number(checklistId);
    if (!id) return;

    try {
      const detailedData = await getTemplateById(id).unwrap();
      if (!detailedData) throw new Error("Template not found");

      const filteredItems = (detailedData.items || [])
        .filter((item) => String(item.id) !== itemId)
        .map((item, index) => ({
          title: item.title,
          category: item.category,
          isRequired: item.isRequired,
          itemOrder: index,
          daysFromStart: 0,
          sortOrder: index,
          description: item.description || "",
        }));

      await updateChecklistTemplate({
        id,
        body: {
          name: detailedData.name,
          description: detailedData.description,
          clientType: detailedData.clientType,
          isActive: detailedData.isActive,
          sortOrder: detailedData.sortOrder,
          items: filteredItems,
        },
      }).unwrap();

      setToastMessage("Item deleted successfully.");
      setIsDeleteItemModalOpen(false);
      setItemToDelete(null);
    } catch (err) {
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const handleItemsReorder = async (checklistId: string, newItems: ProcessChecklistTemplate["items"]) => {
    const id = Number(checklistId);
    if (!id) return;

    const checklist = allChecklists.find((c) => c.id === checklistId);
    if (!checklist) return;

    const mappedItems = newItems.map((item, index) => ({
      title: item.title,
      category: item.category.toUpperCase(),
      isRequired: item.required,
      itemOrder: index,
      daysFromStart: 0,
      sortOrder: index,
      description: item.description || "",
    }));

    try {
      await updateChecklistTemplate({
        id,
        body: {
          name: checklist.title,
          description: checklist.description,
          clientType: "General",
          isActive: true,
          sortOrder: 0,
          items: mappedItems,
        },
      }).unwrap();
    } catch {
      setToastMessage("Failed to update item order.");
    }
  };

  const displayedChecklists = allChecklists;

  const handleCreateTemplate = async (data: CreateTemplateValues) => {
    try {
      const items = data.customItems.map((item, idx) => ({
        title: item.title,
        category: item.category || "General",
        isRequired: item.required,
        description: item.description,
        itemOrder: idx,
        daysFromStart: 0,
        sortOrder: idx,
      }));

      const payload: CreateChecklistTemplatePayload = {
        name: data.templateName,
        description: data.description,
        clientType: "General",
        isActive: true,
        sortOrder: 0,
        items,
      };

      if (editingTemplateId) {
        await updateChecklistTemplate({
          id: editingTemplateId,
          body: payload,
        }).unwrap();
        setToastMessage("Checklist template updated successfully.");
      } else {
        await createChecklistTemplate(payload).unwrap();
        setToastMessage("Checklist template created successfully.");
      }
      setIsCreatePanelOpen(false);
    } catch (err) {
      setToastMessage(getApiErrorMessage(err));
    }
  };

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden gap-6">
      <div className="shrink-0">
        <ChecklistToolbar
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          selectedCategory={selectedCategory}
          setSelectedCategory={setSelectedCategory}
          showCreateTemplate={!staffMode}
          onCreateClick={staffMode ? undefined : handleCreateClick}
          onChecklistItemsClick={
            staffMode ? undefined : () => setIsChecklistItemsPanelOpen(true)
          }
          scrollContainerRef={listScrollRef}
        />
      </div>

      <div
        ref={listScrollRef}
        className="min-h-0 flex-1 overflow-y-auto overscroll-contain pb-4"
      >
        {isFetching && page === 1 ? (
          <div className="py-20 flex justify-center">
            <ContentLoader variant="inline" size="md" />
          </div>
        ) : (
          <div className="space-y-4">
            {displayedChecklists.map((checklist) => (
              <ProcessChecklistCard
                key={checklist.id}
                checklist={checklist}
                readOnly={staffMode}
                isExpanded={expandedChecklistId === checklist.id}
                onToggle={() =>
                  setExpandedChecklistId(
                    expandedChecklistId === checklist.id ? null : checklist.id,
                  )
                }
                onEdit={staffMode ? undefined : () => handleEditChecklist(checklist.id)}
                onDuplicate={
                  staffMode ? undefined : () => handleDuplicateChecklist(checklist.id)
                }
                onDelete={staffMode ? undefined : () => handleDeleteChecklist(checklist.id)}
                onDeleteItem={
                  staffMode
                    ? undefined
                    : (itemId) => handleDeleteItemRequest(checklist.id, itemId)
                }
                onReorderItems={
                  staffMode
                    ? undefined
                    : (newItems) => handleItemsReorder(checklist.id, newItems)
                }
              />
            ))}

            {isFetching && page > 1 && (
              <div className="py-4 flex justify-center">
                <ContentLoader variant="inline" size="md" />
              </div>
            )}

            <div ref={observerTarget} className="h-4" />

            {displayedChecklists.length === 0 && !isFetching && (
              <EmptyChecklistsState
                title="No checklists found"
                description="Process checklists will appear here"
              />
            )}
          </div>
        )}
      </div>

      {toastMessage && (
        <Toast
          message={toastMessage}
          onClose={() => setToastMessage(null)}
          type={
            toastMessage === "Duplicating template..."
              ? "info"
              : toastMessage.toLowerCase().includes("success")
                ? "success"
                : "error"
          }
          duration={3000}
        />
      )}

      {!staffMode ? (
        <>
          <CreateTemplatePanel
            isOpen={isCreatePanelOpen}
            onClose={() => setIsCreatePanelOpen(false)}
            onSubmit={handleCreateTemplate}
            isLoading={isCreating || isUpdating}
            isFetching={isFetchingTemplate && !editInitialData}
            initialData={editInitialData}
            mode={editingTemplateId ? "edit" : "create"}
          />

          <ChecklistItemsPanel
            isOpen={isChecklistItemsPanelOpen}
            onClose={() => setIsChecklistItemsPanelOpen(false)}
          />

          <ConfirmationModal
            type="delete"
            isOpen={isDeleteItemModalOpen}
            onClose={() => {
              setIsDeleteItemModalOpen(false);
              setItemToDelete(null);
            }}
            onConfirm={handleDeleteItemConfirm}
            title="Delete Checklist Item"
            description="Are you sure you want to delete this item? This action cannot be undone."
            confirmButtonText="Delete"
            confirmButtonLoading={isUpdating}
            items={[]}
          />

          <ConfirmationModal
            type="delete"
            isOpen={isDeleteTemplateModalOpen}
            onClose={() => {
              setIsDeleteTemplateModalOpen(false);
              setTemplateToDeleteId(null);
            }}
            onConfirm={handleDeleteTemplateConfirm}
            title="Delete Checklist Template"
            description="Are you sure you want to delete this checklist template? This action cannot be undone and will permanently remove the template and all its items."
            confirmButtonText="Delete Template"
            confirmButtonLoading={isDeletingTemplate}
            items={[]}
          />
        </>
      ) : null}
    </div>
  );
};

export default AdminProcessCheckLists;
