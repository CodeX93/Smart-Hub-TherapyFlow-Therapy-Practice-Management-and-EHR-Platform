import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  setNewPasswordSchema,
  type SetNewPasswordFormValues,
} from "../../../schemas/set-new-password.schema";
import { useLocation, useNavigate } from "react-router-dom";
import TherapistSetNewPasswordForm from "../therapist/set-new-password/TherapistSetNewPasswordForm";
import { usePortalResetPasswordMutation } from "@/store/api/portalAuthApi";
import { getApiErrorMessage } from "@/utils/apiError";

const ClientSetNewPassword = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [resetPassword, { isLoading, isError, error }] =
    usePortalResetPasswordMutation();
  const form = useForm<SetNewPasswordFormValues>({
    resolver: zodResolver(setNewPasswordSchema),
    defaultValues: {
      password: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (values: SetNewPasswordFormValues) => {
    const params = new URLSearchParams(location.search);
    const token = params.get("token") || "";

    if (!token) {
      console.error("No reset token found in URL");
      return;
    }

    try {
      await resetPassword({
        token,
        password: values.password,
      }).unwrap();

      navigate("/auth/password-reset-success");
    } catch (resetError) {
      console.error("Client portal reset password failed:", resetError);
    }
  };

  return (
    <div className="bg-white max-w-134.75 max-h-auto w-full h-full shadow-xs shadow-(--shadow) rounded-xl border-none">
      <div className="md:px-12 md:py-16 px-4 py-8 flex flex-col items-center justify-between gap-8 mb-8 md:mb-0">
        <div className="w-full flex flex-col">
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
            buttonText={isLoading ? "Updating..." : "Reset password"}
            errorMessage={isError ? getApiErrorMessage(error) : undefined}
          />
        </div>
      </div>
    </div>
  );
};

export default ClientSetNewPassword;
