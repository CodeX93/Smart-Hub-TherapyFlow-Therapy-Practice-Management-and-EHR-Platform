import platform from "@/assets/figma/checklists-empty/platform.svg";
import documentBody from "@/assets/figma/checklists-empty/document-body.svg";
import documentPencil from "@/assets/figma/checklists-empty/document-pencil.svg";
import documentLines from "@/assets/figma/checklists-empty/document-lines.svg";
import shadow from "@/assets/figma/checklists-empty/shadow.svg";
import { cn } from "@/lib/utils";

interface EmptyChecklistsStateProps {
  title?: string;
  description?: string;
  className?: string;
}

/** Figma Lab-empty (2107:103012) — document-add illustration for empty checklists. */
const EmptyChecklistsState = ({
  title = "No Checklists Assigned",
  description = "Use the dropdown above to select a process checklist for this client",
  className,
}: EmptyChecklistsStateProps) => {
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
        <div className="relative col-start-1 row-start-1 ml-8 size-14 overflow-clip rounded-[0.729rem]">
          <div className="absolute inset-[8.33%_12.5%]">
            <img
              src={documentBody}
              alt=""
              className="absolute inset-0 block size-full max-w-none"
            />
          </div>
          <div className="absolute bottom-1/4 left-1/2 right-[4.17%] top-[29.17%]">
            <div className="absolute inset-[-1.95%]">
              <img
                src={documentPencil}
                alt=""
                className="absolute inset-0 block size-full max-w-none"
              />
            </div>
          </div>
          <div className="absolute inset-[34.38%_36.46%_26.04%_30.21%]">
            <img
              src={documentLines}
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
        <p className="max-w-sm text-base font-normal leading-6 text-(--text-neutral-600)">
          {description}
        </p>
      </div>
    </div>
  );
};

export default EmptyChecklistsState;
