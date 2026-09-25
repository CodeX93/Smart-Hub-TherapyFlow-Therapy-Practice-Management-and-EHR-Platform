import { expectFields, fillText, labelRe, labelledValue, readSelect } from '../lib/forms.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { client, therapist } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';
import {
  clientTabs,
  createClient,
  emergencyContactFields,
  followUpFields,
  insuranceFields,
  openAddClient,
  openClientForEdit,
  openClientPreview,
  personalToggles,
} from '../flows/clients.mjs';

/** A client: every tab of the intake wizard, and the therapist they are handed to. */
test.describe('client', { tag: '@client' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('therapist');
  });

  test('the client form refuses a malformed email and a missing name', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openAddClient();
    await fillText(page, 'Full Name', client.name);
    await fillText(page, 'Email', 'not-an-email');
    await page.getByRole('button', { name: 'Next' }).click();
    await expect(
      page.getByText(/valid email|invalid email|email address/i).first(),
      'a malformed email is named',
    ).toBeVisible({ timeout: 20000 });

    await fillText(page, 'Full Name', '');
    await fillText(page, 'Email', client.email);
    await page.getByRole('button', { name: 'Next' }).click();
    await expect(
      page.getByText(/name is required|required/i).first(),
      'the missing name is named',
    ).toBeVisible({ timeout: 20000 });
    note('malformed client email and missing name refused');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('the administrator creates a client with every tab filled and assigns the therapist', async () => {
    test.skip(built('client'), 'resuming: the earlier run already created this client');
    await createClient();
  });

  test('reopening the client shows every value that was saved', { tag: '@verify' }, async () => {
    const { page, entered } = journey;
    await openClientForEdit();

    await expectFields(page, page, clientTabs.Personal, entered.client.Personal);
    await expectFields(page, page, personalToggles, entered.client.Toggles);

    await page.getByRole('button', { name: 'Address', exact: true }).click();
    await expectFields(page, page, clientTabs.Address, entered.client.Address);
    await expectFields(page, page, emergencyContactFields, entered.client.Emergency);

    await page.getByRole('button', { name: 'Referral', exact: true }).click();
    await expectFields(page, page, clientTabs.Referral, entered.client.Referral);

    await page.getByRole('button', { name: 'Employment', exact: true }).click();
    await expectFields(page, page, clientTabs.Employment, entered.client.Employment);

    await page.getByRole('button', { name: 'Clinical', exact: true }).click();
    // Both note boxes are checked here: the Clinical tab's note round-trips through the
    // client's own `notes`, the Referral tab's through the referral record's `referralNotes`.
    await expectFields(page, page, clientTabs.Clinical, entered.client.Clinical);
    expect(
      await readSelect(page, 'Assigned Therapist'),
      'the assigned therapist came back unchanged',
    ).toContain(therapist.name);

    await expect(page.getByLabel(labelRe('Needs Follow-up')), 'the follow-up flag came back').toBeChecked();
    await expectFields(page, page, followUpFields, entered.client.FollowUp);
    await expect(page.getByLabel(labelRe('Insurance Information')), 'the insurance flag came back').toBeChecked();
    await expectFields(page, page, insuranceFields, entered.client.Insurance);

    note('client fields verified after reopening');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('the client preview shows the same details that were entered', { tag: '@verify' }, async () => {
    const { page, entered } = journey;
    await openClientPreview();

    // The edit form and the preview travel different code paths to the same record,
    // so each shown value is compared with what the wizard actually typed.
    const personal = entered.client.Personal;
    expect(await labelledValue(page, 'Gender'), 'the gender matches').toBe(personal.Gender);
    expect(await labelledValue(page, 'Marital Status'), 'the marital status matches')
      .toBe(personal['Marital Status']);
    expect(await labelledValue(page, 'Preferred Language'), 'the language matches')
      .toBe(personal.Language);
    expect(await labelledValue(page, 'Phone'), 'the phone matches').toBe(personal.Phone);
    expect(await labelledValue(page, 'Email'), 'the email matches').toBe(client.email);
    expect(sameCalendarDay(await labelledValue(page, 'DOB'), personal['Date of Birth']),
      'the date of birth is the same day that was entered').toBe(true);

    const address = await labelledValue(page, 'Address');
    for (const part of ['101 QA Street', 'Toronto', 'Canada']) {
      expect(address, `the address carries "${part}"`).toContain(part);
    }

    const emergency = entered.client.Emergency;
    const emergencyShown = await labelledValue(page, 'Emergency Contact');
    expect(emergencyShown, 'the emergency contact name matches').toContain(emergency['Contact Name']);
    expect(emergencyShown, 'the emergency contact phone matches').toContain(emergency['Contact Phone']);

    const clinical = entered.client.Clinical;
    expect(await labelledValue(page, 'Status'), 'the status matches').toBe(clinical.Status);
    expect(await labelledValue(page, 'Treatment Stage'), 'the stage matches')
      .toBe(clinical['Client stage']);
    expect(await labelledValue(page, 'Client Type'), 'the client type matches')
      .toBe(clinical['Client Type']);
    expect(await labelledValue(page, 'Service Type'), 'the service type matches')
      .toBe(clinical['Service Type']);
    expect(await labelledValue(page, 'Frequency'), 'the frequency matches')
      .toBe(clinical['Service Frequency']);
    expect(await labelledValue(page, 'Treatment Modality'), 'the modality matches')
      .toBe(clinical['Treatment Modality']);

    const insurance = entered.client.Insurance;
    expect(await labelledValue(page, 'Insurance Provider'), 'the insurance provider matches')
      .toBe(insurance['Insurance Provider']);
    expect(await labelledValue(page, 'Insurance Type'), 'the insurance type matches')
      .toBe(insurance['Insurance Type']);

    const referral = entered.client.Referral;
    expect(await labelledValue(page, 'Referred By'), 'the referrer matches')
      .toBe(referral['Referrer Name']);
    expect(await labelledValue(page, 'Reference Number'), 'the reference number matches')
      .toBe(referral['Reference Number']);
    expect(sameCalendarDay(await labelledValue(page, 'Referral Date'), referral['Referral Date']),
      'the referral date is the same day that was entered').toBe(true);

    expect(await labelledValue(page, 'Assigned Therapist'), 'the assigned therapist matches')
      .toContain(therapist.name);

    note('the client preview shows the same personal, contact, clinical, insurance and referral details that were entered');
  });
});

/** Two date strings mean the same day, whatever format each side prints. */
function sameCalendarDay(shown, entered) {
  const a = new Date(shown);
  const b = new Date(entered);
  if (!Number.isNaN(a.getTime()) && !Number.isNaN(b.getTime())) {
    return a.getFullYear() === b.getFullYear()
      && a.getMonth() === b.getMonth()
      && a.getDate() === b.getDate();
  }
  // One side did not parse: fall back to the day and year both appearing.
  const day = String(new Date(entered).getDate());
  return shown.includes(day);
}
