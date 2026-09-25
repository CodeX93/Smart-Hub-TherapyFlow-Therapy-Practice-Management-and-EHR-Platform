import { built, journey, note } from '../lib/journey.mjs';
import { createOrganisation } from './organisation.mjs';
import { ensureAdministratorSignedIn } from './administrator.mjs';
import { createRoom } from './rooms.mjs';
import { saveCompanyDetails } from './company.mjs';
import { createService } from './services.mjs';
import { createTherapist, fillProfessionalProfile } from './therapists.mjs';
import { createClient } from './clients.mjs';
import { scheduleSession } from './sessions.mjs';

/**
 * Each flow says what has to exist before it can run, and this builds whatever is
 * missing — nothing more. So a single flow can be run on its own and still finds a
 * tenant, an administrator and the records it needs, while a full run, or a run
 * resumed against an earlier tenant, finds them already there and skips straight on.
 */
const build = {
  async organisation() {
    if (built('organisation')) return;
    await createOrganisation();
  },

  async administrator() {
    await need('organisation');
    await ensureAdministratorSignedIn();
  },

  async company() {
    await need('administrator');
    if (built('company')) return;
    await saveCompanyDetails();
  },

  async room() {
    await need('administrator');
    if (built('room')) return;
    await createRoom();
  },

  async service() {
    await need('administrator');
    if (built('service')) return;
    await createService();
  },

  async therapist() {
    await need('administrator');
    if (built('therapist')) return;
    await createTherapist();
  },

  /** The professional profile carries the working hours and the room a session needs. */
  async profile() {
    await need('room', 'therapist');
    if (built('profile')) return;
    await fillProfessionalProfile();
  },

  async client() {
    await need('therapist');
    if (built('client')) return;
    await createClient();
  },

  async session() {
    await need('service', 'profile', 'client');
    if (built('session')) return;
    await scheduleSession();
  },
};

export async function need(...what) {
  for (const thing of what) {
    const make = build[thing];
    if (!make) throw new Error(`No flow knows how to build "${thing}"`);
    await make();
  }
}

/** Says, once, what a flow had to build before it could start. */
export async function needing(...what) {
  const before = Object.keys(journey.state.created).length;
  await need(...what);
  const after = Object.keys(journey.state.created);
  if (after.length > before) note(`prepared ${after.join(', ')} for this flow`);
}
