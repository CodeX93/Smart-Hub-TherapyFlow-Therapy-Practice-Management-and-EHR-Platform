import { useEffect, useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomTextarea from "@/components/form/CustomTextarea";
import { cn } from "@/lib/utils";
import { Form } from "@/components/ui/form";
import { bulkAddSchema, type BulkAddFormData } from "@/schemas/admin-library.schema";

interface BulkTagsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImport: (tags: string[]) => Promise<void> | void;
  isSubmitting?: boolean;
}

const BulkTagsModal = ({
  isOpen,
  onClose,
  onImport,
  isSubmitting = false,
}: BulkTagsModalProps) => {
  const form = useForm<BulkAddFormData>({
    resolver: zodResolver(bulkAddSchema),
    defaultValues: { pastedData: "" },
  });

  const pastedData = form.watch("pastedData");

  useEffect(() => {
    if (isOpen) form.reset({ pastedData: "" });
  }, [isOpen, form]);

  const parsedTags = useMemo(() => {
    if (!pastedData?.trim()) return [];
    return Array.from(
      new Set(
        pastedData
          .split(/[\r\n,]+/)
          .map((tag) => tag.trim())
          .filter(Boolean),
      ),
    ).map((tag, index) => ({ id: index + 1, tag, isValid: tag.length > 0 }));
  }, [pastedData]);

  const validTags = parsedTags.filter((entry) => entry.isValid);
  const isValidImport =
    parsedTags.length > 0 && validTags.length === parsedTags.length;

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="w-full max-w-2xl bg-white rounded-3xl shadow-xl mx-4 flex flex-col max-h-[95vh] overflow-hidden">
        <div className="flex items-center justify-between p-6 pb-2">
          <h2 className="text-xl font-bold text-(--neutral-950)">Bulk Add Tags</h2>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="text-(--text-neutral-600) hover:text-(--neutral-950) transition-colors cursor-pointer p-1 rounded-full hover:bg-(--neutral-50)"
          >
            <X size={24} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto custom-scrollbar flex-1 space-y-6">
          <Form {...form}>
            <form className="space-y-6">
              <CustomTextarea
                control={form.control}
                name="pastedData"
                label="Paste Tags"
                placeholder="featured, recommended, intake&#10;or one tag per line"
                className="min-h-40"
                required
              />
            </form>
          </Form>

          {parsedTags.length > 0 ? (
            <p className="text-sm text-(--text-neutral-600) px-1">
              {validTags.length} tag{validTags.length === 1 ? "" : "s"} ready to apply.
            </p>
          ) : null}
        </div>

        <div className="p-6 pt-2 flex items-center justify-end gap-3 bg-white">
          <Button
            variant="outline"
            onClick={() => form.setValue("pastedData", "")}
            disabled={isSubmitting}
            className="h-11 px-8 rounded-full"
          >
            Clear
          </Button>
          <Button
            onClick={() => onImport(validTags.map((entry) => entry.tag))}
            disabled={!isValidImport || isSubmitting}
            className={cn(
              "h-11 px-8 rounded-full font-semibold",
              "bg-(--bg-primary-dark) text-white",
            )}
            loading={isSubmitting}
            loadingLabel="Applying..."
          >
            {`Apply Tags (${validTags.length})`}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default BulkTagsModal;
