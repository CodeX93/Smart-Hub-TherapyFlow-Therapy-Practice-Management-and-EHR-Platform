import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useState, useCallback, useEffect, useMemo } from "react";
import { Search, Edit2 } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import SettingsLayout from "./SettingsLayout";
import CustomInput from "../form/CustomInput";
import AddServiceCodeModal from "./AddServiceCodeModal";
import DeleteConfirmationModal from "./DeleteConfirmationModal";
import type { ServiceCodeFormData } from "@/schemas/settings.schema";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useCreateAdminBillingServiceMutation,
  useDeleteAdminBillingServiceMutation,
  useGetAdminBillingServicesQuery,
  useLazyGetAdminBillingServiceByIdQuery,
  useUpdateAdminBillingServiceMutation,
  type AdminBillingService,
} from "@/store/api/admin/services.api";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import { isBillingModuleForbidden } from "@/utils/billingErrors";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  parseServiceBaseRate,
  parseServiceDuration,
} from "@/utils/serviceCodeInput";

function mapServiceToFormData(service: AdminBillingService): ServiceCodeFormData {
  return {
    code: service.serviceCode,
    name: service.serviceName,
    description: service.description ?? "",
    duration: service.durationInMinutes ? String(service.durationInMinutes) : "",
    price: String(service.baseRate),
  };
}

