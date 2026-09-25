Request URL
http://localhost:8080/api/v1/portal/forms/assignments
Request Method
GET
Status Code
200 OK
Remote Address
[::1]:8080
Referrer Policy
strict-origin-when-cross-origin
response
[
    {
        "id": 58,
        "templateId": 3,
        "templateVersionId": 23,
        "versionNumber": 6,
        "templateName": "Hippa privacy",
        "templateCategory": "CONSENT",
        "clientId": 10,
        "clientName": "Fahad Jameel",
        "assignedById": 2,
        "assignedByName": "Northstar adminstrator",
        "status": "ASSIGNED",
        "dueDate": "2026-05-30T04:53:47.409Z",
        "instructions": null,
        "completedAt": null,
        "submittedAt": null,
        "reviewedAt": null,
        "reviewedById": null,
        "reviewedByName": null,
        "reviewNotes": null,
        "remindersSent": 0,
        "lastReminderAt": null,
        "createdAt": "2026-05-23T04:53:48.364566Z",
        "updatedAt": "2026-05-23T04:53:48.445479Z",
        "responses": null,
        "signatures": null
    }
]

Request URL
http://localhost:8080/api/v1/portal/forms/responses/58
Request Method
GET
Status Code
200 OK
Remote Address
[::1]:8080
Referrer Policy
strict-origin-when-cross-origin

response 

[]

Request URL
http://localhost:8080/api/v1/portal/forms/signature/58
Request Method
GET
Status Code
404 Not Found
Remote Address
[::1]:8080
Referrer Policy
strict-origin-when-cross-origin

response 
{
    "timestamp": "2026-06-16T11:05:45.781062Z",
    "status": 404,
    "error": "Not Found",
    "message": "Signature not found",
    "code": "GENERIC_001",
    "path": "/api/v1/portal/forms/signature/58",
    "traceId": "39092678-9a48-4025-a9c4-92344fe4fde1",
    "details": null
}

Request URL
http://localhost:8080/api/v1/portal/forms/assignments/58
Request Method
GET
Status Code
200 OK
Remote Address
[::1]:8080
Referrer Policy
strict-origin-when-cross-origin

response 
{
    "template": {
        "instructions": "newwww",
        "requiresSignature": true,
        "name": "Hippa privacy",
        "description": "new desc",
        "id": 3,
        "category": "CONSENT",
        "fields": [
            {
                "id": 17,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "TEXT",
                "label": "DQSASAS",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 0,
                "createdAt": "2026-05-23T04:53:48.379235Z",
                "updatedAt": "2026-05-23T04:53:48.379235Z"
            },
            {
                "id": 18,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "HEADING",
                "label": "SFADAD",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 0,
                "createdAt": "2026-05-23T04:53:48.382386Z",
                "updatedAt": "2026-05-23T04:53:48.382386Z"
            },
            {
                "id": 19,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "HEADING",
                "label": "SFADAD",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 0,
                "createdAt": "2026-05-23T04:53:48.384586Z",
                "updatedAt": "2026-05-23T04:53:48.384586Z"
            },
            {
                "id": 20,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "TEXT",
                "label": "DQSASAS",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 1,
                "createdAt": "2026-05-23T04:53:48.386247Z",
                "updatedAt": "2026-05-23T04:53:48.386247Z"
            },
            {
                "id": 21,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "HEADING",
                "label": "SFADAD",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 1,
                "createdAt": "2026-05-23T04:53:48.387683Z",
                "updatedAt": "2026-05-23T04:53:48.387683Z"
            },
            {
                "id": 22,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "TEXT",
                "label": "DQSASAS",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 1,
                "createdAt": "2026-05-23T04:53:48.389505Z",
                "updatedAt": "2026-05-23T04:53:48.389505Z"
            },
            {
                "id": 23,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "TEXT",
                "label": "DQSASAS",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 2,
                "createdAt": "2026-05-23T04:53:48.391942Z",
                "updatedAt": "2026-05-23T04:53:48.391942Z"
            },
            {
                "id": 24,
                "templateVersionId": 23,
                "sectionId": null,
                "sectionName": null,
                "fieldType": "HEADING",
                "label": "SFADAD",
                "placeholder": null,
                "helpText": null,
                "isRequired": false,
                "options": null,
                "validation": null,
                "defaultValue": null,
                "isRepeatable": null,
                "scoringFormula": null,
                "maxScore": null,
                "autoPopulate": "",
                "conditionalDisplay": "{}",
                "sortOrder": 3,
                "createdAt": "2026-05-23T04:53:48.395156Z",
                "updatedAt": "2026-05-23T04:53:48.395156Z"
            }
        ]
    },
    "instructions": null,
    "completedAt": null,
    "clientId": 10,
    "dueDate": "2026-05-30T04:53:47.409Z",
    "templateId": 3,
    "createdAt": "2026-05-23T04:53:48.364566Z",
    "practiceData": {
        "website": "www.resiliencec.com",
        "address": "111 Waterloo St Unit 406, London, ON N6B 2M4",
        "phone": "+1 (548)866-0366",
        "name": "Resilience Counseling Research & Consultation",
        "email": "resiliencecrc@gmail.com"
    },
    "id": 58,
    "clientData": {
        "clientId": "CL-2026-0001",
        "phone": "03427090835",
        "fullName": "Fahad Jameel",
        "dateOfBirth": "2003-10-11",
        "email": "fahadjamil343@gmail.com"
    },
    "submittedAt": null,
    "therapistData": {
        "phone": null,
        "fullName": "Northstar adminstrator",
        "email": "northstar.admin@therapyflowseed.com"
    },
    "status": "ASSIGNED",
    "updatedAt": "2026-05-23T04:53:48.445479Z"
}


