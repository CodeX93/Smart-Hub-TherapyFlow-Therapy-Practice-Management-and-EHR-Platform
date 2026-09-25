import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { Eye, Pencil } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export interface FeatureCatalogRow {
  name: string;
  keyName: string;
  description: string;
  scope: "Tenant" | "Global";
  type: string;
  globalDefault: boolean;
}

function TagPill(props: { children: React.ReactNode }) {
  return (
    <span className="inline-flex h-5 items-center rounded-full bg-[#eef2f6] px-2.5 text-[0.625rem] font-medium leading-4 text-[#667483]">
      {props.children}
    </span>
  );
}

const FeatureCatalogTable = ({
  rows,
  isLoading,
  onToggleDefault,
  onViewHistory,
  onDelete,
  onEdit,
}: {
  rows: FeatureCatalogRow[];
  isLoading?: boolean;
  onToggleDefault: (keyName: string, next: boolean) => void;
  onViewHistory: (row: FeatureCatalogRow) => void;
  onDelete: (row: FeatureCatalogRow) => void;
  onEdit: (row: FeatureCatalogRow) => void;
}) => {
  return (
    <div className="w-full overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
      <div className="grid grid-cols-[1.55fr_0.82fr_0.82fr_1fr_11.25rem] items-center bg-[#f5f8fb] px-5 py-4">
        <div className="text-[0.75rem] font-semibold leading-4 text-[#23313d]">
          Feature Name / Key
        </div>
        <div className="text-[0.75rem] font-semibold leading-4 text-[#23313d]">
          Scope
        </div>
        <div className="text-[0.75rem] font-semibold leading-4 text-[#23313d]">
          Type
        </div>
        <div className="text-[0.75rem] font-semibold leading-4 text-[#23313d]">
          Global Default
        </div>
        <div className="text-center text-[0.75rem] font-semibold leading-4 text-[#23313d]">
          Action
        </div>
      </div>

      <div className="divide-y divide-[#edf2f7]">
        {isLoading ? (
          <div className="px-5 py-8 text-center text-sm text-[#667483]">
            Loading features...
          </div>
        ) : null}
        {!isLoading && rows.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-[#667483]">
            No features found.
          </div>
        ) : null}
        {rows.map(function (row) {
          return (
            <div
              key={row.keyName}
              className="grid grid-cols-[1.55fr_0.82fr_0.82fr_1fr_11.25rem] items-center px-5 py-3.5"
            >
              <div className="min-w-0 pr-2">
                <div
                  className="truncate text-[0.875rem] font-medium leading-5 text-[#1f2d38]"
                  title={row.name}
                >
                  {row.name}
                </div>
                <div className="mt-1 text-[0.6875rem] font-normal leading-4 text-[#8a96a3]">
                  {row.keyName}
                </div>
              </div>

              <div>
                <TagPill>{row.scope}</TagPill>
              </div>

              <div>
                <TagPill>{row.type}</TagPill>
              </div>

              <div className="flex items-center">
                <Switch
                  checked={row.globalDefault}
                  onCheckedChange={function (next) {
                    onToggleDefault(row.keyName, next);
                  }}
                  className="h-5 w-9"
                  onClassName="bg-[#5f87a4]"
                  offClassName="bg-[#dce5ee]"
                />
              </div>

              <div className="relative flex items-center justify-center">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <button
                      type="button"
                      className="grid h-8 w-8 place-items-center rounded-full text-[#1f2d38] transition-colors hover:bg-[#f4f7fa]"
                      aria-label={"More actions for " + row.name}
                    >
                      <MenuDotsIcon size={16} aria-hidden="true" />
                    </button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent
                    align="end"
                    sideOffset={10}
                    className="w-36 rounded-[0.875rem] border border-[#e3ebf3] bg-white p-2 shadow-[0_12px_28px_rgba(15,23,42,0.08)]"
                  >
                    <DropdownMenuItem
                      className="cursor-pointer rounded-lg px-3 py-2 text-[0.8125rem] font-medium text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                      onSelect={function () {
                        onEdit(row);
                      }}
                    >
                      <Pencil size={14} aria-hidden="true" className="text-[#667483]" />
                      Edit History
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="cursor-pointer rounded-lg px-3 py-2 text-[0.8125rem] font-medium text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                      onSelect={function () {
                        onViewHistory(row);
                      }}
                    >
                      <Eye size={14} aria-hidden="true" className="text-[#667483]" />
                      View History
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="cursor-pointer rounded-lg px-3 py-2 text-[0.8125rem] font-medium text-[#ef4444] focus:bg-[#fff1f1] focus:text-[#ef4444]"
                      onClick={function () {
                        onDelete(row);
                      }}
                    >
                      <TrashIcon size={14} aria-hidden="true" className="text-[#ef4444]" />
                      Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default FeatureCatalogTable;
