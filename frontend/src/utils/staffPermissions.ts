import type { AppRole } from "./roleMapper";
import { isCustomStaffApiRole } from "./roleMapper";

export type StaffRouteId =
  | "clients"
  | "scheduling"
  | "billings"
  | "tasks"
  | "user-profiles"
  | "content-library"
  | "content-assessment"
  | "content-clinical-forms"
  | "content-process-checklists"
  | "compliance-hipaa"
  | "compliance-privacy"
  | "system-notifications";

export type StaffCapability =
  | "deleteClient"
  | "restoreClient"
  | "bulkPortalAccess"
  | "manageUsers"
  | "manageRoles"
  | "manageNotificationSetup"
  | "editChecklistTemplates"
  | "manageLibraryTags"
  | "editSystemOptions"
  | "editPracticeConfiguration"
  | "paySubscriptionInvoice"
  | "manageBillingServices"
  | "manageInvoicePolicies";

export const staffNoAccessPath = "/staff/no-access";

const SUPERVISOR_BLOCKED_CAPABILITIES: StaffCapability[] = [
  "deleteClient",
  "restoreClient",
  "bulkPortalAccess",
  "manageUsers",
  "manageRoles",
  "manageNotificationSetup",
  "editChecklistTemplates",
  "manageLibraryTags",
  "editSystemOptions",
  "editPracticeConfiguration",
  "paySubscriptionInvoice",
  "manageBillingServices",
  "manageInvoicePolicies",
];

const STAFF_ROUTE_PERMISSIONS: Record<
  StaffRouteId,
  { permissions?: string[]; anyPermission?: string[] }
> = {
  clients: {
    anyPermission: [
      "CLIENT_VIEW",
      "CLIENT_VIEW_ALL",
      "CLIENT_VIEW_OWN",
      "CLIENT_VIEW_TEAM",
    ],
  },
  scheduling: {
    anyPermission: ["SESSION_VIEW", "SESSION_CREATE", "SESSION_EDIT"],
  },
  billings: { anyPermission: ["BILLING_VIEW", "BILLING_MANAGE", "BILLING_EDIT"] },
  tasks: { permissions: ["CONSENT_ADMIN_VIEW"] },
  "user-profiles": {
    anyPermission: [
      "USER_VIEW",
      "USER_MANAGE",
      "USER_CREATE",
      "USER_EDIT",
      "USER_DELETE",
    ],
  },
  "content-library": { permissions: ["CONSENT_ADMIN_VIEW"] },
  "content-assessment": {
    anyPermission: ["ASSESSMENT_VIEW", "ASSESSMENT_ASSIGN", "CONSENT_ADMIN_VIEW"],
  },
  "content-clinical-forms": { anyPermission: ["FORM_VIEW", "CONSENT_ADMIN_VIEW"] },
  "content-process-checklists": { permissions: ["CONSENT_ADMIN_VIEW"] },
  "compliance-hipaa": { anyPermission: ["AUDIT_VIEW", "CONSENT_ADMIN_VIEW"] },
  "compliance-privacy": { permissions: ["CONSENT_ADMIN_VIEW"] },
  "system-notifications": { permissions: ["CONSENT_ADMIN_VIEW"] },
};

export const STAFF_ROUTE_PATHS: Record<StaffRouteId, string> = {
  clients: "/staff/clients",
  scheduling: "/staff/scheduling",
  billings: "/staff/billings",
  tasks: "/staff/tasks",
  "user-profiles": "/staff/user-access/profiles",
  "content-library": "/staff/content/library",
  "content-assessment": "/staff/content/assessment",
  "content-clinical-forms": "/staff/content/clinical-forms",
  "content-process-checklists": "/staff/content/process-checklists",
  "compliance-hipaa": "/staff/compliance/hipaa",
  "compliance-privacy": "/staff/compliance/privacy",
  "system-notifications": "/staff/system/notifications",
};

