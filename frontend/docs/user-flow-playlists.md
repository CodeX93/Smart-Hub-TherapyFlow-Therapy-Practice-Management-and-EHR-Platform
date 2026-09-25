# TherapyFlow Learning Hub — Full Chapters, Sections & Articles

This document is the **complete source content** for TherapyFlow Learning Hub.

Use it exactly as structured below when creating Learning Hub content:

| Learning Hub level | Meaning in this doc |
|--------------------|---------------------|
| **Chapter** | `# Chapter N — …` |
| **Section** | `## Section N.N — …` |
| **Article** | `### Article: …` (title + full body below it) |

**Audience:** Org Admin · Therapist · Custom Staff · Client (patient portal)

**Out of scope:** Super Admin / platform hosting / tenant provisioning (do not publish platform-admin material in this Learning Hub set).

**Rule for authors:** Every button, tab, menu label, modal title, and route below matches the current product UI. Do not paraphrase UI labels when teaching clicks.

---

## Learning Hub publish map

| Order | Chapter | Who it is for |
|------:|---------|----------------|
| 1 | Getting Started | Everyone |
| 2 | Clinic Setup (Admin) | Org Admin |
| 3 | People & Access (Admin) | Org Admin |
| 4 | Clients & Portal Access | Org Admin (Therapist overlapping articles in Ch. 8) |
| 5 | Scheduling & Sessions | Org Admin (Therapist overlapping in Ch. 8) |
| 6 | Billing & Payments | Org Admin / Therapist with billing access |
| 7 | Tasks, Content & Compliance | Org Admin |
| 8 | Therapist Workspace | Therapist |
| 9 | Client Portal | Client |
| 10 | Staff (Custom Roles) | Custom staff / supervisors |

**Suggested new-clinic reading order**

1. Chapter 1 — Getting Started  
2. Chapter 2 — Clinic Setup (services, rooms, policies, Stripe)  
3. Chapter 3 — Create therapist + (optional) admin-set schedule  
4. Chapter 4 — Create client; decide portal access yes/no  
5. Chapter 5 — Schedule first appointment  
6. Chapter 8 — Therapist sets schedule / works with clients  
7. Chapter 9 — Client activates portal, books, pays  
8. Chapters 6–7 — Billings, tasks, content as day-to-day needs grow  
9. Chapter 10 — Only if the clinic uses custom staff roles  

---

# Chapter 1 — Getting Started

**Chapter purpose:** Teach every user how to sign in correctly, complete first-time password setup, and recover a forgotten password—without mixing staff and client login portals.

---

## Section 1.1 — Sign in correctly

### Article: Who signs in where

**What you will learn**

TherapyFlow has two separate login portals. Using the wrong portal is the most common first-day mistake.

**Sign-in map**

| Who you are | Open this URL | After a successful login you land on |
|-------------|---------------|--------------------------------------|
| Organisation Admin | `/auth/staff/login` | `/admin/dashboard` |
| Therapist | `/auth/staff/login` | `/therapist/dashboard` |
| Custom staff (role-based) | `/auth/staff/login` | The first page your permissions allow under `/staff/...` |
| Client / patient | `/auth/login` | `/user/appointments` |

**Important rules**

- Admins, therapists, and staff **must** use the staff login (`/auth/staff/login`).  
- Clients **must** use the client login (`/auth/login`).  
- A client account will **not** successfully use the staff portal for normal work.  
- A staff/therapist account will **not** use the client portal as its home workspace.  
- If your organisation has more than one tenant context, the staff login may ask you to confirm which organisation/context applies before continuing.

**Tip for trainers**

Print or pin this table for clinic administrators so they can send the correct link in welcome emails.

---

### Article: First login — set your password (staff / admin / therapist)

**When to use this article**

Use this when a new Admin, Therapist, or Staff user receives a temporary password (for example after an Admin creates the user with an initial password, or after org onboarding creates the primary admin).

**Goal**

Complete the required password change, then sign in for the first time with your own password.

**Step-by-step**

1. Open the staff login page: `/auth/staff/login`.  
2. Enter the **email** (or username, if your clinic uses that on the same field) and the **temporary password** provided by your administrator.  
3. Submit the login form (**Sign In**).  
4. If the system requires a password change (`passwordChangeRequired`), TherapyFlow redirects you to:  
   `/auth/staff/activate-your-account`  
   Page title: **Set your password**.  
5. Enter your new password.  
6. Confirm the new password.  
7. Click **Set password**.  
   - While saving, the button may show **Saving...**.  
8. You are taken to the success page: `/auth/staff/password-reset-success` (Password Reset Successful).  
9. Click **Back to login**.  
10. Sign in again at `/auth/staff/login` using your **new** password.  
11. Confirm you land on the correct home for your role (Admin dashboard, Therapist dashboard, or first Staff page).

**Do not confuse these two paths**

| Situation | Correct path |
|-----------|--------------|
| First login / must set password before session starts | `/auth/staff/activate-your-account` (**Set your password**) |
| Forgot password later (you already had an account and lost access) | `/auth/staff/forgot-password` → `/auth/staff/set-new-password?token=...` |

**Trainer note**

On first-login activation, TherapyFlow does **not** keep you fully signed in until after you complete the password change and sign in again.

---

### Article: Forgot password (Admin / Therapist / Staff)

**Goal**

Reset a forgotten staff-side password using email verification.

**Step-by-step**

1. Open `/auth/staff/forgot-password` (page title context: **Reset Password**).  
2. Enter the email associated with your staff account.  
3. Submit the form so TherapyFlow can send a reset email (the app resolves organisation/login context as needed).  
4. You are shown `/auth/staff/check-email` — open your inbox and look for the reset message.  
5. Click the reset link in the email. It opens something like:  
   `/auth/staff/set-new-password?token=...`  
6. Enter a new password and confirm it.  
7. Submit the reset form.  
8. Success page: `/auth/staff/password-reset-success`.  
9. Click **Back to login** and sign in at `/auth/staff/login` with the new password.

**If the email does not arrive**

- Check spam/junk.  
- Confirm the email address used on your user profile is correct.  
- Ask an Admin to verify your user is Active under **User & Access → User Profiles**.

---

### Article: Forgot password (Client / patient)

**Goal**

Reset a forgotten portal password for a client who already has portal access.

**Step-by-step**

1. Open `/auth/forgot-password`.  
2. Enter the portal email (usually the client’s primary email).  
3. Continue to `/auth/check-email` and open the email.  
4. Open the reset link: `/auth/set-new-password?token=...`.  
5. Set a new password and confirm.  
6. Return to `/auth/login` and sign in.

**If reset does not work**

- Confirm the clinic has **Enable Portal Access** turned on for this client.  
- Ask Admin/Therapist to open the client **Overview → Portal Access Management** and use **Resend Activation Email** if the account was never activated.

---

# Chapter 2 — Clinic Setup (Admin)

**Chapter purpose:** Make the clinic operational before clients and sessions exist—practice details, catalogs, priced services, rooms, invoice rules, client payment integration, and the organisation’s TherapyFlow subscription.

**Primary navigation for this chapter**

Sidebar → **System** → **Settings** → `/admin/settings`

Settings tabs (exact labels):

- **System Options**  
- **Service Prices**  
- **Service Visibility**  
- **Invoice Policies**  
- **Payment Integration**  
- **Subscription**  
- **Therapy Rooms**  
- **Library Categories**  
- **Administration**

