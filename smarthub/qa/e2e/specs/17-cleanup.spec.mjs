import { config } from '../lib/config.mjs';
import { pickMenuItem, toastText } from '../lib/ui.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { org, runTag } from '../lib/fixtures.mjs';
import { openOrganisationDetails, signInAsSuperAdmin } from '../flows/organisation.mjs';

/** The tenant this run built is taken away again, once every step has passed. */
test.describe('cleanup', { tag: '@cleanup' }, () => {
  test.describe.configure({ mode: 'serial' });

  test('the organisation is removed once every step has passed', async () => {
    test.skip(!config.cleanup, 'E2E_CLEANUP=0 keeps the organisation for inspection.');
    test.skip(!built('organisation'), 'this run built no organisation to remove.');
    expect(journey.state.everyStepPassed, 'every earlier step passed, so the tenant can be removed').toBe(true);
    // Signing back in may wait on a second factor a person has to hand over.
    test.setTimeout(900000);

    const { page } = journey;
    await signInAsSuperAdmin();
    const rowActions = await openOrganisationDetails();

    await rowActions.click();
    await pickMenuItem(page, 'Terminate Tenant');
    // The confirmation is a plain overlay, not a dialog role, so key off its own controls.
    const reason = page.getByPlaceholder('Enter a reason');
    await expect(reason, 'the termination confirmation opens').toBeVisible();
    await page.getByRole('button', { name: 'Cancel', exact: true }).click();
    await expect(reason, 'cancelling closes it again').toBeHidden();
    await expect(page.getByText(org.name).first(), 'the tenant survives a cancelled termination')
      .toBeVisible();
    note('cancelled termination left the tenant in place');

    await rowActions.click();
    await pickMenuItem(page, 'Terminate Tenant');
    await expect(reason).toBeVisible();
    await reason.fill(`Automated end-to-end run ${runTag}`);

    // The API only takes 30-365, and the form now states that range and refuses the rest itself.
    const terminate = page.getByRole('button', { name: 'Terminate', exact: true });
    const retention = page.getByPlaceholder('30').first();
    await expect(
      page.getByText(/Retention Days \(30-365\)/i).first(),
      'the form states the allowed retention range',
    ).toBeVisible();

    await retention.fill('0');
    await expect(
      page.getByText(/Retention days must be between 30 and 365/i).first(),
      'a retention period outside the allowed range is refused in the form',
    ).toBeVisible({ timeout: 10000 });
    await expect(terminate, 'Terminate stays disabled while retention is out of range').toBeDisabled();
    note('retention days outside 30-365 refused in the form, before any request is sent');

    await retention.fill('30');
    await expect(terminate, 'Terminate re-enables once retention is in range').toBeEnabled();
    await terminate.click();

    await toastText(page, /Termination scheduled/i, 60000);
    await expect(
      page.getByText(/Termination Scheduled|Terminated/i).first(),
      'the tenant is marked for termination',
    ).toBeVisible({ timeout: 30000 });
    note(`termination scheduled for "${org.name}"`);
    note('Scheduled termination is not a hard purge: the platform purge job fails for organisations that already have audit rows, so confirm the tenant schema separately.');
  });
});