export const STAFF_LANDING_ROUTE_ORDER: StaffRouteId[] = [
  "clients",
  "scheduling",
  "billings",
  "tasks",
  "user-profiles",
  "content-library",
  "content-assessment",
  "content-clinical-forms",
  "content-process-checklists",
  "compliance-hipaa",
  "compliance-privacy",
  "system-notifications",
];

const ALL_STAFF_CAPABILITIES: StaffCapability[] = [
  "deleteClient",
  "restoreClient",
  "bulkPortalAccess",
  "manageUsers",
  "manageRoles",
  "manageNotificationSetup",
  "editChecklistTemplates",
  "manageLibraryTags",
  "editSystemOptions",
  "editPracticeConfiguration",
  "paySubscriptionInvoice",
  "manageBillingServices",
  "manageInvoicePolicies",
];

const CUSTOM_STAFF_CAPABILITY_PERMISSIONS: Record<
  StaffCapability,
  { permission?: string; anyPermission?: string[] }
> = {
  deleteClient: { permission: "CLIENT_DELETE" },
  restoreClient: { permission: "CLIENT_DELETE" },
  bulkPortalAccess: { permission: "CLIENT_EDIT" },
  manageUsers: { permission: "USER_MANAGE" },
  manageRoles: { permission: "ROLE_ADMIN" },
  manageNotificationSetup: { permission: "CONSENT_ADMIN_VIEW" },
  editChecklistTemplates: { permission: "CONSENT_ADMIN_VIEW" },
  manageLibraryTags: { anyPermission: ["USER_VIEW", "CONSENT_ADMIN_VIEW"] },
  editSystemOptions: { permission: "CONSENT_ADMIN_VIEW" },
  editPracticeConfiguration: { permission: "CONSENT_ADMIN_VIEW" },
  paySubscriptionInvoice: { anyPermission: ["BILLING_EDIT", "BILLING_MANAGE"] },
  manageBillingServices: { permission: "BILLING_MANAGE" },
  manageInvoicePolicies: { permission: "BILLING_MANAGE" },
};

export function normalizeApiRoles(roles: string[] | undefined): string[] {
  if (!roles?.length) return [];
  return roles.map((role) => role.trim().toUpperCase()).filter(Boolean);
}

export function hasPermission(permissions: string[], permission: string): boolean {
  const normalized = permission.trim().toUpperCase();
  return permissions.some((entry) => entry.trim().toUpperCase() === normalized);
}

export function hasAnyPermission(permissions: string[], required: string[]): boolean {
  return required.some((permission) => hasPermission(permissions, permission));
}

export function hasAllPermissions(permissions: string[], required: string[]): boolean {
  return required.every((permission) => hasPermission(permissions, permission));
}

export function isSupervisorApiRole(apiRoles: string[]): boolean {
  return apiRoles.includes("SUPERVISOR");
}

export function isCustomStaffUser(
  appRole: AppRole | null,
  apiRoles: string[],
): boolean {
  return appRole === "staff" && isCustomStaffApiRole(apiRoles);
}

export function shouldShowFullStaffMenu(
  permissions: string[],
  apiRoles: string[],
): boolean {
  return isSupervisorApiRole(apiRoles) && !permissions.length;
}

export function canAccessStaffRoute(
  permissions: string[],
  routeId: StaffRouteId,
): boolean {
  const rule = STAFF_ROUTE_PERMISSIONS[routeId];
  if (rule.permissions?.length && !hasAllPermissions(permissions, rule.permissions)) {
    return false;
  }
  if (
    rule.anyPermission?.length &&
    !hasAnyPermission(permissions, rule.anyPermission)
  ) {
    return false;
  }
  return true;
}

export function getAccessibleStaffRoutes(permissions: string[]): StaffRouteId[] {
  return STAFF_LANDING_ROUTE_ORDER.filter((routeId) =>
    canAccessStaffRoute(permissions, routeId),
  );
}

