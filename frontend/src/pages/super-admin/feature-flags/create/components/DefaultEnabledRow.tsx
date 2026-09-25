import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";

interface DefaultEnabledRowProps {
  checked: boolean;
  onChange(next: boolean): void;
  className?: string;
}

function DefaultEnabledRow(props: DefaultEnabledRowProps) {
  return (
    <div
      className={cn(
        "flex w-full items-center justify-between gap-4 rounded-[0.75rem] border border-(--neutral-100) bg-(--bg-primary-light) px-4 py-4",
        props.className
      )}
    >
      <div>
        <div className="text-(--text-gray-900) text-sm font-medium leading-5.5">
          Default Enabled State
        </div>
        <div className="mt-1 text-(--text-neutral-400) text-sm font-normal leading-5">
          Set whether this feature is enabled by default for all applicable
          entities.
        </div>
      </div>

      <Switch checked={props.checked} onCheckedChange={props.onChange} />
    </div>
  );
}

export default DefaultEnabledRow;
