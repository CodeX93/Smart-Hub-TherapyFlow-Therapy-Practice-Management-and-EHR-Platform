import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  setNewPasswordSchema,
  type SetNewPasswordFormValues,
} from "../../../../schemas/set-new-password.schema";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";
import TherapistSetNewPasswordForm from "../set-new-password/TherapistSetNewPasswordForm";
import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import MfaChallengePanel, {
  type MfaFlow,
} from "@/components/auth/MfaChallengePanel";
import {
  authApi,
  useChangePasswordStaffMutation,
  type ResolveTenantOrganisation,
  type StaffLoginResponse,
} from "@/store/api/authApi";
import { getApiErrorMessage, getAuthErrorMessage } from "@/utils/apiError";
import { setAuthSession } from "@/utils/authStorage";
import { setDeviceTrustToken } from "@/utils/deviceTrustStorage";
import { mapApiRolesToAppRole } from "@/utils/roleMapper";
import { useAppDispatch } from "@/store/hooks";
import { setAuthContext, setSession } from "@/store/authSlice";
import { getRedirectPathByRole } from "@/utils/redirectPathByRole";
import { getStaffLandingPath, normalizeApiRoles } from "@/utils/staffPermissions";

type ActivateAccountLocationState = {
  changePasswordToken?: string;
  email?: string;
  message?: string;
  organisationName?: string;
  organisation?: ResolveTenantOrganisation;
  staySignedIn?: boolean;
  loginUsername?: string;
};

