import type { InvoicePolicyFormData } from "@/schemas/settings.schema";
import type {
  InvoicePolicyRequest,
  InvoicePolicyResponse,
  InvoicePolicyServiceOption,
} from "@/store/api/admin/invoicePolicy.api";
import type { SystemOptionValue } from "@/store/api/admin/systemOptions.api";
import { resolveOptionKey } from "@/utils/systemOptions";

export const INVOICE_POLICY_FIELD_LIMITS = {
  policyName: 100,
} as const;

export const INVOICE_POLICY_ALL_SCOPE_KEY = "all";

export function isInvoicePolicyAllScope(value: string | null | undefined): boolean {
  return (value ?? "").trim().toLowerCase() === INVOICE_POLICY_ALL_SCOPE_KEY;
}

export function resolveInvoicePolicyServiceFormValue(
  policy: Pick<InvoicePolicyResponse, "serviceId" | "serviceScopeKey">,
): string {
  if (
    policy.serviceId == null ||
    isInvoicePolicyAllScope(policy.serviceScopeKey)
  ) {
    return INVOICE_POLICY_ALL_SCOPE_KEY;
  }
  return String(policy.serviceId);
}

export function mapInvoicePolicyToFormData(
  policy: InvoicePolicyResponse,
  clientTypes: SystemOptionValue[] = [],
  sessionStatuses: SystemOptionValue[] = [],
): InvoicePolicyFormData {
  return {
    clientTypeKey: resolveOptionKey(clientTypes, policy.clientTypeKey),
    appointmentStatusKey: resolveOptionKey(sessionStatuses, policy.appointmentStatusKey),
    priceType: policy.priceType,
    invoicePrice: String(policy.invoicePrice),
    policyName: policy.policyName ?? "",
    serviceId: resolveInvoicePolicyServiceFormValue(policy),
    priority: policy.priority != null ? String(policy.priority) : "",
    effectiveFrom: policy.effectiveFrom ?? "",
    effectiveTo: policy.effectiveTo ?? "",
    enabled: policy.enabled,
  };
}

export function resolveServiceSelection(
  selectedValue: string,
  serviceOptions: InvoicePolicyServiceOption[],
): { serviceId: number | null; serviceScopeKey: string | null } {
  if (isInvoicePolicyAllScope(selectedValue)) {
    return {
      serviceId: null,
      serviceScopeKey: INVOICE_POLICY_ALL_SCOPE_KEY,
    };
  }

  const matched = serviceOptions.find((option) => {
    if (option.allServices || isInvoicePolicyAllScope(option.optionKey)) {
      return false;
    }
    return option.serviceId != null && String(option.serviceId) === selectedValue;
  });

  if (matched?.serviceId != null) {
    return {
      serviceId: matched.serviceId,
      serviceScopeKey: null,
    };
  }

  const parsed = Number.parseInt(selectedValue, 10);
  if (Number.isFinite(parsed) && parsed > 0) {
    return {
      serviceId: parsed,
      serviceScopeKey: null,
    };
  }

  return {
    serviceId: null,
    serviceScopeKey: INVOICE_POLICY_ALL_SCOPE_KEY,
  };
}

export function buildInvoicePolicyRequest(
  data: InvoicePolicyFormData,
  clientTypes: SystemOptionValue[],
  sessionStatuses: SystemOptionValue[],
  serviceOptions: InvoicePolicyServiceOption[] = [],
): InvoicePolicyRequest {
  const clientType = clientTypes.find((option) => option.optionKey === data.clientTypeKey);
  const sessionStatus = sessionStatuses.find(
    (option) => option.optionKey === data.appointmentStatusKey,
  );
  const { serviceId, serviceScopeKey } = resolveServiceSelection(
    data.serviceId,
    serviceOptions,
  );

  const priority = data.priority?.trim()
    ? Number.parseInt(data.priority, 10)
    : null;

  return {
    clientTypeKey: data.clientTypeKey,
    clientTypeLabel: clientType?.optionLabel ?? data.clientTypeKey,
    appointmentStatusKey: data.appointmentStatusKey,
    appointmentStatusLabel: sessionStatus?.optionLabel ?? data.appointmentStatusKey,
    enabled: data.enabled,
    priceType: data.priceType,
    invoicePrice: Number.parseFloat(data.invoicePrice),
    policyName: data.policyName?.trim() || null,
    serviceId,
    serviceScopeKey,
    effectiveFrom: data.effectiveFrom?.trim() || null,
    effectiveTo: data.effectiveTo?.trim() || null,
    priority: Number.isFinite(priority as number) ? priority : null,
  };
}

export function formatPolicyPrice(
  priceType: string,
  invoicePrice: number,
): string {
  if (priceType === "PERCENTAGE") {
    return `${invoicePrice}%`;
  }
  return `$${invoicePrice.toFixed(2)}`;
}

export function formatInvoicePolicyClientTypeLabel(
  policy: Pick<InvoicePolicyResponse, "clientTypeKey" | "clientTypeLabel">,
): string {
  if (isInvoicePolicyAllScope(policy.clientTypeKey)) {
    return policy.clientTypeLabel?.trim() || "All client types";
  }
  return policy.clientTypeLabel?.trim() || policy.clientTypeKey;
}

export function formatInvoicePolicySessionStatusLabel(
  policy: Pick<InvoicePolicyResponse, "appointmentStatusKey" | "appointmentStatusLabel">,
): string {
  if (isInvoicePolicyAllScope(policy.appointmentStatusKey)) {
    return policy.appointmentStatusLabel?.trim() || "All session statuses";
  }
  return policy.appointmentStatusLabel?.trim() || policy.appointmentStatusKey;
}

export function formatInvoicePolicyServiceScopeLabel(
  policy: Pick<InvoicePolicyResponse, "serviceId" | "serviceScopeKey">,
  serviceNameById: Map<number, string>,
): string {
  if (policy.serviceId == null || isInvoicePolicyAllScope(policy.serviceScopeKey)) {
    return "All services";
  }
  return serviceNameById.get(policy.serviceId) ?? `Service #${policy.serviceId}`;
}

export function toInvoicePolicyServiceSelectOptions(
  serviceOptions: InvoicePolicyServiceOption[],
): Array<{ value: string; label: string }> {
  const mapped = serviceOptions
    .map((option) => {
      if (option.allServices || isInvoicePolicyAllScope(option.optionKey) || option.serviceId == null) {
        return {
          value: INVOICE_POLICY_ALL_SCOPE_KEY,
          label: option.optionLabel || "All services",
        };
      }
      return {
        value: String(option.serviceId),
        label: option.optionLabel || `Service #${option.serviceId}`,
      };
    })
    .filter((option) => Boolean(option.value) && Boolean(option.label));

  const withoutDuplicateAll = mapped.filter(
    (option, index, list) =>
      !(
        isInvoicePolicyAllScope(option.value) &&
        list.findIndex((entry) => isInvoicePolicyAllScope(entry.value)) !== index
      ),
  );

  const hasAll = withoutDuplicateAll.some((option) =>
    isInvoicePolicyAllScope(option.value),
  );

  if (hasAll) return withoutDuplicateAll;

  return [
    { value: INVOICE_POLICY_ALL_SCOPE_KEY, label: "All services" },
    ...withoutDuplicateAll,
  ];
}
