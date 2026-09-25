import { expect } from '@playwright/test';
import { config, plusAddress } from '../lib/config.mjs';
import {
  chooseSelect,
  fillFields,
  fillText,
  labelRe,
  onlyVisible,
  readSelect,
  setToggle,
  slotTimes,
} from '../lib/forms.mjs';
import { chooseRowAction, toastText } from '../lib/ui.mjs';
import { journey, markBuilt, note } from '../lib/journey.mjs';
import { room, runTag, stamp, therapist } from '../lib/fixtures.mjs';

export const userFields = [
  { kind: 'text', label: 'Full Name', value: therapist.name },
  { kind: 'text', label: 'Email', value: therapist.email },
  { kind: 'text', label: 'Username', value: therapist.username },
  { kind: 'text', label: 'Phone', value: therapist.phone },
];

export const educationFields = [
  { kind: 'text', label: 'Degree Type', value: 'MSW' },
  { kind: 'text', label: 'Field of Study', value: 'Clinical Social Work' },
  { kind: 'text', label: 'Institution', value: 'QA University' },
  { kind: 'date', label: 'Graduation Date', day: 15 },
  { kind: 'text', label: 'Accreditation Body', value: 'CSWE' },
  { kind: 'toggle', label: 'Institution is accredited', value: true },
  { kind: 'text', label: 'Notes', value: `QA education note ${stamp}` },
];

export const profileFields = {
  license: [
    { kind: 'text', label: 'License Number', value: `LIC-${stamp}` },
    { kind: 'select', label: 'License Type', value: 'LCSW - Licensed Clinical Social Worker' },
    { kind: 'text', label: 'License State', value: 'Ontario' },
    { kind: 'date', label: 'License Expiration', day: 20 },
  ],
  specializations: [
    { kind: 'list', label: 'Specializations', value: ['CBT', 'Trauma Therapy'] },
    { kind: 'list', label: 'Languages', value: ['English', 'Urdu'] },
    { kind: 'text', label: 'Years of Experience', value: '7' },
    { kind: 'text', label: 'Clinical Experience Summary', value: `Synthetic QA summary ${stamp}` },
  ],
  background: [
    { kind: 'text', label: 'Research Background', value: `QA research ${stamp}` },
    { kind: 'text', label: 'Supervisory Experience', value: `QA supervision ${stamp}` },
    { kind: 'text', label: 'Career Objectives', value: `QA objectives ${stamp}` },
  ],
  emergency: [
    { kind: 'text', label: 'Emergency Contact Name', value: 'Dana Contact' },
    { kind: 'text', label: 'Emergency Contact Number', value: '+14155550144' },
    { kind: 'text', label: 'Emergency Contact Email', value: plusAddress(config.mailbox.address, `${runTag}-ice`) },
    { kind: 'text', label: 'Emergency Contact Relation', value: 'Sibling' },
  ],
};

/** Opens Add New User and fills everything but the password. */
export async function openAddUser(values = therapist) {
  const { page } = journey;
  await page.goto('/admin/user-access/profiles');
  await page.getByRole('button', { name: 'Add New User' }).click();
  await expect(page.getByRole('heading', { name: 'Add New User' })).toBeVisible();
  await fillFields(page, page, [
    { kind: 'text', label: 'Full Name', value: values.name },
    { kind: 'text', label: 'Email', value: values.email },
    { kind: 'text', label: 'Username', value: values.username },
    { kind: 'text', label: 'Phone', value: values.phone },
  ]);
  await chooseSelect(page, page, 'Role', 'Therapist');
}

export async function createTherapist() {
  const { page } = journey;
  await openAddUser();
  await fillText(page, 'Password', therapist.password);
  journey.entered.user = Object.fromEntries(userFields.map((f) => [f.label, String(f.value)]));
  journey.entered.user.Role = await readSelect(page, 'Role');

  await page.getByRole('button', { name: 'Add User' }).click();
  await expect(page.getByRole('heading', { name: 'Add New User' })).toBeHidden({ timeout: 45000 });
  await expect(page.getByText(therapist.name).first(), 'the therapist appears in the user list')
    .toBeVisible({ timeout: 30000 });
  markBuilt('therapist');
  note(`therapist ${therapist.name} created`);
}

export async function openTherapistForEdit() {
  const { page } = journey;
  await page.goto('/admin/user-access/profiles');
  const row = page.getByRole('row').filter({ hasText: therapist.name }).first();
  await expect(row).toBeVisible({ timeout: 30000 });
  await chooseRowAction(page, row, 'Edit Basic Info');
  await expect(page.getByRole('heading', { name: 'Edit User' })).toBeVisible({ timeout: 20000 });
}

