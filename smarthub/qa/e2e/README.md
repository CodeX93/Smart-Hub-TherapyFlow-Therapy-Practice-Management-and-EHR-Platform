# Live enterprise lifecycle suite

Playwright flows that behave like a person using the product, against a **running**
environment — local first, then production. Nothing is seeded through a database or a test
hook: the super admin signs in, creates an enterprise tenant, the new administrator reads
their credentials out of a real mailbox, clears real multi-factor authentication, staffs the
clinic, adds the service it sells, takes on a client, books a session, and the tenant is
removed again once every step has passed.

Each stage also checks the refusal it owes the user: a wrong password, a wrong verification
code, an incomplete form, a duplicate slug, a duplicate room number, a duplicate staff email,
a short password, a malformed client email, a duplicate service code, a day the therapist
does not work, and an administrator reaching for the platform console.

## How it is put together

| Folder | What lives there |
| --- | --- |
| `lib/` | the browser, the mailbox, the shared journey, and the form helpers |
| `flows/` | what a person *does* — create an organisation, staff it, book a session |
| `specs/` | what the product *owes* — one file per flow, each with its own checks |

A flow says what has to exist before it can run (`flows/prerequisites.mjs`) and the suite
builds only that. So the client flow on its own still finds a tenant, an administrator and a
therapist, without repeating the checks that belong to those flows.

This suite is separate from `qa/browser`, which runs against the disposable local harness with
outbound mail blocked. This one needs the real thing.

## Configure

```sh
cp qa/e2e/.env.e2e.example qa/e2e/.env.e2e
```

Fill it in. `qa/e2e/.env.e2e` and `qa/e2e/.state/` are gitignored.

| Setting | What it is |
| --- | --- |
| `E2E_BASE_URL` | `http://localhost:3000` or `https://app.therapyflow.pro` |
| `E2E_SUPERADMIN_USER` / `E2E_SUPERADMIN_PASSWORD` | platform super-admin sign-in |
| `E2E_MAILBOX_ADDRESS` / `E2E_IMAP_PASSWORD` | the mailbox the suite reads codes from. For Gmail this is an **app password**, not the account password |
| `E2E_SUPERADMIN_MAIL_ADDRESS` | only if the super admin's codes arrive at a different address |
| `E2E_ORG_ADMIN_PASSWORD` | fallback if the onboarding email's password cannot be parsed; must equal the backend's `SUPERADMIN_ONBOARDING_DEFAULT_ADMIN_PASSWORD` |
| `E2E_ADMIN_PHONE` | optional. With it the suite also enrolls SMS as a second factor; without it that step reports as skipped |
| `E2E_CLEANUP=0` | keeps the tenant after a green run |

Every run books its own plus-address (`you+e2eab12@gmail.com`) for the new administrator, the
therapist and the client, so runs never read each other's mail.

## Environment prerequisites

The backend must be able to **send mail** and must have an onboarding password configured,
or the journey cannot start. Production already has both. A backend started from
`.local-db/run-local.env` has neither.

### Local: a mail catcher instead of a real inbox

```sh
docker run -d --rm --name smarthub-e2e-mail \
  -p 127.0.0.1:1025:1025 -p 127.0.0.1:8025:8025 axllent/mailpit:latest
```

Then start the backend with the local dev environment plus:

```sh
PORT=8081
SUPERADMIN_ONBOARDING_DEFAULT_ADMIN_PASSWORD=QaOnboard!2026
EMAIL_PROVIDER=smtp
SPRING_MAIL_HOST=127.0.0.1
SPRING_MAIL_PORT=1025
SPRING_MAIL_FROM=no-reply@smarthub.qa.local
SPRING_MAIL_SMTP_AUTH=false
SPRING_MAIL_SMTP_STARTTLS_ENABLE=false
SPRING_MAIL_SMTP_STARTTLS_REQUIRED=false
CORS_ALLOWED_ORIGINS='http://localhost:4300,http://127.0.0.1:4300,http://*.localhost:4300'
```

`EMAIL_PROVIDER=smtp` leaves the SES/SparkPost provider beans out, so `EmailService` falls
back to `JavaMailSender` and the configured SMTP host actually delivers. Without it a local
run fails at the first multi-factor code, because nothing is sent. `CORS_ALLOWED_ORIGINS`
matters because the default list only covers ports 3000, 5173 and 8080 — a dev server on any
other port gets "Unable to reach the server" on the sign-in screen.

The frontend runs from the sibling checkout with `VITE_API_BASE_URL` pointing at that backend:

```sh
cd ../trappy-flow-frontend && npm run dev -- --port 4300 --strictPort
```

With `E2E_MAILPIT_URL` set, the suite reads Mailpit's HTTP API and needs no mail password at
all. Leave it unset on production and give it IMAP settings instead.

### Multi-factor and the super-admin account

Enforcement is `ALL`, so every account is challenged. The suite can only drive an account
whose second factor is **email** — an account already enrolled on TOTP needs an authenticator
app and cannot be automated. A super admin with no confirmed factor is fine: the suite enrolls
email on the first sign-in, and the app then asks it to sign in again, which it does.

SMS needs a configured Twilio account on the target environment. When it is missing, the SMS
enrollment step records why and the journey continues on email.

## Run

```sh
npm run test:app:install   # once, installs Chromium
npm run test:e2e           # the whole journey, in order
```

