import type { JSX } from "react";

import { RoleRouteGuard } from "@/components/auth/RoleRouteGuard";
import type { AppRole } from "@/utils/roleMapper";

const EVERY_ROLE: AppRole[] = [
  "super-admin",
  "admin",
  "therapist",
  "staff",
  "user",
];

/**
 * Session + role gate for a single route element.
 *
 * Reads the real session rather than assuming one: without `role` it only
 * requires a signed-in user, with `role` it also keeps the route to that role
 * and sends anyone else back to their own area.
 */
export const ProtectedRoute = ({
  children,
  role,
}: {
  children: JSX.Element;
  role?: AppRole;
}) => (
  <RoleRouteGuard allow={role ? [role] : EVERY_ROLE}>{children}</RoleRouteGuard>
);
