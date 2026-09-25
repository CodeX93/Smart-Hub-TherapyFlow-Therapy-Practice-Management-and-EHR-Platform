import type { AdminTaskFiltersState } from "@/components/admin-task-sections/AdminTaskFilters";
import type { TaskFiltersState } from "@/components/therapist/tasks/TaskFilters";
import type { SchedulingFilters } from "@/components/scheduling-sections/SchedulingFilterDropdown";
import type { BillingFilters } from "@/types/billing.type";
import type { ClientFilters } from "@/types/client.type";
import type { AppliedFilterChip } from "@/types/appliedFilters";
import type { HIPAAFilters } from "@/types/hipaa.types";
import type { FormStatus } from "@/types/clinical-form.type";
import { BILLING_FILTERS } from "@/pages/therapist/therapist.static";
import { actionTypes, riskLevels } from "@/pages/admin/compliance/compliance.static";
import {
  CLIENT_INVOICE_INSURANCE_OPTIONS,
  CLIENT_INVOICE_STATUS_OPTIONS,
  type ClientInvoiceFilters,
} from "@/utils/clientInvoiceFilters";

import { formatOptionKeyFallback } from "@/utils/systemOptions";

function formatFilterDate(date: Date): string {
  return date.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function labelFromOptions(
  value: string,
  options: { value: string; label: string }[],
): string {
  return options.find((option) => option.value === value)?.label ?? value;
}

function capitalize(value: string): string {
  if (!value) return value;
  return value.charAt(0).toUpperCase() + value.slice(1).replace(/_/g, " ");
}

export type ClientFilterLabelMaps = {
  status?: Map<string, string>;
  stage?: Map<string, string>;
  clientType?: Map<string, string>;
  checklistTemplate?: Map<string, string>;
  reportTemplate?: Map<string, string>;
};

function resolveFilterLabel(
  value: string,
  map?: Map<string, string>,
): string {
  if (!map) return capitalize(value);
  return map.get(value) ?? map.get(value.toLowerCase()) ?? formatOptionKeyFallback(value);
}

const QUICK_CLIENT_FILTER_LABELS: Array<{
  key: keyof ClientFilters;
  label: string;
}> = [
  { key: "hasPortalAccess", label: "Has portal access" },
  { key: "hasPendingTasks", label: "Has pending tasks" },
  { key: "hasNoSessions", label: "Has no sessions" },
  { key: "noSessions", label: "Has no sessions" },
  { key: "needsFollowUp", label: "Needs follow-up" },
  { key: "unassigned", label: "Unassigned" },
];

export function buildClientFilterChips(
  filters: ClientFilters,
  therapistOptions: { value: string; label: string }[] = [],
  labelMaps?: ClientFilterLabelMaps,
): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (filters.clientStatus?.length) {
    const value = filters.clientStatus[0];
    chips.push({
      id: `clientStatus:${value}`,
      label: `Status: ${resolveFilterLabel(value, labelMaps?.status)}`,
    });
  }

  if (filters.clientStage?.length) {
    const value = filters.clientStage[0];
    chips.push({
      id: `clientStage:${value}`,
      label: `Stage: ${resolveFilterLabel(value, labelMaps?.stage)}`,
    });
  }

  if (filters.clientType?.length) {
    const value = filters.clientType[0];
    chips.push({
      id: `clientType:${value}`,
      label: `Type: ${resolveFilterLabel(value, labelMaps?.clientType)}`,
    });
  }

  if (filters.assignedTherapist?.length) {
    const value = filters.assignedTherapist[0];
    const therapistLabel =
      therapistOptions.find((option) => option.value === value)?.label ?? value;
    chips.push({
      id: `assignedTherapist:${value}`,
      label: `Therapist: ${therapistLabel}`,
    });
  }

  if (filters.checklistTemplate?.length) {
    const value = filters.checklistTemplate[0];
    chips.push({
      id: `checklistTemplate:${value}`,
      label: `Checklist: ${resolveFilterLabel(value, labelMaps?.checklistTemplate)}`,
    });
  }

  if (filters.reportTemplate?.length) {
    const value = filters.reportTemplate[0];
    chips.push({
      id: `reportTemplate:${value}`,
      label: `Report: ${resolveFilterLabel(value, labelMaps?.reportTemplate)}`,
    });
  }

  for (const quickFilter of QUICK_CLIENT_FILTER_LABELS) {
    if (!filters[quickFilter.key]) continue;
    if (quickFilter.key === "noSessions" && filters.hasNoSessions) continue;
    chips.push({
      id: quickFilter.key,
      label: quickFilter.label,
    });
  }

  return chips;
}

