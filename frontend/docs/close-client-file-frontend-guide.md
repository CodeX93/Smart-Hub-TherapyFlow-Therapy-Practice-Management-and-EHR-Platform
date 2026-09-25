# Close Client File — Frontend Integration Guide

> **Audience:** Frontend engineers integrating the staff app with the SmartHub close/reopen client file backend (Java/Spring Boot).
>
> **Backend reference:** See [`close-client-file-rebuild.md`](./close-client-file-rebuild.md) for full business rules and edge cases.

---

## 1. Core rule

**Closing a client file is a status change, not a deletion.**

- Closing sets `status` to **inactive** (display value: `"Inactive"`).
- When closing without an explicit `stage`, the backend auto-sets `stage` to **closed** (display value: `"Closed"`).
- All historical data remains: sessions, notes, invoices, documents, transcripts, assessments, and history.
- **Do not** call `DELETE /api/v1/clients/{id}` to close a file — that is a separate admin-only destructive action.

---

## 2. API base path

This backend uses versioned routes:

| Legacy doc path | SmartHub Java path |
|-----------------|-------------------|
| `PUT /api/clients/:id` | `PUT /api/v1/clients/{id}` |
| `POST /api/clients/bulk-update-status` | `POST /api/v1/clients/bulk-update-status` |
| `GET /api/clients/:id/history` | `GET /api/v1/clients/{id}/history` |

All requests require `Authorization: Bearer <token>`.

---

## 3. Close a single client file

### Request

```http
PUT /api/v1/clients/{id}
Content-Type: application/json

{
  "status": "inactive"
}
```

Accepted status values (case-insensitive): `active`, `inactive`, `pending`, `discharged`.

### Response

`200 OK` with updated client body. Example fields:

```json
{
  "id": 42,
  "fullName": "Jane Doe",
  "status": "Inactive",
  "stage": "Closed",
  "...": "..."
}
```

### Backend side effects

- `status` → `Inactive`
- `stage` → `Closed` (only when `stage` was omitted in the request)
- Client history rows: `file_closed` and `stage_change` (when stage changes)
- Audit log: `client_updated`
- **Does not** cancel future or recurring sessions
- **Does not** delete any related records

### Who can close

Roles with `CLIENT_EDIT` and therapist/admin/supervisor access (via `@PreAuthorize`). **Billing specialists are blocked** (not in the allowed role set).

### Confirm dialog copy (use verbatim)

> Are you sure you want to close this client file? The file will become inactive and new sessions/notes cannot be added. All historical data will remain accessible.

---

## 4. Reopen a single client file (admin only)

### Request

```http
PUT /api/v1/clients/{id}
Content-Type: application/json

{
  "status": "active"
}
```

### Backend behavior

- `status` → `Active`
- `stage` is **unchanged** unless you also send `stage` in the body
- Client history row: `file_reopened`
- **Server enforces admin-only reopen** — non-admins receive `403 Forbidden`:
  `"Only administrators can reopen this file"`

### UI policy

- Show **Reopen File** only for users with the **admin** role when `status === "Inactive"`.
- Even though therapists can close files, only admins should see/use reopen in the UI (backend also blocks non-admins).

### Confirm dialog copy (use verbatim)

> Are you sure you want to reopen this client file? The client will be reactivated and new sessions/notes can be added.

---

## 5. Bulk close / reactivate

```http
POST /api/v1/clients/bulk-update-status
Content-Type: application/json

{
  "clientIds": [1, 2, 3],
  "status": "inactive"
}
```

**Roles:** Admin or supervisor (with scope limits for supervisors).

**Response:**

```json
{
  "total": 3,
  "successful": 3,
  "failed": 0,
  "skipped": null,
  "errors": []
}
```

Supervisors receive `403` if any client is outside their supervised therapists' caseload (all-or-nothing — no partial apply).

Each successful bulk item runs the same logic as the single `PUT` (including auto-stage and per-client history).

---

## 6. UI state when file is inactive

Derive from `client.status === "Inactive"` (or case-insensitive `"inactive"` in request payloads):

