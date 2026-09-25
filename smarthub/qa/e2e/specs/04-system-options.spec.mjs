import { fillText } from '../lib/forms.mjs';
import { expect, journey, note, test } from '../lib/journey.mjs';
import { stamp } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';
import { openAddClient } from '../flows/clients.mjs';
import {
  addOption,
  apiGet,
  deleteOption,
  editOption,
  openCategory,
  openSystemOptions,
  removeIfPresent,
  selectOffers,
  settledRow,
} from '../flows/systemOptions.mjs';

/** Every seeded category a fresh tenant is provisioned with. */
const EXPECTED_CATEGORIES = [
  'client_source', 'client_stage', 'client_status', 'client_type',
  'education_level', 'employment_status', 'gender', 'insurance_providers',
  'insurance_types', 'marital_status', 'preferred_language', 'referral_sources',
  'service_frequency', 'service_type', 'session_mode', 'session_status',
  'session_type', 'task_priorities', 'task_priority', 'task_status',
  'task_titles', 'task_types', 'treatment_modalities',
];

/** The gender seeds, key to label — the mapping itself is part of the contract. */
const GENDER_SEEDS = {
  male: 'Male',
  female: 'Female',
  non_binary: 'Non-binary',
  prefer_not_to_say: 'Prefer not to say',
  transgender: 'Transgender',
  other: 'Other',
};

// Salted per invocation: an interrupted earlier run can leave its option behind,
// so a rerun must never reuse a key.
const salt = `${stamp}${Date.now().toString(36).slice(-4)}`;
const qaOption = {
  key: `qa_gender_${salt}`,
  label: `QA Gender ${salt}`,
  renamed: `QA Gender ${salt} v2`,
};

/** The Gender dropdown on the client form is the consumer these changes must reach. */
async function genderOffers(text) {
  const { page } = journey;
  await openAddClient();
  const offered = await selectOffers(page, 'Gender', text);
  await page.getByRole('button', { name: 'Cancel' }).last().click();
  return offered;
}

/**
 * The option catalogue: the seeds a tenant is provisioned with, the mapping from
 * stored key to shown label, and the administrator's whole lifecycle over an option —
 * added, consumed, renamed, refused as a duplicate, deactivated, revived, deleted —
 * with the client form's own dropdown as the witness at every step.
 */
