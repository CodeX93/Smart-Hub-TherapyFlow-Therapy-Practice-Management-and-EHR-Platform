import { expect } from '@playwright/test';
import { chooseSelect, fillFields } from '../lib/forms.mjs';
import { toastText } from '../lib/ui.mjs';
import { journey, markBuilt, note } from '../lib/journey.mjs';
import { company } from '../lib/fixtures.mjs';

/**
 * The clinic's own company details, kept under Settings → Administration. They feed
 * the practice timezone, the note letterhead and the invoices, so what is typed here
 * has to be exactly what every later reader gets back.
 */
export const companyFields = [
  { kind: 'text', label: 'Practice Name', value: company.name },
  { kind: 'text', label: 'Subtitle', value: company.subtitle },
  { kind: 'text', label: 'Tax ID', value: company.taxId },
  { kind: 'text', label: 'NPI Number', value: company.npi },
  { kind: 'text', label: 'License Number', value: company.licenseNumber },
  { kind: 'text', label: 'License State', value: company.licenseState },
  { kind: 'text', label: 'Practice Description', value: company.description },
  { kind: 'text', label: 'Practice Address', value: company.address },
  { kind: 'text', label: 'Phone Number', value: company.phone },
  { kind: 'text', label: 'Email', value: company.email },
  { kind: 'text', label: 'Website URL', value: company.website },
];

/** Opens Settings → Administration and waits for the saved configuration to load. */
export async function openCompanySettings() {
  const { page } = journey;
  await page.goto('/admin/settings?tab=administration');
  await expect(
    page.getByText('Practice Information', { exact: true }).first(),
    'the administration settings open',
  ).toBeVisible({ timeout: 30000 });
  // The form loads the saved configuration after mounting; the name filling in
  // (every tenant is provisioned with one) is the signal the record arrived.
  await expect
    .poll(async () => (await page.getByLabel(/^Practice Name/).first().inputValue()).trim(), {
      timeout: 30000,
      message: 'the saved configuration loads into the form',
    })
    .not.toBe('');
}

/** Fills every company field and saves. The timezone is chosen, not left implied. */
export async function saveCompanyDetails() {
  const { page, entered } = journey;
  await openCompanySettings();
  entered.company = await fillFields(page, page, companyFields);
  // The clinic's zone is deliberately re-picked as the same city the tenant was
  // created with, so saving here never shifts what the schedulers work in.
  entered.company['Select Timezone'] = await chooseSelect(page, page, 'Select Timezone', 'Toronto');

  const save = page.getByRole('button', { name: 'Save Configuration' });
  await expect(save, 'the changed configuration can be saved').toBeEnabled();
  await save.click();
  await toastText(page, /Practice configuration updated successfully/i, 30000);
  markBuilt('company');
  note(`company details saved for ${company.name}`);
}
