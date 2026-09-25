import fs from 'node:fs';
import path from 'node:path';
import { test as base, expect } from '@playwright/test';
import { browserContextOptions, config, stateDir } from './config.mjs';
import { createMailbox } from './mailbox.mjs';
import { client, org, room, runTag, therapist } from './fixtures.mjs';

/**
 * One browser, one mailbox and one record of what has been built, shared by every
 * flow in the run. Whether the run is the whole journey or a single flow, each
 * spec finds the same journey here and adds to it.
 */

/** Resume mode: reuse the tenant an earlier run created, instead of building it again. */
export const resuming = process.env.E2E_RESUME === '1';

export const journey = {
  context: null,
  page: null,
  mailbox: null,
  /** Which account the browser is currently signed in as, so a flow can tell. */
  signedInAs: null,
  state: {
    runTag,
    /** Set false by any failing step, so cleanup refuses to remove a tenant worth keeping. */
    everyStepPassed: true,
    baseUrl: config.baseUrl,
    organisation: org,
    therapist: { name: therapist.name, email: therapist.email, username: therapist.username },
    room,
    client: { name: client.name, email: client.email },
    adminPassword: '',
    created: {},
    secondFactor: {},
    notes: [],
  },
  /** What was typed into each record, so the reopened form can be compared against it. */
  entered: { user: {}, profile: {}, client: {}, session: {}, note: {}, company: {} },
};

export function note(message) {
  journey.state.notes.push(message);
  test.info().annotations.push({ type: 'journey', description: message });
  console.log(`  · ${message}`);
}

const statePath = () => path.join(stateDir, `${runTag}.json`);

export function saveRunState() {
  fs.mkdirSync(stateDir, { recursive: true });
  fs.writeFileSync(
    statePath(),
    JSON.stringify({ ...journey.state, entered: journey.entered }, null, 2),
  );
}

/** True once the named record exists, in this run or in the run being resumed. */
export function built(what) {
  return Boolean(journey.state.created[what]);
}

export function markBuilt(what) {
  journey.state.created[what] = true;
  saveRunState();
}

async function openJourney(browser) {
  journey.browser = browser;
  journey.context = await browser.newContext(browserContextOptions);
  journey.page = await journey.context.newPage();
  journey.mailbox = createMailbox();
  fs.mkdirSync(stateDir, { recursive: true });
  // Whatever this tag has already built is picked up again — whether that is an
  // earlier run being resumed, or this run's own worker having been restarted.
  if (fs.existsSync(statePath())) {
    const saved = JSON.parse(fs.readFileSync(statePath(), 'utf8'));
    Object.assign(journey.state, saved, { notes: [] });
    Object.assign(journey.entered, saved.entered ?? {});
    // Asking to resume is asking for another go at what failed, so the verdict on
    // whether this run may remove the tenant starts over. A worker that merely
    // restarted mid-run keeps the earlier verdict, and cleanup stays refused.
    if (resuming) journey.state.everyStepPassed = true;
  } else if (resuming) {
    throw new Error(`E2E_RESUME=1 was asked for, but ${statePath()} does not exist.`);
  }
}

async function closeJourney() {
  saveRunState();
  await journey.mailbox?.close();
  await journey.context?.close();
  journey.context = null;
  journey.page = null;
}

/**
 * Specs import this `test` rather than Playwright's own, so every one of them shares
 * the same browser and the same record of what the run has built. The journey is set
 * up once per worker, and torn down when the worker finishes — not once per file.
 */
export const test = base.extend({
  journey: [
    async ({ browser }, use) => {
      await openJourney(browser);
      await use(journey);
      await closeJourney();
    },
    { scope: 'worker', auto: true },
  ],

  // Records the outcome of every step, so a failed run can be resumed from it.
  recordStep: [
    async ({}, use) => {
      await use();
      const info = test.info();
      if (info.status !== info.expectedStatus) journey.state.everyStepPassed = false;
      saveRunState();
    },
    { auto: true },
  ],
});

export { expect };
