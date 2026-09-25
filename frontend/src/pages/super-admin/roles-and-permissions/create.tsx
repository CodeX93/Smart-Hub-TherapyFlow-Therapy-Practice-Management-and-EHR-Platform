import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import Toast from "@/components/shared/Toast";
import { cn } from "@/lib/utils";
import {
  type PermissionCatalogSection,
  useCreateSuperAdminRoleMutation,
  useGetPermissionsCatalogQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  CREATE_ROLE_FIELD_LIMITS,
  sanitizeDisplayName,
  sanitizeRoleDescription,
  sanitizeRoleName,
  validateCreateRoleForm,
} from "./createRole.utils";

function PermissionCheckbox(props: {
  id: string;
  checked: boolean;
  onChange: () => void;
  label: string;
  description: string;
}) {
  return (
    <label
      htmlFor={props.id}
      className="flex cursor-pointer items-start gap-3 py-[1.125rem]"
    >
      <Checkbox
        id={props.id}
        checked={props.checked}
        onCheckedChange={props.onChange}
        className="mt-0.5 h-4 w-4 rounded-[0.25rem] border-[#dce5ee] checked:border-[#435564] checked:bg-[#435564]"
      />

      <div className="min-w-0">
        <div className="text-[0.875rem] font-medium leading-[1.375rem] text-[#2b3946]">
          {props.label}
        </div>
        <div className="mt-0.5 text-[0.75rem] font-normal leading-5 text-[#a0acb8]">
          {props.description}
        </div>
      </div>
    </label>
  );
}

function getCardClassName(): string {
  return "overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]";
}

