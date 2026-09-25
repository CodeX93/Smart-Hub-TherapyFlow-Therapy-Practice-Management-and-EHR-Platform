import { z } from "zod";

export const createPlanSchema = z.object({
  planName: z
    .string()
    .min(1, "Plan name is required")
    .max(30, "Plan name must be 30 characters or less"),
  planCode: z
    .string()
    .min(1, "Plan code is required")
    .max(30, "Plan code must be 30 characters or less")
    .regex(
      /^[A-Za-z0-9]+(?:[_-][A-Za-z0-9]+)*$/,
      "Use letters, numbers, underscores, or hyphens"
    ),
  description: z.string().max(500, "Description must be 500 characters or less").optional(),
  billingCycle: z.enum(["Monthly", "Yearly"], { message: "Billing cycle is required" }),
  basePriceUsd: z
    .string()
    .min(1, "Base price is required")
    .max(10, "Base price must be 10 characters or less")
    .refine((v) => !Number.isNaN(Number(v)) && Number(v) >= 0, "Enter a valid price"),
  trialDays: z
    .string()
    .min(1, "Trial days is required")
    .max(3, "Trial days must be 3 digits or less")
    .refine(
      (v) => Number.isInteger(Number(v)) && Number(v) >= 0,
      "Enter a valid number"
    ),
  status: z.enum(["Active", "Draft"], { message: "Status is required" }),
});

export type CreatePlanValues = z.infer<typeof createPlanSchema>;
