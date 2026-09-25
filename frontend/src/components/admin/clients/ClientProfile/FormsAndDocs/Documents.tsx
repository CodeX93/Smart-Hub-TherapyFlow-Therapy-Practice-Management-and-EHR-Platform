import type { UploadedFile } from "@/components/shared/UploadDocumentModal";
import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { AdminClientDocument } from "@/store/api/admin/clients.api";
import { Search, Plus, Eye, Download } from "lucide-react";
import { Button } from "../../../../ui/button";
import { Input } from "../../../../ui/input";
import { Switch } from "../../../../ui/switch";
import DocumentTable from "../../../../shared/DocumentTable";
import UploadDocumentModal from "../../../../shared/UploadDocumentModal";
import DocumentPreviewModal from "../../../../shared/DocumentPreviewModal";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import Toast from "@/components/shared/Toast";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import type { Client } from "@/types/client.type";
import { fetchWithAuth } from "@/utils/fetchWithAuth";
import {
  useDeleteClientDocumentMutation,
  useGetClientDocumentsQuery,
  useShareClientDocumentMutation,
  useUploadClientDocumentMutation,
} from "@/store/api/admin/clients.api";
import { getApiErrorMessage } from "@/utils/apiError";
import EmptyDocumentsState from "@/components/documents/EmptyDocumentsState";

interface DocumentsProps {
  client: Client;
  isActive?: boolean;
  readOnly?: boolean;
}

type DocumentAssetResult =
  | { kind: "url"; value: string }
  | { kind: "blob"; value: Blob };

const normalizeDocumentUrl = (value: string): string => {
  const trimmed = value.trim();
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1);
  }
  return trimmed;
};

const getDocumentIcon = (filename: string): string => {
  const extension = filename.split(".").pop()?.toLowerCase();
  if (extension === "pdf") return "/documents/pdf.svg";
  if (["doc", "docx"].includes(extension || "")) return "/documents/doc.svg";
  if (["jpg", "jpeg", "png", "gif", "svg", "webp", "bmp"].includes(extension || "")) {
    return "/documents/img.svg";
  }
  return "/documents/doc.svg";
};

const formatBytes = (value?: number): string => {
  if (!value || value <= 0) return "-";
  const units = ["B", "KB", "MB", "GB"];
  let size = value;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }
  return `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unitIndex]}`;
};

