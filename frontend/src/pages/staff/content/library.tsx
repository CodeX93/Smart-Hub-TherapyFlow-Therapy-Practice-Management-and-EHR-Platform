import AdminLibrary from "@/pages/admin/content/library";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import { canWriteLibrary } from "@/utils/staffPermissions";

const StaffLibrary = () => {
  const { permissionSet } = useStaffAccess();

  return <AdminLibrary canWriteEntries={canWriteLibrary(permissionSet)} />;
};

export default StaffLibrary;
