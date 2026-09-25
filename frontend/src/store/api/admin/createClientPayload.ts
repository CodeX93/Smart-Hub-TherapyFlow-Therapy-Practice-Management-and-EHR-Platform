import type { AddClientFormValues } from "@/types/add-client.type";
import { isValidInsuranceAmount } from "@/utils/insuranceAmountInput";
import { relationshipOptions } from "@/components/admin/clients/AddNewClientModal/constants";

function trimToUndefined(s: string | undefined): string | undefined {
  const t = s?.trim();
  return t ? t : undefined;
}

function parseOptionalAmount(s: string | undefined): number | undefined {
  const t = trimToUndefined(s);
  if (!t || !isValidInsuranceAmount(t)) return undefined;
  const n = Number.parseFloat(t);
  return Number.isFinite(n) && n >= 0 ? n : undefined;
}

function parseOptionalInteger(s: string | undefined): number | undefined {
  const t = trimToUndefined(s);
  if (!t) return undefined;
  const n = Number.parseInt(t, 10);
  return Number.isFinite(n) ? n : undefined;
}

function mapRelationship(value: string | undefined): string | undefined {
  const v = trimToUndefined(value);
  if (!v) return undefined;
  return v.charAt(0).toUpperCase() + v.slice(1).toLowerCase();
}

export function mapRelationshipToFormValue(apiValue?: string | null): string {
  const raw = apiValue?.trim();
  if (!raw) return "";
  const normalized = raw.toLowerCase();
  const match = relationshipOptions.find(
    (option) =>
      option.value === normalized || option.label.toLowerCase() === normalized,
  );
  return match?.value ?? normalized;
}

/**
 * Maps the Add Client form to POST /api/v1/clients JSON body.
 * System-option fields are sent as optionKey strings without transformation.
 */
