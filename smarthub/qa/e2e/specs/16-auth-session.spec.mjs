import { expect, journey, note, test } from '../lib/journey.mjs';
import { need } from '../flows/prerequisites.mjs';
import { pickMenuItem } from '../lib/ui.mjs';
import { signInAsSuperAdmin } from '../flows/organisation.mjs';
import { openTherapistSession } from '../flows/tasks.mjs';
import { browserContextOptions } from '../lib/config.mjs';
import { client, stamp } from '../lib/fixtures.mjs';
import { openClientPreview } from '../flows/clients.mjs';

const portalPassword = `Qa!${stamp}Portal1`;

/**
 * The refresh token's move into an HttpOnly cookie: no token may be readable from
 * the page any more, a reload must restore the session from the cookie alone, two
 * tabs must survive reloading together, and logout must end what the cookie holds.
 */

/** Every localStorage entry that smells like a token, outside the device-trust keys. */
async function tokenLeaks(page) {
  return page.evaluate(() => {
    const leaks = [];
    for (let i = 0; i < localStorage.length; i += 1) {
      const key = localStorage.key(i);
      if (key.startsWith('tf.deviceTrust.')) continue;
      const value = localStorage.getItem(key) ?? '';
      if (/accessToken|refreshToken/.test(value) || /eyJ[\w-]{10,}\./.test(value)) {
        leaks.push(key);
      }
    }
    return leaks;
  });
}

async function logOut(page, landing) {
  await page.getByRole('button', { name: /^[A-Z]{2}$/ }).last().click();
  // The staff menu says "Logout", the client portal's "Sign out".
  await pickMenuItem(page, /^(Logout|Sign out)$/);
  await page.waitForURL(landing, { timeout: 30000 });
  // Let the login page settle first: a late client-side redirect right after
  // logout can abort a reload started too early.
  await expect(page.getByRole('button', { name: 'Sign In' }), 'the login page rendered')
    .toBeVisible({ timeout: 30000 });
  await page.reload({ waitUntil: 'domcontentloaded' }).catch(async () => {
    await page.waitForTimeout(1000);
    await page.reload({ waitUntil: 'domcontentloaded' });
  });
  await expect(
    page.getByRole('button', { name: 'Sign In' }),
    'a reload after logout stays signed out',
  ).toBeVisible({ timeout: 30000 });
}

async function refreshCookie(context) {
  const cookies = await context.cookies();
  return cookies.find((c) => c.name === 'tf_refresh') ?? null;
}

