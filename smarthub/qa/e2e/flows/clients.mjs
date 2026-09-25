import { expect } from '@playwright/test';
import { chooseSelect, fillFields, fillText, labelRe, setToggle } from '../lib/forms.mjs';
import { chooseRowAction } from '../lib/ui.mjs';
import { journey, markBuilt, note } from '../lib/journey.mjs';
import { client, stamp, therapist } from '../lib/fixtures.mjs';

export const clientTabs = {
  Personal: [
    { kind: 'text', label: 'Full Name', value: client.name },
    { kind: 'text', label: 'Email', value: client.email },
    { kind: 'text', label: 'Phone', value: '+14155550166' },
    { kind: 'date', label: 'Date of Birth', day: 12 },
    { kind: 'select', label: 'Gender', value: 'Female' },
    { kind: 'select', label: 'Marital Status', value: 'Married' },
    { kind: 'text', label: 'Pronouns', value: 'she/her' },
    { kind: 'select', label: 'Language', value: 'English' },
  ],
  Address: [
    { kind: 'text', label: 'Street Address 1', value: '101 QA Street' },
    { kind: 'text', label: 'Street Address 2', value: 'Unit 4' },
    { kind: 'text', label: 'City', value: 'Toronto' },
    { kind: 'text', label: 'State/Province', value: 'Ontario' },
    { kind: 'text', label: 'ZIP / Postal Code', value: 'M4B1B3' },
    { kind: 'text', label: 'Country', value: 'Canada' },
  ],
  Referral: [
    { kind: 'date', label: 'Start Date', day: 10 },
    { kind: 'date', label: 'Referral Date', day: 11 },
    { kind: 'text', label: 'Referrer Name', value: 'Dr QA Referrer' },
    { kind: 'text', label: 'Reference Number', value: `REF-${stamp}` },
    // Seeded per tenant, so take whatever this organisation actually offers.
    { kind: 'select', label: 'Client Source' },
    { kind: 'text', label: 'Referral Notes', value: `Synthetic referral note ${stamp}` },
  ],
  Employment: [
    { kind: 'select', label: 'Employment Status', value: 'Employed Full-Time' },
    { kind: 'select', label: 'Education Level', value: "Bachelor's Degree" },
    { kind: 'text', label: 'Number of Dependents', value: '2' },
  ],
  Clinical: [
    // Scheduling hides every client that is not active, so this one has to be.
    { kind: 'select', label: 'Status', value: 'Active' },
    { kind: 'select', label: 'Client Type' },
    { kind: 'select', label: 'Client stage' },
    { kind: 'select', label: 'Service Type' },
    { kind: 'select', label: 'Service Frequency' },
    { kind: 'select', label: 'Treatment Modality' },
    { kind: 'text', label: 'General notes about the client...', value: `QA general note ${stamp}` },
  ],
};

export const personalToggles = [
  { kind: 'toggle', label: 'Enable Portal Access', value: true },
  { kind: 'toggle', label: 'Email Notifications', value: true },
];

export const followUpFields = [
  { kind: 'select', label: 'Priority' },
  { kind: 'date', label: 'Due Date', day: 28 },
  { kind: 'text', label: 'Brief follow-up notes...', value: `QA follow-up ${stamp}` },
];

export const insuranceFields = [
  { kind: 'select', label: 'Insurance Provider' },
  { kind: 'select', label: 'Insurance Type' },
  { kind: 'text', label: 'Policy Number', value: `POL-${stamp}` },
  { kind: 'text', label: 'Group Number', value: `GRP-${stamp}` },
  { kind: 'money', label: 'Copay Amount', value: '25.00' },
  { kind: 'money', label: 'Deductible', value: '500.00' },
  { kind: 'text', label: 'Insurance Phone', value: '+14155550188' },
];

export const emergencyContactFields = [
  { kind: 'text', label: 'Contact Name', value: 'Sam Emergency' },
  { kind: 'text', label: 'Contact Phone', value: '+14155550177' },
  { kind: 'select', label: 'Relationship to Client', value: 'Sibling' },
];

export async function openAddClient() {
  const { page } = journey;
  await page.goto('/admin/clients');
  await page.getByRole('button', { name: /Add Client/ }).click();
  await expect(page.getByLabel(labelRe('Full Name')).first(), 'the client form opens')
    .toBeVisible({ timeout: 30000 });
}

/** Walks every tab of the client wizard, filling all of it, and saves. */
export async function createClient() {
  const { page, entered } = journey;
  await openAddClient();

  entered.client.Personal = await fillFields(page, page, clientTabs.Personal);
  entered.client.Toggles = await fillFields(page, page, personalToggles);
  await page.getByRole('button', { name: 'Next' }).click();

  entered.client.Address = await fillFields(page, page, clientTabs.Address);
  await page.getByLabel(labelRe('Emergency Contact')).check();
  entered.client.Emergency = await fillFields(page, page, emergencyContactFields);
  await page.getByRole('button', { name: 'Next' }).click();

  entered.client.Referral = await fillFields(page, page, clientTabs.Referral);
  await page.getByRole('button', { name: 'Next' }).click();

  entered.client.Employment = await fillFields(page, page, clientTabs.Employment);
  await page.getByRole('button', { name: 'Next' }).click();

  entered.client.Clinical = await fillFields(page, page, clientTabs.Clinical);
  entered.client.Clinical['Assigned Therapist'] = await chooseSelect(
    page, page, 'Assigned Therapist', therapist.name,
  );

  await setToggle(page, 'Needs Follow-up', true);
  entered.client.FollowUp = await fillFields(page, page, followUpFields);
  await setToggle(page, 'Insurance Information', true);
  entered.client.Insurance = await fillFields(page, page, insuranceFields);

  await page.getByRole('button', { name: 'Create Client' }).click();
  await expect(page.getByText(client.name).first(), 'the client is listed')
    .toBeVisible({ timeout: 60000 });
  markBuilt('client');
  note(`client ${client.name} created and assigned to ${therapist.name}`);
}

export async function openClientForEdit() {
  const { page } = journey;
  await page.goto('/admin/clients');
  const row = page.getByRole('row').filter({ hasText: client.name }).first();
  await expect(row, 'the client row is on screen').toBeVisible({ timeout: 45000 });
  await chooseRowAction(page, row, /^Edit/);
  // The edit form loads its record after opening, so wait for the record, not the field.
  await expect(page.getByLabel(labelRe('Full Name')).first(), 'the saved client loads into the form')
    .toHaveValue(client.name, { timeout: 45000 });
}

/**
 * Opens the client's read-only preview panel (the eye on their row) and expands
 * every collapsed section, so each shown value can be compared with what was typed.
 */
export async function openClientPreview() {
  const { page } = journey;
  await page.goto('/admin/clients');
  const view = page.getByRole('button', { name: `View ${client.name}` }).first();
  await expect(view, 'the client row offers a preview').toBeVisible({ timeout: 45000 });
  await view.click();
  await expect(page.getByText('Assigned Therapist').first(), 'the preview panel opens')
    .toBeVisible({ timeout: 30000 });
  // Only General Information starts expanded; open the rest before reading them.
  for (const section of ['Clinical Status', 'Referral Information']) {
    const heading = page.getByText(section, { exact: true }).first();
    await expect(heading, `the preview has a ${section} section`).toBeVisible();
    await heading.click();
  }
}

/** Fills only the first tab, for the checks the form owes before it will move on. */
export async function fillFirstClientStep({ name, email }) {
  const { page } = journey;
  await fillText(page, 'Full Name', name);
  await fillText(page, 'Email', email);
  await page.getByRole('button', { name: 'Next' }).click();
}
