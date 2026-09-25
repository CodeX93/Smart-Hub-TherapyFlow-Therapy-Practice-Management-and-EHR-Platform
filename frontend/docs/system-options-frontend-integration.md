# System Options — Frontend Integration Guide

> **Purpose:** Replace hardcoded TypeScript/Java enums in dropdowns, filters, and forms with **tenant-configurable** options loaded from the backend.
>
> **Backend base path:** `/api/v1`
>
> **Audience:** Frontend agents and developers building client, session, task, and admin settings UI.

---

## 1. What changed (read this first)

| Before | After |
|--------|-------|
| Hardcoded enum arrays in the frontend (`ClientStatus`, `Gender`, `SessionType`, etc.) | Options loaded per tenant from `/api/v1/system-options` |
| Fixed labels in code | `optionLabel` from API (admin can rename) |
| Enum constant as API value | **`optionKey` string** stored on entities and sent in requests |
| Same options for every org | Each tenant has its own catalog (24 categories, 174 default options) |

**Golden rules for the frontend:**

1. **Display** `optionLabel` in dropdowns, chips, and tables.
2. **Store & submit** `optionKey` in form state and API payloads.
3. **Never** hardcode option lists for configurable fields — always fetch from the API (or from a batch endpoint that wraps it).
4. Options are **tenant-scoped** — cache per logged-in organisation, not globally.
5. Backend accepts **either** `optionKey` or `optionLabel` on input and normalizes to `optionKey`; prefer sending `optionKey` to avoid ambiguity.

---

## 2. API reference

### 2.1 Read endpoints (any authenticated user)

| Method | Path | Use case |
|--------|------|----------|
| `GET` | `/api/v1/system-options/categories` | Load **all** categories with nested options (best for app init / settings) |
| `GET` | `/api/v1/system-options/by-category/{categoryKey}` | Load options for **one** dropdown |
| `GET` | `/api/v1/system-options?categoryId={id}` | Load options when you already have the category DB id |
| `GET` | `/api/v1/system-options/categories/{id}` | Single category with options |
| `GET` | `/api/v1/system-options/{id}` | Single option by id |

**Auth:** `Authorization: Bearer <staff JWT>` — any authenticated staff user.

**Filtering:** `by-category` and list endpoints return only **`isActive: true`** options, sorted by `sortOrder` then `optionLabel`.

### 2.2 Admin endpoints (`USER_MANAGE` permission)

| Method | Path | Use case |
|--------|------|----------|
| `POST` | `/api/v1/system-options/categories` | Create custom category |
| `PUT` | `/api/v1/system-options/categories/{id}` | Update category metadata |
| `DELETE` | `/api/v1/system-options/categories/{id}` | Delete empty custom category |
| `PUT` | `/api/v1/system-options/categories/{id}/options/order` | Reorder options |
| `POST` | `/api/v1/system-options` | Add option to a category |
| `PUT` | `/api/v1/system-options/{id}` | Edit option label, default, active, price |
| `DELETE` | `/api/v1/system-options/{id}` | Remove option (blocked if in use) |
| `GET` | `/api/v1/system-options/categories/{id}/usage` | Usage stats before delete |
| `GET` | `/api/v1/system-options/{id}/usage` | Usage stats for one option |

### 2.3 Client filter batch (includes system options)

For the **client list / filter bar**, you can use the existing batch endpoint instead of calling system-options separately:

```
GET /api/v1/client-filters/batch
```

Requires `CONSENT_ADMIN_VIEW` authority. Returns `systemOptions` map keyed by category:

`client_type`, `referral_sources`, `marital_status`, `employment_status`, `education_level`, `gender`, `preferred_language`, `client_status`, `client_stage`

Use `/api/v1/system-options/...` for all other screens (sessions, tasks, insurance, etc.).

---

## 3. Response shapes

### 3.1 `OptionCategoryResponse`

```json
{
  "id": 12,
  "categoryKey": "client_status",
  "categoryName": "Client Status",
  "description": "Current status of client in the system",
  "isSystem": true,
  "isActive": true,
  "ownershipType": "CONFIGURABLE",
  "optionSource": "SYSTEM_OPTIONS",
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-01-15T10:00:00Z",
  "options": [ /* SystemOptionResponse[] */ ]
}
```

| Field | Frontend use |
|-------|----------------|
| `categoryKey` | Stable identifier — use in code constants and `by-category` URL |
| `categoryName` | Section heading in admin settings |
| `ownershipType` | Always `CONFIGURABLE` — admins can add/edit/reorder/delete |
| `optionSource` | `SYSTEM_OPTIONS` = from DB; `ENUM_VIRTUAL` = temporary fallback if catalog empty |
| `options` | Dropdown items (may be omitted on lightweight list calls) |

