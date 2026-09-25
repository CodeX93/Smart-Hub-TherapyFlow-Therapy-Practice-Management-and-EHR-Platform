import { CalendarIcon } from "@/components/icons/commonIcons";
import { FileText, ClipboardList, FileTextIcon, Settings, UsersRound, LayoutDashboard, LibraryBig, Users, Bell, UserCog, Shield, Lock, FlagIcon, DollarSignIcon, Plug, Pencil, Inbox, CreditCard } from "lucide-react";
import type { MenuItem } from "../types/user.type";
import InvoicesIcon from "./icons-jsx/InvoicesIcon";
import { Accessibility as AccessibilityIcon } from "@solar-icons/react-perf/category/ui/Linear/Accessibility";
import { BillList as BillListIcon } from "@solar-icons/react-perf/category/money/Linear/BillList";
import { ClipboardList as ClipboardListIcon } from "@solar-icons/react-perf/category/notes/Linear/ClipboardList";
import { Settings as SettingsIcon } from "@solar-icons/react-perf/category/settings/Linear/Settings";
import { ShieldMinimalistic as ShieldMinimalisticIcon } from "@solar-icons/react-perf/category/security/Linear/ShieldMinimalistic";
import { UserCircle as UserCircleIcon } from "@solar-icons/react-perf/category/users/Linear/UserCircle";
import { UsersGroupTwoRounded as UsersGroupTwoRoundedIcon } from "@solar-icons/react-perf/category/users/Linear/UsersGroupTwoRounded";
import { Widget5 as Widget5Icon } from "@solar-icons/react-perf/category/settings/Linear/Widget5";

export const adminMenuItems: MenuItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: <Widget5Icon size={20} />,
    path: "/admin/dashboard",
  },
  {
    id: "clients",
    label: "Clients",
    icon: <UsersGroupTwoRoundedIcon size={20} />,
    path: "/admin/clients",
  },
  {
    id: "scheduling",
    label: "Scheduling",
    icon: <CalendarIcon size={20} />,
    path: "/admin/scheduling",
  },
  {
    id: "billings",
    label: "Billings",
    icon: <BillListIcon size={20} />,
    path: "/admin/billings",
  },
  {
    id: "tasks",
    label: "Tasks",
    icon: <ClipboardListIcon size={20} />,
    path: "/admin/tasks",
  },
  {
    id: "content",
    label: "Content",
    icon: <UserCircleIcon size={20} />,
    path: "/admin/content",
    children: [
      { id: "library", label: "Library", icon: <LibraryBig size={18} />, path: "/admin/content/library" },
      { id: "assessment", label: "Assessment", icon: <ClipboardList size={18} />, path: "/admin/content/assessment" },
      { id: "clinical-forms", label: "Clinical Forms", icon: <FileText size={18} />, path: "/admin/content/clinical-forms" },
      { id: "process-checklists", label: "Process Checklists", icon: <ClipboardList size={18} />, path: "/admin/content/process-checklists" },
      { id: "report-templates", label: "Report Templates", icon: <FileText size={18} />, path: "/admin/content/report-templates" },
    ]
  },
  {
    id: "user-access",
    label: "User & Access",
    icon: <AccessibilityIcon size={20} />,
    path: "/admin/user-access",
    children: [
      { id: "user-profiles", label: "User Profiles", icon: <UserCog size={18} />, path: "/admin/user-access/profiles" },
      { id: "role-management", label: "Role Management", icon: <Users size={18} />, path: "/admin/user-access/roles" },
      { id: "duplicate-detection", label: "Duplicate Detection", icon: <UsersRound size={18} />, path: "/admin/user-access/duplicate-detection" },
    ]
  },
  {
    id: "system",
    label: "System",
    icon: <SettingsIcon size={20} />,
    path: "/admin/system",
    children: [
      { id: "notifications", label: "Notifications", icon: <Bell size={18} />, path: "/admin/system/notifications" },
      { id: "settings", label: "Settings", icon: <Settings size={18} />, path: "/admin/settings" },
      {
        id: "payments-and-subscription",
        label: "Payments & Subscription",
        icon: <CreditCard size={18} />,
        path: "/admin/payments-and-subscription",
      },
    ]
  },
  {
    id: "compliance",
    label: "Compliance",
    icon: <ShieldMinimalisticIcon size={20} />,
    path: "/admin/compliance",
    children: [
      { id: "hipaa-audit", label: "HIPAA Audit", icon: <Shield size={18} />, path: "/admin/compliance/hipaa" },
      { id: "privacy-consent", label: "Privacy & Consent", icon: <Lock size={18} />, path: "/admin/compliance/privacy" },
    ]
  }
];

export const superAdminMenuItems: MenuItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: <LayoutDashboard size={20} />,
    path: "/super-admin/dashboard",
  },
  {
    id: "organisations",
    label: "Organisations",
    icon: <UsersRound size={20} />,
    path: "/super-admin/organisations",
  },
  {
    id: "booking-requests",
    label: "Booking Requests",
    icon: <Inbox size={20} />,
    path: "/super-admin/booking-requests",
  },
  {
    id: "roles-and-permissions",
    label: "Roles & Permissions",
    icon: <UserCog size={20} />,
    path: "/super-admin/roles-and-permissions",
  },
  {
    id: "feature-flags",
    label: "Feature Flags",
    icon: <FlagIcon size={20} />,
    path: "/super-admin/feature-flags",
  },
  {
    id: "billings-and-plans",
    label: "Billings & Plans",
    icon: <DollarSignIcon size={20} />,
    path: "/super-admin/billings-and-plans",
  },
  {
    id: "system-settings",
    label: "System Settings",
    icon: <Settings size={20} />,
    path: "/super-admin/system-settings",
  },
  {
    id: "integrations",
    label: "Integrations",
    icon: <Plug size={20} />,
    path: "/super-admin/integrations",
  },
  {
    id: "cms",
    label: "CMS",
    icon: <Pencil size={20} />,
    path: "/super-admin/cms",
  },
  {
    id: "audit-logs",
    label: "Audit Logs",
    icon: <FileText size={20} />,
    path: "/super-admin/audit-logs",
  }
];

export const userMenuItems: MenuItem[] = [
  {
    id: "appointments",
    label: "Appointments",
    icon: <CalendarIcon size={20} />,
    path: "/user/appointments",
  },
  {
    id: "booked-sessions",
    label: "Booked Sessions",
    icon: <CalendarIcon size={20} />,
    path: "/user/booked-sessions",
  },
  {
    id: "invoices",
    label: "Invoices",
    icon: <InvoicesIcon size={20} />,
    path: "/user/invoices",
  },
  {
    id: "documents",
    label: "Documents",
    icon: <FileTextIcon size={20} />,
    path: "/user/documents",
  },
  {
    id: "clinical-forms",
    label: "Clinical Forms",
    icon: <ClipboardList size={20} />,
    path: "/user/clinical-forms",
  },
];

export const therapistMenuItems: MenuItem[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: <Widget5Icon size={20} />,
    path: "/therapist/dashboard",
  },
  {
    id: "clients",
    label: "Clients",
    icon: <UsersGroupTwoRoundedIcon size={20} />,
    path: "/therapist/clients",
  },
  {
    id: "scheduling",
    label: "Scheduling",
    icon: <CalendarIcon size={20} />,
    path: "/therapist/scheduling",
  },
  {
    id: "billings",
    label: "Billings",
    icon: <BillListIcon size={20} />,
    path: "/therapist/billings",
  },
  {
    id: "tasks",
    label: "Tasks",
    icon: <ClipboardListIcon size={20} />,
    path: "/therapist/tasks",
  },
];
