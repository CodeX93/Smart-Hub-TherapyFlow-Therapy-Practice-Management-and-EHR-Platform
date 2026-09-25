import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthLoginForm from "@/components/auth/AuthLoginForm";
import MfaChallengePanel, {
  type MfaFlow,
} from "@/components/auth/MfaChallengePanel";
import AuthTopbar from "@/components/topbar/AuthTopbar";
import {
  useLoginStaffMutation,
  type StaffLoginResponse,
} from "@/store/api/authApi";
import { mapApiRolesToAppRole } from "@/utils/roleMapper";
import { setAuthSession } from "@/utils/authStorage";
import {
  getDeviceTrustToken,
  setDeviceTrustToken,
} from "@/utils/deviceTrustStorage";
import { useAppDispatch } from "@/store/hooks";
import { setSession } from "@/store/authSlice";
import { getApiErrorMessage } from "@/utils/apiError";
import { loginSchema, type LoginFormValues } from "@/schemas/login.schema";

const SuperAdminLogin = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [loginStaff, { isLoading, isError, error }] = useLoginStaffMutation();
  const [mfaFlow, setMfaFlow] = useState<MfaFlow | null>(null);
  const [mfaNotice, setMfaNotice] = useState<string | null>(null);
  const [staySignedIn, setStaySignedIn] = useState(false);

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
      staySignedIn: false,
    },
  });

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const preferStaySignedIn = Boolean(values.staySignedIn);
      setStaySignedIn(preferStaySignedIn);
      const response = await loginStaff({
        username: values.email,
        password: values.password,
        deviceTrustToken: getDeviceTrustToken(values.email) ?? undefined,
        staySignedIn: preferStaySignedIn,
      }).unwrap();

      if (
        response.mfaChallengeToken &&
        (response.mfaRequired || response.mfaEnrollmentRequired)
      ) {
        setMfaFlow({
          mode: response.mfaEnrollmentRequired ? "enroll" : "verify",
          challengeToken: response.mfaChallengeToken,
          accountLabel: values.email,
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

      await completeLogin(response, values.email);
    } catch (err) {
      console.error("Super admin login failed:", err);
    }
  };

  const completeLogin = async (
    response: StaffLoginResponse,
    accountLabel?: string,
  ) => {
      const role = mapApiRolesToAppRole(response.roles);
      if (role !== "super-admin") {
        form.setError("email", {
          type: "manual",
          message: "Not authorized for super admin portal.",
        });
        return;
      }

      if (!response.accessToken) {
        form.setError("email", {
          type: "manual",
          message: response.message || "Login could not be completed.",
        });
        return;
      }

      if (response.deviceTrustToken && accountLabel) {
        setDeviceTrustToken(accountLabel, response.deviceTrustToken);
      }

      const session = {
        accessToken: response.accessToken,
        role,
        user: response.user,
      } as const;

      setAuthSession(session);
      dispatch(setSession(session));

      navigate("/super-admin/dashboard");
  };

  return (
    <div className="min-h-screen bg-(--bg-primary-light) md:px-10 md:py-10 px-4 py-6">
      <div>
        <AuthTopbar />
        <main className="md:px-5 md:pt-4 flex items-center justify-center md:mt-15 mt-8">
          <AuthLayoutWrapper
            title="Welcome back to SmartHub"
            showRightBar={false}
            className="md:min-h-auto"
          >
            {mfaFlow ? (
              <MfaChallengePanel
                flow={mfaFlow}
                staySignedIn={staySignedIn}
                onAuthenticated={async (response) => {
                  if (!("user" in response)) return;
                  await completeLogin(response, mfaFlow.accountLabel);
                }}
                onEnrollmentComplete={(method) => {
                  setMfaFlow(null);
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
                onCancel={() => setMfaFlow(null)}
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
                  loading={isLoading}
                  loadingLabel="Signing in…"
                  errorMessage={isError ? getApiErrorMessage(error) : undefined}
                />
              </>
            )}
          </AuthLayoutWrapper>
        </main>
      </div>
    </div>
  );
};

export default SuperAdminLogin;