You can deep-link tabs with `?tab=` values such as `service-prices`, `therapy-rooms`, `stripe-connect`, `subscription-billing`, `invoice-policies`, `system-options`, `administration`, `library-categories`, `service-visibility`.

---

## Section 2.1 — Practice foundations

### Article: Configure practice details (Administration)

**Goal**

Set the clinic’s identity and contact details used across the product.

**Prerequisites**

- Signed in as Org Admin.  
- Access to **System → Settings**.

**Step-by-step**

1. Go to Sidebar → **System** → **Settings** (`/admin/settings`).  
2. Open the **Administration** tab.  
3. In **Practice Configuration**, fill the available practice fields (practice name, subtitle/description if shown, address, phone, email, website, timezone, and any other fields present).  
4. Click **Save Configuration**.  
5. Confirm the values remain after refresh (saving successful).

**Why this matters**

Practice details support communication, branding context, and timezone-aware scheduling expectations.

---

### Article: Manage System Options (dropdown catalogs)

**Goal**

Maintain the dropdown option lists used by client forms, session types, and other clinic selectors.

**Step-by-step**

1. Open `/admin/settings` → tab **System Options**.  
2. Review existing categories.  
3. To add a category, click **Add Category** and complete the category form.  
4. Inside a category, click **Add New Option** to create an option value.  
5. Edit an option when wording must change.  
6. Delete options that are no longer used (only if safe for existing records).  
7. Reorder options if the UI provides reorder controls, so the most common values appear first.

**Trainer tip**

Create session-type and related clinical options **before** staff start scheduling heavily, so “Session Type” in Schedule New Session has useful values.

---

### Article: Create Library Categories

**Goal**

Define the taxonomy used by the clinical content library.

**Step-by-step**

1. Open Settings → **Library Categories**.  
2. Click **Add Category**.  
3. Enter category details.  
4. Submit with **Add Category** (or **Update Category** when editing).  
5. Confirm the category appears in the list and later in Content → Library filters/tabs where applicable.

---

## Section 2.2 — Services and rooms

### Article: Add service codes and prices

**Goal**

Create billable/bookable services with duration and pricing so scheduling and invoicing have valid services to select.

**Step-by-step**

1. Open Settings → **Service Prices** (`/admin/settings?tab=service-prices`).  
2. Click **Add Service Code**.  
3. Fill the service modal fields (service name, duration, base rate/price, code/identifier, and any additional fields shown).  
4. Submit by clicking **Add Service Code**.  
5. Confirm the service appears in the Service Prices list.  
6. Repeat for each service your clinic offers (intake, follow-up, group, etc.).

**Related setup**

After creating services, immediately review **Service Visibility** so therapists and/or the client portal can see the right services.

---

### Article: Control which services therapists and clients can see

**Goal**

Decide which services appear for therapist booking workflows and which appear in the client portal booking UI.

**Step-by-step**

1. Open Settings → **Service Visibility**.  
2. For each service row, review visibility toggles:  
   - Therapist visibility  
   - Client portal visibility  
3. Turn ON only the services each audience should use.  
4. Save/apply changes if the UI requires an explicit save (follow on-screen controls).  
5. Test:  
   - As Admin/Therapist scheduling, confirm the **Service** dropdown shows the intended list.  
   - As a portal client booking, confirm **Select a service** only lists portal-visible services.

**Common mistake**

Clients cannot book a service that exists in Service Prices but is hidden from portal visibility.

---

### Article: Add therapy rooms

**Goal**

Create physical rooms that therapists can be assigned to, and that in-person sessions can reserve.

**Step-by-step**

1. Open Settings → **Therapy Rooms** (`/admin/settings?tab=therapy-rooms`).  
2. Click **Add Room**.  
3. Enter room details such as room number, room name, room type, and active status.  
4. Keep the room **active** if it should appear in room pickers.  
5. Submit **Add Room**.  
6. Confirm the room appears in the Therapy Rooms list.  
7. Later assign rooms to therapists through:  
   - Admin → User Profiles → **Professional Details** → **Schedule** → **Select Rooms**, or  
   - Therapist → **My Profile** → **Schedule** → **Select Rooms**.

**Why active rooms matter**

Inactive rooms will not be reliably selectable when setting schedules or booking in-person sessions.

---

## Section 2.3 — Invoice policies, Stripe, and subscription

### Article: Create invoice policies

**Goal**

Define automatic (or rule-driven) pricing behaviour based on client type, session status, and optional service/date constraints.

**Prerequisites**

- Billing module/feature enabled for the organisation plan.  
- User has billing manage capability.

**Step-by-step**

1. Open Settings → **Invoice Policies** (`?tab=invoice-policies`).  
2. Click **Add Policy**.  
3. Configure:  
   - Client type  
   - Session status  
   - Price type (fixed amount or percentage, depending on form)  
   - Price value  
   - Optional service link  
   - Optional effective date range  
4. Submit **Create policy**.  
5. To update later: open the policy → edit → **Save changes**.  
6. Use the active toggle to enable/disable a policy without deleting it.  
7. Delete only when the policy is obsolete.

**Trainer note**

Invoice policies work together with session completion/status changes and the Billings workspace (Chapter 6).

---

### Article: Connect Stripe for client invoice payments

**Goal**

Let clients pay invoices online through Stripe Connect for the clinic.

**Important distinction**

- **Payment Integration (Stripe Connect)** = clients paying clinic invoices.  
- **Subscription** tab = clinic paying TherapyFlow SaaS subscription.  
Do not mix the two.

**Step-by-step**

1. Open Settings → **Payment Integration** (`/admin/settings?tab=stripe-connect`).  
2. Read the description/helper text (Connect Stripe so clients can pay invoices online).  
3. Click **Connect Stripe**.  
4. Complete Stripe’s hosted OAuth/onboarding screens.  
5. Return to TherapyFlow (`/stripe/success` or settings with `?stripe=connected`).  
6. Verify connected status on the Payment Integration tab.  
7. Optional advanced setup: open **Advanced Stripe configuration**, enter allowed advanced fields (publishable key, secret, webhook URL/secret as shown), then **Save configuration**.  
8. Use **Refresh** if status looks stale. Use **Disconnect** only intentionally.

**After connecting**

Clients can use **Pay now** on portal invoices when the billing module and Connect status allow it.

---

### Article: Manage the clinic TherapyFlow subscription

**Goal**

Review and maintain the organisation’s TherapyFlow plan billing (trial, subscribe, pay invoice, manage payment method).

**Step-by-step**

1. Open Settings → **Subscription** (`?tab=subscription-billing`), or go to `/billing/subscription` / `/admin/billing/subscription`.  
2. Review current plan, trial remaining (if any), and any `nextAction` style guidance shown on screen.  
3. If trial/subscribe is required, use **Add payment method** / Subscribe actions as offered.  
4. If an unpaid invoice is waiting, use Pay invoice actions as offered.  
5. If management of payment method/invoices is available, open Manage billing / customer portal as offered.  
6. After Checkout, complete `/billing/subscription/success` (the page may poll until subscription is active).

**Trainer note**

Billing capability and plan features can restrict who can complete payment actions.

---

# Chapter 3 — People & Access (Admin)

**Chapter purpose:** Create roles, create therapist/staff users (with passwords), complete professional profiles and schedules, assign supervisors, and review duplicates.

**Primary navigation**

