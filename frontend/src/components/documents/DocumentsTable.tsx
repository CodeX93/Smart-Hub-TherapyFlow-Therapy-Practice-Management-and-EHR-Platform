
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useRef, useState } from "react";
import { ChevronDown, Upload } from "lucide-react";
import { Button } from "../ui/button";
import { Search } from "lucide-react";
import UploadDocumentModal from "../shared/UploadDocumentModal";
import DocumentTableComponent from "../shared/DocumentTable";
import type { DocumentTableColumn } from "../../types/document.type";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { getDocumentIcon } from "../../utils/getDocExtension";
import CustomInput from "../form/CustomInput";
import type { PortalDocumentRow } from "@/utils/portalDocumentDisplay";
import EmptyDocumentsState from "@/components/documents/EmptyDocumentsState";
import { TrashIcon } from "@/components/icons/commonIcons";

interface UploadedFile {
  id: string;
  file: File;
  name: string;
  type: string;
}

interface DocumentsTableProps {
  documents: PortalDocumentRow[];
  isLoading?: boolean;
  isLoadingMore?: boolean;
  hasMore?: boolean;
  onLoadMore: () => void;
  searchQuery: string;
  onSearchChange: (value: string) => void;
  isUploading?: boolean;
  onUpload: (files: UploadedFile[], documentType: string) => Promise<void>;
  onView: (document: PortalDocumentRow) => void;
  onDownload: (document: PortalDocumentRow) => void;
  onDelete: (document: PortalDocumentRow) => void;
  viewingDocumentId: string | null;
  downloadingDocumentId: string | null;
  deletingDocumentId: string | null;
  scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
}

