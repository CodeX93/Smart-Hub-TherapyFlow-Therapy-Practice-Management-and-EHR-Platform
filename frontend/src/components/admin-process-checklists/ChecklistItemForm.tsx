import { X, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomSelect from "@/components/form/CustomSelect";
import { Checkbox } from "@/components/ui/checkbox";
import { CHECKLIST_CATEGORIES } from "@/pages/admin/content/content.static";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
} from "@/components/ui/form";
import type { UseFormReturn } from "react-hook-form";
import type { ChecklistItemValues } from "@/schemas/admin-checklist.schemas";
import CustomMultiSelect from "@/components/form/CustomMultiSelect";

interface ChecklistItemFormProps {
  form: UseFormReturn<ChecklistItemValues>;
  onSubmit: (data: ChecklistItemValues) => void;
  onCancel: () => void;
  isEditing: boolean;
}

const ChecklistItemForm = ({
  form,
  onSubmit,
  onCancel,
  isEditing,
}: ChecklistItemFormProps) => {
  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="flex flex-col justify-between h-full"
      >
        <div>
          {/* Form Header */}
          <div className="flex items-center justify-between p-6 border-b border-(--neutral-100)">
            <h2 className="text-lg font-semibold text-(--text-primary-dark) flex items-center gap-2">
              <ArrowLeft
                size={24}
                className="cursor-pointer text-(--text-neutral-600) hover:text-(--text-primary-dark) transition-colors"
                onClick={onCancel}
              />
              {isEditing ? "Edit Checklist Item" : "Create Checklist Item"}
            </h2>
            <button
              type="button"
              onClick={onCancel}
              className="text-(--text-neutral-400) hover:text-(--text-neutral-600) transition-colors duration-200 cursor-pointer"
            >
              <X size={24} />
            </button>
          </div>

          {/* Form Content */}
          <div className="p-6 space-y-6">
            {/* Item Title */}
            <CustomInput
              control={form.control}
              name="title"
              label="Item Title"
              required
              maxLength={120}
            />

            {/* Category */}
            <CustomSelect
              control={form.control}
              name="category"
              label="Category"
              options={CHECKLIST_CATEGORIES.filter(
                (cat) => cat !== "All Categories",
              ).map((cat) => ({ value: cat, label: cat }))}
              required
            />

            {/* Template */}
            <CustomMultiSelect
              label="Template"
              options={[
                { id: "template-1", label: "Refugee Clients" },
                { id: "template-2", label: "Refugee Clients Lorem Ipsum" },
                { id: "template-3", label: "Refugee Clients" },
                { id: "template-4", label: "Refugee Clients" },
                { id: "template-5", label: "Refugee Clients" },
              ]}
              value={form.watch("templates") || []}
              onChange={(selected) => form.setValue("templates", selected)}
              searchPlaceholder="Search templates..."
            />

            {/* Description */}
            <div className="space-y-1.5">
              <CustomTextarea
                control={form.control}
                name="description"
                label="Description"
                rows={4}
              />
              <p className="text-xs text-(--text-neutral-500)">
                Detailed description of the required action
              </p>
            </div>

            {/* Required Checkbox */}
            <FormField
              control={form.control}
              name="required"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center space-x-2 space-y-0">
                  <FormControl>
                    <Checkbox
                      id="required-item"
                      checked={field.value}
                      onCheckedChange={field.onChange}
                      className="rounded border-(--neutral-200) data-[state=checked]:bg-(--bg-primary-dark) data-[state=checked]:border-(--bg-primary-dark)"
                    />
                  </FormControl>
                  <FormLabel
                    htmlFor="required-item"
                    className="text-sm font-medium text-(--text-neutral-800) cursor-pointer"
                  >
                    Required checklist item
                  </FormLabel>
                </FormItem>
              )}
            />
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 p-6">
          <Button
            type="button"
            variant="ghost"
            onClick={onCancel}
            className="px-6 h-11.5 rounded-full border border-(--neutral-100) text-(--text-neutral-800) hover:bg-(--neutral-50) cursor-pointer"
          >
            Cancel
          </Button>
          <Button
            type="submit"
            className="px-6 h-11.5 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full cursor-pointer"
          >
            {isEditing ? "Update Item" : "Create Item"}
          </Button>
        </div>
      </form>
    </Form>
  );
};

export default ChecklistItemForm;
