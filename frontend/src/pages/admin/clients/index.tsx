import { useDebouncedSearch } from "@/hooks/useDebouncedSearch";

import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useMemo, useEffect, useCallback } from "react";
import { Plus } from "lucide-react";
import { Filter as SolarFilter } from "@solar-icons/react-perf/category/ui/Linear/Filter";
import { Magnifer } from "@solar-icons/react-perf/category/search/Linear/Magnifer";
import { Button } from "@/components/ui/button.tsx";
import ClientsTable from "../../../components/admin/clients/ClientsTable";
import ClientsFilterSidebar from "../../../components/admin/clients/ClientsFilterSidebar";
import AddNewClientModal from "../../../components/admin/clients/AddNewClientModal";
import ClientProfile from "../../../components/admin/clients/ClientProfile";
import DeleteClientModal from "../../../components/admin/clients/ClientProfile/DeleteClientModal";
import { useSidebar } from "@/contexts/sidebar";
import { cn } from "@/lib/utils.ts";
import type { ClientFilters, Client } from "@/types/client.type.ts";
import type { AddClientFormValues } from "@/types/add-client.type.ts";
import CustomInput from "@/components/form/CustomInput";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import { useLocation, useNavigate } from "react-router-dom";
import {
  useCreateAdminClientMutation,
  useCloseAdminClientFileMutation,
  useActivateAdminClientFileMutation,
  useDeleteAdminClientMutation,
  useGetAdminClientByIdQuery,
  useGetAdminClientsQuery,
  useSendClientPortalActivationEmailMutation,
  useUpdateAdminClientPortalAccessMutation,
  useUpdateAdminClientMutation,
  type AdminClientSummary,
  type AdminClientListSummary,
} from "@/store/api/admin/clients.api";
import { mapAddClientFormToPartialClientBody } from "@/store/api/admin/createClientPayload";
import { getApiErrorMessage } from "@/utils/apiError";
import { mapApiClientToFormValues, mapApiClientToResolvedFormValues } from "@/utils/mapApiClientToFormValues";
import { areClientFiltersEqual } from "@/utils/clientFilters";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import {
  buildClientFilterChips,
  removeClientFilterChip,
} from "@/utils/appliedFilterChips";
import { useClientFilterOptions } from "@/hooks/useSystemOptionCatalog";
import { useGetAuthMeQuery } from "@/store/api/authApi";
import { canReopenClientFile } from "@/utils/clientStatus";
import { useGetChecklistTemplatesQuery } from "@/store/api/admin/checklists.api";
import { useGetReportTemplatesQuery } from "@/store/api/admin/reportTemplates.api";

const CLIENT_PROFILE_TABS = new Set([
  "Overview",
  "Sessions",
  "Assessments",
  "Reports",
  "Forms & Docs",
  "Billing",
  "Tasks",
  "Checklists",
  "History",
]);

type ClientsLocationState = {
  clientId?: string | number;
  initialTab?: string;
};

function formatClientsFetchError(error: unknown): string {
  const message = getApiErrorMessage(error);
  if (
    message !== "Something went wrong. Please try again." &&
    message !== "An unexpected error occurred. Please try again."
  ) {
    return message;
  }
  return "Could not load clients. Please refresh and try again.";
}

function formatClientDate(date?: string): string {
  if (!date) return "---";
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return "---";
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  }).format(parsed);
}

function formatRelativeDays(date?: string): string {
  if (!date) return "---";
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return "---";

  // Calendar-day difference in local time (not floor of 24h chunks — that treated
  // "yesterday afternoon" as Today when less than 24h has passed).
  const now = new Date();
  const sessionDay = new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate()).getTime();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const diffDays = Math.round((today - sessionDay) / (1000 * 60 * 60 * 24));

  // Future must not appear as "last session" relative — show empty
  if (diffDays < 0) return "---";
  if (diffDays === 0) return "Today";
  if (diffDays === 1) return "1 day ago";
  return `${diffDays} days ago`;
}

function getAge(dateOfBirth?: string): number | undefined {
  if (!dateOfBirth) return undefined;
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return undefined;

  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const monthDiff = now.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < dob.getDate())) {
    age -= 1;
  }
  return age > 0 ? age : undefined;
}

