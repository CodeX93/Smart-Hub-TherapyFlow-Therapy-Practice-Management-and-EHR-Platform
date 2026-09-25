import { ImapFlow } from 'imapflow';
import { config } from './config.mjs';

/**
 * Picks the mailbox the run reads from: a real IMAP account (production, and any
 * environment that sends through a real provider) or a local SMTP catcher, which
 * lets a laptop run the same journey without handing the suite a mail password.
 */
export function createMailbox() {
  return config.mailpitUrl ? new MailpitMailbox(config.mailpitUrl) : new Mailbox();
}

/** Reads the messages a local Mailpit container caught, over its HTTP API. */
export class MailpitMailbox {
  constructor(baseUrl) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
    /** Message ids already handed out, so no code is ever served twice. */
    this.consumed = new Set();
  }

  async connect() { return this; }

  async close() { /* nothing to release */ }

  async waitForMessage({ to, subjectIncludes, since, timeoutMs = config.mailTimeoutMs, pollMs = 2000 }) {
    const deadline = Date.now() + timeoutMs;
    const cutoff = since.getTime() - 120000;
    let lastError;
    while (Date.now() < deadline) {
      try {
        const query = encodeURIComponent(`to:"${to}"`);
        const listed = await fetchJson(`${this.baseUrl}/api/v1/search?query=${query}&limit=50`);
        const candidates = (listed.messages ?? [])
          .filter((m) => !this.consumed.has(m.ID))
          .filter((m) => !subjectIncludes || (m.Subject ?? '').toLowerCase().includes(subjectIncludes.toLowerCase()))
          .filter((m) => new Date(m.Created).getTime() >= cutoff)
          .sort((a, b) => new Date(b.Created) - new Date(a.Created));
        if (candidates.length) {
          this.consumed.add(candidates[0].ID);
          const full = await fetchJson(`${this.baseUrl}/api/v1/message/${candidates[0].ID}`);
          return {
            subject: full.Subject ?? candidates[0].Subject,
            date: new Date(candidates[0].Created),
            source: JSON.stringify(full),
            body: full.HTML || full.Text || '',
          };
        }
      } catch (error) {
        lastError = error;
      }
      await new Promise((resolve) => setTimeout(resolve, pollMs));
    }
    const detail = lastError ? ` Last Mailpit error: ${lastError.message}` : '';
    throw new Error(
      `No message to ${to} with subject containing "${subjectIncludes}" reached Mailpit within ${Math.round(timeoutMs / 1000)}s.${detail}`,
    );
  }
}

async function fetchJson(url) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`${url} responded ${response.status}`);
  return response.json();
}

/**
 * Reads the real mailbox the application delivers to, the same way the person
 * receiving the message would. No application hook, log or database read is used
 * to obtain a code.
 */
export class Mailbox {
  constructor(settings = config.mailbox) {
    this.settings = settings;
    /**
     * UIDs already handed out, per folder and address, so no code is ever served
     * twice. Deliberately NOT a high-water mark: a sign-in during a test consumes
     * a code that arrived after an email the test still has to read (e.g. the
     * task-assignment email lands before the assignee's sign-in code), and a
     * "skip everything at or below the newest consumed UID" rule would hide that
     * earlier email forever.
     */
    this.consumed = new Map();
  }

  async connect() {
    if (this.client) return this.client;
    this.client = new ImapFlow({
      host: this.settings.host,
      port: this.settings.port,
      secure: true,
      auth: { user: this.settings.user, pass: this.settings.pass },
      logger: false,
    });
    await this.client.connect();
    return this.client;
  }

  async close() {
    if (!this.client) return;
    try { await this.client.logout(); } catch { /* the run is over either way */ }
    this.client = undefined;
  }

  /**
   * Polls INBOX (and Gmail's All Mail when available) until a message addressed to
   * `to` whose subject contains `subjectIncludes` arrives after `since`.
   */
  async waitForMessage({ to, subjectIncludes, since, timeoutMs = config.mailTimeoutMs, pollMs = 5000 }) {
    const deadline = Date.now() + timeoutMs;
    const sinceDate = new Date(since.getTime() - 60000); // IMAP SINCE has day granularity on some servers
    let lastError;
    while (Date.now() < deadline) {
      try {
        const found = await this.#search({ to, subjectIncludes, sinceDate, after: since });
        if (found) return found;
      } catch (error) {
        lastError = error;
        this.client = undefined;
      }
      await new Promise((resolve) => setTimeout(resolve, pollMs));
    }
    const detail = lastError ? ` Last IMAP error: ${lastError.message}` : '';
    throw new Error(
      `No message to ${to} with subject containing "${subjectIncludes}" arrived within ${Math.round(timeoutMs / 1000)}s.${detail}`,
    );
  }

