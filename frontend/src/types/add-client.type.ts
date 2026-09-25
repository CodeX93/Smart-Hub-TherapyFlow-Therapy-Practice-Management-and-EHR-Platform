import { z } from "zod";
import { isValidInsuranceAmount } from "@/utils/insuranceAmountInput";
import { isValidInsurancePhone } from "@/utils/phoneInput";

export const addClientSchema = z.object({
  // Personal Information
  fullName: z
    .string()
    .trim()
    .min(1, "Full Name is required")
    .max(100, "Full Name cannot exceed 100 characters"),
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Invalid email address"),
  phone: z.string().optional(),
  dateOfBirth: z.string().optional(),
  gender: z.string().optional(),
  maritalStatus: z.string().optional(),
  pronouns: z.string().optional(),
  preferredLanguage: z.string().optional(),
  /**
   * Not required: the admin client form renders no timezone input and never sends the
   * value, so demanding one only blocked saves over a field nobody could fill in. The
   * backend resolves the client's zone - their own setting, else the clinic's.
   */
  timezone: z.string().default(""),
  
  // Immediate Setup
  enablePortalAccess: z.boolean().default(false),
  emailNotifications: z.boolean().default(true),
  
  // Address Information
  streetAddress1: z.string().optional(),
  streetAddress2: z.string().optional(),
  city: z.string().optional(),
  stateProvince: z.string().optional(),
  zipPostalCode: z.string().optional(),
  country: z.string().optional(),
  legacyAddress: z.boolean().default(false),
  addressLegacy: z.string().optional(),
  stateLegacy: z.string().optional(),
  zipCodeLegacy: z.string().optional(),
  
  // Emergency Contact (Legacy)
  emergencyContactLegacy: z.boolean().default(false),
  contactName: z.string().optional(),
  contactPhone: z.string().optional(),
  relationshipToClient: z.string().optional(),
  
  // Referral & Case Information
  startDate: z.string().optional(),
  referralDate: z.string().optional(),
  referrerName: z.string().optional(),
  referenceNumber: z.string().optional(),
  clientSource: z.string().optional(),
  legacyReferral: z.boolean().default(false),
  referringPersonName: z.string().optional(),
  referralSource: z.string().optional(),
  referralType: z.string().optional(),
  referralNotes: z.string().optional(),
  
  // Employment & Socioeconomic
  employmentStatus: z.string().optional(),
  educationLevel: z.string().optional(),
  numberOfDependents: z.number().default(0),
  
  // Clinical
  status: z.string().default("active"),
  assignedTherapistId: z.string().optional(),
  clientType: z.string().optional(),
  clientStage: z.string().default("intake"),
  serviceType: z.string().optional(),
  serviceFrequency: z.string().optional(),
  needsFollowUp: z.boolean().default(false),
  priority: z.string().optional(),
  dueDate: z.string().optional(),
  followUpNotes: z.string().optional(),
  insuranceInformation: z.boolean().default(false),
  insuranceProvider: z.string().optional(),
  insuranceType: z.string().optional(),
  treatmentModality: z.string().optional(),
  policyNumber: z.string().optional(),
  groupNumber: z.string().optional(),
  copayAmount: z.string().optional(),
  deductible: z.string().optional(),
  insurancePhone: z.string().optional(),
  generalNotes: z.string().optional(),
}).superRefine((data, ctx) => {
  if (!data.insuranceInformation) return;

  if (!data.insuranceProvider?.trim()) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: "Insurance Provider is required",
      path: ["insuranceProvider"],
    });
  }

  if (!data.policyNumber?.trim()) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: "Policy Number is required",
      path: ["policyNumber"],
    });
  }

  if (!isValidInsuranceAmount(data.copayAmount)) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: "Copay must be a number ≥ 0 with up to 8 digits and 2 decimals (e.g. 25, 25.00)",
      path: ["copayAmount"],
    });
  }

  if (!isValidInsuranceAmount(data.deductible)) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: "Deductible must be a number ≥ 0 with up to 8 digits and 2 decimals (e.g. 25, 25.00)",
      path: ["deductible"],
    });
  }

  if (!isValidInsurancePhone(data.insurancePhone)) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message:
        "Insurance phone must contain only digits, spaces, dashes, parentheses, or a leading +",
      path: ["insurancePhone"],
    });
  }

  if (data.insurancePhone && data.insurancePhone.length > 20) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: "Insurance phone cannot exceed 20 characters",
      path: ["insurancePhone"],
    });
  }
});

export type AddClientFormValues = z.infer<typeof addClientSchema>;
