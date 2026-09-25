
import { ContentLoader } from "@/components/shared/ContentLoader";
import { X } from "lucide-react";
import { createPortal } from "react-dom";
import { Badge } from "@/components/ui/badge";
import { useGetBillingTransactionsQuery } from "@/store/api/admin/billing.api";
import type { Invoice } from "@/types/invoice.type";
import { humanizeKeyLabel } from "@/utils/paymentMethodDisplay";

interface BillingTransactionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: Invoice | null;
}

const BillingTransactionsModal = ({
  isOpen,
  onClose,
  invoice,
}: BillingTransactionsModalProps) => {
  const billingId = invoice ? Number.parseInt(invoice.id, 10) : NaN;

  const { data: transactions = [], isFetching } = useGetBillingTransactionsQuery(
    billingId,
    { skip: !isOpen || !Number.isFinite(billingId) },
  );

  if (!isOpen || !invoice) return null;

  return createPortal(
    <div className="app-modal-overlay fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="app-modal-surface flex max-h-[85vh] w-full max-w-2xl flex-col rounded-3xl">
        <div className="flex items-center justify-between border-b border-(--neutral-100) px-6 py-5">
          <div>
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Payment transactions
            </h2>
            <p className="text-sm text-(--text-neutral-600)">Invoice #{invoice.id}</p>
          </div>
          <button type="button" onClick={onClose} aria-label="Close">
            <X size={22} />
          </button>
        </div>

        <div className="custom-scrollbar flex-1 overflow-y-auto px-6 py-4">
          {isFetching ? (
            <div className="flex justify-center py-10">
              <ContentLoader size="lg" />
            </div>
          ) : transactions.length === 0 ? (
            <p className="py-8 text-center text-sm text-(--text-neutral-600)">
              No transactions recorded for this invoice.
            </p>
          ) : (
            <div className="space-y-3">
              {transactions.map((tx) => (
                <div
                  key={tx.id}
                  className="rounded-2xl border border-(--neutral-100) p-4 text-sm"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p className="font-medium text-(--text-primary-dark)">
                        {humanizeKeyLabel(tx.transactionType)} · ${tx.amount.toFixed(2)}
                      </p>
                      <p className="text-(--text-neutral-600)">
                        {humanizeKeyLabel(tx.provider)} ·{" "}
                        {new Date(tx.createdAt).toLocaleString()}
                      </p>
                    </div>
                    <Badge variant={tx.voided ? "secondary" : "default"}>
                      {tx.voided ? "Voided" : humanizeKeyLabel(tx.status)}
                    </Badge>
                  </div>
                  {tx.failureReason ? (
                    <p className="mt-2 text-xs text-red-600">{tx.failureReason}</p>
                  ) : null}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
};

export default BillingTransactionsModal;
