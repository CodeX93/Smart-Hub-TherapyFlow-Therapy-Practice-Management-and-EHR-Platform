
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from "react";
import { useParams } from "react-router-dom";
import { AlertCircle, X } from "lucide-react";
import ConsentHeader from "@/components/admin-consent-sections/ConsentHeader";
import ConsentSectionCard, {
  type FormSection,
} from "@/components/admin-consent-sections/ConsentSectionCard";
import AddFieldModal from "@/components/admin-consent-sections/AddFieldModal";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import {
  useGetFormTemplateByIdQuery,
  useCreateFormFieldMutation,
  useUpdateFormFieldMutation,
  useDeleteFormFieldMutation,
  useUpdateFormTemplateMutation,
} from "@/store/api/admin/clients.api";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Toast from "@/components/shared/Toast";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  addFieldSchema,
  type AddFieldValues,
} from "../../../../../types/consent.types";

function mapUiFieldTypeToApi(fieldType: string): string {
  switch (fieldType) {
    case "Heading (Read-only)":
      return "heading";
    case "Information Text (Read-only)":
      return "info_text";
    case "Fill-in-the-Blank":
      return "text";
    case "Short Text":
      return "text";
    case "Long Text":
      return "textarea";
    case "Dropdown":
      return "select";
    case "Radio Buttons":
      return "radio";
    case "Single Checkbox":
      return "checkbox";
    case "Multiple Checkboxes":
      return "multi_select";
    case "Date":
      return "date";
    case "Signature":
      return "signature";
    case "File Upload":
      return "file";
    default:
      return "text";
  }
}

function mapApiFieldTypeToUi(fieldType: string): string {
  switch ((fieldType || "").toLowerCase()) {
    case "heading":
      return "Heading (Read-only)";
    case "info_text":
      return "Information Text (Read-only)";
    case "text":
      return "Short Text";
    case "textarea":
      return "Long Text";
    case "select":
      return "Dropdown";
    case "radio":
      return "Radio Buttons";
    case "checkbox":
      return "Single Checkbox";
    case "multi_select":
      return "Multiple Checkboxes";
    case "date":
      return "Date";
    case "signature":
      return "Signature";
    case "file":
      return "File Upload";
    default:
      return "Short Text";
  }
}

function toValidJsonString(input?: string): string {
  const trimmed = (input || "").trim();
  if (!trimmed) return "{}";
  const parsed = JSON.parse(trimmed);
  return JSON.stringify(parsed);
}

function normalizeOptionsForPayload(options: unknown): string {
  if (typeof options === "string") return options;
  if (Array.isArray(options)) {
    return options
      .map((item) =>
        typeof item === "string"
          ? item
          : item && typeof item === "object"
            ? String((item as Record<string, unknown>).optionText ?? (item as Record<string, unknown>).label ?? (item as Record<string, unknown>).value ?? "")
            : "",
      )
      .filter(Boolean)
      .join(",");
  }
  return "";
}

