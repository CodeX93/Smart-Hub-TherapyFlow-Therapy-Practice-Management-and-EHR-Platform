export type CalendarProps = {
  currentMonth: Date;
  setCurrentMonth: (d: Date) => void;
  selectedDate: number;
  setSelectedDate: (d: number) => void;
  disableFuture?: boolean;
  disablePast?: boolean;
  isDropdowns?: boolean;
  minYear?: number;
  maxYear?: number;
  minDate?: Date | null;
  maxDate?: Date | null;
  markedDates?: number[];
};
