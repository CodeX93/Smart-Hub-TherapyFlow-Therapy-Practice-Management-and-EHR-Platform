import AdminAssessment from "@/pages/admin/content/assessment";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import {
  canAssignAssessments,
  canViewAssessments,
} from "@/utils/staffPermissions";

const StaffAssessment = () => {
  const { permissionSet } = useStaffAccess();

  return (
    <AdminAssessment
      assessmentAccess={{
        canView: canViewAssessments(permissionSet),
        canManageTemplates: canAssignAssessments(permissionSet),
        canAssign: canAssignAssessments(permissionSet),
        canChangeAssignmentStatus: canAssignAssessments(permissionSet),
        buildAssessmentPath: "/staff/content/assessment/create-assessment",
      }}
    />
  );
};

export default StaffAssessment;
