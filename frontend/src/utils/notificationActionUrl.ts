/**
 * Turns a notification's action URL into an in-app path, or null when it points
 * anywhere else.
 *
 * Notifications are authored on the server (and some by admins), so their links
 * are untrusted: following an off-site one would hand a signed-in user to a
 * phishing page. The backend only ever links into the app, so anything that is
 * not same-origin — another host, a protocol-relative `//host`, `javascript:`,
 * credentials in the URL — is refused rather than followed.
 */
export function resolveNotificationActionPath(
  actionUrl: string | null | undefined,
  origin: string,
): string | null {
  const trimmed = actionUrl?.trim();
  if (!trimmed) return null;

  // Backslashes are read as slashes by browsers, so "/\evil.com" is "//evil.com".
  if (trimmed.includes("\\")) return null;

  const isRelativePath = trimmed.startsWith("/") && !trimmed.startsWith("//");
  const isAbsoluteHttp = /^https?:\/\//i.test(trimmed);
  if (!isRelativePath && !isAbsoluteHttp) return null;

  let url: URL;
  try {
    url = new URL(trimmed, origin);
  } catch {
    return null;
  }

  if (url.origin !== new URL(origin).origin || url.username || url.password) {
    return null;
  }

  return `${url.pathname}${url.search}${url.hash}`;
}
