
import { ContentLoader } from "@/components/shared/ContentLoader";
import React from "react";
import { Button } from "@/components/ui/button";
import SettingsLayout from "./SettingsLayout";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import CustomTextarea from "../form/CustomTextarea";
import Toast from "@/components/shared/Toast";
import { useForm, type SubmitHandler } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form";

import {
  practiceConfigSchema,
  type PracticeConfigFormData,
} from "@/schemas/settings.schema";
import { TIME_ZONE_OPTIONS } from "@/utils/functions/timezone";
import {
  useGetPracticeConfigurationQuery,
  useUpdatePracticeConfigurationMutation,
} from "@/store/api/admin/systemOptions.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { PRACTICE_CONFIG_FIELD_LIMITS } from "@/utils/practiceConfigInput";

const Administration = () => {
  const [toastMessage, setToastMessage] = React.useState<string | null>(null);
  const [toastType, setToastType] = React.useState<"success" | "error" | "info">("info");
  const { data: practiceConfig, isLoading: isLoadingConfig, isFetching: isFetchingConfig, error: configError, refetch } =
    useGetPracticeConfigurationQuery(undefined, {
      refetchOnMountOrArgChange: true,
    });
  const [updatePracticeConfiguration, { isLoading: isSavingConfig }] =
    useUpdatePracticeConfigurationMutation();

  const form = useForm<PracticeConfigFormData>({
    resolver: zodResolver(practiceConfigSchema),
    mode: "onChange",
    defaultValues: {
      name: "",
      subtitle: "",
      description: "",
      address: "",
      phone: "",
      email: "",
      website: "",
      timezone: "",
      taxId: "",
      licenseNumber: "",
      licenseState: "",
      npiNumber: "",
    },
  });

  const {
    handleSubmit,
    control,
    reset,
    watch,
    formState: { isDirty },
  } = form;

  const isDirtyRef = React.useRef(false);
  isDirtyRef.current = isDirty;

  const watchedName = watch("name") ?? "";
  const watchedSubtitle = watch("subtitle") ?? "";
  const watchedDescription = watch("description") ?? "";
  const watchedAddress = watch("address") ?? "";
  const watchedPhone = watch("phone") ?? "";
  const watchedEmail = watch("email") ?? "";
  const watchedWebsite = watch("website") ?? "";

  const toFormValues = React.useCallback(
    (config: NonNullable<typeof practiceConfig>): PracticeConfigFormData => ({
      name: config.practiceName || "",
      subtitle: config.subtitle || "",
      description: config.description || "",
      address: config.practiceAddress || "",
      phone: config.practicePhone || "",
      email: config.practiceEmail || "",
      website: config.practiceWebsite || "",
      timezone: config.timezone || "",
      taxId: config.taxId || "",
      licenseNumber: config.licenseNumber || "",
      licenseState: config.licenseState || "",
      npiNumber: config.npiNumber || "",
    }),
    [],
  );

  React.useEffect(() => {
    if (!practiceConfig) return;
    // Don't wipe in-progress edits when a background refetch returns.
    if (isDirtyRef.current) return;
    reset(toFormValues(practiceConfig));
  }, [practiceConfig, reset, toFormValues]);

  React.useEffect(() => {
    if (!configError) return;
    setToastType("error");
    setToastMessage(getApiErrorMessage(configError));
  }, [configError]);

  const onSubmit: SubmitHandler<PracticeConfigFormData> = async (data) => {
    try {
      const saved = await updatePracticeConfiguration({
        practiceName: data.name || "",
        subtitle: data.subtitle || "",
        description: data.description || "",
        practiceAddress: data.address || "",
        practicePhone: data.phone || "",
        practiceEmail: data.email || "",
        practiceWebsite: data.website || "",
        timezone: data.timezone || "",
        taxId: data.taxId || "",
        licenseNumber: data.licenseNumber || "",
        licenseState: data.licenseState || "",
        npiNumber: data.npiNumber || "",
      }).unwrap();
      reset(toFormValues(saved));
      await refetch();
      setToastType("success");
      setToastMessage("Practice configuration updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <SettingsLayout>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="h-full min-h-0 overflow-y-auto p-4 custom-scrollbar">
        {isLoadingConfig || isFetchingConfig ? (
          <ContentLoader size="md" className="rounded-xl border border-(--neutral-100) px-4 py-8 text-center text-sm text-(--text-neutral-500) mb-4 gap-2" />
        ) : null}
        <Form {...form}>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
            {/* Practice Information */}
            <section>
              <h3 className="font-bold text-(--text-primary-dark) mb-4">
                Practice Information
              </h3>
              <div className="grid grid-cols-2 gap-4">
                <CustomInput
                  control={control}
                  name="name"
                  label="Practice Name"
                  required
                  maxLength={PRACTICE_CONFIG_FIELD_LIMITS.name}
                  hint={`${watchedName.length}/${PRACTICE_CONFIG_FIELD_LIMITS.name}`}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <CustomInput
                  control={control}
                  name="subtitle"
                  label="Subtitle"
                  maxLength={PRACTICE_CONFIG_FIELD_LIMITS.subtitle}
                  hint={`${watchedSubtitle.length}/${PRACTICE_CONFIG_FIELD_LIMITS.subtitle}`}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <CustomInput
                  control={control}
                  name="taxId"
                  label="Tax ID"
                  required
                  maxLength={200}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <CustomInput
                  control={control}
                  name="npiNumber"
                  label="NPI Number"
                  required
                  maxLength={200}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <CustomInput
                  control={control}
                  name="licenseNumber"
                  label="License Number"
                  maxLength={200}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <CustomInput
                  control={control}
                  name="licenseState"
                  label="License State"
                  maxLength={200}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <div className="col-span-2 mt-2">
                  <CustomTextarea
                    control={control}
                    name="description"
                    label="Practice Description"
                    maxLength={PRACTICE_CONFIG_FIELD_LIMITS.description}
                    hint={`${watchedDescription.length}/${PRACTICE_CONFIG_FIELD_LIMITS.description}`}
                    className="rounded-2xl border-(--neutral-100) shadow-xs"
                  />
                </div>
              </div>
            </section>

            {/* Contact Information */}
            <section>
              <h3 className="font-bold text-(--text-primary-dark) mb-4">
                Contact Information
              </h3>
              <div className="grid grid-cols-2 gap-4">
                <CustomInput
                  control={control}
                  name="address"
                  label="Practice Address"
                  maxLength={PRACTICE_CONFIG_FIELD_LIMITS.address}
                  hint={`${watchedAddress.length}/${PRACTICE_CONFIG_FIELD_LIMITS.address}`}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <CustomInput
                  control={control}
                  name="phone"
                  label="Phone Number"
                  optionalPlusPhone
                  maxLength={PRACTICE_CONFIG_FIELD_LIMITS.phone}
                  hint={`${watchedPhone.length}/${PRACTICE_CONFIG_FIELD_LIMITS.phone} • Optional + prefix`}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />

                <CustomInput
                  control={control}
                  name="email"
                  label="Email"
                  maxLength={PRACTICE_CONFIG_FIELD_LIMITS.email}
                  hint={`${watchedEmail.length}/${PRACTICE_CONFIG_FIELD_LIMITS.email}`}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />
                <CustomInput
                  control={control}
                  name="website"
                  label="Website URL"
                  maxLength={PRACTICE_CONFIG_FIELD_LIMITS.website}
                  hint={`${watchedWebsite.length}/${PRACTICE_CONFIG_FIELD_LIMITS.website}`}
                  className="rounded-2xl border-(--neutral-100) shadow-xs"
                />

                <div className="space-y-2">
                  <CustomSelect
                    control={control}
                    name="timezone"
                    label="Select Timezone"
                    options={TIME_ZONE_OPTIONS}
                    isSearch={true}
                    className="rounded-2xl border-(--neutral-100) shadow-xs"
                  />
                </div>
              </div>
            </section>

            <div className="pt-6 space-y-4 pb-2">
              <Button
                type="submit"
                disabled={!isDirty || isSavingConfig}
                loading={isSavingConfig}
                loadingLabel="Saving..."
                className="rounded-full bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white px-8 h-12 font-bold shadow-lg cursor-pointer disabled:opacity-50"
              >
                Save Configuration
              </Button>
              <p className="text-xs text-(--text-neutral-600)">
                Configuration is automatically used in invoices, reports, and
                other documents.
              </p>
            </div>
          </form>
        </Form>
      </div>
    </SettingsLayout>
    </div>
  );
};

export default Administration;
