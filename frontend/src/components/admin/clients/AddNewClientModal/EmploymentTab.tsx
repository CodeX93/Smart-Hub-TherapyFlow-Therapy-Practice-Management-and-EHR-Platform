
import { ContentLoader } from "@/components/shared/ContentLoader";
import {
  FormField,
  FormItem,
  FormControl,
  FormMessage,
} from "../../../ui/form";
import type { TabComponentProps } from "./types";
import { SystemOptionCategoryKey } from "@/constants/systemOptionCategoryKeys";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";

const EmploymentTab = ({ control, systemOptions }: TabComponentProps) => {
  const isLoading = systemOptions?.isLoading ?? false;
  const employmentStatusOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.EMPLOYMENT_STATUS) ?? [];
  const educationLevelOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.EDUCATION_LEVEL) ?? [];

  if (isLoading) {
    return (
      <ContentLoader className="py-16" />
    );
  }

  return (
    <div className="space-y-6">
      <h3 className="text-lg font-semibold text-(--text-primary-dark)">
        Employment & Socioeconomic
      </h3>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <CustomSelect
            name="employmentStatus"
            label="Employment Status"
            control={control}
            options={employmentStatusOptions}
          />
        </div>

        <div>
          <CustomSelect
            name="educationLevel"
            label="Education Level"
            control={control}
            options={educationLevelOptions}
          />
        </div>

        <FormField
          control={control}
          name="numberOfDependents"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <CustomInput
                  type="number"
                  label="Number of Dependents"
                  {...field}
                  onChange={(e) => {
                    const value = e.target.value;
                    field.onChange(value === "" ? 0 : parseInt(value, 10) || 0);
                  }}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
    </div>
  );
};

export default EmploymentTab;
