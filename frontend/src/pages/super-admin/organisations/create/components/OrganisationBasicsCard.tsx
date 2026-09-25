import type { UseFormReturn } from "react-hook-form";
import type { CreateOrganisationValues } from "../createOrganisation.schema";
import {
  CREATE_ORG_FIELD_LIMITS,
  sanitizeOrganisationName,
  sanitizeTenantSlug,
} from "../createOrganisation.utils";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import SectionCard from "./SectionCard";
import FieldBlock from "./FieldBlock";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const inputClassName = cn(
  "h-10 rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] placeholder:text-[#97a4b0]",
  "focus-visible:border-[#dce5ee] focus-visible:ring-0"
);

const selectTriggerClassName = cn(
  "h-10 w-full rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] [&_svg]:text-[#97a4b0]"
);

const OrganisationBasicsCard = ({
  form,
}: {
  form: UseFormReturn<CreateOrganisationValues>;
}) => {
  const {
    register,
    setValue,
    watch,
    formState: { errors },
  } = form;
  const { ref: organisationNameRef, ...organisationNameField } = register("organisationName");
  const { ref: tenantSlugRef, ...tenantSlugField } = register("tenantSlug");

  return (
    <SectionCard
      title="Organization Basics"
      subtitle="Basic information and branding for the new tenant."
      className="min-h-[18.875rem]"
    >
      <div className="flex flex-col gap-4">
        <FieldBlock
          label="Organization Name"
          required
          error={errors.organisationName?.message}
        >
          <Input
            className={inputClassName}
            placeholder="Enter organization name"
            maxLength={CREATE_ORG_FIELD_LIMITS.organisationName}
            name={organisationNameField.name}
            ref={organisationNameRef}
            onBlur={organisationNameField.onBlur}
            onChange={(event) => {
              const sanitized = sanitizeOrganisationName(event.target.value);
              event.target.value = sanitized;
              organisationNameField.onChange(event);
              setValue("organisationName", sanitized, { shouldValidate: true, shouldDirty: true });
            }}
          />
        </FieldBlock>

        <div>
          <FieldBlock
            label="Tenant Slug"
            required
            error={errors.tenantSlug?.message}
          >
            <div className="grid grid-cols-[3.25rem_1fr] gap-2">
              <Input value="/org/" disabled className={inputClassName} />
              <Input
                className={inputClassName}
                placeholder="your-organization-slug"
                maxLength={CREATE_ORG_FIELD_LIMITS.tenantSlug}
                name={tenantSlugField.name}
                ref={tenantSlugRef}
                onBlur={tenantSlugField.onBlur}
                onChange={(event) => {
                  const sanitized = sanitizeTenantSlug(event.target.value);
                  event.target.value = sanitized;
                  tenantSlugField.onChange(event);
                  setValue("tenantSlug", sanitized, { shouldValidate: true, shouldDirty: true });
                }}
              />
            </div>
          </FieldBlock>
          {!errors.tenantSlug?.message ? (
            <div className="mt-1.5 text-[#a0acb8] text-[0.6875rem] font-normal leading-4">
              Must be unique, lowercase, and hyphenated.
            </div>
          ) : null}
        </div>

        <FieldBlock
          label="Industry / Specialty"
          helper="(Optional)"
          error={errors.industry?.message}
        >
          <Select
            value={watch("industry") || undefined}
            onValueChange={function (value) {
              setValue("industry", value, { shouldValidate: true });
            }}
          >
            <SelectTrigger className={selectTriggerClassName}>
              <SelectValue placeholder="Select industry (optional)" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {[
                "Mental Health Clinic",
                "Group Practice",
                "Behavioral Health",
                "Wellness Center",
              ].map(function (option) {
                return (
                  <SelectItem
                    key={option}
                    value={option}
                    className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                  >
                    {option}
                  </SelectItem>
                );
              })}
            </SelectContent>
          </Select>
        </FieldBlock>
      </div>
    </SectionCard>
  );
};

export default OrganisationBasicsCard;
