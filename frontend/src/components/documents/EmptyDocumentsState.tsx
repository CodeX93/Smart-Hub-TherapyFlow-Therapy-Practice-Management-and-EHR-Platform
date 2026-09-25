import platform from "@/assets/figma/assessments-empty/platform.svg";
import documentBody from "@/assets/figma/assessments-empty/document-body.svg";
import documentFold from "@/assets/figma/assessments-empty/document-fold.svg";
import shadow from "@/assets/figma/assessments-empty/shadow.svg";
import { cn } from "@/lib/utils";

interface EmptyDocumentsStateProps {
  title?: string;
  description?: string;
  className?: string;
}

/** Figma Lab-empty document illustration — used for empty documents lists. */
const EmptyDocumentsState = ({
  title = "No documents found",
  description = "Uploaded documents will appear here",
  className,
}: EmptyDocumentsStateProps) => {
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
          <div className="absolute inset-[19.56%_16.67%_9.61%_16.67%]">
            <img
              src={documentBody}
              alt=""
              className="absolute inset-0 block size-full max-w-none"
            />
          </div>
          <div className="absolute bottom-[75.56%] left-[16.67%] right-1/4 top-[8.33%]">
            <img
              src={documentFold}
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

export default EmptyDocumentsState;
