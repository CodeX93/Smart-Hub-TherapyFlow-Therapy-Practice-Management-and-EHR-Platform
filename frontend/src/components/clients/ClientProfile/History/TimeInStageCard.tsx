import { History2 } from "@solar-icons/react-perf/category/time/Linear/History2";

/**
 * Figma: History → Time in Each Stage card (1559:99461).
 */
const TimeInStageCard = () => {
  return (
    <div className="flex w-full max-w-[16.5rem] flex-col items-start rounded-xl border border-(--neutral-100) bg-white p-3">
      <div className="flex w-full items-center gap-2">
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <p className="text-sm leading-[1.375rem] font-normal text-(--text-neutral-600)">
            Currently in:{" "}
            <span className="font-medium text-(--neutral-950)">Intake</span>
          </p>
          <p className="text-xl leading-7 font-semibold text-(--neutral-950)">
            14 days
          </p>
        </div>
        <div className="flex size-[2.375rem] shrink-0 items-center justify-center rounded-lg bg-(--bg-primary-50) text-(--text-primary-500)">
          <History2 size={24} className="size-6" />
        </div>
      </div>
    </div>
  );
};

export default TimeInStageCard;
