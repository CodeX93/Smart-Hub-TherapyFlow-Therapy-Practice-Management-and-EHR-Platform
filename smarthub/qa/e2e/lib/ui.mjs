import { expect } from '@playwright/test';

/** Radix popovers/dropdowns render into a portal outside the form. */
function popover(page) {
  return page.locator('[data-radix-popper-content-wrapper]').last();
}

/** Radix <Select> on the super-admin forms: the trigger shows its placeholder until a value is picked. */
export async function chooseFromSelect(page, triggerText, optionName, { search } = {}) {
  const trigger = page.getByRole('combobox').filter({ hasText: triggerText }).first();
  await expect(trigger, `select "${triggerText}" is on the page`).toBeVisible();
  await trigger.click();
  if (search) {
    await page.getByPlaceholder(search.placeholder).fill(search.query);
  }
  const option = page.getByRole('option', { name: optionName }).first();
  await expect(option, `option "${optionName}" is offered`).toBeVisible();
  await option.click();
  await expect(option).toBeHidden();
}

/** The shared admin CustomSelect: a button labelled by its field, with an optional search box. */
export async function chooseFromCustomSelect(page, fieldLabel, optionText, { searchPlaceholder } = {}) {
  const trigger = page.getByRole('button', { name: fieldLabel, exact: true }).first();
  await expect(trigger, `"${fieldLabel}" selector is on the page`).toBeVisible();
  await trigger.click();
  const menu = popover(page);
  if (searchPlaceholder) {
    const search = menu.getByPlaceholder(searchPlaceholder);
    if (await search.count()) await search.fill(optionText);
  }
  const option = menu.getByText(optionText, { exact: false }).first();
  await expect(option, `"${optionText}" is offered under ${fieldLabel}`).toBeVisible();
  await option.click();
}

/**
 * Ends the current session the way closing the browser would. Cookies alone are not
 * enough: the app keeps its session and its "trust this device" token in web storage,
 * and an authenticated visitor is redirected away from the sign-in pages.
 */
export async function clearSession(page) {
  await page.goto('/');
  await page.evaluate(() => {
    try { localStorage.clear(); sessionStorage.clear(); } catch { /* storage may be blocked */ }
  });
}

/** Signs in on any of the three staff-facing login pages. */
export async function submitLogin(page, { path, username, password }) {
  await page.goto(path);
  await page.getByLabel(/Email (or|\/) [Uu]sername/).first().fill(username);
  await page.getByLabel(/^Password/).first().fill(password);
  await page.getByRole('button', { name: 'Sign In', exact: true }).click();
}

const MFA_PANEL = /Protect your account|Verify it.s really you|Add another sign-in method/;

export function mfaPanel(page) {
  // The activation screen repeats the panel title as its page heading, so take the first.
  return page.getByRole('heading', { name: MFA_PANEL }).first();
}

/**
 * Drives the real MFA panel: enrollment when the account has no factor yet,
 * verification when it does. Codes come from the mailbox/phone, never from the app.
 */
export async function completeMfa(page, {
  codeFor,           // async (method, sentAt) => '123456'
  emailAddress,
  phone,
  preferredMethod = 'EMAIL',
  /** Which already-enrolled factor to answer with, when the account has more than one. */
  verifyWith = 'EMAIL',
  trustDevice = false,
  alsoEnrollSms = false,
  wrongCodeProbe = false,
  log = () => {},
}) {
  await expect(mfaPanel(page), 'the MFA panel is shown').toBeVisible({ timeout: 30000 });
  const heading = (await mfaPanel(page).textContent())?.trim() ?? '';
  const enrolling = /Protect your account|Add another sign-in method/.test(heading);

  if (!enrolling) {
    return verifyWithCode(page, { codeFor, trustDevice, wrongCodeProbe, log, verifyWith });
  }

  await enrollMethod(page, { method: preferredMethod, codeFor, phone, trustDevice, log });
  const smsResult = alsoEnrollSms && phone
    ? await tryEnrollSms(page, { codeFor, phone, trustDevice, log })
    : { attempted: false, enrolled: false, reason: phone ? 'not requested' : 'no phone number configured' };

  await finishEnrollment(page, log);
  return { enrolled: preferredMethod, emailAddress, sms: smsResult };
}