const ServicePrices = () => {
  const { canCapability } = useStaffAccess();
  const canManageServices = canCapability("manageBillingServices");
  const [searchQuery, setSearchQuery] = useState("");
  const [displayedItems, setDisplayedItems] = useState(20);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const itemsPerPage = 20;
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [editingService, setEditingService] =
    useState<ServiceCodeFormData | null>(null);
  const [activeEditingId, setActiveEditingId] = useState<number | null>(null);
  const [activeToggleKey, setActiveToggleKey] = useState<string | null>(null);
  const [serviceToDelete, setServiceToDelete] = useState<{
    id: number;
    code: string;
  } | null>(null);
  const {
    data: services = [],
    isLoading: isServicesLoading,
    isFetching: isServicesFetching,
    error: servicesError,
  } = useGetAdminBillingServicesQuery();
  const [createService, { isLoading: isCreatingService }] =
    useCreateAdminBillingServiceMutation();
  const [updateService, { isLoading: isUpdatingService }] =
    useUpdateAdminBillingServiceMutation();
  const [deleteService, { isLoading: isDeletingService }] =
    useDeleteAdminBillingServiceMutation();
  const [triggerGetServiceById, { isFetching: isLoadingServiceDetails }] =
    useLazyGetAdminBillingServiceByIdQuery();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const filteredServices = useMemo(
    () =>
      services.filter(
        (s) =>
          s.serviceName.toLowerCase().includes(searchQuery.toLowerCase()) ||
          s.serviceCode.toLowerCase().includes(searchQuery.toLowerCase()),
      ),
    [searchQuery, services],
  );

  const handleLoadMore = useCallback(() => {
    setIsLoadingMore(true);
    setDisplayedItems((prev) => prev + itemsPerPage);
    setIsLoadingMore(false);
  }, []);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: displayedItems < filteredServices.length,
    isLoading: isLoadingMore,
  });

  const paginatedServices = filteredServices.slice(0, displayedItems);

  const toggleVisibility = async (
    service: AdminBillingService,
    key: "therapistVisible" | "clientPortalVisible",
  ) => {
    const toggleId = `${service.id}-${key}`;
    setActiveToggleKey(toggleId);

    try {
      await updateService({
        id: service.id,
        body: {
          serviceCode: service.serviceCode,
          serviceName: service.serviceName,
          description: service.description,
          durationInMinutes: service.durationInMinutes,
          baseRate: service.baseRate,
          isActive: service.isActive,
          therapistVisible:
            key === "therapistVisible"
              ? !service.therapistVisible
              : service.therapistVisible,
          clientPortalVisible:
            key === "clientPortalVisible"
              ? !service.clientPortalVisible
              : service.clientPortalVisible,
        },
      }).unwrap();

      setToastType("success");
      setToastMessage("Service visibility updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setActiveToggleKey(null);
    }
  };

  const handleSaveService = async (data: ServiceCodeFormData) => {
    const baseRate = parseServiceBaseRate(data.price);
    const durationInMinutes = parseServiceDuration(data.duration);

    if (baseRate === undefined) {
      setToastType("error");
      setToastMessage("Base rate is required.");
      return;
    }

    const body = {
      serviceCode: data.code.trim(),
      serviceName: data.name.trim(),
      description: data.description?.trim() || undefined,
      durationInMinutes,
      baseRate,
      isActive: true,
      therapistVisible: true,
      clientPortalVisible: false,
    };

    try {
      if (activeEditingId) {
        const existingService = services.find((service) => service.id === activeEditingId);
        await updateService({
          id: activeEditingId,
          body: {
            ...body,
            isActive: existingService?.isActive ?? true,
            therapistVisible: existingService?.therapistVisible ?? true,
            clientPortalVisible: existingService?.clientPortalVisible ?? false,
            description: existingService?.description,
          },
        }).unwrap();
        setToastType("success");
        setToastMessage("Service updated successfully.");
      } else {
        await createService(body).unwrap();
        setToastType("success");
        setToastMessage("Service created successfully.");
      }

      setIsAddModalOpen(false);
      setEditingService(null);
      setActiveEditingId(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleConfirmDelete = async () => {
    if (!serviceToDelete) return;

    try {
      await deleteService(serviceToDelete.id).unwrap();
      setToastType("success");
      setToastMessage("Service deleted successfully.");
      setIsDeleteModalOpen(false);
      setServiceToDelete(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleEditService = async (serviceId: number) => {
    setActiveEditingId(serviceId);
    setEditingService(null);
    setIsAddModalOpen(true);

    try {
      const service = await triggerGetServiceById(serviceId).unwrap();
      setEditingService(mapServiceToFormData(service));
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setIsAddModalOpen(false);
      setActiveEditingId(null);
    }
  };

  if (isBillingModuleForbidden(servicesError)) {
    return <BillingModuleGate />;
  }

  if (!canManageServices) {
    return (
      <BillingModuleGate
        title="Billing service access required"
        description="You do not have permission to manage billing services. BILLING_MANAGE access is required."
      />
    );
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <SettingsLayout
        title="Services"
        description="Manage pricing and visibility for therapy services"
        actionLabel="Add Service Code"
        onAction={() => {
          setEditingService(null);
          setActiveEditingId(null);
          setIsAddModalOpen(true);
        }}
        toolbar={
          <div className="w-full max-w-md">
            <CustomInput
              placeholder="Search by service code or name..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setDisplayedItems(20);
              }}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="min-h-10 w-full rounded-full pb-0 pt-1.75 shadow-xs"
            />
          </div>
        }
      >
        <ScrollToTopButton />
        <div className="flex h-full min-h-0 flex-col overflow-hidden p-4">
          <div className="min-h-0 flex-1 overflow-auto rounded-2xl border border-(--neutral-100) custom-scrollbar">
            <table className="w-full text-left border-collapse">
              <thead className="sticky top-0 bg-(--bg-primary-50) border-b border-(--neutral-100) z-10">
                <tr>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Service Code
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Service Name
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Session Duration
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Current Price
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Therapist
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Client
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark) text-right">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-(--neutral-100)">
                {isServicesLoading ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="p-10 text-center text-sm text-(--text-neutral-500)"
                    >
                      <ContentLoader size="md" className="gap-2" />
                    </td>
                  </tr>
                ) : paginatedServices.length > 0 ? (
                  paginatedServices.map((service) => (
                    <tr
                      key={service.id}
                      className="hover:bg-(--bg-primary-light) transition-colors"
                    >
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {service.serviceCode}
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {service.serviceName}
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {service.durationInMinutes
                          ? `${service.durationInMinutes} minutes`
                          : "---"}
                      </td>
                      <td className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                        ${service.baseRate.toFixed(2)}
                      </td>
                      <td className="p-4">
                        <Switch
                          checked={service.therapistVisible}
                          disabled={
                            activeToggleKey === `${service.id}-therapistVisible`
                          }
                          onCheckedChange={() =>
                            void toggleVisibility(service, "therapistVisible")
                          }
                          aria-label={`Therapist visibility for ${service.serviceName}`}
                        />
                      </td>
                      <td className="p-4">
                        <Switch
                          checked={service.clientPortalVisible}
                          disabled={
                            activeToggleKey ===
                            `${service.id}-clientPortalVisible`
                          }
                          onCheckedChange={() =>
                            void toggleVisibility(service, "clientPortalVisible")
                          }
                          aria-label={`Client visibility for ${service.serviceName}`}
                        />
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => void handleEditService(service.id)}
                            className="p-2 hover:bg-slate-100 rounded-lg transition-colors text-(--text-neutral-500) hover:text-(--text-primary-dark) cursor-pointer"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => {
                              setServiceToDelete({
                                id: service.id,
                                code: service.serviceCode,
                              });
                              setIsDeleteModalOpen(true);
                            }}
                            className="p-2 hover:bg-red-50 rounded-lg transition-colors text-(--text-neutral-500) hover:text-red-500 cursor-pointer"
                          >
                            <TrashIcon size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={7}
                      className="p-10 text-center text-sm text-(--text-neutral-500)"
                    >
                      No services found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>

            <div
              ref={observerTarget}
              className="h-10 w-full flex items-center justify-center py-8"
            >
              {isLoadingMore ||
                (displayedItems < filteredServices.length && (
                  <ContentLoader variant="inline" size="md" />
                ))}
              {!isServicesLoading && isServicesFetching && displayedItems >= filteredServices.length ? (
                <ContentLoader variant="inline" size="md" />
              ) : null}
            </div>
          </div>
        </div>

        <AddServiceCodeModal
          isOpen={isAddModalOpen}
          onClose={() => {
            setIsAddModalOpen(false);
            setEditingService(null);
            setActiveEditingId(null);
          }}
          onSave={(data) => void handleSaveService(data)}
          initialData={editingService || undefined}
          isSaving={isCreatingService || isUpdatingService}
          isLoadingInitialData={Boolean(activeEditingId && isLoadingServiceDetails)}
        />

        <DeleteConfirmationModal
          isOpen={isDeleteModalOpen}
          onClose={() => {
            setIsDeleteModalOpen(false);
            setServiceToDelete(null);
          }}
          onConfirm={() => void handleConfirmDelete()}
          title={`Delete "${serviceToDelete?.code || "Service Code"}"`}
          description="Are you sure you want to delete this Service Code?"
          isDeleting={isDeletingService}
        />
      </SettingsLayout>
    </div>
  );
};

export default ServicePrices;
