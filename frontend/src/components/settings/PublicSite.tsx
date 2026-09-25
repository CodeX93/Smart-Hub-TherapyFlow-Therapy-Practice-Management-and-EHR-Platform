import { useState, useEffect } from "react";
import { ContentLoader } from "@/components/shared/ContentLoader";
import { Switch } from "@/components/ui/switch";
import SettingsLayout from "./SettingsLayout";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import {
  useCreatePublicSiteServiceMutation,
  useDeletePublicSiteServiceMutation,
  useGetPublicSiteServicesQuery,
  useUpdatePublicSiteServiceMutation,
  type PublicSiteService,
} from "@/store/api/admin/publicSiteServices.api";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import { useStaffAccess } from "@/hooks/useStaffAccess";
import { getApiErrorMessage } from "@/utils/apiError";
import { isBillingModuleForbidden } from "@/utils/billingErrors";

const PublicSite = () => {
  const { canCapability } = useStaffAccess();
  const canManage = canCapability("manageBillingServices");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">(
    "info",
  );
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [newName, setNewName] = useState("");
  const [newDuration, setNewDuration] = useState("30");
  const [newRate, setNewRate] = useState("0");
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [drafts, setDrafts] = useState<
    Record<number, { durationMinutes: string; baseRate: string }>
  >({});

  const {
    data: services = [],
    isLoading,
    error,
  } = useGetPublicSiteServicesQuery();
  const [createService, { isLoading: isCreating }] =
    useCreatePublicSiteServiceMutation();
  const [updateService] = useUpdatePublicSiteServiceMutation();
  const [deleteService] = useDeletePublicSiteServiceMutation();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    const next: Record<number, { durationMinutes: string; baseRate: string }> =
      {};
    for (const s of services) {
      next[s.id] = {
        durationMinutes: String(s.durationMinutes ?? 30),
        baseRate: String(s.baseRate ?? 0),
      };
    }
    setDrafts(next);
  }, [services]);

  const toggleEnabled = async (service: PublicSiteService, enabled: boolean) => {
    if (!canManage) return;
    setTogglingId(service.id);
    try {
      await updateService({
        id: service.id,
        body: { enabled },
      }).unwrap();
      setToastType("success");
      setToastMessage(
        enabled
          ? `${service.name} enabled on public site.`
          : `${service.name} hidden from public site.`,
      );
    } catch (e) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(e));
    } finally {
      setTogglingId(null);
    }
  };

  const savePricing = async (service: PublicSiteService) => {
    if (!canManage) return;
    const draft = drafts[service.id];
    if (!draft) return;
    const durationMinutes = Number.parseInt(draft.durationMinutes, 10);
    const baseRate = Number.parseFloat(draft.baseRate);
    if (!Number.isFinite(durationMinutes) || durationMinutes < 5) {
      setToastType("error");
      setToastMessage("Duration must be at least 5 minutes.");
      return;
    }
    if (!Number.isFinite(baseRate) || baseRate < 0) {
      setToastType("error");
      setToastMessage("Rate must be zero or greater.");
      return;
    }
    setSavingId(service.id);
    try {
      await updateService({
        id: service.id,
        body: { durationMinutes, baseRate },
      }).unwrap();
      setToastType("success");
      setToastMessage(`Updated ${service.name} duration and price.`);
    } catch (e) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(e));
    } finally {
      setSavingId(null);
    }
  };

  const handleAdd = async () => {
    const name = newName.trim();
    if (!name || !canManage) return;
    const durationMinutes = Number.parseInt(newDuration, 10) || 30;
    const baseRate = Number.parseFloat(newRate) || 0;
    try {
      await createService({
        name,
        enabled: true,
        durationMinutes,
        baseRate,
      }).unwrap();
      setNewName("");
      setNewDuration("30");
      setNewRate("0");
      setToastType("success");
      setToastMessage(`${name} added.`);
    } catch (e) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(e));
    }
  };

  const handleRemove = async (service: PublicSiteService) => {
    if (!canManage || service.isSystem) return;
    setDeletingId(service.id);
    try {
      await deleteService(service.id).unwrap();
      setToastType("success");
      setToastMessage(`${service.name} removed.`);
    } catch (e) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(e));
    } finally {
      setDeletingId(null);
    }
  };

  if (isBillingModuleForbidden(error)) {
    return <BillingModuleGate />;
  }

  if (!canManage) {
    return (
      <BillingModuleGate
        title="Billing service access required"
        description="You do not have permission to manage public site services. BILLING_MANAGE access is required."
      />
    );
  }

  return (
    <SettingsLayout
      title="Public Site"
      description="Counseling services for the public Book Appointment dropdown. Set duration and price like billing services. Booking times still use Consultation Schedule hours."
    >
      <div className="flex h-full min-h-0 flex-col gap-4 overflow-y-auto p-4">
        {error ? (
          <p className="text-sm text-red-600">{getApiErrorMessage(error)}</p>
        ) : null}

        <div className="rounded-2xl border border-(--neutral-100) p-4">
          <p className="text-sm text-(--text-neutral-600)">
            Visitors pick a service name. Slot length uses each service’s{" "}
            <strong>duration</strong>; availability windows come from the
            therapist’s Consultation Schedule. Price is stored for the public
            catalog (shown in booking notifications).
          </p>
        </div>

        <div className="flex flex-col gap-2 lg:flex-row lg:items-end">
          <label className="flex min-w-0 flex-1 flex-col gap-1">
            <span className="text-sm text-(--text-neutral-600)">Name</span>
            <input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="e.g. Trauma Counseling"
              className="h-10 rounded-xl border border-(--neutral-100) px-3 text-sm outline-none focus:border-(--bg-primary-dark)"
            />
          </label>
          <label className="flex w-full flex-col gap-1 sm:w-28">
            <span className="text-sm text-(--text-neutral-600)">Duration</span>
            <input
              type="number"
              min={5}
              step={5}
              value={newDuration}
              onChange={(e) => setNewDuration(e.target.value)}
              className="h-10 rounded-xl border border-(--neutral-100) px-3 text-sm outline-none focus:border-(--bg-primary-dark)"
            />
          </label>
          <label className="flex w-full flex-col gap-1 sm:w-28">
            <span className="text-sm text-(--text-neutral-600)">Price</span>
            <input
              type="number"
              min={0}
              step={0.01}
              value={newRate}
              onChange={(e) => setNewRate(e.target.value)}
              className="h-10 rounded-xl border border-(--neutral-100) px-3 text-sm outline-none focus:border-(--bg-primary-dark)"
            />
          </label>
          <Button
            type="button"
            disabled={isCreating || !newName.trim()}
            onClick={() => void handleAdd()}
            className="h-10 rounded-full bg-(--bg-primary-dark) px-5 text-white"
          >
            {isCreating ? "Adding…" : "Add"}
          </Button>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-10">
            <ContentLoader variant="inline" size="md" />
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            <h3 className="text-sm font-medium text-(--text-neutral-600)">
              Public services
            </h3>
            {services.length === 0 ? (
              <p className="text-sm text-(--text-neutral-500)">
                No public services yet. Add one above (or wait for seed
                migration).
              </p>
            ) : (
              services.map((service) => {
                const draft = drafts[service.id] ?? {
                  durationMinutes: String(service.durationMinutes),
                  baseRate: String(service.baseRate),
                };
                const dirty =
                  Number.parseInt(draft.durationMinutes, 10) !==
                    service.durationMinutes ||
                  Math.abs(
                    Number.parseFloat(draft.baseRate) - Number(service.baseRate),
                  ) > 0.001;
                return (
                  <div
                    key={service.id}
                    className="flex flex-col gap-3 rounded-xl border border-(--neutral-100) px-4 py-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="font-medium text-(--text-primary-dark)">
                          {service.name}
                          {service.isSystem ? (
                            <span className="ml-2 text-xs font-normal text-(--text-neutral-400)">
                              (required)
                            </span>
                          ) : null}
                        </p>
                        <p className="truncate text-xs text-(--text-neutral-500)">
                          {service.slug}
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-3">
                        {!service.isSystem ? (
                          <button
                            type="button"
                            disabled={deletingId === service.id}
                            onClick={() => void handleRemove(service)}
                            className="text-sm text-red-600 hover:underline disabled:opacity-50"
                          >
                            Remove
                          </button>
                        ) : null}
                        <Switch
                          checked={Boolean(service.enabled)}
                          disabled={togglingId === service.id}
                          onCheckedChange={(checked) =>
                            void toggleEnabled(service, Boolean(checked))
                          }
                        />
                      </div>
                    </div>
                    <div className="flex flex-wrap items-end gap-2">
                      <label className="flex w-28 flex-col gap-1">
                        <span className="text-xs text-(--text-neutral-500)">
                          Duration (min)
                        </span>
                        <input
                          type="number"
                          min={5}
                          step={5}
                          value={draft.durationMinutes}
                          onChange={(e) =>
                            setDrafts((prev) => ({
                              ...prev,
                              [service.id]: {
                                ...draft,
                                durationMinutes: e.target.value,
                              },
                            }))
                          }
                          className="h-9 rounded-lg border border-(--neutral-100) px-2 text-sm"
                        />
                      </label>
                      <label className="flex w-28 flex-col gap-1">
                        <span className="text-xs text-(--text-neutral-500)">
                          Price
                        </span>
                        <input
                          type="number"
                          min={0}
                          step={0.01}
                          value={draft.baseRate}
                          onChange={(e) =>
                            setDrafts((prev) => ({
                              ...prev,
                              [service.id]: {
                                ...draft,
                                baseRate: e.target.value,
                              },
                            }))
                          }
                          className="h-9 rounded-lg border border-(--neutral-100) px-2 text-sm"
                        />
                      </label>
                      <Button
                        type="button"
                        disabled={!dirty || savingId === service.id}
                        onClick={() => void savePricing(service)}
                        className="h-9 rounded-full bg-(--bg-primary-dark) px-4 text-sm text-white disabled:opacity-40"
                      >
                        {savingId === service.id ? "Saving…" : "Save"}
                      </Button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        )}
      </div>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </SettingsLayout>
  );
};

export default PublicSite;
