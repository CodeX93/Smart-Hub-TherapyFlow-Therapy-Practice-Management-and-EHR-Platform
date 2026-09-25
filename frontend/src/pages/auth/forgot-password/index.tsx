import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from "../../../schemas/reset.schema";
import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthForgotPasswordForm from "@/components/auth/AuthForgotPasswordForm";
import OrgPickerModal from "@/components/auth/OrgPickerModal";
import {
  usePortalForgotPasswordMutation,
  useResolvePortalTenantMutation,
} from "@/store/api/portalAuthApi";
import type { ResolveTenantOrganisation } from "@/store/api/authApi";
import { getApiErrorMessage, getAuthErrorMessage } from "@/utils/apiError";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

const ResetPassword = () => {
  const navigate = useNavigate();
  const [resolvePortalTenant, { isLoading: isResolving }] =
    useResolvePortalTenantMutation();
  const [forgotPassword, { isLoading: isForgotLoading }] =
    usePortalForgotPasswordMutation();

  const [tenantOptions, setTenantOptions] = useState<ResolveTenantOrganisation[]>(
    [],
  );
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);
  const [pendingEmail, setPendingEmail] = useState("");
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

    const email = values.email.trim();
    setCustomError(null);

    try {
      const resolved = await resolvePortalTenant({ email }).unwrap();
      const organisations = resolved.organisations ?? [];
      const orgCount =
        resolved.count > 0 ? resolved.count : organisations.length;
      setPendingEmail(email);
      setTenantOptions(organisations);

      if (organisations.length === 0) {
        if (
          resolved.orgSlug ||
          resolved.organisationId ||
          resolved.organisationName
        ) {
          const fallbackOrg: ResolveTenantOrganisation = {
            organisationId: resolved.organisationId ?? 0,
            name: resolved.organisationName ?? resolved.orgSlug ?? "",
            slug: resolved.orgSlug ?? "",
            subdomain: "",
            status: "ACTIVE",
          };
          await handleTenantSelect(fallbackOrg, email);
          return;
        }
        // Anti-enumeration: same success path when no client orgs match.
        navigate("/auth/check-email");
        return;
      }

      if (orgCount === 1) {
        await handleTenantSelect(organisations[0], email);
        return;
      }

      setIsTenantModalOpen(true);
    } catch (err) {
      // Anti-enumeration: do not reveal whether the email has a portal account.
      console.error("Client forgot password resolution failed:", err);
      navigate("/auth/check-email");
    }
  };

  async function handleTenantSelect(
    organisation: ResolveTenantOrganisation,
    email?: string,
  ) {
    const resolvedEmail = email ?? pendingEmail;
    if (!resolvedEmail) {
      return;
    }

    setCustomError(null);

    try {
      await forgotPassword({
        email: resolvedEmail,
        orgSlug: organisation.slug || undefined,
        orgId:
          organisation.organisationId && organisation.organisationId > 0
            ? String(organisation.organisationId)
            : undefined,
      }).unwrap();

      setIsTenantModalOpen(false);
      navigate("/auth/check-email");
    } catch (err) {
      setCustomError(getAuthErrorMessage(err) || getApiErrorMessage(err));
      console.error("Client forgot password failed:", err);
    }
  }

  return (
    <AuthLayoutWrapper title="Welcome back to SmartHub 👋" showRightBar={true}>
      <AuthForgotPasswordForm
        form={form}
        onSubmit={onSubmit}
        backToLoginPath="/auth/login"
        submitLabel={
          isResolving
            ? "Finding organizations…"
            : isForgotLoading
              ? "Sending..."
              : "Reset Password"
        }
        errorMessage={customError ?? undefined}
      />

      <OrgPickerModal
        open={isTenantModalOpen}
        organisations={tenantOptions}
        onSelect={handleTenantSelect}
        onClose={() => setIsTenantModalOpen(false)}
        title="Select your clinic"
        description="This email is linked to more than one client portal. Choose the clinic you want to reset the password for."
        isLoading={isForgotLoading}
        lookupIdentifier={pendingEmail}
      />
    </AuthLayoutWrapper>
  );
};

export default ResetPassword;