const Documents = ({ client, isActive = true, readOnly = false }: DocumentsProps) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [allDocuments, setAllDocuments] = useState<AdminClientDocument[]>([]);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isUploadInProgress, setIsUploadInProgress] = useState(false);
  const [previewDoc, setPreviewDoc] = useState<{
    id: string;
    name: string;
    size?: string;
    uploadedDate?: string;
    previewUrl?: string;
    downloadUrl?: string;
    mimeType?: string;
    previewHtml?: string;
  } | null>(null);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [deleteDocId, setDeleteDocId] = useState<string | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [viewingId, setViewingId] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [sharingId, setSharingId] = useState<string | null>(null);

  const itemsPerPage = 20;
  const clientId = Number(client.id);

  const {
    data: documentsResponse,
    isLoading,
    isFetching,
    refetch,
  } = useGetClientDocumentsQuery(
    {
      clientId,
      page,
      pageSize: itemsPerPage,
      search: searchQuery || undefined,
    },
    {
      refetchOnMountOrArgChange: true,
    },
  );

  const previousIsActiveRef = useRef(isActive);

  useEffect(() => {
    setPage(1);
    setTotalPages(1);
    setAllDocuments([]);
    setIsLoadingMore(false);
  }, [clientId, searchQuery, setPage]);

  useEffect(() => {
    const becameActive = isActive && !previousIsActiveRef.current;
    previousIsActiveRef.current = isActive;
    if (!becameActive) return;
    void refetch();
  }, [isActive, refetch]);

  useEffect(() => {
    if (!documentsResponse) return;

    const pageItems = documentsResponse.items ?? [];
    setTotalPages(Math.max(documentsResponse.totalPages ?? 1, 1));

    setAllDocuments((previous) => {
      if (page <= 1) return pageItems;

      const existingIds = new Set(previous.map((entry) => Number(entry.id)));
      const newItems = pageItems.filter((entry) => !existingIds.has(Number(entry.id)));

      if (newItems.length === 0) return previous;

      return [...previous, ...newItems];
    });
    setIsLoadingMore(false);
  }, [documentsResponse, page]);

  const [deleteDocument] = useDeleteClientDocumentMutation();
  const [uploadClientDocument] = useUploadClientDocumentMutation();
  const [shareClientDocument] = useShareClientDocumentMutation();

  const [isPreviewLoading, setIsPreviewLoading] = useState(false);

  const fetchDocumentAsset = useCallback(
    async (path: string): Promise<DocumentAssetResult> => {
      const response = await fetchWithAuth(path);

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const contentType = response.headers.get("content-type") || "";
      if (contentType.includes("application/json")) {
        const text = normalizeDocumentUrl(await response.text());
        return { kind: "url", value: text };
      }

      return { kind: "blob", value: await response.blob() };
    },
    [],
  );

  const handleLoadMore = useCallback(() => {
    if (isLoadingMore || isLoading || isFetching) return;
    if (page >= totalPages) return;
    setIsLoadingMore(true);
    setPage((prev) => prev + 1);
  }, [isLoadingMore, isLoading, isFetching, page, totalPages, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: page < totalPages,
    isLoading: isLoadingMore || isLoading || isFetching,
  });

  const normalizedDocuments = useMemo(
    () =>
      allDocuments.map((doc) => ({
        id: String(doc.id),
        name: doc.originalName || doc.fileName || "Untitled document",
        shareInPortal: Boolean(doc.shareWithClient),
        size: formatBytes(doc.fileSize),
        uploadedDate: doc.uploadedAt
          ? new Date(doc.uploadedAt).toLocaleDateString("en-US", {
              month: "short",
              day: "2-digit",
              year: "numeric",
            })
          : "-",
        previewUrl: doc.previewUrl,
        downloadUrl: doc.downloadUrl,
      })),
    [allDocuments],
  );

  const isInitialLoading =
    normalizedDocuments.length === 0 && (isLoading || isFetching);

  const handleUpload = async (files: UploadedFile[], documentType: string) => {
    setIsUploadInProgress(true);
    try {
      await Promise.all(
        files.map((entry) =>
          uploadClientDocument({
            clientId,
            file: entry.file as File,
            documentType,
          }).unwrap(),
        ),
      );
      setPage(1);
      setTotalPages(1);
      setAllDocuments([]);
      setIsLoadingMore(false);
      setIsUploadModalOpen(false);
      setToastType("success");
      setToastMessage("Document uploaded successfully.");
      void refetch();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setIsUploadInProgress(false);
    }
  };

  const handleView = async (doc: (typeof normalizedDocuments)[number]) => {
    const parsedId = Number(doc.id);
    if (!parsedId) return;
    setViewingId(doc.id);
    setIsPreviewLoading(true);
    setIsPreviewModalOpen(true);
    setPreviewDoc({
      id: doc.id,
      name: doc.name,
      size: doc.size,
      uploadedDate: doc.uploadedDate,
    });

    try {
      const payload = await fetchDocumentAsset(
        `/api/v1/clients/${clientId}/documents/${parsedId}/viewer`,
      );
      let objectPreviewUrl: string | undefined = doc.previewUrl;
      let mimeType: string | undefined;
      let previewHtml: string | undefined;

      if (payload.kind === "url") {
        const trimmed = payload.value.trim();
        objectPreviewUrl = trimmed.startsWith("http://") || trimmed.startsWith("https://")
          ? trimmed
          : doc.previewUrl;
      } else {
        mimeType = payload.value.type || undefined;
        if (
          mimeType?.includes("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        ) {
          const [buffer, { default: mammoth }] = await Promise.all([
            payload.value.arrayBuffer(),
            import("mammoth"),
          ]);
          const result = await mammoth.convertToHtml({ arrayBuffer: buffer });
          previewHtml = result.value;
        } else {
          objectPreviewUrl = window.URL.createObjectURL(payload.value);
        }
      }

      setPreviewDoc({
        id: doc.id,
        name: doc.name,
        size: doc.size,
        uploadedDate: doc.uploadedDate,
        previewUrl: objectPreviewUrl,
        downloadUrl: doc.downloadUrl,
        mimeType,
        previewHtml,
      });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setIsPreviewModalOpen(false);
      setPreviewDoc(null);
    } finally {
      setViewingId(null);
      setIsPreviewLoading(false);
    }
  };

  const handleDownload = async (doc: (typeof normalizedDocuments)[number]) => {
    const parsedId = Number(doc.id);
    if (!parsedId) return;
    setDownloadingId(doc.id);
    setToastType("info");
    setToastMessage("Preparing download...");
    try {
      const payload = await fetchDocumentAsset(
        `/api/v1/clients/${clientId}/documents/${parsedId}/download`,
      );
      if (payload.kind === "url") {
        const trimmed = payload.value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
          const response = await fetch(trimmed);
          if (!response.ok) {
            throw new Error(`Download failed with status ${response.status}`);
          }
          const blob = await response.blob();
          const fileUrl = window.URL.createObjectURL(blob);
          const link = document.createElement("a");
          link.href = fileUrl;
          link.download = doc.name;
          document.body.appendChild(link);
          link.click();
          link.remove();
          window.URL.revokeObjectURL(fileUrl);
        } else {
          const blob = new Blob([payload.value], { type: "text/plain" });
          const fileUrl = window.URL.createObjectURL(blob);
          const link = document.createElement("a");
          link.href = fileUrl;
          link.download = doc.name;
          document.body.appendChild(link);
          link.click();
          link.remove();
          window.URL.revokeObjectURL(fileUrl);
        }
      } else {
        const fileUrl = window.URL.createObjectURL(payload.value);
        const link = document.createElement("a");
        link.href = fileUrl;
        link.download = doc.name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(fileUrl);
      }
      setToastType("success");
      setToastMessage("Document downloaded successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setDownloadingId(null);
    }
  };

  const handleDeleteClick = (id: string) => {
    setDeleteDocId(id);
    setIsDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deleteDocId) return;
    const parsedId = Number(deleteDocId);
    if (!parsedId) return;
    setDeletingId(deleteDocId);
    try {
      await deleteDocument({ clientId, id: parsedId }).unwrap();
      setAllDocuments((previous) =>
        previous.filter((entry) => Number(entry.id) !== parsedId),
      );
      setToastType("success");
      setToastMessage("Document deleted successfully.");
      setIsDeleteModalOpen(false);
      setDeleteDocId(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setDeletingId(null);
    }
  };

  const handleToggleShare = async (doc: (typeof normalizedDocuments)[number], checked: boolean) => {
    const parsedId = Number(doc.id);
    if (!parsedId || readOnly) return;
    if (Boolean(doc.shareInPortal) === checked) return;
    setSharingId(doc.id);
    const previousValue = doc.shareInPortal;
    try {
      await shareClientDocument({
        clientId,
        id: parsedId,
        shareWithClient: checked,
      }).unwrap();
      setAllDocuments((previous) =>
        previous.map((entry) => {
          if (Number(entry.id) !== parsedId) return entry;
          return {
            ...entry,
            shareWithClient: checked,
          };
        }),
      );
      setToastType("success");
      setToastMessage(
        `Document ${checked ? "shared" : "unshared"} with portal successfully.`,
      );
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setAllDocuments((previous) =>
        previous.map((entry) => {
          if (Number(entry.id) !== parsedId) return entry;
          return {
            ...entry,
            shareWithClient: previousValue,
          };
        }),
      );
    } finally {
      setSharingId(null);
    }
  };

  const columns = [
    {
      key: "name",
      label: "Name",
      headerContent: "Name",
      width: "36%",
      padding: "0 1.5rem",
      headerPadding: "0 1.5rem",
      render: (value: string) => (
        <div className="flex items-center gap-3 min-w-0">
          <img
            src={getDocumentIcon(value)}
            alt=""
            className="w-8 h-8 rounded"
            onError={(event) =>
              ((event.target as HTMLImageElement).src = "/documents/doc.svg")
            }
          />
          <span className="font-medium text-gray-900 truncate block max-w-[7.5rem] sm:max-w-[10.625rem] md:max-w-[13.75rem] lg:max-w-[17.5rem] xl:max-w-[21.25rem]">
            {value}
          </span>
        </div>
      ),
    },
    {
      key: "size",
      label: "Size",
      width: "12%",
      padding: "0 0.5rem",
      render: (value: string) => <span className="text-gray-500">{value}</span>,
    },
    {
      key: "uploadedDate",
      label: "Uploaded Date",
      width: "16%",
      padding: "0 0.5rem",
      render: (value: string) => <span className="text-gray-500">{value}</span>,
    },
    {
      key: "shareInPortal",
      label: "Share in Portal",
      width: "15%",
      padding: "0 0.5rem",
      render: (value: boolean, row: (typeof normalizedDocuments)[number]) => (
        <div className="flex items-center justify-center">
          {sharingId === row.id ? (
            <ContentLoader variant="inline" size="sm" />
          ) : (
            <Switch
              checked={Boolean(value)}
              disabled={readOnly}
              onCheckedChange={(nextChecked) => {
                void handleToggleShare(row, nextChecked);
              }}
              className="data-[state=checked]:bg-[var(--bg-primary-dark)]"
            />
          )}
        </div>
      ),
    },
    {
      key: "actions",
      label: "Actions",
      width: "20%",
      padding: "0 0.5rem",
      render: (_: unknown, row: (typeof normalizedDocuments)[number]) => (
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              void handleView(row);
            }}
            disabled={viewingId === row.id}
            className="h-9 w-9 border border-gray-200 rounded-lg flex items-center justify-center text-gray-500 hover:bg-gray-50 hover:text-gray-700 transition-colors bg-white cursor-pointer disabled:opacity-60"
            title="View"
          >
            {viewingId === row.id ? (
              <ContentLoader variant="inline" size="sm" />
            ) : (
              <Eye size={18} />
            )}
          </button>
          <button
            onClick={() => {
              void handleDownload(row);
            }}
            disabled={downloadingId === row.id}
            className="h-9 w-9 border border-gray-200 rounded-lg flex items-center justify-center text-gray-500 hover:bg-gray-50 hover:text-gray-700 transition-colors bg-white cursor-pointer disabled:opacity-60"
            title="Download"
          >
            {downloadingId === row.id ? (
              <ContentLoader variant="inline" size="sm" />
            ) : (
              <Download size={18} />
            )}
          </button>
          {!readOnly ? (
          <button
            onClick={() => handleDeleteClick(row.id)}
            disabled={deletingId === row.id}
            className="h-9 w-9 border border-gray-200 rounded-lg flex items-center justify-center text-gray-500 hover:text-red-600 hover:bg-red-50 hover:border-red-100 transition-colors bg-white cursor-pointer disabled:opacity-60"
            title="Delete"
          >
            {deletingId === row.id ? (
              <ContentLoader variant="inline" size="sm" />
            ) : (
              <TrashIcon size={18} />
            )}
          </button>
          ) : null}
        </div>
      ),
    },
  ];

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden animate-in fade-in duration-300">
      <div className="flex items-center justify-between shrink-0">
        <h3 className="text-lg font-bold text-gray-900">Document Management</h3>
        <div className="flex items-center gap-4">
          <div className="relative">
            <Search
 className="size-4.5 absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            <Input
              placeholder="Search"
              value={searchQuery}
              onChange={(event) => {
                setSearchQuery(event.target.value);
                setPage(1);
              }}
              className="pl-11 pr-4 w-[20rem] h-[3rem] rounded-full border-gray-200 bg-white shadow-sm focus-visible:ring-1 focus-visible:ring-[var(--bg-primary-dark)] text-sm"
            />
          </div>
          {!readOnly ? (
          <Button
            onClick={() => setIsUploadModalOpen(true)}
            className="h-[3rem] px-6 rounded-full bg-[var(--bg-primary-dark)] text-white hover:bg-[var(--bg-primary-dark)]/90 font-medium text-sm shadow-sm"
          >
            <Plus className="mr-2" size={18} />
            Upload Document
          </Button>
          ) : null}
        </div>
      </div>
      <p className="text-xs text-gray-500">
        Share in Portal controls client visibility only; this action does not send email notifications.
      </p>

      <div className="flex-1 min-h-0 border border-gray-200 rounded-3xl shadow-sm bg-white overflow-hidden flex flex-col relative">
        <div className="flex-1 min-h-0 overflow-y-auto">
          {isInitialLoading ? (
            <ContentLoader size="lg" className="py-10" />
          ) : (
            <>
              <div className="w-full overflow-x-auto">
                <DocumentTable
                  columns={columns}
                  data={normalizedDocuments}
                  containerClassName="border-none shadow-none rounded-none min-w-[51.25rem]"
                  containerStyle={{ borderRadius: 0, border: "none", boxShadow: "none" }}
                  headerHeight="3.25rem"
                  rowHeight="3.25rem"
                />
              </div>
              {normalizedDocuments.length === 0 ? (
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
              ) : null}
            </>
          )}
        </div>
        {normalizedDocuments.length > 0 && page < totalPages ? (
          <div ref={observerTarget} className="h-12 flex items-center justify-center shrink-0">
            {isLoadingMore || isFetching ? (
              <ContentLoader variant="inline" size="md" />
            ) : null}
          </div>
        ) : null}
      </div>

      <UploadDocumentModal
        isOpen={isUploadModalOpen}
        onClose={() => {
          if (!isUploadInProgress) setIsUploadModalOpen(false);
        }}
        onUpload={(files, type) => {
          void handleUpload(files, type);
        }}
        isUploading={isUploadInProgress}
      />

      <DocumentPreviewModal
        isOpen={isPreviewModalOpen}
        onClose={() => {
          if (previewDoc?.previewUrl?.startsWith("blob:")) {
            window.URL.revokeObjectURL(previewDoc.previewUrl);
          }
          setIsPreviewModalOpen(false);
        }}
        document={previewDoc}
        isLoading={isPreviewLoading}
        onDownload={() => {
          if (!previewDoc) return;
          const row = normalizedDocuments.find((doc) => doc.id === previewDoc.id);
          if (!row) return;
          void handleDownload(row);
        }}
      />

      <ConfirmationModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={() => {
          void handleConfirmDelete();
        }}
        title="Delete Document"
        description="Are you sure you want to delete this document? This action cannot be undone."
        confirmButtonText={deletingId ? "Deleting..." : "Delete"}
        confirmButtonLoading={Boolean(deletingId)}
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
