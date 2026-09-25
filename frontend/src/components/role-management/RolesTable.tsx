import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { ArrowDown, Pencil } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export interface RoleEntry {
  id: string;
  roleName: string;
  displayName: string;
  description: string;
  permissionsAssigned: number;
}

interface RolesTableProps {
  roles: RoleEntry[];
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
  onSort: (key: keyof RoleEntry) => void;
  sortConfig: { key: string; direction: "asc" | "desc" };
}

const RolesTable = ({ roles, onEdit, onDelete, onSort, sortConfig }: RolesTableProps) => {
  return (
    <div className="w-full overflow-hidden rounded-xl border border-(--neutral-100) bg-white">
      <table className="w-full table-fixed border-collapse">
        <thead>
          <tr className="bg-(--bg-primary-50) border-b border-(--neutral-100)">
            <th className="py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              Role Name
            </th>
            <th className="py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              <div
                className="flex items-center gap-1 cursor-pointer select-none"
                onClick={() => onSort("displayName")}
              >
                Display Name
                <ArrowDown
                  size={16}
                  className={`transition-transform ${
                    sortConfig.key === "displayName" &&
                    sortConfig.direction === "desc"
                      ? "rotate-180"
                      : ""
                  }`}
                />
              </div>
            </th>
            <th className="py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              Description
            </th>
            <th className="py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              Permissions Assigned
            </th>
            <th className="w-20 py-4 px-6 text-center text-sm font-semibold text-(--text-primary-dark)">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {roles.map((role) => (
            <tr
              key={role.id}
              className="border-b last:border-0 border-(--neutral-100) hover:bg-gray-50/50 transition-colors"
            >
              <td className="py-4 px-6 text-sm font-medium text-(--text-primary-dark)">
                <div className="min-w-0 max-w-full truncate" title={role.roleName}>
                  {role.roleName}
                </div>
              </td>
              <td className="py-4 px-6 text-sm text-(--text-primary-dark)">
                <div className="min-w-0 max-w-full truncate" title={role.displayName}>
                  {role.displayName}
                </div>
              </td>
              <td className="py-4 px-6 text-sm text-(--text-neutral-600)">
                <div className="min-w-0 max-w-full truncate" title={role.description}>
                  {role.description}
                </div>
              </td>
              <td className="py-4 px-6 text-sm text-(--text-primary-dark)">
                {role.permissionsAssigned}
              </td>
              <td className="w-20 py-4 px-6">
                <div className="flex shrink-0 items-center justify-center gap-5 text-(--text-primary-dark)">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button
                        type="button"
                        className="cursor-pointer rounded-full p-1.5 hover:bg-(--neutral-100) transition-colors"
                      >
                        <MenuDotsIcon size={18} />
                      </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-36 rounded-xl">
                      <DropdownMenuItem
                        className="text-sm cursor-pointer"
                        onClick={() => onEdit(role.id)}
                      >
                        <Pencil size={14} className="mr-2" />
                        Edit
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-sm cursor-pointer text-(--status-denied) focus:text-(--status-denied)"
                        onClick={() => onDelete(role.id)}
                      >
                        <TrashIcon size={14} className="mr-2" />
                        Delete
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default RolesTable;
