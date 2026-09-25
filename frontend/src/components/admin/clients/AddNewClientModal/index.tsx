
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  useForm,
  type Control,
  type FieldErrors,
  type Resolver,
  type SubmitErrorHandler,
  type SubmitHandler,
} from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { Form } from "../../../ui/form";
import { Button } from "../../../ui/button";
import { addClientSchema, type AddClientFormValues } from "../../../../types/add-client.type";
import { cn } from "../../../../lib/utils";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import PersonalTab from "./PersonalTab";
import AddressTab from "./AddressTab";
import ReferralTab from "./ReferralTab";
import EmploymentTab from "./EmploymentTab";
import ClinicalTab from "./ClinicalTab";
import ConsentsTab from "./ConsentsTab";
import type { TabType } from "./types";
import { useLazyGetAdminUsersQuery } from "@/store/api/admin/users.api";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import { useClientSystemOptions } from "@/hooks/useClientSystemOptions";
import {
  buildClientCreateDefaults,
  resolveClientFormOptionFields,
} from "@/utils/systemOptions";

interface AddNewClientModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: AddClientFormValues) => void | Promise<void>;
  /** Disables the Create Client button while the API request runs */
  isSubmitPending?: boolean;
  initialValues?: AddClientFormValues;
  clientId?: number | string | null;
  mode?: "create" | "edit";
  isInitialDataLoading?: boolean;
  hideAssignedTherapist?: boolean;
  hideConsentsTab?: boolean;
  /** When set, use these options instead of loading all therapists. */
  assignedTherapistOptions?: CustomSelectOption[];
}

const BASE_TABS: TabType[] = ["Personal", "Address", "Referral", "Employment", "Clinical"];

/**
 * Where each field lives, so a rejected submit can open the tab holding the problem
 * instead of leaving the user on a tab that looks fine.
 */
const FIELD_TABS: Partial<Record<keyof AddClientFormValues, TabType>> = {
  fullName: "Personal",
  email: "Personal",
  phone: "Personal",
  dateOfBirth: "Personal",
  gender: "Personal",
  maritalStatus: "Personal",
  pronouns: "Personal",
  preferredLanguage: "Personal",
  streetAddress1: "Address",
  streetAddress2: "Address",
  city: "Address",
  stateProvince: "Address",
  zipPostalCode: "Address",
  country: "Address",
  contactName: "Address",
  contactPhone: "Address",
  relationshipToClient: "Address",
  startDate: "Referral",
  referralDate: "Referral",
  referrerName: "Referral",
  referenceNumber: "Referral",
  clientSource: "Referral",
  referralNotes: "Referral",
  employmentStatus: "Employment",
  educationLevel: "Employment",
  numberOfDependents: "Employment",
  status: "Clinical",
  assignedTherapistId: "Clinical",
  clientType: "Clinical",
  clientStage: "Clinical",
  serviceType: "Clinical",
  serviceFrequency: "Clinical",
  treatmentModality: "Clinical",
  priority: "Clinical",
  dueDate: "Clinical",
  followUpNotes: "Clinical",
  insuranceProvider: "Clinical",
  insuranceType: "Clinical",
  policyNumber: "Clinical",
  groupNumber: "Clinical",
  copayAmount: "Clinical",
  deductible: "Clinical",
  insurancePhone: "Clinical",
  generalNotes: "Clinical",
};

const defaultTimezone =
  typeof Intl !== "undefined"
    ? Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC"
    : "UTC";

const defaultFormValues: AddClientFormValues = {
  fullName: "",
  email: "",
  timezone: defaultTimezone,
  enablePortalAccess: false,
  emailNotifications: true,
  legacyAddress: false,
  emergencyContactLegacy: false,
  legacyReferral: false,
  numberOfDependents: 0,
  status: "",
  assignedTherapistId: "",
  clientStage: "",
  needsFollowUp: false,
  insuranceInformation: false,
};

