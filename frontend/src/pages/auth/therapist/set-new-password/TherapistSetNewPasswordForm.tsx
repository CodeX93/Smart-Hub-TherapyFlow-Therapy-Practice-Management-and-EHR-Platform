import { Button } from "../../../../components/ui/button";
import { Form } from "../../../../components/ui/form";
import type { SetNewPasswordFormValues } from "../../../../schemas/set-new-password.schema";
import {
  getSetNewPasswordRequirementStatus,
  meetsSetNewPasswordRequirements,
  SET_NEW_PASSWORD_REQUIREMENT_LABELS,
} from "../../../../schemas/set-new-password.schema";
import { type UseFormReturn } from "react-hook-form";
import CustomInput from "@/components/form/CustomInput";
import { useNavigate } from "react-router-dom";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

interface TherapistSetNewPasswordFormProps {
  form: UseFormReturn<SetNewPasswordFormValues>;
  onSubmit: (values: SetNewPasswordFormValues) => void;
  buttonText?: string;
  role?: string;
  errorMessage?: string;
}

const TherapistSetNewPasswordForm = ({
  form,
  onSubmit,
  buttonText = "Reset password",
  role,
  errorMessage,
}: TherapistSetNewPasswordFormProps) => {
  const navigate = useNavigate();
  const password = form.watch("password") ?? "";
  const confirmPassword = form.watch("confirmPassword") ?? "";
  const requirementStatus = getSetNewPasswordRequirementStatus(password);
  const canSubmit =
    meetsSetNewPasswordRequirements(password) &&
    confirmPassword.length > 0 &&
    password === confirmPassword;

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <CustomInput
          control={form.control}
          name="password"
          label="New Password"
          type="password"
          required
        />

        <CustomInput
          control={form.control}
          name="confirmPassword"
          label="Confirm New Password"
          type="password"
          required
        />

        <div className="space-y-3 pt-2">
          <p className="text-(--text-neutral-400) text-sm font-medium">
            Password requirements
          </p>
          <ul className="space-y-2">
            {SET_NEW_PASSWORD_REQUIREMENT_LABELS.map((label, index) => {
              const met = requirementStatus[index];

              return (
                <li
                  key={label}
                  className="flex items-center gap-2 text-sm leading-5"
                >
                  <Check
                    size={14}
                    className={cn(
                      "shrink-0",
                      met
                        ? "text-emerald-600"
                        : "text-(--text-neutral-400)",
                    )}
                  />
                  <span
                    className={cn(
                      met
                        ? "text-emerald-700"
                        : "text-(--text-neutral-600)",
                    )}
                  >
                    {label}
                  </span>
                </li>
              );
            })}
          </ul>
        </div>

        <Button
          type="submit"
          disabled={!canSubmit}
          className="my-0 w-full h-11.5 bg-(--bg-primary-dark) text-white text-[0.875rem] leading-5.5 font-semibold mt-5 rounded-full cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
        >
          {buttonText}
        </Button>

        {errorMessage ? (
          <div className="rounded-[0.75rem] border border-(--status-denied) bg-(--light-red) px-3 py-2 text-[0.8125rem] text-(--status-denied)">
            {errorMessage}
          </div>
        ) : null}

        <p className="mt-4 text-(--text-neutral-600) text-sm leading-5.5 text-center font-normal">
          Remember your password?{" "}
          <span
            className="text-(--text-primary-500) font-semibold cursor-pointer"
            onClick={() =>
              navigate(
                role === "staff"
                  ? "/auth/staff/login"
                  : role
                    ? `/auth/${role}/login`
                    : "/auth/login",
              )
            }
          >
            Back to Sign in
          </span>
        </p>
      </form>
    </Form>
  );
};

export default TherapistSetNewPasswordForm;
