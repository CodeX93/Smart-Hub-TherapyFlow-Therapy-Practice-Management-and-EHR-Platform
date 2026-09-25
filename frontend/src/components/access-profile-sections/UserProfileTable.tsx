import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { Badge } from "../ui/badge";
import type { UserProfile } from "@/types/user-access-profiles.type";
import { cn } from "@/lib/utils";
import { Eye, MoveDown, Pencil, Search } from "lucide-react";
import { Switch } from "../ui/switch";
import ActionDropdown, { type DropdownAction } from "../shared/ActionDropdown";
import AdminAddUser from "./AdminAddUser";
import { useEffect, useState } from "react";
import DeleteUserModal from "./DeleteUserModal";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import {
  ROLE_OPTIONSS,
} from "@/pages/admin/user-access/profiles/user-access.static";
// import Pagination from "../shared/Pagination"; // Removed
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";

import { useCallback } from "react";
import AddUserModal from "./AddUserModal";
import {
  useActivateAdminUserMutation,
  useCreateAdminUserMutation,
  useDeactivateAdminUserMutation,
  useDeleteAdminUserMutation,
  useLazyGetAdminUserByIdQuery,
  useLazyGetAdminUsersQuery,
  useUpdateAdminUserMutation,
  type AdminUserSummary,
} from "@/store/api/admin/users.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { formatAdminUserRoleLabels } from "@/utils/adminUserRoleDisplay";
import type { AddUserFormValues } from "@/schemas/user-access-profiles.schema";
import EditUserBasicInfoModal, {
  type EditUserBasicInfoValues,
} from "./EditUserBasicInfoModal";
import ConfirmationModal from "../shared/ConfirmationModal";
import Toast from "../shared/Toast";
import type { StaffUserAccessPermissions } from "@/utils/staffUserAccess";
import ProfessionalProfilePreviewModal from "./ProfessionalProfilePreviewModal";
import { useGetAuthMeQuery } from "@/store/api/authApi";

const DEFAULT_USER_ACCESS: StaffUserAccessPermissions = {
  canCreateUser: true,
  canEditUser: true,
  canDeleteUser: true,
  canToggleUserStatus: true,
  canEditProfessionalDetails: true,
  canAssignSupervisor: true,
  showSupervisorTab: true,
  hasRowActions: true,
};

interface UserProfileTableProps {
  isAddModalOpen?: boolean;
  setIsAddModalOpen?: (open: boolean) => void;
  setUserCounts?: (count: number) => void;
  userAccess?: StaffUserAccessPermissions;
}

