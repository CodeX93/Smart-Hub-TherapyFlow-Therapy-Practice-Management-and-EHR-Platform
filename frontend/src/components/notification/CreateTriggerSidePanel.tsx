import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft } from "lucide-react";
import { Button } from "../ui/button";
import { Switch } from "../ui/switch";
import CustomInput from "../form/CustomInput";
import CustomSelect, { type CustomSelectOption } from "../form/CustomSelect";
import CustomTextarea from "../form/CustomTextarea";
import {
  ENTITY_TYPE_FALLBACK_KEYS,
  ENTITY_TYPE_OPTIONS,
  EVENT_TYPE_FALLBACK_KEYS,
  EVENT_TYPE_OPTIONS,
  PRIORITY_OPTIONS,
} from "./notification.static";
import {
  useCreateSuperAdminNotificationTriggerMutation,
  useGetSuperAdminNotificationTriggersMetadataQuery,
  useUpdateSuperAdminNotificationTriggerMutation,
  type SuperAdminNotificationTrigger,
} from "@/store/api/superAdminApi";
import {
  NOTIFICATION_TRIGGER_LIMITS,
  notificationTriggerSchema,
  type NotificationTriggerSchema,
} from "@/schemas/notification.schema";
import { getApiErrorMessage } from "@/utils/apiError";

interface CreateTriggerSidePanelProps {
  isOpen: boolean;
  onClose: () => void;
  trigger?: SuperAdminNotificationTrigger | null;
  onSaved?: (message: string) => void;
}

function parseNumber(value: string): number {
  const parsed = Number.parseInt(value.trim(), 10);
  return Number.isFinite(parsed) ? parsed : 0;
}

