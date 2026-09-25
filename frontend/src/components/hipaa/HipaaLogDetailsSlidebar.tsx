import { X } from "lucide-react";
import { cn } from "../../lib/utils";
import { Badge } from "@/components/ui/badge";
import type { AuditRecord } from "@/types/hipaa.types";
import { formatNotificationMessage } from "@/utils/notificationDisplay";

interface HipaaLogDetailsSlidebarProps {
  record: AuditRecord | null;
  onClose: () => void;
}

const formatAuditDetailText = (value: string | undefined | null) => {
  if (!value || !value.trim()) return "";
  
  try {
    const parsed = JSON.parse(value);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return formatNotificationMessage(value);
  }
};

const HipaaLogDetailsSlidebar = ({
  record,
  onClose,
}: HipaaLogDetailsSlidebarProps) => {
  const isOpen = record !== null;

  return (
    <>
      <div
        className={cn(
          "fixed inset-0 z-50 flex justify-end transition-all duration-300",
          isOpen ? "visible" : "invisible",
        )}
      >
        {/* Backdrop */}
        <div
          className={cn(
            "fixed inset-0 bg-black/40 transition-opacity duration-300",
            isOpen ? "opacity-100" : "opacity-0",
          )}
          onClick={onClose}
        />

        {/* Side Panel */}
        <div
          className={cn(
            "relative w-full max-w-135 bg-white shadow-2xl transition-transform duration-300 flex flex-col h-full",
            isOpen ? "translate-x-0" : "translate-x-full",
          )}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-5 border-b border-(--neutral-100)">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Audit Log Details
            </h2>
            <button
              onClick={onClose}
              className="p-2 -mr-2 rounded-full hover:bg-(--neutral-50) text-(--text-neutral-500) transition-colors"
            >
              <X size={20} />
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
            {record && (
              <>
                <div className="grid gap-6 md:grid-cols-2">
                  <div>
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      Action
                    </p>
                    <p className="text-sm leading-6 whitespace-pre-wrap break-words text-(--text-primary-dark)">
                      {formatAuditDetailText(record.action)}
                    </p>
                  </div>
                  <div>
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      Timestamp
                    </p>
                    <p className="text-sm leading-6 text-(--text-primary-dark)">
                      {record.timestamp}
                    </p>
                  </div>
                  <div>
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      User
                    </p>
                    <p className="text-sm leading-6 text-(--text-primary-dark)">
                      {record.user}
                    </p>
                  </div>
                  <div>
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      MRN
                    </p>
                    <p className="text-sm leading-6 text-(--text-primary-dark)">
                      {record.client || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      Risk Level
                    </p>
                    <Badge
                      variant="outline"
                      className={cn(
                        "w-20 h-6 justify-center text-xs rounded-full shadow-none border-none",
                        record.riskLevel === "Low" &&
                          "bg-(--bg-success-light) text-(--dark-green)",
                        record.riskLevel === "Medium" &&
                          "bg-(--status-pending-light) text-(--status-pending-dark)",
                        record.riskLevel === "High" &&
                          "bg-(--light-red) text-(--dark-red)",
                        record.riskLevel === "Critical" &&
                          "bg-(--status-denied) text-white"
                      )}
                    >
                      {record.riskLevel}
                    </Badge>
                  </div>
                  <div>
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      Result
                    </p>
                    <p className="text-sm leading-6 text-(--text-primary-dark)">
                      {record.result}
                    </p>
                  </div>
                  <div>
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      IP Address
                    </p>
                    <p className="text-sm leading-6 text-(--text-primary-dark)">
                      {record.ipAddress}
                    </p>
                  </div>
                </div>

                {record.details && record.details.trim() && (
                  <div className="pt-4 border-t border-(--neutral-100)">
                    <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                      Details
                    </p>
                    <pre className="text-sm font-mono leading-6 whitespace-pre-wrap break-words text-(--text-primary-dark) bg-(--bg-primary-50) p-4 rounded-lg">
                      {formatAuditDetailText(record.details)}
                    </pre>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default HipaaLogDetailsSlidebar;
