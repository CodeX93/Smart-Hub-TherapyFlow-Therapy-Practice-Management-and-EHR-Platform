import type { UploadedFile } from "@/components/shared/UploadDocumentModal";
import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useState } from 'react';
import { Search, Plus, Eye, Download } from "lucide-react";
import { Button } from "../../../ui/button";
import { Input } from "../../../ui/input";
import { Switch } from "../../../ui/switch";
import DocumentTable from '../../../shared/DocumentTable';
import Pagination from '../../../shared/Pagination';
import UploadDocumentModal from '../../../shared/UploadDocumentModal';
import DocumentPreviewModal from '../../../shared/DocumentPreviewModal';
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import EmptyDocumentsState from "@/components/documents/EmptyDocumentsState";
import { DOCUMENTS_STATIC_CONTENT } from '../../../../pages/therapist/therapist.static';

const getDocumentIcon = (filename: string): string => {
    const extension = filename.split(".").pop()?.toLowerCase();

    if (extension === "pdf") {
        return "/documents/pdf.svg";
    } else if (["doc", "docx"].includes(extension || "")) {
        return "/documents/doc.svg";
    } else if (["jpg", "jpeg", "png", "gif", "svg", "webp", "bmp"].includes(extension || "")) {
        return "/documents/img.svg";
    }
    return "/documents/doc.svg";
};

