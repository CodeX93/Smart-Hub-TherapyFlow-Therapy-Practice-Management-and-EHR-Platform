import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, Bell } from "lucide-react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import ProfileDropdown from "@/components/shared/ProfileDropdown";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import PageHeader from "./components/PageHeader";
import SectionHeading from "./components/SectionHeading";
import ModuleAccessCard, {
  type ModuleToggleRow,
} from "./components/ModuleAccessCard";
import UsageLimitCard, {
  type UsageLimitRow,
} from "./components/UsageLimitCard";
import {
  useGetOrganisationsQuery,
  useGetOrganisationFeaturesQuery,
  useReplaceOrganisationFeaturesMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  buildModuleRowsFromFeatures,
  buildFeaturesPayloadFromRows,
  buildUsageRowsFromFeatures,
  isUsageKey,
} from "./featureRows";
import {
  sanitizeUsageLimitOverride,
  validateEnabledUsageLimits,
} from "./featureOverrides.utils";

function updateModuleRows(
  rows: ModuleToggleRow[],
  keyName: string,
  next: boolean
): ModuleToggleRow[] {
  return rows.map(function (row) {
    if (row.keyName === keyName) {
      return {
        ...row,
        overrideEnabled: next,
      };
    }

    return row;
  });
}

function updateUsageLimitRows(
  rows: UsageLimitRow[],
  keyName: string,
  next: string
): UsageLimitRow[] {
  return rows.map(function (row) {
    if (row.keyName === keyName) {
      return {
        ...row,
        overrideValue: sanitizeUsageLimitOverride(next),
      };
    }

    return row;
  });
}