Sidebar → **User & Access**

- **User Profiles** → `/admin/user-access/profiles`  
- **Role Management** → `/admin/user-access/roles`  
- **Duplicate Detection** → `/admin/user-access/duplicate-detection`

---

## Section 3.1 — Roles

### Article: Create and manage roles & permissions

**Goal**

Define what each staff role can see and do.

**Step-by-step**

1. Go to **User & Access → Role Management** (`/admin/user-access/roles`).  
2. Use search **Search role...** if filtering a long list.  
3. Click **Create Role**.  
4. In modal **Create New Role**, fill:  
   - **Role name**  
   - **Display name**  
   - **Description**  
   - Permission checkboxes for the capabilities this role needs  
5. Submit **Create Role**.  
6. To change later: row **Edit** → adjust → **Update Role**.  
7. To remove: row **Delete** and confirm.

**Recommended approach**

Create/confirm a Therapist role (and any custom staff roles) **before** inviting/creating many users, so **Role** on Add User always has the correct options.

---

## Section 3.2 — Users and professional profiles

### Article: Create a therapist user with password

**Goal**

Create a therapist account that can log in at `/auth/staff/login`.

**Step-by-step**

1. Go to **User & Access → User Profiles** (`/admin/user-access/profiles`).  
2. Confirm the **User Profiles** tab is selected (not Supervisor Assignments).  
3. Click header button **Add New User**.  
4. In the Add User modal, complete:  
   - **Full Name** (required)  
   - **Email** (required)  
   - **Username** (required)  
   - **Phone** (optional)  
   - **Role** (required) — open the searchable Role dropdown and select the Therapist role (or the clinic’s equivalent display name)  
   - **Password** (required; meet length/complexity hints shown under the field)  
5. Click **Add User**.  
6. Confirm toast **User added successfully.** and that the user appears in the table as Active.

**What happens next for the therapist**

1. Therapist opens `/auth/staff/login`.  
2. Signs in with email + the password Admin set (or temporary flow if backend requires password change).  
3. If prompted, completes **Set your password** (Chapter 1).  
4. Lands on `/therapist/dashboard`.

**Note**

Staff creation is **in-app with password**. There is no separate “email-only invite without password” flow in the current frontend.

---

### Article: Edit, activate, deactivate, or delete a user

**Goal**

Maintain staff accounts day-to-day.

**Step-by-step**

1. Open `/admin/user-access/profiles` → **User Profiles**.  
2. Find the user row.  
3. Open the row actions menu (⋯).  
4. Choose one of:  
   - **Edit Basic Info** → update fields → **Save Changes**  
   - **Professional Details** → open professional modal (see next articles)  
   - **Deactivate** (if Active) → confirm when prompted  
   - **Activate** (if Inactive)  
   - **Delete** → confirm deletion  
5. Verify the table status/name reflects the change.

---

### Article: Complete professional details for a therapist (Admin)

**Goal**

Capture license/specializations/background/emergency contact for a therapist from the Admin side.

**Step-by-step**

1. On the therapist’s User Profiles row, open ⋯ → **Professional Details**.  
2. The professional modal sidebar shows:  
   - **License**  
   - **Specializations**  
   - **Background**  
   - **Schedule**  
   - **Emergency Contact**  
3. Start with **License** and enter license number/type/state/expiry as available.  
4. Open **Specializations** and record specialties.  
5. Open **Background** and enter education/experience fields provided.  
6. Open **Emergency Contact** and enter emergency contact details.  
7. Save using the section save controls provided on each section.  
8. Close the modal when finished.

**Note**

Admin professional modal does **not** include therapist self-service sections such as Zoom Integration / Password (those appear in Therapist **My Profile**).

---

### Article: Set a therapist schedule from Admin (optional but recommended)

**Goal**

Give a therapist timezone, working hours, and assigned rooms so Admin scheduling can offer accurate slots and in-person rooms.

**When to do this**

- Optional before first appointment.  
- Strongly recommended before busy scheduling.  
- Can be done later by the therapist personally (Chapter 8).

**Prerequisites**

- Therapist user exists.  
- Rooms created under Settings → Therapy Rooms (if using in-person rooms).

**Step-by-step**

1. `/admin/user-access/profiles` → find therapist → ⋯ → **Professional Details**.  
2. Click sidebar **Schedule**.  
3. Set **Time Zone** (required searchable dropdown).  
4. Open **Select Rooms** multi-select and choose the physical rooms this therapist can use.  
5. Under **Working hours**:  
   - Enable each working day.  
   - Add one or more time slots per day (start/end).  
   - Leave non-working days inactive.  
6. Click **Save Profile**.  
7. Confirm success toast/message and reopen Schedule to verify hours/rooms persisted.

**If no professional profile existed yet**

The first successful save may create the professional profile record; continue until schedule values stick.

---

### Article: Assign a supervisor

**Goal**

Link therapists/staff to supervisors for oversight workflows.

**Step-by-step**

1. Open `/admin/user-access/profiles`.  
2. Switch to tab **Supervisor Assignments**.  
3. Click **Assign Supervisor**.  
4. In modal **Assign Supervisor**, select supervisor and assignee according to the form.  
5. Submit **Assign**.  
6. Confirm the assignment appears in the Supervisor Assignments list.

---

### Article: Run duplicate detection

**Goal**

Review potential duplicate people/users and decide keep vs duplicate.

**Step-by-step**

1. Go to **User & Access → Duplicate Detection** (`/admin/user-access/duplicate-detection`).  
2. Review header **Detection Results**.  
3. Click **Refresh Scan** to run/update detection.  
4. For each suggested record:  
   - Click **View full record** if you need detail (**Full Record** modal).  
   - Choose **Keep this** or **Mark as Duplicate** (confirm **Mark as duplicate** when prompted).  
5. Refresh again after resolving a batch.

---

# Chapter 4 — Clients & Portal Access

**Chapter purpose:** Create patient records, optionally grant portal login, manage portal activation, and operate the full client profile tabs.

**Primary navigation**

Sidebar → **Clients** → `/admin/clients`

---

## Section 4.1 — Create clients and portal access decisions

### Article: Create a client / patient profile

**Goal**

Add a complete client record and optionally enable portal access at creation time.

**Where to go**

1. Sidebar → **Clients** (`/admin/clients`).  
2. Click **Add Client**.  
3. Modal title **Add New Client** opens.

**Wizard tabs (exact order)**

**Personal → Address → Referral → Employment → Clinical**

Footer: **Cancel** | **Next** | (last tab) **Create Client**  
Edit mode uses **Save Changes**. **Consents** tab exists only on **Edit Client**, not create.

**Step-by-step field inventory**

**1. Personal** — section **Personal Information**

| Field | Required |
|-------|----------|
| Full Name | Yes |
| Email | Yes |
| Phone | No |
| Date of Birth | No |
| Gender | No |
| Marital Status | No |
| Pronouns | No |
| Language | No |

**Immediate Setup**

- **Enable Portal Access** — ON grants portal path; OFF is clinic-only until later enable  
- **Email Notifications**

**2. Address** — section **Address Information**

Street Address 1 · Street Address 2 · City · State/Province · ZIP / Postal Code · Country  

Optional **Emergency Contact** checkbox → Contact Name · Contact Phone · Relationship to Client  

**3. Referral** — section **Referral & Case Information**

Start Date · Referral Date · Referrer Name · Reference Number · Client Source · Referral Notes  

