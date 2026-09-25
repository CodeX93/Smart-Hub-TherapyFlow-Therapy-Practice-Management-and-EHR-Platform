import { useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "../form/CustomInput";
import { useForm, type SubmitHandler, type Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form, FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import { roomSchema, type RoomFormData } from "@/schemas/settings.schema";
import CustomTextarea from "../form/CustomTextarea";
import {
  ROOM_FIELD_LIMITS,
  sanitizeRoomCapacityInput,
  sanitizeRoomNumberInput,
} from "@/utils/roomInput";

interface AddRoomModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: RoomFormData) => void;
  initialData?: RoomFormData;
  isSaving?: boolean;
  isLoadingInitialData?: boolean;
}

const AddRoomModal = ({
  isOpen,
  onClose,
  onSave,
  initialData,
  isSaving = false,
  isLoadingInitialData = false,
}: AddRoomModalProps) => {
  const isEdit = !!initialData;

  const form = useForm<RoomFormData>({
    resolver: zodResolver(roomSchema) as Resolver<RoomFormData>,
    mode: "onChange",
    defaultValues: {
      roomNumber: "",
      roomName: "",
      capacity: "",
      equipment: "",
      roomType: "PHYSICAL",
      isActive: true,
    },
  });

  const {
    handleSubmit,
    reset,
    formState: { isValid },
    control,
  } = form;

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        // Room type is always physical — ignore any historical VIRTUAL value in the form.
        reset({ ...initialData, roomType: "PHYSICAL" });
      } else {
        reset({
          roomNumber: "",
          roomName: "",
          capacity: "",
          equipment: "",
          roomType: "PHYSICAL",
          isActive: true,
        });
      }
    }
  }, [initialData, isOpen, reset]);

  if (!isOpen) return null;

  const onSubmit: SubmitHandler<RoomFormData> = (data) => {
    onSave({ ...data, roomType: "PHYSICAL" });
  };

  return (
    <div className="app-modal-overlay fixed inset-0 z-100 flex items-center justify-center p-4 backdrop-blur-[0.125rem]">
      <div className="app-modal-surface relative flex max-h-[90vh] w-full max-w-147 flex-col overflow-hidden rounded-xl transition-all duration-300 animate-in fade-in zoom-in-95">
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={isSaving}
          className="absolute top-6 right-6 z-10 p-1.5 hover:bg-slate-100 rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
        >
          <X size={20} />
        </button>

        <div className="shrink-0 rounded-t-xl bg-white px-5 pt-5 pr-16">
          {/* Header */}
          <div className="mb-4 pr-10">
            <h2 className="text-xl font-bold text-(--text-primary-dark)">
              {isEdit ? "Edit Room" : "Add New Room"}
            </h2>
            <p className="text-sm text-(--text-neutral-500) mt-1">
              {isEdit
                ? "Update therapy room details"
                : "Create a new therapy room"}
            </p>
          </div>
        </div>

        <Form {...form}>
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="flex min-h-0 flex-1 flex-col overflow-hidden"
          >
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-4">
              <div className="space-y-4">
            {isLoadingInitialData ? (
              <div className="rounded-xl border border-(--neutral-100) px-4 py-8 text-center text-sm text-(--text-neutral-500)">
                Loading room details...
              </div>
            ) : null}

            <FormField
              control={control}
              name="roomNumber"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput
                      label="Room Number"
                      required
                      hint="e.g. VR-02"
                      {...field}
                      value={field.value ?? ""}
                      onChange={(event) => {
                        const sanitized = sanitizeRoomNumberInput(event.target.value);
                        event.target.value = sanitized;
                        field.onChange(sanitized);
                      }}
                      maxLength={ROOM_FIELD_LIMITS.roomNumber}
                      className={!isEdit ? "border-(--neutral-950) border-[0.09375rem]" : ""}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="mt-3">
              <CustomInput
                control={control}
                name="roomName"
                label="Room Name"
                required
                hint="e.g. Consultation Room A"
                maxLength={ROOM_FIELD_LIMITS.roomName}
              />
            </div>

            <div className="mt-3">
              <FormField
                control={control}
                name="capacity"
                render={({ field }) => (
                  <FormItem>
                    <FormControl>
                      <CustomInput
                        label="Capacity"
                        type="text"
                        inputMode="numeric"
                        hint={`Optional. Whole number ${ROOM_FIELD_LIMITS.capacityMin}-${ROOM_FIELD_LIMITS.capacityMax}`}
                        {...field}
                        value={field.value ?? ""}
                        onChange={(event) => {
                          const sanitized = sanitizeRoomCapacityInput(event.target.value);
                          event.target.value = sanitized;
                          field.onChange(sanitized);
                        }}
                        maxLength={4}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <CustomTextarea
              control={control}
              name="equipment"
              label="Equipment"
              placeholder="TV, whiteboard..."
              maxLength={ROOM_FIELD_LIMITS.equipment}
            />
              </div>
            </div>

            {/* Footer Actions */}
            <div className="flex shrink-0 items-center justify-end gap-3 border-t border-(--neutral-100) px-5 py-4">
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={onClose}
                disabled={isSaving}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="lg"
                disabled={!isValid || isSaving || isLoadingInitialData}
                loading={isSaving}
                loadingLabel="Saving..."
              >
                {isEdit ? "Update Room" : "Add Room"}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default AddRoomModal;
