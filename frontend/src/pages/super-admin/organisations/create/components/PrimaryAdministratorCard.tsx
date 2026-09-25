import type { UseFormReturn } from "react-hook-form";
import type { CreateOrganisationValues } from "../createOrganisation.schema";
import {
  CREATE_ORG_FIELD_LIMITS,
  sanitizeEmail,
  sanitizePersonName,
} from "../createOrganisation.utils";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import SectionCard from "./SectionCard";
import FieldBlock from "./FieldBlock";

const inputClassName = cn(
  "h-10 rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] placeholder:text-[#97a4b0]",
  "focus-visible:border-[#dce5ee] focus-visible:ring-0"
);

const PrimaryAdministratorCard = ({
  form,
}: {
  form: UseFormReturn<CreateOrganisationValues>;
}) => {
  const {
    register,
    setValue,
    formState: { errors },
  } = form;
  const { ref: firstNameRef, ...firstNameField } = register("firstName");
  const { ref: lastNameRef, ...lastNameField } = register("lastName");
  const { ref: emailRef, ...emailField } = register("email");

  return (
    <SectionCard
      title="Primary Administrator"
      subtitle="This user will be created and assigned the org_admin role automatically."
      className="min-h-[18.125rem]"
    >
      <div className="flex flex-col gap-4">
        <FieldBlock label="First Name" required error={errors.firstName?.message}>
          <Input
            className={inputClassName}
            placeholder="Enter first name"
            maxLength={CREATE_ORG_FIELD_LIMITS.firstName}
            name={firstNameField.name}
            ref={firstNameRef}
            onBlur={firstNameField.onBlur}
            onChange={(event) => {
              const sanitized = sanitizePersonName(
                event.target.value,
                CREATE_ORG_FIELD_LIMITS.firstName,
              );
              event.target.value = sanitized;
              firstNameField.onChange(event);
              setValue("firstName", sanitized, { shouldValidate: true, shouldDirty: true });
            }}
          />
        </FieldBlock>

        <FieldBlock label="Last Name" required error={errors.lastName?.message}>
          <Input
            className={inputClassName}
            placeholder="Enter last name"
            maxLength={CREATE_ORG_FIELD_LIMITS.lastName}
            name={lastNameField.name}
            ref={lastNameRef}
            onBlur={lastNameField.onBlur}
            onChange={(event) => {
              const sanitized = sanitizePersonName(
                event.target.value,
                CREATE_ORG_FIELD_LIMITS.lastName,
              );
              event.target.value = sanitized;
              lastNameField.onChange(event);
              setValue("lastName", sanitized, { shouldValidate: true, shouldDirty: true });
            }}
          />
        </FieldBlock>

        <div>
          <FieldBlock
            label="Email Address"
            required
            error={errors.email?.message}
          >
            <Input
              className={inputClassName}
              placeholder="admin@organization.com"
              type="email"
              inputMode="email"
              autoComplete="email"
              maxLength={CREATE_ORG_FIELD_LIMITS.email}
              name={emailField.name}
              ref={emailRef}
              onBlur={emailField.onBlur}
              onChange={(event) => {
                const sanitized = sanitizeEmail(event.target.value);
                event.target.value = sanitized;
                emailField.onChange(event);
                setValue("email", sanitized, { shouldValidate: true, shouldDirty: true });
              }}
            />
          </FieldBlock>
          {!errors.email?.message ? (
            <div className="mt-1.5 text-[#a0acb8] text-[0.6875rem] font-normal leading-4">
              Must be globally unique across TherapyFlow.
            </div>
          ) : null}
        </div>
      </div>
    </SectionCard>
  );
};

export default PrimaryAdministratorCard;
