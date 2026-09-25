import { Settings, LogOut, ArrowLeft, Menu, ShieldCheck } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "../ui/avatar";
import type { User } from "../../types/user.type";
import { useLocation, useNavigate } from "react-router-dom";
import NotificationDropdown from "../notification";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import { useEffect, useState } from "react";
import TherapistProfileModal from "../modals/TherapistProfile";
import { pageDescriptions } from "./topbar.static";
import { useAppDispatch } from "@/store/hooks";
import { clearSession } from "@/store/authSlice";
import { baseApi } from "@/store/api/baseApi";
import { useAppSelector } from "@/store/hooks";
import { useGetAuthMeQuery, useLogoutStaffMutation } from "@/store/api/authApi";
import { useGetPortalMeQuery } from "@/store/api/portalApi";
import { useStaffIdleLogout } from "@/hooks/useStaffIdleLogout";
import { subscribeToTherapistProfileOpen } from "@/utils/therapistProfileModal";

function getInitials(value: string): string {
  const normalized = value.trim();
  if (!normalized) return "NA";

  const localPart = normalized.includes("@") ? normalized.split("@")[0] : normalized;
  const parts = localPart.split(/[\s._-]+/).filter(Boolean);
  if (parts.length >= 2) {
    return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
  }

  return (parts[0] ?? localPart).slice(0, 2).toUpperCase();
}

