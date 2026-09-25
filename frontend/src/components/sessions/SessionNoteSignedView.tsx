import { Eye, Lock } from "lucide-react";

import type { SessionNoteAmendment } from "@/store/api/admin/sessionNotes.api";

import { RISK_ASSESSMENT_ITEMS } from "@/constants/riskAssessmentItems";
import { cn } from "@/lib/utils";
import {
  RISK_BAND_LABELS,
  summarizeRiskAssessment,
  type RiskBand,
} from "@/utils/sessionNoteRiskScore";
import {
  formatSignedAt,
  NOT_ASSESSED,
  NOT_RECORDED,
  toSignedRiskRows,
  toSignedSections,
} from "@/utils/sessionNoteSignedView";
import { sanitizeHtml } from "@/utils/sanitizeHtml";

const BAND_TEXT_CLASSES: Record<RiskBand, string> = {
  low: "text-(--status-completed-dark)",
  moderate: "text-(--status-pending-dark)",
  high: "text-(--status-pending-dark)",
  severe: "text-(--status-overdue-dark)",
};

export interface SessionNoteSignedViewProps {
  note: {
    sessionFocus?: string | null;
    symptoms?: string | null;
    shortTermGoals?: string | null;
    intervention?: string | null;
    progress?: string | null;
    remarks?: string | null;
    recommendations?: string | null;
    finalContent?: string | null;
    generatedContent?: string | null;
    draftContent?: string | null;
    therapistName?: string | null;
    finalizedAt?: string | null;
    riskSuicidalIdeation?: number | null;
    riskSelfHarm?: number | null;
    riskHomicidalIdeation?: number | null;
    riskPsychosis?: number | null;
    riskSubstanceUse?: number | null;
    riskImpulsivity?: number | null;
    riskAggression?: number | null;
    riskNonAdherence?: number | null;
  };
  practiceTimezone?: string | null;
  amendments?: SessionNoteAmendment[];
  /** Show the unsigned note as it will read once finalized. */
  preview?: boolean;
}

