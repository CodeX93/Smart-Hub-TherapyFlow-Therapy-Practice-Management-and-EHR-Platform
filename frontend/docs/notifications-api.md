# Notifications API Reference

Base URL: `/api/v1/notifications`

## 1) Get My Notifications
- **GET** `/`
- Query params:
  - `unreadOnly` (optional, boolean)

### Response `200`
```json
[
  {
    "id": 101,
    "userId": 12,
    "clientId": null,
    "type": "APPOINTMENT_REMINDER",
    "category": "APPOINTMENT",
    "title": "Session reminder",
    "message": "You have a session at 3:00 PM",
    "data": "{\"sessionId\":123}",
    "priority": "Medium",
    "isRead": false,
    "readAt": null,
    "actionUrl": "/sessions/123",
    "actionLabel": "View session",
    "relatedEntityType": "session",
    "relatedEntityId": 123,
    "expiresAt": null,
    "createdAt": "2026-04-30T10:00:00Z"
  }
]
```

## 2) Get Tenant Notifications
- **GET** `/all`
- Query params:
  - `userId` (optional, long)
  - `unreadOnly` (optional, boolean)

### Response `200`
```json
[
  {
    "id": 201,
    "userId": 22,
    "clientId": null,
    "type": "SYSTEM_MAINTENANCE",
    "category": "SYSTEM",
    "title": "Maintenance Window",
    "message": "Platform update tonight",
    "priority": "Medium",
    "isRead": false,
    "actionUrl": "/notifications",
    "actionLabel": "View details",
    "relatedEntityType": "general",
    "relatedEntityId": null,
    "createdAt": "2026-04-30T11:00:00Z"
  }
]
```

## 3) Get Unread Count
- **GET** `/unread/count`

### Response `200`
```json
7
```

## 4) Get Unread Count (Object)
- **GET** `/unread-count`

### Response `200`
```json
{
  "count": 7
}
```

## 5) Mark Notification as Read
- **PATCH** `/{id}/read`

### Response `200`
No body.

## 6) Mark All as Read
- **PATCH** `/read-all`

### Response `200`
No body.

## 7) Mark All as Read for User
- **PATCH** `/users/{userId}/read-all`

### Response `200`
No body.

## 8) Mark One Notification as Read for User
- **PATCH** `/users/{userId}/{id}/read`

### Response `200`
No body.

## 9) Mark All Read (Alt)
- **PUT** `/mark-all-read`

### Response `200`
```json
{
  "success": true
}
```

## 10) Delete Notification
- **DELETE** `/{id}`

### Response `200`
```json
{
  "success": true
}
```

## 11) Create Notification (Single Recipient)
- **POST** `/`

### Request
```json
{
  "userId": 12,
  "clientId": null,
  "type": "APPOINTMENT_REMINDER",
  "category": "APPOINTMENT",
  "title": "Appointment Reminder",
  "message": "Your appointment starts in 1 hour.",
  "data": "{\"sessionId\":123}",
  "priority": "MEDIUM",
  "actionUrl": "/sessions/123",
  "actionLabel": "View session",
  "relatedEntityType": "session",
  "relatedEntityId": 123,
  "expiresAt": "2026-05-01T00:00:00Z"
}
```

### Response `201`
```json
{
  "id": 301,
  "userId": 12,
  "clientId": null,
  "type": "APPOINTMENT_REMINDER",
  "category": "APPOINTMENT",
  "title": "Appointment Reminder",
  "message": "Your appointment starts in 1 hour.",
  "data": "{\"sessionId\":123}",
  "priority": "Medium",
  "isRead": false,
  "readAt": null,
  "actionUrl": "/sessions/123",
  "actionLabel": "View session",
  "relatedEntityType": "session",
  "relatedEntityId": 123,
  "expiresAt": "2026-05-01T00:00:00Z",
  "createdAt": "2026-04-30T12:00:00Z"
}
```

## 12) Create Tenant Broadcast Notification
- **POST** `/broadcast`

### Request
```json
{
  "targetType": "BOTH",
  "type": "SYSTEM_MAINTENANCE",
  "category": "SYSTEM",
  "title": "Platform Notice",
  "message": "System maintenance tonight at 11 PM.",
  "data": "{\"noticeType\":\"maintenance\"}",
  "priority": "MEDIUM",
  "actionUrl": "/notifications",
  "actionLabel": "View details",
  "relatedEntityType": "general",
  "relatedEntityId": null,
  "expiresAt": "2026-05-02T00:00:00Z"
}
```