export function mapAddClientFormToCreateClientBody(
  data: AddClientFormValues,
): Record<string, unknown> {
  const fullName = trimToUndefined(data.fullName) ?? "";
  const email = trimToUndefined(data.email) ?? "";

  const body: Record<string, unknown> = {
    fullName,
    status: trimToUndefined(data.status) ?? "pending",
    email,
    hasPortalAccess: Boolean(data.enablePortalAccess),
    emailNotifications: Boolean(data.emailNotifications),
    stage: trimToUndefined(data.clientStage) ?? "intake",
  };

  const phone = trimToUndefined(data.phone);
  if (phone) body.phone = phone;

  const dateOfBirth = trimToUndefined(data.dateOfBirth);
  if (dateOfBirth) body.dateOfBirth = dateOfBirth;

  const gender = trimToUndefined(data.gender);
  if (gender) body.gender = gender;

  const maritalStatus = trimToUndefined(data.maritalStatus);
  if (maritalStatus) body.maritalStatus = maritalStatus;

  const preferredLanguage = trimToUndefined(data.preferredLanguage);
  if (preferredLanguage) body.preferredLanguage = preferredLanguage;

  const pronouns = trimToUndefined(data.pronouns);
  if (pronouns) body.pronouns = pronouns;

  const clientType = trimToUndefined(data.clientType);
  if (clientType) body.clientType = clientType;

  const employmentStatus = trimToUndefined(data.employmentStatus);
  if (employmentStatus) body.employmentStatus = employmentStatus;

  const educationLevel = trimToUndefined(data.educationLevel);
  if (educationLevel) body.educationLevel = educationLevel;

  body.numberOfDependents = Number.isFinite(data.numberOfDependents)
    ? Math.max(0, data.numberOfDependents)
    : 0;

  const assignedTherapistId = parseOptionalInteger(data.assignedTherapistId);
  if (assignedTherapistId !== undefined) {
    body.assignedTherapistId = assignedTherapistId;
  }

  const streetAddress1 = trimToUndefined(data.streetAddress1);
  if (streetAddress1) body.streetAddress1 = streetAddress1;

  const streetAddress2 = trimToUndefined(data.streetAddress2);
  if (streetAddress2) body.streetAddress2 = streetAddress2;

  const city = trimToUndefined(data.city);
  if (city) body.city = city;

  const province = trimToUndefined(data.stateProvince);
  if (province) body.province = province;

  const postalCode = trimToUndefined(data.zipPostalCode);
  if (postalCode) body.postalCode = postalCode;

  const country = trimToUndefined(data.country);
  if (country) body.country = country;

  const referrerName = trimToUndefined(data.referrerName);
  if (referrerName) body.referrerName = referrerName;

  const referenceNumber = trimToUndefined(data.referenceNumber);
  if (referenceNumber) body.referenceNumber = referenceNumber;

  const referralDate = trimToUndefined(data.referralDate);
  if (referralDate) body.referralDate = referralDate;

  const startDate = trimToUndefined(data.startDate);
  if (startDate) body.startDate = startDate;

  const clientSource =
    trimToUndefined(data.clientSource) ?? trimToUndefined(data.referralSource);
  if (clientSource) body.clientSource = clientSource;

  const serviceType = trimToUndefined(data.serviceType);
  if (serviceType) body.serviceType = serviceType;

  const serviceFrequency = trimToUndefined(data.serviceFrequency);
  if (serviceFrequency) body.serviceFrequency = serviceFrequency;

  const treatmentModality = trimToUndefined(data.treatmentModality);
  if (treatmentModality) body.treatmentModality = treatmentModality;

  // Two separate boxes, two separate fields: the Clinical tab's note is the client's
  // `notes`, the Referral tab's is the referral record's `referralNotes`. Folding them
  // into one field here discarded whichever lost the tie.
  const notes = trimToUndefined(data.generalNotes);
  if (notes) body.notes = notes;

  const referralNotes = trimToUndefined(data.referralNotes);
  if (referralNotes) body.referralNotes = referralNotes;

  body.needsFollowUp = Boolean(data.needsFollowUp);

  if (data.needsFollowUp) {
    const priority = trimToUndefined(data.priority);
    if (priority) body.priority = priority;

    const followUpDate = trimToUndefined(data.dueDate);
    if (followUpDate) body.followUpDate = followUpDate;

    const followUpNotes = trimToUndefined(data.followUpNotes);
    if (followUpNotes) body.followUpNotes = followUpNotes;
  }

  body.portalEmail = email;

  if (data.emergencyContactLegacy) {
    const en = trimToUndefined(data.contactName);
    const ep = trimToUndefined(data.contactPhone);
    const er = mapRelationship(data.relationshipToClient);
    if (en) body.emergencyContactName = en;
    if (ep) body.emergencyContactPhone = ep;
    if (er) body.emergencyContactRelationship = er;
  }

  if (data.insuranceInformation) {
    const insuranceProvider = trimToUndefined(data.insuranceProvider);
    if (insuranceProvider) body.insuranceProvider = insuranceProvider;

    const insuranceType = trimToUndefined(data.insuranceType);
    if (insuranceType) body.insuranceType = insuranceType;

    const policyNumber = trimToUndefined(data.policyNumber);
    if (policyNumber) body.policyNumber = policyNumber;

    const groupNumber = trimToUndefined(data.groupNumber);
    if (groupNumber) body.groupNumber = groupNumber;

    const insurancePhone = trimToUndefined(data.insurancePhone);
    if (insurancePhone) body.insurancePhone = insurancePhone;

    const copay = parseOptionalAmount(data.copayAmount);
    if (copay !== undefined) body.copayAmount = copay;

    const deductible = parseOptionalAmount(data.deductible);
    if (deductible !== undefined) body.deductible = deductible;
  }

  return body;
}

const ADDRESS_BODY_FIELDS = [
  "streetAddress1",
  "streetAddress2",
  "city",
  "province",
  "postalCode",
  "country",
] as const;

const EMERGENCY_CONTACT_BODY_FIELDS = [
  "emergencyContactName",
  "emergencyContactPhone",
  "emergencyContactRelationship",
] as const;

