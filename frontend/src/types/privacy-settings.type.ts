import type { PortalConsentTypeEnum } from "@/store/api/portalConsentsApi";

export interface ConsentSetting {
  id: string;
  consentType: PortalConsentTypeEnum;
  title: string;
  description: string;
  details?: string[];
  status: "granted" | "withdrawn";
  enabled: boolean;
}
