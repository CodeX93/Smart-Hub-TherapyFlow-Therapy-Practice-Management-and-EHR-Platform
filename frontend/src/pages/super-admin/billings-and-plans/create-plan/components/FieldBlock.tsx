import { cn } from "@/lib/utils";

interface FieldBlockProps {
  label: string;
  helper?: string;
  error?: string;
  children: React.ReactNode;
  className?: string;
}

function FieldBlock(props: FieldBlockProps) {
  return (
    <div className={cn("w-full", props.className)}>
      <div className="text-(--text-gray-900) text-sm font-medium leading-5.5">
        {props.label}
      </div>
      <div className="mt-2">{props.children}</div>
      {props.error ? (
        <div className="mt-2 text-(--status-denied) text-xs font-normal leading-4.5">
          {props.error}
        </div>
      ) : props.helper ? (
        <div className="mt-2 text-(--text-neutral-400) text-xs font-normal leading-4.5">
          {props.helper}
        </div>
      ) : null}
    </div>
  );
}

export default FieldBlock;
