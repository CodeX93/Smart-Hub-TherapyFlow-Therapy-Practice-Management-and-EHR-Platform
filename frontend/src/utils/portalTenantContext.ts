import { buildTenantApiBase } from "@/utils/tenantUrls";

export type PortalTenantContext = {
  orgSlug?: string;
  tenantSubdomain?: string;
};

export function getTenantSubdomainFromHostname(): string | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }

  const hostname = window.location.hostname;
  if (hostname === "localhost" || hostname === "127.0.0.1") {
    return undefined;
  }

  const parts = hostname.split(".");
  if (parts.length < 3) {
    return undefined;
  }

  const subdomain = parts[0];
  if (!subdomain || subdomain === "www" || subdomain === "app") {
    return undefined;
  }

  return subdomain;
}

export function resolvePortalTenantContext(
  searchParams: URLSearchParams,
): PortalTenantContext {
  const orgSlug = searchParams.get("orgSlug")?.trim() || undefined;
  const tenantSubdomain =
    searchParams.get("tenant")?.trim() ||
    searchParams.get("tenantSubdomain")?.trim() ||
    getTenantSubdomainFromHostname() ||
    undefined;

  return { orgSlug, tenantSubdomain };
}

export function buildPortalPublicRequest(
  path: string,
  body: Record<string, string>,
  tenantContext?: PortalTenantContext,
) {
  const headers: Record<string, string> = {};

  if (tenantContext?.tenantSubdomain) {
    headers["X-Tenant-Subdomain"] = tenantContext.tenantSubdomain;
  }

  const base = buildTenantApiBase(tenantContext?.tenantSubdomain ?? undefined);
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;

  return {
    url: `${base}${normalizedPath}`,
    method: "POST" as const,
    body,
    headers: Object.keys(headers).length > 0 ? headers : undefined,
  };
}

export function getPortalActivationErrorMessage(error: unknown): string {
  const message =
    typeof error === "object" &&
    error !== null &&
    "data" in error &&
    typeof (error as { data?: { message?: unknown } }).data?.message === "string"
      ? (error as { data: { message: string } }).data.message
      : "";

  const normalized = message.toLowerCase();

  if (
    normalized.includes("invalid") &&
    normalized.includes("activation token")
  ) {
    return "This activation link is invalid or has expired. Ask your clinic to resend it.";
  }

  if (normalized.includes("password") && normalized.includes("8")) {
    return "Password must be at least 8 characters.";
  }

  if (normalized.includes("portal access not enabled")) {
    return "Portal access is not enabled for this account.";
  }

  if (
    normalized.includes("tenant") ||
    normalized.includes("organisation") ||
    normalized.includes("organization")
  ) {
    return "Could not determine your clinic. Open the link from the email again or contact support.";
  }

  if (message) {
    return message;
  }

  return "Activation failed. Please try again or contact your clinic for a new link.";
}
