import { ArrowDown, ArrowUpRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useGetOrganisationInvoicesQuery } from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { Button } from "@/components/ui/button";
import StatusBadge from "../../../components/StatusBadge";

interface RecentInvoiceRow {
  invoice: string;
  period: string;
  amount: string;
  status: string;
  paidAt: string;
}

function getInvoiceHeaders(): string[] {
  return ["Invoice ID", "Period", "Amount", "Status", "Paid At", ""];
}

function renderHeaderCell(header: string, index: number) {
  if (!header) {
    return null;
  }

  if (index === 0) {
    return (
      <div className="flex items-center gap-1.5 text-(--text-primary-dark) text-[0.8125rem] font-medium leading-4.5">
        <span>{header}</span>
        <ArrowDown size={13} strokeWidth={1.8} aria-hidden="true" />
      </div>
    );
  }

  return (
    <div className="text-(--text-primary-dark) text-[0.8125rem] font-medium leading-4.5">
      {header}
    </div>
  );
}

interface RecentInvoicesCardProps {
  organisationId: number | null;
  slug: string;
  onViewAllBilling: () => void;
}

function RecentInvoicesCard(props: RecentInvoicesCardProps) {
  const navigate = useNavigate();
  const { data: invoiceRows = [], isLoading, isError, error } = useGetOrganisationInvoicesQuery(
    {
      id: props.organisationId ?? 0,
      page: 0,
      pageSize: 50,
    },
    { skip: !props.organisationId }
  );
  const invoices: RecentInvoiceRow[] = invoiceRows.slice(0, 3).map((row) => ({
    invoice: row.invoiceId,
    period: row.period,
    amount: `$${row.amount.toFixed(2)}`,
    status: row.status,
    paidAt:
      row.paidAt && row.paidAt !== "-"
        ? new Date(row.paidAt).toLocaleDateString(undefined, {
            year: "numeric",
            month: "short",
            day: "numeric",
          })
        : "-",
  }));
  const headers = getInvoiceHeaders();

  return (
    <div className="w-full rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) px-4 py-4 shadow-[0_1px_2px_0px_var(--shadow)] md:px-5">
      <div className="flex items-center justify-between gap-4">
        <div className="text-(--text-gray-900) text-[1rem] font-semibold leading-6">
          Recent Invoices
        </div>
        <Button
          type="button"
          variant="tertiary"
          size="sm"
          onClick={props.onViewAllBilling}
        >
          View All Billing
        </Button>
      </div>

      <div className="mt-4 overflow-hidden rounded-[0.875rem] border border-(--neutral-100)">
        <div className="grid grid-cols-[1fr_1fr_1.15fr_0.9fr_1.25fr_2.25rem] items-center bg-(--bg-primary-50) px-4 py-3.5">
          {headers.map(function (header, index) {
            return (
              <div key={header + "-" + index}>
                {renderHeaderCell(header, index)}
              </div>
            );
          })}
        </div>

        {isLoading ? (
          <div className="px-4 py-8 text-sm font-medium text-[#667483]">Loading invoices...</div>
        ) : isError ? (
          <div className="px-4 py-8 text-sm font-medium text-(--status-denied)">
            {getApiErrorMessage(error)}
          </div>
        ) : invoices.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm font-medium text-[#667483]">No invoices found.</div>
        ) : invoices.map(function (invoice, index) {
          return (
            <div
              key={invoice.invoice + "-" + index}
              className="grid grid-cols-[1fr_1fr_1.15fr_0.9fr_1.25fr_2.25rem] items-center border-t border-(--neutral-100) px-4 py-4"
            >
              <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">
                {invoice.invoice}
              </div>
              <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">
                {invoice.period}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {invoice.amount}
              </div>
              <div>
                <StatusBadge variant="green">{invoice.status}</StatusBadge>
              </div>
              <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">
                {invoice.paidAt}
              </div>
              <button
                type="button"
                className="grid h-8 w-8 place-items-center rounded-full text-[#97a4b0] transition-colors hover:bg-[#f5f8fb] hover:text-[#465563]"
                aria-label={"Open invoice " + invoice.invoice}
                onClick={function () {
                  navigate("/super-admin/organisations/" + props.slug + "/invoices", {
                    state:
                      props.organisationId !== null
                        ? { organisationId: props.organisationId }
                        : undefined,
                  });
                }}
              >
                <ArrowUpRight size={16} aria-hidden="true" />
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default RecentInvoicesCard;