**4. Employment** — section **Employment & Socioeconomic**

Employment Status · Education Level · Number of Dependents  

**5. Clinical** — section **Client Stage & Service Type**

Status · Client Type · Client stage · Service Type · Service Frequency · Treatment Modality · **Assigned Therapist** (strongly recommended)  

Optional needs follow-up (Priority, Due Date, notes) and Insurance Information (Provider, Type, Policy Number, Group Number, Copay Amount, Deductible, Insurance Phone) plus general notes.

**Submit**

On Clinical click **Create Client**.  

**After create**

Toast **Client created successfully.** Stay on clients list; open the row for the profile.

**Minimum checklist**

Full Name + Email · Prefer Assigned Therapist · Enable Portal Access only with correct email  

---

### Article: Choose portal access at create time (yes vs no)

**Scenario A — Client should log into the portal**

1. During **Add New Client → Personal → Immediate Setup**, turn **Enable Portal Access** ON.  
2. Ensure a valid primary email is entered (portal uses this email).  
3. Create the client.  
4. TherapyFlow sends `hasPortalAccess: true` with portal email.  
5. Client should receive activation email linking to `/portal/activate/:token`.  
6. If email is missing, use Overview → **Resend Activation Email** after create.

**Scenario B — Client should NOT have portal login**

1. Leave **Enable Portal Access** OFF.  
2. Create the client normally.  
3. Staff continue working the record; client cannot activate portal.  
4. If needs change later, enable portal from Overview (next article).

---

### Article: Enable, resend, or disable portal access later

**Goal**

Manage portal access after the client already exists.

**Step-by-step**

1. Open `/admin/clients` and select the client.  
2. Go to tab **Overview**.  
3. Expand section **Portal Access Management**.  
4. Review **Portal Email** display.  
5. If portal is off: click **Enable Portal Access**.  
6. If portal is on and client never activated: click **Resend Activation Email**.  
7. If portal must be revoked: click **Disable Portal Access**, then confirm in the confirmation modal.  
8. Confirm success toast:  
   - enabled successfully, or  
   - disabled successfully.

**Portal features (when enabled)**

The section may list portal capabilities such as:

- View upcoming appointments  
- Book new appointments online  
- View and pay invoices  
- Upload documents securely  

---

### Article: Edit, close/activate, or delete a client file (Admin)

**Goal**

Maintain the client record lifecycle from Overview.

**Step-by-step**

1. Open the client profile **Overview**.  
2. In the header actions:  
   - Click **Edit** (pencil) → modal **Edit Client** → update tabs including **Consents** if needed → **Save Changes**.  
   - Click **Close File** / **Activate File** (folder) when changing file active/closed status.  
   - Click **Delete** (trash) → confirm deleting the client.  
3. Confirm Overview sections still show correct **General Information**, **Clinical Status**, **Referral Information**, and **Portal Access Management**.

---

### Article: Filter and find clients in the list

**Goal**

Locate clients quickly on a large roster.

**Step-by-step**

1. Open `/admin/clients`.  
2. Use search if available for name/ID/email.  
3. Open **Filters**.  
4. Set filters such as:  
   - **Status**  
   - **Stage**  
   - **Assigned Therapist**  
   - **Client Type**  
   - any quick filters shown  
5. Click **Apply filters**.  
6. Clear with **Clear all** when finished.  
7. Use filter chips (if shown) to remove one filter at a time.

---

## Section 4.2 — Working inside the client profile tabs

**Client profile tabs (exact labels)**

1. **Overview**  
2. **Sessions**  
3. **Assessments**  
4. **Reports**  
5. **Forms & Docs**  
6. **Billing**  
7. **Tasks**  
8. **Checklists**  
9. **History**

---

### Article: Use the Sessions tab for one client

**Goal**

Review and manage that client’s sessions without leaving the profile.

**Step-by-step**

1. Open client → tab **Sessions**.  
2. Review summary cards:  
   - **Total Sessions**  
   - **Completed**  
   - **Scheduled**  
   - **Missed/Cancelled**  
   - **Conflicts**  
3. Filter by status and notes status:  
   - **All Notes** / **With Notes** / **Without Notes**  
4. Click **View Calendar** to jump to `/admin/scheduling`.  
5. Click **Schedule Session** to open **Schedule New Session** for this client.  
6. On a session card:  
   - **Add Note** / **View Note**  
   - Open ⋮ menu for edit/status/invoice/recording actions (see Chapter 5 session lifecycle article).

---

### Article: Assign an assessment to a client

**Step-by-step**

1. Open client → **Assessments**.  
2. Browse available templates.  
3. Click **Assign**.  
4. Modal **Assign Assessment** opens.  
5. Set **Due Date** and **Notes**.  
6. Click **Assign**.  
7. Confirm it appears in the assigned list.  
8. To remove: use **Delete Assessment** and confirm.

---

### Article: Assign clinical forms and upload documents

**Step-by-step**

1. Open client → **Forms & Docs**.  
2. Choose sub-tab **Clinical Forms**.  
3. Use dropdown placeholder **Select form template to assign**.  
4. Click **Assign form(s) to client**.  
5. Confirm under heading **Assigned Forms**.  
6. Switch to sub-tab **Documents**.  
7. Click **Upload Document** and complete the upload modal.  
8. For an existing document row: **View**, **Download**, or **Delete** (confirm **Delete Document**).

---

### Article: Assign a checklist template

**Step-by-step**

1. Open client → **Checklists**.  
2. Use dropdown **Select a checklist template to assign**.  
3. Click **Assign checklist template**.  
4. Under **Assigned Checklists**, open the checklist.  
5. Complete or update checklist item states.  
6. Click **Save**.

---

### Article: Create tasks from the client profile

**Step-by-step**

1. Open client → **Tasks**.  
2. Click **Add task** (or **Create first task** when empty).  
3. Complete modal **Create task**.  
4. Submit **Create task**.  
5. Use card actions later to view, edit, or delete.

---

### Article: Review History, Communications, and Email History

**Step-by-step**

1. Open client → **History**.  
2. Review **Time in Each Stage**.  
3. Review **Timeline**.  
4. Open sub-tab **Communications**.  
5. Search/filter by type (Call/Email/Note) and date range.  
6. Click **Add Note** → fill → **Add note**.  
7. Edit a note → **Save**, or delete → **Delete Note**.  
8. Open sub-tab **Email History** to review emailed events related to the client.

---

# Chapter 5 — Scheduling & Sessions

**Chapter purpose:** Book appointments, import sessions in bulk, manage calendar views, and run the full session lifecycle (status, notes, recording, invoicing).

**Primary navigation**

Sidebar → **Scheduling** → `/admin/scheduling`

---

## Section 5.1 — Booking appointments (Admin)

### Article: Schedule a new session (Admin)

**Goal**

Book a session for a specific client with a specific therapist, including room selection for in-person mode.

**Prerequisites**

- Client exists (Chapter 4).  
- Service exists (Chapter 2).  
- Therapist exists (Chapter 3).  
- Therapist schedule/rooms recommended (Chapter 3 or Chapter 8).  

**Entry points (same modal)**

| Entry | Path |
|-------|------|
| Global scheduling | Sidebar **Scheduling** → `/admin/scheduling` → **New Session** |
| From client profile | `/admin/clients` → client → **Sessions** → **Schedule Session** |
| From dashboard empty upcoming | `/admin/dashboard` → **Schedule Session** |

