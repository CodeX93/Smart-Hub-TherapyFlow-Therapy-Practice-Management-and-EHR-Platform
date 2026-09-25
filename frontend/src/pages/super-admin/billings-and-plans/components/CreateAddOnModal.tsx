import { useEffect, useState } from "react";
import { X } from "lucide-react";
import Toast from "@/components/shared/Toast";
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
  useCreateAddOnCatalogItemMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  CREATE_ADD_ON_LIMITS,
  parseAddOnPriceUsd,
  sanitizeAddOnCode,
  sanitizeAddOnDescription,
  sanitizeAddOnName,
  sanitizeAddOnPrice,
  validateCreateAddOnFields,
} from "../createAddOn.utils";

interface CreateAddOnModalProps {
  open: boolean;
  onClose(): void;
  onCreated?(item: AddOnCatalogItem): void;
}

function getFieldShellClassName(): string {
  return cn(
    "relative overflow-hidden rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem]",
    "shadow-none",
  );
}

function getLabelClassName(): string {
  return "pointer-events-none absolute left-[0.8125rem] top-[0.4375rem] text-[0.625rem] font-medium leading-3 text-[#8b97a3]";
}

function getInputClassName(): string {
  return cn(
    "h-[2.75rem] border-0 bg-transparent px-0 pb-[0.4375rem] pt-[1.125rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0",
  );
}

function getTextareaClassName(): string {
  return cn(
    "min-h-[5.5rem] resize-none border-0 bg-transparent px-0 pb-[0.625rem] pt-[1.375rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0",
  );
}

function getSelectTriggerClassName(): string {
  return cn(
    "h-[2.75rem] w-full min-w-0 rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem]",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] shadow-none [&>span]:truncate",
  );
}

function getSelectContentClassName(): string {
  return cn(
    "z-[10050] rounded-[0.875rem] border border-[#dce5ee] bg-white p-1",
    "shadow-[0_12px_28px_rgba(15,23,42,0.08)]",
  );
}

