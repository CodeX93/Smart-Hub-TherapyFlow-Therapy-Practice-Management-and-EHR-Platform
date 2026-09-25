import React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomInput from "../../form/CustomInput";
import { Button } from "../../ui/button";
import { Form } from "../../ui/form";
import { passwordChangeSchema, THERAPIST_PROFILE_LIMITS, type PasswordChangeFormValues } from "@/schemas/general-profile-modal-schemas";
import { useChangePasswordStaffMutation } from "@/store/api/authApi";
import { useAppDispatch } from "@/store/hooks";
import { clearSession } from "@/store/authSlice";
import { getApiErrorMessage } from "@/utils/apiError";

interface PasswordSectionProps {
  onNotify?: (message: string, type: "success" | "error") => void;
}

const PasswordSection: React.FC<PasswordSectionProps> = ({ onNotify }) => {
  const dispatch = useAppDispatch();
  const [changePassword, { isLoading }] = useChangePasswordStaffMutation();
  const form = useForm<PasswordChangeFormValues>({
    resolver: zodResolver(passwordChangeSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  const handleSave = async (values: PasswordChangeFormValues) => {
    try {
      const response = await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      }).unwrap();

      onNotify?.(
        response.message || "Password changed successfully. Please login again.",
        "success",
      );
      form.reset();
      window.setTimeout(() => {
        dispatch(clearSession());
        window.location.assign("/auth/staff/login");
      }, 1500);
    } catch (error) {
      onNotify?.(getApiErrorMessage(error), "error");
    }
  };

  return (
    <div className="flex flex-col gap-6 h-full pt-6 md:px-1">
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(handleSave)}
          className="flex flex-col gap-4 h-full"
        >
          <CustomInput
            control={form.control}
            label="Current Password"
            name="currentPassword"
            type="password"
            maxLength={THERAPIST_PROFILE_LIMITS.password}
          />
          <CustomInput
            control={form.control}
            label="New Password"
            name="newPassword"
            type="password"
            maxLength={THERAPIST_PROFILE_LIMITS.password}
          />
          <CustomInput
            control={form.control}
            label="Confirm New Password"
            name="confirmPassword"
            type="password"
            maxLength={THERAPIST_PROFILE_LIMITS.password}
          />

          <div className="pt-4 flex justify-end h-full items-end pb-6">
            <Button
              type="submit"
              disabled={isLoading}
            loading={isLoading}
            loadingLabel="Saving..."
              className="rounded-full px-10 py-3 h-14 bg-(--bg-primary-dark) text-white hover:opacity-90 font-semibold text-[1rem] leading-6 cursor-pointer"
            >
            Save changes
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
};

export default PasswordSection;
