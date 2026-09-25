export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  itemsPerPage: number;
  onPageChange: (page: number) => void;
  className?: string;
  variant?: "default" | "clinical-forms";
  itemsPerPageOptions?: number[];
  onItemsPerPageChange?: (itemsPerPage: number) => void;
  hideLabels?: boolean;
}