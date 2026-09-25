import React from "react";
import { X } from "lucide-react";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";

interface ChecklistItem {
  id: string;
  label: string;
}

interface ChecklistItemsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: () => void;
  items: ChecklistItem[];
  selectedItems: string[];
  onSelectedItemsChange: (selectedItems: string[]) => void;
  isSaving?: boolean;
  readOnly?: boolean;
}

const ChecklistItemsModal: React.FC<ChecklistItemsModalProps> = ({
  isOpen,
  onClose,
  onSave,
  items,
  selectedItems,
  onSelectedItemsChange,
  isSaving = false,
  readOnly = false,
}) => {
  if (!isOpen) return null;

  const selectedSet = new Set(selectedItems);

  const handleItemChange = (itemId: string, checked: boolean) => {
    if (isSaving || readOnly) return;
    const next = new Set(selectedSet);
    if (checked) {
      next.add(itemId);
    } else {
      next.delete(itemId);
    }
    onSelectedItemsChange(Array.from(next));
  };

  const handleCancel = () => {
    if (isSaving) return;
    onClose();
  };

  const itemCount = items.length;
  const hasItems = itemCount > 0;

  return (
    <>
      <div
        className="fixed inset-0 z-40 bg-black/50 bg-opacity-50"
        onClick={handleCancel}
      />

      <div className="fixed inset-0 z-50 flex items-center justify-center p-2">
        <div className="flex max-h-[80vh] w-full max-w-181.5 flex-col overflow-hidden rounded-lg bg-white shadow-lg">
          <div className="flex items-center justify-between p-5">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Checklist Items ({itemCount})
            </h2>
            <button
              onClick={handleCancel}
              disabled={isSaving}
              className="cursor-pointer rounded-full p-1 text-(--text-primary-dark) transition-colors duration-200 hover:bg-(--neutral-100) disabled:cursor-not-allowed disabled:opacity-50"
            >
              <X size={20} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto px-5 pb-5">
            {hasItems ? (
              <div className="rounded-xl border border-(--neutral-100) bg-(--neutral-50) p-2">
                {items.map((item) => (
                  <div key={item.id} className="flex items-center gap-2 p-2">
                    <Checkbox
                      id={`item-${item.id}`}
                      checked={selectedSet.has(item.id)}
                      disabled={readOnly}
                      onCheckedChange={(checked) =>
                        handleItemChange(item.id, Boolean(checked))
                      }
                    />
                    <label
                      htmlFor={`item-${item.id}`}
                      className="flex-1 cursor-pointer select-none text-(--text-primary-dark)"
                    >
                      {item.label}{" "}
                      <span className="text-(--status-denied)">*</span>
                    </label>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex min-h-32 items-center justify-center rounded-xl border border-(--neutral-100) bg-(--neutral-50) px-6 py-10 text-center">
                <p className="text-sm font-medium text-(--text-neutral-600)">
                  This checklist template does not have any items.
                </p>
              </div>
            )}
          </div>

          <div className="flex items-center justify-end gap-3 p-5">
            <Button
              variant="outline"
              onClick={handleCancel}
              disabled={isSaving}
              className="h-11.5 cursor-pointer rounded-full border border-(--neutral-200) px-8 text-(--text-neutral-800) hover:bg-(--neutral-50)"
            >
              {readOnly ? "Close" : "Cancel"}
            </Button>
            {!readOnly && hasItems ? (
              <Button
                onClick={onSave}
                disabled={isSaving}
                className="h-11.5 cursor-pointer rounded-full bg-(--bg-primary-dark) px-8 text-white hover:bg-(--bg-primary-dark)/90"
                loading={isSaving}
                loadingLabel="Saving..."
              >
                Save
              </Button>
            ) : null}
          </div>
        </div>
      </div>
    </>
  );
};

export default ChecklistItemsModal;
