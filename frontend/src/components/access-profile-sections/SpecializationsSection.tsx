import React, { forwardRef, useImperativeHandle } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomTextarea from "../form/CustomTextarea";
import CustomInput from "../form/CustomInput";
import CustomStringListInput from "../form/CustomStringListInput";
import { Form } from "../ui/form";
import {
  specializationsSchema,
  USER_ACCESS_PROFILE_LIMITS,
  type SpecializationsFormValues,
} from "@/schemas/user-access-profiles.schema";
import type { AdminUserProfessionalProfile } from "@/store/api/admin/users.api";
import type { ProfileSectionHandle } from "./profileSectionHandle";

interface SpecializationsSectionProps {
  profileData?: AdminUserProfessionalProfile | null;
}

const SpecializationsSection = forwardRef<
  ProfileSectionHandle<SpecializationsFormValues>,
  SpecializationsSectionProps
>(function SpecializationsSection({ profileData }, ref) {
  const form = useForm<SpecializationsFormValues>({
    resolver: zodResolver(specializationsSchema),
    defaultValues: {
      yearsOfExperience: "",
      clinicalSummary: "",
      specializations: [],
      languages: [],
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
      yearsOfExperience: profileData.yearsOfExperience
        ? String(profileData.yearsOfExperience)
        : "",
      clinicalSummary: profileData.clinicalExperience || "",
      specializations: profileData.specializations || [],
      languages: profileData.languages || [],
    });
  }, [form, profileData]);

  return (
    <div className="flex flex-col gap-6 pt-6 pb-4 md:px-1">
      <Form {...form}>
        <form
          onSubmit={(event) => event.preventDefault()}
          className="flex flex-col gap-6"
        >
          <CustomStringListInput
            control={form.control}
            name="specializations"
            label="Specializations"
            placeholder="e.g. CBT, Trauma Therapy"
            hint="Add each specialization separately"
          />
          <CustomStringListInput
            control={form.control}
            name="languages"
            label="Languages"
            placeholder="e.g. English, Spanish"
            hint="Add each language separately"
          />
          <CustomInput
            control={form.control}
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            digitsOnly
            maxLength={USER_ACCESS_PROFILE_LIMITS.yearsOfExperience}
            label="Years of Experience"
            name="yearsOfExperience"
          />
          <CustomTextarea
            control={form.control}
            label="Clinical Experience Summary"
            name="clinicalSummary"
            maxLength={USER_ACCESS_PROFILE_LIMITS.clinicalSummary}
            hint="Describe your clinical experience and expertise..."
          />
        </form>
      </Form>
    </div>
  );
});

export default SpecializationsSection;
