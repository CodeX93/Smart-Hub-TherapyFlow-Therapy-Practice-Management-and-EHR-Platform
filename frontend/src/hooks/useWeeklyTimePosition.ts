import { useState, useEffect } from "react";
import { DateTime } from "luxon";
import { resolveScheduleTimezone } from "@/utils/scheduleTimezone";
import { getTimelineNowIndicatorTop } from "@/components/scheduling-sections/timeline.constants";

export const useWeeklyTimePosition = (
  weekStart: Date,
  timezone?: string | null,
) => {
  const [currentTimePos, setCurrentTimePos] = useState<string | null>(null);
  const [currentDayIndex, setCurrentDayIndex] = useState<number | null>(null);

  useEffect(() => {
    const zone = resolveScheduleTimezone(timezone);
    const daysLocal = Array.from({ length: 7 }).map((_, i) => {
      const d = new Date(weekStart);
      d.setDate(weekStart.getDate() + i);
      return d;
    });

    const calculatePosition = () => {
      const nowInZone = DateTime.now().setZone(zone);
      const pos = getTimelineNowIndicatorTop(nowInZone.hour, nowInZone.minute);

      const todayIndex = daysLocal.findIndex(
        (d) =>
          d.getFullYear() === nowInZone.year &&
          d.getMonth() + 1 === nowInZone.month &&
          d.getDate() === nowInZone.day,
      );
      if (todayIndex >= 0) {
        setCurrentTimePos(pos);
        setCurrentDayIndex(todayIndex);
      } else {
        setCurrentTimePos(null);
        setCurrentDayIndex(null);
      }
    };
    calculatePosition();
    const interval = setInterval(calculatePosition, 60000);
    return () => clearInterval(interval);
  }, [timezone, weekStart]);

  return { currentTimePos, currentDayIndex };
};
