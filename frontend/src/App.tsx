import { useEffect } from "react";
import AppRoutes from "./routes";
import { getDefaultLoginPath, getRedirectPathByRole } from "./utils/redirectPathByRole";
import { getStaffLandingPath } from "./utils/staffPermissions";
import { useLocation, useNavigate } from "react-router-dom";
import { useScrollToTop } from "./hooks/useScrollToTop";
import { getAuthSession, getStoredUserRole } from "./utils/authStorage";
import { useSessionRestore } from "./hooks/useSessionRestore";
import { ContentLoader } from "@/components/shared/ContentLoader";

function getStaffRedirectPath(): string {
  const session = getAuthSession();
  if (session?.role === "staff") {
    return getStaffLandingPath(
      session.permissions ?? [],
      session.apiRoles ?? [],
    );
  }
  return getRedirectPathByRole("staff");
}

const App = () => {
  useScrollToTop();
  const isRestoringSession = useSessionRestore();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const role = getStoredUserRole();
    const currentPath = location.pathname;

    if (!role) {
      const defaultLoginPath = getDefaultLoginPath(currentPath);

      // Exempt explicit login paths from unauthenticated redirects
      if (
        currentPath.startsWith("/auth/") ||
        currentPath === "/auth" ||
        currentPath === "/super-admin/login" ||
        currentPath === "/stripe/success" ||
        currentPath === "/billing/subscription/success"
      ) {
        return;
      }

      // If not authenticated and on a protected route or landing page, redirect to default login
      if (
        currentPath === "/" ||
        currentPath.startsWith("/user") ||
        currentPath.startsWith("/therapist") ||
        currentPath.startsWith("/admin") ||
        currentPath.startsWith("/staff") ||
        currentPath.startsWith("/super-admin")
      ) {
        navigate(defaultLoginPath, { replace: true });
      }
      return;
    }

    if (role === "staff" && currentPath.startsWith("/admin")) {
      navigate(getStaffRedirectPath(), { replace: true });
      return;
    }

    if (role === "admin" && currentPath.startsWith("/staff")) {
      navigate(getRedirectPathByRole("admin"), { replace: true });
      return;
    }

    if (role === "therapist" && currentPath.startsWith("/staff")) {
      navigate(getRedirectPathByRole("therapist"), { replace: true });
      return;
    }

    if (role === "staff" && currentPath.startsWith("/therapist")) {
      navigate(getStaffRedirectPath(), { replace: true });
      return;
    }

    // If authenticated and on root or auth pages, redirect to default route for role
    if (
      currentPath === "/" ||
      currentPath === "/auth" ||
      currentPath.startsWith("/auth/") ||
      currentPath === "/super-admin/login"
    ) {
      const session = getAuthSession();
      const redirectPath =
        role === "staff"
          ? getStaffLandingPath(
              session?.permissions ?? [],
              session?.apiRoles ?? [],
            )
          : getRedirectPathByRole(role);
      if (currentPath !== redirectPath) {
        navigate(redirectPath, { replace: true });
      }
    }
  }, [location.pathname, navigate]);
  if (isRestoringSession) {
    return <ContentLoader className="h-screen" />;
  }

  return (
    <>
      <AppRoutes />
    </>
  );
};

export default App;
