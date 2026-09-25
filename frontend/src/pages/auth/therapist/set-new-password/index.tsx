import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  setNewPasswordSchema,
  type SetNewPasswordFormValues,
} from "../../../../schemas/set-new-password.schema";
import { useLocation, useNavigate } from "react-router-dom";
import TherapistSetNewPasswordForm from "./TherapistSetNewPasswordForm";
import { useResetPasswordStaffMutation } from "@/store/api/authApi";
import { getApiErrorMessage } from "@/utils/apiError";

const TherapistSetNewPassword = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [resetPassword, { isLoading, isError, error }] =
    useResetPasswordStaffMutation();
  const path = location.pathname;

  const role = path.includes("/staff/")
    ? "staff"
    : path.includes("/therapist/")
      ? "staff"
      : path.includes("/admin/")
        ? "admin"
        : "";

  const form = useForm<SetNewPasswordFormValues>({
    resolver: zodResolver(setNewPasswordSchema),
    defaultValues: {
      password: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (values: SetNewPasswordFormValues) => {
    const params = new URLSearchParams(location.search);
    const token = params.get("token") || params.get("changePasswordToken") || "";

    if (!token) {
      console.error("No reset token found in URL");
      return;
    }

    try {
      await resetPassword({
        token,
        newPassword: values.password,
      }).unwrap();

      navigate("/auth/staff/password-reset-success");
    } catch (err) {
      console.error("Reset password failed:", err);
    }
  };

  return (
    <div className="bg-white max-w-134.75 max-h-auto w-full h-full shadow-xs shadow-(--shadow) rounded-xl border-none">
      <div className="md:px-12 md:py-16 px-4 py-8 flex flex-col items-center justify-between gap-8 mb-8 md:mb-0">
        <div className="w-full flex flex-col">
          {/* Icon container */}
          <div className="mb-4 relative self-center transition-all duration-300 hover:scale-105 ">
            <img src="/assets/lock.png" alt="lock" />
          </div>

          <h2 className="md:text-2xl text-xl font-semibold text-center text-(--text-primary-dark) mb-2">
            Set New Password
          </h2>

          <p className="text-(--text-neutral-600) text-center text-sm leading-6 mb-6">
            Enter your new password below
          </p>

          <TherapistSetNewPasswordForm
            form={form}
            onSubmit={onSubmit}
            role={role}
            buttonText={isLoading ? "Updating..." : "Reset password"}
            errorMessage={isError ? getApiErrorMessage(error) : undefined}
          />
        </div>
      </div>
    </div>
  );
};

export default TherapistSetNewPassword;
