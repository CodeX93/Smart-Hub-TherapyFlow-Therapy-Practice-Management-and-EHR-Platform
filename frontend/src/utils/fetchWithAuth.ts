import { refreshSession } from "@/utils/authRefresh";
import { getAuthSession } from "@/utils/authStorage";
import { resolveApiBaseForSession } from "@/utils/tenantUrls";

/** Authenticated fetch with a single 401 → refresh → retry cycle. */
export async function fetchWithAuth(path: string, init?: RequestInit): Promise<Response> {
  let session = getAuthSession();
  const baseUrl = resolveApiBaseForSession(session);
  const url = `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;

  const withToken = (accessToken?: string) =>
    fetch(url, {
      ...init,
      credentials: "include",
      headers: {
        ...init?.headers,
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
    });

  // After a reload the access token is gone until the refresh cookie is exchanged.
  if (session?.role && !session.accessToken) {
    const restored = await refreshSession();
    if (restored.status === "ok") session = restored.session;
  }

  let response = await withToken(session?.accessToken);

  if (response.status === 401 && session?.role) {
    const refreshed = await refreshSession();
    if (refreshed.status === "ok") {
      response = await withToken(refreshed.session.accessToken);
    }
  }

  return response;
}
