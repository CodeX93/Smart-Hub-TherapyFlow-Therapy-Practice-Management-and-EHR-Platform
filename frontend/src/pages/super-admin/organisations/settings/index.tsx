
import { ContentLoader } from "@/components/shared/ContentLoader";
import { type ReactNode, useMemo, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import SuperAdminHeaderActions from "@/components/shared/SuperAdminHeaderActions";
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
  useGetOrganisationsQuery,
  useGetOrganisationSettingsQuery,
  useUpdateOrganisationSettingsMutation,
  type OrganisationSettings,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  DATA_RESIDENCY_OPTIONS,
  LOCALE_OPTIONS,
  normalizeSelectValue,
  REGION_OPTIONS,
  sanitizeBrandColor,
  sanitizeLogoUrl,
  sanitizeSupportAddress,
  sanitizeSupportEmail,
  TENANT_SETTINGS_LIMITS,
  TIMEZONE_OPTIONS,
  validateTenantSettings,
} from "./tenantSettings.utils";

function getEmptySettings(): OrganisationSettings {
  return {
    timezone: "",
    region: "",
    dataResidency: "",
    locale: "",
    logoUrl: "",
    brandPrimaryColor: "",
    brandSecondaryColor: "",
    brandAccentColor: "",
    supportEmail: "",
    supportAddress: "",
  };
}

function getColorPickerValue(value: string): string {
  const trimmed = value.trim();
  return /^#([0-9a-fA-F]{6})$/.test(trimmed) ? trimmed : "#000000";
}

function normalizeSettings(settings: OrganisationSettings): OrganisationSettings {
  return {
    ...settings,
    timezone: normalizeSelectValue(settings.timezone, TIMEZONE_OPTIONS),
    region: normalizeSelectValue(settings.region, REGION_OPTIONS),
    dataResidency: normalizeSelectValue(settings.dataResidency, DATA_RESIDENCY_OPTIONS),
    locale: normalizeSelectValue(settings.locale, LOCALE_OPTIONS),
  };
}

const inputClassName = cn(
  "h-10 rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] placeholder:text-[#97a4b0]",
  "focus-visible:border-[#dce5ee] focus-visible:ring-0",
);

const selectTriggerClassName = cn(
  "h-10 w-full rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] data-[placeholder]:text-[#97a4b0] [&_svg]:text-[#97a4b0]",
);

