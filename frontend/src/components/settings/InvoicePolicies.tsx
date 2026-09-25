import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Edit2, Search } from "lucide-react";
import SettingsLayout from "./SettingsLayout";
import CustomInput from "../form/CustomInput";
import InvoicePolicyModal from "./InvoicePolicyModal";
import DeleteConfirmationModal from "./DeleteConfirmationModal";
import Toast from "@/components/shared/Toast";
import ActionsDropdown from "@/components/shared/ActionsDropdown";
import { Switch } from "@/components/ui/switch";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import { getApiErrorMessage } from "@/utils/apiError";
import { getSessionStatusLabel } from "@/utils/sessionStatusPresentation";
import { isBillingModuleForbidden, isDuplicatePolicyError } from "@/utils/billingErrors";
import {
  buildInvoicePolicyRequest,
  formatInvoicePolicyClientTypeLabel,
  formatInvoicePolicyServiceScopeLabel,
  formatInvoicePolicySessionStatusLabel,
  formatPolicyPrice,
  mapInvoicePolicyToFormData,
} from "@/utils/invoicePolicyForm";
import type { InvoicePolicyFormData } from "@/schemas/settings.schema";
import {
  useActivateInvoicePolicyMutation,
  useCreateInvoicePolicyMutation,
  useDeactivateInvoicePolicyMutation,
  useDeleteInvoicePolicyMutation,
  useGetInvoicePoliciesQuery,
  useGetInvoicePolicyClientTypesQuery,
  useGetInvoicePolicySessionStatusesQuery,
  useGetInvoicePolicyServicesQuery,
  useLazyGetInvoicePolicyByIdQuery,
  useUpdateInvoicePolicyMutation,
  type InvoicePolicyResponse,
} from "@/store/api/admin/invoicePolicy.api";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import { useStaffAccess } from "@/hooks/useStaffAccess";

