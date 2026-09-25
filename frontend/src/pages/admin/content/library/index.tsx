import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useEffect, useMemo, useState } from "react";
import { Plus, Search, Upload, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import CustomInput from "@/components/form/CustomInput";
import LibraryTabs from "@/components/admin-library-sections/LibraryTabs";
import LibraryEntryCard, {
  type LibraryEntryCardData,
} from "@/components/admin-library-sections/LibraryEntryCard";
import AddEntryModal from "@/components/admin-library-sections/AddEntryModal";
import BulkAddModal from "@/components/admin-library-sections/BulkAddModal";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import type { AddEntryFormData } from "@/schemas/admin-library.schema";
import {
  useBulkImportLibraryEntriesMutation,
  useCreateLibraryEntryMutation,
  useCreateLibraryConnectionMutation,
  useCreateLibraryConnectionsBatchMutation,
  useDeleteLibraryConnectionMutation,
  useDeleteAllLibraryEntryConnectionsMutation,
  useDeleteLibraryEntryMutation,
  useBulkDeleteLibraryEntriesMutation,
  useGetLibraryEntriesQuery,
  useGetLibraryEntriesWithConnectionsQuery,
  useGetLibraryTagsQuery,
  useLazyGetLibraryEntryConnectedQuery,
  useLazyGetLibraryEntryByIdQuery,
  useUpdateLibraryEntryMutation,
} from "@/store/api/admin/libraryEntries.api";
import { useGetLibraryCategoriesQuery } from "@/store/api/admin/libraryCategories.api";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import ExpandableText from "@/components/shared/ExpandableText";

const AdminLibrary = ({ canWriteEntries = true }: { canWriteEntries?: boolean }) => {
  const [activeTabId, setActiveTabId] = useState<string>("");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedEntries, setSelectedEntries] = useState<string[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  useEffect(() => {
    if (!toastMessage) return;
    const lower = toastMessage.toLowerCase();
    if (
      lower.includes("sending") ||
      lower.includes("preparing") ||
      lower.includes("started")
    ) {
      setToastType("info");
      return;
    }
    if (lower.startsWith("import completed")) {
      setToastType("info");
      return;
    }
    if (lower.startsWith("import successful")) {
      setToastType("success");
      return;
    }
    setToastType(lower.includes("success") ? "success" : "error");
  }, [toastMessage]);

  const [isAddEntryModalOpen, setIsAddEntryModalOpen] = useState(false);
  const [isBulkAddModalOpen, setIsBulkAddModalOpen] = useState(false);
  const [isBulkImportInProgress, setIsBulkImportInProgress] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [entryToDeleteId, setEntryToDeleteId] = useState<number | null>(null);
  const [isBulkDeleteModalOpen, setIsBulkDeleteModalOpen] = useState(false);
  const [isDeleteConnectionModalOpen, setIsDeleteConnectionModalOpen] = useState(false);
  const [connectionToDeleteId, setConnectionToDeleteId] = useState<number | null>(null);
  const [editingEntryId, setEditingEntryId] = useState<number | null>(null);
  const [editInitialData, setEditInitialData] = useState<AddEntryFormData | null>(null);

  const {
    data: categories = [],
    isLoading: isLoadingCategories,
    isFetching: isFetchingCategories,
    isError: isCategoriesError,
    error: categoriesError,
    refetch: refetchCategories,
  } = useGetLibraryCategoriesQuery(undefined, { refetchOnMountOrArgChange: true });

  const activeCategory = useMemo(
    () => categories.find((category) => String(category.id) === activeTabId) ?? null,
    [categories, activeTabId],
  );

  useEffect(() => {
    if (!activeTabId && categories.length > 0) {
      setActiveTabId(String(categories[0].id));
    }
  }, [activeTabId, categories]);

  const {
    data: entries = [],
    isLoading: isLoadingEntries,
    isFetching: isFetchingEntries,
    isError: isEntriesError,
    error: entriesError,
    refetch: refetchEntries,
  } = useGetLibraryEntriesQuery(
    activeCategory ? { categoryId: activeCategory.id } : undefined,
    { skip: !activeCategory, refetchOnMountOrArgChange: true },
  );
  const {
    data: entriesWithConnections = [],
    isFetching: isFetchingEntriesWithConnections,
    isError: isEntriesWithConnectionsError,
    error: entriesWithConnectionsError,
    refetch: refetchEntriesWithConnections,
  } = useGetLibraryEntriesWithConnectionsQuery(
    activeCategory ? { categoryId: activeCategory.id } : undefined,
    { skip: !activeCategory, refetchOnMountOrArgChange: true },
  );

  const [createLibraryEntry, { isLoading: isCreatingEntry }] = useCreateLibraryEntryMutation();
  const [bulkImportLibraryEntries] = useBulkImportLibraryEntriesMutation();
  const [createLibraryConnection] = useCreateLibraryConnectionMutation();
  const [createLibraryConnectionsBatch] = useCreateLibraryConnectionsBatchMutation();
  const [updateLibraryEntry, { isLoading: isUpdatingEntry }] = useUpdateLibraryEntryMutation();
  const [deleteLibraryEntry, { isLoading: isDeletingEntry }] = useDeleteLibraryEntryMutation();
  const [bulkDeleteLibraryEntries, { isLoading: isBulkDeletingEntries }] =
    useBulkDeleteLibraryEntriesMutation();
  const [deleteConnection, { isLoading: isDeletingConnection }] =
    useDeleteLibraryConnectionMutation();
  const [deleteAllConnectionsForEntry] = useDeleteAllLibraryEntryConnectionsMutation();
  const [triggerGetLibraryEntryById, { isFetching: isFetchingEntryDetails }] =
    useLazyGetLibraryEntryByIdQuery();
  const [triggerGetConnectedEntries] = useLazyGetLibraryEntryConnectedQuery();

  useGetLibraryTagsQuery();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    if (isCategoriesError && categoriesError) {
      setToastMessage(getApiErrorMessage(categoriesError));
    }
  }, [isCategoriesError, categoriesError]);

  useEffect(() => {
    if (isEntriesError && entriesError) {
      setToastMessage(getApiErrorMessage(entriesError));
    }
  }, [isEntriesError, entriesError]);
  useEffect(() => {
    if (isEntriesWithConnectionsError && entriesWithConnectionsError) {
      setToastMessage(getApiErrorMessage(entriesWithConnectionsError));
    }
  }, [isEntriesWithConnectionsError, entriesWithConnectionsError]);

  const tabs = useMemo(
    () => categories.filter((category) => category.isActive).sort((a, b) => a.sortOrder - b.sortOrder),
    [categories],
  );

  const categoryOptions = useMemo(
    () =>
      tabs.map((category) => ({
        value: String(category.id),
        label: category.name,
      })),
    [tabs],
  );

  const filteredEntries = useMemo(
    () =>
      entries.filter((entry) => {
        const query = searchQuery.trim().toLowerCase();
        if (!query) return true;
        const tags = entry.tags.join(" ").toLowerCase();
        return (
          entry.title.toLowerCase().includes(query) ||
          entry.content.toLowerCase().includes(query) ||
          tags.includes(query)
        );
      }),
    [entries, searchQuery],
  );

  const connectionsByEntryId = useMemo(() => {
    const map = new Map<number, (typeof entriesWithConnections)[number]["connectedEntries"]>();
    entriesWithConnections.forEach((item) => {
      map.set(item.entry.id, item.connectedEntries);
    });
    return map;
  }, [entriesWithConnections]);

  const mappedEntries: LibraryEntryCardData[] = useMemo(
    () =>
      filteredEntries.map((entry) => {
        const entryConnections = connectionsByEntryId.get(entry.id) ?? [];
        return {
          id: String(entry.id),
          code: `${entry.categoryName?.slice(0, 3).toUpperCase() || "LIB"}-${entry.id}`,
          title: entry.title,
          content: entry.content,
          usageCount: entry.usageCount ?? 0,
          connectedCount: entryConnections.length,
          connections: entryConnections.map((connection) => ({
            id: String(connection.connectionId),
            code: connection.entryTitle,
            category: connection.categoryName || connection.connectionType,
          })),
        };
      }),
    [connectionsByEntryId, filteredEntries],
  );

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedEntries(mappedEntries.map((entry) => entry.id));
    } else {
      setSelectedEntries([]);
    }
  };

  const handleSelectEntry = (id: string, checked: boolean) => {
    if (checked) {
      setSelectedEntries((prev) => [...prev, id]);
    } else {
      setSelectedEntries((prev) => prev.filter((entryId) => entryId !== id));
    }
  };

  const parseTags = (tags: string) =>
    tags
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean);

  const handleAddEntry = async (data: AddEntryFormData) => {
    try {
      const createdEntry = await createLibraryEntry({
        categoryId: Number(data.category),
        title: data.title.trim(),
        content: data.content.trim(),
        tags: parseTags(data.tags),
        sortOrder: Number(data.sortOrder || 0),
        isActive: true,
      }).unwrap();

      if (data.smartConnect.length > 0) {
        if (data.smartConnect.length === 1) {
          await createLibraryConnection({
            fromEntryId: createdEntry.id,
            toEntryId: Number(data.smartConnect[0]),
            connectionType: data.connectionType,
            strength: 5,
            description: data.connectionDescription.trim(),
          }).unwrap();
        } else {
          await createLibraryConnectionsBatch({
            connections: data.smartConnect.map((toEntryId) => ({
              fromEntryId: createdEntry.id,
              toEntryId: Number(toEntryId),
              connectionType: data.connectionType,
              strength: 5,
              description: data.connectionDescription.trim(),
            })),
          }).unwrap();
        }
      }
      setIsAddEntryModalOpen(false);
      setToastMessage("Library entry created successfully.");
      await refetchEntries();
      await refetchEntriesWithConnections();
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleOpenEdit = async (entryId: string) => {
    const numericId = Number(entryId);
    if (!numericId) return;
    setEditingEntryId(numericId);
    setEditInitialData(null);
    setIsAddEntryModalOpen(true);
    try {
      const entry = await triggerGetLibraryEntryById(numericId).unwrap();
      const connectedEntries = await triggerGetConnectedEntries(numericId).unwrap();
      setEditInitialData({
        title: entry.title,
        content: entry.content,
        category: String(entry.categoryId),
        tags: entry.tags.join(", "),
        sortOrder: entry.sortOrder ?? 0,
        smartConnect: connectedEntries.map((connected) => String(connected.entryId)),
        connectionType: "RELATED",
        connectionDescription: "",
      });
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
      setIsAddEntryModalOpen(false);
      setEditingEntryId(null);
    }
  };

  const handleUpdateEntry = async (data: AddEntryFormData) => {
    if (!editingEntryId) return;
    try {
      await updateLibraryEntry({
        id: editingEntryId,
        body: {
          categoryId: Number(data.category),
          title: data.title.trim(),
          content: data.content.trim(),
          tags: parseTags(data.tags),
          sortOrder: Number(data.sortOrder || 0),
          isActive: true,
        },
      }).unwrap();

      await deleteAllConnectionsForEntry(editingEntryId).unwrap();

      if (data.smartConnect.length > 0) {
        if (data.smartConnect.length === 1) {
          await createLibraryConnection({
            fromEntryId: editingEntryId,
            toEntryId: Number(data.smartConnect[0]),
            connectionType: data.connectionType,
            strength: 5,
            description: data.connectionDescription.trim(),
          }).unwrap();
        } else {
          await createLibraryConnectionsBatch({
            connections: data.smartConnect.map((toEntryId) => ({
              fromEntryId: editingEntryId,
              toEntryId: Number(toEntryId),
              connectionType: data.connectionType,
              strength: 5,
              description: data.connectionDescription.trim(),
            })),
          }).unwrap();
        }
      }

      setIsAddEntryModalOpen(false);
      setEditingEntryId(null);
      setEditInitialData(null);
      setToastMessage("Library entry updated successfully.");
      await refetchEntries();
      await refetchEntriesWithConnections();
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleDeleteEntry = (entryId: string) => {
    setEntryToDeleteId(Number(entryId));
    setIsDeleteModalOpen(true);
  };

  const confirmDeleteEntry = async () => {
    if (!entryToDeleteId) return;
    try {
      await deleteLibraryEntry(entryToDeleteId).unwrap();
      setIsDeleteModalOpen(false);
      setEntryToDeleteId(null);
      setSelectedEntries((prev) => prev.filter((id) => Number(id) !== entryToDeleteId));
      setToastMessage("Library entry deleted successfully.");
      await refetchEntries();
      await refetchEntriesWithConnections();
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleDeleteSelected = () => {
    if (selectedEntries.length === 0) return;
    setIsBulkDeleteModalOpen(true);
  };

  const confirmDeleteSelected = async () => {
    if (selectedEntries.length === 0) return;
    try {
      const result = await bulkDeleteLibraryEntries({
        entryIds: selectedEntries.map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0),
      }).unwrap();
      setIsBulkDeleteModalOpen(false);
      setSelectedEntries([]);
      if (result.failed > 0) {
        setToastMessage(
          `Deleted ${result.deleted} of ${result.total} entries. ${result.failed} failed.`,
        );
      } else {
        setToastMessage("Selected entries deleted successfully.");
      }
      await refetchEntries();
      await refetchEntriesWithConnections();
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleDeleteConnection = (connectionId: string) => {
    const numericId = Number(connectionId);
    if (!numericId) return;
    setConnectionToDeleteId(numericId);
    setIsDeleteConnectionModalOpen(true);
  };

  const confirmDeleteConnection = async () => {
    if (!connectionToDeleteId) return;
    try {
      await deleteConnection(connectionToDeleteId).unwrap();
      setIsDeleteConnectionModalOpen(false);
      setConnectionToDeleteId(null);
      setToastMessage("Connection removed successfully.");
      await refetchEntriesWithConnections();
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleDeleteAllConnections = async (entryId: string) => {
    const numericEntryId = Number(entryId);
    if (!numericEntryId) return;
    try {
      await deleteAllConnectionsForEntry(numericEntryId).unwrap();
      setToastMessage("All visible connections removed successfully.");
      await refetchEntries();
      await refetchEntriesWithConnections();
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleBulkImport = async (payload: {
    categoryId: number;
    entries: Array<{
      title: string;
      content: string;
      domain?: string;
      subdomain?: string;
    }>;
  }) => {
    setIsBulkImportInProgress(true);
    try {
      const result = await bulkImportLibraryEntries(payload).unwrap();
      await refetchEntries();
      await refetchEntriesWithConnections();
      await refetchCategories();
      const extras = [
        result.categoriesCreated > 0 ? `${result.categoriesCreated} categories created` : "",
        result.connectionsCreated > 0 ? `${result.connectionsCreated} connections created` : "",
      ]
        .filter(Boolean)
        .join(", ");
      const extrasSuffix = extras ? ` (${extras}).` : ".";
      if (result.failed > 0 || result.skipped > 0) {
        setToastType("info");
        setToastMessage(
          `Import completed with issues. Imported ${result.successful} entries. Skipped ${result.skipped}, failed ${result.failed}${extrasSuffix}`,
        );
      } else if (result.successful === 0) {
        setToastType("info");
        setToastMessage(`No entries were imported${extrasSuffix.replace(".", "")}`);
      } else {
        setToastType("success");
        setToastMessage(
          `Import successful. Imported ${result.successful} entries. Skipped ${result.skipped}, failed ${result.failed}${extrasSuffix}`,
        );
      }
      return result;
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      throw error;
    } finally {
      setIsBulkImportInProgress(false);
    }
  };

  const isAllSelected =
    mappedEntries.length > 0 && selectedEntries.length === mappedEntries.length;

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden bg-[#fcfcfd]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <AddEntryModal
        isOpen={isAddEntryModalOpen}
        onClose={() => {
          if (!isCreatingEntry && !isUpdatingEntry) {
            setIsAddEntryModalOpen(false);
            setEditingEntryId(null);
            setEditInitialData(null);
          }
        }}
        onAdd={editingEntryId ? handleUpdateEntry : handleAddEntry}
        categories={categoryOptions}
        initialData={editInitialData}
        isSubmitting={isCreatingEntry || isUpdatingEntry || isFetchingEntryDetails}
        mode={editingEntryId ? "edit" : "create"}
      />

      <BulkAddModal
        isOpen={isBulkAddModalOpen}
        onClose={() => {
          if (!isBulkImportInProgress) setIsBulkAddModalOpen(false);
        }}
        categoryId={activeCategory?.id}
        categoryName={activeCategory?.name}
        onImport={handleBulkImport}
        isSubmitting={isBulkImportInProgress}
      />

      <ConfirmationModal
        type="delete"
        isOpen={isDeleteModalOpen}
        onClose={() => {
          if (!isDeletingEntry) setIsDeleteModalOpen(false);
        }}
        onConfirm={() => {
          void confirmDeleteEntry();
        }}
        title="Delete entry?"
        description="Are you sure you want to delete this library entry? This action cannot be undone."
        items={[]}
        confirmButtonText={isDeletingEntry ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingEntry}
      />

      <ConfirmationModal
        type="delete"
        isOpen={isBulkDeleteModalOpen}
        onClose={() => {
          if (!isBulkDeletingEntries) setIsBulkDeleteModalOpen(false);
        }}
        onConfirm={() => {
          void confirmDeleteSelected();
        }}
        title="Delete selected entries?"
        description={`Are you sure you want to delete ${selectedEntries.length} selected ${selectedEntries.length === 1 ? "entry" : "entries"}? This action cannot be undone.`}
        items={[]}
        confirmButtonText={isBulkDeletingEntries ? "Deleting..." : "Delete"}
        confirmButtonLoading={isBulkDeletingEntries}
      />

      <ConfirmationModal
        type="delete"
        isOpen={isDeleteConnectionModalOpen}
        onClose={() => {
          if (!isDeletingConnection) {
            setIsDeleteConnectionModalOpen(false);
            setConnectionToDeleteId(null);
          }
        }}
        onConfirm={() => {
          void confirmDeleteConnection();
        }}
        title="Delete connection?"
        description="Are you sure you want to delete this connection? This action cannot be undone."
        items={[]}
        confirmButtonText={isDeletingConnection ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingConnection}
      />

      <div className="flex min-h-0 flex-1 flex-col gap-6 overflow-hidden">
        <div className="shrink-0">
          {isLoadingCategories || (isFetchingCategories && tabs.length === 0) ? (
            <div className="mb-0 flex h-12 w-full items-center justify-center rounded-full bg-(--neutral-100)">
              <ContentLoader variant="inline" size="sm" />
            </div>
          ) : tabs.length > 0 ? (
            <LibraryTabs
              tabs={tabs.map((tab) => ({ id: String(tab.id), name: tab.name }))}
              activeTabId={activeTabId}
              onTabChange={(tabId) => {
                setActiveTabId(tabId);
                setSelectedEntries([]);
              }}
            />
          ) : null}
        </div>

        <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl border border-(--neutral-100) bg-white">
          <div className="shrink-0 border-b border-(--neutral-100) p-5">
            <div className="flex flex-col gap-3">
              <div className="flex items-start justify-between gap-4">
                <h2
                  className="min-w-0 flex-1 truncate text-xl font-bold text-(--neutral-950)"
                  title={activeCategory?.name ?? "Library"}
                >
                  {activeCategory?.name ?? "Library"}
                </h2>
                <div className="flex shrink-0 items-center gap-2">
                  <CustomInput
                    placeholder={`Search ${(activeCategory?.name || "entries").toLowerCase()}...`}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="rounded-full min-h-10 md:w-70 w-full pb-0 pt-1.75"
                    icon={<Search className="size-4.5 text-(--text-neutral-400)" />}
                  />

                  {canWriteEntries ? (
                    <>
                      <Button
                        variant="outline"
                        onClick={() => setIsBulkAddModalOpen(true)}
                        disabled={!activeCategory}
                        className="h-10 rounded-full px-4 gap-2 border-(--neutral-200) text-(--neutral-950) font-medium cursor-pointer disabled:opacity-50"
                      >
                        <Upload size={18} />
                        Bulk Add
                      </Button>
                      <Button
                        onClick={() => {
                          setEditingEntryId(null);
                          setEditInitialData(null);
                          setIsAddEntryModalOpen(true);
                        }}
                        className="h-10 rounded-full px-4 gap-2 font-medium cursor-pointer"
                      >
                        <Plus size={18} />
                        Add Entry
                      </Button>
                    </>
                  ) : null}
                </div>
              </div>
              <ExpandableText
                text={activeCategory?.description || "Content library entries"}
                className="text-sm text-(--text-neutral-600)"
                collapseThreshold={150}
                collapsedLineClamp={2}
              />
            </div>
          </div>

          {canWriteEntries ? (
            <div className="shrink-0 px-5 pt-4">
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2 rounded-full bg-(--neutral-50) px-4 py-3">
                  <div className="flex items-center gap-3">
                    <Checkbox
                      id="select-all"
                      checked={isAllSelected}
                      onChange={(e) => handleSelectAll(e.target.checked)}
                    />
                    <label
                      htmlFor="select-all"
                      className="cursor-pointer text-sm font-medium text-(--neutral-950)"
                    >
                      Select all
                    </label>
                  </div>
                </div>

                <div className="flex animate-in fade-in zoom-in-95 items-center gap-2 rounded-full bg-(--neutral-50) px-3 py-2 transition-all duration-200">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setSelectedEntries([])}
                      className="cursor-pointer rounded-full p-1 text-(--text-neutral-600) transition-colors hover:bg-(--neutral-200)"
                    >
                      <X size={20} />
                    </button>
                    <span className="text-sm font-medium text-(--neutral-950)">
                      {selectedEntries.length}/{mappedEntries.length} selected
                    </span>
                  </div>

                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleDeleteSelected}
                    className="h-7 cursor-pointer gap-1.5 rounded-full border border-(--neutral-300) px-2 font-medium text-(--neutral-950) hover:bg-transparent hover:text-red-600"
                    disabled={
                      selectedEntries.length === 0 ||
                      isDeletingEntry ||
                      isBulkDeletingEntries
                    }
                  >
                    <TrashIcon size={14} />
                    Delete Selected
                  </Button>
                </div>
              </div>
            </div>
          ) : null}

          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 py-4">
            <div className="flex flex-col gap-3">
              {isLoadingEntries ||
              ((isFetchingEntries || isFetchingEntriesWithConnections) &&
                mappedEntries.length === 0) ? (
                <div className="flex items-center justify-center py-16">
                  <ContentLoader variant="inline" size="md" />
                </div>
              ) : mappedEntries.length > 0 ? (
                <>
                  {mappedEntries.map((entry) => (
                    <LibraryEntryCard
                      key={entry.id}
                      entry={entry}
                      isSelected={selectedEntries.includes(entry.id)}
                      onSelect={
                        canWriteEntries
                          ? (checked) => handleSelectEntry(entry.id, checked)
                          : undefined
                      }
                      onEdit={
                        canWriteEntries
                          ? () => {
                              void handleOpenEdit(entry.id);
                            }
                          : undefined
                      }
                      onDelete={canWriteEntries ? () => handleDeleteEntry(entry.id) : undefined}
                      onDeleteConnection={
                        canWriteEntries
                          ? (connectionId) => {
                              void handleDeleteConnection(connectionId);
                            }
                          : undefined
                      }
                      onDeleteAllConnections={
                        canWriteEntries
                          ? (entryId) => {
                              void handleDeleteAllConnections(entryId);
                            }
                          : undefined
                      }
                      isDeletingConnection={isDeletingConnection}
                    />
                  ))}
                  {(isFetchingEntries ||
                    isFetchingEntriesWithConnections ||
                    isFetchingCategories) && (
                    <div className="flex items-center justify-center gap-2 py-2 text-sm text-(--text-neutral-400)">
                      <ContentLoader variant="inline" size="sm" />
                      Refreshing...
                    </div>
                  )}
                </>
              ) : (
                <div className="py-10 text-center text-(--text-neutral-400)">No data found.</div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminLibrary;
