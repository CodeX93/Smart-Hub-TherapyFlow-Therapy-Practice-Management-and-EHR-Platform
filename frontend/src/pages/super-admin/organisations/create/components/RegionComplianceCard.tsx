import { useState, useMemo } from "react";
import type { UseFormReturn } from "react-hook-form";
import type { CreateOrganisationValues } from "../createOrganisation.schema";
import {
  DATA_RESIDENCY_OPTIONS,
  REGION_OPTIONS,
  TIMEZONE_OPTIONS,
} from "../../complianceOptions";
import SectionCard from "./SectionCard";
import FieldBlock from "./FieldBlock";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

const selectTriggerClassName = cn(
  "h-10 w-full rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] data-[placeholder]:text-[#97a4b0] [&_svg]:text-[#97a4b0]"
);

const RegionComplianceCard = ({
  form,
}: {
  form: UseFormReturn<CreateOrganisationValues>;
}) => {
  const {
    setValue,
    watch,
    formState: { errors },
  } = form;

  const [timezoneSearch, setTimezoneSearch] = useState("");

  const filteredTimezones = useMemo(() => {
    if (!timezoneSearch.trim()) return TIMEZONE_OPTIONS;
    const q = timezoneSearch.toLowerCase();
    return TIMEZONE_OPTIONS.filter((o) => o.label.toLowerCase().includes(q));
  }, [timezoneSearch]);

  return (
    <SectionCard
      title="Region & Compliance"
      subtitle="Data residency cannot be changed after creation."
      className="min-h-[18.125rem]"
    >
      <div className="flex flex-col gap-4">
        <FieldBlock label="Timezone" required error={errors.timezone?.message}>
          <Select
            value={watch("timezone") || undefined}
            onValueChange={function (value) {
              setValue("timezone", value, { shouldValidate: true });
            }}
          >
            <SelectTrigger className={selectTriggerClassName}>
              <SelectValue placeholder="Choose timezone" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)] max-h-[300px]">
              <div className="p-2 border-b border-[#dce5ee] sticky top-0 bg-white z-10">
                <input
                  type="text"
                  placeholder="Search timezone..."
                  className="w-full h-8 px-2 text-[0.875rem] text-[#2b3946] border border-[#dce5ee] rounded-md outline-none focus:border-[#97a4b0] placeholder:text-[#97a4b0]"
                  value={timezoneSearch}
                  onChange={(e) => setTimezoneSearch(e.target.value)}
                  onKeyDown={(e) => e.stopPropagation()}
                />
              </div>
              {filteredTimezones.length === 0 ? (
                <div className="p-3 text-center text-sm text-[#97a4b0]">No results found.</div>
              ) : (
                filteredTimezones.map(function (option) {
                  return (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                    >
                      {option.label}
                    </SelectItem>
                  );
                })
              )}
            </SelectContent>
          </Select>
        </FieldBlock>

        <FieldBlock
          label="Infrastructure Region"
          required
          error={errors.infrastructureRegion?.message}
        >
          <Select
            value={watch("infrastructureRegion") || undefined}
            onValueChange={function (value) {
              setValue("infrastructureRegion", value, { shouldValidate: true });
            }}
          >
            <SelectTrigger className={selectTriggerClassName}>
              <SelectValue placeholder="Choose infrastructure region" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {REGION_OPTIONS.map(function (option) {
                return (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                  >
                    {option.label}
                  </SelectItem>
                );
              })}
            </SelectContent>
          </Select>
        </FieldBlock>

        <FieldBlock
          label="Data Residency"
          required
          error={errors.dataResidency?.message}
        >
          <Select
            value={watch("dataResidency") || undefined}
            onValueChange={function (value) {
              setValue("dataResidency", value, { shouldValidate: true });
            }}
          >
            <SelectTrigger className={selectTriggerClassName}>
              <SelectValue placeholder="Choose data residency" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {DATA_RESIDENCY_OPTIONS.map(function (option) {
                return (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                  >
                    {option.label}
                  </SelectItem>
                );
              })}
            </SelectContent>
          </Select>
        </FieldBlock>

        <div className="w-full rounded-[0.625rem] border border-[#cce3df] bg-[#d9ebe7] px-4 py-3">
          <div className="text-[#4f6b69] text-[0.75rem] font-normal leading-5">
            Organization status will default to{" "}
            <span className="font-semibold text-[#355351]">Pending Activation</span>{" "}
            until the primary admin completes their setup and email verification.
          </div>
        </div>
      </div>
    </SectionCard>
  );
};

export default RegionComplianceCard;
