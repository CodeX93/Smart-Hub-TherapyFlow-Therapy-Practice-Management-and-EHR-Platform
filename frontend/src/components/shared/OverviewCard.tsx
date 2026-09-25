import React from "react";
import { AlertTriangle } from "lucide-react";

export interface OverviewCardBadge {
  text: string;
  tone?: "urgent" | "success" | "neutral";
}

interface OverviewCardProps {
  label: string;
  value: string | number;
  subtext?: string;
  badge?: OverviewCardBadge;
  icon?: React.ReactNode;
  iconSrc?: string;
  className?: string;
  valueClassName?: string;
  labelClassName?: string;
  iconClassName?: string;
  labelFirst?: boolean;
  isText?: string;
}

const badgeToneClass: Record<NonNullable<OverviewCardBadge["tone"]>, string> = {
  urgent: "text-red-600",
  success: "text-green-600",
  neutral: "text-(--text-neutral-600)",
};

const OverviewCard = ({
  label,
  value,
  icon,
  subtext,
  badge,
  iconSrc,
  className = "",
  valueClassName = "",
  labelClassName = "",
  iconClassName = "",
  labelFirst = false,
  isText = "",
}: OverviewCardProps) => {
  const valueNode = (
    <div
      className={`flex items-baseline gap-2 flex-wrap ${
        labelFirst ? "mt-1" : "mb-1"
      }`}
    >
      <span
        className={`md:text-xl font-semibold text-(--text-primary-dark) ${valueClassName}`}
      >
        {value}
      </span>
      {badge?.text ? (
        <span
          className={`inline-flex items-center gap-1 text-xs font-medium ${
            badgeToneClass[badge.tone ?? "neutral"]
          }`}
        >
          {(badge.tone ?? "neutral") === "urgent" ? (
            <AlertTriangle className="w-3 h-3 shrink-0" aria-hidden />
          ) : null}
          {badge.text}
        </span>
      ) : null}
    </div>
  );

  return (
    <div
      className={`bg-white border border-(--neutral-100) rounded-xl shadow-xs md:p-4 p-2.5 flex items-center justify-between flex-1 md:gap-4 gap-2 ${className}`}
    >
      <div className="flex flex-col w-full">
        {labelFirst ? (
          <>
            <span
              className={`md:text-sm text-xs text-(--text-neutral-600) font-normal ${labelClassName}`}
            >
              {label}
            </span>
            {valueNode}
          </>
        ) : (
          <>
            {valueNode}
            <span
              className={`md:text-sm text-xs text-(--text-neutral-600) font-normal ${labelClassName}`}
            >
              {label}
            </span>
          </>
        )}

        {subtext && (
          <span className={`text-xs text-(--text-neutral-600) font-normal`}>
            {subtext}
          </span>
        )}
      </div>
      <div
        className={`rounded-lg flex items-start justify-end h-12 md:w-full ${iconClassName}`}
      >
        {isText && (
          <span
            className={`text-sm text-(--text-neutral-600) font-normal ${labelClassName}`}
          >
            {isText}
          </span>
        )}

        {iconSrc ? <img src={iconSrc} alt={label} className="w-12 h-12" /> : icon}
      </div>
    </div>
  );
};

export default OverviewCard;
