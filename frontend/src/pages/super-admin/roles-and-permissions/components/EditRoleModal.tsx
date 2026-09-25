
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";
import {
  CREATE_ROLE_FIELD_LIMITS,
  sanitizeDisplayName,
  sanitizeRoleDescription,
  sanitizeRoleName,
} from "../createRole.utils";

export interface EditRoleFormValues {
  roleId: number;
  key: string;
  name: string;
  displayName: string;
  description: string;
  isActive: boolean;
  organisationId: number | null;
  /** Existing permission IDs from GET /roles/{id}; edit modal does not change these. */
  permissionIds: number[];
}

interface EditRoleModalProps {
  form: EditRoleFormValues | null;
  isLoading: boolean;
  isSaving: boolean;
  onClose(): void;
  onSave(): void;
  onChange(updater: (previous: EditRoleFormValues) => EditRoleFormValues): void;
}

function getFieldInputClassName(): string {
  return cn(
    "h-[2.75rem] rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-[#dce5ee] focus-visible:ring-0",
  );
}

function EditRoleModal(props: EditRoleModalProps) {
  useEffect(() => {
    if (!props.form) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !props.isSaving) props.onClose();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [props]);

  if (!props.form) return null;

  return (
    <div className="fixed inset-0 z-[10040]">
      <button
        type="button"
        className="absolute inset-0 bg-[rgba(15,23,42,0.26)] backdrop-blur-[0.125rem]"
        aria-label="Close modal"
        onClick={props.onClose}
        disabled={props.isSaving}
      />

      <div className="absolute inset-0 flex items-end justify-center p-0 sm:items-center sm:p-4 sm:py-8">
        <div
          className={cn(
            "flex max-h-[min(100dvh,100%)] w-full max-w-[35rem] flex-col overflow-hidden",
            "rounded-t-[0.875rem] border border-[#e3eaf1] bg-white sm:max-h-[min(90dvh,calc(100dvh-2rem))] sm:rounded-[0.875rem]",
            "shadow-[0_24px_60px_rgba(15,23,42,0.16)]",
          )}
          role="dialog"
          aria-modal="true"
          aria-label="Edit role"
        >
          <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#e8eef4] px-[1.125rem] py-[0.875rem]">
            <div className="min-w-0 truncate pr-2 text-[1.25rem] font-semibold leading-7 text-[#1f2d38]">
              Edit Role
            </div>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={props.onClose}
              disabled={props.isSaving || props.isLoading}
              className="shrink-0"
              aria-label="Close"
            >
              <X size={18} strokeWidth={1.8} aria-hidden="true" />
            </Button>
          </div>

          {props.isLoading ? (
            <div className="flex min-h-[15rem] flex-1 items-center justify-center px-[1.125rem] py-10">
              <div className="flex items-center gap-2 text-[#667483]">
                <ContentLoader variant="inline" size="md" />
                <span className="text-sm">Loading role details...</span>
              </div>
            </div>
          ) : (
            <div
              className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-[1.125rem] py-[1.125rem]"
              style={{ WebkitOverflowScrolling: "touch" }}
            >
              <div className="space-y-4">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div className="min-w-0">
                    <label className="mb-1 block text-[0.6875rem] font-medium text-[#667483]">
                      Name <span className="text-[#ef4444]">*</span>
                    </label>
                    <Input
                      value={props.form.name}
                      onChange={(event) =>
                        props.onChange((previous) => ({
                          ...previous,
                          name: sanitizeRoleName(event.target.value),
                        }))
                      }
                      maxLength={CREATE_ROLE_FIELD_LIMITS.roleName}
                      className={getFieldInputClassName()}
                    />
                    <p className="mt-1.5 text-[0.6875rem] text-[#a0acb8]">
                      Letters, numbers, and underscores only ({props.form.name.trim().length}/
                      {CREATE_ROLE_FIELD_LIMITS.roleName})
                    </p>
                  </div>

                  <div className="min-w-0">
                    <label className="mb-1 block text-[0.6875rem] font-medium text-[#667483]">
                      Display Name <span className="text-[#ef4444]">*</span>
                    </label>
                    <Input
                      value={props.form.displayName}
                      onChange={(event) =>
                        props.onChange((previous) => ({
                          ...previous,
                          displayName: sanitizeDisplayName(event.target.value),
                        }))
                      }
                      maxLength={CREATE_ROLE_FIELD_LIMITS.displayName}
                      className={getFieldInputClassName()}
                    />
                    <p className="mt-1.5 text-[0.6875rem] text-[#a0acb8]">
                      Shown in the UI ({props.form.displayName.length}/
                      {CREATE_ROLE_FIELD_LIMITS.displayName})
                    </p>
                  </div>
                </div>

                <div className="min-w-0">
                  <label className="mb-1 block text-[0.6875rem] font-medium text-[#667483]">
                    Description
                  </label>
                  <Textarea
                    value={props.form.description}
                    onChange={(event) =>
                      props.onChange((previous) => ({
                        ...previous,
                        description: sanitizeRoleDescription(event.target.value),
                      }))
                    }
                    maxLength={CREATE_ROLE_FIELD_LIMITS.description}
                    className="min-h-[6.125rem] resize-none rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem] py-3 text-[0.8125rem] text-[#2b3946] placeholder:text-[#97a4b0] focus-visible:border-[#dce5ee] focus-visible:ring-0"
                  />
                  <p className="mt-1.5 text-[0.6875rem] text-[#a0acb8]">
                    {props.form.description.length}/{CREATE_ROLE_FIELD_LIMITS.description}
                  </p>
                </div>

                <div className="flex items-center justify-between rounded-[0.75rem] border border-[#e4ebf3] bg-[#fbfdff] px-3 py-2.5">
                  <div className="text-[0.875rem] font-medium text-[#2f3d4b]">Role Active</div>
                  <Switch
                    checked={props.form.isActive}
                    onCheckedChange={(value) =>
                      props.onChange((previous) => ({
                        ...previous,
                        isActive: value,
                      }))
                    }
                    className="h-5 w-8"
                    onClassName="bg-[#435564]"
                    offClassName="bg-[#dfe5ec]"
                  />
                </div>
              </div>
            </div>
          )}

          <div className="flex shrink-0 flex-wrap items-center justify-end gap-[0.625rem] border-t border-[#e8eef4] px-[1.125rem] py-[0.9375rem]">
            <Button
              type="button"
              variant="secondary"
              size="lg"
              className="min-w-[5.1875rem]"
              onClick={props.onClose}
              disabled={props.isSaving || props.isLoading}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="primary"
              size="lg"
              className="min-w-[5.1875rem]"
              onClick={props.onSave}
              disabled={props.isSaving || props.isLoading}
              loading={props.isSaving}
              loadingLabel="Saving..."
            >
              Save
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default EditRoleModal;
