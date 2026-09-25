import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  loginSchema,
  type LoginFormValues,
} from "../../../schemas/login.schema";
import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { AUTH_STATIC_CONTENT } from "../auth.static";
import { getRedirectPathByRole } from "../../../utils/redirectPathByRole";
import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthLoginForm from "@/components/auth/AuthLoginForm";
import OrgPickerModal from "@/components/auth/OrgPickerModal";
import MfaChallengePanel, {
  type MfaFlow,
} from "@/components/auth/MfaChallengePanel";
import {
  usePortalLoginMutation,
  useResolvePortalTenantMutation,
  type PortalLoginResponse,
} from "@/store/api/portalAuthApi";
import type {
  ResolveTenantOrganisation,
  StaffLoginResponse,
} from "@/store/api/authApi";
import { portalApi } from "@/store/api/portalApi";
import { setAuthSession } from "@/utils/authStorage";
import {
  getDeviceTrustToken,
  setDeviceTrustToken,
} from "@/utils/deviceTrustStorage";
import { useAppDispatch } from "@/store/hooks";
import { setSession } from "@/store/authSlice";
import {
  getApiErrorMessage,
  getAuthErrorMessage,
  isApiErrorCode,
} from "@/utils/apiError";
import { getTenantSubdomainFromHostname } from "@/utils/portalTenantContext";
import Toast from "@/components/shared/Toast";

