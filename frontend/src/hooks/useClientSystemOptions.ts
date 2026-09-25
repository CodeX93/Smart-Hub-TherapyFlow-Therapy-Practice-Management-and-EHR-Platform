import { useMemo } from "react";
import type { SystemOptionCategoryKey } from "@/constants/systemOptionCategoryKeys";
import { useGetSystemOptionCategoriesQuery } from "@/store/api/admin/systemOptions.api";
import {
  buildOptionsByCategory,
  getCategoryOptions,
  getDefaultOptionKey,
  resolveOptionKey,
  resolveOptionLabel,
  toSelectOptions,
} from "@/utils/systemOptions";
import type { CustomSelectOption } from "@/components/form/CustomSelect";

export function useClientSystemOptions(enabled: boolean) {
  const {
    data: categories,
    isLoading,
    isError,
    refetch,
  } = useGetSystemOptionCategoriesQuery(undefined, { skip: !enabled });

  const optionsByCategory = useMemo(
    () => buildOptionsByCategory(categories),
    [categories],
  );

  const getSelectOptions = useMemo(
    () =>
      (categoryKey: SystemOptionCategoryKey): CustomSelectOption[] =>
        toSelectOptions(getCategoryOptions(optionsByCategory, categoryKey)),
    [optionsByCategory],
  );

  const getDefaultKey = useMemo(
    () =>
      (categoryKey: SystemOptionCategoryKey): string =>
        getDefaultOptionKey(getCategoryOptions(optionsByCategory, categoryKey)),
    [optionsByCategory],
  );

  const resolveLabel = useMemo(
    () =>
      (categoryKey: SystemOptionCategoryKey, optionKey: string | null | undefined): string =>
        resolveOptionLabel(
          getCategoryOptions(optionsByCategory, categoryKey),
          optionKey,
        ),
    [optionsByCategory],
  );

  const matchKey = useMemo(
    () =>
      (categoryKey: SystemOptionCategoryKey, value: string | null | undefined): string =>
        resolveOptionKey(
          getCategoryOptions(optionsByCategory, categoryKey),
          value,
        ),
    [optionsByCategory],
  );

  const isReady = !isLoading && categories !== undefined;

  return {
    optionsByCategory,
    isLoading,
    isReady,
    isError,
    refetch,
    getSelectOptions,
    getDefaultKey,
    resolveLabel,
    matchKey,
  };
}

export type ClientSystemOptionsContext = ReturnType<typeof useClientSystemOptions>;