async function enrollMethod(page, { method, codeFor, phone, trustDevice, log }) {
  const label = { EMAIL: 'Email', SMS: 'Text message', TOTP: 'Authenticator app' }[method];
  const choice = page.getByRole('button', { name: new RegExp(`^${label}`) }).first();
  if (await choice.count()) await choice.click();
  if (method === 'SMS') {
    await page.getByPlaceholder('+14155551234').fill(phone);
  }
  const sentAt = new Date();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  const codeField = page.getByLabel('6-digit code');
  await expect(codeField, `the ${label} code field appears`).toBeVisible({ timeout: 45000 });
  log(`${label} enrollment code requested`);
  const code = await codeFor(method, sentAt);
  await codeField.fill(code);
  await setTrustDevice(page, trustDevice);
  await page.getByRole('button', { name: 'Confirm and continue' }).click();
}

async function tryEnrollSms(page, { codeFor, phone, trustDevice, log }) {
  const addAnother = page.getByRole('button', { name: 'Add another sign-in method' });
  if (!(await addAnother.count())) {
    return { attempted: false, enrolled: false, reason: 'the app did not offer a second method' };
  }
  await addAnother.click();
  try {
    await enrollMethod(page, { method: 'SMS', codeFor, phone, trustDevice, log });
    await expect(page.getByRole('alert')).toHaveCount(0, { timeout: 5000 });
    return { attempted: true, enrolled: true };
  } catch (error) {
    // Twilio not configured, number not deliverable, or no SMS arrived: report, do not fail the run.
    const alert = page.getByRole('alert').first();
    const reason = (await alert.count()) ? (await alert.textContent())?.trim() : error.message;
    log(`SMS second factor not completed: ${reason}`);
    const back = page.getByRole('button', { name: /Skip for now|I saved my codes/ }).first();
    if (await back.count()) await back.click();
    return { attempted: true, enrolled: false, reason };
  }
}

async function finishEnrollment(page, log) {
  const done = page.getByRole('button', { name: /I saved my codes|Continue to portal|Return to sign in/ }).first();
  if (await done.count()) {
    log('recovery codes acknowledged');
    await done.click();
  }
}

async function verifyWithCode(page, { codeFor, trustDevice, wrongCodeProbe, log, verifyWith = 'EMAIL' }) {
  const requestedAt = new Date();
  let method = verifyWith;
  // An authenticator app needs no code sent, so it is picked rather than requested.
  const label = verifyWith === 'TOTP' ? /^Authenticator/ : /^Email/;
  const choice = page.getByRole('button', { name: label }).first();
  if (await choice.count()) {
    await choice.click();
  } else if (verifyWith === 'EMAIL'
      && await page.getByText(/code we texted you|Enter the code sent to \*\*\*/).count()) {
    method = 'SMS';
  }
  const codeField = page.getByLabel('6-digit code');
  await expect(codeField, 'the verification code field appears').toBeVisible({ timeout: 45000 });
  if (wrongCodeProbe) {
    await codeField.fill('000000');
    await page.getByRole('button', { name: 'Verify and continue' }).click();
    await expect(
      page.getByRole('alert').first(),
      'a wrong verification code is refused',
    ).toBeVisible({ timeout: 30000 });
    log('wrong verification code refused');
  }
  const code = await codeFor(method, requestedAt);
  await codeField.fill(code);
  await setTrustDevice(page, trustDevice);
  await page.getByRole('button', { name: 'Verify and continue' }).click();
  log(`signed in with a ${method} code`);
  return { verified: method, wrongCodeRefused: wrongCodeProbe };
}

async function setTrustDevice(page, trust) {
  const checkbox = page.getByLabel(/Trust this device/);
  if (!(await checkbox.count())) return;
  const checked = await checkbox.isChecked();
  if (checked !== trust) await checkbox.click();
}

/** Waits for the app's toast, returning its text. */
export async function toastText(page, pattern, timeout = 20000) {
  const toast = page.getByText(pattern).first();
  await expect(toast, `a toast matching ${pattern} is shown`).toBeVisible({ timeout });
  return (await toast.textContent())?.trim() ?? '';
}

