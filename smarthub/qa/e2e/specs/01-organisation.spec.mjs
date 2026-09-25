import { config } from '../lib/config.mjs';
import { labelledValue } from '../lib/forms.mjs';
import { pickMenuItem, submitLogin, toastText } from '../lib/ui.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { org, superAdminMailAddress } from '../lib/fixtures.mjs';
import {
  createOrganisation,
  duplicateOrganisation,
  fillOrganisationForm,
  openOrganisationDetails,
  signInAsSuperAdmin,
} from '../flows/organisation.mjs';

/**
 * The platform console: a super admin signs in and provisions an enterprise tenant,
 * and the console refuses what it owes a refusal — a wrong password, an incomplete
 * form, a slug that is already taken.
 */
test.describe('organisation', { tag: '@organisation' }, () => {
  test.describe.configure({ mode: 'serial' });

  test('a wrong super-admin password is refused', { tag: '@negative' }, async () => {
    await submitLogin(journey.page, {
      path: '/super-admin/login',
      username: config.superAdmin.username,
      password: `${config.superAdmin.password}-not-the-password`,
    });
    await expect(
      journey.page.getByText(/invalid|incorrect|failed|not match|credentials/i).first(),
      'the app says the credentials are wrong',
    ).toBeVisible({ timeout: 30000 });
    await expect(journey.page, 'and it keeps the visitor on the sign-in page')
      .toHaveURL(/\/super-admin\/login/);
    note('wrong super-admin password refused');
  });

  test('the super admin signs in, clearing multi-factor authentication', async () => {
    const outcome = await signInAsSuperAdmin({ wrongCodeProbe: true });
    note(
      outcome.challenged
        ? `super admin cleared MFA using ${superAdminMailAddress}`
        : 'this environment did not challenge the super admin for a second factor',
    );
  });

  test('the create-organisation form refuses an incomplete submission', { tag: '@negative' }, async () => {
    test.skip(built('organisation'), 'resuming against the organisation an earlier run created');
    await fillOrganisationForm(journey.page, { name: org.name, plan: false });
    await journey.page.getByRole('button', { name: 'Create Organization' }).click();
    await expect(
      journey.page.getByText(/required|Please|select|Choose/i).first(),
      'the form names what is still missing',
    ).toBeVisible({ timeout: 20000 });
    await expect(journey.page, 'and nothing is created').toHaveURL(/\/organisations\/new/);
    note('incomplete organisation form refused');
  });

  test('the super admin creates an enterprise organisation', async () => {
    test.skip(built('organisation'), 'resuming against the organisation an earlier run created');
    // Provisioning a tenant schema can take minutes on a database with many tenants.
    test.setTimeout(420000);
    await createOrganisation();
  });

  test('the same tenant slug cannot be used twice', { tag: '@negative' }, async () => {
    await fillOrganisationForm(journey.page, duplicateOrganisation());
    await journey.page.getByRole('button', { name: 'Create Organization' }).click();
    await toastText(journey.page, /already|exists|taken|unique|in use/i, 60000);
    await expect(journey.page, 'the duplicate is not created').toHaveURL(/\/organisations\/new/);
    note('duplicate tenant slug refused');
  });

  test("the organisation's own details come back as they were entered", { tag: '@verify' }, async () => {
    const { page } = journey;
    const rowActions = await openOrganisationDetails();
    await rowActions.click();
    await pickMenuItem(page, 'View Details');
    await expect(
      page.getByText('General Information', { exact: true }).first(),
      'the details page opens on its overview',
    ).toBeVisible({ timeout: 45000 });

    expect(await labelledValue(page, 'Organization Name')).toBe(org.name);
    expect(await labelledValue(page, 'Organization Slug')).toContain(org.slug);
    expect(
      await labelledValue(page, 'Primary Admin Email'),
      'the administrator address the onboarding mail was sent to',
    ).toBe(org.adminEmail);
    expect(
      await labelledValue(page, 'Support Email'),
      'a support address is its own setting, and nothing has set one yet',
    ).toBe('-');
    // Stored codes, not the labels the form offered: check the code is the one chosen.
    expect(await labelledValue(page, 'Region')).toContain(config.region || 'us-east-1');
    expect(await labelledValue(page, 'Data Residency')).toMatch(/US|United States/i);
    expect(await labelledValue(page, 'Timezone'))
      .toContain(config.timezone.split('/').pop().replace(/_/g, ' '));
    expect(await labelledValue(page, 'Plan')).toMatch(new RegExp(config.planName, 'i'));
    expect(await labelledValue(page, 'Billing Cycle')).toMatch(/month|annual|year/i);
    note(`the tenant page shows ${org.adminEmail} as its primary administrator, and its name, slug, region, residency, timezone, plan and billing cycle all read back`);
  });
});
