import { expect } from '@playwright/test';
import { chooseSelect, fillText } from '../lib/forms.mjs';
import { chooseRowAction, toastText } from '../lib/ui.mjs';
import { journey, note } from '../lib/journey.mjs';
import { extraUser, role } from '../lib/fixtures.mjs';

/**
 * User & Access under the admin menu: the directory of every account on the tenant,
 * with add, edit, deactivate and delete — and the role catalogue those accounts are
 * assigned from, with its permission checkboxes.
 */

export async function openUserProfiles() {
  const { page } = journey;
  await page.goto('/admin/user-access/profiles', { waitUntil: 'domcontentloaded' });
  await expect(page.getByPlaceholder(/Search users by name/), 'the user directory opens')
    .toBeVisible({ timeout: 30000 });
  await expect(page.getByText('Last Login').first(), 'the table renders')
    .toBeVisible({ timeout: 30000 });
}

export async function openRoles() {
  const { page } = journey;
  await page.goto('/admin/user-access/roles', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Create Role' }), 'the role catalogue opens')
    .toBeVisible({ timeout: 30000 });
}

/** The directory row a user occupies, found by their name. */
export function userRow(name) {
  return journey.page.getByRole('row').filter({ hasText: name }).first();
}

/** Adds a supervisor account through the Add New User dialog. */
export async function addUser() {
  const { page } = journey;
  await openUserProfiles();
  await page.getByRole('button', { name: 'Add New User' }).click();
  await expect(page.getByRole('heading', { name: 'Add New User' }), 'the add dialog opens')
    .toBeVisible();
  await fillText(page, 'Full Name', extraUser.name);
  await fillText(page, 'Email', extraUser.email);
  await fillText(page, 'Username', extraUser.username);
  await fillText(page, 'Phone', extraUser.phone);
  await chooseSelect(page, page, 'Role', 'Supervisor');
  await fillText(page, 'Password', extraUser.password);
  await page.getByRole('button', { name: 'Add User' }).click();
  await toastText(page, /User added successfully/i, 30000);
  await expect(userRow(extraUser.name), 'the new user joins the directory')
    .toBeVisible({ timeout: 30000 });
  note(`supervisor "${extraUser.name}" added to the directory`);
}

/** Creates the run's custom role with the first two permissions on offer. */
export async function createRole() {
  const { page } = journey;
  await openRoles();
  await page.getByRole('button', { name: 'Create Role' }).click();
  await expect(page.getByRole('heading', { name: 'Create New Role' }), 'the role dialog opens')
    .toBeVisible();
  await fillText(page, 'Role Name', role.name);
  await fillText(page, 'Display Name', role.displayName);
  await fillText(page, 'Role description', role.description);
  // The first checkbox is Select all; the two after it are real permissions.
  const boxes = page.getByRole('checkbox');
  await expect(boxes.nth(1), 'the permission list is offered').toBeVisible({ timeout: 20000 });
  await boxes.nth(1).check();
  await boxes.nth(2).check();
  await page.getByRole('button', { name: 'Create Role', exact: true }).last().click();
  await toastText(page, /Role created successfully/i, 30000);
  await expect(userRow(role.displayName), 'the new role is listed').toBeVisible({ timeout: 30000 });
  note(`role "${role.displayName}" created with two permissions`);
}

export { chooseRowAction };
