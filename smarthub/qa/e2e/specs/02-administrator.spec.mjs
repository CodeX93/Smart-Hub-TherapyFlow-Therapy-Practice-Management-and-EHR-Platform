import { config } from '../lib/config.mjs';
import { clearSession, submitLogin } from '../lib/ui.mjs';
import { expect, journey, note, test } from '../lib/journey.mjs';
import { org } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';
import { readTemporaryPassword, signInAsAdministrator } from '../flows/administrator.mjs';

/**
 * The tenant's own administrator: the credentials arrive by email, the temporary
 * password is replaced on first use, and a second factor stands between them and
 * the clinic.
 */
test.describe('administrator', { tag: '@administrator' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('organisation');
  });

  test('the new administrator receives credentials, and a wrong password is refused', { tag: '@negative' }, async () => {
    const { page, context } = journey;
    await readTemporaryPassword();

    await clearSession(page);
    await context.clearCookies();
    await submitLogin(page, {
      path: '/auth/staff/login',
      username: org.adminEmail,
      password: `${journey.state.adminPassword}-wrong`,
    });
    await expect(
      page.getByText(/invalid|incorrect|failed|credentials|not match/i).first(),
      'the wrong password is refused',
    ).toBeVisible({ timeout: 40000 });
    note('wrong administrator password refused');
  });

  test('the administrator enrolls a second factor and signs in with an emailed code', async () => {
    const result = await signInAsAdministrator();
    if (result.activated) note('the temporary password was replaced on first sign-in');
    note(`administrator signed in with a code emailed to ${org.adminEmail}`);
    if (result.sms?.enrolled) note(`SMS second factor enrolled on ${config.adminPhone}`);
    else note(`SMS second factor not enrolled: ${result.sms?.reason ?? 'no phone number configured'}`);
  });
});
