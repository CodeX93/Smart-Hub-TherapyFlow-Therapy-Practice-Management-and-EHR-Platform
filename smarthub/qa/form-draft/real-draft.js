// Run with Playwright CLI after signing in to a real isolated QA assignment.
// The synthetic template has two required answers and one optional note.
async page => {
  const answer = name => page.getByRole('textbox', { name, exact: true });
  const save = () => page.getByRole('button', { name: 'Save Draft', exact: true }).click();
  const saved = () => page.getByRole('status').filter({ hasText: 'Draft saved' }).waitFor();
  const check = (condition, message) => { if (!condition) throw new Error(message); };
  await answer('First required answer *').fill('Persisted real backend draft');
  await answer('Optional note').fill('Clear this saved answer');
  await save(); await saved();
  await page.reload();
  await answer('First required answer *').waitFor();
  check(await answer('First required answer *').inputValue() === 'Persisted real backend draft', 'Draft was lost after refresh');
  check(await answer('Second required answer *').inputValue() === '', 'Partial draft unexpectedly requires every field');
  check(await answer('Optional note').inputValue() === 'Clear this saved answer', 'Optional draft answer missing');
  await answer('Optional note').fill('');
  await save(); await saved();
  await page.reload();
  await answer('Optional note').waitFor();
  check(await answer('Optional note').inputValue() === '', 'Cleared answer reappeared');
  await page.screenshot({ path: 'output/playwright/real-draft-restored.png', fullPage: true });
  return { partialDraftRestored: true, clearedAnswerRestored: true, url: page.url() };
}
