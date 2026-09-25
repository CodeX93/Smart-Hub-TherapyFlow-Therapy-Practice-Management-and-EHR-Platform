import { X, FileText, Download } from 'lucide-react';
import { Button } from "../ui/button";
import { sanitizeHtml } from "@/utils/sanitizeHtml";

interface DocumentPreviewModalProps {
    isOpen: boolean;
    onClose: () => void;
    onDownload?: () => void;
    isLoading?: boolean;
    document: {
        id: string;
        name: string;
        size?: string;
        uploadedDate?: string;
        previewUrl?: string;
        mimeType?: string;
        previewHtml?: string;
    } | null;
}

const DocumentPreviewModal = ({
    isOpen,
    onClose,
    document,
    onDownload,
    isLoading = false,
}: DocumentPreviewModalProps) => {
    if (!isOpen || !document) return null;

    const extension = document.name.split('.').pop()?.toLowerCase();
    const mimeType = document.mimeType?.toLowerCase() || "";
    const isImage =
        mimeType.startsWith("image/") ||
        ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension || '');
    const isPdf =
        mimeType === "application/pdf" ||
        extension === 'pdf';
    const isWordDocument =
        mimeType.includes("application/msword") ||
        mimeType.includes("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
        ['doc', 'docx'].includes(extension || '');
    const hasDocxHtmlPreview = Boolean(document.previewHtml?.trim());
    const previewUrl = document.previewUrl || "";

    return (
        <div className="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm animate-in fade-in duration-200">
            <div className="app-modal-surface rounded-2xl w-full max-w-4xl h-[85vh] animate-in zoom-in-95 duration-200 flex flex-col relative overflow-hidden">
                {/* Header */}
                <div className="flex items-center justify-between gap-4 px-6 py-4 border-b border-gray-100 bg-white z-10">
                    <div className="flex min-w-0 flex-1 items-center gap-3">
                        <div className="shrink-0 rounded-lg bg-gray-100 p-2">
                            <FileText size={20} className="text-gray-600" />
                        </div>
                        <div className="min-w-0">
                            <h3
                              className="truncate text-base font-semibold text-gray-900"
                              title={document.name}
                            >
                              {document.name}
                            </h3>
                            <p className="truncate text-xs text-gray-500">
                                {document.size} • Uploaded {document.uploadedDate}
                            </p>
                        </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                        <Button
                            variant="outline"
                            onClick={onDownload}
                            className="h-9 px-3 gap-2 text-gray-600 cursor-pointer"
                        >
                            <Download size={16} />
                            Download
                        </Button>
                        <button
                            type="button"
                            onClick={onClose}
                            className="shrink-0 cursor-pointer rounded-full p-2 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900"
                        >
                            <X size={20} />
                        </button>
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 bg-gray-50 p-6 overflow-hidden flex items-center justify-center relative">
                    <div className="w-full h-full bg-white shadow-sm border border-gray-200 rounded-xl overflow-hidden flex items-center justify-center">
                        {isLoading ? (
                            <div className="text-gray-400 flex flex-col items-center gap-2">
                                <span className="font-medium">Loading preview...</span>
                            </div>
                        ) : hasDocxHtmlPreview ? (
                            <div className="h-full w-full overflow-auto bg-white px-8 py-6 text-left">
                                <div
                                    className="mx-auto max-w-4xl text-sm leading-6 text-gray-800 break-words"
                                    dangerouslySetInnerHTML={{ __html: sanitizeHtml(document.previewHtml) }}
                                />
                            </div>
                        ) : previewUrl && isImage ? (
                            <img src={previewUrl} alt={document.name} className="max-w-full max-h-full object-contain" />
                        ) : previewUrl && isPdf ? (
                            <iframe src={previewUrl} title={document.name} className="w-full h-full border-0" />
                        ) : isImage ? (
                            <div className="text-gray-400 flex flex-col items-center gap-2">
                                <img src="/documents/img.svg" alt="Preview" className="w-24 h-24 opacity-50" />
                                <span className="font-medium">Image Preview ({extension})</span>
                            </div>
                        ) : isPdf ? (
                            <div className="text-gray-400 flex flex-col items-center gap-2">
                                <img src="/documents/pdf.svg" alt="Preview" className="w-24 h-24 opacity-50" />
                                <span className="font-medium">PDF Preview</span>
                                <p className="text-sm max-w-md text-center text-gray-400">
                                    Mock preview. In a real application, this would display the PDF using an iframe or PDF viewer.
                                </p>
                            </div>
                        ) : (
                            <div className="text-gray-400 flex flex-col items-center gap-2">
                                <FileText size={48} className="opacity-20" />
                                <span className="font-medium">
                                    {isWordDocument ? "Word preview not available" : "No preview available"}
                                </span>
                                <p className="text-sm text-gray-400 text-center max-w-md">
                                    {isWordDocument
                                        ? "This document format cannot be rendered in the browser. .docx is supported when the file can be converted to HTML, but legacy .doc still needs backend conversion."
                                        : `This file type (${extension}) cannot be previewed.`}
                                </p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default DocumentPreviewModal;
