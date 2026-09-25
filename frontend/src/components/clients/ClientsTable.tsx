import { Eye } from "lucide-react";
import { Button } from "../ui/button";
import DocumentTableComponent from "../shared/DocumentTable";
import ActionsDropdown from "../shared/ActionsDropdown";
import { createClientActions } from "../shared/actionsHelpers";
import { isClientInactive } from "@/utils/clientStatus";
import type { DocumentTableColumn } from "../../types/document.type";
import type { Client, ClientsPageProps } from "../../types/client.type";

const cellPadding = "0.75rem 1rem";

const ClientsTable = ({
  clients,
  onViewClient,
  onAction,
  viewMode = "closed",
  selectedClientId = null,
}: Omit<ClientsPageProps, "itemsPerPage"> & {
  onViewClient?: (clientId: string) => void;
  onAction?: (clientId: string, action: string) => void;
  viewMode?: "closed" | "half" | "full";
  selectedClientId?: string | null;
}) => {
  const allColumns: DocumentTableColumn<Client>[] = [
    {
      key: "name",
      label: "Client Name",
      width: viewMode === "half" ? "68%" : "28%",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (_, client) => (
        <div
          className={`flex min-w-0 flex-col gap-0.5 ${
            viewMode === "half" ? "cursor-pointer" : ""
          }`}
          onClick={() => viewMode === "half" && onViewClient?.(client.id)}
        >
          <span
            className="block truncate text-xs font-semibold text-(--text-primary-dark) sm:text-sm"
            title={client.name}
          >
            {client.name}
          </span>
          <span
            className="block truncate text-[0.625rem] text-(--text-neutral-600) sm:text-xs"
            title={client.referenceNumber}
          >
            MRN: {client.referenceNumber}
          </span>
        </div>
      ),
    },
    {
      key: "lastSession",
      label: "Last Session",
      width: "15%",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (value) => (
        <span className="whitespace-nowrap text-xs font-normal text-(--text-primary-dark) sm:text-sm">
          {value}
        </span>
      ),
    },
    {
      key: "sinceLastSession",
      label: "Since Last Session",
      width: "18%",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (value) => (
        <span className="whitespace-nowrap text-sm font-normal text-(--text-neutral-600)">
          {value}
        </span>
      ),
    },
    {
      key: "attachedDocs",
      label: "Attached Docs",
      width: "13%",
      align: "center",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (value) => (
        <span className="text-sm font-normal text-(--text-primary-dark)">{value}</span>
      ),
    },
    {
      key: "checklist",
      label: "Checklist",
      width: "13%",
      align: "center",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (value) => (
        <span className="text-sm font-normal text-(--text-neutral-600)">{value}</span>
      ),
    },
    {
      key: "actions",
      label: "Actions",
      width: viewMode === "half" ? "32%" : "13%",
      align: "center",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (_, client) => {
        const isReadOnlyClient = isClientInactive(client);
        const actions = createClientActions(
          () => onAction?.(client.id, "checklist"),
          () => onAction?.(client.id, "scheduled-session"),
          () => onAction?.(client.id, "edit"),
          () => onAction?.(client.id, "create-task"),
          () => onAction?.(client.id, "delete"),
          {
            disableCreateTask: isReadOnlyClient,
            disableEdit: isReadOnlyClient,
          },
        );

        return (
          <div className="flex items-center justify-center gap-1.5">
            {viewMode !== "half" && (
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8 cursor-pointer rounded-xl border border-(--text-neutral-100) bg-white p-0 hover:bg-gray-50"
                onClick={() => onViewClient?.(client.id)}
              >
                <Eye className="size-4 text-(--text-neutral-600)" />
              </Button>
            )}
            <ActionsDropdown actions={actions} />
          </div>
        );
      },
    },
  ];

  const columns =
    viewMode === "half"
      ? allColumns.filter((col) => ["name", "actions"].includes(col.key))
      : allColumns;

  return (
    <DocumentTableComponent
      columns={columns}
      data={clients}
      rowKey={(client: Client) => client.id}
      rowHeight="3.5rem"
      headerHeight="2.5rem"
      stickyHeader
      getRowStyle={(client) =>
        viewMode === "half" && selectedClientId === client.id
          ? { backgroundColor: "var(--neutral-50)" }
          : {}
      }
    />
  );
};

export default ClientsTable;
