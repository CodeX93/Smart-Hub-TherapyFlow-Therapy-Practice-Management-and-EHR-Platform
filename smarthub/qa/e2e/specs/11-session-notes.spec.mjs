import { expectFields } from '../lib/forms.mjs';
import { built, expect, journey, markBuilt, note, test } from '../lib/journey.mjs';
import { need } from '../flows/prerequisites.mjs';
import {
  closeNoteModal,
  draftSessionNote,
  noteFields,
  noteIsSigned,
  openClientSessions,
  openNoteModal,
} from '../flows/notes.mjs';

/**
 * The clinical note on a booked session: it cannot be saved empty, a draft keeps
 * every field, and finalizing signs and locks it so it can only be amended, never
 * quietly edited.
 */
test.describe('session notes', { tag: '@notes' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(900000);
    await need('session');
  });

  test('an empty session note cannot be saved', { tag: '@negative' }, async () => {
    test.skip(built('note'), 'resuming: the note already exists, so the card no longer offers Add note');
    const { page } = journey;
    await openClientSessions();
    await openNoteModal('Add note');
    await expect(
      page.getByRole('button', { name: 'Save draft' }),
      'an empty note cannot be saved as a draft',
    ).toBeDisabled();
    await expect(
      page.getByRole('button', { name: 'Save & finalize' }),
      'and it cannot be finalized either',
    ).toBeDisabled();
    note('an empty session note is refused before any request is sent');
    await closeNoteModal();
  });

  test('a clinical note is drafted on the booked session', async () => {
    test.skip(built('note'), 'resuming: the earlier run already drafted this note');
    await draftSessionNote();
    markBuilt('note');
  });

  test('reopening the draft shows every field that was saved', { tag: '@verify' }, async () => {
    const { page, entered } = journey;
    await openClientSessions();
    // Once signed, the note renders as text rather than fields; the draft round-trip
    // has already been proven by the run that signed it.
    test.skip(
      await noteIsSigned(),
      'resuming: the note is already signed, and a signed note shows no editable fields',
    );
    await openNoteModal('Finish note');
    // The note loads after the editor opens; the first field filling in is the signal.
    await expect
      .poll(async () => (await page.getByLabel(/Session Focus/).first().inputValue()).trim(), {
        timeout: 45000,
        message: 'the saved note loads back into the editor',
      })
      .toBe(entered.note['Session Focus']);
    await expectFields(page, page, noteFields, entered.note);
    note('every clinical field of the draft came back unchanged');
    await closeNoteModal();
  });

  test('finalizing signs the note and locks it against editing', async () => {
    const { page } = journey;
    await openClientSessions();
    const alreadySigned = await noteIsSigned();
    if (!alreadySigned) {
      await openNoteModal('Finish note');

      // The draft opens read-only; editing is its own explicit step.
      await page.getByRole('button', { name: 'Edit Note' }).click();
    const finalize = page.getByRole('button', { name: 'Save & finalize' });
    await expect(finalize, 'the draft can be finalized').toBeEnabled({ timeout: 45000 });

    // A note cannot be signed while the risk assessment is unanswered: the first
    // attempt is refused and the editor lands on the risk step to say so.
    await finalize.click();
    await expect(
      page.getByText(/Answer every risk item/).first(),
      'finalizing is refused while the risk assessment is unanswered',
    ).toBeVisible({ timeout: 20000 });
    note('finalizing was refused until the risk assessment was answered');

    await page.getByRole('button', { name: 'No risk factors identified' }).click();
    await finalize.click();

    // Finalizing is irreversible, so the app asks first.
    await expect(
      page.getByText('Finalize this session note?').first(),
      'finalizing asks for confirmation',
    ).toBeVisible({ timeout: 20000 });
    await page.getByRole('button', { name: 'Finalize note', exact: true }).click();
    } else {
      note('resuming: this note was already signed, so only the locked state is checked');
      await openNoteModal('View note');
    }

    // The editor stays open, now showing the signed note: Amend and Download are
    // offered, and nothing lets it be edited or finalized again.
    await expect(page.getByRole('button', { name: 'Amend' }), 'a signed note can only be amended')
      .toBeVisible({ timeout: 45000 });
    await expect(page.getByRole('button', { name: 'Save & finalize' }), 'it cannot be finalized again')
      .toBeHidden();
    await expect(page.getByRole('button', { name: 'Save draft' }), 'and it cannot be edited as a draft')
      .toBeHidden();
    note('the signed note is locked: only Amend and Download PDF are offered');
    await closeNoteModal();

    await expect(
      page.getByText('Note signed').first(),
      'the card now says the note is signed',
    ).toBeVisible({ timeout: 45000 });
    await expect(
      page.getByRole('button', { name: 'View note', exact: true }).first(),
      'and only offers to view it',
    ).toBeVisible();
    note('the note was finalized and the card shows it as signed');
  });
});
