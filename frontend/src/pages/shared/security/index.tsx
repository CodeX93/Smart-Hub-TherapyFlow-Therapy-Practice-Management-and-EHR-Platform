import { useEffect, useMemo, useRef, useState } from "react";
import { Mail, MessageSquare, MonitorSmartphone, ShieldCheck, Smartphone } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import {
  useConfirmMfaChangeMutation,
  useGetAuthDevicesQuery,
  useGetAuthMeQuery,
  useGetAuthSessionsQuery,
  useGetMfaStatusQuery,
  useRevokeAllTrustedAuthDevicesMutation,
  useRevokeAuthDeviceMutation,
  useRevokeAuthSessionMutation,
  useRevokeOtherAuthSessionsMutation,
  useSendMfaSettingsCodeMutation,
  useStartMfaChangeMutation,
  type MfaMethod,
} from "@/store/api/authApi";
import { getAuthErrorMessage } from "@/utils/apiError";

const methodLabels: Record<MfaMethod, string> = {
  TOTP: "Authenticator app",
  SMS: "Text message",
  EMAIL: "Email",
};

const RESEND_COOLDOWN_SECONDS = 60;

type ChangeStep = "idle" | "verify-current" | "choose-method" | "confirm-new" | "recovery";

const SecuritySettingsPage = () => {
  const { data: authMe } = useGetAuthMeQuery();
  const {
    data: mfaStatus,
    isLoading: mfaLoading,
    refetch: refetchMfa,
  } = useGetMfaStatusQuery();
  const {
    data: sessionsData,
    isLoading: sessionsLoading,
    refetch: refetchSessions,
  } = useGetAuthSessionsQuery();
  const {
    data: devicesData,
    isLoading: devicesLoading,
    isError: devicesError,
    error: devicesQueryError,
    refetch: refetchDevices,
  } = useGetAuthDevicesQuery();

  const [sendSettingsCode, sendSettingsState] = useSendMfaSettingsCodeMutation();
  const [startChange, startChangeState] = useStartMfaChangeMutation();
  const [confirmChange, confirmChangeState] = useConfirmMfaChangeMutation();
  const [revokeSession, revokeSessionState] = useRevokeAuthSessionMutation();
  const [revokeOthers, revokeOthersState] = useRevokeOtherAuthSessionsMutation();
  const [revokeDevice, revokeDeviceState] = useRevokeAuthDeviceMutation();
  const [revokeAllTrusted, revokeAllTrustedState] =
    useRevokeAllTrustedAuthDevicesMutation();

  const [step, setStep] = useState<ChangeStep>("idle");
  const [currentCode, setCurrentCode] = useState("");
  const [selectedMethod, setSelectedMethod] = useState<MfaMethod>("TOTP");
  const [phone, setPhone] = useState("");
  const [newCode, setNewCode] = useState("");
  const [pendingSecret, setPendingSecret] = useState<string | null>(null);
  const [pendingUri, setPendingUri] = useState<string | null>(null);
  const [pendingMasked, setPendingMasked] = useState<string | null>(null);
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [settingsCodeDestination, setSettingsCodeDestination] = useState<string | null>(null);
  const [settingsCodeMethod, setSettingsCodeMethod] = useState<MfaMethod | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);
  const resendTimerRef = useRef<number | null>(null);

  const preferredMethod = (mfaStatus?.method as MfaMethod | undefined) ?? "TOTP";
  const enrolledMethods: MfaMethod[] =
    mfaStatus?.enrolledMethods && mfaStatus.enrolledMethods.length > 0
      ? mfaStatus.enrolledMethods
      : mfaStatus?.enabled
        ? [preferredMethod]
        : [];
  const canAddMore =
    enrolledMethods.length < 3 &&
    (mfaStatus?.smsAvailable !== false ||
      mfaStatus?.emailAvailable !== false ||
      !enrolledMethods.includes("TOTP"));

  const usesCurrentOtpChannel =
    (enrolledMethods.includes("SMS") || enrolledMethods.includes("EMAIL")) &&
    !enrolledMethods.includes("TOTP");

  const emailDisplay =
    (preferredMethod === "EMAIL" && mfaStatus?.maskedDestination) ||
    authMe?.email?.trim() ||
    authMe?.username?.trim() ||
    null;

  /** Matches backend sendSettingsCode channel selection (preferred OTP, else SMS, else EMAIL). */
  const resolveSettingsOtpChannel = (): MfaMethod | null => {
    if (preferredMethod === "SMS" || preferredMethod === "EMAIL") {
      return preferredMethod;
    }
    if (enrolledMethods.includes("SMS")) return "SMS";
    if (enrolledMethods.includes("EMAIL")) return "EMAIL";
    return null;
  };

  const destinationForOtpChannel = (channel: MfaMethod | null): string | null => {
    if (channel === "SMS") {
      return preferredMethod === "SMS"
        ? mfaStatus?.maskedDestination ?? null
        : null;
    }
    if (channel === "EMAIL") {
      return emailDisplay;
    }
    return null;
  };

  const addableMethods = (
    [
      {
        method: "TOTP" as const,
        icon: Smartphone,
        description: "Use Google Authenticator, 1Password, or similar apps.",
        available: true,
      },
      {
        method: "SMS" as const,
        icon: MessageSquare,
        description: "Receive a one-time code by text message.",
        available: mfaStatus?.smsAvailable !== false,
      },
      {
        method: "EMAIL" as const,
        icon: Mail,
        description: "Receive a one-time code by email.",
        available: mfaStatus?.emailAvailable !== false,
      },
    ] as const
  ).filter(
    (option) => option.available && !enrolledMethods.includes(option.method),
  );

  const error =
    localError ||
    (devicesError ? getAuthErrorMessage(devicesQueryError) : null) ||
    (sendSettingsState.isError ? getAuthErrorMessage(sendSettingsState.error) : null) ||
    (startChangeState.isError ? getAuthErrorMessage(startChangeState.error) : null) ||
    (confirmChangeState.isError ? getAuthErrorMessage(confirmChangeState.error) : null);

  const otherSessions = useMemo(
    () => (sessionsData?.sessions ?? []).filter((s) => !s.current),
    [sessionsData],
  );

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

  const enrolledMethodDetail = (method: MfaMethod): string | null => {
    if (method === "EMAIL") {
      return emailDisplay;
    }
    if (method === "SMS") {
      return preferredMethod === "SMS" ? mfaStatus?.maskedDestination ?? null : null;
    }
    return null;
  };

  const resetChangeFlow = () => {
    setStep("idle");
    setCurrentCode("");
    setNewCode("");
    setPhone("");
    setPendingSecret(null);
    setPendingUri(null);
    setPendingMasked(null);
    setSettingsCodeDestination(null);
    setSettingsCodeMethod(null);
    setResendCooldown(0);
    setLocalError(null);
  };

  const beginAddMethod = async () => {
    setLocalError(null);
    setNotice(null);
    setCurrentCode("");
    setSettingsCodeDestination(null);
    setSettingsCodeMethod(null);
    setResendCooldown(0);
    if (addableMethods.length > 0) {
      setSelectedMethod(addableMethods[0].method);
    }
    setStep("choose-method");
  };

  const dispatchSettingsCode = async (startCooldown: boolean) => {
    const expectedChannel = resolveSettingsOtpChannel();
    if (expectedChannel) {
      setSettingsCodeMethod(expectedChannel);
      const expectedDestination = destinationForOtpChannel(expectedChannel);
      if (expectedDestination) {
        setSettingsCodeDestination(expectedDestination);
      }
    }

    const response = await sendSettingsCode().unwrap();
    if (response.sent) {
      const responseMethod =
        response.method === "SMS" || response.method === "EMAIL"
          ? response.method
          : expectedChannel;
      if (responseMethod) {
        setSettingsCodeMethod(responseMethod);
      }
      setSettingsCodeDestination(
        response.maskedDestination?.trim() ||
          destinationForOtpChannel(responseMethod) ||
          null,
      );
      if (startCooldown) {
        setResendCooldown(RESEND_COOLDOWN_SECONDS);
      }
    }
  };

  const continueToVerifyCurrent = () => {
    if (selectedMethod === "SMS" && !phone.trim()) {
      setLocalError("Enter your mobile number including country code (e.g. +14155551234).");
      return;
    }
    setLocalError(null);
    setCurrentCode("");
    setStep("verify-current");
    if (usesCurrentOtpChannel) {
      void dispatchSettingsCode(true).catch((err) => {
        setLocalError(getAuthErrorMessage(err));
      });
    }
  };

  const handleResendSettingsCode = async () => {
    if (resendCooldown > 0 || sendSettingsState.isLoading) return;
    setLocalError(null);
    try {
      await dispatchSettingsCode(true);
    } catch (err) {
      setLocalError(getAuthErrorMessage(err));
    }
  };

  const startNewMethod = async () => {
    if (currentCode.trim().length < 6) {
      setLocalError("Enter your current verification code or a recovery code.");
      return;
    }
    setLocalError(null);
    try {
      const response = await startChange({
        currentCode: currentCode.trim(),
        method: selectedMethod,
        phone: selectedMethod === "SMS" ? phone.trim() : undefined,
      }).unwrap();
      setPendingSecret(response.secret ?? null);
      setPendingUri(response.provisioningUri ?? null);
      setPendingMasked(response.maskedDestination ?? null);
      setStep("confirm-new");
    } catch {
      // surfaced via mutation state
    }
  };

  const finishChange = async () => {
    if (newCode.trim().length !== 6) {
      setLocalError("Enter the 6-digit code for the new method.");
      return;
    }
    setLocalError(null);
    try {
      const response = await confirmChange({ code: newCode.trim() }).unwrap();
      if (response.recoveryCodes?.length) {
        setRecoveryCodes(response.recoveryCodes);
        setStep("recovery");
      } else {
        resetChangeFlow();
        setNotice(`${methodLabels[selectedMethod]} is now available at sign-in.`);
      }
      await refetchMfa();
      await refetchSessions();
    } catch {
      // surfaced via mutation state
    }
  };

  const activeOtpChannel = settingsCodeMethod ?? resolveSettingsOtpChannel();
  const currentChannelLabel = activeOtpChannel
    ? methodLabels[activeOtpChannel]
    : enrolledMethods.includes("TOTP")
      ? methodLabels.TOTP
      : "your current method";
  const currentChannelDestination =
    settingsCodeDestination || destinationForOtpChannel(activeOtpChannel);
  const currentChannelKindLabel =
    activeOtpChannel === "SMS"
      ? "phone number"
      : activeOtpChannel === "EMAIL"
        ? "email"
        : currentChannelLabel.toLowerCase();

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 py-2">
      {notice ? (
        <div className="rounded-xl border border-(--border-success) bg-(--bg-success-light) px-3 py-2 text-sm text-(--dark-green)">
          {notice}
        </div>
      ) : null}
      {error ? (
        <div className="rounded-xl border border-[#f3d4d4] bg-[#fff5f5] px-3 py-2 text-sm text-(--status-denied)">
          {error}
        </div>
      ) : null}

      <section className="overflow-hidden rounded-2xl border border-(--neutral-100) bg-(--surface-white)">
        <div className="flex flex-col gap-4 border-b border-(--neutral-100) px-5 py-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex min-w-0 items-start gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-(--bg-primary-50) text-(--text-primary-dark)">
              <ShieldCheck size={18} aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-base font-semibold text-(--text-primary-dark)">
                  Multi-factor authentication
                </h2>
                {!mfaLoading ? (
                  <span
                    className={cn(
                      "inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold tracking-wide",
                      mfaStatus?.enabled
                        ? "bg-(--bg-success-light) text-(--dark-green)"
                        : "bg-(--neutral-100) text-(--text-neutral-600)",
                    )}
                  >
                    {mfaStatus?.enabled ? "On" : "Off"}
                  </span>
                ) : null}
              </div>
              <p className="mt-1 text-sm leading-5 text-(--text-neutral-600)">
                Enable multiple methods and use any of them at sign-in.
              </p>
            </div>
          </div>

          {!mfaLoading &&
          mfaStatus?.enabled &&
          step === "idle" &&
          canAddMore &&
          addableMethods.length > 0 ? (
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="shrink-0 self-start"
              onClick={() => void beginAddMethod()}
            >
              Add method
            </Button>
          ) : null}
        </div>

        <div className="px-5 py-5">
          {mfaLoading ? (
            <p className="text-sm text-(--text-neutral-600)">Loading MFA status…</p>
          ) : (
            <>
              {enrolledMethods.length > 0 ? (
                <ul className="space-y-2">
                  {enrolledMethods.map((method) => {
                    const detail = enrolledMethodDetail(method);
                    const MethodIcon =
                      method === "SMS" ? MessageSquare : method === "EMAIL" ? Mail : Smartphone;
                    return (
                      <li
                        key={method}
                        className="flex items-center gap-3 rounded-xl border border-(--neutral-100) bg-(--bg-primary-light) px-4 py-3.5"
                      >
                        <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-(--surface-white) text-(--text-primary-dark) shadow-xs">
                          <MethodIcon size={17} aria-hidden="true" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-sm font-semibold text-(--text-primary-dark)">
                              {methodLabels[method]}
                            </span>
                            {method === preferredMethod ? (
                              <span className="rounded-full border border-(--neutral-100) bg-(--surface-white) px-2 py-0.5 text-[11px] font-medium text-(--text-neutral-600)">
                                Default
                              </span>
                            ) : null}
                          </div>
                          {detail ? (
                            <p className="mt-0.5 truncate text-sm text-(--text-neutral-600)">
                              {detail}
                            </p>
                          ) : (
                            <p className="mt-0.5 text-sm text-(--text-neutral-600)">
                              Ready for sign-in
                            </p>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <div className="rounded-xl border border-dashed border-(--neutral-100) bg-(--bg-primary-light) px-4 py-6 text-center">
                  <p className="text-sm text-(--text-neutral-600)">No methods enrolled yet</p>
                </div>
              )}

              <div className="mt-4 flex items-center justify-between gap-3 border-t border-(--neutral-100) pt-4">
                <p className="text-sm text-(--text-neutral-600)">
                  Recovery codes left
                </p>
                <span className="rounded-full bg-(--neutral-100) px-2.5 py-0.5 text-xs font-semibold text-(--text-primary-dark)">
                  {mfaStatus?.unusedRecoveryCodes ?? 0}
                </span>
              </div>

              {step === "choose-method" ? (
                <div className="mt-4 space-y-4 rounded-xl border border-(--neutral-100) p-4">
                  <p className="text-sm font-semibold text-(--text-primary-dark)">
                    Choose a method to add
                  </p>
                  {addableMethods.length === 0 ? (
                    <p className="text-sm text-(--text-neutral-600)">
                      All available methods are already set up.
                    </p>
                  ) : (
                    <div className="space-y-2">
                      {addableMethods.map(({ method: option, icon: Icon, description }) => (
                        <button
                          key={option}
                          type="button"
                          className={cn(
                            "flex w-full items-start gap-3 rounded-xl border px-3 py-3 text-left transition-colors",
                            selectedMethod === option
                              ? "border-(--text-primary-500) bg-(--bg-primary-50)"
                              : "border-(--neutral-100) hover:bg-(--bg-primary-light)",
                          )}
                          onClick={() => setSelectedMethod(option)}
                        >
                          <Icon size={18} className="mt-0.5 shrink-0 text-(--text-primary-dark)" />
                          <span>
                            <span className="block text-sm font-semibold text-(--text-primary-dark)">
                              {methodLabels[option]}
                            </span>
                            <span className="mt-0.5 block text-xs text-(--text-neutral-600)">
                              {description}
                            </span>
                          </span>
                        </button>
                      ))}
                    </div>
                  )}
                  {selectedMethod === "SMS" ? (
                    <label className="block space-y-2">
                      <span className="text-sm font-semibold text-(--text-primary-dark)">
                        Mobile number
                      </span>
                      <Input
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="Mobile number (+1…)"
                        inputMode="tel"
                        autoComplete="tel"
                        className="h-11"
                      />
                      <span className="block text-xs text-(--text-neutral-600)">
                        Include country code, for example +14155551234.
                      </span>
                    </label>
                  ) : null}
                  <div className="flex flex-wrap items-center gap-3 pt-1">
                    <Button
                      type="button"
                      className="min-w-28"
                      disabled={addableMethods.length === 0}
                      onClick={continueToVerifyCurrent}
                    >
                      Continue
                    </Button>
                    <Button type="button" variant="ghost" onClick={resetChangeFlow}>
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : null}

              {step === "verify-current" ? (
                <div className="mt-4 space-y-4 rounded-xl border border-(--neutral-100) p-4">
                  <div className="space-y-1.5">
                    <p className="text-sm font-semibold text-(--text-primary-dark)">
                      Confirm your identity
                    </p>
                    <p className="text-sm leading-relaxed text-(--text-neutral-600)">
                      {usesCurrentOtpChannel ? (
                        <>
                          To add{" "}
                          <span className="font-medium text-(--text-primary-dark)">
                            {methodLabels[selectedMethod]}
                          </span>
                          , we sent a verification code to your{" "}
                          <span className="font-medium text-(--text-primary-dark)">
                            {currentChannelKindLabel}
                          </span>
                          {currentChannelDestination ? (
                            <>
                              {" "}
                              (
                              <span className="font-medium text-(--text-primary-dark)">
                                {currentChannelDestination}
                              </span>
                              )
                            </>
                          ) : null}
                          . Enter that code below to continue
                          {selectedMethod === "SMS" && phone.trim()
                            ? `, then we’ll send a code to ${phone.trim()}`
                            : ""}
                          . You can also use a recovery code.
                        </>
                      ) : (
                        <>
                          Confirm with your authenticator app (or a recovery code) to securely add{" "}
                          <span className="font-medium text-(--text-primary-dark)">
                            {methodLabels[selectedMethod]}
                          </span>
                          {selectedMethod === "SMS" && phone.trim()
                            ? ` (${phone.trim()})`
                            : ""}
                          .
                        </>
                      )}
                    </p>
                  </div>

                  <label className="block space-y-2">
                    <span className="text-sm font-semibold text-(--text-primary-dark)">
                      Verification code
                    </span>
                    <Input
                      value={currentCode}
                      onChange={(e) => setCurrentCode(e.target.value)}
                      placeholder="6-digit code or recovery code"
                      autoComplete="one-time-code"
                      autoFocus
                      className="h-11"
                    />
                  </label>

                  <div className="flex flex-wrap items-center gap-3">
                    <Button
                      type="button"
                      className="min-w-40"
                      loading={startChangeState.isLoading}
                      onClick={() => void startNewMethod()}
                    >
                      {selectedMethod === "SMS" || selectedMethod === "EMAIL"
                        ? "Verify and send code"
                        : "Verify and continue"}
                    </Button>
                    {usesCurrentOtpChannel ? (
                      <Button
                        type="button"
                        variant="outline"
                        className="min-w-40"
                        disabled={resendCooldown > 0 || sendSettingsState.isLoading}
                        loading={sendSettingsState.isLoading}
                        loadingLabel="Sending…"
                        onClick={() => void handleResendSettingsCode()}
                      >
                        {resendCooldown > 0
                          ? `Resend code in ${resendCooldown}s`
                          : "Resend code"}
                      </Button>
                    ) : null}
                    <Button type="button" variant="ghost" onClick={resetChangeFlow}>
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : null}

              {step === "confirm-new" ? (
                <div className="mt-4 space-y-4 rounded-xl border border-(--neutral-100) p-4">
                  {pendingUri ? (
                    <div className="flex justify-center rounded-xl bg-white p-3">
                      <QRCodeSVG value={pendingUri} size={160} />
                    </div>
                  ) : null}
                  {pendingSecret ? (
                    <p className="break-all font-mono text-xs text-(--text-neutral-600)">
                      Setup key: {pendingSecret}
                    </p>
                  ) : null}
                  <p className="text-sm text-(--text-neutral-600)">
                    {pendingMasked
                      ? `Enter the code sent to ${pendingMasked}.`
                      : "Enter a code from your authenticator app."}
                  </p>
                  <label className="block space-y-2">
                    <span className="text-sm font-semibold text-(--text-primary-dark)">
                      6-digit code
                    </span>
                    <Input
                      value={newCode}
                      onChange={(e) => setNewCode(e.target.value)}
                      placeholder="6-digit code"
                      autoComplete="one-time-code"
                      className="h-11"
                    />
                  </label>
                  <div className="flex flex-wrap items-center gap-3">
                    <Button
                      type="button"
                      className="min-w-36"
                      loading={confirmChangeState.isLoading}
                      onClick={() => void finishChange()}
                    >
                      Confirm method
                    </Button>
                    <Button type="button" variant="ghost" onClick={resetChangeFlow}>
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : null}

              {step === "recovery" && recoveryCodes ? (
                <div className="mt-4 space-y-3 rounded-xl border border-(--border-success) bg-(--bg-success-light) p-4">
                  <p className="text-sm font-semibold text-(--dark-green)">
                    {methodLabels[selectedMethod]} MFA is set up. Save these recovery codes.
                  </p>
                  <div className="grid grid-cols-1 gap-1 font-mono text-sm sm:grid-cols-2">
                    {recoveryCodes.map((code) => (
                      <span key={code}>{code}</span>
                    ))}
                  </div>
                  <Button
                    type="button"
                    onClick={() => {
                      setRecoveryCodes(null);
                      resetChangeFlow();
                      setNotice(`${methodLabels[selectedMethod]} MFA is now active.`);
                    }}
                  >
                    Done
                  </Button>
                </div>
              ) : null}
            </>
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-(--neutral-100) bg-(--surface-white)">
        <div className="border-b border-(--neutral-100) px-5 py-4">
          <div className="flex items-center gap-2 text-base font-semibold text-(--text-primary-dark)">
            <MonitorSmartphone size={18} />
            Signed-in devices
          </div>
          <p className="mt-1 text-sm text-(--text-neutral-600)">
            {sessionsLoading
              ? "Loading sessions…"
              : `Signed in on ${sessionsData?.activeCount ?? 0} device${
                  (sessionsData?.activeCount ?? 0) === 1 ? "" : "s"
                }.`}
          </p>
        </div>

        <div className="divide-y divide-(--neutral-100)">
          {(sessionsData?.sessions ?? []).map((session) => (
            <div
              key={session.id}
              className="flex flex-wrap items-center justify-between gap-3 px-5 py-4"
            >
              <div>
                <div className="text-sm font-medium text-(--text-primary-dark)">
                  {session.deviceLabel}
                  {session.current ? (
                    <span className="ml-2 rounded-full bg-(--bg-primary-50) px-2 py-0.5 text-xs text-(--text-primary-500)">
                      This device
                    </span>
                  ) : null}
                </div>
                <div className="mt-1 text-xs text-(--text-neutral-600)">
                  {session.ipAddress && session.ipAddress.toLowerCase() !== "unknown"
                    ? session.ipAddress
                    : "IP pending"}
                  {session.lastActivityAt
                    ? ` · Last active ${new Date(session.lastActivityAt).toLocaleString()}`
                    : ""}
                </div>
              </div>
              {!session.current ? (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  loading={revokeSessionState.isLoading}
                  onClick={async () => {
                    setLocalError(null);
                    try {
                      await revokeSession(session.id).unwrap();
                      await refetchSessions();
                      setNotice("Signed out device.");
                    } catch (err) {
                      setLocalError(getAuthErrorMessage(err));
                    }
                  }}
                >
                  Sign out
                </Button>
              ) : null}
            </div>
          ))}
        </div>

        {otherSessions.length > 0 ? (
          <div className="border-t border-(--neutral-100) px-5 py-4">
            <Button
              type="button"
              variant="outline"
              loading={revokeOthersState.isLoading}
              onClick={async () => {
                setLocalError(null);
                try {
                  await revokeOthers().unwrap();
                  await refetchSessions();
                  setNotice("Signed out all other devices.");
                } catch (err) {
                  setLocalError(getAuthErrorMessage(err));
                }
              }}
            >
              Sign out all other devices
            </Button>
          </div>
        ) : null}
      </section>

      <section className="rounded-2xl border border-(--neutral-100) bg-(--surface-white)">
        <div className="border-b border-(--neutral-100) px-5 py-4">
          <div className="flex items-center gap-2 text-base font-semibold text-(--text-primary-dark)">
            <ShieldCheck size={18} />
            Trusted devices
          </div>
          <p className="mt-1 text-sm text-(--text-neutral-600)">
            {devicesLoading
              ? "Loading trusted devices…"
              : `${devicesData?.trustedCount ?? 0} trusted device${
                  (devicesData?.trustedCount ?? 0) === 1 ? "" : "s"
                } can skip MFA for up to 30 days.`}
          </p>
        </div>

        <div className="divide-y divide-(--neutral-100)">
          {(devicesData?.devices ?? [])
            .filter((device) => device.trusted)
            .map((device) => (
              <div
                key={device.id}
                className="flex flex-wrap items-center justify-between gap-3 px-5 py-4"
              >
                <div>
                  <div className="text-sm font-medium text-(--text-primary-dark)">
                    {device.deviceLabel}
                    {device.current ? (
                      <span className="ml-2 rounded-full bg-(--bg-primary-50) px-2 py-0.5 text-xs text-(--text-primary-500)">
                        This device
                      </span>
                    ) : null}
                  </div>
                  <div className="mt-1 text-xs text-(--text-neutral-600)">
                    {device.ipAddress || "IP unknown"}
                    {device.trustExpiresAt
                      ? ` · Trust expires ${new Date(device.trustExpiresAt).toLocaleString()}`
                      : ""}
                  </div>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  loading={revokeDeviceState.isLoading}
                  onClick={async () => {
                    setLocalError(null);
                    try {
                      await revokeDevice(device.id).unwrap();
                      await refetchDevices();
                      setNotice("Device trust revoked.");
                    } catch (err) {
                      setLocalError(getAuthErrorMessage(err));
                    }
                  }}
                >
                  Revoke trust
                </Button>
              </div>
            ))}
          {!devicesLoading && (devicesData?.trustedCount ?? 0) === 0 ? (
            <div className="px-5 py-4 text-sm text-(--text-neutral-600)">
              No trusted devices yet. Choose “Trust this device” after MFA on your next sign-in.
            </div>
          ) : null}
        </div>

        {(devicesData?.trustedCount ?? 0) > 0 ? (
          <div className="border-t border-(--neutral-100) px-5 py-4">
            <Button
              type="button"
              variant="outline"
              loading={revokeAllTrustedState.isLoading}
              onClick={async () => {
                setLocalError(null);
                try {
                  await revokeAllTrusted().unwrap();
                  await refetchDevices();
                  setNotice("All device trusts revoked.");
                } catch (err) {
                  setLocalError(getAuthErrorMessage(err));
                }
              }}
            >
              Revoke all trusted devices
            </Button>
          </div>
        ) : null}
      </section>
    </div>
  );
};

export default SecuritySettingsPage;