const Documents = () => {
    const [searchQuery, setSearchQuery] = useState("");
    const [currentPage, setCurrentPage] = useState(1);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const [documents, setDocuments] = useState(DOCUMENTS_STATIC_CONTENT.documents);

    // Modal States
    const [previewDoc, setPreviewDoc] = useState<(typeof DOCUMENTS_STATIC_CONTENT.documents)[number] | null>(null);
    const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
    const [deleteDocId, setDeleteDocId] = useState<string | null>(null);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

    const [itemsPerPage, setItemsPerPage] = useState(DOCUMENTS_STATIC_CONTENT.itemsPerPage || 9);
    const [isLoading] = useState(false);

    // Filter
    const filteredDocuments = documents.filter(doc =>
        doc.name.toLowerCase().includes(searchQuery.toLowerCase())
    );

    // Pagination
    const totalPages = Math.ceil(filteredDocuments.length / itemsPerPage);
    const paginatedDocuments = filteredDocuments.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    const handleUpload = (files: UploadedFile[]) => {
        // Mock upload
        const newDocs = files.map(file => ({
            id: file.id,
            name: file.name,
            shareInPortal: false,
            size: "1.2 MB", // Mock size
            uploadedDate: "Just now"
        }));
        setDocuments(prev => [...newDocs, ...prev]);
    };

    const toggleShare = (id: string) => {
        setDocuments(prev => prev.map(doc =>
            doc.id === id ? { ...doc, shareInPortal: !doc.shareInPortal } : doc
        ));
    };

    const handleView = (doc: (typeof documents)[number]) => {
        setPreviewDoc(doc);
        setIsPreviewModalOpen(true);
    };

    const handleDownload = (doc: (typeof documents)[number]) => {
        // Mock download logic
        console.log(`Downloading ${doc.name}...`);
        // In a real app, you would trigger a file download here.
        // For now, we simulate a link click if we had a URL.
        const link = document.createElement('a');
        link.href = '#'; // Replace with actual file URL
        link.download = doc.name;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    const handleDeleteClick = (id: string) => {
        setDeleteDocId(id);
        setIsDeleteModalOpen(true);
    };

    const handleConfirmDelete = () => {
        if (deleteDocId) {
            setDocuments(prev => prev.filter(d => d.id !== deleteDocId));
            setIsDeleteModalOpen(false);
            setDeleteDocId(null);
        }
    };

    const columns = [
        {
            key: "name",
            label: "Name",
            headerContent: "Name", // Use headerContent for explicit check
            width: "40%",
            padding: "0 1.5rem",
            headerPadding: "0 1.5rem",
            render: (value: string) => (
                <div className="flex items-center gap-3">
                    <img src={getDocumentIcon(value)} alt="" className="w-8 h-8 rounded" onError={(e) => (e.target as HTMLImageElement).src = '/documents/doc.svg'} />
                    <span className="font-medium text-gray-900 truncate">{value}</span>
                </div>
            )
        },
        {
            key: "shareInPortal",
            label: "Share in Portal",
            width: "15%",
            padding: "0 0.5rem",
            render: (value: boolean, row: (typeof documents)[number]) => (
                <Switch
                    checked={value}
                    onCheckedChange={() => toggleShare(row.id)}
                    className="data-[state=checked]:bg-[var(--bg-primary-dark)]"
                />
            )
        },
        {
            key: "size",
            label: "Size",
            width: "10%",
            padding: "0 0.5rem",
            render: (value: string) => <span className="text-gray-500">{value}</span>
        },
        {
            key: "uploadedDate",
            label: "Uploaded Date",
            width: "15%",
            padding: "0 0.5rem",
            render: (value: string) => <span className="text-gray-500 flex items-center gap-1">
                {value}
                {/* Add sort icon if needed to match design perfectly, user said 'Uploaded Date (down arrow)' */}
            </span>
        },
        {
            key: "actions",
            label: "Actions",
            width: "15%",
            padding: "0 0.5rem",
            render: (_: unknown, row: (typeof documents)[number]) => (
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => handleView(row)}
                        className="h-9 w-9 border border-gray-200 rounded-lg flex items-center justify-center text-gray-500 hover:bg-gray-50 hover:text-gray-700 transition-colors bg-white cursor-pointer"
                        title="View"
                    >
                        <Eye size={18} />
                    </button>
                    <button
                        onClick={() => handleDownload(row)}
                        className="h-9 w-9 border border-gray-200 rounded-lg flex items-center justify-center text-gray-500 hover:bg-gray-50 hover:text-gray-700 transition-colors bg-white cursor-pointer"
                        title="Download"
                    >
                        <Download size={18} />
                    </button>
                    <button
                        onClick={() => handleDeleteClick(row.id)}
                        className="h-9 w-9 border border-gray-200 rounded-lg flex items-center justify-center text-gray-500 hover:text-red-600 hover:bg-red-50 hover:border-red-100 transition-colors bg-white cursor-pointer"
                        title="Delete"
                    >
                        <TrashIcon size={18} />
                    </button>
                </div>
            )
        }
    ];

    return (
        <div className="flex flex-col h-full gap-4 animate-in fade-in duration-300">
            {/* Header */}
            <div className="flex items-center justify-between shrink-0">
                <h3 className="text-lg font-bold text-gray-900">Document Management</h3>
                <div className="flex items-center gap-4">
                    <div className="relative">
                        <Search className="size-4.5 absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                        <Input
                            placeholder="Search"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="pl-11 pr-4 w-[20rem] h-[3rem] rounded-full border-gray-200 bg-white shadow-sm focus-visible:ring-1 focus-visible:ring-[var(--bg-primary-dark)] text-sm"
                        />
                    </div>
                    <Button
                        onClick={() => setIsUploadModalOpen(true)}
                        className="h-[3rem] px-6 rounded-full bg-[var(--bg-primary-dark)] text-white hover:bg-[var(--bg-primary-dark)]/90 font-medium text-sm shadow-sm"
                    >
                        <Plus className="mr-2" size={18} />
                        Upload Document
                    </Button>
                </div>
            </div>

            {/* Table */}
            <div className="flex-1 min-h-0 border border-gray-200 rounded-3xl shadow-sm bg-white overflow-hidden flex flex-col relative">
                <div className="flex-1 overflow-y-auto">
                    {isLoading ? (
                        <>
                            <DocumentTable
                                columns={columns}
                                data={[]}
                                containerClassName="border-none shadow-none rounded-none"
                                containerStyle={{ borderRadius: 0, border: "none", boxShadow: "none" }}
                                headerHeight="3.25rem"
                                rowHeight="3.25rem"
                            />
                            <ContentLoader size="lg" className="py-10" />
                        </>
                    ) : (
                        <>
                            <DocumentTable
                                columns={columns}
                                data={paginatedDocuments}
                                containerClassName="border-none shadow-none rounded-none"
                                containerStyle={{ borderRadius: 0, border: "none", boxShadow: "none" }}
                                headerHeight="3.25rem"
                                rowHeight="3.25rem"
                            />
                            {paginatedDocuments.length === 0 ? (
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

                {filteredDocuments.length > 0 ? (
                    <div className="py-2 px-6 border-t border-gray-100 bg-white shrink-0">
                        <Pagination
                            currentPage={currentPage}
                            totalPages={totalPages || 1}
                            onPageChange={setCurrentPage}
                            itemsPerPage={itemsPerPage}
                            totalItems={filteredDocuments.length}
                            variant="clinical-forms"
                            itemsPerPageOptions={[9, 18, 27, 45]}
                            onItemsPerPageChange={setItemsPerPage}
                            className="max-w-full pt-0"
                        />
                    </div>
                ) : null}
            </div>

            {/* Upload Modal */}
            <UploadDocumentModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUpload={handleUpload}
            />

            <DocumentPreviewModal
                isOpen={isPreviewModalOpen}
                onClose={() => setIsPreviewModalOpen(false)}
                document={previewDoc}
            />

            <ConfirmationModal
                isOpen={isDeleteModalOpen}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={handleConfirmDelete}
                title="Delete Document"
                description="Are you sure you want to delete this document? This action cannot be undone."
                confirmButtonText="Delete"
                type="delete"
                items={[]}
            />
        </div>
    );
};

export default Documents;
