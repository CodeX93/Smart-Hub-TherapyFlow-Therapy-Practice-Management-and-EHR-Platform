import { type ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  description?: string;
  lastUpdatedText?: string;
  actions?: ReactNode;
}

const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  description,
  lastUpdatedText,
  actions,
}) => {
  return (
    <div className="mb-5 flex items-start justify-between gap-4">
      <div className="min-w-0 flex-1 flex-col gap-2">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <h2 className="text-[2rem] font-semibold leading-[1.05] tracking-[-0.03em] text-[#17212b]">
            {title}
          </h2>
          {lastUpdatedText ? (
            <div className="flex items-center gap-1.5 text-[0.75rem] text-[#8a96a3]">
              <span>{lastUpdatedText}</span>
            </div>
          ) : null}
        </div>
        {description ? (
          <p className="text-[0.875rem] font-normal leading-[1.375rem] text-[#667483]">
            {description}
          </p>
        ) : null}
      </div>

      {actions ? <div className="flex items-center">{actions}</div> : null}
    </div>
  );
};

export default PageHeader;
