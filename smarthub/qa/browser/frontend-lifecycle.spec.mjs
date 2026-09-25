import { test, expect } from '@playwright/test';

const render = async (page, name, props = {}, strict = true) => {
  await page.evaluate(({ name, props, strict }) => window.qa.render(name, props, strict), { name, props, strict });
};
const rows = page => page.getByTestId('rows');
const readRows = async page => JSON.parse(await rows(page).textContent());
const events = page => page.evaluate(() => window.qa.events);
const pageErrors = new WeakMap();

test.beforeEach(async ({ page }) => {
  const errors = [];
  pageErrors.set(page, errors);
  page.on('pageerror', error => errors.push(error.message));
  await page.route('**/*', route => {
    const url = new URL(route.request().url());
    if (['127.0.0.1', 'localhost'].includes(url.hostname) || ['data:', 'blob:'].includes(url.protocol)) return route.continue();
    return route.abort('blockedbyclient');
  });
  await page.goto('/__qa/lifecycle');
  await page.waitForFunction(() => Boolean(window.qa));
});
test.afterEach(async ({ page }) => {
  expect(pageErrors.get(page), 'no uncaught browser errors').toEqual([]);
});

for (const strict of [false, true]) {
  test(`mounted billing pages retain, replace and deduplicate rows (StrictMode=${strict})`, async ({ page }) => {
    const scopeKey = 'client-a:unpaid';
    await render(page, 'billing', { kind: 'raw', scopeKey, page: 1, response: { items: [{ id: 1 }, { id: 2 }] } }, strict);
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }, { id: 2 }]);
    await render(page, 'billing', { kind: 'raw', scopeKey, page: 2 }, strict);
    await expect(page.getByTestId('ready')).toHaveText('false');
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }, { id: 2 }]);
    await render(page, 'billing', { kind: 'raw', scopeKey, page: 2, response: { items: [{ id: 2 }, { id: 3, status: 'pending' }] } }, strict);
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }, { id: 2 }, { id: 3, status: 'pending' }]);
    await render(page, 'billing', { kind: 'raw', scopeKey, page: 2, response: { items: [{ id: 3, status: 'paid' }] } }, strict);
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }, { id: 2 }, { id: 3, status: 'paid' }]);
    await page.getByRole('button', { name: 'Clear prior pages' }).click();
    await expect.poll(() => readRows(page)).toEqual([{ id: 3, status: 'paid' }]);
    await render(page, 'billing', { kind: 'raw', scopeKey: 'client-b:paid', page: 1 }, strict);
    await expect.poll(() => readRows(page)).toEqual([]);
    await render(page, 'billing', { kind: 'raw', scopeKey: 'client-b:paid', page: 1, response: { items: [] } }, strict);
    await expect(page.getByTestId('ready')).toHaveText('true');
    await expect.poll(() => readRows(page)).toEqual([]);
  });

  test(`abandoned billing renders cannot publish uncommitted rows (StrictMode=${strict})`, async ({ page }) => {
    await render(page, 'billing', { kind: 'raw', scopeKey: 'a', page: 1, response: [{ id: 1 }] }, strict);
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }]);
    await render(page, 'billing', { kind: 'raw', scopeKey: 'a', page: 2, response: [{ id: 99 }], suspend: true }, strict);
    await expect(page.getByText('Pending render')).toBeVisible();
    await render(page, 'billing', { kind: 'raw', scopeKey: 'a', page: 2 }, strict);
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }]);
    await expect(page.getByTestId('ready')).toHaveText('false');
  });
}

test('changing a pagination scope resets page before rendering its children', async ({ page }) => {
  await render(page, 'scoped', { scopeKey: 'a:pending' });
  await page.getByRole('button', { name: 'Next page' }).click();
  await expect(page.locator('output')).toHaveText('a:pending:2');
  await render(page, 'scoped', { scopeKey: 'b:paid' });
  await expect(page.locator('output')).toHaveText('b:paid:1');
});

for (const kind of ['records', 'invoices']) {
  test(`mapped billing ${kind} retain earlier pages through loading and reset`, async ({ page }) => {
    const invoice = id => ({ id, status: 'pending', amount: 100, discountAmount: 10, paidAmount: 20, currency: 'CAD' });
    await render(page, 'billing', { kind, page: 1, response: { content: [invoice(1)] } });
    await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['1']);
    await render(page, 'billing', { kind, page: 2 });
    await expect(page.getByTestId('ready')).toHaveText('false');
    await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['1']);
    await render(page, 'billing', { kind, page: 2, response: { content: [invoice(2)] } });
    await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['1', '2']);
    await page.getByRole('button', { name: 'Clear prior pages' }).click();
    await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['2']);
  });
}

