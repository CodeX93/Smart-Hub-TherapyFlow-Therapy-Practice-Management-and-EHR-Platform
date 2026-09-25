import { useCallback, useEffect, useMemo, useState } from "react";
import { X } from "lucide-react";
import { Button } from "../ui/button";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import { cn } from "@/lib/utils";
import { useLazyGetTenantAdminRolesPagedQuery } from "@/store/api/admin/roles.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { formatAdminUserRoleLabel } from "@/utils/adminUserRoleDisplay";
import { USER_ACCESS_PROFILE_LIMITS } from "@/schemas/user-access-profiles.schema";

export interface EditUserBasicInfoValues {
  fullName: string;
  email: string;
  username: string;
  phone: string;
  role: string;
}

function normalizeRoleKey(value: string): string {
  return value.trim().toUpperCase().replace(/[\s-]+/g, "_");
}

function rolesMatch(left: string, right: string): boolean {
  if (!left || !right) return false;
  if (left === right) return true;
  return normalizeRoleKey(left) === normalizeRoleKey(right);
}

function normalizeEditValues(values: EditUserBasicInfoValues): EditUserBasicInfoValues {
  return {
    fullName: values.fullName.trim(),
    email: values.email.trim(),
    username: values.username.trim(),
    phone: values.phone.trim(),
    role: values.role.trim(),
  };
}

function editValuesEqual(
  left: EditUserBasicInfoValues,
  right: EditUserBasicInfoValues,
): boolean {
  const normalizedLeft = normalizeEditValues(left);
  const normalizedRight = normalizeEditValues(right);
  return (
    normalizedLeft.fullName === normalizedRight.fullName &&
    normalizedLeft.email === normalizedRight.email &&
    normalizedLeft.username === normalizedRight.username &&
    normalizedLeft.phone === normalizedRight.phone &&
    normalizedLeft.role === normalizedRight.role
  );
}

interface EditUserBasicInfoModalProps {
  isOpen: boolean;
  isSubmitting?: boolean;
  errorMessage?: string | null;
  initialValues: EditUserBasicInfoValues;
  onClose: () => void;
  onSubmit: (values: EditUserBasicInfoValues) => Promise<void> | void;
}

