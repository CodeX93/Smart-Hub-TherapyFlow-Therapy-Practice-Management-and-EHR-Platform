import type {
  UserProfileEducationEntry,
  UserProfileEducationFormEntry,
  UserProfileEducationPayload,
} from "@/types/user-profile-education.type";
import {
  formatDateOnly,
  parseDateOnly,
} from "./transformer/dates.transformer.ts";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asOptionalNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? undefined : parsed;
  }
  return undefined;
}

export function createEmptyEducationEntry(): UserProfileEducationFormEntry {
  return {
    degreeType: "",
    fieldOfStudy: "",
    institution: "",
    graduationYear: "",
    graduationDate: null,
    isAccredited: true,
    accreditationBody: "",
    notes: "",
  };
}

export function normalizeEducationEntry(
  value: unknown,
): UserProfileEducationEntry | null {
  if (!isRecord(value)) return null;

  return {
    id: asOptionalNumber(value.id),
    degreeType: asString(value.degreeType) || undefined,
    fieldOfStudy: asString(value.fieldOfStudy) || undefined,
    institution: asString(value.institution) || undefined,
    graduationYear: asOptionalNumber(value.graduationYear),
    graduationDate: asString(value.graduationDate) || undefined,
    isAccredited:
      typeof value.isAccredited === "boolean" ? value.isAccredited : undefined,
    accreditationBody: asString(value.accreditationBody) || undefined,
    notes: asString(value.notes) || undefined,
  };
}

export function normalizeEducationEntries(
  value: unknown,
): UserProfileEducationEntry[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((entry) => normalizeEducationEntry(entry))
    .filter((entry): entry is UserProfileEducationEntry => entry !== null);
}

export function mapEducationResponseToForm(
  entries: UserProfileEducationEntry[] = [],
): UserProfileEducationFormEntry[] {
  return entries.map((entry) => ({
    degreeType: entry.degreeType || "",
    fieldOfStudy: entry.fieldOfStudy || "",
    institution: entry.institution || "",
    graduationYear:
      entry.graduationYear !== undefined ? String(entry.graduationYear) : "",
    graduationDate: parseDateOnly(entry.graduationDate),
    isAccredited: entry.isAccredited ?? true,
    accreditationBody: entry.accreditationBody || "",
    notes: entry.notes || "",
  }));
}

export function mapEducationFormToPayload(
  entries: UserProfileEducationFormEntry[],
): UserProfileEducationPayload[] {
  const payloads: UserProfileEducationPayload[] = [];

  for (const entry of entries) {
    const degreeType = entry.degreeType.trim();
    const institution = entry.institution.trim();
    if (!degreeType && !institution) continue;

    const graduationDate = entry.graduationDate
      ? formatDateOnly(entry.graduationDate)
      : undefined;

    // Year is derived from graduation date to avoid a redundant UI field.
    const yearFromDate = entry.graduationDate?.getFullYear();
    const yearFromField = entry.graduationYear.trim()
      ? Number.parseInt(entry.graduationYear.trim(), 10)
      : undefined;
    const graduationYear =
      yearFromDate ??
      (yearFromField !== undefined && !Number.isNaN(yearFromField)
        ? yearFromField
        : undefined);

    payloads.push({
      degreeType: degreeType || undefined,
      fieldOfStudy: entry.fieldOfStudy.trim() || undefined,
      institution: institution || undefined,
      graduationYear,
      graduationDate,
      isAccredited: entry.isAccredited,
      accreditationBody: entry.accreditationBody.trim() || undefined,
      notes: entry.notes.trim() || undefined,
    });
  }

  return payloads;
}

export function areEducationEntriesEqual(
  current: UserProfileEducationEntry[] = [],
  next: UserProfileEducationPayload[] = [],
): boolean {
  return (
    JSON.stringify(mapEducationFormToPayload(mapEducationResponseToForm(current))) ===
    JSON.stringify(next)
  );
}
