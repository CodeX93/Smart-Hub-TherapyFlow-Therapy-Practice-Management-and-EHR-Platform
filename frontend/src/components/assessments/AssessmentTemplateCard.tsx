import { Button } from "../ui/button";
import type { AssessmentTemplate } from "../../pages/therapist/therapist.static";

interface AssessmentTemplateCardProps {
    template: AssessmentTemplate;
    onAssign?: (id: string) => void;
}

const AssessmentTemplateCard = ({ template, onAssign }: AssessmentTemplateCardProps) => {
    return (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm flex flex-col h-full hover:shadow-md transition-shadow duration-200 overflow-hidden">
            <div className="p-5 flex-1">
                <h4 className="text-[1rem] font-semibold text-gray-900 mb-2">
                    {template.title}
                </h4>
                <p className="text-sm text-gray-500 leading-relaxed line-clamp-2">
                    {template.description}
                </p>
            </div>

            <div className="px-4 py-3 sm:px-5 sm:py-4 bg-gray-50/50 border-t border-gray-100 mt-auto space-y-3">
                <span className="text-sm text-gray-500 block min-w-0 truncate">
                    Category: <span className="text-gray-700 font-medium capitalize">{template.category}</span>
                </span>
                {onAssign ? (
                <Button
                    onClick={() => onAssign(template.id)}
                    variant="outline"
                    className="h-9 w-full px-4 bg-white hover:bg-gray-50 text-gray-500 border-gray-200 rounded-full text-sm font-medium transition-colors"
                >
                    Assign to Client
                </Button>
                ) : null}
            </div>
        </div>
    );
};

export default AssessmentTemplateCard;
