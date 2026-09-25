import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  createFeatureSchema,
  type CreateFeatureValues,
} from "../createFeature.schema";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import FormFieldBlock from "./FormFieldBlock";
import DefaultEnabledRow from "./DefaultEnabledRow";

interface CreateFeatureFormProps {
  onCancel(): void;
}

function getInputClassName(): string {
  return cn(
    "h-12 rounded-[1rem] border-(--neutral-100) bg-(--surface-white) px-4 shadow-none",
    "placeholder:text-(--text-neutral-400) text-(--text-primary-dark)",
    "focus-visible:ring-0 focus-visible:border-(--neutral-100)"
  );
}

function getSelectTriggerClassName(): string {
  return cn(
    "w-full rounded-[1rem] border-(--neutral-100) bg-(--surface-white) px-4 shadow-none",
    "data-[size=default]:h-12",
    "text-(--text-primary-dark) [&_svg]:text-(--text-neutral-400)"
  );
}

function getSelectOptions(values: string[]) {
  return values.map(function (value) {
    return (
      <SelectItem
        key={value}
        value={value}
        className="text-(--text-primary-dark) focus:bg-(--bg-primary-50) focus:text-(--text-primary-dark)"
      >
        {value}
      </SelectItem>
    );
  });
}

function CreateFeatureForm(props: CreateFeatureFormProps) {
  const {
    register,
    setValue,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateFeatureValues>({
    resolver: zodResolver(createFeatureSchema),
    defaultValues: {
      featureKey: "",
      featureName: "",
      type: "Custom",
      scope: "Tenant",
      description: "",
      defaultEnabled: false,
    },
    mode: "onBlur",
  });

  const onSubmit = handleSubmit(function () {});

  return (
    <form onSubmit={onSubmit} className="w-full">
      <div className="w-full overflow-hidden rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) p-6 shadow-[0_1px_2px_0px_var(--shadow)]">
        <div className="grid grid-cols-1 gap-x-5 gap-y-5 md:grid-cols-2">
          <FormFieldBlock
            label="Feature Key"
            helper="Must be unique, lowercase, and use underscores."
            error={errors.featureKey?.message}
          >
            <Input
              placeholder="e.g., ai_notes"
              className={getInputClassName()}
              {...register("featureKey")}
            />
          </FormFieldBlock>

          <FormFieldBlock
            label="Feature Name"
            error={errors.featureName?.message}
          >
            <Input
              placeholder="e.g., ai_notes"
              className={getInputClassName()}
              {...register("featureName")}
            />
          </FormFieldBlock>

          <FormFieldBlock label="Type" error={errors.type?.message}>
            <Select
              value={watch("type")}
              onValueChange={function (value) {
                setValue("type", value as CreateFeatureValues["type"]);
              }}
            >
              <SelectTrigger className={getSelectTriggerClassName()}>
                <SelectValue placeholder="Custom" />
              </SelectTrigger>
              <SelectContent className="border-(--neutral-100) bg-(--surface-white) shadow-[0px_12px_24px_var(--shadow)]">
                {getSelectOptions(["Custom", "Core"])}
              </SelectContent>
            </Select>
          </FormFieldBlock>

          <FormFieldBlock label="Scope" error={errors.scope?.message}>
            <Select
              value={watch("scope")}
              onValueChange={function (value) {
                setValue("scope", value as CreateFeatureValues["scope"]);
              }}
            >
              <SelectTrigger className={getSelectTriggerClassName()}>
                <SelectValue placeholder="Tenant" />
              </SelectTrigger>
              <SelectContent className="border-(--neutral-100) bg-(--surface-white) shadow-[0px_12px_24px_var(--shadow)]">
                {getSelectOptions(["Tenant", "Global"])}
              </SelectContent>
            </Select>
          </FormFieldBlock>
        </div>

        <div className="mt-5">
          <FormFieldBlock
            label="Description"
            error={errors.description?.message}
          >
            <Textarea
              placeholder="Briefly describe what this feature controls..."
              className={cn(
                "min-h-[8.5rem] rounded-[1rem] border-(--neutral-100) bg-(--surface-white) px-4 py-3 shadow-none",
                "placeholder:text-(--text-neutral-400) text-(--text-primary-dark)",
                "focus-visible:ring-0 focus-visible:border-(--neutral-100)"
              )}
              {...register("description")}
            />
          </FormFieldBlock>
        </div>

        <DefaultEnabledRow
          className="mt-4"
          checked={watch("defaultEnabled")}
          onChange={function (next) {
            setValue("defaultEnabled", next);
          }}
        />

        <div className="mt-8 flex items-center justify-end gap-3">
          <Button
            type="button"
            variant="secondary"
            size="md"
            onClick={props.onCancel}
          >
            Cancel
          </Button>
          <Button type="submit" variant="primary" size="md">
            Create Feature
          </Button>
        </div>
      </div>
    </form>
  );
}

export default CreateFeatureForm;
