import { expect } from '@playwright/test';
import { config, plusAddress } from '../lib/config.mjs';
import { extractOtpCode } from '../lib/mailbox.mjs';
import { chooseFromSelect, clearSession, signInFully, toastText } from '../lib/ui.mjs';
import { waitForHandedCode } from '../lib/handedCode.mjs';
import { restoreDeviceTrust, saveDeviceTrust } from '../lib/deviceTrust.mjs';
import { journey, markBuilt, note } from '../lib/journey.mjs';
import { org, runTag, superAdminMailAddress } from '../lib/fixtures.mjs';

/** Reads the one-time sign-in code the app just emailed to `address`. */
export async function codeFromMail(address, sentAt) {
  const message = await journey.mailbox.waitForMessage({
    to: address,
    subjectIncludes: 'sign-in code',
    since: sentAt,
  });
  return extractOtpCode(message.body);
}

export async function signInAsSuperAdmin({ wrongCodeProbe = false } = {}) {
  const { page, context } = journey;
  const byAuthenticator = config.superAdminFactor === 'TOTP';
  await clearSession(page);
  await context.clearCookies();
  // Clearing the session takes the device-trust token with it; put it back so a
  // browser this account has already trusted is not challenged all over again.
  if (config.trustDevice && await restoreDeviceTrust(page)) {
    note('this browser was trusted by an earlier run');
  }
  const outcome = await signInFully(page, {
    path: '/super-admin/login',
    username: config.superAdmin.username,
    password: config.superAdmin.password,
    landedUrl: /\/super-admin\/(dashboard|organisations)/,
    // An authenticator code reaches no mailbox, so a person hands it to the run.
    codeFor: byAuthenticator
      ? () => waitForHandedCode({ log: note })
      : (_method, sentAt) => codeFromMail(superAdminMailAddress, sentAt),
    verifyWith: byAuthenticator ? 'TOTP' : 'EMAIL',
    trustDevice: config.trustDevice,
    wrongCodeProbe: wrongCodeProbe && config.wrongCodeProbe,
    log: note,
  });
  expect(outcome.landed, 'the super admin reached the platform console').toBe(true);
  if (config.trustDevice && await saveDeviceTrust(page)) {
    note('this browser is now trusted, so the next run needs no second factor');
  }
  journey.signedInAs = 'super-admin';
  return outcome;
}

/** Fills the create-organisation form, leaving it ready to submit. */
export async function fillOrganisationForm(page, values) {
  await page.goto('/super-admin/organisations/new');
  await page.getByPlaceholder('Enter organization name').fill(values.name);
  if (values.slug !== undefined) await page.getByPlaceholder('your-organization-slug').fill(values.slug);
  if (values.adminFirstName) await page.getByPlaceholder('Enter first name').fill(values.adminFirstName);
  if (values.adminLastName) await page.getByPlaceholder('Enter last name').fill(values.adminLastName);
  if (values.adminEmail) await page.getByPlaceholder('admin@organization.com').fill(values.adminEmail);
  if (values.plan === false) return;

  const city = config.timezone.split('/').pop();
  await chooseFromSelect(page, 'Choose your plan', new RegExp(config.planName, 'i'));
  await chooseFromSelect(page, 'Choose timezone', new RegExp(city, 'i'), {
    search: { placeholder: 'Search timezone...', query: city },
  });
  await chooseFromSelect(page, 'Choose infrastructure region', new RegExp(config.region || 'us-east-1'));
  await chooseFromSelect(page, 'Choose data residency', new RegExp(config.dataResidency || 'United States'));
}

/** The values a duplicate-slug attempt uses, so only the slug is the thing being reused. */
export const duplicateOrganisation = () => ({
  name: `${org.name} duplicate`,
  slug: org.slug,
  adminFirstName: 'Duplicate',
  adminLastName: 'Attempt',
  adminEmail: plusAddress(config.mailbox.address, `${runTag}-dup`),
});

export async function createOrganisation() {
  const { page } = journey;
  if (journey.signedInAs !== 'super-admin') await signInAsSuperAdmin();
  await fillOrganisationForm(page, org);

  await page.getByRole('button', { name: 'Create Organization' }).click();
  // Provisioning a tenant schema can take minutes on a database with many tenants,
  // and the success toast clears itself, so the redirect is the signal to wait on.
  await Promise.race([
    page.waitForURL(/\/super-admin\/organisations$/, { timeout: 360000 }),
    toastText(page, /already|exists|Unable to|error/i, 360000).then((text) => {
      throw new Error(`The organisation was refused: ${text}`);
    }),
  ]);

  await page.getByPlaceholder(/Search/i).first().fill(org.name);
  await expect(page.getByText(org.name).first(), 'the new tenant is listed').toBeVisible({ timeout: 30000 });
  markBuilt('organisation');
  note(`organisation "${org.name}" (${org.slug}) created on the ${config.planName} plan`);
}

/** Opens the tenant's own details page from the platform console. */
export async function openOrganisationDetails() {
  const { page } = journey;
  await page.goto('/super-admin/organisations');
  await page.getByPlaceholder(/Search/i).first().fill(org.name);
  // The tenant list is a grid of divs, so each row is reached through its actions button.
  const rowActions = page.getByRole('button', { name: `Open actions for ${org.name}` });
  await expect(rowActions, 'the new tenant is listed').toBeVisible({ timeout: 30000 });
  return rowActions;
}
