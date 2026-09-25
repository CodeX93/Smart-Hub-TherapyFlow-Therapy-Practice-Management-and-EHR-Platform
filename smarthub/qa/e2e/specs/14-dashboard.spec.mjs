import { expect, journey, note, test } from '../lib/journey.mjs';
import { client } from '../lib/fixtures.mjs';
import { chooseSelect } from '../lib/forms.mjs';
import { need } from '../flows/prerequisites.mjs';
import { apiGet } from '../flows/systemOptions.mjs';

/**
 * The admin dashboard: every stat card must carry the same number the summary API
 * serves — the card is a display, not a source — and the sessions widget must show
 * the session the journey booked.
 */
test.describe('dashboard', { tag: '@dashboard' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('session');
  });

  async function openDashboard() {
    const { page } = journey;
    await page.goto('/admin/dashboard');
    await expect(page.getByText('Active Clients', { exact: true }).first(), 'the dashboard opens')
      .toBeVisible({ timeout: 30000 });
  }

  /** The card around a stat title — the nearest ancestor that carries its number. */
  function statCard(title) {
    return journey.page
      .getByText(title, { exact: true })
      .first()
      .locator('xpath=ancestor::div[position() <= 3][last()]');
  }

  test('every stat card carries the number the summary API serves', { tag: '@verify' }, async () => {
    await openDashboard();
    const summary = await apiGet('/api/v1/admin/dashboard/summary');

    await expect(statCard('Active Clients'), 'the client card shows the active count')
      .toContainText(String(summary.client.active), { timeout: 30000 });
    await expect(statCard('Active Clients'), 'and the total behind it')
      .toContainText(`of ${summary.client.total} total`);
    await expect(statCard("Today's Sessions"), 'the session card shows today')
      .toContainText(String(summary.session.scheduledToday));
    await expect(statCard('Pending Tasks'), 'the task card shows the pending count')
      .toContainText(String(summary.task.pending));
    await expect(statCard('Pending Tasks'), 'and the total behind it')
      .toContainText(`of ${summary.task.total} total`);
    note(
      `cards agree with the API: ${summary.client.active}/${summary.client.total} clients, `
      + `${summary.session.scheduledToday} today, ${summary.task.pending}/${summary.task.total} tasks`,
    );
  });

  test('the sessions widget shows the booked session', { tag: '@verify' }, async () => {
    const { page } = journey;
    await openDashboard();
    // The booked session sits on a coming Monday, which can fall past the month's
    // end, so the period is widened rather than trusting the default.
    await chooseSelect(page, page, 'Period', 'All Time');
    await page.getByRole('button', { name: /^Upcoming/ }).click();
    await expect(
      page.getByText(client.name).first(),
      "the journey's session is under Upcoming",
    ).toBeVisible({ timeout: 30000 });
    note(`the upcoming sessions widget lists ${client.name}'s booking`);
  });
});
