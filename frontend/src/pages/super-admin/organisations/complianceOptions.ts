import { TIME_ZONE_OPTIONS as DYNAMIC_TIME_ZONE_OPTIONS } from "@/utils/functions/timezone";

export const TIMEZONE_OPTIONS = DYNAMIC_TIME_ZONE_OPTIONS;

export const REGION_OPTIONS = [
  { value: "us-east-1", label: "us-east-1 (N. Virginia)", residency: "US" },
  { value: "us-west-2", label: "us-west-2 (Oregon)", residency: "US" },
  { value: "eu-west-1", label: "eu-west-1 (Ireland)", residency: "EU" },
  { value: "eu-central-1", label: "eu-central-1 (Frankfurt)", residency: "EU" },
] as const;

export const DATA_RESIDENCY_OPTIONS = [
  { value: "US", label: "United States (US)" },
  { value: "EU", label: "Europe (EU)" },
] as const;

export type RegionValue = (typeof REGION_OPTIONS)[number]["value"];
export type DataResidencyValue = (typeof DATA_RESIDENCY_OPTIONS)[number]["value"];

export function isValidSelectValue(
  value: string,
  options: ReadonlyArray<{ value: string }>,
): boolean {
  const trimmed = value.trim();
  if (!trimmed) return false;
  return options.some((option) => option.value === trimmed);
}

export function isRegionConsistentWithDataResidency(
  region: string,
  dataResidency: string,
): boolean {
  const regionOption = REGION_OPTIONS.find((option) => option.value === region.trim());
  if (!regionOption) return false;
  return regionOption.residency === dataResidency.trim();
}

export function normalizeSelectValue(
  value: string,
  options: ReadonlyArray<{ value: string; label: string }>,
): string {
  const trimmed = value.trim();
  if (!trimmed) return "";

  const exact = options.find((option) => option.value === trimmed);
  if (exact) return exact.value;

  const normalized = trimmed.toUpperCase();
  const byValue = options.find((option) => option.value.toUpperCase() === normalized);
  if (byValue) return byValue.value;

  const byLabel = options.find(
    (option) =>
      option.label === trimmed ||
      option.label.toLowerCase() === trimmed.toLowerCase() ||
      option.label.includes(trimmed),
  );
  if (byLabel) return byLabel.value;

  const byPartial = options.find(
    (option) =>
      trimmed.includes(option.value) ||
      option.label.toLowerCase().includes(trimmed.toLowerCase()) ||
      trimmed.toLowerCase().includes(option.value.toLowerCase()),
  );
  return byPartial?.value ?? trimmed;
}

export function validateRegionAndDataResidency(values: {
  region: string;
  dataResidency: string;
}): string | null {
  const region = values.region.trim();
  const dataResidency = values.dataResidency.trim();

  if (!region) return "Region is required.";
  if (!dataResidency) return "Data residency is required.";

  if (!isValidSelectValue(region, REGION_OPTIONS)) {
    return "Select a valid infrastructure region.";
  }

  if (!isValidSelectValue(dataResidency, DATA_RESIDENCY_OPTIONS)) {
    return "Select a valid data residency option.";
  }

  if (!isRegionConsistentWithDataResidency(region, dataResidency)) {
    return "Infrastructure region must match the selected data residency.";
  }

  return null;
}
