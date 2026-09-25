import { Button } from "@/components/ui/button";
import letterBody from "@/assets/figma/notes-empty/letter-body.svg";
import letterFlap from "@/assets/figma/notes-empty/letter-flap.svg";
import platform from "@/assets/figma/notes-empty/platform.svg";
import shadow from "@/assets/figma/notes-empty/shadow.svg";
import plus from "@/assets/figma/notes-empty/plus.svg";

interface NotesEmptyStateProps {
  onAddNote?: () => void;
  showAddButton?: boolean;
}

/**
 * Figma empty state (1559:99508) — Lab-empty letter illustration + CTA.
 * Assets exported from Figma; rem sizes scale with dashboard html font-size.
 */
const NotesEmptyState = ({
  onAddNote,
  showAddButton = true,
}: NotesEmptyStateProps) => {
  return (
    <div className="flex w-full flex-col items-center gap-6 text-center">
      <div className="flex w-full max-w-[24.875rem] flex-col items-center gap-4">
        {/* Lab-empty: 120×68 envelope on platform (Figma 1559:99510) */}
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
            <div className="absolute inset-[12.5%_4.17%]">
              <img
                src={letterBody}
                alt=""
                className="absolute inset-0 block size-full max-w-none"
              />
            </div>
            <div className="absolute inset-[27.4%_19.06%_46.96%_19.06%]">
              <img
                src={letterFlap}
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

        <div className="flex w-full flex-col items-center gap-1">
          <h3 className="w-full text-xl leading-7 font-semibold text-(--neutral-950)">
            No notes yet
          </h3>
          <p className="w-full text-base leading-6 font-normal text-(--text-neutral-600)">
            Add a note to keep track of client communications and updates.
          </p>
        </div>
      </div>

      {showAddButton ? (
        <Button
          type="button"
          onClick={onAddNote}
          className="h-auto min-h-0 cursor-pointer gap-2 rounded-full bg-(--bg-primary-dark) px-6 py-3 text-sm leading-[1.375rem] font-semibold text-white shadow-none hover:bg-(--bg-primary-dark)/90"
        >
          <span className="relative inline-flex size-6 shrink-0 items-center justify-center overflow-clip">
            <img
              src={plus}
              alt=""
              className="size-[0.975rem]"
              aria-hidden="true"
            />
          </span>
          Add Note
        </Button>
      ) : null}
    </div>
  );
};

export default NotesEmptyState;