const Topbar: React.FC<{ onMenuClick: () => void; user: User }> = ({
  user,
  onMenuClick,
}) => {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const authRole = useAppSelector((state) => state.auth.role);
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const [logoutStaff] = useLogoutStaffMutation();
  const { pathname } = location;
  const [profileModalOpen, setProfileModalOpen] = useState(false);
  const [profileModalSection, setProfileModalSection] = useState("basic");

  // Other screens ask for a specific tab of this modal (e.g. Zoom setup from scheduling).
  useEffect(
    () =>
      subscribeToTherapistProfileOpen((section) => {
        setProfileModalSection(section);
        setProfileModalOpen(true);
      }),
    [],
  );
  useStaffIdleLogout();
  const { data: authMeData } = useGetAuthMeQuery(undefined, {
    skip: !accessToken || authRole === "user",
  });
  const { data: portalMeData } = useGetPortalMeQuery(undefined, {
    skip: !accessToken || authRole !== "user",
  });
  const displayName =
    portalMeData?.fullName?.trim() ||
    authMeData?.username?.trim() ||
    user.name;
  const displayInitials = getInitials(displayName);

  const isTherapist = location.pathname.includes("therapist");
  const isAdmin = location.pathname.includes("admin");
  const isStaffPortalPath = location.pathname.includes("staff");
  const isClientPortal = authRole === "user" || location.pathname.startsWith("/user");
  const showTherapistProfileMenuItem = isTherapist;
  const showClientProfileMenuItem = isClientPortal && !isTherapist && !isAdmin && !isStaffPortalPath;
  const showPrivacyMenuItem = !(isTherapist || isAdmin || isStaffPortalPath);
  const showStaffSecurityMenuItem =
    isTherapist || isAdmin || isStaffPortalPath ||
    authRole === "admin" ||
    authRole === "therapist" ||
    authRole === "staff";
  const showProfileMenuSeparator =
    showTherapistProfileMenuItem ||
    showClientProfileMenuItem ||
    showPrivacyMenuItem ||
    showStaffSecurityMenuItem;

  const getStaffSecurityPath = () => {
    if (authRole === "admin" || isAdmin) return "/admin/security";
    if (authRole === "therapist" || isTherapist) return "/therapist/security";
    return "/staff/security";
  };

  const topbarTitleRaw = pathname?.split("/").pop()?.replace(/-/g, " ");
  const topbarTitle =
    topbarTitleRaw === "tasks"
      ? "Task Management"
      : topbarTitleRaw === "library"
        ? "Clinical Content Library"
        : topbarTitleRaw === "assessment"
          ? "Assessments"
          : topbarTitleRaw === "process checklists"
            ? "Healthcare Process Checklists"
            : topbarTitleRaw === "privacy"
              ? "Patient Consent Management"
              : topbarTitleRaw === "profiles"
                ? "User Profiles"
                : topbarTitleRaw === "hipaa"
                  ? "HIPAA Audit Trail"
                  : topbarTitleRaw === "roles"
                    ? "Role Management"
                    : topbarTitleRaw === "duplicate detection"
                      ? "Duplicate Detection"
                      : topbarTitleRaw === "settings"
                        ? "System Settings"
                        : topbarTitleRaw === "payments and subscription"
                          ? "Payments & Subscription"
                        : topbarTitleRaw === "security"
                          ? "Security"
                        : topbarTitleRaw === "booked sessions"
                          ? "Booked Sessions"
                          : /^\d+$/.test(topbarTitleRaw || "") &&
                              pathname.includes("/booked-sessions/")
                            ? "Booked Session"
                            : topbarTitleRaw;

  const pageDesc = pageDescriptions[topbarTitle || ""];
  const description =
    typeof pageDesc === "object"
      ? isAdmin || isStaffPortalPath
        ? pageDesc.admin
        : pageDesc.user
      : pageDesc || "Manage and organize your client profiles efficiently";

  const getPostLogoutPath = () => {
    if (authRole === "super-admin") return "/super-admin/login";
    if (authRole === "admin" || authRole === "therapist" || authRole === "staff") {
      return "/auth/staff/login";
    }
    return "/auth/login";
  };

  const handleLogout = async () => {
    try {
      if (authRole && authRole !== "user" && accessToken) {
        await logoutStaff().unwrap();
      }
    } catch {
      // Logout should always proceed locally even if the server call fails.
    } finally {
      dispatch(clearSession());
      dispatch(baseApi.util.resetApiState());
      navigate(getPostLogoutPath(), { replace: true });
    }
  };

  const isHistoryPage = topbarTitle?.toLowerCase() === "history";
  const isMyProfilePage = pathname === "/user/my-profile";
  const isSecurityPage = pathname.endsWith("/security");
  const showBackButton = isHistoryPage || isMyProfilePage || isSecurityPage;

  return (
    <header className="sticky top-0 z-30 shrink-0 bg-(--bg-primary-light)">
      {/* Mobile Top Bar */}
      <div className="flex items-center justify-between w-full h-14 px-4 bg-(--bg-primary-dark) md:hidden">
        <button
          onClick={onMenuClick}
          className="p-2.5 bg-(--btn-link-text-pressed) rounded-lg transition-colors cursor-pointer"
        >
          <Menu className="w-5 h-5 text-white" />
        </button>

        <div className="flex items-center justify-center w-8 h-8 bg-white rounded-md shadow-sm">
          <span className="text-sm font-bold text-(--text-primary-dark)">
            SH
          </span>
        </div>

        <div className="flex items-center gap-3">
          <NotificationDropdown />

          <Avatar className="h-8 w-8">
            <AvatarImage src={user.avatar} />
            <AvatarFallback className="bg-(--bg-primary-100) text-(--text-primary-dark) text-sm font-bold">
              {displayInitials}
            </AvatarFallback>
          </Avatar>
        </div>
      </div>

      {/* Main Header Area */}
      <div className="flex min-h-[5.5rem] items-center justify-between border-b border-gray-100 bg-(--bg-primary-light) px-4 py-4 sm:px-5 lg:px-6 md:min-h-[4.75rem] md:border-none md:py-2 md:pr-1">
        <div className="flex w-full max-w-145 items-center gap-3">
          {showBackButton ? (
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="flex h-6 w-6 shrink-0 cursor-pointer items-center justify-center text-[#101828] transition-colors hover:text-[#101828]/70"
              aria-label="Go back"
            >
              <ArrowLeft className="size-6" />
            </button>
          ) : null}
          <div className="min-w-0 flex-1">
            <h1 className="min-h-7 capitalize text-2xl font-semibold text-(--text-secondary-dark) md:mb-1 md:min-h-6 md:text-xl">
              {topbarTitle}
            </h1>
            <p className="min-h-10 text-sm font-normal wrap-break-word whitespace-normal text-(--text-neutral-600) line-clamp-2 md:min-h-9">
              {description}
            </p>
          </div>
        </div>

        <div className="hidden md:flex items-center md:gap-2">
          {/* Notifications */}
          <NotificationDropdown />

          {/* User Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild className="outline-none">
              <button className="flex items-center ml-2 pl-4 border-l border-gray-300 cursor-pointer">
                <Avatar className="h-8 w-8 bg-(--text-neutral-200)">
                  <AvatarImage src={user.avatar} />
                  <AvatarFallback className="bg-(--text-neutral-200) text-sm font-semibold">
                    {displayInitials}
                  </AvatarFallback>
                </Avatar>
              </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent
              align="end"
              className="w-43 rounded-xl shadow-lg outline-none"
            >
              {showTherapistProfileMenuItem ? (
                <DropdownMenuItem
                  className="gap-2 cursor-pointer py-2.5 text-(--text-secondary-dark)"
                  onClick={() => {
                    setProfileModalSection("basic");
                    setProfileModalOpen(true);
                  }}
                >
                  <img src="/assets/user-icon.png" alt="user-icon" />
                  My Profile
                </DropdownMenuItem>
              ) : null}

              {showClientProfileMenuItem ? (
                <DropdownMenuItem
                  className="gap-2 cursor-pointer py-2.5 text-(--text-secondary-dark)"
                  onClick={() => navigate("/user/my-profile")}
                >
                  <img src="/assets/user-icon.png" alt="user-icon" />
                  My Profile
                </DropdownMenuItem>
              ) : null}

              {showPrivacyMenuItem ? (
                <DropdownMenuItem
                  className="gap-2 cursor-pointer py-2.5 text-(--text-secondary-dark)"
                  onClick={() => navigate("/user/privacy-settings")}
                >
                  <Settings className="size-5" />
                  Privacy Settings
                </DropdownMenuItem>
              ) : null}

              {showStaffSecurityMenuItem ? (
                <DropdownMenuItem
                  className="gap-2 cursor-pointer py-2.5 text-(--text-secondary-dark)"
                  onClick={() => navigate(getStaffSecurityPath())}
                >
                  <ShieldCheck className="size-5" />
                  Security
                </DropdownMenuItem>
              ) : null}

              {showProfileMenuSeparator ? <DropdownMenuSeparator /> : null}

              <DropdownMenuItem
                className="gap-2 text-(--text-secondary-dark) cursor-pointer py-2.5"
                onClick={() => void handleLogout()}
              >
                <LogOut className="size-5" />
                Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      <TherapistProfileModal
        isOpen={profileModalOpen}
        initialSection={profileModalSection}
        onClose={() => setProfileModalOpen(false)}
      />
    </header>
  );
};

export default Topbar;
