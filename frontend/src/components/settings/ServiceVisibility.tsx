
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useCallback, useEffect, useMemo } from "react";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import SettingsLayout from "./SettingsLayout";
import CustomInput from "../form/CustomInput";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useGetAdminBillingServicesQuery,
  useHideAllBillingServicesFromTherapistsMutation,
  useShowAllBillingServicesToTherapistsMutation,
  useUpdateAdminBillingServiceMutation,
  type AdminBillingService,
} from "@/store/api/admin/services.api";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import { isBillingModuleForbidden } from "@/utils/billingErrors";

function formatServiceError(error: unknown): string {
  if (
    typeof error === "object" &&
    error !== null &&
    "data" in error &&
    typeof (error as { data?: unknown }).data === "object" &&
    (error as { data?: Record<string, unknown> }).data !== null
  ) {
    const data = (error as { data?: Record<string, unknown> }).data;
    const message = data?.message;
    if (typeof message === "string" && message.trim()) return message;
  }

  if (
    typeof error === "object" &&
    error !== null &&
    "error" in error &&
    typeof (error as { error?: unknown }).error === "string"
  ) {
    return (error as { error: string }).error;
  }

  return "Something went wrong while updating service visibility.";
}

function TruncatedCell({
  value,
  className = "",
  maxWidthClassName = "max-w-[10rem]",
}: {
  value: string;
  className?: string;
  maxWidthClassName?: string;
}) {
  return (
    <td className={`p-4 text-sm text-(--text-primary-dark) ${className}`}>
      <span className={`block truncate ${maxWidthClassName}`} title={value}>
        {value}
      </span>
    </td>
  );
}

const ServiceVisibility = () => {
  const { canCapability } = useStaffAccess();
  const canManageServices = canCapability("manageBillingServices");
  const [searchQuery, setSearchQuery] = useState("");
  const [displayedItems, setDisplayedItems] = useState(20);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [activeToggleKey, setActiveToggleKey] = useState<string | null>(null);
  const itemsPerPage = 20;
  const {
    data: services = [],
    isLoading: isServicesLoading,
    isFetching: isServicesFetching,
    error: servicesError,
  } = useGetAdminBillingServicesQuery();
  const [updateServiceVisibility] = useUpdateAdminBillingServiceMutation();
  const [showAllToTherapists, { isLoading: isShowingAll }] =
    useShowAllBillingServicesToTherapistsMutation();
  const [hideAllFromTherapists, { isLoading: isHidingAll }] =
    useHideAllBillingServicesFromTherapistsMutation();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const toggleVisibility = async (
    service: AdminBillingService,
    key: "therapistVisible" | "clientPortalVisible",
  ) => {
    const toggleId = `${service.id}-${key}`;
    setActiveToggleKey(toggleId);

    try {
      await updateServiceVisibility({
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
      setToastMessage(formatServiceError(error));
    } finally {
      setActiveToggleKey(null);
    }
  };

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

  const allTherapistVisible = useMemo(
    () => services.length > 0 && services.every((service) => service.therapistVisible),
    [services],
  );

  const allTherapistHidden = useMemo(
    () => services.length > 0 && services.every((service) => !service.therapistVisible),
    [services],
  );

  const handleShowAllToTherapists = async () => {
    try {
      await showAllToTherapists().unwrap();
      setToastType("success");
      setToastMessage("All services are now visible to therapists.");
    } catch (error) {
      setToastType("error");
      setToastMessage(formatServiceError(error));
    }
  };

  const handleHideAllFromTherapists = async () => {
    try {
      await hideAllFromTherapists().unwrap();
      setToastType("success");
      setToastMessage("All services are now hidden from therapists.");
    } catch (error) {
      setToastType("error");
      setToastMessage(formatServiceError(error));
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
        title="Service Code Visibility"
        description="Control which service codes therapists can see and use when booking sessions"
        toolbar={
          <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
            <div className="w-full max-w-md">
              <CustomInput
                placeholder="Search categories..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setDisplayedItems(20);
                }}
                icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
                className="min-h-10 w-full rounded-full pb-0 pt-1.75 shadow-xs"
              />
            </div>
            <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
              <Button
                variant="outline"
                disabled={isShowingAll || isHidingAll || allTherapistVisible}
                loading={isShowingAll}
                loadingLabel="Updating..."
                onClick={() => void handleShowAllToTherapists()}
                className="h-10 cursor-pointer rounded-full border-(--neutral-200) px-6 font-semibold text-(--bg-primary-dark)"
              >
                Show all to Therapists
              </Button>
              <Button
                variant="outline"
                disabled={isShowingAll || isHidingAll || allTherapistHidden}
                loading={isHidingAll}
                loadingLabel="Updating..."
                onClick={() => void handleHideAllFromTherapists()}
                className="h-10 cursor-pointer rounded-full border-(--neutral-200) px-6 font-semibold text-(--bg-primary-dark)"
              >
                Hide all from Therapists
              </Button>
            </div>
          </div>
        }
      >
        <ScrollToTopButton />
        <div className="flex h-full min-h-0 flex-col overflow-hidden p-4">
          <div className="min-h-0 flex-1 overflow-x-auto overflow-y-auto rounded-2xl border border-(--neutral-100) custom-scrollbar">
            <table className="w-full border-collapse text-left">
              <thead className="sticky top-0 bg-(--bg-primary-50) border-b border-(--neutral-100) z-10">
                <tr>
                  <th className="w-[10rem] p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Service Code
                  </th>
                  <th className="min-w-[12rem] p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Service Name
                  </th>
                  <th className="w-[9rem] p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Session Duration
                  </th>
                  <th className="w-[7rem] p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Current Price
                  </th>
                  <th className="w-[11rem] p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Therapist Visible
                  </th>
                  <th className="w-[11rem] p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Client Portal Visible
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-(--neutral-100)">
                {isServicesLoading ? (
                  <tr>
                    <td
                      colSpan={6}
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
                      <TruncatedCell value={service.serviceCode} />
                      <TruncatedCell
                        value={service.serviceName}
                        maxWidthClassName="max-w-[16rem]"
                      />
                      <td className="whitespace-nowrap p-4 text-sm text-(--text-primary-dark)">
                        {service.durationInMinutes
                          ? `${service.durationInMinutes} minutes`
                          : "---"}
                      </td>
                      <td className="whitespace-nowrap p-4 text-sm font-medium text-(--text-primary-dark)">
                        ${service.baseRate.toFixed(2)}
                      </td>
                      <td className="p-4">
                        <div className="flex w-[11rem] items-center gap-2">
                          <Switch
                            checked={service.therapistVisible}
                            disabled={activeToggleKey === `${service.id}-therapistVisible`}
                            onCheckedChange={() =>
                              void toggleVisibility(service, "therapistVisible")
                            }
                          />
                          <span className="text-sm text-(--text-neutral-600)">
                            {service.therapistVisible ? "Visible" : "Hidden"}
                          </span>
                        </div>
                      </td>
                      <td className="p-4">
                        <div className="flex w-[11rem] items-center gap-2">
                          <Switch
                            checked={service.clientPortalVisible}
                            disabled={activeToggleKey === `${service.id}-clientPortalVisible`}
                            onCheckedChange={() =>
                              void toggleVisibility(service, "clientPortalVisible")
                            }
                          />
                          <span className="text-sm text-(--text-neutral-600)">
                            {service.clientPortalVisible ? "Visible" : "Hidden"}
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={6}
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
      </SettingsLayout>
    </div>
  );
};

export default ServiceVisibility;
