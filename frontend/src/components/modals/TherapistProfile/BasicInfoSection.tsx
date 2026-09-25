import React, { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomInput from "../../form/CustomInput";
import { Button } from "../../ui/button";
import { Form } from "../../ui/form";
import {
  therapistBasicInfoSchema,
  THERAPIST_PROFILE_LIMITS,
  type TherapistBasicInfoFormValues,
} from "@/schemas/general-profile-modal-schemas";
import type { UserProfileResponse } from "@/store/api/userProfile.api";

interface BasicInfoSectionProps {
  profileData?: UserProfileResponse;
  isSaving?: boolean;
  onSave: (values: TherapistBasicInfoFormValues) => Promise<void> | void;
}

const BasicInfoSection: React.FC<BasicInfoSectionProps> = ({
  profileData,
  isSaving = false,
  onSave,
}) => {
  const form = useForm<TherapistBasicInfoFormValues>({
    resolver: zodResolver(therapistBasicInfoSchema),
    defaultValues: {
      fullName: "",
      email: "",
      experience: "",
      maxClients: "",
      sessionDuration: "",
      emergencyName: "",
      emergencyPhone: "",
      relationship: "",
    },
  });

  useEffect(() => {
    if (!profileData) return;
    if (form.formState.isDirty) return;
    form.reset({
      fullName: profileData.fullName || "",
      email: profileData.email || "",
      experience:
        profileData.yearsOfExperience !== undefined &&
        profileData.yearsOfExperience !== null
          ? String(profileData.yearsOfExperience)
          : "",
      maxClients:
        profileData.maxClientsPerDay !== undefined &&
        profileData.maxClientsPerDay !== null
          ? String(profileData.maxClientsPerDay)
          : "",
      sessionDuration:
        profileData.sessionDuration !== undefined &&
        profileData.sessionDuration !== null
          ? String(profileData.sessionDuration)
          : "",
      emergencyName: profileData.emergencyContactName || "",
      emergencyPhone: profileData.emergencyContactPhone || "",
      relationship: profileData.emergencyContactRelationship || "",
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
          className="flex flex-col gap-6 h-full"
        >
          <div className="flex flex-col gap-4">
            <CustomInput
              control={form.control}
              label="Full Name"
              name="fullName"
              maxLength={THERAPIST_PROFILE_LIMITS.fullName}
            />
            <CustomInput
              control={form.control}
              label="Email Address"
              name="email"
              type="email"
              maxLength={THERAPIST_PROFILE_LIMITS.email}
              disabled
            />
            <div className="grid grid-cols-2 gap-4">
              <CustomInput
                control={form.control}
                label="Years of Experience"
                name="experience"
                digitsOnly
                inputMode="numeric"
                maxLength={THERAPIST_PROFILE_LIMITS.yearsOfExperience}
                hint="0-99"
              />
              <CustomInput
                control={form.control}
                label="Max Clients / day"
                name="maxClients"
                digitsOnly
                inputMode="numeric"
                maxLength={THERAPIST_PROFILE_LIMITS.maxClientsPerDay}
                hint="1-100"
              />
            </div>
            <CustomInput
              control={form.control}
              label="Session Duration (min)"
              name="sessionDuration"
              digitsOnly
              inputMode="numeric"
              maxLength={THERAPIST_PROFILE_LIMITS.sessionDuration}
              hint="5-480"
            />
          </div>

          <div className="flex flex-col gap-4">
            <h3 className="text-(--neutral-950) text-[1.125rem] leading-6 font-medium px-1">
              Emergency Contact
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <CustomInput
                control={form.control}
                label="Contact Name"
                name="emergencyName"
                maxLength={THERAPIST_PROFILE_LIMITS.emergencyContactName}
              />
              <CustomInput
                control={form.control}
                label="Contact Phone"
                name="emergencyPhone"
                maxLength={THERAPIST_PROFILE_LIMITS.emergencyContactNumber}
              />
            </div>
            <CustomInput
              control={form.control}
              label="Relationship"
              name="relationship"
              maxLength={THERAPIST_PROFILE_LIMITS.emergencyContactRelation}
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

export default BasicInfoSection;
