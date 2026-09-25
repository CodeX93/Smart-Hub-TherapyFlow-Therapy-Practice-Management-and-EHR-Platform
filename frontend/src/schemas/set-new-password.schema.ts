import { z } from "zod";

export function hasPasswordCharacterMix(password: string) {
  return (
    /[a-zA-Z]/.test(password) &&
    /\d/.test(password) &&
    /[^a-zA-Z0-9]/.test(password)
  );
}

export const SET_NEW_PASSWORD_REQUIREMENT_LABELS = [
  "At least 8 characters",
  "Mix of letters, numbers and symbols",
] as const;

export function getSetNewPasswordRequirementStatus(password: string) {
  const hasMinLength = password.length >= 8;
  const hasCharacterMix = hasPasswordCharacterMix(password);

  return [hasMinLength, hasCharacterMix];
}

export function meetsSetNewPasswordRequirements(password: string) {
  return getSetNewPasswordRequirementStatus(password).every(Boolean);
}

export const setNewPasswordSchema = z
  .object({
    password: z
      .string()
      .min(8, "Password must be at least 8 characters long")
      .refine(hasPasswordCharacterMix, {
        message: "Password must include letters, numbers, and symbols",
      }),
    confirmPassword: z
      .string()
      .min(8, "Confirm password must be at least 8 characters long"),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

export type SetNewPasswordFormValues = z.infer<typeof setNewPasswordSchema>;
