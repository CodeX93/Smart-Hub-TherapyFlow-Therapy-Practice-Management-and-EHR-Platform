import { cn } from "@/lib/utils";

export type BillingPlansTab =
  | "overview"
  | "invoices"
  | "plans"
  | "add-ons"
  | "settings";

interface BillingPlansTabsProps {
  value: BillingPlansTab;
  onChange(next: BillingPlansTab): void;
}

interface BillingPlansTabItem {
  key: BillingPlansTab;
  label: string;
}

function getTabItems(): BillingPlansTabItem[] {
  return [
    { key: "overview", label: "Overview" },
    { key: "invoices", label: "Invoices" },
    { key: "plans", label: "Plans & Pricing" },
    { key: "add-ons", label: "Add-ons" },
    { key: "settings", label: "Settings" },
  ];
}

function getTabButtonClassName(isActive: boolean): string {
  return cn(
    "h-8 rounded-full px-4 text-[0.75rem] font-medium leading-4.5 transition-colors",
    isActive
      ? "bg-white text-[#1f2d38] shadow-[0_1px_2px_0_var(--shadow)]"
      : "text-[#667483] hover:text-[#1f2d38]"
  );
}

function BillingPlansTabs(props: BillingPlansTabsProps) {
  return (
    <div className="w-full">
      <div className="inline-flex w-fit items-center gap-1 rounded-full bg-[#eef2f6] p-1">
        {getTabItems().map(function (item) {
          const isActive = props.value === item.key;

          return (
            <button
              key={item.key}
              type="button"
              onClick={function () {
                props.onChange(item.key);
              }}
              className={getTabButtonClassName(isActive)}
            >
              {item.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default BillingPlansTabs;