const InvoicePolicies = () => {
  const { canCapability } = useStaffAccess();
  const canManagePolicies = canCapability("manageInvoicePolicies");
  const [searchQuery, setSearchQuery] = useState("");
  const [displayedItems, setDisplayedItems] = useState(20);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const itemsPerPage = 20;
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [editingPolicyId, setEditingPolicyId] = useState<number | null>(null);
  const [policyToDelete, setPolicyToDelete] = useState<InvoicePolicyResponse | null>(null);
  const [initialFormData, setInitialFormData] = useState<InvoicePolicyFormData | undefined>();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [togglingPolicyId, setTogglingPolicyId] = useState<number | null>(null);

  const {
    data: policies = [],
    isLoading,
    isFetching,
    error: policiesError,
    refetch,
  } = useGetInvoicePoliciesQuery();
  const {
    data: clientTypes = [],
    error: clientTypesError,
  } = useGetInvoicePolicyClientTypesQuery();
  const {
    data: sessionStatuses = [],
    error: sessionStatusesError,
  } = useGetInvoicePolicySessionStatusesQuery();
  const {
    data: serviceOptions = [],
    error: serviceOptionsError,
  } = useGetInvoicePolicyServicesQuery();

  const [createPolicy, { isLoading: isCreating }] = useCreateInvoicePolicyMutation();
  const [updatePolicy, { isLoading: isUpdating }] = useUpdateInvoicePolicyMutation();
  const [activatePolicy] = useActivateInvoicePolicyMutation();
  const [deactivatePolicy] = useDeactivateInvoicePolicyMutation();
  const [deletePolicy, { isLoading: isDeleting }] = useDeleteInvoicePolicyMutation();
  const [triggerGetPolicyById] = useLazyGetInvoicePolicyByIdQuery();

  const moduleForbidden = useMemo(
    () =>
      [policiesError, clientTypesError, sessionStatusesError, serviceOptionsError].some(
        (error) => isBillingModuleForbidden(error),
      ),
    [clientTypesError, policiesError, serviceOptionsError, sessionStatusesError],
  );

  const clientTypeOptions = useMemo(
    () =>
      clientTypes
        .filter((option) => Boolean(option.optionKey?.trim()))
        .map((option) => ({
          value: option.optionKey,
          label: option.optionLabel || option.optionKey,
        })),
    [clientTypes],
  );

  const sessionStatusOptions = useMemo(
    () =>
      sessionStatuses
        .filter((option) => Boolean(option.optionKey?.trim()))
        .map((option) => ({
          value: option.optionKey,
          label: getSessionStatusLabel(option.optionKey),
        })),
    [sessionStatuses],
  );

  const serviceNameById = useMemo(() => {
    const map = new Map<number, string>();
    serviceOptions.forEach((option) => {
      if (option.serviceId != null) {
        map.set(option.serviceId, option.optionLabel);
      }
    });
    return map;
  }, [serviceOptions]);

  const filteredPolicies = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return policies;
    return policies.filter((policy) => {
      const haystack = [
        policy.policyName,
        policy.clientTypeLabel,
        policy.appointmentStatusLabel,
        policy.clientTypeKey,
        policy.appointmentStatusKey,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(query);
    });
  }, [policies, searchQuery]);

  const visiblePolicies = useMemo(
    () => filteredPolicies.slice(0, displayedItems),
    [displayedItems, filteredPolicies],
  );

  const hasMore = displayedItems < filteredPolicies.length;

  const handleLoadMore = useCallback(() => {
    if (!hasMore || isLoadingMore) return;
    setIsLoadingMore(true);
    window.setTimeout(() => {
      setDisplayedItems((current) => current + itemsPerPage);
      setIsLoadingMore(false);
    }, 150);
  }, [hasMore, isLoadingMore, itemsPerPage]);

  const { observerTarget } = useInfiniteScroll({
    hasMore,
    isLoading: isLoadingMore,
    onLoadMore: handleLoadMore,
  });

  useEffect(() => {
    setDisplayedItems(itemsPerPage);
  }, [searchQuery, policies.length, itemsPerPage]);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const showToast = useCallback((message: string, type: "success" | "error" | "info") => {
    setToastType(type);
    setToastMessage(message);
  }, []);

  const handleOpenCreate = () => {
    setEditingPolicyId(null);
    setInitialFormData(undefined);
    setIsModalOpen(true);
  };

  const handleOpenEdit = async (policy: InvoicePolicyResponse) => {
    setEditingPolicyId(policy.id);
    setInitialFormData(undefined);
    setIsModalOpen(true);

    try {
      const freshPolicy = await triggerGetPolicyById(policy.id).unwrap();
      setInitialFormData(
        mapInvoicePolicyToFormData(freshPolicy, clientTypes, sessionStatuses),
      );
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
      setIsModalOpen(false);
      setEditingPolicyId(null);
    }
  };

  const handleSave = async (data: InvoicePolicyFormData) => {
    const body = buildInvoicePolicyRequest(
      data,
      clientTypes,
      sessionStatuses,
      serviceOptions,
    );
    try {
      if (editingPolicyId) {
        await updatePolicy({ id: editingPolicyId, body }).unwrap();
        showToast("Invoice policy updated.", "success");
      } else {
        await createPolicy(body).unwrap();
        showToast("Invoice policy created.", "success");
      }
      setIsModalOpen(false);
      setEditingPolicyId(null);
      setInitialFormData(undefined);
      void refetch();
    } catch (error) {
      showToast(
        isDuplicatePolicyError(error)
          ? "A policy already exists for this client type, session status, and service scope."
          : getApiErrorMessage(error),
        "error",
      );
      throw error;
    }
  };

  const handleToggleEnabled = async (
    policy: InvoicePolicyResponse,
    enabled: boolean,
  ) => {
    setTogglingPolicyId(policy.id);
    try {
      if (enabled) {
        await activatePolicy(policy.id).unwrap();
        showToast("Policy activated.", "success");
      } else {
        await deactivatePolicy(policy.id).unwrap();
        showToast("Policy deactivated.", "success");
      }
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
    } finally {
      setTogglingPolicyId(null);
    }
  };

  const handleConfirmDelete = async () => {
    if (!policyToDelete) return;
    try {
      await deletePolicy(policyToDelete.id).unwrap();
      showToast("Invoice policy deleted.", "success");
      setIsDeleteModalOpen(false);
      setPolicyToDelete(null);
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
    }
  };

  if (moduleForbidden) {
    return <BillingModuleGate />;
  }

  if (!canManagePolicies) {
    return (
      <BillingModuleGate
        title="Invoice policy access required"
        description="You do not have permission to manage invoice policies. BILLING_MANAGE access is required."
      />
    );
  }

  return (
    <>
      <SettingsLayout
        title="Invoice Policies"
        description="Configure dynamic pricing rules by client type and session status. Policies apply when staff creates session billing."
        actionLabel="Add Policy"
        onAction={handleOpenCreate}
        toolbar={
          <div className="w-full max-w-md">
            <CustomInput
              placeholder="Search by client type, status, or policy name..."
              value={searchQuery}
              onChange={(event) => {
                setSearchQuery(event.target.value);
                setDisplayedItems(itemsPerPage);
              }}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="min-h-10 w-full rounded-full pb-0 pt-1.75 shadow-xs"
            />
          </div>
        }
      >
        <ScrollToTopButton />
        <div className="flex h-full min-h-0 flex-col overflow-hidden p-4">
          <div className="custom-scrollbar min-h-0 flex-1 overflow-auto rounded-2xl border border-(--neutral-100)">
            <table className="w-full min-w-[60rem] border-collapse text-left">
              <thead className="sticky top-0 z-10 border-b border-(--neutral-100) bg-(--bg-primary-50)">
                <tr>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Client type
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Session status
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Enabled
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Price type
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Invoice price
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Service scope
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Priority
                  </th>
                  <th className="p-4 text-right text-sm font-semibold text-(--text-primary-dark)">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-(--neutral-100)">
                {isLoading ? (
                  <tr>
                    <td colSpan={8} className="p-10 text-center text-sm text-(--text-neutral-500)">
                      <ContentLoader size="md" className="gap-2" />
                    </td>
                  </tr>
                ) : visiblePolicies.length > 0 ? (
                  visiblePolicies.map((policy) => (
                    <tr
                      key={policy.id}
                      className="transition-colors hover:bg-(--bg-primary-light)"
                    >
                      <td className="p-4 text-sm font-medium text-(--text-primary-dark)">
                        <div className="flex flex-col gap-0.5">
                          <span>{formatInvoicePolicyClientTypeLabel(policy)}</span>
                          {policy.policyName ? (
                            <span
                              className="block max-w-[12rem] truncate text-xs font-normal text-(--text-neutral-500)"
                              title={policy.policyName}
                            >
                              {policy.policyName}
                            </span>
                          ) : null}
                        </div>
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {formatInvoicePolicySessionStatusLabel(policy)}
                      </td>
                      <td className="p-4">
                        <Switch
                          checked={policy.enabled}
                          disabled={togglingPolicyId === policy.id}
                          onCheckedChange={(checked) =>
                            void handleToggleEnabled(policy, checked)
                          }
                        />
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {policy.priceType === "FIXED" ? "Fixed" : "Percentage"}
                      </td>
                      <td className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                        {formatPolicyPrice(policy.priceType, policy.invoicePrice)}
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {formatInvoicePolicyServiceScopeLabel(policy, serviceNameById)}
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {policy.priority ?? "—"}
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex justify-end">
                          <ActionsDropdown
                            actions={[
                              {
                                id: "edit",
                                label: "Edit",
                                icon: <Edit2 size={16} />,
                                onClick: () => void handleOpenEdit(policy),
                              },
                              {
                                id: "delete",
                                label: "Delete",
                                icon: <TrashIcon size={16} />,
                                variant: "destructive",
                                onClick: () => {
                                  setPolicyToDelete(policy);
                                  setIsDeleteModalOpen(true);
                                },
                              },
                            ]}
                          />
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={8} className="p-10 text-center text-sm text-(--text-neutral-500)">
                      No invoice policies found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>

            <div
              ref={observerTarget}
              className="flex h-10 w-full items-center justify-center py-8"
            >
              {isLoadingMore ? (
                <ContentLoader variant="inline" size="md" />
              ) : null}
              {!isLoading && isFetching && visiblePolicies.length > 0 ? (
                <ContentLoader variant="inline" size="md" />
              ) : null}
            </div>
          </div>
        </div>
      </SettingsLayout>

      <InvoicePolicyModal
        isOpen={isModalOpen}
        onClose={() => {
          if (isCreating || isUpdating) return;
          setIsModalOpen(false);
          setEditingPolicyId(null);
          setInitialFormData(undefined);
        }}
        onSave={handleSave}
        initialData={initialFormData}
        isEditMode={editingPolicyId !== null}
        isLoadingInitialData={editingPolicyId !== null && initialFormData === undefined}
        formKey={editingPolicyId !== null ? String(editingPolicyId) : "create"}
        clientTypeOptions={clientTypeOptions}
        sessionStatusOptions={sessionStatusOptions}
        isSaving={isCreating || isUpdating}
      />

      <DeleteConfirmationModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          if (isDeleting) return;
          setIsDeleteModalOpen(false);
          setPolicyToDelete(null);
        }}
        onConfirm={() => void handleConfirmDelete()}
        title={`Delete policy "${policyToDelete?.policyName || policyToDelete?.clientTypeLabel || ""}"?`}
        description="This policy will be permanently removed and will no longer apply to new session billing."
        isDeleting={isDeleting}
      />

      {toastMessage ? (
        <Toast message={toastMessage} type={toastType} onClose={() => setToastMessage(null)} />
      ) : null}
    </>
  );
};

export default InvoicePolicies;
