import { expect } from '@playwright/test';
import { config } from '../lib/config.mjs';
import { extractTemporaryPassword } from '../lib/mailbox.mjs';
import { clearSession, signInFully } from '../lib/ui.mjs';
import { journey, note, resuming, saveRunState } from '../lib/journey.mjs';
import { adminChosenPassword, org } from '../lib/fixtures.mjs';
import { codeFromMail } from './organisation.mjs';

/** Takes the temporary password out of the onboarding mail the tenant's admin was sent. */
export async function readTemporaryPassword() {
  const welcome = await journey.mailbox.waitForMessage({
    to: org.adminEmail,
    subjectIncludes: 'Welcome to SmartHub',
    since: new Date(Date.now() - 20 * 60 * 1000),
  });
  try {
    journey.state.adminPassword = extractTemporaryPassword(welcome.body);
    note('temporary password read from the onboarding email');
  } catch (error) {
    expect(
      config.orgAdminPassword,
      `the onboarding email had no readable password (${error.message}) and E2E_ORG_ADMIN_PASSWORD is unset`,
    ).toBeTruthy();
    journey.state.adminPassword = config.orgAdminPassword;
    note('temporary password taken from E2E_ORG_ADMIN_PASSWORD');
  }
  saveRunState();
  return journey.state.adminPassword;
}

/**
 * Signs the tenant's administrator in the whole way: the temporary password is
 * replaced on the activation screen, a second factor is enrolled, and the app is
 * re-entered with a code it emails.
 */
export async function signInAsAdministrator() {
  const { page, context } = journey;
  if (!journey.state.adminPassword) await readTemporaryPassword();

  await clearSession(page);
  await context.clearCookies();
  const result = await signInFully(page, {
    path: '/auth/staff/login',
    username: org.adminEmail,
    // Once activated, the administrator's own password is the one that works.
    password: journey.state.activated || resuming ? adminChosenPassword : journey.state.adminPassword,
    landedUrl: /\/admin\//,
    activationPassword: adminChosenPassword,
    codeFor: (method, sentAt) =>
      method === 'SMS'
        ? Promise.reject(new Error('SMS codes have no automated reader in this suite'))
        : codeFromMail(org.adminEmail, sentAt),
    phone: config.adminPhone,
    alsoEnrollSms: Boolean(config.adminPhone),
    trustDevice: false,
    log: note,
  });

  journey.state.secondFactor = { ...result, password: undefined };
  journey.state.adminPassword = result.password ?? journey.state.adminPassword;
  if (result.activated) journey.state.activated = true;
  expect(result.landed, 'the administrator reached the admin area').toBe(true);
  journey.signedInAs = 'administrator';
  saveRunState();
  return result;
}

/** Puts the browser back in the administrator's session if it is somewhere else. */
export async function ensureAdministratorSignedIn() {
  if (journey.signedInAs === 'administrator') return;
  await signInAsAdministrator();
}
