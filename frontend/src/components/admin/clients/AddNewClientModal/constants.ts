// Form field options constants

export const genderOptions = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
  { value: "NON_BINARY", label: "Non-Binary" },
  { value: "TRANSGENDER", label: "Transgender" },
  { value: "PREFER_NOT_TO_SAY", label: "Prefer Not to Say" },
  { value: "OTHER", label: "Other" },
];

export const maritalStatusOptions = [
  { value: "SINGLE", label: "Single" },
  { value: "MARRIED", label: "Married" },
  { value: "DIVORCED", label: "Divorced" },
  { value: "WIDOWED", label: "Widowed" },
  { value: "SEPARATED", label: "Separated" },
  { value: "DOMESTIC_PARTNERSHIP", label: "Domestic Partnership" },
  { value: "PREFER_NOT_TO_SAY", label: "Prefer Not to Say" },
];

export const languageOptions = [
  { value: "en", label: "English" },
  { value: "ar", label: "Arabic" },
  { value: "fr", label: "French" },
  { value: "es", label: "Spanish" },
];

export const referralSourcesOptions = [
  { value: "self-referral", label: "Self-Referral" },
  { value: "physician", label: "Physician" },
  { value: "family-friend", label: "Family/Friend" },
  { value: "online-search", label: "Online Search" },
  { value: "insurance-directory", label: "Insurance Directory" },
  { value: "other-therapist", label: "Other Therapist" },
  { value: "employee-assistance-program", label: "Employee Assistance Program" },
  { value: "school-counselor", label: "School Counselor" },
  { value: "lihc", label: "LIHC" },
];

export {
  employmentStatusOptions,
  educationLevelOptions,
} from "@/utils/clientEmploymentFields";

export const clientTypeOptions = [
  { value: "INDIVIDUAL", label: "Individual" },
  { value: "COUPLE", label: "Couple" },
  { value: "FAMILY", label: "Family" },
  { value: "GROUP", label: "Group" },
];

export const statusOptions = [
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "PENDING", label: "Pending" },
  { value: "DISCHARGED", label: "Discharged" },
  { value: "ON_HOLD", label: "On Hold" },
  { value: "WAITLIST", label: "Waitlist" },
];

export const clientStageOptions = [
  { value: "INTAKE", label: "Intake" },
  { value: "ASSESSMENT", label: "Assessment" },
  { value: "ACTIVE_TREATMENT", label: "Active Treatment" },
  { value: "MAINTENANCE", label: "Maintenance" },
  { value: "DISCHARGE_PLANNING", label: "Discharge Planning" },
  { value: "DISCHARGE", label: "Discharge" },
  { value: "FOLLOW_UP", label: "Follow-Up" },
];

export const serviceTypeOptions = [
  { value: "psychotherapy", label: "Psychotherapy" },
  { value: "counseling", label: "Counseling" },
  { value: "assessment", label: "Assessment" },
  { value: "consultation", label: "Consultation" },
  { value: "group-therapy", label: "Group Therapy" },
  { value: "family-therapy", label: "Family Therapy" },
  { value: "couples-therapy", label: "Couples Therapy" },
];

export const serviceFrequencyOptions = [
  { value: "weekly", label: "Weekly" },
  { value: "bi-weekly", label: "Bi-weekly" },
  { value: "monthly", label: "Monthly" },
  { value: "as-needed", label: "As Needed" },
  { value: "intensive", label: "Intensive (Multiple per week)" },
];

export const priorityOptions = [
  { value: "low", label: "Low" },
  { value: "medium", label: "Medium" },
  { value: "high", label: "High" },
  { value: "urgent", label: "Urgent" },
];

export const relationshipOptions = [
  { value: "spouse", label: "Spouse" },
  { value: "parent", label: "Parent" },
  { value: "sibling", label: "Sibling" },
  { value: "child", label: "Child" },
  { value: "friend", label: "Friend" },
  { value: "other", label: "Other" },
];
