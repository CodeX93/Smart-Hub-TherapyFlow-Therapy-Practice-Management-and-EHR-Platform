# Therapy Flow Frontend Context (Super Admin)

Last updated: 2026-04-22

This file captures the working context and implemented changes discussed in the current chat for the Super Admin UI and API integrations.

## High-Level Outcomes

- Super Admin API layer refactored from one large file into module-based files without breaking existing imports from `@/store/api/superAdminApi`.
- Notification Center (side panel) fully integrated for Templates, History (infinite scroll), and Triggers (list + CRUD).
- Organisation details screen and tabs updated with:
  - Dynamic users count from organisation details API (`totalUserCount`).
  - Provision tab actions (health UI, lock, backup).
  - Settings tab brand color fields as color pickers.
  - Breadcrumb navigation made clickable.
  - Organisation users tab actions wired to enable/disable user endpoints with confirmation + reason.
- Audit Logs screen redesigned:
  - Organisation name shown instead of resource type.
  - Action summary hidden under action (only shown on hover).
  - Row expands on hover to show formatted details.
  - Pagination replaced with an infinite loader pattern (aligned with org detail audit logs tab).
- Billing and subscription:
  - Dunning policy page wiring (GET + PUT) accessible from billing tab.
  - Dashboard labels updated from `MRR/ARR` to full names.

## API Refactor (Super Admin)

Goal: reduce `src/store/api/superAdminApi.ts` size while preserving existing exports and integration.

- Compatibility entrypoint remains: `src/store/api/superAdminApi.ts` re-exports from `src/store/api/super-admin`.
- Super Admin API is split into module files under `src/store/api/super-admin/`:
  - `shared.ts` (types + mappers + helpers)
  - `billing.api.ts`
  - `organisations.api.ts`
  - `notifications.api.ts`
  - `security.api.ts`
  - `audit.api.ts`
  - `catalog.api.ts`
  - `dashboard.api.ts`
  - `profile.api.ts`
  - `roles-permissions.api.ts`
- `src/store/api/super-admin/index.ts` re-exports modules and aliases `superAdminApi` (kept for legacy).
- `src/store/api/super-admin/endpoints.ts` is now a compatibility re-export (thin wrapper).

Build verification: `npm run build` has been run multiple times during refactors and passed.

## Notifications (Super Admin)

### Templates

- List templates: `GET /api/v1/super-admin/notifications/templates`
- Upsert template: `PUT /api/v1/super-admin/notifications/templates/{templateKey}`
- UI behavior:
  - Template key/name is disabled in edit mode.
  - Editable: subject, body, and active/inactive status (mapped from `isActive`).

### History

- List history: `GET /api/v1/super-admin/notifications/history` (supports `orgId` query).
- UI:
  - Infinite scroll pattern (loads more on demand).
  - Notification badge count uses real API data instead of static `2`.

### Triggers

- List triggers: `GET /api/v1/super-admin/notifications/triggers`
- Create trigger: `POST /api/v1/super-admin/notifications/triggers`
- Update trigger: `PUT /api/v1/super-admin/notifications/triggers/{id}`
- Delete trigger: `DELETE /api/v1/super-admin/notifications/triggers/{id}`
- Optional metadata API (used when available; otherwise fallback lists are used):
  - `GET /api/v1/super-admin/notifications/triggers/metadata`
- Trigger form contract (matches backend request):
  - `name`, `description`, `eventType`, `entityType` (nullable)
  - `conditionRules` (JSON string)
  - `recipientRules` (JSON string)
  - `priority`
  - `isScheduled`, `scheduleOffsetMinutes`, `batchWindowMinutes`, `maxBatchSize`
  - `isActive`
- UX:
  - Create/edit panel uses the real payload only (old “template/setupType/clientType/therapistId” fields removed).
  - Edit + delete actions available from trigger cards.
  - After create/update, triggers list refetches immediately.

Fallback keys for metadata live in `src/components/notification/notification.static.ts`.

## Organisation Detail (Super Admin)

### Breadcrumbs

