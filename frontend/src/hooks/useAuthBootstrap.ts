import { useEffect } from "react";
import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { authApi } from "@/store/api/authApi";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { clearSession, setAuthContext } from "@/store/authSlice";
import { normalizeApiRoles } from "@/utils/staffPermissions";
import { mapApiRolesToAppRole } from "@/utils/roleMapper";
import type { AppRole } from "@/utils/roleMapper";

const STAFF_PORTAL_ROLES: AppRole[] = ["admin", "therapist", "staff"];

function needsAuthBootstrap(role: AppRole | null | undefined): boolean {
  return Boolean(role && STAFF_PORTAL_ROLES.includes(role));
}

export function useAuthBootstrap(): {
  isAuthBootstrapLoading: boolean;
} {
  const dispatch = useAppDispatch();
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const role = useAppSelector((state) => state.auth.role);
  const authBootstrapResolved = useAppSelector(
    (state) => state.auth.authBootstrapResolved,
  );

  const shouldBootstrap = Boolean(accessToken) && needsAuthBootstrap(role);

  useEffect(() => {
    if (!shouldBootstrap || authBootstrapResolved) return;

    // Run the request and await its own promise rather than watching a cache
    // entry. A subscription can be attached after the entry has already settled
    // and then never hears about it, which used to leave a portal shell on its
    // loader for ever; awaiting the request cannot miss its own result.
    const request = dispatch(
      authApi.endpoints.getAuthMe.initiate(undefined, { forceRefetch: true }),
    );

    let cancelled = false;

    void request.then((result) => {
      if (cancelled) return;

      const data = result.data;
      if (data) {
        dispatch(
          setAuthContext({
            apiRoles: normalizeApiRoles(data.roles),
            permissions: data.permissions ?? data.authorities ?? [],
            tenantSchema: data.tenantSchema,
            organisationId: data.organisationId,
            organisationSlug: data.organisationSlug,
            user: data.user,
            role: mapApiRolesToAppRole(data.roles),
          }),
        );
        return;
      }

      if ((result.error as FetchBaseQueryError | undefined)?.status === 401) {
        dispatch(clearSession());
        window.location.assign("/auth/staff/login");
        return;
      }

      // Any other failure still ends the bootstrap: the session stands, it just
      // carries no refreshed permissions.
      dispatch(setAuthContext({ permissions: [] }));
    });

    return () => {
      cancelled = true;
      request.unsubscribe();
    };
  }, [authBootstrapResolved, dispatch, shouldBootstrap]);

  const isAuthBootstrapLoading = shouldBootstrap && !authBootstrapResolved;

  return { isAuthBootstrapLoading };
}
