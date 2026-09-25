import { useMemo } from "react";
import { useAppSelector } from "@/store/hooks";
import {
  buildStaffPermissionSet,
  canAccessStaffRoute,
  canStaffCapability,
  getStaffCapabilities,
  hasPermission,
  staffHasAny,
  type StaffCapability,
  type StaffRouteId,
} from "@/utils/staffPermissions";

export function useStaffAccess() {
  const role = useAppSelector((state) => state.auth.role);
  const apiRoles = useAppSelector((state) => state.auth.apiRoles);
  const permissions = useAppSelector((state) => state.auth.permissions);

  const permissionSet = useMemo(
    () => buildStaffPermissionSet(permissions),
    [permissions],
  );

  const capabilities = useMemo(
    () => getStaffCapabilities(role, apiRoles),
    [role, apiRoles],
  );

  return {
    role,
    apiRoles,
    permissions,
    permissionSet,
    capabilities,
    can(permission: string) {
      return hasPermission(permissions, permission);
    },
    hasAny(...keys: string[]) {
      return staffHasAny(permissionSet, ...keys);
    },
    canRoute(routeId: StaffRouteId) {
      if (role !== "staff") return true;
      return canAccessStaffRoute(permissions, routeId);
    },
    canCapability(capability: StaffCapability) {
      return canStaffCapability(role, apiRoles, permissions, capability);
    },
  };
}