Breadcrumb text (e.g. `Super Admin / Organisations / <Org>`) updated so:
- `Super Admin` navigates to `/super-admin/dashboard`
- `Organisations` navigates to `/super-admin/organisations`

### Users Tab: Enable/Disable User

- Source list API example: `GET /api/v1/super-admin/organisations/{id}/users?page=0&pageSize=50`
- Mapped to use real `authId`:
  - Fix applied so numeric `authId` is used as the row `id` (prevents fallback IDs like `support-0`).
- Actions:
  - Disable: `POST /api/v1/super-admin/users/{authId}/disable` with `{ reason }`
  - Enable: `POST /api/v1/super-admin/users/{authId}/enable`
- Disable requires confirmation modal with a reason input.
- Modal closes automatically after the API call completes.

### Provision Tab: Health + Lock + Backup

- Health: `GET /api/v1/super-admin/organisations/{id}/health`
  - UI shows a friendly “health card” view (not raw JSON).
- Lock tenant: `POST /api/v1/super-admin/organisations/{id}/lock`
- Backup tenant: `POST /api/v1/super-admin/organisations/{id}/backup`

### Settings Tab: Branding Color Pickers

- `brandPrimaryColor`, `brandSecondaryColor`, `brandAccentColor` converted to color pickers with hex.
- Saved via: `PUT /api/v1/super-admin/organisations/{id}/settings`

### Hide Impersonation Entries (Org list + details)

- Impersonate admin menu entry hidden from organisations list action menu and organisation details where applicable.

## Audit Logs (Super Admin)

- Page: `/super-admin/audit-logs`
- API produces entries containing `organisationName` and `details`, `before`, `after`, etc.
- UI changes:
  - Show `organisationName` column (instead of resource type).
  - Hide action summary under action (only show on hover in the action column).
  - Expand row on hover to show formatted details.
  - Infinite loading used instead of a pagination button.

## Billing and Subscription

- Dunning Policy:
  - GET: `/api/v1/super-admin/billing/dunning-policy`
  - PUT: `/api/v1/super-admin/billing/dunning-policy`
  - Triggered from Billing & Subscription tab and navigates to the dunning policy page.

## Dashboard Metrics Labels

- Labels updated:
  - `MRR` -> `Monthly Recurring Revenue`
  - `ARR` -> `Annual Recurring Revenue`

## Header Notifications Consistency

- Organisation detail header now uses the same notification behavior as the global super-admin header.
- Shared implementation extracted into `src/components/shared/SuperAdminHeaderActions.tsx` and reused.

## Admin Clients

Page: `/admin/clients`

### Clients List + Filters

- List API integrated: `GET /api/v1/clients`
- Query params wired on admin clients page:
  - `page`, `pageSize`, `sortBy`, `sortOrder`
  - `search`
  - `status`
  - `stage`
  - `therapistId`
  - `clientType`
  - `hasPortalAccess`
  - `hasPendingTasks`
  - `hasNoSessions`
  - `needsFollowUp`
  - `unassigned`
- Admin clients page refetches on mount/arg change via RTK Query so revisiting the route calls the endpoint again.
- Filter sidebar was updated from mock checklist/template UI to API-backed filter controls.
- `Clear all` resets filters and search to the default list request.

### Create Client

- Create API integrated: `POST /api/v1/clients`
- Shared payload mapper lives in:
  - `src/store/api/admin/createClientPayload.ts`
- Create modal updated to match supported client API fields:
  - `status`
  - `assignedTherapistId`
  - `clientType`
  - `stage`
  - address, contact, referral, insurance, portal fields
- Therapist dropdown source:
  - `GET /api/v1/admin/users?page=1&pageSize=100&role=THERAPIST&active=true`
- Portal email behavior:
  - Uses the primary email when portal access is enabled.
- `needsFollowUp` is now included in the create payload when enabled.
- Create errors no longer use `window.alert`; admin clients page uses a fixed toast message instead.

### Edit Client

