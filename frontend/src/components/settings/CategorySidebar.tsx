import { TrashIcon } from "@/components/icons/commonIcons";
import React from "react";
import { Search, Edit3 } from "lucide-react";
import { cn } from "@/lib/utils";
import CustomInput from "../form/CustomInput";
import type { OptionCategory } from "@/types/settings.types";

interface CategorySidebarProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  customCategories: OptionCategory[];
  systemCategories: OptionCategory[];
  selectedCategoryId: string | null;
  onSelectCategory: (id: string) => void;
  onEditCategory: (category: OptionCategory) => void;
  onDeleteCategory: (id: string, e: React.MouseEvent) => void;
}

const CategorySidebar = ({
  searchQuery,
  onSearchChange,
  customCategories,
  systemCategories,
  selectedCategoryId,
  onSelectCategory,
  onEditCategory,
  onDeleteCategory,
}: CategorySidebarProps) => {
  const hasSearchQuery = searchQuery.trim().length > 0;
  const hasNoResults =
    customCategories.length === 0 && systemCategories.length === 0;

  return (
    <div className="flex h-full min-h-0 w-full max-w-full flex-col overflow-hidden border-r border-(--neutral-100) md:w-91.25 md:max-w-91.25 md:shrink-0">
      <div className="shrink-0 border-b border-(--neutral-100) p-4">
        <CustomInput
          placeholder="Search categories..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
          className="min-h-10 w-full rounded-full pb-0 pt-1.75 shadow-xs"
        />
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-6 pt-4 custom-scrollbar">
        {hasNoResults ? (
          hasSearchQuery ? (
            <p className="py-6 text-center text-sm text-(--text-neutral-500)">
              No category found
            </p>
          ) : null
        ) : (
          <>
            {customCategories.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-xs font-medium text-(--text-neutral-600) uppercase tracking-wider mb-2">
                  Custom Categories
                </h4>
                {customCategories.map((category) => (
                  <div
                    key={category.id}
                    onClick={() => onSelectCategory(category.id)}
                    className={cn(
                      "group flex items-center justify-between p-4 rounded-xl border border-(--neutral-100) shadow-xs transition-all duration-300 cursor-pointer",
                      selectedCategoryId === category.id
                        ? "border-(--text-primary-500) bg-(--bg-primary-25)"
                        : "",
                    )}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h5 className="min-w-0 truncate text-sm font-semibold text-(--text-primary-dark)">
                          {category.name}
                        </h5>
                        {!category.isCustom && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[0.625rem] font-semibold bg-blue-50 text-blue-600 border border-blue-100">
                            System
                          </span>
                        )}
                      </div>
                      <p className="truncate text-sm text-(--text-neutral-600)">
                        {category.code}
                      </p>
                    </div>
                    <div className="ml-3 flex shrink-0 items-center gap-1">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onEditCategory(category);
                        }}
                        className="p-2.5 rounded-lg transition-all duration-300 ease-in-out hover:bg-(--neutral-100) cursor-pointer"
                      >
                        <Edit3 size={14} className="text-(--text-neutral-600)" />
                      </button>
                      {category.isCustom ? (
                        <button
                          onClick={(e) => onDeleteCategory(category.id, e)}
                          className="p-2.5 transition-all duration-300 rounded-lg ease-in-out hover:bg-red-50 hover:text-red-500 cursor-pointer"
                          aria-label={`Delete ${category.name}`}
                        >
                          <TrashIcon
                            size={14}
                            className="text-(--text-neutral-600) hover:text-red-500"
                          />
                        </button>
                      ) : null}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {systemCategories.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-xs font-medium text-(--text-neutral-600) uppercase tracking-wider mb-2">
                  System Categories
                </h4>
                {systemCategories.map((category) => (
                  <div
                    key={category.id}
                    onClick={() => onSelectCategory(category.id)}
                    className={cn(
                      "group flex items-center justify-between p-4 rounded-xl border border-(--neutral-100) shadow-xs transition-all duration-300 cursor-pointer",
                      selectedCategoryId === category.id
                        ? "border-(--text-primary-500) bg-(--bg-primary-25)"
                        : "",
                    )}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h5 className="min-w-0 truncate text-sm font-semibold text-(--text-primary-dark)">
                          {category.name}
                        </h5>
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[0.625rem] font-semibold bg-blue-50 text-blue-600 border border-blue-100">
                          System
                        </span>
                      </div>
                      <p className="truncate text-sm text-(--text-neutral-600)">
                        {category.code}
                      </p>
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onEditCategory(category);
                      }}
                      className="ml-3 shrink-0 p-2.5 rounded-lg transition-all duration-300 ease-in-out hover:bg-(--neutral-100) cursor-pointer"
                    >
                      <Edit3 size={14} className="text-(--text-neutral-600)" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default CategorySidebar;
