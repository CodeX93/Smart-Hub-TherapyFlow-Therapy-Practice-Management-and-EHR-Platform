import { useMemo } from "react";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import {
  canAssignAssessments,
  canViewAssessments,
  isSupervisorApiRole,
  staffHasAny,
} from "@/utils/staffPermissions";

export function useStaffPortalRestrictions() {
  const { apiRoles, permissions, permissionSet } = useStaffAccess();

  return useMemo(() => {
    const isSupervisor = isSupervisorApiRole(apiRoles);
    const canCreateSessions = staffHasAny(permissionSet, "SESSION_CREATE");
    const canEditSessions = staffHasAny(permissionSet, "SESSION_EDIT");
    const canDeleteSessions = staffHasAny(permissionSet, "SESSION_DELETE");
    const canViewSessions = staffHasAny(permissionSet, "SESSION_VIEW");
    const canManageTasks = staffHasAny(permissionSet, "CONSENT_ADMIN_VIEW");
    const canViewAssessmentsAccess = canViewAssessments(permissionSet);
    const canAssignAssessmentsAccess = canAssignAssessments(permissionSet);
    const canManageAssessmentTemplates = canAssignAssessmentsAccess;
    const canManageClinicalForms =
      staffHasAny(permissionSet, "FORM_TEMPLATE_MANAGE", "FORM_VIEW", "CONSENT_ADMIN_VIEW");
    const canManageChecklists = staffHasAny(permissionSet, "CONSENT_ADMIN_VIEW");

    return {
      isSupervisor,
      supervisorStaffMode: isSupervisor,
      canViewSessions,
      canCreateSessions,
      canEditSessions,
      canDeleteSessions,
      canManageSessions: canCreateSessions || canEditSessions,
      canManageTasks,
      canViewAssessments: canViewAssessmentsAccess,
      canAssignAssessments: canAssignAssessmentsAccess,
      canManageAssessmentTemplates,
      canManageClinicalForms,
      canManageChecklists,
      hideSchedulingEdits: !canEditSessions,
      hideSessionTabActions: !canEditSessions,
      hideTableScheduledSession: !canEditSessions,
      hideTableCreateTask: !canManageTasks,
    };
  }, [apiRoles, permissionSet, permissions]);
}
