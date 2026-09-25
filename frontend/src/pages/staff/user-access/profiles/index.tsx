import UserProfiles from "@/pages/admin/user-access/profiles";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import { getStaffUserAccessPermissions } from "@/utils/staffUserAccess";

const StaffUserProfiles = () => {
  const { permissions } = useStaffAccess();
  const userAccess = getStaffUserAccessPermissions(permissions);

  return <UserProfiles userAccess={userAccess} />;
};

export default StaffUserProfiles;
