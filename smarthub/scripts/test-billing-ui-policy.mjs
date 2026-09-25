// Exercises the actual frontend helpers; no browser, network, or production data.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const frontend = resolve(process.argv[2] || '../trappy-flow-frontend');
const require = createRequire(resolve(frontend, 'package.json'));
const ts = require('typescript');
const luxonUrl = pathToFileURL(require.resolve('luxon')).href;
async function loadHelper(name) {
  const source = readFileSync(resolve(frontend, 'src/utils', name), 'utf8');
  const compiled = ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext },
  }).outputText.replaceAll('"luxon"', JSON.stringify(luxonUrl));
  return import('data:text/javascript;base64,' + Buffer.from(compiled).toString('base64'));
}
const transitions = await loadHelper('sessionStatusTransitions.ts');
const billing = await loadHelper('sessionBillingUi.ts');
let checks = 0;
function check(actual, expected) { assert.deepEqual(actual, expected); checks++; }
for (const [zone, localTime, instant] of [
  ['America/Toronto', '2026-09-10T09:00:00', '2026-09-10T13:00:00Z'],
  ['America/Toronto', '2026-01-10T09:00:00', '2026-01-10T14:00:00Z'],
  ['Asia/Karachi', '2026-09-10T09:00:00', '2026-09-10T04:00:00Z'],
  ['UTC', '2026-09-10T09:00:00', '2026-09-10T09:00:00Z'],
]) {
  for (const delta of [-1, 0, 1]) {
    const now = new Date(Date.parse(instant) + delta);
    const allowed = transitions.getAllowedAppointmentStatuses('scheduled', {
      scheduledAt: localTime, practiceTimezone: zone, now,
    });
    check(allowed.includes('completed'), delta > 0);
    check(allowed.includes('noshow'), delta > 0);
    check(transitions.hasSessionScheduledTimePassed(instant, now, zone), delta > 0);
    for (const status of ['completed', 'no_show', 'no-show']) {
      check(billing.isSessionEligibleForManualBilling({ status, sessionDate: localTime }, now, zone), delta > 0);
    }
    for (const status of ['scheduled', 'cancelled']) {
      check(billing.isSessionEligibleForManualBilling({ status, sessionDate: localTime }, now, zone), false);
    }
    check(billing.isSessionEligibleForManualBilling({ status: 'completed', sessionDate: localTime, billingId: 1 }, now, zone), false);
    check(transitions.getAllowedAppointmentStatuses('scheduled', {
      scheduledAt: localTime, practiceTimezone: zone, now, hasInvoice: true,
    }).includes('cancelled'), false);
  }
}
for (const value of [null, '', 'invalid-date']) {
  check(transitions.hasSessionScheduledTimePassed(value), false);
  check(billing.isSessionEligibleForManualBilling({ status: 'completed', sessionDate: value }), false);
}
console.log(`PASS: ${checks} frontend billing/timezone assertions`);
