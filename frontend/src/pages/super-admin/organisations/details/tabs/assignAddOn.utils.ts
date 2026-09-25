export const ASSIGN_ADD_ON_LIMITS = {
  quantityMin: 1,
  quantityMax: 999_999,
} as const;

export const ASSIGN_ADD_ON_QUANTITY_MAX_DIGITS = String(
  ASSIGN_ADD_ON_LIMITS.quantityMax,
).length;

export function sanitizeAssignAddOnQuantity(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, ASSIGN_ADD_ON_QUANTITY_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > ASSIGN_ADD_ON_LIMITS.quantityMax) {
    return String(ASSIGN_ADD_ON_LIMITS.quantityMax);
  }

  return digitsOnly;
}

export function validateAssignAddOnInput(values: {
  featureCode: string;
  quantity: string;
}): string | null {
  if (!values.featureCode.trim()) {
    return "Please select an add-on.";
  }

  const trimmedQuantity = values.quantity.trim();
  if (!trimmedQuantity) {
    return "Quantity is required.";
  }

  if (!/^\d+$/.test(trimmedQuantity)) {
    return "Enter a valid quantity.";
  }

  const parsed = Number.parseInt(trimmedQuantity, 10);
  if (
    !Number.isFinite(parsed) ||
    parsed < ASSIGN_ADD_ON_LIMITS.quantityMin ||
    parsed > ASSIGN_ADD_ON_LIMITS.quantityMax
  ) {
    return `Quantity must be between ${ASSIGN_ADD_ON_LIMITS.quantityMin} and ${ASSIGN_ADD_ON_LIMITS.quantityMax.toLocaleString()}.`;
  }

  return null;
}
