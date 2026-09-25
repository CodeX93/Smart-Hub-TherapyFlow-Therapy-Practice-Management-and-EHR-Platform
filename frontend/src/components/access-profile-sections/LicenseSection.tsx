import React, { forwardRef, useImperativeHandle } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import CustomDatePicker from "../form/CustomDatePicker";
import { Form } from "../ui/form";
import {
  licenseSchema,
  USER_ACCESS_PROFILE_LIMITS,
  type LicenseFormValues,
} from "@/schemas/user-access-profiles.schema";
import type { AdminUserProfessionalProfile } from "@/store/api/admin/users.api";
import { parseDateOnly } from "@/utils/transformer/dates.transformer";
import type { ProfileSectionHandle } from "./profileSectionHandle";

const licenseOptions = [
  { value: "LCSW", label: "LCSW - Licensed Clinical Social Worker" },
  { value: "LPC", label: "LPC - Licensed Professional Counselor" },
  { value: "LMFT", label: "LMFT - Licensed Marriage and Family Therapist" },
  { value: "Psychologist", label: "Psychologist" },
];

interface LicenseSectionProps {
  profileData?: AdminUserProfessionalProfile | null;
}

const LicenseSection = forwardRef<
  ProfileSectionHandle<LicenseFormValues>,
  LicenseSectionProps
>(function LicenseSection({ profileData }, ref) {
  const form = useForm<LicenseFormValues>({
    resolver: zodResolver(licenseSchema),
    defaultValues: {
      licenseNumber: "",
      licenseType: "",
      licenseState: "",
      licenseExpiration: null,
    },
  });

  const { isDirty } = form.formState;

  useImperativeHandle(
    ref,
    () => ({
      getValues: () => form.getValues(),
      trigger: () => form.trigger(),
      reset: (values) => form.reset(values ?? form.getValues()),
      isDirty: () => isDirty,
    }),
    [form, isDirty],
  );

  React.useEffect(() => {
    if (!profileData) return;
    if (form.formState.isDirty) return;
    form.reset({
      licenseNumber: profileData.licenseNumber || "",
      licenseType: profileData.licenseType || "",
      licenseState: profileData.licenseState || "",
      licenseExpiration: parseDateOnly(profileData.licenseExpiry),
    });
  }, [form, profileData]);

  return (
    <div className="flex flex-col gap-6 pt-6 pb-4">
      <Form {...form}>
        <form
          onSubmit={(event) => event.preventDefault()}
          className="flex flex-col gap-4"
        >
          <CustomInput
            control={form.control}
            label="License Number"
            name="licenseNumber"
            maxLength={USER_ACCESS_PROFILE_LIMITS.licenseNumber}
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
              maxLength={USER_ACCESS_PROFILE_LIMITS.licenseState}
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
        </form>
      </Form>
    </div>
  );
});

export default LicenseSection;
