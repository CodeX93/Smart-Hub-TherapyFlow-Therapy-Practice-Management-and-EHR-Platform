/**
 * Normalize invoice HTML payloads from preview/download APIs.
 */
export function resolveInvoiceHtml(payload: unknown): string {
  if (typeof payload === "string") {
    const trimmed = payload.trim();
    if (!trimmed) {
      return "";
    }
    if (trimmed.startsWith("<")) {
      return payload;
    }
    try {
      const parsed = JSON.parse(payload) as { html?: unknown };
      return typeof parsed?.html === "string" ? parsed.html : payload;
    } catch {
      return payload;
    }
  }
  if (payload && typeof payload === "object" && "html" in payload) {
    const html = (payload as { html?: unknown }).html;
    return typeof html === "string" ? html : "";
  }
  return "";
}
