import { fetchWithAuth } from "@/utils/fetchWithAuth";

export type PortalDocumentAssetResult =
  | { kind: "url"; value: string }
  | { kind: "blob"; value: Blob };

const normalizeDocumentUrl = (value: string): string => {
  const trimmed = value.trim();
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1);
  }

  return trimmed;
};

export async function fetchPortalDocumentAsset(
  path: string,
): Promise<PortalDocumentAssetResult> {
  const response = await fetchWithAuth(path);

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const text = normalizeDocumentUrl(await response.text());
    return { kind: "url", value: text };
  }

  return { kind: "blob", value: await response.blob() };
}

export function getPortalDocumentViewPath(documentId: number): string {
  return `/api/v1/portal/documents/${documentId}/view`;
}

export function getPortalDocumentDownloadPath(documentId: number): string {
  return `/api/v1/portal/documents/${documentId}/download`;
}

function isAbsoluteHttpUrl(value: string): boolean {
  return /^https?:\/\//i.test(value.trim());
}

/**
 * Loads a portal avatar for use in <img src>. Relative API paths require auth,
 * so protected files are fetched and exposed as a blob object URL.
 */
export async function fetchPortalAvatarPreviewUrl(
  avatarUrl: string | null | undefined,
): Promise<string | null> {
  const trimmed = avatarUrl?.trim();
  if (!trimmed) return null;

  if (isAbsoluteHttpUrl(trimmed)) {
    return trimmed;
  }

  const path = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
  const asset = await fetchPortalDocumentAsset(path);

  if (asset.kind === "url") {
    const resolved = asset.value.trim();
    if (!resolved) return null;
    return isAbsoluteHttpUrl(resolved) ? resolved : null;
  }

  return URL.createObjectURL(asset.value);
}

export function revokePortalAvatarPreviewUrl(
  previewUrl: string | null | undefined,
): void {
  if (previewUrl?.startsWith("blob:")) {
    URL.revokeObjectURL(previewUrl);
  }
}
