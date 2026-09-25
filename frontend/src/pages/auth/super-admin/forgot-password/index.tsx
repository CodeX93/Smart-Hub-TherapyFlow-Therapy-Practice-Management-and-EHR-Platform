import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "react-router-dom";
import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthForgotPasswordForm from "@/components/auth/AuthForgotPasswordForm";
import { resetPasswordSchema, type ResetPasswordFormValues } from "@/schemas/reset.schema";
import { useForgotPasswordStaffMutation } from "@/store/api/authApi";
import { getApiErrorMessage } from "@/utils/apiError";

const SuperAdminForgotPassword = () => {
  const navigate = useNavigate();
  const [forgotPassword, { isLoading, isError, error }] =
    useForgotPasswordStaffMutation();

  const form = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      email: "",
    },
  });

  const onSubmit = async (values: ResetPasswordFormValues) => {
    if (!values.email) return;

    try {
      await forgotPassword({ email: values.email }).unwrap();
      navigate("/auth/staff/check-email");
    } catch (err) {
      console.error("Super admin forgot password failed:", err);
    }
  };

  return (
    <div className="min-h-screen bg-(--bg-primary-light) md:px-10 md:py-10 px-4 py-6">
      <main className="md:px-5 md:pt-4 flex items-center justify-center md:mt-15 mt-8">
        <AuthLayoutWrapper
          title="Welcome back to SmartHub"
          showRightBar={false}
          className="md:min-h-auto"
        >
          <AuthForgotPasswordForm
            form={form}
            onSubmit={onSubmit}
            backToLoginPath="/super-admin/login"
            submitLabel={isLoading ? "Sending..." : "Reset Password"}
            errorMessage={isError ? getApiErrorMessage(error) : undefined}
          />
        </AuthLayoutWrapper>
      </main>
    </div>
  );
};

export default SuperAdminForgotPassword;