### 3.2 `SystemOptionResponse`

```json
{
  "id": 101,
  "categoryId": 12,
  "categoryKey": "client_status",
  "categoryName": "Client Status",
  "optionKey": "active",
  "optionLabel": "Active",
  "sortOrder": 1,
  "isDefault": true,
  "isSystem": true,
  "isActive": true,
  "price": 0.0,
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-01-15T10:00:00Z"
}
```

| Field | Frontend use |
|-------|----------------|
| `optionKey` | **Form value** — bind `<Select value={...}>` to this |
| `optionLabel` | **Display text** in dropdowns and read-only views |
| `sortOrder` | Display order (ascending) |
| `isDefault` | Pre-select in create forms when no value set |
| `isActive` | Hide from dropdowns when `false` |
| `price` | Used for billable session types / service pricing where applicable |

---

## 4. Field → category mapping (replace enums with these keys)

Use this table to know **which `categoryKey` to fetch** for each form field.

### 4.1 Client forms (`POST/PUT /api/v1/clients`)

| Form field (JSON) | `categoryKey` | Example `optionKey` values |
|-------------------|---------------|----------------------------|
| `status` | `client_status` | `pending`, `active`, `inactive` |
| `stage` | `client_stage` | `intake`, `active`, `maintenance`, `closed` |
| `clientType` | `client_type` | `Refugee`, `Immigrant`, `Citizen`, `Other` |
| `gender` | `gender` | `male`, `female`, `other`, `prefer_not_to_say` |
| `maritalStatus` | `marital_status` | `single`, `married`, `divorced`, … |
| `preferredLanguage` | `preferred_language` | `english`, `spanish`, `french`, … |
| `serviceType` | `service_type` | `psychotherapy`, `couples_therapy`, … |
| `serviceFrequency` | `service_frequency` | `weekly`, `biweekly`, `monthly`, … |
| `employmentStatus` | `employment_status` | `employed_full_time`, `self_employed`, … |
| `educationLevel` | `education_level` | `high_school`, `bachelor`, `master`, … |
| `clientSource` | `client_source` | `referral`, `website`, `walk_in`, … |
| `referralSource` (referral sub-resource) | `referral_sources` | `self_referral`, `physician`, … |
| `insuranceProvider` | `insurance_providers` | `none`, `blue_cross`, … |
| insurance type fields | `insurance_types` | `private_insurance`, `medicare`, … |

### 4.2 Session forms (`POST/PUT /api/v1/sessions`)

| Form field (JSON) | `categoryKey` | DB column | Notes |
|-------------------|---------------|-----------|-------|
| `sessionMode` | `session_mode` | `sessions.session_mode` | **String option key** — supports `in_person`, `Online`, `phone`, `hybrid`, etc. |
| `sessionType` | `session_type` | `sessions.session_type` | **String option key** — clinical type (`assessment`, `individual`, …) |
| `status` | `session_status` | `sessions.status` | String option key |

**All session request DTOs now accept strings** (not Java enums). Send the `optionKey` from the dropdown directly.

### 4.3 Task forms (`POST/PUT /api/v1/tasks`)

| Form field | `categoryKey` | Notes |
|------------|---------------|-------|
| `status` | `task_status` | String option key |
| `priority` | `task_priority` | **String option key** (`low`, `medium`, `high`, `urgent`) |
| task title picker | `task_titles` | Optional predefined titles |
| task type picker | `task_types` | `client_contact`, `documentation`, … |

### 4.4 Other UI areas

| UI area | `categoryKey` |
|---------|---------------|
| Treatment modality picker | `treatment_modalities` |
| Practice settings | `practice_settings` |
| Client source (intake) | `client_source` |

---

## 5. Full category catalog (default tenant seed)

Each tenant receives these **24 categories** on provisioning. Option counts are defaults from the legacy ClientHub catalog.

