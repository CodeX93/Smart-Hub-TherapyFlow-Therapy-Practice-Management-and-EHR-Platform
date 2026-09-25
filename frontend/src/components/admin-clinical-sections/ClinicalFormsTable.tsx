import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { ArrowDown, Pencil } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import EmptyFormsState from "@/components/admin/clients/ClientProfile/FormsAndDocs/EmptyFormsState";

interface FormEntry {
  id: string;
  name: string;
  category: string;
  signature: "Required" | "Optional";
  status: "Active" | "Inactive";
}

interface ClinicalFormsTableProps {
  forms: FormEntry[];
  isLoading?: boolean;
  updatingStatusId?: string | null;
  onEdit?: (id: string) => void;
  onDelete?: (id: string) => void;
  onToggleStatus?: (id: string) => void;
  onSort: (key: string) => void;
  sortConfig: { key: string; direction: "asc" | "desc" };
  onConsent?: (id: string) => void;
  readOnly?: boolean;
}

const ClinicalFormsTable = ({
  forms,
  isLoading = false,
  updatingStatusId = null,
  onEdit,
  onDelete,
  onToggleStatus,
  onSort,
  sortConfig,
  onConsent,
  readOnly = false,
}: ClinicalFormsTableProps) => {
  return (
    <div className="w-full overflow-x-auto rounded-xl border border-(--neutral-100) bg-white">
      <table className="w-full min-w-[47.5rem] border-collapse">
        <thead>
          <tr className="sticky top-0 z-10 border-b border-(--neutral-100) bg-(--bg-primary-50)">
            <th className="whitespace-nowrap py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              <div
                className="flex items-center gap-1 cursor-pointer select-none"
                onClick={() => onSort("name")}
              >
                Form Name
                <ArrowDown
                  size={16}
                  className={`transition-transform ${
                    sortConfig.key === "name" && sortConfig.direction === "desc"
                      ? "rotate-180"
                      : ""
                  }`}
                />
              </div>
            </th>
            <th className="whitespace-nowrap py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              Category
            </th>
            <th className="whitespace-nowrap py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              <div
                className="flex items-center gap-1 cursor-pointer select-none"
                onClick={() => onSort("signature")}
              >
                Signature
                <ArrowDown
                  size={16}
                  className={`transition-transform ${
                    sortConfig.key === "signature" &&
                    sortConfig.direction === "desc"
                      ? "rotate-180"
                      : ""
                  }`}
                />
              </div>
            </th>
            <th className="whitespace-nowrap py-4 px-6 text-left text-sm font-semibold text-(--text-primary-dark)">
              <div
                className="flex items-center gap-1 cursor-pointer select-none"
                onClick={() => onSort("status")}
              >
                Status
                <ArrowDown
                  size={16}
                  className={`transition-transform ${
                    sortConfig.key === "status" &&
                    sortConfig.direction === "desc"
                      ? "rotate-180"
                      : ""
                  }`}
                />
              </div>
            </th>
            <th className="whitespace-nowrap py-4 px-6 text-right text-sm font-semibold text-(--text-primary-dark)">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {isLoading ? (
            <tr>
              <td
                colSpan={5}
                className="py-8 px-6"
              >
                <ContentLoader size="md" />
              </td>
            </tr>
          ) : forms.length === 0 ? (
            <tr>
              <td colSpan={5} className="px-3 py-8">
                <EmptyFormsState
                  title="No forms found"
                  description="Clinical form templates will appear here"
                />
              </td>
            </tr>
          ) : forms.map((form) => (
            <tr
              key={form.id}
              className="border-b last:border-0 border-(--neutral-100) hover:bg-gray-50/50 transition-colors"
            >
              <td
                className={cn(
                  "whitespace-nowrap py-4 px-6 text-sm font-medium text-(--text-primary-dark)",
                  !readOnly && "cursor-pointer",
                )}
                onClick={readOnly ? undefined : () => onConsent?.(form.id)}
              >
                <span
                  className="block truncate max-w-[20rem]"
                  title={form.name}
                >
                  {form.name}
                </span>
              </td>
              <td className="whitespace-nowrap py-4 px-6">
                <Badge className="bg-(--light-blue) text-(--status-billed) border-none px-3 py-1 text-xs font-medium">
                  {form.category}
                </Badge>
              </td>
              <td className="whitespace-nowrap py-4 px-6 text-sm font-medium text-(--text-primary-dark)">
                {form.signature}
              </td>
              <td className="whitespace-nowrap py-4 px-6 min-w-39">
                <div className="flex items-center gap-2">
                  {updatingStatusId === form.id ? (
                    <ContentLoader variant="inline" size="sm" />
                  ) : (
                    <Switch
                      checked={form.status === "Active"}
                      disabled={readOnly}
                      onCheckedChange={() => onToggleStatus?.(form.id)}
                      className="data-[state=checked]:bg-(--bg-primary-dark)"
                    />
                  )}
                  <span className="text-sm font-medium text-(--text-primary-dark)">
                    {form.status}
                  </span>
                </div>
              </td>
              <td className="whitespace-nowrap py-4 px-6">
                <div className="flex items-center justify-end gap-5 text-(--text-primary-dark)">
                  <button
                    type="button"
                    disabled={readOnly}
                    onClick={() => onEdit?.(form.id)}
                    className={cn(readOnly && "cursor-not-allowed opacity-40")}
                  >
                    <Pencil size={18} />
                  </button>
                  <button
                    type="button"
                    disabled={readOnly}
                    onClick={() => onDelete?.(form.id)}
                    className={cn(readOnly && "cursor-not-allowed opacity-40")}
                  >
                    <TrashIcon size={18} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default ClinicalFormsTable;
