import { Button } from "@/components/ui/button";
import { useMemo, useState } from "react";
import { cn } from "@/lib/utils";
import { Plus } from "lucide-react";
import UserProfileTable from "@/components/access-profile-sections/UserProfileTable";
import SupervisorAssignmentTable from "@/components/access-profile-sections/SupervisorAssignmentTable";
import { USER_ACCESS_TABS, type TabType } from "./user-access.static";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import { useGetSupervisorAssignmentsQuery } from "@/store/api/admin/supervisorAssignments.api";
import type { StaffUserAccessPermissions } from "@/utils/staffUserAccess";

const DEFAULT_USER_ACCESS: StaffUserAccessPermissions = {
  canCreateUser: true,
  canEditUser: true,
  canDeleteUser: true,
  canToggleUserStatus: true,
  canEditProfessionalDetails: true,
  canAssignSupervisor: true,
  showSupervisorTab: true,
  hasRowActions: true,
};

type UserProfilesProps = {
  userAccess?: StaffUserAccessPermissions;
};

const UserProfiles = ({ userAccess = DEFAULT_USER_ACCESS }: UserProfilesProps) => {
  const visibleTabs = useMemo(
    () =>
      USER_ACCESS_TABS.filter(
        (tab) => tab !== "Supervisor Assignments" || userAccess.showSupervisorTab,
      ),
    [userAccess.showSupervisorTab],
  );
  const [activeTab, setActiveTab] = useState<TabType>("User Profiles");
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [userCounts, setUserCounts] = useState(0);
  const [supervisorCounts, setSupervisorCounts] = useState(0);
  const { data: initialSupervisorAssignments = [] } = useGetSupervisorAssignmentsQuery(
    {},
    { skip: !userAccess.showSupervisorTab },
  );

  if (!visibleTabs.includes(activeTab) && activeTab !== (visibleTabs[0] ?? "User Profiles")) {
    setActiveTab(visibleTabs[0] ?? "User Profiles");
  }

  const resetKey = initialSupervisorAssignments.length;
  const [previousResetKey, setPreviousResetKey] = useState(resetKey);
  if (previousResetKey !== resetKey) {
    setPreviousResetKey(resetKey);
    setSupervisorCounts(initialSupervisorAssignments.length);
  }

  const onTabChange = (tab: TabType) => {
    setActiveTab(tab);
  };

  const handleAction = () => {
    if (activeTab === "User Profiles") {
      setIsAddModalOpen(true);
    } else {
      setIsAssignModalOpen(true);
    }
  };

  const showHeaderAction =
    activeTab === "User Profiles"
      ? userAccess.canCreateUser
      : userAccess.canAssignSupervisor;

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden gap-4">
      <ScrollToTopButton />
      <div className="flex shrink-0 items-center justify-between w-full">
        {visibleTabs.length > 1 ? (
          <div className="flex bg-(--neutral-100) p-1 rounded-full w-fit">
            {visibleTabs.map((tab) => (
              <button
                key={tab}
                onClick={() => onTabChange(tab)}
                className={cn(
                  "px-6 py-2 rounded-full text-sm font-medium transition-all duration-300 cursor-pointer",
                  activeTab === tab
                    ? "bg-white text-(--neutral-950) shadow-sm"
                    : "text-(--text-neutral-600) hover:text-(--neutral-950)",
                )}
              >
                {tab} ({tab === "User Profiles" ? userCounts : supervisorCounts})
              </button>
            ))}
          </div>
        ) : (
          <div className="text-lg font-semibold text-(--text-primary-dark)">
            User Profiles ({userCounts})
          </div>
        )}
        {showHeaderAction ? (
          <Button
            onClick={handleAction}
            className="bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/95 text-white rounded-full px-6 py-2.5 h-auto flex items-center gap-2 font-semibold transition-all duration-300 cursor-pointer"
          >
            <Plus size={18} />
            {activeTab === "User Profiles" ? "Add New User" : "Assign Supervisor"}
          </Button>
        ) : (
          <div />
        )}
      </div>

      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        {activeTab === "User Profiles" ? (
          <UserProfileTable
            setIsAddModalOpen={setIsAddModalOpen}
            isAddModalOpen={isAddModalOpen}
            setUserCounts={setUserCounts}
            userAccess={userAccess}
          />
        ) : (
          <SupervisorAssignmentTable
            setIsAssignModalOpen={setIsAssignModalOpen}
            isAssignModalOpen={isAssignModalOpen}
            setSupervisorCounts={setSupervisorCounts}
            canManageAssignments={userAccess.canAssignSupervisor}
          />
        )}
      </div>
    </div>
  );
};

export default UserProfiles;
