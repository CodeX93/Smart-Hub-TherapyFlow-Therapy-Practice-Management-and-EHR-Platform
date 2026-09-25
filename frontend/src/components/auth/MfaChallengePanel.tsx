import { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  Check,
  Copy,
  Download,
  KeyRound,
  Mail,
  MessageSquare,
  ShieldCheck,
  Smartphone,
} from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { LoadingDots } from "@/components/ui/loading-dots";
import { cn } from "@/lib/utils";
import {
  useConfirmRequiredMfaEnrollmentMutation,
  useSendMfaLoginCodeMutation,
  useStartRequiredMfaEnrollmentMutation,
  useVerifyMfaLoginMutation,
  type MfaMethod,
  type StaffLoginResponse,
} from "@/store/api/authApi";
import {
  useVerifyPortalMfaLoginMutation,
  type PortalLoginResponse,
} from "@/store/api/portalAuthApi";
import { getAuthErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import { setDeviceTrustToken } from "@/utils/deviceTrustStorage";

type MfaAuthChannel = "staff" | "portal";

type MfaFlow =
  | {
      mode: "verify";
      challengeToken: string;
      accountLabel: string;
      method?: MfaMethod;
      maskedDestination?: string;
      enrolledMethods?: MfaMethod[];
    }
  | {
      mode: "enroll";
      challengeToken: string;
      accountLabel: string;
      smsAvailable?: boolean;
      emailAvailable?: boolean;
    };

interface MfaChallengePanelProps {
  flow: MfaFlow;
  authChannel?: MfaAuthChannel;
  onAuthenticated: (
    response: StaffLoginResponse | PortalLoginResponse,
  ) => Promise<void> | void;
  onEnrollmentComplete: (method: MfaMethod) => void;
  onCancel: () => void;
  /** Prefer staying signed in after MFA (longer refresh). Default true. */
  staySignedIn?: boolean;
  /** Offer "trust this device" so MFA can be skipped next time. Default true. */
  allowTrustDevice?: boolean;
  /** Optional override (e.g. portal MFA verify endpoint). */
  verifyLoginOverride?: (
    request: {
      mfaChallengeToken: string;
      code: string;
      method?: MfaMethod;
      trustDevice?: boolean;
      staySignedIn?: boolean;
    },
  ) => Promise<StaffLoginResponse>;
}

type EnrollmentState = {
  method: MfaMethod;
  secret?: string;
  provisioningUri?: string;
  maskedDestination?: string;
  codeSent?: boolean;
};

const RESEND_COOLDOWN_SECONDS = 60;

const methodLabels: Record<MfaMethod, string> = {
  TOTP: "Authenticator app",
  SMS: "Text message",
  EMAIL: "Email",
};

const MfaChallengePanel = ({
  flow,
  authChannel = "staff",
  onAuthenticated,
  onEnrollmentComplete,
  onCancel,
  staySignedIn = true,
  allowTrustDevice = true,
  verifyLoginOverride,
}: MfaChallengePanelProps) => {
  const [code, setCode] = useState("");
  const [useRecoveryCode, setUseRecoveryCode] = useState(false);
  const [copied, setCopied] = useState<"secret" | "recovery" | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const [isCompletingAuthentication, setIsCompletingAuthentication] =
    useState(false);
  const [selectedMethod, setSelectedMethod] = useState<MfaMethod>("EMAIL");
  const [phone, setPhone] = useState("");
  const [enrollmentStarted, setEnrollmentStarted] = useState(false);
  const [enrollment, setEnrollment] = useState<EnrollmentState | null>(null);
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [canAddMoreMethods, setCanAddMoreMethods] = useState(false);
  const [enrolledMethods, setEnrolledMethods] = useState<MfaMethod[]>([]);
  const [pendingEnrollmentSession, setPendingEnrollmentSession] =
    useState<StaffLoginResponse | null>(null);
  const [verifyMethodChoice, setVerifyMethodChoice] = useState<MfaMethod | null>(
    null,
  );
  const [trustDevice, setTrustDevice] = useState(true);
  const [resendCooldown, setResendCooldown] = useState(0);
  const resendTimerRef = useRef<number | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("error");

  const [verifyStaffLogin, verifyStaffState] = useVerifyMfaLoginMutation();
  const [verifyPortalLogin, verifyPortalState] = useVerifyPortalMfaLoginMutation();
  const verifyLogin = authChannel === "portal" ? verifyPortalLogin : verifyStaffLogin;
  const verifyState = authChannel === "portal" ? verifyPortalState : verifyStaffState;
  const [sendLoginCode, sendCodeState] = useSendMfaLoginCodeMutation();
  const [startEnrollment, enrollmentState] =
    useStartRequiredMfaEnrollmentMutation();
  const [confirmEnrollment, confirmationState] =
    useConfirmRequiredMfaEnrollmentMutation();

  const enrolledForVerify =
    flow.mode === "verify"
      ? flow.enrolledMethods && flow.enrolledMethods.length > 0
        ? flow.enrolledMethods
        : flow.method
          ? [flow.method]
          : (["TOTP"] as MfaMethod[])
      : [];
  const verifyMethod =
    flow.mode === "verify"
      ? verifyMethodChoice ?? flow.method ?? enrolledForVerify[0] ?? "TOTP"
      : null;
  const isOtpVerify = verifyMethod === "SMS" || verifyMethod === "EMAIL";
  const needsVerifyMethodPick =
    flow.mode === "verify" &&
    enrolledForVerify.length > 1 &&
    verifyMethodChoice == null;

  useEffect(() => {
    if (flow.mode !== "verify" || !isOtpVerify || needsVerifyMethodPick) return;
    setResendCooldown(RESEND_COOLDOWN_SECONDS);
  }, [flow.mode, isOtpVerify, needsVerifyMethodPick, verifyMethod]);

  useEffect(() => {
    if (resendCooldown <= 0) {
      if (resendTimerRef.current != null) {
        window.clearInterval(resendTimerRef.current);
        resendTimerRef.current = null;
      }
      return;
    }
    if (resendTimerRef.current != null) return;
    resendTimerRef.current = window.setInterval(() => {
      setResendCooldown((current) => Math.max(0, current - 1));
    }, 1000);
    return () => {
      if (resendTimerRef.current != null) {
        window.clearInterval(resendTimerRef.current);
        resendTimerRef.current = null;
      }
    };
  }, [resendCooldown]);

  const normalizedCode = useRecoveryCode
    ? code.trim().toUpperCase()
    : code.replace(/\D/g, "").slice(0, 6);

  const handleVerify = async () => {
    if (!normalizedCode) return;
    setLocalError(null);
    try {
      const request = {
        mfaChallengeToken: flow.challengeToken,
        code: normalizedCode,
        method: verifyMethod ?? undefined,
        trustDevice: allowTrustDevice ? trustDevice : false,
        staySignedIn,
      };
      const response = verifyLoginOverride
        ? await verifyLoginOverride(request)
        : await verifyLogin(request).unwrap();
      if (response.deviceTrustToken) {
        setDeviceTrustToken(flow.accountLabel, response.deviceTrustToken);
      }
      setIsCompletingAuthentication(true);
      try {
        await onAuthenticated(response);
      } finally {
        setIsCompletingAuthentication(false);
      }
    } catch (error) {
      const msg = getAuthErrorMessage(error);
      setLocalError(msg);
      setToastType("error");
      setToastMessage(msg);
    }
  };

  const handleResendCode = async () => {
    if (resendCooldown > 0 || sendCodeState.isLoading) return;
    setLocalError(null);
    try {
      const response = await sendLoginCode({
        mfaChallengeToken: flow.challengeToken,
        method: verifyMethod ?? undefined,
      }).unwrap();
      if (response.sent) {
        setResendCooldown(RESEND_COOLDOWN_SECONDS);
      }
    } catch (error) {
      const msg = getAuthErrorMessage(error);
      setLocalError(msg);
      setToastType("error");
      setToastMessage(msg);
    }
  };

  const handleStartEnrollment = async () => {
    if (selectedMethod === "SMS" && !phone.trim()) {
      setLocalError("Enter your mobile number including country code (e.g. +14155551234).");
      return;
    }
    setLocalError(null);
    try {
      const response = await startEnrollment({
        mfaChallengeToken: flow.challengeToken,
        method: selectedMethod,
        phone: selectedMethod === "SMS" ? phone.trim() : undefined,
      }).unwrap();
      setEnrollment({
        method: response.method ?? selectedMethod,
        secret: response.secret,
        provisioningUri: response.provisioningUri,
        maskedDestination: response.maskedDestination,
        codeSent: response.codeSent,
      });
      setEnrollmentStarted(true);
      if (response.method === "SMS" || response.method === "EMAIL") {
        setResendCooldown(RESEND_COOLDOWN_SECONDS);
      }
    } catch (error) {
      const msg = getAuthErrorMessage(error);
      setLocalError(msg);
      setToastType("error");
      setToastMessage(msg);
    }
  };

  const handleConfirmEnrollment = async () => {
    if (normalizedCode.length !== 6) return;
    setLocalError(null);
    try {
      const response = await confirmEnrollment({
        mfaChallengeToken: flow.challengeToken,
        code: normalizedCode,
        trustDevice: allowTrustDevice ? trustDevice : false,
        staySignedIn,
      }).unwrap();
      const recovery =
        response.mfaRecoveryCodes?.length
          ? response.mfaRecoveryCodes
          : (response as { recoveryCodes?: string[] }).recoveryCodes;
      if (recovery?.length) {
        setRecoveryCodes(recovery);
      }
      setEnrolledMethods(
        (response.enrolledMethods as MfaMethod[] | undefined) ??
          enrolledMethods,
      );
      setCanAddMoreMethods(response.canAddMoreMethods === true);
      if (response.accessToken) {
        setPendingEnrollmentSession(response);
        if (response.deviceTrustToken) {
          setDeviceTrustToken(flow.accountLabel, response.deviceTrustToken);
        }
      }
      setCode("");
      setEnrollmentStarted(false);
      setEnrollment(null);
      // No recovery codes and session ready → log in immediately
      if (
        (!recovery || recovery.length === 0) &&
        response.accessToken &&
        response.canAddMoreMethods !== true
      ) {
        setIsCompletingAuthentication(true);
        try {
          await onAuthenticated(response);
        } finally {
          setIsCompletingAuthentication(false);
        }
      }
    } catch (error) {
      const msg = getAuthErrorMessage(error);
      setLocalError(msg);
      setToastType("error");
      setToastMessage(msg);
    }
  };

  const copyText = async (
    value: string,
    target: "secret" | "recovery",
  ) => {
    await navigator.clipboard.writeText(value);
    setCopied(target);
    window.setTimeout(() => setCopied(null), 1600);
  };

  const downloadRecoveryCodes = () => {
    if (!recoveryCodes) return;
    const contents = [
      "SmartHub recovery codes",
      `Account: ${flow.accountLabel}`,
      "Each code can be used once. Store these somewhere private.",
      "",
      ...recoveryCodes,
      "",
    ].join("\n");
    const url = URL.createObjectURL(
      new Blob([contents], { type: "text/plain;charset=utf-8" }),
    );
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "smarthub-recovery-codes.txt";
    anchor.click();
    URL.revokeObjectURL(url);
  };

  // Prefer email as the default enrollment method for new organisation admins.
  const enrollmentOptions: Array<{
    method: MfaMethod;
    icon: typeof ShieldCheck;
    description: string;
  }> = [];
  // Always offer SMS/email during enrollment when the backend has not explicitly disabled them.
  // Login responses set these flags; default to available so users are not stuck on authenticator-only.
  const smsAvailable =
    flow.mode === "enroll" ? flow.smsAvailable !== false : false;
  const emailAvailable =
    flow.mode === "enroll" ? flow.emailAvailable !== false : false;
  if (emailAvailable) {
    enrollmentOptions.push({
      method: "EMAIL",
      icon: Mail,
      description: "Receive a 6-digit code by email.",
    });
  }
  if (smsAvailable) {
    enrollmentOptions.push({
      method: "SMS",
      icon: MessageSquare,
      description: "Receive a 6-digit code by text message.",
    });
  }
  enrollmentOptions.push({
    method: "TOTP",
    icon: ShieldCheck,
    description: "Use Google Authenticator, 1Password, or similar apps.",
  });
  const availableEnrollmentOptions = enrollmentOptions.filter(
    (option) => !enrolledMethods.includes(option.method),
  );

  useEffect(() => {
    if (flow.mode !== "enroll" || enrollmentStarted) return;
    if (enrolledMethods.includes(selectedMethod)) {
      const next = availableEnrollmentOptions[0]?.method;
      if (next) setSelectedMethod(next);
      return;
    }
    // Keep email selected when available; otherwise pick first available option.
    if (emailAvailable && !enrolledMethods.includes("EMAIL")) {
      if (selectedMethod !== "EMAIL") setSelectedMethod("EMAIL");
      return;
    }
    const first = availableEnrollmentOptions[0]?.method;
    if (first && selectedMethod !== first) {
      setSelectedMethod(first);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- re-evaluate when availability/enrollment changes
  }, [
    flow.mode,
    enrollmentStarted,
    emailAvailable,
    enrolledMethods.join(","),
    availableEnrollmentOptions.map((o) => o.method).join(","),
  ]);

  const error =
    localError ||
    (enrollmentState.isError && enrollmentStarted
      ? getAuthErrorMessage(enrollmentState.error)
      : undefined);

  useEffect(() => {
    if (error) {
      setToastType("error");
      setToastMessage(error);
    }
  }, [error]);

  const finishEnrollment = async () => {
    if (pendingEnrollmentSession?.accessToken) {
      setIsCompletingAuthentication(true);
      try {
        await onAuthenticated(pendingEnrollmentSession);
      } finally {
        setIsCompletingAuthentication(false);
      }
      return;
    }
    onEnrollmentComplete(
      enrolledMethods[0] ?? enrollment?.method ?? selectedMethod,
    );
  };

  const continueAddingMethod = () => {
    setRecoveryCodes(null);
    setEnrollmentStarted(false);
    setEnrollment(null);
    setCode("");
    setLocalError(null);
    const remaining = (["TOTP", "SMS", "EMAIL"] as MfaMethod[]).filter(
      (method) => !enrolledMethods.includes(method),
    );
    if (remaining.length > 0) {
      setSelectedMethod(remaining[0]);
    }
  };

  if (recoveryCodes) {
    return (
      <section className="mt-8" aria-labelledby="mfa-recovery-title">
        <div className="flex items-start gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-(--bg-primary-100) text-(--text-primary-500)">
            <ShieldCheck size={21} aria-hidden="true" />
          </div>
          <div>
            <h3
              id="mfa-recovery-title"
              className="text-lg font-semibold text-(--text-primary-dark)"
            >
              Save your recovery codes
            </h3>
            <p className="mt-1 text-sm leading-6 text-(--text-neutral-600)">
              Each code works once if your sign-in method is unavailable. They
              will not be shown again.
            </p>
          </div>
        </div>

        <div className="mt-5 grid grid-cols-1 gap-2 rounded-xl border border-(--neutral-200) bg-(--neutral-50) p-4 font-mono text-sm text-(--neutral-950) sm:grid-cols-2">
          {recoveryCodes.map((recoveryCode) => (
            <span key={recoveryCode} className="break-all">
              {recoveryCode}
            </span>
          ))}
        </div>

        <div className="mt-4 grid gap-2 sm:grid-cols-2">
          <Button
            type="button"
            variant="outline"
            size="lg"
            onClick={() =>
              void copyText(recoveryCodes.join("\n"), "recovery")
            }
          >
            {copied === "recovery" ? <Check /> : <Copy />}
            {copied === "recovery" ? "Copied" : "Copy codes"}
          </Button>
          <Button
            type="button"
            variant="outline"
            size="lg"
            onClick={downloadRecoveryCodes}
          >
            <Download />
            Download
          </Button>
        </div>

        {canAddMoreMethods ? (
          <div className="mt-5 grid gap-2">
            <Button
              type="button"
              size="lg"
              className="w-full"
              onClick={continueAddingMethod}
            >
              Add another sign-in method
            </Button>
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="w-full"
              loading={isCompletingAuthentication}
              loadingLabel="Signing in…"
              onClick={() => void finishEnrollment()}
            >
              {pendingEnrollmentSession
                ? "I saved my codes — continue to portal"
                : "I saved my codes — return to sign in"}
            </Button>
          </div>
        ) : (
          <Button
            type="button"
            size="lg"
            className="mt-5 w-full"
            loading={isCompletingAuthentication}
            loadingLabel="Signing in…"
            onClick={() => void finishEnrollment()}
          >
            {pendingEnrollmentSession
              ? "I saved my codes — continue to portal"
              : "I saved my codes — return to sign in"}
          </Button>
        )}
      </section>
    );
  }

  if (
    flow.mode === "enroll" &&
    !enrollmentStarted &&
    enrolledMethods.length > 0 &&
    !canAddMoreMethods
  ) {
    return (
      <section className="mt-8" aria-labelledby="mfa-done-title">
        <div className="flex items-start gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-(--bg-primary-100) text-(--text-primary-500)">
            <ShieldCheck size={21} aria-hidden="true" />
          </div>
          <div>
            <h3
              id="mfa-done-title"
              className="text-lg font-semibold text-(--text-primary-dark)"
            >
              Sign-in methods ready
            </h3>
            <p className="mt-1 text-sm leading-6 text-(--text-neutral-600)">
              You can use{" "}
              {enrolledMethods.map((m) => methodLabels[m]).join(", ")} at
              sign-in.
            </p>
          </div>
        </div>
        <Button
          type="button"
          size="lg"
          className="mt-5 w-full"
          loading={isCompletingAuthentication}
          loadingLabel="Signing in…"
          onClick={() => void finishEnrollment()}
        >
          {pendingEnrollmentSession ? "Continue to portal" : "Return to sign in"}
        </Button>
      </section>
    );
  }

  const isEnrollment = flow.mode === "enroll";
  const isBusy =
    verifyState.isLoading ||
    enrollmentState.isLoading ||
    confirmationState.isLoading ||
    sendCodeState.isLoading ||
    isCompletingAuthentication;
  const isActionBusy =
    verifyState.isLoading ||
    confirmationState.isLoading ||
    isCompletingAuthentication;

  const verifyDescription = (() => {
    if (useRecoveryCode) {
      return "Enter one of your saved recovery codes.";
    }
    if (needsVerifyMethodPick) {
      return "Choose which verification method you want to use.";
    }
    if (flow.mode === "verify") {
      if (verifyMethod === "SMS") {
        return flow.maskedDestination && enrolledForVerify.length === 1
          ? `Enter the code sent to ${flow.maskedDestination}.`
          : "Enter the code we texted you.";
      }
      if (verifyMethod === "EMAIL") {
        return flow.maskedDestination && enrolledForVerify.length === 1
          ? `Enter the code sent to ${flow.maskedDestination}.`
          : "Enter the code we emailed you.";
      }
    }
    return `Enter the code from your authenticator app for ${flow.accountLabel}.`;
  })();

  const selectVerifyMethod = async (method: MfaMethod) => {
    setVerifyMethodChoice(method);
    setUseRecoveryCode(false);
    setCode("");
    setLocalError(null);
    if (method === "SMS" || method === "EMAIL") {
      try {
        await sendLoginCode({
          mfaChallengeToken: flow.challengeToken,
          method,
        }).unwrap();
        setResendCooldown(RESEND_COOLDOWN_SECONDS);
      } catch (error) {
        const msg = getAuthErrorMessage(error);
        setLocalError(msg);
        setToastType("error");
        setToastMessage(msg);
      }
    }
  };

  return (
    <section className="mt-8" aria-labelledby="mfa-panel-title">
      <Button
        type="button"
        variant="tertiary"
        onClick={onCancel}
        className="mb-5 min-h-11 px-0"
      >
        <ArrowLeft size={17} aria-hidden="true" />
        Back to sign in
      </Button>

      <div className="flex items-start gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-(--bg-primary-100) text-(--text-primary-500)">
          {isEnrollment ? (
            <ShieldCheck size={21} aria-hidden="true" />
          ) : (
            <KeyRound size={20} aria-hidden="true" />
          )}
        </div>
        <div>
          <h3
            id="mfa-panel-title"
            className="text-lg font-semibold text-(--text-primary-dark)"
          >
            {isEnrollment
              ? enrolledMethods.length > 0
                ? "Add another sign-in method"
                : "Protect your account"
              : "Verify it’s really you"}
          </h3>
          <p className="mt-1 text-sm leading-6 text-(--text-neutral-600)">
            {isEnrollment
              ? enrollmentStarted
                ? enrollment?.method === "TOTP"
                  ? "Scan the QR code or enter the setup key, then confirm with a code."
                  : enrollment?.maskedDestination
                    ? `Enter the code sent to ${enrollment.maskedDestination}.`
                    : "Enter the verification code we sent you."
                : enrolledMethods.length > 0
                  ? `Already set up: ${enrolledMethods
                      .map((m) => methodLabels[m])
                      .join(", ")}. You can add another method now or finish later from Security settings.`
                  : "Choose how you want to verify sign-in. You can set up more than one method."
              : verifyDescription}
          </p>
        </div>
      </div>

      {!isEnrollment && needsVerifyMethodPick ? (
        <div className="mt-5 grid gap-3">
          {enrolledForVerify.map((method) => {
            const Icon =
              method === "SMS"
                ? MessageSquare
                : method === "EMAIL"
                  ? Mail
                  : ShieldCheck;
            return (
              <button
                key={method}
                type="button"
                onClick={() => void selectVerifyMethod(method)}
                disabled={sendCodeState.isLoading}
                className="flex items-start gap-3 rounded-xl border border-(--neutral-200) bg-(--neutral-25) p-4 text-left transition-colors hover:border-(--text-primary-500)"
              >
                <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white text-(--text-primary-500)">
                  <Icon size={18} aria-hidden="true" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-(--text-primary-dark)">
                    {methodLabels[method]}
                  </p>
                  <p className="mt-1 text-xs leading-5 text-(--text-neutral-600)">
                    {method === "TOTP"
                      ? "Use a code from your authenticator app."
                      : method === "SMS"
                        ? "Get a one-time code by text message."
                        : "Get a one-time code by email."}
                  </p>
                </div>
              </button>
            );
          })}
          {error ? (
            <div
              role="alert"
              className="rounded-xl border border-(--status-denied) bg-(--light-red) px-3 py-2 text-sm text-(--status-denied)"
            >
              {error}
            </div>
          ) : null}
          <Button
            type="button"
            variant="tertiary"
            className="min-h-11 px-0"
            onClick={() => {
              setUseRecoveryCode(true);
              setVerifyMethodChoice(enrolledForVerify[0] ?? "TOTP");
              setCode("");
              setLocalError(null);
            }}
          >
            Use a recovery code instead
          </Button>
        </div>
      ) : null}

      {isEnrollment && !enrollmentStarted && availableEnrollmentOptions.length > 0 ? (
        <div className="mt-5 grid gap-3">
          <p className="text-sm font-semibold text-(--text-primary-dark)">
            {enrolledMethods.length > 0
              ? "Choose another method"
              : "Choose a verification method"}
          </p>
          {availableEnrollmentOptions.map(({ method, icon: Icon, description }) => (
            <button
              key={method}
              type="button"
              onClick={() => {
                setSelectedMethod(method);
                setLocalError(null);
              }}
              className={cn(
                "flex items-start gap-3 rounded-xl border p-4 text-left transition-colors",
                selectedMethod === method
                  ? "border-(--text-primary-500) bg-(--bg-primary-100)"
                  : "border-(--neutral-200) bg-(--neutral-25) hover:border-(--neutral-300)",
              )}
            >
              <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white text-(--text-primary-500)">
                <Icon size={18} aria-hidden="true" />
              </div>
              <div>
                <p className="text-sm font-semibold text-(--text-primary-dark)">
                  {methodLabels[method]}
                </p>
                <p className="mt-1 text-xs leading-5 text-(--text-neutral-600)">
                  {description}
                </p>
              </div>
            </button>
          ))}

          {selectedMethod === "SMS" ? (
            <label className="block">
              <span className="text-sm font-semibold text-(--text-primary-dark)">
                Mobile number
              </span>
              <Input
                autoFocus
                type="tel"
                inputMode="tel"
                autoComplete="tel"
                placeholder="+14155551234"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                className="mt-2 h-12 rounded-xl bg-(--surface-white) px-4 text-base shadow-none"
              />
              <p className="mt-1 text-xs text-(--text-neutral-600)">
                Include country code. Standard messaging rates may apply.
              </p>
            </label>
          ) : null}

          <Button
            type="button"
            size="lg"
            className="w-full"
            loading={enrollmentState.isLoading}
            loadingLabel="Sending verification…"
            onClick={() => void handleStartEnrollment()}
          >
            Continue
          </Button>
          {enrolledMethods.length > 0 ? (
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="w-full"
              loading={isCompletingAuthentication}
              loadingLabel="Signing in…"
              onClick={() => void finishEnrollment()}
            >
              {pendingEnrollmentSession
                ? "Skip for now — continue to portal"
                : "Skip for now — return to sign in"}
            </Button>
          ) : null}
        </div>
      ) : null}

      {isEnrollment && enrollment?.method === "TOTP" && enrollment.provisioningUri ? (
        <div className="mt-5 grid gap-5 rounded-xl border border-(--neutral-200) bg-(--neutral-25) p-4 sm:grid-cols-[9rem_1fr] sm:items-center">
          <div className="mx-auto rounded-lg border border-(--neutral-100) bg-white p-2">
            <QRCodeSVG
              value={enrollment.provisioningUri}
              size={128}
              level="M"
              aria-label="Authenticator setup QR code"
            />
          </div>
          <div>
            <p className="text-sm font-semibold text-(--text-primary-dark)">
              Scan with your authenticator
            </p>
            <p className="mt-1 text-xs leading-5 text-(--text-neutral-600)">
              Use Microsoft Authenticator, Google Authenticator, 1Password, or
              another TOTP app.
            </p>
            {enrollment.secret ? (
              <div className="mt-3 flex items-center gap-2 rounded-lg bg-(--neutral-50) p-2">
                <code className="min-w-0 flex-1 break-all text-xs text-(--neutral-800)">
                  {enrollment.secret}
                </code>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-11 text-(--text-primary-500)"
                  onClick={() =>
                    void copyText(enrollment.secret!, "secret")
                  }
                  aria-label="Copy manual setup key"
                >
                  {copied === "secret" ? <Check size={17} /> : <Copy size={17} />}
                </Button>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}

      {isEnrollment &&
      enrollmentStarted &&
      enrollment &&
      enrollment.method !== "TOTP" ? (
        <div className="mt-5 flex items-center gap-3 rounded-xl border border-(--neutral-200) bg-(--neutral-25) p-4">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-white text-(--text-primary-500)">
            {enrollment.method === "SMS" ? (
              <Smartphone size={18} aria-hidden="true" />
            ) : (
              <Mail size={18} aria-hidden="true" />
            )}
          </div>
          <p className="text-sm text-(--text-neutral-700)">
            {enrollment.codeSent
              ? `We sent a code to ${enrollment.maskedDestination ?? "your contact"}.`
              : "Check your messages for the verification code."}
          </p>
        </div>
      ) : null}

      {isEnrollment && enrollmentState.isLoading && enrollmentStarted ? (
        <div className="mt-5 flex min-h-20 items-center justify-center gap-3 rounded-xl border border-(--neutral-200) bg-(--neutral-25) p-4 text-sm text-(--text-neutral-600)">
          <LoadingDots label="Sending verification code" />
          <span>Sending verification code…</span>
        </div>
      ) : null}

      {!isEnrollment &&
      !needsVerifyMethodPick &&
      enrolledForVerify.length > 1 &&
      !useRecoveryCode ? (
        <Button
          type="button"
          variant="tertiary"
          className="mt-5 min-h-11 px-0"
          onClick={() => {
            setVerifyMethodChoice(null);
            setCode("");
            setLocalError(null);
          }}
        >
          Use a different method
        </Button>
      ) : null}

      {!isEnrollment &&
      !needsVerifyMethodPick &&
      isOtpVerify &&
      !useRecoveryCode ? (
        <Button
          type="button"
          variant="outline"
          size="lg"
          className="mt-5 w-full"
          disabled={resendCooldown > 0 || sendCodeState.isLoading}
          loading={sendCodeState.isLoading}
          loadingLabel="Sending code…"
          onClick={() => void handleResendCode()}
        >
          {resendCooldown > 0
            ? `Resend code in ${resendCooldown}s`
            : "Resend code"}
        </Button>
      ) : null}

      {!isEnrollment && !needsVerifyMethodPick ? (
        <Button
          type="button"
          variant="tertiary"
          className="mt-5 min-h-11 px-0"
          onClick={() => {
            setUseRecoveryCode((current) => !current);
            setCode("");
            setLocalError(null);
          }}
        >
          {useRecoveryCode
            ? verifyMethod === "TOTP"
              ? "Use an authenticator code"
              : "Use your verification code"
            : "Use a recovery code instead"}
        </Button>
      ) : null}

      {(isEnrollment && enrollmentStarted) ||
      (!isEnrollment && (!needsVerifyMethodPick || useRecoveryCode)) ? (
        <>
          <label className="mt-4 block">
            <span className="text-sm font-semibold text-(--text-primary-dark)">
              {useRecoveryCode ? "Recovery code" : "6-digit code"}
            </span>
            <Input
              autoFocus
              autoComplete="one-time-code"
              inputMode={useRecoveryCode ? "text" : "numeric"}
              disabled={isEnrollment && !enrollmentStarted}
              value={code}
              onChange={(event) =>
                setCode(
                  useRecoveryCode
                    ? event.target.value.toUpperCase()
                    : event.target.value.replace(/\D/g, "").slice(0, 6),
                )
              }
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  void (isEnrollment
                    ? handleConfirmEnrollment()
                    : handleVerify());
                }
              }}
              className={cn(
                "mt-2 h-14 rounded-xl bg-(--surface-white) px-4 text-center font-mono text-xl tracking-[0.3em] text-(--neutral-950) shadow-none",
                error
                  ? "border-(--status-denied) focus-visible:border-(--status-denied) focus-visible:ring-2 focus-visible:ring-(--status-overdue-light)"
                  : "border-(--neutral-200) focus-visible:border-(--text-primary-500) focus-visible:ring-2 focus-visible:ring-(--bg-primary-100)",
              )}
              aria-invalid={Boolean(error)}
              aria-describedby={error ? "mfa-error" : undefined}
            />
          </label>

          {error ? (
            <div
              id="mfa-error"
              role="alert"
              className="mt-3 rounded-xl border border-(--status-denied) bg-(--light-red) px-3 py-2 text-sm text-(--status-denied)"
            >
              {error}
            </div>
          ) : null}

          {allowTrustDevice ? (
            <label
              htmlFor="trust-this-device"
              className="mt-4 flex cursor-pointer items-start gap-2 text-sm text-(--text-neutral-700)"
            >
              <Checkbox
                id="trust-this-device"
                className="mt-0.5"
                checked={trustDevice}
                onCheckedChange={(checked) => setTrustDevice(Boolean(checked))}
              />
              <span>
                <span className="font-semibold text-(--text-primary-dark)">
                  Trust this device for 30 days
                </span>
                <span className="mt-0.5 block text-(--text-neutral-600)">
                  Skip the verification code on this browser until trust expires.
                </span>
              </span>
            </label>
          ) : null}

          <Button
            type="button"
            size="lg"
            className="mt-5 w-full"
            loading={isActionBusy}
            loadingLabel={
              isCompletingAuthentication
                ? "Signing in…"
                : isEnrollment
                  ? "Confirming verification…"
                  : "Verifying code…"
            }
            disabled={
              isBusy ||
              (useRecoveryCode
                ? normalizedCode.length < 8
                : normalizedCode.length !== 6) ||
              (isEnrollment && !enrollmentStarted)
            }
            onClick={() =>
              void (isEnrollment ? handleConfirmEnrollment() : handleVerify())
            }
          >
            {isEnrollment
              ? "Confirm and continue"
              : "Verify and continue"}
          </Button>
        </>
      ) : null}
      {toastMessage && (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      )}
    </section>
  );
};

export type { MfaFlow };
export default MfaChallengePanel;
