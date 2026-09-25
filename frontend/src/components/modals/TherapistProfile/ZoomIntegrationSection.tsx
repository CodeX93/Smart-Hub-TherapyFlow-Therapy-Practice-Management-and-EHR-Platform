
import { ContentLoader } from "@/components/shared/ContentLoader";
import Toast from "@/components/shared/Toast";
import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check, ChevronDown, ChevronUp, X } from "lucide-react";
import CustomInput from "../../form/CustomInput";
import { Button } from "../../ui/button";
import { Form } from "../../ui/form";
import {
  zoomIntegrationSchema,
  type ZoomIntegrationFormValues,
} from "@/schemas/general-profile-modal-schemas";
import {
  useGetZoomStatusQuery,
  useGetCurrentUserQuery,
  useSaveZoomCredentialsMutation,
  useDeleteZoomCredentialsMutation,
  useTestZoomCredentialsMutation,
} from "@/store/api/userProfile.api";

type ToastType = "success" | "error" | "info";

const ZoomIntegrationSection: React.FC = () => {
  const [isHowToOpen, setIsHowToOpen] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: ToastType } | null>(null);
  const [isConfirmDeleteOpen, setIsConfirmDeleteOpen] = useState(false);

  const { data: zoomStatus, isLoading: isLoadingStatus } = useGetZoomStatusQuery();
  const { data: currentUser } = useGetCurrentUserQuery();

  const [saveZoomCredentials, { isLoading: isSaving }] = useSaveZoomCredentialsMutation();
  const [deleteZoomCredentials, { isLoading: isDeleting }] = useDeleteZoomCredentialsMutation();
  const [testZoomCredentials, { isLoading: isTesting }] = useTestZoomCredentialsMutation();

  const form = useForm<ZoomIntegrationFormValues>({
    resolver: zodResolver(zoomIntegrationSchema),
    defaultValues: {
      accountId: "",
      clientId: "",
      clientSecret: "",
    },
  });

  // Pre-fill accountId when status loads
  useEffect(() => {
    if (zoomStatus?.configured && zoomStatus.accountId) {
      form.setValue("accountId", zoomStatus.accountId);
    }
  }, [zoomStatus, form]);

  const showToast = (message: string, type: ToastType) => {
    setToast({ message, type });
  };

  const handleSave = async (values: ZoomIntegrationFormValues) => {
    try {
      await saveZoomCredentials({
        accountId: values.accountId,
        clientId: values.clientId,
        clientSecret: values.clientSecret,
      }).unwrap();
      showToast("Zoom integration saved successfully!", "success");
      form.reset({ accountId: values.accountId, clientId: "", clientSecret: "" });
    } catch {
      showToast("Failed to save Zoom credentials. Please try again.", "error");
    }
  };

  const handleTestConnection = async () => {
    try {
      const result = await testZoomCredentials().unwrap();
      if (result.configured) {
        showToast("Zoom connection is working!", "success");
      } else {
        showToast("Connection test failed — credentials may be invalid.", "error");
      }
    } catch {
      showToast("Connection test failed. Check your credentials and try again.", "error");
    }
  };

  const handleConfirmDelete = async () => {
    try {
      await deleteZoomCredentials().unwrap();
      showToast("Zoom integration removed successfully.", "success");
      setIsConfirmDeleteOpen(false);
      form.reset({ accountId: "", clientId: "", clientSecret: "" });
    } catch {
      showToast("Failed to remove Zoom credentials. Please try again.", "error");
      setIsConfirmDeleteOpen(false);
    }
  };

  const isConfigured = zoomStatus?.configured ?? false;

  return (
    <div className="flex flex-col gap-6 h-full pt-6 md:px-1 relative">
      {toast ? (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      ) : null}

      {/* Connection Status Banner */}
      {isLoadingStatus ? (
        <div className="flex items-center gap-3 p-4 bg-(--bg-primary-light) rounded-2xl border border-(--neutral-100)">
          <ContentLoader variant="inline" size="md" />
          <span className="text-sm text-(--text-neutral-600)">Checking connection status...</span>
        </div>
      ) : isConfigured ? (
        <div className="flex flex-col gap-4 rounded-2xl border border-(--neutral-100) bg-(--bg-primary-light) p-4">
          <div className="min-w-0 space-y-1">
            <div className="flex items-center gap-2 text-(--success-green)">
              <Check size={20} className="shrink-0" />
              <span className="font-medium">Connected</span>
            </div>
            {(zoomStatus?.accountId || currentUser?.fullName) && (
              <p className="text-xs text-(--text-neutral-500)">
                Account: {zoomStatus?.accountId || currentUser?.email}
                {zoomStatus?.lastUpdatedAt && (
                  <> · Updated {new Date(zoomStatus.lastUpdatedAt).toLocaleDateString("en-US", { month: "short", day: "2-digit", year: "numeric" })}</>
                )}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={handleTestConnection}
              disabled={isTesting}
              loading={isTesting}
              loadingLabel="Testing..."
              className="h-10 w-full shrink-0 rounded-full px-6 font-semibold cursor-pointer sm:w-auto"
            >
              Test Connection
            </Button>
            <Button
              type="button"
              onClick={() => setIsConfirmDeleteOpen(true)}
              className="flex h-10 w-full shrink-0 items-center justify-center gap-2 rounded-full bg-(--status-denied) px-6 font-semibold text-white shadow-none hover:bg-(--status-denied)/90 cursor-pointer sm:w-auto"
            >
              <X className="size-4 text-white" />
              Remove Integration
            </Button>
          </div>
        </div>
      ) : (
        <div className="flex items-center gap-3 p-4 bg-amber-50 rounded-2xl border border-amber-200">
          <span className="h-2.5 w-2.5 rounded-full bg-amber-400 shrink-0" />
          <span className="text-sm font-medium text-amber-700">Not connected. Enter your credentials below to configure Zoom.</span>
        </div>
      )}

      {/* Confirm Delete Dialog */}
      {isConfirmDeleteOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50" onClick={() => setIsConfirmDeleteOpen(false)} />
          <div className="relative bg-white rounded-2xl shadow-xl w-full max-w-sm p-6 flex flex-col gap-5 z-[60] animate-in fade-in zoom-in duration-200">
            <h3 className="text-base font-semibold text-(--text-primary-dark)">Remove Zoom Integration?</h3>
            <p className="text-sm text-(--text-neutral-600)">Your Zoom credentials will be permanently deleted. You'll need to re-enter them to reconnect.</p>
            <div className="flex gap-3 justify-end">
              <Button
                variant="outline"
                onClick={() => setIsConfirmDeleteOpen(false)}
                className="rounded-full px-6 h-10 cursor-pointer"
              >
                Cancel
              </Button>
              <Button
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                loading={isDeleting}
                loadingLabel="Removing..."
                className="rounded-full px-6 h-10 bg-(--status-denied) text-white hover:bg-(--status-denied)/90 cursor-pointer shadow-none"
              >
                Remove
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* How-to Accordion */}
      <div className="flex flex-col gap-3">
        <button
          type="button"
          onClick={() => setIsHowToOpen(!isHowToOpen)}
          className="flex items-center justify-between w-full text-left cursor-pointer"
        >
          <span className="text-(--neutral-950) text-[1rem] font-medium leading-6">
            How to get your Zoom credentials:
          </span>
          {isHowToOpen ? <ChevronUp size={24} /> : <ChevronDown size={24} />}
        </button>

        <div
          className={`grid transition-all duration-300 ease-in-out ${
            isHowToOpen
              ? "grid-rows-[1fr] opacity-100 mt-2"
              : "grid-rows-[0fr] opacity-0"
          }`}
        >
          <div className="overflow-hidden">
            <div className="flex flex-col gap-2 text-(--text-secondary-light) text-[0.875rem] leading-6 list-decimal pl-4">
              <p>
                1. Go to{" "}
                <a
                  href="https://marketplace.zoom.us/develop/apps/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-(--neutral-950) underline underline-offset-2 hover:opacity-80"
                >
                  https://marketplace.zoom.us/develop/apps/
                </a>
              </p>
              <p>2. Sign in with your Zoom account</p>
              <p>3. At the bottom left, click "Developer"</p>
              <p>4. Expand the "Build App" dropdown and create a new app</p>
              <p>5. Choose "Server-to-Server OAuth" app type</p>
              <p>6. Fill in app information and create the app</p>
              <p>7. Open Scopes → Add Scopes → Meeting, and add scopes to create, read, update, and delete meetings (for example meeting:write:meeting, meeting:read:meeting, meeting:update:meeting, meeting:delete:meeting — or the matching :admin scopes)</p>
              <p>8. Activate the app if Zoom asks you to after adding scopes</p>
              <p>9. Copy the Account ID, Client ID, and Client Secret from the app credentials page</p>
              <p>10. Paste them below and save</p>
            </div>
          </div>
        </div>
      </div>

      {/* Credentials Form */}
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(handleSave)}
          className="flex flex-col gap-6 h-full"
        >
          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-2 gap-4">
              <CustomInput
                control={form.control}
                label="Zoom Account ID"
                name="accountId"
              />
              <CustomInput
                control={form.control}
                label="Zoom Client ID"
                name="clientId"
              />
            </div>
            <CustomInput
              control={form.control}
              label="Zoom Client Secret"
              name="clientSecret"
              type="password"
            />
          </div>

          <div className="pt-4 flex justify-end h-full items-end pb-6">
            <Button
              type="submit"
              disabled={isSaving}
              loading={isSaving}
              loadingLabel="Saving..."
              className="rounded-full px-10 py-3 h-14 font-semibold text-[1rem] leading-6 cursor-pointer"
            >
              {isConfigured ? (
                "Update Credentials"
              ) : (
                "Save changes"
              )}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
};

export default ZoomIntegrationSection;
