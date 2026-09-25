
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useState, useCallback } from "react";

import RolesToolbar from "@/components/role-management/RolesToolbar";
import RolesTable, {
  type RoleEntry,
} from "@/components/role-management/RolesTable";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import RoleModal, {
  type RoleFormValues,
} from "@/components/role-management/RoleModal";
import Toast from "@/components/shared/Toast";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import {
  useCreateRoleMutation,
  useDeleteRoleMutation,
  useGetAdminRolesPagedQuery,
  useGetPermissionsListQuery,
  useLazyGetRoleByIdQuery,
  useUpdateRoleMutation,
} from "@/store/api/admin/roles.api";
import { getApiErrorMessage } from "@/utils/apiError";

const Roles = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const normalizedSearchQuery = searchQuery.trim();
  const [sortConfig, setSortConfig] = useState<{
    key: keyof RoleEntry;
    direction: "asc" | "desc";
  }>({
    key: "displayName",
    direction: "asc",
  });
  const [page, setPage] = useState(1);
  const [allRoles, setAllRoles] = useState<RoleEntry[]>([]);
  const itemsPerPage = 20;

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<"create" | "edit">("create");
  const [selectedRole, setSelectedRole] = useState<RoleEntry | null>(null);
  const [selectedRolePermissionIds, setSelectedRolePermissionIds] = useState<string[]>([]);
  const [isRoleDetailsLoading, setIsRoleDetailsLoading] = useState(false);
  const [isModalSubmitting, setIsModalSubmitting] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [roleToDelete, setRoleToDelete] = useState<RoleEntry | null>(null);

  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("error");
  const {
    data: rolesResponse,
    isLoading: isRolesLoading,
    isFetching: isRolesFetching,
    isError: isRolesError,
    error: rolesError,
    refetch,
  } = useGetAdminRolesPagedQuery({
    search: normalizedSearchQuery || undefined,
    page,
    pageSize: itemsPerPage,
    sortBy: sortConfig.key,
    sortDirection: sortConfig.direction,
  }, {
    refetchOnMountOrArgChange: true,
  });
  const {
    data: permissionsCatalog = [],
    isFetching: isPermissionsLoading,
  } = useGetPermissionsListQuery(undefined, {
    skip: !isModalOpen,
    refetchOnMountOrArgChange: true,
  });
  const [getRoleById] = useLazyGetRoleByIdQuery();
  const [createRole] = useCreateRoleMutation();
  const [deleteRole, { isLoading: isDeletingRole }] = useDeleteRoleMutation();
  const [updateRole] = useUpdateRoleMutation();

  const resolvedEditPermissionIds = (() => {
    if (selectedRolePermissionIds.length === 0 || permissionsCatalog.length === 0) {
      return selectedRolePermissionIds;
    }

    const catalogIds = new Set(permissionsCatalog.map((permission) => String(permission.id)));
    const catalogIdByName = new Map(
      permissionsCatalog.map((permission) => [permission.name.trim().toUpperCase(), String(permission.id)]),
    );

    return Array.from(
      new Set(
        selectedRolePermissionIds.flatMap((entry) => {
          if (catalogIds.has(entry)) return [entry];
          const byName = catalogIdByName.get(entry.trim().toUpperCase());
          if (byName) return [byName];
          // Keep numeric IDs even if missing from the current catalog.
          if (/^\d+$/.test(entry)) return [entry];
          return [];
        }),
      ),
    );
  })();

  const refreshRolesList = useCallback(async () => {
    if (page !== 1) {
      setPage(1);
      return;
    }
    await refetch();
  }, [page, refetch, setPage]);

  const handleSort = (key: keyof RoleEntry) => {
    setSortConfig((prev) => {
      const next = {
        key,
        direction:
          prev.key === key && prev.direction === "asc"
            ? ("desc" as const)
            : ("asc" as const),
      };

      if (prev.key !== next.key || prev.direction !== next.direction) {
        setPage(1);
        setAllRoles([]);
      }

      return next;
    });
  };

  useEffect(() => {
    if (!rolesResponse || rolesResponse.page !== page) {
      return;
    }

    const mappedRoles: RoleEntry[] = rolesResponse.items.map((role) => ({
      id: String(role.id),
      roleName: role.name || "-",
      displayName: role.displayName || role.name || "-",
      description: role.description || "-",
      permissionsAssigned: role.permissionsAssignedCount ?? role.permissions?.length ?? 0,
    }));

    setAllRoles((previous) => {
      if (page === 1) return mappedRoles;
      const existingIds = new Set(previous.map((item) => item.id));
      const merged = [...previous];
      mappedRoles.forEach((entry) => {
        if (!existingIds.has(entry.id)) merged.push(entry);
      });
      return merged;
    });
  }, [rolesResponse, page]);

  useEffect(() => {
    if (!isRolesError || !rolesError) return;
    setToastType("error");
    setToastMessage(getApiErrorMessage(rolesError));
  }, [isRolesError, rolesError]);

  const handleEdit = async (id: string) => {
    const role = allRoles.find((r) => r.id === id);
    if (!role) return;
    setModalMode("edit");
    setSelectedRole(role);
    setSelectedRolePermissionIds([]);
    setIsModalOpen(true);
    setIsRoleDetailsLoading(true);
    try {
      const detail = await getRoleById(Number(id)).unwrap();
      const permissionIds = (detail.permissions ?? [])
        .flatMap((permission) => {
          if (Number.isFinite(permission.id) && permission.id > 0) {
            return [String(permission.id)];
          }
          const name = permission.name?.trim();
          return name ? [name] : [];
        });
      setSelectedRole({
        ...role,
        roleName: detail.name || role.roleName,
        displayName: detail.displayName || detail.name || role.displayName,
        description: detail.description || "",
        permissionsAssigned:
          detail.permissionsAssignedCount ?? detail.permissions?.length ?? role.permissionsAssigned,
      });
      setSelectedRolePermissionIds(permissionIds);
    } catch (error) {
      setIsModalOpen(false);
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setIsRoleDetailsLoading(false);
    }
  };

  const handleCreateRole = () => {
    setSelectedRole(null);
    setSelectedRolePermissionIds([]);
    setIsRoleDetailsLoading(false);
    setModalMode("create");
    setIsModalOpen(true);
  };

  const handleDeleteClick = (id: string) => {
    const role = allRoles.find((entry) => entry.id === id);
    if (!role) return;
    setRoleToDelete(role);
    setIsDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!roleToDelete) return;
    try {
      await deleteRole(Number(roleToDelete.id)).unwrap();
      setToastType("success");
      setToastMessage("Role deleted successfully.");
      setIsDeleteModalOpen(false);
      setRoleToDelete(null);
      await refreshRolesList();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedRole(null);
    setSelectedRolePermissionIds([]);
    setIsRoleDetailsLoading(false);
  };

  const handleModalSubmit = async (data: RoleFormValues) => {
    try {
      setIsModalSubmitting(true);
      const permissionIds = data.permissions
        .map((entry) => Number(entry))
        .filter((entry) => Number.isFinite(entry) && entry > 0);

      if (modalMode === "create") {
        const normalizedRoleName = data.roleName.trim().toUpperCase().replace(/[\s-]+/g, "_");
        await createRole({
          name: normalizedRoleName,
          displayName: data.displayName,
          description: data.description,
          isSystem: false,
          isActive: true,
          permissions: permissionIds,
        }).unwrap();
        setToastType("success");
        setToastMessage("Role created successfully.");
      } else if (modalMode === "edit" && selectedRole) {
        const roleId = Number(selectedRole.id);
        const normalizedRoleName = data.roleName.trim().toUpperCase().replace(/[\s-]+/g, "_");
        await updateRole({
          id: roleId,
          body: {
            name: normalizedRoleName,
            displayName: data.displayName,
            description: data.description,
            isSystem: false,
            isActive: true,
            permissions: permissionIds,
          },
        }).unwrap();
        setToastType("success");
        setToastMessage("Role updated successfully.");
      }

      await refreshRolesList();
      handleModalClose();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setIsModalSubmitting(false);
    }
  };

  const handleLoadMore = useCallback(() => {
    if (isRolesFetching) return;
    const totalPages = rolesResponse?.totalPages ?? 1;
    if (page >= totalPages) return;
    setPage((prev) => prev + 1);
  }, [isRolesFetching, page, rolesResponse?.totalPages, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: page < (rolesResponse?.totalPages ?? 1),
    isLoading: isRolesFetching,
  });

  const paginatedRoles = allRoles;

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden gap-6">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <ScrollToTopButton />

      <div className="shrink-0">
        <RolesToolbar
          searchQuery={searchQuery}
          setSearchQuery={(query) => {
            setSearchQuery(query);
            setPage(1);
            setAllRoles([]);
          }}
          onCreateRole={handleCreateRole}
        />
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain pb-4">
        {(isRolesLoading ||
          (isRolesFetching && (allRoles.length === 0 || rolesResponse?.page !== page))) ? (
          <ContentLoader size="md" className="h-80 rounded-xl border border-(--neutral-100) bg-white" />
        ) : isRolesError ? (
          <div className="h-80 w-full rounded-xl border border-(--neutral-100) bg-white flex flex-col items-center justify-center gap-3">
            <p className="text-sm text-(--status-denied)">
              {getApiErrorMessage(rolesError)}
            </p>
            <button
              type="button"
              onClick={() => void refetch()}
              className="text-sm font-semibold text-(--text-primary-500) cursor-pointer"
            >
              Retry
            </button>
          </div>
        ) : paginatedRoles.length === 0 ? (
          <div className="h-80 w-full rounded-xl border border-(--neutral-100) bg-white flex items-center justify-center">
            <p className="text-sm text-(--text-neutral-600)">No role found</p>
          </div>
        ) : (
          <RolesTable
            roles={paginatedRoles}
            onEdit={handleEdit}
            onDelete={handleDeleteClick}
            onSort={handleSort}
            sortConfig={sortConfig}
          />
        )}

        <div
          ref={observerTarget}
          className="h-10 w-full flex items-center justify-center"
        >
          {isRolesFetching && page > 1 && (
            <ContentLoader variant="inline" size="md" />
          )}
        </div>
      </div>

      <RoleModal
        isOpen={isModalOpen}
        onClose={handleModalClose}
        onSubmit={handleModalSubmit}
        mode={modalMode}
        initialData={
          selectedRole
            ? {
                roleName: selectedRole.roleName,
                displayName: selectedRole.displayName,
                description: selectedRole.description,
                permissions: resolvedEditPermissionIds,
              }
            : undefined
        }
        permissionsCatalog={permissionsCatalog}
        isSubmitting={isModalSubmitting}
        isLoadingInitialData={isPermissionsLoading || (modalMode === "edit" && isRoleDetailsLoading)}
      />

      <ConfirmationModal
        type="delete"
        isOpen={isDeleteModalOpen}
        onClose={() => {
          if (isDeletingRole) return;
          setIsDeleteModalOpen(false);
          setRoleToDelete(null);
        }}
        onConfirm={() => {
          void handleConfirmDelete();
        }}
        title="Delete role?"
        description={`Are you sure you want to delete "${roleToDelete?.displayName || roleToDelete?.roleName || "this role"}"? This action cannot be undone.`}
        items={[]}
        confirmButtonText={isDeletingRole ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingRole}
      />
    </div>
  );
};

export default Roles;
