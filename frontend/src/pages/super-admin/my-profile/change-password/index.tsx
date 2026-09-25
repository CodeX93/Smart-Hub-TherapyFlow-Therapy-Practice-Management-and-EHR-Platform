import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import { cn } from "@/lib/utils";
import { useChangeSuperAdminPasswordMutation } from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { useNavigate } from "react-router-dom";

function getSectionClassName(): string {
  return cn(
    "overflow-hidden rounded-[1rem] border border-[#e7edf3] bg-white",
    "shadow-[0_1px_2px_rgba(15,23,42,0.04)]"
  );
}

function getInputClassName(): string {
  return cn(
    "h-full rounded-[1rem] border-0 bg-transparent px-4 shadow-none",
    "text-[0.875rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function FieldLabel(props: { children: React.ReactNode }) {
  return (
    <div className="pointer-events-none absolute left-4 top-3 text-[0.6875rem] font-medium leading-4 text-[#8a96a3]">
      {props.children}
    </div>
  );
}

function getFieldShellClassName(hasError?: boolean): string {
  return cn(
    "relative h-[3.75rem] overflow-hidden rounded-[1rem] border bg-white",
    hasError ? "border-[#ef4444]" : "border-[#dce5ee]"
  );
}

function getFloatingInputPaddingClassName(hasValue: boolean): string {
  return hasValue ? "pb-2 pt-7" : "py-0";
}

function getPasswordValidationError(
  currentPassword: string,
  newPassword: string,
  confirmPassword: string
): string | null {
  if (!currentPassword.trim()) {
    return "Current password is required.";
  }
  if (!newPassword.trim()) {
    return "New password is required.";
  }
  if (newPassword.trim().length < 8) {
    return "New password must be at least 8 characters.";
  }
  if (newPassword === currentPassword) {
    return "New password must be different from current password.";
  }
  if (confirmPassword !== newPassword) {
    return "Confirm password must match new password.";
  }
  return null;
}

function SuperAdminChangePasswordPage() {
  const navigate = useNavigate();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const [changePassword, { isLoading }] = useChangeSuperAdminPasswordMutation();

  async function handleSave() {
    setFormError(null);
    setSuccessMessage(null);

    const validationError = getPasswordValidationError(
      currentPassword,
      newPassword,
      confirmPassword
    );

    if (validationError) {
      setFormError(validationError);
      return;
    }

    try {
      const response = await changePassword({
        currentPassword,
        newPassword,
      }).unwrap();

      setSuccessMessage(response.message || "Password updated successfully.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (error) {
      setFormError(getApiErrorMessage(error));
    }
  }

  return (
    <SuperAdminPageShell
      title="Change Password"
      description="Update your password to keep your account secure."
    >
      <section className={getSectionClassName()}>
        <div className="px-6 py-6">
          <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
            Update Password
          </div>
        </div>

        <div className="border-t border-[#edf2f7] px-6 py-6">
          {formError ? (
            <div className="mb-4 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {formError}
            </div>
          ) : null}

          {successMessage ? (
            <div className="mb-4 rounded-[0.75rem] border border-[#d8ead7] bg-[#f4fbf3] px-4 py-3 text-sm text-[#166534]">
              {successMessage}
            </div>
          ) : null}

          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            <div className={getFieldShellClassName()}>
              {currentPassword ? <FieldLabel>Current Password</FieldLabel> : null}
              <Input
                type={showCurrentPassword ? "text" : "password"}
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                placeholder="Current Password"
                className={cn(
                  getInputClassName().replace("px-4", "pl-4 pr-12"),
                  getFloatingInputPaddingClassName(Boolean(currentPassword))
                )}
              />
              <button
                type="button"
                onClick={() => setShowCurrentPassword((prev) => !prev)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#8a96a3] hover:text-[#435564] transition-colors cursor-pointer"
                aria-label={showCurrentPassword ? "Hide current password" : "Show current password"}
              >
                {showCurrentPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            <div className={getFieldShellClassName()}>
              {newPassword ? <FieldLabel>New Password</FieldLabel> : null}
              <Input
                type={showNewPassword ? "text" : "password"}
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                placeholder="New Password"
                className={cn(
                  getInputClassName().replace("px-4", "pl-4 pr-12"),
                  getFloatingInputPaddingClassName(Boolean(newPassword))
                )}
              />
              <button
                type="button"
                onClick={() => setShowNewPassword((prev) => !prev)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#8a96a3] hover:text-[#435564] transition-colors cursor-pointer"
                aria-label={showNewPassword ? "Hide new password" : "Show new password"}
              >
                {showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            <div className={getFieldShellClassName()}>
              {confirmPassword ? <FieldLabel>Confirm New Password</FieldLabel> : null}
              <Input
                type={showConfirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="Confirm New Password"
                className={cn(
                  getInputClassName().replace("px-4", "pl-4 pr-12"),
                  getFloatingInputPaddingClassName(Boolean(confirmPassword))
                )}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#8a96a3] hover:text-[#435564] transition-colors cursor-pointer"
                aria-label={showConfirmPassword ? "Hide confirm password" : "Show confirm password"}
              >
                {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <div className="mt-7 flex justify-end gap-3">
            <Button
              variant="secondary"
              size="md"
              onClick={() => navigate("/super-admin/my-profile")}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={handleSave}
              disabled={isLoading}
              loading={isLoading}
              loadingLabel="Updating..."
            >
              Update Password
            </Button>
          </div>
        </div>
      </section>
    </SuperAdminPageShell>
  );
}

export default SuperAdminChangePasswordPage;
