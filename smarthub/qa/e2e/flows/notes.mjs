import { expect } from '@playwright/test';
import { fillFields } from '../lib/forms.mjs';
import { journey, note } from '../lib/journey.mjs';
import { stamp } from '../lib/fixtures.mjs';
import { openClientPreview } from './clients.mjs';

/**
 * A session's clinical note, written from the client's own Sessions tab. The card
 * on that tab says where the note stands — Add note, Finish note, View note — and
 * the same words drive this flow.
 */
export const noteFields = [
  { kind: 'text', label: 'Session Focus', value: `QA focus ${stamp}` },
  { kind: 'text', label: 'Symptoms', value: `QA symptoms ${stamp}` },
  { kind: 'text', label: 'Progress', value: `QA progress ${stamp}` },
  { kind: 'text', label: 'Short-term Goals', value: `QA goals ${stamp}` },
  { kind: 'text', label: 'Intervention', value: `QA intervention ${stamp}` },
  { kind: 'text', label: 'Remarks', value: `QA remarks ${stamp}` },
  { kind: 'text', label: 'Recommendations', value: `QA recommendations ${stamp}` },
];

/** Opens the client's Sessions tab and waits for the booked session's card. */
export async function openClientSessions() {
  const { page } = journey;
  await openClientPreview();
  await page.getByRole('button', { name: 'Sessions', exact: true }).click();
  // The tab loads its sessions after opening; the card's note button is the signal.
  const anyNoteButton = ['Add note', 'Write note', 'Finish note', 'View note']
    .map((name) => page.getByRole('button', { name, exact: true }))
    .reduce((a, b) => a.or(b))
    .first();
  await expect(anyNoteButton, "the booked session's card is on the tab")
    .toBeVisible({ timeout: 45000 });
}

/** True once the card reports the note as signed. */
export async function noteIsSigned() {
  return journey.page.getByText('Note signed').first().isVisible().catch(() => false);
}

/**
 * Clicks the card's note button. The wording follows the note's state — Add note,
 * Finish note, View note — so a resumed run passes the states it will accept.
 */
export async function openNoteModal(action) {
  const { page } = journey;
  const names = Array.isArray(action) ? action : [action];
  let button = page.getByRole('button', { name: names[0], exact: true });
  for (const name of names.slice(1)) {
    button = button.or(page.getByRole('button', { name, exact: true }));
  }
  button = button.first();
  await expect(button, `the session card offers ${names.join(' or ')}`).toBeVisible({ timeout: 45000 });
  await button.click();
  return page;
}

/** Fills the clinical tab and saves the note as a draft. */
export async function draftSessionNote() {
  const { page, entered } = journey;
  await openClientSessions();
  await openNoteModal('Add note');
  entered.note = await fillFields(page, page, noteFields);
  const save = page.getByRole('button', { name: 'Save draft' });
  await expect(save, 'a filled note can be saved').toBeEnabled();
  await save.click();
  await expect(
    page.getByText('Note in draft').first(),
    'the card now says the note is in draft',
  ).toBeVisible({ timeout: 45000 });
  note('a clinical note was drafted on the booked session');
}

/**
 * Closes the note modal by whichever control this build offers: the footer button
 * (Cancel on a draft, Close on a signed note) in older builds, or the header X
 * (aria-label "Close") once the footer button is removed. The footer button sits
 * after the header X in the DOM, so `.last()` prefers it while it still exists.
 */
export async function closeNoteModal() {
  const { page } = journey;
  await page.getByRole('button', { name: /^(Cancel|Close)$/ }).last().click();
}
