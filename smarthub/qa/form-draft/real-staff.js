// Run after opening the synthetic client's completed assignment details.
async page => {
  await page.getByRole('heading', { name: 'Form Assignment Details', exact: true }).waitFor();
  for (const text of ['COMPLETED', 'Persisted real backend draft', 'Final real backend answer']) {
    if (await page.getByText(text, { exact: true }).count() !== 1) throw new Error('Staff details missing: ' + text);
  }
  const signature = page.getByRole('img', { name: 'Signature from QA Alpha Client', exact: true });
  await signature.waitFor();
  if (!await signature.evaluate(img => img.complete && img.naturalWidth > 0)) throw new Error('Staff signature image failed to load');
  await page.screenshot({ path: 'output/playwright/real-draft-staff-completed.png', fullPage: true });
  return { staffCompletedStatus: true, staffAnswersMatch: true, staffSignatureRendered: true };
}
