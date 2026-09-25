import { expect, journey, note, test } from '../lib/journey.mjs';
import { org } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';

/** One tenant's administrator must see nothing of the platform, or of anyone else. */
test.describe('tenant isolation', { tag: '@isolation' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('administrator');
  });

  test('the organisation administrator gets no platform data from the super-admin area', { tag: '@negative' }, async () => {
    const { page } = journey;
    await page.goto('/super-admin/organisations');

    // The route is role-guarded, so a tenant administrator never reaches the platform
    // console: the guard sends them back to their own dashboard.
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 30000 });
    await expect(page, 'the super-admin area sends a tenant administrator back to their own dashboard')
      .toHaveURL(/\/admin\/dashboard/);

    await expect(
      page.getByRole('link', { name: 'Billings & Plans' }),
      'the platform console does not render for them',
    ).toBeHidden();
    await expect(
      page.getByRole('button', { name: 'New Organization' }),
      'and no platform action is offered',
    ).toBeHidden();

    const rowText = (await page.getByRole('row').allInnerTexts()).join(' | ');
    expect(rowText, 'no other tenant is listed for an organisation administrator')
      .not.toMatch(/Resilience|resiliencecounseling/i);
    expect(rowText, 'not even its own tenant record is served here').not.toContain(org.name);

    note('the super-admin area does not open for a tenant administrator: /super-admin/organisations redirects to /admin/dashboard');
  });
});
