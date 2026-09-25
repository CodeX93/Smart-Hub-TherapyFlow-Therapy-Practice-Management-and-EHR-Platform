import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from "lucide-react";
import { Button } from "../ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import type { PaginationProps } from "../../types/pagination.type";

const Pagination = ({
  currentPage,
  totalPages,
  totalItems,
  itemsPerPage,
  onPageChange,
  className,
  variant = "default",
  itemsPerPageOptions,
  onItemsPerPageChange,
  hideLabels = false,
}: PaginationProps) => {
  const startIndex = (currentPage - 1) * itemsPerPage + 1;
  const endIndex = Math.min(currentPage * itemsPerPage, totalItems);

  const handlePreviousPage = () => {
    if (currentPage > 1) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      onPageChange(currentPage + 1);
    }
  };

  const handleFirstPage = () => {
    if (currentPage > 1) {
      onPageChange(1);
    }
  };

  const handleLastPage = () => {
    if (currentPage < totalPages) {
      onPageChange(totalPages);
    }
  };

  // Clinical forms variant
  if (variant === "clinical-forms") {
    return (
      <div
        className={`flex flex-col md:flex-row gap-2 items-center justify-between pt-4 w-full max-w-450 ${
          className || ""
        }`}
      >
        {/* Rows per page */}
        <div className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
          {!hideLabels && <span>Rows per page:</span>}
          <Select
            value={itemsPerPage.toString()}
            onValueChange={(value) => onItemsPerPageChange?.(Number(value))}
          >
            <SelectTrigger className="h-8 w-[4.375rem] rounded-lg border border-(--text-neutral-100) bg-white text-sm cursor-pointer">
              <SelectValue />
            </SelectTrigger>
            <SelectContent className="min-w-[4.375rem]">
              {itemsPerPageOptions?.map((option) => (
                <SelectItem
                  key={option}
                  value={option.toString()}
                  className="cursor-pointer"
                >
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Results and navigation */}
        <div className="flex items-center gap-4">
          {!hideLabels && (
            <span className="text-sm text-(--text-neutral-600)">
              {startIndex}-{endIndex} of {totalItems} results
            </span>
          )}
          <div className="flex items-center gap-1">
            <button
              onClick={handleFirstPage}
              disabled={currentPage === 1}
              className="w-8 h-8 rounded flex items-center justify-center hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer text-(--text-neutral-600)"
            >
              <ChevronsLeft className="size-4" />
            </button>
            <button
              onClick={handlePreviousPage}
              disabled={currentPage === 1}
              className="w-8 h-8 rounded flex items-center justify-center hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer text-(--text-neutral-600)"
            >
              <ChevronLeft className="size-4" />
            </button>
            <button
              onClick={handleNextPage}
              disabled={currentPage === totalPages}
              className="w-8 h-8 rounded flex items-center justify-center hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer text-(--text-neutral-600)"
            >
              <ChevronRight className="size-4" />
            </button>
            <button
              onClick={handleLastPage}
              disabled={currentPage === totalPages}
              className="w-8 h-8 rounded flex items-center justify-center hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer text-(--text-neutral-600)"
            >
              <ChevronsRight className="size-4" />
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Default variant (for invoices/documents)
  return (
    <div
      className={`flex items-center justify-end gap-4 mt-4 pt-4 w-full max-w-[112.5rem] ${
        className || ""
      }`}
    >
      <div className="flex items-center gap-4 text-[0.75rem] leading-4.5 text-(--text-neutral-600)">
        <span>
          {startIndex} - {endIndex} of {totalItems}
        </span>
        <span className="h-4 w-px bg-[#EDEEF1]" />
        <div className="flex items-center gap-2">
          <Select
            value={currentPage.toString()}
            onValueChange={(value) => onPageChange(Number(value))}
          >
            <SelectTrigger className="h-8 w-14 rounded-lg border border-[#EDEEF1] bg-[#F3F7F8] text-xs px-3">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {Array.from({ length: totalPages }, (_, index) => (
                <SelectItem key={index + 1} value={(index + 1).toString()}>
                  {index + 1}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <span>of {totalPages} Pages</span>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-lg border border-[#EDEEF1] bg-[#F3F7F8]"
          onClick={handlePreviousPage}
          disabled={currentPage === 1}
        >
          <ChevronLeft className="size-4 text-(--text-neutral-600)" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8 rounded-lg border border-[#1B1C20] bg-white"
          onClick={handleNextPage}
          disabled={currentPage === totalPages}
        >
          <ChevronRight className="size-4 text-(--text-primary-dark)" />
        </Button>
      </div>
    </div>
  );
};

export default Pagination;
