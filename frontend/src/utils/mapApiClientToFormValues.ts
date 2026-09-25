import type { AddClientFormValues } from "@/types/add-client.type";
import type { AdminClientSummary } from "@/store/api/admin/clients.api";
import { mapClientInsuranceToFormValues } from "@/utils/clientInsurance";
import { resolveClientFormOptionFields } from "@/utils/systemOptions";
import type { SystemOptionValue } from "@/store/api/admin/systemOptions.api";
import { mapRelationshipToFormValue } from "@/store/api/admin/createClientPayload";

export function mapApiClientToFormValues(client: AdminClientSummary): AddClientFormValues {
  return {
    fullName: client.fullName || "",
    email: client.email || "",
    phone: client.phone || "",
    dateOfBirth: client.dateOfBirth || "",
    gender: client.gender?.trim() || "",
    maritalStatus: client.maritalStatus?.trim() || "",
    pronouns: client.pronouns || "",
    preferredLanguage: client.preferredLanguage?.trim() || "",
    // The API resolves this: the client's own setting, else the clinic's. Reading the
    // device here showed whoever opened the record their own timezone, not the client's.
    timezone: client.timezone?.trim() || "",
    enablePortalAccess: Boolean(client.hasPortalAccess),
    emailNotifications: client.emailNotifications ?? true,
    streetAddress1: client.streetAddress1 || "",
    streetAddress2: client.streetAddress2 || "",
    city: client.city || "",
    stateProvince: client.province || "",
    zipPostalCode: client.postalCode || "",
    country: client.country || "",
    legacyAddress: false,
    addressLegacy: "",
    stateLegacy: "",
    zipCodeLegacy: "",
    emergencyContactLegacy: Boolean(
      client.emergencyContactName ||
        client.emergencyContactPhone ||
        client.emergencyContactRelationship,
    ),
    contactName: client.emergencyContactName || "",
    contactPhone: client.emergencyContactPhone || "",
    relationshipToClient: mapRelationshipToFormValue(client.emergencyContactRelationship),
    startDate: client.startDate || "",
    referralDate: client.referralDate || "",
    referrerName: client.referrerName || "",
    referenceNumber: client.referenceNumber || "",
    clientSource: client.clientSource || "",
    legacyReferral: false,
    referringPersonName: "",
    referralSource: client.clientSource || "",
    referralType: "",
    referralNotes: client.referralNotes || "",
    employmentStatus: client.employmentStatus?.trim() || "",
    educationLevel: client.educationLevel?.trim() || "",
    numberOfDependents:
      typeof client.numberOfDependents === "number" && Number.isFinite(client.numberOfDependents)
        ? client.numberOfDependents
        : 0,
    status: client.status?.trim() || "",
    assignedTherapistId: client.assignedTherapistId
      ? String(client.assignedTherapistId)
      : "",
    clientType: client.clientType?.trim() || "",
    clientStage: client.stage?.trim() || "",
    serviceType: client.serviceType?.trim() || "",
    serviceFrequency: client.serviceFrequency?.trim() || "",
    treatmentModality: client.treatmentModality?.trim() || "",
    needsFollowUp: Boolean(client.needsFollowUp),
    priority: client.priority?.trim() || "",
    dueDate: client.followUpDate || "",
    followUpNotes: client.followUpNotes || "",
    ...mapClientInsuranceToFormValues(client),
    generalNotes: client.notes || "",
  };
}

export function mapApiClientToResolvedFormValues(
  client: AdminClientSummary,
  optionsByCategory: Map<string, SystemOptionValue[]>,
): AddClientFormValues {
  return resolveClientFormOptionFields(
    mapApiClientToFormValues(client),
    optionsByCategory,
  );
}