const EditUserBasicInfoModalContent = ({
  isOpen,
  onClose,
  onSubmit,
  initialValues,
  isSubmitting = false,
  errorMessage,
}: EditUserBasicInfoModalProps) => {
  const [values, setValues] = useState<EditUserBasicInfoValues>(initialValues);
  const [rolesPage, setRolesPage] = useState(1);
  const [rolesTotalPages, setRolesTotalPages] = useState(1);
  const [roleOptions, setRoleOptions] = useState<
    { value: string; label: string; name?: string }[]
  >([]);
  const [rolesError, setRolesError] = useState<string | null>(null);
  const [isRoleDropdownOpen, setIsRoleDropdownOpen] = useState(false);
  const [loadTenantRoles, { isFetching: isFetchingRoles }] = useLazyGetTenantAdminRolesPagedQuery();


  const hasMoreRoles = rolesPage < rolesTotalPages;



  const fetchRolesPage = useCallback((targetPage: number, replace: boolean) => {
    return loadTenantRoles({
        page: targetPage,
        pageSize: 20,
        sortBy: "displayName",
        sortDirection: "asc",
      }).unwrap().then(response => {

      setRolesPage(response.page);
      setRolesTotalPages(response.totalPages);
      setRolesError(null);

      const mapped = response.items.map((role) => ({
        value: role.displayName || role.name,
        label: role.displayName || role.name,
        name: role.name,
      }));

      setRoleOptions((previous) => {
        if (replace) return mapped;
        const existing = new Set(previous.map((entry) => entry.value));
        const appended = mapped.filter((entry) => !existing.has(entry.value));
        return [...previous, ...appended];
      });
    }).catch(error => {
      setRolesError(getApiErrorMessage(error));
    });
  }, [loadTenantRoles]);

  useEffect(() => { void fetchRolesPage(1, true); }, [fetchRolesPage]);

  const matchedOption = roleOptions.find((option) => rolesMatch(option.value, values.role) || rolesMatch(option.name ?? "", values.role));
  if (matchedOption && matchedOption.value !== values.role) {
    setValues({ ...values, role: matchedOption.value });
  }

  const resolvedRoleOptions = useMemo(() => {
    if (!values.role) return roleOptions;

    const hasCurrent = roleOptions.some(
      (option) =>
        rolesMatch(option.value, values.role) ||
        rolesMatch(option.name ?? "", values.role),
    );
    if (hasCurrent) return roleOptions;

    return [
      {
        value: values.role,
        label: formatAdminUserRoleLabel(values.role) || values.role,
      },
      ...roleOptions,
    ];
  }, [roleOptions, values.role]);

  const isDirty = useMemo(
    () => !editValuesEqual(values, initialValues),
    [initialValues, values],
  );

  const isAdminRoleLocked = useMemo(
    () => normalizeRoleKey(initialValues.role) === "ADMIN",
    [initialValues.role],
  );

  if (!isOpen) return null;

  const isValid =
    values.fullName.trim().length >= 2 &&
    values.fullName.trim().length <= USER_ACCESS_PROFILE_LIMITS.fullName &&
    values.username.trim().length >= 3 &&
    values.username.trim().length <= USER_ACCESS_PROFILE_LIMITS.username &&
    values.email.includes("@") &&
    values.email.trim().length <= USER_ACCESS_PROFILE_LIMITS.email &&
    (values.phone.trim() === "" ||
      (/^\+?\d*$/.test(values.phone.trim()) &&
        values.phone.trim().length <= USER_ACCESS_PROFILE_LIMITS.phone)) &&
    values.role.trim().length > 0;

  async function handleSubmit() {
    if (!isValid || !isDirty || isSubmitting) return;
    await onSubmit(values);
  }

  return (
    <div className="fixed inset-0 z-999 flex items-center justify-center">
      <div
        className="fixed inset-0 bg-black/40 backdrop-blur-[0.0625rem]"
        onClick={() => {
          if (!isSubmitting) onClose();
        }}
      />

      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-145 mx-4 z-1000 overflow-hidden flex flex-col animate-in fade-in zoom-in duration-200">
        <div className="flex items-center justify-between p-6">
          <h2 className="text-xl font-semibold text-(--text-primary-dark)">Edit User</h2>
          <button
            onClick={onClose}
            className="text-(--text-neutral-400) hover:text-(--text-primary-dark) p-1 cursor-pointer rounded-full transition-all duration-200 hover:bg-(--neutral-100)"
            disabled={isSubmitting}
          >
            <X size={22} />
          </button>
        </div>

        <div className="px-6 py-3 overflow-y-auto space-y-5">
          {errorMessage ? (
            <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {errorMessage}
            </div>
          ) : null}

          <div className="mb-3">
            <CustomInput
              label="Full Name"
              value={values.fullName}
              onChange={(event) =>
                setValues((previous) => ({ ...previous, fullName: event.target.value }))
              }
              required
              maxLength={USER_ACCESS_PROFILE_LIMITS.fullName}
              className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100) focus:border-(--neutral-600)"
            />
          </div>

          <div className="mb-3">
            <CustomInput
              label="Email"
              value={values.email}
              disabled
              required
              maxLength={USER_ACCESS_PROFILE_LIMITS.email}
              className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100)"
            />
          </div>

          <div className="mb-3">
            <CustomInput
              label="Username"
              value={values.username}
              disabled
              maxLength={USER_ACCESS_PROFILE_LIMITS.username}
              required
              className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100)"
            />
          </div>

          <div className="mb-3">
            <CustomInput
              label="Phone"
              value={values.phone}
              type="tel"
              inputMode="tel"
              maxLength={USER_ACCESS_PROFILE_LIMITS.phone}
              hint={`Optional. May start with +, up to ${USER_ACCESS_PROFILE_LIMITS.phone} characters`}
              onChange={(event) =>
                setValues((previous) => ({ ...previous, phone: event.target.value }))
              }
              className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100) focus:border-(--neutral-600)"
            />
          </div>

          <div>
            <CustomSelect
              label="Role"
              options={resolvedRoleOptions}
              value={values.role}
              onChange={(value) => setValues((previous) => ({ ...previous, role: value }))}
              placeholder="Select role"
              className="pt-7 min-h-14 bg-white rounded-xl shadow-none border-(--neutral-100)"
              isSearch={false}
              disabled={isAdminRoleLocked}
              onOpenChange={(open) => {
                if (isAdminRoleLocked) return;
                setIsRoleDropdownOpen(open);
                if (open && roleOptions.length === 0 && !isFetchingRoles) {
                  void fetchRolesPage(1, true);
                }
              }}
              onMenuScrollToEnd={() => {
                if (
                  isAdminRoleLocked ||
                  !isRoleDropdownOpen ||
                  isFetchingRoles ||
                  !hasMoreRoles
                ) {
                  return;
                }
                void fetchRolesPage(rolesPage + 1, false);
              }}
              hasMore={hasMoreRoles}
              isLoadingMore={isFetchingRoles && rolesPage > 0}
              loadingMoreLabel="Loading roles..."
            />
            {rolesError ? (
              <p className="text-xs text-(--status-denied) mt-1 px-1">{rolesError}</p>
            ) : null}
          </div>
        </div>

        <div className="p-6 pt-8">
          <div className="flex items-center justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting}
              className="h-11 px-8 rounded-full border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-600) font-semibold transition-all cursor-pointer"
            >
              Cancel
            </Button>
            <Button
              type="button"
              onClick={handleSubmit}
              disabled={!isValid || !isDirty || isSubmitting}
              loading={isSubmitting}
              loadingLabel="Saving..."
              className={cn(
                "h-11 px-8 rounded-full font-semibold transition-all cursor-pointer",
                isValid && isDirty && !isSubmitting
                  ? "bg-(--bg-primary-dark) text-white hover:bg-(--bg-primary-dark)/90"
                  : "bg-(--neutral-100) text-(--text-neutral-400) cursor-not-allowed"
              )}
            >
              Save Changes
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

const EditUserBasicInfoModal = (props: EditUserBasicInfoModalProps) => props.isOpen ? <EditUserBasicInfoModalContent {...props} /> : null;

export default EditUserBasicInfoModal;
