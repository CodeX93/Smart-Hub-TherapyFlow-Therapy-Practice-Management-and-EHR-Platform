import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import type { ResetPasswordFormValues } from "@/schemas/reset.schema";
import { type UseFormReturn } from "react-hook-form";
import CustomInput from "@/components/form/CustomInput";
import { NavLink } from "react-router-dom";

interface AuthForgotPasswordFormProps {
  form: UseFormReturn<ResetPasswordFormValues>;
  onSubmit: (values: ResetPasswordFormValues) => void;
  backToLoginPath: string; // Add back link prop
  submitLabel?: string;
  showBackToLogin?: boolean;
  errorMessage?: string;
}

const AuthForgotPasswordForm = ({
  form,
  onSubmit,
  backToLoginPath,
  submitLabel = "Reset Password",
  showBackToLogin = true,
  errorMessage,
}: AuthForgotPasswordFormProps) => {
  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 mt-8">
        <CustomInput
          control={form.control}
          name="email"
          label="Email"
          type="email"
          required
        />

        <Button
          type="submit"
          className="my-0 w-full h-11.5 bg-(--bg-primary-dark) text-white text-[0.875rem] leading-5.5 font-semibold mt-5 rounded-full cursor-pointer"
        >
          {submitLabel}
        </Button>

        {errorMessage ? (
          <div className="rounded-[0.75rem] border border-(--status-denied) bg-(--light-red) px-3 py-2 text-[0.8125rem] text-(--status-denied)">
            {errorMessage}
          </div>
        ) : null}

        {showBackToLogin && (
          <p className="mt-4 text-(--text-neutral-600) text-sm leading-5.5 text-center font-normal">
            Remember your password?{" "}
            <NavLink
              to={backToLoginPath}
              className="text-(--text-primary-500) font-semibold hover:underline"
            >
              Back to Sign in
            </NavLink>
          </p>
        )}
      </form>
    </Form>
  );
};

export default AuthForgotPasswordForm;