export function getStaffLandingPath(
  permissions: string[],
  apiRoles: string[],
): string {
  if (shouldShowFullStaffMenu(permissions, apiRoles)) {
    return STAFF_ROUTE_PATHS.clients;
  }

  const accessibleRoutes = getAccessibleStaffRoutes(permissions);
  if (!accessibleRoutes.length) {
    return staffNoAccessPath;
  }

  return STAFF_ROUTE_PATHS[accessibleRoutes[0]];
}

function hasCustomStaffCapabilityPermission(
  permissions: string[],
  capability: StaffCapability,
): boolean {
  const rule = CUSTOM_STAFF_CAPABILITY_PERMISSIONS[capability];
  if (rule.permission) {
    return hasPermission(permissions, rule.permission);
  }
  if (rule.anyPermission?.length) {
    return hasAnyPermission(permissions, rule.anyPermission);
  }
  return false;
}

export function getStaffCapabilities(
  appRole: AppRole | null,
  apiRoles: string[],
): Record<StaffCapability, boolean> {
  const allEnabled = Object.fromEntries(
    ALL_STAFF_CAPABILITIES.map((capability) => [capability, true]),
  ) as Record<StaffCapability, boolean>;

  if (appRole !== "staff") {
    return allEnabled;
  }

  if (isCustomStaffApiRole(apiRoles)) {
    return Object.fromEntries(
      ALL_STAFF_CAPABILITIES.map((capability) => [capability, false]),
    ) as Record<StaffCapability, boolean>;
  }

  const capabilities = { ...allEnabled };
  if (isSupervisorApiRole(apiRoles)) {
    for (const blocked of SUPERVISOR_BLOCKED_CAPABILITIES) {
      capabilities[blocked] = false;
    }
  }

  return capabilities;
}

export function canStaffCapability(
  appRole: AppRole | null,
  apiRoles: string[],
  permissions: string[],
  capability: StaffCapability,
): boolean {
  if (isCustomStaffUser(appRole, apiRoles)) {
    return hasCustomStaffCapabilityPermission(permissions, capability);
  }

  const capabilities = getStaffCapabilities(appRole, apiRoles);
  if (!capabilities[capability]) {
    return false;
  }

  switch (capability) {
    case "deleteClient":
    case "restoreClient":
      return hasPermission(permissions, "CLIENT_DELETE") && capabilities[capability];
    case "bulkPortalAccess":
      return hasPermission(permissions, "CLIENT_EDIT") && capabilities[capability];
    case "manageUsers":
    case "manageRoles":
      return hasPermission(permissions, "USER_MANAGE") && capabilities[capability];
    case "manageBillingServices":
    case "manageInvoicePolicies":
    case "paySubscriptionInvoice":
      return (
        hasAnyPermission(permissions, ["BILLING_EDIT", "BILLING_MANAGE"]) &&
        capabilities[capability]
      );
    default:
      return capabilities[capability];
  }
}

export function buildStaffPermissionSet(permissions: string[]): Set<string> {
  return new Set(
    permissions
      .map((entry) => entry.trim().toUpperCase())
      .filter(Boolean),
  );
}

export function staffHasAny(
  permissions: string[] | Set<string>,
  ...keys: string[]
): boolean {
  const perms =
    permissions instanceof Set ? permissions : buildStaffPermissionSet(permissions);
  return keys.some((key) => perms.has(key.trim().toUpperCase()));
}

export function canViewAssessments(permissions: string[] | Set<string>): boolean {
  return staffHasAny(permissions, "ASSESSMENT_VIEW", "CONSENT_ADMIN_VIEW");
}

export function canAssignAssessments(permissions: string[] | Set<string>): boolean {
  return staffHasAny(permissions, "ASSESSMENT_ASSIGN", "CONSENT_ADMIN_VIEW");
}

export function canWriteLibrary(permissions: string[] | Set<string>): boolean {
  return staffHasAny(permissions, "CONSENT_ADMIN_VIEW");
}

export function canReadLibraryTags(permissions: string[] | Set<string>): boolean {
  return staffHasAny(permissions, "USER_VIEW", "CONSENT_ADMIN_VIEW");
}