### Response `202`
```json
{
  "status": "accepted",
  "message": "Broadcast queued for delivery"
}
```

## 13) Get User Preferences
- **GET** `/preferences`

### Response `200`
```json
[
  {
    "id": 1,
    "userId": 12,
    "notificationType": "APPOINTMENT_REMINDER",
    "emailEnabled": true,
    "smsEnabled": false,
    "inAppEnabled": true,
    "quietHoursStart": "22:00",
    "quietHoursEnd": "07:00"
  }
]
```

## 14) Update User Preference
- **PUT** `/preferences/{triggerType}`

### Request
```json
{
  "emailEnabled": true,
  "smsEnabled": false,
  "inAppEnabled": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "07:00"
}
```

### Response `200`
```json
{
  "id": 1,
  "userId": 12,
  "notificationType": "APPOINTMENT_REMINDER",
  "emailEnabled": true,
  "smsEnabled": false,
  "inAppEnabled": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "07:00"
}
```

## 15) Get Notification Stats
- **GET** `/stats`

### Response `200`
```json
{
  "total": 120,
  "unread": 9,
  "read": 111,
  "byCategory": {
    "APPOINTMENT": 50,
    "SYSTEM": 20,
    "TASK": 30,
    "BILLING": 20
  }
}
```

## 16) Get Setup Health
- **GET** `/setup-health`

### Response `200`
```json
{
  "scope": "tenant",
  "healthy": true,
  "coveragePercent": 100,
  "items": []
}
```

## 17) Get Setup Coverage
- **GET** `/setup/coverage`

### Response `200`
```json
{
  "scope": "tenant",
  "healthy": true,
  "coveragePercent": 100,
  "items": []
}
```

## 18) Get Event Catalog
- **GET** `/setup/events`

### Response `200`
```json
{
  "scope": "tenant",
  "events": [
    {
      "eventType": "session_scheduled",
      "defaultChannels": ["in_app", "email"],
      "required": true,
      "sessionHealthEvent": true
    },
    {
      "eventType": "task_assigned",
      "defaultChannels": ["in_app"],
      "required": true,
      "sessionHealthEvent": false
    }
  ]
}
```

## 19) Get Action Metadata
- **GET** `/setup/action-metadata`

### Response `200`
```json
{
  "scope": "tenant",
  "entities": [
    {
      "relatedEntityType": "session",
      "actionUrlTemplate": "/sessions/{id}",
      "defaultActionLabel": "View session",
      "exampleActionUrl": "/sessions/123"
    },
    {
      "relatedEntityType": "client",
      "actionUrlTemplate": "/clients/{id}",
      "defaultActionLabel": "View client",
      "exampleActionUrl": "/clients/456"
    }
  ]
}
```

## 20) Upsert Action Metadata
- **PUT** `/setup/action-metadata/{relatedEntityType}`

### Request
```json
{
  "actionUrlTemplate": "/sessions/{id}",
  "defaultActionLabel": "Open session",
  "exampleActionUrl": "/sessions/123",
  "sortOrder": 10,
  "isActive": true
}
```

### Response `200`
```json
{
  "relatedEntityType": "session",
  "actionUrlTemplate": "/sessions/{id}",
  "defaultActionLabel": "Open session",
  "exampleActionUrl": "/sessions/123"
}
```

## 21) Delete Action Metadata
- **DELETE** `/setup/action-metadata/{relatedEntityType}`

### Response `200`
```json
{
  "success": true,
  "relatedEntityType": "session"
}
```

## 22) Seed Action Metadata
- **POST** `/setup/action-metadata/seed`
- Query params:
  - `overwrite` (optional boolean, default `false`)

### Response `200`
```json
{
  "created": 11,
  "updated": 0,
  "skipped": 0
}
```

## 23) Sync Notification Defaults
- **POST** `/setup/sync`

### Response `200`
```json
{
  "success": true,
  "message": "Notification defaults synced"
}
```

## 24) Cleanup Expired Notifications
- **POST** `/cleanup`

### Response `200`
```json
{
  "success": true,
  "message": "Expired notifications cleaned up"
}
```

## 25) Get Triggers
- **GET** `/triggers`