**Modal title:** **Schedule New Session**

**Step-by-step field order**

1. Open the modal using one entry point above.  
2. Select **Session Type**.  
3. Select **Client**.  
4. Select **Service** (duration influences available slots).  
5. Select **Therapist** (Admin-only field).  
   - If the client already has an **Assigned Therapist**, this often pre-fills.  
6. Choose **Session Mode**:  
   - In person — “Face to face at clinic”  
   - Virtual — “Online video session” (meeting link messaging appears for virtual)  
7. Select **Date**.  
8. Pick an available time slot from loaded availability.  
9. If in person: select **Room** after date and time are chosen (list comes from therapist assigned rooms + availability).  
10. Optional: fill **Session notes or special instructions**.  
11. Configure recurrence fields only if offered and needed.  
12. Submit the schedule.  
13. Success modal shows **Session scheduled successfully!**  
    - Recurring booking success may show **Recurring sessions scheduled!**  
14. Calendar refreshes; verify the appointment appears under the correct Day/Week/Month/All Sessions view.

**Calendar views on `/admin/scheduling`**

- **Day**  
- **Week**  
- **Month**  
- **All Sessions**

Use therapist filters (for example **All Therapists**) when reviewing calendars.

---

### Article: Bulk import sessions with CSV (Admin)

**Goal**

Create many sessions at once from a template file.

**Step-by-step**

1. Open `/admin/scheduling`.  
2. Click **Import** (upload icon).  
3. Modal **Bulk Upload Sessions** opens.  
4. Click **Download Template**.  
5. Fill the CSV using the template columns.  
6. Return to the modal and upload the completed file.  
7. Click **Upload Sessions**.  
8. Review success/error feedback.  
9. Confirm imported sessions appear in calendar / **All Sessions**.

---

## Section 5.2 — Session lifecycle and dashboard operations

### Article: Manage session status, notes, recording, and invoices

**Goal**

Operate an existing session across its lifecycle from scheduling or the client Sessions tab.

**Where this works**

- Client **Sessions** tab  
- `/admin/scheduling` calendar and **All Sessions**  
- Dashboard overdue session actions  

**Common session card/menu actions**

Open the session actions menu (⋮). Available items depend on current status:

| Menu label | What it does |
|------------|--------------|
| **Edit Session Details** | Opens **Edit Session** modal |
| **Create invoice** | Opens **Create invoice** (when status is billable) |
| **Record Session** | Opens **Record Session** |
| **View Transcript** | Opens transcript after recording exists |
| **Mark as Scheduled** | Status transition |
| **Mark as Confirmed** | Status transition |
| **Mark as Overdue** | Status transition |
| **Mark as In Progress** | Status transition |
| **Mark as Completed** | Status transition |
| **Mark as Cancelled** | Status transition |
| **Mark as Rescheduled** | Status transition |
| **Mark as No-Show** | Status transition |
| **Cancel Upcoming Series** | Cancels future recurrence group sessions when applicable |
| **Join Meeting** | Available to therapist/staff for virtual sessions (not shown on Admin path) |
| **Download PDF** | After note submitted |
| **Reopen Note** | After note submitted |
| **Delete Session** | Removes session after confirmation |

**Notes workflow**

1. On a session card click **Add Note** (or **View Note** if exists).  
2. Complete note content in **Add Session Note** / note editor views.  
3. Submit/save according to note UI.  
4. Later use **Download PDF**, **Reopen Note**, or delete confirmations as needed.

**Confirm modals you may see**

- Cancel upcoming series?  
- Delete session?  
- Reopen session note?  

---

### Article: Use the Admin dashboard for daily session and task triage

**Path:** `/admin/dashboard`

**What you will see**

Stat cards:

- **Active Clients**  
- **Today's Sessions**  
- **Pending Tasks**  
- **Billing Overview**

**Sessions panel**

1. Switch tabs **Previous**, **Upcoming**, **Overdue**.  
2. If Upcoming is empty, click **Schedule Session**.  
3. Click **View all** to open `/admin/scheduling`.  
4. On overdue session ⋮: change status marks or **Edit Session Details**.

**Tasks panels**

1. In **Upcoming Deadlines**, open task ⋮ for **View Detail**, **Edit Task**, **Delete**.  
2. Click **View all** → `/admin/tasks`.  
3. In **Recent Tasks**, use same actions; empty state **Create new task** → `/admin/tasks`.  
4. History shortcut **View all** can go to `/admin/tasks/history`.  
5. Delete confirm title form: `Delete task "…"?` with button **Delete task**.

---

# Chapter 6 — Billing & Payments

**Chapter purpose:** Operate clinic invoice records—record payments, email/download invoices, discounts, refunds, split payments, and transaction voiding.

---

## Section 6.1 — Billings workspace (Admin)

### Article: Work in Admin Billings day to day

**Path:** Sidebar → **Billings** → `/admin/billings`

**Step-by-step overview**

1. Open Billings and review overview cards/stats.  
2. Locate the billing records table (UI section label may appear as **Billling Records**).  
3. Search and open filters for:  
   - **Billing Status**  
   - **Payment Status**  
   - **Payment Method**  
   - **Client Type**  
   - **Session Type**  
   - **Service code**  
   - amount / date ranges  
4. Apply filters and review matching invoices.

**Record a payment**

1. On an unpaid/actionable row click **Pay now**.  
2. Modal **Record Payment** opens.  
3. Enter **Payment Amount**, **Payment Method**, **Reference Number**, notes as shown.  
4. Submit **Record Payment**.  
5. Confirm status updates on the row.

**Row ⋮ actions (when available)**

- **Email Invoice**  
- **Preview Invoice**  
- **Download Invoice**  
- **Apply Discount** → modal **Apply Discount**  
- **Split Payment** → **Record split payment**  
- **Refund Payment** → **Issue refund**  
- **View Transactions** → void a transaction with reason when needed  
- **Change Status**  
- **Mark as Billed / Paid / Denied / Follow-up / Pending**

**Client-scoped billing**

1. Open client profile → tab **Billing**.  
2. Use the same pay/preview/email/refund patterns, limited to that client.

**Session-origin invoice**

From a session ⋮ menu, use **Create invoice** when the session status is billable, then finish collection in Billings or client Billing tab.

**Org subscription reminder**

Clinic SaaS subscription management is under Settings → **Subscription**, not this Billings table.

---

# Chapter 7 — Tasks, Content & Compliance

**Chapter purpose:** Run operational tasks, build reusable content templates, configure notifications, and review compliance surfaces.

---

## Section 7.1 — Tasks (Admin)

### Article: Create, track, and complete tasks

**Path:** `/admin/tasks`

**Step-by-step**

1. Open Sidebar → **Tasks**.  
2. Review cards: **Total Tasks**, **In Progress**, **Pending**, **Completed**.  
3. Switch tabs **All tasks** / **Active tasks**.  
4. Search with **Search tasks, clients...**.  
5. Apply filters if shown and review chips.  
6. Click **Add task**.  
7. Complete **Create task** modal fields (title, client link, due date, assignees, priority/notes as available).  
8. Submit **Create task**.  
9. On a task card open ⋮:  
   - **Task Details**  
   - **Edit Task**  
   - **Delete**  
10. Click **View History** → `/admin/tasks/history`.  
11. On history page use tabs **All tasks**, **Completed**, **Overdue**, **Recent**, and search **Search task history...**.

