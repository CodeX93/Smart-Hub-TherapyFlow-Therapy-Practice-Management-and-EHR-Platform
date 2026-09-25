import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useLocation, useNavigate } from "react-router-dom";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";
import { useAppDispatch } from "@/store/hooks";
import { clearSession } from "@/store/authSlice";
import { baseApi } from "@/store/api/baseApi";
import { useAppSelector } from "@/store/hooks";
import { useGetAuthMeQuery, useLogoutStaffMutation } from "@/store/api/authApi";

function getInitials(value: string): string {
  const normalized = value.trim();
  if (!normalized) {
    return "NA";
  }

  const localPart = normalized.includes("@") ? normalized.split("@")[0] : normalized;
  const parts = localPart
    .split(/[\s._-]+/)
    .filter(Boolean);

  if (parts.length >= 2) {
    return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
  }

  const compact = parts[0] ?? localPart;
  return compact.slice(0, 2).toUpperCase();
}

const ProfileDropdown = ({
  initials,
  fullName,
  onLogout,
  className,
}: {
  initials: string;
  fullName: string;
  onLogout?: () => void;
  className?: string;
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const authRole = useAppSelector((state) => state.auth.role);
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const [logoutStaff] = useLogoutStaffMutation();
  const showMyProfile = location.pathname.startsWith("/super-admin");
  const { data: authMeData } = useGetAuthMeQuery(undefined, {
    skip: !accessToken || authRole === "user",
  });
  const displayName = authMeData?.username?.trim() || fullName;
  const displayInitials = getInitials(displayName || initials);

  const getPostLogoutPath = () => {
    if (authRole === "super-admin") return "/super-admin/login";
    if (authRole === "admin" || authRole === "therapist" || authRole === "staff") {
      return "/auth/staff/login";
    }
    return "/auth/login";
  };

  async function handleLogout() {
    try {
      if (authRole && authRole !== "user" && accessToken) {
        await logoutStaff().unwrap();
      }
    } catch {
      // Logout should always proceed locally even if the server call fails.
    } finally {
      dispatch(clearSession());
      dispatch(baseApi.util.resetApiState());
      onLogout?.();
      navigate(getPostLogoutPath(), { replace: true });
    }
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button type="button" className={cn("outline-none", className)}>
          <Avatar className="h-9 w-9 bg-(--neutral-100)">
            <AvatarFallback className="bg-(--neutral-100) text-(--text-primary-dark) text-xs font-semibold">
              {displayInitials}
            </AvatarFallback>
          </Avatar>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        sideOffset={8}
        className={cn(
          "w-56 rounded-xl border border-(--neutral-100) bg-(--surface-white) p-2",
          "shadow-[0px_12px_24px_var(--shadow)]"
        )}
      >
        <div className="px-3 py-2">
          <div
            className="max-w-[11.25rem] truncate text-(--text-gray-900) text-sm font-semibold leading-5"
            title={displayName}
          >
            {displayName}
          </div>
        </div>

        <DropdownMenuSeparator className="bg-(--neutral-100) -mx-2 my-2" />

        {showMyProfile ? (
          <DropdownMenuItem
            className={cn(
              "cursor-pointer rounded-lg px-3 py-2 text-sm font-medium",
              "text-(--text-gray-900) focus:bg-(--bg-primary-50) focus:text-(--text-gray-900)"
            )}
            onSelect={function () {
              navigate("/super-admin/my-profile");
            }}
          >
            My Profile
          </DropdownMenuItem>
        ) : null}

        {(authRole === "admin" ||
          authRole === "therapist" ||
          authRole === "staff" ||
          authRole === "super-admin") && (
          <DropdownMenuItem
            className={cn(
              "cursor-pointer rounded-lg px-3 py-2 text-sm font-medium",
              "text-(--text-gray-900) focus:bg-(--bg-primary-50) focus:text-(--text-gray-900)"
            )}
            onSelect={function () {
              if (authRole === "super-admin") {
                navigate("/super-admin/security");
              } else if (authRole === "admin") {
                navigate("/admin/security");
              } else if (authRole === "therapist") {
                navigate("/therapist/security");
              } else {
                navigate("/staff/security");
              }
            }}
          >
            Security
          </DropdownMenuItem>
        )}

        <DropdownMenuItem
          className={cn(
            "cursor-pointer rounded-lg px-3 py-2 text-sm font-medium",
            "text-(--status-denied) focus:bg-(--light-red) focus:text-(--status-denied)"
          )}
          onSelect={() => void handleLogout()}
        >
          Logout
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default ProfileDropdown;
