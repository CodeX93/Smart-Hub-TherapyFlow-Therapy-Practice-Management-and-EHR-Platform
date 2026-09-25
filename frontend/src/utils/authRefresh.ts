import { getRefreshPathForRole } from "@/utils/authApiPaths";
import { getAuthSession, setAuthSession, type StoredSession } from "@/utils/authStorage";
import { resolveApiBaseForSession } from "@/utils/tenantUrls";

/**
 * The one way this app gets a new access token.
 *
 * The refresh token lives in an HttpOnly cookie the backend sets, so script never
 * sees it: the request just carries credentials. The backend rotates that cookie on
 * every refresh and treats a replayed one as theft, revoking the whole session, so
 * two refreshes must never race with the same cookie. Calls in this tab share one
 * request, and a Web Lock makes other tabs wait their turn, by which time the
 * browser already holds the rotated cookie.
 */

const REFRESH_LOCK = "tf-auth-refresh";

/**
 * What a refresh came to. Only "ended" means the session is over (the backend
 * refused the cookie); "unavailable" (rate limited, server error, offline) says
 * nothing about the session and must not sign anyone out.
 */
export type RefreshOutcome =
  | { status: "ok"; session: StoredSession }
  | { status: "ended" }
  | { status: "unavailable" };

/** Pauses between attempts when the refresh endpoint is momentarily unavailable. */
const RETRY_DELAYS_MS = [500, 1500, 3000];

let inFlight: Promise<RefreshOutcome> | null = null;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

async function requestRefresh(session: StoredSession): Promise<RefreshOutcome> {
  const base = resolveApiBaseForSession(session);
  let response: Response;
  try {
    response = await fetch(`${base}${getRefreshPathForRole(session.role)}`, {
      method: "POST",
      credentials: "include",
    });
  } catch {
    return { status: "unavailable" };
  }
  if (response.status === 401 || response.status === 403) return { status: "ended" };
  if (!response.ok) return { status: "unavailable" };

  const payload = (await response.json().catch(() => null)) as { accessToken?: unknown } | null;
  if (typeof payload?.accessToken !== "string" || !payload.accessToken) {
    return { status: "unavailable" };
  }

  // Re-read: the session may have changed (e.g. /auth/me context) while we waited.
  const latest = getAuthSession() ?? session;
  const next: StoredSession = { ...latest, accessToken: payload.accessToken };
  setAuthSession(next);
  return { status: "ok", session: next };
}

/** Retries only the "unavailable" outcome, a few times, before giving up for now. */
async function requestRefreshWithRetry(session: StoredSession): Promise<RefreshOutcome> {
  let outcome = await requestRefresh(session);
  for (const delay of RETRY_DELAYS_MS) {
    if (outcome.status !== "unavailable") break;
    await sleep(delay);
    outcome = await requestRefresh(session);
  }
  return outcome;
}

async function withCrossTabLock<T>(task: () => Promise<T>): Promise<T> {
  const locks = typeof navigator !== "undefined" ? navigator.locks : undefined;
  if (!locks) return task();
  // The lock is held until the task settles; its result passes straight through.
  return (await locks.request(REFRESH_LOCK, task)) as T;
}

/**
 * Exchanges the refresh cookie for a new access token and stores it in memory.
 * "ended" when there is no session to refresh or the backend refused the cookie
 * (expired, revoked, signed out elsewhere); "unavailable" when it could not be
 * asked (after a few retries).
 */
export function refreshSession(): Promise<RefreshOutcome> {
  if (!inFlight) {
    inFlight = withCrossTabLock(async (): Promise<RefreshOutcome> => {
      const session = getAuthSession();
      if (!session?.role) return { status: "ended" };
      return requestRefreshWithRetry(session);
    }).finally(() => {
      inFlight = null;
    });
  }
  return inFlight;
}
