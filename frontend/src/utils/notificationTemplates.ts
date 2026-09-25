const TEMPLATE_CHANNEL_TYPES = new Set(["in_app", "email", "sms"]);

const TEMPLATE_NAME_EVENT_SUFFIXES = ["_in_app", "_email", "_sms", "_emaill"] as const;

export function isNotificationTemplateChannelType(
  value: string | null | undefined,
): boolean {
  if (!value?.trim()) return false;
  return TEMPLATE_CHANNEL_TYPES.has(
    value.trim().toLowerCase().replace(/-/g, "_"),
  );
}

export function normalizeNotificationTemplateChannelType(
  value: string | null | undefined,
): string {
  if (!value?.trim()) return "IN_APP";

  const normalized = value.trim().toLowerCase().replace(/-/g, "_");
  if (normalized === "in_app") return "IN_APP";
  if (normalized === "email") return "EMAIL";
  if (normalized === "sms") return "SMS";

  return value.trim().toUpperCase();
}

export function deriveNotificationEventTypeFromTemplateName(
  name: string | null | undefined,
): string | null {
  if (!name?.trim()) return null;

  const normalized = name.trim().toLowerCase();
  for (const suffix of TEMPLATE_NAME_EVENT_SUFFIXES) {
    if (normalized.endsWith(suffix)) {
      return normalized.slice(0, -suffix.length);
    }
  }

  return null;
}

function findCanonicalEventType(
  candidate: string,
  validEventTypes: string[],
): string | null {
  if (!candidate.trim()) return null;

  const exact = validEventTypes.find((value) => value === candidate);
  if (exact) return exact;

  const lower = candidate.toLowerCase();
  const caseInsensitive = validEventTypes.find(
    (value) => value.toLowerCase() === lower,
  );
  if (caseInsensitive) return caseInsensitive;

  return candidate.includes("_") ? lower : null;
}

export function resolveNotificationTemplateChannelType(args: {
  name: string;
  type: string;
  eventType: string;
}): string {
  if (isNotificationTemplateChannelType(args.type)) {
    return normalizeNotificationTemplateChannelType(args.type);
  }

  if (isNotificationTemplateChannelType(args.eventType)) {
    return normalizeNotificationTemplateChannelType(args.eventType);
  }

  const derivedEventType = deriveNotificationEventTypeFromTemplateName(args.name);
  if (derivedEventType && args.name.trim()) {
    const suffix = args.name.trim().toLowerCase().slice(derivedEventType.length);
    if (suffix.endsWith("_email") || suffix === "email") return "EMAIL";
    if (suffix.endsWith("_in_app") || suffix === "in_app") return "IN_APP";
    if (suffix.endsWith("_sms") || suffix === "sms") return "SMS";
  }

  return normalizeNotificationTemplateChannelType(args.type);
}

export function resolveNotificationTemplateEventType(args: {
  name: string;
  type: string;
  eventType: string;
  validEventTypes?: string[];
}): string {
  const validEventTypes = args.validEventTypes ?? [];
  const trimmedEventType = args.eventType?.trim() ?? "";

  if (trimmedEventType && !isNotificationTemplateChannelType(trimmedEventType)) {
    const resolved = findCanonicalEventType(trimmedEventType, validEventTypes);
    if (resolved) return resolved;
  }

  const fromName = deriveNotificationEventTypeFromTemplateName(args.name);
  if (fromName) {
    const resolved = findCanonicalEventType(fromName, validEventTypes);
    if (resolved) return resolved;
    return fromName;
  }

  if (trimmedEventType && !isNotificationTemplateChannelType(trimmedEventType)) {
    return findCanonicalEventType(trimmedEventType, validEventTypes) ?? trimmedEventType;
  }

  return "";
}
