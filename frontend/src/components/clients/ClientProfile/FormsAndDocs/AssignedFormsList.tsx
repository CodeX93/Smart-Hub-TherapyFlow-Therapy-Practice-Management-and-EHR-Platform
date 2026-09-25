import { CalendarIcon, TrashIcon } from "@/components/icons/commonIcons";
import { Eye, Download } from "lucide-react";
import { cn } from '../../../../lib/utils';

interface AssignedFormsListProps {
    forms: string[]; // IDs of assigned templates
    onDelete: (id: string) => void;
}

// Adding mock descriptions for the list view since simple ID array was passed
const MOCK_DETAILS: Record<string, { desc: string }> = {
    '1': { desc: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum aute irure dolor' },
    '2': { desc: 'Consectetur adipiscing elit' },
    '3': { desc: 'Lorem ipsum dolor sit amet' },
    '4': { desc: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum' },
    '5': { desc: 'Dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident' },
};

const AssignedFormsList = ({ forms, onDelete }: AssignedFormsListProps) => {
    // In a real app we would map form data properly. Here we mock it based on ID.
    const getFormDetails = (id: string, index: number) => {
        // Mock alternating status
        const status = index % 2 === 0 ? 'Pending' : 'Completed';
        const assignedDate = 'Dec 12, 2025';
        return {
            status,
            assignedDate,
            description: MOCK_DETAILS[id]?.desc || "Clinical form description goes here..."
        };
    };

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {forms.map((formId, index) => {
                const details = getFormDetails(formId, index);

                return (
                    <div key={`${formId}-${index}`} className="group bg-white border border-gray-200 rounded-2xl hover:shadow-sm transition-all overflow-hidden flex flex-col">
                        <div className="p-6 flex-1">
                            {/* Status Badge */}
                            <div className="mb-4">
                                <span className={cn(
                                    "text-xs font-medium px-3 py-1.5 rounded-full",
                                    details.status === 'Completed'
                                        ? "bg-emerald-50 text-emerald-700"
                                        : "bg-gray-100 text-gray-600"
                                )}>
                                    {details.status}
                                </span>
                            </div>

                            {/* Title/Description */}
                            <p className="text-base text-gray-900 font-medium leading-relaxed">
                                {details.description}
                            </p>
                        </div>

                        {/* Footer: Date & Actions */}
                        <div className="px-6 py-4 bg-[#F8FAFC] border-t border-gray-100 flex items-center justify-between mt-auto">
                            <div className="flex items-center gap-2 text-sm text-gray-500">
                                <CalendarIcon size={16} className="text-gray-400" />
                                <span>Assigned: {details.assignedDate}</span>
                            </div>

                            <div className="flex items-center gap-3">
                                <button className="p-2 bg-white border border-gray-200 text-gray-700 hover:text-gray-900 hover:border-gray-300 rounded-lg transition-all shadow-sm cursor-pointer">
                                    {details.status === 'Completed' ? <Download size={18} /> : <Eye size={18} />}
                                </button>
                                <button
                                    onClick={() => onDelete(formId)}
                                    className="p-2 bg-white border border-gray-200 text-red-500 hover:text-red-600 hover:border-red-200 hover:bg-red-50 rounded-lg transition-all shadow-sm cursor-pointer"
                                >
                                    <TrashIcon size={18} />
                                </button>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
};

export default AssignedFormsList;