const CLEARABLE_BODY_FIELDS = new Set<string>([
  ...ADDRESS_BODY_FIELDS,
  ...EMERGENCY_CONTACT_BODY_FIELDS,
  "phone",
  "dateOfBirth",
  "gender",
  "maritalStatus",
  "preferredLanguage",
  "pronouns",
  "clientType",
  "employmentStatus",
  "educationLevel",
  "assignedTherapistId",
  "referrerName",
  "referenceNumber",
  "referralDate",
  "startDate",
  "clientSource",
  "serviceType",
  "serviceFrequency",
  "treatmentModality",
  "notes",
  "referralNotes",
  "priority",
  "followUpDate",
  "followUpNotes",
  "insuranceProvider",
  "insuranceType",
  "policyNumber",
  "groupNumber",
  "insurancePhone",
  "copayAmount",
  "deductible",
]);

function valuesEqual(left: unknown, right: unknown): boolean {
  return JSON.stringify(left) === JSON.stringify(right);
}

function includeAddressGroupIfChanged(
  partialBody: Record<string, unknown>,
  currentBody: Record<string, unknown>,
  initialBody: Record<string, unknown>,
): void {
  const addressChanged = ADDRESS_BODY_FIELDS.some(
    (field) => !valuesEqual(currentBody[field], initialBody[field]),
  );
  if (!addressChanged) return;

  for (const field of ADDRESS_BODY_FIELDS) {
    if (field in currentBody) {
      partialBody[field] = currentBody[field];
    } else if (field in initialBody) {
      partialBody[field] = null;
    }
  }
}

function applyEmergencyContactPartialUpdate(
  partialBody: Record<string, unknown>,
  current: AddClientFormValues,
  initial: AddClientFormValues,
  currentBody: Record<string, unknown>,
  initialBody: Record<string, unknown>,
): void {
  if (initial.emergencyContactLegacy && !current.emergencyContactLegacy) {
    for (const field of EMERGENCY_CONTACT_BODY_FIELDS) {
      partialBody[field] = null;
    }
    return;
  }

  if (!current.emergencyContactLegacy) return;

  const emergencyChanged = EMERGENCY_CONTACT_BODY_FIELDS.some(
    (field) => !valuesEqual(currentBody[field], initialBody[field]),
  );
  if (!emergencyChanged) return;

  for (const field of EMERGENCY_CONTACT_BODY_FIELDS) {
    if (field in currentBody) {
      partialBody[field] = currentBody[field];
    } else if (field in initialBody) {
      partialBody[field] = null;
    }
  }
}

export function mapAddClientFormToPartialClientBody(
  current: AddClientFormValues,
  initial: AddClientFormValues,
): Record<string, unknown> {
  const currentBody = mapAddClientFormToCreateClientBody(current);
  const initialBody = mapAddClientFormToCreateClientBody(initial);
  const partialBody: Record<string, unknown> = {};

  const allKeys = new Set([
    ...Object.keys(currentBody),
    ...Object.keys(initialBody),
  ]);

  allKeys.forEach((key) => {
    if (
      EMERGENCY_CONTACT_BODY_FIELDS.includes(
        key as (typeof EMERGENCY_CONTACT_BODY_FIELDS)[number],
      )
    ) {
      return;
    }
    if (ADDRESS_BODY_FIELDS.includes(key as (typeof ADDRESS_BODY_FIELDS)[number])) {
      return;
    }

    const currentHasKey = key in currentBody;
    const initialHasKey = key in initialBody;
    const currentVal = currentBody[key];
    const initialVal = initialBody[key];

    if (valuesEqual(currentVal, initialVal)) return;

    if (currentHasKey) {
      partialBody[key] = currentVal;
      return;
    }

    if (initialHasKey && CLEARABLE_BODY_FIELDS.has(key)) {
      partialBody[key] = null;
    }
  });

  includeAddressGroupIfChanged(partialBody, currentBody, initialBody);
  applyEmergencyContactPartialUpdate(
    partialBody,
    current,
    initial,
    currentBody,
    initialBody,
  );

  return partialBody;
}
