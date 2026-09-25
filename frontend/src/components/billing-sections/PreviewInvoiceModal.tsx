import { ContentLoader } from "@/components/shared/ContentLoader";
import { X, Printer } from "lucide-react";
import { createPortal } from "react-dom";
import { Button } from "../ui/button";
import type { PreviewInvoiceModalProps } from "@/types/billing.type";
import { openInvoiceHtmlForPrint } from "@/utils/openInvoiceForPrint";

const PreviewInvoiceModal = ({
  isOpen,
  onClose,
  invoice,
  htmlContent,
  isLoading,
}: PreviewInvoiceModalProps) => {
  if (!isOpen || !invoice) return null;

  const handlePrint = () => {
    if (!htmlContent) return;
    openInvoiceHtmlForPrint(htmlContent);
  };

  return createPortal(
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-2 md:p-4" data-scheduling-nested-modal>
      <div
        className="app-modal-overlay fixed inset-0 transition-opacity animate-in fade-in duration-200"
        onClick={onClose}
      />

      <div className="app-modal-surface relative z-[1101] flex h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-[#F9FAFB] animate-in fade-in zoom-in duration-200">
        <div className="z-10 flex min-w-0 items-center justify-between border-b border-(--neutral-200) bg-white px-6 py-4 shadow-sm">
          <div className="flex min-w-0 flex-1 flex-col gap-1 pr-4">
            <h2 className="text-lg font-semibold text-(--text-primary-dark)">
              Invoice Preview
            </h2>
            <p
              className="truncate text-sm text-(--text-neutral-500)"
              title={`${invoice.service} • ${invoice.client}`}
            >
              {invoice.service} • {invoice.client}
            </p>
          </div>
          <button
            onClick={onClose}
            className="cursor-pointer rounded-full p-2 transition-colors hover:bg-gray-100"
            aria-label="Close invoice preview"
          >
            <X size={24} className="text-(--text-neutral-600)" />
          </button>
        </div>

        <div className="custom-scrollbar relative flex flex-1 justify-center overflow-y-auto bg-[#F4F5F7] px-4 py-8">
          {isLoading ? (
            <div className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/50 backdrop-blur-sm">
              <ContentLoader size="xl" className="mb-4" />
              <p className="text-sm font-medium text-(--text-neutral-600)">
                Generating Preview...
              </p>
            </div>
          ) : htmlContent ? (
            <div
              className="w-full max-w-3xl overflow-hidden rounded-sm border border-(--neutral-200) bg-white shadow-md"
              style={{ minHeight: "66rem" }}
            >
              <iframe
                title="Invoice preview"
                srcDoc={htmlContent}
                sandbox="allow-same-origin"
                className="w-full border-0 bg-white"
                style={{ minHeight: "66rem", height: "100%" }}
              />
            </div>
          ) : (
            <div className="flex h-full flex-col items-center justify-center text-(--text-neutral-500)">
              <p>No preview available.</p>
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 border-t border-(--neutral-200) bg-white px-6 py-4">
          <Button variant="secondary" size="md" onClick={onClose}>
            Close
          </Button>
          <Button
            variant="primary"
            size="md"
            onClick={handlePrint}
            disabled={!htmlContent || Boolean(isLoading)}
            className="gap-2"
          >
            <Printer size={16} />
            Print
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
};

export default PreviewInvoiceModal;
