import platform from "@/assets/figma/billing-empty/platform.svg";
import billBody from "@/assets/figma/billing-empty/bill-body.svg";
import billLines from "@/assets/figma/billing-empty/bill-lines.svg";
import shadow from "@/assets/figma/billing-empty/shadow.svg";
import { cn } from "@/lib/utils";

interface EmptyBillingStateProps {
  title?: string;
  description?: string;
  className?: string;
}

/** Figma Lab-empty (2107:104125) — bill-list illustration for empty billing. */
const EmptyBillingState = ({
  title = "No billing records available",
  description = "Billing records will appear here",
  className,
}: EmptyBillingStateProps) => {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-4 py-12 text-center",
        className,
      )}
    >
      <div
        className="relative inline-grid shrink-0 grid-cols-[max-content] grid-rows-[max-content] place-items-start leading-[0]"
        aria-hidden="true"
      >
        <img
          src={platform}
          alt=""
          className="col-start-1 row-start-1 mt-11 h-6 w-[7.5rem]"
        />
        <div className="relative col-start-1 row-start-1 ml-8 size-14 overflow-clip">
          <div className="absolute inset-[8.33%_12.5%]">
            <img
              src={billBody}
              alt=""
              className="absolute inset-0 block size-full max-w-none"
            />
          </div>
          <div className="absolute inset-[28.13%_26.04%_36.46%_26.04%]">
            <img
              src={billLines}
              alt=""
              className="absolute inset-0 block size-full max-w-none"
            />
          </div>
        </div>
        <div className="relative col-start-1 row-start-1 ml-7 mt-[3.1625rem] h-[0.6875rem] w-[4.0625rem]">
          <div className="absolute inset-[-42.64%_-7.22%]">
            <img
              src={shadow}
              alt=""
              className="absolute inset-0 block size-full max-w-none"
            />
          </div>
        </div>
      </div>

      <div className="flex flex-col items-center gap-1">
        <h4 className="text-xl font-semibold leading-7 text-(--neutral-950)">
          {title}
        </h4>
        <p className="text-base font-normal leading-6 text-(--text-neutral-600)">
          {description}
        </p>
      </div>
    </div>
  );
};

export default EmptyBillingState;