test.describe('system options', { tag: '@options' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('administrator');
  });

  test('the seeded catalogue is complete, mapped and free of duplicates', { tag: '@verify' }, async () => {
    const categories = await apiGet('/api/v1/system-options/categories');
    const byKey = new Map(categories.map((c) => [c.categoryKey, c]));

    for (const key of EXPECTED_CATEGORIES) {
      expect(byKey.has(key), `the ${key} category is seeded`).toBe(true);
      const active = (byKey.get(key).options ?? []).filter((o) => o.isActive);
      expect(active.length, `${key} offers at least one active option`).toBeGreaterThan(0);
    }

    // Stored keys map to their human labels — the label is shown, the key is saved.
    const gender = Object.fromEntries(
      (byKey.get('gender').options ?? []).map((o) => [o.optionKey, o.optionLabel]),
    );
    for (const [key, label] of Object.entries(GENDER_SEEDS)) {
      expect(gender[key], `gender key "${key}" carries its label`).toBe(label);
    }

    // No category may offer the same label twice: a person cannot tell two apart.
    for (const category of categories) {
      const labels = (category.options ?? []).filter((o) => o.isActive).map((o) => o.optionLabel);
      expect(
        new Set(labels).size,
        `${category.categoryKey} has no duplicate labels (${labels.join(', ')})`,
      ).toBe(labels.length);
    }

    // Anomalies worth an eye, without failing the run over them.
    if (byKey.has('task_priority') && byKey.has('task_priorities')) {
      note('finding: task_priority AND task_priorities both exist as categories — one of them is a duplicate');
    }
    const empty = categories.filter((c) => (c.options ?? []).length === 0).map((c) => c.categoryKey);
    if (empty.length) note(`finding: categories with no options at all: ${empty.join(', ')}`);
    note(`the catalogue serves ${categories.length} categories, all expected seeds present and mapped`);
  });

  test('an added option reaches the dropdown that consumes it', async () => {
    await openSystemOptions();
    await openCategory('Gender Options');
    // An earlier half-finished run may have left the option behind in either name.
    await removeIfPresent(qaOption.renamed);
    await removeIfPresent(qaOption.label);
    await addOption(qaOption);
    expect(await genderOffers(qaOption.label), 'the client form offers the new option').toBe(true);
    note(`"${qaOption.label}" appeared in the client form's Gender dropdown`);
  });

  test('an empty option is refused', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openSystemOptions();
    await openCategory('Gender Options');
    await page.getByRole('button', { name: 'Add Option' }).click();
    await expect(page.getByRole('heading', { name: 'Add New Option' })).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Add Option', exact: true }).last(),
      'an option without a key and label cannot even be submitted',
    ).toBeDisabled();
    note('an empty option cannot be submitted at all');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('a duplicate option key is refused', { tag: '@negative' }, async () => {
    const { page } = journey;
    await page.getByRole('button', { name: 'Add Option' }).click();
    await fillText(page, 'Option Key', qaOption.key);
    await fillText(page, 'Option Label', `${qaOption.label} duplicate`);
    await page.getByRole('button', { name: 'Add Option', exact: true }).last().click();
    await expect(
      page.getByText(/already|exists|duplicate|in use|unique/i).first(),
      'the duplicate key is refused',
    ).toBeVisible({ timeout: 30000 });
    note('a second option with the same key refused');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('a renamed option follows into the dropdown', async () => {
    await openSystemOptions();
    await openCategory('Gender Options');
    await editOption(qaOption.label, { label: qaOption.renamed });
    expect(await genderOffers(qaOption.renamed), 'the dropdown shows the new label').toBe(true);
    expect(await genderOffers(qaOption.label), 'and no longer the old one').toBe(false);
    note(`the rename to "${qaOption.renamed}" followed into the client form`);
  });

  test('a deleted option is gone from the catalogue and the dropdown', async () => {
    const { page } = journey;
    await openSystemOptions();
    await openCategory('Gender Options');
    await deleteOption(qaOption.renamed);
    expect(await genderOffers(qaOption.renamed), 'the dropdown no longer offers it').toBe(false);

    // The seeded system options themselves offer no delete at all.
    await openSystemOptions();
    await openCategory('Gender Options');
    const maleRow = page.getByRole('heading', { name: 'Male', exact: true }).first()
      .locator('xpath=ancestor::div[4]');
    await maleRow.hover();
    // Drag handle + edit — and no third control: a seeded option is never deletable.
    expect(await maleRow.getByRole('button').count(), 'a seeded option can be edited but never deleted')
      .toBe(2);
    note('the QA option is fully gone, and seeded options expose no delete control');
  });

  test('a deactivated option leaves the dropdown and returns when revived', async () => {
    const doomed = { key: `qa_gender_off_${salt}`, label: `QA Gender Off ${salt}` };
    await openSystemOptions();
    await openCategory('Gender Options');
    await addOption(doomed);
    await editOption(doomed.label, { active: false });

    expect(await genderOffers(doomed.label), 'a deactivated option is not offered to new records')
      .toBe(false);

    // The management screen keeps sight of the inactive option — it is the only
    // place one can be revived or deleted from, so it must not vanish there too.
    await openSystemOptions();
    await openCategory('Gender Options');
    const row = await settledRow(doomed.label);
    await expect(row.getByText('Inactive', { exact: true }), 'the row wears its Inactive badge')
      .toBeVisible();
    note('the deactivated option stays listed on the admin screen, marked Inactive');

    await editOption(doomed.label, { active: true });
    expect(await genderOffers(doomed.label), 'the revived option returns to the dropdown')
      .toBe(true);
    note(`"${doomed.label}" was revived from the admin screen and returned to the client form`);

    // Nothing consumed the option, so it deletes cleanly — no stranded rows for reruns.
    await openSystemOptions();
    await openCategory('Gender Options');
    await deleteOption(doomed.label);
  });

  test('the client form still labels its seeded values correctly', { tag: '@verify' }, async () => {
    const { page } = journey;
    await openAddClient();
    for (const label of Object.values(GENDER_SEEDS)) {
      expect(await selectOffers(page, 'Gender', label), `Gender offers "${label}"`).toBe(true);
    }
    await page.getByRole('button', { name: 'Cancel' }).last().click();
    note('all six seeded gender labels are offered on the client form');
  });
});