export function removeClientFilterChip(
  filters: ClientFilters,
  chipId: string,
): ClientFilters {
  const next: ClientFilters = { ...filters };

  if (chipId.startsWith("clientStatus:")) {
    next.clientStatus = [];
    return next;
  }
  if (chipId.startsWith("clientStage:")) {
    next.clientStage = [];
    return next;
  }
  if (chipId.startsWith("clientType:")) {
    next.clientType = [];
    return next;
  }
  if (chipId.startsWith("assignedTherapist:")) {
    next.assignedTherapist = [];
    return next;
  }
  if (chipId.startsWith("checklistTemplate:")) {
    next.checklistTemplate = [];
    return next;
  }
  if (chipId.startsWith("reportTemplate:")) {
    next.reportTemplate = [];
    return next;
  }
  if (chipId === "hasNoSessions" || chipId === "noSessions") {
    next.hasNoSessions = false;
    next.noSessions = false;
    return next;
  }
  if (chipId in next) {
    (next as Record<string, unknown>)[chipId] = false;
  }

  return next;
}

export function buildBillingFilterChips(filters: BillingFilters): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (filters.billingStatus) {
    chips.push({
      id: `billingStatus:${filters.billingStatus}`,
      label: `Status: ${labelFromOptions(filters.billingStatus, BILLING_FILTERS.billingStatusOptions)}`,
    });
  }

  if (filters.paymentStatus) {
    chips.push({
      id: `paymentStatus:${filters.paymentStatus}`,
      label: `Payment: ${labelFromOptions(filters.paymentStatus, BILLING_FILTERS.paymentStatusOptions)}`,
    });
  }

  if (filters.paymentMethod) {
    chips.push({
      id: `paymentMethod:${filters.paymentMethod}`,
      label: `Method: ${labelFromOptions(filters.paymentMethod, BILLING_FILTERS.paymentMethodOptions)}`,
    });
  }

  if (filters.clientType) {
    chips.push({
      id: `clientType:${filters.clientType}`,
      label: `Client type: ${labelFromOptions(filters.clientType, BILLING_FILTERS.clientTypeFilterOptions)}`,
    });
  }

  if (filters.sessionType) {
    chips.push({
      id: `sessionType:${filters.sessionType}`,
      label: `Session type: ${labelFromOptions(filters.sessionType, BILLING_FILTERS.sessionTypeFilterOptions)}`,
    });
  }

  if (filters.serviceCode) {
    chips.push({
      id: `serviceCode:${filters.serviceCode}`,
      label: `Service: ${filters.serviceCode}`,
    });
  }

  if (filters.minAmount !== null && filters.minAmount !== undefined) {
    chips.push({
      id: "minAmount",
      label: `Min: $${filters.minAmount}`,
    });
  }

  if (filters.maxAmount !== null && filters.maxAmount !== undefined) {
    chips.push({
      id: "maxAmount",
      label: `Max: $${filters.maxAmount}`,
    });
  }

  if (filters.startDate) {
    chips.push({
      id: "startDate",
      label: `From: ${formatFilterDate(filters.startDate)}`,
    });
  }

  if (filters.endDate) {
    chips.push({
      id: "endDate",
      label: `To: ${formatFilterDate(filters.endDate)}`,
    });
  }

  return chips;
}

export function removeBillingFilterChip(
  filters: BillingFilters,
  chipId: string,
): BillingFilters {
  const next = { ...filters };

  if (chipId.startsWith("billingStatus:")) {
    next.billingStatus = null;
    return next;
  }
  if (chipId.startsWith("paymentStatus:")) {
    next.paymentStatus = null;
    return next;
  }
  if (chipId.startsWith("paymentMethod:")) {
    next.paymentMethod = null;
    return next;
  }
  if (chipId.startsWith("clientType:")) {
    next.clientType = null;
    return next;
  }
  if (chipId.startsWith("sessionType:")) {
    next.sessionType = null;
    return next;
  }
  if (chipId.startsWith("serviceCode:")) {
    next.serviceCode = null;
    return next;
  }
  if (chipId === "minAmount") {
    next.minAmount = null;
    return next;
  }
  if (chipId === "maxAmount") {
    next.maxAmount = null;
    return next;
  }
  if (chipId === "startDate") {
    next.startDate = null;
    return next;
  }
  if (chipId === "endDate") {
    next.endDate = null;
    return next;
  }

  return next;
}