  async #search({ to, subjectIncludes, sinceDate, after }) {
    const client = await this.connect();
    const mailboxes = ['INBOX', '[Gmail]/All Mail'];
    for (const name of mailboxes) {
      let lock;
      try {
        lock = await client.getMailboxLock(name);
      } catch {
        continue; // server does not expose this mailbox
      }
      try {
        const uids = await client.search({ to, since: sinceDate }, { uid: true });
        if (!uids?.length) continue;
        const consumedHere = this.#consumedIn(name, to);
        const newest = uids.slice(-25).reverse();
        for (const uid of newest) {
          // Every round of a sign-in requests a fresh code; a message that has already
          // answered one carries a code the app has since burnt, so never serve it twice.
          if (consumedHere.has(uid)) continue;
          const message = await client.fetchOne(String(uid), { envelope: true, source: true }, { uid: true });
          if (!message) continue;
          const subject = message.envelope?.subject ?? '';
          const date = message.envelope?.date ? new Date(message.envelope.date) : new Date(0);
          if (subjectIncludes && !subject.toLowerCase().includes(subjectIncludes.toLowerCase())) continue;
          if (after && date.getTime() < after.getTime() - 120000) continue;
          const source = message.source.toString('utf8');
          if (!source.toLowerCase().includes(to.toLowerCase())) continue;
          consumedHere.add(uid);
          return { subject, date, source, body: decodeBody(source) };
        }
      } finally {
        lock.release();
      }
    }
    return null;
  }

  /** The set of already-served UIDs for one folder and address. */
  #consumedIn(folder, to) {
    const key = `${folder}\n${to.toLowerCase()}`;
    let set = this.consumed.get(key);
    if (!set) {
      set = new Set();
      this.consumed.set(key, set);
    }
    return set;
  }
}

/** Undoes quoted-printable/base64 transfer encoding enough to read codes and links out of the HTML. */
export function decodeBody(source) {
  const headerEnd = source.indexOf('\r\n\r\n');
  const headers = headerEnd > 0 ? source.slice(0, headerEnd) : '';
  let body = headerEnd > 0 ? source.slice(headerEnd + 4) : source;
  const encoding = /content-transfer-encoding:\s*([\w-]+)/i.exec(source)?.[1]?.toLowerCase();
  if (encoding === 'base64' || /base64/i.test(headers)) {
    const base64 = body.replace(/[^A-Za-z0-9+/=]/g, '');
    try { body = Buffer.from(base64, 'base64').toString('utf8'); } catch { /* fall through */ }
  }
  if (encoding === 'quoted-printable' || /quoted-printable/i.test(source)) {
    body = body
      .replace(/=\r?\n/g, '')
      .replace(/=([0-9A-Fa-f]{2})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)));
  }
  return body;
}

/** Pulls the 6-digit sign-in code out of the branded OTP email. */
export function extractOtpCode(body) {
  const marked = /email-otp[^>]*>\s*([0-9]{6})\s*</.exec(body);
  if (marked) return marked[1];
  const text = body.replace(/<[^>]+>/g, ' ');
  const labelled = /Sign-?in code\s*([0-9]{6})/i.exec(text);
  if (labelled) return labelled[1];
  const bare = /(?<![0-9])([0-9]{6})(?![0-9])/.exec(text);
  if (bare) return bare[1];
  throw new Error('No 6-digit code found in the message body.');
}

/** Pulls the temporary password out of the organisation onboarding email. */
export function extractTemporaryPassword(body) {
  const coded = /<code[^>]*>([^<]{6,})<\/code>/i.exec(body);
  if (coded) return decodeEntities(coded[1].trim());
  const text = body.replace(/<[^>]+>/g, '\n').replace(/\n{2,}/g, '\n');
  const labelled = /Temporary Password\s*\n?\s*([^\s\n]{6,})/i.exec(text);
  if (labelled) return decodeEntities(labelled[1].trim());
  throw new Error('No temporary password found in the onboarding email.');
}

function decodeEntities(value) {
  return value
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'");
}
