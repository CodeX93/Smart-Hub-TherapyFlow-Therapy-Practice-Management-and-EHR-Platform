import AdminClinicalForms from "@/pages/admin/content/clinical-forms";
import { useStaffPortalRestrictions } from "@/hooks/useStaffPortalRestrictions";

const StaffClinicalForms = () => {
  const { supervisorStaffMode } = useStaffPortalRestrictions();
  return <AdminClinicalForms staffMode={supervisorStaffMode} />;
};

export default StaffClinicalForms;