| `categoryKey` | Display name | Options | Default `optionKey` |
|---------------|--------------|---------|---------------------|
| `client_source` | Client Sources | 7 | `referral` |
| `client_stage` | Client Stage | 4 | `intake` |
| `client_status` | Client Status | 3 | `pending` |
| `client_type` | Client Types | 4 | `Refugee` |
| `education_level` | Education Levels | 9 | `elementary` |
| `employment_status` | Employment Status | 9 | `employed_full_time` |
| `gender` | Gender Options | 6 | `male` |
| `insurance_providers` | Insurance Providers | 34 | `none` |
| `insurance_types` | Insurance Types | 6 | `private_insurance` |
| `marital_status` | Marital Status | 8 | `single` |
| `practice_settings` | Practice Settings | 0 | — |
| `preferred_language` | Preferred Languages | 7 | `english` |
| `referral_sources` | Referral Sources | 9 | `self_referral` |
| `service_frequency` | Service Frequency | 5 | `weekly` |
| `service_type` | Service Types | 7 | `psychotherapy` |
| `session_mode` | Session Modes | 4 | `in_person` |
| `session_status` | Session Status | 6 | `scheduled` |
| `session_type` | Session Types | 3 | `assessment` |
| `task_priorities` | Task Priorities | 4 | `medium` |
| `task_priority` | Task Priority | 4 | `low` |
| `task_status` | Task Status | 5 | `pending` |
| `task_titles` | Task Titles | 15 | `initial_assessment` |
| `task_types` | Task Types | 7 | `client_contact` |
| `treatment_modalities` | Treatment Modalities | 8 | `cbt` |

Admins can add options to any category. Keys and labels are **not guaranteed identical across tenants** after customization.

---

## 6. TypeScript types (recommended)

```typescript
export interface SystemOption {
  id: number;
  categoryId: number;
  categoryKey: string;
  categoryName: string;
  optionKey: string;
  optionLabel: string;
  sortOrder: number;
  isDefault: boolean;
  isSystem: boolean;
  isActive: boolean;
  price: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface OptionCategory {
  id: number;
  categoryKey: string;
  categoryName: string;
  description?: string;
  isSystem: boolean;
  isActive: boolean;
  ownershipType: 'CONFIGURABLE' | 'WORKFLOW_STRICT';
  optionSource: 'SYSTEM_OPTIONS' | 'ENUM_VIRTUAL';
  options?: SystemOption[];
}

/** Stable category keys — use instead of hardcoded enum arrays */
export const SystemOptionCategoryKey = {
  CLIENT_STATUS: 'client_status',
  CLIENT_STAGE: 'client_stage',
  CLIENT_TYPE: 'client_type',
  CLIENT_SOURCE: 'client_source',
  GENDER: 'gender',
  MARITAL_STATUS: 'marital_status',
  PREFERRED_LANGUAGE: 'preferred_language',
  SERVICE_TYPE: 'service_type',
  SERVICE_FREQUENCY: 'service_frequency',
  EMPLOYMENT_STATUS: 'employment_status',
  EDUCATION_LEVEL: 'education_level',
  REFERRAL_SOURCES: 'referral_sources',
  INSURANCE_PROVIDERS: 'insurance_providers',
  INSURANCE_TYPES: 'insurance_types',
  SESSION_STATUS: 'session_status',
  SESSION_MODE: 'session_mode',
  SESSION_TYPE: 'session_type',
  TASK_STATUS: 'task_status',
  TASK_PRIORITY: 'task_priority',
  TASK_TITLES: 'task_titles',
  TASK_TYPES: 'task_types',
  TREATMENT_MODALITIES: 'treatment_modalities',
  PRACTICE_SETTINGS: 'practice_settings',
} as const;

export type SystemOptionCategoryKey =
  (typeof SystemOptionCategoryKey)[keyof typeof SystemOptionCategoryKey];
```

---

## 7. Data fetching patterns

### 7.1 App-level cache (recommended)

Load once after login, keyed by tenant/org id:

```typescript
// GET /api/v1/system-options/categories
const categories = await api.get<OptionCategory[]>('/system-options/categories');

const optionsByCategory = new Map<string, SystemOption[]>();
for (const cat of categories) {
  optionsByCategory.set(cat.categoryKey, cat.options ?? []);
}

export function getOptions(categoryKey: string): SystemOption[] {
  return optionsByCategory.get(categoryKey) ?? [];
}
```

Invalidate cache when admin saves changes in system settings (or on `focus` / interval for settings page).

### 7.2 Single dropdown (lazy)

```typescript
// GET /api/v1/system-options/by-category/client_status
const options = await api.get<SystemOption[]>(
  `/system-options/by-category/${categoryKey}`
);
```

### 7.3 React Query example

```typescript
export function useSystemOptions(categoryKey: string) {
  return useQuery({
    queryKey: ['system-options', tenantId, categoryKey],
    queryFn: () => api.get<SystemOption[]>(`/system-options/by-category/${categoryKey}`),
    staleTime: 5 * 60 * 1000,
  });
}
```

---

## 8. Dropdown component pattern

### 8.1 Generic select

