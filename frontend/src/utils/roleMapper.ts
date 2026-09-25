export type AppRole = "super-admin" | "admin" | "therapist" | "staff" | "user";

const ROLE_PRIORITY: AppRole[] = [
  "super-admin",
  "admin",
  "therapist",
  "staff",
  "user",
];

export const FIXED_PORTAL_API_ROLES = new Set([
  "SUPER_ADMIN",
  "PLATFORM_SUPER_ADMIN",
  "ADMIN",
  "THERAPIST",
  "SUPERVISOR",
  "CLIENT",
]);

export function normalizePortalApiRoles(roles: string[] | undefined): string[] {
  if (!roles?.length) return [];
  return roles.map((role) => role.trim().toUpperCase()).filter(Boolean);
}

export function isCustomStaffApiRole(apiRoles: string[]): boolean {
  if (!apiRoles.length || apiRoles.includes("CLIENT")) return false;
  return !apiRoles.some((role) => FIXED_PORTAL_API_ROLES.has(role));
}

export function mapApiRolesToAppRole(roles: string[] | undefined): AppRole {
  if (!roles || roles.length === 0) {
    return "user";
  }

  const normalized = normalizePortalApiRoles(roles);
  const map: Record<string, AppRole> = {
    SUPER_ADMIN: "super-admin",
    PLATFORM_SUPER_ADMIN: "super-admin",
    ADMIN: "admin",
    THERAPIST: "therapist",
    SUPERVISOR: "therapist",
    CLIENT: "user",
  };

  const mapped = normalized
    .map((role) => map[role])
    .filter(Boolean) as AppRole[];

  for (const role of ROLE_PRIORITY) {
    if (mapped.includes(role)) {
      return role;
    }
  }

  if (isCustomStaffApiRole(normalized)) {
    return "staff";
  }

  return "user";
}
