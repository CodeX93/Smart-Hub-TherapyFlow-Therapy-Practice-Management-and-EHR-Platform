
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomSelect from "../form/CustomSelect";
import CustomInput from "../form/CustomInput";
import Toast from "@/components/shared/Toast";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  useCreateSessionBillingMutation,
  useLazyGetSessionBillingQuery,
} from "@/store/api/admin/billing.api";
import { useGetAdminBillingServicesQuery } from "@/store/api/admin/services.api";

interface CreateSessionBillingModalProps {
  isOpen: boolean;
  onClose: () => void;
  sessionId: number;
  onCreated?: (billingId: number) => void;
}

const CreateSessionBillingModalContent = ({
  isOpen,
  onClose,
  sessionId,
  onCreated,
}: CreateSessionBillingModalProps) => {
  const [serviceId, setServiceId] = useState("");
  const [units, setUnits] = useState("1");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [existingBillingId, setExistingBillingId] = useState<number | null>(null);

  const { data: services = [], isLoading: isServicesLoading } =
    useGetAdminBillingServicesQuery(undefined, { skip: !isOpen });
  const [triggerGetBilling, { isFetching: isCheckingBilling }] =
    useLazyGetSessionBillingQuery();
  const [createBilling, { isLoading: isCreating }] = useCreateSessionBillingMutation();

  useEffect(() => {
    if (!isOpen) return;

    void triggerGetBilling(sessionId)
      .unwrap()
      .then((billing) => setExistingBillingId(billing.id))
      .catch(() => setExistingBillingId(null));
  }, [isOpen, sessionId, triggerGetBilling]);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  if (!isOpen) return null;

  const serviceOptions = [
    { value: "", label: "Default service (from session)" },
    ...services.map((service) => ({
      value: String(service.id),
      label: `${service.serviceCode} — ${service.serviceName}`,
    })),
  ];

  const handleCreate = async () => {
    if (existingBillingId) {
      setToastType("info");
      setToastMessage("An invoice already exists for this session.");
      return;
    }

    const parsedUnits = Number.parseInt(units, 10);
    if (!Number.isFinite(parsedUnits) || parsedUnits < 1) {
      setToastType("error");
      setToastMessage("Units must be at least 1.");
      return;
    }

    try {
      const response = await createBilling({
        sessionId,
        body: {
          serviceId: serviceId ? Number.parseInt(serviceId, 10) : undefined,
          units: parsedUnits,
        },
      }).unwrap();
      setToastType("success");
      setToastMessage("Invoice created successfully.");
      onCreated?.(response.id);
      onClose();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const isBusy = isCheckingBilling || isCreating || isServicesLoading;

  return createPortal(
    <>
      <div
        data-create-invoice-modal
        className="app-modal-overlay fixed inset-0 z-[1100] flex items-center justify-center p-4"
      >
        <div className="app-modal-surface w-full max-w-lg rounded-3xl">
          <div className="flex items-center justify-between border-b border-(--neutral-100) px-6 py-5">
            <div>
              <h2 className="text-xl font-semibold text-(--text-primary-dark)">
                Create invoice
              </h2>
              <p className="mt-1 text-sm text-(--text-neutral-600)">
                Rates are calculated server-side from invoice policies.
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="text-(--text-neutral-600) hover:text-(--text-primary-dark)"
              aria-label="Close"
            >
              <X size={22} />
            </button>
          </div>

          <div className="space-y-4 px-6 py-5">
            {isCheckingBilling ? (
              <div className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <ContentLoader variant="inline" size="sm" />
                Checking existing billing...
              </div>
            ) : existingBillingId ? (
              <p className="rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-900">
                Invoice #{existingBillingId} already exists for this session.
              </p>
            ) : (
              <>
                <CustomSelect
                  label="Service (optional)"
                  placeholder="Default service (from session)"
                  value={serviceId}
                  onChange={setServiceId}
                  options={serviceOptions}
                  isSearch
                />
                <CustomInput
                  label="Units"
                  type="number"
                  min={1}
                  value={units}
                  onChange={(event) => setUnits(event.target.value)}
                  digitsOnly
                />
              </>
            )}
          </div>

          <div className="flex justify-end gap-3 border-t border-(--neutral-100) px-6 py-5">
            <Button
              type="button"
              variant="secondary"
              size="md"
              onClick={onClose}
              disabled={isCreating}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="primary"
              size="md"
              onClick={() => void handleCreate()}
              disabled={isBusy || Boolean(existingBillingId)}
              loading={isCreating}
              loadingLabel="Creating..."
            >
              Create invoice
            </Button>
          </div>
        </div>
      </div>

      {toastMessage ? (
        <Toast message={toastMessage} type={toastType} onClose={() => setToastMessage(null)} />
      ) : null}
    </>,
    document.body,
  );
};

const CreateSessionBillingModal = (props: CreateSessionBillingModalProps) => props.isOpen ? <CreateSessionBillingModalContent key={props.sessionId} {...props} /> : null;

export default CreateSessionBillingModal;
