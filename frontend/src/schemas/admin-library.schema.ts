import * as z from "zod";

const noLeadingSpace = (fieldName: string) =>
  z.string().refine((value) => !/^\s/.test(value), {
    message: `${fieldName} cannot start with space`,
  });

export const addEntrySchema = z.object({
  title: noLeadingSpace("Title")
    .trim()
    .min(1, "Title is required")
    .max(120, "Title cannot exceed 120 characters"),
  content: noLeadingSpace("Content").trim().min(1, "Content is required"),
  category: noLeadingSpace("Category").trim().min(1, "Category is required"),
  tags: noLeadingSpace("Tags").trim().min(1, "At least one tag is required"),
  sortOrder: z.number(),
  smartConnect: z.array(z.string()),
  connectionType: z.enum(["RELATED", "REFERENCE", "DERIVED", "SUPPLEMENT", "OTHER"]),
  connectionDescription: z
    .string()
    .refine((value) => value.length === 0 || !/^\s/.test(value), {
      message: "Connection description cannot start with space",
    }),
}).superRefine((data, ctx) => {
  const MAX_TAG_LENGTH = 30;
  const tags = data.tags
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);

  const tooLongTag = tags.find((tag) => tag.length > MAX_TAG_LENGTH);
  if (tooLongTag) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ["tags"],
      message: `Each tag name must be at most ${MAX_TAG_LENGTH} characters`,
    });
  }
});

export type AddEntryFormData = z.infer<typeof addEntrySchema>;

export const bulkAddSchema = z.object({
  pastedData: z.string().min(1, "Data is required"),
});

export type BulkAddFormData = z.infer<typeof bulkAddSchema>;

export const bulkImportSchema = z.object({
  pastedData: z.string().min(1, "Paste data to import"),
});

export type BulkImportFormData = z.infer<typeof bulkImportSchema>;
