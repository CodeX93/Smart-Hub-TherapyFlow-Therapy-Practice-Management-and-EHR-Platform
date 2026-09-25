/* eslint-disable @typescript-eslint/no-explicit-any */
export interface Document {
  id: string;
  name: string;
  category: string;
  size: string;
  uploadedDate: string;
}

export interface DocumentTableColumn<T = any> {
  key: string;
  label: string;
  width: string;
  padding?: string;
  headerPadding?: string;
  align?: "left" | "center" | "right";
  headerContent?: React.ReactNode;
  render?: (value: any, row: T, index: number) => React.ReactNode;
  className?: string;
  headerClassName?: string;
}

export interface DocumentTableProps<T = any> {
  columns: DocumentTableColumn<T>[];
  data: T[];
  containerClassName?: string;
  containerStyle?: React.CSSProperties;
  rowHeight?: string;
  headerHeight?: string;
  headerCellStyle?: React.CSSProperties;
  stickyHeader?: boolean;
  rowKey?: (row: T, index: number) => string | number;
  getRowClassName?: (row: T, index: number) => string;
  getRowStyle?: (row: T, index: number) => React.CSSProperties;
}

export interface DocumentsTableProps {
  documents: Document[];
  itemsPerPage?: number;
}
