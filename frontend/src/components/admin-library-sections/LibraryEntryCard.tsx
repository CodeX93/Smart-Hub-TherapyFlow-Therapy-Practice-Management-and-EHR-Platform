import { TrashIcon } from "@/components/icons/commonIcons";
import React, { useRef, useState } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { ChevronDown, ChevronUp, Link as LinkIcon, Clock, Pencil } from "lucide-react";
import ConnectedEntriesList from "./ConnectedEntriesList";
import { cn } from "@/lib/utils";

export interface LibraryEntryCardConnection {
  id: string;
  code: string;
  category: string;
}

export interface LibraryEntryCardData {
  id: string;
  code: string;
  title: string;
  content: string;
  usageCount: number;
  connectedCount: number;
  connections: LibraryEntryCardConnection[];
}

interface LibraryEntryCardProps {
  entry: LibraryEntryCardData;
  isSelected: boolean;
  onSelect?: (checked: boolean) => void;
  onEdit?: () => void;
  onDelete?: () => void;
  onDeleteConnection?: (connectionId: string) => void;
  onDeleteAllConnections?: (entryId: string) => void;
  isDeletingConnection?: boolean;
}

const LibraryEntryCard: React.FC<LibraryEntryCardProps> = ({
  entry,
  isSelected,
  onSelect,
  onEdit,
  onDelete,
  onDeleteConnection,
  onDeleteAllConnections,
  isDeletingConnection = false,
}) => {
  const expandedSectionRef = useRef<HTMLDivElement>(null);
  const [isExpanded, setIsExpanded] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [deletedEntries, setDeletedEntries] = useState<string[]>([]);
  const [selectedCategory, setSelectedCategory] = useState("all");

  const visibleEntries = entry.connections.filter(
    (conn) => !deletedEntries.includes(conn.id)
  );

  const filteredEntries = visibleEntries.filter((conn) => {
    const matchesSearch = conn.code.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory =
      selectedCategory === "all" || conn.category.toLowerCase() === selectedCategory.toLowerCase();
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="border border-(--neutral-100) rounded-lg bg-white transition-all duration-200 overflow-hidden">
      <div className="p-4 flex items-center gap-2">
        {onSelect ? (
          <div className="">
            <Checkbox
              id={`entry-${entry.id}`}
              checked={isSelected}
              onChange={(e) => onSelect(e.target.checked)}
            />
          </div>
        ) : null}
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="text-(--text-neutral-600) hover:text-(--neutral-950) transition-colors cursor-pointer"
        >
          {isExpanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
        </button>

        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-3">
            <h3
              className="min-w-0 flex-1 pr-2 text-base font-semibold text-(--neutral-950) truncate"
              title={entry.title}
            >
              {entry.title}
            </h3>
            <div className="flex shrink-0 items-center">
              {onEdit ? (
                <button
                  onClick={onEdit}
                  className="p-2 text-(--text-neutral-400) hover:text-(--neutral-950) transition-colors cursor-pointer rounded-full hover:bg-(--neutral-50)"
                >
                  <Pencil size={18} />
                </button>
              ) : null}
              {onDelete ? (
                <button
                  onClick={onDelete}
                  className="p-2 text-(--text-neutral-400) hover:text-red-600 transition-colors cursor-pointer rounded-full hover:bg-(--neutral-50)"
                >
                  <TrashIcon size={18} />
                </button>
              ) : null}
            </div>
          </div>

          <div className="mb-1 flex flex-wrap items-start gap-3 text-sm text-(--text-neutral-600)">
            <span className="min-w-0 flex-1 truncate" title={entry.content || "-"}>
              {entry.content || "-"}
            </span>
            <div className="flex shrink-0 items-center gap-1.5 border-l border-(--neutral-100) px-3">
              <LinkIcon size={16} />
              <span>{entry.connectedCount}</span>
            </div>
            <div className="flex shrink-0 items-center gap-1.5 border-l border-(--neutral-100) px-3">
              <Clock size={16} />
              <span>Used {entry.usageCount} times</span>
            </div>
          </div>
          <p className="text-xs text-(--text-neutral-400)">Code: {entry.code}</p>
        </div>
      </div>

      {/* Expanded Content */}
      <div
        ref={expandedSectionRef}
        className={cn(
          "relative overflow-hidden transition-[max-height,opacity] duration-300 ease-in-out bg-(--neutral-50)",
          isExpanded
            ? "max-h-[31.25rem] opacity-100 border-t border-(--neutral-100)"
            : "max-h-0 opacity-0"
        )}
      >
        <ConnectedEntriesList
          portalContainerRef={expandedSectionRef}
          visibleEntries={visibleEntries}
          filteredEntries={filteredEntries}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          onDeleteEntry={(id) => {
            onDeleteConnection?.(id);
          }}
          selectedCategory={selectedCategory}
          onCategoryChange={setSelectedCategory}
          onDeleteAll={() => {
            setDeletedEntries(visibleEntries.map((e) => e.id));
            onDeleteAllConnections?.(entry.id);
          }}
          isDeleting={isDeletingConnection}
        />
      </div>
    </div>
  );
};

export default LibraryEntryCard;
