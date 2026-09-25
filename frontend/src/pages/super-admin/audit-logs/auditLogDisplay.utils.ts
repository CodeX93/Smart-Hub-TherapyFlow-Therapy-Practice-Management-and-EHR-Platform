export interface ParsedAuditDetailItem {
  key: string;
  value: string;
}

export type AuditChangeState = "changed" | "added" | "removed" | "unchanged";

export interface AuditChangeRow {
  key: string;
  beforeValue: unknown;
  afterValue: unknown;
  state: AuditChangeState;
}

export function formatActionOptionLabel(value: string): string {
  if (!value) return "—";
  if (value === "All Actions") return value;
  return value
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
    .join(" ");
}

export function normalizeBackendTokens(value: string): string {
  return value.replace(/\b[A-Z]+(?:_[A-Z]+)+\b/g, (token) =>
    formatActionOptionLabel(token)
  );
}

export function toAuditLabel(key: string): string {
  return key
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

export function stringifyAuditDetailValue(raw: string): string {
  const trimmed = raw.trim();
  if (!trimmed) return "—";

  if (
    (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
    (trimmed.startsWith("[") && trimmed.endsWith("]"))
  ) {
    try {
      return JSON.stringify(JSON.parse(trimmed), null, 2);
    } catch {
      return normalizeBackendTokens(trimmed);
    }
  }

  return normalizeBackendTokens(trimmed);
}

export function formatArrayLikeDetailValue(raw: string): string {
  const trimmed = raw.trim();
  if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
    return stringifyAuditDetailValue(raw);
  }

  const inner = trimmed.slice(1, -1).trim();
  if (!inner) return "[]";

  const parts: string[] = [];
  let current = "";
  let squareDepth = 0;
  let curlyDepth = 0;
  let inString = false;

  for (let i = 0; i < inner.length; i += 1) {
    const char = inner[i];
    const prev = i > 0 ? inner[i - 1] : "";

    if (char === '"' && prev !== "\\") {
      inString = !inString;
    }

    if (!inString) {
      if (char === "[") squareDepth += 1;
      if (char === "]" && squareDepth > 0) squareDepth -= 1;
      if (char === "{") curlyDepth += 1;
      if (char === "}" && curlyDepth > 0) curlyDepth -= 1;
    }

    if (char === "," && !inString && squareDepth === 0 && curlyDepth === 0) {
      if (current.trim()) parts.push(current.trim());
      current = "";
      continue;
    }

    current += char;
  }

  if (current.trim()) parts.push(current.trim());

  const normalized = parts
    .map((part) => {
      const pretty = stringifyAuditDetailValue(part);
      return pretty.length > 0 ? pretty : "—";
    })
    .filter((part) => {
      const value = part.trim().toLowerCase();
      return value !== "null" && value !== "—";
    });

  return normalized.map((part) => `• ${part}`).join("\n");
}

export function summarizeAuthoritiesPayload(raw: string): string | null {
  const trimmed = raw.trim();
  if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null;

  try {
    const parsed = JSON.parse(trimmed) as {
      authId?: number;
      loginIdentifier?: string;
      authorities?: Array<{ authority?: string }>;
    };
    if (!parsed || typeof parsed !== "object") return null;

    const authorities = Array.isArray(parsed.authorities)
      ? parsed.authorities
          .map((item) => item?.authority)
          .filter((item): item is string => Boolean(item))
      : [];

    const shownAuthorities = authorities.slice(0, 8);
    const remainingCount = Math.max(0, authorities.length - shownAuthorities.length);

    const lines = [
      parsed.authId ? `Auth ID: ${parsed.authId}` : null,
      parsed.loginIdentifier ? `Login: ${parsed.loginIdentifier}` : null,
      authorities.length > 0
        ? `Authorities: ${shownAuthorities.join(", ")}${remainingCount > 0 ? ` (+${remainingCount} more)` : ""}`
        : null,
    ].filter((line): line is string => Boolean(line));

    return lines.length > 0 ? lines.join("\n") : null;
  } catch {
    return null;
  }
}

export function parseAuditDetails(details: string | null): ParsedAuditDetailItem[] {
  if (!details?.trim()) return [];

  const text = details.trim();
  const regex = /([A-Za-z][A-Za-z0-9_]*)=/g;
  const matches = Array.from(text.matchAll(regex));

  if (!matches.length) {
    return [{ key: "Details", value: stringifyAuditDetailValue(text) }];
  }

  const parsed: ParsedAuditDetailItem[] = [];
  for (let index = 0; index < matches.length; index += 1) {
    const match = matches[index];
    const key = match[1];
    const valueStart = (match.index ?? 0) + match[0].length;
    const valueEnd =
      index + 1 < matches.length ? (matches[index + 1].index ?? text.length) : text.length;
    const rawValue = text.slice(valueStart, valueEnd).replace(/,\s*$/, "").trim();
    const summary = summarizeAuthoritiesPayload(rawValue);
    const formattedValue =
      key.toLowerCase() === "details"
        ? formatArrayLikeDetailValue(rawValue)
        : summary ?? stringifyAuditDetailValue(rawValue);

    parsed.push({
      key: toAuditLabel(key),
      value: formattedValue,
    });
  }

  return parsed;
}

export function formatAuditDateTime(isoString: string): string {
  if (!isoString) return "—";
  try {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) return isoString;
    return date.toLocaleString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  } catch {
    return isoString;
  }
}