const DocumentsTable = ({
  documents,
  isLoading = false,
  isLoadingMore = false,
  hasMore = false,
  onLoadMore,
  searchQuery,
  onSearchChange,
  isUploading = false,
  onUpload,
  onView,
  onDownload,
  onDelete,
  viewingDocumentId,
  downloadingDocumentId,
  deletingDocumentId,
  scrollContainerRef,
}: DocumentsTableProps) => {
  const internalScrollRef = useRef<HTMLDivElement>(null);
  const listScrollRef = scrollContainerRef ?? internalScrollRef;
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore,
    hasMore,
    isLoading: isLoadingMore,
    scrollRootRef: listScrollRef,
  });

  const handleUpload = async (files: UploadedFile[], documentType: string) => {
    try {
      await onUpload(files, documentType);
      setIsUploadModalOpen(false);
    } catch {
      // Parent shows toast for upload errors.
    }
  };

  const columns: DocumentTableColumn<PortalDocumentRow>[] = [
    {
      key: "name",
      label: "Name",
      width: "38%",
      padding: "1.25rem 0.75rem",
      headerPadding: "0.75rem 0.75rem",
      render: (_, document) => {
        const iconSrc = getDocumentIcon(document.name);
        return (
          <div className="flex w-full min-w-0 items-center gap-2.5">
            <img
              src={iconSrc}
              alt=""
              className="h-8 w-8 shrink-0"
              aria-hidden="true"
            />
            <span
              className="block min-w-0 flex-1 truncate text-[0.875rem] leading-[1.375rem] font-normal text-(--text-primary-dark)"
              title={document.name}
            >
              {document.name}
            </span>
          </div>
        );
      },
    },
    {
      key: "category",
      label: "Category",
      width: "16%",
      padding: "1.25rem 0.75rem",
      headerPadding: "0.75rem 0.75rem",
      render: (value) => (
        <span className="block min-w-0 truncate" title={String(value ?? "")}>
          {value}
        </span>
      ),
    },
    {
      key: "size",
      label: "Size",
      width: "12%",
      align: "center",
      padding: "1.25rem 0.75rem",
      headerPadding: "0.75rem 0.75rem",
      render: (value) => (
        <span className="block min-w-0 truncate" title={String(value ?? "")}>
          {value}
        </span>
      ),
    },
    {
      key: "uploadedDate",
      label: "Uploaded Date",
      width: "18%",
      padding: "1.25rem 0.75rem",
      headerPadding: "0.75rem 0.75rem",
      headerContent: (
        <div className="flex items-center gap-1">
          Uploaded Date
          <ChevronDown className="size-4 text-(--text-primary-dark)" />
        </div>
      ),
      render: (value) => (
        <span className="block min-w-0 truncate" title={String(value ?? "")}>
          {value}
        </span>
      ),
    },
    {
      key: "actions",
      label: "Actions",
      width: "9.5rem",
      align: "center",
      padding: "1.25rem 0.5rem",
      headerPadding: "0.75rem 0.5rem",
      className: "w-[9.5rem]",
      render: (_, document) => (
        <div className="flex w-full items-center justify-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="h-9 w-9 shrink-0 cursor-pointer rounded-xl border border-(--text-neutral-100) bg-white p-0 hover:bg-gray-50"
            disabled={viewingDocumentId === document.id}
            loading={viewingDocumentId === document.id}
            loadingLabel={`Opening ${document.name}...`}
            onClick={() => onView(document)}
            aria-label={`View ${document.name}`}
          >
            <img
              src="/documents/view.svg"
              alt=""
              className="h-9 w-9"
              aria-hidden="true"
            />
          </Button>
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="h-9 w-9 shrink-0 cursor-pointer rounded-xl border border-(--text-neutral-100) bg-white p-0 hover:bg-gray-50"
            disabled={downloadingDocumentId === document.id}
            loading={downloadingDocumentId === document.id}
            loadingLabel={`Downloading ${document.name}...`}
            onClick={() => onDownload(document)}
            aria-label={`Download ${document.name}`}
          >
            <img
              src="/documents/download.svg"
              alt=""
              className="h-9 w-9"
              aria-hidden="true"
            />
          </Button>
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="h-9 w-9 shrink-0 cursor-pointer rounded-xl border border-(--text-neutral-100) bg-white p-0 text-(--text-neutral-600) hover:border-red-100 hover:bg-red-50 hover:text-red-600"
            disabled={deletingDocumentId === document.id}
            loading={deletingDocumentId === document.id}
            loadingLabel={`Deleting ${document.name}...`}
            onClick={() => onDelete(document)}
            aria-label={`Delete ${document.name}`}
          >
            <TrashIcon size={18} />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="flex min-h-0 w-full flex-1 flex-col overflow-hidden">
      <div className="mb-4 flex shrink-0 items-center justify-between gap-4">
        <h3 className="text-xl leading-6 font-semibold tracking-0 text-(--text-primary-dark)">
          Uploaded Documents
        </h3>
        <div className="flex items-center gap-2">
          <CustomInput
            placeholder="Search..."
            value={searchQuery}
            onChange={(event) => onSearchChange(event.target.value)}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="min-h-10 rounded-full pt-1.75 pb-0 md:w-79"
          />
          <Button
            type="button"
            onClick={() => setIsUploadModalOpen(true)}
            className="flex h-10 cursor-pointer items-center gap-2 rounded-full text-sm font-semibold"
          >
            <Upload className="size-4" />
            Upload Document
          </Button>
        </div>
      </div>

      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        {isLoading ? (
          <div className="flex flex-1 items-center justify-center gap-2">
            <ContentLoader variant="inline" size="md" />
            <span className="text-sm text-(--text-neutral-600)">
              Loading documents...
            </span>
          </div>
        ) : documents.length === 0 ? (
          <div className="flex flex-1 items-center justify-center rounded-lg border border-(--neutral-100) bg-white px-4">
            <EmptyDocumentsState
              title={
                searchQuery.trim()
                  ? "No documents match your search"
                  : "No documents found"
              }
              description={
                searchQuery.trim()
                  ? "Try a different search term"
                  : "Uploaded documents will appear here"
              }
            />
          </div>
        ) : (
          <div
            ref={listScrollRef}
            className="min-h-0 flex-1 overflow-x-hidden overflow-y-auto overscroll-contain rounded-xl border border-[#EDEEF1] bg-white shadow-[0px_2px_2px_0px_#1E282E0A]"
          >
            <DocumentTableComponent
              columns={columns}
              data={documents}
              rowKey={(document) => document.id}
              stickyHeader
              containerClassName="overflow-x-hidden overflow-visible border-0 shadow-none"
              containerStyle={{
                border: "none",
                boxShadow: "none",
                borderRadius: 0,
                background: "transparent",
              }}
            />
            <div
              ref={observerTarget}
              className="flex h-10 w-full items-center justify-center"
            >
              {isLoadingMore ? (
                <ContentLoader variant="inline" size="md" />
              ) : null}
            </div>
          </div>
        )}
      </div>

      <UploadDocumentModal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        onUpload={handleUpload}
        isUploading={isUploading}
      />
    </div>
  );
};

export default DocumentsTable;
