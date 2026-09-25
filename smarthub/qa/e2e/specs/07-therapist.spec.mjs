import {
  expectFields,
  expectSlotType,
  fillText,
  labelRe,
  onlyVisible,
  readSelect,
  readText,
  slotTimes,
} from '../lib/forms.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { room, therapist } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';
import {
  createTherapist,
  educationFields,
  fillProfessionalProfile,
  openAddUser,
  openProfessionalProfile,
  openTherapistForEdit,
  profileFields,
  userFields,
} from '../flows/therapists.mjs';

/**
 * A therapist: the account itself, then the professional profile that gives them a
 * licence, a room, a working day and the hours a session can later be booked in.
 */
test.describe('therapist', { tag: '@therapist' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('administrator', 'room');
  });

  test('a therapist account cannot be created with a short password', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openAddUser();
    await fillText(page, 'Password', 'short');
    await expect(
      page.getByRole('button', { name: 'Add User' }),
      'the app will not submit a password under eight characters',
    ).toBeDisabled();
    note('short therapist password refused');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('the administrator creates a therapist with every basic field filled', async () => {
    test.skip(built('therapist'), 'resuming: the earlier run already created this therapist');
    await createTherapist();
  });

  test('the same therapist email cannot be used for a second account', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openAddUser({ ...therapist, name: `${therapist.name} duplicate`, username: `${therapist.username}-2` });
    await fillText(page, 'Password', therapist.password);
    await page.getByRole('button', { name: 'Add User' }).click();
    await expect(
      page.getByText(/already|exists|taken|in use|duplicate/i).first(),
      'the duplicate email is refused',
    ).toBeVisible({ timeout: 45000 });
    note('duplicate therapist email refused');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('reopening the therapist shows every value that was saved', { tag: '@verify' }, async () => {
    const { page, entered } = journey;
    await openTherapistForEdit();
    // A resumed run may not have recorded these itself; the values are fixed by the run tag.
    const expectedUser = entered.user?.Role
      ? entered.user
      : { ...Object.fromEntries(userFields.map((f) => [f.label, String(f.value)])), Role: 'Therapist' };
    await expectFields(page, page, userFields, expectedUser);
    expect(await readSelect(page, 'Role'), 'the role came back unchanged').toContain(expectedUser.Role);
    note('therapist basic fields verified after reopening');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test("the administrator fills the therapist's whole professional profile", async () => {
    await fillProfessionalProfile();
  });

  test('reopening the professional profile shows every value that was saved', { tag: '@verify' }, async () => {
    const { page, entered } = journey;
    const modal = await openProfessionalProfile();

    await modal.getByRole('button', { name: 'License', exact: true }).click();
    // The editor fetches the profile after opening; wait for the record to arrive.
    await expect(modal.getByLabel(labelRe('License Number')).first(), 'the saved profile loads')
      .toHaveValue(entered.profile.license['License Number'], { timeout: 45000 });
    await expectFields(page, modal, profileFields.license, entered.profile.license);

    await modal.getByRole('button', { name: 'Specializations', exact: true }).click();
    await expectFields(page, modal, profileFields.specializations, entered.profile.specializations);

    await modal.getByRole('button', { name: 'Background', exact: true }).click();
    await expectFields(page, modal, profileFields.background, entered.profile.background);
    // Saved education entries come back collapsed behind a summary of their own values.
    const educationSummary = modal.getByRole('button', { name: /MSW/ }).first();
    await expect(educationSummary, 'the education entry is listed').toBeVisible({ timeout: 20000 });
    await educationSummary.click();
    await expectFields(page, modal, educationFields, entered.profile.education);

    await modal.getByRole('button', { name: 'Schedule', exact: true }).click();
    expect(
      await readSelect(modal, 'Time Zone'),
      "the therapist's own schedule timezone survived the reopen instead of reverting to the clinic's",
    ).toContain(entered.profile.schedule['Time Zone']);
    await expect(modal.getByText(room.name, { exact: false }).first(), 'the assigned room came back')
      .toBeVisible();
    await expect(await onlyVisible(modal.getByLabel(labelRe('Monday'))), 'Monday is still a working day')
      .toBeChecked();
    expect(await readText(modal, 'Max Clients / day')).toBe('6');
    expect(await readText(modal, 'Session Duration (min)')).toBe('50');
    expect(await slotTimes(modal), "Monday's hours came back unchanged")
      .toEqual(entered.profile.schedule.slots);
    await expectSlotType(modal, 'In-person');

    await modal.getByRole('button', { name: 'Consultation Schedule', exact: true }).click();
    await expect(
      modal.getByRole('heading', { name: 'Consultation / public site hours' }),
      'the consultation hours section opens',
    ).toBeVisible();
    await expect(
      await onlyVisible(modal.getByLabel(labelRe('Tuesday'))),
      'the consultation day switched on before saving is still a consultation day',
    ).toBeChecked();
    expect(await slotTimes(modal), 'the consultation hours came back unchanged')
      .toEqual(entered.profile.consultation.slots);
    note('the consultation hours came back unchanged');

    await modal.getByRole('button', { name: 'Emergency Contact', exact: true }).click();
    await expectFields(page, modal, profileFields.emergency, entered.profile.emergency);

    note('professional profile verified after reopening');
    await page.keyboard.press('Escape');
  });
});