Tasks can also be created from a client profile **Tasks** tab (Chapter 4).

---

## Section 7.2 — Content templates

### Article: Manage the clinical library

**Path:** `/admin/content/library`

**Step-by-step**

1. Open **Content → Library**.  
2. Use category tabs (for example **Session Focus**, **Symptoms**, **Short-term goals**, **Interventions**, **Progress**).  
3. Click **Add Entry** to create a single entry (**Add New Entry** modal).  
4. Or click **Bulk Add** to paste/import multiple entries via the wizard.  
5. Edit entries as needed.  
6. Delete entries/connections carefully using confirmations such as **Delete entry?**.

---

### Article: Create and assign assessment templates

**Path:** `/admin/content/assessment`

**Step-by-step**

1. Open **Content → Assessment**.  
2. Use tabs **Templates (n)** and **Active Assignments (n)**.  
3. Click **New Template** and complete template creation/builder flow (including create-assessment route when offered).  
4. From a template row, assign using **Assign Assessment** modal → **Assign**.  
5. On Active Assignments, update statuses such as **Pending**, **In Progress**, **Completed** when available.  
6. Client-side assignment can also be done from the client **Assessments** tab.

---

### Article: Create clinical form templates

**Path:** `/admin/content/clinical-forms`

**Step-by-step**

1. Open **Content → Clinical Forms**.  
2. Click **New Form Template**.  
3. Create/configure the template.  
4. Open builder/detail routes when needed (for example consent form builder path under clinical-forms).  
5. Toggle template **Active** / **Inactive**.  
6. Edit or delete templates from row actions.  
7. Assign forms to clients from client **Forms & Docs → Clinical Forms**.

---

### Article: Create process checklist templates

**Path:** `/admin/content/process-checklists`

**Step-by-step**

1. Open **Content → Process Checklists**.  
2. Click **Create Template**.  
3. Build template structure and checklist items.  
4. Duplicate templates when creating variants.  
5. Delete templates/items via confirmations (**Delete Checklist Template**, **Delete Checklist Item**).  
6. Assign to clients from client **Checklists** tab.

---

### Article: Upload and manage report templates

**Path:** `/admin/content/report-templates`

**Step-by-step**

1. Open **Content → Report Templates**.  
2. Click **Upload Template**.  
3. Upload Word/PDF (as accepted by the modal).  
4. Toggle template active state.  
5. Edit metadata if available.  
6. Delete with **Delete template?** confirmation when obsolete.  
7. Generated/report outputs appear later under client **Reports** as configured.

---

## Section 7.3 — Notifications (Admin)

### Article: Configure notifications, triggers, and templates

**Path:** `/admin/system/notifications`

**Step-by-step**

1. Open Sidebar → **System → Notifications**.  
2. Choose top tab **Notifications** or **SMS Notifications**.  
3. Inside Notifications, move through sub-tabs:  
   - **Notifications**  
   - **Event Catalog**  
   - **Triggers**  
   - **Templates**  
   - **Preferences**  
4. Click **Create Notification** to send/create a notification with targeting, type, priority, and expiration fields as shown.  
5. Create/edit automation with **Create Trigger** / **Edit Trigger**.  
6. Create/edit content with **Create Template** / **Edit Template**.  
7. Adjust preference defaults as required by clinic policy.

---

## Section 7.4 — Compliance (Admin)

### Article: Review Privacy & Consent

**Path:** `/admin/compliance/privacy`

**Step-by-step**

1. Open **Compliance → Privacy & Consent**.  
2. Search with **Search by client ID, name or email...**.  
3. Filter by consent type and status dropdowns.  
4. Click **Refresh** to reload.  
5. Review consent columns (portal access, AI, data sharing, etc. as shown in the grid).

---

### Article: Use the HIPAA audit log

**Path:** `/admin/compliance/hipaa`

**Step-by-step**

1. Open **Compliance → HIPAA Audit**.  
2. Search with **Search by username...**.  
3. Set period and filters (action type, risk, PHI-only, date range).  
4. Review charts for user activity and risk distribution.  
5. Scroll the audit log table for event detail.  
6. Click **Export Report** when you need an exportable audit artifact.

---

# Chapter 8 — Therapist Workspace

**Chapter purpose:** Teach therapists how to land in their portal, set their own schedule, manage clients, schedule sessions, and use billings/tasks/notifications.

**Therapist sidebar (exact labels)**

- **Dashboard** → `/therapist/dashboard`  
- **Clients** → `/therapist/clients`  
- **Scheduling** → `/therapist/scheduling`  
- **Billings** → `/therapist/billings`  
- **Tasks** → `/therapist/tasks`

---

## Section 8.1 — Therapist first days

### Article: Therapist login and dashboard walkthrough

**Step-by-step**

1. Sign in at `/auth/staff/login`.  
2. Complete password activation if required (Chapter 1).  
3. Land on `/therapist/dashboard`.  
4. Review cards:  
   - **Active Clients**  
   - **Today's Sessions**  
   - **Pending Tasks**  
   - **Billing Overview**  
5. In Sessions panel use tabs **Previous**, **Upcoming**, **Overdue**.  
6. Click **View all** to open `/therapist/scheduling`, or empty-state **Schedule Session**.  
7. For overdue sessions, open ⋮ to edit details or mark statuses.  
8. Use **Upcoming Deadlines** and **Recent Tasks** widgets to open `/therapist/tasks` or `/therapist/tasks/history`.

---

### Article: Set your own schedule in My Profile

**Goal**

Therapist self-service alternative to Admin professional Schedule setup.

**Step-by-step**

1. From any therapist page, open top bar avatar menu.  
2. Click **My Profile**.  
3. Therapist profile modal sidebar includes:  
   - **Basic Info**  
   - **License**  
   - **Specializations**  
   - **Background**  
   - **Schedule**  
   - **Zoom Integration**  
   - **Password**  
4. Open **Schedule**.  
5. Set **Time Zone**.  
6. Choose rooms with **Select Rooms**.  
7. Configure **Working hours** day by day.  
8. Click **Save Profile**.  
9. For other sections, use **Save changes** after edits.  
10. Optionally configure **Zoom Integration** so virtual sessions can connect cleanly.  
11. Use **Password** section if changing password while already signed in.

**Note**

Emergency contact fields live under **Basic Info** for therapist self-profile (not a separate sidebar item like Admin professional modal).

---

## Section 8.2 — Therapist clients and scheduling

### Article: View assigned clients and create a patient

**Path:** `/therapist/clients`

**Step-by-step**

1. Open Sidebar → **Clients**.  
2. Review the list of clients assigned by Admin and any clients you created.  
3. Click a row to open half/full profile panel.  
4. Use the same nine tabs as Admin: Overview, Sessions, Assessments, Reports, Forms & Docs, Billing, Tasks, Checklists, History.  
5. To create a patient, click **Add Client**.  
6. Complete **Add New Client** tabs (**Personal** through **Clinical**).  
7. Optionally enable **Enable Portal Access** on Personal.  
8. Set/confirm **Assigned Therapist** on Clinical (often yourself).  
9. Submit **Create Client**.  
10. Manage portal later from **Overview → Portal Access Management** using **Enable Portal Access**, **Resend Activation Email**, or **Disable Portal Access**.  
11. Header actions on therapist profile typically include **Edit** and **Delete** (confirm `Delete Client "…"` when deleting).

