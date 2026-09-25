import { expect } from '@playwright/test';

/** Labels render as "Field *" when required, so anchor the match instead of matching loosely. */
export function labelRe(label) {
  return new RegExp(`^${escapeRe(label)}\\s*\\*?$`);
}

/** Some fields append a hint to the label, e.g. "Room Number * e.g. VR-02". */
function labelPrefixRe(label) {
  return new RegExp(`^${escapeRe(label)}\\s*\\*?(?:\\s|$)`);
}

function escapeRe(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Finds a field by its label, preferring an exact match so that "Email" never
 * resolves to "Email Notifications", and falling back to a label that carries a hint.
 */
async function field(scope, label) {
  const exact = scope.getByLabel(labelRe(label));
  const matches = (await exact.count()) ? exact : scope.getByLabel(labelPrefixRe(label));
  // A modal keeps its other sections mounted but hidden, and they repeat these labels.
  return onlyVisible(matches);
}

/** Prefers a visible match, so hidden siblings in unopened sections never win. */
export async function onlyVisible(locator) {
  const shown = locator.filter({ visible: true });
  return (await shown.count()) ? shown.first() : locator.first();
}

function popover(page) {
  return page.locator('[data-radix-popper-content-wrapper]').last();
}

export async function fillText(scope, label, value) {
  const input = await field(scope, label);
  await expect(input, `"${label}" is editable`).toBeVisible();
  await input.fill(String(value));
}

export async function readText(scope, label) {
  const input = await field(scope, label);
  return (await input.inputValue()).trim();
}

/**
 * Picks an option in the shared CustomSelect. With no `option` the first entry is
 * taken, which keeps the run working against tenant-seeded lists whose contents
 * differ per organisation. Returns the label that ended up selected.
 */
export async function chooseSelect(page, scope, label, option) {
  const trigger = scope.getByRole('button', { name: labelRe(label) }).first();
  await expect(trigger, `"${label}" selector is on the page`).toBeVisible();
  await trigger.click();
  const menu = popover(page);
  await expect(menu, `the "${label}" menu opens`).toBeVisible();
  const search = menu.getByRole('textbox').first();
  if (option && (await search.count())) await search.fill(option);
  const items = menu.locator('div.cursor-pointer');
  let target = items.first();
  if (option) {
    const exact = menu.getByText(option, { exact: true });
    target = (await exact.count()) ? exact.first() : menu.getByText(option, { exact: false }).first();
  }
  await expect(target, `"${label}" offers ${option ?? 'at least one option'}`).toBeVisible();
  const chosen = (await target.innerText()).trim().split('\n')[0];
  await target.click();
  await expect(menu).toBeHidden();
  return chosen;
}

/** The selected label, read back off the closed trigger. */
export async function readSelect(scope, label) {
  const trigger = scope.getByRole('button', { name: labelRe(label) }).first();
  const text = (await trigger.innerText()).trim();
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && line !== label && line !== `${label} *`)
    .join(' ')
    .trim();
}