function prettyJson(value: string): string {
  if (!value.trim()) return "{\n}";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function buildEventOptions(values: string[]): CustomSelectOption[] {
  return values.map((value) => {
    const fallback = EVENT_TYPE_OPTIONS.find((item) => item.value === value);
    return {
      value,
      label:
        fallback?.label ??
        value
          .split("_")
          .filter(Boolean)
          .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
          .join(" "),
      group: fallback?.group,
    };
  });
}

function buildEntityOptions(values: string[]): CustomSelectOption[] {
  return values.map((value) => {
    const fallback = ENTITY_TYPE_OPTIONS.find((item) => item.value === value);
    return {
      value,
      label:
        fallback?.label ??
        value
          .toLowerCase()
          .split("_")
          .filter(Boolean)
          .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
          .join(" "),
    };
  });
}

const CreateTriggerSidePanel = ({
  isOpen,
  onClose,
  trigger,
  onSaved,
}: CreateTriggerSidePanelProps) => {
  const [submitError, setSubmitError] = useState<string | null>(null);
  const isEditMode = Boolean(trigger);
  const {
    data: metadata,
    isError: isMetadataError,
  } = useGetSuperAdminNotificationTriggersMetadataQuery(undefined, {
    skip: !isOpen,
  });
  const [createTrigger, { isLoading: isCreating }] =
    useCreateSuperAdminNotificationTriggerMutation();
  const [updateTrigger, { isLoading: isUpdating }] =
    useUpdateSuperAdminNotificationTriggerMutation();

  const {
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<NotificationTriggerSchema>({
    resolver: zodResolver(notificationTriggerSchema),
    defaultValues: {
      name: "",
      description: "",
      eventType: "",
      entityType: "CLIENT",
      conditionRules: "{\n}",
      recipientRules: "{\n}",
      priority: "HIGH",
      isScheduled: false,
      scheduleOffsetMinutes: "0",
      batchWindowMinutes: "5",
      maxBatchSize: "10",
      isActive: true,
    },
  });

  useEffect(() => {
    if (!isOpen) return;

    reset({
      name: trigger?.name ?? "",
      description: trigger?.description ?? "",
      eventType: trigger?.eventType ?? "",
      entityType: trigger?.entityType ?? "CLIENT",
      conditionRules: prettyJson(trigger?.conditionRules ?? "{}"),
      recipientRules: prettyJson(trigger?.recipientRules ?? "{}"),
      priority: trigger?.priority ?? "HIGH",
      isScheduled: trigger?.isScheduled ?? false,
      scheduleOffsetMinutes: String(trigger?.scheduleOffsetMinutes ?? 0),
      batchWindowMinutes: String(trigger?.batchWindowMinutes ?? 5),
      maxBatchSize: String(trigger?.maxBatchSize ?? 10),
      isActive: trigger?.isActive ?? true,
    });
    setSubmitError(null);
  }, [isOpen, reset, trigger]);

  const eventOptions = useMemo(() => {
    const values =
      !isMetadataError && metadata?.eventTypes?.length
        ? metadata.eventTypes
        : EVENT_TYPE_FALLBACK_KEYS;
    return buildEventOptions(values);
  }, [isMetadataError, metadata?.eventTypes]);

  const entityOptions = useMemo(() => {
    const values =
      !isMetadataError && metadata?.entityTypes?.length
        ? metadata.entityTypes
        : ENTITY_TYPE_FALLBACK_KEYS;
    return buildEntityOptions(values);
  }, [isMetadataError, metadata?.entityTypes]);

  async function onSubmit(data: NotificationTriggerSchema) {
    setSubmitError(null);
    const payload = {
      name: data.name.trim(),
      description: data.description?.trim() ?? "",
      eventType: data.eventType.trim(),
      entityType: data.entityType?.trim() ? data.entityType.trim() : null,
      conditionRules: data.conditionRules,
      recipientRules: data.recipientRules,
      priority: data.priority.trim(),
      isScheduled: data.isScheduled,
      scheduleOffsetMinutes: parseNumber(data.scheduleOffsetMinutes),
      batchWindowMinutes: parseNumber(data.batchWindowMinutes),
      maxBatchSize: parseNumber(data.maxBatchSize),
      isActive: data.isActive,
    };

    try {
      if (trigger) {
        await updateTrigger({
          id: trigger.id,
          body: payload,
        }).unwrap();
        onSaved?.("Trigger updated successfully.");
      } else {
        await createTrigger(payload).unwrap();
        onSaved?.("Trigger created successfully.");
      }
      onClose();
    } catch (error) {
      setSubmitError(getApiErrorMessage(error));
    }
  }

  if (!isOpen) return null;

  const isSubmitting = isCreating || isUpdating;

  return (
    <>
      <div
        className="fixed inset-0 z-50 bg-black/20 transition-opacity animate-in fade-in"
        onClick={onClose}
      />

      <div className="fixed top-0 right-0 z-50 flex h-full w-full flex-col bg-white shadow-2xl animate-in slide-in-from-right duration-300 md:w-170">
        <div className="shrink-0 px-6 pt-5">
          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="rounded-full p-1 text-(--text-neutral-600) transition-colors hover:bg-(--neutral-100) cursor-pointer"
            >
              <ArrowLeft size={20} />
            </button>
            <h2 className="text-lg font-semibold text-(--text-primary-dark)">
              {isEditMode ? "Edit Notification Trigger" : "Create Notification Trigger"}
            </h2>
          </div>
        </div>

        <form
          id="trigger-form"
          onSubmit={handleSubmit(onSubmit)}
          className="flex-1 space-y-4 overflow-y-auto p-6"
        >
          <CustomInput
            label="Trigger Name"
            required
            value={watch("name")}
            onChange={(event) => setValue("name", event.target.value, { shouldValidate: true })}
            maxLength={NOTIFICATION_TRIGGER_LIMITS.name}
            hint={`${watch("name").length}/${NOTIFICATION_TRIGGER_LIMITS.name}`}
          />
          {errors.name ? <span className="text-sm text-red-500">{errors.name.message}</span> : null}

          <div className="h-2" aria-hidden="true" />

          <CustomTextarea
            label="Description"
            value={watch("description")}
            onChange={(event) =>
              setValue("description", event.target.value, { shouldValidate: true })
            }
            className="min-h-28"
            maxLength={NOTIFICATION_TRIGGER_LIMITS.description}
            hint={`${watch("description")?.length ?? 0}/${NOTIFICATION_TRIGGER_LIMITS.description}`}
          />

          <CustomSelect
            label="Event Type"
            required
            value={watch("eventType")}
            onChange={(value) => setValue("eventType", value, { shouldValidate: true })}
            options={eventOptions}
            isGrouped
            placeholder="Search event type..."
            contentClassName="z-[10060]"
          />
          {errors.eventType ? (
            <span className="text-sm text-red-500">{errors.eventType.message}</span>
          ) : null}

          <CustomSelect
            label="Entity Type"
            value={watch("entityType")}
            onChange={(value) => setValue("entityType", value, { shouldValidate: true })}
            options={entityOptions}
            placeholder="Search entity type..."
            contentClassName="z-[10060]"
          />

          <CustomSelect
            label="Priority"
            required
            value={watch("priority")}
            onChange={(value) => setValue("priority", value, { shouldValidate: true })}
            options={PRIORITY_OPTIONS}
            isSearch={false}
            contentClassName="z-[10060]"
          />
          {errors.priority ? (
            <span className="text-sm text-red-500">{errors.priority.message}</span>
          ) : null}

          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div>
              <CustomInput
                label="Schedule Offset Minutes"
                value={watch("scheduleOffsetMinutes")}
                onChange={(event) =>
                  setValue("scheduleOffsetMinutes", event.target.value, {
                    shouldValidate: true,
                  })
                }
                inputMode="numeric"
                digitsOnly
                maxLength={5}
              />
              {errors.scheduleOffsetMinutes ? (
                <span className="text-sm text-red-500">
                  {errors.scheduleOffsetMinutes.message}
                </span>
              ) : null}
            </div>
            <div>
              <CustomInput
                label="Batch Window Minutes"
                value={watch("batchWindowMinutes")}
                onChange={(event) =>
                  setValue("batchWindowMinutes", event.target.value, {
                    shouldValidate: true,
                  })
                }
                inputMode="numeric"
                digitsOnly
                maxLength={5}
              />
              {errors.batchWindowMinutes ? (
                <span className="text-sm text-red-500">
                  {errors.batchWindowMinutes.message}
                </span>
              ) : null}
            </div>
            <div>
              <CustomInput
                label="Max Batch Size"
                value={watch("maxBatchSize")}
                onChange={(event) =>
                  setValue("maxBatchSize", event.target.value, {
                    shouldValidate: true,
                  })
                }
                inputMode="numeric"
                digitsOnly
                maxLength={5}
              />
              {errors.maxBatchSize ? (
                <span className="text-sm text-red-500">{errors.maxBatchSize.message}</span>
              ) : null}
            </div>
          </div>

          <CustomTextarea
            label="Condition Rules JSON"
            required
            value={watch("conditionRules")}
            onChange={(event) =>
              setValue("conditionRules", event.target.value, { shouldValidate: true })
            }
            className="min-h-44 font-mono text-sm"
            maxLength={NOTIFICATION_TRIGGER_LIMITS.jsonRules}
            hint={`${watch("conditionRules").length}/${NOTIFICATION_TRIGGER_LIMITS.jsonRules}`}
          />
          {errors.conditionRules ? (
            <span className="text-sm text-red-500">{errors.conditionRules.message}</span>
          ) : null}

          <CustomTextarea
            label="Recipient Rules JSON"
            required
            value={watch("recipientRules")}
            onChange={(event) =>
              setValue("recipientRules", event.target.value, { shouldValidate: true })
            }
            className="min-h-44 font-mono text-sm"
            maxLength={NOTIFICATION_TRIGGER_LIMITS.jsonRules}
            hint={`${watch("recipientRules").length}/${NOTIFICATION_TRIGGER_LIMITS.jsonRules}`}
          />
          {errors.recipientRules ? (
            <span className="text-sm text-red-500">{errors.recipientRules.message}</span>
          ) : null}

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="rounded-xl border border-(--neutral-100) bg-white px-4 py-3">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-medium text-(--text-primary-dark)">Scheduled</p>
                  <p className="text-xs text-(--text-neutral-600)">
                    Toggle delayed notification scheduling.
                  </p>
                </div>
                <Switch
                  checked={watch("isScheduled")}
                  onCheckedChange={(checked) =>
                    setValue("isScheduled", checked, { shouldValidate: true })
                  }
                />
              </div>
            </div>

            <div className="rounded-xl border border-(--neutral-100) bg-white px-4 py-3">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-medium text-(--text-primary-dark)">Status</p>
                  <p className="text-xs text-(--text-neutral-600)">
                    Control whether the trigger is active.
                  </p>
                </div>
                <Switch
                  checked={watch("isActive")}
                  onCheckedChange={(checked) =>
                    setValue("isActive", checked, { shouldValidate: true })
                  }
                />
              </div>
            </div>
          </div>

          {submitError ? (
            <div className="rounded-xl border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
              {submitError}
            </div>
          ) : null}
        </form>

        <div className="shrink-0 border-t border-(--neutral-100) p-6 pt-3">
          <div className="flex justify-end gap-3">
            <Button
              variant="outline"
              onClick={onClose}
              className="h-11 rounded-full border-(--neutral-200) px-6 text-(--text-primary-dark) hover:bg-(--neutral-50) hover:text-(--text-primary-dark) cursor-pointer"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              form="trigger-form"
              disabled={isSubmitting}
              loading={isSubmitting}
              loadingLabel={isEditMode ? "Saving..." : "Creating..."}
              className="h-11 rounded-full bg-(--bg-primary-dark) px-6 text-white shadow-none hover:bg-(--bg-primary-dark)/90 cursor-pointer"
            >
              {isEditMode ? "Save Trigger" : "Create Trigger"}
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};

export default CreateTriggerSidePanel;