test('real task query replaces edited/deleted page rows and resets client and therapist filters', async ({ page }) => {
  let updated = false;
  const requests = [];
  let releaseClient;
  const clientReady = new Promise(resolve => { releaseClient = resolve; });
  await page.route('**/api/v1/tasks?*', async route => {
    const url = new URL(route.request().url());
    expect(url.hostname).toBe('127.0.0.1');
    const client = url.searchParams.get('clientId');
    const assigned = url.searchParams.get('assignedToId');
    const number = Number(url.searchParams.get('page'));
    requests.push({ client, assigned, page: number });
    if (client === '2') await clientReady;
    const ids = number === 1 ? [1, 2] : updated ? [4] : [3, 4];
    const items = ids.map(id => ({ id: Number(client) * 100 + id, title: `${client}-${assigned}-${id}${updated ? '-updated' : ''}`, status: 'pending', priority: 'medium' }));
    await route.fulfill({ json: { items, page: number, pageSize: 10, totalCount: 20, totalPages: 2 } });
  });
  await render(page, 'tasks', { clientId: '1', assignedToId: 7 });
  await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['101', '102']);
  await page.getByRole('button', { name: 'Next page' }).click();
  await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['101', '102', '103', '104']);
  updated = true;
  await page.getByRole('button', { name: 'Refetch current page' }).click();
  await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['101', '102', '104']);
  await expect.poll(async () => (await readRows(page)).at(-1).title).toBe('1-7-4-updated');
  await page.getByRole('button', { name: 'Refresh after mutation' }).click();
  await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['101', '102']);
  await render(page, 'tasks', { clientId: '2', assignedToId: 7 });
  await expect.poll(() => requests.some(request => request.client === '2')).toBe(true);
  await expect.poll(() => readRows(page)).toEqual([]);
  releaseClient();
  await expect.poll(async () => (await readRows(page)).map(row => row.id)).toEqual(['201', '202']);
  expect(requests.filter(request => request.client === '2').every(request => request.page === 1)).toBe(true);
  await page.getByRole('button', { name: 'Next page' }).click();
  await expect.poll(async () => (await readRows(page)).length).toBe(3);
  await render(page, 'tasks', { clientId: '2', assignedToId: 8 });
  await expect.poll(async () => (await readRows(page))[0]?.title).toBe('2-8-1-updated');
  expect(requests.filter(request => request.assigned === '8').every(request => request.page === 1)).toBe(true);
});

for (const role of ['adminTasks', 'therapistTasks']) {
  test(`${role}: client changes close old comments; create triggers open only once`, async ({ page }) => {
    await page.route('**/api/v1/**', route => {
      const url = new URL(route.request().url());
      expect(url.hostname).toBe('127.0.0.1');
      if (url.pathname.endsWith('/auth/me')) return route.fulfill({ json: { user: { id: 7, firstName: 'QA', lastName: 'Staff' } } });
      if (url.pathname.endsWith('/tasks')) {
        const client = url.searchParams.get('clientId');
        return route.fulfill({ json: { items: [{ id: Number(client) * 100, title: `Client ${client} task`, priority: 'medium', status: 'pending', createdAt: '2026-09-01T12:00:00Z' }], totalCount: 1, totalPages: 1, page: 1, pageSize: 10 } });
      }
      if (url.pathname.endsWith('/comments') || url.pathname.includes('system-options')) return route.fulfill({ json: [] });
      return route.fulfill({ json: { items: [], totalCount: 0 } });
    });
    await render(page, role, { client: { id: '1', name: 'QA Client A' } });
    await expect(page.getByRole('heading', { name: 'Client 1 task' })).toBeVisible();
    await page.getByRole('button', { name: 'View Comments' }).click();
    await expect(page.getByRole('dialog', { name: 'Task Comments' })).toBeVisible();
    await render(page, role, { client: { id: '2', name: 'QA Client B' } });
    await expect(page.getByRole('dialog', { name: 'Task Comments' })).toBeHidden();
    await expect(page.getByRole('heading', { name: 'Client 2 task' })).toBeVisible();
    const props = { client: { id: '2', name: 'QA Client B' }, openCreateTrigger: 1 };
    await render(page, role, props);
    await page.getByRole('button', { name: 'Close create task' }).click();
    await render(page, role, props);
    await expect(page.getByRole('button', { name: 'Close create task' })).toBeHidden();
    await render(page, role, { ...props, openCreateTrigger: 2 });
    await expect(page.getByRole('button', { name: 'Close create task' })).toBeVisible();
  });
}

test('organisation inputs keep registration, sanitization, blur, focus and submitted values', async ({ page }) => {
  await render(page, 'organisation');
  await page.getByPlaceholder('Enter first name').fill('  Ada  ');
  await page.getByPlaceholder('Enter last name').fill('Lovelace');
  await page.getByPlaceholder('admin@organization.com').fill('ADA@EXAMPLE.TEST');
  await page.getByPlaceholder('Enter organization name').fill('QA Clinic');
  await page.getByPlaceholder('your-organization-slug').fill('QA CLINIC!');
  await page.getByRole('button', { name: 'Focus first name' }).click();
  await expect(page.getByPlaceholder('Enter first name')).toBeFocused();
  await page.getByRole('button', { name: 'Submit fixture' }).click();
  await expect.poll(events.bind(null, page)).toHaveLength(1);
  const [values] = await events(page);
  expect(values).toMatchObject({ firstName: '  Ada  ', lastName: 'Lovelace', email: 'ADA@EXAMPLE.TEST', organisationName: 'QA Clinic', tenantSlug: 'qaclinic', trialDays: '14' });
  const state = JSON.parse(await page.getByTestId('form-state').textContent());
  for (const field of ['firstName', 'lastName', 'email', 'organisationName', 'tenantSlug']) {
    expect(state.dirty[field], `${field} marked dirty`).toBe(true);
    expect(state.touched[field], `${field} marked touched`).toBe(true);
  }
});

