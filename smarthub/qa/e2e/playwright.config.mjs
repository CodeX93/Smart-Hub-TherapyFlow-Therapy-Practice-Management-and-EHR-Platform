import { defineConfig } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { browserContextOptions, config } from './lib/config.mjs';

// Playwright reloads this config in every worker it starts, and starts a fresh one
// after a failed hook. Settling the run tag here, once, keeps all of them on the same
// tenant instead of each building its own.
if (!process.env.E2E_RUN_TAG) process.env.E2E_RUN_TAG = `e2e${Date.now().toString(36)}`;
if (!process.env.E2E_RUN_ID) process.env.E2E_RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');

const e2eDir = path.dirname(fileURLToPath(import.meta.url));
const runId = process.env.E2E_RUN_ID;
const output = path.join(e2eDir, '..', '..', 'qa-results', `e2e-${runId}`);

/**
 * One project per flow, so a flow can be run on its own — `--project=client` builds
 * only what a client needs and then exercises the client. `full` is the whole
 * journey in order; `create` builds everything without reading any of it back.
 * Every flow ends with cleanup, unless E2E_CLEANUP=0 keeps the tenant for a resumed run.
 */
const flow = (name, tag) => ({
  name,
  grep: new RegExp(`${tag}|@cleanup`),
});

const flows = [
  { name: 'full' },
  // Everything the product needs built, with none of the read-back checks.
  { name: 'create', grepInvert: /@verify/ },
  // Only the read-back checks, against a tenant an earlier run left behind.
  { name: 'verify', grep: /@verify/ },
  flow('organisation', '@organisation'),
  flow('administrator', '@administrator'),
  flow('company', '@company'),
  flow('options', '@options'),
  flow('room', '@room'),
  flow('service', '@service'),
  flow('therapist', '@therapist'),
  flow('client', '@client'),
  flow('session', '@session'),
  flow('notifications', '@notifications'),
  flow('notes', '@notes'),
  flow('tasks', '@tasks'),
  flow('users', '@users'),
  flow('dashboard', '@dashboard'),
  flow('compliance', '@compliance'),
  flow('auth', '@authcookie'),
  flow('isolation', '@isolation'),
  { name: 'cleanup', grep: /@cleanup/ },
];

/**
 * Playwright runs every project when none is named, which here would mean running
 * the journey once per flow. Asking for no flow in particular means the whole one.
 * Only the command line carries `--project`, and the workers do not see it, so the
 * answer is settled once here and passed on the same way the run tag is.
 */
if (!process.env.E2E_NAMED_FLOW) {
  const named = process.argv.some((arg) => arg === '--project' || arg.startsWith('--project='));
  process.env.E2E_NAMED_FLOW = named ? '1' : '0';
}
const namesAFlow = process.env.E2E_NAMED_FLOW === '1';

export default defineConfig({
  testDir: path.join(e2eDir, 'specs'),
  testMatch: '**/*.spec.mjs',
  // A live journey talks to real mail and a real backend; give each step room.
  timeout: Number(process.env.E2E_TEST_TIMEOUT_MS || 240000),
  expect: { timeout: 20000 },
  workers: 1,
  fullyParallel: false,
  retries: 0,
  forbidOnly: true,
  projects: namesAFlow ? flows : [flows[0]],
  reporter: [
    ['list'],
    ['json', { outputFile: path.join(output, 'results.json') }],
    ['html', { outputFolder: path.join(output, 'report'), open: 'never' }],
  ],
  outputDir: path.join(output, 'artifacts'),
  use: {
    ...browserContextOptions,
    browserName: 'chromium',
    headless: process.env.E2E_HEADED !== '1',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
});