- Client detail API integrated: `GET /api/v1/clients/{id}`
- Eye icon and profile open flow now fetch full client detail before rendering overview/profile content.
- Edit modal implemented by reusing the same tabbed modal UI as create mode.
- Edit entry points:
  - Overview header pencil
  - Row action menu `Edit Client`
- Update API path:
  - `PATCH /api/v1/clients/{id}`
- Edit now sends only changed fields:
  - Diff is built by comparing mapped initial form values against current form values.
  - Partial payload builder lives in `src/store/api/admin/createClientPayload.ts`.
- Fixed edit mapping for select fields returned by API using display labels:
  - `status`
  - `stage`
  - `clientType`
- Fixed edit crash before request:
  - Payload mapper no longer assumes `fullName` and `email` are always defined before calling `.trim()`.

### Delete Client

- Delete API integrated: `DELETE /api/v1/clients/{id}`
- Delete confirmation modal now calls the real endpoint.
- Confirm button shows loading state while request is in flight.
- On success:
  - Modal closes
  - Profile closes
  - Client is removed from local list state
  - Success toast is shown

### Client Profile / Overview

- Profile detail/header/overview now use `GET /api/v1/clients/{id}` data instead of only list-row summary data.
- Overview mapping fixes:
  - Address now renders actual API-backed address instead of hardcoded placeholder text.
  - Portal section prefers `portalEmail` from client detail, falling back to `email`.

### Portal Access

- Portal access update API integrated:
  - `PUT /api/v1/clients/{id}/portal-access`
  - body: `{ enable, email }`
- Overview tab portal actions now call the real endpoint.
- If portal access is enabled:
  - `Disable Portal Access` opens confirmation and then calls the endpoint.
- If portal access is disabled:
  - `Enable Portal Access` is shown and calls the endpoint directly.
- Selected client state and current list row are updated after successful portal access change.

### Date Picker Fixes

- Shared date picker updated so form-controlled dates are stored as strings (`YYYY-MM-DD`) instead of raw `Date` objects.
- This fixed DOB validation error:
  - `Invalid input: expected string, received date`
- Calendar year dropdown was expanded:
  - now supports years from `1900` through the current year
  - no longer starts around current year minus 20.

### Files Touched

- `src/pages/admin/clients/index.tsx`
- `src/store/api/admin/clients.api.ts`
- `src/store/api/admin/createClientPayload.ts`
- `src/components/admin/clients/AddNewClientModal/index.tsx`
- `src/components/admin/clients/AddNewClientModal/ClinicalTab.tsx`
- `src/components/admin/clients/AddNewClientModal/constants.ts`
- `src/components/admin/clients/ClientsFilterSidebar.tsx`
- `src/components/admin/clients/ClientProfile/index.tsx`
- `src/components/admin/clients/ClientProfile/OverviewTab.tsx`
- `src/components/admin/clients/ClientProfile/PortalAccessSection.tsx`
- `src/components/admin/clients/ClientProfile/DeleteClientModal.tsx`
- `src/components/shared/ConfirmationModal.tsx`
- `src/components/form/CustomDatePicker.tsx`
- `src/components/appointment-sections/Calendar.tsx`
- `src/types/client.type.ts`
- `src/types/add-client.type.ts`

## Notable Files (Entry Points)

- Super Admin API public entrypoint: `src/store/api/superAdminApi.ts`
- Super Admin API internal folder: `src/store/api/super-admin/`
- Notification UI components: `src/components/notification/`
- Organisation detail page: `src/pages/super-admin/organisations/details/index.tsx`
- Organisation users tab: `src/pages/super-admin/organisations/details/tabs/UsersTab.tsx`
- Audit logs page: `src/pages/super-admin/audit-logs/index.tsx`

## Known Constraints / Notes

- Trigger metadata endpoint is optional; UI falls back to hardcoded event/entity keys if it fails.
- Notification history API currently has no “mark as read” endpoint; UI uses count from returned list.
- Some earlier questions (e.g. Azure Static Web Apps 404 for direct URL access to reset-password route) may require route rewrites in Azure config; not handled in the implemented changes captured here.
