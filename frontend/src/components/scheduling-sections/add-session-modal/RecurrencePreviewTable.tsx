
import { ContentLoader } from "@/components/shared/ContentLoader";
import type { RecurrencePreviewResponse } from "@/types/recurringSessions";

interface RecurrencePreviewTableProps {
  preview: RecurrencePreviewResponse | null;
  isLoading?: boolean;
  error?: string | null;
}

const RecurrencePreviewTable = ({
  preview,
  isLoading = false,
  error = null,
}: RecurrencePreviewTableProps) => {
  if (isLoading) {
    return (
      <div className="flex items-center gap-2 rounded-xl border border-(--neutral-100) bg-white px-4 py-3 text-sm text-(--text-neutral-600)">
        <ContentLoader variant="inline" size="sm" />
        Checking recurring dates...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-[#b42318]">
        {error}
      </div>
    );
  }

  if (!preview) return null;

  return (
    <div className="space-y-3 rounded-2xl border border-(--neutral-100) bg-white p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-semibold text-(--text-primary-dark)">
          Recurrence preview
        </p>
        <p className="text-xs text-(--text-neutral-600)">
          {preview.freeCount} free · {preview.conflictCount} conflict
          {preview.conflictCount === 1 ? "" : "s"} · {preview.totalRequested} total
        </p>
      </div>

      {preview.conflictCount > 0 ? (
        <p className="text-xs text-(--status-overdue-dark)">
          Conflicting dates will be skipped when you create the series.
        </p>
      ) : null}

      <div className="max-h-48 overflow-y-auto custom-scrollbar rounded-xl border border-(--neutral-100)">
        <table className="w-full text-left text-sm">
          <thead className="sticky top-0 bg-(--bg-primary-50)">
            <tr>
              <th className="px-3 py-2 font-medium text-(--text-primary-dark)">Date</th>
              <th className="px-3 py-2 font-medium text-(--text-primary-dark)">Time</th>
              <th className="px-3 py-2 font-medium text-(--text-primary-dark)">Status</th>
            </tr>
          </thead>
          <tbody>
            {preview.sessions.map((session) => (
              <tr key={`${session.localDate}-${session.sessionTime}`} className="border-t border-(--neutral-100)">
                <td className="px-3 py-2 text-(--text-primary-dark)">{session.localDate}</td>
                <td className="px-3 py-2 text-(--text-neutral-600)">{session.sessionTime}</td>
                <td className="px-3 py-2">
                  {session.hasConflict ? (
                    <span
                      className="text-xs font-medium text-(--status-overdue-dark)"
                      title={session.reasons.join(", ")}
                    >
                      Conflict
                    </span>
                  ) : (
                    <span className="text-xs font-medium text-(--status-paid)">Available</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default RecurrencePreviewTable;
