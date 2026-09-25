import { cn } from "@/lib/utils";

export type OrganisationDetailsTab =
  | "overview"
  | "billing"
  | "addons"
  | "provision"
  | "feature-overrides"
  | "audit-logs";

interface TabItem {
  key: OrganisationDetailsTab;
  label: string;
}

interface TabButtonProps {
  item: TabItem;
  active?: boolean;
  onClick: () => void;
}

interface OrganisationTabsProps {
  value: OrganisationDetailsTab;
  onChange: (next: OrganisationDetailsTab) => void;
}

function TabButton(props: TabButtonProps) {
  return (
    <button
      type="button"
      onClick={props.onClick}
      className={cn(
        "h-8 rounded-full px-4 text-[0.8125rem] font-medium leading-5 transition-colors",
        props.active
          ? "bg-white text-[#273540] shadow-[0_1px_2px_rgba(15,23,42,0.06)]"
          : "text-[#667483]"
      )}
    >
      {props.item.label}
    </button>
  );
}

const TAB_ITEMS: TabItem[] = [
  { key: "overview", label: "Overview" },
  { key: "billing", label: "Billing & Subscription" },
  { key: "addons", label: "Add-ons" },
  { key: "provision", label: "Provision" },
  { key: "feature-overrides", label: "Feature Overrides" },
  { key: "audit-logs", label: "Audit Logs" },
];

function OrganisationTabs(props: OrganisationTabsProps) {
  return (
    <div className="min-w-0 w-full overflow-x-auto">
      <div className="inline-flex min-w-max items-center gap-1 rounded-full bg-[#eef2f6] p-1">
        {TAB_ITEMS.map(function (item) {
          return (
            <TabButton
              key={item.key}
              item={item}
              active={props.value === item.key}
              onClick={function () {
                props.onChange(item.key);
              }}
            />
          );
        })}
      </div>
    </div>
  );
}

export default OrganisationTabs;
