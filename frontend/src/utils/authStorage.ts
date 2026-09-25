import type { AppRole } from "./roleMapper";

export type StoredSession = {
  /** Held in memory only; empty after a reload until `refreshSession` runs. */
  accessToken: string;
  role: AppRole;
  tenantSubdomain?: string;
  tenantSlug?: string;
  tenantSchema?: string;
  organisationId?: number;
  organisationSlug?: string;
  user?: unknown;
  client?: unknown;
  apiRoles?: string[];
  permissions?: string[];
};

type PersistedSession = Omit<StoredSession, "accessToken">;

const AUTH_SESSION_KEY = "auth_session";
const LEGACY_USER_KEY = "loggedInUser";

/**
 * Tokens never touch localStorage, where any script on the page could read them.
 * The access token lives here for the life of the tab; the refresh token is an
 * HttpOnly cookie the backend owns.
 */
let memorySession: StoredSession | null = null;

function persist(session: PersistedSession): string {
  const serialized = JSON.stringify(session);
  localStorage.setItem(AUTH_SESSION_KEY, serialized);
  localStorage.setItem(LEGACY_USER_KEY, JSON.stringify({ role: session.role }));
  return serialized;
}

export function setAuthSession(session: StoredSession) {
  const persisted: Partial<StoredSession> = { ...session };
  delete persisted.accessToken;
  const serialized = persist(persisted as PersistedSession);
  // Keep a detached copy, never the caller's objects: authSlice passes Immer
  // drafts, which are revoked when the reducer returns and throw on any later read.
  memorySession = {
    ...(JSON.parse(serialized) as PersistedSession),
    accessToken: session.accessToken,
  };
}

export function getAuthSession(): StoredSession | null {
  if (memorySession) return memorySession;
  const raw = localStorage.getItem(AUTH_SESSION_KEY);
  if (!raw || raw === "undefined") {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as PersistedSession & {
      accessToken?: unknown;
      refreshToken?: unknown;
    };
    const { accessToken, refreshToken, ...persisted } = parsed;
    // Sessions saved before tokens left localStorage still hold them: scrub them.
    if (accessToken !== undefined || refreshToken !== undefined) {
      persist(persisted);
    }
    return { ...persisted, accessToken: "" };
  } catch {
    return null;
  }
}

export function clearAuthSession() {
  memorySession = null;
  localStorage.removeItem(AUTH_SESSION_KEY);
  localStorage.removeItem(LEGACY_USER_KEY);
}

export function getStoredUserRole(): StoredSession["role"] | null {
  const session = getAuthSession();
  if (session?.role) {
    return session.role;
  }

  const legacyRaw = localStorage.getItem(LEGACY_USER_KEY);
  if (!legacyRaw || legacyRaw === "undefined") {
    return null;
  }

  try {
    const legacy = JSON.parse(legacyRaw) as { role?: StoredSession["role"] };
    return legacy.role ?? null;
  } catch {
    return null;
  }
}
