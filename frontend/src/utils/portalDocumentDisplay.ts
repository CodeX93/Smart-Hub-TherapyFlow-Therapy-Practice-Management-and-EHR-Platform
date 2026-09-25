import type { PortalDocument } from "@/store/api/portalApi";
import type { Document } from "@/types/document.type";

const DOCUMENT_TYPE_LABELS: Record<string, string> = {
  intake_form: "Forms and Intake",
  consent: "Consent",
  insurance_card: "Insurance Documents",
  id_document: "ID Document",
  medical_record: "Medical Records",
  prescription: "Prescription",
  lab_result: "Lab Results",
  referral_letter: "Referral Letter",
};

function formatDocumentDate(value: string | null | undefined): string {
  if (!value) return "-";

  const dateOnlyMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  const parsed = dateOnlyMatch
    ? new Date(
        Number.parseInt(dateOnlyMatch[1], 10),
        Number.parseInt(dateOnlyMatch[2], 10) - 1,
        Number.parseInt(dateOnlyMatch[3], 10),
      )
    : new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleDateString("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  });
}

export function formatPortalDocumentBytes(value?: number | null): string {
  if (!value || value <= 0) return "-";

  const units = ["B", "KB", "MB", "GB"];
  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unitIndex]}`;
}

export function formatPortalDocumentCategory(document: PortalDocument): string {
  const documentType = document.documentType?.trim();
  if (documentType) {
    return (
      DOCUMENT_TYPE_LABELS[documentType] ||
      documentType
        .replace(/_/g, " ")
        .replace(/\b\w/g, (char) => char.toUpperCase())
    );
  }

  const category = document.category?.trim();
  if (category) return category;

  return "Document";
}

export interface PortalDocumentRow extends Document {
  mimeType?: string;
  previewUrl?: string;
  downloadUrl?: string;
}

export function mapPortalDocumentToRow(
  document: PortalDocument,
): PortalDocumentRow {
  return {
    id: String(document.id),
    name: document.originalName || document.fileName || "Untitled document",
    category: formatPortalDocumentCategory(document),
    size: formatPortalDocumentBytes(document.fileSize),
    uploadedDate: formatDocumentDate(document.uploadedAt || document.createdAt),
    mimeType: document.mimeType || undefined,
    previewUrl: document.previewUrl || undefined,
    downloadUrl: document.downloadUrl || undefined,
  };
}

export function filterPortalDocuments(
  documents: PortalDocumentRow[],
  searchQuery: string,
): PortalDocumentRow[] {
  const normalizedQuery = searchQuery.trim().toLowerCase();
  if (!normalizedQuery) return documents;

  return documents.filter((document) => {
    const haystack = [
      document.name,
      document.category,
      document.size,
      document.uploadedDate,
    ]
      .join(" ")
      .toLowerCase();

    return haystack.includes(normalizedQuery);
  });
}

export function downloadPortalDocumentBlob(blob: Blob, fileName: string): void {
  const fileUrl = window.URL.createObjectURL(blob);
  const link = window.document.createElement("a");
  link.href = fileUrl;
  link.download = fileName;
  window.document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(fileUrl);
}
