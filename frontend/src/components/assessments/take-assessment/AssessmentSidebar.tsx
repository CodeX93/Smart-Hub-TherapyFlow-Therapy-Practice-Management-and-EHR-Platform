import { CheckCircle2 } from "lucide-react";
import { cn } from "../../../lib/utils";
import type { AssessmentSection } from "../../../pages/therapist/therapist.static";

interface AssessmentSidebarProps {
    sections: AssessmentSection[];
    activeSectionIndex: number;
    onSectionChange: (index: number) => void;
    completedSections: number[]; // Array of section text indices
}

const AssessmentSidebar = ({
    sections,
    activeSectionIndex,
    onSectionChange,
    completedSections
}: AssessmentSidebarProps) => {
    return (
        <div className="w-80 shrink-0 border-r border-gray-100 bg-white/50 flex flex-col h-full overflow-hidden">
            <div className="p-6 pb-2">
                <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">
                    {sections.length} sections
                </span>
            </div>

            <div className="flex-1 overflow-y-auto px-4 pb-6 space-y-1">
                {sections.map((section, index) => {
                    const isActive = activeSectionIndex === index;
                    const isCompleted = completedSections.includes(index);

                    return (
                        <button
                            key={section.id}
                            onClick={() => onSectionChange(index)}
                            className={cn(
                                "w-full min-w-0 flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all text-left",
                                isActive
                                    ? "bg-gray-100 text-gray-900"
                                    : "text-gray-500 hover:bg-gray-50 hover:text-gray-700"
                            )}
                        >
                            <span className={cn(
                                "flex shrink-0 items-center justify-center w-6 h-6 rounded-md text-xs",
                                isActive
                                    ? "bg-white shadow-sm text-gray-900 font-semibold"
                                    : isCompleted
                                        ? "text-emerald-600"
                                        : "bg-gray-100 text-gray-500"
                            )}>
                                {isCompleted && !isActive ? (
                                    <CheckCircle2 size={16} />
                                ) : (
                                    index + 1
                                )}
                            </span>
                            <span className="min-w-0 flex-1 break-all line-clamp-2">{section.title}</span>
                        </button>
                    );
                })}
            </div>
        </div>
    );
};

export default AssessmentSidebar;
