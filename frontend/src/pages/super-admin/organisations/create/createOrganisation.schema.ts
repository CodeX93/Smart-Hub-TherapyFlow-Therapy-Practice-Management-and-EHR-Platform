import { z } from "zod";
import {
  DATA_RESIDENCY_OPTIONS,
  isRegionConsistentWithDataResidency,
  isValidSelectValue,
  REGION_OPTIONS,
  TIMEZONE_OPTIONS,
} from "../complianceOptions";
import { CREATE_ORG_FIELD_LIMITS } from "./createOrganisation.utils";

const optionalUserLimit = z.union([
  z.literal(""),
  z
    .string()
    .refine((v) => /^\d+$/.test(v), "Enter a valid number")
    .refine((v) => {
      const parsed = Number.parseInt(v, 10);
      return Number.isFinite(parsed) && parsed >= 0 && parsed <= CREATE_ORG_FIELD_LIMITS.userLimitMax;
    }, `Limit must be between 0 and ${CREATE_ORG_FIELD_LIMITS.userLimitMax.toLocaleString()}`),
]);

export const createOrganisationSchema = z
  .object({
    organisationName: z
      .string()
      .min(1, "Organization name is required")
      .max(CREATE_ORG_FIELD_LIMITS.organisationName, "Organization name is too long"),
    tenantSlug: z
      .string()
      .min(1, "Tenant slug is required")
      .max(CREATE_ORG_FIELD_LIMITS.tenantSlug, "Tenant slug is too long")
      .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, "Must be lowercase and hyphenated"),
    industry: z
      .string()
      .max(CREATE_ORG_FIELD_LIMITS.industry, "Too long")
      .optional(),

    firstName: z
      .string()
      .min(1, "First name is required")
      .max(CREATE_ORG_FIELD_LIMITS.firstName, "First name is too long"),
    lastName: z
      .string()
      .min(1, "Last name is required")
      .max(CREATE_ORG_FIELD_LIMITS.lastName, "Last name is too long"),
    email: z
      .string()
      .min(1, "Email is required")
      .max(CREATE_ORG_FIELD_LIMITS.email, "Email is too long")
      .email("Enter a valid email"),

    plan: z.string().min(1, "Plan is required"),
    billingCycle: z
      .union([z.literal(""), z.enum(["Monthly", "Yearly"])])
      .refine((value) => value !== "", "Billing cycle is required"),
    trialDays: z
      .string()
      .min(1, "Trial days is required")
      .refine((v) => /^\d+$/.test(v), "Enter a valid number")
      .refine((v) => {
        const parsed = Number.parseInt(v, 10);
        return (
          Number.isFinite(parsed) &&
          parsed >= 0 &&
          parsed <= CREATE_ORG_FIELD_LIMITS.trialDaysMax
        );
      }, `Trial days must be between 0 and ${CREATE_ORG_FIELD_LIMITS.trialDaysMax}`),
    therapistsOverride: optionalUserLimit,
    supervisorsOverride: optionalUserLimit,
    clientsOverride: optionalUserLimit,

    timezone: z
      .string()
      .min(1, "Timezone is required")
      .refine(
        (value) => isValidSelectValue(value, TIMEZONE_OPTIONS),
        "Select a valid timezone",
      ),
    infrastructureRegion: z
      .string()
      .min(1, "Infrastructure region is required")
      .refine(
        (value) => isValidSelectValue(value, REGION_OPTIONS),
        "Select a valid infrastructure region",
      ),
    dataResidency: z
      .string()
      .min(1, "Data residency is required")
      .refine(
        (value) => isValidSelectValue(value, DATA_RESIDENCY_OPTIONS),
        "Select a valid data residency option",
      ),
  })
  .superRefine((values, context) => {
    if (
      values.infrastructureRegion &&
      values.dataResidency &&
      !isRegionConsistentWithDataResidency(
        values.infrastructureRegion,
        values.dataResidency,
      )
    ) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Infrastructure region must match the selected data residency.",
        path: ["infrastructureRegion"],
      });
    }
  });

export type CreateOrganisationValues = z.infer<typeof createOrganisationSchema>;
