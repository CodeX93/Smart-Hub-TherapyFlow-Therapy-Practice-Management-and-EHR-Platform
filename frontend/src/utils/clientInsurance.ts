import type { AdminClientSummary } from "@/store/api/admin/clients.api";

type InsuranceFields = Pick<
  AdminClientSummary,
  | "insuranceProvider"
  | "insuranceType"
  | "policyNumber"
  | "groupNumber"
  | "insurancePhone"
  | "copayAmount"
  | "deductible"
>;

export function hasClientInsuranceInformation(client: InsuranceFields): boolean {
  if (client.insuranceProvider?.trim()) return true;
  if (client.insuranceType?.trim()) return true;
  if (client.policyNumber?.trim()) return true;
  if (client.groupNumber?.trim()) return true;
  if (client.insurancePhone?.trim()) return true;
  if (typeof client.copayAmount === "number" && Number.isFinite(client.copayAmount)) {
    return true;
  }
  if (typeof client.deductible === "number" && Number.isFinite(client.deductible)) {
    return true;
  }
  return false;
}

export function mapClientInsuranceToFormValues(client: InsuranceFields) {
  return {
    insuranceInformation: hasClientInsuranceInformation(client),
    insuranceProvider: client.insuranceProvider || "",
    insuranceType: client.insuranceType || "",
    policyNumber: client.policyNumber || "",
    groupNumber: client.groupNumber || "",
    copayAmount:
      client.copayAmount !== undefined && client.copayAmount !== null
        ? String(client.copayAmount)
        : "",
    deductible:
      client.deductible !== undefined && client.deductible !== null
        ? String(client.deductible)
        : "",
    insurancePhone: client.insurancePhone || "",
  };
}
