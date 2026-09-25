
import { ContentLoader } from "@/components/shared/ContentLoader";
import type { TabComponentProps } from "./types";
import { SystemOptionCategoryKey } from "@/constants/systemOptionCategoryKeys";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";
import CustomTextarea from "@/components/form/CustomTextarea";

const ReferralTab = ({ control, systemOptions }: TabComponentProps) => {
  const isLoading = systemOptions?.isLoading ?? false;
  const clientSourceOptions =
    systemOptions?.getSelectOptions(SystemOptionCategoryKey.CLIENT_SOURCE) ?? [];

  if (isLoading) {
    return (
      <ContentLoader className="py-16" />
    );
  }

  return (
    <div className="space-y-6">
      <h3 className="text-lg font-semibold text-(--text-primary-dark)">
        Referral & Case Information
      </h3>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <CustomDatePicker
            label="Start Date"
            control={control}
            name="startDate"
            disableFuture
          />
        </div>

        <div>
          <CustomDatePicker
            label="Referral Date"
            control={control}
            name="referralDate"
            disableFuture
          />
        </div>

        <div>
          <CustomInput
            label="Referrer Name"
            control={control}
            name="referrerName"
            hint="Who referred this client?"
            maxLength={100}
          />
        </div>

        <div>
          <CustomInput
            label="Reference Number"
            control={control}
            name="referenceNumber"
            hint="Case reference ID"
            maxLength={50}
          />
        </div>

        <div>
          <CustomSelect
            name="clientSource"
            label="Client Source"
            control={control}
            options={clientSourceOptions}
          />
        </div>
      </div>

      <div className="space-y-2">
        <CustomTextarea
          label="Referral Notes"
          control={control}
          name="referralNotes"
          className="min-h-24"
          maxLength={500}
        />
      </div>
    </div>
  );
};

export default ReferralTab;
