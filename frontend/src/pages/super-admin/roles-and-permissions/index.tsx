import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { useEffect, useMemo, useState } from "react";
import { Edit3, Plus, Upload } from "lucide-react";
import { useNavigate } from "react-router-dom";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import Toast from "@/components/shared/Toast";
import { cn } from "@/lib/utils";
import {
  useDeleteSuperAdminRoleMutation,
  useGetRolesPermissionsMatrixQuery,
  useLazyGetSuperAdminRoleByIdQuery,
  useLazyGetRolesPermissionsMatrixExportQuery,
  useToggleRolesPermissionsMatrixCellMutation,
  useUpdateSuperAdminRoleMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import EditRoleModal, { type EditRoleFormValues } from "./components/EditRoleModal";
import { validateEditRoleForm } from "./createRole.utils";

type PermissionRow = {
  permissionId: number;
  key: string;
  label: string;
  module: string;
  group: string;
  roles: Record<string, boolean>;
};

type RoleListItem = {
  roleId: number;
  key: string;
  displayName: string;
  description: string;
  isSystem: boolean;
  active: boolean;
  organisationId: number | null;
};

type EditRoleForm = EditRoleFormValues;

function humanizeRole(value: string): string {
  return value
    .replaceAll("_", " ")
    .split(" ")
    .filter(Boolean)
    .map((part) => part[0].toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

function toApiRoleName(roleKey: string): string {
  return roleKey.replaceAll(" ", "_").toUpperCase();
}

function truncateText(value: string, maxLength: number): string {
  if (value.length <= maxLength) return value;
  return `${value.slice(0, maxLength - 1)}...`;
}

function extractPermissionIdsFromRoleDetail(
  responsePermissions: unknown[],
  permissionIdByName: Record<string, number>,
): number[] {
  const ids: number[] = [];

  for (const entry of responsePermissions) {
    if (typeof entry === "number" && Number.isFinite(entry)) {
      ids.push(entry);
      continue;
    }

    if (typeof entry === "string" && entry.trim()) {
      const mapped = permissionIdByName[entry.trim()];
      if (typeof mapped === "number") ids.push(mapped);
      continue;
    }

    if (!entry || typeof entry !== "object") continue;

    const record = entry as Record<string, unknown>;
    const idValue = record.id ?? record.permissionId;
    if (typeof idValue === "number" && Number.isFinite(idValue)) {
      ids.push(idValue);
      continue;
    }
    if (typeof idValue === "string" && idValue.trim()) {
      const parsed = Number.parseInt(idValue, 10);
      if (Number.isFinite(parsed)) {
        ids.push(parsed);
        continue;
      }
    }

    const name =
      typeof record.name === "string"
        ? record.name.trim()
        : typeof record.permissionName === "string"
          ? record.permissionName.trim()
          : "";
    if (name && typeof permissionIdByName[name] === "number") {
      ids.push(permissionIdByName[name]);
    }
  }

  return Array.from(new Set(ids));
}

function PermissionIndicator(props: {
  id: string;
  checked: boolean;
  onToggle: () => void;
  disabled?: boolean;
}) {
  return (
    <Checkbox
      id={props.id}
      checked={props.checked}
      onCheckedChange={props.onToggle}
      disabled={props.disabled}
      className={cn(
        "h-4 w-4 rounded-[0.25rem] border-[#d9e2ec] checked:border-[#435361] checked:bg-[#435361]",
        "focus-visible:ring-[#435361]",
        props.disabled ? "cursor-not-allowed opacity-60" : "cursor-pointer",
      )}
    />
  );
}

const FILTER_SELECT_CONTENT_CLASS = cn(
  "z-[10050] max-h-[min(15rem,var(--radix-select-content-available-height))]",
  "rounded-[0.75rem] border border-[#e1e8ef] bg-white p-1",
  "shadow-[0_12px_28px_rgba(15,23,42,0.12)]",
);

const FILTER_SELECT_TRIGGER_CLASS = cn(
  "h-12 w-full min-w-0 rounded-full border-[#dde6ef] bg-white text-[#24313f] shadow-none",
  "[&>span]:line-clamp-1 [&>span]:truncate",
);

function RolesAndPermissions() {
  const navigate = useNavigate();
  const [roleSearch, setRoleSearch] = useState("");
  const [permissionSearch, setPermissionSearch] = useState("");
  const [moduleFilter, setModuleFilter] = useState("All");
  const [groupFilter, setGroupFilter] = useState("All");
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [permissions, setPermissions] = useState<PermissionRow[]>([]);
  const [roles, setRoles] = useState<RoleListItem[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("error");
  const [pendingCells, setPendingCells] = useState<Record<string, boolean>>({});
  const [deleteRoleTarget, setDeleteRoleTarget] = useState<RoleListItem | null>(null);
  const [editRoleForm, setEditRoleForm] = useState<EditRoleForm | null>(null);
  const [selectedRoleDescription, setSelectedRoleDescription] = useState<string>("");

  const {
    data: matrixData,
    isLoading: isMatrixLoading,
    isError: isMatrixError,
    error: matrixError,
    refetch,
  } = useGetRolesPermissionsMatrixQuery();
  const [toggleMatrixPermission] = useToggleRolesPermissionsMatrixCellMutation();
  const [updateRole, { isLoading: isUpdatingRole }] = useUpdateSuperAdminRoleMutation();
  const [deleteRole, { isLoading: isDeletingRole }] = useDeleteSuperAdminRoleMutation();
  const [getSuperAdminRoleById] = useLazyGetSuperAdminRoleByIdQuery();
  const [exportMatrixCsv, { isFetching: isExportingCsv }] =
    useLazyGetRolesPermissionsMatrixExportQuery();
  const [isEditRoleLoading, setIsEditRoleLoading] = useState(false);

  useEffect(() => {
    if (!matrixData) return;

    const apiRoles: RoleListItem[] = (matrixData.roles ?? []).map((role) => ({
      roleId: role.roleId,
      key: role.name,
      displayName: role.displayName || humanizeRole(role.name),
      description: role.description ?? "",
      isSystem: role.isSystem,
      active: role.active,
      organisationId: role.organisationId,
    }));

    const apiPermissions: PermissionRow[] = (matrixData.permissions ?? []).map((permission) => {
      const rolesMap = apiRoles.reduce<Record<string, boolean>>((acc, role) => {
        const apiRole = matrixData.roles.find((item) => item.roleId === role.roleId);
        acc[role.key] = Boolean(apiRole?.permissions?.includes(permission.name));
        return acc;
      }, {});

      return {
        permissionId: permission.permissionId,
        key: permission.name,
        label: permission.displayName || permission.name,
        module: permission.category ? permission.category.replaceAll("_", " ") : "general",
        group: permission.category ? permission.category.replaceAll("_", " ") : "general",
        roles: rolesMap,
      };
    });

    setRoles(apiRoles);
    setPermissions(apiPermissions);
  }, [matrixData]);

  const allRoleKeys = useMemo(() => {
    return roles.map((role) => role.key);
  }, [roles]);

  useEffect(() => {
    if (!allRoleKeys.length) return;
    setSelectedRole((previous) => (previous && allRoleKeys.includes(previous) ? previous : allRoleKeys[0]));
  }, [allRoleKeys]);

  const visibleRoleKeys = useMemo(() => {
    const needle = roleSearch.trim().toLowerCase();
    if (!needle) return allRoleKeys;
    return allRoleKeys.filter((roleKey) => {
      const role = roles.find((item) => item.key === roleKey);
      const label = role?.displayName ?? humanizeRole(roleKey);
      return label.toLowerCase().includes(needle);
    });
  }, [allRoleKeys, roleSearch, roles]);

  const selectedRoleMeta = useMemo(
    () => roles.find((role) => role.key === selectedRole) ?? null,
    [roles, selectedRole]
  );
  const deleteRoleNameForModal = useMemo(() => {
    const raw = deleteRoleTarget?.displayName?.trim() || "";
    if (!raw) return "";
    return truncateText(raw, 48);
  }, [deleteRoleTarget?.displayName]);

  const moduleOptions = useMemo(
    () => ["All", ...Array.from(new Set(permissions.map((row) => row.module))).sort()],
    [permissions]
  );
  const groupOptions = useMemo(
    () => ["All", ...Array.from(new Set(permissions.map((row) => row.group))).sort()],
    [permissions]
  );

  useEffect(() => {
    const selected = roles.find((role) => role.key === selectedRole);
    if (!selected) {
      setSelectedRoleDescription("");
      return;
    }

    let isMounted = true;
    void getSuperAdminRoleById(selected.roleId, false)
      .unwrap()
      .then((response) => {
        if (!isMounted) return;
        const description =
          typeof response.description === "string" ? response.description.trim() : "";
        setSelectedRoleDescription(description);
      })
      .catch(() => {
        if (!isMounted) return;
        setSelectedRoleDescription("");
      });

    return () => {
      isMounted = false;
    };
  }, [getSuperAdminRoleById, roles, selectedRole]);

  const filteredPermissions = useMemo(() => {
    const needle = permissionSearch.trim().toLowerCase();
    return permissions.filter((row) => {
      const matchesSearch =
        row.key.toLowerCase().includes(needle) ||
        row.module.toLowerCase().includes(needle) ||
        row.group.toLowerCase().includes(needle);
      const matchesModule = moduleFilter === "All" || row.module === moduleFilter;
      const matchesGroup = groupFilter === "All" || row.group === groupFilter;
      return matchesSearch && matchesModule && matchesGroup;
    });
  }, [permissions, permissionSearch, moduleFilter, groupFilter]);

  const groupedPermissions = useMemo(
    () =>
      Object.entries(
        filteredPermissions.reduce<Record<string, PermissionRow[]>>((acc, item) => {
          if (!acc[item.module]) {
            acc[item.module] = [];
          }
          acc[item.module].push(item);
          return acc;
        }, {})
      ),
    [filteredPermissions]
  );

  const permissionIdByName = useMemo(
    () =>
      permissions.reduce<Record<string, number>>((acc, permission) => {
        acc[permission.key] = permission.permissionId;
        return acc;
      }, {}),
    [permissions]
  );

  async function handleTogglePermission(permissionKey: string) {
    if (!selectedRole) return;
    const row = permissions.find((item) => item.key === permissionKey);
    if (!row) return;

    const currentlyGranted = Boolean(row.roles[selectedRole]);
    const nextGranted = !currentlyGranted;
    const cellKey = `${permissionKey}:${selectedRole}`;
    setToastMessage(null);

    setPermissions((previous) =>
      previous.map((item) => {
        if (item.key !== permissionKey) return item;
        return {
          ...item,
          roles: {
            ...item.roles,
            [selectedRole]: nextGranted,
          },
        };
      })
    );
    setPendingCells((previous) => ({ ...previous, [cellKey]: true }));

    try {
      await toggleMatrixPermission({
        roleName: toApiRoleName(selectedRole),
        permissionName: permissionKey,
        granted: nextGranted,
      }).unwrap();
      setToastType("success");
      setToastMessage(
        `${row.label} ${nextGranted ? "granted" : "revoked"} successfully.`,
      );
    } catch (toggleIssue) {
      setPermissions((previous) =>
        previous.map((item) => {
          if (item.key !== permissionKey) return item;
          return {
            ...item,
            roles: {
              ...item.roles,
              [selectedRole]: currentlyGranted,
            },
          };
        })
      );
      setToastType("error");
      setToastMessage(getApiErrorMessage(toggleIssue));
    } finally {
      setPendingCells((previous) => {
        const next = { ...previous };
        delete next[cellKey];
        return next;
      });
    }
  }

  async function handleOpenEditRole(roleKey: string) {
    const role = roles.find((item) => item.key === roleKey);
    if (!role) return;
    setIsEditRoleLoading(true);
    setEditRoleForm({
      roleId: role.roleId,
      key: role.key,
      name: "",
      displayName: "",
      description: "",
      isActive: role.active,
      organisationId: role.organisationId,
      permissionIds: [],
    });
    setToastMessage(null);
    try {
      const response = await getSuperAdminRoleById(role.roleId, false).unwrap();
      const roleName = typeof response.name === "string" && response.name.trim()
        ? response.name.trim()
        : role.key;
      const roleDisplayName = typeof response.displayName === "string" && response.displayName.trim()
        ? response.displayName.trim()
        : role.displayName;
      const roleDescription = typeof response.description === "string"
        ? response.description
        : "";

      const responsePermissions = Array.isArray(response.permissions) ? response.permissions : [];
      const permissionIds = extractPermissionIdsFromRoleDetail(
        responsePermissions,
        permissionIdByName,
      );
      const rolePermissionNames = responsePermissions
        .map((entry) => {
          if (typeof entry === "string") return entry.trim();
          if (!entry || typeof entry !== "object") return "";
          const name = (entry as { name?: unknown; permissionName?: unknown }).name
            ?? (entry as { permissionName?: unknown }).permissionName;
          return typeof name === "string" ? name.trim() : "";
        })
        .filter(Boolean);

      setEditRoleForm({
        roleId: role.roleId,
        key: role.key,
        name: roleName,
        displayName: roleDisplayName,
        description: roleDescription,
        isActive: typeof response.isActive === "boolean" ? response.isActive : role.active,
        organisationId:
          typeof response.organisationId === "number" ? response.organisationId : role.organisationId,
        permissionIds,
      });

      if (rolePermissionNames.length > 0) {
        setPermissions((previous) =>
          previous.map((permission) => ({
            ...permission,
            roles: {
              ...permission.roles,
              [role.key]: rolePermissionNames.includes(permission.key),
            },
          }))
        );
      }
    } catch (error) {
      setEditRoleForm(null);
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setIsEditRoleLoading(false);
    }
  }

  async function handleSaveRoleEdit() {
    if (!editRoleForm) return;

    const validationError = validateEditRoleForm({
      roleName: editRoleForm.name,
      displayName: editRoleForm.displayName,
      description: editRoleForm.description,
    });
    if (validationError) {
      setToastType("error");
      setToastMessage(validationError);
      return;
    }

    const trimmedRoleName = editRoleForm.name.trim();
    const trimmedDisplayName = editRoleForm.displayName.trim();
    const matrixPermissionIds = permissions
      .filter((permission) => Boolean(permission.roles[editRoleForm.key]))
      .map((permission) => permissionIdByName[permission.key])
      .filter((permissionId): permissionId is number => typeof permissionId === "number");

    // Prefer IDs from role detail (edit modal doesn't change permissions).
    // Fall back to matrix grants only if detail had none.
    const grantedPermissionIds = Array.from(
      new Set(
        editRoleForm.permissionIds.length > 0
          ? editRoleForm.permissionIds
          : matrixPermissionIds,
      ),
    );

    if (grantedPermissionIds.length === 0) {
      setToastType("error");
      setToastMessage("This role must keep at least one permission.");
      return;
    }

    try {
      setToastMessage(null);
      await updateRole({
        roleId: editRoleForm.roleId,
        body: {
          name: trimmedRoleName || editRoleForm.key,
          displayName: trimmedDisplayName || trimmedRoleName || editRoleForm.key,
          description: editRoleForm.description.trim(),
          isActive: editRoleForm.isActive,
          permissions: grantedPermissionIds,
        },
      }).unwrap();
      setEditRoleForm(null);
      setToastType("success");
      setToastMessage("Role updated successfully.");
      await refetch();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  }

  async function handleConfirmDeleteRole() {
    if (!deleteRoleTarget) return;
    try {
      setToastMessage(null);
      await deleteRole(deleteRoleTarget.roleId).unwrap();
      setDeleteRoleTarget(null);
      setToastType("success");
      setToastMessage("Role deleted successfully.");
      await refetch();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  }

  async function handleExportCsv() {
    const filters =
      moduleFilter === "All" && groupFilter === "All"
        ? undefined
        : {
            module: moduleFilter !== "All" ? moduleFilter : undefined,
            permissionGroup: groupFilter !== "All" ? groupFilter : undefined,
          };
    try {
      const blob = await exportMatrixCsv(filters).unwrap();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "roles-permissions-matrix.csv";
      anchor.rel = "noopener";
      anchor.click();
      URL.revokeObjectURL(url);
    } catch {
      // noop
    }
  }

  const errorMessage = isMatrixError ? getApiErrorMessage(matrixError) : null;

  return (
    <SuperAdminPageShell
      title="Roles & Permissions"
      description="Select a role first, then manage all permissions for that role."
    >
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
          <Button
            type="button"
            variant="secondary"
            size="md"
            disabled={isExportingCsv || isMatrixLoading}
            onClick={handleExportCsv}
            loading={isExportingCsv}
            loadingLabel="Exporting..."
          >
            <Upload size={14} aria-hidden="true" />
            Export
          </Button>
          <Button
            variant="primary"
            size="md"
            onClick={() => navigate("/super-admin/roles-and-permissions/create")}
          >
            <Plus size={15} aria-hidden="true" />
            Create Custom Role
          </Button>
        </div>

        <div className="grid h-[calc(100vh-13.75rem)] min-h-[42.5rem] grid-cols-1 rounded-[1rem] border border-[#e6edf2] bg-white lg:grid-cols-[17.5rem_minmax(0,1fr)]">
          <aside className="flex h-full min-h-0 min-w-0 flex-col border-r border-[#e6edf2] bg-[#fbfdff]">
            <div className="p-4">
              <Input
                value={roleSearch}
                onChange={(event) => setRoleSearch(event.target.value)}
                placeholder="Search roles..."
                className="h-11 rounded-full border-[#dde6ef] bg-white text-[#24313f] shadow-none placeholder:text-[#9aa5b1]"
              />
            </div>
            <div className="flex-1 overflow-y-auto">
              {visibleRoleKeys.map((roleKey) => {
                const selected = roleKey === selectedRole;
                const permissionCount = permissions.filter((row) => Boolean(row.roles[roleKey])).length;
                const roleMeta = roles.find((role) => role.key === roleKey);
                return (
                  <div
                    key={roleKey}
                    className={`w-full border-t border-[#e6edf2] px-4 py-3 text-left transition-colors ${selected ? "bg-[#eef4ff]" : "hover:bg-[#f5f9ff]"}`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <button
                        type="button"
                        onClick={() => setSelectedRole(roleKey)}
                        className="min-w-0 flex-1 overflow-hidden text-left"
                      >
                        <div
                          className="truncate text-[1rem] font-semibold text-[#24313f]"
                          title={roleMeta?.displayName ?? humanizeRole(roleKey)}
                        >
                          {roleMeta?.displayName ?? humanizeRole(roleKey)}
                        </div>
                        <div className="mt-1 text-[0.8125rem] text-[#7b8794]">
                          {permissionCount} permissions
                        </div>
                      </button>
                      {roleMeta ? (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <button
                              type="button"
                              onClick={(event) => event.stopPropagation()}
                              className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[#7b8794] hover:bg-[#e9eff6] hover:text-[#435361]"
                              aria-label={`Actions for ${roleMeta.displayName}`}
                            >
                              <MenuDotsIcon size={16} />
                            </button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent
                            align="end"
                            sideOffset={8}
                            collisionPadding={12}
                            className="z-[100] w-[9.5rem] rounded-[0.625rem] border border-[#e1e8ef] bg-white p-1 shadow-[0_12px_28px_rgba(15,23,42,0.12)]"
                          >
                            <DropdownMenuItem
                              onClick={(event) => {
                                event.stopPropagation();
                                handleOpenEditRole(roleKey);
                              }}
                              className="text-[#24313f]"
                            >
                              <Edit3 size={14} />
                              Edit
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={(event) => {
                                event.stopPropagation();
                                setDeleteRoleTarget(roleMeta);
                              }}
                              className="text-[#c0392b]"
                            >
                              <TrashIcon size={14} />
                              Delete
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      ) : null}
                    </div>
                  </div>
                );
              })}
              {!isMatrixLoading && visibleRoleKeys.length === 0 ? (
                <div className="px-4 py-8 text-[0.8125rem] text-[#7b8794]">No roles found.</div>
              ) : null}
            </div>
          </aside>

          <section className="flex min-h-0 min-w-0 flex-col overflow-hidden">
            <div className="relative shrink-0 border-b border-[#e6edf2] bg-white px-5 py-4">
              <div className="min-w-0 overflow-hidden">
                <div
                  className="truncate text-[1.25rem] font-semibold text-[#24313f]"
                  title={
                    selectedRoleMeta?.displayName ??
                    (selectedRole ? humanizeRole(selectedRole) : "Role Permissions")
                  }
                >
                  {selectedRoleMeta?.displayName ?? (selectedRole ? humanizeRole(selectedRole) : "Role Permissions")}
                </div>
                {selectedRoleDescription ? (
                  <div
                    className="mt-1 line-clamp-2 break-all text-[0.8125rem] text-[#7b8794]"
                    title={selectedRoleDescription}
                  >
                    {selectedRoleDescription}
                  </div>
                ) : null}
              </div>
              <div className="mt-3 flex flex-col gap-3 md:flex-row md:items-center">
                <Input
                  value={permissionSearch}
                  onChange={(event) => setPermissionSearch(event.target.value)}
                  placeholder="Search permissions..."
                  className="h-12 rounded-full border-[#dde6ef] bg-white text-[#24313f] shadow-none placeholder:text-[#9aa5b1] md:min-w-0 md:flex-1"
                />
                <div className="relative w-full shrink-0 md:w-[10.625rem]">
                  <Select value={moduleFilter} onValueChange={setModuleFilter}>
                    <SelectTrigger className={FILTER_SELECT_TRIGGER_CLASS}>
                      <SelectValue placeholder="Module: All" />
                    </SelectTrigger>
                    <SelectContent
                      position="popper"
                      side="bottom"
                      align="start"
                      collisionPadding={12}
                      className={FILTER_SELECT_CONTENT_CLASS}
                    >
                      {moduleOptions.map((module) => (
                        <SelectItem key={module} value={module}>
                          {module === "All" ? "Module: All" : module}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="relative w-full shrink-0 md:w-[10.625rem]">
                  <Select value={groupFilter} onValueChange={setGroupFilter}>
                    <SelectTrigger className={FILTER_SELECT_TRIGGER_CLASS}>
                      <SelectValue placeholder="Group: All" />
                    </SelectTrigger>
                    <SelectContent
                      position="popper"
                      side="bottom"
                      align="start"
                      collisionPadding={12}
                      className={FILTER_SELECT_CONTENT_CLASS}
                    >
                      {groupOptions.map((group) => (
                        <SelectItem key={group} value={group}>
                          {group === "All" ? "Group: All" : group}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>

            <div className="relative z-0 min-h-0 flex-1 overflow-y-auto p-5">
              {isMatrixLoading ? (
                <div className="text-sm text-[#7b8794]">Loading roles and permissions...</div>
              ) : null}
              {errorMessage ? <div className="text-sm text-red-600">{errorMessage}</div> : null}
              {!isMatrixLoading && !errorMessage && groupedPermissions.length === 0 ? (
                <div className="text-sm text-[#7b8794]">No permissions matched the current filters.</div>
              ) : null}

              <div className="space-y-4">
                {groupedPermissions.map(([moduleName, rows]) => (
                  <div key={moduleName} className="overflow-hidden rounded-[0.75rem] border border-[#e6edf2] bg-[#fbfdff]">
                    <div className="flex items-center justify-between border-b border-[#e6edf2] px-4 py-3">
                      <div className="text-[0.9375rem] font-semibold text-[#24313f]">{moduleName}</div>
                      <div className="text-[0.8125rem] text-[#7b8794]">{rows.length} permissions</div>
                    </div>

                    <div className="grid grid-cols-1 gap-x-6 gap-y-3 px-4 py-4 md:grid-cols-2">
                      {rows.map((permission) => {
                        const checked = selectedRole ? Boolean(permission.roles[selectedRole]) : false;
                        const cellKey = `${permission.key}:${selectedRole ?? ""}`;
                        return (
                          <div key={permission.key} className="flex items-center justify-between gap-3">
                            <label
                              htmlFor={`permission-${permission.permissionId}`}
                              className="cursor-pointer text-[0.875rem] text-[#2f3d4b]"
                            >
                              {permission.label}
                            </label>
                            <PermissionIndicator
                              id={`permission-${permission.permissionId}`}
                              checked={checked}
                              onToggle={() => handleTogglePermission(permission.key)}
                              disabled={!selectedRole || Boolean(pendingCells[cellKey])}
                            />
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>
      </div>

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(deleteRoleTarget)}
        onClose={() => setDeleteRoleTarget(null)}
        onConfirm={handleConfirmDeleteRole}
        title={`Delete Role "${deleteRoleNameForModal}"`}
        description="Are you sure you want to delete this role? This action cannot be undone."
        items={[
          "Role metadata and assignment settings",
          "Role-to-permission mappings",
          "Custom configuration tied to this role",
        ]}
        confirmButtonText={isDeletingRole ? "Deleting..." : "Delete role"}
        confirmButtonDisabled={isDeletingRole}
      />

      <EditRoleModal
        form={editRoleForm}
        isLoading={isEditRoleLoading}
        isSaving={isUpdatingRole}
        onClose={() => setEditRoleForm(null)}
        onSave={handleSaveRoleEdit}
        onChange={(updater) => {
          setEditRoleForm((previous) => (previous ? updater(previous) : previous));
        }}
      />
    </SuperAdminPageShell>
  );
}

export default RolesAndPermissions;
