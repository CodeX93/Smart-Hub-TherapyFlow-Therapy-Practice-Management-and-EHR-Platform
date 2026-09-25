import { useEffect, useState } from "react";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { clearSession, updateTokens } from "@/store/authSlice";
import { refreshSession } from "@/utils/authRefresh";
import { getLoginPathForRole } from "@/utils/redirectPathByRole";

/** How long to wait before asking again while the server cannot be reached. */
const UNAVAILABLE_RETRY_MS = 5000;

/**
 * After a reload the session's role survives but its access token, kept in memory
 * only, does not. Exchange the refresh cookie for a new one before any guard sees
 * the store.
 *
 * Only a refusal ends the session: it is cleared and the visitor lands on the
 * sign-in page of the role they had. A rate limit, server error or network failure
 * says nothing about the session, so the app keeps waiting and asks again.
 *
 * Returns true while the session is still being restored.
 */
export function useSessionRestore(): boolean {
  const dispatch = useAppDispatch();
  const pending = useAppSelector((state) => state.auth.sessionRestorePending);
  const role = useAppSelector((state) => state.auth.role);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!pending) return;
    let cancelled = false;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;

    // StrictMode runs this twice; refreshSession shares one request between them.
    void refreshSession().then((outcome) => {
      if (cancelled) return;
      if (outcome.status === "ok") {
        dispatch(updateTokens({ accessToken: outcome.session.accessToken }));
      } else if (outcome.status === "ended") {
        dispatch(clearSession());
        // A full navigation, so no route guard on the protected page can first
        // send the cleared session to the default (client) sign-in instead.
        window.location.replace(getLoginPathForRole(role));
      } else {
        retryTimer = setTimeout(() => setAttempt((n) => n + 1), UNAVAILABLE_RETRY_MS);
      }
    });

    return () => {
      cancelled = true;
      if (retryTimer) clearTimeout(retryTimer);
    };
  }, [attempt, dispatch, pending, role]);

  return pending;
}
