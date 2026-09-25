import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Button } from "../ui/button";
import { Form } from "../ui/form";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import { cn } from "@/lib/utils";
import {
  addUserSchema,
  USER_ACCESS_PROFILE_LIMITS,
  type AddUserFormValues,
} from "@/schemas/user-access-profiles.schema";
import { useLazyGetTenantAdminRolesPagedQuery } from "@/store/api/admin/roles.api";
import { getApiErrorMessage } from "@/utils/apiError";

interface AddUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (user: AddUserFormValues) => Promise<void> | void;
  isSubmitting?: boolean;
  errorMessage?: string | null;
}

  const prioritizeRoleOptions = (
    options: { value: string; label: string }[],
  ) => {
    const priority = ["THERAPIST", "ADMIN"];
    const rank = (value: string) => {
      const index = priority.indexOf(value.trim().toUpperCase());
      return index === -1 ? priority.length : index;
    };

    return [...options].sort((left, right) => {
      const rankDiff = rank(left.value) - rank(right.value);
      if (rankDiff !== 0) return rankDiff;
      return left.label.localeCompare(right.label);
    });
  };

const AddUserModalContent = ({
  isOpen,
  onClose,
  onAdd,
  isSubmitting = false,
  errorMessage,
}: AddUserModalProps) => {
  const form = useForm<AddUserFormValues>({
    resolver: zodResolver(addUserSchema),
    mode: "onChange",
    defaultValues: {
      fullName: "",
      email: "",
      username: "",
      phone: "",
      role: "",
      password: "",
    },
  });
  const [rolesPage, setRolesPage] = useState(1);
  const [rolesTotalPages, setRolesTotalPages] = useState(1);
  const [roleOptions, setRoleOptions] = useState<{ value: string; label: string }[]>([]);
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
        value: role.name,
        label: role.displayName || role.name,
      }));

      setRoleOptions((previous) => {
        if (replace) return prioritizeRoleOptions(mapped);
        const existing = new Set(previous.map((entry) => entry.value));
        const appended = mapped.filter((entry) => !existing.has(entry.value));
        return prioritizeRoleOptions([...previous, ...appended]);
      });
    }).catch(error => {
      setRolesError(getApiErrorMessage(error));
    });
  }, [loadTenantRoles]);



  useEffect(() => { void fetchRolesPage(1, true); }, [fetchRolesPage]);

  const roleSelectOptions = useMemo(() => roleOptions, [roleOptions]);

  if (!isOpen) return null;

  const onSubmit = async (values: AddUserFormValues) => {
    await onAdd(values);
  };

  const handleClose = () => {
    if (isSubmitting) return;
    onClose();
  };

  return (
    <div className="fixed inset-0 z-999 flex items-center justify-center p-4">
      {/* Overlay */}
      <div
        className="fixed inset-0 bg-black/40 backdrop-blur-[0.0625rem]"
        onClick={handleClose}
      />

      {/* Modal Container */}
      <div className="relative z-1000 flex h-full max-h-[90vh] w-full max-w-145 min-h-0 flex-col overflow-hidden rounded-2xl bg-white shadow-2xl animate-in fade-in zoom-in duration-200">
        {/* Header */}
        <div className="flex shrink-0 items-center justify-between p-6 pb-4">
          <h2 className="text-xl font-semibold text-(--text-primary-dark)">
            Add New User
          </h2>
          <button
            onClick={handleClose}
            className="cursor-pointer rounded-full p-1 text-(--text-neutral-400) transition-all duration-200 hover:bg-(--neutral-100) hover:text-(--text-primary-dark)"
          >
            <X size={22} />
          </button>
        </div>

        {/* Content */}
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-6 py-2">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <CustomInput
                control={form.control}
                name="fullName"
                label="Full Name"
                required
                maxLength={USER_ACCESS_PROFILE_LIMITS.fullName}
                className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100) focus:border-(--neutral-600)"
              />

              <CustomInput
                control={form.control}
                name="email"
                label="Email"
                required
                maxLength={USER_ACCESS_PROFILE_LIMITS.email}
                className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100) focus:border-(--neutral-600)"
              />

              <CustomInput
                control={form.control}
                name="username"
                label="Username"
                required
                maxLength={USER_ACCESS_PROFILE_LIMITS.username}
                className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100) focus:border-(--neutral-600)"
              />

              <CustomInput
                control={form.control}
                name="phone"
                label="Phone"
                type="tel"
                inputMode="tel"
                maxLength={USER_ACCESS_PROFILE_LIMITS.phone}
                hint={`Optional. May start with +, up to ${USER_ACCESS_PROFILE_LIMITS.phone} characters`}
                className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100) focus:border-(--neutral-600)"
              />

              <CustomSelect
                control={form.control}
                required
                name="role"
                label="Role"
                options={roleSelectOptions}
                placeholder="Search role..."
                className="pt-7 min-h-14 bg-white rounded-xl shadow-none border-(--neutral-100)"
                onOpenChange={(open) => {
                  setIsRoleDropdownOpen(open);
                  if (open && roleOptions.length === 0 && !isFetchingRoles) {
                    void fetchRolesPage(1, true);
                  }
                }}
                onMenuScrollToEnd={() => {
                  if (!isRoleDropdownOpen || isFetchingRoles || !hasMoreRoles) return;
                  void fetchRolesPage(rolesPage + 1, false);
                }}
                hasMore={hasMoreRoles}
                isLoadingMore={isFetchingRoles && rolesPage > 0}
                loadingMoreLabel="Loading roles..."
              />
              {rolesError ? (
                <p className="text-xs text-(--status-denied) -mt-2 px-1">{rolesError}</p>
              ) : null}

              <CustomInput
                control={form.control}
                name="password"
                label="Password"
                type="password"
                required
                maxLength={USER_ACCESS_PROFILE_LIMITS.password}
                hint={`Required. ${USER_ACCESS_PROFILE_LIMITS.password} characters max, at least 8`}
                className="rounded-xl min-h-14 pt-7 shadow-none border-(--neutral-100) focus:border-(--neutral-600)"
              />
            </form>
          </Form>
        </div>

        {/* Footer */}
        <div className="shrink-0 border-t border-(--neutral-100) p-6">
          {errorMessage ? (
            <div className="mb-4 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {errorMessage}
            </div>
          ) : null}
          <div className="flex items-center justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isSubmitting}
              className="h-11 px-8 rounded-full border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-600) font-semibold transition-all cursor-pointer"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              onClick={form.handleSubmit(onSubmit)}
              disabled={!form.formState.isValid || isSubmitting}
              loading={isSubmitting}
              loadingLabel="Adding..."
              className={cn(
                "h-11 px-8 rounded-full font-semibold transition-all cursor-pointer",
                form.formState.isValid && !isSubmitting
                  ? "bg-(--bg-primary-dark) text-white hover:bg-(--bg-primary-dark)/90"
                  : "bg-(--neutral-100) text-(--text-neutral-400) cursor-not-allowed",
              )}
            >
              Add User
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

const AddUserModal = (props: AddUserModalProps) => props.isOpen ? <AddUserModalContent {...props} /> : null;

export default AddUserModal;
