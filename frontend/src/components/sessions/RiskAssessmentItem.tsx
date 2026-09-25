import { cn } from "../../lib/utils";

interface RiskAssessmentItemProps {
    number: number;
    title: string;
    description: string;
    options: string[];
    selectedOption: string | null;
    onSelect: (option: string) => void;
    maxScore?: number;
}

const RiskAssessmentItem = ({
    number,
    title,
    description,
    options,
    selectedOption,
    onSelect,
    maxScore
}: RiskAssessmentItemProps) => {
    // The worst answer is the last option, so the denominator follows the options.
    const itemMaxScore = maxScore ?? Math.max(0, options.length - 1);
    const selectedIndex = selectedOption ? options.indexOf(selectedOption) : -1;
    const isAnswered = selectedIndex >= 0;

    return (
        <div className="mb-5">
            {/* Header - Title and Score on same line */}
            <div className="flex items-center justify-between mb-1.5">
                <h4 className="text-[0.9375rem] font-semibold text-(--text-primary-dark)">
                    {number}. {title}
                </h4>
                <span
                    className={cn(
                        "text-[0.8125rem] font-normal",
                        isAnswered ? "text-(--text-neutral-600)" : "text-(--status-pending-dark)"
                    )}
                >
                    {isAnswered ? `${selectedIndex}/${itemMaxScore}` : "Not answered"}
                </span>
            </div>

            {/* Description */}
            <p className="text-[0.8125rem] text-(--text-neutral-400) mb-3 leading-relaxed">
                {description}
            </p>

            {/* Options */}
            <div className="flex flex-wrap gap-2">
                {options.map((option) => (
                    <button
                        key={option}
                        type="button"
                        onClick={() => onSelect(option)}
                        className={cn(
                            "px-5 py-2 text-[0.875rem] font-medium rounded-full transition-all cursor-pointer border",
                            selectedOption === option
                                ? "bg-(--bg-primary-dark) text-white border-(--bg-primary-dark)"
                                : "bg-white text-(--text-neutral-800) border-(--neutral-200) hover:bg-(--neutral-50)"
                        )}
                    >
                        {option}
                    </button>
                ))}
            </div>
        </div>
    );
};

export default RiskAssessmentItem;