/** Opens a CustomDatePicker and clicks a day in the month it opens on. */
export async function pickDate(page, scope, label, day) {
  const trigger = scope.getByRole('button', { name: new RegExp(label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')) }).first();
  await expect(trigger, `"${label}" date field is on the page`).toBeVisible();
  await trigger.click();
  const calendar = popover(page);
  const cell = calendar.getByRole('button', { name: String(day), exact: true }).first();
  await expect(cell, `day ${day} can be chosen for "${label}"`).toBeEnabled();
  await cell.click();
  await expect(calendar).toBeHidden();
  return readDate(scope, label);
}

export async function readDate(scope, label) {
  const trigger = scope.getByRole('button', { name: new RegExp(label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')) }).first();
  const text = (await trigger.innerText()).trim();
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && line !== label && line !== `${label} *`)
    .join(' ')
    .trim();
}

async function toggleLocator(scope, label) {
  for (const role of ['checkbox', 'switch']) {
    const byRole = scope.getByRole(role, { name: labelRe(label) });
    if (await byRole.count()) return onlyVisible(byRole);
  }
  return field(scope, label);
}

export async function setToggle(scope, label, on) {
  const toggle = await toggleLocator(scope, label);
  await expect(toggle, `"${label}" toggle is on the page`).toBeVisible();
  if ((await toggle.isChecked()) !== on) await toggle.click();
}

export async function readToggle(scope, label) {
  return (await toggleLocator(scope, label)).isChecked();
}

/** CustomStringListInput: type a value, press Enter, repeat. */
export async function addListValues(scope, label, values) {
  // The same word can label a sidebar section and the field itself; the field comes later.
  const block = scope.getByText(label, { exact: true }).last().locator('xpath=..');
  for (const value of values) {
    const input = block.getByRole('textbox').first();
    await expect(input, `"${label}" accepts a new entry`).toBeVisible();
    await input.fill(value);
    await input.press('Enter');
  }
}

export async function readListValues(scope, label, values) {
  const found = [];
  for (const value of values) {
    if (await scope.getByRole('button', { name: `Remove ${value}` }).count()) found.push(value);
  }
  return found;
}

/**
 * Fills a declarative field list and returns what was actually entered, so the
 * same map can be asserted after reopening the record.
 */
export async function fillFields(page, scope, fields) {
  const entered = {};
  for (const field of fields) {
    if (field.kind === 'text' || field.kind === 'money') {
      await fillText(scope, field.label, field.value);
      entered[field.label] = String(field.value);
    } else if (field.kind === 'select') {
      entered[field.label] = await chooseSelect(page, scope, field.label, field.value);
    } else if (field.kind === 'date') {
      entered[field.label] = await pickDate(page, scope, field.label, field.day);
    } else if (field.kind === 'toggle') {
      await setToggle(scope, field.label, field.value);
      entered[field.label] = field.value;
    } else if (field.kind === 'list') {
      await addListValues(scope, field.label, field.value);
      entered[field.label] = field.value;
    }
  }
  return entered;
}

/** Reads the same field list back and compares it with what was entered. */
export async function expectFields(page, scope, fields, entered) {
  for (const field of fields) {
    const expected = entered[field.label];
    if (expected === undefined || expected === '') continue;
    if (field.kind === 'money') {
      // The app normalises amounts (25.00 comes back as 25), so compare the value, not the text.
      expect(Number(await readText(scope, field.label)), `"${field.label}" came back unchanged`)
        .toBe(Number(expected));
    } else if (field.kind === 'text') {
      expect(await readText(scope, field.label), `"${field.label}" came back unchanged`).toBe(expected);
    } else if (field.kind === 'select') {
      expect(await readSelect(scope, field.label), `"${field.label}" came back unchanged`).toContain(expected);
    } else if (field.kind === 'date') {
      expect(await readDate(scope, field.label), `"${field.label}" came back unchanged`).toBe(expected);
    } else if (field.kind === 'toggle') {
      expect(await readToggle(scope, field.label), `"${field.label}" came back unchanged`).toBe(expected);
    } else if (field.kind === 'list') {
      expect(await readListValues(scope, field.label, expected), `"${field.label}" came back unchanged`)
        .toEqual(expected);
    }
  }
}

/**
 * Read-only detail pages render a label and its value as two sibling blocks,
 * with no control to read. Returns the value shown next to `label`.
 */
export async function labelledValue(scope, label) {
  const node = scope.getByText(label, { exact: true }).first();
  await expect(node, `"${label}" is shown on the page`).toBeVisible();
  return (await node.locator('xpath=following-sibling::*[1]').innerText()).trim();
}

/** The slot start/end pickers carry no label of their own; read them in order. */
export async function slotTimes(scope) {
  return (await scope.getByRole('button', { name: 'Search...' }).filter({ visible: true }).allInnerTexts())
    .map((text) => text.trim())
    .filter(Boolean);
}

/** TypeButton marks its active choice with a border class and no ARIA state. */
export async function expectSlotType(scope, label) {
  await expect(
    scope.getByRole('button', { name: label, exact: true }).filter({ visible: true }).first(),
    `the slot is still set to ${label}`,
  ).toHaveClass(/border-\(--neutral-950\)/);
}

const WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

/** The next date falling on `weekday`, at least `minDaysAhead` days from today. */
export function nextWeekdayDate(weekday, minDaysAhead = 1) {
  const wanted = WEEKDAYS.indexOf(weekday);
  if (wanted < 0) throw new Error(`${weekday} is not a day of the week`);
  const date = new Date();
  date.setHours(12, 0, 0, 0);
  date.setDate(date.getDate() + minDaysAhead);
  while (date.getDay() !== wanted) date.setDate(date.getDate() + 1);
  return date;
}

/**
 * Opens a CustomDatePicker, pages forward to the month `date` is in, and clicks
 * the day. Unlike `pickDate` this one is not limited to the month it opens on,
 * so it can reach a working day that falls after the turn of the month.
 */
export async function pickCalendarDate(page, scope, label, date) {
  const trigger = scope.getByRole('button', { name: labelPrefixRe(label) }).first();
  await expect(trigger, `"${label}" date field is on the page`).toBeVisible();
  await trigger.click();
  const calendar = popover(page);
  const year = calendar.getByRole('button', { name: 'Select year' });
  await expect(year, `the "${label}" calendar opens`).toBeVisible();
  const heading = year.locator('xpath=ancestor::span[1]');
  const nextMonth = year.locator('xpath=ancestor::span[1]/following-sibling::button[1]');

  const wanted = `${MONTHS[date.getMonth()]} ${date.getFullYear()}`;
  for (let step = 0; step < 24; step += 1) {
    if ((await heading.innerText()).trim() === wanted) break;
    await nextMonth.click();
  }
  expect((await heading.innerText()).trim(), `the calendar reached ${wanted}`).toBe(wanted);

  // A day that already holds sessions labels itself "12, has scheduled sessions".
  const day = date.getDate();
  const cell = calendar.getByRole('button', { name: new RegExp(`^${day}(,|$)`) }).first();
  await expect(cell, `${wanted.split(' ')[0]} ${day} can be chosen for "${label}"`).toBeEnabled();
  await cell.click();
  await expect(calendar).toBeHidden();
  return readDate(scope, label);
}
