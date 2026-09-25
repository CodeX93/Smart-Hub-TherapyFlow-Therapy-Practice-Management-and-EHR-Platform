// Continue real-draft.js after independently checking persisted draft state.
async page => {
  const answer = name => page.getByRole('textbox', { name, exact: true });
  const check = (condition, message) => { if (!condition) throw new Error(message); };
  await answer('Second required answer *').fill('Final real backend answer');
  await page.getByRole('button', { name: 'Type Name', exact: true }).click();
  await answer('Type your full name Your typed name will be used as your electronic signature').fill('QA Alpha Client');
  await page.getByRole('button', { name: 'Submit Form', exact: true }).click();
  await page.getByText('Completed', { exact: true }).waitFor();
  await page.reload();
  await page.getByText('Completed', { exact: true }).waitFor();
  check(await answer('First required answer *').inputValue() === 'Persisted real backend draft', 'Draft answer changed at submission');
  check(await answer('Second required answer *').inputValue() === 'Final real backend answer', 'Final answer missing');
  check(await answer('Optional note').inputValue() === '', 'Cleared answer returned at submission');
  check(await answer('First required answer *').isDisabled(), 'Submitted form is editable');
  await page.getByRole('img', { name: 'Signature', exact: true }).waitFor();
  check(await page.getByRole('img', { name: 'Signature', exact: true }).evaluate(img => img.complete && img.naturalWidth > 0), 'Saved signature did not render');
  check(await page.getByRole('button', { name: 'Save Draft', exact: true }).count() === 0, 'Completed form still offers draft editing');
  await page.screenshot({ path: 'output/playwright/real-draft-completed.png', fullPage: true });
  return { completedAndReopened: true, answersPreserved: true, signatureRendered: true, readOnly: true };
}