function TenantSettings() {
  const { slug } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [values, setValues] = useState<OrganisationSettings>(getEmptySettings);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const organisationIdFromState =
    (location.state as { organisationId?: number | null } | null)?.organisationId ?? null;
  const shouldResolveIdBySlug = organisationIdFromState === null && Boolean(slug);

  const slugLookupFilters = useMemo(
    () => ({
      search: slug ?? "",
      status: "",
      plan: "",
      createdFrom: "",
      createdTo: "",
      region: "",
      dataResidency: "",
      page: 1,
      pageSize: 25,
      sort: "createdAt",
      order: "desc" as const,
      exportData: false,
    }),
    [slug],
  );

  const { data: organisationListData } = useGetOrganisationsQuery(slugLookupFilters, {
    skip: !shouldResolveIdBySlug,
  });

  const resolvedOrganisation = useMemo(() => {
    return organisationListData?.rows.find((row) => row.slug === slug) ?? null;
  }, [organisationListData?.rows, slug]);

  const resolvedOrganisationId = useMemo(() => {
    if (organisationIdFromState) return organisationIdFromState;
    if (!resolvedOrganisation) return null;
    const parsed = Number.parseInt(resolvedOrganisation.id, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }, [organisationIdFromState, resolvedOrganisation]);

  const orgName = useMemo(() => {
    if (resolvedOrganisation?.name) return resolvedOrganisation.name;
    if (!slug) return "Organisation";
    return slug
      .split("-")
      .filter(Boolean)
      .map((part) => part[0]?.toUpperCase() + part.slice(1))
      .join(" ");
  }, [resolvedOrganisation, slug]);

  const {
    currentData: settingsData,
    isLoading: isLoadingSettings,
    isError: isSettingsError,
    error: settingsError,
    refetch: refetchSettings,
  } = useGetOrganisationSettingsQuery(resolvedOrganisationId ?? 0, {
    skip: resolvedOrganisationId === null,
  });
  const [updateOrganisationSettings, { isLoading: isSaving }] =
    useUpdateOrganisationSettingsMutation();

  const [settingsScope, setSettingsScope] = useState(resolvedOrganisationId);
  const [seededOrganisationId, setSeededOrganisationId] = useState<number | null>(null);
  if (settingsScope !== resolvedOrganisationId) {
    setSettingsScope(resolvedOrganisationId);
    setSeededOrganisationId(null);
    setValues(getEmptySettings());
  }
  if (settingsData && (settingsScope !== resolvedOrganisationId || resolvedOrganisationId !== seededOrganisationId)) {
    setSeededOrganisationId(resolvedOrganisationId);
    setValues(normalizeSettings(settingsData));
  }

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (isSettingsError && settingsError !== reportedError) {
    setReportedError(settingsError);
    setToastType("error");
    setToastMessage(getApiErrorMessage(settingsError));
  }

  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  function handleBackToOrganisation() {
    if (!slug) {
      navigate("/super-admin/organisations");
      return;
    }

    navigate("/super-admin/organisations/" + slug, {
      state: {
        organisationId: resolvedOrganisationId,
        organisationSlug: slug,
      },
    });
  }

  function setField<K extends keyof OrganisationSettings>(
    key: K,
    value: OrganisationSettings[K],
  ) {
    setValues((previous) => ({ ...previous, [key]: value }));
  }

  function handleDiscard() {
    setValues(normalizeSettings(settingsData ?? getEmptySettings()));
    handleBackToOrganisation();
  }

  async function handleSave() {
    if (!resolvedOrganisationId) {
      showToast("error", "Organisation id is missing.");
      return;
    }

    const validationError = validateTenantSettings(values);
    if (validationError) {
      showToast("error", validationError);
      return;
    }

    try {
      await updateOrganisationSettings({
        id: resolvedOrganisationId,
        body: {
          ...values,
          timezone: values.timezone.trim(),
          region: values.region.trim(),
          dataResidency: values.dataResidency.trim(),
          locale: values.locale.trim(),
          logoUrl: values.logoUrl.trim(),
          brandPrimaryColor: values.brandPrimaryColor.trim(),
          brandSecondaryColor: values.brandSecondaryColor.trim(),
          brandAccentColor: values.brandAccentColor.trim(),
          supportEmail: values.supportEmail.trim(),
          supportAddress: values.supportAddress.trim(),
        },
      }).unwrap();
      await refetchSettings();
      showToast("success", `Tenant settings updated for ${orgName}.`);
    } catch (error) {
      showToast("error", getApiErrorMessage(error));
    }
  }

  return (
    <div className="relative flex h-full min-h-0 w-full flex-col bg-[#FAFAFB]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      {isSaving ? (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-white/50 backdrop-blur-[0.0625rem]">
          <div className="flex flex-col items-center gap-2 rounded-xl border border-[#e3ebf3] bg-white p-4 shadow-lg">
            <ContentLoader size="xl" />
            <span className="text-sm font-medium text-[#435564]">Saving settings...</span>
          </div>
        </div>
      ) : null}

      <div className="flex shrink-0 flex-col gap-6 pb-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              className="shrink-0"
              onClick={handleBackToOrganisation}
            >
              <ArrowLeft size={14} aria-hidden="true" />
              Back
            </Button>

            <div className="min-w-0 flex flex-1 items-center gap-1.5 overflow-hidden text-[0.8125rem] font-medium text-[#7c8a97]">
              <button
                type="button"
                onClick={() => navigate("/super-admin/dashboard")}
                className="shrink-0 cursor-pointer transition-colors hover:text-[#2b3946] hover:underline"
              >
                Super Admin
              </button>
              <span className="shrink-0">/</span>
              <button
                type="button"
                onClick={() => navigate("/super-admin/organisations")}
                className="shrink-0 cursor-pointer transition-colors hover:text-[#2b3946] hover:underline"
              >
                Organisations
              </button>
              <span className="shrink-0">/</span>
              <button
                type="button"
                onClick={handleBackToOrganisation}
                className="min-w-0 truncate cursor-pointer transition-colors hover:text-[#2b3946] hover:underline"
                title={orgName}
              >
                {orgName}
              </button>
              <span className="shrink-0">/</span>
              <span className="min-w-0 truncate text-[#2b3946]" title="Tenant Settings">
                Tenant Settings
              </span>
            </div>
          </div>

          <SuperAdminHeaderActions />
        </div>

        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-[#1f2d38] text-[1.5rem] font-semibold leading-7">
              Tenant Settings
            </h1>
            <p className="mt-1 text-[#667483] text-[0.8125rem] leading-5">
              Configure timezone, residency, locale, branding, and support details.
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-3">
            <Button
              variant="secondary"
              size="md"
              onClick={handleDiscard}
              disabled={isSaving}
            >
              Discard
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={handleSave}
              disabled={isSaving || isLoadingSettings}
              loading={isSaving}
              loadingLabel="Saving..."
            >
              Save Changes
            </Button>
          </div>
        </div>
      </div>

      <div
        className="min-h-0 flex-1 overflow-y-auto pb-6 pr-1"
        style={{
          scrollBehavior: "smooth",
          scrollbarGutter: "stable",
          overscrollBehavior: "contain",
        }}
      >
        <div className="rounded-[1rem] border border-[#e3ebf3] bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
          {isLoadingSettings ? (
            <div className="mb-4 text-sm text-[#667483]">Loading tenant settings...</div>
          ) : null}

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Field label="Timezone" required>
              <Select
                value={values.timezone || undefined}
                onValueChange={(value) => setField("timezone", value)}
              >
                <SelectTrigger className={selectTriggerClassName}>
                  <SelectValue placeholder="Choose timezone" />
                </SelectTrigger>
                <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
                  {TIMEZONE_OPTIONS.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                    >
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>

            <Field label="Region" required>
              <Select
                value={values.region || undefined}
                onValueChange={(value) => setField("region", value)}
              >
                <SelectTrigger className={selectTriggerClassName}>
                  <SelectValue placeholder="Choose infrastructure region" />
                </SelectTrigger>
                <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
                  {REGION_OPTIONS.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                    >
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>

            <Field label="Data Residency" required>
              <Select
                value={values.dataResidency || undefined}
                onValueChange={(value) => setField("dataResidency", value)}
              >
                <SelectTrigger className={selectTriggerClassName}>
                  <SelectValue placeholder="Choose data residency" />
                </SelectTrigger>
                <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
                  {DATA_RESIDENCY_OPTIONS.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                    >
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>

            <Field label="Locale" required>
              <Select
                value={values.locale || undefined}
                onValueChange={(value) => setField("locale", value)}
              >
                <SelectTrigger className={selectTriggerClassName}>
                  <SelectValue placeholder="Choose locale" />
                </SelectTrigger>
                <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
                  {LOCALE_OPTIONS.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                    >
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>

            <Field label="Logo URL">
              <Input
                className={inputClassName}
                placeholder="https://example.com/logo.png"
                maxLength={TENANT_SETTINGS_LIMITS.logoUrl}
                value={values.logoUrl}
                onChange={(event) => setField("logoUrl", sanitizeLogoUrl(event.target.value))}
              />
            </Field>

            <Field label="Support Email">
              <Input
                className={inputClassName}
                placeholder="support@organization.com"
                type="email"
                inputMode="email"
                autoComplete="email"
                maxLength={TENANT_SETTINGS_LIMITS.supportEmail}
                value={values.supportEmail}
                onChange={(event) =>
                  setField("supportEmail", sanitizeSupportEmail(event.target.value))
                }
              />
            </Field>

            <Field label="Brand Primary Color">
              <div className="flex items-center gap-2">
                <Input
                  type="color"
                  value={getColorPickerValue(values.brandPrimaryColor)}
                  onChange={(event) => setField("brandPrimaryColor", event.target.value)}
                  className="h-10 w-12 p-1"
                />
                <Input
                  className={inputClassName}
                  value={values.brandPrimaryColor}
                  onChange={(event) =>
                    setField("brandPrimaryColor", sanitizeBrandColor(event.target.value))
                  }
                  placeholder="#000000"
                  maxLength={TENANT_SETTINGS_LIMITS.brandColor}
                />
              </div>
            </Field>

            <Field label="Brand Secondary Color">
              <div className="flex items-center gap-2">
                <Input
                  type="color"
                  value={getColorPickerValue(values.brandSecondaryColor)}
                  onChange={(event) => setField("brandSecondaryColor", event.target.value)}
                  className="h-10 w-12 p-1"
                />
                <Input
                  className={inputClassName}
                  value={values.brandSecondaryColor}
                  onChange={(event) =>
                    setField("brandSecondaryColor", sanitizeBrandColor(event.target.value))
                  }
                  placeholder="#000000"
                  maxLength={TENANT_SETTINGS_LIMITS.brandColor}
                />
              </div>
            </Field>

            <Field label="Brand Accent Color">
              <div className="flex items-center gap-2">
                <Input
                  type="color"
                  value={getColorPickerValue(values.brandAccentColor)}
                  onChange={(event) => setField("brandAccentColor", event.target.value)}
                  className="h-10 w-12 p-1"
                />
                <Input
                  className={inputClassName}
                  value={values.brandAccentColor}
                  onChange={(event) =>
                    setField("brandAccentColor", sanitizeBrandColor(event.target.value))
                  }
                  placeholder="#000000"
                  maxLength={TENANT_SETTINGS_LIMITS.brandColor}
                />
              </div>
            </Field>

            <Field label="Support Address" className="md:col-span-2">
              <Textarea
                value={values.supportAddress}
                onChange={(event) =>
                  setField("supportAddress", sanitizeSupportAddress(event.target.value))
                }
                placeholder="Enter support mailing address"
                maxLength={TENANT_SETTINGS_LIMITS.supportAddress}
                className="min-h-[5.75rem] rounded-[0.875rem] border-[#dce5ee] bg-white px-4 py-3 text-[0.875rem] text-[#2b3946] placeholder:text-[#97a4b0] focus-visible:border-[#dce5ee] focus-visible:ring-0"
              />
            </Field>
          </div>
        </div>
      </div>
    </div>
  );
}

function Field(props: {
  label: string;
  required?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={cn("space-y-1.5", props.className)}>
      <label className="text-[#8a96a3] text-[0.6875rem] font-semibold uppercase tracking-[0.015rem]">
        {props.label}
        {props.required ? <span className="text-[#ef4444]"> *</span> : null}
      </label>
      {props.children}
    </div>
  );
}

export default TenantSettings;
