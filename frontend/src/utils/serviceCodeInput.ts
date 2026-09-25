import {
  INSURANCE_AMOUNT_PATTERN,
  isValidInsuranceAmount,
  sanitizeInsuranceAmountInput,
} from "@/utils/insuranceAmountInput";

export const SERVICE_FIELD_LIMITS = {
  serviceCode: 50,
  serviceName: 255,
  description: 1000,
  durationMin: 1,
  durationMax: 1440,
} as const;

export const SERVICE_CODE_PATTERN = /^[A-Za-z0-9_-]+$/;

export function sanitizeServiceCodeInput(value: string): string {
  return value.replace(/[^A-Za-z0-9_-]/g, "").slice(0, SERVICE_FIELD_LIMITS.serviceCode);
}

export function sanitizeServiceDurationInput(value: string): string {
  return value.replace(/\D/g, "").slice(0, 4);
}

export function isValidServiceDuration(value?: string | null): boolean {
  const trimmed = value?.trim();
  if (!trimmed) return false;

  if (!/^\d+$/.test(trimmed)) return false;

  const parsed = Number.parseInt(trimmed, 10);
  return (
    Number.isFinite(parsed) &&
    parsed >= SERVICE_FIELD_LIMITS.durationMin &&
    parsed <= SERVICE_FIELD_LIMITS.durationMax
  );
}

export const SERVICE_BASE_RATE_PATTERN = INSURANCE_AMOUNT_PATTERN;

export function sanitizeServiceBaseRateInput(value: string): string {
  return sanitizeInsuranceAmountInput(value);
}

export function isValidServiceBaseRate(value?: string | null): boolean {
  const trimmed = value?.trim();
  if (!trimmed) return false;
  return isValidInsuranceAmount(trimmed);
}

export function parseServiceBaseRate(value?: string | null): number | undefined {
  if (!isValidServiceBaseRate(value)) return undefined;
  return Number.parseFloat(value!.trim());
}

export function parseServiceDuration(value?: string | null): number | undefined {
  if (!isValidServiceDuration(value)) return undefined;
  return Number.parseInt(value!.trim(), 10);
}
