import { useMemo } from "react";
import { SystemOptionCategoryKey } from "@/constants/systemOptionCategoryKeys";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import type { SystemOptionValue } from "@/store/api/admin/systemOptions.api";
import { useGetSystemOptionCategoriesQuery } from "@/store/api/admin/systemOptions.api";
import {
  buildLabelMap,
  buildOptionsByCategory,
  getCategoryOptions,
  resolveOptionLabel,
  toFilterSelectOptions,
} from "@/utils/systemOptions";

export function useSystemOptionCatalog(enabled = true) {
  const { data: categories, isLoading, isFetching, isError } =
    useGetSystemOptionCategoriesQuery(undefined, { skip: !enabled });

  const optionsByCategory = useMemo(
    () => buildOptionsByCategory(categories),
    [categories],
  );

  const getOptions = useMemo(
    () => (categoryKey: string) => getCategoryOptions(optionsByCategory, categoryKey),
    [optionsByCategory],
  );

  const getFilterOptions = useMemo(
    () => (categoryKey: string, allLabel: string) =>
      toFilterSelectOptions(getCategoryOptions(optionsByCategory, categoryKey), allLabel),
    [optionsByCategory],
  );

  const resolveLabel = useMemo(
    () => (categoryKey: string, optionKey: string | null | undefined) =>
      resolveOptionLabel(getCategoryOptions(optionsByCategory, categoryKey), optionKey),
    [optionsByCategory],
  );

  const isLoadingState = isLoading || isFetching;
  const isReady = !isLoading && !isFetching && categories !== undefined;

  return useMemo(
    () => ({
      optionsByCategory,
      isLoading: isLoadingState,
      isReady,
      isError,
      getOptions,
      getFilterOptions,
      resolveLabel,
    }),
    [
      optionsByCategory,
      isLoadingState,
      isReady,
      isError,
      getOptions,
      getFilterOptions,
      resolveLabel,
    ],
  );
}

export type SystemOptionCatalog = ReturnType<typeof useSystemOptionCatalog>;

export function useClientFilterOptions(enabled: boolean) {
  const catalog = useSystemOptionCatalog(enabled);

  const statusOptions = useMemo<CustomSelectOption[]>(
    () => catalog.getFilterOptions(SystemOptionCategoryKey.CLIENT_STATUS, "All Statuses"),
    [catalog],
  );

  const stageOptions = useMemo<CustomSelectOption[]>(
    () => catalog.getFilterOptions(SystemOptionCategoryKey.CLIENT_STAGE, "All Stages"),
    [catalog],
  );

  const clientTypeOptions = useMemo<CustomSelectOption[]>(
    () => catalog.getFilterOptions(SystemOptionCategoryKey.CLIENT_TYPE, "All Types"),
    [catalog],
  );

  const labelMaps = useMemo(() => {
    const status = buildLabelMap(
      catalog.getOptions(SystemOptionCategoryKey.CLIENT_STATUS),
    );
    const stage = buildLabelMap(
      catalog.getOptions(SystemOptionCategoryKey.CLIENT_STAGE),
    );
    const clientType = buildLabelMap(
      catalog.getOptions(SystemOptionCategoryKey.CLIENT_TYPE),
    );
    return { status, stage, clientType };
  }, [catalog]);

  return {
    ...catalog,
    statusOptions,
    stageOptions,
    clientTypeOptions,
    labelMaps,
  };
}

export function resolveCatalogLabel(
  map: Map<string, string>,
  value: string,
): string {
  return map.get(value) ?? map.get(value.toLowerCase()) ?? value;
}

export function useClientOverviewLabels(client: {
  gender?: string;
  maritalStatus?: string;
  preferredLanguage?: string;
  clientStage?: string;
  clientStatus?: string;
  clientType?: string;
  serviceType?: string;
  serviceFrequency?: string;
  employmentStatus?: string;
  educationLevel?: string;
  referralSource?: string;
  insuranceProvider?: string;
  insuranceType?: string;
  treatmentModality?: string;
}) {
  const catalog = useSystemOptionCatalog(true);

  return useMemo(
    () => ({
      gender: catalog.resolveLabel(SystemOptionCategoryKey.GENDER, client.gender),
      maritalStatus: catalog.resolveLabel(
        SystemOptionCategoryKey.MARITAL_STATUS,
        client.maritalStatus,
      ),
      preferredLanguage: catalog.resolveLabel(
        SystemOptionCategoryKey.PREFERRED_LANGUAGE,
        client.preferredLanguage,
      ),
      clientStage: catalog.resolveLabel(
        SystemOptionCategoryKey.CLIENT_STAGE,
        client.clientStage,
      ),
      clientStatus: catalog.resolveLabel(
        SystemOptionCategoryKey.CLIENT_STATUS,
        client.clientStatus,
      ),
      clientType: catalog.resolveLabel(
        SystemOptionCategoryKey.CLIENT_TYPE,
        client.clientType,
      ),
      serviceType: catalog.resolveLabel(
        SystemOptionCategoryKey.SERVICE_TYPE,
        client.serviceType,
      ),
      serviceFrequency: catalog.resolveLabel(
        SystemOptionCategoryKey.SERVICE_FREQUENCY,
        client.serviceFrequency,
      ),
      employmentStatus: catalog.resolveLabel(
        SystemOptionCategoryKey.EMPLOYMENT_STATUS,
        client.employmentStatus,
      ),
      educationLevel: catalog.resolveLabel(
        SystemOptionCategoryKey.EDUCATION_LEVEL,
        client.educationLevel,
      ),
      referralSource: catalog.resolveLabel(
        SystemOptionCategoryKey.CLIENT_SOURCE,
        client.referralSource,
      ),
      insuranceProvider: catalog.resolveLabel(
        SystemOptionCategoryKey.INSURANCE_PROVIDERS,
        client.insuranceProvider,
      ),
      insuranceType: catalog.resolveLabel(
        SystemOptionCategoryKey.INSURANCE_TYPES,
        client.insuranceType,
      ),
      treatmentModality: catalog.resolveLabel(
        SystemOptionCategoryKey.TREATMENT_MODALITIES,
        client.treatmentModality,
      ),
      isLoading: catalog.isLoading,
    }),
    [catalog, client],
  );
}

