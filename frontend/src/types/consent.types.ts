import * as z from "zod";

export const addFieldSchema = z
  .object({
    type: z.string(),
    label: z.string().optional(),
    placeholder: z.string().optional(),
    helpText: z.string().optional(),
    required: z.boolean(),
    headingText: z.string().optional(),
    sectionTitle: z.string().optional(),
    contentText: z.string().optional(),
    templateText: z.string().optional(),
    options: z.string().optional(),
    defaultValue: z.string().optional(),
    autoPopulate: z.string().optional(),
    conditionalDisplay: z.string().optional(),
    validation: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.type === "Heading (Read-only)") {
      if (!data.headingText || data.headingText.trim() === "") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Heading Text is required",
          path: ["headingText"],
        });
      }
    } else if (data.type === "Information Text (Read-only)") {
      if (!data.contentText || data.contentText.trim() === "") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Content Text is required",
          path: ["contentText"],
        });
      }
    } else {
      // For all other types, label is required
      if (!data.label || data.label.trim() === "") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Field Label is required",
          path: ["label"],
        });
      }

      if (data.type === "Fill-in-the-Blank") {
        if (!data.templateText || data.templateText.trim() === "") {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Template Text is required",
            path: ["templateText"],
          });
        }
      }

      if (
        data.type === "Dropdown" ||
        data.type === "Radio Buttons" ||
        data.type === "Multiple Checkboxes"
      ) {
        if (!data.options || data.options.trim() === "") {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Options are required",
            path: ["options"],
          });
        }
      }
    }
  });

export type AddFieldValues = z.infer<typeof addFieldSchema>;
