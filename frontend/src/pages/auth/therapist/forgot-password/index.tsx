import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from "../../../../schemas/reset.schema";
import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthForgotPasswordForm from "@/components/auth/AuthForgotPasswordForm";
import OrgPickerModal from "@/components/auth/OrgPickerModal";
import {
  useForgotPasswordStaffMutation,
  useResolveTenantMutation,
  type ResolveTenantOrganisation,
} from "@/store/api/authApi";
import { getApiErrorMessage, getAuthErrorMessage } from "@/utils/apiError";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

const TherapistForgotPassword = () => {
  const navigate = useNavigate();
  const [resolveTenant, { isLoading: isResolving, isError: isResolveError, error: resolveError }] =
    useResolveTenantMutation();
  const [forgotPassword, { isLoading: isForgotLoading, isError: isForgotError, error: forgotError }] =
    useForgotPasswordStaffMutation();

  const [tenantOptions, setTenantOptions] = useState<ResolveTenantOrganisation[]>([]);
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);
  const [pendingEmail, setPendingEmail] = useState<string>("");
  const [customError, setCustomError] = useState<string | null>(null);

  const form = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      email: "",
    },
  });

  const onSubmit = async (values: ResetPasswordFormValues) => {
    if (!values.email) {
      return;
    }

    setCustomError(null);

    try {
      const resolved = await resolveTenant({ email: values.email }).unwrap();
      const organisations = resolved.organisations ?? [];
      const orgCount = resolved.count > 0 ? resolved.count : organisations.length;
      setPendingEmail(values.email);
      setTenantOptions(organisations);

      if (organisations.length === 0) {
        navigate("/auth/staff/check-email");
        return;
      }

      if (orgCount === 1) {
        await handleTenantSelect(organisations[0], values.email);
        return;
      }

      setIsTenantModalOpen(true);
    } catch (err) {
      console.error("Staff forgot password resolution failed:", err);
    }
  };

  async function handleTenantSelect(
    organisation: ResolveTenantOrganisation,
    email?: string,
  ) {
    const resolvedEmail = email ?? pendingEmail;
    if (!resolvedEmail) return;

    setCustomError(null);

    try {
      const result = await forgotPassword({
        email: resolvedEmail,
        orgSlug: organisation.slug || undefined,
        orgId: organisation.organisationId
          ? String(organisation.organisationId)
          : undefined,
      }).unwrap();

      if (result.needsTenantSelection) {
        setIsTenantModalOpen(true);
        return;
      }

      setIsTenantModalOpen(false);
      navigate("/auth/staff/check-email");
    } catch (err) {
      setCustomError(getAuthErrorMessage(err));
      console.error("Staff forgot password failed:", err);
    }
  }

  return (
    <AuthLayoutWrapper
      title="Welcome back to SmartHub"
      showRightBar={false}
      className="md:min-h-auto"
    >
      <AuthForgotPasswordForm
        form={form}
        onSubmit={onSubmit}
        backToLoginPath="/auth/staff/login"
        submitLabel={
          isResolving ? "Resolving..." : isForgotLoading ? "Sending..." : "Reset Password"
        }
        errorMessage={
          customError
            ? customError
            : isResolveError
              ? getApiErrorMessage(resolveError)
              : isForgotError
                ? getAuthErrorMessage(forgotError)
                : undefined
        }
      />

      <OrgPickerModal
        open={isTenantModalOpen}
        organisations={tenantOptions}
        onSelect={handleTenantSelect}
        onClose={() => setIsTenantModalOpen(false)}
        title="Select Organization"
        description="Choose the tenant you want to reset password for."
        isLoading={isForgotLoading}
        lookupIdentifier={pendingEmail}
      />
    </AuthLayoutWrapper>
  );
};

export default TherapistForgotPassword;