const AddNewClientModal = ({
  isOpen,
  onClose,
  onSubmit,
  isSubmitPending = false,
  initialValues,
  clientId,
  mode = "create",
  isInitialDataLoading = false,
  hideAssignedTherapist = false,
  hideConsentsTab = false,
  assignedTherapistOptions,
}: AddNewClientModalProps) => {
  const [activeTab, setActiveTab] = useState<TabType>("Personal");
  const [triggerGetUsers, { isFetching: isFetchingTherapists }] =
    useLazyGetAdminUsersQuery();
  const systemOptions = useClientSystemOptions(isOpen);

  const form = useForm<AddClientFormValues>({
    resolver: zodResolver(addClientSchema) as unknown as Resolver<AddClientFormValues>,
    defaultValues: initialValues ?? defaultFormValues,
    mode: "onChange",
  });

  const [therapistOptions, setTherapistOptions] = useState<CustomSelectOption[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const assignedTherapistId = form.watch("assignedTherapistId");
  const editFormSeedKeyRef = useRef<string | null>(null);
  /** Prevents create-mode reset from wiping keystrokes when isDirty / options deps change. */
  const createFormSeededRef = useRef(false);
  const insuranceInformation = form.watch("insuranceInformation");
  const insuranceProvider = form.watch("insuranceProvider");
  const policyNumber = form.watch("policyNumber");
  const tabs =
    mode === "edit" && !hideConsentsTab
      ? [...BASE_TABS, "Consents" as TabType]
      : BASE_TABS;

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    if (!isOpen) return;
    if (hideAssignedTherapist) return;
    if (assignedTherapistOptions) {
      setTherapistOptions(assignedTherapistOptions);
      return;
    }

    void triggerGetUsers({
      page: 1,
      pageSize: 100,
      role: "THERAPIST",
      active: true,
    })
      .unwrap()
      .then((response) => {
        setTherapistOptions(
          response.items.map((user) => ({
            value: String(user.id),
            label: user.fullName?.trim() || user.email || user.username,
          })),
        );
      })
      .catch((error) => {
        setTherapistOptions([]);
        setToastMessage(getApiErrorMessage(error));
      });
  }, [assignedTherapistOptions, hideAssignedTherapist, isOpen, triggerGetUsers]);

  const clinicalTabTherapistOptions = useMemo<CustomSelectOption[]>(() => {
    if (isFetchingTherapists && therapistOptions.length === 0) {
      return [{ value: "", label: "Loading therapists...", disabled: true }];
    }

    if (therapistOptions.length === 0) {
      return [{ value: "", label: "No therapists available", disabled: true }];
    }

    return therapistOptions;
  }, [isFetchingTherapists, therapistOptions]);

  const { handleSubmit: originalHandleSubmit, trigger } = form;
  // Subscribe during render so isDirty/isSubmitting stay accurate in effects (RHF Proxy).
  const { isDirty, isSubmitting } = form.formState;
  const handleSubmit = originalHandleSubmit as unknown as (
    onSubmit: SubmitHandler<AddClientFormValues>,
    onInvalid?: SubmitErrorHandler<AddClientFormValues>
  ) => (e?: React.BaseSyntheticEvent) => Promise<void>;

  const isLastTab = activeTab === tabs[tabs.length - 1];
  const currentTabIndex = tabs.indexOf(activeTab);

  // Get required fields for each tab based on schema
  const getRequiredFieldsForTab = (tab: TabType): (keyof AddClientFormValues)[] => {
    switch (tab) {
      case "Personal":
        return ["fullName", "email"];
      case "Clinical":
        return insuranceInformation
          ? ["insuranceProvider", "policyNumber"]
          : [];
      default:
        return [];
    }
  };

  const isClinicalInsuranceIncomplete =
    activeTab === "Clinical" &&
    insuranceInformation &&
    (!insuranceProvider?.trim() || !policyNumber?.trim());

  // Check if current tab is valid using form validation
  const isCurrentTabValid = async (): Promise<boolean> => {
    const requiredFields = getRequiredFieldsForTab(activeTab);
    if (requiredFields.length === 0) return true;

    const isValid = await trigger(requiredFields as (keyof AddClientFormValues)[]);
    return isValid;
  };

  // Check if tab can be switched to (validate required fields of the tab we're leaving)
  const canSwitchToTab = async (targetTab: TabType): Promise<boolean> => {
    // If switching to a previous tab or same tab, allow it
    const targetIndex = tabs.indexOf(targetTab);
    if (targetIndex <= currentTabIndex) return true;

    // If moving forward, validate current tab first
    const requiredFields = getRequiredFieldsForTab(activeTab);
    if (requiredFields.length === 0) return true;

    const isValid = await trigger(requiredFields as (keyof AddClientFormValues)[]);
    return isValid;
  };

  const isFormBootstrapping =
    systemOptions.isLoading ||
    (isInitialDataLoading && !initialValues) ||
    (mode === "edit" && !initialValues);

  useEffect(() => {
    if (!isOpen) {
      form.reset(defaultFormValues);
      setActiveTab("Personal");
      editFormSeedKeyRef.current = null;
      createFormSeededRef.current = false;
      return;
    }

    if (!systemOptions.isReady) return;
    if (mode === "edit" && !initialValues) return;

    // Parent re-renders with a new initialValues object while PATCH runs; never clobber
    // in-flight edits with stale API data (or empty defaults mid-close).
    if (isSubmitPending || isSubmitting) return;

    const editSeedKey =
      mode === "edit" ? `${String(clientId ?? "")}:${isOpen ? "open" : "closed"}` : null;

    // Create: seed once per open. Re-running on isDirty cleared every keystroke.
    if (mode === "create") {
      if (createFormSeededRef.current) return;
    }

    // Keep dirty user edits; allow re-seed from fresher API data when form is pristine.
    if (
      mode === "edit" &&
      editSeedKey &&
      editFormSeedKeyRef.current === editSeedKey &&
      isDirty
    ) {
      return;
    }

    const baseValues = initialValues ?? defaultFormValues;
    const resolvedValues = resolveClientFormOptionFields(
      baseValues,
      systemOptions.optionsByCategory,
    );

    if (mode === "create" && !initialValues) {
      const defaults = buildClientCreateDefaults(systemOptions.optionsByCategory);
      if (defaults.status) resolvedValues.status = defaults.status;
      if (defaults.clientStage) resolvedValues.clientStage = defaults.clientStage;
    }

    form.reset(resolvedValues);
    if (mode === "create") {
      createFormSeededRef.current = true;
    }
    if (editSeedKey) {
      editFormSeedKeyRef.current = editSeedKey;
    }
  }, [
    clientId,
    form,
    initialValues,
    isDirty,
    isOpen,
    isSubmitPending,
    isSubmitting,
    mode,
    systemOptions.isReady,
    systemOptions.optionsByCategory,
  ]);

  if (!isOpen) return null;

  const onFormSubmit = async (data: AddClientFormValues) => {
    try {
      await Promise.resolve(onSubmit(data));
      // Close first; the !isOpen effect resets the form so we don't flash empty fields.
      onClose();
    } catch {
      /* Parent handles errors; keep modal open */
    }
  };

  /**
   * react-hook-form drops a rejected submit on the floor, so without this a validation
   * failure on a tab the user is not looking at left Save Changes doing nothing at all:
   * no request, no error, no clue.
   */
  const onInvalidSubmit = (errors: FieldErrors<AddClientFormValues>) => {
    const firstField = (Object.keys(errors) as (keyof AddClientFormValues)[])[0];
    if (!firstField) return;

    const targetTab = FIELD_TABS[firstField];
    if (targetTab && tabs.includes(targetTab) && targetTab !== activeTab) {
      setActiveTab(targetTab);
    }

    const message = errors[firstField]?.message;
    setToastMessage(
      typeof message === "string" && message.trim()
        ? message
        : "Some client details still need fixing before this can be saved.",
    );
  };

  const handleClose = () => {
    form.reset(initialValues ?? defaultFormValues);
    setActiveTab("Personal");
    onClose();
  };

  const handleNext = async (e?: React.MouseEvent<HTMLButtonElement>) => {
    e?.preventDefault();
    e?.stopPropagation();

    // Validate current tab before proceeding
    const isValid = await isCurrentTabValid();
    if (!isValid) return;

    // Move to next tab
    if (currentTabIndex < tabs.length - 1) {
      setActiveTab(tabs[currentTabIndex + 1]);
    }
  };

  const handleTabClick = async (targetTab: TabType) => {
    if (targetTab === activeTab) return;

    if (mode === "edit") {
      setActiveTab(targetTab);
      return;
    }

    const canSwitch = await canSwitchToTab(targetTab);
    if (!canSwitch) return;

    setActiveTab(targetTab);
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case "Personal":
        return (
          <PersonalTab
            control={form.control as unknown as Control<AddClientFormValues>}
            mode={mode}
            systemOptions={systemOptions}
          />
        );
      case "Address":
        return <AddressTab control={form.control as unknown as Control<AddClientFormValues>} />;
      case "Referral":
        return (
          <ReferralTab
            control={form.control as unknown as Control<AddClientFormValues>}
            systemOptions={systemOptions}
          />
        );
      case "Employment":
        return (
          <EmploymentTab
            control={form.control as unknown as Control<AddClientFormValues>}
            systemOptions={systemOptions}
          />
        );
      case "Clinical":
        return (
          <ClinicalTab
            control={form.control as unknown as Control<AddClientFormValues>}
            therapistOptions={clinicalTabTherapistOptions}
            assignedTherapistId={assignedTherapistId}
            onAssignedTherapistChange={(value) => {
              form.setValue("assignedTherapistId", value, {
                shouldDirty: true,
                shouldTouch: true,
                shouldValidate: true,
              });
            }}
            hideAssignedTherapist={hideAssignedTherapist}
            mode={mode}
            systemOptions={systemOptions}
          />
        );
      case "Consents":
        return <ConsentsTab clientId={clientId} />;
      default:
        return null;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="w-full max-w-4xl h-[85vh] bg-white rounded-lg shadow-lg overflow-hidden flex flex-col mx-4">
        {/* Header */}
        <div className="flex justify-between items-center px-6 py-6">
          <h2 className="text-lg font-semibold text-gray-900">
            {mode === "edit" ? "Edit Client" : "Add New Client"}
          </h2>
          <button
            onClick={handleClose}
            className="text-gray-500 hover:text-gray-700 transition-colors cursor-pointer"
          >
            <X size={24} />
          </button>
        </div>

        {/* Tabs */}
        <div className="flex gap-2 px-2 py-1 mx-6 mb-3 bg-gray-100 rounded-full">
          {tabs.map((tab) => (
            <button
              key={tab}
              type="button"
              onClick={() => handleTabClick(tab)}
              className={cn(
                "flex-1 px-4 py-2 text-sm font-medium rounded-full transition-all cursor-pointer text-center",
                activeTab === tab
                  ? "bg-white text-gray-900"
                  : "text-gray-400 hover:text-gray-500"
              )}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Form Content */}
        <Form {...form}>
          <form onSubmit={handleSubmit(onFormSubmit, onInvalidSubmit)} className="flex-1 flex flex-col min-h-0">
            {isFormBootstrapping ? (
              <ContentLoader size="md" className="-1 p-6" />
            ) : (
              <div className="flex-1 overflow-y-auto p-6 space-y-6">
                {renderTabContent()}
              </div>
            )}

            {/* Footer */}
            <div className="flex justify-end items-center gap-3 px-6 py-4 flex-none">
              <Button
                type="button"
                variant="outline"
                onClick={handleClose}
                className="px-6 py-2 h-11.5 rounded-full cursor-pointer text-sm font-normal"
              >
                Cancel
              </Button>
              {mode === "edit" || isLastTab ? (
                <Button
                  type="submit"
                  disabled={isSubmitPending || isFormBootstrapping}
                  className="px-6 py-2 h-11.5 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90  rounded-full cursor-pointer text-sm font-normal"
                  loading={isSubmitPending}
                  loadingLabel={mode === "edit"
                      ? "Saving..."
                      : "Creating..."}
                >
                  {mode === "edit"
                      ? "Save Changes"
                      : "Create Client"}
                </Button>
              ) : (
                <Button
                  type="button"
                  onClick={handleNext}
                  disabled={isSubmitPending || isFormBootstrapping || isClinicalInsuranceIncomplete}
                  className="px-6 py-2 w-30 h-11.5 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 rounded-full cursor-pointer text-sm font-normal disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </Button>
              )}
            </div>
          </form>
        </Form>
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

export default AddNewClientModal;