const UserProfileTable = ({
  setUserCounts,
  isAddModalOpen,
  setIsAddModalOpen,
  userAccess = DEFAULT_USER_ACCESS,
}: UserProfileTableProps) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [professionalUser, setProfessionalUser] = useState<UserProfile | null>(null);
  const [previewUser, setPreviewUser] = useState<UserProfile | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editUserId, setEditUserId] = useState<number | null>(null);
  const [editInitialValues, setEditInitialValues] = useState<EditUserBasicInfoValues>({
    fullName: "",
    email: "",
    username: "",
    phone: "",
    role: "",
  });
  const [editUserErrorMessage, setEditUserErrorMessage] = useState<string | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<UserProfile | null>(null);
  const [isDeactivateModalOpen, setIsDeactivateModalOpen] = useState(false);
  const [userToDeactivate, setUserToDeactivate] = useState<UserProfile | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<string>("All");

  const [data, setData] = useState<UserProfile[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [createUserErrorMessage, setCreateUserErrorMessage] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const itemsPerPage = 25;
  const [triggerGetUsers, { isFetching, isError, error }] =
    useLazyGetAdminUsersQuery();
  const [triggerGetUserById, { isFetching: isFetchingUserById }] = useLazyGetAdminUserByIdQuery();
  const [createAdminUser, { isLoading: isCreatingUser }] = useCreateAdminUserMutation();
  const [updateAdminUser, { isLoading: isUpdatingUser }] = useUpdateAdminUserMutation();
  const [deleteAdminUser, { isLoading: isDeletingUser }] = useDeleteAdminUserMutation();
  const [deactivateAdminUser, { isLoading: isDeactivatingUser }] = useDeactivateAdminUserMutation();
  const [activateAdminUser, { isLoading: isActivatingUser }] = useActivateAdminUserMutation();
  const { data: authMeData } = useGetAuthMeQuery();
  const currentUserId = authMeData?.user?.id;

  function isCurrentUser(user: UserProfile): boolean {
    if (currentUserId == null) return false;
    return String(currentUserId) === String(user.id);
  }

  useEffect(() => {
    setUserCounts?.(totalCount);
  }, [totalCount, setUserCounts]);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  function toTitleCase(value: string): string {
    const normalized = value.trim().toLowerCase();
    if (!normalized) return "";
    return normalized.charAt(0).toUpperCase() + normalized.slice(1);
  }

  function formatLastLogin(lastLogin?: string | null): string {
    if (!lastLogin) return "-";
    const parsed = new Date(lastLogin);
    if (Number.isNaN(parsed.getTime())) return "-";
    return parsed.toLocaleString("en-US", {
      month: "short",
      day: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  function mapApiUserToRow(user: AdminUserSummary): UserProfile {
    const fullName = user.fullName?.trim();
    return {
      id: String(user.id),
      name: fullName || toTitleCase(user.username.split("@")[0] ?? user.username),
      username: user.username,
      email: user.email,
      roles: formatAdminUserRoleLabels(user.roles),
      status: user.active ? "Active" : "Inactive",
      lastLogin: formatLastLogin(user.lastLogin),
    };
  }

  function mapRoleFilterToApiRole(value: string): string | undefined {
    if (value === "All") return undefined;
    if (value === "Admin") return "ADMIN";
    if (value === "Supervisor") return "SUPERVISOR";
    if (value === "Therapist") return "THERAPIST";
    return undefined;
  }

  function mapRoleToApiRole(value: string): string {
    const normalized = value.trim().toUpperCase().replace(/\s+/g, "_");
    if (normalized === "ADMINISTRATOR") return "ADMIN";
    if (normalized === "BILLING_SPECIALIST") return "BILLING_SPECIALIST";
    if (normalized === "THERAPIST") return "THERAPIST";
    if (normalized === "SUPERVISOR") return "SUPERVISOR";
    return normalized;
  }

  const loadUsersPage = useCallback(
    async (targetPage: number, replace = false) => {
      try {
        setIsLoadingMore(!replace && targetPage > 1);
        const response = await triggerGetUsers({
          page: targetPage,
          pageSize: itemsPerPage,
          search: searchQuery.trim() || undefined,
          role: mapRoleFilterToApiRole(filter),
        }).unwrap();

        const mappedItems = response.items.map(mapApiUserToRow);
        setTotalPages(response.totalPages);
        setTotalCount(response.totalCount);
        setPage(response.page);
        setData((previous) => (replace ? mappedItems : [...previous, ...mappedItems]));
      } finally {
        setIsLoadingMore(false);
      }
    },
    [filter, searchQuery, triggerGetUsers, setPage]
  );

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      void loadUsersPage(1, true);
    }, 300);
    return () => {
      window.clearTimeout(timeout);
    };
  }, [loadUsersPage]);

  const handleLoadMore = useCallback(() => {
    if (isFetching || isLoadingMore) return;
    const nextPage = page + 1;
    if (nextPage > totalPages) return;
    void loadUsersPage(nextPage, false);
  }, [isFetching, isLoadingMore, loadUsersPage, page, totalPages]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: page < totalPages,
    isLoading: isLoadingMore || isFetching,
  });
  const currentData = data;

  async function handleAddUser(values: AddUserFormValues) {
    setCreateUserErrorMessage(null);
    try {
      const phone = values.phone.trim();
      await createAdminUser({
        username: values.username.trim(),
        fullName: values.fullName.trim(),
        password: values.password,
        email: values.email.trim(),
        ...(phone ? { phone } : {}),
        roles: [mapRoleToApiRole(values.role)],
        active: true,
      }).unwrap();

      setIsAddModalOpen?.(false);
      setToastMessage({ type: "success", text: "User added successfully." });
      void loadUsersPage(1, true);
    } catch (createError) {
      const errorMsg = getApiErrorMessage(createError) || "Failed to add user.";
      setCreateUserErrorMessage(errorMsg);
      setToastMessage({ type: "error", text: errorMsg });
      throw createError;
    }
  }

  function updateRowStatus(id: string, nextStatus: UserProfile["status"]) {
    setData((previous) =>
      previous.map((profile) =>
        profile.id === id ? { ...profile, status: nextStatus } : profile
      )
    );
  }

  async function handleStatusToggle(user: UserProfile, active: boolean) {
    if (isCurrentUser(user)) return;

    const numericId = Number.parseInt(user.id, 10);
    if (Number.isNaN(numericId)) return;

    setEditUserErrorMessage(null);
    try {
      if (active) {
        await activateAdminUser(numericId).unwrap();
        updateRowStatus(user.id, "Active");
        setToastMessage({ type: "success", text: "User activated successfully." });
        return;
      }
      setUserToDeactivate(user);
      setIsDeactivateModalOpen(true);
    } catch (statusError) {
      setToastMessage({ type: "error", text: getApiErrorMessage(statusError) });
    }
  }

  async function handleConfirmDeactivate() {
    if (!userToDeactivate) return;
    const numericId = Number.parseInt(userToDeactivate.id, 10);
    if (Number.isNaN(numericId)) return;

    try {
      setEditUserErrorMessage(null);
      await deactivateAdminUser(numericId).unwrap();
      updateRowStatus(userToDeactivate.id, "Inactive");
      setIsDeactivateModalOpen(false);
      setUserToDeactivate(null);
      setToastMessage({ type: "success", text: "User deactivated successfully." });
    } catch (deactivateError) {
      setToastMessage({ type: "error", text: getApiErrorMessage(deactivateError) });
    }
  }

  const handleDeleteClick = (user: UserProfile) => {
    if (isCurrentUser(user)) return;
    setUserToDelete(user);
    setIsDeleteModalOpen(true);
  };

  async function handleConfirmDelete() {
    if (!userToDelete) return;
    const numericId = Number.parseInt(userToDelete.id, 10);
    if (Number.isNaN(numericId)) return;

    try {
      setEditUserErrorMessage(null);
      await deleteAdminUser(numericId).unwrap();
      setIsDeleteModalOpen(false);
      setUserToDelete(null);
      setToastMessage({ type: "success", text: "User deleted successfully." });
      void loadUsersPage(1, true);
    } catch (deleteError) {
      const message = getApiErrorMessage(deleteError);
      setToastMessage({ type: "error", text: message });
    }
  }

  async function handleEditBasicInfo(user: UserProfile) {
    const numericId = Number.parseInt(user.id, 10);
    if (Number.isNaN(numericId)) return;

    try {
      setEditUserErrorMessage(null);
      const userDetails = await triggerGetUserById(numericId).unwrap();
      setEditUserId(userDetails.id);
      setEditInitialValues({
        fullName: userDetails.fullName || "",
        email: userDetails.email || "",
        username: userDetails.username || "",
        phone: userDetails.phone || "",
        role: userDetails.roles[0] ?? "",
      });
      setIsEditModalOpen(true);
    } catch (detailError) {
      setToastMessage({ type: "error", text: getApiErrorMessage(detailError) });
    }
  }

  async function handleEditSubmit(values: EditUserBasicInfoValues) {
    if (editUserId === null) return;

    try {
      setEditUserErrorMessage(null);
      await updateAdminUser({
        id: editUserId,
        body: {
          username: values.username.trim(),
          fullName: values.fullName.trim(),
          email: values.email.trim(),
          phone: values.phone.trim(),
          roles: [mapRoleToApiRole(values.role)],
        },
      }).unwrap();

      setIsEditModalOpen(false);
      setEditUserId(null);
      setToastMessage({ type: "success", text: "User updated successfully." });
      void loadUsersPage(1, true);
    } catch (updateError) {
      setEditUserErrorMessage(getApiErrorMessage(updateError));
      setToastMessage({ type: "error", text: getApiErrorMessage(updateError) });
      throw updateError;
    }
  }

  const getActions = (user: UserProfile): DropdownAction[] => {
    const actions: DropdownAction[] = [];
    const isSelf = isCurrentUser(user);

    if (userAccess.canEditUser) {
      actions.push({
        label: "Edit Basic Info",
        icon: <Pencil size={18} />,
        onClick: () => handleEditBasicInfo(user),
      });
    }

    if (
      userAccess.canEditProfessionalDetails &&
      user.roles.some((role) => role.trim().toLowerCase() === "therapist")
    ) {
      actions.push({
        label: "Professional Details",
        icon: <Pencil size={18} />,
        onClick: () => {
          setProfessionalUser(user);
          setIsModalOpen(true);
        },
      });
    }

    if (userAccess.canToggleUserStatus && !isSelf) {
      actions.push({
        label: user.status === "Active" ? "Deactivate" : "Activate",
        icon: <MoveDown size={18} />,
        onClick: () => void handleStatusToggle(user, user.status !== "Active"),
      });
    }

    if (userAccess.canDeleteUser && !isSelf) {
      actions.push({
        label: "Delete",
        icon: <TrashIcon size={18} />,
        onClick: () => handleDeleteClick(user),
      });
    }

    return actions;
  };

  const columnCount =
    5 +
    (userAccess.canToggleUserStatus ? 1 : 0) +
    (userAccess.hasRowActions ? 1 : 0);

  return (
    <div className="flex h-full min-h-0 flex-col">
      {toastMessage ? (
        <Toast
          message={toastMessage.text}
          type={toastMessage.type}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="mb-4 flex shrink-0 items-center gap-3">
        <div className="max-w-sm w-full">
          <CustomInput
            placeholder="Search users by name, username, or email..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
            }}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="rounded-full min-h-10 w-full pb-0 pt-1.75 bg-white shadow-xs border-(--neutral-100)"
          />
        </div>

        <div>
          <CustomSelect
            options={ROLE_OPTIONSS}
            value={filter}
            onChange={(value) => {
              setFilter(value as string);
            }}
            placeholder="All Roles"
            className="rounded-full max-h-10 pb-0 pt-0 bg-white shadow-xs border-(--neutral-100)"
            isSearch={false}
            contentClassName="min-w-fit"
          />
        </div>
      </div>
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-(--shadow)">
        <div className="min-h-0 flex-1 overflow-auto overscroll-contain">
          <table className="w-full text-left border-collapse">
            <thead className="sticky top-0 z-10 bg-(--bg-primary-50)">
              <tr className="h-11.5 border-b border-(--neutral-100)">
              <th className="px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Name
              </th>
              <th className="px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Username
              </th>
              <th className="px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Email
              </th>
              {userAccess.canToggleUserStatus ? (
                <th className="px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                  Status
                </th>
              ) : null}
              <th className="px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Role
              </th>
              <th className="px-4 py-3 text-sm font-semibold text-(--text-primary-dark)">
                Last Login
              </th>
              {userAccess.hasRowActions ? (
                <th className="px-4 py-3 text-center text-sm font-semibold text-(--text-primary-dark)">
                  Actions
                </th>
              ) : null}
            </tr>
          </thead>
          <tbody>
            {isError ? (
              <tr>
                <td
                  colSpan={columnCount}
                  className="px-4 py-5 text-sm font-medium text-(--status-denied)"
                >
                  {getApiErrorMessage(error)}
                </td>
              </tr>
            ) : null}
            {!isError && isFetching && currentData.length === 0 ? (
              <tr>
                <td colSpan={columnCount} className="px-4 py-16">
                  <ContentLoader className="min-h-48" />
                </td>
              </tr>
            ) : null}
            {!isError && !isFetching && currentData.length === 0 ? (
              <tr>
                <td
                  colSpan={columnCount}
                  className="px-4 py-5 text-center text-sm font-medium text-(--text-neutral-600)"
                >
                  No users found.
                </td>
              </tr>
            ) : null}
            {currentData.map((record) => (
              <tr
                key={record.id}
                className="h-18 border-t border-(--neutral-100) transition-colors hover:bg-(--neutral-50)/30"
              >
                <td className="px-4 py-3 text-sm font-medium text-(--text-primary-dark)">
                  <span className="block max-w-44 truncate" title={record.name}>
                    {record.name}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm font-medium text-(--text-primary-dark)">
                  <span className="block max-w-44 truncate" title={record.username}>
                    {record.username}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm font-medium text-(--text-primary-dark)">
                  <span className="block max-w-52 truncate" title={record.email}>
                    {record.email}
                  </span>
                </td>
                {userAccess.canToggleUserStatus ? (
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={record.status === "Active"}
                        onCheckedChange={(checked) => void handleStatusToggle(record, checked)}
                        disabled={
                          isCurrentUser(record) ||
                          isActivatingUser ||
                          isDeactivatingUser ||
                          isDeletingUser ||
                          isFetchingUserById
                        }
                      />
                      <span
                        className={cn(
                          "text-sm",
                          record.status === "Active"
                            ? "text-(--text-primary-dark) font-medium"
                            : "text-(--text-neutral-600)",
                        )}
                      >
                        {record.status}
                      </span>
                    </div>
                  </td>
                ) : null}
                <td className="px-4 py-3">
                  <div className="flex flex-wrap items-center gap-1.5">
                    {record.roles.length > 0 ? (
                      record.roles.map((roleLabel) => (
                        <Badge
                          key={`${record.id}-${roleLabel}`}
                          variant="outline"
                          className="w-fit h-6 px-3 text-[0.75rem] font-medium rounded-full shadow-none border bg-(--neutral-50) text-(--text-neutral-600) border-(--neutral-100)"
                        >
                          {roleLabel}
                        </Badge>
                      ))
                    ) : (
                      <span className="text-sm text-(--text-neutral-500)">-</span>
                    )}
                  </div>
                </td>
                <td className="px-4 py-3 text-sm font-medium text-(--text-primary-dark)">
                  {record.lastLogin}
                </td>
                {userAccess.hasRowActions ? (
                  <td className="px-4 py-3 text-center">
                    <div className="flex items-center justify-center gap-1">
                      {record.roles.some(
                        (role) => role.trim().toLowerCase() === "therapist",
                      ) ? (
                        <button
                          type="button"
                          onClick={() => setPreviewUser(record)}
                          className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-full text-(--text-neutral-500) transition-colors hover:bg-(--neutral-50) hover:text-(--text-primary-dark)"
                          aria-label={`Preview professional profile for ${record.name}`}
                          title="Preview professional profile"
                        >
                          <Eye size={18} />
                        </button>
                      ) : (
                        <span className="inline-block h-9 w-9 shrink-0" aria-hidden />
                      )}
                      <ActionDropdown actions={getActions(record)} />
                    </div>
                  </td>
                ) : null}
              </tr>
            ))}
          </tbody>
          </table>

          <div
            ref={observerTarget}
            className="flex h-10 w-full items-center justify-center"
          >
            {(isLoadingMore || (isFetching && currentData.length > 0)) && (
              <ContentLoader variant="inline" size="md" />
            )}
          </div>
        </div>
      </div>

      <AddUserModal
        isOpen={!!isAddModalOpen}
        onClose={() => {
          setCreateUserErrorMessage(null);
          setIsAddModalOpen?.(false);
        }}
        onAdd={handleAddUser}
        isSubmitting={isCreatingUser}
        errorMessage={createUserErrorMessage}
      />

      <EditUserBasicInfoModal
        isOpen={isEditModalOpen}
        onClose={() => {
          setEditUserErrorMessage(null);
          setIsEditModalOpen(false);
        }}
        onSubmit={handleEditSubmit}
        initialValues={editInitialValues}
        isSubmitting={isUpdatingUser}
        errorMessage={editUserErrorMessage}
      />

      <AdminAddUser
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setProfessionalUser(null);
        }}
        userId={professionalUser ? Number.parseInt(professionalUser.id, 10) : null}
        userName={professionalUser?.name}
      />

      <ProfessionalProfilePreviewModal
        isOpen={Boolean(previewUser)}
        onClose={() => setPreviewUser(null)}
        userId={previewUser ? Number.parseInt(previewUser.id, 10) : null}
        userName={previewUser?.name}
        userEmail={previewUser?.email}
      />

      <DeleteUserModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          if (isDeletingUser) return;
          setIsDeleteModalOpen(false);
        }}
        onConfirm={handleConfirmDelete}
        userName={userToDelete?.name || ""}
        isLoading={isDeletingUser}
      />

      <ConfirmationModal
        type="disable"
        isOpen={isDeactivateModalOpen}
        onClose={() => {
          setIsDeactivateModalOpen(false);
          setUserToDeactivate(null);
        }}
        onConfirm={() => void handleConfirmDeactivate()}
        title={`Deactivate User "${userToDeactivate?.name || ""}"`}
        description="This will mark the user as inactive and prevent further access until re-activated."
        confirmButtonText={isDeactivatingUser ? "Deactivating..." : "Deactivate"}
      />
    </div>
  );
};

export default UserProfileTable;
