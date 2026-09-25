import { useFormContext } from "react-hook-form";
import { formatTimezoneDisplayLabel } from "@/utils/therapistTimezone";

type ScheduleTimezoneNoteProps = {
  timezone?: string | null;
};

const ScheduleTimezoneNote = ({ timezone }: ScheduleTimezoneNoteProps) => {
  let watchedTimezone: string | undefined;
  try {
    const form = useFormContext<{ timezone?: string }>();
    watchedTimezone = form.watch("timezone");
  } catch {
    watchedTimezone = undefined;
  }

  const resolved = (timezone ?? watchedTimezone)?.trim();
  if (!resolved) return null;

  return (
    <p className="text-xs text-(--text-neutral-600) -mt-2">
      Times below are in{" "}
      <span className="font-medium text-(--text-primary-dark)">
        {formatTimezoneDisplayLabel(resolved)}
      </span>
      , not your device local time.
    </p>
  );
};

export default ScheduleTimezoneNote;
