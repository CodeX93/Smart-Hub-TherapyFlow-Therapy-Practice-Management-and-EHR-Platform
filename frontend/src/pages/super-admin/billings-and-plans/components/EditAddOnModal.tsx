import { useEffect, useMemo, useState } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import {
  type AddOnBillingCycle,
  type AddOnCatalogItem,
  type AddOnCatalogStatus,
  useGetAddOnCatalogItemQuery,
  useUpdateAddOnCatalogItemMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

interface EditAddOnModalProps {
  open: boolean;
  code: string | null;
  onClose(): void;
  onSaved?(item: AddOnCatalogItem): void;
  onSaveError?(message: string): void;
}

function getModalShellClassName(): string {
  return cn(
    "w-full max-w-[40rem] rounded-[0.875rem] border border-[#e3eaf1] bg-white",
    "shadow-[0_24px_60px_rgba(15,23,42,0.16)]"
  );
}

function getFieldShellClassName(): string {
  return cn(
    "relative overflow-hidden rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem]",
    "shadow-none"
  );
}

function getLabelClassName(): string {
  return "pointer-events-none absolute left-[0.8125rem] top-[0.4375rem] text-[0.625rem] font-medium leading-3 text-[#8b97a3]";
}

function getInputClassName(): string {
  return cn(
    "h-[2.75rem] border-0 bg-transparent px-0 pb-[0.4375rem] pt-[1.125rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function getTextareaClassName(): string {
  return cn(
    "min-h-[5.5rem] resize-none border-0 bg-transparent px-0 pb-[0.625rem] pt-[1.375rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function getSelectTriggerClassName(): string {
  return cn(
    "h-[2.75rem] w-full rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem]",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] shadow-none"
  );
}

function parsePriceUsd(input: string): number {
  const trimmed = input.trim();
  if (!trimmed) return 0;
  const normalized = trimmed.replaceAll(",", "");
  const parsed = Number.parseFloat(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
}

const ADD_ON_LIMITS = {
  nameMax: 80,
  descriptionMax: 500,
  priceMaxChars: 10,
} as const;

type EditAddOnErrors = {
  name?: string;
  description?: string;
  priceUsd?: string;
};

function validateEditAddOnFields(input: {
  name: string;
  description: string;
  priceUsd: string;
}): EditAddOnErrors {
  const errors: EditAddOnErrors = {};
  const name = input.name.trim();
  const description = input.description.trim();
  const price = input.priceUsd.trim();

  if (!name) {
    errors.name = "Name is required.";
  } else if (name.length > ADD_ON_LIMITS.nameMax) {
    errors.name = `Name must be ${ADD_ON_LIMITS.nameMax} characters or less.`;
  }

  if (description.length > ADD_ON_LIMITS.descriptionMax) {
    errors.description = `Description must be ${ADD_ON_LIMITS.descriptionMax} characters or less.`;
  }

  if (!price) {
    errors.priceUsd = "Price is required.";
  } else if (price.length > ADD_ON_LIMITS.priceMaxChars) {
    errors.priceUsd = `Price must be ${ADD_ON_LIMITS.priceMaxChars} characters or less.`;
  } else if (!/^\d+(\.\d{1,2})?$/.test(price)) {
    errors.priceUsd = "Enter a valid price (up to 2 decimals).";
  } else if (parsePriceUsd(price) < 0) {
    errors.priceUsd = "Price cannot be negative.";
  }

  return errors;
}

function EditAddOnModalContent(props: EditAddOnModalProps) {
  const code = props.code ?? "";
  const shouldLoad = props.open && Boolean(props.code);

  const {
    currentData: addOn,
    isLoading: isLoadingAddOn,
    isError: isAddOnError,
    error: addOnError,
  } = useGetAddOnCatalogItemQuery(code, {
    skip: !shouldLoad,
    refetchOnMountOrArgChange: true,
  });

  const [updateAddOn, { isLoading: isSaving }] = useUpdateAddOnCatalogItemMutation();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [priceUsd, setPriceUsd] = useState("");
  const [billingCycle, setBillingCycle] = useState<AddOnBillingCycle>("MONTHLY");
  const [status, setStatus] = useState<AddOnCatalogStatus>("ACTIVE");
  const [fieldErrors, setFieldErrors] = useState<EditAddOnErrors>({});

  const loadErrorMessage = useMemo(() => {
    if (!isAddOnError) return null;
    return getApiErrorMessage(addOnError);
  }, [addOnError, isAddOnError]);

  const draftKey = code;
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (props.open && addOn && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setName(addOn.name ?? "");
    setDescription(addOn.description ?? "");
    setPriceUsd(String(addOn.priceUsd ?? 0));
    setBillingCycle(addOn.billingCycle ?? "MONTHLY");
    setStatus(addOn.status ?? "ACTIVE");
    setFieldErrors({});
  }

  useEffect(() => {
    if (!props.open) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        props.onClose();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [props, props.open]);

  async function handleSave() {
    if (!props.code) return;
    const validationErrors = validateEditAddOnFields({ name, description, priceUsd });
    setFieldErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;
    try {
      const updated = await updateAddOn({
        code: props.code,
        body: {
          name: name.trim(),
          description: description.trim(),
          priceUsd: parsePriceUsd(priceUsd),
          billingCycle,
          status,
        },
      }).unwrap();
      props.onSaved?.(updated);
      props.onClose();
    } catch (error) {
      props.onSaveError?.(getApiErrorMessage(error));
    }
  }

  function handleNameChange(value: string) {
    setName(value);
    setFieldErrors(validateEditAddOnFields({ name: value, description, priceUsd }));
  }

  function handleDescriptionChange(value: string) {
    setDescription(value);
    setFieldErrors(validateEditAddOnFields({ name, description: value, priceUsd }));
  }

  function handlePriceChange(value: string) {
    if (!/^\d*(\.\d{0,2})?$/.test(value)) return;
    setPriceUsd(value);
    setFieldErrors(validateEditAddOnFields({ name, description, priceUsd: value }));
  }

  const hasValidationErrors = Object.keys(
    validateEditAddOnFields({ name, description, priceUsd })
  ).length > 0;

  if (!props.open) return null;

  return (
    <div className="fixed inset-0 z-9999">
      <button
        type="button"
        className="absolute inset-0 bg-[rgba(15,23,42,0.26)] backdrop-blur-[0.125rem]"
        aria-label="Close modal"
        onClick={props.onClose}
        disabled={isSaving}
      />

      <div className="absolute inset-0 flex items-center justify-center px-4 py-10">
        <div
          className={getModalShellClassName()}
          role="dialog"
          aria-modal="true"
          aria-label="Edit add-on"
        >
          <div className="flex items-start justify-between gap-4 px-[1.125rem] pt-[0.875rem]">
            <div className="text-[1.25rem] font-semibold leading-7 text-[#1f2d38]">
              Edit Add-on
            </div>

            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={props.onClose}
              disabled={isSaving}
              aria-label="Close"
            >
              <X size={18} strokeWidth={1.8} aria-hidden="true" />
            </Button>
          </div>

          <div className="px-[1.125rem] pb-[0.9375rem] pt-[1.125rem]">
            {isLoadingAddOn ? (
              <div className="rounded-[0.875rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
                Loading add-on...
              </div>
            ) : null}
            {loadErrorMessage ? (
              <div className="rounded-[0.875rem] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {loadErrorMessage}
              </div>
            ) : null}
            <div className="mt-4 flex flex-col gap-[0.75rem]">
              <div className={getFieldShellClassName()}>
                <div className={getLabelClassName()}>Name</div>
                <Input
                  value={name}
                  maxLength={ADD_ON_LIMITS.nameMax}
                  onChange={(e) => handleNameChange(e.target.value)}
                  className={getInputClassName()}
                />
              </div>
              <div className={cn("mt-1 text-[0.6875rem]", fieldErrors.name ? "text-[#dc2626]" : "text-[#8a96a3]")}>
                {fieldErrors.name ?? `Max ${ADD_ON_LIMITS.nameMax} characters.`}
              </div>

              <div className={getFieldShellClassName()}>
                <div className={getLabelClassName()}>Description</div>
                <Textarea
                  value={description}
                  maxLength={ADD_ON_LIMITS.descriptionMax}
                  onChange={(e) => handleDescriptionChange(e.target.value)}
                  className={getTextareaClassName()}
                />
              </div>
              <div
                className={cn(
                  "mt-1 text-[0.6875rem]",
                  fieldErrors.description ? "text-[#dc2626]" : "text-[#8a96a3]"
                )}
              >
                {fieldErrors.description ?? `Optional. Max ${ADD_ON_LIMITS.descriptionMax} characters.`}
              </div>

              <div className={getFieldShellClassName()}>
                <div className={getLabelClassName()}>Price (USD)</div>
                <Input
                  value={priceUsd}
                  maxLength={ADD_ON_LIMITS.priceMaxChars}
                  inputMode="decimal"
                  onChange={(e) => handlePriceChange(e.target.value)}
                  className={getInputClassName()}
                />
              </div>
              <div
                className={cn(
                  "mt-1 text-[0.6875rem]",
                  fieldErrors.priceUsd ? "text-[#dc2626]" : "text-[#8a96a3]"
                )}
              >
                {fieldErrors.priceUsd ?? `Required. Up to 2 decimals, max ${ADD_ON_LIMITS.priceMaxChars} chars.`}
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                    Billing Cycle
                  </div>
                  <Select value={billingCycle} onValueChange={(v) => setBillingCycle(v as AddOnBillingCycle)}>
                    <SelectTrigger className={getSelectTriggerClassName()}>
                      <SelectValue placeholder="Select" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="MONTHLY">Monthly</SelectItem>
                      <SelectItem value="ANNUAL">Annual</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                    Status
                  </div>
                  <Select value={status} onValueChange={(v) => setStatus(v as AddOnCatalogStatus)}>
                    <SelectTrigger className={getSelectTriggerClassName()}>
                      <SelectValue placeholder="Select" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="INACTIVE">Archived</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>

            <div className="mt-[1.375rem] flex items-center justify-end gap-[0.625rem]">
              <Button
                type="button"
                variant="secondary"
                size="lg"
                className="min-w-[5.1875rem]"
                onClick={props.onClose}
                disabled={isSaving}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="primary"
                size="lg"
                className="min-w-[7.125rem]"
                onClick={handleSave}
                disabled={
                  isSaving ||
                  isLoadingAddOn ||
                  Boolean(loadErrorMessage) ||
                  hasValidationErrors
                }
                loading={isSaving}
                loadingLabel="Saving..."
              >
                Save Changes
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function EditAddOnModal(props: EditAddOnModalProps) { return props.open ? <EditAddOnModalContent key={props.code} {...props} /> : null; }
export default EditAddOnModal;
