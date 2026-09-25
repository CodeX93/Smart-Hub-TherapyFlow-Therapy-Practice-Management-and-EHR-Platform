import React, { useEffect, useMemo } from "react";
import { useForm, type Control } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomTextarea from "../../form/CustomTextarea";
import EducationEntriesField, {
  type EducationEntriesFormValues,
} from "../../shared/EducationEntriesField";
import { Button } from "../../ui/button";
import { mapEducationResponseToForm } from "@/utils/userProfileEducation";
import { Form } from "../../ui/form";
import {
  backgroundSchema,
  THERAPIST_PROFILE_LIMITS,
  type BackgroundFormValues,
} from "@/schemas/general-profile-modal-schemas";
import type { UserProfileResponse } from "@/store/api/userProfile.api";

interface BackgroundSectionProps {
  profileData?: UserProfileResponse;
  isSaving?: boolean;
  onSave: (values: BackgroundFormValues) => Promise<void> | void;
}

const BackgroundSection: React.FC<BackgroundSectionProps> = ({
  profileData,
  isSaving = false,
  onSave,
}) => {
  const form = useForm<BackgroundFormValues>({
    resolver: zodResolver(backgroundSchema),
    defaultValues: {
      careerObjectives: "",
      education: [],
    },
  });

  const educationEntries = useMemo(
    () => mapEducationResponseToForm(profileData?.education),
    [profileData?.education],
  );

  useEffect(() => {
    if (!profileData) return;
    if (form.formState.isDirty) return;
    form.reset({
      careerObjectives: profileData.careerObjectives || "",
      education: educationEntries,
    });
  }, [form, profileData, educationEntries]);

  return (
    <div className="flex h-full min-w-0 flex-col gap-6 pt-6 md:px-1">
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(async (values) => {
            await onSave(values);
            form.reset(values);
          })}
          className="flex h-full min-w-0 flex-col gap-6"
        >
          <EducationEntriesField
            control={form.control as Control<EducationEntriesFormValues>}
            initialEducation={educationEntries}
            syncToken={profileData?.id}
          />

          <CustomTextarea
            control={form.control}
            label="Career Objectives"
            name="careerObjectives"
            hint="Describe your professional goals and career objectives"
            maxLength={THERAPIST_PROFILE_LIMITS.careerObjectives}
          />

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

export default BackgroundSection;
