# Date, Time, and Timezone Policy

The organisation timezone selected in **Admin Settings -> Administration -> Timezone** is the source of truth for business-time display, scheduling, calendar boundaries, billing, reports, and notifications.

- Exact moments are received and sent as ISO-8601 UTC timestamps and formatted in the configured practice timezone.
- Date-only values use `YYYY-MM-DD` and must not be converted through JavaScript `Date` for display.
- Recurring availability retains local day/time plus its IANA timezone; generated sessions are converted to UTC.
- The browser timezone must not silently replace the practice timezone.
- If practice configuration is unavailable, use UTC as a safe fallback and surface the configuration problem where appropriate.
- Calendar filters must build practice-local day boundaries and send the corresponding UTC instant range.
- A change to the practice timezone changes presentation and future wall-clock scheduling rules; it does not rewrite existing UTC instants or shift existing date-only values.

Shared scheduling conversions belong in `src/utils/scheduleTimezone.ts` and `src/utils/therapistTimezone.ts`. Do not introduce direct `toISOString()` date-key logic or implicit `Intl.DateTimeFormat().resolvedOptions().timeZone` fallbacks in business flows.
