import { type ReactNode } from "react";
import SuperAdminPageHeader from "@/components/shared/SuperAdminPageHeader";
import { cn } from "@/lib/utils";

interface SuperAdminPageShellProps {
  title: string;
  description?: string;
  meta?: ReactNode;
  toolbar?: ReactNode;
  userInitials?: string;
  userFullName?: string;
  notificationCount?: number;
  className?: string;
  children: ReactNode;
}

function SuperAdminPageShell(props: SuperAdminPageShellProps) {
  return (
    <div
      className={cn(
        "flex min-h-full w-full flex-col gap-4 bg-[#FAFAFB] pb-6",
        props.className
      )}
    >
      <SuperAdminPageHeader
        title={props.title}
        description={props.description}
        meta={props.meta}
        toolbar={props.toolbar}
        userInitials={props.userInitials}
        userFullName={props.userFullName}
        notificationCount={props.notificationCount}
      />
      {props.children}
    </div>
  );
}

export default SuperAdminPageShell;