test('notification timeout uses latest callback, restarts for a new message and cleans up', async ({ page }) => {
  await page.clock.install();
  await render(page, 'toast', { message: 'First message', callback: 'old' });
  await expect(page.getByText('First message')).toBeVisible();
  await page.clock.runFor(700);
  await render(page, 'toast', { message: 'First message', callback: 'latest' });
  await page.clock.runFor(301);
  expect(await events(page)).toEqual(['latest']);
  await render(page, 'toast', { message: 'Second message', callback: 'second' });
  await expect(page.getByText('Second message')).toBeVisible();
  await page.clock.runFor(700);
  await render(page, 'toast', { message: 'Third message', callback: 'third' });
  await expect(page.getByText('Third message')).toBeVisible();
  await page.clock.runFor(301);
  expect(await events(page)).toEqual(['latest']);
  await page.clock.runFor(700);
  expect(await events(page)).toEqual(['latest', 'third']);
  await render(page, 'toast', { message: 'Unmounted', callback: 'unmounted' });
  await expect(page.getByText('Unmounted')).toBeVisible();
  await page.evaluate(() => window.qa.unmount());
  await page.clock.runFor(1001);
  expect(await events(page)).toEqual(['latest', 'third']);
});

for (const custom of [false, true]) {
  test(`action menu retains anchor, nested actions and scroll dismissal (custom=${custom})`, async ({ page }) => {
    await render(page, 'dropdown', { custom });
    const trigger = custom ? page.getByRole('button', { name: 'Custom actions' }) : page.getByRole('button').first();
    await trigger.click();
    await expect(page.getByRole('button', { name: 'Disabled action' })).toBeDisabled();
    await page.getByRole('button', { name: 'More actions' }).click();
    await page.getByRole('button', { name: 'Nested action' }).click();
    await expect(page.getByRole('button', { name: 'Choose action' })).toBeHidden();
    await trigger.click();
    await expect(page.getByRole('button', { name: 'Choose action' })).toBeVisible();
    await page.getByTestId('scroll-container').evaluate(node => { node.scrollTop = 40; });
    await expect(page.getByRole('button', { name: 'Choose action' })).toBeHidden();
    const actual = await events(page);
    expect(actual.filter(event => event === 'nested')).toHaveLength(1);
    expect(actual).not.toContain('disabled');
    if (custom) expect(actual).toContain('custom-ref-attached');
  });
}

test('drawn signature survives resize and clears through the real form', async ({ page }) => {
  await render(page, 'signature');
  await page.getByRole('button', { name: /Draw/ }).click();
  const canvas = page.locator('canvas');
  const box = await canvas.boundingBox();
  await page.mouse.move(box.x + 20, box.y + 20);
  await page.mouse.down();
  await page.mouse.move(box.x + 120, box.y + 80, { steps: 10 });
  await page.mouse.up();
  await expect(page.getByTestId('signature')).toContainText('data:image/png');
  await page.getByRole('button', { name: 'Resize signature' }).click();
  await expect.poll(() => canvas.evaluate(node => Array.from(node.getContext('2d').getImageData(0, 0, node.width, node.height).data).some(value => value > 0))).toBe(true);
  await page.getByRole('button', { name: 'Clear', exact: true }).click();
  await expect(page.getByTestId('signature')).toHaveText('');
  await expect.poll(() => canvas.evaluate(node => Array.from(node.getContext('2d').getImageData(0, 0, node.width, node.height).data).every(value => value === 0))).toBe(true);
});

for (const pageNumber of [0, 1]) {
  test(`shared list cache replaces pages and applies edits across loaded pages (base=${pageNumber})`, async ({ page }) => {
    await render(page, 'paged', { scopeKey: 'client-a', page: pageNumber, items: [{ id: 1 }, { id: 2 }] });
    await render(page, 'paged', { scopeKey: 'client-a', page: pageNumber + 1, items: [{ id: 2, title: 'updated' }, { id: 3 }] });
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }, { id: 2, title: 'updated' }, { id: 3 }]);
    await page.getByRole('button', { name: 'Remove row two' }).click();
    await expect.poll(() => readRows(page)).toEqual([{ id: 1 }, { id: 3 }]);
    await render(page, 'paged', { scopeKey: 'client-b', page: pageNumber });
    await expect.poll(() => readRows(page)).toEqual([]);
    await expect(page.getByTestId('ready')).toHaveText('false');
  });
}

