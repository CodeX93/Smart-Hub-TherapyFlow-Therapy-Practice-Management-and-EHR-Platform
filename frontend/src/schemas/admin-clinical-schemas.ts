import * as z from "zod";

export const createFormTemplateSchema = z.object({
  formName: z
    .string()
    .min(1, "Form Name is required")
    .refine((value) => value.trim().length > 0, {
      message: "Form Name is required",
    })
    .max(100, "Form Name must be 100 characters or less"),
  category: z.string().min(1, "Category is required"),
  description: z.string().optional(),
  instructions: z.string().optional(),
  requiresSignature: z.boolean(),
  active: z.boolean(),
  isSystemTemplate: z.boolean(),
  sortOrder: z
    .string()
    .optional()
    .refine((value) => {
      const trimmed = value?.trim();
      if (!trimmed) return true;
      if (!/^\d+$/.test(trimmed)) return false;
      const parsed = Number.parseInt(trimmed, 10);
      return Number.isFinite(parsed) && parsed >= 0 && parsed <= 9999;
    }, {
      message: "Sort order must be a whole number between 0 and 9999.",
    }),
});

export type CreateFormTemplateValues = z.infer<typeof createFormTemplateSchema>;
