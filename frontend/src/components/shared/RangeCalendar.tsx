import { ChevronLeft, ChevronRight } from "lucide-react";
import { monthNames, dayLabels } from "../../pages/user/user.static";
import { getDaysArray } from "../../utils/transformer/dates.transformer";
import { cn } from "@/lib/utils";

interface RangeCalendarProps {
    currentMonth: Date;
    setCurrentMonth: (date: Date) => void;
    selectedRange: { from: Date | undefined; to: Date | undefined } | undefined;
    onSelectRange: (range: { from: Date | undefined; to: Date | undefined } | undefined) => void;
    disableFuture?: boolean;
    disablePast?: boolean;
}

export default function RangeCalendar({
    currentMonth,
    setCurrentMonth,
    selectedRange,
    onSelectRange,
    disableFuture = false,
    disablePast = false,
}: RangeCalendarProps) {
    const days = getDaysArray(currentMonth);

    const handlePrevMonth = () => {
        setCurrentMonth(
            new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1)
        );
    };

    const handleNextMonth = () => {
        setCurrentMonth(
            new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1)
        );
    };

    const handleDayClick = (day: number) => {
        const clickedDate = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day);

        // Logic for range selection
        if (!selectedRange?.from || (selectedRange.from && selectedRange.to)) {
            // Start new range
            onSelectRange({ from: clickedDate, to: undefined });
        } else {
            // Complete range
            if (clickedDate < selectedRange.from) {
                onSelectRange({ from: clickedDate, to: selectedRange.from });
            } else {
                onSelectRange({ from: selectedRange.from, to: clickedDate });
            }
        }
    };

    return (
        <div className="w-full">
            <div className="flex items-center justify-between mb-4 px-2">
                <button onClick={handlePrevMonth} className="cursor-pointer hover:bg-gray-100 p-1 rounded-full text-gray-600 transition-colors">
                    <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="font-medium text-gray-900 text-sm">
                    {monthNames[currentMonth?.getMonth()]} {currentMonth.getFullYear()}
                </span>
                <button onClick={handleNextMonth} className="cursor-pointer hover:bg-gray-100 p-1 rounded-full text-gray-600 transition-colors">
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>

            <div className="flex flex-col">
                <div className="grid grid-cols-7 mb-2 place-items-center">
                    {dayLabels?.map((day) => (
                        <div
                            key={day}
                            className="text-center w-8 text-[0.6875rem] font-medium text-gray-400 uppercase tracking-wider"
                        >
                            {day}
                        </div>
                    ))}
                </div>
                <div className="grid grid-cols-7 gap-y-1">
                    {days?.map((day, idx) => {
                        if (!day) return <div key={idx} className="h-8 w-8" />;

                        const currentDate = new Date(
                            currentMonth.getFullYear(),
                            currentMonth.getMonth(),
                            day
                        );

                        // Create today's date, strip time for accurate comparison
                        const today = new Date();
                        today.setHours(0, 0, 0, 0);

                        // Check disable conditions
                        const isFuture = currentDate > today;
                        const isPast = currentDate < today;
                        const isDisabled = (disableFuture && isFuture) || (disablePast && isPast);

                        // Range Logic
                        const isSelected = selectedRange?.from && currentDate.getTime() === selectedRange.from.getTime();
                        const isEnd = selectedRange?.to && currentDate.getTime() === selectedRange.to.getTime();
                        const isBetween = selectedRange?.from && selectedRange?.to &&
                            currentDate > selectedRange.from && currentDate < selectedRange.to;

                        const isRangeStart = isSelected && selectedRange?.to;
                        const isRangeEnd = isEnd;

                        return (
                            <button
                                key={idx}
                                disabled={isDisabled}
                                onClick={() => !isDisabled && handleDayClick(day)}
                                className={cn(
                                    "h-8 w-8 flex justify-center items-center text-sm transition-all relative z-10",
                                    isDisabled ? "text-gray-300 cursor-not-allowed" : "cursor-pointer text-gray-700 hover:bg-gray-100 rounded-full",

                                    // Selection Styles
                                    (isSelected || isEnd) && !isDisabled && "bg-[#517889] text-white hover:bg-[#517889] rounded-full",

                                    // Range Between Styles need special handling to span properly. 
                                    // Since we are using grid gap, we might need to adjust or remove gap for continuous look.
                                    // For simple implementation without touching grid gap heavily:
                                    isBetween && !isDisabled && "bg-[#517889]/10 text-[#517889] rounded-none hover:bg-[#517889]/20",

                                    // Fix rounding for start/end of range if between exists
                                    isBetween && "w-full",
                                    (isRangeStart || isRangeEnd) && isBetween && "rounded-full" // This logic is tricky with grid.
                                )}
                            // Use inline style or specific logic for continuous backgound if needed.
                            // For now, simple circle selection is robust.
                            >
                                <span className={cn(
                                    "flex h-8 w-8 items-center justify-center rounded-full z-20",
                                    (isSelected || isEnd) && "bg-[#517889] text-white",
                                    isBetween && "bg-[#517889]/10 text-[#517889] rounded-none w-full mx-[-0.125rem]" // attempting to stretch
                                )}>
                                    {day}
                                </span>
                            </button>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}
