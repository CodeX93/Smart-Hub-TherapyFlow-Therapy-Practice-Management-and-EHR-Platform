import type { ReactNode } from "react";
import platform from "@/assets/figma/tasks-empty/platform.svg";
import clipboardBody from "@/assets/figma/tasks-empty/clipboard-body.svg";
import clipboardClip from "@/assets/figma/tasks-empty/clipboard-clip.svg";
import clipboardLines from "@/assets/figma/tasks-empty/clipboard-lines.svg";
import shadow from "@/assets/figma/tasks-empty/shadow.svg";
import { cn } from "@/lib/utils";

interface EmptyTasksStateProps {
  title?: string;
  description?: string;
  children?: ReactNode;
  className?: string;
}

/** Figma Lab-empty — clipboard illustration for empty tasks. */
const EmptyTasksState = ({
  title = "No tasks found",
  description = "Tasks will appear here",
  children,
  className,
}: EmptyTasksStateProps) => {
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
          <div className="absolute inset-[16.67%_12.5%_8.34%_12.5%]">
            <img
              src={clipboardBody}
              alt=""
              className="absolute inset-0 block size-full max-w-none"
            />
          </div>
          <div className="absolute bottom-3/4 left-1/3 right-1/3 top-[8.33%]">
            <div className="absolute inset-[-10.71%_-5.36%]">
              <img
                src={clipboardClip}
                alt=""
                className="absolute inset-0 block size-full max-w-none"
              />
            </div>
          </div>
          <div className="absolute inset-[40.63%_26.04%_23.96%_26.04%]">
            <img
              src={clipboardLines}
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
        <p className="max-w-[19.375rem] text-base font-normal leading-6 text-(--text-neutral-600)">
          {description}
        </p>
      </div>
      {children}
    </div>
  );
};

export default EmptyTasksState;
