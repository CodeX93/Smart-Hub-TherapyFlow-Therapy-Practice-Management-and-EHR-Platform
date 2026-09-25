
import { ContentLoader } from "@/components/shared/ContentLoader";
import React, { useState } from "react";
import SettingsLayout from "./SettingsLayout";
import {
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { arrayMove, sortableKeyboardCoordinates } from "@dnd-kit/sortable";
import AddCategoryModal from "./AddCategoryModal";
import OptionModal from "./OptionModal";
import DeleteConfirmationModal from "./DeleteConfirmationModal";
import CategorySidebar from "./CategorySidebar";
import OptionList from "./OptionList";
import type { OptionValue } from "@/types/settings.types";
import {
  useCreateSystemOptionCategoryMutation,
  useCreateSystemOptionMutation,
  useGetSystemOptionCategoriesQuery,
  useLazyGetSystemOptionCategoryByIdQuery,
  useLazyGetSystemOptionByIdQuery,
  useUpdateSystemOptionMutation,
  useUpdateSystemOptionCategoryMutation,
  useDeleteSystemOptionCategoryMutation,
  useDeleteSystemOptionMutation,
  useReorderCategoryOptionsMutation,
} from "@/store/api/admin/systemOptions.api";

import Toast from "@/components/shared/Toast";

const SystemOptions = () => {
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(
    null,
  );
  const [searchQuery, setSearchQuery] = useState("");
  const [isAddCategoryModalOpen, setIsAddCategoryModalOpen] = useState(false);
  const [isOptionModalOpen, setIsOptionModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isDeleteOptionModalOpen, setIsDeleteOptionModalOpen] = useState(false);
  const [editingOption, setEditingOption] = useState<OptionValue | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [categoryToDelete, setCategoryToDelete] = useState<string | null>(null);
  const [editingCategory, setEditingCategory] = useState<{
    id: string;
    key: string;
    name: string;
    description?: string;
    isSystem?: boolean;
    isActive?: boolean;
  } | null>(null);
  // The management screen must see inactive options too — they can only be
  // re-enabled or deleted from here. Consuming dropdowns keep the filtered default.
  const {
    data: fetchedCategories = [],
    isLoading: isCategoriesLoading,
    isFetching: isCategoriesFetching,
    isError: isCategoriesError,
    error: categoriesError,
    refetch: refetchCategories,
  } = useGetSystemOptionCategoriesQuery({ includeInactive: true });
  const [createCategory, { isLoading: isCreatingCategory }] =
    useCreateSystemOptionCategoryMutation();
  const [triggerGetCategoryById, { isFetching: isSelectedCategoryFetching }] =
    useLazyGetSystemOptionCategoryByIdQuery();
  const [updateCategory, { isLoading: isUpdatingCategory }] =
    useUpdateSystemOptionCategoryMutation();
  const [createOption, { isLoading: isCreatingOption }] =
    useCreateSystemOptionMutation();
  const [triggerGetOptionById] = useLazyGetSystemOptionByIdQuery();
  const [updateOption, { isLoading: isUpdatingOption }] =
    useUpdateSystemOptionMutation();
  const [deleteCategory, { isLoading: isDeletingCategory }] =
    useDeleteSystemOptionCategoryMutation();
  const [deleteOption, { isLoading: isDeletingOption }] =
    useDeleteSystemOptionMutation();
  const [reorderOptions] =
    useReorderCategoryOptionsMutation();
  const [selectedCategoryDetails, setSelectedCategoryDetails] = useState<{
    id: string;
    name: string;
    code: string;
    description?: string;
    options: OptionValue[];
    isCustom?: boolean;
    isActive?: boolean;
  } | null>(null);
  const [categories, setCategories] = useState<
    {
      id: string;
      name: string;
      code: string;
      description?: string;
      options: OptionValue[];
      isCustom?: boolean;
      isActive?: boolean;
    }[]
  >([]);

  const prevCategoriesRef = React.useRef<string>("");

  React.useEffect(() => {
    if (!fetchedCategories || fetchedCategories.length === 0) return;
    
    const currentSignature = JSON.stringify(
      fetchedCategories.map(c => ({
        id: c.id,
        name: c.categoryName,
        options: c.options.map(o => ({ id: o.id, order: o.sortOrder }))
      }))
    );
    
    if (prevCategoriesRef.current === currentSignature) return;
    prevCategoriesRef.current = currentSignature;
    
    setCategories(
      fetchedCategories.map((category) => ({
        id: String(category.id),
        name: category.categoryName,
        code: category.categoryKey,
        description: category.description,
        options: category.options.map((option) => ({
          id: String(option.id),
          label: option.optionLabel,
          value: option.optionKey,
          isDefault: option.isDefault,
          isSystem: option.isSystem,
          isActive: option.isActive,
        })),
        isCustom: !category.isSystem,
        isActive: category.isActive,
      })),
    );
  }, [fetchedCategories]);

  React.useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  React.useEffect(() => {
    if (!selectedCategoryId && fetchedCategories.length > 0) {
      setSelectedCategoryId(String(fetchedCategories[0].id));
    }
  }, [fetchedCategories, selectedCategoryId]);

  React.useEffect(() => {
    if (!selectedCategoryId) {
      setSelectedCategoryDetails(null);
      return;
    }

    void (async () => {
      try {
        const category = await triggerGetCategoryById(
          { id: Number(selectedCategoryId), includeInactive: true },
        ).unwrap();
        setSelectedCategoryDetails({
          id: String(category.id),
          name: category.categoryName,
          code: category.categoryKey,
          description: category.description,
          options: category.options.map((option) => ({
            id: String(option.id),
            label: option.optionLabel,
            value: option.optionKey,
            isDefault: option.isDefault,
            isSystem: option.isSystem,
            isActive: option.isActive,
          })),
          isCustom: !category.isSystem,
          isActive: category.isActive,
        });
      } catch (error) {
        setToastMessage(formatCategoryError(error));
        setSelectedCategoryDetails(null);
      }
    })();
  }, [selectedCategoryId, triggerGetCategoryById]);

  const formatCategoryError = (error: unknown): string => {
    if (
      typeof error === "object" &&
      error !== null &&
      "data" in error &&
      typeof (error as { data?: unknown }).data === "object" &&
      (error as { data?: Record<string, unknown> }).data !== null
    ) {
      const data = (error as { data?: Record<string, unknown> }).data;
      const message = data?.message;
      if (typeof message === "string" && message.trim()) return message;
    }

    if (
      typeof error === "object" &&
      error !== null &&
      "error" in error &&
      typeof (error as { error?: unknown }).error === "string"
    ) {
      return (error as { error: string }).error;
    }

    return "Something went wrong while processing option categories.";
  };

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id && selectedCategoryId) {
      const category = categories.find((c) => c.id === selectedCategoryId);
      if (!category) return;
      const oldIndex = category.options.findIndex((o) => o?.id === active.id);
      const newIndex = category.options.findIndex((o) => o?.id === over.id);
      if (oldIndex < 0 || newIndex < 0) return;

      const optimisticOptions = arrayMove(category.options, oldIndex, newIndex);
      const newOrder = optimisticOptions.map((opt, idx) => ({
        optionId: Number(opt.id),
        sortOrder: idx,
      }));

      // Optimistically update UI
      setCategories((prev) => {
        return prev.map((cat) => {
          if (cat.id === selectedCategoryId) {
            return { ...cat, options: optimisticOptions };
          }
          return cat;
        });
      });
      setSelectedCategoryDetails((prev) => {
        if (!prev || prev.id !== selectedCategoryId) return prev;
        return { ...prev, options: optimisticOptions };
      });

      // Save order to API and revert on failure
      void reorderOptions({
        categoryId: Number(selectedCategoryId),
        options: newOrder,
      })
        .unwrap()
        .then(() => {
          setToastType("success");
          setToastMessage("Options order updated.");
        })
        .catch((error) => {
          // Revert on failure by refetching
          setToastType("error");
          setToastMessage(formatCategoryError(error));
          void refetchCategories();
        });
    }
  };

  const handleAddCategory = async (data: {
    key: string;
    name: string;
    description: string;
    isSystem: boolean;
    isActive: boolean;
  }) => {
    try {
      const created = await createCategory({
        categoryKey: data.key.trim(),
        categoryName: data.name.trim(),
        description: data.description.trim() || undefined,
        isSystem: data.isSystem,
        isActive: data.isActive,
      }).unwrap();

      setToastType("success");
      setToastMessage("Category created successfully.");
      setIsAddCategoryModalOpen(false);
      setSelectedCategoryId(String(created.id));
    } catch (error) {
      setToastType("error");
      setToastMessage(formatCategoryError(error));
    }
  };

  const handleEditCategory = async (data: {
    key: string;
    name: string;
    description: string;
    isSystem: boolean;
    isActive: boolean;
  }) => {
    if (!editingCategory) return;

    try {
      await updateCategory({
        id: Number(editingCategory.id),
        body: {
          categoryKey: data.key.trim(),
          categoryName: data.name.trim(),
          description: data.description.trim() || undefined,
          isActive: data.isActive,
        },
      }).unwrap();

      setToastType("success");
      setToastMessage("Category updated successfully.");
      setEditingCategory(null);
      setIsAddCategoryModalOpen(false);
    } catch (error) {
      setToastType("error");
      setToastMessage(formatCategoryError(error));
    }
  };

  const handleConfirmDeleteCategory = async () => {
    if (!categoryToDelete) return;
    try {
      await deleteCategory(Number(categoryToDelete)).unwrap();
      setToastType("success");
      setToastMessage("Category deleted successfully.");
      if (selectedCategoryId === categoryToDelete) {
        setSelectedCategoryId(null);
        setSelectedCategoryDetails(null);
      }
      setIsDeleteModalOpen(false);
      void refetchCategories();
    } catch (error) {
      setToastType("error");
      setToastMessage(formatCategoryError(error));
    } finally {
      setCategoryToDelete(null);
    }
  };

  const [optionToDelete, setOptionToDelete] = useState<string | null>(null);

  const handleDeleteOptionClick = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setOptionToDelete(id);
    setIsDeleteOptionModalOpen(true);
  };

  const handleConfirmDeleteOption = async () => {
    if (!optionToDelete || !selectedCategoryId) return;
    try {
      await deleteOption(Number(optionToDelete)).unwrap();
      setToastType("success");
      setToastMessage("Option deleted successfully.");
      setSelectedCategoryDetails((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          options: prev.options.filter((opt) => opt.id !== optionToDelete),
        };
      });
      setIsDeleteOptionModalOpen(false);
    } catch (error) {
      setToastType("error");
      setToastMessage(formatCategoryError(error));
    } finally {
      setOptionToDelete(null);
    }
  };

  const handleDeleteClick = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setCategoryToDelete(id);
    setIsDeleteModalOpen(true);
  };

  const handleOptionSave = async (data: {
    label: string;
    value: string;
    isDefault: boolean;
    isSystem: boolean;
    isActive: boolean;
  }) => {
    if (!selectedCategoryId) return;

    if (editingOption) {
      try {
        const activeCategory = selectedCategoryDetails ??
          categories.find((cat) => cat.id === selectedCategoryId);
        const existingOption = activeCategory?.options.find(
          (option) => option.id === editingOption.id,
        );
        const previousOptionKey = existingOption?.value || editingOption.value;

        const updatedOption = await updateOption({
          id: Number(editingOption.id),
          oldOptionKey: previousOptionKey,
          body: {
            categoryId: Number(selectedCategoryId),
            optionKey: data.value.trim(),
            optionLabel: data.label.trim(),
            sortOrder: activeCategory?.options.findIndex(
              (option) => option.id === editingOption.id,
            ) ?? 0,
            isDefault: data.isDefault,
            isActive: data.isActive,
            price: 0,
          },
        }).unwrap();

        setSelectedCategoryDetails((prev) => {
          if (!prev || prev.id !== selectedCategoryId) return prev;
          return {
            ...prev,
            options: prev.options.map((option) =>
              option.id === editingOption.id
                ? {
                  ...option,
                  label: updatedOption.optionLabel,
                  value: updatedOption.optionKey,
                  isDefault: updatedOption.isDefault,
                  isActive: updatedOption.isActive,
                }
                : option,
            ),
          };
        });

        setToastType("success");
        setToastMessage("Option updated successfully.");
        setEditingOption(null);
        setIsOptionModalOpen(false);
      } catch (error) {
        setToastType("error");
        setToastMessage(formatCategoryError(error));
      }
      return;
    }

    try {
      const activeCategory = selectedCategoryDetails ??
        categories.find((cat) => cat.id === selectedCategoryId);

      const createdOption = await createOption({
        categoryId: Number(selectedCategoryId),
        optionKey: data.value.trim(),
        optionLabel: data.label.trim(),
        sortOrder: activeCategory?.options.length ?? 0,
        isDefault: data.isDefault,
        isSystem: data.isSystem,
        isActive: data.isActive,
        price: 0,
      }).unwrap();

      setSelectedCategoryDetails((prev) => {
        if (!prev || prev.id !== selectedCategoryId) return prev;
        return {
          ...prev,
          options: [
            ...prev.options,
            {
              id: String(createdOption.id),
              label: createdOption.optionLabel,
              value: createdOption.optionKey,
              isDefault: createdOption.isDefault,
              isActive: createdOption.isActive,
            },
          ],
        };
      });

      setToastType("success");
      setToastMessage("Option created successfully.");
      setIsOptionModalOpen(false);
      setEditingOption(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(formatCategoryError(error));
    }
  };

  const normalizedSearch = searchQuery.trim().toLowerCase();
  const filteredCategories = categories.filter((cat) => {
    if (!normalizedSearch) return true;
    return (
      cat.name.toLowerCase().includes(normalizedSearch) ||
      cat.code.toLowerCase().includes(normalizedSearch)
    );
  });

  const customCategories = filteredCategories.filter((cat) => cat.isCustom);
  const systemCategories = filteredCategories.filter((cat) => !cat.isCustom);
  const categoryPendingDelete = categories.find(
    (category) => category.id === categoryToDelete,
  );

  const selectedCategory =
    selectedCategoryDetails ?? categories.find((c) => c.id === selectedCategoryId) ?? null;

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
        title="System Options"
        description="Manage dropdown options and categories"
        actionLabel="Add Category"
        onAction={() => {
          setEditingCategory(null);
          setIsAddCategoryModalOpen(true);
        }}
      >
        {isCategoriesLoading ? (
          <div className="flex h-full w-full items-center justify-center">
            <div className="flex items-center gap-2 text-(--text-neutral-600)">
              <ContentLoader variant="inline" size="md" />
            </div>
          </div>
        ) : isCategoriesError ? (
          <div className="flex h-full w-full flex-col items-center justify-center gap-4 px-8 text-center">
            <p className="text-sm text-(--text-neutral-600)">
              {formatCategoryError(categoriesError)}
            </p>
            <button
              type="button"
              onClick={() => void refetchCategories()}
              className="rounded-full border border-(--neutral-200) px-4 py-2 text-sm font-semibold text-(--bg-primary-dark)"
            >
              Retry
            </button>
          </div>
        ) : (
          <div className="flex h-full min-h-0 overflow-hidden">
            <CategorySidebar
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              customCategories={customCategories}
              systemCategories={systemCategories}
              selectedCategoryId={selectedCategoryId}
              onSelectCategory={(id) => {
                setSelectedCategoryId(id);
                setSelectedCategoryDetails(null);
              }}
              onEditCategory={(cat) => {
                void (async () => {
                  try {
                    const category = await triggerGetCategoryById(
                      { id: Number(cat.id), includeInactive: true },
                    ).unwrap();
                    setEditingCategory({
                      id: String(category.id),
                      key: category.categoryKey,
                      name: category.categoryName,
                      description: category.description,
                      isSystem: category.isSystem,
                      isActive: category.isActive,
                    });
                    setIsAddCategoryModalOpen(true);
                  } catch (error) {
                    setToastMessage(formatCategoryError(error));
                  }
                })();
              }}
              onDeleteCategory={handleDeleteClick}
            />

            <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
              <OptionList
                selectedCategory={selectedCategory || null}
                isLoading={isSelectedCategoryFetching}
                onAddOption={() => {
                  setEditingOption(null);
                  setIsOptionModalOpen(true);
                }}
                onEditOption={(option) => {
                  void (async () => {
                    try {
                      const fetchedOption = await triggerGetOptionById(
                        Number(option.id),
                      ).unwrap();
                      setEditingOption({
                        id: String(fetchedOption.id),
                        label: fetchedOption.optionLabel,
                        value: fetchedOption.optionKey,
                        isDefault: fetchedOption.isDefault,
                        isSystem: fetchedOption.isSystem,
                        isActive: fetchedOption.isActive,
                      });
                      setIsOptionModalOpen(true);
                    } catch (error) {
                      setToastMessage(formatCategoryError(error));
                    }
                  })();
                }}
                onDeleteOption={(option, e) => handleDeleteOptionClick(option.id, e)}
                sensors={sensors}
                handleDragEnd={handleDragEnd}
              />
              {isCategoriesFetching ? (
                <div className="px-4 pb-4 text-xs text-(--text-neutral-500)">
                  Syncing latest categories...
                </div>
              ) : null}
            </div>
          </div>
        )}

        <AddCategoryModal
          isOpen={isAddCategoryModalOpen}
          onClose={() => {
            setIsAddCategoryModalOpen(false);
            setEditingCategory(null);
          }}
          onAdd={editingCategory ? handleEditCategory : handleAddCategory}
          initialData={editingCategory || undefined}
          isSubmitting={isCreatingCategory || isUpdatingCategory}
        />
        <OptionModal
          isOpen={isOptionModalOpen}
          onClose={() => setIsOptionModalOpen(false)}
          onSave={handleOptionSave}
          initialData={editingOption || undefined}
          categoryName={selectedCategory?.name}
          isSubmitting={isCreatingOption || isUpdatingOption}
        />
        <DeleteConfirmationModal
          isOpen={isDeleteModalOpen}
          onClose={() => {
            if (!isDeletingCategory) {
              setIsDeleteModalOpen(false);
              setCategoryToDelete(null);
            }
          }}
          onConfirm={() => void handleConfirmDeleteCategory()}
          title={`Delete "${categoryPendingDelete?.name ?? "Category"}"`}
          description="Are you sure you want to delete this category? This action cannot be undone."
          isDeleting={isDeletingCategory}
        />
        <DeleteConfirmationModal
          isOpen={isDeleteOptionModalOpen}
          onClose={() => setIsDeleteOptionModalOpen(false)}
          onConfirm={handleConfirmDeleteOption}
          title="Delete Option"
          description="Are you sure you want to delete this option?"
          isDeleting={isDeletingOption}
        />
      </SettingsLayout>
    </div>
  );
};

export default SystemOptions;
