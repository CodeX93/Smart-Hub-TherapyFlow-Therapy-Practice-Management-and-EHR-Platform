import { useEffect, useMemo, useRef, useState } from "react";
import DocumentsTable from "../../../components/documents/DocumentsTable";
import DocumentPreviewModal from "@/components/shared/DocumentPreviewModal";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useDeletePortalDocumentMutation,
  useGetPortalDocumentsQuery,
  useUploadPortalDocumentMutation,
  validatePortalDocumentFile,
  PORTAL_DOCUMENTS_PAGE_SIZE,
  type PortalDocument,
} from "@/store/api/portalApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  downloadPortalDocumentBlob,
  filterPortalDocuments,
  mapPortalDocumentToRow,
  type PortalDocumentRow,
} from "@/utils/portalDocumentDisplay";
import {
  fetchPortalDocumentAsset,
  getPortalDocumentDownloadPath,
  getPortalDocumentViewPath,
} from "@/utils/portalDocumentAssets";
import ConfirmationModal from "@/components/shared/ConfirmationModal";

interface PreviewDocumentState {
  id: string;
  name: string;
  size?: string;
  uploadedDate?: string;
  previewUrl?: string;
  mimeType?: string;
  previewHtml?: string;
}

const Documents = () => {
  const listScrollRef = useRef<HTMLDivElement>(null);
  const previewObjectUrlRef = useRef<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(1);
  const [accumulatedDocuments, setAccumulatedDocuments] = useState<PortalDocument[]>(
    [],
  );
  const [totalPages, setTotalPages] = useState(1);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">(
    "error",
  );
  const [viewingDocumentId, setViewingDocumentId] = useState<string | null>(
    null,
  );
  const [downloadingDocumentId, setDownloadingDocumentId] = useState<
    string | null
  >(null);
  const [deletingDocumentId, setDeletingDocumentId] = useState<string | null>(
    null,
  );
  const [deleteDocId, setDeleteDocId] = useState<string | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [previewDoc, setPreviewDoc] = useState<PreviewDocumentState | null>(
    null,
  );
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);

  const listQueryArgs = useMemo(
    () => ({
      page,
      pageSize: PORTAL_DOCUMENTS_PAGE_SIZE,
    }),
    [page],
  );

  const {
    data: documentsPage,
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useGetPortalDocumentsQuery(listQueryArgs, {
    refetchOnMountOrArgChange: true,
  });

  const [uploadPortalDocument, { isLoading: isUploading }] =
    useUploadPortalDocumentMutation();
  const [deletePortalDocument] = useDeletePortalDocumentMutation();

  useEffect(() => {
    if (!documentsPage) return;

    setTotalPages(documentsPage.totalPages || 1);

    setAccumulatedDocuments((current) => {
      if (documentsPage.page <= 1) {
        return documentsPage.items;
      }

      const existingIds = new Set(current.map((document) => document.id));
      const nextItems = documentsPage.items.filter(
        (document) => !existingIds.has(document.id),
      );
      return [...current, ...nextItems];
    });
  }, [documentsPage]);

  const documentRows = useMemo(
    () => accumulatedDocuments.map(mapPortalDocumentToRow),
    [accumulatedDocuments],
  );

  const filteredDocuments = useMemo(
    () => filterPortalDocuments(documentRows, searchQuery),
    [documentRows, searchQuery],
  );

  const hasMore = page < totalPages;
  const isInitialLoading =
    (isLoading || isFetching) && accumulatedDocuments.length === 0;
  const isLoadingMore =
    isFetching && page > 1 && accumulatedDocuments.length > 0;

  const handleLoadMore = () => {
    if (!hasMore || isFetching) return;
    setPage((currentPage) => currentPage + 1);
  };

  useEffect(() => {
    if (!isError) return;
    setToastType("error");
    setToastMessage(getApiErrorMessage(error));
  }, [error, isError]);

  const clearPreviewObjectUrl = () => {
    if (previewObjectUrlRef.current) {
      window.URL.revokeObjectURL(previewObjectUrlRef.current);
      previewObjectUrlRef.current = null;
    }
  };

  const handleClosePreview = () => {
    setIsPreviewModalOpen(false);
    setPreviewDoc(null);
    setIsPreviewLoading(false);
    setViewingDocumentId(null);
    clearPreviewObjectUrl();
  };

  const resolveDocumentAsset = async (
    path: string,
    fallbackUrl?: string,
  ): Promise<{
    previewUrl?: string;
    mimeType?: string;
    previewHtml?: string;
    blob?: Blob;
  }> => {
    const payload = await fetchPortalDocumentAsset(path);

    if (payload.kind === "url") {
      const trimmed = payload.value.trim();
      if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return { previewUrl: trimmed };
      }

      return { previewUrl: fallbackUrl };
    }

    const mimeType = payload.value.type || undefined;

    if (
      mimeType?.includes(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      )
    ) {
      const [buffer, { default: mammoth }] = await Promise.all([
        payload.value.arrayBuffer(),
        import("mammoth"),
      ]);
      const result = await mammoth.convertToHtml({ arrayBuffer: buffer });
      return { previewHtml: result.value, mimeType };
    }

    clearPreviewObjectUrl();
    const objectPreviewUrl = window.URL.createObjectURL(payload.value);
    previewObjectUrlRef.current = objectPreviewUrl;

    return {
      previewUrl: objectPreviewUrl,
      mimeType,
      blob: payload.value,
    };
  };

  const handleView = async (document: PortalDocumentRow) => {
    const parsedId = Number.parseInt(document.id, 10);
    if (Number.isNaN(parsedId)) return;

    setViewingDocumentId(document.id);
    setIsPreviewLoading(true);
    setIsPreviewModalOpen(true);
    setPreviewDoc({
      id: document.id,
      name: document.name,
      size: document.size,
      uploadedDate: document.uploadedDate,
    });
    setToastMessage(null);

    try {
      const asset = await resolveDocumentAsset(
        getPortalDocumentViewPath(parsedId),
        document.previewUrl,
      );

      setPreviewDoc({
        id: document.id,
        name: document.name,
        size: document.size,
        uploadedDate: document.uploadedDate,
        previewUrl: asset.previewUrl,
        mimeType: asset.mimeType || document.mimeType,
        previewHtml: asset.previewHtml,
      });
    } catch (viewError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(viewError));
      handleClosePreview();
    } finally {
      setIsPreviewLoading(false);
      setViewingDocumentId(null);
    }
  };

  const handleDownload = async (document: PortalDocumentRow) => {
    const parsedId = Number.parseInt(document.id, 10);
    if (Number.isNaN(parsedId)) return;

    setDownloadingDocumentId(document.id);
    setToastType("info");
    setToastMessage("Preparing download...");

    try {
      const payload = await fetchPortalDocumentAsset(
        getPortalDocumentDownloadPath(parsedId),
      );

      if (payload.kind === "url") {
        const trimmed = payload.value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
          const response = await fetch(trimmed);
          if (!response.ok) {
            throw new Error(`Download failed with status ${response.status}`);
          }
          downloadPortalDocumentBlob(
            await response.blob(),
            document.name,
          );
        } else {
          const blob = new Blob([payload.value], { type: "text/plain" });
          downloadPortalDocumentBlob(blob, document.name);
        }
      } else {
        downloadPortalDocumentBlob(payload.value, document.name);
      }

      setToastType("success");
      setToastMessage("Document downloaded successfully.");
    } catch (downloadError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(downloadError));
    } finally {
      setDownloadingDocumentId(null);
    }
  };

  const handleDeleteClick = (document: PortalDocumentRow) => {
    setDeleteDocId(document.id);
    setIsDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deleteDocId) return;
    const parsedId = Number.parseInt(deleteDocId, 10);
    if (Number.isNaN(parsedId)) return;

    setDeletingDocumentId(deleteDocId);
    try {
      await deletePortalDocument(parsedId).unwrap();
      setAccumulatedDocuments((current) =>
        current.filter((document) => String(document.id) !== deleteDocId),
      );
      setIsDeleteModalOpen(false);
      setDeleteDocId(null);
      setToastType("success");
      setToastMessage("Document deleted successfully.");
      void refetch();
    } catch (deleteError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(deleteError));
    } finally {
      setDeletingDocumentId(null);
    }
  };

  const handleUpload = async (
    files: { file: File }[],
    documentType: string,
  ) => {
    const fileEntry = files[0];
    if (!fileEntry) return;

    const validationError = validatePortalDocumentFile(fileEntry.file);
    if (validationError) {
      setToastType("error");
      setToastMessage(validationError);
      return;
    }

    try {
      setToastMessage(null);
      await uploadPortalDocument({
        file: fileEntry.file,
        documentType,
      }).unwrap();
      setPage(1);
      setAccumulatedDocuments([]);
      setTotalPages(1);
      setToastType("success");
      setToastMessage("Document uploaded successfully.");
      void refetch();
    } catch (uploadError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(uploadError));
      throw uploadError;
    }
  };

  return (
    <div className="flex h-[calc(100vh-10rem)] min-h-0 flex-col overflow-hidden">
      <ScrollToTopButton containerRef={listScrollRef} />
      <DocumentsTable
        documents={filteredDocuments}
        isLoading={isInitialLoading}
        isLoadingMore={isLoadingMore}
        hasMore={hasMore}
        onLoadMore={handleLoadMore}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        isUploading={isUploading}
        onUpload={handleUpload}
        onView={(document) => void handleView(document)}
        onDownload={(document) => void handleDownload(document)}
        onDelete={handleDeleteClick}
        viewingDocumentId={viewingDocumentId}
        downloadingDocumentId={downloadingDocumentId}
        deletingDocumentId={deletingDocumentId}
        scrollContainerRef={listScrollRef}
      />
      <DocumentPreviewModal
        isOpen={isPreviewModalOpen}
        onClose={handleClosePreview}
        isLoading={isPreviewLoading}
        document={previewDoc}
        onDownload={() => {
          if (!previewDoc) return;
          const row = filteredDocuments.find((doc) => doc.id === previewDoc.id);
          if (row) {
            void handleDownload(row);
          }
        }}
      />
      <ConfirmationModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          if (deletingDocumentId) return;
          setIsDeleteModalOpen(false);
          setDeleteDocId(null);
        }}
        onConfirm={() => {
          void handleConfirmDelete();
        }}
        title="Delete Document"
        description="Are you sure you want to delete this document? This action cannot be undone."
        confirmButtonText={deletingDocumentId ? "Deleting..." : "Delete"}
        confirmButtonLoading={Boolean(deletingDocumentId)}
        type="delete"
        items={[]}
      />
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

export default Documents;
