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
import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";
import { getApiErrorMessage } from "@/utils/apiError";
import { useUpdateFeatureCatalogEntryMutation } from "@/store/api/superAdminApi";
import type { FeatureCatalogRow } from "../catalog/components/FeatureCatalogTable";
import {
  EDIT_FEATURE_LIMITS,
  EDIT_FEATURE_SCOPE_OPTIONS,
  EDIT_FEATURE_TYPE_OPTIONS,
  sanitizeFeatureDescription,
  sanitizeFeatureName,
  validateEditFeatureForm,
} from "../editFeature.utils";

interface EditFeatureModalProps {
  row: FeatureCatalogRow | null;
  onClose(): void;
  onUpdated(): void;
}

function getFieldInputClassName(): string {
  return cn(
    "h-[2.75rem] rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-[#dce5ee] focus-visible:ring-0",
  );
}

function getSelectTriggerClassName(): string {
  return cn(
    "h-[2.75rem] w-full min-w-0 rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] [&>span]:truncate",
  );
}

function getSelectContentClassName(): string {
  return cn(
    "z-[10050] rounded-[0.875rem] border border-[#dce5ee] bg-white p-1",
    "shadow-[0_12px_28px_rgba(15,23,42,0.08)]",
  );
}

function EditFeatureModalContent(props: EditFeatureModalProps) {
  const [updateFeatureCatalogEntry, { isLoading: isUpdating }] =
    useUpdateFeatureCatalogEntryMutation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [form, setForm] = useState({
    key: "",
    name: "",
    description: "",
    scope: "Tenant",
    type: "Core",
    defaultEnabled: false,
  });

  const draftKey = props.row?.keyName;
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (props.row && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setForm({
      key: props.row.keyName,
      name: props.row.name,
      description: props.row.description || "",
      scope: props.row.scope,
      type: props.row.type || "Core",
      defaultEnabled: props.row.globalDefault,
    });
    setToastMessage(null);
  }

  useEffect(() => {
    if (!props.row) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") props.onClose();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [props]);

  if (!props.row) return null;

  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  async function handleSubmit() {
    const validationError = validateEditFeatureForm({
      name: form.name,
      description: form.description,
      scope: form.scope,
      type: form.type,
    });
    if (validationError) {
      showToast("error", validationError);
      return;
    }

    try {
      await updateFeatureCatalogEntry({
        key: props.row!.keyName,
        body: {
          key: props.row!.keyName,
          name: form.name.trim(),
          description: form.description.trim(),
          scope: form.scope.trim().toLowerCase(),
          type: form.type.trim().toLowerCase(),
          defaultEnabled: form.defaultEnabled,
        },
      }).unwrap();

      showToast("success", "Feature updated successfully.");
      props.onUpdated();
      window.setTimeout(() => {
        props.onClose();
      }, 700);
    } catch (error) {
      showToast("error", getApiErrorMessage(error));
    }
  }

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
        disabled={isUpdating}
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
          aria-label="Edit feature"
        >
          <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#e8eef4] px-[1.125rem] py-[0.875rem]">
            <div className="min-w-0 truncate pr-2 text-[1.25rem] font-semibold leading-7 text-[#1f2d38]">
              Edit Feature
            </div>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={props.onClose}
              disabled={isUpdating}
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
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="min-w-0 md:col-span-1">
                <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                  Key
                </div>
                <Input
                  value={form.key}
                  readOnly
                  disabled
                  title={form.key}
                  className={cn(getFieldInputClassName(), "truncate bg-[#f8fafc] text-[#667483]")}
                />
              </div>

              <div className="min-w-0 md:col-span-1">
                <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                  Name <span className="text-[#ef4444]">*</span>
                </div>
                <Input
                  value={form.name}
                  maxLength={EDIT_FEATURE_LIMITS.nameMax}
                  placeholder="Enter feature name"
                  title={form.name}
                  onChange={(event) =>
                    setForm((previous) => ({
                      ...previous,
                      name: sanitizeFeatureName(event.target.value),
                    }))
                  }
                  className={cn(getFieldInputClassName(), "truncate")}
                />
              </div>

              <div className="min-w-0">
                <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                  Scope <span className="text-[#ef4444]">*</span>
                </div>
                <Select
                  value={form.scope}
                  onValueChange={(value) =>
                    setForm((previous) => ({ ...previous, scope: value }))
                  }
                >
                  <SelectTrigger className={getSelectTriggerClassName()}>
                    <SelectValue placeholder="Select scope" />
                  </SelectTrigger>
                  <SelectContent
                    position="popper"
                    side="bottom"
                    align="start"
                    collisionPadding={12}
                    className={getSelectContentClassName()}
                  >
                    {EDIT_FEATURE_SCOPE_OPTIONS.map((option) => (
                      <SelectItem key={option} value={option}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="min-w-0">
                <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                  Type <span className="text-[#ef4444]">*</span>
                </div>
                <Select
                  value={form.type}
                  onValueChange={(value) =>
                    setForm((previous) => ({ ...previous, type: value }))
                  }
                >
                  <SelectTrigger className={getSelectTriggerClassName()}>
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent
                    position="popper"
                    side="bottom"
                    align="start"
                    collisionPadding={12}
                    className={getSelectContentClassName()}
                  >
                    {EDIT_FEATURE_TYPE_OPTIONS.map((option) => (
                      <SelectItem key={option} value={option}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="min-w-0 md:col-span-2">
                <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                  Description
                </div>
                <Textarea
                  value={form.description}
                  maxLength={EDIT_FEATURE_LIMITS.descriptionMax}
                  placeholder="Optional description"
                  onChange={(event) =>
                    setForm((previous) => ({
                      ...previous,
                      description: sanitizeFeatureDescription(event.target.value),
                    }))
                  }
                  className="min-h-[5.75rem] resize-none rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem] py-3 text-[0.8125rem] text-[#2b3946] placeholder:text-[#97a4b0] focus-visible:border-[#dce5ee] focus-visible:ring-0"
                />
              </div>

              <div className="flex min-w-0 items-center gap-3 md:col-span-2">
                <span className="text-[0.8125rem] font-medium text-[#334155]">Default Enabled</span>
                <Switch
                  checked={form.defaultEnabled}
                  onCheckedChange={(next) =>
                    setForm((previous) => ({ ...previous, defaultEnabled: next }))
                  }
                  onClassName="bg-[#435564]"
                  offClassName="bg-[#dce5ee]"
                />
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
              disabled={isUpdating}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="primary"
              size="lg"
              className="min-w-[7.125rem]"
              onClick={handleSubmit}
              disabled={isUpdating}
              loading={isUpdating}
              loadingLabel="Updating..."
            >
              Update Feature
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function EditFeatureModal(props: EditFeatureModalProps) { return props.row ? <EditFeatureModalContent key={props.row?.keyName} {...props} /> : null; }
export default EditFeatureModal;
