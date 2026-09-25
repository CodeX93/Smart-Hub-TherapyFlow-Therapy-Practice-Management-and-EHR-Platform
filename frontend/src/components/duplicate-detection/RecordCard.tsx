import type { DuplicateRecord } from "@/types/duplicate-detection.types";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

interface RecordCardProps {
  record: DuplicateRecord;
  onMarkAsDuplicate: (recordId: string) => void;
  onViewFullRecord: (record: DuplicateRecord) => void;
  otherRecordName: string;
}

const RecordCard = ({
  record,
  onMarkAsDuplicate,
  onViewFullRecord,
  otherRecordName,
}: RecordCardProps) => {
  const infoItems = [
    { label: "Phone", value: record.phone },
    { label: "Email", value: record.email },
    { label: "DOB", value: record.dob },
    { label: "Status", value: record.status },
    { label: "Created", value: record.created },
  ];

  const statItems = [
    { label: "Sessions", value: record.stats.sessions },
    { label: "Documents", value: record.stats.documents },
    { label: "Billing", value: record.stats.billing },
  ];

  return (
    <div className="flex flex-col p-5 bg-white rounded-2xl border border-(--neutral-100) shadow-xs h-full">
      <div className="mb-4">
        <div className="flex items-center justify-between w-full">
          <h4 className="font-semibold text-(--text-primary-dark)">
            {record.name}
          </h4>
          <span
            className={cn(
              "px-3 py-1 text-sm rounded-full",
              record.isRecommendedToKeep
                ? "bg-(--status-completed-light) text-(--dark-green)"
                : "bg-(--dashboard-status-pending-light) text-(--status-pending-dark)",
            )}
          >
            {record.isRecommendedToKeep ? "Keep this" : "Mark as Duplicate"}
          </span>
        </div>
        <span className="text-sm text-(--text-neutral-600)">
          #{record.clientId}
        </span>
      </div>

      <div className="space-y-2 mb-6">
        {infoItems.map((item, idx) => (
          <div key={idx} className="flex justify-between text-sm">
            <span className="text-(--text-neutral-600)">{item.label}</span>
            <span className="font-medium text-(--text-primary-dark) truncate max-w-50.25">
              {item.value}
            </span>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-3 gap-2 mb-6 py-4">
        {statItems.map((item, idx) => (
          <div key={idx} className="text-center relative">
            {idx < statItems.length - 1 && (
              <div className="absolute right-0 top-1/2 -translate-y-1/2 h-8 w-px bg-(--neutral-100)" />
            )}
            <div className="text-lg font-semibold text-(--text-primary-dark)">
              {item.value}
            </div>
            <div className="text-sm text-(--text-neutral-600)">
              {item.label}
            </div>
          </div>
        ))}
      </div>

      <div className="mt-auto space-y-3">
        <Button
          variant={record.isRecommendedToKeep ? "outline" : "default"}
          onClick={() => onMarkAsDuplicate(record.id)}
          className={cn(
            "w-full rounded-full h-11 px-4 font-semibold transition-all duration-300 ease-in-out cursor-pointer border",
            record.isRecommendedToKeep
              ? "border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--bg-primary-dark) hover:text-white"
              : "border-transparent bg-(--bg-primary-dark) text-white hover:bg-transparent hover:border-(--neutral-200) hover:text-(--bg-primary-dark)",
          )}
        >
          <span className="truncate w-full text-center">
            Mark as Duplicate of {otherRecordName}
            {!record.isRecommendedToKeep && " (Recommended)"}
          </span>
        </Button>
        <div className="flex justify-center items-center">
          <Button
            variant="link"
            className="w-fit text-sm font-semibold text-(--text-primary-dark) cursor-pointer"
            onClick={() => onViewFullRecord(record)}
          >
            View Full Record
          </Button>
        </div>
      </div>
    </div>
  );
};

export default RecordCard;
