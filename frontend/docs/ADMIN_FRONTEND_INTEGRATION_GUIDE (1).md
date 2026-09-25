# Admin Frontend Integration Guide

## Purpose
This document lists the backend APIs required for tenant admin frontend integration.

## Base
- Base URL: `/api/v1`
- Auth: `Authorization: Bearer <access_token>`
- Content type: `application/json` (except upload/download endpoints)

## Tenant Context (Frontend)
For tenant-admin APIs, tenant context must be resolved per request. You do not need subdomain routing to use this backend.

Option 1: Resolve tenant at login
- Send tenant details in login payload:
```json
{
  "username": "admin@clinic.com",
  "password": "********",
  "orgIdentifier": "slug",
  "orgValue": "northstar-clinic"
}
```

Option 2: Send tenant header in API calls
- `X-Tenant-Schema: tenant_8`
- or `X-Tenant-Subdomain: northstar`

Recommended frontend behavior:
- Always send `Authorization: Bearer <token>`.
- For tenant-admin calls, also send one tenant header (`X-Tenant-Schema` preferred).
- Do not send tenant headers for platform super-admin/public endpoints unless required.

## Auth APIs
| Method | Path | Usage |
|---|---|---|
| POST | `/api/v1/auth/login` | Login and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout session |
| GET | `/api/v1/auth/me` | Get current user + roles/permissions |
| POST | `/api/v1/auth/resolve-tenant` | Resolve tenant before login (if needed) |
| GET | `/api/v1/auth/login-context` | Fetch login context metadata |

## Dashboard (Tenant Admin)
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/admin/dashboard/summary` | Single endpoint for top cards: active clients, today's sessions, pending tasks, billing overview |

### Legacy/Composable dashboard data APIs
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/clients/stats` | Client stats |
| GET | `/api/v1/sessions/upcoming` | Upcoming sessions |
| GET | `/api/v1/sessions/overdue` | Overdue sessions |
| GET | `/api/v1/tasks/pending/count` | Pending tasks count |
| GET | `/api/v1/billing/statistics` | Billing stats |

## Clients Module
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/clients` | List clients |
| GET | `/api/v1/clients/{id}` | Client detail |
| POST | `/api/v1/clients` | Create client |
| PUT | `/api/v1/clients/{id}` | Update client |
| PATCH | `/api/v1/clients/{id}` | Partial update client |
| DELETE | `/api/v1/clients/{id}` | Delete client |
| GET | `/api/v1/clients/{id}/history` | Client history timeline |
| GET | `/api/v1/clients/{id}/email-history` | Client email/communication history |
| GET | `/api/v1/clients/{id}/stage-durations` | Stage durations |
| GET | `/api/v1/clients/{clientId}/sessions` | Sessions for client |
| GET | `/api/v1/clients/{clientId}/sessions/summary` | Session KPI summary for client detail tab |
| GET | `/api/v1/clients/export` | Export clients |

### Client Create/Update Payload Compatibility
`POST /api/v1/clients`, `PUT /api/v1/clients/{id}`, and `PATCH /api/v1/clients/{id}` now support both standard fields and legacy-compatible aliases in the same payload.

Supported compatibility fields:
- `timezone`
- `legacyAddress` or `addressLegacy`
- `stateLegacy`
- `zipCodeLegacy`
- `emergencyContactLegacy`
- `startDate`
- `legacyReferral`
- `referringPersonName`
- `referralType`
- `employmentStatus`
- `educationLevel`
- `numberOfDependents` or `dependents`
- `priority`
- `dueDate` or `followUpDate`
- `generalNotes`
- `followUpNotes`
- `referralNotes`

Example create/update request:
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "status": "active",
  "stage": "intake",
  "timezone": "America/New_York",
  "streetAddress1": "123 Main Street",
  "city": "Toronto",
  "province": "Ontario",
  "postalCode": "M5H 2N2",
  "country": "Canada",
  "legacyAddress": "123 Main Street, Toronto, Ontario",
  "stateLegacy": "Ontario",
  "zipCodeLegacy": "M5H 2N2",
  "emergencyContactName": "Jane Doe",
  "emergencyContactPhone": "+1-555-987-6543",
  "emergencyContactRelationship": "Spouse",
  "emergencyContactLegacy": "Jane Doe (Spouse) +1-555-987-6543",
  "referringPersonName": "Dr. Smith",
  "legacyReferral": "Website",
  "referralType": "External",
  "referralNotes": "Referred for anxiety intake",
  "employmentStatus": "EMPLOYED_FULL_TIME",
  "educationLevel": "BACHELOR",
  "numberOfDependents": 2,
  "startDate": "2026-04-23",
  "priority": "High",
  "dueDate": "2026-05-01",
  "followUpNotes": "Call after first session",
  "generalNotes": "Prefers morning appointments",
  "hasPortalAccess": true,
  "portalEmail": "john.portal@example.com",
  "emailNotifications": true
}
```

Example response (`POST` returns `201`, `PUT/PATCH` returns `200`):
```json
{
  "id": 451,
  "clientId": "CL-2026-0451",
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "dateOfBirth": null,
  "gender": null,
  "maritalStatus": null,
  "preferredLanguage": null,
  "pronouns": null,
  "status": "Active",
  "stage": "Intake",
  "clientType": null,
  "assignedTherapistId": null,
  "assignedTherapistName": null,
  "streetAddress1": "123 Main Street",
  "streetAddress2": null,
  "city": "Toronto",
  "province": "Ontario",
  "postalCode": "M5H 2N2",
  "country": "Canada",
  "emergencyContactName": "Jane Doe",
  "emergencyContactPhone": "+1-555-987-6543",
  "emergencyContactRelationship": "Spouse",
  "insuranceProvider": null,
  "policyNumber": null,
  "groupNumber": null,
  "insurancePhone": null,
  "copayAmount": null,
  "deductible": null,
  "referrerName": "Dr. Smith",
  "referralDate": null,
  "referenceNumber": null,
  "clientSource": "Website",
  "hasPortalAccess": true,
  "portalEmail": "john.portal@example.com",
  "emailNotifications": true,
  "notes": "Prefers morning appointments",
  "serviceType": null,
  "serviceFrequency": null,
  "createdAt": "2026-04-23T08:00:00Z",
  "updatedAt": "2026-04-23T08:00:00Z",
  "lastSessionDate": null,
  "nextAppointmentDate": null
}
```

### Normalized Client Endpoints: Alias Compatibility
These endpoints also accept legacy-compatible aliases in their request payloads:

| Method | Path | Alias support |
|---|---|---|
| POST/PUT | `/api/v1/clients/{clientId}/addresses` and `/api/v1/clients/{clientId}/addresses/{addressId}` | `legacyAddress`/`addressLegacy` -> `streetAddress1`, `stateLegacy` -> `stateProvince`, `zipCodeLegacy` -> `postalCode` |
| PUT | `/api/v1/clients/{clientId}/referral` | `referringPersonName` -> `referrerName`, `legacyReferral` -> `clientSource` |
| PUT | `/api/v1/clients/{clientId}/employment` | `numberOfDependents` -> `dependents` |

Address request example:
```json
{
  "addressType": "HOME",
  "legacyAddress": "123 Main Street",
  "city": "Toronto",
  "stateLegacy": "Ontario",
  "zipCodeLegacy": "M5H 2N2",
  "country": "Canada",
  "isPrimary": true
}
```

Referral request example:
```json
{
  "referringPersonName": "Dr. Jane Smith",
  "legacyReferral": "Website",
  "referralType": "External",
  "referralNotes": "Referred for anxiety treatment"
}
```

Employment request example:
```json
{
  "employmentStatus": "EMPLOYED_FULL_TIME",
  "educationLevel": "BACHELOR",
  "numberOfDependents": 2
}
```

