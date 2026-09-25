import { useWatch } from "react-hook-form";
import { FormField, FormItem, FormLabel, FormControl } from "../../../ui/form";
import type { TabComponentProps } from "./types";
import { relationshipOptions } from "./constants";
import CustomInput from "@/components/form/CustomInput";
import { Checkbox } from "@/components/ui/checkbox";
import CustomSelect from "@/components/form/CustomSelect";

const AddressTab = ({ control }: TabComponentProps) => {
  const emergencyContactLegacy = useWatch({
    control,
    name: "emergencyContactLegacy",
  });

  return (
    <div className="space-y-6">
      <h3 className="text-lg font-semibold text-(--text-primary-dark)">
        Address Information
      </h3>

      <div className="space-y-4">
        <div>
          <CustomInput
            label="Street Address 1"
            control={control}
            name="streetAddress1"
            maxLength={120}
          />
        </div>

        <div>
          <CustomInput
            label="Street Address 2"
            control={control}
            name="streetAddress2"
            hint="Apartment, Suite, etc."
            maxLength={120}
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <CustomInput
              label="City"
              control={control}
              name="city"
              maxLength={50}
            />
          </div>

          <div>
            <CustomInput
              label="State/Province"
              control={control}
              name="stateProvince"
              maxLength={50}
            />
          </div>

          <div>
            <CustomInput
              label="ZIP / Postal Code"
              control={control}
              name="zipPostalCode"
              maxLength={20}
            />
          </div>

          <div>
            <CustomInput
              label="Country"
              control={control}
              name="country"
              maxLength={50}
            />
          </div>
        </div>

        {/* Emergency Contact */}
        <FormField
          control={control}
          name="emergencyContactLegacy"
          render={({ field }) => (
            <FormItem className="flex items-center space-x-2">
              <FormControl>
                <Checkbox
                  id="emergencyContactLegacy"
                  checked={field.value}
                  onCheckedChange={field.onChange}
                />
              </FormControl>
              <FormLabel
                htmlFor="emergencyContactLegacy"
                className="text-sm font-semibold text-(--text-primary-dark) cursor-pointer"
              >
                Emergency Contact
              </FormLabel>
            </FormItem>
          )}
        />

        {emergencyContactLegacy && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <CustomInput
                  label="Contact Name"
                  control={control}
                  name="contactName"
                  maxLength={100}
                />
              </div>

              <div>
                <CustomInput
                  type="tel"
                  label="Contact Phone"
                  control={control}
                  name="contactPhone"
                  maxLength={20}
                />
              </div>
            </div>

            <div>
              <CustomSelect
                name="relationshipToClient"
                label="Relationship to Client"
                control={control}
                options={relationshipOptions}
              />
              <p className="mt-2 text-xs text-(--text-neutral-600)">
                e.g., Spouse, Parent
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AddressTab;
