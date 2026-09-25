import { baseApi } from "./baseApi";

export type PortalConsentTypeEnum =
  | "AI_PROCESSING"
  | "DATA_SHARING"
  | "RESEARCH"
  | "MARKETING"
  | "SMS_COMMUNICATION"
  | "EMAIL_COMMUNICATION"
  | "TELEHEALTH"
  | "AUDIO_RECORDING"
  | "VIDEO_RECORDING"
  | "HIPAA_PRIVACY"
  | "HIPAA_AUTHORIZATION"
  | "TREATMENT"
  | "INSURANCE_SHARING"
  | "PAYMENT_AUTHORIZATION"
  | "PHOTOGRAPHY"
  | "ELECTRONIC_RECORDS"
  | "PARENTAL_CONSENT"
  | "EMERGENCY_CONTACT"
  | "OTHER";

export interface PortalConsent {
  id: number;
  clientId: number;
  consentType: string;
  consentVersion: string;
  granted: boolean;
  grantedAt: string | null;
  withdrawnAt: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt?: string;
}

export interface PortalConsentTogglePayload {
  consentType: PortalConsentTypeEnum;
  granted: boolean;
  consentVersion?: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return 0;
}

function asNullableString(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed || null;
}

function normalizePortalConsent(entry: unknown): PortalConsent | null {
  if (!isRecord(entry)) return null;

  return {
    id: asNumber(entry.id),
    clientId: asNumber(entry.clientId),
    consentType: asNullableString(entry.consentType) ?? "",
    consentVersion: asNullableString(entry.consentVersion) ?? "1.0",
    granted: Boolean(entry.granted),
    grantedAt: asNullableString(entry.grantedAt),
    withdrawnAt: asNullableString(entry.withdrawnAt),
    notes: asNullableString(entry.notes),
    createdAt: asNullableString(entry.createdAt) ?? "",
    updatedAt: asNullableString(entry.updatedAt) ?? undefined,
  };
}

function normalizePortalConsentsResponse(payload: unknown): PortalConsent[] {
  const rows = Array.isArray(payload) ? payload : [];
  return rows
    .map((entry) => normalizePortalConsent(entry))
    .filter((entry): entry is PortalConsent => entry !== null);
}

export const portalConsentsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getPortalConsents: builder.query<PortalConsent[], void>({
      query: () => "/api/v1/portal/consents",
      transformResponse: (payload) => normalizePortalConsentsResponse(payload),
      providesTags: ["PortalConsents"],
    }),
    togglePortalConsent: builder.mutation<
      PortalConsent,
      PortalConsentTogglePayload
    >({
      query: (body) => ({
        url: "/api/v1/portal/consents/toggle",
        method: "PUT",
        body: {
          consentType: body.consentType,
          granted: body.granted,
          consentVersion: body.consentVersion ?? "1.0",
        },
      }),
      transformResponse: (payload) =>
        normalizePortalConsent(payload) ?? {
          id: 0,
          clientId: 0,
          consentType: "",
          consentVersion: "1.0",
          granted: false,
          grantedAt: null,
          withdrawnAt: null,
          notes: null,
          createdAt: "",
        },
      async onQueryStarted(_arg, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(
            portalConsentsApi.util.updateQueryData(
              "getPortalConsents",
              undefined,
              (draft) => {
                const index = draft.findIndex(
                  (item) =>
                    item.id === data.id ||
                    item.consentType.toUpperCase() ===
                      data.consentType.toUpperCase(),
                );

                if (index >= 0) {
                  draft[index] = data;
                  return;
                }

                draft.push(data);
              },
            ),
          );
        } catch {
          // Keep cached consent values when the toggle request fails.
        }
      },
    }),
  }),
});

export const {
  useGetPortalConsentsQuery,
  useTogglePortalConsentMutation,
} = portalConsentsApi;
