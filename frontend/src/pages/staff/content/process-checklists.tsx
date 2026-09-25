import AdminProcessCheckLists from "@/pages/admin/content/process-checklists";
import { useStaffPortalRestrictions } from "@/hooks/useStaffPortalRestrictions";

const StaffProcessCheckLists = () => {
  const { supervisorStaffMode } = useStaffPortalRestrictions();
  return <AdminProcessCheckLists staffMode={supervisorStaffMode} />;
};

export default StaffProcessCheckLists;
