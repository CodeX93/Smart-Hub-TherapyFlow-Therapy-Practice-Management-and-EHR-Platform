import DOMPurify from "dompurify";

/**
 * Every string passed to `dangerouslySetInnerHTML` goes through here. Notes,
 * consents, form copy and converted documents are all user- or tenant-authored,
 * so none of them is trusted markup.
 */
export function sanitizeHtml(html: string | null | undefined): string {
  if (!html) return "";
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } });
}
