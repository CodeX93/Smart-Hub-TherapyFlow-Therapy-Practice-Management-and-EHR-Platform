import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useParams, useSearchParams, useNavigate } from "react-router-dom";
import { useState } from "react";
import {
  setNewPasswordSchema,
  type SetNewPasswordFormValues,
} from "@/schemas/set-new-password.schema";
import TherapistSetNewPasswordForm from "@/pages/auth/therapist/set-new-password/TherapistSetNewPasswordForm";
import { usePortalActivateMutation } from "@/store/api/portalAuthApi";
import {
  getPortalActivationErrorMessage,
  resolvePortalTenantContext,
} from "@/utils/portalTenantContext";
import { setAuthSession } from "@/utils/authStorage";
import { useAppDispatch } from "@/store/hooks";
import { setSession } from "@/store/authSlice";
import { getRedirectPathByRole } from "@/utils/redirectPathByRole";
import Toast from "@/components/shared/Toast";

const PortalActivatePage = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { token = "" } = useParams<{ token: string }>();
  const [searchParams] = useSearchParams();
  const tenantContext = resolvePortalTenantContext(searchParams);
  const [activateAccount, { isLoading }] = usePortalActivateMutation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const form = useForm<SetNewPasswordFormValues>({
    resolver: zodResolver(setNewPasswordSchema),
    defaultValues: {
      password: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (values: SetNewPasswordFormValues) => {
    if (!token.trim()) {
      return;
    }

    setToastMessage(null);

    try {
      const response = await activateAccount({
        token: token.trim(),
        password: values.password,
        orgSlug: tenantContext.orgSlug,
        tenantSubdomain: tenantContext.tenantSubdomain,
      }).unwrap();

      const session = {
        accessToken: response.accessToken,
        role: "user" as const,
        tenantSubdomain: tenantContext.tenantSubdomain,
        tenantSlug: tenantContext.orgSlug,
        client: response.client,
      };

      setAuthSession(session);
      dispatch(setSession(session));
      navigate(getRedirectPathByRole("user"), { replace: true });
    } catch (activateError) {
      setToastMessage(getPortalActivationErrorMessage(activateError));
    }
  };

  if (!token.trim()) {
    return (
      <div className="bg-white max-w-134.75 w-full rounded-xl border-none shadow-xs shadow-(--shadow)">
        <div className="px-4 py-8 md:px-12 md:py-16">
          <h2 className="text-center text-xl font-semibold text-(--text-primary-dark) md:text-2xl">
            Invalid activation link
          </h2>
          <p className="mt-3 text-center text-sm leading-6 text-(--text-neutral-600)">
            This activation link is missing a token. Open the link from your
            email or ask your clinic to resend activation.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white max-w-134.75 w-full rounded-xl border-none shadow-xs shadow-(--shadow)">
      <div className="flex flex-col items-center justify-between gap-8 px-4 py-8 md:px-12 md:py-16">
        <div className="flex w-full flex-col">
          <h2 className="mb-2 text-center text-xl font-semibold text-(--text-primary-dark) md:text-2xl">
            Activate Your Account
          </h2>

          <p className="mb-6 text-center text-sm leading-6 text-(--text-neutral-600)">
            Set a secure password to activate your client portal access.
          </p>

          <TherapistSetNewPasswordForm
            form={form}
            onSubmit={onSubmit}
            buttonText={isLoading ? "Activating..." : "Activate account"}
          />
        </div>
      </div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type="error"
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

export default PortalActivatePage;
