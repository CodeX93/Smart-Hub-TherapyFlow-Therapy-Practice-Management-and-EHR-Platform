import { expectFields, fillText, readSelect } from '../lib/forms.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { need } from '../flows/prerequisites.mjs';
import { companyFields, openCompanySettings, saveCompanyDetails } from '../flows/company.mjs';

/**
 * The company details under Settings → Administration: the form refuses what it
 * owes a refusal, keeps every value that was typed, and gives it all back after a
 * fresh page load — which, with caching on, also proves the save reaches past the
 * cache instead of the form reading its own stale copy back.
 */
test.describe('company details', { tag: '@company' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('administrator');
  });

  test('malformed identifiers and addresses are refused', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openCompanySettings();

    await fillText(page, 'Tax ID', 'not-a-tax-id');
    await fillText(page, 'NPI Number', '12345');
    await fillText(page, 'Email', 'not-an-email');
    // A bare scheme always fails the URL parser; text with spaces can slip through it.
    await fillText(page, 'Website URL', 'https://');
    await page.getByRole('button', { name: 'Save Configuration' }).click();

    await expect(page.getByText(/Tax ID must be in format/i).first(), 'a malformed tax id is named')
      .toBeVisible({ timeout: 20000 });
    await expect(page.getByText(/NPI number must be exactly 10 digits/i).first(), 'a short NPI is named')
      .toBeVisible();
    await expect(page.getByText(/Invalid email address/i).first(), 'a malformed email is named')
      .toBeVisible();
    await expect(page.getByText(/Invalid URL/i).first(), 'a malformed website is named')
      .toBeVisible();
    note('malformed tax id, NPI, email and website all refused in the form');
  });

  test('the administrator fills in and saves the company details', async () => {
    test.skip(built('company'), 'resuming: the earlier run already saved these details');
    await saveCompanyDetails();
  });

  test('a fresh page load gives every saved value back', { tag: '@verify' }, async () => {
    const { page, entered } = journey;
    // A full reopen, not the form's own state: the values must come back from the server.
    await openCompanySettings();
    await expectFields(page, page, companyFields, entered.company);
    expect(await readSelect(page, 'Select Timezone'), 'the timezone came back unchanged')
      .toContain('Toronto');
    note('company name, identifiers, contact details and timezone all came back after a reload');
  });
});
