import type { OrganisationFeaturesResponse } from "@/store/api/superAdminApi";
import type { ModuleToggleRow } from "./components/ModuleAccessCard";
import type { UsageLimitRow } from "./components/UsageLimitCard";
import {
  isUsageKey,
  parseUsageLimitOverride,
  sanitizeUsageLimitOverride,
} from "./featureOverrides.utils";

function isUsageFeature(
  keyName: string,
  state: { enabled: boolean; usageLimit: number | null }
): boolean {
  return isUsageKey(keyName) || state.usageLimit !== null;
}

function toTitleCaseToken(token: string): string {
  if (token === "AI" || token === "SSO") {
    return token;
  }

  if (token.length <= 2 && token === token.toUpperCase()) {
    return token;
  }

  const lower = token.toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}

function humanizeFeatureKey(keyName: string): string {
  return keyName
    .split("_")
    .filter(Boolean)
    .map(toTitleCaseToken)
    .join(" ");
}

export function buildModuleRowsFromFeatures(
  features?: OrganisationFeaturesResponse
): ModuleToggleRow[] {
  if (!features) {
    return [];
  }

  return Object.entries(features).map(([keyName, state]) => ({
      title: humanizeFeatureKey(keyName),
      keyName,
      planDefault: "",
      overrideEnabled: state.enabled,
    }));
}

export function buildUsageRowsFromFeatures(
  features?: OrganisationFeaturesResponse
): UsageLimitRow[] {
  if (!features) {
    return [];
  }

  return Object.entries(features)
    .filter(([keyName, state]) => isUsageFeature(keyName, state) && state.enabled)
    .map(([keyName, state]) => ({
      title: humanizeFeatureKey(keyName),
      keyName,
      planDefault: "",
      overrideValue: state.usageLimit === null
        ? ""
        : sanitizeUsageLimitOverride(String(state.usageLimit)),
    }));
}

export function buildFeaturesPayloadFromRows(
  moduleRows: ModuleToggleRow[],
  usageRows: UsageLimitRow[],
  currentFeatures?: OrganisationFeaturesResponse
): OrganisationFeaturesResponse {
  const payload: OrganisationFeaturesResponse = {
    ...(currentFeatures ?? {}),
  };

  const enabledByKey = moduleRows.reduce<Record<string, boolean>>((acc, row) => {
    acc[row.keyName] = row.overrideEnabled;
    return acc;
  }, {});

  moduleRows.forEach((row) => {
    payload[row.keyName] = {
      enabled: row.overrideEnabled,
      usageLimit: currentFeatures?.[row.keyName]?.usageLimit ?? null,
    };
  });

  usageRows.forEach((row) => {
    const parsedUsageLimit = parseUsageLimitOverride(row.overrideValue);
    const fallbackUsageLimit = currentFeatures?.[row.keyName]?.usageLimit;
    payload[row.keyName] = {
      enabled: enabledByKey[row.keyName] ?? currentFeatures?.[row.keyName]?.enabled ?? false,
      usageLimit:
        parsedUsageLimit !== null
          ? parsedUsageLimit
          : typeof fallbackUsageLimit === "number"
            ? fallbackUsageLimit
            : 0,
    };
  });

  moduleRows.forEach((row) => {
    if (!isUsageKey(row.keyName)) return;
    const current = payload[row.keyName];
    if (!current) return;
    if (typeof current.usageLimit === "number" && Number.isFinite(current.usageLimit)) return;
    const fallbackUsageLimit = currentFeatures?.[row.keyName]?.usageLimit;
    payload[row.keyName] = {
      ...current,
      usageLimit: typeof fallbackUsageLimit === "number" ? fallbackUsageLimit : 0,
    };
  });

  return payload;
}

export { isUsageKey } from "./featureOverrides.utils";
