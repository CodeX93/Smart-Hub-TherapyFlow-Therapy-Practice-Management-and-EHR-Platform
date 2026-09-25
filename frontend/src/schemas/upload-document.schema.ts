import { z } from "zod";

export const uploadDocumentSchema = z.object({
  documentType: z.string().min(1, "Please select a document type"),
  description: z.string().optional(),
});

export type UploadDocumentFormValues = z.infer<typeof uploadDocumentSchema>;

