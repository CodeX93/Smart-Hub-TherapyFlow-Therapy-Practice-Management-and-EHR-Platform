import { expect } from '@playwright/test';
import { fillText } from '../lib/forms.mjs';
import { toastText } from '../lib/ui.mjs';
import { journey, note } from '../lib/journey.mjs';

/**
 * The System Options screen under Settings: the seeded lookup lists every dropdown
 * in the product feeds from, and the controls an administrator has over them —
 * add, rename, deactivate, delete. What changes here has to follow through to the
 * dropdowns that consume it, which is what these helpers let the specs prove.
 */

export async function openSystemOptions() {
  const { page } = journey;
  await page.goto('/admin/settings?tab=system-options');
  await expect(page.getByRole('button', { name: 'Add Category' }).first(), 'the option catalogue opens')
    .toBeVisible({ timeout: 30000 });
}

let currentCategory = null;

/** Opens one category from the sidebar, by its display name. */
export async function openCategory(name) {
  currentCategory = name;
  const { page } = journey;
  const entry = page.getByText(name, { exact: true }).first();
  await expect(entry, `the catalogue lists ${name}`).toBeVisible({ timeout: 30000 });
  await entry.click();
  await expect(page.getByRole('button', { name: 'Add Option' }), `${name} opens`)
    .toBeVisible({ timeout: 30000 });
  // The options arrive after the category opens; every row prints its key.
  await expect(page.getByText(/^Key: /).first(), `${name}'s options load`)
    .toBeVisible({ timeout: 30000 });
}

/** The row an option occupies in the open category, found by its label. */
export function optionRow(label) {
  return journey.page
    .getByRole('heading', { name: label, exact: true })
    .first()
    .locator('xpath=ancestor::div[4]');
}

/**
 * The row, re-opening the category first when a background refetch has collapsed
 * the panel — the screen re-syncs its categories and drops the selection meanwhile.
 */
async function settledRow(label) {
  const { page } = journey;
  const row = optionRow(label);
  await expect(async () => {
    if (!(await page.getByRole('button', { name: 'Add Option' }).isVisible().catch(() => false))) {
      await openCategory(currentCategory);
    }
    await expect(row).toBeVisible({ timeout: 5000 });
  }, `the option "${label}" is listed`).toPass({ timeout: 45000 });
  return row;
}

async function fillOptionModal({ key, label, active }) {
  const { page } = journey;
  if (key !== undefined) await fillText(page, 'Option Key', key);
  if (label !== undefined) await fillText(page, 'Option Label', label);
  if (active !== undefined) {
    // The modal's three switches carry no accessible names; they come in a fixed
    // order — Default Selection, System Option, Active — and are the only switches
    // on the screen while the modal is open.
    const toggle = page.getByRole('switch').nth(2);
    await expect(toggle, 'the Active switch is in the modal').toBeVisible();
    if ((await toggle.getAttribute('aria-checked')) !== String(active)) {
      await toggle.click();
    }
    await expect(toggle, `the Active switch reads ${active}`)
      .toHaveAttribute('aria-checked', String(active));
  }
}

/** Clears a leftover option from an earlier half-finished run, so runs repeat cleanly. */
export async function removeIfPresent(label) {
  const { page } = journey;
  if (await page.getByRole('heading', { name: label, exact: true }).count()) {
    await deleteOption(label);
    note(`leftover option "${label}" from an earlier run removed first`);
  }
}

/** Present when a half-finished earlier run may have left it in either name. */
export { settledRow };

export async function addOption({ key, label }) {
  const { page } = journey;
  await page.getByRole('button', { name: 'Add Option' }).click();
  await expect(page.getByRole('heading', { name: 'Add New Option' })).toBeVisible();
  await fillOptionModal({ key, label });
  await page.getByRole('button', { name: 'Add Option', exact: true }).last().click();
  await toastText(page, /Option created successfully/i, 30000);
  await expect(optionRow(label), 'the new option is listed').toBeVisible({ timeout: 30000 });
  note(`option "${label}" (${key}) added`);
}

/** Opens an option's editor; the row's controls only paint on hover. */
export async function openOptionEditor(label) {
  const { page } = journey;
  const row = await settledRow(label);
  await row.hover();
  // The drag handle also announces itself as a button, so edit is the second one.
  await row.getByRole('button').nth(1).click();
  await expect(page.getByRole('heading', { name: 'Edit Option' })).toBeVisible({ timeout: 20000 });
}

export async function editOption(label, changes) {
  const { page } = journey;
  await openOptionEditor(label);
  await fillOptionModal(changes);
  await page.getByRole('button', { name: 'Update Option' }).click();
  await toastText(page, /Option updated successfully/i, 30000);
}

export async function deleteOption(label) {
  const { page } = journey;
  const row = await settledRow(label);
  await row.hover();
  await row.getByRole('button').last().click();
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await toastText(page, /Option deleted successfully/i, 30000);
  await expect(page.getByRole('heading', { name: label, exact: true }), 'the option is gone')
    .toBeHidden({ timeout: 30000 });
  note(`option "${label}" deleted`);
}

/** Whether a CustomSelect currently offers an entry — opened, read, closed again. */
export async function selectOffers(scope, label, text) {
  const { page } = journey;
  const trigger = scope.getByRole('button', { name: new RegExp(`^${label}\\s*\\*?$`) }).first();
  await expect(trigger, `the "${label}" selector is on the page`).toBeVisible({ timeout: 30000 });
  await trigger.click();
  const menu = page.locator('[data-radix-popper-content-wrapper]').last();
  await expect(menu, `the "${label}" menu opens`).toBeVisible();
  const offered = (await menu.getByText(text, { exact: true }).count()) > 0;
  await page.keyboard.press('Escape');
  await expect(menu).toBeHidden();
  return offered;
}

async function bearerToken() {
  const { page } = journey;
  const stored = await page.evaluate(() => {
    try { return JSON.parse(localStorage.getItem('auth_session') ?? '{}').accessToken ?? null; }
    catch { return null; }
  });
  if (stored) return stored;
  // Sessions that keep the token in memory only never write it to storage; the
  // app's own next request carries it, so a reload is provoked and the header read.
  const carried = page.waitForRequest(
    (request) => (request.headers().authorization ?? '').startsWith('Bearer '),
    { timeout: 30000 },
  );
  await page.reload({ waitUntil: 'domcontentloaded' });
  const request = await carried;
  return request.headers().authorization.slice('Bearer '.length);
}

/**
 * Calls the backend as the signed-in user, with the token the app itself holds.
 * Production rotates that token, so a stale one earns a 401 — a reload lets the
 * app refresh it, and the call is made once more with the fresh token.
 */
export async function apiGet(path) {
  const { page } = journey;
  const base = process.env.E2E_API_BASE_URL || 'http://127.0.0.1:8081';
  for (let attempt = 1; ; attempt++) {
    const token = await bearerToken();
    expect(token, 'the signed-in session holds an access token').toBeTruthy();
    const response = await page.request.get(`${base}${path}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.status() === 401 && attempt === 1) {
      note(`${path} answered 401 — reloading so the app refreshes its token`);
      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);
      continue;
    }
    expect(response.ok(), `${path} responds (${response.status()})`).toBe(true);
    return response.json();
  }
}
