import * as z from "zod";
import { educationEntriesSchema } from "@/schemas/user-profile-education.schema";
import { USER_ACCESS_PROFILE_LIMITS } from "@/schemas/user-access-profiles.schema";

export { USER_ACCESS_PROFILE_LIMITS as THERAPIST_PROFILE_LIMITS };

export const THERAPIST_PROFILE_NUMERIC_LIMITS = {
  yearsOfExperienceMax: 99,
  maxClientsPerDayMax: 100,
  sessionDurationMin: 5,
  sessionDurationMax: 480,
} as const;

const optionalNumericStringSchema = (
  maxDigits: number,
  min: number,
  max: number,
  label: string,
) =>
  z
    .string()
    .max(
      maxDigits,
      `${label} must be ${maxDigits} characters or less`,
    )
    .refine(
      (value) => {
        if (!value.trim()) return true;
        if (!/^\d+$/.test(value)) return false;
        const parsed = Number(value);
        return parsed >= min && parsed <= max;
      },
      { message: `${label} must be between ${min} and ${max}` },
    );

export const licenseSchema = z.object({
  licenseNumber: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.licenseNumber,
      `License number must be ${USER_ACCESS_PROFILE_LIMITS.licenseNumber} characters or less`,
    ),
  licenseType: z.string(),
  licenseState: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.licenseState,
      `License state must be ${USER_ACCESS_PROFILE_LIMITS.licenseState} characters or less`,
    ),
  licenseExpiration: z.date().nullable(),
});

export type LicenseFormValues = z.infer<typeof licenseSchema>;

const profileStringListSchema = z
  .array(
    z
      .string()
      .trim()
      .min(1, "Value cannot be empty")
      .max(50, "Each value must be 50 characters or less"),
  )
  .max(20, "Maximum 20 items allowed");

export const specializationsSchema = z.object({
  clinicalSummary: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.clinicalSummary,
      `Clinical summary must be ${USER_ACCESS_PROFILE_LIMITS.clinicalSummary} characters or less`,
    ),
  researchBackground: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.researchBackground,
      `Research background must be ${USER_ACCESS_PROFILE_LIMITS.researchBackground} characters or less`,
    ),
  supervisoryExperience: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.supervisoryExperience,
      `Supervisory experience must be ${USER_ACCESS_PROFILE_LIMITS.supervisoryExperience} characters or less`,
    ),
  specializations: profileStringListSchema,
  languages: profileStringListSchema,
});

export type SpecializationsFormValues = z.infer<typeof specializationsSchema>;

export const backgroundSchema = z.object({
  careerObjectives: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.careerObjectives,
      `Career objectives must be ${USER_ACCESS_PROFILE_LIMITS.careerObjectives} characters or less`,
    )
    .optional(),
  education: educationEntriesSchema,
});

export type BackgroundFormValues = z.infer<typeof backgroundSchema>;

export const timeSlotSchema = z.object({
  id: z.string(),
  startTime: z.string(),
  endTime: z.string(),
  type: z.enum(["virtual", "in-person"]),
  roomIds: z.array(z.string()),
});

export const workingDaySchema = z.object({
  id: z.string(),
  label: z.string(),
  active: z.boolean(),
  slots: z.array(timeSlotSchema),
});

export const scheduleSchema = z.object({
  timezone: z.string().min(1, "Timezone is required"),
  physicalRoomIds: z
    .array(z.string())
    .min(1, "At least one room is required"),
  workingHours: z.array(workingDaySchema),
});

export type ScheduleFormValues = z.infer<typeof scheduleSchema>;

export const therapistBasicInfoSchema = z.object({
  fullName: z
    .string()
    .min(1, "Full name is required")
    .max(
      USER_ACCESS_PROFILE_LIMITS.fullName,
      `Full name must be ${USER_ACCESS_PROFILE_LIMITS.fullName} characters or less`,
    ),
  email: z
    .string()
    .email("Invalid email address")
    .max(
      USER_ACCESS_PROFILE_LIMITS.email,
      `Email must be ${USER_ACCESS_PROFILE_LIMITS.email} characters or less`,
    ),
  experience: optionalNumericStringSchema(
    USER_ACCESS_PROFILE_LIMITS.yearsOfExperience,
    0,
    THERAPIST_PROFILE_NUMERIC_LIMITS.yearsOfExperienceMax,
    "Years of experience",
  ),
  maxClients: optionalNumericStringSchema(
    USER_ACCESS_PROFILE_LIMITS.maxClientsPerDay,
    1,
    THERAPIST_PROFILE_NUMERIC_LIMITS.maxClientsPerDayMax,
    "Max clients per day",
  ),
  sessionDuration: optionalNumericStringSchema(
    USER_ACCESS_PROFILE_LIMITS.sessionDuration,
    THERAPIST_PROFILE_NUMERIC_LIMITS.sessionDurationMin,
    THERAPIST_PROFILE_NUMERIC_LIMITS.sessionDurationMax,
    "Session duration",
  ),
  emergencyName: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.emergencyContactName,
      `Contact name must be ${USER_ACCESS_PROFILE_LIMITS.emergencyContactName} characters or less`,
    ),
  emergencyPhone: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.emergencyContactNumber,
      `Contact phone must be ${USER_ACCESS_PROFILE_LIMITS.emergencyContactNumber} characters or less`,
    ),
  relationship: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.emergencyContactRelation,
      `Relationship must be ${USER_ACCESS_PROFILE_LIMITS.emergencyContactRelation} characters or less`,
    ),
});

export type TherapistBasicInfoFormValues = z.infer<
  typeof therapistBasicInfoSchema
>;

export const passwordChangeSchema = z
  .object({
    currentPassword: z
      .string()
      .min(1, "Current password is required")
      .max(
        USER_ACCESS_PROFILE_LIMITS.password,
        `Password must be ${USER_ACCESS_PROFILE_LIMITS.password} characters or less`,
      ),
    newPassword: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .max(
        USER_ACCESS_PROFILE_LIMITS.password,
        `Password must be ${USER_ACCESS_PROFILE_LIMITS.password} characters or less`,
      ),
    confirmPassword: z
      .string()
      .min(1, "Please confirm your password")
      .max(
        USER_ACCESS_PROFILE_LIMITS.password,
        `Password must be ${USER_ACCESS_PROFILE_LIMITS.password} characters or less`,
      ),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

export type PasswordChangeFormValues = z.infer<typeof passwordChangeSchema>;

export const zoomIntegrationSchema = z.object({
  accountId: z.string(),
  clientId: z.string(),
  clientSecret: z.string(),
});

export type ZoomIntegrationFormValues = z.infer<typeof zoomIntegrationSchema>;
