# System Options — Frontend Delta (V44 + workflow changes)

> **Purpose:** Frontend-only guide for the **latest** backend changes. Use this alongside the main [System Options Frontend Integration Guide](./system-options-frontend-integration.md).
>
> **Backend migration:** tenant Flyway **V44** (adds new DB columns). Restart the app or run tenant migrations before testing.
>
> **Audience:** Frontend developers updating task, client, and insurance forms.

---

## 1. What changed (summary)

| Area | Before | After |
|------|--------|-------|
| **Task title** | Free text only | Optional catalog link via `titleKey`; `title` can still be custom text |
| **Task type** | Not stored | Optional `taskType` → `task_types` catalog |
| **Insurance type** | Not stored | Optional `insuranceType` → `insurance_types` catalog |
| **Treatment modality** | Not on client | Optional `treatmentModality` on client → `treatment_modalities` catalog |
| **Bulk client status** | Hardcoded `active` / `inactive` / `pending` / `discharged` | Any valid `client_status` catalog key |
| **Status/stage transitions** | Backend enum graph (inconsistent) | **Removed** — any valid catalog value allowed |

All new fields accept **`optionKey`** (preferred) or **`optionLabel`** on input; backend normalizes to keys.

---

## 2. Fetch catalog options

Same as the main guide:

```http
GET /api/v1/system-options/by-category/{categoryKey}
Authorization: Bearer <staff JWT>
```

| UI control | `categoryKey` |
|------------|---------------|
| Task title picker | `task_titles` |
| Task type | `task_types` |
| Insurance type | `insurance_types` |
| Treatment modality | `treatment_modalities` |
| Bulk status dropdown | `client_status` |

---

## 3. Task forms

**Endpoints:** `POST /api/v1/tasks`, `PUT /api/v1/tasks/{id}`

### 3.1 New request fields

| Field | Required | `categoryKey` | Notes |
|-------|----------|---------------|-------|
| `title` | Yes on create* | — | Display text shown in UI |
| `titleKey` | No | `task_titles` | Send when user picks from predefined titles |
| `taskType` | No | `task_types` | e.g. `client_contact`, `documentation` |

\*On create, `title` is required unless you send `titleKey` (backend derives `title` from catalog label).

### 3.2 How title resolution works

```
User picks catalog title  →  send titleKey (+ optional title label)
User types custom title   →  send title only (titleKey stays null)
User sends key as title   →  backend auto-links titleKey if it matches catalog
```

**Recommended pattern (dropdown):**

```typescript
// User selects "Complete Initial Assessment"
onTitleSelect(option: SystemOption) {
  setTitleKey(option.optionKey);      // "initial_assessment"
  setTitle(option.optionLabel);       // "Complete Initial Assessment"
}
```

**Custom title:**

```typescript
setTitleKey(null);
setTitle("Call pharmacy about refill");
```

### 3.3 Create task example

```http
POST /api/v1/tasks
Content-Type: application/json

{
  "clientId": 42,
  "titleKey": "initial_assessment",
  "title": "Complete Initial Assessment",
  "taskType": "documentation",
  "status": "pending",
  "priority": "medium",
  "dueDate": "2026-07-10T17:00:00Z"
}
```

Custom title example:

```json
{
  "clientId": 42,
  "title": "Follow up with school counselor",
  "taskType": "client_contact",
  "status": "pending",
  "priority": "high"
}
```

### 3.4 Task response (new fields)

```json
{
  "id": 99,
  "title": "Complete Initial Assessment",
  "titleKey": "initial_assessment",
  "taskType": "documentation",
  "status": "pending",
  "priority": "medium",
  "clientId": 42,
  "clientName": "Jane Doe"
}
```

| Response field | Format | Display |
|----------------|--------|---------|
| `titleKey` | Raw **option key** (or `null`) | Resolve via `task_titles` cache |
| `taskType` | Raw **option key** (or `null`) | Resolve via `task_types` cache |
| `title` | Plain text | Show as-is |
| `status`, `priority` | Raw option keys | Resolve via catalog |

### 3.5 TypeScript

