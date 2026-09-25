Request URL
https://api.therapyflow.pro/api/v1/tasks
Request Method
POST
Status Code
403 Forbidden

payload
{"title":"Complete Initial Assessment","description":"testing","priority":"low","status":"pending","clientId":10,"assignedToId":6,"dueDate":"2026-04-07T18:59:59.000Z"}

response
{
    "timestamp": "2026-04-24T04:54:32.030027466Z",
    "status": 403,
    "error": "Forbidden",
    "message": "You have no supervised therapists assigned. Cannot assign tasks.",
    "code": "AUTH_003",
    "path": "/api/v1/tasks",
    "traceId": "517b2e3c-82c8-4b9d-9616-38a2053c2a6c",
    "details": null
}

2. 