
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { AlertTriangle, MessageSquare, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import Toast from "@/components/shared/Toast";
import {
  type AdminClientConsentHistoryItem,
  useGetAdminClientConsentHistoryQuery,
  useRecordAdminClientConsentMutation,
} from "@/store/api/admin/consents.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { cn } from "@/lib/utils";

interface ConsentsTabProps {
  clientId?: number | string | null;
}

type ToastState = {
  message: string;
  type: "success" | "error";
} | null;

const AI_CONSENT_TYPE = "AI Processing Consent";
const SMS_CONSENT_TYPE = "SMS Communication";

const normalizeConsentType = (value: string) =>
  value.replace(/[^a-z0-9]/gi, "").toLowerCase();

const isAiConsent = (consentType: string) => {
  const normalized = normalizeConsentType(consentType);
  return normalized.includes("aiprocessing") || normalized.includes("aiconsent");
};

const isSmsConsent = (consentType: string) => {
  const normalized = normalizeConsentType(consentType);
  return normalized.includes("sms") || normalized.includes("textnotification");
};

const getConsentTime = (consent: AdminClientConsentHistoryItem) => {
  const value =
    consent.updatedAt || consent.createdAt || consent.grantedAt || consent.withdrawnAt || "";
  const time = value ? new Date(value).getTime() : 0;
  return Number.isFinite(time) ? time : 0;
};

const getLatestConsent = (
  consents: AdminClientConsentHistoryItem[] | undefined,
  matcher: (consentType: string) => boolean,
) => {
  return [...(consents ?? [])]
    .filter((consent) => matcher(consent.consentType))
    .sort((a, b) => getConsentTime(b) - getConsentTime(a))[0];
};

const isConsentGranted = (consent?: AdminClientConsentHistoryItem) =>
  Boolean(consent?.granted && !consent.withdrawnAt);

const ConsentStatusPill = ({
  granted,
  grantedText,
  blockedText,
}: {
  granted: boolean;
  grantedText: string;
  blockedText: string;
}) => (
  <span
    className={cn(
      "inline-flex max-w-full items-center gap-1 rounded-full px-3 py-1 text-sm font-semibold",
      granted ? "bg-emerald-50 text-emerald-700" : "bg-neutral-100 text-neutral-900",
    )}
  >
    {granted ? <ShieldCheck size={15} /> : <AlertTriangle size={15} />}
    <span className="truncate">{granted ? grantedText : blockedText}</span>
  </span>
);

const ConsentsTab = ({ clientId }: ConsentsTabProps) => {
  const numericClientId = Number(clientId);
  const hasClientId = Number.isFinite(numericClientId) && numericClientId > 0;
  const {
    data: consents,
    isFetching,
    isError,
    error,
    refetch,
  } = useGetAdminClientConsentHistoryQuery(numericClientId, {
    skip: !hasClientId,
    refetchOnMountOrArgChange: true,
  });
  const [recordConsent] = useRecordAdminClientConsentMutation();
  const [aiGranted, setAiGranted] = useState(false);
  const [smsGranted, setSmsGranted] = useState(false);
  const [aiNotes, setAiNotes] = useState("");
  const [smsNotes, setSmsNotes] = useState("");
  const [savingConsentType, setSavingConsentType] = useState<"ai" | "sms" | null>(null);
  const [toast, setToast] = useState<ToastState>(null);

  const latestAiConsent = useMemo(
    () => getLatestConsent(consents, isAiConsent),
    [consents],
  );
  const latestSmsConsent = useMemo(
    () => getLatestConsent(consents, isSmsConsent),
    [consents],
  );

  useEffect(() => {
    setAiGranted(isConsentGranted(latestAiConsent));
    setAiNotes(latestAiConsent?.notes ?? "");
  }, [latestAiConsent]);

  useEffect(() => {
    setSmsGranted(isConsentGranted(latestSmsConsent));
    setSmsNotes(latestSmsConsent?.notes ?? "");
  }, [latestSmsConsent]);

  const initialAiGranted = isConsentGranted(latestAiConsent);
  const initialSmsGranted = isConsentGranted(latestSmsConsent);
  const initialAiNotes = latestAiConsent?.notes ?? "";
  const initialSmsNotes = latestSmsConsent?.notes ?? "";
  const aiHasChanges = aiGranted !== initialAiGranted || aiNotes.trim() !== initialAiNotes.trim();
  const smsHasChanges =
    smsGranted !== initialSmsGranted || smsNotes.trim() !== initialSmsNotes.trim();

  const showToast = (message: string, type: "success" | "error") => {
    setToast({ message, type });
  };

  useEffect(() => {
    if (!isError || !error) return;
    showToast(getApiErrorMessage(error), "error");
  }, [isError, error]);

  const handleSaveConsent = async (type: "ai" | "sms") => {
    if (!hasClientId) {
      showToast("Client ID is required to record consent.", "error");
      return;
    }

    const isAi = type === "ai";
    const granted = isAi ? aiGranted : smsGranted;
    const notes = (isAi ? aiNotes : smsNotes).trim();

    try {
      setSavingConsentType(type);
      await recordConsent({
        clientId: numericClientId,
        consentType: isAi ? AI_CONSENT_TYPE : SMS_CONSENT_TYPE,
        granted,
        consentVersion: "1.0",
        source: isAi ? "signed_consent_form" : "client_approved_by_phone_or_in_person",
        notes,
        auditReason: isAi
          ? "Staff recorded AI processing consent from signed consent form."
          : "Staff recorded SMS appointment notification consent.",
      }).unwrap();
      showToast(granted ? "Consent recorded successfully." : "Consent withdrawn successfully.", "success");
      void refetch();
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
    } finally {
      setSavingConsentType(null);
    }
  };

  const renderConsentCard = ({
    id,
    icon,
    statusTitle,
    granted,
    grantedText,
    blockedText,
    checkboxTitle,
    checkboxDescription,
    notes,
    placeholder,
    onGrantedChange,
    onNotesChange,
    onSave,
    hasChanges,
    isSaving,
  }: {
    id: "ai" | "sms";
    icon: ReactNode;
    statusTitle: string;
    granted: boolean;
    grantedText: string;
    blockedText: string;
    checkboxTitle: string;
    checkboxDescription: string;
    notes: string;
    placeholder: string;
    onGrantedChange: (checked: boolean) => void;
    onNotesChange: (value: string) => void;
    onSave: () => void;
    hasChanges: boolean;
    isSaving: boolean;
  }) => (
    <div className="space-y-5">
      <div className="flex items-start justify-between gap-4 rounded-2xl border border-neutral-200 bg-neutral-50 px-5 py-4">
        <div className="min-w-0 space-y-2">
          <h4 className="text-base font-semibold text-slate-700">{statusTitle}</h4>
          <ConsentStatusPill
            granted={granted}
            grantedText={grantedText}
            blockedText={blockedText}
          />
        </div>
        <span className="shrink-0 text-slate-400">{icon}</span>
      </div>

      <div className="rounded-2xl border border-neutral-200 bg-white px-5 py-5">
        <div className="flex items-start gap-4">
          <Checkbox
            id={`${id}-consent`}
            checked={granted}
            onCheckedChange={onGrantedChange}
            className="mt-1 h-5 w-5 border-(--neutral-200)"
          />
          <div className="min-w-0 flex-1">
            <label
              htmlFor={`${id}-consent`}
              className="block cursor-pointer text-base font-semibold text-neutral-950"
            >
              {checkboxTitle}
            </label>
            <p className="mt-2 text-sm leading-6 text-slate-500">{checkboxDescription}</p>
          </div>
        </div>

        <label
          htmlFor={`${id}-consent-notes`}
          className="mt-6 block text-base font-semibold text-neutral-950"
        >
          Notes (optional)
        </label>
        <textarea
          id={`${id}-consent-notes`}
          value={notes}
          onChange={(event) => onNotesChange(event.target.value)}
          placeholder={placeholder}
          maxLength={500}
          className="mt-3 min-h-24 w-full resize-y rounded-xl border border-neutral-200 px-4 py-3 text-sm text-slate-700 outline-none transition focus:border-(--bg-primary-dark) focus:ring-2 focus:ring-slate-200"
        />
        <div className="mt-4 flex justify-end">
          <Button
            type="button"
            onClick={onSave}
            disabled={Boolean(savingConsentType) || isFetching || !hasClientId || !hasChanges}
            className="h-11 rounded-xl bg-(--bg-primary-dark) px-6 text-white hover:bg-(--bg-primary-dark)/90 disabled:cursor-not-allowed disabled:opacity-60"
            loading={isSaving}
            loadingLabel="Saving consent..."
          >

            {granted ? "Record Consent" : "Withdraw Consent"}
          </Button>
        </div>
      </div>
    </div>
  );

  if (!hasClientId) {
    return (
      <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        Save the client before recording consents.
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h3 className="text-2xl font-semibold text-slate-900">Privacy & Consent</h3>
        <p className="mt-2 text-base text-slate-500">
          Record consent on behalf of the client when they sign the in-clinic consent form.
        </p>
      </div>

      {isFetching && !consents ? (
        <ContentLoader size="lg" className="min-h-40 rounded-2xl border border-neutral-200" />
      ) : (
        <>
          {renderConsentCard({
            id: "ai",
            icon: <ShieldCheck size={28} />,
            statusTitle: "Current AI processing consent",
            granted: aiGranted,
            grantedText: "Recorded - AI enabled",
            blockedText: "Not recorded - AI blocked",
            checkboxTitle: "AI processing consent (recorded from signed consent form)",
            checkboxDescription:
              "Tick this only after the client has signed the consent form that explicitly authorizes AI-assisted clinical documentation. Untick to withdraw consent.",
            notes: aiNotes,
            placeholder: "e.g., Signed consent form on 2026-04-22 at intake appointment",
            onGrantedChange: setAiGranted,
            onNotesChange: setAiNotes,
            onSave: () => handleSaveConsent("ai"),
            hasChanges: aiHasChanges,
            isSaving: savingConsentType === "ai",
          })}

          {renderConsentCard({
            id: "sms",
            icon: <MessageSquare size={28} />,
            statusTitle: "Current SMS notification consent",
            granted: smsGranted,
            grantedText: "Recorded - texts enabled",
            blockedText: "Not recorded - no texts sent",
            checkboxTitle: "SMS appointment notifications (client approved by phone/in person)",
            checkboxDescription:
              "Tick this only after the client has explicitly approved receiving appointment text messages (booking confirmations and reminders) to their mobile number. SMS is off by default. Untick to stop all texts.",
            notes: smsNotes,
            placeholder: "e.g., Client verbally approved SMS reminders at intake on 2026-04-22",
            onGrantedChange: setSmsGranted,
            onNotesChange: setSmsNotes,
            onSave: () => handleSaveConsent("sms"),
            hasChanges: smsHasChanges,
            isSaving: savingConsentType === "sms",
          })}
        </>
      )}

      {toast ? (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      ) : null}
    </div>
  );
};

export default ConsentsTab;
