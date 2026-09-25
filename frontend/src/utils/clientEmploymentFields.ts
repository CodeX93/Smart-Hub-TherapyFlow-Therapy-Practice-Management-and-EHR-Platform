export type SelectOption = {
  value: string;
  label: string;
};

/** Backend EmploymentStatus enum constants. */
export const employmentStatusOptions: SelectOption[] = [
  { value: "EMPLOYED_FULL_TIME", label: "Employed Full-Time" },
  { value: "EMPLOYED_PART_TIME", label: "Employed Part-Time" },
  { value: "SELF_EMPLOYED", label: "Self-Employed" },
  { value: "UNEMPLOYED", label: "Unemployed" },
  { value: "RETIRED", label: "Retired" },
  { value: "STUDENT", label: "Student" },
  { value: "DISABLED", label: "Disabled" },
  { value: "HOMEMAKER", label: "Homemaker" },
  { value: "PREFER_NOT_TO_SAY", label: "Prefer Not to Say" },
];

/** Backend EducationLevel enum constants. */
export const educationLevelOptions: SelectOption[] = [
  { value: "LESS_THAN_HIGH_SCHOOL", label: "Less than High School" },
  { value: "HIGH_SCHOOL", label: "High School Diploma/GED" },
  { value: "SOME_COLLEGE", label: "Some College" },
  { value: "ASSOCIATE", label: "Associate Degree" },
  { value: "BACHELOR", label: "Bachelor's Degree" },
  { value: "MASTER", label: "Master's Degree" },
  { value: "DOCTORATE", label: "Doctorate/Professional Degree" },
  { value: "PREFER_NOT_TO_SAY", label: "Prefer Not to Say" },
  { value: "OTHER", label: "Other" },
];

const LEGACY_EMPLOYMENT_STATUS: Record<string, string> = {
  EMPLOYED: "EMPLOYED_FULL_TIME",
};

const LEGACY_EDUCATION_LEVEL: Record<string, string> = {
  BACHELORS: "BACHELOR",
  MASTERS: "MASTER",
  DOCTORATES: "DOCTORATE",
  ASSOCIATES: "ASSOCIATE",
};

function normalizeToken(value?: string | null): string {
  return value?.trim().toUpperCase().replace(/[\s-]+/g, "_") ?? "";
}

/** Map form/API strings to backend employment status enum constants. */
export function normalizeEmploymentStatusValue(value?: string | null): string {
  const normalized = normalizeToken(value);
  if (!normalized) return "";
  return LEGACY_EMPLOYMENT_STATUS[normalized] ?? normalized;
}

/** Map form/API strings to backend education level enum constants. */
export function normalizeEducationLevelValue(value?: string | null): string {
  const normalized = normalizeToken(value);
  if (!normalized) return "";
  return LEGACY_EDUCATION_LEVEL[normalized] ?? normalized;
}