const TherapistActivateYourAccount = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const state = (location.state as ActivateAccountLocationState | null) ?? {};
  const [changePassword, { isLoading, isError, error }] =
    useChangePasswordStaffMutation();
  const [mfaFlow, setMfaFlow] = useState<MfaFlow | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isCompletingLogin, setIsCompletingLogin] = useState(false);

  const form = useForm<SetNewPasswordFormValues>({
    resolver: zodResolver(setNewPasswordSchema),
    defaultValues: {
      password: "",
      confirmPassword: "",
    },
  });

  if (!state.changePasswordToken) {
    return <Navigate to="/auth/staff/login" replace />;
  }

  async function completeStaffSession(resolved: StaffLoginResponse) {
    const organisation = state.organisation;
    const loginUsername =
      state.loginUsername?.trim() ||
      resolved.user?.email ||
      resolved.user?.username ||
      state.email ||
      "";

    if (resolved.deviceTrustToken && loginUsername) {
      setDeviceTrustToken(loginUsername, resolved.deviceTrustToken);
    }

    if (!resolved.accessToken) {
      setSubmitError(
        resolved.message || "Login could not be completed. Please try again.",
      );
      return;
    }

    const preliminaryRole = mapApiRolesToAppRole(resolved.roles);
    if (preliminaryRole === "user") {
      setSubmitError("Client accounts must sign in through the Client Portal.");
      return;
    }

    const tenantSchema =
      resolved.tenantSchema ??
      (organisation?.slug ? `tenant_${organisation.slug}` : undefined);

    if (
      (preliminaryRole === "admin" ||
        preliminaryRole === "therapist" ||
        preliminaryRole === "staff") &&
      (!tenantSchema || tenantSchema === "public")
    ) {
      setSubmitError(
        "Login could not be completed for the selected organization. Please try again.",
      );
      return;
    }

    const session = {
      accessToken: resolved.accessToken,
      role: preliminaryRole,
      tenantSlug: organisation?.slug || resolved.organisationSlug || undefined,
      tenantSchema,
      organisationId:
        resolved.organisationId ?? organisation?.organisationId,
      organisationSlug:
        resolved.organisationSlug ?? organisation?.slug ?? undefined,
      user: resolved.user,
      apiRoles: (resolved.roles ?? []).map((entry) => entry.toUpperCase()),
      permissions: resolved.permissions ?? [],
      authBootstrapResolved: false,
    } as const;

    setAuthSession(session);
    dispatch(setSession(session));

    const meResult = await dispatch(
      authApi.endpoints.getAuthMe.initiate(undefined, { forceRefetch: true }),
    );

    if ("error" in meResult && meResult.error) {
      setSubmitError(getAuthErrorMessage(meResult.error));
      return;
    }

    const me = meResult.data;
    if (!me) {
      setSubmitError("Could not verify your account. Please try again.");
      return;
    }

    const role = mapApiRolesToAppRole(me.roles);
    if (role === "user") {
      setSubmitError("Client accounts must sign in through the Client Portal.");
      return;
    }

    if (
      (role === "admin" || role === "therapist" || role === "staff") &&
      (!me.tenantSchema || me.tenantSchema === "public")
    ) {
      setSubmitError(
        "Login could not be completed for the selected organization. Please try again.",
      );
      return;
    }

    dispatch(
      setAuthContext({
        apiRoles: normalizeApiRoles(me.roles),
        permissions: me.permissions ?? me.authorities ?? [],
        tenantSchema: me.tenantSchema,
        organisationId: me.organisationId,
        organisationSlug: me.organisationSlug,
        user: me.user,
        role,
      }),
    );

    const landingPath =
      role === "staff"
        ? getStaffLandingPath(
            me.permissions ?? me.authorities ?? [],
            normalizeApiRoles(me.roles),
          )
        : getRedirectPathByRole(role);
    navigate(landingPath, { replace: true });
  }

  const onSubmit = async (values: SetNewPasswordFormValues) => {
    setSubmitError(null);
    try {
      const resolved = await changePassword({
        changePasswordToken: state.changePasswordToken,
        newPassword: values.password,
      }).unwrap();

      if (
        resolved.mfaChallengeToken &&
        (resolved.mfaRequired || resolved.mfaEnrollmentRequired)
      ) {
        const accountLabel =
          state.loginUsername?.trim() || state.email || "your account";
        setMfaFlow({
          mode: resolved.mfaEnrollmentRequired ? "enroll" : "verify",
          challengeToken: resolved.mfaChallengeToken,
          accountLabel,
          ...(resolved.mfaEnrollmentRequired
            ? {
                smsAvailable: resolved.mfaSmsAvailable !== false,
                emailAvailable: resolved.mfaEmailAvailable !== false,
              }
            : {
                method: resolved.mfaMethod,
                maskedDestination: resolved.mfaMaskedDestination,
                enrolledMethods: resolved.mfaMethods,
              }),
        });
        return;
      }

      if (resolved.accessToken) {
        setIsCompletingLogin(true);
        try {
          await completeStaffSession(resolved);
        } finally {
          setIsCompletingLogin(false);
        }
        return;
      }

      // Fallback: unusual response — send user to sign-in with a clear message.
      navigate("/auth/staff/login", {
        replace: true,
        state: {
          mfaNotice:
            resolved.message ||
            "Your password has been set. Sign in with your new password to continue.",
        },
      });
    } catch (submitError) {
      console.error("Activate account failed:", submitError);
      setSubmitError(getApiErrorMessage(submitError));
    }
  };

  if (mfaFlow) {
    return (
      <AuthLayoutWrapper title="Protect your account" showRightBar={false}>
        <MfaChallengePanel
          flow={mfaFlow}
          staySignedIn={Boolean(state.staySignedIn)}
          onAuthenticated={async (response) => {
            if (!("user" in response)) return;
            setIsCompletingLogin(true);
            try {
              await completeStaffSession(response);
            } finally {
              setIsCompletingLogin(false);
            }
          }}
          onEnrollmentComplete={() => {
            navigate("/auth/staff/login", {
              replace: true,
              state: {
                mfaNotice:
                  "MFA is set up. Sign in with your new password to continue.",
              },
            });
          }}
          onCancel={() => {
            setMfaFlow(null);
          }}
        />
        {submitError ? (
          <div
            role="alert"
            className="mt-4 rounded-xl border border-(--status-denied) bg-(--light-red) px-3 py-2 text-sm text-(--status-denied)"
          >
            {submitError}
          </div>
        ) : null}
      </AuthLayoutWrapper>
    );
  }

  return (
    <div className="bg-white max-w-134.75 max-h-auto w-full h-full shadow-xs shadow-(--shadow) rounded-xl border-none">
      <div className="md:px-12 md:py-16 px-4 py-8 flex flex-col items-center justify-between gap-8 mb-8 md:mb-0">
        <div className="w-full flex flex-col">
          <h2 className="md:text-2xl text-xl font-semibold text-center text-(--text-primary-dark) mb-2">
            Set your password
          </h2>

          <p className="text-(--text-neutral-600) text-center text-sm leading-6 mb-6">
            {state.organisationName
              ? `Create a secure password to activate your ${state.organisationName} account.`
              : "Create a secure password to activate your account."}
          </p>

          <TherapistSetNewPasswordForm
            form={form}
            onSubmit={onSubmit}
            buttonText={
              isLoading || isCompletingLogin ? "Saving..." : "Set password"
            }
            role="staff"
            errorMessage={
              submitError
                ? submitError
                : isError
                  ? getApiErrorMessage(error)
                  : undefined
            }
          />
        </div>
      </div>
    </div>
  );
};

export default TherapistActivateYourAccount;