export function buildSchedulingFilterChips(
  filters: SchedulingFilters,
  options?: {
    statusOptions?: { value: string; label: string }[];
    serviceCodeOptions?: { value: string; label: string }[];
    therapistOptions?: { value: string; label: string }[];
  },
): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (filters.status) {
    chips.push({
      id: `status:${filters.status}`,
      label: `Status: ${labelFromOptions(filters.status, options?.statusOptions ?? [])}`,
    });
  }

  if (filters.serviceCode) {
    chips.push({
      id: `serviceCode:${filters.serviceCode}`,
      label: `Service: ${labelFromOptions(filters.serviceCode, options?.serviceCodeOptions ?? [])}`,
    });
  }

  if (filters.therapist) {
    chips.push({
      id: `therapist:${filters.therapist}`,
      label: `Therapist: ${labelFromOptions(filters.therapist, options?.therapistOptions ?? [])}`,
    });
  }

  if (filters.startDate) {
    chips.push({
      id: "startDate",
      label: `From: ${formatFilterDate(filters.startDate)}`,
    });
  }

  if (filters.endDate) {
    chips.push({
      id: "endDate",
      label: `To: ${formatFilterDate(filters.endDate)}`,
    });
  }

  if (filters.includeHiddenServices) {
    chips.push({
      id: "includeHiddenServices",
      label: "Include hidden services",
    });
  }

  return chips;
}

export function removeSchedulingFilterChip(
  filters: SchedulingFilters,
  chipId: string,
): SchedulingFilters {
  const next = { ...filters };

  if (chipId.startsWith("status:")) {
    next.status = null;
    return next;
  }
  if (chipId.startsWith("serviceCode:")) {
    next.serviceCode = null;
    return next;
  }
  if (chipId.startsWith("therapist:")) {
    next.therapist = null;
    return next;
  }
  if (chipId === "startDate") {
    next.startDate = null;
    return next;
  }
  if (chipId === "endDate") {
    next.endDate = null;
    return next;
  }
  if (chipId === "includeHiddenServices") {
    next.includeHiddenServices = false;
    return next;
  }

  return next;
}

type TaskFilters = AdminTaskFiltersState | TaskFiltersState;

export const EMPTY_TASK_FILTERS: TaskFilters = {
  status: null,
  priority: null,
  assignee: null,
  assigneeName: null,
  startDate: null,
  endDate: null,
};

export type TaskFilterChipLabels = {
  statusLabel?: (key: string | null | undefined) => string;
  priorityLabel?: (key: string | null | undefined) => string;
};

export function buildTaskFilterChips(
  filters: TaskFilters,
  labels?: TaskFilterChipLabels,
): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (filters.status) {
    chips.push({
      id: `status:${filters.status}`,
      label: `Status: ${labels?.statusLabel?.(filters.status) ?? formatOptionKeyFallback(filters.status)}`,
    });
  }

  if (filters.priority) {
    chips.push({
      id: `priority:${filters.priority}`,
      label: `Priority: ${labels?.priorityLabel?.(filters.priority) ?? formatOptionKeyFallback(filters.priority)}`,
    });
  }

  if (filters.assignee) {
    chips.push({
      id: `assignee:${filters.assignee}`,
      label: `Assignee: ${filters.assigneeName || filters.assignee}`,
    });
  }

  if (filters.startDate) {
    chips.push({
      id: "startDate",
      label: `From: ${formatFilterDate(filters.startDate)}`,
    });
  }

  if (filters.endDate) {
    chips.push({
      id: "endDate",
      label: `To: ${formatFilterDate(filters.endDate)}`,
    });
  }

  return chips;
}

export function removeTaskFilterChip(filters: TaskFilters, chipId: string): TaskFilters {
  const next = { ...filters };

  if (chipId.startsWith("status:")) {
    next.status = null;
    return next;
  }
  if (chipId.startsWith("priority:")) {
    next.priority = null;
    return next;
  }
  if (chipId.startsWith("assignee:")) {
    next.assignee = null;
    next.assigneeName = null;
    return next;
  }
  if (chipId === "startDate") {
    next.startDate = null;
    return next;
  }
  if (chipId === "endDate") {
    next.endDate = null;
    return next;
  }

  return next;
}

export const EMPTY_HIPAA_FILTERS: HIPAAFilters = {
  startDate: null,
  endDate: null,
  actionType: null,
  riskLevel: null,
  phiAccessOnly: null,
};

