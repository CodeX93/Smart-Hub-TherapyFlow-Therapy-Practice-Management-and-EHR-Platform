
import { ContentLoader } from "@/components/shared/ContentLoader";
import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";

import { useAppSelector } from "@/store/hooks";
import {
  canAccessStaffRoute,
  getStaffLandingPath,
  type StaffRouteId,
} from "@/utils/staffPermissions";

type StaffRouteGuardProps = {
  routeId: StaffRouteId;
  children: ReactNode;
};

export function StaffRouteGuard({ routeId, children }: StaffRouteGuardProps) {
  const role = useAppSelector((state) => state.auth.role);
  const permissions = useAppSelector((state) => state.auth.permissions);
  const apiRoles = useAppSelector((state) => state.auth.apiRoles);
  const authBootstrapResolved = useAppSelector((state) => state.auth.authBootstrapResolved);

  if (role !== "staff") {
    return <>{children}</>;
  }

  if (!authBootstrapResolved) {
    return (
      <ContentLoader className="-1 py-16" />
    );
  }

  if (canAccessStaffRoute(permissions, routeId)) {
    return <>{children}</>;
  }

  return (
    <Navigate
      to={getStaffLandingPath(permissions, apiRoles)}
      replace
    />
  );
}