```tsx
interface SystemOptionSelectProps {
  categoryKey: string;
  value: string | null | undefined;
  onChange: (optionKey: string) => void;
  placeholder?: string;
  allowClear?: boolean;
}

function SystemOptionSelect({ categoryKey, value, onChange, placeholder, allowClear }: SystemOptionSelectProps) {
  const { data: options = [], isLoading } = useSystemOptions(categoryKey);

  return (
    <Select
      loading={isLoading}
      value={value ?? undefined}
      placeholder={placeholder ?? 'Select...'}
      allowClear={allowClear}
      onChange={onChange}
      options={options.map((opt) => ({
        value: opt.optionKey,   // stored value
        label: opt.optionLabel, // displayed text
      }))}
    />
  );
}
```

### 8.2 Default value on create forms

```typescript
function getDefaultOptionKey(options: SystemOption[]): string | undefined {
  return options.find((o) => o.isDefault)?.optionKey ?? options[0]?.optionKey;
}

// Create client form init:
const statusOptions = getOptions('client_status');
const initialStatus = getDefaultOptionKey(statusOptions) ?? 'pending';
```

### 8.3 Display label for read-only views (tables, detail pages)

API responses return raw `optionKey` on entities (e.g. `client.status = "active"`). Resolve label client-side:

```typescript
function resolveOptionLabel(
  options: SystemOption[],
  optionKey: string | null | undefined
): string {
  if (!optionKey) return '—';
  const match = options.find(
    (o) => o.optionKey.toLowerCase() === optionKey.toLowerCase()
  );
  return match?.optionLabel ?? optionKey.replace(/_/g, ' ');
}

// Usage in client table:
resolveOptionLabel(statusOptions, client.status); // "Active"
```

Alternatively, build a `Map<optionKey, optionLabel>` when options load.

---

## 9. Form submit checklist

When replacing an enum dropdown, verify each field:

| Step | Action |
|------|--------|
| 1 | Remove hardcoded `enum` / `as const` array for that field |
| 2 | Fetch options via `categoryKey` from mapping table (§4) |
| 3 | Bind select `value` to `optionKey` |
| 4 | On submit, send `optionKey` string in JSON body |
| 5 | On edit, pre-fill with the `optionKey` from GET response |
| 6 | In list/detail views, display `optionLabel` via resolver (§8.3) |
| 7 | For filters, send `optionKey` as query param (backend resolves key or label) |

**Do not** send display labels unless you have no other choice — backend accepts them but `optionKey` is unambiguous.

---

## 10. Screen-by-screen migration checklist

### Client create / edit

| UI control | Remove enum | Fetch category | Form field |
|------------|-------------|----------------|------------|
| Status | `ClientStatus` | `client_status` | `status` |
| Stage | `ClientStage` | `client_stage` | `stage` |
| Client type | `ClientType` | `client_type` | `clientType` |
| Gender | `Gender` | `gender` | `gender` |
| Marital status | `MaritalStatus` | `marital_status` | `maritalStatus` |
| Preferred language | hardcoded list | `preferred_language` | `preferredLanguage` |
| Service type | `ServiceType` | `service_type` | `serviceType` |
| Service frequency | `ServiceFrequency` | `service_frequency` | `serviceFrequency` |
| Employment status | `EmploymentStatus` | `employment_status` | `employmentStatus` |
| Education level | `EducationLevel` | `education_level` | `educationLevel` |
| Client source | hardcoded | `client_source` | `clientSource` |
| Referral source | `ReferralSource` | `referral_sources` | referral fields |
| Insurance provider | hardcoded | `insurance_providers` | `insuranceProvider` |

### Client list filters

Use `GET /api/v1/client-filters/batch` → `response.systemOptions[categoryKey].options` for filter dropdowns.

### Session create / edit / calendar

| UI control | Fetch category | Form field |
|------------|----------------|------------|
| Session mode (online / in-person / phone) | `session_mode` | `sessionMode` |
| Clinical session type | `session_type` | `sessionType` |
| Status | `session_status` | `status` |

### Task create / edit

| UI control | Fetch category | Form field |
|------------|----------------|------------|
| Status | `task_status` | `status` |
| Priority | `task_priority` | `priority` |

### Admin — System options settings page

Build CRUD UI using admin endpoints (§2.2). Requires `USER_MANAGE` permission.

Suggested UX:
- Left panel: category list from `GET /categories`
- Right panel: draggable option list (`sortOrder`), inline edit label, toggle `isActive`, mark `isDefault`
- Show usage warnings from `/usage` before delete

---

## 11. Example API calls

### Load all options (app init)

