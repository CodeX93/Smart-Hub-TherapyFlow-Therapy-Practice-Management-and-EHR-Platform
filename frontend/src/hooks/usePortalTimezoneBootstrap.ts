import { useEffect, useRef } from "react";
import { useAppSelector } from "@/store/hooks";
import {
  useGetPortalMeQuery,
  useUpdatePortalTimezoneMutation,
} from "@/store/api/portalApi";
import {
  getSafeBrowserTimezone,
  hasPortalTimezone,
} from "@/utils/timezoneValidation";

/**
 * Seeds a portal account's timezone from the client's device, once.
 *
 * This runs only while GET /portal/me reports no timezone, so it is a first-login
 * guess at where the client is, not a standing sync. Once a timezone exists -
 * whether seeded here or chosen by the client - nothing overwrites it: booking and
 * reschedule deliberately never infer a zone from a request offset. Failures are
 * silent; a client with no timezone falls back to the clinic's.
 */
export function usePortalTimezoneBootstrap(): void {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const authRole = useAppSelector((state) => state.auth.role);
  const isPortalClient = Boolean(accessToken) && authRole === "user";

  const { data: profile, isLoading, isFetching, isError } =
    useGetPortalMeQuery(undefined, {
      skip: !isPortalClient,
    });
  const [updateTimezone] = useUpdatePortalTimezoneMutation();
  const syncAttemptedRef = useRef(false);

  useEffect(() => {
    if (!isPortalClient) return;
    if (isLoading || isFetching) return;
    if (isError || !profile) return;
    if (hasPortalTimezone(profile.timezone)) return;
    if (syncAttemptedRef.current) return;

    const browserTimezone = getSafeBrowserTimezone();
    if (!browserTimezone) return;

    syncAttemptedRef.current = true;

    void updateTimezone({ timezone: browserTimezone }).unwrap().catch(() => {
      // Keep attempted=true for this mount to avoid retry loops on failure.
      // A full page reload will try again once.
    });
  }, [
    isPortalClient,
    isError,
    isFetching,
    isLoading,
    profile,
    updateTimezone,
  ]);
}
