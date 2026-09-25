import type { PortalConsent, PortalConsentTypeEnum } from "@/store/api/portalConsentsApi";
import type { ConsentSetting } from "@/types/privacy-settings.type";

const CONSENT_TYPE_DISPLAY_NAMES: Record<PortalConsentTypeEnum, string> = {
  AI_PROCESSING: "AI Processing Consent",
  DATA_SHARING: "Data Sharing",
  RESEARCH: "Research Participation",
  MARKETING: "Marketing Communications",
  SMS_COMMUNICATION: "SMS Communication",
  EMAIL_COMMUNICATION: "Email Communication",
  TELEHEALTH: "Telehealth Consent",
  AUDIO_RECORDING: "Audio Recording",
  VIDEO_RECORDING: "Video Recording",
  HIPAA_PRIVACY: "HIPAA Privacy Notice",
  HIPAA_AUTHORIZATION: "HIPAA Authorization",
  TREATMENT: "Consent for Treatment",
  INSURANCE_SHARING: "Insurance Information Sharing",
  PAYMENT_AUTHORIZATION: "Payment Authorization",
  PHOTOGRAPHY: "Photography Consent",
  ELECTRONIC_RECORDS: "Electronic Health Records",
  PARENTAL_CONSENT: "Parental/Guardian Consent",
  EMERGENCY_CONTACT: "Emergency Contact Authorization",
  OTHER: "Other",
};

const CONSENT_TYPE_METADATA: Partial<
  Record<
    PortalConsentTypeEnum,
    Pick<ConsentSetting, "title" | "description" | "details">
  >
> = {
  AI_PROCESSING: {
    title: "AI-Assisted Clinical Documentation",
    description:
      "Allow AI tools to help your therapist generate session notes and assessment reports. This helps improve the quality and efficiency of your care.",
    details: [
      "Your clinical data will be processed by OpenAI's GPT-4 to assist in generating clinical documentation.",
      "All data is pseudonymized before processing to protect your identity.",
      "AI-generated content is always reviewed and edited by your therapist before being added to your records.",
      "Your data is not used to train AI models or shared with third parties.",
      "You can withdraw this consent at any time, and your therapist will use traditional documentation methods.",
    ],
  },
  SMS_COMMUNICATION: {
    title: "SMS Appointment Notifications",
    description:
      "Receive appointment confirmations and reminders by text message to your mobile number.",
    details: [
      "Messages may include booking confirmations and appointment reminders.",
      "Standard message and data rates from your carrier may apply.",
      "You can turn off SMS notifications at any time from this page.",
    ],
  },
  DATA_SHARING: {
    title: "Data Sharing with Healthcare Providers",
    description:
      "Allow your clinical information to be shared with other healthcare providers involved in your care.",
    details: [
      "We share information only with providers directly involved in your care.",
      "You can request a list of organizations with whom your data has been shared.",
      "You may limit or withdraw this sharing at any time, subject to legal obligations.",
    ],
  },
  RESEARCH: {
    title: "Anonymous Research & Quality Improvement",
    description:
      "Allow your de-identified data to be used for research and improving mental health services.",
    details: [
      "Any data used for research is de-identified and cannot be traced back to you.",
      "Research helps us improve our services and understand treatment outcomes.",
      "You can opt out at any time without affecting your access to care.",
    ],
  },
  MARKETING: {
    title: "Marketing & Service Updates",
    description:
      "Receive updates about new services, wellness tips, and practice news.",
    details: [
      "We may send occasional emails about new services and wellness resources.",
      "You can change your communication preferences at any time.",
      "We never sell your contact information to third-party marketers.",
    ],
  },
};

function normalizeConsentTypeKey(value: string): string {
  return value.trim().toLowerCase().replace(/[^a-z0-9]+/g, "");
}

function consentTypeKeys(consentType: PortalConsentTypeEnum): Set<string> {
  const displayName = CONSENT_TYPE_DISPLAY_NAMES[consentType];
  return new Set(
    [consentType, displayName, consentType.replace(/_/g, " ")].map(
      normalizeConsentTypeKey,
    ),
  );
}

export function matchesPortalConsentType(
  apiConsentType: string,
  consentType: PortalConsentTypeEnum,
): boolean {
  const normalizedApiType = normalizeConsentTypeKey(apiConsentType);
  return consentTypeKeys(consentType).has(normalizedApiType);
}

export function resolvePortalConsentTypeEnum(
  apiConsentType: string,
): PortalConsentTypeEnum {
  for (const enumType of Object.keys(
    CONSENT_TYPE_DISPLAY_NAMES,
  ) as PortalConsentTypeEnum[]) {
    if (matchesPortalConsentType(apiConsentType, enumType)) {
      return enumType;
    }
  }

  return "OTHER";
}

function getConsentTimestamp(consent: PortalConsent): number {
  const value =
    consent.updatedAt ?? consent.grantedAt ?? consent.createdAt ?? "";
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? consent.id : parsed;
}

function getLatestConsentByType(
  consents: PortalConsent[],
): Map<PortalConsentTypeEnum, PortalConsent> {
  const latestByType = new Map<PortalConsentTypeEnum, PortalConsent>();

  for (const consent of consents) {
    const consentType = resolvePortalConsentTypeEnum(consent.consentType);
    const existing = latestByType.get(consentType);

    if (!existing || getConsentTimestamp(consent) > getConsentTimestamp(existing)) {
      latestByType.set(consentType, consent);
    }
  }

  return latestByType;
}

export function mapPortalConsentsToSettings(
  apiConsents: PortalConsent[],
): ConsentSetting[] {
  const latestByType = getLatestConsentByType(apiConsents);

  return Array.from(latestByType.entries())
    .map(([consentType, record]) => {
      const metadata = CONSENT_TYPE_METADATA[consentType];
      const granted = record.granted;

      return {
        id: normalizeConsentTypeKey(record.consentType),
        consentType,
        title: metadata?.title ?? record.consentType,
        description:
          metadata?.description ??
          `Manage your ${record.consentType.toLowerCase()} preferences.`,
        details: metadata?.details,
        enabled: granted,
        status: granted ? "granted" : "withdrawn",
      } satisfies ConsentSetting;
    })
    .sort((left, right) => left.title.localeCompare(right.title));
}