| UI element | Behavior |
|------------|----------|
| Banner | Red warning: **"This client file is INACTIVE"** — include that no new sessions/notes can be added; historical data remains accessible |
| Status badge | Red **INACTIVE** |
| Close File button | Hidden (client already inactive) |
| Reopen File button | Visible only for **admin** when inactive |
| Schedule session | **Disabled** with tooltip explaining inactive file |
| Add / edit notes | **Disabled** with tooltip explaining inactive file |
| View sessions, notes, documents, invoices | **Enabled** (read-only access) |

### Backend enforcement

The API also blocks **new** sessions and note create/update for inactive clients:

- Session create → `400` `"Cannot schedule sessions for an inactive client file"`
- Note create/update → `400` `"Cannot add or edit notes for an inactive client file"`

Existing scheduled sessions are **not** auto-cancelled when a file is closed.

---

## 7. Client history timeline

Fetch history:

```http
GET /api/v1/clients/{id}/history
```

Close/reopen events appear with timeline `eventType` values:

| Action | `eventType` in response |
|--------|-------------------------|
| Close file | `file_closed` |
| Reopen file | `file_reopened` |
| Stage auto-set to closed | `stage_change` |

Example history entry after close:

```json
{
  "eventType": "file_closed",
  "fromValue": "Active",
  "toValue": "Inactive",
  "description": "Client file closed and set to inactive",
  "createdByName": "therapist@example.com",
  "createdAt": "2026-06-15T10:30:00Z"
}
```

---

## 8. Recommended frontend implementation

### TanStack Query mutation (example)

```typescript
const updateClientStatusMutation = useMutation({
  mutationFn: ({ clientId, status }: { clientId: number; status: "active" | "inactive" }) =>
    apiRequest(`/api/v1/clients/${clientId}`, "PUT", { status }),
  onSuccess: (_, variables) => {
    queryClient.invalidateQueries({ queryKey: [`/api/v1/clients/${variables.clientId}`] });
    queryClient.invalidateQueries({ queryKey: ["/api/v1/clients"] });
    const action = variables.status === "inactive" ? "closed" : "reopened";
    toast({ title: "Success", description: `Client file ${action} successfully` });
  },
  onError: (error: Error) =>
    toast({
      title: "Error",
      description: error.message || "Failed to update client status",
      variant: "destructive",
    }),
});

function handleCloseFile(clientId: number) {
  if (window.confirm(CLOSE_CONFIRM_MESSAGE)) {
    updateClientStatusMutation.mutate({ clientId, status: "inactive" });
  }
}

function handleReopenFile(clientId: number) {
  if (window.confirm(REOPEN_CONFIRM_MESSAGE)) {
    updateClientStatusMutation.mutate({ clientId, status: "active" });
  }
}
```

### Invalidate after success

- `GET /api/v1/clients/{id}` — detail view
- `GET /api/v1/clients` — list view
- `GET /api/v1/clients/{id}/history` — if history panel is open

---

## 9. Error handling

| HTTP | Meaning | Frontend action |
|------|---------|-----------------|
| `401` | Not authenticated | Redirect to login |
| `403` | Insufficient role (e.g. billing specialist, non-admin reopen) | Show permission error |
| `404` | Client not found | Show not-found state |
| `400` | Invalid status/stage or invalid transition | Show validation message |
| `400` | Schedule/note on inactive client | Show inactive file message |

---

## 10. What not to do

| Don't | Do instead |
|-------|------------|
| `DELETE /api/v1/clients/{id}` to "close" | `PUT` with `{ "status": "inactive" }` |
| Cancel sessions client-side on close | Leave existing sessions; backend does not cancel them |
| Allow therapists to reopen in UI | Admin-only reopen button + API |
| Send `assignedTherapistId` on close unless admin | Omit field; backend strips it for non-admins |

---

## 11. Test checklist for frontend QA

- [ ] Close active client → status badge INACTIVE, stage Closed, banner shown
- [ ] Close confirm dialog uses exact copy from §3
- [ ] Schedule session and add/edit note controls disabled when inactive
- [ ] Historical sessions/notes/documents still load
- [ ] Reopen as admin → status Active, toast success, banner removed
- [ ] Reopen as therapist → button hidden; if forced via API, 403 shown
- [ ] Bulk close from client list works for admin/supervisor
- [ ] History timeline shows `file_closed` / `file_reopened` entries