```http
GET /api/v1/system-options/categories
Authorization: Bearer eyJ...
```

### Load one dropdown

```http
GET /api/v1/system-options/by-category/gender
Authorization: Bearer eyJ...
```

**Response:**

```json
[
  { "id": 1, "categoryId": 7, "categoryKey": "gender", "categoryName": "Gender Options",
    "optionKey": "male", "optionLabel": "Male", "sortOrder": 1, "isDefault": true, "isActive": true, "price": 0 },
  { "id": 2, "categoryId": 7, "categoryKey": "gender", "categoryName": "Gender Options",
    "optionKey": "female", "optionLabel": "Female", "sortOrder": 2, "isDefault": false, "isActive": true, "price": 0 }
]
```

### Create client (submit option keys)

```http
POST /api/v1/clients
Content-Type: application/json

{
  "fullName": "Jane Doe",
  "status": "pending",
  "stage": "intake",
  "gender": "female",
  "maritalStatus": "single",
  "clientType": "Refugee",
  "serviceType": "psychotherapy",
  "serviceFrequency": "weekly",
  "employmentStatus": "employed_full_time",
  "educationLevel": "bachelor"
}
```

### Create session

```http
POST /api/v1/sessions
Content-Type: application/json

{
  "clientId": 42,
  "therapistId": 7,
  "sessionDate": "2026-07-10T14:00:00Z",
  "sessionMode": "in_person",
  "sessionType": "assessment",
  "status": "scheduled",
  "durationMinutes": 60
}
```

---

## 12. Edge cases & pitfalls

### 12.1 Mixed-case option keys

Legacy catalog preserves keys like `Online`, `Refugee`. **Do not** force lowercase in the frontend. Use the exact `optionKey` from the API.

### 12.2 Hyphen vs underscore

Backend treats `in-person` and `in_person` as equivalent for matching. After save, use whatever key the API returns on the session/client object.

### 12.3 Empty catalog / new tenant

If migrations are still running, `optionSource` may briefly be `ENUM_VIRTUAL` with enum-derived fallback options. Dropdowns still work; re-fetch after tenant provisioning completes.

### 12.4 `task_priority` vs `task_priorities`

Both categories exist in the default seed. Use **`task_priority`** for the task `priority` field unless product says otherwise.

### 12.5 `client_source` vs `referral_sources`

- `clientSource` on client → `client_source`
- Referral sub-entity source → `referral_sources`

### 12.6 Status transition validation

Client status changes are validated server-side (invalid transitions return `400`). Frontend can still show all `client_status` options; backend enforces rules.

### 12.7 Caching

Backend caches per tenant (`systemOptions` cache). Frontend should still cache locally to avoid N+1 requests per form field.

### 12.8 All request fields are strings now

Client, session, and task create/update APIs accept **string option keys** from dropdowns. No Java enums on request DTOs for configurable fields. Send `optionKey` values from `/api/v1/system-options`.

---

## 13. Quick reference — remove these hardcoded enums from dropdowns

| Old frontend enum (remove from dropdowns) | Replace with `categoryKey` |
|-------------------------------------------|----------------------------|
| `ClientStatus` | `client_status` |
| `ClientStage` | `client_stage` |
| `ClientType` | `client_type` |
| `Gender` | `gender` |
| `MaritalStatus` | `marital_status` |
| `ServiceType` | `service_type` |
| `ServiceFrequency` | `service_frequency` |
| `EmploymentStatus` | `employment_status` |
| `EducationLevel` | `education_level` |
| `ReferralSource` | `referral_sources` |
| `SessionStatus` | `session_status` |
| `SessionType` (mode dropdown) | `session_mode` |
| Clinical session type | `session_type` |
| `TaskStatus` | `task_status` |
| Task priority constants | `task_priority` |

---

## 14. Related backend docs

- [Client API](./client-api.md) — client CRUD field reference (update enum section to point here)
- [Recurring Sessions](./recurring-sessions.md) — recurrence payloads include `sessionMode` / `sessionType`

---

## 15. Summary for frontend agents

```
1. DELETE hardcoded enum arrays used for <Select> options.
2. FETCH  GET /api/v1/system-options/by-category/{categoryKey}
           OR GET /api/v1/system-options/categories (once per session)
3. DISPLAY option.optionLabel
4. STORE   option.optionKey in form state
5. SUBMIT  optionKey string in API request body
6. RESOLVE optionKey → optionLabel in tables using cached options map
7. ADMIN   CRUD via /api/v1/system-options/* with USER_MANAGE permission
```

All categories are **admin-configurable**. The frontend must not assume fixed option lists.
