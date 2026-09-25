
import { ContentLoader } from "@/components/shared/ContentLoader";
import CustomInput from "@/components/form/CustomInput";
import { useMemo, useState } from "react";
import { RefreshCcw, Search } from "lucide-react";
import CustomSelect from "@/components/form/CustomSelect";
import { Button } from "@/components/ui/button";
import {
  consentStatuses,
  consentTypes,
  type ConsentRecord,
} from "../compliance.static";
import ConsentTable from "@/components/admin-privacy-sections/ConsentTable";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import {
  type AdminConsentManagementParams,
  type AdminConsentManagementRow,
  useGetAdminConsentManagementQuery,
  useLazyGetAdminConsentManagementRefreshQuery,
} from "@/store/api/admin/consents.api";
import { getApiErrorMessage } from "@/utils/apiError";

const PrivacyPolicy = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("ALL");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  const queryArgs = useMemo<AdminConsentManagementParams>(
    () => ({
      consentType: typeFilter === "ALL" ? undefined : (typeFilter as AdminConsentManagementParams["consentType"]),
      status: statusFilter === "ALL" ? undefined : (statusFilter as AdminConsentManagementParams["status"]),
      search: searchQuery.trim() || undefined,
    }),
    [searchQuery, statusFilter, typeFilter],
  );

  const {
    data: consentRows = [],
    error,
    isLoading,
    isFetching,
    refetch,
  } = useGetAdminConsentManagementQuery(queryArgs);
  const [refreshConsentGrid, { isFetching: isRefreshing }] =
    useLazyGetAdminConsentManagementRefreshQuery();

  const tableData = useMemo<ConsentRecord[]>(
    () => consentRows.map((row, index) => mapConsentRowToRecord(row, index)),
    [consentRows],
  );

  const errorMessage = error ? getApiErrorMessage(error) : null;

  const handleRefresh = async () => {
    try {
      await refreshConsentGrid(queryArgs, true).unwrap();
      await refetch();
    } catch {
      // surface through current query state/message only
    }
  };

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden">
      <ScrollToTopButton />
      <div className="flex shrink-0 w-full flex-col gap-3 lg:flex-row lg:items-center">
        <div className="flex w-full min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap lg:flex-nowrap lg:items-center">
          <div className="w-full min-w-0 sm:flex-1">
            <CustomInput
              placeholder="Search by client ID, name or email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="rounded-full min-h-10 pb-0 w-full pt-1.75 shadow-xs"
            />
          </div>
          <div className="w-full sm:w-52 shrink-0">
            <CustomSelect
              options={consentTypes}
              value={typeFilter}
              onChange={(value) => setTypeFilter(value as string)}
              className="rounded-full max-h-10 pb-0 pt-0 bg-white shadow-xs"
              isSearch={false}
              contentClassName="min-w-fit"
            />
          </div>
          <div className="w-full sm:w-56 shrink-0">
            <CustomSelect
              options={consentStatuses}
              value={statusFilter}
              onChange={(value) => setStatusFilter(value as string)}
              className="rounded-full max-h-10 pb-0 pt-0 bg-white shadow-xs"
              isSearch={false}
              contentClassName="min-w-fit"
            />
          </div>
        </div>
        <Button
          variant="outline"
          className="h-10 w-fit shrink-0 px-8 rounded-full border-(--neutral-200) text-(--text-primary-dark) hover:border-(--neutral-600) transition-colors bg-transparent font-semibold cursor-pointer shadow-xs lg:ml-auto"
          onClick={handleRefresh}
          disabled={isRefreshing}
          loading={isRefreshing}
          loadingLabel="Refreshing..."
        >
          <RefreshCcw size={18} />
          Refresh
        </Button>
      </div>
      {errorMessage ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 mt-4 shrink-0">
          {errorMessage}
        </div>
      ) : null}
      {isLoading || isFetching ? (
        <ContentLoader size="md" className="shrink-0 py-10 text-(--text-neutral-600) mr-2" />
      ) : (
        <div className="mt-4 flex min-h-0 flex-1 flex-col overflow-hidden">
          <ConsentTable data={tableData} />
        </div>
      )}
    </div>
  );
};

export default PrivacyPolicy;

function mapConsentRowToRecord(
  row: AdminConsentManagementRow,
  index: number,
): ConsentRecord {
  return {
    id: `${row.clientId || index}`,
    clientId: row.clientId || "---",
    fullName: row.fullName || "Unknown Client",
    email: row.email || "",
    portalAccess: row.portalAccess ? "Enabled" : "Disabled",
    aiProcessing: normalizeConsentDisplay(row.aiProcessing),
    dataSharing: normalizeConsentDisplay(row.dataSharing),
    research: normalizeConsentDisplay(row.research),
    marketing: normalizeConsentDisplay(row.marketing),
  };
}

function normalizeConsentDisplay(value: string): "Not Set" | "Granted" | "Denied" {
  const normalized = value.trim().toLowerCase();
  if (normalized === "granted") return "Granted";
  if (normalized === "denied") return "Denied";
  return "Not Set";
}
