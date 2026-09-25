import { expect, journey, note, test } from '../lib/journey.mjs';
import { client, org, therapist } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';

/**
 * Booking a session is not finished when the row is saved: the people involved have
 * to hear about it. The backend fans session_scheduled out to the administrators,
 * the assigned therapist and the client — each gets an email, and staff also get an
 * in-app notification behind the bell. Both arrivals are checked against the moment
 * the booking was made, so an older notification can never stand in for this one.
 */
test.describe('session notifications', { tag: '@notifications' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(900000);
    await need('session');
  });

  test('the booking shows up behind the bell for the administrator', { tag: '@verify' }, async () => {
    const { page, state } = journey;
    expect(state.clientMrn, 'the booking recorded which client record it was for').toBeTruthy();

    // The dashboard keeps loading widgets long after it is usable; the bell's own
    // visibility wait below is the readiness signal that matters.
    await page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });
    const bell = page.locator('button:has(svg.lucide-bell)').filter({ visible: true }).first();
    await expect(bell, 'the header offers the notification bell').toBeVisible({ timeout: 30000 });

    // Delivery runs after the booking commits, so give it a few reopenings to arrive.
    const entry = page.getByText(`Session Scheduled`, { exact: true }).first();
    await expect(async () => {
      await bell.click();
      await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible({ timeout: 5000 });
      await expect(entry).toBeVisible({ timeout: 3000 });
    }, 'a Session Scheduled notification arrives for the administrator').toPass({ timeout: 90000 });

    // The notification names the client by record number, never by name.
    await expect(
      page.getByText(state.clientMrn).first(),
      'the notification carries the client record it is about',
    ).toBeVisible();
    const panelText = await page.getByRole('heading', { name: 'Notifications' })
      .locator('xpath=ancestor::div[2]').innerText();
    expect(panelText, 'the notification does not leak the client name').not.toContain(client.name);
    note(`the administrator's bell shows "Session Scheduled" for ${state.clientMrn}, with no client name leaked`);
    await page.keyboard.press('Escape');
  });

  test('the booking emails the administrator, the therapist and the client', { tag: '@verify' }, async () => {
    const { mailbox, state } = journey;
    const since = new Date(state.sessionBookedAt);
    for (const [who, address] of [
      ['administrator', org.adminEmail],
      ['therapist', therapist.email],
      ['client', client.email],
    ]) {
      const message = await mailbox.waitForMessage({
        to: address,
        subjectIncludes: 'Session Scheduled',
        since,
      });
      expect(message.subject, `the ${who}'s subject names the client record`)
        .toContain(state.clientMrn);
      note(`the ${who} (${address}) received "${message.subject}"`);
    }
  });
});
