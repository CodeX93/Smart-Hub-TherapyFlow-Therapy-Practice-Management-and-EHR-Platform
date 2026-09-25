import { paymentMethods } from "@/pages/therapist/therapist.static";

/** Turn backend keys like `canada_life` / `credit_card` into UI labels. */
export function humanizeKeyLabel(value: string): string {
  return value
    .trim()
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

const EXTRA_PAYMENT_METHOD_LABELS: Record<string, string> = {
  credit_balance: "Credit Balance",
};

/**
 * Map payment method API keys to the same labels used in Record Payment.
 * Falls back to a title-cased key when unknown.
 */
export function formatPaymentMethodLabel(
  value: string | null | undefined,
): string | undefined {
  if (!value?.trim()) return undefined;

  const trimmed = value.trim();
  const normalized = trimmed.toLowerCase().replace(/[\s-]+/g, "_");

  const fromStatic = paymentMethods.find(
    (method) =>
      method.value.toLowerCase() === normalized ||
      method.label.toLowerCase() === trimmed.toLowerCase(),
  );
  if (fromStatic) return fromStatic.label;

  const extra = EXTRA_PAYMENT_METHOD_LABELS[normalized];
  if (extra) return extra;

  return humanizeKeyLabel(trimmed);
}