function FeatureToggles() {
  const { slug } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
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
    [slug]
  );
  const { data: organisationListData } = useGetOrganisationsQuery(slugLookupFilters, {
    skip: !shouldResolveIdBySlug,
  });
  const resolvedOrganisation = useMemo(() => {
    return organisationListData?.rows.find((row) => row.slug === slug) ?? null;
  }, [organisationListData?.rows, slug]);
  const organisationId = useMemo(() => {
    if (organisationIdFromState !== null) return organisationIdFromState;
    if (!resolvedOrganisation) return null;
    const parsed = Number.parseInt(resolvedOrganisation.id, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }, [organisationIdFromState, resolvedOrganisation]);
  const orgName = useMemo(() => {
    if (resolvedOrganisation?.name) return resolvedOrganisation.name;
    if (!slug) return "Organization";
    return slug
      .split("-")
      .filter(Boolean)
      .map((part) => part[0]?.toUpperCase() + part.slice(1))
      .join(" ");
  }, [resolvedOrganisation?.name, slug]);

  const [moduleRows, setModuleRows] = useState<ModuleToggleRow[]>([]);
  const [limitRows, setLimitRows] = useState<UsageLimitRow[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const {
    data: featureState,
    isLoading,
    isError,
    error,
    refetch: refetchFeatures,
  } = useGetOrganisationFeaturesQuery(organisationId ?? 0, {
    skip: organisationId === null,
  });
  const [replaceOrganisationFeatures, { isLoading: isSavingFeatures }] =
    useReplaceOrganisationFeaturesMutation();

  useEffect(() => {
    setModuleRows(buildModuleRowsFromFeatures(featureState));
    setLimitRows(buildUsageRowsFromFeatures(featureState));
  }, [featureState]);

  function handleLogout() {
    window.location.href = "/super-admin/login";
  }

  function handleBackToOrganisation() {
    if (!slug) {
      navigate("/super-admin/organisations");
      return;
    }

    navigate("/super-admin/organisations/" + slug, {
      state: {
        organisationId,
        organisationSlug: slug,
      },
    });
  }

  function handleDiscard() {
    handleBackToOrganisation();
  }

  async function handleSave() {
    if (!organisationId) {
      setToastType("error");
      setToastMessage("Organisation id is missing.");
      return;
    }

    const validationError = validateEnabledUsageLimits(moduleRows, limitRows);
    if (validationError) {
      setToastType("error");
      setToastMessage(validationError);
      return;
    }

    try {
      const payload = buildFeaturesPayloadFromRows(
        moduleRows,
        limitRows,
        featureState
      );

      await replaceOrganisationFeatures({
        id: organisationId,
        body: payload,
      }).unwrap();

      await refetchFeatures();
      setToastType("success");
      setToastMessage(`Feature toggles updated for ${orgName}.`);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  }

  function handleModuleRowChange(keyName: string, next: boolean) {
    setModuleRows(function (previousRows) {
      return updateModuleRows(previousRows, keyName, next);
    });
    if (!isUsageKey(keyName)) {
      return;
    }
    setLimitRows((previousRows) => {
      const existing = previousRows.find((row) => row.keyName === keyName);
      if (next) {
        if (existing) return previousRows;
        const fallbackLimit = featureState?.[keyName]?.usageLimit;
        return [
          ...previousRows,
          {
            title: keyName
              .split("_")
              .filter(Boolean)
              .map((part) => part[0]?.toUpperCase() + part.slice(1).toLowerCase())
              .join(" "),
            keyName,
            planDefault: "",
            overrideValue:
              typeof fallbackLimit === "number" && Number.isFinite(fallbackLimit)
                ? sanitizeUsageLimitOverride(String(fallbackLimit))
                : "",
          },
        ];
      }
      return previousRows.filter((row) => row.keyName !== keyName);
    });
  }

  function handleUsageLimitRowChange(keyName: string, next: string) {
    setLimitRows(function (previousRows) {
      return updateUsageLimitRows(previousRows, keyName, next);
    });
  }

  return (
    <div className="flex h-full min-h-0 w-full flex-col bg-[#FAFAFB]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="flex shrink-0 flex-col gap-7">
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
              <span className="min-w-0 truncate text-[#2b3946]" title="Feature Toggles">
                Feature Toggles
              </span>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-3">
            <button
              type="button"
              className="relative flex h-9 w-9 items-center justify-center rounded-full text-[#24313f] transition-colors hover:bg-[#eff4f8]"
              aria-label="Notifications"
            >
              <Bell size={17} strokeWidth={1.9} aria-hidden="true" />
              <span className="absolute right-0.5 top-0.5 flex h-[1.125rem] min-w-[1.125rem] items-center justify-center rounded-full bg-[#ef4444] px-1 text-[0.625rem] font-semibold leading-none text-white">
                4
              </span>
            </button>

            <ProfileDropdown
              initials="JS"
              fullName="Jordan Smith"
              onLogout={handleLogout}
            />
          </div>
        </div>

        <PageHeader
          onDiscard={handleDiscard}
          onSave={handleSave}
          isSaving={isSavingFeatures}
        />

        {isLoading ? (
          <div className="rounded-[0.75rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
            Loading feature state...
          </div>
        ) : null}

        {isError ? (
          <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
            {getApiErrorMessage(error)}
          </div>
        ) : null}
      </div>

      <div
        className="min-h-0 flex-1 overflow-y-auto pb-6 pr-1"
        style={{
          scrollBehavior: "smooth",
          scrollbarGutter: "stable",
          overscrollBehavior: "contain",
        }}
      >
        <div className="flex flex-col gap-7">
          <div className="w-full">
            <SectionHeading
              title="Module & Access Controls"
              subtitle="Enable or disable entire modules or core capabilities."
            />
            <div className="mt-4">
              <ModuleAccessCard rows={moduleRows} onChange={handleModuleRowChange} />
            </div>
          </div>

          <div className="w-full">
            <SectionHeading
              title="Usage Limitations"
              subtitle="Set strict numeric limits on features. Leave empty or set to unlimited where applicable."
            />
            <div className="mt-4">
              <UsageLimitCard rows={limitRows} onChange={handleUsageLimitRowChange} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default FeatureToggles;
