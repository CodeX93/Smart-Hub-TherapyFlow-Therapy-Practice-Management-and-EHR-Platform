import { useEffect, useMemo, useRef, useState, type HTMLAttributes, type HTMLInputTypeAttribute } from "react";
import { Upload } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import { cn } from "@/lib/utils";
import {
  useDisableSuperAdmin2faMutation,
  useEnableSuperAdmin2faMutation,
  useGetSuperAdmin2faStatusQuery,
  useGetSuperAdminMeQuery,
  useGetSuperAdminMeProfileQuery,
  useUploadSuperAdminProfilePictureMutation,
  useUpdateSuperAdminMeMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { useNavigate } from "react-router-dom";

const PROFILE_FIELD_LIMITS = {
  fullName: 200,
  email: 254,
  phone: 20,
} as const;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const PHONE_PATTERN = /^\+?\d+$/;

function sanitizeFullName(value: string): string {
  return value.slice(0, PROFILE_FIELD_LIMITS.fullName);
}

function sanitizeEmail(value: string): string {
  return value.replace(/\s/g, "").slice(0, PROFILE_FIELD_LIMITS.email);
}

function sanitizePhone(value: string): string {
  let cleaned = "";
  for (const char of value) {
    if (char === "+" && cleaned.length === 0) {
      cleaned += "+";
    } else if (/\d/.test(char)) {
      cleaned += char;
    }
  }
  return cleaned.slice(0, PROFILE_FIELD_LIMITS.phone);
}

type ProfileFieldErrors = Partial<
  Record<"fullName" | "emailAddress" | "phoneNumber", string>
>;

function validateProfileFields(values: {
  fullName: string;
  emailAddress: string;
  phoneNumber: string;
}): ProfileFieldErrors {
  const errors: ProfileFieldErrors = {};
  const fullName = values.fullName.trim();
  const emailAddress = values.emailAddress.trim();
  const phoneNumber = values.phoneNumber.trim();

  if (!fullName) {
    errors.fullName = "Full name is required.";
  } else if (fullName.length > PROFILE_FIELD_LIMITS.fullName) {
    errors.fullName = `Full name must be ${PROFILE_FIELD_LIMITS.fullName} characters or less.`;
  }

  if (!emailAddress) {
    errors.emailAddress = "Email address is required.";
  } else if (emailAddress.length > PROFILE_FIELD_LIMITS.email) {
    errors.emailAddress = `Email must be ${PROFILE_FIELD_LIMITS.email} characters or less.`;
  } else if (!EMAIL_PATTERN.test(emailAddress)) {
    errors.emailAddress = "Enter a valid email address.";
  }

  if (phoneNumber) {
    if (phoneNumber.length > PROFILE_FIELD_LIMITS.phone) {
      errors.phoneNumber = `Phone number must be ${PROFILE_FIELD_LIMITS.phone} characters or less.`;
    } else if (!PHONE_PATTERN.test(phoneNumber)) {
      errors.phoneNumber = "Use an optional leading + followed by digits only.";
    }
  }

  return errors;
}

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

function getFieldShellClassName(hasError = false): string {
  return cn(
    "relative h-[3.75rem] overflow-hidden rounded-[1rem] border bg-white",
    hasError ? "border-[#f3a4a4]" : "border-[#dce5ee]",
  );
}

function getFloatingInputPaddingClassName(hasValue: boolean): string {
  return hasValue ? "pb-2 pt-7" : "py-0";
}

function FieldBlock(props: {
  label: string;
  value: string;
  onChange?: (value: string) => void;
  maxLength?: number;
  type?: HTMLInputTypeAttribute;
  inputMode?: HTMLAttributes<HTMLInputElement>["inputMode"];
  autoComplete?: string;
  error?: string | null;
  hint?: string;
  disabled?: boolean;
}) {
  const hasError = Boolean(props.error);
  const isDisabled = Boolean(props.disabled);

  return (
    <div>
      <div
        className={cn(
          getFieldShellClassName(hasError),
          isDisabled && "bg-[#f7fafc]",
        )}
      >
        {props.value ? <FieldLabel>{props.label}</FieldLabel> : null}
        <Input
          value={props.value}
          onChange={
            isDisabled
              ? undefined
              : function (event) {
                  props.onChange?.(event.target.value);
                }
          }
          placeholder={props.label}
          type={props.type}
          inputMode={props.inputMode}
          autoComplete={props.autoComplete}
          maxLength={props.maxLength}
          disabled={isDisabled}
          readOnly={isDisabled}
          aria-invalid={hasError}
          className={cn(
            getInputClassName(),
            getFloatingInputPaddingClassName(Boolean(props.value)),
            isDisabled && "cursor-not-allowed text-[#667483]",
          )}
        />
      </div>
      {props.error ? (
        <p className="mt-1.5 text-[0.6875rem] leading-4 text-(--status-denied)">
          {props.error}
        </p>
      ) : props.hint ? (
        <p className="mt-1.5 text-[0.6875rem] leading-4 text-[#8f9aa6]">
          {props.hint}
        </p>
      ) : null}
    </div>
  );
}

function SecurityRow(props: {
  title: string;
  description: string;
  buttonLabel: string;
  onButtonClick?: () => void;
  isBusy?: boolean;
  disabled?: boolean;
}) {
  return (
    <div className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
          {props.title}
        </div>
        <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#8f9aa6]">
          {props.description}
        </div>
      </div>

      <Button
        variant="secondary"
        size="md"
        onClick={props.onButtonClick}
        disabled={props.disabled || props.isBusy}
      >
        {props.buttonLabel}
      </Button>
    </div>
  );
}

function MyProfilePage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [fullName, setFullName] = useState("");
  const [emailAddress, setEmailAddress] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [fieldErrors, setFieldErrors] = useState<ProfileFieldErrors>({});
  const [saveErrorMessage, setSaveErrorMessage] = useState<string | null>(null);
  const [saveSuccessMessage, setSaveSuccessMessage] = useState<string | null>(null);
  const [twoFaErrorMessage, setTwoFaErrorMessage] = useState<string | null>(null);
  const [pictureErrorMessage, setPictureErrorMessage] = useState<string | null>(null);
  const [pictureSuccessMessage, setPictureSuccessMessage] = useState<string | null>(null);
  const [profilePictureUrl, setProfilePictureUrl] = useState("");
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const {
    data: meData,
    isLoading: isLoadingProfile,
    isError: isProfileError,
    error: profileError,
    refetch: refetchProfile,
  } = useGetSuperAdminMeQuery();
  const [updateMe, { isLoading: isSavingProfile }] = useUpdateSuperAdminMeMutation();
  const {
    data: meProfileData,
    isLoading: isLoadingMeProfile,
    isError: isMeProfileError,
    error: meProfileError,
    refetch: refetchMeProfile,
  } = useGetSuperAdminMeProfileQuery();
  const [uploadProfilePicture, { isLoading: isUploadingProfilePicture }] =
    useUploadSuperAdminProfilePictureMutation();

  const {
    data: twoFaStatusData,
    isLoading: isLoading2faStatus,
    isError: is2faStatusError,
    error: twoFaStatusError,
    refetch: refetch2faStatus,
  } = useGetSuperAdmin2faStatusQuery();
  const [enable2fa, { isLoading: isEnabling2fa }] = useEnableSuperAdmin2faMutation();
  const [disable2fa, { isLoading: isDisabling2fa }] = useDisableSuperAdmin2faMutation();

  useEffect(() => {
    if (!meData) return;

    setUsername((meData.username ?? "").trim());
    setFullName(sanitizeFullName((meData.fullName ?? "").trim()));
    setEmailAddress(sanitizeEmail((meData.email ?? "").trim()));
    setPhoneNumber(sanitizePhone((meData.phone ?? "").trim()));
    setProfilePictureUrl(
      (meProfileData?.profilePicture ?? meData.profilePicture ?? "").trim(),
    );
  }, [meData, meProfileData]);

  const is2faBusy = isEnabling2fa || isDisabling2fa;
  const is2faSupported = twoFaStatusData?.supported ?? false;
  const is2faEnabled = twoFaStatusData?.enabled ?? false;
  const isSavingAnyProfile = isSavingProfile;
  const isLoadingAnyProfile = isLoadingProfile || isLoadingMeProfile;
  const isProfileErrorAny = isProfileError || isMeProfileError;
  const profileErrorAny = isProfileError ? profileError : meProfileError;
  const initials = useMemo(() => {
    const parts = fullName
      .split(" ")
      .filter(Boolean)
      .slice(0, 2);
    if (!parts.length) return "SA";
    return parts.map((part) => part[0]?.toUpperCase() ?? "").join("");
  }, [fullName]);

  async function handleSaveProfile() {
    if (!meData) return;

    const validationErrors = validateProfileFields({
      fullName,
      emailAddress,
      phoneNumber,
    });
    setFieldErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    try {
      setSaveErrorMessage(null);
      setSaveSuccessMessage(null);
      await updateMe({
        username: meData.username,
        fullName: fullName.trim(),
        email: emailAddress.trim(),
        phone: phoneNumber.trim(),
        active: meData.active,
        roles: meData.roles ?? [],
      }).unwrap();

      setSaveSuccessMessage("Profile updated successfully.");
      await refetchProfile();
    } catch (error) {
      setSaveErrorMessage(getApiErrorMessage(error));
    }
  }

  function handleFullNameChange(value: string) {
    setFieldErrors((previous) => ({ ...previous, fullName: undefined }));
    setFullName(sanitizeFullName(value));
  }

  function handleEmailChange(value: string) {
    setFieldErrors((previous) => ({ ...previous, emailAddress: undefined }));
    setEmailAddress(sanitizeEmail(value));
  }

  function handlePhoneChange(value: string) {
    setFieldErrors((previous) => ({ ...previous, phoneNumber: undefined }));
    setPhoneNumber(sanitizePhone(value));
  }

  async function handleProfilePictureChange(
    event: React.ChangeEvent<HTMLInputElement>
  ) {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      setPictureErrorMessage(null);
      setPictureSuccessMessage(null);
      const formData = new FormData();
      formData.append("file", file);
      const response = await uploadProfilePicture(formData).unwrap();
      await refetchMeProfile();
      const apiMessage = response?.message?.trim();
      if (apiMessage) {
        if (response?.supported === false) {
          setPictureErrorMessage(apiMessage);
        } else {
          setPictureSuccessMessage(apiMessage);
        }
      } else {
        setPictureSuccessMessage("Profile picture updated successfully.");
      }
    } catch (error) {
      setPictureErrorMessage(getApiErrorMessage(error));
    } finally {
      event.target.value = "";
    }
  }

  async function handle2faToggle() {
    try {
      setTwoFaErrorMessage(null);
      if (is2faEnabled) {
        await disable2fa().unwrap();
      } else {
        await enable2fa().unwrap();
      }
      await refetch2faStatus();
    } catch (error) {
      setTwoFaErrorMessage(getApiErrorMessage(error));
    }
  }

  return (
    <SuperAdminPageShell
      title="My Profile"
      description="Manage your account settings, security, and preferences."
    >
      <section className={getSectionClassName()}>
        <div className="px-6 py-6">
          <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
            My Profile
          </div>
        </div>

        <div className="border-t border-[#edf2f7] px-6 py-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <Avatar className="h-[4.875rem] w-[4.875rem] border border-[#d9e2ea] bg-[#eceff3]">
            <AvatarImage src={profilePictureUrl} />
            <AvatarFallback className="bg-[#eceff3] text-[1rem] font-semibold leading-none text-[#2b3946]">
              {initials}
            </AvatarFallback>
          </Avatar>

          <div>
            <Button
              variant="secondary"
              size="md"
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploadingProfilePicture}
              loading={isUploadingProfilePicture}
              loadingLabel="Uploading..."
            >
              <Upload size={16} aria-hidden="true" />
              Upload new picture
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/jpg,image/gif"
              className="hidden"
              onChange={handleProfilePictureChange}
            />
            <div className="mt-3 text-[0.75rem] leading-[1.375rem] text-[#8f9aa6]">
              JPG, GIF or PNG. Max size of 800K
            </div>
            {pictureErrorMessage ? (
              <div className="mt-2 rounded-[0.625rem] border border-[#f3d4d4] bg-[#fff5f5] px-3 py-2 text-xs text-(--status-denied)">
                {pictureErrorMessage}
              </div>
            ) : null}
            {pictureSuccessMessage ? (
              <div className="mt-2 rounded-[0.625rem] border border-[#d8ead7] bg-[#f4fbf3] px-3 py-2 text-xs text-[#166534]">
                {pictureSuccessMessage}
              </div>
            ) : null}
          </div>
          </div>
        </div>
      </section>

      <section className={getSectionClassName()}>
        <div className="px-6 py-6">
          <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
            Personal Information
          </div>
        </div>

        <div className="border-t border-[#edf2f7] px-6 py-6">
          {isLoadingAnyProfile ? (
            <div className="mb-4 rounded-[0.75rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
              Loading profile...
            </div>
          ) : null}
          {isProfileErrorAny ? (
            <div className="mb-4 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {getApiErrorMessage(profileErrorAny)}
            </div>
          ) : null}
          {saveErrorMessage ? (
            <div className="mb-4 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {saveErrorMessage}
            </div>
          ) : null}
          {saveSuccessMessage ? (
            <div className="mb-4 rounded-[0.75rem] border border-[#d8ead7] bg-[#f4fbf3] px-4 py-3 text-sm text-[#166534]">
              {saveSuccessMessage}
            </div>
          ) : null}
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            <FieldBlock
              label="Username"
              value={username}
              disabled
              hint="Username cannot be changed"
            />
            <FieldBlock
              label="Full name"
              value={fullName}
              onChange={handleFullNameChange}
              maxLength={PROFILE_FIELD_LIMITS.fullName}
              autoComplete="name"
              error={fieldErrors.fullName}
              hint={`${fullName.length}/${PROFILE_FIELD_LIMITS.fullName}`}
            />
            <FieldBlock
              label="Email address"
              value={emailAddress}
              onChange={handleEmailChange}
              type="email"
              inputMode="email"
              autoComplete="email"
              maxLength={PROFILE_FIELD_LIMITS.email}
              error={fieldErrors.emailAddress}
              hint={`${emailAddress.length}/${PROFILE_FIELD_LIMITS.email}`}
            />
            <FieldBlock
              label="Phone number"
              value={phoneNumber}
              onChange={handlePhoneChange}
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              maxLength={PROFILE_FIELD_LIMITS.phone}
              error={fieldErrors.phoneNumber}
              hint={`${phoneNumber.length}/${PROFILE_FIELD_LIMITS.phone} • Optional + prefix`}
            />
          </div>

          <div className="mt-7 flex justify-end">
            <Button
              variant="primary"
              size="md"
              onClick={handleSaveProfile}
              disabled={isLoadingAnyProfile || isSavingAnyProfile}
              loading={isSavingAnyProfile}
              loadingLabel="Saving..."
            >
              Save Changes
            </Button>
          </div>
        </div>
      </section>

      <section className={getSectionClassName()}>
        <div className="px-6 py-6">
          <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
            Security
          </div>
          <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#8f9aa6]">
            Ensure your account is secure by updating your password and enabling
            2FA.
          </div>
        </div>

        <div className="divide-y divide-[#edf2f7] border-t border-[#edf2f7]">
          {is2faStatusError ? (
            <div className="mx-6 mt-6 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {getApiErrorMessage(twoFaStatusError)}
            </div>
          ) : null}
          {twoFaErrorMessage ? (
            <div className="mx-6 mt-6 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {twoFaErrorMessage}
            </div>
          ) : null}
          {twoFaStatusData?.message ? (
            <div className="mx-6 mt-6 rounded-[0.75rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
              {twoFaStatusData.message}
            </div>
          ) : null}
          <SecurityRow
            title="Two-Factor Authentication"
            description="Add an extra layer of security to your account by requiring a code from your authenticator app."
            buttonLabel={
              isLoading2faStatus
                ? "Loading..."
                : is2faEnabled
                ? "Disable 2FA"
                : "Enable 2FA"
            }
            onButtonClick={handle2faToggle}
            isBusy={is2faBusy}
            disabled={isLoading2faStatus || !is2faSupported}
          />
          <SecurityRow
            title="Change Password"
            description="Update your password to keep your account secure. We recommend using a strong password manager."
            buttonLabel="Update Password"
            onButtonClick={() => navigate("/super-admin/my-profile/change-password")}
          />
        </div>
      </section>
    </SuperAdminPageShell>
  );
}

export default MyProfilePage;
