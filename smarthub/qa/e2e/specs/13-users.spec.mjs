import { built, expect, journey, markBuilt, note, test } from '../lib/journey.mjs';
import { extraUser, org, role, therapist } from '../lib/fixtures.mjs';
import { fillText } from '../lib/forms.mjs';
import { chooseRowAction, toastText } from '../lib/ui.mjs';
import { need } from '../flows/prerequisites.mjs';
import { addUser, createRole, openUserProfiles, userRow } from '../flows/users.mjs';

const adminRow = () => userRow(org.adminEmail);
const adminFullName = `${org.adminFirstName} ${org.adminLastName}`;

/**
 * The user directory and the role catalogue: everyone the journey created is
 * listed with their role, a new account can be added, renamed, deactivated,
 * revived and deleted — but never one's own — and a custom role lives a whole
 * life of its own.
 */
test.describe('user access', { tag: '@users' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('therapist');
  });

  test('the directory lists everyone the journey created', { tag: '@verify' }, async () => {
    await openUserProfiles();
    const admin = adminRow();
    await expect(admin, 'the administrator is in the directory').toBeVisible({ timeout: 30000 });
    await expect(
      admin.getByText(adminFullName),
      'under the first/last name the onboarding form collected',
    ).toBeVisible();
    await expect(admin.getByText('Admin', { exact: true }), 'with the admin role').toBeVisible();
    await expect(admin.getByText('Active', { exact: true }), 'and is active').toBeVisible();
    const ther = userRow(therapist.name);
    await expect(ther, 'the therapist is in the directory').toBeVisible();
    await expect(ther.getByText('Therapist', { exact: true }), 'with the therapist role').toBeVisible();
    note('the administrator and the therapist are both listed, active, with their roles');
  });

  test('an incomplete user is refused', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openUserProfiles();
    await page.getByRole('button', { name: 'Add New User' }).click();
    await expect(page.getByRole('heading', { name: 'Add New User' })).toBeVisible();
    const submit = page.getByRole('button', { name: 'Add User' });
    if (await submit.isEnabled()) {
      await submit.click();
      await expect(
        page.getByText(/required|invalid|enter/i).first(),
        'the empty form is named and refused',
      ).toBeVisible({ timeout: 20000 });
    }
    note('a user without their details cannot be added');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('a supervisor account is added to the directory', async () => {
    test.skip(built('extraUser'), 'resuming: the earlier run already added this user');
    await addUser();
    markBuilt('extraUser');
  });

  test('edited basic info follows into the directory', async () => {
    const { page } = journey;
    await openUserProfiles();
    await chooseRowAction(page, userRow(extraUser.name), 'Edit Basic Info');
    await expect(page.getByRole('heading', { name: 'Edit User' }), 'the edit dialog opens')
      .toBeVisible();
    await fillText(page, 'Full Name', extraUser.renamed);
    await page.getByRole('button', { name: 'Save Changes' }).click();
    await toastText(page, /User updated successfully/i, 30000);
    await expect(userRow(extraUser.renamed), 'the directory shows the new name')
      .toBeVisible({ timeout: 30000 });
    note(`the rename to "${extraUser.renamed}" followed into the directory`);
  });

  test('a deactivated user is marked inactive and can be revived', async () => {
    const { page } = journey;
    await openUserProfiles();
    await chooseRowAction(page, userRow(extraUser.renamed), 'Deactivate');
    await expect(
      page.getByRole('heading', { name: /Deactivate User/ }),
      'deactivation asks for a confirmation',
    ).toBeVisible();
    await page.getByRole('button', { name: 'Deactivate', exact: true }).last().click();
    await toastText(page, /User deactivated successfully/i, 30000);
    await expect(
      userRow(extraUser.renamed).getByText('Inactive', { exact: true }),
      'the row reads Inactive',
    ).toBeVisible({ timeout: 30000 });

    await chooseRowAction(page, userRow(extraUser.renamed), 'Activate');
    await toastText(page, /User activated successfully/i, 30000);
    await expect(
      userRow(extraUser.renamed).getByText('Active', { exact: true }),
      'the row reads Active again',
    ).toBeVisible({ timeout: 30000 });
    note('the user was deactivated behind a confirmation and revived without one');
  });

  test('nobody can deactivate or delete themselves', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openUserProfiles();
    const own = adminRow();
    await expect(own.getByRole('switch'), 'the own status switch is locked').toBeDisabled();
    await own.getByRole('button').last().click();
    const menu = page.locator('[data-radix-popper-content-wrapper]').last();
    await expect(menu.getByRole('button', { name: 'Edit Basic Info' }), 'the own menu opens')
      .toBeVisible();
    await expect(menu.getByRole('button', { name: 'Deactivate' }), 'no self-deactivation')
      .toHaveCount(0);
    await expect(menu.getByRole('button', { name: 'Delete' }), 'no self-deletion')
      .toHaveCount(0);
    await page.keyboard.press('Escape');
    note('the administrator cannot deactivate or delete their own account');
  });

  test('a custom role lives its whole life', async () => {
    const { page } = journey;
    await createRole();

    // Rename it, and the catalogue follows.
    await chooseRowAction(page, userRow(role.displayName), 'Edit');
    await expect(page.getByRole('heading', { name: 'Edit Role' }), 'the edit dialog opens')
      .toBeVisible();
    await fillText(page, 'Display Name', role.renamed);
    await page.getByRole('button', { name: 'Update Role' }).click();
    await toastText(page, /Role updated successfully/i, 30000);
    await expect(userRow(role.renamed), 'the catalogue shows the new display name')
      .toBeVisible({ timeout: 30000 });

    // And deleting it removes it for good.
    await chooseRowAction(page, userRow(role.renamed), 'Delete');
    await expect(page.getByRole('heading', { name: 'Delete role?' }), 'deletion asks first')
      .toBeVisible();
    await page.getByRole('button', { name: 'Delete', exact: true }).last().click();
    await toastText(page, /Role deleted successfully/i, 30000);
    await expect(userRow(role.renamed), 'the role is gone').toBeHidden({ timeout: 30000 });
    note(`role "${role.renamed}" was created, renamed and deleted cleanly`);
  });

  test('the added user can be deleted', async () => {
    const { page } = journey;
    await openUserProfiles();
    await chooseRowAction(page, userRow(extraUser.renamed), 'Delete');
    await expect(page.getByRole('heading', { name: /Delete User/ }), 'deletion asks first')
      .toBeVisible();
    await page.getByRole('button', { name: 'Delete User' }).click();
    await toastText(page, /User deleted successfully/i, 30000);
    await expect(userRow(extraUser.renamed), 'the user is gone').toBeHidden({ timeout: 30000 });
    note('the supervisor account was deleted cleanly');
  });
});
