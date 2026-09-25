import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  loginSchema,
  type LoginFormValues,
} from "../../../../schemas/login.schema";
import { useEffect, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { getRedirectPathByRole } from "../../../../utils/redirectPathByRole";
import { getStaffLandingPath } from "@/utils/staffPermissions";
import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthLoginForm from "@/components/auth/AuthLoginForm";
import MfaChallengePanel, {
  type MfaFlow,
} from "@/components/auth/MfaChallengePanel";
import OrgPickerModal from "@/components/auth/OrgPickerModal";
import Toast from "@/components/shared/Toast";
import {
  authApi,
  useLoginStaffMutation,
  useResolveTenantMutation,
  type ResolveTenantOrganisation,
  type StaffLoginResponse,
} from "@/store/api/authApi";
import { setAuthSession } from "@/utils/authStorage";
import {
  getDeviceTrustToken,
  setDeviceTrustToken,
} from "@/utils/deviceTrustStorage";
import { mapApiRolesToAppRole } from "@/utils/roleMapper";
import { useAppDispatch } from "@/store/hooks";
import { setAuthContext, setSession } from "@/store/authSlice";
import {
  getApiErrorMessage,
  getAuthErrorMessage,
  isApiErrorCode,
} from "@/utils/apiError";
import { normalizeApiRoles } from "@/utils/staffPermissions";

const TherapistLogin = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const [resolveTenant, { isLoading: isResolving, isError: isResolveError, error: resolveError }] =
    useResolveTenantMutation();
  const [loginStaff, { isLoading: isTenantLoginLoading, isError: isTenantError, error: tenantError }] =
    useLoginStaffMutation();
  const [tenantOptions, setTenantOptions] = useState<ResolveTenantOrganisation[]>([]);
  const [pendingCredentials, setPendingCredentials] = useState<{
    username: string;
    password: string;
    staySignedIn: boolean;
  } | null>(null);
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);
  const [customError, setCustomError] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [mfaNotice, setMfaNotice] = useState<string | null>(null);
  const [mfaFlow, setMfaFlow] = useState<MfaFlow | null>(null);
  const [pendingMfaOrganisation, setPendingMfaOrganisation] =
    useState<ResolveTenantOrganisation | null>(null);
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
      staySignedIn: false,
    },
  });

  const notice = (location.state as { mfaNotice?: string } | null)?.mfaNotice;
  const [handledNotice, setHandledNotice] = useState<string | undefined>(undefined);
  if (notice && notice !== handledNotice) {
    setHandledNotice(notice);
    setMfaNotice(notice);
  }
  useEffect(() => {
    const notice = (location.state as { mfaNotice?: string } | null)?.mfaNotice;
    if (notice) {

      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, location.state, navigate]);

  const onSubmit = async (values: LoginFormValues) => {
    if (!values.email || !values.password) {
      return;
    }

    const identifier = values.email.trim();
    setCustomError(null);
    setToastMessage(null);

    try {
      const resolved = await resolveTenant({ email: identifier }).unwrap();
      const organisations = resolved.organisations ?? [];
      const orgCount = resolved.count > 0 ? resolved.count : organisations.length;
      setPendingCredentials({
        username: identifier,
        password: values.password,
        staySignedIn: Boolean(values.staySignedIn),
      });
      setTenantOptions(organisations);

      if (organisations.length === 0) {
        // Backend may return top-level org fields with an empty organisations array.
        if (resolved.orgSlug || resolved.organisationId || resolved.organisationName) {
          const fallbackOrg: ResolveTenantOrganisation = {
            organisationId: resolved.organisationId ?? 0,
            name: resolved.organisationName ?? resolved.orgSlug ?? "",
            slug: resolved.orgSlug ?? "",
            subdomain: "",
            status: "ACTIVE",
          };
          await handleTenantSelect(fallbackOrg, {
            username: identifier,
            password: values.password,
            staySignedIn: Boolean(values.staySignedIn),
          });
          return;
        }
        setCustomError("No organizations found for this account.");
        return;
      }

      if (orgCount === 1) {
        await handleTenantSelect(organisations[0], {
          username: identifier,
          password: values.password,
          staySignedIn: Boolean(values.staySignedIn),
        });
        return;
      }

      setIsTenantModalOpen(true);
    } catch (error) {
      console.error("Staff login failed:", error);
    }
  };

  async function handleTenantSelect(
    organisation: ResolveTenantOrganisation,
    credentials?: { username: string; password: string; staySignedIn: boolean },
    verifiedResponse?: StaffLoginResponse,
  ) {
    const resolvedCredentials = credentials ?? pendingCredentials;
    if (!resolvedCredentials) {
      return;
    }

    setCustomError(null);
    setToastMessage(null);

    const loginUsername =
      organisation.username?.trim() || resolvedCredentials.username;
    const staySignedIn = Boolean(resolvedCredentials.staySignedIn);
    const deviceTrustToken = getDeviceTrustToken(loginUsername) ?? undefined;

    try {
      const resolved =
        verifiedResponse ??
        (await loginStaff({
          username: loginUsername,
          password: resolvedCredentials.password,
          orgSlug: organisation.slug || undefined,
          orgId:
            organisation.organisationId && organisation.organisationId > 0
              ? String(organisation.organisationId)
              : undefined,
          deviceTrustToken,
          staySignedIn,
        }).unwrap());

      if (
        !verifiedResponse &&
        resolved.mfaChallengeToken &&
        (resolved.mfaRequired || resolved.mfaEnrollmentRequired)
      ) {
        setIsTenantModalOpen(false);
        setPendingMfaOrganisation(organisation);
        setMfaFlow({
          mode: resolved.mfaEnrollmentRequired ? "enroll" : "verify",
          challengeToken: resolved.mfaChallengeToken,
          accountLabel: loginUsername,
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

      if (resolved.deviceTrustToken) {
        setDeviceTrustToken(loginUsername, resolved.deviceTrustToken);
      }

      if (resolved.passwordChangeRequired) {
        if (!resolved.changePasswordToken) {
          setCustomError(
            resolved.message ||
              "Password change is required, but the server did not return a change token.",
          );
          return;
        }

        setIsTenantModalOpen(false);
        navigate("/auth/staff/activate-your-account", {
          state: {
            changePasswordToken: resolved.changePasswordToken,
            email: resolved.user.email || loginUsername,
            message: resolved.message,
            organisationName: organisation.name,
            organisation,
            staySignedIn,
            loginUsername,
          },
        });
        return;
      }

      if (!resolved.accessToken) {
        setCustomError(
          resolved.message || "Login could not be completed. Please try again.",
        );
        return;
      }

      const preliminaryRole = mapApiRolesToAppRole(resolved.roles);
      if (preliminaryRole === "user") {
        setCustomError("Client accounts must sign in through the Client Portal.");
        return;
      }

      const tenantSchema =
        resolved.tenantSchema ??
        (organisation.slug ? `tenant_${organisation.slug}` : undefined);

      if (
        (preliminaryRole === "admin" ||
          preliminaryRole === "therapist" ||
          preliminaryRole === "staff") &&
        (!tenantSchema || tenantSchema === "public")
      ) {
        setCustomError(
          "Login could not be completed for the selected organization. Please try again.",
        );
        return;
      }

      const session = {
        accessToken: resolved.accessToken,
        role: preliminaryRole,
        tenantSlug: organisation.slug || resolved.organisationSlug || undefined,
        tenantSchema,
        organisationId: resolved.organisationId ?? organisation.organisationId,
        organisationSlug:
          resolved.organisationSlug ?? organisation.slug ?? undefined,
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
        setCustomError(getAuthErrorMessage(meResult.error));
        return;
      }

      const me = meResult.data;
      if (!me) {
        setCustomError("Could not verify your account. Please try again.");
        return;
      }

      if (
        me.organisationId &&
        organisation.organisationId &&
        me.organisationId !== organisation.organisationId
      ) {
        setCustomError("Organization mismatch. Please select the correct organization.");
        return;
      }

      if (
        me.organisationSlug &&
        organisation.slug &&
        me.organisationSlug !== organisation.slug
      ) {
        setCustomError("Organization mismatch. Please select the correct organization.");
        return;
      }

      const role = mapApiRolesToAppRole(me.roles);
      if (role === "user") {
        setCustomError("Client accounts must sign in through the Client Portal.");
        return;
      }

      if (
        (role === "admin" || role === "therapist" || role === "staff") &&
        (!me.tenantSchema || me.tenantSchema === "public")
      ) {
        setCustomError(
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

      setIsTenantModalOpen(false);

      const landingPath =
        role === "staff"
          ? getStaffLandingPath(me.permissions ?? me.authorities ?? [], normalizeApiRoles(me.roles))
          : getRedirectPathByRole(role);
      navigate(landingPath, { replace: true });
    } catch (err) {
      if (isApiErrorCode(err, "TENANT_SELECTION_REQUIRED")) {
        setIsTenantModalOpen(true);
        return;
      }

      const message = getAuthErrorMessage(err);
      // Keep credential errors visible above the org picker (form sits behind it).
      if (isTenantModalOpen) {
        setToastMessage(message);
      } else {
        setCustomError(message);
      }
      console.error("Tenant login failed:", err);
    }
  }

  const bottomContent = (
    <>
      <p className="text-(--text-neutral-600) text-sm text-center mt-2">
        Are you a client?
      </p>
      <NavLink
        to="/auth/login"
        className="text-sm text-(--text-primary-500) font-semibold mt-2 flex justify-center gap-2"
      >
        Access Client Portal
        <ArrowUpRight size={20} />
      </NavLink>
    </>
  );

  return (
    <AuthLayoutWrapper
      title="Welcome back to SmartHub"
      bottomContent={bottomContent}
      showRightBar={false}
    >
      {mfaFlow && pendingMfaOrganisation ? (
        <MfaChallengePanel
          flow={mfaFlow}
          staySignedIn={Boolean(pendingCredentials?.staySignedIn)}
          onAuthenticated={(response) => {
            if (!("user" in response)) return;
            return handleTenantSelect(
              pendingMfaOrganisation,
              pendingCredentials ?? undefined,
              response,
            );
          }}
          onEnrollmentComplete={(method) => {
            setMfaFlow(null);
            setPendingMfaOrganisation(null);
            const methodLabel =
              method === "SMS"
                ? "Text message"
                : method === "EMAIL"
                  ? "Email"
                  : "Authenticator app";
            setMfaNotice(
              `${methodLabel} MFA is set up. Sign in again to continue.`,
            );
          }}
          onCancel={() => {
            setMfaFlow(null);
            setPendingMfaOrganisation(null);
          }}
        />
      ) : (
        <>
          {mfaNotice ? (
            <div
              role="status"
              className="mt-6 rounded-xl border border-(--border-success) bg-(--bg-success-light) px-3 py-2 text-sm text-(--dark-green)"
            >
              {mfaNotice}
            </div>
          ) : null}
          <AuthLoginForm
            form={form}
            onSubmit={onSubmit}
            forgotPasswordPath="/auth/staff/forgot-password"
            identifierLabel="Email / Username"
            identifierInputType="text"
            loading={isResolving || isTenantLoginLoading}
            loadingLabel={
              isResolving ? "Finding organizations…" : "Signing in…"
            }
            errorMessage={
              isTenantModalOpen
                ? undefined
                : customError
                  ? customError
                  : isResolveError
                    ? getApiErrorMessage(resolveError)
                    : isTenantError
                      ? getAuthErrorMessage(tenantError)
                      : undefined
            }
          />
        </>
      )}

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type="error"
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <OrgPickerModal
        open={isTenantModalOpen}
        organisations={tenantOptions}
        onSelect={handleTenantSelect}
        onClose={() => setIsTenantModalOpen(false)}
        isLoading={isTenantLoginLoading}
        lookupIdentifier={pendingCredentials?.username}
      />
    </AuthLayoutWrapper>
  );
};

export default TherapistLogin;
