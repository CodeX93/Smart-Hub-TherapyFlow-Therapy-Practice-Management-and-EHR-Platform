import { cn } from "@/lib/utils";

interface SectionCardProps {
  title: string;
  children: React.ReactNode;
  className?: string;
}

function SectionCard(props: SectionCardProps) {
  return (
    <div
      className={cn(
        "w-full rounded-[1rem] border border-(--neutral-100) bg-(--surface-white)",
        "px-5 py-5 shadow-[0_2px_2px_0_var(--shadow)]",
        props.className
      )}
    >
      <div className="text-(--text-gray-900) text-base font-semibold leading-6">
        {props.title}
      </div>
      <div className="mt-5">{props.children}</div>
    </div>
  );
}

export default SectionCard;
