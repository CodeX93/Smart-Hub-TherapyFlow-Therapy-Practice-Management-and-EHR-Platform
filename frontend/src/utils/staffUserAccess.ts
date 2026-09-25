import { hasAnyPermission, hasPermission } from "./staffPermissions";

export type StaffUserAccessPermissions = {
  canCreateUser: boolean;
  canEditUser: boolean;
  canDeleteUser: boolean;
  canToggleUserStatus: boolean;
  canEditProfessionalDetails: boolean;
  canAssignSupervisor: boolean;
  showSupervisorTab: boolean;
  hasRowActions: boolean;
};

export function getStaffUserAccessPermissions(
  permissions: string[],
): StaffUserAccessPermissions {
  const canManage = hasPermission(permissions, "USER_MANAGE");
  const canCreateUser = canManage || hasPermission(permissions, "USER_CREATE");
  const canEditUser = canManage || hasPermission(permissions, "USER_EDIT");
  const canDeleteUser = canManage || hasPermission(permissions, "USER_DELETE");
  const canEditProfessionalDetails = canEditUser;

  return {
    canCreateUser,
    canEditUser,
    canDeleteUser,
    canToggleUserStatus: false,
    canEditProfessionalDetails,
    canAssignSupervisor: false,
    showSupervisorTab: false,
    hasRowActions:
      canEditUser || canEditProfessionalDetails || canDeleteUser,
  };
}

export function canAccessStaffUserManagement(permissions: string[]): boolean {
  return hasAnyPermission(permissions, [
    "USER_VIEW",
    "USER_MANAGE",
    "USER_CREATE",
    "USER_EDIT",
    "USER_DELETE",
  ]);
}
