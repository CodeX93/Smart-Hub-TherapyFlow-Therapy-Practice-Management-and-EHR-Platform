function getBaseHostname(hostname: string): string {
  if (hostname === "localhost") {
    return "localhost";
  }

  if (hostname.includes("therapy-flow-api")) {
    return hostname;
  }

  if (hostname.endsWith(".canadacentral.azurecontainerapps.io")) {
    return `therapy-flow-api.${hostname}`;
  }

  const parts = hostname.split(".");
  if (parts.length <= 2) {
    return hostname;
  }

  return parts.slice(1).join(".");
}

export function buildTenantApiUrl(subdomain: string, path: string): string {
  const base = new URL(getBaseApiUrl());
  const baseHostname = getBaseHostname(base.hostname);
  base.hostname = `${subdomain}.${baseHostname}`;
  base.pathname = path.startsWith("/") ? path : `/${path}`;
  base.search = "";
  base.hash = "";
  return base.toString();
}

export function buildTenantFrontendUrl(subdomain: string): string {
  const { protocol, hostname, port } = window.location;
  const baseHostname = getBaseHostname(hostname);
  const host = `${subdomain}.${baseHostname}${port ? `:${port}` : ""}`;
  return `${protocol}//${host}`;
}

export function getBaseApiUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? window.location.origin;
}

export function buildTenantApiBase(subdomain?: string | null): string {
  const baseUrl = getBaseApiUrl();
  if (!subdomain) {
    return baseUrl;
  }

  const base = new URL(baseUrl);
  const baseHostname = getBaseHostname(base.hostname);
  base.hostname = `${subdomain}.${baseHostname}`;
  base.pathname = "";
  base.search = "";
  base.hash = "";
  // Avoid `host//api/...` when callers join with a leading slash.
  return base.toString().replace(/\/$/, "");
}

type SessionApiBaseInput = {
  role?: string | null;
  tenantSubdomain?: string | null;
  tenantSchema?: string | null;
};

function tenantSubdomainFromBrowserHost(): string | undefined {
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

export function resolveApiBaseForSession(
  session?: SessionApiBaseInput | null,
): string {
  if (session?.tenantSchema && session.tenantSchema !== "public") {
    return getBaseApiUrl();
  }

  if (session?.role === "super-admin") {
    return getBaseApiUrl();
  }

  // Client portal: tenant is on the JWT. Only rewrite API host when the browser
  // is already on a real tenant subdomain — never from a stored org slug.
  if (session?.role === "user") {
    return buildTenantApiBase(tenantSubdomainFromBrowserHost());
  }

  return buildTenantApiBase(session?.tenantSubdomain);
}