for (const portal of [false, true]) {
  test(`${portal ? 'client' : 'super-admin'} notifications retain unread state after failure and confirm successful reads`, async ({ page }) => {
    let failRead = true;
    let read = false;
    let readRequests = 0;
    await page.route('**/api/v1/**', async route => {
      const url = new URL(route.request().url());
      expect(url.hostname).toBe('127.0.0.1');
      if (route.request().method() === 'PATCH') {
        readRequests++;
        if (failRead) return route.fulfill({ status: 500, json: { message: 'QA read rejected' } });
        read = true;
        return route.fulfill({ status: 204 });
      }
      if (url.pathname.includes('count')) return route.fulfill({ json: read ? 0 : 1 });
      const items = [{ id: 1, title: 'QA notification', message: 'Synthetic task comment notification', isRead: read, status: 'PENDING', createdAt: '2026-09-11T12:00:00Z' }];
      return route.fulfill({ json: portal ? { items, page: 1, pageSize: 20, totalPages: 1, totalCount: 1 } : items });
    });
    await render(page, 'notifications', { useApi: !portal, usePortalApi: portal });
    await expect(page.getByRole('heading', { name: 'QA notification' })).toBeVisible();
    await page.getByTitle('Mark as read', { exact: true }).click();
    await expect.poll(() => readRequests).toBe(1);
    await expect(page.getByText('QA read rejected')).toBeVisible();
    await expect(page.getByTitle('Mark as read', { exact: true })).toBeVisible();
    failRead = false;
    await page.getByTitle('Mark as read', { exact: true }).click();
    await expect(page.getByTitle('Read', { exact: true })).toBeVisible();
    await expect(page.getByTitle('Mark as read', { exact: true })).toHaveCount(0);
  });
}

test('report editor preserves drafts on refetch and resets on record changes and reopen', async ({ page }) => {
  const template = { id: 1, name: 'First template', description: 'Original', defaultIncludeProfile: true };
  await render(page, 'editReport', { template });
  const name = page.getByRole('textbox', { name: 'Template name' });
  await expect(name).toHaveValue('First template');
  await name.fill('Unsaved draft');
  await render(page, 'editReport', { template: { ...template, description: 'Background refresh' } });
  await expect(name).toHaveValue('Unsaved draft');
  await render(page, 'editReport', { template: { ...template, id: 2, name: 'Second template' } });
  await expect(name).toHaveValue('Second template');
  await name.fill('Discard on close');
  await render(page, 'editReport', { template: null });
  await expect(name).toHaveCount(0);
  await render(page, 'editReport', { template });
  await expect(name).toHaveValue('First template');
});

test('upload report modal clears its previous draft when reopened', async ({ page }) => {
  await render(page, 'uploadReport', { isOpen: true });
  const name = page.getByRole('textbox', { name: 'Template name' });
  await name.fill('Draft upload');
  await render(page, 'uploadReport', { isOpen: false });
  await render(page, 'uploadReport', { isOpen: true });
  await expect(name).toHaveValue('');
});

test('AI template editor keeps edits through a background catalog refresh', async ({ page }) => {
  const template = { id: 1, name: 'QA template', instructions: 'Original instructions' };
  const props = { isOpen: true, selectedTemplateId: 1, templates: [template] };
  await render(page, 'aiTemplate', props);
  const name = page.locator('input').filter({ visible: true }).first();
  await expect(name).toHaveValue('QA template');
  await name.fill('Unsaved clinical draft');
  await render(page, 'aiTemplate', { ...props, templates: [{ ...template, instructions: 'Refetched instructions' }] });
  await expect(name).toHaveValue('Unsaved clinical draft');
  await render(page, 'aiTemplate', { ...props, isOpen: false });
  await render(page, 'aiTemplate', props);
  await expect(name).toHaveValue('QA template');
});

for (const pendingPermission of [false, true]) {
  test(`closing voice recording releases microphone resources (pending permission=${pendingPermission})`, async ({ page }) => {
    await page.evaluate(pending => {
      window.qaAudio = { stops: 0, starts: 0, pending };
      const stream = { getTracks: () => [{ stop() { window.qaAudio.stops++; } }] };
      Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: {
        getUserMedia: () => pending ? new Promise(resolve => { window.qaAudio.release = () => resolve(stream); }) : Promise.resolve(stream),
      } });
      window.MediaRecorder = class {
        state = 'inactive'; mimeType = 'audio/webm';
        start() { this.state = 'recording'; window.qaAudio.starts++; }
        stop() { this.state = 'inactive'; this.onstop?.(); }
      };
    }, pendingPermission);
    await render(page, 'voice', { isOpen: true });
    await page.getByRole('button').filter({ has: page.locator('svg.lucide-mic') }).click();
    if (pendingPermission) await expect.poll(() => page.evaluate(() => Boolean(window.qaAudio.release))).toBe(true);
    else await expect.poll(() => page.evaluate(() => window.qaAudio.starts)).toBe(1);
    await render(page, 'voice', { isOpen: false });
    await expect(page.getByRole('heading', { name: 'Voice Recording' })).toHaveCount(0);
    if (pendingPermission) await page.evaluate(() => window.qaAudio.release());
    await expect.poll(() => page.evaluate(() => window.qaAudio.stops)).toBe(1);
    expect(await page.evaluate(() => window.qaAudio.starts)).toBe(pendingPermission ? 0 : 1);
  });
}

