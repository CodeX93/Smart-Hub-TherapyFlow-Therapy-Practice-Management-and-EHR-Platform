
import { ContentLoader } from "@/components/shared/ContentLoader";
import { Navigate } from "react-router-dom";

import { useAppSelector } from "@/store/hooks";
import { getStaffLandingPath } from "@/utils/staffPermissions";

export function StaffIndexRedirect() {
  const permissions = useAppSelector((state) => state.auth.permissions);
  const apiRoles = useAppSelector((state) => state.auth.apiRoles);
  const authBootstrapResolved = useAppSelector((state) => state.auth.authBootstrapResolved);

  if (!authBootstrapResolved) {
    return (
      <ContentLoader className="-1 py-16" />
    );
  }

  return (
    <Navigate
      to={getStaffLandingPath(permissions, apiRoles)}
      replace
    />
  );
}
