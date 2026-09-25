
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useMemo, useState } from "react";
import FilterDropdown from "../../../components/clinical-forms/FilterDropdown";
import ClinicalFormCard from "../../../components/clinical-forms/ClinicalFormCard";

import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import type { FormStatus } from "../../../types/clinical-form.type";
import CustomInput from "@/components/form/CustomInput";
import { Search } from "lucide-react";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import { useGetPortalFormAssignmentsQuery } from "@/store/api/portalFormsApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  filterPortalFormCards,
  mapPortalFormAssignmentToCard,
} from "@/utils/portalFormDisplay";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import EmptyFormsState from "@/components/admin/clients/ClientProfile/FormsAndDocs/EmptyFormsState";
import {
  buildClinicalFormFilterChips,
  removeClinicalFormFilterChip,
} from "@/utils/appliedFilterChips";

const ITEMS_PER_PAGE = 20;

const ClinicalForms = () => {
  const [selectedFilters, setSelectedFilters] = useState<FormStatus[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [displayedItems, setDisplayedItems] = useState(ITEMS_PER_PAGE);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const {
    data: assignments = [],
    isLoading,
    isFetching,
    isError,
    error,
  } = useGetPortalFormAssignmentsQuery();

  const formCards = useMemo(
    () => assignments.map(mapPortalFormAssignmentToCard),
    [assignments],
  );

  const filteredForms = useMemo(
    () => filterPortalFormCards(formCards, searchQuery, selectedFilters),
    [formCards, searchQuery, selectedFilters],
  );

  const isInitialLoading = (isLoading || isFetching) && assignments.length === 0;

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (isError && error && error !== reportedError) {
    setReportedError(error);
    setToastMessage(getApiErrorMessage(error));
  }

  const resetKey = JSON.stringify([searchQuery, selectedFilters, assignments.length]);
  const [previousResetKey, setPreviousResetKey] = useState(resetKey);
  if (previousResetKey !== resetKey) {
    setPreviousResetKey(resetKey);
    setDisplayedItems(ITEMS_PER_PAGE);
  }

  const handleLoadMore = useCallback(() => {
    setDisplayedItems((previous) => previous + ITEMS_PER_PAGE);
  }, []);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: displayedItems < filteredForms.length,
    isLoading: false,
  });

  const currentForms = filteredForms.slice(0, displayedItems);

  const appliedFilterChips = useMemo(
    () => buildClinicalFormFilterChips(selectedFilters),
    [selectedFilters],
  );

  const handleRemoveFilterChip = useCallback((chipId: string) => {
    setSelectedFilters((currentFilters) =>
      removeClinicalFormFilterChip(currentFilters, chipId),
    );
  }, []);

  const handleClearAllFilters = useCallback(() => {
    setSelectedFilters([]);
  }, []);

  return (
    <div className="w-full">
      <ScrollToTopButton />
      <div className="mb-6 flex flex-col gap-3">
        <div className="flex items-center gap-2 justify-start">
        <div>
          <CustomInput
            placeholder="Search"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="rounded-full min-h-10 md:w-79 pb-0 pt-1.75"
          />
        </div>
        <FilterDropdown
          selectedFilters={selectedFilters}
          onFilterChange={setSelectedFilters}
        />
        </div>
        <AppliedFiltersBar
          chips={appliedFilterChips}
          onRemove={handleRemoveFilterChip}
          onClearAll={handleClearAllFilters}
          className="w-full"
        />
      </div>

      {isInitialLoading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-sm text-(--text-neutral-600)">
          <ContentLoader variant="inline" size="md" />
          <span>Loading clinical forms...</span>
        </div>
      ) : currentForms.length === 0 ? (
        <EmptyFormsState
          title={
            searchQuery.trim() || selectedFilters.length > 0
              ? "No clinical forms found"
              : "No forms assigned yet"
          }
          description={
            searchQuery.trim() || selectedFilters.length > 0
              ? "Try adjusting your search or filters"
              : "Assigned clinical forms will appear here"
          }
          className="rounded-xl border border-(--neutral-100) bg-white"
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {currentForms.map((form) => (
            <ClinicalFormCard key={form.id} form={form} />
          ))}
        </div>
      )}

      <div
        ref={observerTarget}
        className="h-10 w-full flex items-center justify-center mt-4"
      >
        {displayedItems < filteredForms.length ? (
          <ContentLoader variant="inline" size="md" />
        ) : null}
      </div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type="error"
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

export default ClinicalForms;