function buildAddress(client: AdminClientSummary): string | undefined {
  const parts = [
    client.streetAddress1,
    client.streetAddress2,
    client.city,
    client.province,
    client.postalCode,
    client.country,
  ].filter((part): part is string => Boolean(part && part.trim()));

  return parts.length > 0 ? parts.join(", ") : undefined;
}

function mapApiClientToUiClient(client: AdminClientSummary): Client {
  return {
    id: String(client.id),
    name: client.fullName || "Unnamed Client",
    referenceNumber: client.clientId || client.referenceNumber || `#${client.id}`,
    therapist: client.assignedTherapistName || "Unassigned",
    assignedTherapistId: client.assignedTherapistId,
    lastSession: formatClientDate(client.lastSessionDate),
    sinceLastSession: formatRelativeDays(client.lastSessionDate),
    lastSessionDate: client.lastSessionDate,
    attachedDocs: client.documentCount ?? 0,
    checklist: String(client.checklistCount ?? 0),
    clientId: client.clientId || undefined,
    age: getAge(client.dateOfBirth),
    serviceType: client.serviceType,
    isMVAClient: client.clientType === "MVA",
    insurance: client.insuranceProvider,
    insuranceProvider: client.insuranceProvider,
    policyNumber: client.policyNumber,
    groupNumber: client.groupNumber,
    insurancePhone: client.insurancePhone,
    copayAmount: client.copayAmount,
    deductible: client.deductible,
    dateOfBirth: client.dateOfBirth ? formatClientDate(client.dateOfBirth) : undefined,
    gender: client.gender,
    maritalStatus: client.maritalStatus,
    preferredLanguage: client.preferredLanguage,
    phone: client.phone,
    email: client.email,
    portalEmail: client.portalEmail,
    address: buildAddress(client),
    emergencyContact:
      client.emergencyContactName || client.emergencyContactPhone
        ? {
            name: client.emergencyContactName || "---",
            phone: client.emergencyContactPhone || "---",
          }
        : undefined,
    employmentStatus: client.employmentStatus,
    educationLevel: client.educationLevel,
    numberOfDependents: client.numberOfDependents,
    needsFollowUp: client.needsFollowUp,
    priority: client.priority,
    followUpDate: client.followUpDate,
    followUpNotes: client.followUpNotes,
    clientStatus: client.status,
    clientStage: client.stage,
    clientType: client.clientType,
    serviceFrequency: client.serviceFrequency,
    treatmentModality: client.treatmentModality,
    insuranceType: client.insuranceType,
    referrerName: client.referrerName,
    referralNumber: client.referenceNumber,
    startDate: client.startDate,
    referralDate: client.referralDate ? formatClientDate(client.referralDate) : undefined,
    referralSource: client.clientSource,
    portalAccessEnabled: Boolean(client.hasPortalAccess),
    lastLogin: client.lastLogin ?? null,
    notes: client.notes,
  };
}

/**
 * Maps the slim GET /clients list payload to the UI Client shape. Only populates the
 * columns ClientsTable renders (name, reference number, therapist, last session, docs,
 * checklist, status/stage) since the backend intentionally omits insurance/address/notes
 * demographics from the list response to avoid per-row N+1 lookups. Full details are
 * hydrated separately via getAdminClientById when a client profile is opened.
 */
function mapApiClientSummaryToUiClient(client: AdminClientListSummary): Client {
  return {
    id: String(client.id),
    name: client.fullName || "Unnamed Client",
    referenceNumber: client.clientId || client.referenceNumber || `#${client.id}`,
    therapist: client.assignedTherapistName || "Unassigned",
    assignedTherapistId: client.assignedTherapistId,
    lastSession: formatClientDate(client.lastSessionDate),
    sinceLastSession: formatRelativeDays(client.lastSessionDate),
    lastSessionDate: client.lastSessionDate,
    attachedDocs: client.documentCount ?? 0,
    checklist: String(client.checklistCount ?? 0),
    clientId: client.clientId || undefined,
    clientStatus: client.status,
    clientStage: client.stage,
  };
}

