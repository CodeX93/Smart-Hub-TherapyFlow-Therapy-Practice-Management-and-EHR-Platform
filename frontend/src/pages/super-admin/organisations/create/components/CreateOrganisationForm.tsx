import { useEffect, useMemo } from "react";
import { useForm, type FieldErrors } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  createOrganisationSchema,
  type CreateOrganisationValues,
} from "../createOrganisation.schema";
import OrganisationBasicsCard from "./OrganisationBasicsCard";
import PrimaryAdministratorCard from "./PrimaryAdministratorCard";
import PlanBillingCard from "./PlanBillingCard";
import RegionComplianceCard from "./RegionComplianceCard";
import {
  useCreateOrganisationMutation,
  useGetBillingPlansQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { useNavigate } from "react-router-dom";

function getFirstFormErrorMessage(
  fieldErrors: FieldErrors<CreateOrganisationValues>
): string | null {
  for (const value of Object.values(fieldErrors)) {
    if (value && typeof value === "object" && "message" in value && value.message) {
      return String(value.message);
    }
  }

  return null;
}

const CreateOrganisationForm = ({
  onSubmittingChange,
  onToast,
}: {
  onSubmittingChange?: (isSubmitting: boolean) => void;
  onToast?: (type: "success" | "error", message: string) => void;
}) => {
  const navigate = useNavigate();
  const [createOrganisation, { isLoading: isCreating }] =
    useCreateOrganisationMutation();
  const {
    data: plansData,
    isLoading: isPlansLoading,
    isError: isPlansError,
    error: plansError,
  } = useGetBillingPlansQuery();
  const planOptions = useMemo(
    () =>
      (plansData ?? []).filter((plan) => plan.status.trim().toUpperCase() === "ACTIVE"),
    [plansData]
  );
  const plansErrorMessage = isPlansError ? getApiErrorMessage(plansError) : null;
  const form = useForm<CreateOrganisationValues>({
    resolver: zodResolver(createOrganisationSchema),
    defaultValues: {
      organisationName: "",
      tenantSlug: "",
      industry: "",
      firstName: "",
      lastName: "",
      email: "",
      plan: "",
      billingCycle: "",
      trialDays: "",
      therapistsOverride: "",
      supervisorsOverride: "",
      clientsOverride: "",
      timezone: "",
      infrastructureRegion: "",
      dataResidency: "",
    },
    mode: "onBlur",
  });

  useEffect(() => {
    if (!plansErrorMessage) return;
    onToast?.("error", plansErrorMessage);
  }, [onToast, plansErrorMessage]);

  async function handleSubmit(values: CreateOrganisationValues) {
    try {
      await createOrganisation({
        name: values.organisationName.trim(),
        slug: values.tenantSlug.trim(),
        subdomain: values.tenantSlug.trim(),
        primaryAdminEmail: values.email.trim(),
        primaryAdminFirstName: values.firstName.trim(),
        primaryAdminLastName: values.lastName.trim(),
        plan: values.plan.trim(),
        billingCycle: values.billingCycle.trim(),
        trialDays: Number.parseInt(values.trialDays, 10),
        timezone: values.timezone.trim(),
        region: values.infrastructureRegion.trim(),
        dataResidency: values.dataResidency.trim(),
        provisionTenant: true,
      }).unwrap();

      onToast?.("success", "Organization created successfully.");
      window.setTimeout(() => {
        navigate("/super-admin/organisations", {
          state: {
            shouldRefetch: true,
          },
        });
      }, 800);
    } catch (error) {
      const apiError = error as {
        data?: {
          details?: {
            errors?: Record<string, string>;
          };
          message?: string;
        };
      };
      const fieldErrors = apiError?.data?.details?.errors;
      const firstFieldError =
        fieldErrors && Object.keys(fieldErrors).length > 0
          ? fieldErrors[Object.keys(fieldErrors)[0]]
          : undefined;

      onToast?.(
        "error",
        firstFieldError ||
          apiError?.data?.message ||
          getApiErrorMessage(error) ||
          "Unable to create organisation right now.",
      );
    }
  }

  function handleInvalidSubmit(fieldErrors: FieldErrors<CreateOrganisationValues>) {
    const message = getFirstFormErrorMessage(fieldErrors);
    if (message) {
      onToast?.("error", message);
    }
  }

  useEffect(() => {
    onSubmittingChange?.(isCreating);
  }, [isCreating, onSubmittingChange]);

  return (
    <form
      id="create-organisation-form"
      onSubmit={form.handleSubmit(handleSubmit, handleInvalidSubmit)}
      className="w-full"
    >
      <div className="flex flex-col gap-4">
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-2 xl:items-stretch">
          <OrganisationBasicsCard form={form} />
          <PlanBillingCard
            form={form}
            planOptions={planOptions}
            isPlansLoading={isPlansLoading}
            plansErrorMessage={plansErrorMessage}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-2 xl:items-stretch">
          <PrimaryAdministratorCard form={form} />
          <RegionComplianceCard form={form} />
        </div>
      </div>
    </form>
  );
};

export default CreateOrganisationForm;
