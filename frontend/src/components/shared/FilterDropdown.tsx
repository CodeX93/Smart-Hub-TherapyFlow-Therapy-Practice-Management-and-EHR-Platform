import { ChevronDown } from "lucide-react";

interface FilterDropdownProps {
    options: string[];
    defaultValue?: string;
    onChange?: (value: string) => void;
}

const FilterDropdown = ({ options, defaultValue, onChange }: FilterDropdownProps) => {
    return (
        <div className="relative">
            <select
                className="px-4 py-2 text-sm text-gray-700 bg-white border border-gray-200 rounded-full cursor-pointer hover:bg-gray-50 appearance-none pr-10"
                defaultValue={defaultValue || options[0]}
                onChange={(e) => onChange?.(e.target.value)}
            >
                {options.map((option) => (
                    <option key={option} value={option}>
                        {option}
                    </option>
                ))}
            </select>
            <ChevronDown className="size-4 absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 pointer-events-none" />
        </div>
    );
};

export default FilterDropdown;
