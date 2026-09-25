import type { AppRole } from "@/utils/roleMapper";

const staffRefreshPath =
  import.meta.env.VITE_API_AUTH_REFRESH_PATH ?? "/api/v1/auth/refresh";
const clientRefreshPath =
  import.meta.env.VITE_API_PORTAL_REFRESH_PATH ?? "/api/v1/portal/refresh";

export function getRefreshPathForRole(role: AppRole): string {
  if (role === "user") {
    return clientRefreshPath;
  }

  return staffRefreshPath;
}
