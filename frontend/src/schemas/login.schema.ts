import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().min(1, "Email or username is required"),
  password: z.string().min(6, "Minimum 6 characters"),
  // Keep required (not optional/default) so zodResolver input/output types
  // match useForm<LoginFormValues>; defaultValues supply false in forms.
  staySignedIn: z.boolean(),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