function getInputClassName(): string {
  return cn(
    "h-full rounded-[1rem] border-0 bg-transparent px-4 shadow-none",
    "text-[0.875rem] text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function getTextareaClassName(): string {
  return cn(
    "min-h-[6.125rem] rounded-[1rem] border-[#dce5ee] bg-white px-4 py-3 shadow-none",
    "text-[0.875rem] text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-[#dce5ee] focus-visible:ring-0"
  );
}

function FieldHint(props: { children: React.ReactNode }) {
  return (
    <div className="mt-1.5 text-[0.6875rem] font-normal leading-4 text-[#a0acb8]">
      {props.children}
    </div>
  );
}

function FieldLabel(props: { children: React.ReactNode }) {
  return (
    <div className="pointer-events-none absolute left-4 top-3 text-[0.6875rem] font-medium leading-4 text-[#8a96a3]">
      {props.children}
    </div>
  );
}

function getFloatingInputPaddingClassName(hasValue: boolean): string {
  return hasValue ? "pb-2 pt-7" : "py-0";
}

function getFieldShellClassName(): string {
  return "relative h-[3.75rem] overflow-hidden rounded-[1rem] border border-[#dce5ee] bg-white";
}

function CreateCustomRole() {
  const navigate = useNavigate();
  const [roleName, setRoleName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [description, setDescription] = useState("");
  const [selectedPermissions, setSelectedPermissions] = useState<Record<string, boolean>>({});
  const {
    data: sections = [],
    isLoading: isPermissionsCatalogLoading,
    isError: isPermissionsCatalogError,
    error: permissionsCatalogError,
  } = useGetPermissionsCatalogQuery();
  const [createRole, { isLoading: isCreatingRole }] = useCreateSuperAdminRoleMutation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("error");
  const trimmedRoleName = roleName.trim();

  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (isPermissionsCatalogError && permissionsCatalogError && permissionsCatalogError !== reportedError) {
    setReportedError(permissionsCatalogError);
    setToastType("error");
    setToastMessage(getApiErrorMessage(permissionsCatalogError));
  }

  const missingPermissionKeys = sections.flatMap(section => section.items.map(item => item.key)).filter(key => !(key in selectedPermissions));
  if (missingPermissionKeys.length > 0) {
    setSelectedPermissions({ ...selectedPermissions, ...Object.fromEntries(missingPermissionKeys.map(key => [key, true])) });
  }

  function togglePermission(key: string) {
    setSelectedPermissions(function (previous) {
      return {
        ...previous,
        [key]: !previous[key],
      };
    });
  }

  function toggleSection(section: PermissionCatalogSection) {
    const allSelected = section.items.every(function (item) {
      return selectedPermissions[item.key];
    });

    setSelectedPermissions(function (previous) {
      const next = { ...previous };

      section.items.forEach(function (item) {
        next[item.key] = !allSelected;
      });

      return next;
    });
  }

  async function handleCreateRole() {
    const validationError = validateCreateRoleForm({
      roleName,
      displayName,
      description,
    });
    if (validationError) {
      showToast("error", validationError);
      return;
    }

    const selectedPermissionIds = sections
      .flatMap((section) => section.items)
      .filter((item) => selectedPermissions[item.key])
      .map((item) => {
        if (item.id > 0) return item.id;
        const parsedFromKey = Number.parseInt(item.key, 10);
        return Number.isFinite(parsedFromKey) && parsedFromKey > 0 ? parsedFromKey : 0;
      })
      .filter((id) => id > 0);

    if (selectedPermissionIds.length === 0) {
      showToast("error", "Select at least one permission.");
      return;
    }

    try {
      await createRole({
        name: trimmedRoleName,
        displayName: displayName.trim(),
        description: description.trim(),
        isActive: true,
        permissions: Array.from(new Set(selectedPermissionIds)),
      }).unwrap();
      showToast("success", "Role created successfully.");
      window.setTimeout(() => {
        navigate("/super-admin/roles-and-permissions");
      }, 700);
    } catch (error) {
      showToast("error", getApiErrorMessage(error));
    }
  }

  return (
    <SuperAdminPageShell
      title="Create Custom Role"
      description="Define a new custom role and assign specific platform permissions."
    >
      {toastMessage ? (
        <Toast
          type={toastType}
          message={toastMessage}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="flex flex-col gap-4">
        <div className={getCardClassName()}>
          <div className="px-6 py-6">
            <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
              Role Details
            </div>
            <div className="mt-1 text-[0.875rem] font-normal leading-[1.375rem] text-[#a0acb8]">
              Basic information about the custom role.
            </div>
          </div>

          <div className="border-t border-[#edf2f7] px-6 py-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <div className={getFieldShellClassName()}>
                  {roleName ? <FieldLabel>Role Name <span className="text-red-500">*</span></FieldLabel> : null}
                  <Input
                    value={roleName}
                    onChange={function (event) {
                      setRoleName(sanitizeRoleName(event.target.value));
                    }}
                    placeholder="Role Name *"
                    maxLength={CREATE_ROLE_FIELD_LIMITS.roleName}
                    className={cn(
                      getInputClassName(),
                      getFloatingInputPaddingClassName(Boolean(roleName))
                    )}
                  />
                </div>
                <FieldHint>
                  A unique machine-readable identifier. Use letters, numbers, and underscores ({trimmedRoleName.length}/{CREATE_ROLE_FIELD_LIMITS.roleName}).
                </FieldHint>
              </div>

              <div>
                <div className={getFieldShellClassName()}>
                  {displayName ? <FieldLabel>Display Name <span className="text-red-500">*</span></FieldLabel> : null}
                  <Input
                    value={displayName}
                    onChange={function (event) {
                      setDisplayName(sanitizeDisplayName(event.target.value));
                    }}
                    placeholder="Display Name *"
                    maxLength={CREATE_ROLE_FIELD_LIMITS.displayName}
                    className={cn(
                      getInputClassName(),
                      getFloatingInputPaddingClassName(Boolean(displayName))
                    )}
                  />
                </div>
                <FieldHint>
                  The human-readable name shown in the UI ({displayName.length}/{CREATE_ROLE_FIELD_LIMITS.displayName}).
                </FieldHint>
              </div>
            </div>

            <div className="mt-4">
              <div className="text-[1rem] font-medium leading-6 text-[#1f2d38]">
                Description
              </div>
              <Textarea
                value={description}
                onChange={function (event) {
                  setDescription(sanitizeRoleDescription(event.target.value));
                }}
                placeholder="Briefly describe what this feature controls..."
                maxLength={CREATE_ROLE_FIELD_LIMITS.description}
                className={cn(getTextareaClassName(), "mt-2")}
              />
              <FieldHint>{description.length}/{CREATE_ROLE_FIELD_LIMITS.description}</FieldHint>
            </div>
          </div>
        </div>

        <div className={getCardClassName()}>
          <div className="px-6 py-6">
            <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
              Permissions
            </div>
            <div className="mt-1 text-[0.875rem] font-normal leading-[1.375rem] text-[#a0acb8]">
              Select the API and module permissions to grant to this role.
            </div>
          </div>

          <div className="border-t border-[#edf2f7]">
            {isPermissionsCatalogLoading ? (
              <div className="px-6 py-10 text-sm text-[#7c8a97]">
                Loading permissions...
              </div>
            ) : null}
            {!isPermissionsCatalogLoading &&
            !isPermissionsCatalogError &&
            sections.length === 0 ? (
              <div className="px-6 py-10 text-sm text-[#7c8a97]">
                No permissions found.
              </div>
            ) : null}
            {!isPermissionsCatalogLoading &&
            !isPermissionsCatalogError &&
            sections.length > 0
              ? sections.map(function (section) {
                  return (
                    <div key={section.title} className="border-b border-[#edf2f7] last:border-b-0">
                      <div className="flex items-center justify-between bg-[#f5f8fb] px-6 py-[0.8125rem]">
                        <div className="text-[0.875rem] font-medium leading-[1.375rem] text-[#1f2d38]">
                          {section.title}
                        </div>
                        <button
                          type="button"
                          onClick={function () {
                            toggleSection(section);
                          }}
                          className="text-[0.875rem] font-medium leading-[1.375rem] text-[#5b87a4] hover:opacity-80"
                        >
                          Select All
                        </button>
                      </div>

                      <div className="px-6">
                        {section.items.map(function (permission, index) {
                          return (
                            <div
                              key={permission.key}
                              className={cn(
                                "border-b border-[#edf2f7]",
                                index === section.items.length - 1 ? "last:border-b-0" : ""
                              )}
                            >
                              <PermissionCheckbox
                                id={`create-role-permission-${permission.key}`}
                                checked={Boolean(selectedPermissions[permission.key])}
                                onChange={function () {
                                  togglePermission(permission.key);
                                }}
                                label={permission.key}
                                description={permission.description}
                              />
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  );
                })
              : null}
          </div>

          <div className="flex items-center justify-end gap-3 px-6 py-6">
            <Button
              type="button"
              variant="secondary"
              size="md"
              onClick={function () {
                navigate("/super-admin/roles-and-permissions");
              }}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="primary"
              size="md"
              onClick={handleCreateRole}
              disabled={isCreatingRole}
              loading={isCreatingRole}
              loadingLabel="Creating..."
            >
              Create Role
            </Button>
          </div>
        </div>
      </div>
    </SuperAdminPageShell>
  );
}

export default CreateCustomRole;