const ACTIVATION_HEADING = /Set your password/;

export function activationPanel(page) {
  return page.getByRole('heading', { name: ACTIVATION_HEADING }).first();
}

/**
 * A cold backend can take a while to answer the first sign-in, so wait for whichever
 * comes first: the activation screen a temporary password leads to, the MFA panel, or
 * the landing page of an account that needs neither.
 */
export async function afterSignIn(page, landedUrlPattern, timeout = 60000) {
  const panel = mfaPanel(page).waitFor({ state: 'visible', timeout }).then(() => 'mfa');
  const activate = activationPanel(page).waitFor({ state: 'visible', timeout }).then(() => 'activate');
  const landed = page.waitForURL(landedUrlPattern, { timeout }).then(() => 'landed');
  return Promise.race([panel, activate, landed]);
}

/** The screen a temporary password leads to: choose the password the account keeps. */
export async function setNewPassword(page, password) {
  await page.getByLabel(/^New Password/).first().fill(password);
  await page.getByLabel(/^Confirm New Password/).first().fill(password);
  await page.getByRole('button', { name: /^Set password$/ }).click();
}

/**
 * Signs in and carries the account through whatever the app asks for. A first
 * enrollment ends with "sign in again to continue", so this repeats the sign-in
 * once and verifies with a freshly delivered code.
 */
export async function signInFully(page, {
  path, username, password, landedUrl, codeFor, activationPassword,
  phone, alsoEnrollSms = false, trustDevice = false, wrongCodeProbe = false,
  verifyWith = 'EMAIL', rounds = 3, log = () => {},
}) {
  let outcome = { challenged: false, landed: false, activated: false };
  let currentPassword = password;
  // Each round answers one thing the app asks for and then sends the visitor back to
  // sign in: a temporary password to replace, a factor to enrol, a code to verify.
  // How many it takes differs by environment, so allow one more than local needs.
  for (let round = 1; round <= rounds; round += 1) {
    await submitLogin(page, { path, username, password: currentPassword });
    let stage = await afterSignIn(page, landedUrl);
    if (stage === 'activate') {
      if (!activationPassword) {
        throw new Error('The account was sent to "Set your password" but no activationPassword was given.');
      }
      await setNewPassword(page, activationPassword);
      outcome.activated = true;
      currentPassword = activationPassword;
      log('temporary password replaced on the activation screen');
      stage = await afterSignIn(page, landedUrl);
    }
    if (stage === 'landed') {
      return { ...outcome, landed: true, rounds: round, password: currentPassword };
    }
    outcome.challenged = true;
    const mfa = await completeMfa(page, {
      codeFor,
      verifyWith,
      phone,
      alsoEnrollSms: alsoEnrollSms && round === 1,
      trustDevice,
      wrongCodeProbe,
      log,
    });
    outcome = { ...outcome, ...mfa };
    const landed = await page.waitForURL(landedUrl, { timeout: 30000 }).then(() => true).catch(() => false);
    if (landed) return { ...outcome, landed: true, rounds: round, password: currentPassword };
    log('the app asked for a fresh sign-in after enrollment');
  }
  return outcome;
}

/**
 * Opens a table row's action menu and picks an entry. The two menus in the product
 * render differently — one as a Radix dropdown, one as plain buttons in a popover —
 * so accept either.
 */
export async function pickMenuItem(page, name) {
  const menu = page.locator('[data-radix-popper-content-wrapper]').last();
  const item = menu.getByRole('menuitem', { name }).or(menu.getByRole('button', { name })).first();
  await expect(item, `the menu offers "${name}"`).toBeVisible();
  await item.click();
}

export async function chooseRowAction(page, row, name) {
  await row.getByRole('button').last().click();
  const menu = page.locator('[data-radix-popper-content-wrapper]').last();
  const item = menu.getByRole('menuitem', { name }).or(menu.getByRole('button', { name })).first();
  await expect(item, `the row menu offers "${name}"`).toBeVisible();
  await item.click();
}