### Duplicate Detection (User & Access -> Duplicate Detection)
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/clients/duplicates` | Scan and return potential duplicate groups (also use for `Refresh Scan`) |
| POST | `/api/v1/clients/{id}/mark-duplicate` | Mark selected client as duplicate of another client |
| POST | `/api/v1/clients/{id}/unmark-duplicate` | Undo duplicate marking |
| GET | `/api/v1/clients/{id}` | View full client record from duplicate card |

Request body for `POST /api/v1/clients/{id}/mark-duplicate`:
```json
{
  "duplicateOfClientId": 456
}
```

### Duplicate Detection UI mapping
| UI action | API |
|---|---|
| Load duplicate groups page | `GET /api/v1/clients/duplicates` |
| Refresh Scan button | `GET /api/v1/clients/duplicates` |
| Mark as Duplicate button (confirmation modal) | `POST /api/v1/clients/{duplicateClientId}/mark-duplicate` with `duplicateOfClientId=<keepClientId>` |
| Keep this / reverse decision | `POST /api/v1/clients/{id}/unmark-duplicate` |
| View Full Record | `GET /api/v1/clients/{id}` |

## Scheduling Module
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/sessions` | List/filter sessions (supports dashboard filters and search) |
| GET | `/api/v1/sessions/{id}` | Session detail |
| POST | `/api/v1/sessions` | Create session |
| PUT | `/api/v1/sessions/{id}` | Update session |
| PUT | `/api/v1/sessions/{id}/status` | Update session status |
| GET | `/api/v1/sessions/recent` | Recent sessions |
| GET | `/api/v1/sessions/upcoming` | Upcoming sessions |
| GET | `/api/v1/sessions/overdue` | Overdue sessions |
| GET | `/api/v1/sessions/history/{year}/{month}/{day}/day` | Day view |
| GET | `/api/v1/sessions/history/{year}/{week}/week` | Week view |
| GET | `/api/v1/sessions/history/{year}/{month}/month` | Month view |
| GET | `/api/v1/sessions/conflicts/check` | Check therapist/room conflicts before booking |
| GET | `/api/v1/sessions/availability` | Availability matrix for selected date |
| GET | `/api/v1/sessions/stats/overview` | Session counters for cards (`today`, `thisWeek`, `thisMonth`, `upcoming`, `completed`, `cancelled`, `total`) |
| GET | `/api/v1/sessions/bulk-upload/template` | Download CSV template for import |
| GET | `/api/v1/sessions/bulk-upload/template/static` | Download static CSV template from resources |
| GET | `/api/v1/sessions/bulk-upload/template.xlsx` | Download Excel (.xlsx) template for import |
| GET | `/api/v1/sessions/bulk-upload/template.xlsx/static` | Download static Excel (.xlsx) template from resources |
| POST | `/api/v1/sessions/bulk-upload/import` | Import sessions from CSV text payload |
| POST | `/api/v1/sessions/bulk-upload/import-file` | Import sessions from CSV or XLSX file upload |

Template storage notes:
- Static CSV template path: `src/main/resources/templates/session_upload_template.csv`
- Static XLSX template path: `src/main/resources/templates/session_upload_template.xlsx`
- If static template file is missing, backend automatically falls back to generated template.
| GET | `/api/v1/rooms` | Rooms list |
| GET | `/api/v1/rooms/available` | Exact-slot room options by therapist/date/sessionType/service/duration |
| GET | `/api/v1/rooms/check-availability` | Detailed room/therapist/client availability conflicts |
| POST | `/api/v1/rooms` | Create room |
| PUT | `/api/v1/rooms/{roomId}` | Update room |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room |
| GET | `/api/v1/therapist-availability/availability/slots` | Available slots for selected therapist/date/service |
| GET | `/api/v1/portal/rooms/available-slot` | Client-portal exact-slot room options for selected therapist/date/sessionType |

### Scheduling filters for frontend (GET `/api/v1/sessions`)
- `page`, `pageSize`
- `startDate`, `endDate`
- `therapistId`, `clientId`
- `clientSearch` (new, partial match on client name)
- `status`, `sessionType`
- `serviceId` (new)
- `serviceCode` (new, partial match)
- `roomId`
- `mySessionsOnly` (new, boolean)
- `includeHiddenServices`

Examples:
- `/api/v1/sessions?clientSearch=sarah`
- `/api/v1/sessions?serviceCode=PSY`
- `/api/v1/sessions?serviceId=10`
- `/api/v1/sessions?mySessionsOnly=true`

### Scheduling dropdown data sources (Frontend wiring)
| UI dropdown/filter | API |
|---|---|
| Clients | `GET /api/v1/clients` |
| Services | `GET /api/v1/billing/services` |
| Therapists | `GET /api/v1/users?role=THERAPIST` |
| Time slots | `GET /api/v1/therapist-availability/availability/slots?therapistId=&date=&serviceId=` |
| Room availability | `GET /api/v1/rooms/check-availability` |