const installAssessmentSpeech = async page => page.evaluate(() => {
  window.qaSpeech = { starts: 0, stops: 0, aborts: 0, instance: null };
  class FakeSpeechRecognition {
    continuous = false;
    interimResults = false;
    lang = 'en-US';
    onresult = null;
    onerror = null;
    onend = null;
    start() {
      window.qaSpeech.starts += 1;
      window.qaSpeech.instance = this;
    }
    stop() {
      window.qaSpeech.stops += 1;
      window.qaSpeech.instance = null;
      this.onend?.();
    }
    abort() {
      window.qaSpeech.aborts += 1;
      window.qaSpeech.instance = null;
      this.onend?.();
    }
  }
  window.SpeechRecognition = FakeSpeechRecognition;
  window.webkitSpeechRecognition = FakeSpeechRecognition;
  window.qaSpeech.emit = (transcript, isFinal = true) => {
    const recognition = window.qaSpeech.instance;
    if (!recognition?.onresult) return false;
    recognition.onresult({
      resultIndex: 0,
      results: Object.assign([{ isFinal, 0: { transcript } }], { length: 1 }),
    });
    return true;
  };
});

for (const kind of ['text', 'textarea', 'voice']) {
  test(`assessment ${kind} fields show a mic and fill from speech [ASSESS-VOICE]`, async ({ page }) => {
    await installAssessmentSpeech(page);
    await render(page, 'assessmentVoice', { kind, value: 'Existing answer' });
    const mic = page.getByTestId('assessment-voice-mic');
    await expect(mic).toBeVisible();
    await expect(page.getByTestId('assessment-answer')).toHaveText('Existing answer');

    await mic.click();
    await expect(page.getByText('Voice Recording', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Start Recording', exact: true }).click();
    await expect.poll(() => page.evaluate(() => window.qaSpeech.starts)).toBeGreaterThan(0);
    await expect.poll(() => page.evaluate(() => window.qaSpeech.emit('spoken clinical note'))).toBe(true);
    await expect(page.getByTestId('assessment-answer')).toHaveText('Existing answer spoken clinical note');

    await page.getByRole('button', { name: 'Cancel recording', exact: true }).click();
    await expect(page.getByTestId('assessment-answer')).toHaveText('Existing answer');
    await expect(page.getByText('Voice Recording', { exact: true })).toHaveCount(0);

    await mic.click();
    await page.getByRole('button', { name: 'Start Recording', exact: true }).click();
    await expect.poll(() => page.evaluate(() => window.qaSpeech.emit('confirmed spoken note'))).toBe(true);
    await page.getByRole('button', { name: 'Confirm recording', exact: true }).click();
    await expect(page.getByTestId('assessment-answer')).toHaveText('Existing answer confirmed spoken note');
    await expect(page.getByText('Voice Recording', { exact: true })).toHaveCount(0);
  });
}

test('system email toolbar formats the selected text and retains the draft', async ({ page }) => {
  await page.route('**/api/v1/**', route => route.fulfill({ json: [] }));
  await render(page, 'systemSettings');
  const body = page.locator('textarea');
  await expect(body).toBeVisible();
  await body.fill('QA selected text');
  await body.evaluate(element => { element.focus(); element.setSelectionRange(3, 11); });
  await page.getByRole('button', { name: 'Bold', exact: true }).click();
  await expect(body).toHaveValue('QA <strong>selected</strong> text');
  await page.getByRole('button', { name: 'Clear formatting', exact: true }).click();
  await expect(body).toHaveValue('QA selected text');
});

test('integration settings preserve drafts on refetch and isolate provider changes', async ({ page }) => {
  let zoomClient = 'qa-zoom-client';
  let releaseStripe;
  const stripeReady = new Promise(resolve => { releaseStripe = resolve; });
  await page.route('**/api/v1/super-admin/integrations/*', async route => {
    expect(route.request().method()).toBe('GET');
    const key = new URL(route.request().url()).pathname.split('/').at(-1);
    if (key === 'stripe') await stripeReady;
    await route.fulfill({ json: { key, enabled: false, clientId: key === 'zoom' ? zoomClient : 'qa-stripe-client',
      publishableKey: key === 'stripe' ? 'pk_test_qa_synthetic' : null, secret: '***' } });
  });
  const client = page.getByPlaceholder('OAuth client ID (Zoom) or Stripe Connect client ID (ca_...)');
  await render(page, 'integration', { integrationKey: 'zoom' });
  await expect(client).toHaveValue('qa-zoom-client');
  await client.fill('qa-unsaved-zoom-draft');
  zoomClient = 'qa-refetched-zoom';
  await page.evaluate(() => window.qa.refetchIntegration('zoom'));
  await expect(client).toHaveValue('qa-unsaved-zoom-draft');
  await render(page, 'integration', { integrationKey: 'stripe' });
  await expect.poll(() => page.locator('input').evaluateAll(inputs => inputs.map(input => input.value))).not.toContain('qa-unsaved-zoom-draft');
  releaseStripe();
  await expect(client).toHaveValue('qa-stripe-client');
  await expect(page.getByPlaceholder('Must start with pk_ or rk_')).toHaveValue('pk_test_qa_synthetic');
  await render(page, 'integration', { integrationKey: 'zoom' });
  await expect(client).toHaveValue('qa-refetched-zoom');
});

test('session-note drafts survive refetch, cancel restores data and changing session resets the editor', async ({ page }) => {
  let focus = 'Original note focus';
  const note = id => ({ id, sessionId: id, clientId: 1, therapistId: 2, date: '2026-09-11T12:00:00Z',
    sessionFocus: id === 1 ? focus : 'Second session focus', symptoms: 'Synthetic symptoms' });
  await page.route('**/api/v1/session-notes/**', async route => {
    expect(route.request().method()).toBe('GET');
    const path = new URL(route.request().url()).pathname;
    const list = path.match(/sessions\/(\d+)\/notes$/);
    await route.fulfill({ json: list ? [note(Number(list[1]))] : note(Number(path.split('/').at(-1))) });
  });
  await render(page, 'noteDetail', { isOpen: true, sessionId: 1 });
  const field = page.getByRole('textbox', { name: 'Session Focus', exact: true });
  await expect(field).toHaveValue('Original note focus');
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await field.fill('Unsaved clinical draft');
  focus = 'Refetched note focus';
  await page.evaluate(() => window.qa.refetchNote(1));
  await expect(field).toHaveValue('Unsaved clinical draft');
  await page.getByRole('button', { name: 'Cancel', exact: true }).click();
  await expect(field).toHaveValue('Refetched note focus');
  await expect(field).toBeDisabled();
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await field.fill('Discard when session changes');
  await render(page, 'noteDetail', { isOpen: true, sessionId: 2 });
  await expect(field).toHaveValue('Second session focus');
  await expect(field).toBeDisabled();
});


for (const role of ['admin', 'staff', 'therapist']) {
  test(`client search waits for results and debounces typing (${role})`, async ({ page }) => {
    const searches = [];
    let release;
    const held = new Promise(resolve => { release = resolve; });
    const reply = items => ({ items, totalPages: 1, totalCount: items.length, page: 1, pageSize: 25 });
    await page.route('**/api/**', async route => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith('/api/')) return route.continue();
      if (url.pathname === '/api/v1/auth/me') return route.fulfill({ json: { user: { id: 17 }, roles: [role.toUpperCase()], permissions: [] } });
      if (url.pathname === '/api/v1/clients') {
        const search = url.searchParams.get('search') || '';
        searches.push(search);
        if (search) {
          await held;
          return route.fulfill({ json: reply([]) });
        }
        return route.fulfill({ json: reply([{ id: 1, fullName: 'Synthetic Search Client', clientId: 'CL-2099-0001' }]) });
      }
      return route.fulfill({ json: [] });
    });
    await render(page, 'clients', { role });
    await expect(page.getByText('Synthetic Search Client', { exact: true })).toBeVisible();
    const search = page.getByPlaceholder('Search name, email, phone, MRN or DOB...');
    try {
      await search.pressSequentially('CL-2099-9999', { delay: 30 });
      await expect.poll(() => searches.includes('CL-2099-9999')).toBe(true);
      expect(searches.filter(Boolean)).toEqual(['CL-2099-9999']);
      await expect(page.getByText('No clients found.', { exact: true })).toHaveCount(0);
      await expect(page.getByRole('status').filter({ hasText: 'Searching clients' })).toBeVisible();
    } finally { release(); }
    await expect(page.getByText('No clients found.', { exact: true })).toBeVisible();
    await expect(page.getByRole('status').filter({ hasText: 'Searching clients' })).toHaveCount(0);
    await search.fill('');
    await expect(page.getByText('Synthetic Search Client', { exact: true })).toBeVisible();
  });
}