const CreateConsent = () => {
  const { formId } = useParams<{ formId: string }>();
  const id = Number(formId);
  const [sections, setSections] = useState<FormSection[]>([]);
  const [isAddFieldModalOpen, setIsAddFieldModalOpen] = useState(false);
  const [editingFieldId, setEditingFieldId] = useState<string | null>(null);
  const [deleteFieldId, setDeleteFieldId] = useState<string | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [toastConfig, setToastConfig] = useState<{
    message: string;
    type: "success" | "error";
  } | null>(null);

  const {
    currentData: formTemplate,
    isLoading: isLoadingForm,
    isError,
    refetch: refetchTemplate,
  } = useGetFormTemplateByIdQuery(id, {
    skip: isNaN(id),
    refetchOnMountOrArgChange: true,
  });

  const [createFormField, { isLoading: isCreatingField }] =
    useCreateFormFieldMutation();
  const [updateFormField, { isLoading: isUpdatingField }] =
    useUpdateFormFieldMutation();
  const [deleteFormField, { isLoading: isDeletingField }] =
    useDeleteFormFieldMutation();
  const [updateFormTemplate, { isLoading: isSavingTemplate }] =
    useUpdateFormTemplateMutation();

  const [seededTemplateId, setSeededTemplateId] = useState<number | null>(null);
  if (formTemplate && seededTemplateId !== formTemplate.id) {
    setSeededTemplateId(formTemplate.id);

      const mappedSections: FormSection[] = [];
      
      // Support nested sections if present
      if (formTemplate.activeVersion?.sections) {
        formTemplate.activeVersion.sections.forEach((section) => {
          section.fields?.forEach((field) => {
            mappedSections.push({
              id: String(field.id),
              title: field.label,
              type: field.fieldType,
              content: field.placeholder || "",
              required: field.isRequired,
              options: Array.isArray(field.options)
                ? field.options
                    .map((o) =>
                      typeof o === "string"
                        ? o
                        : o?.optionText || o?.label || o?.value || "",
                    )
                    .filter(Boolean)
                    .join(",")
                : typeof field.options === "string"
                  ? field.options
                  : "",
            });
          });
        });
      } 
      // Support top-level fields
      else if (formTemplate.fields) {
        formTemplate.fields.forEach((field) => {
          mappedSections.push({
            id: String(field.id),
            title: field.label,
            type: field.fieldType,
            content: field.placeholder || "",
            required: field.isRequired,
            options: Array.isArray(field.options)
              ? field.options
                  .map((o) =>
                    typeof o === "string"
                      ? o
                      : o?.optionText || o?.label || o?.value || "",
                  )
                  .filter(Boolean)
                  .join(",")
              : typeof field.options === "string"
                ? field.options
                : "",
          });
        });
      }

      setSections(mappedSections);

  }

  const form = useForm<AddFieldValues>({
    resolver: zodResolver(addFieldSchema),
    defaultValues: {
      type: "Short Text",
      label: "",
      placeholder: "",
      helpText: "",
      required: false,
      headingText: "",
      sectionTitle: "",
      contentText: "",
      templateText: "",
      options: "",
    },
    mode: "onChange",
  });

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      setSections((items: FormSection[]) => {
        const oldIndex = items.findIndex(
          (item: FormSection) => item.id === active.id
        );
        const newIndex = items.findIndex(
          (item: FormSection) => item.id === over.id
        );
        return arrayMove(items, oldIndex, newIndex);
      });
    }
  };

  const handleMoveUp = (index: number) => {
    if (index === 0) return;
    const newSections = [...sections];
    [newSections[index - 1], newSections[index]] = [
      newSections[index],
      newSections[index - 1],
    ];
    setSections(newSections);
  };

  const handleMoveDown = (index: number) => {
    if (index === sections.length - 1) return;
    const newSections = [...sections];
    [newSections[index + 1], newSections[index]] = [
      newSections[index],
      newSections[index + 1],
    ];
    setSections(newSections);
  };

  const handleDelete = (id: string) => {
    setDeleteFieldId(id);
    setIsDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deleteFieldId) return;
    try {
      await deleteFormField({ id: Number(deleteFieldId), templateId: id }).unwrap();
      await refetchTemplate();
      setToastConfig({ message: "Field deleted successfully", type: "success" });
      setIsDeleteModalOpen(false);
      setDeleteFieldId(null);
    } catch (error) {
      setToastConfig({ message: getApiErrorMessage(error), type: "error" });
    }
  };

  const handleEdit = (id: string) => {
    const section = sections.find((entry) => entry.id === id);
    if (!section) return;
    const uiType = mapApiFieldTypeToUi(section.type);
    const isHeading = uiType === "Heading (Read-only)";
    const isInfoText = uiType === "Information Text (Read-only)";
    const isFillInBlank = uiType === "Fill-in-the-Blank";

    form.reset({
      type: uiType,
      label: section.title || "",
      placeholder:
        !isHeading && !isInfoText && !isFillInBlank ? section.content || "" : "",
      helpText: "",
      required: Boolean(section.required),
      headingText: isHeading ? section.title || "" : "",
      sectionTitle: isInfoText ? section.title || "" : "",
      contentText: isInfoText ? section.content || "" : "",
      templateText: isFillInBlank ? section.content || "" : "",
      options: section.options || "",
      defaultValue: "",
      autoPopulate: "",
      conditionalDisplay: "",
      validation: "",
    });
    setEditingFieldId(id);
    setIsAddFieldModalOpen(true);
  };

  const handleAddField = () => {
    setEditingFieldId(null);
    form.reset({
      type: "Short Text",
      label: "",
      placeholder: "",
      helpText: "",
      required: false,
      headingText: "",
      sectionTitle: "",
      contentText: "",
      templateText: "",
      options: "",
      defaultValue: "",
      autoPopulate: "",
      conditionalDisplay: "",
      validation: "",
    });
    setIsAddFieldModalOpen(true);
  };

  const handleFieldAdd = async (data: AddFieldValues) => {
    if (!id) return;

    try {
      let finalLabel = data.label || "";
      
      // Map specialized fields to label for backend validation
      if (data.type === "Heading (Read-only)") {
        finalLabel = data.headingText || "Heading";
      } else if (data.type === "Information Text (Read-only)") {
        finalLabel = data.sectionTitle || "Information";
      }

      const normalizedValidation = toValidJsonString(data.validation);
      const normalizedConditionalDisplay = toValidJsonString(data.conditionalDisplay);

      const payload = {
        templateId: id,
        fieldType: mapUiFieldTypeToApi(data.type),
        label: finalLabel,
        placeholder: data.type === "Fill-in-the-Blank" ? data.templateText || "" : data.placeholder || "",
        helpText: data.helpText || "",
        isRequired: data.required || false,
        options: data.options || "",
        defaultValue: data.defaultValue || "",
        autoPopulate: data.autoPopulate || "",
        conditionalDisplay: normalizedConditionalDisplay,
        validation: normalizedValidation,
        sortOrder: sections.length,
      };

      if (editingFieldId) {
        await updateFormField({
          id: Number(editingFieldId),
          body: {
            ...payload,
            sortOrder: sections.findIndex((entry) => entry.id === editingFieldId),
          },
        }).unwrap();
        setToastConfig({ message: "Field updated successfully", type: "success" });
      } else {
        await createFormField(payload).unwrap();
        setToastConfig({ message: "Field added successfully", type: "success" });
      }

      await refetchTemplate();
      setIsAddFieldModalOpen(false);
      setEditingFieldId(null);
      
      form.reset({
        type: "Short Text",
        label: "",
        placeholder: "",
        helpText: "",
        required: false,
        headingText: "",
        sectionTitle: "",
        contentText: "",
        templateText: "",
        options: "",
        defaultValue: "",
        autoPopulate: "",
        conditionalDisplay: "",
        validation: "",
      });
    } catch (error) {
      const fallbackMessage =
        error instanceof SyntaxError
          ? "Validation and conditional display must be valid JSON."
          : getApiErrorMessage(error);
      setToastConfig({ message: fallbackMessage, type: "error" });
    }
  };

  const handleCloseModal = () => {
    setIsAddFieldModalOpen(false);
    setEditingFieldId(null);
    form.reset();
  };

  const handlePreview = () => {
    setIsPreviewModalOpen(true);
  };

  const handleSaveTemplate = async () => {
    if (!id || !formTemplate) return;
    try {
      const sourceFields =
        formTemplate.activeVersion?.fields ??
        formTemplate.fields ??
        [];

      const sourceFieldById = new Map(
        sourceFields.map((field) => [String(field.id), field]),
      );

      const orderedFields = sections
        .map((section, index) => {
          const original = sourceFieldById.get(section.id);
          if (!original) return null;

          const canonicalFieldType = mapUiFieldTypeToApi(
            mapApiFieldTypeToUi(String(original.fieldType || "")),
          );

          return {
            templateId: id,
            fieldType: canonicalFieldType,
            label: String(original.label ?? section.title ?? ""),
            placeholder: String(original.placeholder ?? section.content ?? ""),
            helpText: String(original.helpText ?? ""),
            isRequired: Boolean(original.isRequired ?? section.required),
            options: normalizeOptionsForPayload(original.options),
            validation: toValidJsonString(
              typeof original.validation === "string" ? original.validation : "",
            ),
            defaultValue: String(original.defaultValue ?? ""),
            autoPopulate: String(original.autoPopulate ?? ""),
            conditionalDisplay: toValidJsonString(
              typeof original.conditionalDisplay === "string"
                ? original.conditionalDisplay
                : "",
            ),
            sortOrder: index,
          };
        })
        .filter((field) => field !== null);

      await updateFormTemplate({
        id,
        body: {
          name: formTemplate.name || "Untitled Form",
          description: formTemplate.description || "",
          category: formTemplate.category || "consent",
          instructions: formTemplate.instructions || "",
          requiresSignature: Boolean(formTemplate.requiresSignature),
          isActive: formTemplate.isActive ?? true,
          sortOrder: formTemplate.sortOrder ?? 0,
          fields: orderedFields,
        },
      }).unwrap();

      await refetchTemplate();
      setToastConfig({ message: "Form template saved successfully", type: "success" });
    } catch (error) {
      setToastConfig({ message: getApiErrorMessage(error), type: "error" });
    }
  };

  return (
    <div className="bg-(--bg-primary-light)">
      <div className="max-w-5xl mx-auto px-6 pb-20">
        <ConsentHeader
          title="Informed Consent"
          onAddField={handleAddField}
          onPreview={handlePreview}
          onSave={() => {
            void handleSaveTemplate();
          }}
          isSaving={isSavingTemplate}
        />

        {isLoadingForm ? (
          <div className="flex flex-col items-center justify-center py-20">
            <ContentLoader size="xl" className="mb-4" />
            <p className="text-gray-500 font-medium text-sm">Loading form content...</p>
          </div>
        ) : isError ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <div className="bg-red-50 p-4 rounded-2xl mb-4">
              <ContentLoader size="xl" className="rotate-45" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Failed to load form</h3>
            <p className="text-gray-500 max-w-sm">We couldn't retrieve the form template details. Please try again or contact support.</p>
          </div>
        ) : sections.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center border-2 border-dashed border-gray-200 rounded-3xl bg-white shadow-sm">
            <div className="bg-(--bg-primary-light) p-4 rounded-2xl mb-4">
              <AlertCircle className="text-(--primary-500)" size={40} />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No fields yet</h3>
            <p className="text-gray-500 max-w-sm mb-6">There is no field created yet please create one.</p>
            <button
              onClick={handleAddField}
              className="px-6 py-2.5 bg-(--primary-500) text-white rounded-xl font-medium hover:bg-(--primary-600) transition-all active:scale-95"
            >
              Add Your First Field
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <DndContext
              sensors={sensors}
              collisionDetection={closestCenter}
              onDragEnd={handleDragEnd}
            >
              <SortableContext
                items={sections.map((s: FormSection) => s.id)}
                strategy={verticalListSortingStrategy}
              >
                {sections.map((section: FormSection, index: number) => (
                  <ConsentSectionCard
                    key={section.id}
                    section={section}
                    onMoveUp={() => handleMoveUp(index)}
                    onMoveDown={() => handleMoveDown(index)}
                    onEdit={() => handleEdit(section.id)}
                    onDelete={() => handleDelete(section.id)}
                    isFirst={index === 0}
                    isLast={index === sections.length - 1}
                  />
                ))}
              </SortableContext>
            </DndContext>
          </div>
        )}
      </div>

      <AddFieldModal
        isOpen={isAddFieldModalOpen}
        onClose={handleCloseModal}
        onAdd={handleFieldAdd}
        form={form}
        mode={editingFieldId ? "edit" : "add"}
        isSubmitting={isCreatingField || isUpdatingField}
      />

      {toastConfig && (
        <Toast
          message={toastConfig.message}
          type={toastConfig.type}
          onClose={() => setToastConfig(null)}
        />
      )}

      <ConfirmationModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          if (isDeletingField) return;
          setIsDeleteModalOpen(false);
          setDeleteFieldId(null);
        }}
        onConfirm={() => {
          void handleConfirmDelete();
        }}
        title="Delete Field"
        description="Are you sure you want to delete this field? This action cannot be undone."
        confirmButtonText={isDeletingField ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingField}
        type="delete"
        items={[]}
      />

      {isPreviewModalOpen ? (
        <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="relative w-full max-w-3xl max-h-[90vh] bg-white rounded-2xl shadow-lg mx-4 flex flex-col overflow-hidden">
            <button
              onClick={() => setIsPreviewModalOpen(false)}
              className="absolute top-4 right-4 p-1 hover:bg-(--neutral-50) rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
            >
              <X size={20} />
            </button>
            <div className="px-6 py-5 border-b border-(--neutral-100)">
              <h3 className="text-lg font-semibold text-(--text-primary-dark)">Form Preview</h3>
              <p className="text-sm text-(--text-neutral-500)">
                {formTemplate?.name || "Informed Consent"}
              </p>
            </div>
            <div className="p-6 overflow-y-auto space-y-4">
              {sections.length === 0 ? (
                <div className="text-sm text-gray-500">No fields available for preview.</div>
              ) : (
                sections.map((section) => (
                  <div key={section.id} className="border border-(--neutral-100) rounded-xl p-4">
                    <div className="text-sm font-semibold text-(--text-primary-dark)">
                      {section.title}
                      {section.required ? <span className="text-red-500 ml-1">*</span> : null}
                    </div>
                    <div className="text-xs text-(--text-neutral-500) mt-1">
                      Type: {mapApiFieldTypeToUi(section.type)}
                    </div>
                    {section.options ? (
                      <div className="text-xs text-(--text-neutral-600) mt-2">
                        Options: {section.options}
                      </div>
                    ) : null}
                    {section.content ? (
                      <div className="text-xs text-(--text-neutral-600) mt-2">
                        {section.content}
                      </div>
                    ) : null}
                  </div>
                ))
              )}
            </div>
            <div className="px-6 py-4 border-t border-(--neutral-100) flex justify-end">
              <button
                onClick={() => setIsPreviewModalOpen(false)}
                className="px-6 py-2 rounded-full border border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--neutral-50) cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};

export default CreateConsent;