### One flow at a time

Every flow is a project, so name the one you want. Each builds only what it needs first, and
clears the tenant away afterwards:

```sh
npm run test:e2e -- --project=organisation    # the platform console and a new tenant
npm run test:e2e -- --project=administrator   # credentials, activation, second factor
npm run test:e2e -- --project=room            # a therapy room
npm run test:e2e -- --project=service         # the clinic's own service and its price
npm run test:e2e -- --project=therapist       # the account and the whole professional profile
npm run test:e2e -- --project=client          # the intake wizard and the therapist assignment
npm run test:e2e -- --project=session         # booking against the therapist's working hours
npm run test:e2e -- --project=isolation       # what a tenant admin must not see
npm run test:e2e -- --project=cleanup         # terminate a tenant an earlier run left
```

### Building without reading it all back

`create` fills everything in and skips every "does it come back?" check; `verify` is only
those checks, against a tenant that already exists:

```sh
E2E_CLEANUP=0 npm run test:e2e -- --project=create
E2E_RUN_TAG=<tag from that run> E2E_RESUME=1 npm run test:e2e -- --project=verify
```

Useful variants:

```sh
E2E_HEADED=1 npm run test:e2e                       # watch it happen
E2E_CLEANUP=0 npm run test:e2e                      # keep the tenant afterwards
npm run test:e2e -- --project=client --grep @negative   # only the refusals a flow owes
```

A flow run on its own has to build its own tenant, which is the slow part. To run one against
a tenant that already exists, resume into it — see below — and it starts in seconds.

## Running against production

Production settings live in their own gitignored file, so the local ones are left alone:

```sh
E2E_ENV_FILE=.env.e2e.prod npm run test:e2e
```

That file needs a real mailbox the suite can read over IMAP — for Gmail an **app
password**, not the account password — and a super admin whose second factor is email
rather than an authenticator app. Leave `E2E_MAILPIT_URL` out of it: the catcher is local.

Know what a production run leaves behind. It creates a real enterprise tenant, sends real
onboarding and sign-in mail, and at the end schedules that tenant for termination — which
is not a purge (see the last section), so the tenant stays. Give it its own run first with
`--project=organisation` if you would rather see one tenant created than the whole journey.

### When the codes cannot be read

An account whose sign-in codes reach no mailbox the suite can open — a suppressed
address, a shared inbox, an authenticator app — is answered by hand:

```sh
E2E_SUPERADMIN_MFA=TOTP      # answer with the authenticator instead of email
E2E_TRUST_DEVICE=1           # keep the trust the app hands back, so only run one is challenged
E2E_WRONG_CODE_PROBE=0       # leave out the deliberately wrong code, on an account that may lock
```

The run stops and prints where to write the code:

```sh
echo 123456 > qa/e2e/.state/mfa-code.txt
```

Send a code that has *just* appeared — it is used within a second or two of being
written, and a code already halfway through its thirty seconds is often refused by
the time it arrives. With `E2E_TRUST_DEVICE=1` this happens once: the token the app
returns is kept in `qa/e2e/.state/device-trust.json` and put back on the next run,
which then signs in without a challenge. That token is a real credential for that
account; the directory is gitignored and the trust expires on its own.

## Resuming a failed run

Each step writes `qa/e2e/.state/<run-tag>.json` with the tenant it built and every value it
typed, so a failed run can be continued instead of repeated:

```sh
E2E_RUN_TAG=<tag from the failed run> E2E_RESUME=1 npm run test:e2e
# or just the flow you fixed:
E2E_RUN_TAG=<tag> E2E_RESUME=1 npm run test:e2e -- --project=client
```

A resumed run signs in with the password the activation step already set, and skips building
anything the earlier run finished, while every verification step reads what that run recorded.
Finish with one ordinary full run.

Reports land in `qa-results/e2e-<timestamp>/` — `report/index.html`, `results.json`, and
traces, screenshots and video for anything that failed. The run also writes
`qa/e2e/.state/<run-tag>.json` with the tenant, staff and client it created, and the notes it
recorded along the way.

Steps within a flow run in order and every flow shares one browser session; a failure stops
the rest of that flow. The tenant is
removed **only** when every earlier step passed — a failed run deliberately leaves the
organisation in place so it can be inspected.

## Keep the local database small

Every organisation create makes the backend walk **every** `tenant_*` schema, so a local
database that has collected test tenants (or orphan schemas from earlier deletions) turns a
two-second create into a multi-minute one. When creates start crawling, purge back to the one
real tenant:

```sh
export PGHOST=127.0.0.1 PGPORT=5433 PGDATABASE=therapyflow PGUSER=therapyflow PGSSLMODE=disable
export PGPASSWORD=...
./scripts/purge-demo-organisations.sh --dry-run   # read the list first
./scripts/purge-demo-organisations.sh --confirm
```

It keeps `KEEP_ID` (50 by default) and drops everything else, including orphan schemas.

## Known limitation: termination is not a purge

The final step uses the product's own **Terminate Tenant** action and asserts what the product
reports: the tenant moves to *Termination Scheduled*. That is not a hard delete. The platform
purge job cannot complete for an organisation that already has platform audit rows, which is
every organisation in practice, so the tenant schema survives. Removing the data for real
still needs the separate purge SQL. Treat the green run as "the tenant was scheduled for
termination", not "the tenant is gone".
