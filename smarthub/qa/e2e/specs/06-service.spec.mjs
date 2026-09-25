import { readText } from '../lib/forms.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { service } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';
import {
  createService,
  fillServiceForm,
  openServicesSettings,
  submitServiceForm,
} from '../flows/services.mjs';

/**
 * The clinic's own service. Everything a tenant starts with is the Consultation
 * service, which only carries the public site's hours, so nothing can be booked
 * with a therapist until the clinic adds a service of its own.
 */
test.describe('service', { tag: '@service' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('administrator');
  });

  test('the administrator adds the clinic\'s own service', async () => {
    test.skip(built('service'), 'resuming: the earlier run already created this service');
    await createService();
  });

  test('a duplicate service code is refused', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openServicesSettings();
    await fillServiceForm({ ...service, name: `${service.name} duplicate` });
    await submitServiceForm();
    await expect(
      page.getByText(/already|exists|duplicate|unique|in use/i).first(),
      'the duplicate service code is refused',
    ).toBeVisible({ timeout: 30000 });
    note('duplicate service code refused');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('reopening the service shows every value that was saved', { tag: '@verify' }, async () => {
    const { page } = journey;
    await openServicesSettings();
    const row = page.getByRole('row').filter({ hasText: service.code }).first();
    await expect(row, 'the service is listed').toBeVisible({ timeout: 30000 });
    // The row's edit and delete controls are icon-only, with no accessible name.
    await row.getByRole('button').nth(-2).click();
    await expect(page.getByRole('heading', { name: 'Edit Service Code' }), 'the service opens for editing')
      .toBeVisible({ timeout: 30000 });

    // These labels carry a hint ("Service Code * e.g. PSY-60"), so read them tolerantly.
    await expect.poll(async () => readText(page, 'Service Code'), { timeout: 20000 }).toBe(service.code);
    expect(await readText(page, 'Service Name'), 'the service name came back').toBe(service.name);
    expect(await readText(page, 'Description'), 'the description came back').toBe(service.description);
    expect(await readText(page, 'Session Duration (minutes)'), 'the duration came back')
      .toBe(service.duration);
    expect(Number(await readText(page, 'Base Rate (USD)')), 'the base rate came back')
      .toBe(Number(service.rate));
    note('service code, name, description, duration and base rate verified after reopening');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });
});
