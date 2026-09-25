
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { CheckCircle2, ChevronDown, CreditCard, ExternalLink, RefreshCw, Unplug } from "lucide-react";
import SettingsLayout from "./SettingsLayout";
import CustomInput from "../form/CustomInput";
import Toast from "@/components/shared/Toast";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import DeleteConfirmationModal from "./DeleteConfirmationModal";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import { getApiErrorMessage } from "@/utils/apiError";
import { isBillingModuleForbidden } from "@/utils/billingErrors";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import {
  useDisconnectStripeConnectMutation,
  useGetStripeConnectConfigQuery,
  useGetStripeConnectStatusQuery,
  useRefreshStripeConnectMutation,
  useStartStripeConnectOAuthMutation,
  useUpdateStripeConnectConfigMutation,
  type StripeOnboardingStatus,
} from "@/store/api/admin/stripeConnect.api";

function statusBadgeVariant(status: StripeOnboardingStatus) {
  switch (status) {
    case "CONNECTED":
      return "default";
    case "PENDING":
      return "secondary";
    case "RESTRICTED":
      return "destructive";
    default:
      return "outline";
  }
}

function formatStatusLabel(status: StripeOnboardingStatus): string {
  return status.replace(/_/g, " ");
}

const StripeConnect = () => {
  const { canCapability } = useStaffAccess();
  const canManagePayments = canCapability("manageBillingServices");
  const [searchParams, setSearchParams] = useSearchParams();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [isDisconnectModalOpen, setIsDisconnectModalOpen] = useState(false);
  const [isAdvancedSettingsOpen, setIsAdvancedSettingsOpen] = useState(false);
  const [publishableKey, setPublishableKey] = useState("");
  const [secretKey, setSecretKey] = useState("");
  const [webhookEndpointUrl, setWebhookEndpointUrl] = useState("");
  const [webhookSecret, setWebhookSecret] = useState("");

  const {
    data: status,
    isLoading: isStatusLoading,
    isFetching: isStatusFetching,
    error: statusError,
    refetch: refetchStatus,
  } = useGetStripeConnectStatusQuery(undefined, { refetchOnMountOrArgChange: true });
  const {
    data: config,
    isLoading: isConfigLoading,
    refetch: refetchConfig,
  } = useGetStripeConnectConfigQuery();

  const [startOAuth, { isLoading: isStartingOAuth }] = useStartStripeConnectOAuthMutation();
  const [refreshConnect, { isLoading: isRefreshing }] = useRefreshStripeConnectMutation();
  const [disconnectConnect, { isLoading: isDisconnecting }] =
    useDisconnectStripeConnectMutation();
  const [updateConfig, { isLoading: isSavingConfig }] =
    useUpdateStripeConnectConfigMutation();

  const moduleForbidden = isBillingModuleForbidden(statusError);

  const draftKey = "stripe-config";
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (config && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setPublishableKey(config.publishableKey ?? "");
    setWebhookEndpointUrl(config.webhookEndpointUrl ?? "");
  }

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const stripeResult = searchParams.get("stripe");
  const [handledStripeResult, setHandledStripeResult] = useState<string | null>(null);
  if (stripeResult !== handledStripeResult) {
    setHandledStripeResult(stripeResult);
    if (stripeResult === "connected") {
      setToastType("success");
      setToastMessage("Stripe Connect onboarding completed. Refreshing status...");
    } else if (stripeResult === "error") {
      setToastType("error");
      setToastMessage("Stripe Connect onboarding did not complete.");
    }
  }
  useEffect(() => {
    if (!stripeResult) return;
    if (stripeResult === "connected") void Promise.all([refetchStatus(), refetchConfig()]);
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete("stripe");
    setSearchParams(nextParams, { replace: true });
  }, [stripeResult, searchParams, setSearchParams, refetchStatus, refetchConfig]);

  const showToast = (message: string, type: "success" | "error" | "info") => {
    setToastType(type);
    setToastMessage(message);
  };

  const handleConnect = async () => {
    try {
      const response = await startOAuth().unwrap();
      if (!response.authorizeUrl) {
        showToast("Stripe authorization URL was not returned.", "error");
        return;
      }
      window.location.href = response.authorizeUrl;
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
    }
  };

  const handleRefresh = async () => {
    try {
      await refreshConnect().unwrap();
      showToast("Stripe Connect status refreshed.", "success");
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
    }
  };

  const handleDisconnect = async () => {
    try {
      await disconnectConnect().unwrap();
      showToast("Stripe Connect disconnected.", "success");
      setIsDisconnectModalOpen(false);
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
    }
  };

  const handleSaveConfig = async () => {
    try {
      await updateConfig({
        publishableKey: publishableKey.trim() || undefined,
        secretKey: secretKey.trim() || undefined,
        webhookEndpointUrl: webhookEndpointUrl.trim() || undefined,
        webhookSecret: webhookSecret.trim() || undefined,
      }).unwrap();
      setSecretKey("");
      setWebhookSecret("");
      showToast("Stripe configuration saved.", "success");
      void refetchConfig();
    } catch (error) {
      showToast(getApiErrorMessage(error), "error");
    }
  };

  if (moduleForbidden) {
    return <BillingModuleGate />;
  }

  if (!canManagePayments) {
    return (
      <BillingModuleGate
        title="Payment integration access required"
        description="You do not have permission to manage Stripe Connect. BILLING_MANAGE access is required."
      />
    );
  }

  const isBusy = isStatusLoading || isStatusFetching || isConfigLoading;

  return (
    <>
      <SettingsLayout
        title="Payment Integration"
        description="Connect Stripe so clients can pay invoices online. Requires Billing module and STRIPE_PAYMENTS plan feature."
      >
        <div className="custom-scrollbar h-full space-y-6 overflow-y-auto p-6">
          <section className="rounded-3xl border border-(--neutral-100) bg-white p-6 shadow-xs">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-full bg-(--bg-primary-50) text-(--primary-500)">
                    <CreditCard size={22} />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-(--text-primary-dark)">
                      Stripe Connect
                    </h3>
                    <p className="text-sm text-(--text-neutral-600)">
                      Accept online payments from client portal invoices.
                    </p>
                  </div>
                </div>
                {status ? (
                  <div className="mt-4 flex flex-wrap items-center gap-2">
                    <Badge variant={statusBadgeVariant(status.onboardingStatus)}>
                      {formatStatusLabel(status.onboardingStatus)}
                    </Badge>
                    <Badge variant={status.chargesEnabled ? "default" : "secondary"}>
                      Charges {status.chargesEnabled ? "enabled" : "disabled"}
                    </Badge>
                    <Badge variant={status.payoutsEnabled ? "default" : "secondary"}>
                      Payouts {status.payoutsEnabled ? "enabled" : "disabled"}
                    </Badge>
                  </div>
                ) : null}
              </div>

              <div className="flex flex-wrap gap-2">
                {status?.onboardingStatus === "NOT_CONNECTED" ? (
                  <Button
                    type="button"
                    onClick={() => void handleConnect()}
                    disabled={isStartingOAuth || isBusy}
                    loading={isStartingOAuth}
                    loadingLabel="Connecting Stripe..."
                    className="rounded-full bg-(--bg-primary-dark) px-5 hover:bg-(--bg-primary-dark)/90"
                  >
                    <ExternalLink className="mr-2 h-4 w-4" />
                    Connect Stripe
                  </Button>
                ) : (
                  <>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => void handleRefresh()}
                      disabled={isRefreshing || isBusy}
                      className="rounded-full px-5"
                    >
                      <RefreshCw className="mr-2 h-4 w-4" />
                      Refresh
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setIsDisconnectModalOpen(true)}
                      disabled={isDisconnecting || isBusy}
                      className="rounded-full px-5"
                    >
                      <Unplug className="mr-2 h-4 w-4" />
                      Disconnect
                    </Button>
                  </>
                )}
              </div>
            </div>

            {isBusy ? (
              <div className="mt-6 flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <ContentLoader variant="inline" size="sm" />
              </div>
            ) : null}

            {status ? (
              <div className="mt-6 grid grid-cols-1 gap-3 text-sm text-(--text-neutral-600) sm:grid-cols-2">
                <p>
                  <span className="font-medium text-(--text-primary-dark)">Account:</span>{" "}
                  {status.connectAccountId || "Not connected"}
                </p>
                <p>
                  <span className="font-medium text-(--text-primary-dark)">Country:</span>{" "}
                  {status.country || "—"}
                </p>
                <p>
                  <span className="font-medium text-(--text-primary-dark)">Currency:</span>{" "}
                  {status.defaultCurrency?.toUpperCase() || "—"}
                </p>
                <p>
                  <span className="font-medium text-(--text-primary-dark)">Last synced:</span>{" "}
                  {status.lastSyncedAt
                    ? new Date(status.lastSyncedAt).toLocaleString()
                    : "—"}
                </p>
              </div>
            ) : null}

            {status?.onboardingStatus === "RESTRICTED" &&
            [...(status.currentlyDueRequirements ?? []), ...(status.pastDueRequirements ?? [])]
              .length > 0 ? (
              <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-4">
                <p className="text-sm font-medium text-amber-900">Action required</p>
                <ul className="mt-2 space-y-1 text-sm text-amber-800">
                  {[...(status.currentlyDueRequirements ?? []), ...(status.pastDueRequirements ?? [])].map(
                    (item) => (
                      <li key={item} className="flex items-start gap-2">
                        <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
                        <span>{item}</span>
                      </li>
                    ),
                  )}
                </ul>
              </div>
            ) : null}
          </section>

          <section className="overflow-hidden rounded-3xl border border-(--neutral-100) bg-white shadow-xs">
            <button
              type="button"
              onClick={() => setIsAdvancedSettingsOpen((open) => !open)}
              aria-expanded={isAdvancedSettingsOpen}
              className="flex w-full cursor-pointer items-center justify-between gap-4 px-6 py-5 text-left transition-colors hover:bg-(--neutral-50)"
            >
              <div className="min-w-0">
                <h3 className="text-lg font-semibold text-(--text-primary-dark)">
                  Advanced Settings
                </h3>
                <p className="mt-1 text-sm text-(--text-neutral-600)">
                  Use only if Connect Stripe fails — enter API keys manually.
                </p>
              </div>
              <ChevronDown
                size={20}
                className={`shrink-0 text-(--text-neutral-600) transition-transform duration-200 ${
                  isAdvancedSettingsOpen ? "rotate-180" : ""
                }`}
              />
            </button>

            <div
              className={`grid transition-all duration-300 ease-in-out ${
                isAdvancedSettingsOpen
                  ? "grid-rows-[1fr] opacity-100"
                  : "grid-rows-[0fr] opacity-0"
              }`}
            >
              <div className="overflow-hidden">
                <div className="space-y-5 border-t border-(--neutral-100) px-6 py-5">
                  <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3">
                    <p className="text-sm font-medium text-amber-950">
                      Manual key setup — only when Connect Stripe fails
                    </p>
                    <p className="mt-1 text-sm leading-5 text-amber-900">
                      Prefer the Connect Stripe button above. Use this section only if
                      that connection fails, then enter your Stripe keys here to connect
                      manually. Secret values are never shown again after save.
                    </p>
                  </div>

                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <CustomInput
                      label="Publishable key"
                      stopFloating
                      value={publishableKey}
                      onChange={(event) => setPublishableKey(event.target.value)}
                      placeholder="pk_live_..."
                    />
                    <CustomInput
                      label="Secret key"
                      stopFloating
                      type="password"
                      value={secretKey}
                      onChange={(event) => setSecretKey(event.target.value)}
                      placeholder={
                        config?.secretKeyConfigured
                          ? "Configured — enter to replace"
                          : "sk_live_..."
                      }
                    />
                    <CustomInput
                      label="Webhook endpoint URL"
                      stopFloating
                      value={webhookEndpointUrl}
                      onChange={(event) => setWebhookEndpointUrl(event.target.value)}
                      placeholder="https://api.example.com/..."
                    />
                    <CustomInput
                      label="Webhook secret"
                      stopFloating
                      type="password"
                      value={webhookSecret}
                      onChange={(event) => setWebhookSecret(event.target.value)}
                      placeholder={
                        config?.webhookSecretConfigured
                          ? "Configured — enter to replace"
                          : "whsec_..."
                      }
                    />
                  </div>

                  <div className="flex justify-end">
                    <Button
                      type="button"
                      onClick={() => void handleSaveConfig()}
                      disabled={isSavingConfig}
                      loading={isSavingConfig}
                      loadingLabel="Saving..."
                      className="rounded-full bg-(--bg-primary-dark) px-6 hover:bg-(--bg-primary-dark)/90"
                    >
                      Save configuration
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </SettingsLayout>

      <DeleteConfirmationModal
        isOpen={isDisconnectModalOpen}
        onClose={() => {
          if (isDisconnecting) return;
          setIsDisconnectModalOpen(false);
        }}
        onConfirm={() => void handleDisconnect()}
        title="Disconnect Stripe Connect?"
        description="Clients will not be able to pay invoices online until Stripe is connected again."
        isDeleting={isDisconnecting}
      />

      {toastMessage ? (
        <Toast message={toastMessage} type={toastType} onClose={() => setToastMessage(null)} />
      ) : null}
    </>
  );
};

export default StripeConnect;
