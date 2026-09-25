import { z } from "zod";

export const createFeatureSchema = z.object({
  featureKey: z
    .string()
    .min(1, "Feature key is required")
    .regex(/^[a-z0-9]+(?:_[a-z0-9]+)*$/, "Use lowercase and underscores only"),
  featureName: z.string().min(1, "Feature name is required"),
  type: z.enum(["Custom", "Core"], { message: "Type is required" }),
  scope: z.enum(["Tenant", "Global"], { message: "Scope is required" }),
  description: z.string().max(500, "Description is too long").optional(),
  defaultEnabled: z.boolean(),
});

export type CreateFeatureValues = z.infer<typeof createFeatureSchema>;
