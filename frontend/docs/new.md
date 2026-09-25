perfect now we have to integrate the the admin dahsbaord related endpoints | Method | Path | Usage |
  |---|---|---|
  | GET | `/api/v1/clients/stats` | Client stats |
  | GET | `/api/v1/sessions/upcoming` | Upcoming sessions |
  | GET | `/api/v1/sessions/overdue` | Overdue sessions |
  | GET | `/api/v1/tasks/pending/count` | Pending tasks count |
  | GET | `/api/v1/billing/statistics` | Billing stats | these are the endpoints see the ADMIN_FRONTEND_INTEGRATION_GUIDE.md
  in this these endpoints are there which we have to integrate in the dhasbOARD of admin on this page /admin/dashboard on this
  look into the elements avaialble there if need any other endpoint just tell me but the rest of it implemnet it and r3emove
  the static fallback and proper loader on the api calling and error handling should be done look the toast or any error its
  already there as a component reuse them GET
  /api/v1/clients/stats
  Get client statistics


  Get aggregated statistics about clients. Requires THERAPIST, ADMIN, or SUPERVISOR role.

  Parameters
  Cancel
  No parameters

  Execute
  Responses
  Code  Description     Links
  200
  OK

  Media type

  application/json
  Controls Accept header.
  Example Value
  Schema
  {
    "totalClients": 0,
    "activeClients": 0,
    "pendingClients": 0,
    "completedClients": 0
  } 2. GET
  /api/v1/sessions/upcoming


  Parameters
  Cancel
  Name  Description
  limit
  integer($int32)
  (query)
  10
  Execute
  Responses
  Code  Description     Links
  200
  OK

  Media type

  application/json
  Controls Accept header.
  Example Value
  Schema
  [
    {
      "id": 0,
      "clientId": 0,
      "clientName": "string",
      "therapistId": 0,
      "therapistName": "string",
      "sessionDate": "2026-04-23T07:32:50.265Z",
      "duration": 0,
      "sessionType": "string",
      "status": "string",
      "serviceId": 0,
      "serviceName": "string",
      "roomId": 0,
      "roomName": "string",
      "notes": "string",
      "zoomEnabled": true,
      "zoomMeetingId": "string",
      "zoomJoinUrl": "string",
      "zoomPassword": "string",
      "createdAt": "2026-04-23T07:32:50.265Z",
      "updatedAt": "2026-04-23T07:32:50.265Z"
    }
  ]
  No links

  3. GET
  /api/v1/sessions/recent


  Parameters
  Cancel
  Name  Description
  limit
  integer($int32)
  (query)
  10
  Execute
  Responses
  Code  Description     Links
  200
  OK

  Media type

  application/json
  Controls Accept header.
  Example Value
  Schema
  [
    {
      "id": 0,
      "clientId": 0,
      "clientName": "string",
      "therapistId": 0,
      "therapistName": "string",
      "sessionDate": "2026-04-23T07:33:06.893Z",
      "duration": 0,
      "sessionType": "string",
      "status": "string",
      "serviceId": 0,
      "serviceName": "string",
      "roomId": 0,
      "roomName": "string",
      "notes": "string",
      "zoomEnabled": true,
      "zoomMeetingId": "string",
      "zoomJoinUrl": "string",
      "zoomPassword": "string",
      "createdAt": "2026-04-23T07:33:06.893Z",
      "updatedAt": "2026-04-23T07:33:06.893Z"
    }
  ]
  No links

  5.  GET
  /api/v1/sessions/overdue


  Parameters
  Cancel
  No parameters

  Execute
  Responses
  Code  Description     Links
  200
  OK

  Media type

  application/json
  Controls Accept header.
  Example Value
  Schema
  [
    {
      "id": 0,
      "clientId": 0,
      "clientName": "string",
      "therapistId": 0,
      "therapistName": "string",
      "sessionDate": "2026-04-23T07:33:32.895Z",
      "duration": 0,
      "sessionType": "string",
      "status": "string",
      "serviceId": 0,
      "serviceName": "string",
      "roomId": 0,
      "roomName": "string",
      "notes": "string",
      "zoomEnabled": true,
      "zoomMeetingId": "string",
      "zoomJoinUrl": "string",
      "zoomPassword": "string",
      "createdAt": "2026-04-23T07:33:32.895Z",
      "updatedAt": "2026-04-23T07:33:32.895Z"
    }
  ]
  No links

  GET 6. GET
  /api/v1/tasks/pending/count


  Parameters
  Cancel
  No parameters

  Execute
  Responses
  Code  Description     Links
  200
  OK

  Media type

  application/json
  Controls Accept header.
  Example Value
  Schema
  {} 7. GET
  /api/v1/tasks/recent


  Parameters
  Cancel
  Name  Description
  limit
  integer($int32)
  (query)
  10
  Execute
  Responses
  Code  Description     Links
  200
  OK

  Media type

  application/json
  Controls Accept header.
  Example Value
  Schema
  [
    {
      "id": 0,
      "title": "string",
      "description": "string",
      "status": "string",
      "priority": "string",
      "dueDate": "2026-04-23T07:34:43.571Z",
      "clientId": 0,
      "clientName": "string",
      "assignedToId": 0,
      "assignedToName": "string",
      "createdAt": "2026-04-23T07:34:43.571Z",
      "updatedAt": "2026-04-23T07:34:43.571Z"
    }
  ]
  No links

  8. GET
  /api/v1/tasks/upcoming


  Parameters
  Cancel
  Name  Description
  limit
  integer($int32)
  (query)
  10
  Execute
  Responses
  Code  Description     Links
  200
  OK

  Media type

  application/json
  Controls Accept header.
  Example Value
  Schema
      "id": 0,
      "title": "string",
      "description": "string",
      "status": "string",
      "priority": "string",
      "dueDate": "2026-04-23T07:35:25.957Z",
      "clientId": 0,
      "clientName": "string",
      "assignedToId": 0,
      "assignedToName": "string",
      "createdAt": "2026-04-23T07:35:25.957Z",
      "updatedAt": "2026-04-23T07:35:25.957Z"
    }
  ]
  No links

  analyze this list and implemnet it