export function getActionBadgeClass(action: string): string {
  const normalized = action.toUpperCase();
  if (normalized.includes("DRIFT") || normalized.includes("ERROR")) {
    return "bg-[#fef3c7] text-[#92400e]";
  }
  if (
    normalized.includes("SUSPEND") ||
    normalized.includes("TERMINAT") ||
    normalized.includes("ARCHIVED") ||
    normalized.includes("DENIED")
  ) {
    return "bg-[#fee2e2] text-[#991b1b]";
  }
  if (
    normalized.includes("CREATED") ||
    normalized.includes("ONBOARDED") ||
    normalized.includes("REACTIVATED")
  ) {
    return "bg-[#dcfce7] text-[#166534]";
  }
  if (normalized.includes("UPDATED") || normalized.includes("IMPERSONATION") || normalized.includes("PATCH")) {
    return "bg-[#e0f2fe] text-[#075985]";
  }
  if (normalized.startsWith("API_")) {
    return "bg-[#eef2f6] text-[#374151]";
  }
  return "bg-[#eef2f6] text-[#374151]";
}

export function getLogLevelBadgeClass(logLevel: string | null | undefined): string {
  const normalized = (logLevel ?? "").toUpperCase();
  if (normalized === "ERROR" || normalized === "FATAL") {
    return "bg-[#fee2e2] text-[#991b1b]";
  }
  if (normalized === "WARN" || normalized === "WARNING") {
    return "bg-[#fef3c7] text-[#92400e]";
  }
  if (normalized === "INFO") {
    return "bg-[#e0f2fe] text-[#075985]";
  }
  if (normalized === "TRACE" || normalized === "DEBUG") {
    return "bg-[#f1f5f9] text-[#64748b]";
  }
  return "bg-[#eef2f6] text-[#5b6977]";
}

export function getAuditListActionLabel(entry: {
  action: string;
  resourceType?: string | null;
  resourceId?: string | null;
  details?: string | null;
  actionSummary?: string | null;
}): string {
  const methodMatch = entry.details?.match(/\bmethod=([A-Z]+)\b/);
  if (
    (entry.resourceType ?? "").toUpperCase() === "API" &&
    entry.resourceId &&
    methodMatch?.[1]
  ) {
    return `${methodMatch[1]} ${entry.resourceId}`;
  }

  if (entry.actionSummary?.trim()) {
    const summary = entry.actionSummary.trim();
    if (summary.length <= 90) return summary;
  }

  return formatActionOptionLabel(entry.action);
}

export function formatAuditValue(value: unknown): string {
  if (value == null) return "—";
  if (typeof value === "string") return value;
  if (typeof value === "number" || typeof value === "boolean") return String(value);
  if (Array.isArray(value)) {
    return value.length ? value.map((item) => formatAuditValue(item)).join(", ") : "[]";
  }
  if (typeof value === "object") {
    try {
      return JSON.stringify(value);
    } catch {
      return "—";
    }
  }
  return String(value);
}

export function buildAuditChangeRows(
  before: Record<string, unknown> | null,
  after: Record<string, unknown> | null
): AuditChangeRow[] {
  if (!before && !after) return [];

  const beforeKeys = before ? Object.keys(before) : [];
  const afterKeys = after ? Object.keys(after) : [];
  const keyOrder = [...beforeKeys, ...afterKeys.filter((key) => !beforeKeys.includes(key))];

  return keyOrder.map((key) => {
    const beforeValue = before ? before[key] : undefined;
    const afterValue = after ? after[key] : undefined;
    const hasBefore = before ? Object.prototype.hasOwnProperty.call(before, key) : false;
    const hasAfter = after ? Object.prototype.hasOwnProperty.call(after, key) : false;

    if (!hasBefore && hasAfter) {
      return { key, beforeValue: null, afterValue, state: "added" as const };
    }
    if (hasBefore && !hasAfter) {
      return { key, beforeValue, afterValue: null, state: "removed" as const };
    }

    return {
      key,
      beforeValue,
      afterValue,
      state:
        JSON.stringify(beforeValue) === JSON.stringify(afterValue)
          ? ("unchanged" as const)
          : ("changed" as const),
    };
  });
}

export function getChangePillClassName(state: AuditChangeState): string {
  if (state === "changed") return "bg-[#fef3c7] text-[#92400e]";
  if (state === "added") return "bg-[#dcfce7] text-[#166534]";
  if (state === "removed") return "bg-[#fee2e2] text-[#991b1b]";
  return "bg-[#eef2f6] text-[#5b6977]";
}
