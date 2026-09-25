
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import CustomSelect from "@/components/form/CustomSelect";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import Toast from "@/components/shared/Toast";
import {
  useGetPortalMeQuery,
  useGetPortalTimezoneQuery,
  useUpdatePortalTimezoneMutation,
} from "@/store/api/portalApi";
import { useGetMyTimezonesQuery } from "@/store/api/userProfile.api";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  fetchPortalAvatarPreviewUrl,
  revokePortalAvatarPreviewUrl,
} from "@/utils/portalDocumentAssets";
import { TIME_ZONE_OPTIONS } from "@/utils/functions/timezone";

const clientProfileSchema = z.object({
  timezone: z.string().min(1, "Timezone is required"),
});

type ClientProfileFormValues = z.infer<typeof clientProfileSchema>;

function getInitials(value: string): string {
  const normalized = value.trim();
  if (!normalized) return "CL";

  const parts = normalized.split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
  }

  return normalized.slice(0, 2).toUpperCase();
}

function ReadOnlyField({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="min-w-0 rounded-xl border border-(--neutral-100) bg-(--bg-primary-light)/60 px-4 py-3.5">
      <p className="text-xs font-medium text-(--text-neutral-500)">{label}</p>
      <p
        className="mt-1 truncate text-sm font-medium text-(--text-primary-dark)"
        title={value}
      >
        {value}
      </p>
    </div>
  );
}