const SessionNoteSignedView = ({
  note,
  practiceTimezone,
  amendments = [],
  preview = false,
}: SessionNoteSignedViewProps) => {
  const sections = toSignedSections([
    { label: "Session focus", value: note.sessionFocus },
    { label: "Symptoms", value: note.symptoms },
    { label: "Progress", value: note.progress },
    { label: "Short-term goals", value: note.shortTermGoals },
    { label: "Intervention", value: note.intervention },
    { label: "Remarks", value: note.remarks },
    { label: "Recommendations", value: note.recommendations },
  ]);

  const scoreByItemId: Record<string, number | null | undefined> = {
    suicidal_ideation: note.riskSuicidalIdeation,
    homicidal_ideation: note.riskHomicidalIdeation,
    substance_abuse: note.riskSubstanceUse,
    self_harm: note.riskSelfHarm,
    danger_to_others: note.riskAggression,
    psychosis: note.riskPsychosis,
    impulse_control: note.riskImpulsivity,
    medication_compliance: note.riskNonAdherence,
  };

  const riskRows = toSignedRiskRows(RISK_ASSESSMENT_ITEMS, scoreByItemId);
  const riskSelections = Object.fromEntries(
    RISK_ASSESSMENT_ITEMS.map((item, index) => [item.id, riskRows[index].answer]),
  );
  const riskSummary = summarizeRiskAssessment(RISK_ASSESSMENT_ITEMS, riskSelections);

  const signedAt = formatSignedAt(note.finalizedAt, practiceTimezone);
  const finalNote =
    note.finalContent?.trim() ||
    note.generatedContent?.trim() ||
    note.draftContent?.trim() ||
    "";

  return (
    <div className="space-y-6 px-6 pb-6 pt-6">
      {preview ? (
        // Same document, but nothing is signed yet — say so before anything else.
        <div className="rounded-xl border border-(--neutral-200) bg-(--neutral-50) px-4 py-3">
          <div className="flex flex-wrap items-center gap-2">
            <Eye size={14} className="shrink-0 text-(--bg-primary-dark)" />
            <span className="text-[0.8125rem] font-semibold uppercase tracking-wide text-(--bg-primary-dark)">
              Preview · not signed yet
            </span>
          </div>
          <p className="mt-1 text-sm text-(--text-primary-dark)">
            {note.therapistName?.trim() || "Clinician not recorded"}
          </p>
          <p className="mt-1 text-xs text-(--text-neutral-600)">
            This is how the note will read once you finalize it. Nothing here is saved or signed.
          </p>
        </div>
      ) : (
      /* Signature block — the first thing a reader needs to know. */
      <div className="rounded-xl border border-(--border-success) bg-(--bg-success-light) px-4 py-3">
        <div className="flex flex-wrap items-center gap-2">
          <Lock size={14} className="shrink-0 text-(--status-completed-dark)" />
          <span className="text-[0.8125rem] font-semibold uppercase tracking-wide text-(--status-completed-dark)">
            Signed and locked
          </span>
        </div>
        <p className="mt-1 text-sm text-(--text-primary-dark)">
          {note.therapistName?.trim() || "Clinician not recorded"}
          {signedAt ? ` · ${signedAt}` : ""}
        </p>
        <p className="mt-1 text-xs text-(--text-neutral-600)">
          This note can no longer be edited. Reopen it or add an amendment to record a change.
        </p>
      </div>
      )}

      {/* A change after signing belongs next to the signature it qualifies, not
          below the record where a reader may never reach it. */}
      {amendments.length > 0 ? (
        <section className="space-y-3">
          <h3 className="text-base font-semibold text-(--text-primary-dark)">
            Amendments ({amendments.length})
          </h3>
          <ol className="space-y-3">
            {amendments.map((amendment) => (
              <li
                key={amendment.id}
                className="rounded-xl border border-(--neutral-200) bg-(--neutral-50) px-4 py-3"
              >
                <p className="text-xs font-medium uppercase tracking-wide text-(--text-neutral-500)">
                  {amendment.reason}
                </p>
                <p className="mt-1 text-sm leading-6 whitespace-pre-wrap break-words text-(--text-primary-dark) [overflow-wrap:anywhere]">
                  {amendment.amendmentText}
                </p>
                <p className="mt-2 text-xs text-(--text-neutral-600)">
                  {amendment.createdByUserName?.trim() || "Clinician not recorded"}
                  {formatSignedAt(amendment.signedAt ?? amendment.createdAt, practiceTimezone)
                    ? ` · ${formatSignedAt(amendment.signedAt ?? amendment.createdAt, practiceTimezone)}`
                    : ""}
                </p>
              </li>
            ))}
          </ol>
        </section>
      ) : null}

      <section className="space-y-4">
        <h3 className="text-base font-semibold text-(--text-primary-dark)">
          Clinical documentation
        </h3>
        <dl className="space-y-4">
          {sections.map((section) => (
            <div key={section.label} className="border-b border-(--neutral-100) pb-3 last:border-b-0">
              <dt className="text-xs font-medium uppercase tracking-wide text-(--text-neutral-500)">
                {section.label}
              </dt>
              <dd
                className={cn(
                  "mt-1 text-sm leading-6 whitespace-pre-wrap break-words [overflow-wrap:anywhere]",
                  section.value
                    ? "text-(--text-primary-dark)"
                    : "text-(--text-neutral-400) italic",
                )}
              >
                {section.value ?? NOT_RECORDED}
              </dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-base font-semibold text-(--text-primary-dark)">
            Risk assessment
          </h3>
          <span className="text-[0.8125rem] text-(--text-neutral-600)">
            {riskSummary.answeredCount === 0 ? (
              <span className="text-(--status-pending-dark)">{NOT_ASSESSED}</span>
            ) : (
              <>
                <span className={cn("font-medium", BAND_TEXT_CLASSES[riskSummary.band])}>
                  {RISK_BAND_LABELS[riskSummary.band]}
                </span>
                {` · ${riskSummary.score}/${riskSummary.maxScore}`}
                {riskSummary.isComplete
                  ? ""
                  : ` · ${riskSummary.answeredCount} of ${riskSummary.totalCount} answered`}
              </>
            )}
          </span>
        </div>
        <dl className="space-y-2">
          {riskRows.map((row) => (
            <div
              key={row.title}
              className="flex flex-wrap items-baseline justify-between gap-2 border-b border-(--neutral-100) pb-2 last:border-b-0"
            >
              <dt className="text-sm text-(--text-neutral-700)">{row.title}</dt>
              <dd
                className={cn(
                  "text-sm",
                  row.answer
                    ? "font-medium text-(--text-primary-dark)"
                    : "italic text-(--text-neutral-400)",
                )}
              >
                {row.answer ?? NOT_ASSESSED}
              </dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="space-y-3">
        <h3 className="text-base font-semibold text-(--text-primary-dark)">Final note</h3>
        {finalNote ? (
          <div
            className="report-content text-sm leading-6 text-(--text-primary-dark)"
            // Server-rendered note content, same source the PDF is built from.
            dangerouslySetInnerHTML={{ __html: sanitizeHtml(finalNote) }}
          />
        ) : (
          <p className="text-sm italic text-(--text-neutral-400)">{NOT_RECORDED}</p>
        )}
      </section>
    </div>
  );
};

export default SessionNoteSignedView;
