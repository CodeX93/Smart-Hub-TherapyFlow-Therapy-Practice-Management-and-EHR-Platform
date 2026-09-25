import { useCallback, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { clearSession } from "@/store/authSlice";
import { baseApi } from "@/store/api/baseApi";
import { usePortalLogoutMutation } from "@/store/api/portalAuthApi";

/** Matches server `app.auth.session.idle-timeout-seconds` (default 900). */
const DEFAULT_IDLE_TIMEOUT_MS = 900_000;
const ACTIVITY_EVENTS: Array<keyof WindowEventMap> = [
  "mousemove",
  "mousedown",
  "keydown",
  "touchstart",
  "scroll",
  "click",
];

/**
 * Client portal idle logout: after `idleTimeoutMs` without activity, revoke
 * the portal session and return to client login.
 */
export function usePortalIdleLogout(idleTimeoutMs: number = DEFAULT_IDLE_TIMEOUT_MS) {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const authRole = useAppSelector((state) => state.auth.role);
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const [portalLogout] = usePortalLogoutMutation();
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const loggingOutRef = useRef(false);

  const isPortal = Boolean(accessToken) && authRole === "user";

  const performLogout = useCallback(async () => {
    if (loggingOutRef.current) return;
    loggingOutRef.current = true;
    try {
      await portalLogout().unwrap();
    } catch {
      // Still clear local session on idle even if server logout fails.
    } finally {
      dispatch(clearSession());
      dispatch(baseApi.util.resetApiState());
      navigate("/auth/login", { replace: true });
      loggingOutRef.current = false;
    }
  }, [dispatch, navigate, portalLogout]);

  const resetTimer = useCallback(() => {
    if (!isPortal) return;
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      void performLogout();
    }, idleTimeoutMs);
  }, [idleTimeoutMs, isPortal, performLogout]);

  useEffect(() => {
    if (!isPortal) {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
      return;
    }

    resetTimer();
    const onActivity = () => resetTimer();
    for (const event of ACTIVITY_EVENTS) {
      window.addEventListener(event, onActivity, { passive: true });
    }
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
      for (const event of ACTIVITY_EVENTS) {
        window.removeEventListener(event, onActivity);
      }
    };
  }, [isPortal, resetTimer]);
}
