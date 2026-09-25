import { cn } from "@/lib/utils";

export type CmsSectionKey =
  | "header"
  | "hero"
  | "features"
  | "customization"
  | "communication"
  | "security"
  | "benefits"
  | "faq"
  | "footer"
  | "seo"
  | "settings";

const CMS_SECTIONS: Array<{ key: CmsSectionKey; label: string }> = [
  { key: "header", label: "Header" },
  { key: "hero", label: "Hero" },
  { key: "features", label: "Features" },
  { key: "customization", label: "Customization" },
  { key: "communication", label: "Communication" },
  { key: "security", label: "Security" },
  { key: "benefits", label: "Benefits" },
  { key: "faq", label: "FAQ" },
  { key: "footer", label: "Footer" },
  { key: "seo", label: "SEO" },
  { key: "settings", label: "Site settings" },
];

type Props = {
  active: CmsSectionKey;
  onChange: (key: CmsSectionKey) => void;
};

export default function CmsSectionNav({ active, onChange }: Props) {
  return (
    <div className="w-full overflow-x-auto pb-1">
      <div className="flex min-w-max flex-nowrap items-center gap-1 rounded-full bg-[#eceff3] p-1">
        {CMS_SECTIONS.map((section) => (
          <button
            key={section.key}
            type="button"
            onClick={() => onChange(section.key)}
            className={cn(
              "whitespace-nowrap rounded-full px-[1.125rem] py-[0.4375rem] text-[0.875rem] font-medium leading-[1.375rem] transition-colors",
              active === section.key
                ? "bg-white text-[#2f3945] shadow-[0_1px_2px_rgba(15,23,42,0.05)]"
                : "text-[#697584] hover:text-[#2f3945]",
            )}
          >
            {section.label}
          </button>
        ))}
      </div>
    </div>
  );
}