```typescript
export interface CreateTaskRequest {
  clientId: number;
  title: string;
  titleKey?: string;
  taskType?: string;
  description?: string;
  status: string;
  priority: string;
  assignedToId?: number;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  titleKey?: string;
  taskType?: string | null; // send null or "" to clear
  description?: string;
  status?: string;
  priority?: string;
  clientId?: number;
  assignedToId?: number;
  dueDate?: string;
}

export interface TaskResponse {
  id: number;
  title: string;
  titleKey?: string | null;
  taskType?: string | null;
  status: string;
  priority: string;
  // ...existing fields
}
```

### 3.6 UI checklist — tasks

- [ ] Add **Task type** dropdown → `GET .../by-category/task_types` → bind `taskType` to `optionKey`
- [ ] Add **Title** dropdown (optional) → `GET .../by-category/task_titles` → set `titleKey` + `title` label on select
- [ ] Keep free-text title input for custom tasks
- [ ] On edit, pre-fill `titleKey` / `taskType` from GET response
- [ ] In task list, resolve `taskType` and `titleKey` to labels from cached options

---

## 4. Client — treatment modality

**Endpoints:** `POST /api/v1/clients`, `PUT /api/v1/clients/{id}`

### 4.1 Request field

| Field | Required | `categoryKey` | Example keys |
|-------|----------|---------------|--------------|
| `treatmentModality` | No | `treatment_modalities` | `cbt`, `dbt`, `emdr`, `psychodynamic` |

```json
{
  "fullName": "Jane Doe",
  "status": "active",
  "stage": "intake",
  "treatmentModality": "cbt"
}
```

Clear the value: send `""` or `null` on update.

### 4.2 Client response

`ClientResponse.treatmentModality` returns the **resolved label** (e.g. `"Cognitive Behavioral Therapy"`), not the raw key — same pattern as `status` / `serviceType` on the client list.

For edit forms, prefer keeping the **option key** in form state (from your dropdown selection) rather than round-tripping the label from GET.

### 4.3 UI checklist — client

- [ ] Add **Treatment modality** dropdown on client create/edit
- [ ] Fetch `treatment_modalities` from system options
- [ ] Submit `optionKey`; display `optionLabel` in read-only views

---

## 5. Insurance — insurance type

**Endpoints:**

- `PUT /api/v1/clients/{clientId}/insurance` (upsert)
- `GET /api/v1/clients/{clientId}/insurance`
- Also on nested fields in `POST/PUT /api/v1/clients` (`insuranceType` on create/update client)

### 5.1 Request field

| Field | Required | `categoryKey` | Example keys |
|-------|----------|---------------|--------------|
| `insuranceProvider` | Yes (insurance upsert) | `insurance_providers` | `blue_cross`, `none` |
| `insuranceType` | No | `insurance_types` | `private_insurance`, `medicare`, `medicaid`, `self_pay` |

```http
PUT /api/v1/clients/42/insurance
Content-Type: application/json

{
  "insuranceProvider": "blue_cross",
  "insuranceType": "private_insurance",
  "policyNumber": "POL123456789",
  "groupNumber": "GRP001"
}
```

### 5.2 Response formats (important)

| Endpoint | `insuranceType` format |
|----------|------------------------|
| `ClientInsuranceResponse` | Raw **option key** |
| `ClientResponse` (embedded insurance) | Resolved **label** |

```typescript
// Dedicated insurance endpoint — keys
interface ClientInsuranceResponse {
  insuranceProvider: string; // key
  insuranceType?: string | null; // key
  policyNumber: string;
  // ...
}

// Client list/detail — labels for display
interface ClientResponse {
  insuranceProvider?: string;
  insuranceType?: string; // label when present
  treatmentModality?: string; // label when present
  // ...
}
```

### 5.3 UI checklist — insurance

- [ ] Add **Insurance type** dropdown → `insurance_types`
- [ ] Submit `insuranceType` as `optionKey` alongside `insuranceProvider`
- [ ] On insurance edit screen, use `GET .../insurance` (keys) to pre-fill dropdowns
- [ ] On client list, use label from `ClientResponse` or resolve keys client-side

---

