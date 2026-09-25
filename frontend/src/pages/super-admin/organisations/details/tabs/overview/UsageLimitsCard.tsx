import { cn } from "@/lib/utils";
import type { OrganisationRow } from "../../../organisations.data";

interface UsageBarProps {
  label: string;
  valueText: string;
  percent: number;
  fillClassName: string;
}

function getUsageWidth(percent: number): string {
  const safePercent = Math.max(0, Math.min(100, percent));
  return safePercent + "%";
}

function UsageBar(props: UsageBarProps) {
  return (
    <div className="w-full">
      <div className="flex items-center justify-between gap-4">
        <span className="text-(--text-neutral-600) text-sm font-medium leading-5.5">
          {props.label}
        </span>
        <span className="text-(--text-neutral-400) text-sm font-normal leading-5.5">
          {props.valueText}
        </span>
      </div>
      <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-(--neutral-100)">
        <div
          className={cn("h-full rounded-full", props.fillClassName)}
          style={{ width: getUsageWidth(props.percent) }}
        />
      </div>
    </div>
  );
}

function getUsagePercent(used: number, limit: number | null | undefined): number {
  if (limit === null || limit === undefined || limit <= 0) {
    return 0;
  }
  return (used / limit) * 100;
}

function formatLimit(limit: number | null | undefined): string {
  if (limit === null || limit === undefined || limit <= 0) {
    return "Unlimited";
  }
  return String(limit);
}

interface UsageLimitsCardProps {
  org: OrganisationRow;
}

function UsageLimitsCard(props: UsageLimitsCardProps) {
  const usage = props.org.subscription.userUsage;
  const limits = props.org.subscription.userLimits;

  const therapistUsed = usage?.therapist ?? 0;
  const supervisorUsed = usage?.supervisor ?? 0;
  const clientUsed = usage?.client ?? 0;

  const therapistLimit = limits?.therapist ?? null;
  const supervisorLimit = limits?.supervisor ?? null;
  const clientLimit = limits?.client ?? null;

  return (
    <div className="w-full rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) px-5 py-5 shadow-[0_1px_2px_0px_var(--shadow)]">
      <div className="text-(--text-gray-900) text-[1rem] font-semibold leading-6">
        Usage &amp; Limits
      </div>

      <div className="mt-5 flex flex-col gap-5">
        <UsageBar
          label="Therapists"
          valueText={`${therapistUsed} / ${formatLimit(therapistLimit)}`}
          percent={getUsagePercent(therapistUsed, therapistLimit)}
          fillClassName="bg-(--status-pending)"
        />
        <UsageBar
          label="Supervisors"
          valueText={`${supervisorUsed} / ${formatLimit(supervisorLimit)}`}
          percent={getUsagePercent(supervisorUsed, supervisorLimit)}
          fillClassName="bg-(--status-pending)"
        />
        <UsageBar
          label="Clients"
          valueText={`${clientUsed} / ${formatLimit(clientLimit)}`}
          percent={getUsagePercent(clientUsed, clientLimit)}
          fillClassName="bg-(--bg-primary-dark)"
        />
      </div>
    </div>
  );
}

export default UsageLimitsCard;
