/** Stable category keys — use instead of hardcoded enum arrays */
export const SystemOptionCategoryKey = {
  CLIENT_STATUS: "client_status",
  CLIENT_STAGE: "client_stage",
  CLIENT_TYPE: "client_type",
  CLIENT_SOURCE: "client_source",
  GENDER: "gender",
  MARITAL_STATUS: "marital_status",
  PREFERRED_LANGUAGE: "preferred_language",
  SERVICE_TYPE: "service_type",
  SERVICE_FREQUENCY: "service_frequency",
  EMPLOYMENT_STATUS: "employment_status",
  EDUCATION_LEVEL: "education_level",
  REFERRAL_SOURCES: "referral_sources",
  INSURANCE_PROVIDERS: "insurance_providers",
  INSURANCE_TYPES: "insurance_types",
  SESSION_STATUS: "session_status",
  SESSION_MODE: "session_mode",
  SESSION_TYPE: "session_type",
  TASK_STATUS: "task_status",
  TASK_PRIORITY: "task_priority",
  TASK_TITLES: "task_titles",
  TASK_TYPES: "task_types",
  TREATMENT_MODALITIES: "treatment_modalities",
  PRACTICE_SETTINGS: "practice_settings",
} as const;

export type SystemOptionCategoryKey =
  (typeof SystemOptionCategoryKey)[keyof typeof SystemOptionCategoryKey];
