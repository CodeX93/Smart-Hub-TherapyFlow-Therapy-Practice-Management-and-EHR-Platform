import { Switch } from "@/components/ui/switch";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import type { RecurrenceFormState } from "@/types/recurringSessions";
import {
  MONTH_OPTIONS,
  WEEKDAY_OPTIONS,
  formatRecurrenceSummary,
  toggleDayOfWeek,
  toggleMonthOfYear,
} from "@/utils/recurringSessions";
import { cn } from "@/lib/utils";

interface RecurrenceFieldsProps {
  value: RecurrenceFormState;
  onChange: (next: RecurrenceFormState) => void;
  disabled?: boolean;
  timezoneLabel?: string;
  hideToggle?: boolean;
}

const fieldControlClass =
  "h-12 w-full rounded-xl border border-(--neutral-200) bg-white px-3 text-sm text-(--text-primary-dark) outline-none focus:border-(--neutral-600)";

const fieldSpacingClass = "mt-1.5";

const RecurrenceFields = ({
  value,
  onChange,
  disabled = false,
  timezoneLabel,
  hideToggle = false,
}: RecurrenceFieldsProps) => {
  const setField = <K extends keyof RecurrenceFormState>(
    key: K,
    fieldValue: RecurrenceFormState[K],
  ) => {
    onChange({ ...value, [key]: fieldValue });
  };

  return (
    <div className="space-y-4 rounded-2xl border border-(--neutral-100) bg-(--neutral-50) p-4">
      {!hideToggle ? (
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-(--text-primary-dark)">
            Recurring appointment
          </p>
          <p className="text-xs text-(--text-neutral-600) mt-1">
            Schedule multiple sessions with one rule. Times use{" "}
            {timezoneLabel ? (
              <span className="font-medium text-(--text-primary-dark)">
                {timezoneLabel}
              </span>
            ) : (
              "the therapist timezone"
            )}
            .
          </p>
        </div>
        <Switch
          checked={value.isRecurring}
          onCheckedChange={(checked) => setField("isRecurring", checked)}
          disabled={disabled}
        />
      </div>
      ) : (
        <div>
          <p className="text-sm font-semibold text-(--text-primary-dark)">
            Repeat schedule
          </p>
          <p className="text-xs text-(--text-neutral-600) mt-1">
            Set how often this session should repeat. Times use{" "}
            {timezoneLabel ? (
              <span className="font-medium text-(--text-primary-dark)">
                {timezoneLabel}
              </span>
            ) : (
              "the therapist timezone"
            )}
            .
          </p>
        </div>
      )}

      {value.isRecurring || hideToggle ? (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3 items-end">
            <label className="flex flex-col text-sm font-medium text-(--text-primary-dark)">
              Repeat
              <select
                value={value.recurrenceType}
                disabled={disabled}
                onChange={(event) =>
                  setField(
                    "recurrenceType",
                    event.target.value === "monthly" ? "monthly" : "weekly",
                  )
                }
                className={cn(fieldSpacingClass, fieldControlClass)}
              >
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
              </select>
            </label>

            <label className="flex flex-col text-sm font-medium text-(--text-primary-dark)">
              Every
              <div className={cn(fieldSpacingClass, "flex h-12 items-center gap-2")}>
                <input
                  type="number"
                  min={1}
                  max={8}
                  disabled={disabled}
                  value={value.interval}
                  onChange={(event) => {
                    const parsed = Number.parseInt(event.target.value, 10);
                    setField(
                      "interval",
                      Number.isFinite(parsed)
                        ? Math.min(8, Math.max(1, parsed))
                        : 1,
                    );
                  }}
                  className="h-full w-20 rounded-xl border border-(--neutral-200) bg-white px-3 text-sm outline-none focus:border-(--neutral-600)"
                />
                <span className="text-sm text-(--text-neutral-600)">
                  {value.recurrenceType === "weekly" ? "week(s)" : "month(s)"}
                </span>
              </div>
            </label>
          </div>

          {value.recurrenceType === "weekly" ? (
            <div>
              <p className="text-sm font-medium text-(--text-primary-dark) mb-2">
                Days of week
              </p>
              <div className="flex flex-wrap gap-2">
                {WEEKDAY_OPTIONS.map((day) => {
                  const selected = value.daysOfWeek.includes(day.value);
                  return (
                    <button
                      key={day.value}
                      type="button"
                      disabled={disabled}
                      onClick={() =>
                        setField("daysOfWeek", toggleDayOfWeek(value.daysOfWeek, day.value))
                      }
                      className={cn(
                        "h-9 min-w-12 rounded-full px-3 text-sm font-medium transition-colors",
                        selected
                          ? "bg-(--bg-primary-dark) text-white"
                          : "bg-white border border-(--neutral-200) text-(--text-primary-dark)",
                      )}
                    >
                      {day.label}
                    </button>
                  );
                })}
              </div>
            </div>
          ) : (
            <div>
              <p className="text-sm font-medium text-(--text-primary-dark) mb-2">
                Months (optional filter)
              </p>
              <div className="flex flex-wrap gap-2">
                {MONTH_OPTIONS.map((month) => {
                  const selected = value.monthsOfYear.includes(month.value);
                  return (
                    <button
                      key={month.value}
                      type="button"
                      disabled={disabled}
                      onClick={() =>
                        setField(
                          "monthsOfYear",
                          toggleMonthOfYear(value.monthsOfYear, month.value),
                        )
                      }
                      className={cn(
                        "h-9 rounded-full px-3 text-sm font-medium transition-colors",
                        selected
                          ? "bg-(--bg-primary-dark) text-white"
                          : "bg-white border border-(--neutral-200) text-(--text-primary-dark)",
                      )}
                    >
                      {month.label}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col text-sm font-medium text-(--text-primary-dark)">
              Ends
              <select
                value={value.endMode}
                disabled={disabled}
                onChange={(event) =>
                  setField("endMode", event.target.value === "until" ? "until" : "count")
                }
                className={cn(fieldSpacingClass, fieldControlClass)}
              >
                <option value="count">After number of sessions</option>
                <option value="until">On date</option>
              </select>
            </label>

            {value.endMode === "count" ? (
              <label className="flex flex-col text-sm font-medium text-(--text-primary-dark)">
                Number of sessions
                <input
                  type="number"
                  min={1}
                  max={60}
                  value={value.count}
                  disabled={disabled}
                  onChange={(event) => {
                    const parsed = Number.parseInt(event.target.value, 10);
                    setField(
                      "count",
                      Number.isFinite(parsed) ? Math.min(60, Math.max(1, parsed)) : 1,
                    );
                  }}
                  className={cn(fieldSpacingClass, fieldControlClass)}
                />
              </label>
            ) : (
              <label className="flex flex-col text-sm font-medium text-(--text-primary-dark)">
                Until date
                <CustomDatePicker
                  compact
                  label="Select date"
                  date={value.untilDate}
                  onDateChange={(date) => setField("untilDate", date)}
                  disablePast
                  disabled={disabled}
                  className={fieldSpacingClass}
                />
              </label>
            )}
          </div>

          <p className="text-xs text-(--text-neutral-600)">
            {formatRecurrenceSummary(value)}
          </p>
        </div>
      ) : null}
    </div>
  );
};

export default RecurrenceFields;