const Login = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [resolvePortalTenant, { isLoading: isResolving }] =
    useResolvePortalTenantMutation();
  const [portalLogin, { isLoading: isPortalLoginLoading }] =
    usePortalLoginMutation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [mfaNotice, setMfaNotice] = useState<string | null>(null);
  const [mfaFlow, setMfaFlow] = useState<MfaFlow | null>(null);
  const [tenantOptions, setTenantOptions] = useState<ResolveTenantOrganisation[]>(
    [],
  );
  const [pendingCredentials, setPendingCredentials] = useState<{
    email: string;
    password: string;
    staySignedIn: boolean;
  } | null>(null);
  const [pendingMfaOrganisation, setPendingMfaOrganisation] =
    useState<ResolveTenantOrganisation | null>(null);
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
      staySignedIn: false,
    },
  });

  async function completePortalLogin(
    response: PortalLoginResponse,
    email?: string,
  ) {
    if (!response.accessToken) {
      setToastMessage(
        response.message || "Login could not be completed. Please try again.",
      );
      return;
    }

    const trustKey = email ?? response.client?.email ?? response.client?.portalEmail;
    if (response.deviceTrustToken && trustKey) {
      setDeviceTrustToken(trustKey, response.deviceTrustToken);
    }

    const tenantSubdomain = getTenantSubdomainFromHostname();
    const session = {
      accessToken: response.accessToken,
      role: "user" as const,
      tenantSubdomain,
      client: response.client,
    };

    setAuthSession(session);
    dispatch(setSession(session));
    setIsTenantModalOpen(false);
    setMfaFlow(null);
    setPendingMfaOrganisation(null);
    navigate(getRedirectPathByRole("user"), { replace: true });
  }

  /**
   * Portal verify-login responses include `client`. Required MFA enrollment confirm
   * uses the shared `/auth/mfa/required-enrollment/confirm` endpoint and returns
   * staff-shaped tokens only — same pattern as staff first login: use the session
   * and hydrate the client profile, then enter the portal.
   */
  async function completePortalAuthentication(
    response: PortalLoginResponse | StaffLoginResponse,
    email?: string,
  ) {
    if ("client" in response && response.accessToken) {
      await completePortalLogin(response, email);
      return;
    }

    if (!response.accessToken) {
      setToastMessage(
        response.message || "Login could not be completed. Please try again.",
      );
      return;
    }

    const trustKey =
      email?.trim() ||
      ("user" in response
        ? response.user?.email || response.user?.username || ""
        : "");

    if (response.deviceTrustToken && trustKey) {
      setDeviceTrustToken(trustKey, response.deviceTrustToken);
    }

    const tenantSubdomain = getTenantSubdomainFromHostname();
    const provisionalSession = {
      accessToken: response.accessToken,
      role: "user" as const,
      tenantSubdomain,
      client: {
        id: 0,
        clientId: "",
        fullName: "",
        email: trustKey,
        portalEmail: trustKey || undefined,
      },
    };
    setAuthSession(provisionalSession);
    dispatch(setSession(provisionalSession));

    const meResult = await dispatch(
      portalApi.endpoints.getPortalMe.initiate(undefined, { forceRefetch: true }),
    );

    if ("error" in meResult || !meResult.data) {
      setToastMessage(
        "error" in meResult
          ? getAuthErrorMessage(meResult.error)
          : "Could not load your portal profile. Please try signing in again.",
      );
      return;
    }

    const me = meResult.data;
    await completePortalLogin(
      {
        accessToken: response.accessToken,
        tokenType: response.tokenType ?? "Bearer",
        expiresIn: response.expiresIn ?? 0,
        client: {
          id: me.id,
          clientId: me.clientId,
          fullName: me.fullName,
          email: me.email || trustKey,
          phone: me.phone,
          assignedTherapistId: me.assignedTherapistId,
          timezone: me.timezone,
          avatarUrl: me.avatarUrl,
          portalEmail: trustKey || me.email,
        },
        deviceTrustToken: response.deviceTrustToken,
      },
      trustKey || me.email,
    );
  }

  const onSubmit = async (values: LoginFormValues) => {
    if (!values.email || !values.password) {
      return;
    }

    const email = values.email.trim();
    setToastMessage(null);
    setMfaNotice(null);

    try {
      const resolved = await resolvePortalTenant({ email }).unwrap();
      const organisations = resolved.organisations ?? [];
      const orgCount =
        resolved.count > 0 ? resolved.count : organisations.length;
      const credentials = {
        email,
        password: values.password,
        staySignedIn: Boolean(values.staySignedIn),
      };
      setPendingCredentials(credentials);
      setTenantOptions(organisations);

      if (organisations.length === 0) {
        if (resolved.orgSlug || resolved.organisationId || resolved.organisationName) {
          const fallbackOrg: ResolveTenantOrganisation = {
            organisationId: resolved.organisationId ?? 0,
            name: resolved.organisationName ?? resolved.orgSlug ?? "",
            slug: resolved.orgSlug ?? "",
            subdomain: "",
            status: "ACTIVE",
          };
          await handleTenantSelect(fallbackOrg, credentials);
          return;
        }
        setToastMessage("No client portal organizations found for this email.");
        return;
      }

      if (orgCount === 1) {
        await handleTenantSelect(organisations[0], credentials);
        return;
      }

      setIsTenantModalOpen(true);
    } catch (loginError) {
      setToastMessage(getApiErrorMessage(loginError));
      console.error("Client portal login failed:", loginError);
    }
  };

  async function handleTenantSelect(
    organisation: ResolveTenantOrganisation,
    credentials?: { email: string; password: string; staySignedIn: boolean },
    verifiedResponse?: PortalLoginResponse,
  ) {
    const resolvedCredentials = credentials ?? pendingCredentials;
    if (!resolvedCredentials) {
      return;
    }

    setToastMessage(null);
    setMfaNotice(null);

    try {
      const response =
        verifiedResponse ??
        (await portalLogin({
          email: resolvedCredentials.email,
          password: resolvedCredentials.password,
          orgSlug: organisation.slug || undefined,
          orgId:
            organisation.organisationId && organisation.organisationId > 0
              ? String(organisation.organisationId)
              : undefined,
          deviceTrustToken:
            getDeviceTrustToken(resolvedCredentials.email) ?? undefined,
          staySignedIn: resolvedCredentials.staySignedIn,
        }).unwrap());

      if (
        !verifiedResponse &&
        response.mfaChallengeToken &&
        (response.mfaRequired || response.mfaEnrollmentRequired)
      ) {
        setIsTenantModalOpen(false);
        setPendingMfaOrganisation(organisation);
        setMfaFlow({
          mode: response.mfaEnrollmentRequired ? "enroll" : "verify",
          challengeToken: response.mfaChallengeToken,
          accountLabel: resolvedCredentials.email,
          ...(response.mfaEnrollmentRequired
            ? {
                smsAvailable: response.mfaSmsAvailable !== false,
                emailAvailable: response.mfaEmailAvailable !== false,
              }
            : {
                method: response.mfaMethod,
                maskedDestination: response.mfaMaskedDestination,
                enrolledMethods: response.mfaMethods,
              }),
        });
        return;
      }

      await completePortalLogin(response, resolvedCredentials.email);
    } catch (loginError) {
      if (isApiErrorCode(loginError, "TENANT_SELECTION_REQUIRED")) {
        setIsTenantModalOpen(true);
        return;
      }

      setToastMessage(getAuthErrorMessage(loginError));
      console.error("Client portal login failed:", loginError);
    }
  }

  const bottomContent = (
    <>
      <p className="text-(--text-neutral-600) text-[0.875rem] leading-5.5 text-center font-normal mt-2">
        Staff member?
      </p>
      <NavLink
        to="/auth/staff/login"
        className="text-[0.875rem] text-(--text-primary-500) font-semibold leading-5.5 mt-2 flex justify-center gap-2"
      >
        Access Staff Portal
        <ArrowUpRight size={20} />
      </NavLink>
    </>
  );

  return (
    <AuthLayoutWrapper
      title={AUTH_STATIC_CONTENT?.loginTitle}
      bottomContent={bottomContent}
      showRightBar={true}
    >
      {mfaFlow && pendingMfaOrganisation ? (
        <MfaChallengePanel
          authChannel="portal"
          flow={mfaFlow}
          staySignedIn={Boolean(pendingCredentials?.staySignedIn)}
          onAuthenticated={async (response) => {
            await completePortalAuthentication(
              response,
              pendingCredentials?.email,
            );
          }}
          onEnrollmentComplete={(method) => {
            // Used only when enrollment did not issue a session (no access token).
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
            forgotPasswordPath="/auth/forgot-password"
            loading={isResolving || isPortalLoginLoading}
            loadingLabel={
              isResolving ? "Finding organizations…" : "Signing in…"
            }
            identifierLabel="Email"
            identifierInputType="email"
          />
        </>
      )}
      <div className="mt-4 text-(--text-neutral-600) text-[0.875rem] leading-5.5 text-center font-normal">
        {AUTH_STATIC_CONTENT?.newClientGuide}
      </div>

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
        onSelect={(organisation) => {
          void handleTenantSelect(organisation);
        }}
        onClose={() => setIsTenantModalOpen(false)}
        isLoading={isPortalLoginLoading}
        lookupIdentifier={pendingCredentials?.email}
        title="Select your clinic"
        description="This email is linked to more than one client portal. Choose the clinic you want to access."
      />
    </AuthLayoutWrapper>
  );
};

export default Login;
