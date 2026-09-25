import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { getAuthSession, setAuthSession, clearAuthSession } from "@/utils/authStorage";
import type { AppRole } from "@/utils/roleMapper";
import type { ClientProfile, StaffProfile } from "@/store/api/authApi";
import { clearStoredSessionTranscripts } from "@/components/sessions/recording/sessionTranscriptStore";

const STAFF_PORTAL_ROLES: AppRole[] = ["admin", "therapist", "staff"];

function needsAuthBootstrap(role: AppRole | null | undefined): boolean {
  return Boolean(role && STAFF_PORTAL_ROLES.includes(role));
}

type AuthState = {
  accessToken: string | null;
  role: AppRole | null;
  tenantSubdomain: string | null;
  tenantSlug: string | null;
  tenantSchema: string | null;
  organisationId: number | null;
  organisationSlug: string | null;
  user: StaffProfile | null;
  client: ClientProfile | null;
  apiRoles: string[];
  permissions: string[];
  authBootstrapResolved: boolean;
  /**
   * A reload keeps the session's role but not its access token, which lives in
   * memory only. Until the refresh cookie is exchanged for a new one, the app
   * must wait rather than treat the visitor as signed out.
   */
  sessionRestorePending: boolean;
};

const stored = getAuthSession();

const initialState: AuthState = {
  accessToken: stored?.accessToken || null,
  role: stored?.role ?? null,
  tenantSubdomain: stored?.tenantSubdomain ?? null,
  tenantSlug: stored?.tenantSlug ?? null,
  tenantSchema: stored?.tenantSchema ?? null,
  organisationId: stored?.organisationId ?? null,
  organisationSlug: stored?.organisationSlug ?? null,
  user: (stored?.user as StaffProfile) ?? null,
  client: (stored?.client as ClientProfile) ?? null,
  apiRoles: stored?.apiRoles ?? [],
  permissions: stored?.permissions ?? [],
  authBootstrapResolved: !needsAuthBootstrap(stored?.role ?? null),
  sessionRestorePending: Boolean(stored?.role && !stored.accessToken),
};

type SessionPayload = {
  accessToken: string;
  role: AppRole;
  tenantSubdomain?: string;
  tenantSlug?: string;
  tenantSchema?: string;
  organisationId?: number;
  organisationSlug?: string;
  user?: StaffProfile;
  client?: ClientProfile;
  apiRoles?: string[];
  permissions?: string[];
  authBootstrapResolved?: boolean;
};

type AuthContextPayload = {
  apiRoles?: string[];
  permissions?: string[];
  tenantSchema?: string | null;
  organisationId?: number | null;
  organisationSlug?: string | null;
  user?: StaffProfile | null;
  role?: AppRole;
};

function persistSession(state: AuthState, overrides?: Partial<SessionPayload>) {
  if (!state.role || !state.accessToken) return;

  setAuthSession({
    accessToken: overrides?.accessToken ?? state.accessToken,
    role: overrides?.role ?? state.role,
    tenantSubdomain: overrides?.tenantSubdomain ?? state.tenantSubdomain ?? undefined,
    tenantSlug: overrides?.tenantSlug ?? state.tenantSlug ?? undefined,
    tenantSchema: overrides?.tenantSchema ?? state.tenantSchema ?? undefined,
    organisationId: overrides?.organisationId ?? state.organisationId ?? undefined,
    organisationSlug: overrides?.organisationSlug ?? state.organisationSlug ?? undefined,
    user: overrides?.user ?? state.user ?? undefined,
    client: overrides?.client ?? state.client ?? undefined,
    apiRoles: overrides?.apiRoles ?? state.apiRoles,
    permissions: overrides?.permissions ?? state.permissions,
  });
}

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setSession: (state, action: PayloadAction<SessionPayload>) => {
      state.accessToken = action.payload.accessToken;
      state.role = action.payload.role;
      state.tenantSubdomain = action.payload.tenantSubdomain ?? null;
      state.tenantSlug = action.payload.tenantSlug ?? null;
      state.tenantSchema = action.payload.tenantSchema ?? null;
      state.organisationId = action.payload.organisationId ?? null;
      state.organisationSlug = action.payload.organisationSlug ?? null;
      state.user = action.payload.user ?? null;
      state.client = action.payload.client ?? null;
      state.apiRoles = action.payload.apiRoles ?? [];
      state.permissions = action.payload.permissions ?? [];
      state.authBootstrapResolved =
        action.payload.authBootstrapResolved ??
        !needsAuthBootstrap(action.payload.role);
      state.sessionRestorePending = false;
      persistSession(state, action.payload);
    },
    setAuthContext: (state, action: PayloadAction<AuthContextPayload>) => {
      if (action.payload.apiRoles) {
        state.apiRoles = action.payload.apiRoles;
      }
      if (action.payload.permissions) {
        state.permissions = action.payload.permissions;
      }
      if (action.payload.tenantSchema !== undefined) {
        state.tenantSchema = action.payload.tenantSchema;
      }
      if (action.payload.organisationId !== undefined) {
        state.organisationId = action.payload.organisationId;
      }
      if (action.payload.organisationSlug !== undefined) {
        state.organisationSlug = action.payload.organisationSlug;
      }
      if (action.payload.user !== undefined) {
        state.user = action.payload.user;
      }
      if (action.payload.role) {
        state.role = action.payload.role;
      }
      state.authBootstrapResolved = true;
      persistSession(state);
    },
    clearSession: (state) => {
      state.accessToken = null;
      state.role = null;
      state.tenantSubdomain = null;
      state.tenantSlug = null;
      state.tenantSchema = null;
      state.organisationId = null;
      state.organisationSlug = null;
      state.user = null;
      state.client = null;
      state.apiRoles = [];
      state.permissions = [];
      state.authBootstrapResolved = true;
      state.sessionRestorePending = false;
      clearAuthSession();
      clearStoredSessionTranscripts();
    },
    updateTokens: (state, action: PayloadAction<{ accessToken: string }>) => {
      state.accessToken = action.payload.accessToken;
      state.sessionRestorePending = false;
      persistSession(state, action.payload);
    },
  },
});

export const { setSession, setAuthContext, clearSession, updateTokens } = authSlice.actions;
export default authSlice.reducer;
