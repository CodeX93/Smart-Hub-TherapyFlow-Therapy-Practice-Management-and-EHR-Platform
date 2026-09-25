import { ChevronDown } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";

const AutofillVariablesAccordion = () => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="border border-(--neutral-100) rounded-xl overflow-hidden bg-white">
      <button
        type="button"
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center justify-between p-4 text-sm font-medium text-(--text-neutral-600) hover:bg-(--neutral-50) transition-colors cursor-pointer"
      >
        <span>Available autofill variables (click to expand)</span>
        <div
          className={cn(
            "transition-transform duration-300",
            isExpanded ? "rotate-180" : "rotate-0"
          )}
        >
          <ChevronDown size={18} />
        </div>
      </button>
      <div
        className={cn(
          "grid transition-all duration-300 ease-in-out",
          isExpanded
            ? "grid-rows-[1fr] opacity-100"
            : "grid-rows-[0fr] opacity-0"
        )}
      >
        <div className="overflow-hidden">
          <div className="p-4 pt-0 text-xs text-(--text-neutral-500) space-y-3 bg-(--neutral-50)/30">
            <div className="space-y-1">
              <p className="font-semibold text-(--text-neutral-700)">
                Client Information
              </p>
              <p>
                • &#123;&#123;CLIENT_NAME&#125;&#125;,
                &#123;&#123;CLIENT_ID&#125;&#125;,
                &#123;&#123;CLIENT_EMAIL&#125;&#125;,
                &#123;&#123;CLIENT_PHONE&#125;&#125;,
                &#123;&#123;CLIENT_DOB&#125;&#125;
              </p>
            </div>
            <div className="space-y-1">
              <p className="font-semibold text-(--text-neutral-700)">
                Therapist Information
              </p>
              <p>
                • &#123;&#123;THERAPIST_NAME&#125;&#125;,
                &#123;&#123;THERAPIST_EMAIL&#125;&#125;,
                &#123;&#123;THERAPIST_PHONE&#125;&#125;
              </p>
            </div>
            <div className="space-y-1">
              <p className="font-semibold text-(--text-neutral-700)">
                Practice Information
              </p>
              <p>
                • &#123;&#123;PRACTICE_NAME&#125;&#125;,
                &#123;&#123;PRACTICE_ADDRESS&#125;&#125;,
                &#123;&#123;PRACTICE_PHONE&#125;&#125;,
                &#123;&#123;PRACTICE_EMAIL&#125;&#125;,
                &#123;&#123;PRACTICE_WEBSITE&#125;&#125;
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AutofillVariablesAccordion;
