import React, { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomInput from "../../form/CustomInput";
import CustomSelect from "../../form/CustomSelect";
import CustomDatePicker from "../../form/CustomDatePicker";
import { Button } from "../../ui/button";
import { Form } from "../../ui/form";
import { licenseSchema, THERAPIST_PROFILE_LIMITS, type LicenseFormValues } from "@/schemas/general-profile-modal-schemas";
import type { UserProfileResponse } from "@/store/api/userProfile.api";
import { parseDateOnly } from "@/utils/transformer/dates.transformer";

const licenseOptions = [
  { value: "LCSW", label: "LCSW - Licensed Clinical Social Worker" },
  { value: "LPC", label: "LPC - Licensed Professional Counselor" },
  { value: "LMFT", label: "LMFT - Licensed Marriage and Family Therapist" },
  { value: "Psychologist", label: "Psychologist" },
];

interface LicenseSectionProps {
  profileData?: UserProfileResponse;
  isSaving?: boolean;
  onSave: (values: LicenseFormValues) => Promise<void> | void;
}

const LicenseSection: React.FC<LicenseSectionProps> = ({
  profileData,
  isSaving = false,
  onSave,
}) => {
  const form = useForm<LicenseFormValues>({
    resolver: zodResolver(licenseSchema),
    defaultValues: {
      licenseNumber: "123456",
      licenseType: "LCSW",
      licenseState: "CA",
      licenseExpiration: new Date(2025, 11, 25),
    },
  });

  useEffect(() => {
    if (!profileData) return;
    if (form.formState.isDirty) return;
    const parsedExpiry = parseDateOnly(profileData.licenseExpiry);

    form.reset({
      licenseNumber: profileData.licenseNumber || "",
      licenseType: profileData.licenseType || "",
      licenseState: profileData.licenseState || "",
      licenseExpiration: parsedExpiry,
    });
  }, [form, profileData]);

  return (
    <div className="flex flex-col gap-6 h-full pt-6">
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(async (values) => {
            await onSave(values);
            form.reset(values);
          })}
          className="flex flex-col gap-4 h-full"
        >
          <CustomInput
            control={form.control}
            label="License Number"
            name="licenseNumber"
            maxLength={THERAPIST_PROFILE_LIMITS.licenseNumber}
          />

          <CustomSelect
            control={form.control}
            label="License Type"
            name="licenseType"
            options={licenseOptions}
          />

          <div className="grid grid-cols-2 gap-4">
            <CustomInput
              control={form.control}
              label="License State"
              name="licenseState"
              maxLength={THERAPIST_PROFILE_LIMITS.licenseState}
            />
            <CustomDatePicker
              control={form.control}
              label="License Expiration"
              name="licenseExpiration"
              valueType="date"
              minYear={2000}
              maxYear={new Date().getFullYear() + 50}
            />
          </div>

          <div className="pt-4 flex justify-end h-full items-end pb-6">
            <Button
              type="submit"
              disabled={isSaving}
              className="rounded-full px-10 py-3 h-14 bg-(--bg-primary-dark) text-white hover:opacity-90 font-semibold text-[1rem] leading-6 cursor-pointer"
            >
              Save changes
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
};

export default LicenseSection;