### Session stats APIs (implemented)
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/sessions/stats/overview` | Staff stats endpoint with optional filters: `therapistId`, `clientId`, `startDate`, `endDate`, `timezone` |
| GET | `/api/v1/therapists/{therapistId}/bookings/count` | Therapist booking counters (`total`, `upcoming`, `completed`, `cancelled`) |
| GET | `/api/v1/therapists/{therapistId}/session-stats` | Therapist detailed stats (`sessionsByStatus`, `sessionsByType`, `avgDuration`, `totalHours`) |
| GET | `/api/v1/clients/{clientId}/sessions/summary` | Client session summary cards |
| GET | `/api/v1/portal/me/sessions-stats` | Client self stats in portal (`today`, `thisWeek`, `thisMonth`, `upcoming`, `completed`, `cancelled`, `total`) |

### Screenshot alignment status
- Already available in backend (added here for documentation completeness):
  - Conflict checks (`/api/v1/sessions/conflicts/check`)
  - Availability (`/api/v1/sessions/availability`)
  - Bulk import APIs (`/api/v1/sessions/bulk-upload/*`)
  - Room availability (`/api/v1/rooms/check-availability`)
  - Therapist slots (`/api/v1/therapist-availability/availability/slots`)
- Implemented now:
  - Service filtering on sessions list (`serviceId` / `serviceCode`)
  - Client name search on sessions list (`clientSearch`)
  - My Sessions only toggle (`mySessionsOnly`)
- Still needs review:
  - Therapist schedule endpoints in `TherapistAvailabilityController` appear to have a possible double path prefix and should be validated during frontend integration.

## Tasks Module
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/tasks` | List/filter tasks (main dashboard) |
| GET | `/api/v1/tasks/history` | Task history list/filter/search |
| GET | `/api/v1/tasks/stats` | Task stats |
| GET | `/api/v1/tasks/recent` | Recent tasks |
| GET | `/api/v1/tasks/upcoming` | Upcoming deadlines |
| GET | `/api/v1/tasks/pending/count` | Pending task count |
| GET | `/api/v1/tasks/{id}` | Task detail |
| POST | `/api/v1/tasks` | Create task |
| PUT | `/api/v1/tasks/{id}` | Update task |
| DELETE | `/api/v1/tasks/{id}` | Delete task |
| GET | `/api/v1/tasks/{taskId}/comments` | List task comments |
| POST | `/api/v1/tasks/{taskId}/comments` | Add task comment |
| PUT | `/api/v1/tasks/{taskId}/comments/{commentId}` | Edit task comment |
| DELETE | `/api/v1/tasks/{taskId}/comments/{commentId}` | Delete task comment |
| POST | `/api/v1/tasks/check-overdue` | Mark/check overdue tasks |

### Tasks filters for frontend (GET `/api/v1/tasks` and `/api/v1/tasks/history`)
- `page`, `pageSize`
- `status` (`pending`, `in_progress`, `completed`, `overdue`, `cancelled`)
- `priority` (`low`, `medium`, `high`, `urgent`)
- `assignedToId`
- `clientId`
- `search` (matches title/description/client/assignee)
- `dateFilter` (`due_date`, `overdue`, `this_week`)
- `fromDate`, `toDate` (due date range)
- `sortBy`, `sortOrder`

Examples:
- `/api/v1/tasks?status=in_progress&priority=high`
- `/api/v1/tasks?search=assessment`
- `/api/v1/tasks?dateFilter=overdue`
- `/api/v1/tasks?fromDate=2026-04-01&toDate=2026-04-30`
- `/api/v1/tasks/history?search=sarah&status=completed`

### Task dashboard mapping (UI -> API)
| UI action | API |
|---|---|
| Top cards (`Total`, `Pending`, `In Progress`, `Completed`, `Needs Attention`) | `GET /api/v1/tasks/stats` |
| All tasks board | `GET /api/v1/tasks` |
| Search bar | `GET /api/v1/tasks?search=` |
| Filters drawer (status/priority/assignee/from/to) | `GET /api/v1/tasks` with query params |
| All Dates dropdown (`Due Date`, `Overdue`, `This Week`) | `GET /api/v1/tasks?dateFilter=` |
| View History page | `GET /api/v1/tasks/history` |
| Task by status tab | `GET /api/v1/tasks?status=` |
| Add task | `POST /api/v1/tasks` |
| Edit task | `PUT /api/v1/tasks/{id}` |
| Delete task | `DELETE /api/v1/tasks/{id}` |
| View task details | `GET /api/v1/tasks/{id}` |
| Assign task to user | `POST/PUT /api/v1/tasks` with `assignedToId` |
| Task comments timeline | `GET/POST/PUT/DELETE /api/v1/tasks/{taskId}/comments*` |

## Billing Module
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/billing/statistics` | Billing overview cards |
| GET | `/api/v1/billing/clients/{clientId}/stats` | Client-wise billing stats (`due`, `paid`, counts by status) |
| GET | `/api/v1/billing/billing` | Billing records list |
| GET | `/api/v1/billing/history` | Billing history |
| GET | `/api/v1/billing/services` | Services list |
| POST | `/api/v1/billing/services` | Create service |
| PUT | `/api/v1/billing/services/{id}` | Update service |
| POST | `/api/v1/billing/sessions/{sessionId}/billing` | Create billing for session |
| GET | `/api/v1/billing/sessions/{sessionId}/billing` | Billing by session |
| PATCH | `/api/v1/billing/billing/{id}/status` | Change billing status |
| POST | `/api/v1/billing/billing/{id}/record-payment` | Record payment |
| GET | `/api/v1/billing/billing/{id}/invoice-preview` | Invoice preview |
| GET | `/api/v1/billing/billing/{id}/invoice-download` | Invoice download |
| POST | `/api/v1/billing/billing/{id}/send-invoice-email` | Email invoice to client |
| PATCH | `/api/v1/billing/billing/{id}/discount` | Apply/remove discount |

### Billing filters for frontend (GET `/api/v1/billing/billing`)
- `page`, `size`, `sort`, `direction`
- `clientId`
- `clientSearch` (new, partial match by client name)
- `therapistId`
- `status` (enum: `pending`, `billed`, `paid`, `denied`, `refunded`, `follow_up`)
- `serviceCode` (already filterable)
- `clientType`
- `paymentMethod`
- `startDate`, `endDate`
- `minAmount`, `maxAmount`

Examples:
- `/api/v1/billing/billing?serviceCode=PSY-01`
- `/api/v1/billing/billing?clientSearch=sarah`
- `/api/v1/billing/billing?status=follow_up&startDate=2026-04-01&endDate=2026-04-30`
- `/api/v1/billing/billing?status=paid`

### Billing action mapping (UI -> API)
| UI action | API |
|---|---|
| Top cards (Outstanding, Total Collected, Active Clients, Total Records) | `GET /api/v1/billing/statistics` |
| Client profile billing cards (Due/Paid/Total) | `GET /api/v1/billing/clients/{clientId}/stats` |
| Billing table list | `GET /api/v1/billing/billing` |
| Pay now / Record payment modal | `POST /api/v1/billing/billing/{id}/record-payment` |
| Mark as Billed/Paid/Denied/Follow-up/Pending | `PATCH /api/v1/billing/billing/{id}/status` |
| Preview Invoice | `GET /api/v1/billing/billing/{id}/invoice-preview` |
| Download Invoice | `GET /api/v1/billing/billing/{id}/invoice-download` |
| Email Invoice | `POST /api/v1/billing/billing/{id}/send-invoice-email` |
| Apply Discount | `PATCH /api/v1/billing/billing/{id}/discount` |

### Billing screenshot alignment status
- Already available in backend:
  - `serviceCode` filter
  - `clientType` filter
  - date range filter
  - record payment modal APIs
  - invoice preview/download/email
  - status change and discount APIs
- Implemented now:
  - `clientSearch` filter for billing records list
  - unified enum `status` filter for billing records (`pending`, `billed`, `paid`, `denied`, `refunded`, `follow_up`)
  - `refunded` now works through payment records
  - statistics endpoint counts are aligned with billing statuses for top cards

## User & Access Module
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/admin/users` | List users (admin panel preferred endpoint) |
| GET | `/api/v1/admin/users/{id}` | User detail |
| POST | `/api/v1/admin/users` | Create user |
| PUT | `/api/v1/admin/users/{id}` | Update user |
| DELETE | `/api/v1/admin/users/{id}` | Delete user |
| POST | `/api/v1/admin/users/{id}/activate` | Activate user |
| POST | `/api/v1/admin/users/{id}/deactivate` | Deactivate user |
| POST | `/api/v1/admin/users/{id}/assign-role` | Assign/replace user roles |
| GET | `/api/v1/users` | Legacy users list (still available) |
| GET | `/api/v1/users/{id}` | Legacy user detail |
| POST | `/api/v1/users` | Legacy create |
| PUT | `/api/v1/users/{id}` | Legacy update |
| DELETE | `/api/v1/users/{id}` | Legacy delete |
| GET | `/api/v1/users/{userId}/profile` | Professional profile detail |
| POST | `/api/v1/users/{userId}/profile` | Create professional profile |
| PUT | `/api/v1/users/{userId}/profile` | Update professional profile |
| DELETE | `/api/v1/users/{userId}/profile` | Delete professional profile |
| GET | `/api/v1/users/supervisor-assignments` | List supervisor assignments |
| POST | `/api/v1/users/supervisor-assignments` | Assign supervisor |
| PUT | `/api/v1/users/supervisor-assignments/{id}` | Update assignment |
| DELETE | `/api/v1/users/supervisor-assignments/{id}` | Delete assignment |
| GET | `/api/v1/roles` | List roles |
| POST | `/api/v1/roles` | Create role |
| PUT | `/api/v1/roles/{id}` | Update role |
| DELETE | `/api/v1/roles/{id}` | Delete role |
| PUT | `/api/v1/roles/{id}/permissions` | Assign permissions to role |
| GET | `/api/v1/permissions` | List permissions |

### Unified Directory API (Recommended for role-based lists)
Use this endpoint when frontend needs reusable lists for therapists, admins, supervisors, other staff roles, and clients without creating separate integrations.

| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/admin/directory` | Unified paginated directory for `USER` and `CLIENT` entities |

Request query params:
- `page` (default: `1`)
- `pageSize` (default from backend pagination config)
- `entityType` (`USER` or `CLIENT`, default: `USER`)
- `search` (optional)
- `role` (optional; for `entityType=USER`, e.g. `THERAPIST`, `ADMIN`, `SUPERVISOR`)
- `active` (optional; for `entityType=USER`)
- `clientStatus` (optional; for `entityType=CLIENT`, e.g. `ACTIVE`, `INACTIVE`, `PENDING`)

Example requests:
- Therapists list:  
  `GET /api/v1/admin/directory?entityType=USER&role=THERAPIST&page=1&pageSize=25`
- Admins list:  
  `GET /api/v1/admin/directory?entityType=USER&role=ADMIN&page=1&pageSize=25`
- All clients list:  
  `GET /api/v1/admin/directory?entityType=CLIENT&page=1&pageSize=25`

Example response (`entityType=USER`):
```json
{
  "items": [
    {
      "id": 12,
      "entityType": "USER",
      "role": "THERAPIST",
      "roleId": 3,
      "roles": [
        {
          "roleId": 3,
          "role": "THERAPIST",
          "displayName": "Therapist"
        }
      ],
      "name": "Dr. Sarah Khan",
      "email": "sarah.khan@clinic.com",
      "username": "sarah.khan",
      "phone": "+1-555-123-9001",
      "active": true,
      "status": "ACTIVE",
      "profilePicture": "https://cdn.example.com/users/12.png",
      "clientId": null,
      "assignedTherapistId": null,
      "assignedTherapistName": null,
      "lastLogin": "2026-04-22T10:15:30Z",
      "createdAt": "2026-01-15T08:00:00Z",
      "updatedAt": "2026-04-22T10:15:30Z",
      "otherFields": {
        "timezone": "America/New_York",
        "availabilityStatus": "AVAILABLE"
      }
    }
  ],
  "totalCount": 1,
  "page": 1,
  "pageSize": 25,
  "totalPages": 1
}
```

Example response (`entityType=CLIENT`):
```json
{
  "items": [
    {
      "id": 451,
      "entityType": "CLIENT",
      "role": "CLIENT",
      "roleId": 5,
      "roles": [
        {
          "roleId": 5,
          "role": "CLIENT",
          "displayName": "Client"
        }
      ],
      "name": "John Doe",
      "email": "john.doe@email.com",
      "username": null,
      "phone": "+1-555-555-0001",
      "active": true,
      "status": "Active",
      "profilePicture": null,
      "clientId": "CL-2026-0451",
      "assignedTherapistId": 12,
      "assignedTherapistName": "Dr. Sarah Khan",
      "lastLogin": null,
      "createdAt": "2026-02-01T09:10:11Z",
      "updatedAt": "2026-04-20T14:11:00Z",
      "otherFields": {
        "stage": "INTAKE",
        "clientType": "INDIVIDUAL",
        "hasPortalAccess": true,
        "preferredLanguage": "English",
        "dateOfBirth": "1995-04-10"
      }
    }
  ],
  "totalCount": 1,
  "page": 1,
  "pageSize": 25,
  "totalPages": 1
}
```

### User Profiles filters (GET `/api/v1/admin/users`)
- `page`, `pageSize`
- `search` (name/username/email)
- `role`
- `active`

### Supervisor Assignments filters (GET `/api/v1/users/supervisor-assignments`)
- `supervisorId`
- `therapistId`
- `active`
- `search` (supervisor/therapist name, username, email)
- `requiredMeetingFrequency` (`WEEKLY`, `BIWEEKLY`, `MONTHLY`, etc.)

### Role Management filters (GET `/api/v1/roles`)
- `search` (role name, display name, description)

### User & role data model flow (verified)
- Authentication identity is created in `public.auth_identities`.
- Role bindings are saved in `public.auth_identity_roles`.
- Roles are managed in `public.roles`.
- Tenant staff profile is stored in tenant schema `users` (`auth_id` FK to `public.auth_identities`).
- Extended professional data is stored in tenant schema `user_profiles` and related `user_profile_*` tables.

## Therapist Profile Schedule (Screens Deep Check)
This section maps the 3 attached Therapist Profile Schedule screens (working days, multiple time ranges, virtual/in-person toggle, add/copy/delete rows, save changes) to backend APIs.

### Screen controls observed
- Day enable/disable checkbox (`Monday`, `Tuesday`, ...).
- Multiple shifts per day (example: `09:00-13:30` and `15:00-17:00` on same day).
- Per-row `Virtual` / `In-person` toggle.
- `+` add shift row for the day.
- Copy icon (duplicate day/row behavior in UI).
- Delete/trash icon for a row.
- Save changes button.

### API inventory for this feature (with request/response)
| Method | Path | Use in therapist schedule UI | Request (important fields) | Response (important fields) |
|---|---|---|---|---|
| GET | `/api/v1/users/{userId}/profile` | Load therapist profile in admin mode | Path: `userId` | `UserProfileResponse` including `workingHours`, `workingDays`, `availabilityStatus`, `timezone`, `virtualRoomId`, `availablePhysicalRoomIds` |
| PUT | `/api/v1/users/{userId}/profile` | Save full schedule changes for therapist (admin) | `UserProfileRequest` with `workingHours`, `workingDays`, `sessionDuration`, `availabilityStatus`, `timezone`, `virtualRoomId`, `availablePhysicalRoomIds` | Updated `UserProfileResponse` |
| PATCH | `/api/v1/users/{userId}/profile` | Partial update (same payload contract as PUT) | `UserProfileRequest` (only changed fields) | Updated `UserProfileResponse` |
| GET | `/api/v1/users/me/profile` | Load schedule for logged-in therapist self-edit mode | None | `UserProfileResponse` |
| PUT | `/api/v1/users/me/profile` | Save schedule in self-edit mode | `UserProfileRequest` | Updated `UserProfileResponse` |
| GET | `/api/v1/rooms?activeOnly=true` | Load room options to support in-person/virtual setup | Query: `activeOnly=true` | `RoomResponse[]` with `id`, `roomName`, `roomType`, `isActive` |
| GET | `/api/v1/therapist-availability/therapist-blocked-times?therapistId={id}` | Load blocked/unavailable intervals if UI shows exceptions | Query: `therapistId` | `TherapistBlockedTimeResponse[]` |
| POST | `/api/v1/therapist-availability/therapist-blocked-times` | Create blocked interval | `therapistId`, `startTime`, `endTime`, `blockType`, optional recurrence fields | Created `TherapistBlockedTimeResponse` |
| PATCH | `/api/v1/therapist-availability/therapist-blocked-times/{id}` | Update blocked interval | Same as create (partial allowed by service) | Updated `TherapistBlockedTimeResponse` |
| DELETE | `/api/v1/therapist-availability/therapist-blocked-times/{id}` | Delete blocked interval | Path: `id` | `204 No Content` |
| GET | `/api/v1/therapist-availability/availability/slots?therapistId={id}&date={yyyy-MM-dd}&serviceId={id}` | Validate slot generation after schedule changes | Query: `therapistId`, `date`, `serviceId`, optional `sessionType` (`online`/`in-person`) | `AvailableSlotResponse[]` (`time`, `localTime`, `available`, `therapistBusy`, `roomBusy`) |

### Request examples (frontend-ready)
`PUT /api/v1/users/{userId}/profile` (schedule-focused payload):
```json
{
  "workingHours": "[{\"day\":\"monday\",\"enabled\":true,\"start\":\"09:00\",\"end\":\"13:30\",\"mode\":\"virtual\"},{\"day\":\"monday\",\"enabled\":true,\"start\":\"15:00\",\"end\":\"17:00\",\"mode\":\"in-person\"},{\"day\":\"tuesday\",\"enabled\":true,\"start\":\"09:00\",\"end\":\"17:00\",\"mode\":\"both\"}]",
  "workingDays": ["monday", "tuesday"],
  "sessionDuration": 50,
  "availabilityStatus": "AVAILABLE",
  "timezone": "Asia/Karachi",
  "virtualRoomId": 12,
  "availablePhysicalRoomIds": [3, 7]
}
```

`POST /api/v1/therapist-availability/therapist-blocked-times`:
```json
{
  "therapistId": 45,
  "startTime": "2026-04-30T09:00:00Z",
  "endTime": "2026-04-30T12:00:00Z",
  "allDay": false,
  "blockType": "MEETING",
  "reason": "Clinical supervision",
  "isRecurring": false,
  "isActive": true
}
```

### Response examples (frontend parsing targets)
`GET /api/v1/users/{userId}/profile`:
```json
{
  "id": 45,
  "fullName": "Dr. Sarah Khan",
  "workingDays": ["MONDAY", "TUESDAY"],
  "workingHours": "[{\"day\":\"monday\",\"enabled\":true,\"start\":\"09:00\",\"end\":\"13:30\",\"mode\":\"virtual\"},{\"day\":\"monday\",\"enabled\":true,\"start\":\"15:00\",\"end\":\"17:00\",\"mode\":\"in-person\"}]",
  "sessionDuration": 50,
  "availabilityStatus": "AVAILABLE",
  "timezone": "Asia/Karachi",
  "virtualRoomId": 12,
  "availablePhysicalRoomIds": [3, 7]
}
```

`GET /api/v1/rooms?activeOnly=true`:
```json
[
  {
    "id": 12,
    "roomNumber": "VR-01",
    "roomName": "Virtual Room 01",
    "isActive": true,
    "roomType": "VIRTUAL"
  },
  {
    "id": 3,
    "roomNumber": "101",
    "roomName": "Therapy Room 101",
    "isActive": true,
    "roomType": "PHYSICAL"
  }
]
```

`GET /api/v1/therapist-availability/availability/slots?...`:
```json
[
  {
    "time": "2026-04-30T09:00:00Z",
    "timezone": "Asia/Karachi",
    "localTime": "2026-04-30T14:00:00",
    "available": true,
    "therapistBusy": false,
    "roomBusy": false
  }
]
```

### UI-to-API mapping for the 3 screenshots
| UI action | Backend behavior |
|---|---|
| Toggle day on | Include that day in `workingHours` JSON with at least one shift row |
| Toggle day off (`Not available`) | Remove all `workingHours` rows for that day, optionally remove from `workingDays` |
| Add shift (`+`) | Add another row in `workingHours` with same `day` and new `start`/`end` |
| Select row modality `Virtual/In-person` | Persist per shift as `mode` (`virtual`, `in-person`, `both`) in `workingHours` JSON |
| Delete shift (trash) | Remove that row from `workingHours` and `PUT/PATCH` profile |
| Copy icon | No dedicated API; copy logic should be frontend-side, then persist via profile update |
| Save changes | `PUT /api/v1/users/{userId}/profile` (admin) or `PUT /api/v1/users/me/profile` (self) |

### Current backend notes
1. Per-shift modality is now persisted in `workingHours[*].mode`.
2. Slot API supports optional `sessionType` filtering:
   - `GET /api/v1/therapist-availability/availability/slots?...&sessionType=online`
   - `GET /api/v1/portal/therapists/{therapistId}/availability?...&sessionType=in-person`
3. Base room configuration remains profile-level (`virtualRoomId`, `availablePhysicalRoomIds`) and is used when resolving availability.
4. No dedicated duplicate endpoint for the copy icon (frontend should clone rows locally and save).
5. `availabilityStatus` enum currently supports `AVAILABLE`, `BUSY`, `UNAVAILABLE`.

### Validation rules (implemented)
1. Overlapping shifts are rejected per day.
2. Shift ordering is strict: each next shift `start` must be greater than previous shift `end` for the same day.
3. If invalid, backend returns `400 Bad Request` with clear message.

Example invalid payload:
```json
{
  "workingHours": "[{\"day\":\"monday\",\"enabled\":true,\"start\":\"09:00\",\"end\":\"12:00\"},{\"day\":\"monday\",\"enabled\":true,\"start\":\"11:00\",\"end\":\"17:00\"}]"
}
```

Example error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid working-hours shifts for MONDAY: shift 2 start time (11:00) must be greater than previous shift end time (12:00)"
}
```

### Integration guidance for frontend now
1. Persist per-row schedule + modality using `workingHours` JSON as source of truth.
2. When user selects booking context, pass `sessionType` to slots endpoints for accurate filtering.
3. Persist room configuration through `virtualRoomId` + `availablePhysicalRoomIds`.
4. Use blocked-times APIs for exceptions (vacation/meeting/sick leave), not for base weekly schedule.

## Content Module
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/library/categories` | Library categories |
| GET | `/api/v1/library/categories/{id}` | Get single category |
| POST | `/api/v1/library/categories` | Create category |
| PUT | `/api/v1/library/categories/{id}` | Update category |
| DELETE | `/api/v1/library/categories/{id}` | Delete category |
| GET | `/api/v1/library/entries` | Library entries |
| GET | `/api/v1/library/entries/with-connections` | Library entries with usage + connected entries (with categories) |
| GET | `/api/v1/library/entries/{id}` | Get single entry |
| POST | `/api/v1/library/entries` | Create library entry |
| POST | `/api/v1/library/entries/bulk` | Bulk add library entries (single category) |
| DELETE | `/api/v1/library/entries/bulk` | Bulk delete library entries |
| PUT | `/api/v1/library/entries/{id}` | Update library entry |
| DELETE | `/api/v1/library/entries/{id}` | Delete library entry |
| GET | `/api/v1/library/search` | Search entries (`q`, optional `categoryId`) |
| POST | `/api/v1/library/entries/{id}/increment-usage` | Increment "Used X times" counter |
| GET | `/api/v1/forms/templates` | Form templates |
| POST | `/api/v1/forms/templates` | Create form template |
| PATCH | `/api/v1/forms/templates/{id}` | Update form template |
| DELETE | `/api/v1/forms/templates/{id}` | Delete form template |
| GET | `/api/v1/notes/clients/{clientId}/notes` | Client notes |
| POST | `/api/v1/notes` | Create note |
| PATCH | `/api/v1/notes/{id}` | Update note |
| DELETE | `/api/v1/notes/{id}` | Delete note |
| GET | `/api/v1/clients/{clientId}/documents` | Client documents |
| POST | `/api/v1/clients/{clientId}/documents` | Upload document |
| GET | `/api/v1/documents/reviews/dashboard` | Review dashboard data |

### Clinical Content Library UI mapping (tabs + list + add/delete/search + bulk add)
| UI action | API |
|---|---|
| Load category tabs/dropdown (dynamic) | `GET /api/v1/library/categories` |
| Add/Edit/Delete category | `POST/PUT/DELETE /api/v1/library/categories*` |
| List entries for selected category tab | `GET /api/v1/library/entries?categoryId={id}` |
| Single API for list + usage + connected entries with categories | `GET /api/v1/library/entries/with-connections?categoryId={id?}` |
| Add entry modal | `POST /api/v1/library/entries` |
| Edit entry | `PUT /api/v1/library/entries/{id}` |
| Delete single entry | `DELETE /api/v1/library/entries/{id}` |
| Delete selected entries | `DELETE /api/v1/library/entries/bulk` |
| Search by title/content/tag/category name | `GET /api/v1/library/search?q={term}&categoryId={id?}` |
| Bulk add modal (paste title + content rows) | `POST /api/v1/library/entries/bulk` |
| Increment usage when entry is used in workflow | `POST /api/v1/library/entries/{id}/increment-usage` |

## System Module (Tenant Admin -> System Settings Tabs)

### 1) System Options tab
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/system-options/categories` | List categories |
| GET | `/api/v1/system-options/categories/{id}` | Category details with options |
| POST | `/api/v1/system-options/categories` | Create category |
| PUT | `/api/v1/system-options/categories/{id}` | Update category |
| DELETE | `/api/v1/system-options/categories/{id}` | Delete category |
| GET | `/api/v1/system-options?categoryId={id}` | List options by category |
| GET | `/api/v1/system-options/by-category/{categoryKey}` | List active options by category key |
| POST | `/api/v1/system-options` | Create option |
| PUT | `/api/v1/system-options/{id}` | Update option |
| DELETE | `/api/v1/system-options/{id}` | Delete option |

### 2) Service Prices tab
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/billing/services` | List service codes/prices |
| GET | `/api/v1/billing/services/{id}` | Get service |
| POST | `/api/v1/billing/services` | Create service code |
| PUT | `/api/v1/billing/services/{id}` | Update service code/price/duration/visibility |

### 3) Service Visibility tab
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/billing/services` | Load visibility flags (`therapistVisible`, `clientPortalVisible`) |
| PUT | `/api/v1/billing/services/{id}` | Toggle row visibility flags |
| POST | `/api/v1/billing/services/visibility/therapists/show-all` | Bulk show all to therapists |
| POST | `/api/v1/billing/services/visibility/therapists/hide-all` | Bulk hide all from therapists |

### 4) Therapy Rooms tab
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/rooms` | List rooms |
| GET | `/api/v1/rooms/{roomId}` | Get room |
| POST | `/api/v1/rooms` | Create room |
| PUT | `/api/v1/rooms/{roomId}` | Update room |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room |
| GET | `/api/v1/rooms/{roomId}/availability` | Check room availability |
| GET | `/api/v1/rooms/check-availability` | Detailed conflict check |

### Room Management API contract (verified)
Request DTO (`RoomRequest`) fields:
- `roomNumber` (required)
- `roomName` (required)
- `capacity` (optional)
- `equipment` (optional)
- `isActive` (required)
- `roomType` (required: `PHYSICAL` or `VIRTUAL`)

Response DTO (`RoomResponse`) fields:
- `id`, `roomNumber`, `roomName`, `capacity`, `equipment`
- `isActive`, `roomType`
- `createdAt`, `updatedAt`

Example create/update request:
```json
{
  "roomNumber": "VR-02",
  "roomName": "Virtual Room 02",
  "capacity": 2,
  "equipment": "Zoom-capable",
  "isActive": true,
  "roomType": "VIRTUAL"
}
```

Example list response:
```json
[
  {
    "id": 12,
    "roomNumber": "VR-02",
    "roomName": "Virtual Room 02",
    "capacity": 2,
    "equipment": "Zoom-capable",
    "isActive": true,
    "roomType": "VIRTUAL",
    "createdAt": "2026-04-24T10:00:00Z",
    "updatedAt": "2026-04-24T10:00:00Z"
  }
]
```

Detailed availability response (`GET /api/v1/rooms/check-availability`):
```json
{
  "available": false,
  "message": "Conflicts found",
  "roomConflicts": [
    {
      "sessionId": 902,
      "startTime": "2026-04-30T09:00:00Z",
      "endTime": "2026-04-30T10:00:00Z",
      "reason": "Room occupied"
    }
  ],
  "therapistConflicts": [],
  "clientConflicts": []
}
```

Authorization notes (from backend `@PreAuthorize`):
- Read endpoints (`GET /api/v1/rooms`, `GET /api/v1/rooms/{id}`, availability endpoints): require `CONSENT_ADMIN_VIEW`.
- Create/Delete room: require `ROOM_MANAGE`.
- Update room: require `USER_MANAGE`.

### Service Management API contract (verified)
Available backend APIs:
- `GET /api/v1/billing/services` (filters: `activeOnly`, `therapistVisible`, `clientPortalVisible`)
- `GET /api/v1/billing/services/{id}`
- `POST /api/v1/billing/services`
- `PUT /api/v1/billing/services/{id}`
- `POST /api/v1/billing/services/visibility/therapists/show-all`
- `POST /api/v1/billing/services/visibility/therapists/hide-all`

Notes:
- There is no `DELETE /api/v1/billing/services/{id}` endpoint in current backend.
- Create and update both use `CreateServiceRequest` contract.

Authorization notes (from backend `@PreAuthorize`):
- Service list/detail: role in (`BILLING_SPECIALIST`, `ADMIN`, `SUPERVISOR`, `THERAPIST`) + `BILLING_VIEW`.
- Create service: `ADMIN` + `BILLING_CREATE`.
- Update service and show-all/hide-all therapist visibility: `ADMIN` + `BILLING_EDIT`.

Request DTO (`CreateServiceRequest`) fields:
- `serviceCode` (required)
- `serviceName` (required)
- `description` (optional)
- `durationInMinutes` (optional)
- `baseRate` (required)
- `isActive` (optional)
- `therapistVisible` (optional)
- `clientPortalVisible` (optional)

Response DTO (`ServiceResponse`) fields:
- `id`, `serviceCode`, `serviceName`, `description`
- `durationInMinutes`, `baseRate`
- `isActive`, `therapistVisible`, `clientPortalVisible`
- `createdAt`, `updatedAt`

Example create/update request:
```json
{
  "serviceCode": "PSY-60",
  "serviceName": "Psychotherapy Session - 60 minutes",
  "description": "Standard session",
  "durationInMinutes": 60,
  "baseRate": 150.00,
  "isActive": true,
  "therapistVisible": true,
  "clientPortalVisible": false
}
```

Example list response:
```json
[
  {
    "id": 1,
    "serviceCode": "PSY-60",
    "serviceName": "Psychotherapy Session - 60 minutes",
    "description": "Standard session",
    "durationInMinutes": 60,
    "baseRate": 150.00,
    "isActive": true,
    "therapistVisible": true,
    "clientPortalVisible": false,
    "createdAt": "2026-04-20T10:10:00Z",
    "updatedAt": "2026-04-24T09:00:00Z"
  }
]
```

## Session + Zoom End-To-End Flow (Therapist/Admin/Client)
### Therapist profile and availability setup
1. Therapist/Admin updates profile schedule and modality:
   - `PUT /api/v1/users/{userId}/profile` or `PUT /api/v1/users/me/profile`
   - key fields: `workingHours`, `timezone`, `virtualRoomId`, `availablePhysicalRoomIds`
2. Optional blocked times:
   - `POST/PATCH/DELETE /api/v1/therapist-availability/therapist-blocked-times*`
3. Validate slots:
   - `GET /api/v1/therapist-availability/availability/slots?...&sessionType=online|in-person`
4. Availability access policy:
   - therapist availability endpoints are protected by `CONSENT_ADMIN_VIEW`.
   - profile endpoints are protected by `THERAPIST_OR_ADMIN` with `USER_VIEW/USER_EDIT`.

### Therapist Zoom integration setup
| Method | Path | Usage |
|---|---|---|
| PUT | `/api/v1/users/me/zoom-credentials` | Create/update therapist zoom credentials |
| DELETE | `/api/v1/users/me/zoom-credentials` | Remove zoom credentials |
| GET | `/api/v1/users/me/zoom-credentials/status` | Check if zoom is configured |
| POST | `/api/v1/users/me/zoom-credentials/test` | Validate credentials against Zoom OAuth |
| GET | `/api/v1/users/{userId}/zoom-credentials/status` | Admin/therapist profile-level zoom status |

### Organization Stripe Connect (admin-managed, dynamic per org)
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/admin/stripe-connect/status` | Get current org connect status/capabilities |
| POST | `/api/v1/admin/stripe-connect/oauth/start` | Start org Stripe OAuth |
| GET | `/api/v1/admin/stripe-connect/oauth/callback` | OAuth callback landing endpoint |
| POST | `/api/v1/admin/stripe-connect/refresh` | Refresh connect status from Stripe |
| POST | `/api/v1/admin/stripe-connect/disconnect` | Disconnect org stripe account |
| POST | `/api/v1/stripe/webhook/platform` | Platform webhook receiver (public, signature-verified) |
| POST | `/api/v1/stripe/webhook/connect` | Connect webhook receiver (public, signature-verified) |
| POST | `/api/v1/stripe/invoices/{invoiceId}/pay` | Start client checkout for invoice |

Stripe notes:
- Org admin Stripe connect APIs require `BILLING_MANAGE`.
- Webhook endpoints are `permitAll` in security, protected by Stripe signature verification in backend.
- Charges are executed against the connected account for the active organization.

### Session management flow APIs
| Step | API |
|---|---|
| Create session | `POST /api/v1/sessions` |
| Update session | `PUT /api/v1/sessions/{id}` |
| Update status | `PUT /api/v1/sessions/{id}/status` |
| Get one session | `GET /api/v1/sessions/{id}` |
| List/filter sessions | `GET /api/v1/sessions` |
| Session history (day/week/month/all) | `GET /api/v1/sessions/history/*` |
| Recent/upcoming/overdue | `GET /api/v1/sessions/recent`, `/upcoming`, `/overdue` |
| Conflict checks | `GET /api/v1/sessions/conflicts/check` |
| Room/therapist/client availability check | `GET /api/v1/rooms/check-availability` |

### Zoom usage in sessions
1. During session creation/update:
   - if `zoomEnabled=true` and therapist Zoom is configured, backend attempts to create/update Zoom meeting.
2. Zoom details retrieval for meeting start/join:
   - `GET /api/v1/sessions/{id}/zoom/start`
3. Client portal booking:
   - `POST /api/v1/portal/appointments`
   - for online booking, backend tries to create Zoom meeting and returns zoom details when available.

### 5) Administration tab (Practice Configuration)
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/practice-configuration` | Get practice configuration |
| PUT | `/api/v1/practice-configuration` | Save practice configuration |
| GET | `/api/v1/system-options/categories` + `/{id}` | Alternate implementation used by some UIs (`practice_settings` category) |
| PUT/POST | `/api/v1/system-options/{id}` or `/api/v1/system-options` | Update/create individual `practice_*` options |

### APIs Not Found When Using `/api` (without `/v1`)
If frontend calls `/api/...` directly (and no rewrite is configured), these routes are not found:

| Method | Not Found Path | Use This Instead |
|---|---|---|
| GET | `/api/services` | `/api/v1/billing/services` |
| POST | `/api/services` | `/api/v1/billing/services` |
| PUT | `/api/services/{id}` | `/api/v1/billing/services/{id}` |
| DELETE | `/api/services/{id}` | No controller route currently exposed in `BillingController` |
| PUT | `/api/services/{id}/visibility` | `/api/v1/billing/services/{id}` (send `therapistVisible` / `clientPortalVisible`) |
| GET | `/api/rooms` | `/api/v1/rooms` |
| POST | `/api/rooms` | `/api/v1/rooms` |
| PUT | `/api/rooms/{id}` | `/api/v1/rooms/{roomId}` |
| DELETE | `/api/rooms/{id}` | `/api/v1/rooms/{roomId}` |
| GET | `/api/system-options/categories` | `/api/v1/system-options/categories` |
| GET | `/api/system-options/categories/{id}` | `/api/v1/system-options/categories/{id}` |
| POST | `/api/system-options/categories` | `/api/v1/system-options/categories` |
| PUT | `/api/system-options/categories/{id}` | `/api/v1/system-options/categories/{id}` |
| DELETE | `/api/system-options/categories/{id}` | `/api/v1/system-options/categories/{id}` |
| GET | `/api/system-options` | `/api/v1/system-options` |
| POST | `/api/system-options` | `/api/v1/system-options` |
| PUT | `/api/system-options/{id}` | `/api/v1/system-options/{id}` |
| DELETE | `/api/system-options/{id}` | `/api/v1/system-options/{id}` |

## Compliance Module
This section maps the Admin Dashboard Compliance screens:
- `Compliance > HIPAA Audit`
- `Compliance > Privacy & Consent`

### Compliance APIs (Complete)
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/audit/logs` | HIPAA audit table (paginated) with filters |
| GET | `/api/v1/audit/stats` | HIPAA summary cards + chart stats |
| GET | `/api/v1/audit/export` | Export HIPAA audit report (CSV) |
| GET | `/api/v1/audit/clients/{clientId}` | Client-specific audit history |
| GET | `/api/v1/admin/consents/management` | Privacy & Consent management grid (client summary view) |
| GET | `/api/v1/admin/consents` | Full consent records list (optional filters) |
| GET | `/api/v1/admin/consents/clients/{clientId}` | Full consent history for one client |

### 1) HIPAA Audit APIs

#### `GET /api/v1/audit/logs`
Purpose:
- Load main audit table.
- Support search/filter drawer.

Query params:
- `startDate` (`YYYY-MM-DD`)
- `endDate` (`YYYY-MM-DD`)
- `riskLevel` (`all`, `low`, `medium`, `high`, `critical`)
- `hipaaOnly` (`true|false`)
- `action` (`all` or specific action value)
- `username` (partial search)
- `clientId` (numeric)
- `resourceType` (string)
- `page` (default `0`)
- `size` (default `50`)

Response shape:
- Spring page object with `content[]` of audit rows.
- Row fields include:
  - `id`, `userId`, `username`
  - `action`, `result`
  - `resourceType`, `resourceId`
  - `clientId`, `clientName`
  - `ipAddress`, `userAgent`, `sessionId`
  - `riskLevel`, `hipaaRelevant`
  - `details`, `dataFields`, `accessReason`
  - `timestamp`

Examples:
- `/api/v1/audit/logs?page=0&size=20`
- `/api/v1/audit/logs?username=eman`
- `/api/v1/audit/logs?action=client_viewed&riskLevel=high`
- `/api/v1/audit/logs?startDate=2026-01-01&endDate=2026-01-31`

#### `GET /api/v1/audit/stats`
Purpose:
- Load top stat cards and risk distribution metrics.

Query params:
- `startDate` (`YYYY-MM-DD`)
- `endDate` (`YYYY-MM-DD`)
- `riskLevel` (`all`, `low`, `medium`, `high`, `critical`)
- `hipaaOnly` (`true|false`)

Response fields:
- `totalActivities`
- `phiAccess`
- `highRiskEvents`
- `failedAttempts`
- `lowRiskEvents`
- `mediumRiskEvents`
- `criticalRiskEvents`
- `userActivity[]` with:
  - `username`
  - `activityCount`
  - `lastActivity`

#### `GET /api/v1/audit/export`
Purpose:
- Export current audit slice to CSV (`text/csv`).

Query params:
- `startDate` (`YYYY-MM-DD`)
- `endDate` (`YYYY-MM-DD`)
- `riskLevel` (`all`, `low`, `medium`, `high`, `critical`)
- `hipaaOnly` (`true|false`)
- `limit` (default `1000`)

Notes:
- Returns CSV payload with content-disposition attachment filename:
  - `hipaa_audit_report_<date>.csv`
- Requires export permission/feature entitlement.

#### `GET /api/v1/audit/clients/{clientId}`
Purpose:
- Client-level audit timeline in client detail/related compliance view.

Query params:
- `page` (default `0`)
- `size` (default `50`)

### 2) Privacy & Consent APIs

#### `GET /api/v1/admin/consents/management`
Purpose:
- Main `Patient Consent Management` grid in admin compliance screen.

Query params:
- `consentType`:
  - `ALL`
  - `AI_PROCESSING`
  - `DATA_SHARING`
  - `RESEARCH`
  - `MARKETING`
- `status`:
  - `ALL`
  - `GRANTED`
  - `DENIED` (also supports `WITHDRAWN` / `DENIED_WITHDRAWN` aliases)
- `search` (client ID, full name, or email partial search)

Response row fields:
- `clientId`, `fullName`, `email`
- `portalAccess`
- `aiProcessing`, `dataSharing`, `research`, `marketing`
- `allConsents[]`:
  - `id`, `consentType`, `status`, `version`, `grantedAt`, `withdrawnAt`

Grid status enum values:
- `GRANTED`
- `DENIED`
- `NOT_SET`

Examples:
- `/api/v1/admin/consents/management`
- `/api/v1/admin/consents/management?search=CL-2025-0001`
- `/api/v1/admin/consents/management?consentType=RESEARCH`
- `/api/v1/admin/consents/management?status=DENIED_WITHDRAWN`

#### `GET /api/v1/admin/consents`
Purpose:
- Full/raw consent listing API (non-summary view).

Query params:
- `consentType` (display-name based type value)
- `granted` (`true|false`)

#### `GET /api/v1/admin/consents/clients/{clientId}`
Purpose:
- Full consent history for one client.

Response item fields:
- `id`, `clientId`
- `consentType`, `consentVersion`
- `granted`, `grantedAt`, `withdrawnAt`
- `ipAddress`, `userAgent`, `notes`
- `createdAt`, `updatedAt`

### Compliance UI mapping (screenshot parity)
| UI element | API |
|---|---|
| HIPAA table load | `GET /api/v1/audit/logs` |
| Username search | `GET /api/v1/audit/logs?username=` |
| Action Type filter | `GET /api/v1/audit/logs?action=` |
| Risk Level filter | `GET /api/v1/audit/logs?riskLevel=` |
| Date range filter | `GET /api/v1/audit/logs?startDate=&endDate=` |
| HIPAA cards and charts | `GET /api/v1/audit/stats` |
| Export report button | `GET /api/v1/audit/export` |
| Privacy grid load | `GET /api/v1/admin/consents/management` |
| Privacy search | `GET /api/v1/admin/consents/management?search=` |
| Consent type dropdown | `GET /api/v1/admin/consents/management?consentType=` |
| Consent status dropdown | `GET /api/v1/admin/consents/management?status=` |

## Notifications (Top bar)
| Method | Path | Usage |
|---|---|---|
| GET | `/api/v1/notifications` | Notifications list |
| GET | `/api/v1/notifications/unread/count` | Unread count |
| PATCH | `/api/v1/notifications/{id}/read` | Mark one as read |
| PATCH | `/api/v1/notifications/read-all` | Mark all as read |
| GET | `/api/v1/notifications/stats` | Notification stats |
| GET | `/api/v1/notifications/triggers` | List event triggers (recipient rules, scheduling, active flag) |
| POST | `/api/v1/notifications/triggers` | Create trigger (org/tenant specific) |
| PUT | `/api/v1/notifications/triggers/{id}` | Update trigger |
| DELETE | `/api/v1/notifications/triggers/{id}` | Delete trigger |
| GET | `/api/v1/notifications/templates` | List templates |
| POST | `/api/v1/notifications/templates` | Create template |
| PUT | `/api/v1/notifications/templates/{id}` | Update template |
| DELETE | `/api/v1/notifications/templates/{id}` | Delete template |

### Session Notification/Reminder Events (dynamic, org-wise)
- `session_scheduled`
- `session_rescheduled`
- `session_cancelled`
- `session_reminder` (24h reminder flow)

Template variable examples for session events:
- `{{clientName}}`, `{{therapistName}}`
- `{{sessionDate}}`, `{{duration}}`
- `{{sessionMode}}`, `{{sessionType}}`
- `{{serviceName}}`, `{{roomName}}`
- `{{zoomEnabled}}`, `{{zoomJoinUrl}}`, `{{zoomPassword}}`

Recommended recipient rule JSON for full flow (admin + therapist + supervisor + client):
```json
{
  "roles": ["ADMIN"],
  "assignedTherapist": true,
  "supervisorOfTherapist": true,
  "sessionClient": true
}
```

## Sample request and response payloads

### 1) Login
`POST /api/v1/auth/login`

Request:
```json
{
  "username": "admin@therapyflow.com",
  "password": "SecurePassword123!",
  "orgIdentifier": "slug",
  "orgValue": "acme-clinic"
}
```

Response:
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "admin@therapyflow.com",
  "email": "admin@therapyflow.com",
  "roles": ["ADMIN"],
  "permissions": ["CLIENT_VIEW", "SESSION_VIEW", "BILLING_VIEW"],
  "expiresIn": 3600,
  "passwordChangeRequired": false
}
```

### 2) Dashboard summary (single endpoint)
`GET /api/v1/admin/dashboard/summary`

Response:
```json
{
  "client": {
    "active": 2,
    "total": 4
  },
  "session": {
    "scheduledToday": 2
  },
  "task": {
    "pending": 2,
    "total": 10
  },
  "billing": {
    "estimatedThisMonth": 3.00,
    "totalCollected": 3.00,
    "outstandingBalance": 12.00
  }
}
```

### 3) Create client
`POST /api/v1/clients`

Request:
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "status": "active",
  "stage": "intake",
  "clientType": "individual",
  "assignedTherapistId": 3,
  "serviceType": "Psychotherapy",
  "serviceFrequency": "Weekly"
}
```

Response:
```json
{
  "id": 101,
  "clientId": "CL-000101",
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "status": "active",
  "stage": "intake",
  "clientType": "individual",
  "assignedTherapistId": 3,
  "assignedTherapistName": "Dr. Sarah Khan",
  "hasPortalAccess": false,
  "emailNotifications": true,
  "createdAt": "2026-04-20T10:05:00Z",
  "updatedAt": "2026-04-20T10:05:00Z"
}
```

### 4) Create session
`POST /api/v1/sessions`

Request:
```json
{
  "clientId": 101,
  "therapistId": 3,
  "sessionDate": "2026-04-21T10:00:00Z",
  "sessionType": "psychotherapy",
  "status": "scheduled",
  "duration": 60,
  "zoomEnabled": false
}
```

Response:
```json
{
  "id": 901,
  "clientId": 101,
  "clientName": "John Doe",
  "therapistId": 3,
  "therapistName": "Dr. Sarah Khan",
  "sessionDate": "2026-04-21T10:00:00Z",
  "duration": 60,
  "sessionType": "psychotherapy",
  "status": "scheduled",
  "zoomEnabled": false,
  "createdAt": "2026-04-20T10:10:00Z",
  "updatedAt": "2026-04-20T10:10:00Z"
}
```

### 5) Create task
`POST /api/v1/tasks`

Request:
```json
{
  "title": "Follow up with client",
  "description": "Check in on progress and schedule next session",
  "priority": "medium",
  "status": "pending",
  "clientId": 101,
  "assignedToId": 3,
  "dueDate": "2026-04-23T12:00:00Z"
}
```

Response:
```json
{
  "id": 501,
  "title": "Follow up with client",
  "description": "Check in on progress and schedule next session",
  "status": "pending",
  "priority": "medium",
  "dueDate": "2026-04-23T12:00:00Z",
  "clientId": 101,
  "clientName": "John Doe",
  "assignedToId": 3,
  "assignedToName": "Dr. Sarah Khan",
  "createdAt": "2026-04-20T10:20:00Z",
  "updatedAt": "2026-04-20T10:20:00Z"
}
```

### 6) Task stats
`GET /api/v1/tasks/stats`

Response:
```json
{
  "totalTasks": 10,
  "pendingTasks": 2,
  "inProgressTasks": 3,
  "completedTasks": 5,
  "overdueTasks": 1,
  "needsAttentionTasks": 2,
  "highPriorityTasks": 1,
  "urgentTasks": 0
}
```

### 7) Billing statistics
`GET /api/v1/billing/statistics`

Response:
```json
{
  "outstandingBalance": 5000.00,
  "totalCollected": 25000.00,
  "activeClients": 45,
  "totalBillingRecords": 120,
  "pendingRecords": 15,
  "paidRecords": 85,
  "deniedRecords": 5,
  "followUpRecords": 10
}
```

### 8) Record payment
`POST /api/v1/billing/billing/{id}/record-payment`

Request:
```json
{
  "paymentAmount": 150.00,
  "paymentMethod": "card",
  "referenceNumber": "TXN-12345",
  "notes": "Paid at front desk"
}
```

Response:
```json
{
  "id": 701,
  "sessionBillingId": 550,
  "amount": 150.00,
  "paymentMethod": "card",
  "paymentSource": "manual",
  "status": "succeeded",
  "paymentDate": "2026-04-20T10:30:00Z",
  "reference": "TXN-12345",
  "notes": "Paid at front desk",
  "createdAt": "2026-04-20T10:30:00Z",
  "updatedAt": "2026-04-20T10:30:00Z"
}
```

### 9) Create user (staff/admin/therapist)
`POST /api/v1/users`

Request:
```json
{
  "username": "jane.doe",
  "fullName": "Jane Doe",
  "password": "SecurePassword123!",
  "email": "jane.doe@clinic.com",
  "roles": ["THERAPIST"],
  "active": true
}
```

Response:
```json
{
  "id": 22,
  "username": "jane.doe",
  "fullName": "Jane Doe",
  "email": "jane.doe@clinic.com",
  "active": true,
  "roles": ["THERAPIST"],
  "createdAt": "2026-04-20T10:40:00Z",
  "updatedAt": "2026-04-20T10:40:00Z"
}
```

### 10) Assign permissions to role
`PUT /api/v1/roles/{id}/permissions`

Request:
```json
{
  "permissionIds": [1, 2, 3, 4]
}
```

Response:
```json
{
  "id": 2,
  "name": "THERAPIST",
  "displayName": "Therapist",
  "description": "Therapist role",
  "isSystem": true,
  "isActive": true,
  "permissions": [
    {
      "id": 1,
      "name": "CLIENT_VIEW"
    },
    {
      "id": 2,
      "name": "SESSION_VIEW"
    }
  ],
  "userCount": 8
}
```

### 11) Notification unread count
`GET /api/v1/notifications/unread/count`

Response:
```json
5
```

### 12) Client session KPI summary
`GET /api/v1/clients/{clientId}/sessions/summary`

Response:
```json
{
  "clientId": 101,
  "totalSessions": 100,
  "completed": 20,
  "scheduled": 20,
  "missedCancelled": 20,
  "conflicts": 0
}
```

### 13) Client email history
`GET /api/v1/clients/{id}/email-history`

Response:
```json
{
  "history": [
    {
      "id": 1101,
      "clientId": 101,
      "eventType": "Portal Access Activated",
      "eventSource": "API",
      "fromValue": "disabled",
      "toValue": "activation_sent",
      "description": "Portal access activated and invitation email sent to client",
      "changeSummary": null,
      "createdByUserId": 3,
      "createdByName": "admin@therapyflow.com",
      "createdAt": "2026-04-20T12:00:00Z"
    }
  ],
  "message": null,
  "count": 1
}
```

## Suggested frontend integration order
1. Implement auth flow (`login`, `refresh`, `me`).
2. Implement dashboard with `GET /api/v1/admin/dashboard/summary`.
3. Build module pages one by one using sectioned APIs above.
4. Add notifications and audit/compliance views.

## Notes
- Prefer the single dashboard endpoint for card data to reduce frontend API fan-out.
- Keep token refresh interceptor in frontend API client.
- Use query params for paging/filtering on list endpoints.

## Screen coverage matrix (current status)

### Present in backend
- Dashboard top cards as single API:
  - `GET /api/v1/admin/dashboard/summary`
- Dashboard fallback/composable APIs:
  - `GET /api/v1/clients/stats`
  - `GET /api/v1/tasks/pending/count`
  - `GET /api/v1/billing/statistics`
  - `GET /api/v1/sessions/upcoming`
  - `GET /api/v1/sessions/overdue`
- Clients list + search/filter + detail:
  - `GET /api/v1/clients`
  - `GET /api/v1/clients/{id}`
- Add/Edit client modal (Personal/Address/Clinical):
  - `POST /api/v1/clients`
  - `PUT /api/v1/clients/{id}`
  - `PATCH /api/v1/clients/{id}`
- Referral/Employment tab data:
  - `GET|PUT|DELETE /api/v1/clients/{clientId}/referral`
  - `GET|PUT|DELETE /api/v1/clients/{clientId}/employment`
- Sessions tab:
  - `GET /api/v1/clients/{clientId}/sessions`
  - `GET /api/v1/clients/{clientId}/sessions/summary`
  - `GET /api/v1/sessions` and status/update/history endpoints
  - `GET|POST|PUT|DELETE /api/v1/session-notes/*`
- Assessments tab:
  - `GET /api/v1/assessments/templates`
  - `GET|POST /api/v1/assessments/clients/{clientId}/assessments`
  - assignments/report endpoints under `/api/v1/assessments/*`
- Forms & Docs tab:
  - `GET /api/v1/forms/templates`
  - `POST /api/v1/forms/assignments`
  - `GET /api/v1/forms/assignments/client/{clientId}`
  - `GET|POST /api/v1/clients/{clientId}/documents`
- Billing tab:
  - `GET /api/v1/billing/history`
  - `GET /api/v1/billing/billing/{id}/invoice-preview`
  - `GET /api/v1/billing/billing/{id}/invoice-download`
  - `POST /api/v1/billing/billing/{id}/send-invoice-email`
- Tasks tab:
  - `GET|POST /api/v1/tasks`
  - `GET /api/v1/tasks/clients/{clientId}/tasks`
- Checklists tab:
  - `GET /api/v1/checklists/checklist-templates`
  - `GET|POST /api/v1/checklists/clients/{clientId}/checklists`
- History timeline:
  - `GET /api/v1/clients/{id}/history`
  - `GET /api/v1/clients/{id}/email-history`
  - `GET /api/v1/audit/clients/{clientId}`

### Not present as dedicated API (gaps)
- No separate referral field explicitly named `startDate` in current referral DTO contract.
  - Available referral date field is `referralDate`.

### Recommendation for frontend
- Use current APIs for now (all major flows are supported).
- Keep referral `startDate` mapped to backend `referralDate` until a field rename/alias is introduced.
