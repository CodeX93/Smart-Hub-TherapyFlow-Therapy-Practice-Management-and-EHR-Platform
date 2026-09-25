import { staffDefaultLandingPath } from "./staffMenu";
import { getStaffLandingPath } from "./staffPermissions";

export const getRedirectPathByRole = (
  role: string,
  options?: {
    permissions?: string[];
    apiRoles?: string[];
  },
) => {
  switch (role) {
    case "admin":
      return "/admin/dashboard";
    case "staff":
      if (options?.permissions && options?.apiRoles) {
        return getStaffLandingPath(options.permissions, options.apiRoles);
      }
      return staffDefaultLandingPath;
    case "super-admin":
      return "/super-admin/dashboard";
    case "therapist":
      return "/therapist/dashboard";
    case "user":
    default:
      return "/user/appointments";
  }
};

/**
 * Where a visitor with no session signs in. The area they were trying to open
 * says who they are - a signed-out therapist opening /therapist/... belongs on
 * the staff sign-in, not the client portal's. Anywhere else, the platform
 * console's own host sends them to its sign-in, and everyone else to the client
 * portal's.
 */
export const getDefaultLoginPath = (
  pathname: string = window.location.pathname,
): string => {
  if (pathname === "/super-admin" || pathname.startsWith("/super-admin/")) {
    return "/super-admin/login";
  }
  if (/^\/(admin|therapist|staff)(\/|$)/.test(pathname)) {
    return "/auth/staff/login";
  }
  const hostname = window.location.hostname;
  const isSuperAdminSubdomain =
    hostname.startsWith("superadmin.") || hostname.startsWith("super-admin.");
  return isSuperAdminSubdomain ? "/super-admin/login" : "/auth/login";
};

/** The sign-in page that owns a role's session: where it goes once that session ends. */
export const getLoginPathForRole = (role: string | null | undefined): string => {
  if (role === "super-admin") {
    return "/super-admin/login";
  }
  if (role === "admin" || role === "therapist" || role === "staff") {
    return "/auth/staff/login";
  }
  if (role === "user") {
    return "/auth/login";
  }
  // No role left (e.g. already signed out): go by the area being opened.
  return getDefaultLoginPath();
};