### Response `200`
```json
[
  {
    "id": 1,
    "name": "Session Scheduled",
    "eventType": "session_scheduled",
    "entityType": "SESSION",
    "priority": "medium",
    "isActive": true
  }
]
```

## 26) Create Trigger
- **POST** `/triggers`

### Request
```json
{
  "name": "Session Reminder",
  "description": "Notify before session",
  "eventType": "session_reminder",
  "entityType": "SESSION",
  "conditionRules": "{}",
  "recipientRules": "{}",
  "priority": "high",
  "delayMinutes": 0,
  "batchWindowMinutes": 5,
  "maxBatchSize": 10,
  "isScheduled": false,
  "isActive": true
}
```

### Response `201`
```json
{
  "id": 2,
  "name": "Session Reminder",
  "eventType": "session_reminder",
  "entityType": "SESSION",
  "priority": "high",
  "isActive": true
}
```

## 27) Update Trigger
- **PUT** `/triggers/{id}`

### Request
```json
{
  "name": "Session Reminder Updated",
  "description": "Notify before session",
  "eventType": "session_reminder",
  "entityType": "SESSION",
  "conditionRules": "{}",
  "recipientRules": "{}",
  "priority": "high",
  "delayMinutes": 15,
  "batchWindowMinutes": 5,
  "maxBatchSize": 10,
  "isScheduled": true,
  "isActive": true
}
```

### Response `200`
```json
{
  "id": 2,
  "name": "Session Reminder Updated",
  "eventType": "session_reminder",
  "entityType": "SESSION",
  "priority": "high",
  "isActive": true
}
```

## 28) Delete Trigger
- **DELETE** `/triggers/{id}`

### Response `204`
No body.

## 29) Get Templates
- **GET** `/templates`
- Query params:
  - `type` (optional string)

### Response `200`
```json
[
  {
    "id": 1,
    "name": "Session Reminder Email",
    "type": "email",
    "eventType": "session_reminder",
    "subjectTemplate": "Reminder: Your session",
    "bodyTemplate": "Your session is at {{sessionTime}}",
    "isSystem": true,
    "isActive": true
  }
]
```

## 30) Create Template
- **POST** `/templates`

### Request
```json
{
  "name": "Billing Due Reminder",
  "type": "email",
  "eventType": "bill_due_reminder",
  "subjectTemplate": "Invoice Due",
  "bodyTemplate": "Invoice {{invoiceId}} is due on {{dueDate}}",
  "actionUrlTemplate": "/billing/{id}",
  "actionLabel": "View invoice",
  "isSystem": false,
  "isActive": true
}
```

### Response `201`
```json
{
  "id": 2,
  "name": "Billing Due Reminder",
  "type": "email",
  "eventType": "bill_due_reminder",
  "subjectTemplate": "Invoice Due",
  "bodyTemplate": "Invoice {{invoiceId}} is due on {{dueDate}}",
  "isSystem": false,
  "isActive": true
}
```

## 31) Update Template
- **PUT** `/templates/{id}`

### Request
```json
{
  "name": "Billing Due Reminder v2",
  "type": "email",
  "eventType": "bill_due_reminder",
  "subjectTemplate": "Invoice Due Soon",
  "bodyTemplate": "Invoice {{invoiceId}} is due tomorrow",
  "actionUrlTemplate": "/billing/{id}",
  "actionLabel": "Open invoice",
  "isSystem": false,
  "isActive": true
}
```

### Response `200`
```json
{
  "id": 2,
  "name": "Billing Due Reminder v2",
  "type": "email",
  "eventType": "bill_due_reminder",
  "subjectTemplate": "Invoice Due Soon",
  "bodyTemplate": "Invoice {{invoiceId}} is due tomorrow",
  "isSystem": false,
  "isActive": true
}
```

## 32) Delete Template
- **DELETE** `/templates/{id}`

### Response `204`
No body.

---

## Common Error Shape
```json
{
  "timestamp": "2026-04-30T12:34:56Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_NOTIFICATION_PRIORITY",
  "message": "Invalid priority. Allowed values: LOW, MEDIUM, HIGH, URGENT",
  "path": "/api/v1/notifications"
}
```

## Notes
- For **single create** (`POST /`): provide either `userId` or `clientId`.
- `priority` accepted values: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- Broadcast (`POST /broadcast`) runs async and returns `202`.
- Action metadata is now tenant-dynamic and DB-backed.
