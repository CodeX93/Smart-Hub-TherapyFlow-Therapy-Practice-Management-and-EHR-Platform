import z from "zod";

export const createTemplateFormSchema = z.object({
  templateName: z.string().min(1, "Template name is required"),
  category: z.string().min(1, "Category is required"),
  description: z.string().min(1, "Description is required"),
  version: z.string().min(1, "Version is required"),
  isStandardized: z.boolean(),
});

export type CreateTemplateFormValues = z.infer<typeof createTemplateFormSchema>;

export const assignTemplateFormSchema = z.object({
  clientId: z.string().min(1, "Client selection is required"),
  dueDate: z
    .string()
    .min(1, "Due date is required")
    .refine((value) => {
      const parsed = new Date(value);
      return !Number.isNaN(parsed.getTime());
    }, "Invalid due date")
    .refine((value) => {
      const selected = new Date(value);
      selected.setHours(0, 0, 0, 0);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      return selected > today;
    }, "Please select a future date"),
  notes: z.string().optional(),
});

export type AssignTemplateFormValues = z.infer<typeof assignTemplateFormSchema>;
