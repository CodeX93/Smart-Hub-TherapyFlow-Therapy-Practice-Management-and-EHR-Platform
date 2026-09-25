import { TrashIcon } from "@/components/icons/commonIcons";
import { X, Plus, Search, Edit2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import CustomInput from "@/components/form/CustomInput";
import { CHECKLIST_CATEGORIES } from "@/pages/admin/content/content.static";
import type { ChecklistItem } from "@/types/checklist-item";
import { useState } from "react";
import CustomSelect from "../form/CustomSelect";

interface ChecklistItemListProps {
  items: ChecklistItem[];
  onClose: () => void;
  onCreateClick: () => void;
  onEditClick: (item: ChecklistItem) => void;
  onDeleteClick: (id: string) => void;
}

const categoryStyles = {
  Intake: "bg-(--light-blue) text-(--status-billed)",
  Assessment: "bg-(--neutral-100) text-(--text-primary-dark)",
  Ongoing: "bg-(--status-completed-light) text-(--dark-green)",
  Discharge:
    "bg-(--dashboard-status-pending-light) text-(--dashboard-status-pending-dark)",
};

const ChecklistItemList = ({
  items,
  onClose,
  onCreateClick,
  onEditClick,
  onDeleteClick,
}: ChecklistItemListProps) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("All Categories");

  const filteredItems = items.filter(
    (item) =>
      (item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.category.toLowerCase().includes(searchQuery.toLowerCase())) &&
      (categoryFilter === "" ||
        categoryFilter === "All Categories" ||
        item.category === categoryFilter),
  );

  return (
    <>
      {/* Header */}
      <div className="flex items-center justify-between p-5 border-b border-(--neutral-100)">
        <h2 className="text-lg font-semibold text-(--text-primary-dark)">
          Checklist Items
        </h2>
        <button
          onClick={onClose}
          className="text-(--text-neutral-400) hover:text-(--text-neutral-600) transition-colors duration-200 cursor-pointer"
        >
          <X size={24} />
        </button>
      </div>

      {/* Create Button */}
      <div className="px-5 pt-5">
        <Button
          onClick={onCreateClick}
          className="w-full h-10 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full flex items-center justify-center gap-2 cursor-pointer"
        >
          <Plus size={18} />
          Create Checklist Item
        </Button>
      </div>

      {/* Search and Filter */}
      <div className="flex items-center gap-2 w-full md:w-auto p-5">
        <CustomInput
          placeholder="Search..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
          className="rounded-full min-h-10 md:w- pb-0 pt-1.75 shadow-xs"
        />

        <CustomSelect
          options={CHECKLIST_CATEGORIES.map((cat) => ({
            value: cat,
            label: cat,
          }))}
          value={categoryFilter}
          onChange={(value) => setCategoryFilter(value)}
          className="rounded-full max-h-10 w-50 pb-0 pt-0 bg-white shadow-xs"
          isSearch={false}
        />
      </div>

      {/* Items List */}
      <div className="px-5 pb-6 space-y-2">
        {filteredItems.length === 0 ? (
          <div className="text-center py-10 text-(--text-neutral-400)">
            {items.length === 0
              ? "No items yet. Create one!"
              : "No items match your search."}
          </div>
        ) : (
          filteredItems.map((item) => (
            <div
              key={item.id}
              className="flex items-center justify-between p-3 bg-white border-b border-(--neutral-100)"
            >
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-medium text-(--text-primary-dark) truncate">
                  {item.title}
                  {item.required && (
                    <span className="text-red-500 ml-1">*</span>
                  )}
                </h3>
              </div>
              <div className="flex items-center gap-2 ml-3">
                <Badge
                  className={cn(
                    "px-2 py-1 rounded-full text-xs font-medium whitespace-nowrap",
                    categoryStyles[
                      item.category as keyof typeof categoryStyles
                    ],
                  )}
                >
                  {item.category}
                </Badge>
                <button
                  onClick={() => onEditClick(item)}
                  className="text-(--text-neutral-600) cursor-pointer"
                >
                  <Edit2 size={16} />
                </button>
                <button
                  onClick={() => onDeleteClick(item.id)}
                  className="text-(--text-neutral-600) cursor-pointer"
                >
                  <TrashIcon size={16} />
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </>
  );
};

export default ChecklistItemList;
