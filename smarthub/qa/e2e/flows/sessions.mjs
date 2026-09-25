import { expect } from '@playwright/test';
import { chooseSelect, nextWeekdayDate, pickCalendarDate, readSelect } from '../lib/forms.mjs';
import { pickMenuItem } from '../lib/ui.mjs';
import { journey, markBuilt, note } from '../lib/journey.mjs';
import { client, room, service, session, therapist } from '../lib/fixtures.mjs';

/** The scheduling modal, keyed off its own heading. */
function sessionModal(page, heading = 'Schedule New Session') {
  return page.getByRole('heading', { name: heading }).locator('xpath=ancestor::div[2]');
}

/** The block holding the available times, so its buttons are never confused with others. */
function slotArea(modal) {
  return modal.getByText(/^Select time slot/).first().locator('xpath=ancestor::div[1]');
}

/**
 * A client who already has a therapist takes that therapist with them: the form
 * locks the field rather than offering a choice. Either way, this says who it is.
 */
export async function pickTherapist(modal) {
  const { page } = journey;
  const picker = modal.getByRole('button', { name: /^Therapist\s*\*?$/ });
  if (!(await picker.count())) return null;
  if (await picker.first().isDisabled()) return readSelect(modal, 'Therapist');
  return chooseSelect(page, modal, 'Therapist', therapist.name);
}

export async function openScheduling() {
  await journey.page.goto('/admin/scheduling');
}

export async function openNewSession() {
  const { page } = journey;
  await openScheduling();
  await page.getByRole('button', { name: 'New Session' }).click();
  await expect(page.getByRole('heading', { name: 'Schedule New Session' }), 'the scheduling form opens')
    .toBeVisible({ timeout: 30000 });
  return sessionModal(page);
}

/**
 * Fills the scheduling form for the run's own client, therapist and room, on the
 * next working day the therapist actually keeps. Stops before submitting.
 */
export async function fillSessionForm(modal, { room: wantRoom = true } = {}) {
  const { page, entered } = journey;
  const filled = {};

  // Session types are seeded per tenant, so take what this one offers. The service
  // has to be the clinic's own: the seeded Consultation one only has public-site hours.
  filled['Session Type'] = await chooseSelect(page, modal, 'Session Type', undefined);
  filled.Client = await chooseSelect(page, modal, 'Client', client.name);
  filled.Service = await chooseSelect(page, modal, 'Service', service.name);

  filled.Therapist = await pickTherapist(modal);

  await modal.getByRole('button', { name: /In[- ]?Person/i }).click();
  filled['Session Mode'] = 'In Person';

  const day = nextWeekdayDate(session.weekday);
  filled.Date = await pickCalendarDate(page, modal, 'Date', day);

  const slots = slotArea(modal).getByRole('button');
  await expect(
    slots.first(),
    `the therapist's ${session.weekday} hours offer at least one time`,
  ).toBeVisible({ timeout: 45000 });
  filled['Time slot'] = (await slots.first().innerText()).trim();
  await slots.first().click();

  if (wantRoom) {
    filled.Room = await chooseSelect(page, modal, 'Room', room.name);
  }

  Object.assign(entered.session, filled);
  return filled;
}

export async function submitSession(modal) {
  await modal.getByRole('button', { name: 'Schedule Session' }).click();
}

export async function scheduleSession() {
  const { page, entered } = journey;
  const modal = await openNewSession();
  await fillSessionForm(modal);
  // What the booking should set off — notifications, mail — is checked against
  // this moment and this client record.
  journey.state.sessionBookedAt = new Date().toISOString();
  journey.state.clientMrn = (entered.session.Client?.match(/\(([^)]+)\)\s*$/) ?? [])[1] ?? null;
  await submitSession(modal);

  await expect(
    page.getByRole('heading', { name: 'Session scheduled successfully!' }),
    'the app confirms the session was scheduled',
  ).toBeVisible({ timeout: 60000 });
  await page.getByRole('button', { name: 'Done' }).click();
  markBuilt('session');
  note(
    `session booked for ${client.name} with ${therapist.name} on ${entered.session.Date}`
    + ` at ${entered.session['Time slot']} in ${room.name}`,
  );
  return entered.session;
}

/** Finds the booked session in the schedule and opens it for editing. */
export async function openSessionForEdit() {
  const { page } = journey;
  await openScheduling();
  // The calendar views only show the week they open on; the list shows every session.
  await page.getByRole('button', { name: 'All Sessions' }).click();
  const row = page.getByRole('row').filter({ hasText: client.name }).first();
  await expect(row, 'the booked session is listed').toBeVisible({ timeout: 45000 });
  await row.getByRole('button', { name: 'Session actions' }).click();
  await pickMenuItem(page, 'Edit Session Details');
  await expect(page.getByRole('heading', { name: 'Edit Session' }), 'the session opens for editing')
    .toBeVisible({ timeout: 30000 });
  const modal = sessionModal(page, 'Edit Session');
  // The editor fetches the session, then its date, its time and only then the rooms
  // free at that time, so the room filling in is the signal that the record landed.
  await expect
    .poll(async () => readSelect(modal, 'Room'), {
      timeout: 90000,
      message: 'the saved session loads into the form',
    })
    .not.toBe('');
  return modal;
}

/** Reads back the values a saved session shows in its editor. */
export async function readSession(modal) {
  return {
    'Session Type': await readSelect(modal, 'Session Type'),
    Client: await readSelect(modal, 'Client'),
    Service: await readSelect(modal, 'Service'),
    Therapist: await readSelect(modal, 'Therapist'),
    Room: await readSelect(modal, 'Room'),
  };
}
