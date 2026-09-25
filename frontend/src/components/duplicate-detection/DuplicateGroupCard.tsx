import { Info, Users, Copy } from "lucide-react";
import type { DuplicateGroup, DuplicateRecord } from "@/types/duplicate-detection.types";
import RecordCard from "./RecordCard";

interface DuplicateGroupCardProps {
  group: DuplicateGroup;
  onMarkAsDuplicate: (groupId: string, recordId: string) => void;
  onViewFullRecord: (record: DuplicateRecord) => void;
}

const DuplicateGroupCard = ({
  group,
  onMarkAsDuplicate,
  onViewFullRecord,
}: DuplicateGroupCardProps) => {
  const onCopyClick = () => {
    navigator.clipboard.writeText(group.groupNumber.toString());
  };

  return (
    <div className="flex bg-white rounded-3xl border border-(--neutral-100) overflow-hidden shadow-xs mb-6 min-h-125">
      {/* Sidebar Info */}
      <div className="w-65 p-6 flex flex-col">
        <div className="flex-1 flex flex-col">
          <div className="mb-2">
            <div
              onClick={onCopyClick}
              className="flex items-center justify-center w-13 h-13 rounded-xl bg-(--bg-primary-50) border border-(--neutral-100) text-(--text-primary-500) shadow-xs cursor-pointer hover:bg-(--bg-primary-100) transition-colors duration-300"
            >
              <Copy size={24} />
            </div>
            <h3 className="font-semibold text-(--text-primary-dark) mt-4">
              Duplicate Group #{group.groupNumber}
            </h3>
          </div>

          <div className="flex flex-wrap gap-2 mb-8">
            {group.matchReasons.map((reason, idx) => (
              <span
                key={idx}
                className="px-3 py-1 bg-white border border-(--neutral-200) rounded-full text-xs font-medium text-(--text-primary-dark) shadow-xs"
              >
                {reason}
              </span>
            ))}
          </div>
        </div>

        <div className="flex-1 flex flex-col">
          <div className="space-y-4">
            <div>
              <Info size={18} className="text-(--text-primary-400) flex-none" />
              <div className="space-y-2 mt-1">
                <h5 className="text-sm text-(--text-neutral-600)">
                  Smart Recommendation:
                </h5>
                <p className="text-sm font-semibold text-(--text-primary-dark)">
                  Keep: {group.recommendation.keepName} (
                  {group.recommendation.keepId})
                </p>
                <div className="pt-2">
                  <h6 className="text-sm text-(--text-neutral-600) mb-1">
                    Reason:
                  </h6>
                  <p className="text-sm font-semibold text-(--text-primary-dark)">
                    {group.recommendation.reason}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-auto pt-6 border-t border-(--neutral-200) border-dashed flex items-center gap-2 text-(--neutral-700)">
            <Users size={16} />
            <span className="text-sm">{group.records.length} Records</span>
          </div>
        </div>
      </div>

      {/* Records Content */}
      <div className="flex-1 py-2 pr-2">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-full">
          {group.records.map((record, idx) => (
            <RecordCard
              key={record.id}
              record={record}
              onMarkAsDuplicate={(recordId) =>
                onMarkAsDuplicate(group.id, recordId)
              }
              onViewFullRecord={onViewFullRecord}
              otherRecordName={group.records[idx === 0 ? 1 : 0].name}
            />
          ))}
        </div>
      </div>
    </div>
  );
};

export default DuplicateGroupCard;
