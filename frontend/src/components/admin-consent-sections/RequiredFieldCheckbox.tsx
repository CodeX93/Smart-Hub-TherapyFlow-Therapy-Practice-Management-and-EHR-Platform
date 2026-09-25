import {
  FormField,
  FormItem,
  FormControl,
  FormLabel,
} from "@/components/ui/form";
import { Checkbox } from "@/components/ui/checkbox";
import type { Control } from "react-hook-form";

interface RequiredFieldCheckboxProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  control: Control<any>;
}

const RequiredFieldCheckbox = ({ control }: RequiredFieldCheckboxProps) => {
  return (
    <FormField
      control={control}
      name="required"
      render={({ field }) => (
        <FormItem className="flex flex-row items-center space-x-2">
          <FormControl>
            <Checkbox
              id="required-field"
              checked={field.value}
              onCheckedChange={field.onChange}
              className="rounded border-(--neutral-200) data-[state=checked]:bg-(--bg-primary-dark) data-[state=checked]:border-(--bg-primary-dark)"
            />
          </FormControl>
          <FormLabel
            htmlFor="required-field"
            className="text-sm font-medium text-(--text-neutral-800) cursor-pointer"
          >
            Required Field
          </FormLabel>
        </FormItem>
      )}
    />
  );
};

export default RequiredFieldCheckbox;
