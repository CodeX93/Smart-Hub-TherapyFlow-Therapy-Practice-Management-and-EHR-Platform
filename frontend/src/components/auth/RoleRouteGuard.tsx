import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { useAppSelector } from "@/store/hooks";
import type { AppRole } from "@/utils/roleMapper";
import {
  getDefaultLoginPath,
  getRedirectPathByRole,
} from "@/utils/redirectPathByRole";

type RoleRouteGuardProps = {
  /** Roles allowed to render this branch of the route tree. */
  allow: AppRole[];
  children: ReactNode;
};

/**
 * Keeps a whole route branch to the roles that own it.
 *
 * A signed-out visitor goes to the sign-in page for the host they are on; a
 * signed-in user of another role goes back to their own landing page rather than
 * seeing a console they have no right to. The backend refuses the data either
 * way, but the shell must not render for them in the first place.
 *
 * The decision is made from the session role alone, so it is immediate and never
 * waits on a request: a role refined later by /auth/me lands in the store and
 * re-runs this guard.
 */
export function RoleRouteGuard({ allow, children }: RoleRouteGuardProps) {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const role = useAppSelector((state) => state.auth.role);
  const location = useLocation();

  if (!accessToken || !role) {
    return <Navigate to={getDefaultLoginPath(location.pathname)} replace />;
  }

  if (allow.includes(role)) {
    return <>{children}</>;
  }

  // Where a staff member lands depends on permissions that may not have arrived
  // yet, so hand them to the staff index, which owns that choice and waits for
  // them. Every other role has a fixed landing page.
  const landingPath =
    role === "staff" ? "/staff" : getRedirectPathByRole(role);

  return <Navigate to={landingPath} replace />;
}