Request URL
http://localhost:8080/api/v1/portal/forms/responses
Request Method
POST
Status Code
500 Internal Server Error
Remote Address
[::1]:8080
Referrer Policy
strict-origin-when-cross-origin

payload
{"assignmentId":58,"assignmentFieldId":20,"value":"wwfwfwfwfwfwfwfwff"}

response 
{
    "timestamp": "2026-06-16T11:06:15.479296Z",
    "status": 500,
    "error": "Database Schema Error",
    "message": "A database schema error occurred. This usually indicates that the database needs to be updated with the latest migrations. Please contact the system administrator.",
    "code": "GENERIC_003",
    "path": "/api/v1/portal/forms/responses",
    "traceId": "79e8a259-25aa-4bd8-875f-735ea88ff7d8",
    "details": null
}


Request URL
http://localhost:8080/api/v1/portal/forms/responses
Request Method
POST
Status Code
500 Internal Server Error
Remote Address
[::1]:8080
Referrer Policy
strict-origin-when-cross-origin

payload 
{"assignmentId":58,"assignmentFieldId":22,"value":"wfwfwfwfwfwf"}
response 

{
    "timestamp": "2026-06-16T11:06:17.489547Z",
    "status": 500,
    "error": "Database Schema Error",
    "message": "A database schema error occurred. This usually indicates that the database needs to be updated with the latest migrations. Please contact the system administrator.",
    "code": "GENERIC_003",
    "path": "/api/v1/portal/forms/responses",
    "traceId": "c5d3b678-f0c8-49df-8959-d5301f3c53fe",
    "details": null
}


Request URL
http://localhost:8080/api/v1/portal/forms/responses
Request Method
POST
Status Code
500 Internal Server Error
Remote Address
[::1]:8080
Referrer Policy
strict-origin-when-cross-origin

payload
{"assignmentId":58,"assignmentFieldId":23,"value":"fwfwfwfwfwfwfwfwf"}

response

{
    "timestamp": "2026-06-16T11:06:18.909055Z",
    "status": 500,
    "error": "Database Schema Error",
    "message": "A database schema error occurred. This usually indicates that the database needs to be updated with the latest migrations. Please contact the system administrator.",
    "code": "GENERIC_003",
    "path": "/api/v1/portal/forms/responses",
    "traceId": "df60b997-db84-4534-aaf7-c17ef1736daf",
    "details": null
}