import fs from 'node:fs';
import path from 'node:path';
import { stateDir } from './config.mjs';

/**
 * When an account is asked to trust the browser, the app keeps an opaque token in
 * localStorage under `tf.deviceTrust.<login>`. Every sign-in here starts by clearing
 * that storage, which would throw the trust away with it — so the token is kept
 * beside the run state and put back, and a later run is not challenged again.
 *
 * The tokens are real credentials for the accounts they belong to. They live in
 * qa/e2e/.state/, which is gitignored, and expire on their own.
 */
const PREFIX = 'tf.deviceTrust.';
const trustPath = () => path.join(stateDir, 'device-trust.json');

function read() {
  try {
    return JSON.parse(fs.readFileSync(trustPath(), 'utf8'));
  } catch {
    return {};
  }
}

/** Keeps whatever device-trust tokens the page has picked up. */
export async function saveDeviceTrust(page) {
  const found = await page.evaluate((prefix) => {
    const out = {};
    try {
      for (let i = 0; i < localStorage.length; i += 1) {
        const key = localStorage.key(i);
        if (key?.startsWith(prefix)) out[key] = localStorage.getItem(key);
      }
    } catch { /* storage may be blocked */ }
    return out;
  }, PREFIX);
  if (!Object.keys(found).length) return false;
  fs.mkdirSync(stateDir, { recursive: true });
  fs.writeFileSync(trustPath(), JSON.stringify({ ...read(), ...found }, null, 2));
  return true;
}

/** Puts them back after the session storage has been cleared. Must run on the app's origin. */
export async function restoreDeviceTrust(page) {
  const saved = read();
  const keys = Object.keys(saved);
  if (!keys.length) return false;
  await page.evaluate((entries) => {
    try {
      for (const [key, value] of Object.entries(entries)) localStorage.setItem(key, value);
    } catch { /* storage may be blocked */ }
  }, saved);
  return true;
}

export function forgetDeviceTrust() {
  fs.rmSync(trustPath(), { force: true });
}
