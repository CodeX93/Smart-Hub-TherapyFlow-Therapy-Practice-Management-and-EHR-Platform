import type { ReactNode } from "react";
import { X } from "lucide-react";

interface AssessmentHeaderProps {
    title: string;
    subtitle?: string;
    onClose: () => void;
    /** The step bar. */
    children?: ReactNode;
}

const AssessmentHeader = ({ title, subtitle, onClose, children }: AssessmentHeaderProps) => (
    <div className="w-full min-w-0 shrink-0 border-b border-(--neutral-100) bg-white px-8 pt-6">
        <div className="mb-5 flex items-start justify-between gap-4">
            <div className="min-w-0">
                <h2 className="truncate text-xl font-semibold text-(--text-primary-dark)" title={title}>
                    {title}
                </h2>
                {subtitle ? (
                    <p className="mt-1 truncate text-sm text-(--text-neutral-600)">{subtitle}</p>
                ) : null}
            </div>
            <button
                type="button"
                onClick={onClose}
                aria-label="Close assessment"
                className="shrink-0 cursor-pointer rounded-full p-2 transition-colors hover:bg-(--neutral-50)"
            >
                <X className="text-(--text-neutral-600)" size={20} />
            </button>
        </div>
        {children}
    </div>
);

export default AssessmentHeader;