test.describe('auth session cookie', { tag: '@authcookie' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('client');
  });

  test('the admin keeps no token in storage, only the HttpOnly cookie', { tag: '@verify' }, async () => {
    const { page, context } = journey;
    await page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Active Clients', { exact: true }).first()).toBeVisible({ timeout: 30000 });

    expect(await tokenLeaks(page), 'no token-shaped value survives in localStorage').toEqual([]);
    const cookie = await refreshCookie(context);
    expect(cookie, 'the tf_refresh cookie is set').toBeTruthy();
    expect(cookie.httpOnly, 'and it is HttpOnly').toBe(true);
    expect(cookie.path, 'scoped to the auth endpoints').toBe('/api/v1/auth');
    note('admin session: storage is clean, tf_refresh is HttpOnly on /api/v1/auth');
  });

  test('a reload keeps the administrator where they were', async () => {
    const { page } = journey;
    await page.goto('/admin/user-access/profiles', { waitUntil: 'domcontentloaded' });
    await expect(page.getByPlaceholder(/Search users by name/)).toBeVisible({ timeout: 30000 });
    await page.reload({ waitUntil: 'domcontentloaded' });
    await expect(
      page.getByPlaceholder(/Search users by name/),
      'the protected page survives a reload without bouncing to login',
    ).toBeVisible({ timeout: 30000 });
    expect(page.url(), 'and the URL never fell back to the login page').toContain('/admin/user-access/profiles');
    note('a full reload restored the admin session from the cookie');
  });

  test('two tabs reloaded together both stay signed in', async () => {
    const { page, context } = journey;
    const second = await context.newPage();
    try {
      await page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });
      await second.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });
      await expect(second.getByText('Active Clients', { exact: true }).first()).toBeVisible({ timeout: 30000 });
      await Promise.all([
        page.reload({ waitUntil: 'domcontentloaded' }),
        second.reload({ waitUntil: 'domcontentloaded' }),
      ]);
      await expect(
        page.getByText('Active Clients', { exact: true }).first(),
        'the first tab kept its session',
      ).toBeVisible({ timeout: 30000 });
      await expect(
        second.getByText('Active Clients', { exact: true }).first(),
        'and so did the second, refreshing at the same moment',
      ).toBeVisible({ timeout: 30000 });
      note('two tabs reloaded simultaneously and neither was logged out');
    } finally {
      await second.close();
    }
  });

  test('the client portal rides its own cookie, and logout ends it', async () => {
    test.setTimeout(420000);
    const { page } = journey;

    // Creating the client already sent an activation email, and the resend below
    // replaces its token. On slow mail the old message can be the only one there
    // when polling starts, so consume it first — only newer mail can answer then.
    await journey.mailbox.waitForMessage({
      to: client.email,
      subjectIncludes: 'Activate Your SmartHub Portal Account',
      since: new Date(Date.now() - 30 * 60 * 1000),
      timeoutMs: 20000,
    }).catch(() => null);

    // The administrator switches the portal on; the invitation lands in the mail.
    const invitedAt = new Date();
    await openClientPreview();
    // The portal section starts collapsed; its controls only exist once opened.
    await page.getByText('Portal Access Management', { exact: true }).first().click();
    // A client created with an email is portal-enabled from birth, so the profile
    // offers a resend; a client without gets the enable button instead.
    const enable = page.getByRole('button', { name: 'Enable Portal Access' });
    const resend = page.getByRole('button', { name: 'Resend Activation Email' });
    await expect(enable.or(resend).first(), 'the portal section offers an invitation')
      .toBeVisible({ timeout: 30000 });
    if (await enable.isVisible().catch(() => false)) {
      await enable.click();
    } else {
      await resend.click();
    }
    await expect(resend, 'the portal is switched on').toBeVisible({ timeout: 30000 });

    const invitation = await journey.mailbox.waitForMessage({
      to: client.email,
      subjectIncludes: 'Activate Your SmartHub Portal Account',
      since: invitedAt,
    });
    const token = invitation.body.match(/\/portal\/activate\/([\w.-]+)/)?.[1];
    expect(token, 'the invitation carries an activation link').toBeTruthy();

    // The client activates in a browser of their own and lands signed in.
    const context = await journey.browser.newContext(browserContextOptions);
    const portalPage = await context.newPage();
    try {
      await portalPage.goto(`/portal/activate/${token}`, { waitUntil: 'domcontentloaded' });
      // The portal's activation form ends in "Activate account", not "Set password".
      await portalPage.getByLabel(/^New Password/).first().fill(portalPassword);
      await portalPage.getByLabel(/^Confirm New Password/).first().fill(portalPassword);
      await portalPage.getByRole('button', { name: /^(Activate account|Set password)$/ }).click();
      const landed = await portalPage
        .waitForURL(/\/user\//, { timeout: 30000 })
        .then(() => true)
        .catch(() => false);
      if (!landed) {
        // The portal login form labels its fields Email and Password — not the
        // staff form's "Email or username" that submitLogin expects.
        await portalPage.goto('/auth/login', { waitUntil: 'domcontentloaded' });
        await portalPage.getByLabel(/^Email/).first().fill(client.email, { timeout: 30000 });
        await portalPage.getByLabel(/^Password/).first().fill(portalPassword, { timeout: 30000 });
        await portalPage.getByRole('button', { name: 'Sign In' }).click();
        await portalPage.waitForURL(/\/user\//, { timeout: 30000 });
      }
      note('the client activated the portal and is signed in');

      expect(await tokenLeaks(portalPage), 'no token-shaped value in the portal storage').toEqual([]);
      const cookies = await context.cookies();
      const portalCookie = cookies.find((c) => c.name === 'tf_portal_refresh');
      expect(portalCookie, 'the tf_portal_refresh cookie is set').toBeTruthy();
      expect(portalCookie.httpOnly, 'and it is HttpOnly').toBe(true);
      expect(portalCookie.path, 'scoped to the portal endpoints').toBe('/api/v1/portal');

      await portalPage.reload({ waitUntil: 'domcontentloaded' });
      await expect(
        portalPage.getByText(/Appointments|Booked Sessions|Invoices/i).first(),
        'the portal survives a reload',
      ).toBeVisible({ timeout: 30000 });
      expect(portalPage.url()).toContain('/user/');

      await logOut(portalPage, /\/auth\/login/);
      note('portal: storage clean, tf_portal_refresh HttpOnly on /api/v1/portal, reload holds, logout ends it');
    } finally {
      await context.close();
    }
  });

  test('the therapist rides the same cookie, and logout ends it', async () => {
    test.setTimeout(420000);
    const { context, page: staffPage } = await openTherapistSession();
    try {
      expect(await tokenLeaks(staffPage), 'no token-shaped value in the staff storage').toEqual([]);
      const cookie = await refreshCookie(context);
      expect(cookie, 'the staff session holds its tf_refresh cookie').toBeTruthy();
      expect(cookie.httpOnly).toBe(true);

      await staffPage.goto('/therapist/dashboard', { waitUntil: 'domcontentloaded' });
      await staffPage.reload({ waitUntil: 'domcontentloaded' });
      await expect(
        staffPage.getByText(/Dashboard|Today/i).first(),
        'the therapist survives a reload',
      ).toBeVisible({ timeout: 30000 });
      expect(staffPage.url()).toContain('/therapist/');

      await logOut(staffPage, /\/auth\/staff\/login/);
      note('the refresh cookie was revoked by logout — a reload cannot resurrect the session');
    } finally {
      await context.close();
    }
  });

  test('the administrator logs out, and a reload stays out', async () => {
    const { page } = journey;
    await page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Active Clients', { exact: true }).first()).toBeVisible({ timeout: 30000 });
    await logOut(page, /\/auth\/staff\/login/);
    note('admin logout revoked the cookie; a reload stays on the staff login');
  });

  test('the super admin signs in fresh onto the cookie too', async () => {
    test.setTimeout(420000);
    const { page, context } = journey;
    await signInAsSuperAdmin();
    expect(await tokenLeaks(page), 'no token-shaped value in the super admin storage').toEqual([]);
    const cookie = await refreshCookie(context);
    expect(cookie, 'the super admin session holds tf_refresh').toBeTruthy();
    await page.reload({ waitUntil: 'domcontentloaded' });
    await expect(
      page.getByRole('button', { name: /New Organization|Add Organization/ }).or(
        page.getByText('Organisations', { exact: true }),
      ).first(),
      'the super admin survives a reload',
    ).toBeVisible({ timeout: 30000 });
    note('super admin: storage clean, cookie present, reload restores the session');

    await logOut(page, /\/super-admin\/login/);
    note('super admin logout revoked the cookie; a reload stays on their login');
  });
});
