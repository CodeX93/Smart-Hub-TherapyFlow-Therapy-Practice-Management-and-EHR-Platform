import { useEffect, useState } from "react";
import { DateTime } from "luxon";
import { resolveScheduleTimezone } from "@/utils/scheduleTimezone";
import { getTimelineNowIndicatorTop } from "@/components/scheduling-sections/timeline.constants";

export const useTimelinePosition = (timezone?: string | null) => {
  const [currentTimePos, setCurrentTimePos] = useState<string>("0rem");

  useEffect(() => {
    const zone = resolveScheduleTimezone(timezone);

    const calculatePosition = () => {
      const nowInZone = DateTime.now().setZone(zone);
      setCurrentTimePos(
        getTimelineNowIndicatorTop(nowInZone.hour, nowInZone.minute),
      );
    };

    calculatePosition();
    const interval = setInterval(calculatePosition, 60000);
    return () => clearInterval(interval);
  }, [timezone]);

  return currentTimePos;
};
