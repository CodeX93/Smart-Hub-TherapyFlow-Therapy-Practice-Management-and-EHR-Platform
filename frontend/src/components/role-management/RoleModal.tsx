
import { ContentLoader } from "@/components/shared/ContentLoader";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomInput from "@/components/form/CustomInput";
import { Checkbox } from "@/components/ui/checkbox";
import { useForm, useWatch } from "react-hook-form";
import { Form } from "@/components/ui/form";
import { useEffect, useMemo, useRef } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import type { AdminRolePermission } from "@/store/api/admin/roles.api";

interface RoleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: RoleFormValues) => void;
  initialData?: RoleFormValues;
  mode: "create" | "edit";
  permissionsCatalog: AdminRolePermission[];
  isSubmitting?: boolean;
  isLoadingInitialData?: boolean;
}

export interface RoleFormValues {
  roleName: string;
  displayName: string;
  description: string;
  permissions: string[]; // array of permission IDs
}

const ROLE_NAME_MAX_LENGTH = 50;
const DISPLAY_NAME_MAX_LENGTH = 50;
const DESCRIPTION_MAX_LENGTH = 500;

const SESSION_SCHEDULING_PERMISSION_NAMES = new Set([
  "SESSION_CREATE",
  "SESSION_EDIT",
]);

const SESSION_REQUIRED_PERMISSION_NAMES = new Set([
  "ROOM_MANAGE",
  "USER_VIEW",
  "USER_MANAGE",
]);

const EXCLUDED_PERMISSION_CATEGORIES = new Set(["CLIENT_PORTAL", "AI"]);

function normalizePermissionCategory(category?: string): string {
  return (category || "general").trim().toUpperCase().replace(/\s+/g, "_");
}

function isExcludedRolePermission(permission: AdminRolePermission): boolean {
  const category = normalizePermissionCategory(permission.category);
  if (EXCLUDED_PERMISSION_CATEGORIES.has(category)) {
    return true;
  }

  const name = permission.name.trim().toUpperCase();
  return (
    name === "CLIENT_PORTAL_ACCESS" ||
    name.startsWith("CLIENT_PORTAL_") ||
    name === "AI_USE" ||
    name.startsWith("AI_")
  );
}

function applySessionPermissionDependencies(
  permissionsCatalog: AdminRolePermission[],
  selectedPermissionIds: string[],
): string[] {
  const hasSessionSchedulingPermission = permissionsCatalog.some(
    (permission) =>
      SESSION_SCHEDULING_PERMISSION_NAMES.has(permission.name.toUpperCase()) &&
      selectedPermissionIds.includes(String(permission.id)),
  );

  if (!hasSessionSchedulingPermission) {
    return selectedPermissionIds;
  }

  const requiredIds = permissionsCatalog
    .filter((permission) =>
      SESSION_REQUIRED_PERMISSION_NAMES.has(permission.name.toUpperCase()),
    )
    .map((permission) => String(permission.id));

  return Array.from(new Set([...selectedPermissionIds, ...requiredIds]));
}

const roleFormSchema = z.object({
  roleName: z
    .string()
    .min(1, "Role name is required")
    .max(ROLE_NAME_MAX_LENGTH, `Role name must be ${ROLE_NAME_MAX_LENGTH} characters or less`),
  displayName: z
    .string()
    .min(1, "Display name is required")
    .max(
      DISPLAY_NAME_MAX_LENGTH,
      `Display name must be ${DISPLAY_NAME_MAX_LENGTH} characters or less`
    ),
  description: z
    .string()
    .max(DESCRIPTION_MAX_LENGTH, `Description must be ${DESCRIPTION_MAX_LENGTH} characters or less`),
  permissions: z.array(z.string()),
});

