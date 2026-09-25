import { config, plusAddress } from './config.mjs';

/**
 * Everything one run works with, derived from a single tag so that two runs never
 * collide and a resumed run rebuilds exactly the same names it used before.
 */
export const runTag = (process.env.E2E_RUN_TAG || `e2e${Date.now().toString(36)}`).toLowerCase();
export const stamp = runTag.slice(-6);

/** The password the administrator sets for themselves on the activation screen. */
export const adminChosenPassword = `Qa!${stamp}Admin1`;

export const org = {
  name: `QA Enterprise ${runTag}`,
  slug: `qa-${runTag}`,
  adminFirstName: 'Quinn',
  adminLastName: 'Auditor',
  adminEmail: plusAddress(config.mailbox.address, runTag),
};

export const therapist = {
  name: `QA Therapist ${stamp}`,
  email: plusAddress(config.mailbox.address, `${runTag}-therapist`),
  username: `qa-ther-${stamp}`,
  phone: '+14155550123',
  password: `Qa!${stamp}Ther1`,
};

export const room = {
  number: `QA-${stamp}`,
  name: `QA Room ${stamp}`,
  capacity: '4',
  equipment: 'Two chairs, whiteboard',
};

export const client = {
  name: `QA Client ${stamp}`,
  email: plusAddress(config.mailbox.address, `${runTag}-client`),
};

/** The clinic's own company details, saved under Settings → Administration. */
export const company = {
  name: `QA Practice ${stamp}`,
  subtitle: 'Counselling and assessment',
  taxId: '12-3456789',
  npi: '1234567890',
  licenseNumber: `PRC-${stamp.toUpperCase()}`,
  licenseState: 'Ontario',
  description: `Synthetic QA practice ${stamp}`,
  address: '77 QA Avenue, Toronto',
  phone: '+14155550199',
  email: plusAddress(config.mailbox.address, `${runTag}-practice`),
  website: `https://qa-${stamp}.example.com`,
};

/**
 * The clinic's own service. The seeded Consultation service is bound to the public
 * site's consultation hours, so an ordinary booking needs a service of its own.
 */
export const service = {
  code: `QA-${stamp.toUpperCase()}`,
  name: `QA Therapy Hour ${stamp}`,
  description: 'Standard QA session',
  duration: '50',
  rate: '120.00',
};

/**
 * The therapist's working day is Monday, so a session has to land on a Monday for
 * the availability search to offer any times at all.
 */
export const session = {
  weekday: 'Monday',
  notes: `QA session note ${stamp}`,
};

export const superAdminMailAddress =
  process.env.E2E_SUPERADMIN_MAIL_ADDRESS
  || (config.superAdmin.username.includes('@') ? config.superAdmin.username : config.mailbox.address);

export const task = {
  title: `QA Task ${stamp}`,
  description: 'Confirm the intake paperwork arrived before the first session.',
  discardTitle: `QA Task ${stamp} discard`,
  comment: `Waiting on the intake paperwork (${stamp}).`,
  commentEdited: `Intake paperwork arrived, filing it (${stamp}).`,
  therapistComment: `Reviewed and scheduled the follow-up (${stamp}).`,
};

export const extraUser = {
  name: `QA Supervisor ${stamp}`,
  renamed: `QA Supervisor ${stamp} Sr`,
  email: plusAddress(config.mailbox.address, `${runTag}-supervisor`),
  username: `qa-sup-${stamp}`,
  phone: '+14155550188',
  password: `Qa!${stamp}Sup1`,
};

export const role = {
  name: `qa_role_${stamp}`,
  displayName: `QA Role ${stamp}`,
  renamed: `QA Role ${stamp} v2`,
  description: 'Sees clients, changes nothing else.',
};
