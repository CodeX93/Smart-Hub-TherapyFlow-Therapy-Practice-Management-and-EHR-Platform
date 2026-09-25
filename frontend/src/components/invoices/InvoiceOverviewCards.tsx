
import { ContentLoader } from "@/components/shared/ContentLoader";
import OverviewCard from "../shared/OverviewCard";

import type { InvoiceOverview } from "../../types/invoice.type";

interface InvoiceOverviewCardsProps {
  overview: InvoiceOverview;
  isLoading?: boolean;
}

const InvoiceOverviewCards = ({
  overview,
  isLoading = false,
}: InvoiceOverviewCardsProps) => {
  const cards = [
    {
      label: "Total Invoices",
      value: overview.totalInvoices.toString().padStart(2, "0"),
      iconSrc: "/invoice/card-1.svg",
    },
    {
      label: "Total Billed",
      value: overview.totalBilled,
      iconSrc: "/invoice/card-2.svg",
    },
    {
      label: "Total Paid",
      value: overview.totalPaid,
      iconSrc: "/invoice/card-3.svg",
    },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 rounded-2xl border border-(--neutral-100) bg-white py-10">
        <ContentLoader variant="inline" size="md" />
        <span className="text-sm text-(--text-neutral-600)">
          Loading invoice summary...
        </span>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
      {cards.map((card, index) => (
        <OverviewCard
          key={index}
          label={card.label}
          value={card.value}
          iconSrc={card.iconSrc}
        />
      ))}
    </div>
  );
};

export default InvoiceOverviewCards;
