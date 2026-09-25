
import { ContentLoader } from "@/components/shared/ContentLoader";
import { AlertTriangle, ExternalLink, Eye, X } from "lucide-react";
import SettingsLayout from "./SettingsLayout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import Toast from "@/components/shared/Toast";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import { isBillingModuleForbidden } from "@/utils/billingErrors";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  useGetTenantSubscriptionQuery,
  useGetTenantSubscriptionInvoicesQuery,
  useOpenTenantSubscriptionPortalMutation,
  usePayTenantSubscriptionInvoiceMutation,
  useStartTenantSubscriptionCheckoutMutation,
} from "@/store/api/admin/billing.api";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import type { TenantSubscriptionInvoice } from "@/types/sessionBilling.type";
import {
  formatSubscriptionDate,
  getNextActionButtonLabel,
  getSelfServeUnavailableMessage,
  getTrialCheckoutHelperText,
} from "@/utils/subscriptionSelfServe";

interface SubscriptionBillingProps {
  variant?: "settings" | "page";
}

function formatMoney(value?: number | null): string {
  if (value == null || Number.isNaN(value)) return "—";
  return `$${value.toFixed(2)}`;
}

function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="grid grid-cols-1 gap-1 border-b border-(--neutral-100) py-3 last:border-b-0 sm:grid-cols-[10rem_1fr] sm:gap-4">
      <dt className="text-sm font-medium text-(--text-neutral-600)">{label}</dt>
      <dd className="break-words text-sm text-(--text-primary-dark)">{value ?? "—"}</dd>
    </div>
  );
}