function CreateAddOnModalContent(props: CreateAddOnModalProps) {
  const [createAddOn, { isLoading: isSaving }] = useCreateAddOnCatalogItemMutation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [priceUsd, setPriceUsd] = useState("");
  const [billingCycle, setBillingCycle] = useState<AddOnBillingCycle>("MONTHLY");
  const [status, setStatus] = useState<AddOnCatalogStatus>("ACTIVE");

  useEffect(() => {
    if (!props.open) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") props.onClose();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [props.onClose, props.open]);



  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  async function handleCreate() {
    const validationError = validateCreateAddOnFields({
      code,
      name,
      description,
      priceUsd,
    });
    if (validationError) {
      showToast("error", validationError);
      return;
    }

    try {
      const created = await createAddOn({
        code: code.trim(),
        name: name.trim(),
        description: description.trim(),
        priceUsd: parseAddOnPriceUsd(priceUsd),
        billingCycle,
        status,
      }).unwrap();

      showToast("success", "Add-on created successfully.");
      props.onCreated?.(created);
      window.setTimeout(() => {
        props.onClose();
      }, 700);
    } catch (error) {
      showToast("error", getApiErrorMessage(error));
    }
  }

  if (!props.open) return null;

  return (
    <div className="fixed inset-0 z-[10040]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <button
        type="button"
        className="absolute inset-0 bg-[rgba(15,23,42,0.26)] backdrop-blur-[0.125rem]"
        aria-label="Close modal"
        onClick={props.onClose}
        disabled={isSaving}
      />

      <div className="absolute inset-0 flex items-end justify-center p-0 sm:items-center sm:p-4 sm:py-8">
        <div
          className={cn(
            "flex max-h-[min(100dvh,100%)] w-full max-w-[40rem] flex-col overflow-hidden",
            "rounded-t-[0.875rem] border border-[#e3eaf1] bg-white sm:max-h-[min(90dvh,calc(100dvh-2rem))] sm:rounded-[0.875rem]",
            "shadow-[0_24px_60px_rgba(15,23,42,0.16)]",
          )}
          role="dialog"
          aria-modal="true"
          aria-label="Create add-on"
        >
          <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#e8eef4] px-[1.125rem] py-[0.875rem]">
            <div className="min-w-0 truncate pr-2 text-[1.25rem] font-semibold leading-7 text-[#1f2d38]">
              Create Add-on
            </div>

            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={props.onClose}
              disabled={isSaving}
              className="shrink-0"
              aria-label="Close"
            >
              <X size={18} strokeWidth={1.8} aria-hidden="true" />
            </Button>
          </div>

          <div
            className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-[1.125rem] py-[1.125rem]"
            style={{ WebkitOverflowScrolling: "touch" }}
          >
            <div className="flex flex-col gap-[0.75rem]">
              <div className={getFieldShellClassName()}>
                <div className={getLabelClassName()}>
                  Code <span className="text-[#ef4444]">*</span>
                </div>
                <Input
                  value={code}
                  maxLength={CREATE_ADD_ON_LIMITS.codeMax}
                  placeholder="e.g. extra_storage"
                  onChange={(event) => setCode(sanitizeAddOnCode(event.target.value))}
                  className={getInputClassName()}
                />
              </div>

              <div className={getFieldShellClassName()}>
                <div className={getLabelClassName()}>
                  Name <span className="text-[#ef4444]">*</span>
                </div>
                <Input
                  value={name}
                  maxLength={CREATE_ADD_ON_LIMITS.nameMax}
                  placeholder="Enter add-on name"
                  onChange={(event) => setName(sanitizeAddOnName(event.target.value))}
                  className={getInputClassName()}
                />
              </div>

              <div className={getFieldShellClassName()}>
                <div className={getLabelClassName()}>Description</div>
                <Textarea
                  value={description}
                  maxLength={CREATE_ADD_ON_LIMITS.descriptionMax}
                  placeholder="Optional description"
                  onChange={(event) =>
                    setDescription(sanitizeAddOnDescription(event.target.value))
                  }
                  className={getTextareaClassName()}
                />
              </div>

              <div className={getFieldShellClassName()}>
                <div className={getLabelClassName()}>
                  Price (USD) <span className="text-[#ef4444]">*</span>
                </div>
                <Input
                  value={priceUsd}
                  maxLength={CREATE_ADD_ON_LIMITS.priceMaxChars}
                  inputMode="decimal"
                  placeholder="0.00"
                  onChange={(event) => setPriceUsd(sanitizeAddOnPrice(event.target.value))}
                  className={getInputClassName()}
                />
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div className="min-w-0">
                  <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                    Billing Cycle <span className="text-[#ef4444]">*</span>
                  </div>
                  <Select
                    value={billingCycle}
                    onValueChange={(value) => setBillingCycle(value as AddOnBillingCycle)}
                  >
                    <SelectTrigger className={getSelectTriggerClassName()}>
                      <SelectValue placeholder="Select billing cycle" />
                    </SelectTrigger>
                    <SelectContent
                      position="popper"
                      side="bottom"
                      align="start"
                      collisionPadding={12}
                      className={getSelectContentClassName()}
                    >
                      <SelectItem value="MONTHLY">Monthly</SelectItem>
                      <SelectItem value="ANNUAL">Annual</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="min-w-0">
                  <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                    Status <span className="text-[#ef4444]">*</span>
                  </div>
                  <Select
                    value={status}
                    onValueChange={(value) => setStatus(value as AddOnCatalogStatus)}
                  >
                    <SelectTrigger className={getSelectTriggerClassName()}>
                      <SelectValue placeholder="Select status" />
                    </SelectTrigger>
                    <SelectContent
                      position="popper"
                      side="bottom"
                      align="start"
                      collisionPadding={12}
                      className={getSelectContentClassName()}
                    >
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="INACTIVE">Archived</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>
          </div>

          <div className="flex shrink-0 flex-wrap items-center justify-end gap-[0.625rem] border-t border-[#e8eef4] px-[1.125rem] py-[0.9375rem]">
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
              onClick={handleCreate}
              disabled={isSaving}
              loading={isSaving}
              loadingLabel="Creating..."
            >
              Create Add-on
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function CreateAddOnModal(props: CreateAddOnModalProps) { return props.open ? <CreateAddOnModalContent {...props} /> : null; }
export default CreateAddOnModal;