const ClientMyProfile = () => {
  const avatarPreviewRef = useRef<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(null);
  const {
    data: profile,
    isLoading: isProfileLoading,
  } = useGetPortalMeQuery();
  const { data: timezoneData, isLoading: isTimezoneLoading } =
    useGetPortalTimezoneQuery();
  const { data: timezoneOptionsResponse, isLoading: isTimezoneOptionsLoading } =
    useGetMyTimezonesQuery();
  const [updateTimezone, { isLoading: isSavingTimezone }] =
    useUpdatePortalTimezoneMutation();

  const form = useForm<ClientProfileFormValues>({
    resolver: zodResolver(clientProfileSchema),
    defaultValues: {
      timezone: "",
    },
  });

  const timezoneOptions = useMemo(() => {
    const apiTimezones = timezoneOptionsResponse?.timezones ?? [];
    if (apiTimezones.length > 0) {
      return apiTimezones.map((timezone) => ({
        label: timezone,
        value: timezone,
      }));
    }

    return TIME_ZONE_OPTIONS.map((option) => ({
      label: option.label,
      value: option.value,
    }));
  }, [timezoneOptionsResponse?.timezones]);

  useEffect(() => {
    const resolvedTimezone =
      timezoneData?.timezone || profile?.timezone || "";
    if (resolvedTimezone) {
      form.reset({ timezone: resolvedTimezone });
    }
  }, [form, profile?.timezone, timezoneData?.timezone]);

  useEffect(() => {
    let cancelled = false;

    const loadAvatarPreview = async () => {
      if (avatarPreviewRef.current) {
        revokePortalAvatarPreviewUrl(avatarPreviewRef.current);
        avatarPreviewRef.current = null;
      }

      if (!profile?.avatarUrl) {
        if (!cancelled) setAvatarPreviewUrl(null);
        return;
      }

      try {
        const previewUrl = await fetchPortalAvatarPreviewUrl(profile.avatarUrl);
        if (cancelled) {
          revokePortalAvatarPreviewUrl(previewUrl);
          return;
        }

        avatarPreviewRef.current = previewUrl;
        setAvatarPreviewUrl(previewUrl);
      } catch {
        if (!cancelled) setAvatarPreviewUrl(null);
      }
    };

    void loadAvatarPreview();

    return () => {
      cancelled = true;
      if (avatarPreviewRef.current) {
        revokePortalAvatarPreviewUrl(avatarPreviewRef.current);
        avatarPreviewRef.current = null;
      }
    };
  }, [profile?.avatarUrl]);

  const onSubmit = async (values: ClientProfileFormValues) => {
    try {
      setToastMessage(null);
      await updateTimezone({ timezone: values.timezone }).unwrap();
      setToastType("success");
      setToastMessage("Timezone updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const isLoading =
    isProfileLoading || isTimezoneLoading || isTimezoneOptionsLoading;
  const displayName = profile?.fullName?.trim() || "Client";
  const initials = getInitials(displayName);

  return (
    <div className="mx-auto w-full max-w-4xl px-1 sm:px-0">
      <div className="relative overflow-hidden rounded-2xl border border-(--neutral-100) bg-white shadow-xs">
        {isLoading ? (
          <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center bg-white/80">
            <div className="flex items-center gap-2">
              <ContentLoader variant="inline" size="md" />
              <span className="text-sm text-(--text-neutral-600)">
                Loading profile...
              </span>
            </div>
          </div>
        ) : null}

        <div
          className={`space-y-8 px-4 py-6 sm:px-6 sm:py-8 md:px-8 ${
            isLoading ? "invisible" : ""
          }`}
          aria-hidden={isLoading}
        >
          <section className="rounded-2xl border border-(--neutral-100) bg-(--bg-primary-light)/35 p-4 sm:p-5 md:p-6">
            <div className="flex min-h-[7.5rem] items-center sm:min-h-[6.5rem]">
              <div className="flex min-w-0 items-center gap-4">
                <Avatar className="h-20 w-20 shrink-0 border border-(--neutral-100) bg-white sm:h-24 sm:w-24">
                  <AvatarImage
                    className="object-cover"
                    src={avatarPreviewUrl || undefined}
                  />
                  <AvatarFallback className="bg-(--bg-primary-100) text-base font-semibold text-(--text-primary-dark) sm:text-lg">
                    {initials}
                  </AvatarFallback>
                </Avatar>
                <div className="min-w-0">
                  <p className="truncate text-lg font-semibold text-(--text-primary-dark) sm:text-xl">
                    {displayName}
                  </p>
                  <p className="mt-1 truncate text-sm text-(--text-neutral-600)">
                    {profile?.email || "-"}
                  </p>
                </div>
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <div>
              <h2 className="text-base font-semibold text-(--text-primary-dark)">
                Account Information
              </h2>
              <p className="mt-1 text-sm text-(--text-neutral-600)">
                These details are read-only.
              </p>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4">
              <ReadOnlyField
                label="Full Name"
                value={profile?.fullName || "-"}
              />
              <ReadOnlyField
                label="Client ID"
                value={profile?.clientId || "-"}
              />
              <ReadOnlyField label="Email" value={profile?.email || "-"} />
              <ReadOnlyField label="Phone" value={profile?.phone || "-"} />
            </div>
          </section>

          <section className="space-y-4 border-t border-(--neutral-100) pt-6">
            <div>
              <h2 className="text-base font-semibold text-(--text-primary-dark)">
                Preferences
              </h2>
              <p className="mt-1 text-sm text-(--text-neutral-600)">
                Choose the timezone used for your appointments.
              </p>
            </div>

            <Form {...form}>
              <form
                onSubmit={form.handleSubmit(onSubmit)}
                className="space-y-5"
              >
                <div className="w-full min-w-0 max-w-full">
                  <CustomSelect
                    control={form.control}
                    name="timezone"
                    label="Timezone"
                    options={timezoneOptions}
                    required
                    isSearch
                    placeholder="Search timezone..."
                    className="w-full"
                    contentClassName="w-[min(100vw-2rem,var(--radix-popover-trigger-width))] max-w-[calc(100vw-2rem)]"
                  />
                </div>

                <div className="flex justify-stretch sm:justify-end">
                  <Button
                    type="submit"
                    disabled={isSavingTimezone || isLoading}
                    className="h-11.5 w-full rounded-full bg-(--bg-primary-dark) px-8 text-white sm:w-auto"
                    loading={isSavingTimezone}
                    loadingLabel="Saving..."
                  >
                    Save timezone
                  </Button>
                </div>
              </form>
            </Form>
          </section>
        </div>
      </div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

export default ClientMyProfile;
