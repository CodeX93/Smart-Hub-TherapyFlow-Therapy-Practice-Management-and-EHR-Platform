import { NavLink } from "react-router-dom";
import { Controller } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Form } from "@/components/ui/form";
import type { LoginFormValues } from "@/schemas/login.schema";
import { type UseFormReturn } from "react-hook-form";
import CustomInput from "@/components/form/CustomInput";

interface AuthLoginFormProps {
  form: UseFormReturn<LoginFormValues>;
  onSubmit: (values: LoginFormValues) => void;
  forgotPasswordPath?: string;
  submitLabel?: string;
  submitButtonClassName?: string;
  loading?: boolean;
  loadingLabel?: string;
  errorMessage?: string;
  identifierLabel?: string;
  identifierInputType?: "email" | "text";
  showStaySignedIn?: boolean;
}

const AuthLoginForm = ({
  form,
  onSubmit,
  forgotPasswordPath,
  submitLabel = "Sign In",
  submitButtonClassName,
  loading = false,
  loadingLabel = "Signing in…",
  errorMessage,
  identifierLabel = "Email or username",
  identifierInputType = "text",
  showStaySignedIn = true,
}: AuthLoginFormProps) => {
  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 mt-8">
        <CustomInput
          control={form.control}
          name="email"
          label={identifierLabel}
          type={identifierInputType}
          required
        />

        <CustomInput
          control={form.control}
          name="password"
          label="Password"
          type="password"
          required
        />

        <div className="mt-2 flex items-center justify-between gap-3">
          {showStaySignedIn ? (
            <Controller
              control={form.control}
              name="staySignedIn"
              render={({ field }) => (
                <label
                  htmlFor="stay-signed-in"
                  className="inline-flex cursor-pointer items-center gap-2 text-[0.875rem] text-(--text-neutral-700)"
                >
                  <Checkbox
                    id="stay-signed-in"
                    checked={Boolean(field.value)}
                    onCheckedChange={(checked) => field.onChange(checked)}
                  />
                  Stay signed in
                </label>
              )}
            />
          ) : (
            <span />
          )}

          {forgotPasswordPath ? (
            <NavLink
              to={forgotPasswordPath}
              className="inline-flex text-[0.875rem] text-(--text-primary-500) font-semibold leading-5.5 hover:underline"
            >
              Forgot your password?
            </NavLink>
          ) : null}
        </div>

        {errorMessage ? (
          <div className="rounded-[0.75rem] border border-(--status-denied) bg-(--light-red) px-3 py-2 text-[0.8125rem] text-(--status-denied)">
            {errorMessage}
          </div>
        ) : null}

        <Button
          type="submit"
          size="lg"
          loading={loading}
          loadingLabel={loadingLabel}
          className={`my-0 mt-5 w-full ${submitButtonClassName ?? ""}`}
        >
          {submitLabel}
        </Button>
      </form>
    </Form>
  );
};

export default AuthLoginForm;
