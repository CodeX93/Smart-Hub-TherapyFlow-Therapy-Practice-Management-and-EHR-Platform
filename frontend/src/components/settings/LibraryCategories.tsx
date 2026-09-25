import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useEffect, useMemo, useState } from "react";
import { Pencil, Search } from "lucide-react";
import SettingsLayout from "./SettingsLayout";
import CustomInput from "@/components/form/CustomInput";
import AddLibraryCategoryModal from "./AddLibraryCategoryModal";
import ActionDropdown from "@/components/shared/ActionDropdown";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import {
  useCreateLibraryCategoryMutation,
  useDeleteLibraryCategoryMutation,
  useGetLibraryCategoriesQuery,
  useLazyGetLibraryCategoryByIdQuery,
  useUpdateLibraryCategoryMutation,
} from "@/store/api/admin/libraryCategories.api";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";

const LibraryCategories = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<{
    id: number;
    name: string;
    description?: string;
    parentId?: number | null;
    sortOrder?: number;
    isActive: boolean;
  } | null>(null);
  const [categoryToDelete, setCategoryToDelete] = useState<{
    id: number;
    name: string;
  } | null>(null);

  const {
    data: categories = [],
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useGetLibraryCategoriesQuery(undefined, {
    refetchOnMountOrArgChange: true,
  });
  const [createCategory, { isLoading: isCreatingCategory }] =
    useCreateLibraryCategoryMutation();
  const [triggerGetCategoryById, { isFetching: isFetchingCategoryById }] =
    useLazyGetLibraryCategoryByIdQuery();
  const [updateCategory, { isLoading: isUpdatingCategory }] =
    useUpdateLibraryCategoryMutation();
  const [deleteCategory, { isLoading: isDeletingCategory }] =
    useDeleteLibraryCategoryMutation();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (isError && error && error !== reportedError) {
    setReportedError(error);
    setToastType("error");
    setToastMessage(getApiErrorMessage(error));
  }

  const filteredCategories = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return categories;
    return categories.filter((category) => {
      const haystack =
        `${category.name} ${category.description ?? ""} ${category.parentName ?? ""}`.toLowerCase();
      return haystack.includes(query);
    });
  }, [categories, searchQuery]);

  const handleSubmitCategory = async (payload: {
    name: string;
    description?: string;
    parentId?: number;
    sortOrder?: number;
    isActive: boolean;
  }) => {
    try {
      if (editingCategory?.id) {
        await updateCategory({
          id: editingCategory.id,
          body: payload,
        }).unwrap();
        setToastType("success");
        setToastMessage("Library category updated successfully.");
      } else {
        await createCategory(payload).unwrap();
        setToastType("success");
        setToastMessage("Library category created successfully.");
      }
      setIsAddModalOpen(false);
      setEditingCategory(null);
      await refetch();
    } catch (categoryError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(categoryError));
    }
  };

  const handleEditCategory = async (id: number) => {
    try {
      const category = await triggerGetCategoryById(id).unwrap();
      setEditingCategory({
        id: category.id,
        name: category.name,
        description: category.description,
        parentId: category.parentId ?? null,
        sortOrder: category.sortOrder,
        isActive: category.isActive,
      });
      setIsAddModalOpen(true);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleDeleteCategory = (id: number, name: string) => {
    setCategoryToDelete({ id, name });
    setIsDeleteModalOpen(true);
  };

  const confirmDeleteCategory = async () => {
    if (!categoryToDelete) return;
    try {
      await deleteCategory(categoryToDelete.id).unwrap();
      setToastType("success");
      setToastMessage("Library category deleted successfully.");
      setIsDeleteModalOpen(false);
      setCategoryToDelete(null);
      await refetch();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const parentOptions = useMemo(
    () => [
      { value: "", label: "No Parent" },
      ...categories.map((category) => ({
        value: String(category.id),
        label: category.name,
      })),
    ],
    [categories],
  );

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <SettingsLayout
        title="Library Categories"
        description="Manage categories used in the content library."
        actionLabel="Add Category"
        onAction={() => {
          setEditingCategory(null);
          setIsAddModalOpen(true);
        }}
        toolbar={
          <div className="w-full max-w-md">
            <CustomInput
              placeholder="Search categories..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="min-h-10 w-full rounded-full pb-0 pt-1.75 shadow-xs"
            />
          </div>
        }
      >
        <div className="flex h-full min-h-0 flex-col overflow-hidden p-4">
          <div className="min-h-0 flex-1 overflow-auto rounded-2xl border border-(--neutral-100) custom-scrollbar">
            <table className="w-full text-left border-collapse">
              <thead className="sticky top-0 bg-(--bg-primary-50) border-b border-(--neutral-100) z-10">
                <tr>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Name
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Description
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Parent
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Sort Order
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Status
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark) text-right">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-(--neutral-100)">
                {isLoading ? (
                  <tr>
                    <td
                      colSpan={6}
                      className="p-10 text-center text-sm text-(--text-neutral-500)"
                    >
                      <ContentLoader size="md" className="gap-2" />
                    </td>
                  </tr>
                ) : filteredCategories.length > 0 ? (
                  filteredCategories.map((category) => (
                    <tr
                      key={category.id}
                      className="hover:bg-(--bg-primary-light) transition-colors"
                    >
                      <td className="max-w-56 p-4 text-sm text-(--text-primary-dark) font-medium">
                        <div className="truncate" title={category.name}>
                          {category.name}
                        </div>
                      </td>
                      <td className="max-w-80 p-4 text-sm text-(--text-primary-dark)">
                        <div className="truncate" title={category.description || "—"}>
                          {category.description || "—"}
                        </div>
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {category.parentName || "—"}
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {category.sortOrder}
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {category.isActive ? "Active" : "Inactive"}
                      </td>
                      <td className="p-4 text-right">
                        <ActionDropdown
                          actions={[
                            {
                              label: "Edit",
                              icon: <Pencil size={16} />,
                              onClick: () => {
                                void handleEditCategory(category.id);
                              },
                            },
                            {
                              label: "Delete",
                              icon: <TrashIcon size={16} />,
                              onClick: () =>
                                handleDeleteCategory(category.id, category.name),
                            },
                          ]}
                        />
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={6}
                      className="p-10 text-center text-sm text-(--text-neutral-500)"
                    >
                      No categories found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {isFetching && !isLoading ? (
            <div className="mt-3 flex items-center justify-center gap-2 text-sm text-(--text-neutral-500)">
              <ContentLoader variant="inline" size="sm" />
              Refreshing categories...
            </div>
          ) : null}
        </div>
      </SettingsLayout>

      <AddLibraryCategoryModal
        isOpen={isAddModalOpen}
        onClose={() => {
          if (!isCreatingCategory && !isUpdatingCategory && !isFetchingCategoryById) {
            setIsAddModalOpen(false);
            setEditingCategory(null);
          }
        }}
        onAdd={handleSubmitCategory}
        parentOptions={parentOptions}
        isSubmitting={isCreatingCategory || isUpdatingCategory || isFetchingCategoryById}
        initialData={editingCategory}
      />

      <ConfirmationModal
        type="delete"
        isOpen={isDeleteModalOpen}
        onClose={() => {
          if (!isDeletingCategory) setIsDeleteModalOpen(false);
        }}
        onConfirm={() => {
          void confirmDeleteCategory();
        }}
        title="Delete category?"
        description={`Are you sure you want to delete "${categoryToDelete?.name ?? "this category"}"?`}
        items={[]}
        confirmButtonText={isDeletingCategory ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingCategory}
      />
    </div>
  );
};

export default LibraryCategories;
