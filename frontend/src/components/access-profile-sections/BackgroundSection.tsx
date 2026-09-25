import React, { forwardRef, useImperativeHandle } from "react";
import { useForm, type Control } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomTextarea from "../form/CustomTextarea";
import EducationEntriesField, {
  type EducationEntriesFormValues,
} from "../shared/EducationEntriesField";
import { mapEducationResponseToForm } from "@/utils/userProfileEducation";
import { Form } from "../ui/form";
import {
  backgroundSchema,
  USER_ACCESS_PROFILE_LIMITS,
  type BackgroundFormValues,
} from "@/schemas/user-access-profiles.schema";
import type { AdminUserProfessionalProfile } from "@/store/api/admin/users.api";
import type { ProfileSectionHandle } from "./profileSectionHandle";

interface BackgroundSectionProps {
  profileData?: AdminUserProfessionalProfile | null;
}

const BackgroundSection = forwardRef<
  ProfileSectionHandle<BackgroundFormValues>,
  BackgroundSectionProps
>(function BackgroundSection({ profileData }, ref) {
  const form = useForm<BackgroundFormValues>({
    resolver: zodResolver(backgroundSchema),
    defaultValues: {
      researchBackground: "",
      supervisoryExperience: "",
      careerObjectives: "",
      education: [],
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

  const educationEntries = React.useMemo(
    () => mapEducationResponseToForm(profileData?.education),
    [profileData?.education],
  );

  React.useEffect(() => {
    if (!profileData) return;
    if (form.formState.isDirty) return;
    form.reset({
      researchBackground: profileData.researchBackground || "",
      supervisoryExperience: profileData.supervisoryExperience || "",
      careerObjectives: profileData.careerObjectives || "",
      education: educationEntries,
    });
  }, [form, profileData, educationEntries]);

  return (
    <div className="flex min-w-0 flex-col gap-6 pt-6 pb-4 md:px-1">
      <Form {...form}>
        <form
          onSubmit={(event) => event.preventDefault()}
          className="flex min-w-0 flex-col gap-6"
        >
          <EducationEntriesField
            control={form.control as Control<EducationEntriesFormValues>}
            initialEducation={educationEntries}
            syncToken={profileData?.id}
          />

          <CustomTextarea
            control={form.control}
            label="Research Background"
            name="researchBackground"
            maxLength={USER_ACCESS_PROFILE_LIMITS.researchBackground}
            hint="Describe your research experience and publications"
          />

          <CustomTextarea
            control={form.control}
            label="Supervisory Experience"
            name="supervisoryExperience"
            maxLength={USER_ACCESS_PROFILE_LIMITS.supervisoryExperience}
            hint="Describe your experience supervising other professionals"
          />

          <CustomTextarea
            control={form.control}
            label="Career Objectives"
            name="careerObjectives"
            maxLength={USER_ACCESS_PROFILE_LIMITS.careerObjectives}
            hint="Describe your professional goals and career objectives"
          />
        </form>
      </Form>
    </div>
  );
});

export default BackgroundSection;