export async function openProfessionalProfile() {
  const { page } = journey;
  await page.goto('/admin/user-access/profiles');
  const row = page.getByRole('row').filter({ hasText: therapist.name }).first();
  await expect(row, 'the therapist row is on screen').toBeVisible({ timeout: 30000 });
  await chooseRowAction(page, row, 'Professional Details');
  const modal = page.locator('[data-admin-profile-modal]');
  await expect(modal.getByText(/Professional Profile for/), 'the profile editor opens')
    .toBeVisible({ timeout: 30000 });
  return modal;
}

/**
 * Fills every section of the professional profile, including the working day and
 * consultation hours a session later needs, and saves it.
 */
export async function fillProfessionalProfile() {
  const { page, entered } = journey;
  const modal = await openProfessionalProfile();

  await modal.getByRole('button', { name: 'License', exact: true }).click();
  entered.profile.license = await fillFields(page, modal, profileFields.license);

  await modal.getByRole('button', { name: 'Specializations', exact: true }).click();
  entered.profile.specializations = await fillFields(page, modal, profileFields.specializations);

  await modal.getByRole('button', { name: 'Background', exact: true }).click();
  const existingEducation = modal.getByRole('button', { name: /MSW/ });
  if (await existingEducation.count()) {
    await existingEducation.first().click(); // expand the entry a previous run created
  } else {
    await modal.getByRole('button', { name: 'Add Education' }).click();
  }
  entered.profile.education = await fillFields(page, modal, educationFields);
  await setToggle(modal, 'Institution is accredited', true);
  entered.profile.education['Institution is accredited'] = true;
  entered.profile.background = await fillFields(page, modal, profileFields.background);

  await modal.getByRole('button', { name: 'Schedule', exact: true }).click();
  // A therapist may sit outside the clinic's zone: the practice timezone is only the
  // default a new profile starts from, and a pick of their own has to survive a reopen.
  // Choose a zone that is deliberately not the clinic's so a silent revert would fail.
  const timezoneCity = config.therapistTimezone.split('/').pop().replace(/_/g, ' ');
  entered.profile.schedule = {
    'Time Zone': await chooseSelect(page, modal, 'Time Zone', timezoneCity),
  };
  expect(entered.profile.schedule['Time Zone'], 'the therapist zone is not the clinic zone')
    .not.toContain(config.timezone.split('/').pop().replace(/_/g, ' '));
  await modal.getByRole('button', { name: /Select Rooms/ }).click();
  const roomMenu = page.locator('[data-radix-popper-content-wrapper]').last();
  await expect(roomMenu.getByText(room.name, { exact: false }).first(), 'the new room is offered')
    .toBeVisible();
  await roomMenu.getByText(room.name, { exact: false }).first().click();
  await page.keyboard.press('Escape');
  await (await onlyVisible(modal.getByLabel(labelRe('Monday')))).check();
  await (await onlyVisible(modal.getByRole('button', { name: 'In-person', exact: true }))).click();
  await fillText(modal, 'Max Clients / day', '6');
  await fillText(modal, 'Session Duration (min)', '50');
  entered.profile.schedule['Max Clients / day'] = '6';
  entered.profile.schedule['Session Duration (min)'] = '50';
  entered.profile.schedule.slots = await slotTimes(modal);
  expect(entered.profile.schedule.slots.length, 'the working day offers a start and end time')
    .toBeGreaterThanOrEqual(2);

  await modal.getByRole('button', { name: 'Consultation Schedule', exact: true }).click();
  await expect(
    modal.getByRole('heading', { name: 'Consultation / public site hours' }),
    'the consultation hours section opens',
  ).toBeVisible();
  await (await onlyVisible(modal.getByLabel(labelRe('Tuesday')))).check();
  entered.profile.consultation = { slots: await slotTimes(modal) };

  await modal.getByRole('button', { name: 'Emergency Contact', exact: true }).click();
  entered.profile.emergency = await fillFields(page, modal, profileFields.emergency);

  await modal.getByRole('button', { name: 'Save Profile' }).click();
  // The editor stays open after saving, so the toast is the confirmation to wait on.
  // A resumed run can legitimately have nothing left to change, and then no toast appears.
  const saved = await toastText(page, /Profile (created|updated) successfully/i, 60000).catch(() => '');
  if (saved) {
    note(`professional profile saved for ${therapist.name}, with room ${room.name} on Monday (${saved})`);
  } else {
    await expect(
      page.getByText(/fix the highlighted fields|unable|failed/i).first(),
      'the profile was not refused',
    ).toBeHidden();
    note('the profile already held these values, so the save had nothing to send');
  }
  markBuilt('profile');
  await page.keyboard.press('Escape');
  await modal.locator('button').first().click().catch(() => {});
  return modal;
}
