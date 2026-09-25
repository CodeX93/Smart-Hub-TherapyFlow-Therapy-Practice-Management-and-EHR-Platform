import { useCallback, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { clearSession } from "@/store/authSlice";
import { baseApi } from "@/store/api/baseApi";
import { useLogoutStaffMutation } from "@/store/api/authApi";
import type { AppRole } from "@/utils/roleMapper";

function getStaffIdleLogoutPath(role: AppRole | null): string {
  if (role === "super-admin") {
    return "/super-admin/login";
  }
  return "/auth/therapist/login";
}

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
 * Staff-only idle logout: after `idleTimeoutMs` without user activity, logout
 * and clear session (mirrors backend idle timeout for local HIPAA posture).
 */
export function useStaffIdleLogout(idleTimeoutMs: number = DEFAULT_IDLE_TIMEOUT_MS) {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const authRole = useAppSelector((state) => state.auth.role);
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const [logoutStaff] = useLogoutStaffMutation();
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const loggingOutRef = useRef(false);

  const isStaff =
    Boolean(accessToken) &&
    authRole != null &&
    authRole !== "user";

  const logoutPath = getStaffIdleLogoutPath(authRole);

  const performLogout = useCallback(async () => {
    if (loggingOutRef.current) return;
    loggingOutRef.current = true;
    try {
      await logoutStaff().unwrap();
    } catch {
      // Still clear local session on idle even if server logout fails.
    } finally {
      dispatch(clearSession());
      dispatch(baseApi.util.resetApiState());
      navigate(logoutPath, { replace: true });
      loggingOutRef.current = false;
    }
  }, [dispatch, logoutPath, logoutStaff, navigate]);

  const resetTimer = useCallback(() => {
    if (!isStaff) return;
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      void performLogout();
    }, idleTimeoutMs);
  }, [idleTimeoutMs, isStaff, performLogout]);

  useEffect(() => {
    if (!isStaff) {
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
  }, [isStaff, resetTimer]);
}
