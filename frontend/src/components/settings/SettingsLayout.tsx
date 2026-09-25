import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";

interface SettingsLayoutProps {
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  toolbar?: ReactNode;
  children: ReactNode;
}

const SettingsLayout = ({
  title,
  description,
  actionLabel,
  onAction,
  toolbar,
  children,
}: SettingsLayoutProps) => {
  const showHeader = Boolean(title || description || actionLabel);

  return (
    <div className="flex h-full min-h-0 flex-1 flex-col overflow-hidden rounded-3xl border border-(--neutral-100) bg-white shadow-xs">
      {showHeader ? (
        <div className="flex shrink-0 flex-col gap-4 border-b border-(--neutral-100) bg-white p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            {title ? (
              <h2 className="text-xl font-bold text-(--text-primary-dark)">
                {title}
              </h2>
            ) : null}
            {description ? (
              <p className="mt-1 text-sm text-(--text-neutral-600)">
                {description}
              </p>
            ) : null}
          </div>
          {actionLabel ? (
            <Button
              onClick={onAction}
              className="h-10 w-full shrink-0 cursor-pointer items-center gap-2 rounded-full bg-(--bg-primary-dark) px-5! text-white hover:bg-(--bg-primary-dark)/95 sm:w-auto"
            >
              <Plus size={24} />
              {actionLabel}
            </Button>
          ) : null}
        </div>
      ) : null}
      {toolbar ? (
        <div className="shrink-0 border-b border-(--neutral-100) bg-white px-4 py-4">
          {toolbar}
        </div>
      ) : null}
      <div className="min-h-0 flex-1 overflow-hidden">{children}</div>
    </div>
  );
};

export default SettingsLayout;
