import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { cn } from "@/lib/utils";
import { MOCK_CHECKLIST_ITEMS } from "@/pages/admin/content/content.static";
import type { ChecklistItem } from "@/types/checklist-item";
import ChecklistItemList from "./ChecklistItemList";
import ChecklistItemForm from "./ChecklistItemForm";
import {
  checklistItemSchema,
  type ChecklistItemValues,
} from "@/schemas/admin-checklist.schemas";

interface ChecklistItemsPanelProps {
  isOpen: boolean;
  onClose: () => void;
  items?: ChecklistItem[];
  onItemsChange?: (items: ChecklistItem[]) => void;
}

const ChecklistItemsPanel = ({
  isOpen,
  onClose,
  items: initialItems = MOCK_CHECKLIST_ITEMS,
  onItemsChange,
}: ChecklistItemsPanelProps) => {
  const [items, setItems] = useState<ChecklistItem[]>(initialItems);
  const [isCreateMode, setIsCreateMode] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const form = useForm<ChecklistItemValues>({
    resolver: zodResolver(checklistItemSchema),
    defaultValues: {
      id: "",
      title: "",
      category: "",
      description: "",
      templates: [],
      required: false,
    },
  });

  const handleCloseForm = () => {
    setIsCreateMode(false);
    setEditingId(null);
    form.reset({
      id: "",
      title: "",
      category: "",
      description: "",
      templates: [],
      required: false,
    });
  };

  useEffect(() => {
    if (!isOpen) {
      handleCloseForm();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  const handleCreateClick = () => {
    form.reset({
      id: `item-${Date.now()}`,
      title: "",
      category: "",
      description: "",
      templates: [],
      required: false,
    });
    setEditingId(null);
    setIsCreateMode(true);
  };

  const handleEditClick = (item: ChecklistItem) => {
    form.reset(item);
    setEditingId(item.id);
    setIsCreateMode(true);
  };

  const handleDeleteItem = (id: string) => {
    const updatedItems = items.filter((item) => item.id !== id);
    setItems(updatedItems);
    onItemsChange?.(updatedItems);
  };

  const handleSaveItem = (data: ChecklistItemValues) => {
    let updatedItems: ChecklistItem[];
    if (editingId) {
      updatedItems = items.map((item) =>
        item.id === editingId ? (data as ChecklistItem) : item,
      );
    } else {
      updatedItems = [...items, data as ChecklistItem];
    }

    setItems(updatedItems);
    onItemsChange?.(updatedItems);
    handleCloseForm();
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className={cn(
          "fixed inset-0 bg-black/50 z-50 transition-opacity duration-300",
          isOpen ? "opacity-100" : "opacity-0 pointer-events-none",
        )}
        onClick={onClose}
      />

      {/* Panel Container */}
      <div
        className={cn(
          "fixed right-0 top-0 h-full w-full max-w-125 bg-white z-50 shadow-2xl overflow-y-auto custom-scrollbar transition-transform duration-300",
          isOpen ? "translate-x-0" : "translate-x-full",
        )}
      >
        {!isCreateMode ? (
          <ChecklistItemList
            items={items}
            onClose={onClose}
            onCreateClick={handleCreateClick}
            onEditClick={handleEditClick}
            onDeleteClick={handleDeleteItem}
          />
        ) : (
          <ChecklistItemForm
            form={form}
            onSubmit={handleSaveItem}
            onCancel={handleCloseForm}
            isEditing={!!editingId}
          />
        )}
      </div>
    </>
  );
};

export default ChecklistItemsPanel;