for (const role of ['admin', 'staff', 'therapist']) {
  test(`client search ignores an older response and accepts partial MRN (${role})`, async ({ page }) => {
    const requests = [];
    let releaseOld;
    let deliveredOld;
    const oldDelivered = new Promise(resolve => { deliveredOld = resolve; });
    const oldResponse = new Promise(resolve => { releaseOld = resolve; });
    const reply = (name, id = 1) => ({ items: [{ id, fullName: name, clientId: 'CL-2099-0395' }], totalPages: 1, totalCount: 1, page: 1, pageSize: 25 });
    await page.route('**/api/**', async route => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith('/api/')) return route.continue();
      if (url.pathname === '/api/v1/auth/me') return route.fulfill({ json: { user: { id: 17 }, roles: [role.toUpperCase()], permissions: [] } });
      if (url.pathname === '/api/v1/clients') {
        const search = url.searchParams.get('search') || '';
        requests.push({ search, page: url.searchParams.get('page') });
        if (search === 'CL-2099') {
          await oldResponse;
          await route.fulfill({ json: reply('Old Search Result', 2) });
          deliveredOld();
          return;
        }
        if (!search) {
          const pageNumber = Number(url.searchParams.get('page'));
          return route.fulfill({ json: { ...reply(pageNumber === 1 ? 'Initial Client' : 'Second Page Client', pageNumber),
            page: pageNumber, totalPages: 2, totalCount: 2 } });
        }
        return route.fulfill({ json: reply('Partial MRN Result') });
      }
      return route.fulfill({ json: [] });
    });
    await render(page, 'clients', { role });
    await expect(page.getByText('Initial Client', { exact: true })).toBeVisible();
    await expect(page.getByText('Second Page Client', { exact: true })).toBeVisible();
    const search = page.getByPlaceholder('Search name, email, phone, MRN or DOB...');
    await search.fill('CL-2099');
    await expect.poll(() => requests.some(r => r.search === 'CL-2099')).toBe(true);
    try {
      await search.fill('0395');
      await expect(page.getByText('Partial MRN Result', { exact: true })).toBeVisible();
      expect(requests.find(r => r.search === '0395').page).toBe('1');
    } finally { releaseOld(); }
    await oldDelivered;
    await expect(page.getByText('Old Search Result', { exact: true })).toHaveCount(0);
    // Whitespace normalization must not clear a cached, unchanged query.
    await search.fill(' 0395 ');
    await expect(page.getByText('Partial MRN Result', { exact: true })).toBeVisible();
  });
}

