
import { ContentLoader } from "@/components/shared/ContentLoader";
import {
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormDescription,
} from "../../../ui/form";
import { Switch } from "../../../ui/switch";
import type { TabComponentProps } from "./types";
import { SystemOptionCategoryKey } from "@/constants/systemOptionCategoryKeys";
import CustomInput from "@/components/form/CustomInput";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import CustomSelect from "@/components/form/CustomSelect";

const PersonalTab = ({ control, mode = "create", systemOptions }: TabComponentProps) => {
  const isEditMode = mode === "edit";
  const isLoading = systemOptions?.isLoading ?? false;

  const genderOptions = systemOptions?.getSelectOptions(SystemOptionCategoryKey.GENDER) ?? [];
  const maritalStatusOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.MARITAL_STATUS) ?? [];
  const languageOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.PREFERRED_LANGUAGE) ?? [];

  if (isLoading) {
    return (
      <ContentLoader className="py-16" />
    );
  }

  return (
    <div className="space-y-6">
      <h3 className="text-lg font-semibold text-(--text-primary-dark)">
        Personal Information
      </h3>

      <div className="grid grid-cols-2 gap-4">
        <div className="min-w-0">
          <CustomInput
            label="Full Name"
            control={control}
            name="fullName"
            required
            maxLength={100}
          />
        </div>

        <div className="min-w-0">
          <CustomInput
            type="email"
            label="Email"
            control={control}
            name="email"
            required
            maxLength={254}
            disabled={isEditMode}
            readOnly={isEditMode}
          />
        </div>

        <div className="min-w-0">
          <CustomInput
            type="tel"
            label="Phone"
            control={control}
            name="phone"
            maxLength={20}
          />
        </div>

        <div className="min-w-0">
          <CustomDatePicker
            label="Date of Birth"
            control={control}
            name="dateOfBirth"
            disableFuture
          />
        </div>

        <div>
          <CustomSelect
            name="gender"
            label="Gender"
            control={control}
            options={genderOptions}
          />
        </div>

        <div>
          <CustomSelect
            name="maritalStatus"
            label="Marital Status"
            control={control}
            options={maritalStatusOptions}
          />
        </div>

        <div>
          <CustomInput
            label="Pronouns"
            control={control}
            name="pronouns"
            maxLength={10}
          />
        </div>

        <div>
          <CustomSelect
            name="preferredLanguage"
            label="Language"
            control={control}
            options={languageOptions}
          />
        </div>
      </div>

      {/* Immediate Setup */}
      <div className="space-y-4 pt-4">
        <h3 className="text-lg font-semibold text-(--text-primary-dark)">
          Immediate Setup
        </h3>

        <FormField
          control={control}
          name="enablePortalAccess"
          render={({ field }) => (
            <FormItem className="flex items-center justify-between rounded-lg border border-(--neutral-100) p-4">
              <div className="space-y-0.5">
                <FormLabel className="text-sm font-medium text-(--text-primary-dark) cursor-pointer">
                  Enable Portal Access
                </FormLabel>
                <FormDescription className="text-xs text-(--text-neutral-600)">
                  Client will use their primary email address for portal login
                </FormDescription>
              </div>
              <FormControl>
                <Switch
                  checked={field.value}
                  onCheckedChange={field.onChange}
                />
              </FormControl>
            </FormItem>
          )}
        />

        <FormField
          control={control}
          name="emailNotifications"
          render={({ field }) => (
            <FormItem className="flex items-center justify-between rounded-lg border border-(--neutral-100) p-4">
              <div className="space-y-0.5">
                <FormLabel className="text-sm font-medium text-(--text-primary-dark) cursor-pointer">
                  Email Notifications
                </FormLabel>
                <FormDescription className="text-xs text-(--text-neutral-600)">
                  Client wants to receive email updates
                </FormDescription>
              </div>
              <FormControl>
                <Switch
                  checked={field.value}
                  onCheckedChange={field.onChange}
                />
              </FormControl>
            </FormItem>
          )}
        />
      </div>
    </div>
  );
};

export default PersonalTab;
