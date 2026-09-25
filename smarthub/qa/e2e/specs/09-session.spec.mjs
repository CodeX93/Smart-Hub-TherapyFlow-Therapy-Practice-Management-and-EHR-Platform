import { chooseSelect, nextWeekdayDate, pickCalendarDate } from '../lib/forms.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { client, room, service, session, therapist } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';
import {
  openNewSession,
  pickTherapist,
  openSessionForEdit,
  readSession,
  scheduleSession,
} from '../flows/sessions.mjs';

/**
 * Booking a session is where the clinic's own records have to agree: the client,
 * their therapist, that therapist's working hours and a free room. The form is only
 * allowed to offer times the therapist actually keeps.
 */
test.describe('session', { tag: '@session' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(900000);
    await need('service', 'profile', 'client');
  });

  test('a session cannot be scheduled before it has been filled in', { tag: '@negative' }, async () => {
    const modal = await openNewSession();
    await expect(
      modal.getByRole('button', { name: 'Schedule Session' }),
      'the form will not submit an empty session',
    ).toBeDisabled();
    note('an empty session form cannot be submitted');
    await modal.getByRole('button', { name: 'Cancel' }).click();
  });

  test('a day the therapist does not work offers no times', { tag: '@negative' }, async () => {
    const { page } = journey;
    const modal = await openNewSession();
    await chooseSelect(page, modal, 'Session Type', undefined);
    await chooseSelect(page, modal, 'Client', client.name);
    await chooseSelect(page, modal, 'Service', service.name);
    await pickTherapist(modal);
    await modal.getByRole('button', { name: /In[- ]?Person/i }).click();

    // Monday is the one working day the profile was given, so a Wednesday has none.
    await pickCalendarDate(page, modal, 'Date', nextWeekdayDate('Wednesday'));
    await expect(
      modal.getByText(/No available time slots|not available|no slots/i).first(),
      'a day outside the working schedule offers nothing to book',
    ).toBeVisible({ timeout: 45000 });
    await expect(
      modal.getByRole('button', { name: 'Schedule Session' }),
      'and the session cannot be submitted without a time',
    ).toBeDisabled();
    note(`a Wednesday offers no times, because the therapist only works ${session.weekday}`);
    await modal.getByRole('button', { name: 'Cancel' }).click();
  });

  test('the administrator books a session for the client with their therapist', async () => {
    test.skip(built('session'), 'resuming: the earlier run already booked this session');
    test.setTimeout(300000);
    await scheduleSession();
  });

  test('the booked session shows the client, therapist and room it was booked with', { tag: '@verify' }, async () => {
    const { entered } = journey;
    const modal = await openSessionForEdit();
    const saved = await readSession(modal);

    expect(saved.Client, 'the session is still the same client').toContain(client.name);
    expect(saved.Room, 'the room came back unchanged').toContain(room.name);
    expect(saved['Session Type'], 'the session type came back unchanged')
      .toContain(entered.session['Session Type']);
    expect(saved.Service, 'the service came back unchanged').toContain(entered.session.Service);
    expect(saved.Therapist, "the session kept the client's own therapist").toContain(therapist.name);
    note('session client, type, service and room verified after reopening');
    await modal.getByRole('button', { name: 'Cancel' }).click();
  });
});
