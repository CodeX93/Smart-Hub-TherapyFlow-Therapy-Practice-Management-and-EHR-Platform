const ISO_INSTANT_IN_TEXT_PATTERN =
  /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})?/g;

/** Format an API timestamp in the user's local timezone. */
export function formatNotificationTimestamp(
  value: string | null | undefined,
): string {
  if (!value) return "-";

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;

  return parsed.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

/** Replace embedded ISO instants in notification copy with local date/time text. */
export function formatNotificationMessage(
  message: string | null | undefined,
): string {
  if (!message?.trim()) return "-";

  return message.replace(ISO_INSTANT_IN_TEXT_PATTERN, (match) =>
    formatNotificationTimestamp(match),
  );
}

export function formatNotificationDateGroup(
  value: string | null | undefined,
): string | undefined {
  if (!value) return undefined;

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return undefined;

  const today = new Date();
  const startOfToday = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate(),
  );
  const startOfTarget = new Date(
    parsed.getFullYear(),
    parsed.getMonth(),
    parsed.getDate(),
  );
  const dayDiff = Math.round(
    (startOfToday.getTime() - startOfTarget.getTime()) / (24 * 60 * 60 * 1000),
  );

  if (dayDiff === 0) return undefined;
  if (dayDiff === 1) return "Yesterday";

  return parsed.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
  });
}
