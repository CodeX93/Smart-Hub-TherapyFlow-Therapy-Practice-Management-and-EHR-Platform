import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const e2eDir = path.dirname(path.dirname(fileURLToPath(import.meta.url)));

/** Reads qa/e2e/.env.e2e (gitignored) without overriding a value already in the environment. */
export function loadEnvFile(file = path.join(e2eDir, '.env.e2e')) {
  if (!fs.existsSync(file)) return;
  for (const rawLine of fs.readFileSync(file, 'utf8').split('\n')) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const separator = line.indexOf('=');
    if (separator < 1) continue;
    const key = line.slice(0, separator).trim();
    let value = line.slice(separator + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) process.env[key] = value;
  }
}

// A run against another environment reads its own file, so local settings survive:
//   E2E_ENV_FILE=.env.e2e.prod npm run test:e2e
loadEnvFile(path.resolve(e2eDir, process.env.E2E_ENV_FILE || '.env.e2e'));

function required(key) {
  const value = process.env[key];
  if (!value) throw new Error(`${key} is required. Set it in qa/e2e/.env.e2e (see .env.e2e.example).`);
  return value;
}

/** Turns base@gmail.com into base+suffix@gmail.com so every run owns a distinct inbox address. */
export function plusAddress(address, suffix) {
  const at = address.indexOf('@');
  if (at < 1) throw new Error(`E2E_MAILBOX_ADDRESS is not an email address: ${address}`);
  return `${address.slice(0, at)}+${suffix}${address.slice(at)}`;
}

export const config = {
  get baseUrl() { return required('E2E_BASE_URL').replace(/\/$/, ''); },
  get superAdmin() {
    return { username: required('E2E_SUPERADMIN_USER'), password: required('E2E_SUPERADMIN_PASSWORD') };
  },
  /** When set, mail is read from a local Mailpit catcher instead of a real IMAP account. */
  get mailpitUrl() { return process.env.E2E_MAILPIT_URL || ''; },
  get mailbox() {
    return {
      address: required('E2E_MAILBOX_ADDRESS'),
      host: process.env.E2E_IMAP_HOST || 'imap.gmail.com',
      port: Number(process.env.E2E_IMAP_PORT || 993),
      user: process.env.E2E_IMAP_USER || required('E2E_MAILBOX_ADDRESS'),
      pass: process.env.E2E_MAILPIT_URL ? '' : required('E2E_IMAP_PASSWORD'),
    };
  },
  /** Fallback when the onboarding email cannot be read: SUPERADMIN_ONBOARDING_DEFAULT_ADMIN_PASSWORD. */
  get orgAdminPassword() { return process.env.E2E_ORG_ADMIN_PASSWORD || ''; },
  get adminPhone() { return process.env.E2E_ADMIN_PHONE || ''; },
  get planName() { return process.env.E2E_PLAN_NAME || 'Enterprise'; },
  get billingCycle() { return process.env.E2E_BILLING_CYCLE || 'Monthly'; },
  get trialDays() { return process.env.E2E_TRIAL_DAYS || '14'; },
  get timezone() { return process.env.E2E_TIMEZONE || 'America/Toronto'; },
  /**
   * A zone deliberately different from the clinic's, for asserting that a therapist
   * who sits elsewhere keeps their own schedule timezone instead of the practice one.
   */
  get therapistTimezone() {
    const override = process.env.E2E_THERAPIST_TIMEZONE;
    if (override) return override;
    return this.timezone === 'Asia/Karachi' ? 'Europe/Berlin' : 'Asia/Karachi';
  },
  get region() { return process.env.E2E_REGION || ''; },
  get dataResidency() { return process.env.E2E_DATA_RESIDENCY || ''; },
  /** Delete the organisation at the end only when every earlier step passed (default true). */
  get cleanup() { return process.env.E2E_CLEANUP !== '0'; },
  get mailTimeoutMs() { return Number(process.env.E2E_MAIL_TIMEOUT_MS || 180000); },
  /**
   * EMAIL reads the super admin's code out of the mailbox; TOTP waits for a person to
   * hand one over, for an account whose codes cannot be delivered or read.
   */
  get superAdminFactor() { return (process.env.E2E_SUPERADMIN_MFA || 'EMAIL').toUpperCase(); },
  /**
   * Ask the app to trust this browser for 30 days. The token is kept between runs,
   * so only the first run has to answer a second factor.
   */
  get trustDevice() { return process.env.E2E_TRUST_DEVICE === '1'; },
  /** Submitting a deliberately wrong code first; off for accounts that lock out. */
  get wrongCodeProbe() { return process.env.E2E_WRONG_CODE_PROBE !== '0'; },
};

export const stateDir = path.join(e2eDir, '.state');
export const e2eRoot = e2eDir;

/**
 * The browser context the suite drives. Playwright applies `use` options only to
 * contexts a test asks for; this journey keeps one context across every flow, and so
 * opens it itself and has to carry the same options over by hand.
 */
export const browserContextOptions = {
  baseURL: config.baseUrl,
  viewport: { width: 1440, height: 1000 },
  ignoreHTTPSErrors: true,
};