function SubscriptionInvoiceDetailsModal({
  invoice,
  onClose,
  canPay,
  paying,
  onPay,
}: {
  invoice: TenantSubscriptionInvoice;
  onClose: () => void;
  canPay: boolean;
  paying: boolean;
  onPay: () => void;
}) {
  return (
    <div
      className="app-modal-overlay fixed inset-0 z-100 flex items-center justify-center p-4 backdrop-blur-[0.125rem]"
      role="dialog"
      aria-modal="true"
      aria-labelledby="subscription-invoice-details-title"
      onClick={onClose}
    >
      <div
        className="app-modal-surface relative w-full max-w-lg overflow-hidden rounded-xl bg-white p-6 shadow-lg transition-all duration-300 animate-in fade-in zoom-in-95"
        onClick={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 rounded-full p-1.5 text-(--text-neutral-500) transition-colors hover:bg-slate-100"
          aria-label="Close invoice details"
        >
          <X size={20} />
        </button>

        <div className="mb-4 pr-10">
          <h2
            id="subscription-invoice-details-title"
            className="text-xl font-bold text-(--text-primary-dark)"
          >
            Invoice #{invoice.invoiceId}
          </h2>
          <p className="mt-1 text-sm text-(--text-neutral-500)">
            Subscription invoice details
          </p>
        </div>

        <dl className="mt-2">
          <DetailRow
            label="Status"
            value={
              <Badge variant="outline" className="uppercase">
                {invoice.status}
              </Badge>
            }
          />
          <DetailRow label="Amount" value={formatMoney(invoice.amount)} />
          <DetailRow
            label="Outstanding"
            value={formatMoney(invoice.outstandingBalance)}
          />
          <DetailRow label="Total paid" value={formatMoney(invoice.totalPaid)} />
          <DetailRow
            label="Refunded"
            value={formatMoney(invoice.refundedAmount)}
          />
          <DetailRow
            label="Due date"
            value={
              invoice.dueDate ? formatSubscriptionDate(invoice.dueDate) : "—"
            }
          />
          <DetailRow
            label="Billing period"
            value={
              invoice.billingPeriodStart || invoice.billingPeriodEnd
                ? `${formatSubscriptionDate(invoice.billingPeriodStart)} – ${formatSubscriptionDate(invoice.billingPeriodEnd)}`
                : "—"
            }
          />
          <DetailRow
            label="Paid at"
            value={invoice.paidAt ? formatSubscriptionDate(invoice.paidAt) : "—"}
          />
          <DetailRow
            label="Created"
            value={formatSubscriptionDate(invoice.createdAt)}
          />
          <DetailRow
            label="Subscription"
            value={
              invoice.subscriptionId != null
                ? `#${invoice.subscriptionId}`
                : "—"
            }
          />
          <DetailRow
            label="Provider invoice"
            value={invoice.providerInvoiceId || "—"}
          />
        </dl>

        <div className="mt-6 flex flex-wrap items-center justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Close
          </Button>
          {canPay ? (
            <Button
              type="button"
              className="rounded-full"
              disabled={paying}
              loading={paying}
              loadingLabel="Paying invoice..."
              onClick={onPay}
            >
              <ExternalLink className="mr-2 h-4 w-4" />
              Pay invoice
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

const SubscriptionBilling = ({ variant = "settings" }: SubscriptionBillingProps) => {
  const { canCapability, hasAny } = useStaffAccess();
  const canRead = hasAny("BILLING_VIEW", "BILLING_EDIT", "BILLING_MANAGE");
  const canManage = canCapability("paySubscriptionInvoice");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<
    "checkout" | "portal" | "pay" | null
  >(null);
  const [payingInvoiceId, setPayingInvoiceId] = useState<number | null>(null);
  const [selectedInvoice, setSelectedInvoice] =
    useState<TenantSubscriptionInvoice | null>(null);

  const {
    data: subscription,
    isLoading: isSubLoading,
    error: subError,
  } = useGetTenantSubscriptionQuery(undefined, { skip: !canRead });
  const {
    data: invoicesPage,
    isLoading: isInvoicesLoading,
    error: invoicesError,
  } = useGetTenantSubscriptionInvoicesQuery(
    { page: 0, size: 10, status: "all" },
    { skip: !canRead },
  );
  const [payInvoice] = usePayTenantSubscriptionInvoiceMutation();
  const [startCheckout] = useStartTenantSubscriptionCheckoutMutation();
  const [openPortal] = useOpenTenantSubscriptionPortalMutation();

  const moduleForbidden = useMemo(
    () => [subError, invoicesError].some((error) => isBillingModuleForbidden(error)),
    [invoicesError, subError],
  );

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const redirectToUrl = (url?: string | null) => {
    if (!url) {
      setToastMessage("Redirect URL was not returned.");
      return;
    }
    window.location.assign(url);
  };

  const handleCheckout = async () => {
    try {
      setActionLoading("checkout");
      const response = await startCheckout().unwrap();
      redirectToUrl(response.checkoutUrl);
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setActionLoading(null);
    }
  };

  const handlePortal = async () => {
    try {
      setActionLoading("portal");
      const response = await openPortal().unwrap();
      redirectToUrl(response.portalUrl);
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setActionLoading(null);
    }
  };

  const handlePay = async (invoiceId: number) => {
    try {
      setPayingInvoiceId(invoiceId);
      setActionLoading("pay");
      const response = await payInvoice(invoiceId).unwrap();
      redirectToUrl(response.paymentUrl);
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setPayingInvoiceId(null);
      setActionLoading(null);
    }
  };

  if (moduleForbidden) {
    return <BillingModuleGate />;
  }

  if (!canRead) {
    return (
      <BillingModuleGate
        title="Subscription billing access required"
        description="You do not have permission to view organisation subscription billing."
      />
    );
  }

  const isBusy = isSubLoading || isInvoicesLoading;
  const nextAction = subscription?.nextAction ?? "none";
  const canSelfServe = subscription?.canSelfServeRenew !== false;
  const primaryActionLabel = getNextActionButtonLabel(nextAction, subscription);
  const pendingInvoiceId = subscription?.pendingInvoiceId ?? null;
  const trialHelperText = getTrialCheckoutHelperText(subscription);
  const selfServeUnavailableMessage = getSelfServeUnavailableMessage(subscription);
  const showTrialCheckout =
    Boolean(
      subscription?.trialing ||
        subscription?.subscriptionStatus?.toLowerCase() === "trialing",
    ) &&
    subscription?.providerBillingConfigured !== true &&
    canManage;

  const isInvoicePayable = (invoice: TenantSubscriptionInvoice) =>
    canManage &&
    canSelfServe &&
    invoice.outstandingBalance > 0 &&
    invoice.status?.toLowerCase() !== "paid";

  const renderCheckoutButton = (className?: string) => (
    <Button
      type="button"
      className={className ?? "rounded-full"}
      disabled={actionLoading === "checkout"}
      loading={actionLoading === "checkout"}
      loadingLabel="Opening checkout..."
      onClick={() => void handleCheckout()}
    >
      {primaryActionLabel ?? "Add payment method"}
    </Button>
  );

  const renderPrimaryAction = () => {
    if (!canManage) return null;

    if (nextAction === "subscribe" || (showTrialCheckout && nextAction === "none")) {
      return renderCheckoutButton();
    }

    if (!canSelfServe) return null;

    if (nextAction === "pay_invoice" && pendingInvoiceId) {
      return (
        <Button
          type="button"
          className="rounded-full"
          disabled={actionLoading === "pay"}
          loading={actionLoading === "pay"}
          loadingLabel="Paying invoice..."
          onClick={() => void handlePay(pendingInvoiceId)}
        >
          <ExternalLink className="mr-2 h-4 w-4" />
          {primaryActionLabel}
        </Button>
      );
    }

    if (nextAction === "update_payment_method") {
      return (
        <Button
          type="button"
          variant="outline"
          className="rounded-full"
          disabled={actionLoading === "portal"}
          loading={actionLoading === "portal"}
          loadingLabel="Opening billing portal..."
          onClick={() => void handlePortal()}
        >
          {primaryActionLabel}
        </Button>
      );
    }

    return null;
  };

  const content = (
    <div className="custom-scrollbar h-full space-y-6 overflow-y-auto p-6">
      {isBusy ? (
        <div className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
          <ContentLoader variant="inline" size="sm" />
        </div>
      ) : subscription ? (
        <>
          {subscription.trialing && subscription.trialEndAt ? (
            <div className="rounded-2xl border border-sky-200 bg-sky-50 px-4 py-3">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1 text-sm text-sky-950">
                  <p>Trial active until {formatSubscriptionDate(subscription.trialEndAt)}.</p>
                  {showTrialCheckout && trialHelperText ? (
                    <p className="text-sky-900">{trialHelperText}</p>
                  ) : null}
                </div>
                {showTrialCheckout ? renderCheckoutButton("shrink-0 rounded-full") : null}
              </div>
            </div>
          ) : null}

          {subscription.accessRestricted ? (
            <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3">
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />
              <div className="space-y-1">
                <p className="text-sm font-semibold text-amber-950">Access restricted</p>
                <p className="text-sm text-amber-900">
                  {nextAction === "contact_support"
                    ? "Contact support to renew your subscription and restore full access."
                    : "Resolve subscription billing to restore full organisation access."}
                </p>
                {nextAction === "contact_support" ? (
                  <a
                    href="mailto:support@therapyflow.pro"
                    className="inline-block text-sm font-medium text-amber-950 underline"
                  >
                    Contact support
                  </a>
                ) : null}
              </div>
            </div>
          ) : null}

          <section className="rounded-3xl border border-(--neutral-100) bg-white p-6 shadow-xs">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-lg font-semibold text-(--text-primary-dark)">
                    {subscription.planName}
                  </h3>
                  <Badge>{subscription.subscriptionStatus}</Badge>
                  <Badge variant="outline">{subscription.billingCycle}</Badge>
                </div>
                <p className="mt-2 text-sm text-(--text-neutral-600)">
                  ${subscription.priceAtTime.toFixed(2)} / {subscription.billingCycle}
                </p>
                {subscription.currentPeriodEnd ? (
                  <p className="mt-1 text-sm text-(--text-neutral-600)">
                    Current period ends {formatSubscriptionDate(subscription.currentPeriodEnd)}
                  </p>
                ) : null}
                {subscription.billingMode ? (
                  <p className="mt-1 text-xs text-(--text-neutral-500)">
                    Billing mode: {subscription.billingMode.replaceAll("_", " ")}
                  </p>
                ) : null}
                {selfServeUnavailableMessage ? (
                  <p className="mt-2 text-sm text-(--text-neutral-600)">
                    {selfServeUnavailableMessage}{" "}
                    <a
                      href="mailto:support@therapyflow.pro"
                      className="font-medium text-(--primary-600) underline"
                    >
                      Contact support
                    </a>
                  </p>
                ) : null}
              </div>
              <div className="flex flex-wrap items-center gap-2">
                {!showTrialCheckout ? renderPrimaryAction() : null}
                {canManage &&
                subscription.providerBillingConfigured &&
                nextAction === "none" ? (
                  <Button
                    type="button"
                    variant="link"
                    className="h-auto px-0 text-(--primary-600)"
                    disabled={actionLoading === "portal"}
                    loading={actionLoading === "portal"}
                    loadingLabel="Opening billing portal..."
                    onClick={() => void handlePortal()}
                  >
                    Manage billing
                  </Button>
                ) : null}
              </div>
            </div>
          </section>
        </>
      ) : (
        <p className="text-sm text-(--text-neutral-600)">No active subscription found.</p>
      )}

      <section className="rounded-3xl border border-(--neutral-100) bg-white p-6 shadow-xs">
        <h3 className="text-lg font-semibold text-(--text-primary-dark)">Subscription invoices</h3>
        <div className="mt-4 space-y-3">
          {(invoicesPage?.content ?? []).length === 0 ? (
            <p className="text-sm text-(--text-neutral-600)">No subscription invoices.</p>
          ) : (
            invoicesPage?.content.map((invoice) => {
              const payable = isInvoicePayable(invoice);
              return (
                <div
                  key={invoice.invoiceId}
                  className="group flex flex-col gap-3 rounded-2xl border border-(--neutral-100) bg-white p-4 transition-all duration-200 hover:border-(--bg-primary-100) hover:bg-(--bg-primary-50) hover:shadow-sm sm:flex-row sm:items-center sm:justify-between"
                >
                  <button
                    type="button"
                    className="flex min-w-0 flex-1 cursor-pointer items-center gap-3 rounded-xl text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-(--bg-primary-100) focus-visible:ring-offset-2"
                    onClick={() => setSelectedInvoice(invoice)}
                  >
                    <div className="min-w-0 flex-1">
                      <p className="font-medium text-(--text-primary-dark) transition-colors group-hover:text-(--text-primary-500)">
                        Invoice #{invoice.invoiceId}
                      </p>
                      <p className="text-sm text-(--text-neutral-600)">
                        {formatMoney(invoice.amount)} · Due {invoice.dueDate ?? "—"} ·{" "}
                        {invoice.status}
                      </p>
                      <p className="mt-1 text-xs font-medium text-(--text-primary-500)">
                        View details
                      </p>
                    </div>
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-transparent text-(--text-neutral-400) transition-all duration-200 group-hover:border-(--neutral-100) group-hover:bg-white group-hover:text-(--text-primary-500) group-hover:shadow-sm">
                      <Eye className="h-4 w-4" aria-hidden />
                    </span>
                  </button>
                  {payable ? (
                    <Button
                      type="button"
                      className="rounded-full"
                      disabled={payingInvoiceId === invoice.invoiceId}
                      loading={payingInvoiceId === invoice.invoiceId}
                      loadingLabel="Paying invoice..."
                      onClick={() => void handlePay(invoice.invoiceId)}
                    >
                      <ExternalLink className="mr-2 h-4 w-4" />
                      Pay invoice
                    </Button>
                  ) : null}
                </div>
              );
            })
          )}
        </div>
      </section>
    </div>
  );

  return (
    <>
      {variant === "settings" ? (
        <SettingsLayout
          title="Subscription billing"
          description="Your organisation's TherapyFlow SaaS subscription and invoices."
        >
          {content}
        </SettingsLayout>
      ) : (
        <div className="mx-auto w-full max-w-5xl">
          <div className="mb-6">
            <h1 className="text-2xl font-semibold text-(--text-primary-dark)">
              Subscription billing
            </h1>
            <p className="mt-1 text-sm text-(--text-neutral-600)">
              Your organisation&apos;s TherapyFlow SaaS subscription and invoices.
            </p>
          </div>
          <div className="rounded-3xl border border-(--neutral-100) bg-white shadow-xs">
            {content}
          </div>
        </div>
      )}
      {selectedInvoice ? (
        <SubscriptionInvoiceDetailsModal
          invoice={selectedInvoice}
          onClose={() => setSelectedInvoice(null)}
          canPay={isInvoicePayable(selectedInvoice)}
          paying={payingInvoiceId === selectedInvoice.invoiceId}
          onPay={() => void handlePay(selectedInvoice.invoiceId)}
        />
      ) : null}
      {toastMessage ? (
        <Toast message={toastMessage} type="error" onClose={() => setToastMessage(null)} />
      ) : null}
    </>
  );
};

export default SubscriptionBilling;
