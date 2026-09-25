import { type ReactNode } from "react";
import SuperAdminHeaderActions from "@/components/shared/SuperAdminHeaderActions";

interface SuperAdminPageHeaderProps {
  title: string;
  description?: string;
  meta?: ReactNode;
  toolbar?: ReactNode;
  userInitials?: string;
  userFullName?: string;
  notificationCount?: number;
}

function SuperAdminPageHeader(props: SuperAdminPageHeaderProps) {
  return (
    <div
      className={
        props.toolbar
          ? "sticky top-0 z-20 -mt-3 flex flex-col gap-4 bg-[#FAFAFB] pt-3 pb-4"
          : "sticky top-0 z-20 -mt-3 flex flex-col gap-4 bg-[#FAFAFB] pt-3"
      }
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <h1 className="text-[1.25rem] font-semibold leading-[1.08] tracking-[-0.03em] text-[#1f2d38]">
              {props.title}
            </h1>
            {props.meta ? (
              <div className="flex items-center gap-1.5 text-[0.75rem] font-medium text-[#8a96a3]">
                {props.meta}
              </div>
            ) : null}
          </div>

          {props.description ? (
            <p className="text-[0.875rem] font-normal leading-5.5 text-[#667483]">
              {props.description}
            </p>
          ) : null}
        </div>

        <SuperAdminHeaderActions
          userInitials={props.userInitials}
          userFullName={props.userFullName}
          notificationCount={props.notificationCount}
        />
      </div>

      {props.toolbar ? <div className="w-full">{props.toolbar}</div> : null}
    </div>
  );
}

export default SuperAdminPageHeader;