---

### Article: Schedule a session as a therapist

**Path:** `/therapist/scheduling`

**Step-by-step**

1. Open Sidebar → **Scheduling**.  
2. Choose view **Day**, **Week**, **Month**, or **All Sessions**.  
3. Click **New Session**.  
4. Modal **Schedule New Session** opens.  
5. Complete fields:  
   - **Session Type**  
   - **Client**  
   - **Service**  
   - **Session Mode**  
   - **Date**  
   - Time slot  
   - **Room** (in-person)  
   - Optional notes  
6. There is **no Therapist picker** — you are automatically the session therapist.  
7. Submit and confirm success **Session scheduled successfully!**  
8. Alternate entry: open client → **Sessions** → **Schedule Session**.  
9. To edit later, use **Edit Session Details** / **Edit Session**.  
10. For virtual sessions, use ⋮ → **Join Meeting** when available.

---

## Section 8.3 — Therapist billings, tasks, and notifications

### Article: Therapist billings workspace

**Path:** `/therapist/billings`

Use the same invoice operations taught in Chapter 6 (record payment, email/preview/download, discount, split, refund, transactions, status changes), scoped to the therapist’s clients and permissions.

---

### Article: Therapist tasks

**Path:** `/therapist/tasks`

**Step-by-step**

1. Open **Tasks**.  
2. Use tabs **All tasks** and **Active tasks**.  
3. Click **Add task** → modal title **Create new task** → submit **Create task**.  
4. Edit with **Edit task** → **Save changes**.  
5. Open details with **Task Details**.  
6. Delete with confirm **Delete task?** / **Delete**.  
7. Click **View History** → `/therapist/tasks/history` and use tabs **All tasks**, **Completed**, **Overdue**, **Recent**.

---

### Article: Therapist notifications inbox

**Step-by-step**

1. Click the top bar notifications bell.  
2. Click **View all**.  
3. Land on `/therapist/system/notifications`.  
4. Work in the **Notifications** inbox.  
5. Create client-scoped notifications when the create action is available.  

**Difference from Admin**

Therapists do **not** get the full Admin configuration suite (Event Catalog / Triggers / Templates / Preferences) in the same way Admin does.

---

# Chapter 9 — Client Portal

**Chapter purpose:** Teach patients how to activate access, book appointments, rate sessions, update profile, upload documents, complete forms, and pay invoices.

**Client sidebar (exact labels)**

- **Appointments** → `/user/appointments`  
- **Booked Sessions** → `/user/booked-sessions`  
- **Invoices** → `/user/invoices`  
- **Documents** → `/user/documents`  
- **Clinical Forms** → `/user/clinical-forms`

---

## Section 9.1 — Activate and sign in

### Article: Activate your portal account from email

**Goal**

Set the first password from the clinic activation email and enter the portal.

**Prerequisites**

- Clinic turned **Enable Portal Access** ON for your record.  
- Activation email received (or resent by clinic).

**Step-by-step**

1. Open the activation link from email (`/portal/activate/:token`, may include organisation query parameters).  
2. Confirm page title **Activate Your Account**.  
3. Enter password and confirm password.  
4. Click **Activate account** (shows **Activating...** while processing).  
5. TherapyFlow activates the account (`POST /api/v1/portal/activate`), signs you in, and redirects to `/user/appointments`.  

**If the link is invalid**

You may see **Invalid activation link**. Ask the clinic to resend activation from client Overview → **Resend Activation Email**.

---

### Article: Sign in to the Client Portal after activation

**Step-by-step**

1. Open `/auth/login`.  
2. Enter portal email and password.  
3. Submit login.  
4. Land on `/user/appointments`.  
5. Use left navigation for Appointments, Booked Sessions, Invoices, Documents, Clinical Forms.  
6. Use avatar menu for **My Profile** and **Privacy Settings**.

---

## Section 9.2 — Book and review appointments

### Article: Book an appointment online

**Path:** `/user/appointments`

**Prerequisites**

- Portal access activated.  
- Clinic published portal-visible services in **Service Visibility**.  
- Therapist availability exists for the chosen mode/date.

**Step-by-step**

1. Open **Appointments**.  
2. Choose mode toggle:  
   - **In person**  
   - **Virtual Visit**  
3. In **Select Date & Time**, pick a future date on the calendar (past dates disabled).  
4. Select an available time from the time slots list.  
5. Under heading **Choose the type of service you need**, open **Select a service**.  
6. Choose the service (duration and price may display in the option).  
7. Click **Next** (shows **Booking...** while loading confirmation if already submitting).  
8. Review modal **Confirm Your Appointment**.  
9. Click **Confirm Booking**.  
10. Success modal **Appointment Confirmed** appears.  
11. Upcoming list refreshes on the Appointments page.

**What is not available on this screen today**

Upcoming cards are informational; clients do **not** cancel or join meetings from this list in the current UI.

---

### Article: View booked sessions and rate a completed session

**Path:** `/user/booked-sessions`

**Step-by-step**

1. Open sidebar **Booked Sessions**.  
2. Use search placeholder **Search by service, therapist, or location...** if needed.  
3. Open a session card/detail (`/user/booked-sessions/:sessionId`) with **Back to booked sessions** to return.  
4. For a completed session, click **Rate session** (list) or **Rate this session** (detail).  
5. Modal title **Rate your session** opens.  
6. Enter **Rating (0–10)** and optional **Comment**.  
7. Click **Submit rating** (or **Cancel** to close).  

**Not in client UI today**

- Join meeting button for clients  
- Cancel appointment button for clients  

(Clinic staff manage cancellations/status from Admin/Therapist scheduling tools.)

---

## Section 9.3 — Profile, documents, forms, and payments

### Article: Update My Profile and Privacy Settings

**Step-by-step**

1. Open avatar menu → **My Profile** (`/user/my-profile`).  
2. Review read-only **Account Information**:  
   - **Full Name**  
   - **Client ID**  
   - **Email**  
   - **Phone**  
3. Click **Upload new picture** to change avatar (JPEG/PNG/GIF/WebP, max size as enforced; button may show **Uploading...**).  
4. Under Preferences, choose **Timezone** (search **Search timezone...**).  
5. Click **Save timezone** (may show **Saving...**).  
6. Open avatar menu → **Privacy Settings** (`/user/privacy-settings`) to manage consents/preferences available there.

---

### Article: Upload documents securely

**Path:** `/user/documents`

**Step-by-step**

1. Open **Documents**.  
2. Confirm heading **Uploaded Documents**.  
3. Click **Upload Document**.  
4. In modal **Upload Document**, choose file and required metadata fields.  
5. Submit **Upload Document** (shows **Uploading...** while in progress).  
6. Confirm the document appears in the list.  
7. Use preview/view/download actions from the table as available.

---

### Article: Complete assigned clinical forms

**Path:** `/user/clinical-forms`

**Step-by-step**

1. Open **Clinical Forms**.  
2. Search with **Search** and filter by status if needed.  
3. On a form card click **Start**, **Continue**, or **View**.  
4. Detail route opens `/user/clinical-forms/:formId`.  
5. Use **Back to Forms** if you need to return.  
6. Complete required fields.  
7. Complete signature component when required.  
8. Click **Submit Form** (shows **Submitting...**).  
9. Confirm the form status updates on the list.

---

### Article: Pay an invoice online

**Path:** `/user/invoices`

