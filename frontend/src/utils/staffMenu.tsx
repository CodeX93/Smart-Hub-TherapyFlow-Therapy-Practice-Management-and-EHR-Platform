import { CalendarIcon } from "@/components/icons/commonIcons";
import { ClipboardList, FileText, LibraryBig, Shield, Bell } from "lucide-react";
import type { MenuItem } from "../types/user.type";
import type { StaffRouteId } from "./staffPermissions";
import {
  canAccessStaffRoute,
  shouldShowFullStaffMenu,
} from "./staffPermissions";
import { Accessibility as AccessibilityIcon } from "@solar-icons/react-perf/category/ui/Linear/Accessibility";
import { BillList as BillListIcon } from "@solar-icons/react-perf/category/money/Linear/BillList";
import { ClipboardList as ClipboardListIcon } from "@solar-icons/react-perf/category/notes/Linear/ClipboardList";
import { Settings as SettingsIcon } from "@solar-icons/react-perf/category/settings/Linear/Settings";
import { ShieldMinimalistic as ShieldMinimalisticIcon } from "@solar-icons/react-perf/category/security/Linear/ShieldMinimalistic";
import { UserCircle as UserCircleIcon } from "@solar-icons/react-perf/category/users/Linear/UserCircle";
import { UsersGroupTwoRounded as UsersGroupTwoRoundedIcon } from "@solar-icons/react-perf/category/users/Linear/UsersGroupTwoRounded";
import { Widget5 as Widget5Icon } from "@solar-icons/react-perf/category/settings/Linear/Widget5";

type StaffMenuItem = Omit<MenuItem, "children"> & {
  routeId?: StaffRouteId;
  supervisorHidden?: boolean;
  children?: StaffMenuItem[];
};

const STAFF_MENU_DEFINITION: StaffMenuItem[] = [
  {
    id: "clients",
    label: "Clients",
    icon: <UsersGroupTwoRoundedIcon size={20} />,
    path: "/staff/clients",
    routeId: "clients",
  },
  {
    id: "scheduling",
    label: "Scheduling",
    icon: <CalendarIcon size={20} />,
    path: "/staff/scheduling",
    routeId: "scheduling",
  },
  {
    id: "billings",
    label: "Billings",
    icon: <BillListIcon size={20} />,
    path: "/staff/billings",
    routeId: "billings",
  },
  {
    id: "tasks",
    label: "Tasks",
    icon: <ClipboardListIcon size={20} />,
    path: "/staff/tasks",
    routeId: "tasks",
  },
  {
    id: "user-access",
    label: "User & Access",
    icon: <AccessibilityIcon size={20} />,
    path: "/staff/user-access/profiles",
    routeId: "user-profiles",
    supervisorHidden: true,
  },
  {
    id: "content",
    label: "Content",
    icon: <UserCircleIcon size={20} />,
    path: "/staff/content",
    children: [
      {
        id: "library",
        label: "Library",
        icon: <LibraryBig size={18} />,
        path: "/staff/content/library",
        routeId: "content-library",
      },
      {
        id: "assessment",
        label: "Assessment",
        icon: <ClipboardList size={18} />,
        path: "/staff/content/assessment",
        routeId: "content-assessment",
      },
      {
        id: "clinical-forms",
        label: "Clinical Forms",
        icon: <FileText size={18} />,
        path: "/staff/content/clinical-forms",
        routeId: "content-clinical-forms",
      },
      {
        id: "process-checklists",
        label: "Process Checklists",
        icon: <ClipboardList size={18} />,
        path: "/staff/content/process-checklists",
        routeId: "content-process-checklists",
      },
    ],
  },
  {
    id: "compliance",
    label: "Compliance",
    icon: <ShieldMinimalisticIcon size={20} />,
    path: "/staff/compliance",
    children: [
      {
        id: "hipaa-audit",
        label: "HIPAA Audit",
        icon: <Shield size={18} />,
        path: "/staff/compliance/hipaa",
        routeId: "compliance-hipaa",
      },
    ],
  },
  {
    id: "system",
    label: "System",
    icon: <SettingsIcon size={20} />,
    path: "/staff/system",
    children: [
      {
        id: "notifications",
        label: "Notifications",
        icon: <Bell size={18} />,
        path: "/staff/system/notifications",
        routeId: "system-notifications",
      },
    ],
  },
];

function stripRouteIds(items: StaffMenuItem[]): MenuItem[] {
  return items.map(function (item) {
    const menuItem: StaffMenuItem = { ...item };
    delete menuItem.routeId;
    if (menuItem.children) {
      return {
        ...menuItem,
        children: menuItem.children.map(function (child) {
          const childItem = { ...child };
          delete childItem.routeId;
          return childItem;
        }),
      };
    }
    return menuItem;
  });
}

function filterStaffMenuItems(
  items: StaffMenuItem[],
  permissions: string[],
): MenuItem[] {
  return items
    .map(function (item) {
      if (item.children?.length) {
        const children = filterStaffMenuItems(item.children, permissions);
        if (!children.length) return null;
        return { ...item, children };
      }

      if (item.routeId && !canAccessStaffRoute(permissions, item.routeId)) {
        return null;
      }

      const menuItem = { ...item };
      delete menuItem.routeId;
      return menuItem;
    })
    .filter(Boolean) as MenuItem[];
}

export function buildStaffMenuItems(
  permissions: string[],
  apiRoles: string[],
): MenuItem[] {
  if (shouldShowFullStaffMenu(permissions, apiRoles)) {
    return stripRouteIds(
      STAFF_MENU_DEFINITION.filter((item) => !item.supervisorHidden),
    );
  }

  if (!permissions.length) {
    return [];
  }

  return filterStaffMenuItems(STAFF_MENU_DEFINITION, permissions);
}

export { getStaffLandingPath, staffNoAccessPath } from "./staffPermissions";

export const staffDefaultLandingPath = "/staff/clients";

export const staffPortalHomeIcon = <Widget5Icon size={20} />;
