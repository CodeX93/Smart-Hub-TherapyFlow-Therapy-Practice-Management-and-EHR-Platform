import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check, X } from 'lucide-react';
import { cn } from '../../../../../lib/utils';

export interface ChecklistTemplate {
    id: string;
    title: string;
    totalItems: number;
}

interface ChecklistAssignmentDropdownProps {
    options: ChecklistTemplate[];
    selectedValues: string[];
    onChange: (values: string[]) => void;
}

const ChecklistAssignmentDropdown = ({ options, selectedValues, onChange }: ChecklistAssignmentDropdownProps) => {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);

    // Close on click outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const toggleOption = (id: string) => {
        if (selectedValues.includes(id)) {
            onChange(selectedValues.filter(v => v !== id));
        } else {
            onChange([...selectedValues, id]);
        }
    };

    const removeOption = (e: React.MouseEvent, id: string) => {
        e.stopPropagation();
        onChange(selectedValues.filter(v => v !== id));
    };

    const selectedOptions = options.filter(opt => selectedValues.includes(opt.id));
    const isFloating = isOpen || selectedValues.length > 0;

    return (
        <div className="relative" ref={containerRef}>
            {/* Trigger Area */}
            <div
                onClick={() => setIsOpen(!isOpen)}
                className={cn(
                    "min-h-[2.75rem] w-full border rounded-[1.125rem] px-4 pt-6 pb-2.5 flex items-center justify-between cursor-pointer bg-white transition-all relative group",
                    isOpen ? "border-[#1E293B] border-2 ring-0" : "border-gray-200 hover:border-gray-300"
                )}
            >
                <span
                    className={cn(
                        "absolute left-4 transition-all duration-200 pointer-events-none font-medium",
                        isFloating
                            ? "top-3.5 -translate-y-1/2 text-[0.6875rem] text-[#64748B]"
                            : "top-1/2 -translate-y-1/2 text-base text-[#64748B]"
                    )}
                >
                    Select a checklist template to assign
                </span>

                <div className="flex flex-wrap gap-2 flex-1 relative z-10 w-full min-h-[1.5rem]">
                    {selectedOptions.map(opt => (
                        <span
                            key={opt.id}
                            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#F1F5F9] text-sm text-[#334155] font-medium"
                        >
                            <span className="truncate max-w-[12.5rem]">{opt.title}</span>
                            <button
                                onClick={(e) => removeOption(e, opt.id)}
                                className="text-[#94A3B8] hover:text-[#64748B] ml-0.5"
                            >
                                <X size={14} strokeWidth={2.5} />
                            </button>
                        </span>
                    ))}
                </div>

                <div className="absolute right-4 top-1/2 -translate-y-1/2 text-[#94A3B8] pointer-events-none">
                    <ChevronDown size={20} className={cn("transition-transform duration-200", isOpen && "rotate-180")} />
                </div>
            </div>

            {/* Dropdown Menu */}
            {isOpen && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-2xl shadow-xl z-50 max-h-[18.75rem] overflow-x-hidden overflow-y-auto animate-in fade-in zoom-in-95 duration-100 p-2">
                    <div className="space-y-1">
                        {options.map(option => {
                            const isSelected = selectedValues.includes(option.id);
                            return (
                                <div
                                    key={option.id}
                                    onClick={() => toggleOption(option.id)}
                                    className="flex min-w-0 items-start gap-3 px-3 py-2.5 hover:bg-gray-50 rounded-xl cursor-pointer group transition-colors"
                                >
                                    <div className={cn(
                                        "w-5 h-5 mt-0.5 shrink-0 rounded-md border flex items-center justify-center transition-colors shadow-sm",
                                        isSelected ? "bg-[#1E293B] border-[#1E293B] text-white" : "border-gray-300 group-hover:border-gray-400 bg-white"
                                    )}>
                                        {isSelected && <Check size={12} strokeWidth={3} />}
                                    </div>
                                    <span
                                        className={cn(
                                            "flex min-w-0 flex-1 items-center gap-2 pt-0.5 text-sm transition-colors",
                                            isSelected ? "text-[#0F172A] font-medium" : "text-[#475569]",
                                        )}
                                    >
                                        <span
                                            className="min-w-0 flex-1 truncate"
                                            title={option.title}
                                        >
                                            {option.title}
                                        </span>
                                        <span className="shrink-0 text-gray-300">|</span>
                                        <span className="shrink-0 text-gray-500 font-normal whitespace-nowrap">
                                            {option.totalItems} Items
                                        </span>
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
};

export default ChecklistAssignmentDropdown;
