import type { CustomSelectOption } from "@/components/form/CustomSelect";
import type { AddClientFormValues } from "@/types/add-client.type";
import type { SystemOptionCategoryKey } from "@/constants/systemOptionCategoryKeys";
import type {
  SystemOptionCategory,
  SystemOptionValue,
} from "@/store/api/admin/systemOptions.api";

export function buildOptionsByCategory(
  categories: SystemOptionCategory[] | undefined,
): Map<string, SystemOptionValue[]> {
  const map = new Map<string, SystemOptionValue[]>();
  if (!categories) return map;

  for (const category of categories) {
    if (!category.categoryKey) continue;
    map.set(
      category.categoryKey,
      (category.options ?? []).filter((option) => option.isActive),
    );
  }

  return map;
}

export function getCategoryOptions(
  optionsByCategory: Map<string, SystemOptionValue[]>,
  categoryKey: string,
): SystemOptionValue[] {
  return optionsByCategory.get(categoryKey) ?? [];
}

export function toSelectOptions(options: SystemOptionValue[]): CustomSelectOption[] {
  return [...options]
    .sort((a, b) => a.sortOrder - b.sortOrder || a.optionLabel.localeCompare(b.optionLabel))
    .map((option) => ({
      value: option.optionKey,
      label: option.optionLabel,
    }));
}

function normalizeToken(value: string): string {
  return value.trim().toLowerCase().replace(/[\s-]+/g, "_");
}

/** Match API/form value to catalog optionKey (case-insensitive key or label). */
export function resolveOptionKey(
  options: SystemOptionValue[],
  value: string | null | undefined,
): string {
  if (!value?.trim()) return "";

  const trimmed = value.trim();
  const exact = options.find((option) => option.optionKey === trimmed);
  if (exact) return exact.optionKey;

  const trimmedLower = trimmed.toLowerCase();
  const byKeyCi = options.find(
    (option) => option.optionKey.toLowerCase() === trimmedLower,
  );
  if (byKeyCi) return byKeyCi.optionKey;

  const byLabelCi = options.find(
    (option) => option.optionLabel.toLowerCase() === trimmedLower,
  );
  if (byLabelCi) return byLabelCi.optionKey;

  const normalized = normalizeToken(trimmed);
  const byKey = options.find(
    (option) => normalizeToken(option.optionKey) === normalized,
  );
  if (byKey) return byKey.optionKey;

  const byLabel = options.find(
    (option) => normalizeToken(option.optionLabel) === normalized,
  );
  if (byLabel) return byLabel.optionKey;

  return trimmed;
}

export function resolveOptionLabel(
  options: SystemOptionValue[],
  optionKey: string | null | undefined,
): string {
  if (!optionKey?.trim()) return "—";

  const resolvedKey = resolveOptionKey(options, optionKey);
  const match = options.find((option) => option.optionKey === resolvedKey);
  return match?.optionLabel ?? formatOptionKeyFallback(optionKey);
}

export function getDefaultOptionKey(options: SystemOptionValue[]): string {
  return (
    options.find((option) => option.isDefault)?.optionKey ??
    options[0]?.optionKey ??
    ""
  );
}

export function matchesOptionKey(
  value: string | null | undefined,
  expectedKey: string,
): boolean {
  if (!value?.trim()) return false;
  return normalizeToken(value) === normalizeToken(expectedKey);
}

const CLIENT_OPTION_FIELDS = [
  "gender",
  "maritalStatus",
  "preferredLanguage",
  "clientSource",
  "employmentStatus",
  "educationLevel",
  "status",
  "clientStage",
  "clientType",
  "serviceType",
  "serviceFrequency",
  "insuranceProvider",
  "insuranceType",
  "treatmentModality",
  "priority",
] as const satisfies ReadonlyArray<keyof AddClientFormValues>;

const CLIENT_FIELD_CATEGORY: Record<
  (typeof CLIENT_OPTION_FIELDS)[number],
  SystemOptionCategoryKey
> = {
  gender: "gender",
  maritalStatus: "marital_status",
  preferredLanguage: "preferred_language",
  clientSource: "client_source",
  employmentStatus: "employment_status",
  educationLevel: "education_level",
  status: "client_status",
  clientStage: "client_stage",
  clientType: "client_type",
  serviceType: "service_type",
  serviceFrequency: "service_frequency",
  insuranceProvider: "insurance_providers",
  insuranceType: "insurance_types",
  treatmentModality: "treatment_modalities",
  priority: "task_priority",
};

export function resolveClientFormOptionFields(
  values: AddClientFormValues,
  optionsByCategory: Map<string, SystemOptionValue[]>,
): AddClientFormValues {
  const resolved = { ...values };

  for (const field of CLIENT_OPTION_FIELDS) {
    const raw = values[field];
    if (typeof raw !== "string" || !raw.trim()) continue;

    const categoryKey = CLIENT_FIELD_CATEGORY[field];
    const options = getCategoryOptions(optionsByCategory, categoryKey);
    if (options.length === 0) continue;

    resolved[field] = resolveOptionKey(options, raw);
  }

  return resolved;
}

export function toFilterSelectOptions(
  options: SystemOptionValue[],
  allLabel: string,
): CustomSelectOption[] {
  return [{ label: allLabel, value: "all" }, ...toSelectOptions(options)];
}

export function buildLabelMap(options: SystemOptionValue[]): Map<string, string> {
  const map = new Map<string, string>();
  for (const option of options) {
    map.set(option.optionKey, option.optionLabel);
    map.set(option.optionKey.toLowerCase(), option.optionLabel);
  }
  return map;
}

export function formatOptionKeyFallback(optionKey: string): string {
  return optionKey
    .trim()
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

export function buildClientCreateDefaults(
  optionsByCategory: Map<string, SystemOptionValue[]>,
): Pick<AddClientFormValues, "status" | "clientStage"> {
  const statusOptions = getCategoryOptions(optionsByCategory, "client_status");
  const activeStatusKey = resolveOptionKey(statusOptions, "active");

  return {
    status: activeStatusKey || getDefaultOptionKey(statusOptions),
    clientStage: getDefaultOptionKey(
      getCategoryOptions(optionsByCategory, "client_stage"),
    ),
  };
}
