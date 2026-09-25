import { cn } from "@/lib/utils";

/** Full client name that wraps instead of stretching or clipping the task modal. */
export function TaskModalClientHeading({
  action,
  name,
}: {
  action: "Create" | "Edit";
  name?: string | null;
}) {
  const displayName = name?.trim() || "Client";

  return (
    <h2 className="min-w-0 text-xl font-semibold leading-7 text-[#1B1C20]">
      <span className="block">{action} Task for</span>
      <span
        className="mt-1 block break-all [overflow-wrap:anywhere] text-base font-semibold leading-snug text-[#1B1C20] sm:text-lg"
        title={displayName}
      >
        {displayName}
      </span>
    </h2>
  );
}

/** Locked client field: shows the full name (no ellipsis) without breaking the modal grid. */
export function TaskLockedClientField({
  name,
  clientId,
  className,
}: {
  name?: string | null;
  clientId?: string | null;
  className?: string;
}) {
  const primary = name?.trim() || "Client";
  const secondary = clientId?.trim() ? clientId.trim() : null;

  return (
    <div
      className={cn(
        "relative w-full min-h-15 rounded-2xl border border-[#D8DBDF] bg-[#F6F6F6]/70 px-3 pt-7 pb-2.5",
        className,
      )}
    >
      <span className="absolute left-3 top-3.5 -translate-y-1/2 text-xs text-(--text-secondary-light)">
        Client <span className="text-red-500">*</span>
      </span>
      <p
        className="break-all [overflow-wrap:anywhere] text-sm leading-snug text-(--neutral-950)"
        title={secondary ? `${primary} (${secondary})` : primary}
      >
        {primary}
        {secondary ? (
          <span className="mt-0.5 block text-xs font-normal text-[#5B616E]">
            Ref: {secondary}
          </span>
        ) : null}
      </p>
    </div>
  );
}
