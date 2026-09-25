import z from "zod";

export const checklistItemSchema = z.object({
  id: z.string(),
  title: z
    .string()
    .min(1, "Item title is required")
    .refine((value) => value.trim().length > 0, {
      message: "Item title is required",
    })
    .max(120, "Item title must be 120 characters or less"),
  category: z.string().min(1, "Category is required"),
  description: z.string().max(500, "Description must be 500 characters or less").optional(),
  templates: z.array(z.string()).optional(),
  required: z.boolean(),
});

export type ChecklistItemValues = z.infer<typeof checklistItemSchema>;

export const createTemplateSchema = z.object({
  templateName: z
    .string()
    .min(1, "Template name is required")
    .refine((value) => value.trim().length > 0, {
      message: "Template name is required",
    })
    .max(120, "Template name must be 120 characters or less"),
  description: z.string().max(500, "Description must be 500 characters or less").optional(),
  customItems: z.array(checklistItemSchema),
});

export type CreateTemplateValues = z.infer<typeof createTemplateSchema>;