const Clients = () => {
  const itemsPerPage = 25;
  const { setIsCollapsed } = useSidebar();
  const initialFilters = useMemo<ClientFilters>(
    () => ({
      clientStatus: [],
      clientStage: [],
      clientType: [],
      assignedTherapist: [],
      hasPortalAccess: false,
      hasPendingTasks: false,
      hasNoSessions: false,
      needsFollowUp: false,
      unassigned: false,
      checklistTemplate: [],
      reportTemplate: [],
    }),
    [],
  );

  const [searchQuery, setSearchQuery] = useState("");
  const { search: debouncedSearch, isPending: isSearchPending } = useDebouncedSearch(searchQuery);
  const [page, setPage] = useState(1);
  const [allClients, setAllClients] = useState<Client[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);

  const [filters, setFilters] = useState<ClientFilters>(initialFilters);
  const [localFilters, setLocalFilters] = useState<ClientFilters>(initialFilters);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [isAddClientModalOpen, setIsAddClientModalOpen] = useState(false);
  const [isEditClientModalOpen, setIsEditClientModalOpen] = useState(false);
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null);
  const [editingClientId, setEditingClientId] = useState<number | null>(null);
  const [isClientProfileOpen, setIsClientProfileOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [viewMode, setViewMode] = useState<"closed" | "half" | "full">(
    "closed",
  );
  const [requestedProfileAction, setRequestedProfileAction] = useState<
    "checklist" | "scheduled-session" | "create-task" | null
  >(null);
  const [requestedProfileActionNonce, setRequestedProfileActionNonce] = useState(0);
  const [profileNavTab, setProfileNavTab] = useState<string | null>(null);

  const location = useLocation();
  const navigate = useNavigate();
  const locationState = (location.state as ClientsLocationState | null) ?? null;
  const stateClientId = locationState?.clientId;

  const [createAdminClient, { isLoading: isCreatingClient }] =
    useCreateAdminClientMutation();
  const [deleteAdminClient, { isLoading: isDeletingClient }] =
    useDeleteAdminClientMutation();
  const [updateAdminClientPortalAccess, { isLoading: isUpdatingPortalAccess }] =
    useUpdateAdminClientPortalAccessMutation();
  const [closeAdminClientFile, { isLoading: isClosingClientFile }] =
    useCloseAdminClientFileMutation();
  const [activateAdminClientFile, { isLoading: isActivatingClientFile }] =
    useActivateAdminClientFileMutation();
  const [sendClientPortalActivationEmail, { isLoading: isSendingPortalActivation }] =
    useSendClientPortalActivationEmailMutation();
  const [updateAdminClient, { isLoading: isUpdatingClient }] =
    useUpdateAdminClientMutation();
  const { data: authMe } = useGetAuthMeQuery();
  const { labelMaps, optionsByCategory } = useClientFilterOptions(true);
  const { data: checklistTemplatesResponse } = useGetChecklistTemplatesQuery({
    page: 1,
    pageSize: 200,
  });
  const { data: reportTemplates } = useGetReportTemplatesQuery();
  const clientFilterLabelMaps = useMemo(
    () => ({
      ...labelMaps,
      checklistTemplate: new Map(
        (checklistTemplatesResponse?.items ?? []).map((item) => [
          String(item.id),
          item.name,
        ]),
      ),
      reportTemplate: new Map(
        (reportTemplates ?? []).map((item) => [String(item.id), item.name]),
      ),
    }),
    [labelMaps, checklistTemplatesResponse?.items, reportTemplates],
  );
  const canReopenFile = canReopenClientFile(authMe);

  const appliedFilters = useMemo(
    () => ({
      search: debouncedSearch || undefined,
      status:
        filters.clientStatus && filters.clientStatus.length > 0
          ? filters.clientStatus[0]
          : undefined,
      stage:
        filters.clientStage && filters.clientStage.length > 0
          ? filters.clientStage[0]
          : undefined,
      therapistId:
        filters.assignedTherapist && filters.assignedTherapist.length > 0
          ? Number.parseInt(filters.assignedTherapist[0], 10) || undefined
          : undefined,
      clientType:
        filters.clientType && filters.clientType.length > 0
          ? filters.clientType[0]
          : undefined,
      hasPortalAccess: filters.hasPortalAccess ? true : undefined,
      hasPendingTasks: filters.hasPendingTasks ? true : undefined,
      hasNoSessions:
        (filters.hasNoSessions || filters.noSessions) ? true : undefined,
      needsFollowUp: filters.needsFollowUp ? true : undefined,
      unassigned: filters.unassigned ? true : undefined,
      checklistTemplateId:
        filters.checklistTemplate && filters.checklistTemplate.length > 0
          ? Number.parseInt(filters.checklistTemplate[0], 10) || undefined
          : undefined,
      reportTemplateId:
        filters.reportTemplate && filters.reportTemplate.length > 0
          ? Number.parseInt(filters.reportTemplate[0], 10) || undefined
          : undefined,
    }),
    [debouncedSearch, filters],
  );

  const {
    currentData: clientsResponse,
    isLoading: isLoadingClients,
    isFetching: isFetchingClients,
    isError: isClientsError,
    error: clientsError,
  } = useGetAdminClientsQuery({
    page,
    pageSize: itemsPerPage,
    sortBy: "createdAt",
    sortOrder: "desc",
    ...appliedFilters,
  }, {
    skip: isSearchPending,
    refetchOnMountOrArgChange: true,
  });

  const {
    data: selectedClientResponse,
    isFetching: isFetchingSelectedClient,
  } = useGetAdminClientByIdQuery(selectedClientId ?? 0, {
    skip: selectedClientId === null,
    refetchOnMountOrArgChange: true,
  });

  const {
    data: editingClientResponse,
    isLoading: isLoadingEditingClient,
  } = useGetAdminClientByIdQuery(editingClientId ?? 0, {
    skip: editingClientId === null,
    refetchOnMountOrArgChange: true,
  });

  const editClientInitialValues = useMemo(
    () =>
      editingClientResponse
        ? mapApiClientToFormValues(editingClientResponse)
        : undefined,
    [editingClientResponse],
  );

  const therapistOptions = useMemo<CustomSelectOption[]>(() => {
    const therapistMap = new Map<string, string>();

    allClients.forEach((client) => {
      if (client.assignedTherapistId && client.therapist !== "Unassigned") {
        therapistMap.set(String(client.assignedTherapistId), client.therapist);
      }
    });

    return Array.from(therapistMap.entries())
      .map(([value, label]) => ({ value, label }))
      .sort((left, right) => left.label.localeCompare(right.label));
  }, [allClients]);

  useEffect(() => {
    if (!clientsResponse || isSearchPending || isFetchingClients) return;

    const mappedClients = clientsResponse.items.map(mapApiClientSummaryToUiClient);

    setTotalPages(clientsResponse.totalPages);
    setTotalCount(clientsResponse.totalCount);
    setAllClients((previousClients) => {
      if (page === 1) return mappedClients;

      const merged = new Map(previousClients.map((client) => [client.id, client]));
      mappedClients.forEach((client) => {
        merged.set(client.id, client);
      });
      return Array.from(merged.values());
    });
  }, [clientsResponse, page, isSearchPending, isFetchingClients]);

  useEffect(() => {
    if (!selectedClientResponse) return;
    setSelectedClient((previous) => {
      const mapped = mapApiClientToUiClient(selectedClientResponse);
      if (previous?.id === mapped.id) return { ...previous, ...mapped };
      return mapped;
    });
  }, [selectedClientResponse]);

  // Auto-open profile when clientId is present (e.g. from Billings → client name)
  useEffect(() => {
    if (!stateClientId) return;

    const targetId = String(stateClientId);
    const parsedId = Number.parseInt(targetId, 10);
    if (!Number.isFinite(parsedId) || parsedId <= 0) return;

    const tab = locationState?.initialTab;
    const resolvedTab =
      typeof tab === "string" && CLIENT_PROFILE_TABS.has(tab) ? tab : null;

    // Clear nav state first so list updates do not re-trigger this open.
    navigate(location.pathname, { replace: true, state: {} });

    setSelectedClient((previous) => {
      if (previous?.id === targetId) return previous;
      return (
        allClients.find(
          (c) => c.id === targetId || String(c.clientId) === targetId,
        ) ?? null
      );
    });
    setSelectedClientId(parsedId);
    setRequestedProfileAction(null);
    setRequestedProfileActionNonce((prev) => prev + 1);
    setProfileNavTab(resolvedTab);
    setIsClientProfileOpen(true);
    setViewMode("half");
    // eslint-disable-next-line react-hooks/exhaustive-deps -- open once per inbound clientId nav state
  }, [stateClientId]);

  const hasMore = !isSearchPending && page < totalPages;

  const handleLoadMore = useCallback(() => {
    if (hasMore && !isFetchingClients) {
      setPage((prev) => prev + 1);
    }
  }, [hasMore, isFetchingClients, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore,
    isLoading: isFetchingClients,
  });

  const currentClients = allClients;

  const handleViewClient = (clientId: string) => {
    const client = allClients.find((c) => c.id === clientId);
    if (client) {
      setSelectedClient(client);
      setSelectedClientId(Number.parseInt(client.id, 10) || null);
      setRequestedProfileAction(null);
      setRequestedProfileActionNonce(0);
      setProfileNavTab(null);
      setIsClientProfileOpen(true);
      setViewMode("half");
    }
  };

  const handleClientAction = (clientId: string, action: string) => {
    const client = allClients.find((c) => c.id === clientId);
    if (!client) return;

    if (action === "edit") {
      handleEditClient(clientId);
      return;
    }

    if (action === "delete") {
      handleDeleteClient(clientId);
      return;
    }

    if (action === "checklist" || action === "scheduled-session" || action === "create-task") {
      setSelectedClient(client);
      setSelectedClientId(Number.parseInt(client.id, 10) || null);
      setProfileNavTab(null);
      setIsClientProfileOpen(true);
      setViewMode("half");
      setRequestedProfileAction(action);
      setRequestedProfileActionNonce((prev) => prev + 1);
    }
  };

  const profileInitialTab = useMemo(() => {
    switch (requestedProfileAction) {
      case "checklist":
        return "Checklists";
      case "scheduled-session":
        return "Sessions";
      case "create-task":
        return "Tasks";
      default:
        // null = preserve whatever tab is already open when switching clients
        return profileNavTab;
    }
  }, [requestedProfileAction, profileNavTab]);

  const handleEditClient = (clientId: string) => {
    const parsedId = Number.parseInt(clientId, 10);
    if (!parsedId) return;
    setRequestedProfileAction(null);
    setProfileNavTab(null);
    setEditingClientId(parsedId);
    setIsEditClientModalOpen(true);
  };

  const handleDeleteClient = (clientId: string) => {
    const client = allClients.find((c) => c.id === clientId);
    if (client) {
      setRequestedProfileAction(null);
      setSelectedClient(client);
      setSelectedClientId(Number.parseInt(client.id, 10) || null);
      setIsDeleteModalOpen(true);
    }
  };

  const handleConfirmDelete = () => {
    if (!selectedClient) return;

    void (async () => {
      try {
        await deleteAdminClient(Number.parseInt(selectedClient.id, 10)).unwrap();
        setAllClients((previousClients) =>
          previousClients.filter((client) => client.id !== selectedClient.id),
        );
        setTotalCount((previousCount) => Math.max(0, previousCount - 1));
        setIsDeleteModalOpen(false);
        setIsClientProfileOpen(false);
        setSelectedClient(null);
        setSelectedClientId(null);
        setViewMode("closed");
        setToastType("success");
        setToastMessage("Client deleted successfully.");
      } catch (error) {
        setToastType("error");
        setToastMessage(getApiErrorMessage(error));
      }
    })();
  };

  useEffect(() => {
    if (isFilterOpen) {
      setLocalFilters(filters);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isFilterOpen]);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  // Collapse sidebar when profile opens
  useEffect(() => {
    setIsCollapsed(viewMode !== "closed");
  }, [viewMode, setIsCollapsed]);

  const resetClientList = useCallback(() => {
    setPage(1);
    setAllClients([]);
    setTotalPages(0);
    setTotalCount(0);
  }, [setPage]);

  const handleApplyFilters = () => {
    if (areClientFiltersEqual(localFilters, filters)) {
      setIsFilterOpen(false);
      return;
    }

    resetClientList();
    setFilters(localFilters);
    setIsFilterOpen(false);
  };

  const handleClearAll = () => {
    const clearedFilters: ClientFilters = { ...initialFilters };
    const searchAlreadyClear = searchQuery.trim() === "";

    if (areClientFiltersEqual(filters, clearedFilters) && searchAlreadyClear) {
      setLocalFilters(clearedFilters);
      setIsFilterOpen(false);
      return;
    }

    resetClientList();
    setSearchQuery("");
    setLocalFilters(clearedFilters);
    setFilters(clearedFilters);
    setIsFilterOpen(false);
  };

  const handleClearAllFiltersOnly = () => {
    if (areClientFiltersEqual(filters, initialFilters)) return;

    resetClientList();
    setLocalFilters(initialFilters);
    setFilters(initialFilters);
  };

  const handleRemoveFilterChip = (chipId: string) => {
    resetClientList();
    const nextFilters = removeClientFilterChip(filters, chipId);
    setFilters(nextFilters);
    setLocalFilters(nextFilters);
  };

  const appliedFilterChips = useMemo(
    () => buildClientFilterChips(filters, therapistOptions, clientFilterLabelMaps),
    [filters, therapistOptions, clientFilterLabelMaps],
  );

  const handleLocalFiltersChange = (newFilters: ClientFilters) => {
    setLocalFilters(newFilters);
  };

  const handleAddClient = async (data: AddClientFormValues) => {
    try {
      await createAdminClient(data).unwrap();
      setToastType("success");
      setToastMessage("Client created successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      throw error;
    }
  };

  const handleUpdateClient = async (data: AddClientFormValues) => {
    if (!editingClientId || !editingClientResponse) return;
    try {
      const initialValues = mapApiClientToResolvedFormValues(
        editingClientResponse,
        optionsByCategory,
      );
      const partialBody = mapAddClientFormToPartialClientBody(data, initialValues);

      if (Object.keys(partialBody).length === 0) {
        setIsEditClientModalOpen(false);
        setEditingClientId(null);
        return;
      }

      const updatedClient = await updateAdminClient({
        id: editingClientId,
        body: partialBody,
      }).unwrap();

      const mappedClient = mapApiClientToUiClient(updatedClient);
      setSelectedClient((previous) =>
        previous?.id === mappedClient.id ? { ...previous, ...mappedClient } : mappedClient,
      );
      setAllClients((previousClients) =>
        previousClients.map((client) =>
          client.id === String(updatedClient.id) ? { ...client, ...mappedClient } : client,
        ),
      );
      setToastType("success");
      setToastMessage("Client updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      throw error;
    }
  };

  const handleUpdatePortalAccess = async (enable: boolean) => {
    if (!selectedClient) return;

    const email = selectedClient.portalEmail || selectedClient.email;
    if (!email) {
      setToastType("error");
      setToastMessage("Portal email is required to update portal access.");
      return;
    }

    try {
      const updatedClient = await updateAdminClientPortalAccess({
        id: Number.parseInt(selectedClient.id, 10),
        enable,
        email,
      }).unwrap();

      const mappedClient = mapApiClientToUiClient(updatedClient);
      setSelectedClient((previous) =>
        previous?.id === mappedClient.id ? { ...previous, ...mappedClient } : mappedClient,
      );
      setAllClients((previousClients) =>
        previousClients.map((client) =>
          client.id === String(updatedClient.id) ? { ...client, ...mappedClient } : client,
        ),
      );
      setToastType("success");
      setToastMessage(
        enable ? "Portal access enabled successfully." : "Portal access disabled successfully.",
      );
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleResendPortalActivation = async () => {
    if (!selectedClient) return;
    try {
      const response = await sendClientPortalActivationEmail(
        Number.parseInt(selectedClient.id, 10),
      ).unwrap();
      setToastType("success");
      setToastMessage(response?.message || "Portal activation email sent successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleCloseClientFile = async (clientId: string) => {
    const parsedId = Number.parseInt(clientId, 10);
    if (!Number.isFinite(parsedId)) return;

    try {
      const updatedClient = await closeAdminClientFile(parsedId).unwrap();
      const mappedClient = mapApiClientToUiClient(updatedClient);

      setSelectedClient(mappedClient);
      setAllClients((previousClients) =>
        previousClients.map((client) =>
          client.id === String(updatedClient.id) ? mappedClient : client,
        ),
      );

      setToastType("success");
      setToastMessage("Client file closed successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      throw error;
    }
  };

  const handleActivateClientFile = async (clientId: string) => {
    const parsedId = Number.parseInt(clientId, 10);
    if (!Number.isFinite(parsedId)) return;

    try {
      const updatedClient = await activateAdminClientFile(parsedId).unwrap();
      const mappedClient = mapApiClientToUiClient(updatedClient);

      setSelectedClient(mappedClient);
      setAllClients((previousClients) =>
        previousClients.map((client) =>
          client.id === String(updatedClient.id) ? mappedClient : client,
        ),
      );

      setToastType("success");
      setToastMessage("Client activated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      throw error;
    }
  };

  const handleToggleFullView = () => {
    setViewMode(viewMode === "full" ? "half" : "full");
  };

  const handleCloseProfile = () => {
    setIsClientProfileOpen(false);
    setSelectedClient(null);
    setSelectedClientId(null);
    setRequestedProfileAction(null);
    setRequestedProfileActionNonce(0);
    setProfileNavTab(null);
    setViewMode("closed");
  };

  return (
    <div className="relative flex h-full min-h-0 gap-4 overflow-hidden">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
          duration={2500}
        />
      ) : null}
      <ScrollToTopButton />
      {/* Client List Section - Hide when viewMode is 'full' */}
      {/* {viewMode !== "full" && (isCollapsed || viewMode === "closed") && ( */}
      {viewMode !== "full" && (
        <div
          className={cn(
            "flex h-full min-h-0 min-w-0 flex-col overflow-hidden transition-all duration-700",
            viewMode === "half" ? "w-82 shrink-0" : "flex-1",
          )}
        >
          {/* Search and Actions Bar */}
          <div className="mb-4 flex shrink-0 items-center justify-between gap-2">
            <div className="flex items-center gap-2 relative">
              <CustomInput
                title="DOB: YYYY-MM-DD or MM/DD/YYYY. Names: full name or prefixes of at least 2 letters."
                placeholder={
                  viewMode === "half"
                    ? "Search..."
                    : "Search name, email, phone, MRN or DOB..."
                }
                value={searchQuery}
                onChange={(e) => {
                  if (e.target.value.trim() !== searchQuery.trim()) resetClientList();
                  setSearchQuery(e.target.value);
                }}
                icon={
                  <Magnifer
                    size={20}
                    color="#5B616E"
                  />
                }
                className={cn(
                  "rounded-full min-h-10 pb-0 pt-1.75",
                  viewMode === "half" ? "w-auto" : "w-[24.9rem]",
                )}
              />
              <button
                onClick={() => setIsFilterOpen(true)}
                className={cn(
                  "border border-(--neutral-100) bg-white shadow-(--shadow) flex items-center gap-2 cursor-pointer hover:bg-(--neutral-50) transition-colors",
                  viewMode === "half"
                    ? "h-10 w-10 justify-center rounded-lg p-0"
                    : "h-10 px-4 rounded-full",
                )}
              >
                <span
                  className={cn(
                    "font-medium text-(--text-neutral-800)",
                    viewMode === "half" ? "text-xs" : "text-sm",
                  )}
                >
                  {viewMode === "half" ? "" : "Filters"}
                </span>
                <SolarFilter
                  size={20}
                  color="#1B1C20"
                />
              </button>
              <ClientsFilterSidebar
                isOpen={isFilterOpen}
                onClose={() => setIsFilterOpen(false)}
                filters={localFilters}
                onFiltersChange={handleLocalFiltersChange}
                onApply={handleApplyFilters}
                onClearAll={handleClearAll}
                therapistOptions={therapistOptions}
              />
            </div>
            <Button
              onClick={() => setIsAddClientModalOpen(true)}
              className={cn(
                "font-semibold bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white flex items-center gap-2 cursor-pointer ml-auto",
                viewMode === "half"
                  ? "h-10 w-10 justify-center rounded-lg p-0 text-xs"
                  : "h-10 px-4 text-sm rounded-full",
              )}
            >
              <Plus size={20} strokeWidth={1.33} />
              {viewMode === "half" ? "" : "Add Client"}
            </Button>
          </div>

          <AppliedFiltersBar
            chips={appliedFilterChips}
            onRemove={handleRemoveFilterChip}
            onClearAll={handleClearAllFiltersOnly}
            className="mb-3 shrink-0"
          />

          {/* Clients Table */}
          <div className="flex-1 min-h-0 min-w-0 overflow-auto pr-1">
            {isClientsError ? (
              <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {formatClientsFetchError(clientsError)}
              </div>
            ) : (
              <>
                <ClientsTable
                  clients={currentClients}
                  onViewClient={handleViewClient}
                  onAction={handleClientAction}
                  viewMode={viewMode}
                  selectedClientId={selectedClient?.id ?? null}
                />
                {!isLoadingClients && !isFetchingClients && !isSearchPending && clientsResponse?.items.length === 0 && currentClients.length === 0 && (
                  <div className="py-6 text-center text-sm text-(--text-neutral-600)">
                    No clients found.
                  </div>
                )}
                {!isLoadingClients && !isFetchingClients && !isSearchPending && currentClients.length > 0 && (
                  <div className="mt-2 text-xs text-(--text-neutral-600)">
                    Showing {currentClients.length} of {totalCount} clients
                  </div>
                )}
                {hasMore || isFetchingClients || isSearchPending ? (
                  <div
                    ref={observerTarget}
                    className="flex h-8 w-full items-center justify-center"
                  >
                    {(isFetchingClients || isSearchPending) && (
                      <div role="status" className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                        <ContentLoader variant="inline" size="md" />
                        {searchQuery.trim() ? "Searching clients..." : "Loading clients..."}
                      </div>
                    )}
                  </div>
                ) : null}
              </>
            )}
          </div>
        </div>
      )}

      {/* Client Profile Side Panel */}
      {isClientProfileOpen && !selectedClient && isFetchingSelectedClient ? (
        <div
          className={cn(
            "relative min-w-0 overflow-hidden border-l border-(--neutral-100) bg-white transition-all duration-700",
            viewMode === "half" &&
              "fixed inset-y-0 right-0 left-0 z-50 shadow-[-1px_0_2px_#1E282E1A] md:left-112",
            viewMode === "full" && "fixed top-0 left-0 right-0 bottom-0 z-50",
          )}
        >
          <ContentLoader size="md" className="h-full" />
        </div>
      ) : null}
      {isClientProfileOpen && selectedClient && (
        <div
          className={cn(
            "relative min-w-0 overflow-hidden border-l border-(--neutral-100) bg-white transition-all duration-700",
            viewMode === "half" &&
              "fixed inset-y-0 right-0 left-0 z-50 shadow-[-1px_0_2px_#1E282E1A] md:left-112",
            viewMode === "full" && "fixed top-0 left-0 right-0 bottom-0 z-50",
          )}
        >
          <ClientProfile
            client={selectedClient}
            isOpen={isClientProfileOpen}
            viewMode={viewMode}
            onClose={handleCloseProfile}
            onToggleFullView={handleToggleFullView}
            onEdit={handleEditClient}
            onDelete={handleDeleteClient}
            onCloseFile={handleCloseClientFile}
            onActivateFile={canReopenFile ? handleActivateClientFile : undefined}
            isClosingFile={isClosingClientFile}
            isActivatingFile={isActivatingClientFile}
            isPortalAccessUpdating={isUpdatingPortalAccess}
            isPortalActivationSending={isSendingPortalActivation}
            onUpdatePortalAccess={handleUpdatePortalAccess}
            onResendPortalActivation={handleResendPortalActivation}
            requestedAction={requestedProfileAction}
            requestedActionNonce={requestedProfileActionNonce}
            initialTab={profileInitialTab}
          />
          {isFetchingSelectedClient &&
          selectedClientId !== null &&
          Number.parseInt(selectedClient.id, 10) !== selectedClientId ? (
            <ContentLoader size="md" className="pointer-events-none absolute inset-0 bg-white/40" />
          ) : null}
        </div>
      )}

      {/* Add New Client Modal */}
      <AddNewClientModal
        isOpen={isAddClientModalOpen}
        onClose={() => setIsAddClientModalOpen(false)}
        onSubmit={handleAddClient}
        isSubmitPending={isCreatingClient}
      />

      <AddNewClientModal
        isOpen={isEditClientModalOpen}
        onClose={() => {
          setIsEditClientModalOpen(false);
          setEditingClientId(null);
        }}
        onSubmit={handleUpdateClient}
        isSubmitPending={isUpdatingClient}
        initialValues={editClientInitialValues}
        clientId={editingClientId}
        mode="edit"
        isInitialDataLoading={isLoadingEditingClient && !editingClientResponse}
      />

      {/* Delete Client Modal */}
      <DeleteClientModal
        client={selectedClient}
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        isConfirmPending={isDeletingClient}
      />
    </div>
  );
};

export default Clients;