const HIPAA_ACTION_OPTIONS = actionTypes.map((option) => ({
  value: option.value,
  label: option.label,
}));

const HIPAA_RISK_OPTIONS = riskLevels.map((option) => ({
  value: option.value,
  label: option.label,
}));

const CLINICAL_FORM_STATUS_LABELS: Record<FormStatus, string> = {
  pending: "Pending",
  "in-progress": "In Progress",
  completed: "Completed",
};

export function buildHipaaFilterChips(filters: HIPAAFilters): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (filters.actionType && filters.actionType !== "all") {
    chips.push({
      id: `actionType:${filters.actionType}`,
      label: `Action: ${labelFromOptions(filters.actionType, HIPAA_ACTION_OPTIONS)}`,
    });
  }

  if (filters.riskLevel && filters.riskLevel !== "all") {
    chips.push({
      id: `riskLevel:${filters.riskLevel}`,
      label: `Risk: ${labelFromOptions(filters.riskLevel, HIPAA_RISK_OPTIONS)}`,
    });
  }

  if (filters.startDate) {
    chips.push({
      id: "startDate",
      label: `From: ${formatFilterDate(filters.startDate)}`,
    });
  }

  if (filters.endDate) {
    chips.push({
      id: "endDate",
      label: `To: ${formatFilterDate(filters.endDate)}`,
    });
  }

  if (filters.phiAccessOnly === true) {
    chips.push({
      id: "phiAccessOnly",
      label: "PHI access only",
    });
  }

  return chips;
}

export function removeHipaaFilterChip(
  filters: HIPAAFilters,
  chipId: string,
): HIPAAFilters {
  const next = { ...filters };

  if (chipId.startsWith("actionType:")) {
    next.actionType = null;
    return next;
  }
  if (chipId.startsWith("riskLevel:")) {
    next.riskLevel = null;
    return next;
  }
  if (chipId === "startDate") {
    next.startDate = null;
    return next;
  }
  if (chipId === "endDate") {
    next.endDate = null;
    return next;
  }
  if (chipId === "phiAccessOnly") {
    next.phiAccessOnly = null;
    return next;
  }

  return next;
}

export function buildClientInvoiceFilterChips(
  filters: ClientInvoiceFilters,
): AppliedFilterChip[] {
  const chips: AppliedFilterChip[] = [];

  if (filters.paymentStatus) {
    chips.push({
      id: `paymentStatus:${filters.paymentStatus}`,
      label: `Status: ${labelFromOptions(filters.paymentStatus, CLIENT_INVOICE_STATUS_OPTIONS)}`,
    });
  }

  if (filters.insuranceCovered) {
    chips.push({
      id: `insuranceCovered:${filters.insuranceCovered}`,
      label: `Coverage: ${labelFromOptions(filters.insuranceCovered, CLIENT_INVOICE_INSURANCE_OPTIONS)}`,
    });
  }

  if (filters.startDate) {
    chips.push({
      id: "startDate",
      label: `From: ${formatFilterDate(filters.startDate)}`,
    });
  }

  if (filters.endDate) {
    chips.push({
      id: "endDate",
      label: `To: ${formatFilterDate(filters.endDate)}`,
    });
  }

  return chips;
}

export function removeClientInvoiceFilterChip(
  filters: ClientInvoiceFilters,
  chipId: string,
): ClientInvoiceFilters {
  const next = { ...filters };

  if (chipId.startsWith("paymentStatus:")) {
    next.paymentStatus = null;
    return next;
  }
  if (chipId.startsWith("insuranceCovered:")) {
    next.insuranceCovered = null;
    return next;
  }
  if (chipId === "startDate") {
    next.startDate = null;
    return next;
  }
  if (chipId === "endDate") {
    next.endDate = null;
    return next;
  }

  return next;
}

export function buildClinicalFormFilterChips(
  selectedFilters: FormStatus[],
): AppliedFilterChip[] {
  return selectedFilters.map((status) => ({
    id: `status:${status}`,
    label: `Status: ${CLINICAL_FORM_STATUS_LABELS[status]}`,
  }));
}

export function removeClinicalFormFilterChip(
  selectedFilters: FormStatus[],
  chipId: string,
): FormStatus[] {
  if (!chipId.startsWith("status:")) return selectedFilters;
  const status = chipId.slice("status:".length) as FormStatus;
  return selectedFilters.filter((value) => value !== status);
}