const RoleModal = ({
  isOpen,
  onClose,
  onSubmit,
  initialData,
  mode,
  permissionsCatalog,
  isSubmitting = false,
  isLoadingInitialData = false,
}: RoleModalProps) => {
  const form = useForm<RoleFormValues>({
    resolver: zodResolver(roleFormSchema),
    mode: "onChange",
    defaultValues: {
      roleName: "",
      displayName: "",
      description: "",
      permissions: [],
    },
  });

  const seededOpenRef = useRef(false);

  useEffect(() => {
    if (!isOpen) {
      seededOpenRef.current = false;
      return;
    }
    // Wait until role details + permission catalog finish loading so edit
    // mode does not seed once with empty permissions and then ignore the
    // real assigned IDs when they arrive.
    if (isLoadingInitialData) return;
    if (mode === "edit" && !initialData) return;
    if (seededOpenRef.current) return;

    if (mode === "edit" && initialData) {
      form.reset(initialData);
    } else {
      form.reset({
        roleName: "",
        displayName: "",
        description: "",
        permissions: [],
      });
    }
    seededOpenRef.current = true;
  }, [isOpen, mode, initialData, form, isLoadingInitialData]);

  const selectedPermissions =
    useWatch({
      control: form.control,
      name: "permissions",
    }) || [];

  const visiblePermissions = useMemo(
    () => permissionsCatalog.filter((permission) => !isExcludedRolePermission(permission)),
    [permissionsCatalog],
  );

  const visiblePermissionIds = useMemo(
    () => new Set(visiblePermissions.map((permission) => String(permission.id))),
    [visiblePermissions],
  );

  const hiddenSelectedPermissions = useMemo(
    () => selectedPermissions.filter((permissionId) => !visiblePermissionIds.has(permissionId)),
    [selectedPermissions, visiblePermissionIds],
  );

  const selectedVisiblePermissionsCount = useMemo(
    () => selectedPermissions.filter((permissionId) => visiblePermissionIds.has(permissionId)).length,
    [selectedPermissions, visiblePermissionIds],
  );

  useEffect(() => {
    if (!isOpen || !permissionsCatalog.length) return;

    const withDependencies = applySessionPermissionDependencies(
      permissionsCatalog,
      selectedPermissions,
    );

    const isMissingDependency = withDependencies.some(
      (permissionId) => !selectedPermissions.includes(permissionId),
    );

    if (isMissingDependency) {
      form.setValue("permissions", withDependencies, { shouldDirty: true });
    }
  }, [form, isOpen, permissionsCatalog, selectedPermissions]);

  const totalPermissionsCount = visiblePermissions.length;

  const groupedPermissions = useMemo(() => {
    return visiblePermissions.reduce<Record<string, AdminRolePermission[]>>((acc, permission) => {
      const key = permission.category || "general";
      if (!acc[key]) acc[key] = [];
      acc[key].push(permission);
      return acc;
    }, {});
  }, [visiblePermissions]);

  const isAllSelected =
    visiblePermissions.length > 0 &&
    visiblePermissions.every((permission) =>
      selectedPermissions.includes(String(permission.id)),
    );

  const handleSelectAll = (checked: boolean) => {
    const visibleIds = visiblePermissions.map((permission) => String(permission.id));
    if (checked) {
      form.setValue(
        "permissions",
        Array.from(new Set([...hiddenSelectedPermissions, ...visibleIds])),
      );
    } else {
      form.setValue("permissions", hiddenSelectedPermissions);
    }
  };

  const handleTogglePermission = (id: string, checked: boolean) => {
    const current = [...selectedPermissions];
    const next = checked
      ? current.includes(id)
        ? current
        : [...current, id]
      : current.filter((pId) => pId !== id);

    form.setValue(
      "permissions",
      applySessionPermissionDependencies(permissionsCatalog, next),
      { shouldDirty: true },
    );
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div className="w-full max-w-2xl bg-white rounded-2xl shadow-lg flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="p-6 pb-4 flex items-center justify-between border-b border-(--neutral-50)">
          <h2 className="text-xl font-bold text-(--neutral-950)">
            {mode === "create" ? "Create New Role" : "Edit Role"}
          </h2>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onClose}
            aria-label="Close role modal"
          >
            <X size={24} aria-hidden="true" />
          </Button>
        </div>

        {/* Content */}
        <div className="p-6 pt-6 overflow-y-auto custom-scrollbar flex-1">
          {isLoadingInitialData ? (
            <ContentLoader size="md" />
          ) : (
          <Form {...form}>
            <form className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <CustomInput
                    control={form.control}
                    name="roleName"
                    label="Role Name"
                    required
                    placeholder=" "
                    maxLength={ROLE_NAME_MAX_LENGTH}
                    hint="e.g, custom_therapist"
                  />
                </div>
                <div className="space-y-1">
                  <CustomInput
                    control={form.control}
                    name="displayName"
                    label="Display Name"
                    required
                    placeholder=" "
                    maxLength={DISPLAY_NAME_MAX_LENGTH}
                    hint="e.g, Custom Therapist"
                  />
                </div>
              </div>

              <CustomTextarea
                control={form.control}
                name="description"
                label="Role description"
                className="min-h-32"
                placeholder=" "
                maxLength={DESCRIPTION_MAX_LENGTH}
              />

              {/* Permissions Section */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-bold text-(--neutral-950)">
                    Permissions
                  </h3>
                  <div className="flex items-center gap-4">
                    <span className="text-sm text-(--text-neutral-600)">
                      {selectedVisiblePermissionsCount}/{totalPermissionsCount}{" "}
                      selected
                    </span>
                    <div className="flex items-center gap-2">
                      <Checkbox
                        id="select-all"
                        checked={isAllSelected}
                        onCheckedChange={handleSelectAll}
                      />
                      <label
                        htmlFor="select-all"
                        className="text-sm font-medium text-(--neutral-950) cursor-pointer"
                      >
                        Select all
                      </label>
                    </div>
                  </div>
                </div>

                <div className="bg-(--bg-primary-50)/30 border border-(--neutral-100) rounded-xl p-6 space-y-8">
                  {Object.entries(groupedPermissions).map(([groupKey, groupPermissions]) => (
                    <div key={groupKey} className="space-y-3">
                      <h4 className="text-xs font-bold text-(--text-neutral-600) uppercase tracking-wider">
                        {groupKey.replaceAll("_", " ")}
                      </h4>
                      <div className="grid grid-cols-2 gap-x-8 gap-y-4">
                        {groupPermissions.map((permission) => (
                          <div
                            key={permission.name}
                            className="flex items-center gap-3"
                          >
                            <Checkbox
                              id={String(permission.id)}
                              checked={selectedPermissions.includes(
                                String(permission.id),
                              )}
                              onCheckedChange={(checked) =>
                                handleTogglePermission(String(permission.id), checked)
                              }
                            />
                            <label
                              htmlFor={String(permission.id)}
                              className="text-sm font-medium text-(--text-primary-dark) cursor-pointer"
                            >
                              {permission.displayName || permission.name}
                            </label>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </form>
          </Form>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end items-center gap-4 p-6 pt-4 border-t border-(--neutral-100)">
          <Button
            variant="secondary"
            size="lg"
            onClick={onClose}
            disabled={isSubmitting || isLoadingInitialData}
            className="min-w-32"
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            size="lg"
            onClick={form.handleSubmit(onSubmit)}
            disabled={isSubmitting || isLoadingInitialData || !form.formState.isValid}
            loading={isSubmitting}
            loadingLabel={mode === "create" ? "Creating..." : "Updating..."}
            className="min-w-32"
          >
            {mode === "create"
              ? "Create Role"
              : "Update Role"}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default RoleModal;
