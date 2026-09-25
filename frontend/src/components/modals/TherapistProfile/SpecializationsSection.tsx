import React, { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomTextarea from "../../form/CustomTextarea";
import CustomStringListInput from "../../form/CustomStringListInput";
import { Button } from "../../ui/button";
import { Form } from "../../ui/form";
import {
  specializationsSchema,
  THERAPIST_PROFILE_LIMITS,
  type SpecializationsFormValues,
} from "@/schemas/general-profile-modal-schemas";
import type { UserProfileResponse } from "@/store/api/userProfile.api";

interface SpecializationsSectionProps {
  profileData?: UserProfileResponse;
  isSaving?: boolean;
  onSave: (values: SpecializationsFormValues) => Promise<void> | void;
}

const SpecializationsSection: React.FC<SpecializationsSectionProps> = ({
  profileData,
  isSaving = false,
  onSave,
}) => {
  const form = useForm<SpecializationsFormValues>({
    resolver: zodResolver(specializationsSchema),
    defaultValues: {
      clinicalSummary: "",
      researchBackground: "",
      supervisoryExperience: "",
      specializations: [],
      languages: [],
    },
  });

  useEffect(() => {
    if (!profileData) return;
    if (form.formState.isDirty) return;
    form.reset({
      clinicalSummary: profileData.clinicalExperience || "",
      researchBackground: profileData.researchBackground || "",
      supervisoryExperience: profileData.supervisoryExperience || "",
      specializations: profileData.specializations || [],
      languages: profileData.languages || [],
    });
  }, [form, profileData]);

  return (
    <div className="flex flex-col gap-6 h-full pt-6 md:px-1">
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(async (values) => {
            await onSave(values);
            form.reset(values);
          })}
          className="flex flex-col gap-6 h-full"
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
          <CustomTextarea
            control={form.control}
            label="Clinical Experience Summary"
            name="clinicalSummary"
            hint="Describe your clinical experience and expertise..."
            maxLength={THERAPIST_PROFILE_LIMITS.clinicalSummary}
          />
          <CustomTextarea
            control={form.control}
            label="Research Background"
            name="researchBackground"
            hint="Describe your research experience and publications"
            maxLength={THERAPIST_PROFILE_LIMITS.researchBackground}
          />
          <CustomTextarea
            control={form.control}
            label="Supervisory Experience"
            name="supervisoryExperience"
            hint="Describe your experience supervising other professionals"
            maxLength={THERAPIST_PROFILE_LIMITS.supervisoryExperience}
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

export default SpecializationsSection;
