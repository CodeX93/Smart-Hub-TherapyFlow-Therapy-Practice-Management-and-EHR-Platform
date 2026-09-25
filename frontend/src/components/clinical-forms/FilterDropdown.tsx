import { useState, useRef, useEffect } from "react";
import { Filter as SolarFilter } from "@solar-icons/react-perf/category/ui/Linear/Filter";
import type { FormStatus } from "../../types/clinical-form.type";

interface FilterDropdownProps {
  selectedFilters: FormStatus[];
  onFilterChange: (filters: FormStatus[]) => void;
}

const FilterDropdown = ({
  selectedFilters,
  onFilterChange,
}: FilterDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const filterOptions: { value: FormStatus; label: string }[] = [
    { value: "pending", label: "Pending" },
    { value: "in-progress", label: "In Progress" },
    { value: "completed", label: "Completed" },
  ];

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleCheckboxChange = (value: FormStatus) => {
    const newFilters = selectedFilters.includes(value)
      ? selectedFilters.filter((f) => f !== value)
      : [...selectedFilters, value];
    onFilterChange(newFilters);
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-10 h-10 rounded-lg cursor-pointer shadow-xs shadow-[#1E282E0A] border border-(--text-neutral-100) bg-white flex items-center justify-center hover:bg-gray-50"
      >
        <SolarFilter size={20} color="#1B1C20" />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-48 bg-white border border-(--text-neutral-100) rounded-lg shadow-lg z-50">
          <div className="p-3 space-y-2">
            {filterOptions.map((option) => (
              <label
                key={option.value}
                className="flex items-center gap-2 cursor-pointer hover:bg-gray-50 p-2 rounded"
              >
                <input
                  type="checkbox"
                  checked={selectedFilters.includes(option.value)}
                  onChange={() => handleCheckboxChange(option.value)}
                  className="w-4 h-4 rounded border-gray-300 text-(--bg-primary-dark) focus:ring-(--bg-primary-dark) cursor-pointer"
                />
                <span className="text-sm text-(--text-primary-dark)">
                  {option.label}
                </span>
              </label>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default FilterDropdown;

