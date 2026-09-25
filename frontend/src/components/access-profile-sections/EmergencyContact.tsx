import React, { forwardRef, useImperativeHandle } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import CustomInput from "../form/CustomInput";
import { Form } from "../ui/form";
import {
  emergencyContactSchema,
  USER_ACCESS_PROFILE_LIMITS,
  type EmergencyContactFormValues,
} from "@/schemas/user-access-profiles.schema";
import type { AdminUserProfessionalProfile } from "@/store/api/admin/users.api";
import type { ProfileSectionHandle } from "./profileSectionHandle";

interface EmergencyContactProps {
  profileData?: AdminUserProfessionalProfile | null;
}

const EmergencyContact = forwardRef<
  ProfileSectionHandle<EmergencyContactFormValues>,
  EmergencyContactProps
>(function EmergencyContact({ profileData }, ref) {
  const form = useForm<EmergencyContactFormValues>({
    resolver: zodResolver(emergencyContactSchema),
    defaultValues: {
      emergencyContactName: "",
      emergencyContactNumber: "",
      emergencyContactEmail: "",
      emergencyContactRelation: "",
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
      emergencyContactName: profileData.emergencyContactName || "",
      emergencyContactNumber: profileData.emergencyContactPhone || "",
      emergencyContactEmail: profileData.emergencyContactEmail || "",
      emergencyContactRelation: profileData.emergencyContactRelationship || "",
    });
  }, [form, profileData]);

  return (
    <div className="flex flex-col gap-6 pt-6 pb-4 md:px-1">
      <Form {...form}>
        <form
          onSubmit={(event) => event.preventDefault()}
          className="flex flex-col gap-4"
        >
          <CustomInput
            control={form.control}
            label="Emergency Contact Name"
            name="emergencyContactName"
            type="text"
            maxLength={USER_ACCESS_PROFILE_LIMITS.emergencyContactName}
          />
          <CustomInput
            control={form.control}
            label="Emergency Contact Number"
            name="emergencyContactNumber"
            type="tel"
            inputMode="tel"
            maxLength={USER_ACCESS_PROFILE_LIMITS.emergencyContactNumber}
            hint={`Optional. May start with +, up to ${USER_ACCESS_PROFILE_LIMITS.emergencyContactNumber} characters`}
          />
          <CustomInput
            control={form.control}
            label="Emergency Contact Email"
            name="emergencyContactEmail"
            type="email"
            maxLength={USER_ACCESS_PROFILE_LIMITS.emergencyContactEmail}
          />
          <CustomInput
            control={form.control}
            label="Emergency Contact Relation"
            name="emergencyContactRelation"
            type="text"
            maxLength={USER_ACCESS_PROFILE_LIMITS.emergencyContactRelation}
            hint="e.g., Spouse, Parent, Sibling"
          />
        </form>
      </Form>
    </div>
  );
});

export default EmergencyContact;
