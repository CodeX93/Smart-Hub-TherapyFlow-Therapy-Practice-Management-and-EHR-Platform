import { CreditCard, ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";

interface BillingModuleGateProps {
  title?: string;
  description?: string;
}

const BillingModuleGate = ({
  title = "Billing module not available",
  description = "Billing is not included in your plan. Please upgrade to access billing, invoicing, and invoice policies.",
}: BillingModuleGateProps) => {
  return (
    <div className="flex h-full min-h-[20rem] flex-1 flex-col items-center justify-center rounded-3xl border border-(--neutral-100) bg-white px-6 py-12 text-center shadow-xs">
      <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-(--bg-primary-50) text-(--primary-500)">
        <ShieldAlert size={28} />
      </div>
      <h2 className="text-xl font-bold text-(--text-primary-dark)">{title}</h2>
      <p className="mt-2 max-w-lg text-sm text-(--text-neutral-600)">{description}</p>
      <div className="mt-6 flex items-center gap-2 text-sm text-(--text-neutral-500)">
        <CreditCard size={16} />
        <span>Contact your administrator to enable the Billing module.</span>
      </div>
      <Button
        type="button"
        variant="outline"
        className="mt-6 rounded-full px-6"
        onClick={() => window.location.reload()}
      >
        Retry
      </Button>
    </div>
  );
};

export default BillingModuleGate;