for (const role of ['admin', 'staff', 'therapist']) {
  test(`client search field submits all input formats to the shared search API (${role})`, async ({ page }) => {
    const requests = [];
    await page.route('**/api/**', async route => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith('/api/')) return route.continue();
      if (url.pathname === '/api/v1/auth/me') return route.fulfill({ json: { user: { id: 17 }, roles: [role.toUpperCase()], permissions: [] } });
      if (url.pathname === '/api/v1/clients') {
        requests.push(url.searchParams.get('search') || '');
        return route.fulfill({ json: { items: [], totalPages: 0, totalCount: 0, page: 1, pageSize: 25 } });
      }
      return route.fulfill({ json: [] });
    });
    await render(page, 'clients', { role });
    await expect(page.getByText('No clients found.', { exact: true })).toBeVisible();
    const search = page.getByPlaceholder('Search name, email, phone, MRN or DOB...');
    const width = await search.evaluate(input => input.parentElement.parentElement.getBoundingClientRect().width);
    expect(width).toBeCloseTo(398.4, 0); // Original 332 px × 1.20 at the default root font size.
    await expect(search).toHaveAttribute('title', /DOB.*YYYY-MM-DD.*MM\/DD\/YYYY/);
    for (const value of ['Jane Doe', ' JANE.DOE@EXAMPLE.COM ', '+1 (519) 555-0123', '5195550123', '1988-04-19', '04/19/1988', '0395']) {
      await search.fill(value);
      await expect.poll(() => requests.at(-1)).toBe(value.trim());
      await expect(page.getByText('No clients found.', { exact: true })).toBeVisible();
    }
    // This checks transport through the real page/API hook, not server matching.
    expect(requests.filter(Boolean)).toEqual(['Jane Doe', 'JANE.DOE@EXAMPLE.COM', '+1 (519) 555-0123', '5195550123', '1988-04-19', '04/19/1988', '0395']);
  });
}

for (const role of ['admin', 'staff', 'therapist']) {
  test(`billing reference opens overview and name opens billing (${role})`, async ({ page }) => {
    await page.route('**/api/**', route => new URL(route.request().url()).pathname.startsWith('/api/')
      ? route.fulfill({ json: { timezone: 'America/Toronto' } }) : route.continue());
    for (const history of [false, true]) {
      const records = [
        { id: 1, clientId: 42, clientName: 'Synthetic Client', clientReferenceNumber: 'REF-123', totalAmount: 100, amountDue: 100, billingStatus: 'pending' },
        { id: 2, clientId: 43, clientName: 'No Reference', clientReferenceNumber: null, totalAmount: 100, amountDue: 100, billingStatus: 'pending' },
        { id: 3, clientId: 44, clientName: 'Blank Reference', clientReferenceNumber: '  ', totalAmount: 100, amountDue: 100, billingStatus: 'pending' },
      ];
      await render(page, 'billingLinks', { role, records, history });
      const reference = page.getByRole('link', { name: 'Ref: REF-123' });
      await expect(reference).toBeVisible();
      await expect(page.getByRole('link', { name: /^Ref:/ })).toHaveCount(1);
      await reference.focus();
      await page.keyboard.press('Enter');
      await expect(page.getByTestId('location')).toContainText(`"path":"/${role}/clients"`);
      await expect(page.getByTestId('location')).toContainText('"initialTab":"Overview"');
      await expect(page.getByTestId('location')).toContainText('"clientId":"42"');
      await page.getByRole('link', { name: 'Synthetic Client', exact: true }).click();
      await expect(page.getByTestId('location')).toContainText('"initialTab":"Billing"');
    }
  });
}

