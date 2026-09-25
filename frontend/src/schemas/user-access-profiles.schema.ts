import * as z from "zod";
import { educationEntriesSchema } from "@/schemas/user-profile-education.schema";

export const USER_ACCESS_PROFILE_LIMITS = {
  fullName: 100,
  email: 254,
  username: 50,
  password: 128,
  phone: 20,
  licenseNumber: 50,
  licenseState: 50,
  yearsOfExperience: 10,
  clinicalSummary: 500,
  researchBackground: 500,
  supervisoryExperience: 500,
  careerObjectives: 500,
  emergencyContactName: 100,
  emergencyContactNumber: 20,
  emergencyContactEmail: 255,
  emergencyContactRelation: 50,
  maxClientsPerDay: 10,
  sessionDuration: 10,
} as const;

const optionalAdminUserPhoneSchema = z
  .string()
  .max(
    USER_ACCESS_PROFILE_LIMITS.phone,
    `Phone must be ${USER_ACCESS_PROFILE_LIMITS.phone} characters or less`,
  )
  .refine((value) => !value.trim() || /^\+?\d*$/.test(value.trim()), {
    message: "Phone must contain digits and may start with +",
  });

export const addUserSchema = z.object({
  fullName: z
    .string()
    .min(2, "Full name is required")
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
  username: z
    .string()
    .min(3, "Username must be at least 3 characters")
    .max(
      USER_ACCESS_PROFILE_LIMITS.username,
      `Username must be ${USER_ACCESS_PROFILE_LIMITS.username} characters or less`,
    ),
  phone: optionalAdminUserPhoneSchema,
  role: z.string().min(1, "Please select a role"),
  password: z
    .string()
    .min(8, "Password must be at least 8 characters")
    .max(
      USER_ACCESS_PROFILE_LIMITS.password,
      `Password must be ${USER_ACCESS_PROFILE_LIMITS.password} characters or less`,
    ),
});

export type AddUserFormValues = z.infer<typeof addUserSchema>;

// Zod Schemas for Proffessional Detail Sections

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
  yearsOfExperience: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.yearsOfExperience,
      `Years of experience must be ${USER_ACCESS_PROFILE_LIMITS.yearsOfExperience} characters or less`,
    ),
  clinicalSummary: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.clinicalSummary,
      `Clinical summary must be ${USER_ACCESS_PROFILE_LIMITS.clinicalSummary} characters or less`,
    ),
  specializations: profileStringListSchema,
  languages: profileStringListSchema,
});

export type SpecializationsFormValues = z.infer<typeof specializationsSchema>;

export const backgroundSchema = z.object({
  researchBackground: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.researchBackground,
      `Research background must be ${USER_ACCESS_PROFILE_LIMITS.researchBackground} characters or less`,
    )
    .optional(),
  supervisoryExperience: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.supervisoryExperience,
      `Supervisory experience must be ${USER_ACCESS_PROFILE_LIMITS.supervisoryExperience} characters or less`,
    )
    .optional(),
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

export const emergencyContactSchema = z.object({
  emergencyContactName: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.emergencyContactName,
      `Emergency contact name must be ${USER_ACCESS_PROFILE_LIMITS.emergencyContactName} characters or less`,
    ),
  emergencyContactNumber: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.emergencyContactNumber,
      `Emergency contact number must be ${USER_ACCESS_PROFILE_LIMITS.emergencyContactNumber} characters or less`,
    )
    .refine((value) => !value.trim() || /^\+?\d*$/.test(value.trim()), {
      message: "Emergency contact number must contain digits and may start with +",
    }),
  emergencyContactEmail: z
    .string()
    .trim()
    .max(
      USER_ACCESS_PROFILE_LIMITS.emergencyContactEmail,
      `Emergency contact email must be ${USER_ACCESS_PROFILE_LIMITS.emergencyContactEmail} characters or less`,
    )
    .refine((value) => !value || z.string().email().safeParse(value).success, {
      message: "Invalid emergency contact email",
    }),
  emergencyContactRelation: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.emergencyContactRelation,
      `Emergency contact relation must be ${USER_ACCESS_PROFILE_LIMITS.emergencyContactRelation} characters or less`,
    ),
});

export type EmergencyContactFormValues = z.infer<typeof emergencyContactSchema>;

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
  maxClientsPerDay: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.maxClientsPerDay,
      `Max clients per day must be ${USER_ACCESS_PROFILE_LIMITS.maxClientsPerDay} characters or less`,
    ),
  sessionDuration: z
    .string()
    .max(
      USER_ACCESS_PROFILE_LIMITS.sessionDuration,
      `Session duration must be ${USER_ACCESS_PROFILE_LIMITS.sessionDuration} characters or less`,
    ),
  workingHours: z.array(workingDaySchema),
});

export type ScheduleFormValues = z.infer<typeof scheduleSchema>;

/** Public-site consultation hours only — separate from clinical Schedule. */
export const consultationScheduleSchema = z.object({
  workingHours: z.array(workingDaySchema),
});

export type ConsultationScheduleFormValues = z.infer<
  typeof consultationScheduleSchema
>;

export const assignSupervisorSchema = z.object({
  supervisorId: z.string().min(1, "Supervisor is required"),
  therapistId: z.string().min(1, "Therapist is required"),
  assignmentType: z.string().min(1, "Assignment type is required"),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  frequency: z.string().min(1, "Frequency is required"),
  notes: z.string().optional(),
}).refine(
  (data) => !data.startDate || !data.endDate || data.endDate >= data.startDate,
  {
    message: "End date cannot be before start date",
    path: ["endDate"],
  },
);

export type AssignSupervisorFormValues = z.infer<typeof assignSupervisorSchema>;
