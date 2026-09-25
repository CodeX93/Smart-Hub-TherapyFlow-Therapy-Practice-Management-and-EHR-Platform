import { cn } from "@/lib/utils";

interface PolicyNoticeProps {
  className?: string;
}

function PolicyNotice(props: PolicyNoticeProps) {
  return (
    <div
      className={cn(
        "w-full rounded-[0.875rem] border border-[#86efcc] bg-[#ecfdf5] px-4 py-4",
        props.className
      )}
    >
      <div className="text-[#405261] text-[0.75rem] font-normal leading-6">
        <span className="font-semibold">Policy Check:</span>{" "}
        <span>
          Impersonation is restricted to 60 minutes per session by your current
          security policy. Role access is limited to the selected user&apos;s
          permissions.
        </span>
      </div>
    </div>
  );
}

export default PolicyNotice;
