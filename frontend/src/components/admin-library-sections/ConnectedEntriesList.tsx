import { TrashIcon } from "@/components/icons/commonIcons";
import { Search, Link2, X } from "lucide-react";
import { useRef, useState, type RefObject } from "react";
import { Badge } from "@/components/ui/badge";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import type { LibraryEntryCardConnection } from "./LibraryEntryCard";
import ConfirmationModal from "../shared/ConfirmationModal";

interface ConnectedEntriesListProps {
  visibleEntries: LibraryEntryCardConnection[];
  filteredEntries: LibraryEntryCardConnection[];
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onDeleteEntry: (id: string) => void;
  selectedCategory: string;
  onCategoryChange: (category: string) => void;
  onDeleteAll: () => void;
  isDeleting?: boolean;
  portalContainerRef?: RefObject<HTMLElement | null>;
}

const ConnectedEntriesList: React.FC<ConnectedEntriesListProps> = ({
  visibleEntries,
  filteredEntries,
  searchQuery,
  onSearchChange,
  onDeleteEntry,
  selectedCategory,
  onCategoryChange,
  onDeleteAll,
  isDeleting = false,
  portalContainerRef,
}) => {
  const connectionsScrollRef = useRef<HTMLDivElement>(null);
  const [isDeleteAllModalOpen, setIsDeleteAllModalOpen] = useState(false);
  const uniqueCategories = Array.from(
    new Set(visibleEntries.map((entry) => entry.category).filter(Boolean)),
  );
  const categoryOptions = [
    { label: "All Categories", value: "all" },
    ...uniqueCategories.map((category) => ({ label: category, value: category })),
  ];

  return (
    <div className="p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Link2 size={20} />
          <h4 className="text-sm font-semibold text-(--neutral-950)">
            Connected Entries ({visibleEntries.length})
          </h4>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-[15rem]">
            <CustomInput
              placeholder="Search connections..."
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              className="rounded-full min-h-10 pb-0 pt-1.75"
              icon={<Search className="size-4.5 text-(--text-neutral-400)" />}
            />
          </div>

          <CustomSelect
            value={selectedCategory}
            onChange={(value: string) => onCategoryChange(value)}
            options={categoryOptions}
            className="rounded-full max-h-10 min-w-[11.25rem] w-fit pb-0 pt-0 bg-white shadow-xs"
            contentClassName="min-w-[12.5rem]"
            isSearch={false}
            closeOnScroll
            scrollContainerRefs={
              portalContainerRef
                ? [portalContainerRef, connectionsScrollRef]
                : [connectionsScrollRef]
            }
            avoidCollisions={false}
          />

          {visibleEntries.length > 0 ? (
            <button
              onClick={() => setIsDeleteAllModalOpen(true)}
              disabled={isDeleting}
              className="h-9 flex items-center gap-2 text-(--neutral-950) hover:text-red-600 transition-colors cursor-pointer text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <TrashIcon size={16} />
              Delete All
            </button>
          ) : null}
        </div>
      </div>

      <div
        ref={connectionsScrollRef}
        className="max-h-[21.875rem] overflow-y-auto p-1 pr-2 custom-scrollbar"
      >
        <div className="grid grid-cols-5 gap-3">
          {filteredEntries.map((entry) => (
            <div
              key={entry.id}
              className="relative flex min-w-0 flex-col items-start gap-2 p-3 pr-9 bg-white border border-(--neutral-100) rounded-md group"
            >
              <button
                onClick={() => onDeleteEntry(entry.id)}
                disabled={isDeleting}
                className="absolute top-2 right-2 z-10 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-black text-white transition-opacity duration-300 cursor-pointer opacity-0 group-hover:opacity-100 disabled:cursor-not-allowed disabled:opacity-50"
                title="Remove entry"
              >
                <X size={12} />
              </button>
              <span
                className="w-full min-w-0 truncate text-sm font-medium text-(--bg-primary-dark)"
                title={entry.code}
              >
                {entry.code}
              </span>
              <Badge
                className="max-w-full min-w-0 truncate bg-[#EEF4FF] text-[#448DF2] hover:bg-[#EEF4FF] border-none font-normal py-1"
                title={entry.category}
              >
                <span className="block truncate">{entry.category}</span>
              </Badge>
            </div>
          ))}
          {filteredEntries.length === 0 && visibleEntries.length > 0 && (
            <div className="col-span-5 text-center py-6 text-(--text-neutral-400) text-sm">
              No connected entries found
            </div>
          )}
        </div>
      </div>

      <ConfirmationModal
        type="delete"
        isOpen={isDeleteAllModalOpen}
        onClose={() => setIsDeleteAllModalOpen(false)}
        onConfirm={() => {
          onDeleteAll();
          setIsDeleteAllModalOpen(false);
        }}
        title="Delete all connected entries?"
        description="Are you sure you want to remove all connected entries? This action cannot be undone."
        items={[
          "All current connections in this entry",
        ]}
        confirmButtonText="Delete all"
        confirmButtonLoading={isDeleting}
        confirmButtonDisabled={isDeleting}
      />
    </div>
  );
};

export default ConnectedEntriesList;