export function useTaskSystemOptions(enabled: boolean) {
  const catalog = useSystemOptionCatalog(enabled);

  return useMemo(
    () => ({
      isLoading: catalog.isLoading,
      priorityOptions: catalog.getOptions(SystemOptionCategoryKey.TASK_PRIORITY),
      statusOptions: catalog.getOptions(SystemOptionCategoryKey.TASK_STATUS),
      titleOptions: catalog.getOptions(SystemOptionCategoryKey.TASK_TITLES),
      taskTypeOptions: catalog.getOptions(SystemOptionCategoryKey.TASK_TYPES),
      resolvePriorityLabel: (key?: string | null) =>
        catalog.resolveLabel(SystemOptionCategoryKey.TASK_PRIORITY, key),
      resolveStatusLabel: (key?: string | null) =>
        catalog.resolveLabel(SystemOptionCategoryKey.TASK_STATUS, key),
      resolveTaskTypeLabel: (key?: string | null) =>
        catalog.resolveLabel(SystemOptionCategoryKey.TASK_TYPES, key),
      getDefaultPriority: () => {
        const options = catalog.getOptions(SystemOptionCategoryKey.TASK_PRIORITY);
        return (
          options.find((option) => option.isDefault)?.optionKey ??
          options[0]?.optionKey ??
          "medium"
        );
      },
      getDefaultStatus: () => {
        const options = catalog.getOptions(SystemOptionCategoryKey.TASK_STATUS);
        return (
          options.find((option) => option.isDefault)?.optionKey ??
          options[0]?.optionKey ??
          "pending"
        );
      },
    }),
    [catalog],
  );
}

export function useTaskOptionPresentation() {
  const taskOptions = useTaskSystemOptions(true);

  return useMemo(
    () => ({
      isLoading: taskOptions.isLoading,
      priorityLabel: taskOptions.resolvePriorityLabel,
      statusLabel: taskOptions.resolveStatusLabel,
      taskTypeLabel: taskOptions.resolveTaskTypeLabel,
    }),
    [taskOptions],
  );
}

export function useSessionSystemOptions(enabled: boolean) {
  const catalog = useSystemOptionCatalog(enabled);

  return useMemo(
    () => ({
      isLoading: catalog.isLoading,
      isReady: catalog.isReady,
      sessionModeOptions: catalog.getOptions(SystemOptionCategoryKey.SESSION_MODE),
      sessionTypeOptions: catalog.getOptions(SystemOptionCategoryKey.SESSION_TYPE),
      serviceTypeOptions: catalog.getOptions(SystemOptionCategoryKey.SERVICE_TYPE),
      sessionStatusOptions: catalog.getOptions(SystemOptionCategoryKey.SESSION_STATUS),
      resolveSessionModeLabel: (key?: string | null) =>
        catalog.resolveLabel(SystemOptionCategoryKey.SESSION_MODE, key),
      resolveSessionStatusLabel: (key?: string | null) =>
        catalog.resolveLabel(SystemOptionCategoryKey.SESSION_STATUS, key),
      getDefaultSessionModeKey: () => {
        const options = catalog.getOptions(SystemOptionCategoryKey.SESSION_MODE);
        return (
          options.find((option) => option.isDefault)?.optionKey ??
          options[0]?.optionKey ??
          "online"
        );
      },
      isInPersonMode: (optionKey: string): boolean => {
        const normalized = optionKey.toLowerCase().replace(/[\s_-]+/g, "");
        return (
          normalized.includes("person") ||
          normalized === "inperson" ||
          normalized === "inperson"
        );
      },
      isVirtualMode: (optionKey: string): boolean => {
        const normalized = optionKey.toLowerCase().replace(/[\s_-]+/g, "");
        return !(
          normalized.includes("person") ||
          normalized === "inperson" ||
          normalized === "inperson"
        );
      },
      resolveSessionModeKey: (internalMode: "virtual" | "in-person"): string => {
        const options = catalog.getOptions(SystemOptionCategoryKey.SESSION_MODE);
        const targets =
          internalMode === "in-person"
            ? ["in_person", "in-person", "in person"]
            : ["online", "virtual", "video"];
        for (const target of targets) {
          const match = options.find(
            (option) =>
              option.optionKey.toLowerCase().replace(/[\s_-]+/g, "") ===
              target.replace(/[\s_-]+/g, ""),
          );
          if (match) return match.optionKey;
        }
        return internalMode === "in-person" ? "in_person" : "online";
      },
      mapSessionModeToInternal: (optionKey: string): "virtual" | "in-person" => {
        const normalized = optionKey.toLowerCase().replace(/[\s_-]+/g, "");
        if (
          normalized.includes("person") ||
          normalized === "inperson" ||
          normalized === "inperson"
        ) {
          return "in-person";
        }
        return "virtual";
      },
    }),
    [catalog],
  );
}

export type { SystemOptionValue };
