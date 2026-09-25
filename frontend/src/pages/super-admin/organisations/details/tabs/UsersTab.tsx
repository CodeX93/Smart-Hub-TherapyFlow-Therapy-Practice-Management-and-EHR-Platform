import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { useMemo, useState } from "react";
import { Download, Search } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import type {
  OrganisationMember,
  OrganisationMemberRole,
  OrganisationMemberStatus,
  OrganisationRow,
} from "../../organisations.data";
import {
  useDisableSuperAdminUserMutation,
  useEnableSuperAdminUserMutation,
  useGetOrganisationUsersQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

const STATUS_OPTIONS: Array<OrganisationMemberStatus | "All"> = [
  "All",
  "Active",
  "Inactive",
  "Suspended",
];

const ROLE_OPTIONS: Array<OrganisationMemberRole | "All"> = [
  "All",
  "Admin",
  "Supervisor",
  "Therapist",
  "Client",
];

interface UsersTabProps {
  org: OrganisationRow;
  organisationId: number | null;
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map(function (part) {
      return part[0]?.toUpperCase() ?? "";
    })
    .join("");
}

function getStatusClassName(status: OrganisationMemberStatus): string {
  if (status === "Active") {
    return "bg-[#dcfce7] text-[#16a34a]";
  }

  if (status === "Pending") {
    return "bg-[#dcfce7] text-[#22c55e]";
  }

  if (status === "Inactive") {
    return "bg-[#fef2f2] text-[#ef4444]";
  }

  return "bg-[#d1fae5] text-[#059669]";
}

function getSelectLabel(prefix: string, value: string): string {
  return prefix + ": " + value;
}

function normalizeApiRole(value: string): OrganisationMemberRole {
  const normalized = value.trim().toLowerCase();
  if (normalized.includes("supervisor")) return "Supervisor";
  if (normalized.includes("therapist")) return "Therapist";
  if (normalized.includes("client")) return "Client";
  return "Admin";
}

function normalizeApiStatus(value: string): OrganisationMemberStatus {
  const normalized = value.trim().toLowerCase();
  if (normalized.includes("pending")) return "Pending";
  if (normalized.includes("inactive")) return "Inactive";
  if (normalized.includes("suspend") || normalized.includes("locked")) return "Suspended";
  return "Active";
}

function toApiStatusFilter(
  status: OrganisationMemberStatus | "All",
): string | undefined {
  if (status === "All") return undefined;
  if (status === "Suspended") return "locked";
  return status;
}

function downloadCsv(rows: OrganisationMember[], orgName: string) {
  const header = ["Name", "Email", "Role", "Status", "Last Login"];
  const lines = rows.map(function (row) {
    return [row.name, row.email, row.role, row.status, row.lastLogin]
      .map(function (value) {
        return '"' + value.replaceAll('"', '""') + '"';
      })
      .join(",");
  });
  const blob = new Blob([[header.join(","), ...lines].join("\n")], {
    type: "text/csv;charset=utf-8;",
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download =
    orgName.toLowerCase().replaceAll(" ", "-") + "-users-export.csv";
  link.click();
  window.URL.revokeObjectURL(url);
}

function UsersTab(props: UsersTabProps) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<OrganisationMemberStatus | "All">(
    "All"
  );
  const [roleFilter, setRoleFilter] = useState<OrganisationMemberRole | "All">(
    "All"
  );
  const searchValue = search.trim();
  const [actionErrorMessage, setActionErrorMessage] = useState<string | null>(
    null
  );
  const [disableTargetUser, setDisableTargetUser] =
    useState<OrganisationMember | null>(null);
  const [disableReason, setDisableReason] = useState("");
  const {
    data: users = [],
    isLoading: isUsersLoading,
    isError: isUsersError,
    error: usersError,
    refetch: refetchUsers,
  } = useGetOrganisationUsersQuery(
    {
      id: props.organisationId ?? 0,
      search: searchValue || undefined,
      role: roleFilter === "All" ? undefined : roleFilter,
      status: toApiStatusFilter(statusFilter),
      page: 0,
      pageSize: 50,
    },
    { skip: !props.organisationId, refetchOnMountOrArgChange: true }
  );
  const [disableSuperAdminUser, { isLoading: isDisablePending }] =
    useDisableSuperAdminUserMutation();
  const [enableSuperAdminUser, { isLoading: isEnablePending }] =
    useEnableSuperAdminUserMutation();

  const filteredUsers = useMemo(
    function () {
      return users.map(function (member) {
        return {
          id: member.id,
          name: member.name,
          email: member.email,
          role: normalizeApiRole(member.role),
          status: normalizeApiStatus(member.status),
          lastLogin: member.lastLogin,
        } as OrganisationMember;
      });
    },
    [users]
  );

  async function onEnableUser(member: OrganisationMember) {
    setActionErrorMessage(null);
    try {
      await enableSuperAdminUser(member.id).unwrap();
      await refetchUsers();
    } catch (error) {
      setActionErrorMessage(getApiErrorMessage(error));
    }
  }

  async function onConfirmDisableUser() {
    if (!disableTargetUser) return;

    setActionErrorMessage(null);
    try {
      await disableSuperAdminUser({
        authId: disableTargetUser.id,
        reason: disableReason.trim(),
      }).unwrap();
      await refetchUsers();
    } catch (error) {
      setActionErrorMessage(getApiErrorMessage(error));
    } finally {
      setDisableTargetUser(null);
      setDisableReason("");
    }
  }

  return (
    <div className="flex w-full flex-col gap-3">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex min-w-0 flex-col gap-3 lg:flex-row lg:items-center">
          <div className="relative w-full lg:w-[18.875rem]">
            <Search
 className="size-4 absolute top-1/2 left-3.5 -translate-y-1/2 text-[#667483]"
 aria-hidden="true" />
            <Input
              value={search}
              onChange={function (event) {
                setSearch(event.target.value);
              }}
              placeholder="Search by name or email..."
              className={cn(
                "h-11 rounded-full border border-[#dce5ee] bg-white pl-10 pr-4 shadow-none",
                "text-[0.8125rem] text-[#21303d] placeholder:text-[#97a4b0]"
              )}
            />
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <Select
              value={statusFilter}
              onValueChange={function (value) {
                setStatusFilter(value as OrganisationMemberStatus | "All");
              }}
            >
              <SelectTrigger
                className={cn(
                  "h-11 w-full rounded-full border border-[#dce5ee] bg-white px-4 shadow-none sm:w-[7.5rem]",
                  "text-[0.8125rem] text-[#52606d] [&_svg]:text-[#97a4b0]"
                )}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
                {STATUS_OPTIONS.map(function (status) {
                  return (
                    <SelectItem
                      key={status}
                      value={status}
                      className="text-[#21303d] focus:bg-[#f4f7fa] focus:text-[#21303d]"
                    >
                      {getSelectLabel("Status", status)}
                    </SelectItem>
                  );
                })}
              </SelectContent>
            </Select>

            <Select
              value={roleFilter}
              onValueChange={function (value) {
                setRoleFilter(value as OrganisationMemberRole | "All");
              }}
            >
              <SelectTrigger
                className={cn(
                  "h-11 w-full rounded-full border border-[#dce5ee] bg-white px-4 shadow-none sm:w-[6.75rem]",
                  "text-[0.8125rem] text-[#52606d] [&_svg]:text-[#97a4b0]"
                )}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
                {ROLE_OPTIONS.map(function (role) {
                  return (
                    <SelectItem
                      key={role}
                      value={role}
                      className="text-[#21303d] focus:bg-[#f4f7fa] focus:text-[#21303d]"
                    >
                      {getSelectLabel("Role", role)}
                    </SelectItem>
                  );
                })}
              </SelectContent>
            </Select>
          </div>
        </div>

        <Button
          variant="outline"
          className={cn(
            "h-11 rounded-full border border-[#dce5ee] bg-white px-4 text-[0.8125rem] font-medium text-[#52606d]",
            "hover:bg-[#f8fafc]"
          )}
          onClick={function () {
            downloadCsv(filteredUsers, props.org.name);
          }}
        >
          <Download size={15} aria-hidden="true" />
          Export
        </Button>
      </div>

      <div className="overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        {actionErrorMessage ? (
          <div className="border-b border-[#fecdca] bg-[#fffbfa] px-4 py-2 text-sm text-[#b42318]">
            {actionErrorMessage}
          </div>
        ) : null}
        <div className="grid grid-cols-[1.6fr_1.85fr_1fr_1fr_1.2fr_4.5rem] items-center bg-[#f5f8fb] px-4 py-3.5">
          <div className="text-[0.75rem] font-semibold text-[#273540]">Name</div>
          <div className="text-[0.75rem] font-semibold text-[#273540]">Email</div>
          <div className="text-[0.75rem] font-semibold text-[#273540]">Role</div>
          <div className="text-[0.75rem] font-semibold text-[#273540]">Status</div>
          <div className="text-[0.75rem] font-semibold text-[#273540]">
            Last Login
          </div>
          <div className="text-right text-[0.75rem] font-semibold text-[#273540]">
            Actions
          </div>
        </div>

        {isUsersLoading ? (
          <div className="px-4 py-10 text-center text-sm font-medium text-[#7b8794]">
            Loading users...
          </div>
        ) : isUsersError ? (
          <div className="px-4 py-10 text-center text-sm font-medium text-(--status-denied)">
            {getApiErrorMessage(usersError)}
          </div>
        ) : filteredUsers.length === 0 ? (
          <div className="px-4 py-10 text-center text-sm font-medium text-[#7b8794]">
            No users match the current filters.
          </div>
        ) : (
          <div className="divide-y divide-[#edf2f7]">
            {filteredUsers.map(function (member) {
              return (
                <div
                  key={member.id}
                  className="grid grid-cols-[1.6fr_1.85fr_1fr_1fr_1.2fr_4.5rem] items-center px-4 py-3.5"
                >
                  <div className="flex min-w-0 items-center gap-2.5">
                    <Avatar className="h-7.5 w-7.5 border border-[#ead9c8] bg-[#efe6dd]">
                      <AvatarFallback className="bg-[#efe6dd] text-[0.625rem] font-semibold text-[#7a6757]">
                        {getInitials(member.name)}
                      </AvatarFallback>
                    </Avatar>
                    <span className="truncate text-[0.8125rem] font-medium text-[#2b3946]">
                      {member.name}
                    </span>
                  </div>

                  <div className="truncate pr-4 text-[0.8125rem] text-[#52606d]">
                    {member.email}
                  </div>

                  <div className="text-[0.8125rem] text-[#52606d]">{member.role}</div>

                  <div>
                    <span
                      className={cn(
                        "inline-flex rounded-full px-2.5 py-1 text-[0.6875rem] font-medium leading-none",
                        getStatusClassName(member.status)
                      )}
                    >
                      {member.status}
                    </span>
                  </div>

                  <div className="text-[0.8125rem] text-[#52606d]">
                    {member.lastLogin}
                  </div>

                  <div className="flex justify-end">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button
                          type="button"
                          className="grid h-8 w-8 place-items-center rounded-full text-[#273540] transition-colors hover:bg-[#f4f7fa]"
                          aria-label={"More actions for " + member.name}
                        >
                          <MenuDotsIcon size={16} aria-hidden="true" />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-36">
                        {member.status === "Active" ? (
                          <DropdownMenuItem
                            disabled={isDisablePending}
                            onClick={function () {
                              setDisableReason("");
                              setDisableTargetUser(member);
                            }}
                            className="text-[#b42318] focus:text-[#b42318]"
                          >
                            Disable
                          </DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem
                            disabled={isEnablePending}
                            onClick={function () {
                              void onEnableUser(member);
                            }}
                          >
                            Enable
                          </DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
      <ConfirmationModal
        type="disable"
        isOpen={Boolean(disableTargetUser)}
        onClose={function () {
          setDisableTargetUser(null);
          setDisableReason("");
        }}
        onConfirm={function () {
          void onConfirmDisableUser();
        }}
        title={disableTargetUser ? `Disable "${disableTargetUser.name}"` : undefined}
        description="Are you sure you want to disable this user account?"
        reasonLabel="Reason"
        reasonPlaceholder="Enter disable reason"
        reasonValue={disableReason}
        onReasonChange={setDisableReason}
        confirmButtonText={isDisablePending ? "Disabling..." : "Disable user"}
        confirmButtonDisabled={isDisablePending || !disableReason.trim()}
      />
    </div>
  );
}

export default UsersTab;
