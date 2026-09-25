/**
 * Emptiness for rich-text editor content.
 *
 * A WYSIWYG editor does not hand back "" when the user clears it — it keeps the
 * markup that holds the caret, typically `<p><br></p>` or `<p>&nbsp;</p>`.
 * Testing that with `.trim()` reports an empty editor as full, which leaves
 * controls enabled and steps ticked on a note that has nothing in it.
 */

/** Tags that carry content even with no text of their own. */
const EMBEDDED_CONTENT = /<(img|video|audio|iframe|table|hr|input|svg|canvas|object|embed)\b/i;

export function richTextToPlainText(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(p|div|li|h[1-6]|tr)>/gi, "\n")
    .replace(/<[^>]*>/g, "")
    .replace(/&nbsp;|&#160;|&#xA0;/gi, " ")
    .replace(/&amp;/gi, "&")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">")
    .replace(/\u00a0/g, " ");
}

export function isEmptyRichText(html?: string | null): boolean {
  if (!html) return true;
  // An image or a table is content even though it contributes no text.
  if (EMBEDDED_CONTENT.test(html)) return false;
  return richTextToPlainText(html).trim().length === 0;
}

export function hasRichTextContent(html?: string | null): boolean {
  return !isEmptyRichText(html);
}
