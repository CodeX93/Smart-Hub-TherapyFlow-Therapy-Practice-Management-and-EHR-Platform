export const INVOICE_ADJUSTMENT_LIMITS = {
  reason: 500,
  amountWholeDigits: 7,
  amountDecimalPlaces: 2,
  amountMax: 9_999_999.99,
} as const;

export function parseCurrencyDisplay(value: string): number | null {
  const cleaned = value.replace(/[$,\s]/g, "");
  if (!cleaned) return null;
  const parsed = Number.parseFloat(cleaned);
  return Number.isFinite(parsed) ? parsed : null;
}

export function sanitizeAdjustmentAmount(value: string): string {
  let sanitized = value.replace(/[^0-9.]/g, "");
  const firstDot = sanitized.indexOf(".");

  if (firstDot !== -1) {
    const whole = sanitized.slice(0, firstDot);
    const decimal = sanitized
      .slice(firstDot + 1)
      .replace(/\./g, "")
      .slice(0, INVOICE_ADJUSTMENT_LIMITS.amountDecimalPlaces);
    sanitized =
      decimal.length > 0 || sanitized.endsWith(".")
        ? `${whole}.${decimal}`
        : whole;
  }

  const [wholePart = "", decimalPart] = sanitized.split(".");
  const limitedWhole = wholePart.slice(0, INVOICE_ADJUSTMENT_LIMITS.amountWholeDigits);

  if (decimalPart !== undefined) {
    return decimalPart.length > 0 || sanitized.endsWith(".")
      ? `${limitedWhole}.${decimalPart}`
      : limitedWhole;
  }

  return limitedWhole;
}

export function sanitizeAdjustmentReason(value: string): string {
  return value.slice(0, INVOICE_ADJUSTMENT_LIMITS.reason);
}

export function validateInvoiceAdjustment(values: {
  amount: string;
  reason: string;
  maxAmountUsd?: number | null;
  amountLabel?: string;
}): string | null {
  const amountLabel = values.amountLabel ?? "Amount";
  const trimmedAmount = values.amount.trim();

  if (!trimmedAmount) {
    return `Please enter a valid ${amountLabel.toLowerCase()}.`;
  }

  const amountUsd = Number.parseFloat(trimmedAmount);
  if (!Number.isFinite(amountUsd) || amountUsd <= 0) {
    return `Please enter a valid ${amountLabel.toLowerCase()}.`;
  }

  if (amountUsd > INVOICE_ADJUSTMENT_LIMITS.amountMax) {
    return `${amountLabel} is too large.`;
  }

  if (values.maxAmountUsd != null && amountUsd > values.maxAmountUsd) {
    return `${amountLabel} cannot exceed the current balance of $${values.maxAmountUsd.toFixed(2)}.`;
  }

  const trimmedReason = values.reason.trim();
  if (!trimmedReason) {
    return "Please enter a reason.";
  }

  if (trimmedReason.length > INVOICE_ADJUSTMENT_LIMITS.reason) {
    return "Reason is too long.";
  }

  return null;
}
