import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { monthNames, dayLabels } from "../../pages/user/user.static";
import {
  DEFAULT_FILTER_CALENDAR_MIN_YEAR,
  getDaysArray,
  getDefaultCalendarMaxYear,
} from "../../utils/transformer/dates.transformer";
import type { CalendarProps } from "../../types/calendar.types";
import CustomSelect from "../form/CustomSelect";

export default function Calendar({
  currentMonth,
  setCurrentMonth,
  selectedDate,
  setSelectedDate,
  disableFuture = false,
  disablePast = false,
  isDropdowns = true,
  minYear = DEFAULT_FILTER_CALENDAR_MIN_YEAR,
  maxYear,
  minDate,
  maxDate,
  markedDates = [],
}: CalendarProps) {
  const [isYearPickerOpen, setIsYearPickerOpen] = useState(false);
  const days = getDaysArray(currentMonth);

  const handlePrevMonth = () => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1),
    );
  };

  const handleNextMonth = () => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1),
    );
  };

  const handleMonthChange = (val: string) => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), parseInt(val)));
  };

  const handleYearChange = (val: string) => {
    setCurrentMonth(new Date(parseInt(val), currentMonth.getMonth()));
  };

  const monthOptions = monthNames.map((name, idx) => ({
    label: name.slice(0, 3),
    value: idx.toString(),
  }));

  const resolvedMaxYear = maxYear ?? getDefaultCalendarMaxYear();
  const startYear = Math.min(minYear, resolvedMaxYear);
  const years = Array.from(
    { length: resolvedMaxYear - startYear + 1 },
    (_, i) => startYear + i,
  );

  const yearOptions = years.map((year) => ({
    label: year.toString(),
    value: year.toString(),
  }));

  const handleYearSelect = (year: number) => {
    setCurrentMonth(new Date(year, currentMonth.getMonth()));
    setIsYearPickerOpen(false);
  };

  return (
    <div className="w-full md:mb-4">
      {!isDropdowns ? (
        <div className="flex items-center justify-between mb-4">
          <button
            type="button"
            onClick={handlePrevMonth}
            className="cursor-pointer"
            disabled={isYearPickerOpen}
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="font-medium text-(--text-primary-dark)">
            {monthNames[currentMonth?.getMonth()]}{" "}
            <button
              type="button"
              onClick={() => setIsYearPickerOpen((open) => !open)}
              className="cursor-pointer rounded px-1 hover:bg-gray-100 transition-colors"
              aria-label="Select year"
            >
              {currentMonth.getFullYear()}
            </button>
          </span>
          <button
            type="button"
            onClick={handleNextMonth}
            className="cursor-pointer"
            disabled={isYearPickerOpen}
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      ) : (
        <div className="flex items-center gap-3 mb-4">
          <button
            onClick={handlePrevMonth}
            className="cursor-pointer p-1 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ChevronLeft className="w-5 h-5 text-(--text-neutral-600)" />
          </button>

          <div className="flex-1 flex gap-2 justify-center">
            <CustomSelect
              value={currentMonth.getMonth().toString()}
              onChange={handleMonthChange}
              options={monthOptions}
              isSearch={false}
              className="h-10 pt-0 pb-0 rounded-full w-24 shadow-none px-3"
              contentClassName="w-24 min-w-0"
            />

            <CustomSelect
              value={currentMonth.getFullYear().toString()}
              onChange={handleYearChange}
              options={yearOptions}
              isSearch={false}
              className="h-10 pt-0 pb-0 rounded-full w-24 shadow-none px-3"
              contentClassName="w-24 min-w-0"
            />
          </div>

          <button
            onClick={handleNextMonth}
            className="cursor-pointer p-1 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ChevronRight className="w-5 h-5 text-(--text-neutral-600)" />
          </button>
        </div>
      )}

      {isYearPickerOpen && !isDropdowns ? (
        <div className="grid max-h-56 grid-cols-4 gap-2 overflow-y-auto place-items-center">
          {years.map((year) => {
            const isSelected = year === currentMonth.getFullYear();
            return (
              <button
                key={year}
                type="button"
                onClick={() => handleYearSelect(year)}
                className={`h-9 w-full rounded-full text-sm font-medium transition ${
                  isSelected
                    ? "bg-(--text-primary-500) text-white"
                    : "text-(--text-primary-dark) hover:bg-gray-100 cursor-pointer"
                }`}
              >
                {year}
              </button>
            );
          })}
        </div>
      ) : (
        <div className="flex flex-col">
          <div className="grid grid-cols-7 mb-3 place-items-center">
            {dayLabels?.map((day) => (
              <div
                key={day}
                className="text-center w-fit text-xs font-medium text-(--text-neutral-800)"
              >
                {day}
              </div>
            ))}
          </div>
          <div className="grid grid-cols-7 place-items-center">
            {days?.map((day, idx) => {
              if (!day) return <div key={idx} />;

              const dateToCheck = new Date(
                currentMonth.getFullYear(),
                currentMonth.getMonth(),
                day,
              );

              const today = new Date();
              today.setHours(0, 0, 0, 0);
              const normalizedMinDate = minDate ? new Date(minDate) : null;
              const normalizedMaxDate = maxDate ? new Date(maxDate) : null;
              normalizedMinDate?.setHours(0, 0, 0, 0);
              normalizedMaxDate?.setHours(0, 0, 0, 0);

              const isFuture = dateToCheck > today;
              const isPast = dateToCheck < today;
              const isBeforeMinDate = normalizedMinDate
                ? dateToCheck < normalizedMinDate
                : false;
              const isAfterMaxDate = normalizedMaxDate
                ? dateToCheck > normalizedMaxDate
                : false;

              const isDisabled =
                (disableFuture && isFuture) ||
                (disablePast && isPast) ||
                isBeforeMinDate ||
                isAfterMaxDate;
              const isMarked = markedDates.includes(day);
              const isSelected = day === selectedDate && !isDisabled;

              return (
                <button
                  key={idx}
                  disabled={isDisabled}
                  onClick={() => !isDisabled && setSelectedDate(day)}
                  aria-label={
                    isMarked
                      ? `${day}, has scheduled sessions`
                      : String(day)
                  }
                  className={`relative h-8 w-8 flex justify-center items-center rounded-full text-sm font-medium transition ${
                    isSelected
                      ? "bg-(--text-primary-500) text-white rounded-full"
                      : isDisabled
                        ? "text-(--text-neutral-200) cursor-not-allowed"
                        : "text-(--text-primary-dark) hover:bg-gray-100 cursor-pointer"
                  }`}
                >
                  {day}
                  {isMarked ? (
                    <span
                      aria-hidden="true"
                      className={`absolute bottom-0.5 h-1 w-1 rounded-full ${
                        isSelected ? "bg-white" : "bg-(--text-primary-500)"
                      }`}
                    />
                  ) : null}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
