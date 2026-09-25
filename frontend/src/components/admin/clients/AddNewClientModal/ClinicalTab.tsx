
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useWatch } from "react-hook-form";
import { FormField, FormItem, FormLabel, FormControl, FormMessage } from "../../../ui/form";
import type { TabComponentProps } from "./types";
import { SystemOptionCategoryKey } from "@/constants/systemOptionCategoryKeys";
import CustomSelect from "@/components/form/CustomSelect";
import { Checkbox } from "@/components/ui/checkbox";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomInput from "@/components/form/CustomInput";
import { sanitizeInsuranceAmountInput } from "@/utils/insuranceAmountInput";
import { sanitizeInsurancePhoneInput } from "@/utils/phoneInput";

const ClinicalTab = ({
  control,
  therapistOptions = [],
  assignedTherapistId = "",
  onAssignedTherapistChange,
  hideAssignedTherapist = false,
  systemOptions,
}: TabComponentProps) => {
  const needsFollowUp = useWatch({ control, name: "needsFollowUp" });
  const insuranceInformation = useWatch({
    control,
    name: "insuranceInformation",
  });
  const isLoading = systemOptions?.isLoading ?? false;

  const statusOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.CLIENT_STATUS) ?? [];
  const clientTypeOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.CLIENT_TYPE) ?? [];
  const clientStageOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.CLIENT_STAGE) ?? [];
  const serviceTypeOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.SERVICE_TYPE) ?? [];
  const serviceFrequencyOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.SERVICE_FREQUENCY) ?? [];
  const priorityOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.TASK_PRIORITY) ?? [];
  const insuranceProviderOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.INSURANCE_PROVIDERS) ?? [];
  const insuranceTypeOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.INSURANCE_TYPES) ?? [];
  const treatmentModalityOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.TREATMENT_MODALITIES) ?? [];

  if (isLoading) {
    return (
      <ContentLoader className="py-16" />
    );
  }

  return (
    <div className="space-y-6">
      <h3 className="text-lg font-semibold text-(--text-primary-dark)">
        Client Stage & Service Type
      </h3>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <CustomSelect
            name="status"
            label="Status"
            control={control}
            options={statusOptions}
          />
        </div>

        <div>
          <CustomSelect
            name="clientType"
            label="Client Type"
            control={control}
            options={clientTypeOptions}
          />
        </div>

        <div>
          <CustomSelect
            name="clientStage"
            label="Client stage"
            control={control}
            options={clientStageOptions}
          />
        </div>

        <div>
          <CustomSelect
            name="serviceType"
            label="Service Type"
            control={control}
            options={serviceTypeOptions}
          />
        </div>

        <div>
          <CustomSelect
            name="serviceFrequency"
            label="Service Frequency"
            control={control}
            options={serviceFrequencyOptions}
          />
        </div>

        <div>
          <CustomSelect
            name="treatmentModality"
            label="Treatment Modality"
            control={control}
            options={treatmentModalityOptions}
            placeholder="Select modality"
          />
        </div>

        {!hideAssignedTherapist ? (
          <div className="col-span-2">
            <CustomSelect
              label="Assigned Therapist"
              value={assignedTherapistId}
              onChange={onAssignedTherapistChange}
              options={therapistOptions}
              placeholder="Select therapist"
              isSearch={true}
            />
          </div>
        ) : null}
      </div>

      {/* Needs Follow-up */}
      <FormField
        control={control}
        name="needsFollowUp"
        render={({ field }) => (
          <FormItem className="flex items-center space-x-2">
            <FormControl>
              <Checkbox
                id="needsFollowUp"
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            </FormControl>
            <FormLabel
              htmlFor="needsFollowUp"
              className="text-sm font-bold text-gray-700 cursor-pointer"
            >
              Needs Follow-up
            </FormLabel>
          </FormItem>
        )}
      />

      {needsFollowUp && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <CustomSelect
                name="priority"
                label="Priority"
                control={control}
                options={priorityOptions}
              />
            </div>

            <div>
              <CustomDatePicker
                label="Due Date"
                control={control}
                name="dueDate"
                disablePast
              />
            </div>
          </div>

          <div className="space-y-2">
            <CustomTextarea
              label="Brief follow-up notes..."
              control={control}
              name="followUpNotes"
              className="min-h-24"
            />
          </div>
        </div>
      )}

      {/* Insurance Information */}
      <FormField
        control={control}
        name="insuranceInformation"
        render={({ field }) => (
          <FormItem className="flex items-center space-x-2">
            <FormControl>
              <Checkbox
                id="insuranceInformation"
                checked={field.value}
                onCheckedChange={field.onChange}
              />
            </FormControl>
            <FormLabel
              htmlFor="insuranceInformation"
              className="text-sm font-bold text-gray-700 cursor-pointer"
            >
              Insurance Information
            </FormLabel>
          </FormItem>
        )}
      />

      {insuranceInformation && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <CustomSelect
                name="insuranceProvider"
                label="Insurance Provider"
                control={control}
                options={insuranceProviderOptions}
                required
              />
            </div>

            <div>
              <CustomSelect
                name="insuranceType"
                label="Insurance Type"
                control={control}
                options={insuranceTypeOptions}
                placeholder="Select type"
              />
            </div>

            <div>
              <CustomInput
                label="Policy Number"
                control={control}
                name="policyNumber"
                required
                maxLength={50}
              />
            </div>

            <div>
              <CustomInput
                label="Group Number"
                control={control}
                name="groupNumber"
                maxLength={50}
              />
            </div>

            <FormField
              control={control}
              name="copayAmount"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput
                      label="Copay Amount"
                      inputMode="decimal"
                      {...field}
                      value={field.value ?? ""}
                      onChange={(event) => {
                        const sanitized = sanitizeInsuranceAmountInput(event.target.value);
                        event.target.value = sanitized;
                        field.onChange(sanitized);
                      }}
                      maxLength={11}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={control}
              name="deductible"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput
                      label="Deductible"
                      inputMode="decimal"
                      {...field}
                      value={field.value ?? ""}
                      onChange={(event) => {
                        const sanitized = sanitizeInsuranceAmountInput(event.target.value);
                        event.target.value = sanitized;
                        field.onChange(sanitized);
                      }}
                      maxLength={11}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={control}
              name="insurancePhone"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput
                      type="tel"
                      label="Insurance Phone"
                      {...field}
                      value={field.value ?? ""}
                      onChange={(event) => {
                        const sanitized = sanitizeInsurancePhoneInput(event.target.value);
                        event.target.value = sanitized;
                        field.onChange(sanitized);
                      }}
                      maxLength={20}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </div>
      )}

      <div className="space-y-2">
        <CustomTextarea
          label="General notes about the client..."
          control={control}
          name="generalNotes"
          className="min-h-32"
          maxLength={500}
        />
      </div>
    </div>
  );
};

export default ClinicalTab;
