import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { CalendarIcon, Download, Loader2, Trash2 } from "lucide-react";
import { Button } from "../ui/button";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import type { AssessmentHistoryItem } from "../../pages/therapist/therapist.static";
import { cn } from "@/lib/utils";
import {
    ASSESSMENT_CARD_ACTIONS,
    ASSESSMENT_CARD_LABELS,
    ASSESSMENT_CARD_NEXT_STEP,
    getAssessmentCardState,
    hasAssessmentReport,
    isAssessmentActionPrimary,
} from "@/utils/assessmentCardState";

interface AssessmentHistoryCardProps {
    assessment: AssessmentHistoryItem;
    onOpen: (id: string) => void;
    onDownloadPdf?: (id: string) => void;
    onDownloadWord?: (id: string) => void;
    onDelete?: (id: string) => void;
    downloadingPdfId?: string | null;
    downloadingWordId?: string | null;
    isDeleting?: boolean;
    readOnly?: boolean;
}

/** Same palette as the session card's note badge. */
const BADGE_CLASS_NAMES = {
    "not-started": "bg-[#FCF3E2] text-[#8A5A00]",
    "in-progress": "bg-(--neutral-100) text-(--text-neutral-600)",
    "report-draft": "bg-(--neutral-100) text-(--text-neutral-600)",
    finalized: "bg-[#E8F4EE] text-[#157347]",
} as const;

const AssessmentHistoryCard = ({
    assessment,
    onOpen,
    onDownloadPdf,
    onDownloadWord,
    onDelete,
    downloadingPdfId = null,
    downloadingWordId = null,
    isDeleting = false,
    readOnly = false,
}: AssessmentHistoryCardProps) => {
    const state = getAssessmentCardState(assessment.rawStatus);
    const isFinalized = state === "finalized";
    const isDownloadingPdf = downloadingPdfId === assessment.id;
    const isDownloadingWord = downloadingWordId === assessment.id;

    // A read-only viewer can look at a report but cannot answer or write one.
    const canOpen = !readOnly || hasAssessmentReport(state);
    const actionLabel = readOnly ? "View report" : ASSESSMENT_CARD_ACTIONS[state];
    const actionPrimary = !readOnly && isAssessmentActionPrimary(state);

    const canDownload = isFinalized && Boolean(onDownloadPdf || onDownloadWord);
    const canDelete = !readOnly && Boolean(onDelete);

    return (
        <div className="flex h-full flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-xs transition-colors duration-200 hover:border-(--neutral-200)">
            <div className="flex-1 p-5">
                <div className="mb-2 flex items-start justify-between gap-3">
                    <h4 className="min-w-0 text-base font-semibold text-(--text-primary-dark) [overflow-wrap:anywhere]">
                        {assessment.title}
                    </h4>
                    <span
                        className={cn(
                            "shrink-0 rounded-full px-2 py-1 text-xs font-medium",
                            BADGE_CLASS_NAMES[state],
                        )}
                    >
                        {ASSESSMENT_CARD_LABELS[state]}
                    </span>
                </div>
                <p className="line-clamp-2 text-sm leading-relaxed text-(--text-neutral-600)">
                    {assessment.description}
                </p>
                <p className="mt-3 text-sm font-medium text-(--text-primary-dark)">
                    {ASSESSMENT_CARD_NEXT_STEP[state]}
                </p>
            </div>

            <div className="mt-auto flex items-center justify-between gap-3 border-t border-(--neutral-100) bg-(--neutral-50) px-4 py-3">
                <div className="flex min-w-0 items-center gap-2 text-(--text-neutral-800)">
                    {assessment.date ? (
                        <>
                            <CalendarIcon size={16} className="shrink-0" />
                            <span className="truncate text-sm">
                                {isFinalized ? "Completed" : "Due"} {assessment.date}
                            </span>
                        </>
                    ) : null}
                </div>

                <div className="flex shrink-0 items-center gap-2">
                    {canOpen ? (
                        <Button
                            variant={actionPrimary ? "default" : "outline"}
                            onClick={() => onOpen(assessment.id)}
                            className={cn(
                                "h-8 cursor-pointer rounded-full px-3 text-xs",
                                actionPrimary
                                    ? "bg-(--bg-primary-dark) font-semibold text-white hover:bg-(--bg-primary-dark)/90"
                                    : "border-(--neutral-200) bg-transparent font-normal text-(--text-neutral-800) hover:bg-(--neutral-50)",
                            )}
                        >
                            {actionLabel}
                        </Button>
                    ) : null}

                    {canDownload || canDelete ? (
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <button
                                    type="button"
                                    aria-label="More actions"
                                    className="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-full text-(--text-neutral-600) transition-all hover:bg-white"
                                >
                                    <MenuDotsIcon size={18} />
                                </button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-48">
                                {canDownload && onDownloadPdf ? (
                                    <DropdownMenuItem
                                        onSelect={(event) => {
                                            event.preventDefault();
                                            onDownloadPdf(assessment.id);
                                        }}
                                        disabled={isDownloadingPdf}
                                        className="cursor-pointer text-sm font-medium"
                                    >
                                        {isDownloadingPdf ? (
                                            <Loader2 size={14} className="mr-2 animate-spin" />
                                        ) : (
                                            <Download size={14} className="mr-2" />
                                        )}
                                        {isDownloadingPdf ? "Preparing PDF..." : "Download PDF"}
                                    </DropdownMenuItem>
                                ) : null}
                                {canDownload && onDownloadWord ? (
                                    <DropdownMenuItem
                                        onSelect={(event) => {
                                            event.preventDefault();
                                            onDownloadWord(assessment.id);
                                        }}
                                        disabled={isDownloadingWord}
                                        className="cursor-pointer text-sm font-medium"
                                    >
                                        {isDownloadingWord ? (
                                            <Loader2 size={14} className="mr-2 animate-spin" />
                                        ) : (
                                            <Download size={14} className="mr-2" />
                                        )}
                                        {isDownloadingWord ? "Preparing Word..." : "Download Word"}
                                    </DropdownMenuItem>
                                ) : null}
                                {canDownload && canDelete ? <DropdownMenuSeparator /> : null}
                                {canDelete && onDelete ? (
                                    <DropdownMenuItem
                                        onClick={() => onDelete(assessment.id)}
                                        disabled={isDeleting}
                                        className="cursor-pointer text-sm font-medium text-red-600 focus:text-red-600"
                                    >
                                        <Trash2 size={14} className="mr-2" />
                                        {isDeleting ? "Deleting..." : "Delete Assessment"}
                                    </DropdownMenuItem>
                                ) : null}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    ) : null}
                </div>
            </div>
        </div>
    );
};

export default AssessmentHistoryCard;
