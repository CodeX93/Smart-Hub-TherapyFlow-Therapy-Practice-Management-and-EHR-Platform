import type { PortalFormAssignmentField } from "@/store/api/portalFormsApi";
import {
  isPortalFormEditableField,
  normalizePortalFieldType,
} from "@/utils/portalFormDisplay";

export function getPortalAssignmentFieldId(
  field: PortalFormAssignmentField,
): number {
  return field.id;
}

export function getPortalSavableFormFields(
  fields: PortalFormAssignmentField[],
): PortalFormAssignmentField[] {
  return fields.filter(isPortalFormEditableField);
}

export function getPortalFilledSavableFields(
  fields: PortalFormAssignmentField[],
  values: Record<number, string>,
): PortalFormAssignmentField[] {
  return getPortalSavableFormFields(fields).filter((field) =>
    Boolean(values[field.id]?.trim()),
  );
}

function isPngDataUrl(value: string): boolean {
  return value.startsWith("data:image/png;base64,");
}

function typedSignatureToPngDataUrl(text: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement("canvas");
    canvas.width = 500;
    canvas.height = 120;
    const context = canvas.getContext("2d");

    if (!context) {
      reject(new Error("Unable to create signature image."));
      return;
    }

    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.fillStyle = "#000000";
    context.font = "italic 36px Georgia, 'Times New Roman', serif";
    context.textAlign = "center";
    context.textBaseline = "middle";
    context.fillText(text, canvas.width / 2, canvas.height / 2);

    resolve(canvas.toDataURL("image/png"));
  });
}

export async function normalizePortalSignatureData(
  signature: string,
): Promise<string> {
  const trimmed = signature.trim();
  if (!trimmed) return "";

  // Drawn signatures are always data-URL images — keep them as-is.
  if (isPngDataUrl(trimmed) || trimmed.startsWith("data:image/")) {
    return trimmed;
  }

  // Only plain typed names are rendered into a script-style PNG.
  return typedSignatureToPngDataUrl(trimmed);
}

export function isPortalHeadingField(field: PortalFormAssignmentField): boolean {
  return normalizePortalFieldType(field.fieldType) === "heading";
}
