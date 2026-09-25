
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from "react";
import { Search, X } from "lucide-react";
import CustomInput from "../form/CustomInput";
import type { LibraryEntry } from "@/store/api/admin/libraryEntries.api";
import { filterLibraryEntriesForField } from "@/utils/sessionNoteLibrary";

interface SelectLibraryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (entry: LibraryEntry) => void;
  fieldName: string;
  categoryId: number;
  entries: LibraryEntry[];
  isLoading?: boolean;
}

const SelectLibraryModalContent = ({
  isOpen,
  onClose,
  onSelect,
  fieldName,
  categoryId,
  entries,
  isLoading = false,
}: SelectLibraryModalProps) => {
  const [searchQuery, setSearchQuery] = useState("");



  const filteredEntries = filterLibraryEntriesForField(entries, categoryId, searchQuery);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-9999 flex items-center justify-center bg-black/40 p-4">
      <div className="flex h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-xl bg-white p-5">
        <div className="mb-2 flex shrink-0 items-start justify-between">
          <div className="space-y-1">
            <h2 className="text-2xl font-semibold text-(--text-primary-dark)">
              Select from Library - {fieldName}
            </h2>
            <p className="text-base text-(--text-neutral-500)">
              Choose pre-written clinical content to insert into this field
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="cursor-pointer rounded-full p-1 transition-colors hover:bg-(--neutral-50)"
          >
            <X size={24} className="text-(--text-neutral-900)" />
          </button>
        </div>

        <div className="my-4 shrink-0">
          <CustomInput
            placeholder="Search title, content, or tags..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="min-h-10 w-full rounded-full pt-1.75 pb-0"
          />
        </div>

        <div className="min-h-0 flex-1 space-y-3 overflow-y-auto custom-scrollbar">
          {isLoading ? (
            <ContentLoader size="md" className="py-12 text-(--text-neutral-500) mr-2" />
          ) : filteredEntries.length === 0 ? (
            <p className="py-12 text-center text-sm text-(--text-neutral-500)">
              No entries found in this category.
            </p>
          ) : (
            filteredEntries.map((entry) => (
              <button
                key={entry.id}
                type="button"
                onClick={() => {
                  onSelect(entry);
                  onClose();
                }}
                className="group w-full cursor-pointer rounded-2xl border border-(--neutral-100) p-4 text-left transition-all hover:border-(--neutral-200)"
              >
                <div className="mb-2 flex items-start justify-between gap-3">
                  <span className="text-base font-bold text-(--text-primary-dark)">
                    {entry.title}
                  </span>
                  <span className="shrink-0 text-sm text-(--text-neutral-500)">
                    Used {entry.usageCount}x
                  </span>
                </div>
                <p className="line-clamp-2 text-base leading-relaxed text-(--text-neutral-600)">
                  {entry.content}
                </p>
                {entry.tags.length > 0 ? (
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {entry.tags.slice(0, 3).map((tag) => (
                      <span
                        key={`${entry.id}-${tag}`}
                        className="rounded-full bg-(--neutral-50) px-2 py-0.5 text-xs text-(--text-neutral-600)"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                ) : null}
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

const SelectLibraryModal = (props: SelectLibraryModalProps) => props.isOpen ? <SelectLibraryModalContent key={props.fieldName} {...props} /> : null;

export default SelectLibraryModal;