## 6. Bulk client status update

**Endpoint:** `POST /api/v1/clients/bulk-update-status`

### 6.1 Change

**Before:** Only `active`, `inactive`, `pending`, `discharged` accepted.

**After:** Any valid key from the tenant's `client_status` catalog (same as single-client update).

```http
POST /api/v1/clients/bulk-update-status
Content-Type: application/json

{
  "clientIds": [10, 11, 12],
  "status": "inactive"
}
```

### 6.2 UI checklist — bulk actions

- [ ] Populate bulk status dropdown from `client_status` catalog (not a hardcoded array)
- [ ] Remove `discharged` if it is not in the tenant catalog (default seed has `pending`, `active`, `inactive` only)
- [ ] If admin adds custom statuses, they appear automatically when fetched from API

---

## 7. Client status & stage (workflow reminder)

No code changes required if you already migrated to dynamic options, but **remove** any leftover transition logic:

| Remove | Keep |
|--------|------|
| `ALLOWED_STATUS_TRANSITIONS` maps | Fetch all active `client_status` / `client_stage` options |
| Filtering dropdown to "valid next" states | Submit any valid catalog key |
| Handling `400 Invalid status transition` | Handle `403` on reopen (`inactive` → `active`, admin only) |
| | File close: `status: inactive` auto-sets `stage` to `closed` when stage omitted |

---

## 8. Default seed examples

### `task_titles` (partial)

| `optionKey` | `optionLabel` |
|-------------|---------------|
| `initial_assessment` | Complete Initial Assessment |
| `treatment_plan` | Develop Treatment Plan |
| `insurance_verification` | Verify Insurance Coverage |
| `progress_review` | Review Client Progress |

### `task_types`

| `optionKey` | `optionLabel` |
|-------------|---------------|
| `client_contact` | Client Contact |
| `documentation` | Documentation |
| `billing` | Billing |
| `scheduling` | Scheduling |

### `insurance_types`

| `optionKey` | `optionLabel` |
|-------------|---------------|
| `private_insurance` | Private Insurance |
| `medicare` | Medicare |
| `medicaid` | Medicaid |
| `self_pay` | Self Pay |

### `treatment_modalities`

| `optionKey` | `optionLabel` |
|-------------|---------------|
| `cbt` | Cognitive Behavioral Therapy |
| `dbt` | Dialectical Behavior Therapy |
| `emdr` | EMDR |
| `psychodynamic` | Psychodynamic Therapy |

Tenants can add or rename options in admin settings — do not hardcode these lists.

---

## 9. React dropdown example (task type)

```tsx
function TaskTypeSelect({ value, onChange }: { value?: string; onChange: (key: string) => void }) {
  const { data: options = [] } = useSystemOptions('task_types');

  return (
    <Select
      allowClear
      placeholder="Task type (optional)"
      value={value}
      onChange={onChange}
      options={options.map((o) => ({ value: o.optionKey, label: o.optionLabel }))}
    />
  );
}
```

---

## 10. Error handling

| HTTP | When |
|------|------|
| `400` | Invalid `optionKey` (not in catalog): `taskType`, `titleKey`, `insuranceType`, `treatmentModality`, bulk `status` |
| `403` | Reopen file (`inactive` → `active`) by non-admin |
| `404` | Client / task / insurance not found |

---

## 11. Migration checklist (frontend team)

```
Tasks
  [ ] task_types dropdown → taskType
  [ ] task_titles dropdown → titleKey + title label
  [ ] Custom title still supported
  [ ] Task list/detail shows taskType label

Client
  [ ] treatmentModality dropdown on create/edit
  [ ] Bulk status from client_status catalog

Insurance
  [ ] insuranceType dropdown on insurance form
  [ ] insuranceProvider still from insurance_providers catalog

Cleanup
  [ ] Remove status/stage transition restriction UI
  [ ] Remove hardcoded bulk status array
```

---

## 12. Related docs

- [System Options — Full Frontend Integration Guide](./system-options-frontend-integration.md)
- [Recurring Sessions](./recurring-sessions.md) — session `sessionMode` / `sessionType` (unchanged in V44)
