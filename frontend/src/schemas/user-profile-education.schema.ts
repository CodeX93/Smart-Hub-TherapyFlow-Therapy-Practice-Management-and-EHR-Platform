import * as z from "zod";

export const EDUCATION_FIELD_LIMITS = {
  degreeType: 100,
  fieldOfStudy: 100,
  institution: 150,
  graduationYear: 4,
  accreditationBody: 100,
  notes: 500,
  maxEntries: 20,
} as const;

export const educationEntrySchema = z.object({
  degreeType: z
    .string()
    .trim()
    .min(1, "Degree type is required")
    .max(
      EDUCATION_FIELD_LIMITS.degreeType,
      `Degree type must be ${EDUCATION_FIELD_LIMITS.degreeType} characters or less`,
    ),
  fieldOfStudy: z
    .string()
    .max(
      EDUCATION_FIELD_LIMITS.fieldOfStudy,
      `Field of study must be ${EDUCATION_FIELD_LIMITS.fieldOfStudy} characters or less`,
    ),
  institution: z
    .string()
    .trim()
    .min(1, "Institution is required")
    .max(
      EDUCATION_FIELD_LIMITS.institution,
      `Institution must be ${EDUCATION_FIELD_LIMITS.institution} characters or less`,
    ),
  graduationYear: z
    .string()
    .max(
      EDUCATION_FIELD_LIMITS.graduationYear,
      `Graduation year must be ${EDUCATION_FIELD_LIMITS.graduationYear} characters or less`,
    ),
  graduationDate: z.date().nullable(),
  isAccredited: z.boolean(),
  accreditationBody: z
    .string()
    .max(
      EDUCATION_FIELD_LIMITS.accreditationBody,
      `Accreditation body must be ${EDUCATION_FIELD_LIMITS.accreditationBody} characters or less`,
    ),
  notes: z
    .string()
    .max(
      EDUCATION_FIELD_LIMITS.notes,
      `Notes must be ${EDUCATION_FIELD_LIMITS.notes} characters or less`,
    ),
});

export const educationEntriesSchema = z
  .array(educationEntrySchema)
  .max(
    EDUCATION_FIELD_LIMITS.maxEntries,
    `Maximum ${EDUCATION_FIELD_LIMITS.maxEntries} education entries allowed`,
  );

export type EducationEntryFormValues = z.infer<typeof educationEntrySchema>;
