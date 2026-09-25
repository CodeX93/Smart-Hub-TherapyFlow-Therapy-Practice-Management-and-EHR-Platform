import type { IntegrationKey } from "@/store/api/super-admin/integrations.api";

export type IntegrationFieldKey =
  | "clientId"
  | "publishableKey"
  | "secret"
  | "connectClientSecret"
  | "platformWebhookSecret"
  | "connectWebhookSecret"
  | "webhookUrl";

export interface IntegrationMeta {
  key: IntegrationKey;
  label: string;
  description: string;
  fields: IntegrationFieldKey[];
}

export const INTEGRATION_FIELD_LABELS: Record<IntegrationFieldKey, string> = {
  clientId: "Client ID",
  publishableKey: "Publishable key",
  secret: "Secret / API key",
  connectClientSecret: "Connect client secret",
  platformWebhookSecret: "Platform webhook secret",
  connectWebhookSecret: "Connect webhook secret",
  webhookUrl: "Webhook URL",
};

export const INTEGRATION_FIELD_HINTS: Partial<Record<IntegrationFieldKey, string>> = {
  clientId: "OAuth client ID (Zoom) or Stripe Connect client ID (ca_...)",
  publishableKey: "Must start with pk_ or rk_",
  secret: "Platform secret key (sk_...) for subscriptions and customers",
  connectClientSecret: "Required when Connect client ID is set (typically same sk_)",
  platformWebhookSecret:
    "Must start with whsec_ — subscription destination → /webhook/platform",
  connectWebhookSecret:
    "Must start with whsec_ — Connect destination → /webhook/connect",
  webhookUrl: "Optional HTTPS reference URL, max 255 characters",
};

export const SUPER_ADMIN_INTEGRATIONS: IntegrationMeta[] = [
  {
    key: "stripe",
    label: "Stripe",
    description: "Platform billing, subscriptions, and Stripe Connect for tenant payments.",
    fields: [
      "publishableKey",
      "secret",
      "clientId",
      "connectClientSecret",
      "platformWebhookSecret",
      "connectWebhookSecret",
      "webhookUrl",
    ],
  },
  {
    key: "zoom",
    label: "Zoom",
    description: "Video conferencing OAuth credentials for therapist sessions.",
    fields: ["clientId", "secret"],
  },
  {
    key: "openai",
    label: "OpenAI",
    description: "AI transcription and session note features.",
    fields: ["secret"],
  },
  {
    key: "sparkpost",
    label: "SparkPost",
    description: "Transactional email delivery provider.",
    fields: ["secret"],
  },
  {
    key: "ses",
    label: "Amazon SES",
    description: "AWS Simple Email Service credentials.",
    fields: ["secret"],
  },
];

export const WEBHOOK_URL_MAX_LENGTH = 255;