for (const role of ['admin', 'staff', 'therapist']) {
  test(`billing filters send every dropdown option and clear (${role})`, async ({ page }) => {
    test.setTimeout(120000);
    const requests = [];
    const statisticsRequests = [];
    await page.route('**/api/**', route => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith('/api/')) return route.continue();
      if (url.pathname === '/api/v1/billing/statistics') {
        statisticsRequests.push(Object.fromEntries(url.searchParams));
        return route.fulfill({ json: {totalBillingRecords: 97} });
      }
      if (url.pathname === '/api/v1/billing/billing') {
        requests.push(Object.fromEntries(url.searchParams));
        return route.fulfill({ json: { content: [], totalElements: 0, totalPages: 0, number: 0, last: true } });
      }
      return route.fulfill({ json: { timezone: 'America/Toronto' } });
    });
    await render(page, 'billings', { role });
    await expect.poll(() => requests.length).toBeGreaterThan(0);
    expect(requests[0].startDate).toBeTruthy(); expect(requests[0].endDate).toBeTruthy();
    await expect(page.getByText('Total Records', {exact:true}).locator('..')).toContainText('0');
    await expect(page.getByText('Total Records', {exact:true}).locator('..')).not.toContainText('97');
    const sets = [
      ['Billing Status','status',[['Pending','pending'],['Partial','partial'],['Paid','paid'],['Denied','denied']]],
      ['Payment Status','paymentStatus',[['Pending','pending'],['Paid','paid'],['Partial','partial'],['Failed','failed'],['Refunded','refunded']]],
      ['Payment Method','paymentMethod',[['Cash','cash'],['Check','check'],['Credit Card','credit_card'],['Debit Card','debit_card'],['Insurance','insurance'],['Bank Transfer','bank_transfer'],['Online Payment','online_payment'],['Credit Balance','credit_balance']]],
      ['Client Type','clientType',[['Individual','individual'],['Couple','couple'],['Family','family'],['Group','group'],['Refugee','refugee'],['MVA','mva']]],
      ['Session Type','sessionType',[['In Person','in-person'],['Online','online']]],
    ];
    for (const [label,param,options] of sets) {
      for (const [text,value] of options) {
        await page.getByRole('button', {name:'Filters',exact:true}).click();
        await page.getByRole('button', {name:'Clear all',exact:true}).first().click();
        await page.getByRole('button', {name:'Filters',exact:true}).click();
        await page.getByRole('button', {name:label,exact:true}).click();
        await page.getByText(text,{exact:true}).last().click();
        await page.getByRole('button', {name:'Apply filters',exact:true}).click();
        await expect.poll(() => requests.at(-1)?.[param]).toBe(value);
        expect(requests.at(-1).page).toBe('0');
        await expect.poll(() => statisticsRequests.at(-1)?.[param]).toBe(value);
      }
    }
    await page.getByRole('button', {name:'Filters',exact:true}).click();
    await page.getByRole('button', {name:'Clear all',exact:true}).first().click();
    await expect.poll(() => requests.at(-1)?.sessionType).toBeUndefined();
  });
}

for (const role of ['admin', 'staff', 'therapist']) {
  test(`billing filters dates chips cancellation and combined statuses (${role})`, async ({ page }) => {
    const requests = [];
    await page.route('**/api/**', route => {
      const url = new URL(route.request().url());
      if (!url.pathname.startsWith('/api/')) return route.continue();
      if (url.pathname === '/api/v1/billing/billing') {
        requests.push(Object.fromEntries(url.searchParams));
        return route.fulfill({ json: { content: [], totalElements: 0, totalPages: 0, number: 0, last: true } });
      }
      return route.fulfill({ json: { timezone: 'America/Toronto' } });
    });
    await render(page, 'billings', { role });
    await expect.poll(() => requests.length).toBeGreaterThan(0);
    await page.getByRole('button',{name:'Filters',exact:true}).click();
    await page.getByRole('button',{name:/^Start Date/}).click();
    await page.getByRole('button',{name:'20',exact:true}).click();
    await page.getByRole('button',{name:/^End Date/}).click();
    await page.getByRole('button',{name:'10',exact:true}).click();
    await expect(page.getByText('Start date cannot be after end date')).toBeVisible();
    await expect(page.getByRole('button',{name:'Apply filters'})).toBeDisabled();
    await page.getByRole('button',{name:/^End Date/}).click();
    await page.getByRole('button',{name:'20',exact:true}).click();
    await page.getByRole('button',{name:'Apply filters'}).click();
    await expect.poll(() => requests.at(-1)?.startDate?.slice(-2)).toBe('20');
    expect(requests.at(-1).endDate).toBe(requests.at(-1).startDate);
    await page.getByRole('button',{name:/^Remove From:/}).click();
    await expect.poll(() => requests.at(-1)?.startDate).toBeUndefined();
    expect(requests.at(-1).endDate).toBeTruthy();
    await page.getByRole('button',{name:'Filters',exact:true}).click();
    await page.getByRole('button',{name:'Billing Status',exact:true}).click();
    await page.getByText('Denied',{exact:true}).last().click();
    await page.getByRole('button',{name:'Close filters'}).click();
    expect(requests.at(-1).status).toBeUndefined();
    await page.getByRole('button',{name:'Filters',exact:true}).click();
    await page.getByRole('button',{name:'Billing Status',exact:true}).click();
    await page.getByText('Pending',{exact:true}).last().click();
    await page.getByRole('button',{name:'Payment Status',exact:true}).click();
    await page.getByText('Paid',{exact:true}).last().click();
    await page.getByRole('button',{name:'Apply filters'}).click();
    await expect.poll(() => requests.at(-1)?.status).toBe('pending');
    // Each active status must reach the API independently; currently paymentStatus is lost.
    expect.soft(requests.at(-1).paymentStatus, 'payment status is not silently discarded').toBe('paid');
  });
}

test('Mark as Paid opens payment recording instead of changing status', async ({page}) => {
  await render(page, 'billingLinks', {role: 'admin', records: [{id: 1, clientId: 42, clientName: 'Synthetic Client', totalAmount: 100, amountDue: 100, billingStatus: 'pending'}]});
  await page.getByRole('row').last().getByRole('button').last().click();
  await page.getByRole('button', {name: 'Change Status', exact: true}).click();
  await page.getByRole('button', {name: 'Mark as Paid', exact: true}).click();
  await expect.poll(() => events(page)).toEqual([{payment: '1'}]);
});
