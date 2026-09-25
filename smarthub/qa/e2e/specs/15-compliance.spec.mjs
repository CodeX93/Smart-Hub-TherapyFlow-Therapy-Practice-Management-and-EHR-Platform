import { expect, journey, note, test } from '../lib/journey.mjs';
import { client } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';

/**
 * The compliance corner: the HIPAA audit trail must carry the journey's own PHI
 * actions, record even its own reading, and export as a report — and the consent
 * management page must list the client the journey created.
 */
test.describe('compliance', { tag: '@compliance' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('client');
  });

  async function openHipaa() {
    const { page } = journey;
    await page.goto('/admin/compliance/hipaa', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Total Activities', { exact: true }).first(), 'the audit trail opens')
      .toBeVisible({ timeout: 30000 });
    await expect(page.getByText('IP Address').first(), 'the log table renders')
      .toBeVisible({ timeout: 30000 });
  }

  test('the audit trail carries the journey\'s own actions', { tag: '@verify' }, async () => {
    const { page } = journey;
    await openHipaa();
    // The trail lists newest first behind an infinite scroll; in a full journey the
    // client's creation sits pages down, so the search scrolls until it surfaces.
    const row = page.getByText('Client Created').first();
    await expect(async () => {
      if (!(await row.isVisible().catch(() => false))) {
        await page.getByRole('row').last().scrollIntoViewIfNeeded();
        await page.waitForTimeout(400);
      }
      await expect(row, 'creating the client left a PHI audit row').toBeVisible({ timeout: 1000 });
    }).toPass({ timeout: 90000 });
    note('the "Client Created" action the journey performed is on the audit trail');
  });

  test('the audit log records even its own reading', async () => {
    const { page } = journey;
    await openHipaa();
    // The previous look at this page wrote an "Audit Log View" row of its own; a
    // fresh load must therefore already list one. Watching the watcher is the point.
    await expect(
      page.getByText('Audit Log View').first(),
      'reading the audit log is itself audited',
    ).toBeVisible({ timeout: 30000 });
    note('the audit trail records its own readers — an Audit Log View row is present');
  });

  test('the audit report exports as a dated file', async () => {
    const { page } = journey;
    await openHipaa();
    const downloading = page.waitForEvent('download', { timeout: 45000 });
    await page.getByRole('button', { name: 'Export Report' }).click();
    const download = await downloading;
    expect(
      download.suggestedFilename(),
      'the report file carries its date',
    ).toMatch(/^hipaa-audit-report-\d{4}-\d{2}-\d{2}\.csv$/);
    note(`the audit report downloaded as ${download.suggestedFilename()}`);
  });

  test('consent management lists the journey\'s client', { tag: '@verify' }, async () => {
    const { page } = journey;
    await page.goto('/admin/compliance/privacy', { waitUntil: 'domcontentloaded' });
    await expect(
      page.getByText('Portal Access').first(),
      'the consent table renders',
    ).toBeVisible({ timeout: 30000 });
    await expect(
      page.getByRole('row').filter({ hasText: client.name }).first(),
      'the client has a consent row',
    ).toBeVisible({ timeout: 30000 });
    note(`${client.name} is listed on the consent management page`);
  });
});
