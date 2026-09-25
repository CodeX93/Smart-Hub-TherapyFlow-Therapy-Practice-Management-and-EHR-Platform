import { Fragment, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { AuditRecord } from "@/types/hipaa.types";
import { formatNotificationMessage } from "@/utils/notificationDisplay";
import { Eye } from "lucide-react";
import HipaaLogDetailsSlidebar from "./HipaaLogDetailsSlidebar";

interface HIPAATableProps {
  data: AuditRecord[];
}

const formatAuditDetailText = (value: string) =>
  value.trim() ? formatNotificationMessage(value) : value;

const HipaaTable = ({ data }: HIPAATableProps) => {
  const [selectedRecord, setSelectedRecord] = useState<AuditRecord | null>(null);

  return (
    <div className="w-full overflow-x-auto overflow-y-auto rounded-xl border border-(--neutral-100) shadow-(--shadow) bg-white">
      <table className="w-full min-w-[64rem] table-fixed text-left border-collapse">
        <thead>
          <tr className="h-11.5 bg-(--bg-primary-50) border-b border-(--neutral-100)">
            <th className="w-[12%] px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
              Timestamp
            </th>
            <th className="w-[11%] px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
              User
            </th>
            <th className="w-[13%] px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
              Action
            </th>
            <th className="w-[10%] px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
              MRN
            </th>
            <th className="w-[10%] px-4 py-3 text-center text-sm font-semibold text-(--text-primary-dark)">
              Risk Level
            </th>
            <th className="w-[10%] px-4 py-3 text-center text-sm font-semibold text-(--text-primary-dark)">
              Result
            </th>
            <th className="w-[13%] px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
              IP Address
            </th>
            <th className="w-[5%] px-4 py-3"></th>
          </tr>
        </thead>
        <tbody>
          {data.length > 0 ? (
            data.map((record) => {
              const formattedAction = formatAuditDetailText(record.action);

              return (
                <Fragment key={record.id}>
                  <tr className="h-18 border-t border-(--neutral-100) transition-colors hover:bg-(--neutral-50)/30">
                    <td className="px-4 py-3 text-sm text-(--text-primary-dark)">
                      {record.timestamp.split(" ").slice(0, 3).join(" ")}
                      <br />
                      <span className="text-xs text-(--text-neutral-600)">
                        {record.timestamp.split(" ").slice(3).join(" ")}
                      </span>
                    </td>
                    <td className="max-w-0 overflow-hidden px-4 py-3 text-sm text-(--text-primary-dark)">
                      <span className="block truncate" title={record.user}>
                        {record.user}
                      </span>
                    </td>
                    <td className="max-w-0 overflow-hidden px-4 py-3 text-sm text-(--text-primary-dark)">
                      <span className="block truncate" title={formattedAction}>
                        {formattedAction}
                      </span>
                    </td>
                    <td className="max-w-0 overflow-hidden px-4 py-3 text-sm text-(--text-primary-dark)">
                      <span className="block truncate" title={record.client}>
                        {record.client}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
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
                    </td>
                    <td className="px-4 py-3 text-center">
                      <Badge
                        variant="outline"
                        className={cn(
                          "w-20 h-6 justify-center text-xs rounded-full shadow-none border-none",
                          record.result === "Success"
                            ? "bg-(--status-info-light) text-(--status-info-dark)"
                            : "bg-(--light-red) text-(--dark-red)",
                        )}
                      >
                        {record.result}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 text-sm text-(--text-primary-dark)">
                      {record.ipAddress}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => setSelectedRecord(record)}
                        className="text-(--text-neutral-500) hover:text-(--text-primary-dark) hover:bg-(--neutral-100) p-1 rounded-md transition-colors"
                        title="View Details"
                      >
                        <Eye size={16} />
                      </button>
                    </td>
                  </tr>
                </Fragment>
              );
            })
          ) : (
            <tr>
              <td
                colSpan={8}
                className="py-12 text-center text-sm text-(--text-neutral-500)"
              >
                No audit logs found.
              </td>
            </tr>
          )}
        </tbody>
      </table>
      <HipaaLogDetailsSlidebar
        record={selectedRecord}
        onClose={() => setSelectedRecord(null)}
      />
    </div>
  );
};

export default HipaaTable;