**Prerequisites**

- Clinic Stripe Connect / online payments available.  
- Invoice is payable.

**Step-by-step**

1. Open **Invoices**.  
2. Find the unpaid invoice.  
3. Click **Pay now**.  
4. Complete Stripe Checkout on Stripe’s hosted page.  
5. Return to invoices with `?payment=success`.  
6. Confirm toast **Payment completed successfully.**  
7. You may briefly see processing banner text that invoice status will update shortly.  
8. If you cancel Checkout (`?payment=cancelled`), you may see **Payment was cancelled.**  
9. When available, click **Receipt** to download a receipt.  

If online payments are unavailable, a disabled-reason banner explains why Pay now is blocked.

---

# Chapter 10 — Staff (Custom Roles)

**Chapter purpose:** Explain how custom staff accounts differ from Admin and Therapist when access is permission-driven.

---

## Section 10.1 — Permission-based staff access

### Article: Sign in and understand the staff menu

**Step-by-step**

1. Sign in at `/auth/staff/login`.  
2. Complete first-password activation if required (Chapter 1).  
3. TherapyFlow loads permissions from your role.  
4. You land on the first permitted `/staff/...` route.  
5. If no routes are permitted, you see `/staff/no-access` (**No access configured**).  
6. Sidebar items appear only when allowed, commonly including subsets of:  
   - Clients  
   - Scheduling  
   - Billings  
   - Tasks  
   - User Management (when permitted)  
   - Content children (Library, Assessment, Clinical Forms, Process Checklists, etc.)  
   - Compliance (often HIPAA Audit)  
   - Notifications  
7. There is **no** dedicated staff dashboard equivalent to Admin/Therapist dashboards.

**How to use other chapters with staff accounts**

- If you can open Clients, follow Chapter 4 steps for the screens you can access.  
- If you can open Scheduling, follow Chapter 5, noting some edit actions may be hidden for supervisors.  
- If you can open Billings/Tasks/Content/Compliance/Notifications, use Chapters 6–7 with permission-limited buttons.  
- Org subscription actions (when allowed) may appear at `/staff/billing/subscription`.

**Supervisor pattern**

Supervisors may see a broad menu but lack write actions such as:

- certain scheduling edits  
- billing service/policy management  
- subscription pay actions  
- some portal bulk/admin-only controls  

Always treat missing buttons as permission design, not a broken page.

---

# Appendix A — Complete menu maps for authors

## Admin sidebar → routes

| Sidebar label | Route |
|---------------|--------|
| Dashboard | `/admin/dashboard` |
| Clients | `/admin/clients` |
| Scheduling | `/admin/scheduling` |
| Billings | `/admin/billings` |
| Tasks | `/admin/tasks` |
| Content → Library | `/admin/content/library` |
| Content → Assessment | `/admin/content/assessment` |
| Content → Clinical Forms | `/admin/content/clinical-forms` |
| Content → Process Checklists | `/admin/content/process-checklists` |
| Content → Report Templates | `/admin/content/report-templates` |
| User & Access → User Profiles | `/admin/user-access/profiles` |
| User & Access → Role Management | `/admin/user-access/roles` |
| User & Access → Duplicate Detection | `/admin/user-access/duplicate-detection` |
| System → Notifications | `/admin/system/notifications` |
| System → Settings | `/admin/settings` |
| Compliance → HIPAA Audit | `/admin/compliance/hipaa` |
| Compliance → Privacy & Consent | `/admin/compliance/privacy` |

## Therapist sidebar → routes

| Sidebar label | Route |
|---------------|--------|
| Dashboard | `/therapist/dashboard` |
| Clients | `/therapist/clients` |
| Scheduling | `/therapist/scheduling` |
| Billings | `/therapist/billings` |
| Tasks | `/therapist/tasks` |

## Client portal sidebar → routes

| Sidebar label | Route |
|---------------|--------|
| Appointments | `/user/appointments` |
| Booked Sessions | `/user/booked-sessions` |
| Invoices | `/user/invoices` |
| Documents | `/user/documents` |
| Clinical Forms | `/user/clinical-forms` |

## Settings tab deep links (Admin)

| Tab label | Example URL |
|-----------|-------------|
| System Options | `/admin/settings?tab=system-options` |
| Service Prices | `/admin/settings?tab=service-prices` |
| Service Visibility | `/admin/settings?tab=service-visibility` |
| Invoice Policies | `/admin/settings?tab=invoice-policies` |
| Payment Integration | `/admin/settings?tab=stripe-connect` |
| Subscription | `/admin/settings?tab=subscription-billing` |
| Therapy Rooms | `/admin/settings?tab=therapy-rooms` |
| Library Categories | `/admin/settings?tab=library-categories` |
| Administration | `/admin/settings?tab=administration` |

---

# Appendix B — End-to-end clinic go-live checklist (tenant only)

Use this checklist as a Learning Hub “capstone” article or quiz sheet.

1. Admin first login and password set (Chapter 1).  
2. Settings → Administration practice details saved.  
3. System Options seeded for needed dropdowns.  
4. Service Prices created.  
5. Service Visibility configured (therapist + portal).  
6. Therapy Rooms created and active.  
7. Invoice Policies created (if billing is used).  
8. Payment Integration (Stripe Connect) completed for client payments.  
9. Subscription payment method/plan handled if required.  
10. Roles reviewed/created.  
11. Therapist user created with password (**Add New User**).  
12. Therapist schedule set by Admin and/or therapist My Profile Schedule.  
13. Client created; portal enabled only if patient should log in.  
14. If portal ON: client activates via email and lands on Appointments.  
15. Admin (or therapist) schedules first session with correct therapist/service/mode/room.  
16. Therapist can see assigned client, join virtual meeting when applicable, add notes, update status.  
17. Invoice created/paid through Billings or client portal **Pay now**.  
18. Tasks/content/forms assigned as needed for ongoing care.

---

# Appendix C — UI accuracy notes for Learning Hub editors

Keep these labels exact when writing articles:

| Topic | Exact UI text |
|-------|----------------|
| Add staff user button | **Add New User** |
| Add staff user submit | **Add User** |
| Create client button | **Add Client** |
| Create client modal | **Add New Client** |
| Edit client modal | **Edit Client** |
| Scheduling primary CTA | **New Session** |
| Scheduling modal create | **Schedule New Session** |
| Scheduling modal edit | **Edit Session** |
| Scheduling success | **Session scheduled successfully!** |
| Admin bulk upload CTA | **Import** |
| Bulk modal | **Bulk Upload Sessions** |
| Calendar fourth tab | **All Sessions** |
| Staff first password page | **Set your password** / **Set password** |
| Client activate page | **Activate Your Account** / **Activate account** |
| Client booking confirm | **Confirm Your Appointment** / **Confirm Booking** |
| Client booking success | **Appointment Confirmed** |
| Client invoice pay | **Pay now** |
| Client rate session | **Rate session** / **Rate your session** |
| Profile schedule save (therapist) | **Save Profile** |
| Portal later enable | **Enable Portal Access** |
| Portal resend | **Resend Activation Email** |

Do **not** document client Join/Cancel appointment controls—they are not present in the current client portal UI.

---

*Source of truth: TherapyFlow frontend routes, labels, and wired flows for Admin, Therapist, Staff, and Client. Super Admin / platform operations intentionally excluded from this Learning Hub set. Update articles when product UI labels or flows change.*
