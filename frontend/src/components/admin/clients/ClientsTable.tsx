import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { Eye } from "lucide-react";
import { Button } from "../../ui/button";
import DocumentTableComponent from "../../shared/DocumentTable";
import ActionsDropdown from "../../shared/ActionsDropdown";
import { createClientActions } from "../../shared/actionsHelpers";
import { isClientInactive } from "@/utils/clientStatus";
import type { DocumentTableColumn } from "../../../types/document.type";
import type { Client, ClientsPageProps } from "../../../types/client.type";

const cellPadding = "0.75rem";

const ClientsTable = ({
  clients,
  onViewClient,
  onAction,
  viewMode = "closed",
  selectedClientId = null,
  staffMode = false,
  hideEdit = false,
  hideDelete,
  hideScheduledSession,
  hideCreateTask,
}: Omit<ClientsPageProps, "itemsPerPage"> & {
  onViewClient?: (clientId: string) => void;
  onAction?: (clientId: string, action: string) => void;
  viewMode?: "closed" | "half" | "full";
  selectedClientId?: string | null;
  staffMode?: boolean;
  hideEdit?: boolean;
  hideDelete?: boolean;
  hideScheduledSession?: boolean;
  hideCreateTask?: boolean;
}) => {
  const allColumns: DocumentTableColumn<Client>[] = [
    {
      key: "name",
      label: "Client Name",
      width: viewMode === "half" ? "70.4%" : "20.2%",
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
            className="block truncate text-sm font-medium leading-[1.375rem] text-(--text-primary-dark)"
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
      key: "therapist",
      label: "Therapist",
      width: "13.2%",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (value) => (
        <span
          className="block truncate text-sm font-normal text-(--text-primary-dark)"
          title={String(value ?? "")}
        >
          {value}
        </span>
      ),
    },
    {
      key: "lastSession",
      label: "Last Session",
      width: "14.1%",
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
      width: "15.8%",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (value) => (
        <span className="whitespace-nowrap text-sm font-normal text-(--text-primary-dark)">
          {value}
        </span>
      ),
    },
    {
      key: "attachedDocs",
      label: "Attached Docs",
      width: "13.2%",
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
      width: "12.3%",
      align: "center",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (value) => (
        <span className="text-sm font-normal text-(--text-primary-dark)">{value}</span>
      ),
    },
    {
      key: "actions",
      label: "Actions",
      width: viewMode === "half" ? "29.6%" : "11.2%",
      align: "center",
      padding: cellPadding,
      headerPadding: cellPadding,
      render: (_, client) => {
        const actions = createClientActions(
          () => onAction?.(client.id, "checklist"),
          () => onAction?.(client.id, "scheduled-session"),
          () => onAction?.(client.id, "edit"),
          () => onAction?.(client.id, "create-task"),
          () => onAction?.(client.id, "delete"),
          {
            disableCreateTask: isClientInactive(client),
            disableEdit: isClientInactive(client),
            hideScheduledSession: hideScheduledSession ?? staffMode,
            hideCreateTask: hideCreateTask ?? staffMode,
            hideDelete: hideDelete ?? staffMode,
            hideEdit,
          },
        );

        return (
          <div className="flex items-center justify-center gap-0.5">
            {viewMode !== "half" && (
              <Button
                variant="ghost"
                size="icon"
                className="h-9 w-9 cursor-pointer rounded-xl p-0 hover:bg-gray-50"
                onClick={() => onViewClient?.(client.id)}
                aria-label={`View ${client.name}`}
              >
                <Eye className="size-5 text-(--text-neutral-600)" />
              </Button>
            )}
            <ActionsDropdown
              actions={actions}
              triggerClassName="!rounded-lg border-transparent !bg-transparent shadow-none [&_svg]:!size-5 [&_svg]:!text-(--text-primary-dark)"
              triggerIcon={
                <MenuDotsIcon size={20} color="#1B1C20" />
              }
            />
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
      rowHeight="4.25rem"
      headerHeight="2.875rem"
      headerCellStyle={{
        fontSize: "0.875rem",
        lineHeight: "1.375rem",
      }}
      stickyHeader
      getRowStyle={(client) =>
        viewMode === "half" && selectedClientId === client.id
          ? { backgroundColor: "var(--neutral-100)" }
          : {}
      }
    />
  );
};

export default ClientsTable;
