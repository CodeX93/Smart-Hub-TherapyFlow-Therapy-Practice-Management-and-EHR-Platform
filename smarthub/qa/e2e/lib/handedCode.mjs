import fs from 'node:fs';
import path from 'node:path';
import { stateDir } from './config.mjs';

/**
 * A second factor nobody can read out of a mailbox — an authenticator app's code —
 * has to be handed to the run by the person sitting with it. The run waits for the
 * code to appear in a file, uses it once, and removes it, so the next prompt waits
 * for a fresh one rather than replaying a code that has already expired.
 */
export const handedCodePath = () =>
  path.resolve(process.env.E2E_CODE_FILE || path.join(stateDir, 'mfa-code.txt'));

export function clearHandedCode() {
  fs.rmSync(handedCodePath(), { force: true });
}

/** Writes a code for a waiting run to pick up. */
export function handOverCode(code) {
  fs.mkdirSync(path.dirname(handedCodePath()), { recursive: true });
  fs.writeFileSync(handedCodePath(), `${code}\n`);
}

export async function waitForHandedCode({ timeoutMs = 600000, pollMs = 2000, log = () => {} } = {}) {
  const file = handedCodePath();
  clearHandedCode();
  log(`waiting for an authenticator code — write six digits into ${file}`);
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const code = fs.existsSync(file) ? (fs.readFileSync(file, 'utf8').match(/\d{6}/) ?? [])[0] : undefined;
    if (code) {
      clearHandedCode();
      log('authenticator code received');
      return code;
    }
    await new Promise((resolve) => setTimeout(resolve, pollMs));
  }
  throw new Error(
    `No authenticator code was written to ${file} within ${Math.round(timeoutMs / 1000)}s.`,
  );
}
