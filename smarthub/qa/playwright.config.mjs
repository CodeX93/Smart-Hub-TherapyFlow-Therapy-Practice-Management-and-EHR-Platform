import { defineConfig } from '@playwright/test';
import path from 'node:path';
const output = process.env.QA_RUN_DIR;
if (!output) throw new Error('Use scripts/test-app.sh; QA_RUN_DIR is required.');
export default defineConfig({
  testDir: './browser', timeout: 60000, expect: { timeout: 12000 }, workers: 1,
  fullyParallel: false, retries: 0, forbidOnly: true,
  reporter: [['list'], ['json', { outputFile: path.join(output, 'playwright.json') }], ['html', { outputFolder: path.join(output, 'playwright'), open: 'never' }]],
  outputDir: path.join(output, 'browser-artifacts'),
  use: { baseURL: process.env.QA_FRONTEND_URL, browserName: 'chromium', headless: true,
    viewport: { width: 1440, height: 1000 }, serviceWorkers: 'block',
    trace: 'retain-on-failure', screenshot: 'only-on-failure', video: 'retain-on-failure' },
});
