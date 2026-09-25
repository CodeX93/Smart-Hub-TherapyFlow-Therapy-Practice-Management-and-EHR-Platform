# SmartHub source-discovered testing inventory

Status: **discovered, not tested**. Generated from local working-tree source. IDs are snapshot identifiers; assign stable scenario IDs during reconciliation.

Backend revision: `0d8ae48a1eab8a0128e5d0ef5b0247ac0288a10552b311a4c28f272171c48251`. Frontend revision: `8126f0354a5fd10d269dde94e1e4c477be6322da`.

Tracked working-tree changes at discovery: present; see JSON manifest. Revision IDs alone do not identify all inspected content.

- Working-tree source snapshot, not a test execution or deployed-state audit.
- Java annotation scan requires reconciliation with runtime route registration; composed/inherited mappings may be missed.
- Frontend AST declarations and control candidates do not prove reachability or backend wiring.
- No existing test is credited as passing. Every discovered item still requires scenario mapping and execution.

## Counts

- endpoints: 870
- queryInputs: 573
- requestFields: 1390
- jobsAndListeners: 29
- routes: 137
- filterTypes: 66
- filterState: 392
- uiControls: 2766
- frontendFiles: 865
- testFiles: 223

## Frontend route declarations

- [ ] `/` — [src/routes/index.tsx:124](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:124)
- [ ] `/auth` — [src/routes/index.tsx:126](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:126)
- [ ] `/auth/login` — [src/routes/index.tsx:129](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:129)
- [ ] `/auth/forgot-password` — [src/routes/index.tsx:130](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:130)
- [ ] `/auth/check-email` — [src/routes/index.tsx:131](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:131)
- [ ] `/auth/password-reset-success` — [src/routes/index.tsx:132](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:132)
- [ ] `/auth/set-new-password` — [src/routes/index.tsx:136](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:136)
- [ ] `/auth/activate-your-account` — [src/routes/index.tsx:140](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:140)
- [ ] `/auth/staff/login` — [src/routes/index.tsx:146](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:146)
- [ ] `/auth/staff/forgot-password` — [src/routes/index.tsx:147](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:147)
- [ ] `/auth/staff/check-email` — [src/routes/index.tsx:151](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:151)
- [ ] `/auth/staff/password-reset-success` — [src/routes/index.tsx:152](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:152)
- [ ] `/auth/staff/set-new-password` — [src/routes/index.tsx:156](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:156)
- [ ] `/auth/staff/activate-your-account` — [src/routes/index.tsx:160](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:160)
- [ ] `/auth/therapist/login` — [src/routes/index.tsx:164](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:164)
- [ ] `/auth/therapist/forgot-password` — [src/routes/index.tsx:165](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:165)
- [ ] `/auth/therapist/check-email` — [src/routes/index.tsx:169](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:169)
- [ ] `/auth/therapist/password-reset-success` — [src/routes/index.tsx:173](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:173)
- [ ] `/auth/therapist/set-new-password` — [src/routes/index.tsx:177](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:177)
- [ ] `/auth/therapist/activate-your-account` — [src/routes/index.tsx:181](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:181)
- [ ] `/portal` — [src/routes/index.tsx:187](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:187)
- [ ] `/portal/activate/:token` — [src/routes/index.tsx:188](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:188)
- [ ] `/portal/invoices` — [src/routes/index.tsx:190](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:190)
- [ ] `/stripe/success` — [src/routes/index.tsx:191](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:191)
- [ ] `/billing/subscription` — [src/routes/index.tsx:192](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:192)
- [ ] `/billing/subscription/success` — [src/routes/index.tsx:193](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:193)
- [ ] `/super-admin/login` — [src/routes/index.tsx:195](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:195)
- [ ] `/super-admin/forgot-password` — [src/routes/index.tsx:196](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:196)
- [ ] `/user` — [src/routes/index.tsx:199](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:199)
- [ ] `/user/appointments` — [src/routes/index.tsx:200](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:200)
- [ ] `/user/booked-sessions` — [src/routes/index.tsx:201](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:201)
- [ ] `/user/booked-sessions/:sessionId` — [src/routes/index.tsx:202](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:202)
- [ ] `/user/invoices` — [src/routes/index.tsx:203](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:203)
- [ ] `/user/documents` — [src/routes/index.tsx:204](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:204)
- [ ] `/user/clinical-forms` — [src/routes/index.tsx:205](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:205)
- [ ] `/user/clinical-forms/:formId` — [src/routes/index.tsx:206](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:206)
- [ ] `/user/privacy-settings` — [src/routes/index.tsx:207](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:207)
- [ ] `/user/my-profile` — [src/routes/index.tsx:208](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:208)
- [ ] `/therapist` — [src/routes/index.tsx:211](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:211)
- [ ] `/therapist/dashboard` — [src/routes/index.tsx:212](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:212)
- [ ] `/therapist/clients` — [src/routes/index.tsx:213](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:213)
- [ ] `/therapist/clients/:clientId` — [src/routes/index.tsx:214](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:214)
- [ ] `/therapist/scheduling` — [src/routes/index.tsx:215](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:215)
- [ ] `/therapist/billings` — [src/routes/index.tsx:216](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:216)
- [ ] `/therapist/tasks` — [src/routes/index.tsx:217](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:217)
- [ ] `/therapist/tasks/history` — [src/routes/index.tsx:218](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:218)
- [ ] `/therapist/system/notifications` — [src/routes/index.tsx:219](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:219)
- [ ] `/therapist/system/notification` — [src/routes/index.tsx:220](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:220)
- [ ] `/therapist/security` — [src/routes/index.tsx:221](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:221)
- [ ] `/admin` — [src/routes/index.tsx:224](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:224)
- [ ] `/admin/dashboard` — [src/routes/index.tsx:225](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:225)
- [ ] `/admin/scheduling` — [src/routes/index.tsx:226](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:226)
- [ ] `/admin/settings` — [src/routes/index.tsx:227](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:227)
- [ ] `/admin/payments-and-subscription` — [src/routes/index.tsx:228](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:228)
- [ ] `/admin/security` — [src/routes/index.tsx:229](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:229)
- [ ] `/admin/clients` — [src/routes/index.tsx:230](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:230)
- [ ] `/admin/billings` — [src/routes/index.tsx:231](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:231)
- [ ] `/admin/tasks` — [src/routes/index.tsx:232](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:232)
- [ ] `/admin/tasks/history` — [src/routes/index.tsx:233](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:233)
- [ ] `/admin/system/notifications` — [src/routes/index.tsx:234](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:234)
- [ ] `/admin/system/notification` — [src/routes/index.tsx:235](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:235)
- [ ] `/admin/content/library` — [src/routes/index.tsx:236](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:236)
- [ ] `/admin/content/assessment` — [src/routes/index.tsx:237](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:237)
- [ ] `/admin/content/clinical-forms` — [src/routes/index.tsx:238](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:238)
- [ ] `/admin/content/process-checklists` — [src/routes/index.tsx:239](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:239)
- [ ] `/admin/content/report-templates` — [src/routes/index.tsx:240](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:240)
- [ ] `/admin/compliance/privacy` — [src/routes/index.tsx:241](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:241)
- [ ] `/admin/compliance/hipaa` — [src/routes/index.tsx:242](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:242)
- [ ] `/admin/user-access/profiles` — [src/routes/index.tsx:243](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:243)
- [ ] `/admin/user-access/roles` — [src/routes/index.tsx:244](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:244)
- [ ] `/admin/user-access/duplicate-detection` — [src/routes/index.tsx:245](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:245)
- [ ] `/admin/billing/subscription` — [src/routes/index.tsx:246](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:246)
- [ ] `/staff` — [src/routes/index.tsx:249](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:249)
- [ ] `/staff` (index) — [src/routes/index.tsx:250](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:250)
- [ ] `/staff/no-access` — [src/routes/index.tsx:251](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:251)
- [ ] `/staff/clients` — [src/routes/index.tsx:252](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:252)
- [ ] `/staff/clients/:clientId` — [src/routes/index.tsx:260](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:260)
- [ ] `/staff/scheduling` — [src/routes/index.tsx:268](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:268)
- [ ] `/staff/billings` — [src/routes/index.tsx:276](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:276)
- [ ] `/staff/tasks` — [src/routes/index.tsx:284](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:284)
- [ ] `/staff/tasks/history` — [src/routes/index.tsx:292](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:292)
- [ ] `/staff/user-access/profiles` — [src/routes/index.tsx:300](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:300)
- [ ] `/staff/system/notifications` — [src/routes/index.tsx:308](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:308)
- [ ] `/staff/system/notification` — [src/routes/index.tsx:316](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:316)
- [ ] `/staff/security` — [src/routes/index.tsx:317](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:317)
- [ ] `/staff/content/library` — [src/routes/index.tsx:318](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:318)
- [ ] `/staff/content/assessment` — [src/routes/index.tsx:326](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:326)
- [ ] `/staff/content/clinical-forms` — [src/routes/index.tsx:334](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:334)
- [ ] `/staff/content/process-checklists` — [src/routes/index.tsx:342](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:342)
- [ ] `/staff/compliance/privacy` — [src/routes/index.tsx:350](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:350)
- [ ] `/staff/compliance/hipaa` — [src/routes/index.tsx:358](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:358)
- [ ] `/staff/billing/subscription` — [src/routes/index.tsx:366](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:366)
- [ ] `admin/content/clinical-forms/consent/:formId` — [src/routes/index.tsx:368](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:368)
- [ ] `admin/content/assessment/create-assessment` — [src/routes/index.tsx:369](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:369)
- [ ] `staff/content/clinical-forms/consent/:formId` — [src/routes/index.tsx:370](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:370)
- [ ] `staff/content/assessment/create-assessment` — [src/routes/index.tsx:371](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:371)
- [ ] `/super-admin` — [src/routes/index.tsx:373](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:373)
- [ ] `/super-admin/dashboard` — [src/routes/index.tsx:374](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:374)
- [ ] `/super-admin/organisations` — [src/routes/index.tsx:375](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:375)
- [ ] `/super-admin/organisations/new` — [src/routes/index.tsx:376](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:376)
- [ ] `/super-admin/organisations/:slug` — [src/routes/index.tsx:377](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:377)
- [ ] `/super-admin/organisations/:slug/settings` — [src/routes/index.tsx:378](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:378)
- [ ] `/super-admin/organisations/:slug/impersonate` — [src/routes/index.tsx:379](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:379)
- [ ] `/super-admin/organisations/:slug/feature-toggles` — [src/routes/index.tsx:383](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:383)
- [ ] `/super-admin/organisations/:slug/invoices` — [src/routes/index.tsx:387](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:387)
- [ ] `/super-admin/roles-and-permissions` — [src/routes/index.tsx:391](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:391)
- [ ] `/super-admin/roles-and-permissions/create` — [src/routes/index.tsx:395](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:395)
- [ ] `/super-admin/feature-flags` — [src/routes/index.tsx:399](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:399)
- [ ] `/super-admin/feature-flags/rollout` — [src/routes/index.tsx:400](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:400)
- [ ] `/super-admin/feature-flags/create` — [src/routes/index.tsx:401](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:401)
- [ ] `/super-admin/billings-and-plans` — [src/routes/index.tsx:402](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:402)
- [ ] `/super-admin/billings-and-plans/create-plan` — [src/routes/index.tsx:403](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:403)
- [ ] `/super-admin/billings-and-plans/plan-entitlements` — [src/routes/index.tsx:404](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:404)
- [ ] `/super-admin/billings-and-plans/dunning-policy` — [src/routes/index.tsx:405](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:405)
- [ ] `/super-admin/billings-and-plans/invoices/:invoiceId` — [src/routes/index.tsx:406](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:406)
- [ ] `/super-admin/my-profile` — [src/routes/index.tsx:410](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:410)
- [ ] `/super-admin/my-profile/change-password` — [src/routes/index.tsx:411](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:411)
- [ ] `/super-admin/security` — [src/routes/index.tsx:412](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:412)
- [ ] `/super-admin/system-settings` — [src/routes/index.tsx:413](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:413)
- [ ] `/super-admin/integrations` — [src/routes/index.tsx:414](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:414)
- [ ] `/super-admin/cms` — [src/routes/index.tsx:415](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:415)
- [ ] `/super-admin/cms/landing-page` — [src/routes/index.tsx:416](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:416)
- [ ] `/super-admin/cms/learning-hub` — [src/routes/index.tsx:417](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:417)
- [ ] `/super-admin/cms/learning-hub/new` — [src/routes/index.tsx:418](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:418)
- [ ] `/super-admin/cms/learning-hub/:id` — [src/routes/index.tsx:419](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:419)
- [ ] `/super-admin/audit-logs` — [src/routes/index.tsx:420](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:420)
- [ ] `/super-admin/booking-requests` — [src/routes/index.tsx:421](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:421)
- [ ] `/super-admin/booking-requests/:id` — [src/routes/index.tsx:422](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:422)
- [ ] `/errors/404` — [src/routes/index.tsx:426](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:426)
- [ ] `/errors/401` — [src/routes/index.tsx:427](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:427)
- [ ] `/errors/403` — [src/routes/index.tsx:428](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:428)
- [ ] `/errors/500` — [src/routes/index.tsx:429](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:429)
- [ ] `/errors/503` — [src/routes/index.tsx:430](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:430)
- [ ] `/errors/session-expired` — [src/routes/index.tsx:431](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:431)
- [ ] `/errors/offline` — [src/routes/index.tsx:432](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:432)
- [ ] `/errors/maintenance` — [src/routes/index.tsx:433](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:433)
- [ ] `*` — [src/routes/index.tsx:435](/Users/apple/IdeaProjects/trappy-flow-frontend/src/routes/index.tsx:435)

## Frontend filter and query type fields

### AssessmentFiltersProps

[src/components/admin-assessment-sections/AssessmentFilters.tsx:10](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/admin-assessment-sections/AssessmentFilters.tsx:10)

- [ ] `searchQuery: string;`
- [ ] `onSearchChange: (query: string) => void;`
- [ ] `selectedFilter: string;`
- [ ] `onFilterChange: (value: string) => void;`
- [ ] `categoryOptions?: { value: string; label: string }[];`
- [ ] `value: string;`
- [ ] `label: string`
- [ ] `activeTab: string;`
- [ ] `scrollContainerRef?: RefObject<HTMLElement \| null>;`

### AdminTaskFiltersState

[src/components/admin-task-sections/AdminTaskFilters.tsx:10](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/admin-task-sections/AdminTaskFilters.tsx:10)

- [ ] `status: string \| null;`
- [ ] `priority: string \| null;`
- [ ] `assignee: string \| null;`
- [ ] `startDate: Date \| null;`
- [ ] `endDate: Date \| null;`

### AdminTaskFiltersProps

[src/components/admin-task-sections/AdminTaskFilters.tsx:18](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/admin-task-sections/AdminTaskFilters.tsx:18)

- [ ] `onApplyFilters: (filters: AdminTaskFiltersState) => void;`
- [ ] `appliedFilters?: AdminTaskFiltersState;`

### ClientsFilterSidebarProps

[src/components/admin/clients/ClientsFilterSidebar.tsx:47](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/admin/clients/ClientsFilterSidebar.tsx:47)

- [ ] `isOpen: boolean;`
- [ ] `onClose: () => void;`
- [ ] `filters: ClientFilters;`
- [ ] `onFiltersChange: (filters: ClientFilters) => void;`
- [ ] `onApply: () => void;`
- [ ] `onClearAll: () => void;`
- [ ] `therapistOptions: CustomSelectOption[];`
- [ ] `checklistTemplateOptions?: CustomSelectOption[];`
- [ ] `reportTemplateOptions?: CustomSelectOption[];`

### ClientAppointmentsFilterPanelProps

[src/components/appointment-sections/ClientAppointmentsFilterPanel.tsx:16](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/appointment-sections/ClientAppointmentsFilterPanel.tsx:16)

- [ ] `filters: ClientAppointmentFilters;`
- [ ] `setFilters: (filters: ClientAppointmentFilters) => void;`
- [ ] `serviceOptions: { value: string; label: string }[];`
- [ ] `value: string;`
- [ ] `label: string`

### FilterDropdownProps

[src/components/billing-sections/FilterDropdown.tsx:12](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/billing-sections/FilterDropdown.tsx:12)

- [ ] `className?: string;`
- [ ] `filters: BillingFilters;`
- [ ] `setFilters: (filters: BillingFilters) => void;`

### ClientsFilterSidebarProps

[src/components/clients/ClientsFilterDropdown.tsx:17](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/clients/ClientsFilterDropdown.tsx:17)

- [ ] `isOpen: boolean;`
- [ ] `onClose: () => void;`
- [ ] `filters: ClientFilters;`
- [ ] `onFiltersChange: (filters: ClientFilters) => void;`
- [ ] `onApply: () => void;`
- [ ] `onClearAll: () => void;`

### FilterDropdownProps

[src/components/clinical-forms/FilterDropdown.tsx:5](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/clinical-forms/FilterDropdown.tsx:5)

- [ ] `selectedFilters: FormStatus[];`
- [ ] `onFilterChange: (filters: FormStatus[]) => void;`

### FilterDropdownProps

[src/components/hipaa/HipaaFilterSlidebar.tsx:11](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/hipaa/HipaaFilterSlidebar.tsx:11)

- [ ] `className?: string;`
- [ ] `filters: HIPAAFilters;`
- [ ] `handleApplyFilter: (filters: HIPAAFilters) => void;`
- [ ] `handleClearFilters: (filters: HIPAAFilters) => void;`
- [ ] `actionTypes: FilterType[];`
- [ ] `riskLevels: FilterType[];`

### ClientInvoicesFilterPanelProps

[src/components/invoices/ClientInvoicesFilterPanel.tsx:16](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/invoices/ClientInvoicesFilterPanel.tsx:16)

- [ ] `filters: ClientInvoiceFilters;`
- [ ] `setFilters: (filters: ClientInvoiceFilters) => void;`

### FilterType

[src/components/notification/NotificationTab.tsx:33](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/notification/NotificationTab.tsx:33)

Resolve inherited/aliased fields from declaration in JSON.

### SchedulingFilters

[src/components/scheduling-sections/SchedulingFilterDropdown.tsx:11](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/scheduling-sections/SchedulingFilterDropdown.tsx:11)

- [ ] `startDate: Date \| null;`
- [ ] `endDate: Date \| null;`
- [ ] `status: string \| null;`
- [ ] `serviceCode: string \| null;`
- [ ] `therapist?: string \| null;`
- [ ] `includeHiddenServices?: boolean;`

### SchedulingFilterDropdownProps

[src/components/scheduling-sections/SchedulingFilterDropdown.tsx:20](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/scheduling-sections/SchedulingFilterDropdown.tsx:20)

- [ ] `className?: string;`
- [ ] `filters: SchedulingFilters;`
- [ ] `setFilters: (filters: SchedulingFilters) => void;`
- [ ] `statusOptions: { value: string; label: string }[];`
- [ ] `value: string;`
- [ ] `label: string`
- [ ] `serviceCodeOptions: { value: string; label: string }[];`
- [ ] `value: string;`
- [ ] `label: string`
- [ ] `therapistOptions?: { value: string; label: string }[];`
- [ ] `value: string;`
- [ ] `label: string`
- [ ] `onApply?: (f: SchedulingFilters) => void;`
- [ ] `onClear?: (f: SchedulingFilters) => void;`

### AppliedFiltersBarProps

[src/components/shared/AppliedFiltersBar.tsx:5](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/shared/AppliedFiltersBar.tsx:5)

- [ ] `chips: AppliedFilterChip[];`
- [ ] `onRemove: (chipId: string) => void;`
- [ ] `onClearAll: () => void;`
- [ ] `className?: string;`

### FilterDropdownProps

[src/components/shared/FilterDropdown.tsx:3](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/shared/FilterDropdown.tsx:3)

- [ ] `options: string[];`
- [ ] `defaultValue?: string;`
- [ ] `onChange?: (value: string) => void;`

### TaskTimeRangeFilterProps

[src/components/shared/TaskTimeRangeFilter.tsx:4](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/shared/TaskTimeRangeFilter.tsx:4)

- [ ] `value: TaskTimeRange \| null;`
- [ ] `onChange: (value: TaskTimeRange) => void;`

### BarChartConfigParams

[src/components/shared/charts/BarChart.tsx:7](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/shared/charts/BarChart.tsx:7)

- [ ] `categories: string[];`
- [ ] `colors?: string[];`
- [ ] `tooltipLabel?: string;`
- [ ] `columnWidth?: string;`

### DonutChartConfigParams

[src/components/shared/charts/DonutChart.tsx:7](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/shared/charts/DonutChart.tsx:7)

- [ ] `labels: string[];`
- [ ] `colors: string[];`
- [ ] `donutSize?: string;`
- [ ] `showDataLabels?: boolean;`
- [ ] `tooltipSuffix?: string;`

### TaskFiltersState

[src/components/therapist/tasks/TaskFilters.tsx:10](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/therapist/tasks/TaskFilters.tsx:10)

- [ ] `status: string \| null;`
- [ ] `priority: string \| null;`
- [ ] `assignee: string \| null;`
- [ ] `startDate: Date \| null;`
- [ ] `endDate: Date \| null;`

### TaskFiltersProps

[src/components/therapist/tasks/TaskFilters.tsx:18](/Users/apple/IdeaProjects/trappy-flow-frontend/src/components/therapist/tasks/TaskFilters.tsx:18)

- [ ] `onApplyFilters: (filters: TaskFiltersState) => void;`
- [ ] `appliedFilters?: TaskFiltersState;`

### FilterType

[src/pages/admin/compliance/compliance.static.ts:5](/Users/apple/IdeaProjects/trappy-flow-frontend/src/pages/admin/compliance/compliance.static.ts:5)

- [ ] `label: string;`
- [ ] `value: string;`

### AdminAuditRiskFilter

[src/store/api/admin/audit.api.ts:4](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/audit.api.ts:4)

Resolve inherited/aliased fields from declaration in JSON.

### AdminAuditFilters

[src/store/api/admin/audit.api.ts:6](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/audit.api.ts:6)

- [ ] `startDate?: string;`
- [ ] `endDate?: string;`
- [ ] `riskLevel?: AdminAuditRiskFilter;`
- [ ] `hipaaOnly?: boolean;`
- [ ] `action?: string;`
- [ ] `username?: string;`
- [ ] `clientId?: number;`
- [ ] `resourceType?: string;`

### AdminAuditDashboardParams

[src/store/api/admin/audit.api.ts:17](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/audit.api.ts:17)

- [ ] `period?: AdminAuditPeriod;`
- [ ] `page?: number;`
- [ ] `size?: number;`

### AdminAuditLogsParams

[src/store/api/admin/audit.api.ts:23](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/audit.api.ts:23)

- [ ] `page?: number;`
- [ ] `size?: number;`

### AdminAuditStatsParams

[src/store/api/admin/audit.api.ts:28](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/audit.api.ts:28)

Resolve inherited/aliased fields from declaration in JSON.

### AdminAuditExportParams

[src/store/api/admin/audit.api.ts:33](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/audit.api.ts:33)

- [ ] `limit?: number;`

### AdminClientAuditHistoryParams

[src/store/api/admin/audit.api.ts:37](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/audit.api.ts:37)

- [ ] `clientId: number;`
- [ ] `page?: number;`
- [ ] `size?: number;`

### ChecklistTemplatesListParams

[src/store/api/admin/checklists.api.ts:30](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/checklists.api.ts:30)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `search?: string;`
- [ ] `category?: string;`

### AdminAssessmentTemplatesListParams

[src/store/api/admin/clients.api.ts:238](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/clients.api.ts:238)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`

### AdminAssessmentAssignmentsListParams

[src/store/api/admin/clients.api.ts:276](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/clients.api.ts:276)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `search?: string;`
- [ ] `templateId?: number;`
- [ ] `clientId?: number;`
- [ ] `status?: string;`
- [ ] `from?: string;`
- [ ] `to?: string;`

### AdminFormTemplatesListParams

[src/store/api/admin/clients.api.ts:550](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/clients.api.ts:550)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `search?: string;`
- [ ] `category?: string;`

### AdminClientsListParams

[src/store/api/admin/clients.api.ts:597](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/clients.api.ts:597)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `search?: string;`
- [ ] `status?: string;`
- [ ] `stage?: string;`
- [ ] `therapistId?: number;`
- [ ] `clientType?: string;`
- [ ] `hasPortalAccess?: boolean;`
- [ ] `hasPendingTasks?: boolean;`
- [ ] `hasNoSessions?: boolean;`
- [ ] `needsFollowUp?: boolean;`
- [ ] `unassigned?: boolean;`
- [ ] `includeUnassigned?: boolean;`
- [ ] `checklistTemplateId?: number;`
- [ ] `reportTemplateId?: number;`
- [ ] `sortBy?: string;`
- [ ] `sortOrder?: "asc" \| "desc";`

### AdminConsentManagementParams

[src/store/api/admin/consents.api.ts:17](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/consents.api.ts:17)

- [ ] `consentType?: AdminConsentManagementType;`
- [ ] `status?: AdminConsentManagementStatus;`
- [ ] `search?: string;`

### AdminSessionsListParams

[src/store/api/admin/dashboard.api.ts:60](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/dashboard.api.ts:60)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `startDate?: string;`
- [ ] `endDate?: string;`
- [ ] `therapistId?: number;`
- [ ] `clientId?: number;`
- [ ] `clientSearch?: string;`
- [ ] `status?: string;`
- [ ] `sessionType?: string;`
- [ ] `serviceId?: number;`
- [ ] `serviceCode?: string;`
- [ ] `roomId?: number;`
- [ ] `mySessionsOnly?: boolean;`
- [ ] `includeHiddenServices?: boolean;`
- [ ] `view?: "summary" \| "calendar";`

### SessionOverviewStatsParams

[src/store/api/admin/dashboard.api.ts:108](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/dashboard.api.ts:108)

- [ ] `therapistId?: number;`
- [ ] `clientId?: number;`
- [ ] `startDate?: string;`
- [ ] `endDate?: string;`
- [ ] `timezone?: string;`

### AdminRolesPagedParams

[src/store/api/admin/roles.api.ts:36](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/roles.api.ts:36)

- [ ] `search?: string;`
- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `sortBy?: string;`
- [ ] `sortDirection?: "asc" \| "desc";`

### GetAvailableRoomsParams

[src/store/api/admin/rooms.api.ts:26](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/rooms.api.ts:26)

- [ ] `therapistId: number;`
- [ ] `sessionDate: string;`
- [ ] `sessionType: "online" \| "in-person";`
- [ ] `serviceId?: number;`
- [ ] `duration?: number;`
- [ ] `excludeSessionId?: number;`

### SupervisorAssignmentsListParams

[src/store/api/admin/supervisorAssignments.api.ts:27](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/supervisorAssignments.api.ts:27)

- [ ] `supervisorId?: number;`
- [ ] `therapistId?: number;`
- [ ] `active?: boolean;`
- [ ] `search?: string;`
- [ ] `requiredMeetingFrequency?: string;`

### TasksQueryParams

[src/store/api/admin/tasks.api.ts:29](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/tasks.api.ts:29)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `status?: string;`
- [ ] `priority?: string;`
- [ ] `assignedToId?: number;`
- [ ] `clientId?: number;`
- [ ] `search?: string;`
- [ ] `dateFilter?: string;`
- [ ] `fromDate?: string;`
- [ ] `toDate?: string;`
- [ ] `dateField?: "dueDate" \| "createdAt" \| "updatedAt";`
- [ ] `sortBy?: string;`
- [ ] `sortOrder?: string;`

### TaskStatsQueryParams

[src/store/api/admin/tasks.api.ts:65](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/tasks.api.ts:65)

- [ ] `assignedToId?: number;`
- [ ] `fromDate?: string;`
- [ ] `toDate?: string;`
- [ ] `dateField?: "dueDate" \| "createdAt" \| "updatedAt";`

### GetTherapistAvailabilitySlotsParams

[src/store/api/admin/therapistAvailability.api.ts:6](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/therapistAvailability.api.ts:6)

- [ ] `therapistId: number;`
- [ ] `date: string;`
- [ ] `serviceId: number;`
- [ ] `sessionType?: TherapistAvailabilitySessionType;`

### AdminUsersListParams

[src/store/api/admin/users.api.ts:23](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/admin/users.api.ts:23)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `search?: string;`
- [ ] `role?: string;`
- [ ] `active?: boolean;`

### PortalSessionHistoryParams

[src/store/api/portalApi.ts:60](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/portalApi.ts:60)

- [ ] `scope: PortalSessionHistoryScope;`

### PortalAvailableSlotsParams

[src/store/api/portalApi.ts:97](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/portalApi.ts:97)

- [ ] `startDate: string;`
- [ ] `endDate: string;`
- [ ] `sessionType: "online" \| "in-person";`
- [ ] `serviceId: number;`

### PortalInvoicesQueryParams

[src/store/api/portalApi.ts:170](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/portalApi.ts:170)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `paymentStatus?: string;`
- [ ] `insuranceCovered?: boolean;`
- [ ] `startDate?: string;`
- [ ] `endDate?: string;`
- [ ] `search?: string;`

### PortalDocumentsQueryParams

[src/store/api/portalApi.ts:216](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/portalApi.ts:216)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`

### PortalAppointmentsQueryParams

[src/store/api/portalApi.ts:231](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/portalApi.ts:231)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `status?: string;`

### PortalNotificationsQueryParams

[src/store/api/portalNotificationsApi.ts:23](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/portalNotificationsApi.ts:23)

- [ ] `page?: number;`
- [ ] `pageSize?: number;`
- [ ] `unreadOnly?: boolean;`

### SuperAdminNotificationHistoryFilters

[src/store/api/super-admin/shared.ts:218](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:218)

- [ ] `orgId?: string \| number;`
- [ ] `page?: number;`
- [ ] `size?: number;`

### AuditLogFilters

[src/store/api/super-admin/shared.ts:294](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:294)

- [ ] `page: number;`
- [ ] `size: number;`
- [ ] `authId?: number;`
- [ ] `mine?: boolean;`
- [ ] `action?: string;`
- [ ] `resourceType?: string;`
- [ ] `resourceId?: string;`
- [ ] `q?: string;`
- [ ] `createdFrom?: string;`
- [ ] `createdTo?: string;`
- [ ] `sort?: string;`
- [ ] `order?: "asc" \| "desc";`

### OrganisationAuditLogFilters

[src/store/api/super-admin/shared.ts:314](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:314)

- [ ] `id: number;`
- [ ] `page?: number;`
- [ ] `size?: number;`
- [ ] `action?: string;`
- [ ] `resourceType?: string;`
- [ ] `q?: string;`
- [ ] `createdFrom?: string;`
- [ ] `createdTo?: string;`
- [ ] `sort?: string;`
- [ ] `order?: "asc" \| "desc";`

### OrganisationListFilters

[src/store/api/super-admin/shared.ts:339](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:339)

- [ ] `search: string;`
- [ ] `status: string;`
- [ ] `plan: string;`
- [ ] `createdFrom: string;`
- [ ] `createdTo: string;`
- [ ] `region: string;`
- [ ] `dataResidency: string;`
- [ ] `page: number;`
- [ ] `pageSize: number;`
- [ ] `sort: string;`
- [ ] `order: "asc" \| "desc";`
- [ ] `exportData: boolean;`

### OrganisationUsersQueryParams

[src/store/api/super-admin/shared.ts:500](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:500)

- [ ] `id: number;`
- [ ] `search?: string;`
- [ ] `role?: string;`
- [ ] `status?: string;`
- [ ] `page?: number;`
- [ ] `pageSize?: number;`

### OrganisationInvoicesQueryParams

[src/store/api/super-admin/shared.ts:522](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:522)

- [ ] `id: number;`
- [ ] `status?: string;`
- [ ] `sort?: string;`
- [ ] `page?: number;`
- [ ] `pageSize?: number;`

### SuperAdminBillingInvoicesQueryParams

[src/store/api/super-admin/shared.ts:591](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:591)

- [ ] `orgId?: number;`
- [ ] `organisationId?: number;`
- [ ] `status?: string;`
- [ ] `sort?: string;`
- [ ] `page?: number;`
- [ ] `pageSize?: number;`

### SuperAdminBillingInvoicesExportQueryParams

[src/store/api/super-admin/shared.ts:608](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:608)

- [ ] `organisationId?: number;`
- [ ] `status?: string;`

### RolesPermissionsMatrixFilters

[src/store/api/super-admin/shared.ts:956](/Users/apple/IdeaProjects/trappy-flow-frontend/src/store/api/super-admin/shared.ts:956)

- [ ] `module?: string;`
- [ ] `permissionGroup?: string;`

### AppliedFilterChip

[src/types/appliedFilters.ts:1](/Users/apple/IdeaProjects/trappy-flow-frontend/src/types/appliedFilters.ts:1)

- [ ] `id: string;`
- [ ] `label: string;`

### BillingFilters

[src/types/billing.type.ts:7](/Users/apple/IdeaProjects/trappy-flow-frontend/src/types/billing.type.ts:7)

- [ ] `startDate: Date \| null;`
- [ ] `endDate: Date \| null;`
- [ ] `paymentStatus: string \| null;`
- [ ] `billingStatus: string \| null;`
- [ ] `clientId?: number \| null;`
- [ ] `therapistId?: number \| null;`
- [ ] `serviceCode?: string \| null;`
- [ ] `clientType?: string \| null;`
- [ ] `sessionType?: string \| null;`
- [ ] `paymentMethod?: string \| null;`
- [ ] `minAmount?: number \| null;`
- [ ] `maxAmount?: number \| null;`

### ClientFilters

[src/types/client.type.ts:67](/Users/apple/IdeaProjects/trappy-flow-frontend/src/types/client.type.ts:67)

- [ ] `clientStatus?: string[];`
- [ ] `clientStage?: string[];`
- [ ] `clientType?: string[];`
- [ ] `assignedTherapist?: string[];`
- [ ] `hasPortalAccess?: boolean;`
- [ ] `hasPendingTasks?: boolean;`
- [ ] `hasNoSessions?: boolean;`
- [ ] `needsFollowUp?: boolean;`
- [ ] `unassigned?: boolean;`
- [ ] `checklistTemplate?: string[];`
- [ ] `reportTemplate?: string[];`
- [ ] `noSessions?: boolean;`

### HIPAAFilters

[src/types/hipaa.types.ts:22](/Users/apple/IdeaProjects/trappy-flow-frontend/src/types/hipaa.types.ts:22)

- [ ] `startDate: Date \| null;`
- [ ] `endDate: Date \| null;`
- [ ] `actionType: string \| null;`
- [ ] `riskLevel: string \| null;`
- [ ] `phiAccessOnly: boolean \| null;`

### ClientFilterLabelMaps

[src/utils/appliedFilterChips.ts:40](/Users/apple/IdeaProjects/trappy-flow-frontend/src/utils/appliedFilterChips.ts:40)

- [ ] `status?: Map<string, string>;`
- [ ] `stage?: Map<string, string>;`
- [ ] `clientType?: Map<string, string>;`
- [ ] `checklistTemplate?: Map<string, string>;`
- [ ] `reportTemplate?: Map<string, string>;`

### TaskFilters

[src/utils/appliedFilterChips.ts:394](/Users/apple/IdeaProjects/trappy-flow-frontend/src/utils/appliedFilterChips.ts:394)

Resolve inherited/aliased fields from declaration in JSON.

### ClientAppointmentFilters

[src/utils/clientAppointmentFilters.ts:7](/Users/apple/IdeaProjects/trappy-flow-frontend/src/utils/clientAppointmentFilters.ts:7)

- [ ] `startDate: Date \| null;`
- [ ] `endDate: Date \| null;`
- [ ] `status: string \| null;`
- [ ] `serviceId: string \| null;`
- [ ] `sessionMode: string \| null;`

### ClientInvoiceFilters

[src/utils/clientInvoiceFilters.ts:3](/Users/apple/IdeaProjects/trappy-flow-frontend/src/utils/clientInvoiceFilters.ts:3)

- [ ] `startDate: Date \| null;`
- [ ] `endDate: Date \| null;`
- [ ] `paymentStatus: string \| null;`
- [ ] `insuranceCovered: string \| null;`

## Backend mapped operations and query inputs

- [ ] **API-0001 getDashboardSummary** — base `("/api/v1/admin")`, `@GetMapping("/dashboard/summary")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminController.java:30](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminController.java:30)
- [ ] **API-0002 testEmail** — base `("/api/v1/admin")`, `@PostMapping("/test-email")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminController.java:47](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminController.java:47)
- [ ] **API-0003 getDirectory** — base `("/api/v1/admin/directory")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminDirectoryController.java:32](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminDirectoryController.java:32)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `entityType`, `search`, `role`, `active`, `clientStatus`
- [ ] **API-0004 getIntegrationHealth** — base `("/api/v1/admin/integrations")`, `@GetMapping("/health")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:37)
  - Query inputs (one scenario family per field): `date`, `serviceId`, `sessionType`, `timezone`, `includeSlots`, `runZoomLiveTest`, `therapistIds`
- [ ] **API-0005 getTherapistZoomAvailability** — base `("/api/v1/admin/integrations")`, `@GetMapping("/therapists/zoom-availability")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:58](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:58)
  - Query inputs (one scenario family per field): `date`, `serviceId`, `sessionType`, `timezone`, `runZoomLiveTest`, `therapistIds`
- [ ] **API-0006 testStripeIntegration** — base `("/api/v1/admin/integrations")`, `@PostMapping("/stripe/test")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:78](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:78)
- [ ] **API-0007 testTherapistZoomIntegration** — base `("/api/v1/admin/integrations")`, `@PostMapping("/zoom/test/{therapistId}")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:89](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminIntegrationHealthController.java:89)
- [ ] **API-0008 searchOrganizations** — base `({"/api/admin", "/api/v1/admin"})`, `@GetMapping({"/organizations", "/organisations"})` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminOrganizationController.java:32](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminOrganizationController.java:32)
  - Query inputs (one scenario family per field): `search`, `status`, `plan`, `createdAtFrom`, `createdAtTo`, `timezone`, `region`, `dataResidency`
- [ ] **API-0009 getUsers** — base `("/api/v1/admin/users")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:42](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:42)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `search`, `role`, `active`
- [ ] **API-0010 getUser** — base `("/api/v1/admin/users")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:83](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:83)
- [ ] **API-0011 createUser** — base `("/api/v1/admin/users")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:103](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:103)
- [ ] **API-0012 updateUser** — base `("/api/v1/admin/users")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:136](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:136)
- [ ] **API-0013 deleteUser** — base `("/api/v1/admin/users")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:163](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:163)
- [ ] **API-0014 activateUser** — base `("/api/v1/admin/users")`, `@PostMapping("/{id}/activate")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:186](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:186)
- [ ] **API-0015 deactivateUser** — base `("/api/v1/admin/users")`, `@PostMapping("/{id}/deactivate")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:210](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:210)
- [ ] **API-0016 assignRole** — base `("/api/v1/admin/users")`, `@PostMapping("/{id}/assign-role")` — [src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:234](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/AdminUserController.java:234)
- [ ] **API-0017 getStatus** — base `("/api/v1/admin/stripe-connect")`, `@GetMapping("/status")` — [src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:42](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:42)
- [ ] **API-0018 startOauth** — base `("/api/v1/admin/stripe-connect")`, `@PostMapping("/oauth/start")` — [src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:49](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:49)
- [ ] **API-0019 oauthCallback** — base `("/api/v1/admin/stripe-connect")`, `@GetMapping("/oauth/callback")` — [src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:59](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:59)
  - Query inputs (one scenario family per field): `state`, `code`, `error`, `errorDescription`
- [ ] **API-0020 refresh** — base `("/api/v1/admin/stripe-connect")`, `@PostMapping("/refresh")` — [src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:82](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:82)
- [ ] **API-0021 disconnect** — base `("/api/v1/admin/stripe-connect")`, `@PostMapping("/disconnect")` — [src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:92](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:92)
- [ ] **API-0022 getTenantStripeConfig** — base `("/api/v1/admin/stripe-connect")`, `@GetMapping("/config")` — [src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:102](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:102)
- [ ] **API-0023 upsertTenantStripeConfig** — base `("/api/v1/admin/stripe-connect")`, `@PutMapping("/config")` — [src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:109](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/OrgStripeConnectController.java:109)
- [ ] **API-0024 listOrganisations** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:124](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:124)
  - Query inputs (one scenario family per field): `search`, `status`, `plan`, `createdFrom`, `createdTo`, `region`, `dataResidency`, `page`, `pageSize`, `sort`, `order`, `export`
- [ ] **API-0025 getOrganisation** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:176](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:176)
- [ ] **API-0026 createOrganisation** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:188](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:188)
- [ ] **API-0027 checkSlugAvailability** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/slugs/{slug}/available")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:212](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:212)
- [ ] **API-0028 updateOrganisation** — base `("/api/v1/super-admin")`, `@PatchMapping("/organisations/{id}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:220](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:220)
- [ ] **API-0029 getOrganisationHealth** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/health")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:242](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:242)
- [ ] **API-0030 updateTenantSettings** — base `("/api/v1/super-admin")`, `@PutMapping("/organisations/{id}/settings")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:258](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:258)
- [ ] **API-0031 getTenantSettings** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/settings")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:272](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:272)
- [ ] **API-0032 getTenantStripeConfig** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/stripe-config")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:284](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:284)
- [ ] **API-0033 upsertTenantStripeConfig** — base `("/api/v1/super-admin")`, `@PutMapping("/organisations/{id}/stripe-config")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:293](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:293)
- [ ] **API-0034 getSchemaVersion** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/schema-version")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:303](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:303)
- [ ] **API-0035 listAuditLogs** — base `("/api/v1/super-admin")`, `@GetMapping("/audit-logs")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:315](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:315)
  - Query inputs (one scenario family per field): `page`, `size`, `authId`, `mine`, `action`, `resourceType`, `resourceId`, `logLevel`, `q`, `createdFrom`, `createdTo`, `sort`, `order`
- [ ] **API-0036 getOrganisationFeatures** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/features")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:358](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:358)
- [ ] **API-0037 listHipaaAuditLogs** — base `("/api/v1/super-admin")`, `@GetMapping("/hipaa-audit-logs")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:372](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:372)
  - Query inputs (one scenario family per field): `page`, `size`, `organisationId`, `username`, `action`, `resourceType`, `logLevel`, `riskLevel`, `result`, `startDate`, `endDate`
- [ ] **API-0038 getOrganisationFeaturesList** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/features/list")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:397](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:397)
- [ ] **API-0039 getOrganisationFeatureDetails** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/features/details")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:416](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:416)
- [ ] **API-0040 setOrganisationFeatures** — base `("/api/v1/super-admin")`, `@PutMapping("/organisations/{id}/features")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:444](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:444)
- [ ] **API-0041 getOrganisationSsoSettings** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/sso/settings")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:465](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:465)
- [ ] **API-0042 setOrganisationSsoDomains** — base `("/api/v1/super-admin")`, `@PutMapping("/organisations/{id}/sso/domains")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:471](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:471)
- [ ] **API-0043 setOrganisationSsoRedirectUri** — base `("/api/v1/super-admin")`, `@PutMapping("/organisations/{id}/sso/providers/{provider}/redirect-uri")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:486](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:486)
- [ ] **API-0044 getOrganisationRollouts** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/rollouts")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:503](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:503)
- [ ] **API-0045 upsertOrganisationRollouts** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/rollouts")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:520](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:520)
- [ ] **API-0046 deleteOrganisationRollout** — base `("/api/v1/super-admin")`, `@DeleteMapping("/organisations/{id}/rollouts/{ruleId}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:552](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:552)
- [ ] **API-0047 getDunningPolicy** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/dunning-policy")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:567](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:567)
- [ ] **API-0048 upsertDunningPolicy** — base `("/api/v1/super-admin")`, `@PutMapping("/billing/dunning-policy")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:577](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:577)
- [ ] **API-0049 listBillingContacts** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/billing/contacts")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:601](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:601)
- [ ] **API-0050 upsertBillingContact** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/billing/contacts")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:618](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:618)
- [ ] **API-0051 removeBillingContact** — base `("/api/v1/super-admin")`, `@DeleteMapping("/organisations/{id}/billing/contacts/{contactId}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:646](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:646)
- [ ] **API-0052 listBillingNotificationTemplates** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/notification-templates")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:670](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:670)
- [ ] **API-0053 upsertBillingNotificationTemplate** — base `("/api/v1/super-admin")`, `@PutMapping("/billing/notification-templates/{eventKey}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:683](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:683)
- [ ] **API-0054 listBillingNotificationLogs** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/billing/notification-logs")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:706](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:706)
- [ ] **API-0055 runBillingLifecycleNow** — base `("/api/v1/super-admin")`, `@PostMapping("/billing/lifecycle/run")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:724](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:724)
- [ ] **API-0056 listOrganisationUsers** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/users")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:742](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:742)
  - Query inputs (one scenario family per field): `search`, `role`, `status`, `page`, `pageSize`
- [ ] **API-0057 listOrganisationAdmins** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/admins")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:778](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:778)
  - Query inputs (one scenario family per field): `search`, `status`, `page`, `pageSize`
- [ ] **API-0058 startOrganisationImpersonation** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{organisationKey}/impersonate")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:811](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:811)
- [ ] **API-0059 listOrganisationAuditLogs** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/audit-logs")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:846](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:846)
  - Query inputs (one scenario family per field): `page`, `size`, `action`, `resourceType`, `logLevel`, `q`, `createdFrom`, `createdTo`, `sort`, `order`
- [ ] **API-0060 getOrganisationUsage** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/usage")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:881](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:881)
  - Query inputs (one scenario family per field): `period`, `targetKey`
- [ ] **API-0061 listOrganisationInvoices** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/invoices")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:893](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:893)
  - Query inputs (one scenario family per field): `status`, `sort`, `page`, `pageSize`
- [ ] **API-0062 listInvoices** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/invoices")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:916](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:916)
  - Query inputs (one scenario family per field): `orgId`, `organisationId`, `status`, `sort`, `page`, `pageSize`
- [ ] **API-0063 getInvoiceDetail** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/invoices/{invoiceId}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:942](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:942)
- [ ] **API-0064 downloadInvoicePdf** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/invoices/{invoiceId}/pdf")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:962](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:962)
- [ ] **API-0065 sendInvoicePaymentReminder** — base `("/api/v1/super-admin")`, `@PostMapping("/billing/invoices/{invoiceId}/send-reminder")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:974](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:974)
- [ ] **API-0066 listRecentInvoices** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/invoices/recent")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1000](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1000)
  - Query inputs (one scenario family per field): `orgId`, `organisationId`, `limit`
- [ ] **API-0067 exportInvoicesCsv** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/invoices/export")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1021](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1021)
  - Query inputs (one scenario family per field): `organisationId`, `status`
- [ ] **API-0068 listInvoiceAdjustments** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/invoices/{invoiceId}/adjustments")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1053](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1053)
- [ ] **API-0069 applyInvoiceCredit** — base `("/api/v1/super-admin")`, `@PostMapping("/billing/invoices/{invoiceId}/credits")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1083](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1083)
- [ ] **API-0070 applyInvoiceRefund** — base `("/api/v1/super-admin")`, `@PostMapping("/billing/invoices/{invoiceId}/refunds")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1102](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1102)
- [ ] **API-0071 listInvoiceDisputes** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/invoices/{invoiceId}/disputes")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1121](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1121)
- [ ] **API-0072 openInvoiceDispute** — base `("/api/v1/super-admin")`, `@PostMapping("/billing/invoices/{invoiceId}/disputes")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1139](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1139)
- [ ] **API-0073 updateInvoiceDispute** — base `("/api/v1/super-admin")`, `@PutMapping("/billing/invoices/{invoiceId}/disputes/{disputeId}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1166](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1166)
- [ ] **API-0074 getRevenueReport** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/revenue-report")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1192](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1192)
  - Query inputs (one scenario family per field): `period`, `groupBy`
- [ ] **API-0075 getRevenueAnalytics** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/revenue-analytics")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1219](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1219)
  - Query inputs (one scenario family per field): `months`
- [ ] **API-0076 createBillingExportJob** — base `("/api/v1/super-admin")`, `@PostMapping("/billing/exports")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1246](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1246)
- [ ] **API-0077 getBillingExportJob** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/exports/{jobId}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1268](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1268)
- [ ] **API-0078 downloadBillingExport** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/exports/{jobId}/download")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1283](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1283)
  - Query inputs (one scenario family per field): `token`
- [ ] **API-0079 listAddonCatalog** — base `("/api/v1/super-admin")`, `@GetMapping("/billing/addon-catalog")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1316](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1316)
- [ ] **API-0080 upsertAddonPrice** — base `("/api/v1/super-admin")`, `@PutMapping("/billing/addon-catalog/{featureCode}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1339](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1339)
- [ ] **API-0081 archiveAddonCatalogEntry** — base `("/api/v1/super-admin")`, `@PutMapping("/billing/addon-catalog/{featureCode}/archive")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1368](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1368)
- [ ] **API-0082 unarchiveAddonCatalogEntry** — base `("/api/v1/super-admin")`, `@PutMapping("/billing/addon-catalog/{featureCode}/unarchive")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1392](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1392)
- [ ] **API-0083 listOrganisationAddons** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/addons")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1416](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1416)
- [ ] **API-0084 assignAddonToOrganisation** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/addons")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1442](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1442)
- [ ] **API-0085 unassignOrganisationAddon** — base `("/api/v1/super-admin")`, `@DeleteMapping("/organisations/{id}/addons/{purchaseId}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1471](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1471)
- [ ] **API-0086 unassignOrganisationAddonByFeatureCode** — base `("/api/v1/super-admin")`, `@DeleteMapping("/organisations/{id}/addons")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1499](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1499)
  - Query inputs (one scenario family per field): `featureCode`
- [ ] **API-0087 updatePlanEntitlements** — base `("/api/v1/super-admin")`, `@PutMapping("/plans/{planName}/entitlements")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1531](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1531)
- [ ] **API-0088 getPlanEntitlements** — base `("/api/v1/super-admin")`, `@GetMapping("/plans/{planName}/entitlements")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1558](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1558)
- [ ] **API-0089 listPlans** — base `("/api/v1/super-admin")`, `@GetMapping("/plans")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1568](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1568)
- [ ] **API-0090 createPlan** — base `("/api/v1/super-admin")`, `@PostMapping("/plans")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1602](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1602)
- [ ] **API-0091 getPlan** — base `("/api/v1/super-admin")`, `@GetMapping("/plans/{planCode}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1638](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1638)
- [ ] **API-0092 getPlanDetails** — base `("/api/v1/super-admin")`, `@GetMapping("/plans/{planCode}/details")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1672](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1672)
- [ ] **API-0093 updatePlan** — base `("/api/v1/super-admin")`, `@PutMapping("/plans/{planName}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1679](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1679)
- [ ] **API-0094 deletePlan** — base `("/api/v1/super-admin")`, `@DeleteMapping("/plans/{planCode}")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1717](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1717)
- [ ] **API-0095 setPlanDefault** — base `("/api/v1/super-admin")`, `@PutMapping("/plans/{planCode}/default")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1741](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1741)
- [ ] **API-0096 archivePlan** — base `("/api/v1/super-admin")`, `@PutMapping("/plans/{planCode}/archive")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1755](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1755)
- [ ] **API-0097 unarchivePlan** — base `("/api/v1/super-admin")`, `@PutMapping("/plans/{planCode}/unarchive")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1767](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1767)
- [ ] **API-0098 provisionTenant** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/provision")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1804](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1804)
  - Query inputs (one scenario family per field): `sync`
- [ ] **API-0099 reprovisionTenant** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/reprovision")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1847](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1847)
- [ ] **API-0100 seedOrganisationTenantDefaults** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/tenant-defaults/seed")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1898](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1898)
- [ ] **API-0101 backfillOrganisationClinicalTemplates** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/clinical-templates/backfill")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1926](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1926)
- [ ] **API-0102 backfillClinicalTemplatesBulk** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/clinical-templates/backfill")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1964](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1964)
  - Query inputs (one scenario family per field): `limit`
- [ ] **API-0103 bootstrapOrganisationStaffProfiles** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/staff-profiles/bootstrap")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1998](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:1998)
- [ ] **API-0104 bootstrapStaffProfilesBulk** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/staff-profiles/bootstrap")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:2033](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:2033)
  - Query inputs (one scenario family per field): `limit`
- [ ] **API-0105 lockTenantForMaintenance** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/lock")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:2065](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:2065)
- [ ] **API-0106 triggerTenantBackup** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/backup")` — [src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:2088](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/controller/SuperAdminController.java:2088)
- [ ] **API-0107 generateSessionNoteTemplate** — base `("/api/v1/ai")`, `@PostMapping("/session-notes/template")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:32](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:32)
- [ ] **API-0108 chatWithAssistant** — base `("/api/v1/ai")`, `@PostMapping("/assistant/chat")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:53](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:53)
- [ ] **API-0109 generateTemplate** — base `("/api/v1/ai")`, `@PostMapping("/generate-template")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:74](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:74)
- [ ] **API-0110 getTemplates** — base `("/api/v1/ai")`, `@GetMapping("/templates")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:99](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:99)
- [ ] **API-0111 generateFromTemplate** — base `("/api/v1/ai")`, `@PostMapping("/generate-from-template")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:106](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:106)
- [ ] **API-0112 getFieldOptions** — base `("/api/v1/ai")`, `@GetMapping("/field-options/{templateId}/{field}")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:115](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:115)
- [ ] **API-0113 getConnectedSuggestions** — base `("/api/v1/ai")`, `@PostMapping("/connected-suggestions")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:125](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:125)
- [ ] **API-0114 generateSuggestions** — base `("/api/v1/ai")`, `@PostMapping("/generate-suggestions")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:136](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:136)
- [ ] **API-0115 generateClinicalReport** — base `("/api/v1/ai")`, `@PostMapping("/generate-clinical-report")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:145](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:145)
- [ ] **API-0116 regenerateContent** — base `("/api/v1/ai")`, `@PostMapping("/regenerate-content/{sessionNoteId}")` — [src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:166](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/controller/AiController.java:166)
- [ ] **API-0117 getTemplates** — base `("/api/v1/assessments")`, `@GetMapping("/templates")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:30](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:30)
- [ ] **API-0118 getTemplate** — base `("/api/v1/assessments")`, `@GetMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:37)
- [ ] **API-0119 getTemplateSections** — base `("/api/v1/assessments")`, `@GetMapping("/templates/{id}/sections")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:44](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:44)
- [ ] **API-0120 createTemplate** — base `("/api/v1/assessments")`, `@PostMapping("/templates")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:55](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:55)
- [ ] **API-0121 getClientAssessments** — base `("/api/v1/assessments")`, `@GetMapping("/clients/{clientId}/assessments")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:117](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:117)
- [ ] **API-0122 assignAssessment** — base `("/api/v1/assessments")`, `@PostMapping("/clients/{clientId}/assessments")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:127](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:127)
- [ ] **API-0123 getAssignment** — base `("/api/v1/assessments")`, `@GetMapping("/assignments/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:139](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:139)
- [ ] **API-0124 submitResponses** — base `("/api/v1/assessments")`, `@PostMapping("/assignments/{id}/responses")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:149](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:149)
- [ ] **API-0125 deleteAssignment** — base `("/api/v1/assessments")`, `@DeleteMapping("/assignments/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:163](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:163)
- [ ] **API-0126 submitBatchResponses** — base `("/api/v1/assessments")`, `@PostMapping("/assignments/{id}/responses/batch")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:174](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:174)
- [ ] **API-0127 getReport** — base `("/api/v1/assessments")`, `@GetMapping("/assignments/{assignmentId}/report")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:191](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:191)
- [ ] **API-0128 generateReport** — base `("/api/v1/assessments")`, `@PostMapping("/assignments/{assignmentId}/generate-report")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:205](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:205)
- [ ] **API-0129 updateReportDraft** — base `("/api/v1/assessments")`, `@PutMapping("/assignments/{assignmentId}/report")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:225](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:225)
- [ ] **API-0130 finalizeReport** — base `("/api/v1/assessments")`, `@PostMapping("/assignments/{assignmentId}/report/finalize")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:251](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:251)
- [ ] **API-0131 unfinalizeReport** — base `("/api/v1/assessments")`, `@PostMapping("/assignments/{assignmentId}/report/unfinalize")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:271](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:271)
- [ ] **API-0132 transcribeAudio** — base `("/api/v1/assessments")`, `@PostMapping("/transcribe")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:291](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:291)
  - Query inputs (one scenario family per field): `audioFile`, `assignmentId`
- [ ] **API-0133 downloadAssessmentPdf** — base `("/api/v1/assessments")`, `@GetMapping("/assignments/{assignmentId}/download/pdf")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:321](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:321)
- [ ] **API-0134 downloadAssessmentDocx** — base `("/api/v1/assessments")`, `@GetMapping("/assignments/{assignmentId}/download/docx")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:351](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:351)
- [ ] **API-0135 updateTemplate** — base `("/api/v1/assessments")`, `@PutMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:383](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:383)
- [ ] **API-0136 deleteTemplate** — base `("/api/v1/assessments")`, `@DeleteMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:398](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:398)
- [ ] **API-0137 createSection** — base `("/api/v1/assessments")`, `@PostMapping("/templates/{templateId}/sections")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:414](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:414)
- [ ] **API-0138 updateSection** — base `("/api/v1/assessments")`, `@PutMapping("/sections/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:429](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:429)
- [ ] **API-0139 deleteSection** — base `("/api/v1/assessments")`, `@DeleteMapping("/sections/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:444](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:444)
- [ ] **API-0140 createQuestion** — base `("/api/v1/assessments")`, `@PostMapping("/sections/{sectionId}/questions")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:460](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:460)
- [ ] **API-0141 updateQuestion** — base `("/api/v1/assessments")`, `@PutMapping("/questions/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:475](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:475)
- [ ] **API-0142 deleteQuestion** — base `("/api/v1/assessments")`, `@DeleteMapping("/questions/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:490](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:490)
- [ ] **API-0143 recalculateScores** — base `("/api/v1/assessments")`, `@PostMapping("/assignments/{id}/recalculate-scores")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:506](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:506)
- [ ] **API-0144 updateStatus** — base `("/api/v1/assessments")`, `@PutMapping("/assignments/{id}/status")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:522](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:522)
- [ ] **API-0145 downloadAssessmentPdfFile** — base `("/api/v1/assessments")`, `@GetMapping("/assignments/{assignmentId}/download/pdf-file")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:539](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:539)
- [ ] **API-0146 bulkCreateSections** — base `("/api/v1/assessments")`, `@PostMapping("/templates/{templateId}/sections/bulk")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:569](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:569)
- [ ] **API-0147 bulkCreateQuestions** — base `("/api/v1/assessments")`, `@PostMapping("/sections/{sectionId}/questions/bulk")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:584](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:584)
- [ ] **API-0148 bulkDeleteSections** — base `("/api/v1/assessments")`, `@DeleteMapping("/sections/bulk")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:599](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:599)
- [ ] **API-0149 bulkDeleteQuestions** — base `("/api/v1/assessments")`, `@DeleteMapping("/questions/bulk")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:613](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:613)
- [ ] **API-0150 getQuestionOptions** — base `("/api/v1/assessments")`, `@GetMapping("/questions/{questionId}/options")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:629](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:629)
- [ ] **API-0151 deleteQuestionOptions** — base `("/api/v1/assessments")`, `@DeleteMapping("/questions/{questionId}/options")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:643](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:643)
- [ ] **API-0152 createQuestionOption** — base `("/api/v1/assessments")`, `@PostMapping("/question-options")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:657](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:657)
- [ ] **API-0153 bulkCreateQuestionOptions** — base `("/api/v1/assessments")`, `@PostMapping("/question-options/bulk")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:671](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:671)
- [ ] **API-0154 updateQuestionOption** — base `("/api/v1/assessments")`, `@PatchMapping("/question-options/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:685](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:685)
- [ ] **API-0155 deleteQuestionOption** — base `("/api/v1/assessments")`, `@DeleteMapping("/question-options/{id}")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:700](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:700)
- [ ] **API-0156 createTemplateVersion** — base `("/api/v1/assessments")`, `@PostMapping("/templates/{id}/versions")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:716](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:716)
  - Query inputs (one scenario family per field): `version`
- [ ] **API-0157 getTemplateVersions** — base `("/api/v1/assessments")`, `@GetMapping("/templates/{id}/versions")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:741](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:741)
- [ ] **API-0158 getAnalytics** — base `("/api/v1/assessments")`, `@GetMapping("/analytics")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:766](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:766)
  - Query inputs (one scenario family per field): `templateId`, `clientId`, `startDate`, `endDate`
- [ ] **API-0159 createReminder** — base `("/api/v1/assessments")`, `@PostMapping("/assignments/{id}/reminders")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:784](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:784)
- [ ] **API-0160 listAssignments** — base `("/api/v1/assessments")`, `@GetMapping("/assignments")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:802](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:802)
  - Query inputs (one scenario family per field): `templateId`, `clientId`, `status`, `from`, `to`, `search`, `page`, `pageSize`
- [ ] **API-0161 createAssignment** — base `("/api/v1/assessments")`, `@PostMapping("/assignments")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:823](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:823)
- [ ] **API-0162 getAssignmentResponses** — base `("/api/v1/assessments")`, `@GetMapping("/assignments/{assignmentId}/responses")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:840](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:840)
- [ ] **API-0163 submitResponsesRoot** — base `("/api/v1/assessments")`, `@PostMapping("/responses")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:854](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:854)
- [ ] **API-0164 exportAsCsv** — base `("/api/v1/assessments")`, `@GetMapping("/export/csv")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:875](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:875)
  - Query inputs (one scenario family per field): `templateId`, `clientId`, `startDate`, `endDate`
- [ ] **API-0165 exportAsExcel** — base `("/api/v1/assessments")`, `@GetMapping("/export/excel")` — [src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:901](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/controller/AssessmentController.java:901)
  - Query inputs (one scenario family per field): `templateId`, `clientId`, `startDate`, `endDate`
- [ ] **API-0166 getAuditLogs** — base `("/api/v1/audit")`, `@GetMapping("/logs")` — [src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:62](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:62)
  - Query inputs (one scenario family per field): `startDate`, `endDate`, `riskLevel`, `hipaaOnly`, `action`, `username`, `clientId`, `resourceType`, `logLevel`, `page`, `size`
- [ ] **API-0167 getAuditDashboard** — base `("/api/v1/audit")`, `@GetMapping("/dashboard")` — [src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:140](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:140)
  - Query inputs (one scenario family per field): `startDate`, `endDate`, `period`, `riskLevel`, `hipaaOnly`, `action`, `username`, `clientId`, `resourceType`, `logLevel`, `page`, `size`
- [ ] **API-0168 getAuditStatistics** — base `("/api/v1/audit")`, `@GetMapping("/stats")` — [src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:202](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:202)
  - Query inputs (one scenario family per field): `startDate`, `endDate`, `riskLevel`, `hipaaOnly`, `logLevel`
- [ ] **API-0169 exportAuditLogs** — base `("/api/v1/audit")`, `@GetMapping("/export")` — [src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:243](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:243)
  - Query inputs (one scenario family per field): `startDate`, `endDate`, `riskLevel`, `hipaaOnly`, `logLevel`, `limit`
- [ ] **API-0170 getClientAuditHistory** — base `("/api/v1/audit")`, `@GetMapping("/clients/{clientId}")` — [src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:315](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:315)
  - Query inputs (one scenario family per field): `page`, `size`
- [ ] **API-0171 getTenantGlobalAuditHealth** — base `("/api/v1/audit")`, `@GetMapping("/global-health")` — [src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:335](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/audit/controller/AuditLogController.java:335)
  - Query inputs (one scenario family per field): `hours`, `topN`
- [ ] **API-0172 listDevices** — base `("/api/v1/auth/devices")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthDeviceController.java:22](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthDeviceController.java:22)
- [ ] **API-0173 revokeDevice** — base `("/api/v1/auth/devices")`, `@DeleteMapping("/{deviceId}")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthDeviceController.java:36](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthDeviceController.java:36)
- [ ] **API-0174 revokeAllTrustedDevices** — base `("/api/v1/auth/devices")`, `@DeleteMapping` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthDeviceController.java:44](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthDeviceController.java:44)
- [ ] **API-0175 listSessions** — base `("/api/v1/auth/sessions")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthSessionController.java:22](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthSessionController.java:22)
- [ ] **API-0176 revokeOtherSessions** — base `("/api/v1/auth/sessions")`, `@DeleteMapping("/others")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthSessionController.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthSessionController.java:37)
- [ ] **API-0177 revokeSession** — base `("/api/v1/auth/sessions")`, `@DeleteMapping("/{sessionId}")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthSessionController.java:46](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthSessionController.java:46)
- [ ] **API-0178 authenticateUser** — base `("/api/v1/auth")`, `@PostMapping("/login")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:52](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:52)
- [ ] **API-0179 getLoginContext** — base `("/api/v1/auth")`, `@GetMapping("/login-context")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:135](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:135)
  - Query inputs (one scenario family per field): `email`, `username`, `identifier`, `orgSlug`
- [ ] **API-0180 resolveTenant** — base `("/api/v1/auth")`, `@PostMapping("/resolve-tenant")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:162](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:162)
- [ ] **API-0181 refreshToken** — base `("/api/v1/auth")`, `@PostMapping("/refresh")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:191](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:191)
- [ ] **API-0182 logout** — base `("/api/v1/auth")`, `@PostMapping("/logout")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:237](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:237)
- [ ] **API-0183 getCurrentUser** — base `("/api/v1/auth")`, `@GetMapping("/me")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:276](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:276)
- [ ] **API-0184 forgotPassword** — base `("/api/v1/auth")`, `@PostMapping("/forgot-password")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:336](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:336)
- [ ] **API-0185 validateResetPasswordToken** — base `("/api/v1/auth")`, `@GetMapping("/reset-password/validate")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:384](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:384)
  - Query inputs (one scenario family per field): `token`
- [ ] **API-0186 resetPassword** — base `("/api/v1/auth")`, `@PostMapping("/reset-password")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:402](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:402)
- [ ] **API-0187 changePassword** — base `("/api/v1/auth")`, `@PostMapping("/change-password")` — [src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:440](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/AuthenticationController.java:440)
- [ ] **API-0188 verifyLogin** — base `("/api/v1/auth/mfa")`, `@PostMapping("/verify-login")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:29](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:29)
- [ ] **API-0189 sendLoginCode** — base `("/api/v1/auth/mfa")`, `@PostMapping("/send-login-code")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:43](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:43)
- [ ] **API-0190 startRequiredEnrollment** — base `("/api/v1/auth/mfa")`, `@PostMapping("/required-enrollment")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:49](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:49)
- [ ] **API-0191 confirmRequiredEnrollment** — base `("/api/v1/auth/mfa")`, `@PostMapping("/required-enrollment/confirm")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:57](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:57)
- [ ] **API-0192 status** — base `("/api/v1/auth/mfa")`, `@GetMapping("/status")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:70](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:70)
- [ ] **API-0193 startEnrollment** — base `("/api/v1/auth/mfa")`, `@PostMapping("/enrollment")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:78](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:78)
- [ ] **API-0194 confirmEnrollment** — base `("/api/v1/auth/mfa")`, `@PostMapping("/enrollment/confirm")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:90](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:90)
- [ ] **API-0195 disable** — base `("/api/v1/auth/mfa")`, `@DeleteMapping` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:98](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:98)
- [ ] **API-0196 sendSettingsCode** — base `("/api/v1/auth/mfa")`, `@PostMapping("/send-settings-code")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:106](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:106)
- [ ] **API-0197 startChange** — base `("/api/v1/auth/mfa")`, `@PostMapping("/change/start")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:112](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:112)
- [ ] **API-0198 confirmChange** — base `("/api/v1/auth/mfa")`, `@PostMapping("/change/confirm")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:124](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:124)
- [ ] **API-0199 regenerateRecoveryCodes** — base `("/api/v1/auth/mfa")`, `@PostMapping("/recovery-codes")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:135](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:135)
- [ ] **API-0200 stepUp** — base `("/api/v1/auth/mfa")`, `@PostMapping("/step-up")` — [src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:143](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/MfaController.java:143)
- [ ] **API-0201 getRoles** — base `("/api/v1")`, `@GetMapping("/roles")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:49](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:49)
  - Query inputs (one scenario family per field): `search`
- [ ] **API-0202 getOrganisationRolesForTenantAdmin** — base `("/api/v1")`, `@GetMapping("/tenant-admin/roles")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:59](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:59)
  - Query inputs (one scenario family per field): `search`
- [ ] **API-0203 getRolesPaged** — base `("/api/v1")`, `@GetMapping("/roles/paged")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:71](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:71)
  - Query inputs (one scenario family per field): `search`, `page`, `pageSize`, `sortBy`, `sortDirection`
- [ ] **API-0204 getOrganisationRolesPagedForTenantAdmin** — base `("/api/v1")`, `@GetMapping("/tenant-admin/roles/paged")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:85](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:85)
  - Query inputs (one scenario family per field): `search`, `page`, `pageSize`, `sortBy`, `sortDirection`
- [ ] **API-0205 getRoleForTenantAdmin** — base `("/api/v1")`, `@GetMapping("/tenant-admin/roles/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:102](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:102)
- [ ] **API-0206 getRole** — base `("/api/v1")`, `@GetMapping("/roles/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:111](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:111)
- [ ] **API-0207 createRole** — base `("/api/v1")`, `@PostMapping("/roles")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:119](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:119)
- [ ] **API-0208 createRoleForTenantAdmin** — base `("/api/v1")`, `@PostMapping("/tenant-admin/roles")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:183](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:183)
- [ ] **API-0209 updateRole** — base `("/api/v1")`, `@PutMapping("/roles/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:195](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:195)
- [ ] **API-0210 updateRoleForTenantAdmin** — base `("/api/v1")`, `@PutMapping("/tenant-admin/roles/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:207](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:207)
- [ ] **API-0211 deleteRole** — base `("/api/v1")`, `@DeleteMapping("/roles/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:220](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:220)
- [ ] **API-0212 deleteRoleForTenantAdmin** — base `("/api/v1")`, `@DeleteMapping("/tenant-admin/roles/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:231](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:231)
- [ ] **API-0213 updateRolePermissions** — base `("/api/v1")`, `@PutMapping("/roles/{id}/permissions")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:244](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:244)
- [ ] **API-0214 updateRolePermissionsForTenantAdmin** — base `("/api/v1")`, `@PutMapping("/tenant-admin/roles/{id}/permissions")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:256](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:256)
- [ ] **API-0215 getPermissions** — base `("/api/v1")`, `@GetMapping("/permissions")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:271](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:271)
- [ ] **API-0216 getOrganisationPermissionsForTenantAdmin** — base `("/api/v1")`, `@GetMapping("/tenant-admin/permissions")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:279](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:279)
- [ ] **API-0217 getTenantPermissionPolicy** — base `("/api/v1")`, `@GetMapping("/tenant-admin/permissions/policy")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:289](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:289)
- [ ] **API-0218 getPermissionForTenantAdmin** — base `("/api/v1")`, `@GetMapping("/tenant-admin/permissions/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:298](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:298)
- [ ] **API-0219 getPermission** — base `("/api/v1")`, `@GetMapping("/permissions/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:307](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:307)
- [ ] **API-0220 createPermission** — base `("/api/v1")`, `@PostMapping("/permissions")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:315](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:315)
- [ ] **API-0221 createPermissionForTenantAdmin** — base `("/api/v1")`, `@PostMapping("/tenant-admin/permissions")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:326](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:326)
- [ ] **API-0222 updatePermission** — base `("/api/v1")`, `@PutMapping("/permissions/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:338](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:338)
- [ ] **API-0223 updatePermissionForTenantAdmin** — base `("/api/v1")`, `@PutMapping("/tenant-admin/permissions/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:350](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:350)
- [ ] **API-0224 deletePermission** — base `("/api/v1")`, `@DeleteMapping("/permissions/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:363](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:363)
- [ ] **API-0225 deletePermissionForTenantAdmin** — base `("/api/v1")`, `@DeleteMapping("/tenant-admin/permissions/{id}")` — [src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:374](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/RolePermissionController.java:374)
- [ ] **API-0226 startSso** — base `("/api/v1/auth/sso")`, `@GetMapping("/{provider}")` — [src/main/java/com/smart/therapy/flow/auth/controller/SsoController.java:41](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/SsoController.java:41)
  - Query inputs (one scenario family per field): `redirect_uri`, `orgId`, `orgSlug`, `orgIdentifier`, `orgValue`
- [ ] **API-0227 callback** — base `("/api/v1/auth/sso")`, `@GetMapping("/callback")` — [src/main/java/com/smart/therapy/flow/auth/controller/SsoController.java:64](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/SsoController.java:64)
  - Query inputs (one scenario family per field): `code`, `state`, `error`
- [ ] **API-0228 ssoConfig** — base `("/api/v1/auth/sso")`, `@GetMapping("/config")` — [src/main/java/com/smart/therapy/flow/auth/controller/SsoController.java:94](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/controller/SsoController.java:94)
  - Query inputs (one scenario family per field): `orgId`, `orgSlug`, `orgIdentifier`, `orgValue`
- [ ] **API-0229 getMySubscriptionDetails** — base `("/api/v1/billing")`, `@GetMapping("/subscription/me")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:63](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:63)
- [ ] **API-0230 getTenantSubscriptionInvoices** — base `("/api/v1/billing")`, `@GetMapping("/subscription/invoices")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:74](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:74)
  - Query inputs (one scenario family per field): `status`, `page`, `size`
- [ ] **API-0231 payTenantSubscriptionInvoice** — base `("/api/v1/billing")`, `@PostMapping("/subscription/invoices/{invoiceId}/pay")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:93](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:93)
- [ ] **API-0232 createSubscriptionCheckout** — base `("/api/v1/billing")`, `@PostMapping("/subscription/checkout")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:110](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:110)
- [ ] **API-0233 createSubscriptionPortal** — base `("/api/v1/billing")`, `@PostMapping("/subscription/portal")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:125](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:125)
- [ ] **API-0234 getServices** — base `("/api/v1/billing")`, `@GetMapping("/services")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:140](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:140)
  - Query inputs (one scenario family per field): `activeOnly`, `therapistVisible`, `clientPortalVisible`
- [ ] **API-0235 getService** — base `("/api/v1/billing")`, `@GetMapping("/services/{id}")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:169](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:169)
- [ ] **API-0236 createService** — base `("/api/v1/billing")`, `@PostMapping("/services")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:177](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:177)
- [ ] **API-0237 updateService** — base `("/api/v1/billing")`, `@PutMapping("/services/{id}")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:246](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:246)
- [ ] **API-0238 deleteService** — base `("/api/v1/billing")`, `@DeleteMapping("/services/{id}")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:293](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:293)
- [ ] **API-0239 showAllServicesForTherapists** — base `("/api/v1/billing")`, `@PostMapping("/services/visibility/therapists/show-all")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:325](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:325)
- [ ] **API-0240 hideAllServicesFromTherapists** — base `("/api/v1/billing")`, `@PostMapping("/services/visibility/therapists/hide-all")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:349](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:349)
- [ ] **API-0241 createSessionBilling** — base `("/api/v1/billing")`, `@PostMapping("/sessions/{sessionId}/billing")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:373](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:373)
- [ ] **API-0242 getSessionBilling** — base `("/api/v1/billing")`, `@GetMapping("/sessions/{sessionId}/billing")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:387](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:387)
- [ ] **API-0243 getBillingRecords** — base `("/api/v1/billing")`, `@GetMapping("/billing")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:395](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:395)
  - Query inputs (one scenario family per field): `clientId`, `clientSearch`, `therapistId`, `status`, `serviceCode`, `clientType`, `sessionType`, `paymentMethod`, `startDate`, `endDate`, `minAmount`, `maxAmount`, `page`, `size`, `sort`, `direction`
- [ ] **API-0244 updatePaymentStatus** — base `("/api/v1/billing")`, `@PatchMapping("/billing/{id}/payment-status")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:531](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:531)
  - Query inputs (one scenario family per field): `paymentStatus`, `stripePaymentIntentId`
- [ ] **API-0245 applyDiscount** — base `("/api/v1/billing")`, `@PatchMapping("/billing/{id}/discount")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:546](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:546)
- [ ] **API-0246 getBillingStatistics** — base `("/api/v1/billing")`, `@GetMapping("/statistics")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:672](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:672)
  - Query inputs (one scenario family per field): `startDate`, `endDate`
- [ ] **API-0247 getClientBillingStats** — base `("/api/v1/billing")`, `@GetMapping("/clients/{clientId}/stats")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:701](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:701)
- [ ] **API-0248 getBillingHistory** — base `("/api/v1/billing")`, `@GetMapping("/history")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:723](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:723)
  - Query inputs (one scenario family per field): `clientId`, `therapistId`, `paymentStatus`, `billingStatus`, `startDate`, `endDate`, `page`, `limit`
- [ ] **API-0249 recordPayment** — base `("/api/v1/billing")`, `@PostMapping("/billing/{id}/record-payment")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:775](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:775)
- [ ] **API-0250 recordSplitPayment** — base `("/api/v1/billing")`, `@PostMapping("/billing/{id}/record-split-payment")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:812](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:812)
- [ ] **API-0251 getPaymentGuidance** — base `("/api/v1/billing")`, `@GetMapping("/billing/{id}/payment-guidance")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:827](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:827)
- [ ] **API-0252 editPayment** — base `("/api/v1/billing")`, `@PatchMapping("/billing/{id}/payments/{paymentId}")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:836](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:836)
- [ ] **API-0253 refundPayment** — base `("/api/v1/billing")`, `@PostMapping("/billing/{id}/refund-payment")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:851](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:851)
- [ ] **API-0254 getRefundHistory** — base `("/api/v1/billing")`, `@GetMapping("/billing/{id}/refunds")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:888](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:888)
- [ ] **API-0255 getBillingTransactions** — base `("/api/v1/billing")`, `@GetMapping("/billing/{id}/transactions")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:907](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:907)
- [ ] **API-0256 voidBillingTransaction** — base `("/api/v1/billing")`, `@PostMapping("/billing/{id}/transactions/{transactionId}/void")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:927](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:927)
- [ ] **API-0257 changeBillingStatus** — base `("/api/v1/billing")`, `@PatchMapping("/billing/{id}/status")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:959](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:959)
- [ ] **API-0258 sendInvoiceEmail** — base `("/api/v1/billing")`, `@PostMapping("/billing/{id}/send-invoice-email")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:996](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:996)
- [ ] **API-0259 getInvoicePreview** — base `("/api/v1/billing")`, `@GetMapping("/billing/{id}/invoice-preview")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:1020](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:1020)
- [ ] **API-0260 downloadInvoice** — base `("/api/v1/billing")`, `@GetMapping("/billing/{id}/invoice-download")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:1044](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:1044)
- [ ] **API-0261 downloadInvoicePdf** — base `("/api/v1/billing")`, `@GetMapping("/billing/{id}/invoice")` — [src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:1076](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/BillingController.java:1076)
- [ ] **API-0262 listPolicies** — base `("/api/v1/billing/invoice-policies")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:58](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:58)
- [ ] **API-0263 getPolicy** — base `("/api/v1/billing/invoice-policies")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:76](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:76)
- [ ] **API-0264 createPolicy** — base `("/api/v1/billing/invoice-policies")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:85](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:85)
- [ ] **API-0265 updatePolicy** — base `("/api/v1/billing/invoice-policies")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:97](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:97)
- [ ] **API-0266 activatePolicy** — base `("/api/v1/billing/invoice-policies")`, `@PatchMapping("/{id}/activate")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:110](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:110)
- [ ] **API-0267 deactivatePolicy** — base `("/api/v1/billing/invoice-policies")`, `@PatchMapping("/{id}/deactivate")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:122](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:122)
- [ ] **API-0268 deletePolicy** — base `("/api/v1/billing/invoice-policies")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:134](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:134)
- [ ] **API-0269 getClientTypeOptions** — base `("/api/v1/billing/invoice-policies")`, `@GetMapping("/options/client-types")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:146](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:146)
- [ ] **API-0270 getAppointmentStatusOptions** — base `("/api/v1/billing/invoice-policies")`, `@GetMapping("/options/appointment-statuses")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:154](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:154)
- [ ] **API-0271 getServiceOptions** — base `("/api/v1/billing/invoice-policies")`, `@GetMapping("/options/services")` — [src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:162](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/controller/InvoicePolicyController.java:162)
- [ ] **API-0272 createRequest** — base `("/api/v1/public/booking-requests")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/booking/controller/PublicBookingController.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/booking/controller/PublicBookingController.java:19)
- [ ] **API-0273 getAllRequests** — base `("/api/v1/super-admin/booking-requests")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:23](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:23)
- [ ] **API-0274 getRequestById** — base `("/api/v1/super-admin/booking-requests")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:28](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:28)
- [ ] **API-0275 updateStatus** — base `("/api/v1/super-admin/booking-requests")`, `@PatchMapping("/{id}/status")` — [src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:33)
  - Query inputs (one scenario family per field): `status`, `statusMessage`
- [ ] **API-0276 deleteRequest** — base `("/api/v1/super-admin/booking-requests")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:41](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/booking/controller/SuperAdminBookingController.java:41)
- [ ] **API-0277 getClients** — base `("/api/v1/clients")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:54)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `search`, `status`, `stage`, `therapistId`, `clientType`, `hasPortalAccess`, `hasPendingTasks`, `hasNoSessions`, `needsFollowUp`, `unassigned`, `includeUnassigned`, `checklistTemplateId`, `reportTemplateId`, `sortBy`, `sortOrder`
- [ ] **API-0278 getClientStats** — base `("/api/v1/clients")`, `@GetMapping("/stats")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:202](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:202)
- [ ] **API-0279 getClient** — base `("/api/v1/clients")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:215](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:215)
- [ ] **API-0280 createClient** — base `("/api/v1/clients")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:232](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:232)
- [ ] **API-0281 updateClient** — base `("/api/v1/clients")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:364](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:364)
- [ ] **API-0282 patchClient** — base `("/api/v1/clients")`, `@PatchMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:450](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:450)
- [ ] **API-0283 deleteClient** — base `("/api/v1/clients")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:532](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:532)
- [ ] **API-0284 restoreClient** — base `("/api/v1/clients")`, `@PostMapping("/{id}/restore")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:550](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:550)
- [ ] **API-0285 updatePortalAccess** — base `("/api/v1/clients")`, `@PutMapping("/{id}/portal-access")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:611](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:611)
- [ ] **API-0286 sendPortalActivation** — base `("/api/v1/clients")`, `@PostMapping("/{id}/send-portal-activation")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:629](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:629)
- [ ] **API-0287 getClientHistory** — base `("/api/v1/clients")`, `@GetMapping("/{id}/history")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:646](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:646)
- [ ] **API-0288 getClientEmailHistory** — base `("/api/v1/clients")`, `@GetMapping("/{id}/email-history")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:662](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:662)
- [ ] **API-0289 getStageDurations** — base `("/api/v1/clients")`, `@GetMapping("/{id}/stage-durations")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:678](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:678)
- [ ] **API-0290 getClientSessions** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/sessions")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:694](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:694)
- [ ] **API-0291 getClientSessionSummary** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/sessions/summary")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:710](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:710)
- [ ] **API-0292 getClientSessionConflicts** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/session-conflicts")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:726](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:726)
- [ ] **API-0293 getClientSessionTranscriptStatuses** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/session-transcripts/status")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:742](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:742)
- [ ] **API-0294 bulkUploadClients** — base `("/api/v1/clients")`, `@PostMapping("/bulk-upload")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:757](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:757)
- [ ] **API-0295 bulkUpdateStage** — base `("/api/v1/clients")`, `@PostMapping("/bulk-update-stage")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:779](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:779)
- [ ] **API-0296 bulkReassignTherapist** — base `("/api/v1/clients")`, `@PostMapping("/bulk-reassign-therapist")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:795](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:795)
- [ ] **API-0297 bulkUpdatePortalAccess** — base `("/api/v1/clients")`, `@PostMapping("/bulk-portal-access")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:811](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:811)
- [ ] **API-0298 bulkUpdateStatus** — base `("/api/v1/clients")`, `@PostMapping("/bulk-update-status")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:827](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:827)
- [ ] **API-0299 exportClients** — base `("/api/v1/clients")`, `@GetMapping("/export")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:843](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:843)
- [ ] **API-0300 detectDuplicates** — base `("/api/v1/clients")`, `@GetMapping("/duplicates")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:863](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:863)
- [ ] **API-0301 markDuplicate** — base `("/api/v1/clients")`, `@PostMapping("/{id}/mark-duplicate")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:877](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:877)
- [ ] **API-0302 unmarkDuplicate** — base `("/api/v1/clients")`, `@PostMapping("/{id}/unmark-duplicate")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:896](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:896)
- [ ] **API-0303 getClientContacts** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/contacts")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:922](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:922)
- [ ] **API-0304 createClientContact** — base `("/api/v1/clients")`, `@PostMapping("/{clientId}/contacts")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:940](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:940)
- [ ] **API-0305 updateClientContact** — base `("/api/v1/clients")`, `@PutMapping("/{clientId}/contacts/{contactId}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:957](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:957)
- [ ] **API-0306 deleteClientContact** — base `("/api/v1/clients")`, `@DeleteMapping("/{clientId}/contacts/{contactId}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:976](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:976)
- [ ] **API-0307 getClientAddresses** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/addresses")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:996](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:996)
- [ ] **API-0308 createClientAddress** — base `("/api/v1/clients")`, `@PostMapping("/{clientId}/addresses")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1012](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1012)
- [ ] **API-0309 updateClientAddress** — base `("/api/v1/clients")`, `@PutMapping("/{clientId}/addresses/{addressId}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1038](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1038)
- [ ] **API-0310 deleteClientAddress** — base `("/api/v1/clients")`, `@DeleteMapping("/{clientId}/addresses/{addressId}")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1066](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1066)
- [ ] **API-0311 getClientInsurance** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/insurance")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1086](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1086)
- [ ] **API-0312 upsertClientInsurance** — base `("/api/v1/clients")`, `@PutMapping("/{clientId}/insurance")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1102](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1102)
- [ ] **API-0313 deleteClientInsurance** — base `("/api/v1/clients")`, `@DeleteMapping("/{clientId}/insurance")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1119](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1119)
- [ ] **API-0314 getClientReferral** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/referral")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1137](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1137)
- [ ] **API-0315 upsertClientReferral** — base `("/api/v1/clients")`, `@PutMapping("/{clientId}/referral")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1153](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1153)
- [ ] **API-0316 deleteClientReferral** — base `("/api/v1/clients")`, `@DeleteMapping("/{clientId}/referral")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1179](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1179)
- [ ] **API-0317 getClientEmployment** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/employment")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1197](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1197)
- [ ] **API-0318 upsertClientEmployment** — base `("/api/v1/clients")`, `@PutMapping("/{clientId}/employment")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1213](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1213)
- [ ] **API-0319 deleteClientEmployment** — base `("/api/v1/clients")`, `@DeleteMapping("/{clientId}/employment")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1238](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1238)
- [ ] **API-0320 getClientSmsLog** — base `("/api/v1/clients")`, `@GetMapping("/{clientId}/sms-log")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1254](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1254)
  - Query inputs (one scenario family per field): `from`, `to`, `page`, `pageSize`
- [ ] **API-0321 exportClientSmsLog** — base `("/api/v1/clients")`, `@GetMapping(value = "/{clientId}/sms-log/export", produces = "text/csv")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1284](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientController.java:1284)
  - Query inputs (one scenario family per field): `from`, `to`
- [ ] **API-0322 getClientFiltersBatch** — base `("/api/v1/client-filters")`, `@GetMapping("/batch")` — [src/main/java/com/smart/therapy/flow/client/controller/ClientFilterController.java:24](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/ClientFilterController.java:24)
- [ ] **API-0323 getConsentManagementList** — base `("/api/v1/admin/consents")`, `@GetMapping("/management")` — [src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:42](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:42)
  - Query inputs (one scenario family per field): `consentType`, `status`, `search`
- [ ] **API-0324 refreshConsentManagementList** — base `("/api/v1/admin/consents")`, `@GetMapping("/management/refresh")` — [src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:83](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:83)
  - Query inputs (one scenario family per field): `consentType`, `status`, `search`
- [ ] **API-0325 getAllConsents** — base `("/api/v1/admin/consents")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:105](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:105)
  - Query inputs (one scenario family per field): `consentType`, `granted`
- [ ] **API-0326 getClientConsents** — base `("/api/v1/admin/consents")`, `@GetMapping("/clients/{clientId}")` — [src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:121](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:121)
- [ ] **API-0327 recordStaffConsent** — base `("/api/v1/admin/consents")`, `@PostMapping("/clients/{clientId}")` — [src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:137](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:137)
- [ ] **API-0328 recordVerbalAiConsent** — base `("/api/v1/admin/consents")`, `@PostMapping("/clients/{clientId}/verbal-ai-consent")` — [src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:158](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/controller/PatientConsentController.java:158)
  - Query inputs (one scenario family per field): `granted`, `notes`
- [ ] **API-0329 getPortalLoginContext** — base `("/api/v1/portal")`, `@GetMapping("/login-context")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:78](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:78)
  - Query inputs (one scenario family per field): `email`, `identifier`, `orgSlug`
- [ ] **API-0330 login** — base `("/api/v1/portal")`, `@PostMapping("/login")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:109](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:109)
- [ ] **API-0331 verifyMfaLogin** — base `("/api/v1/portal")`, `@PostMapping("/mfa/verify-login")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:197](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:197)
- [ ] **API-0332 refreshToken** — base `("/api/v1/portal")`, `@PostMapping("/refresh")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:218](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:218)
- [ ] **API-0333 getCurrentClient** — base `("/api/v1/portal")`, `@GetMapping("/me")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:245](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:245)
- [ ] **API-0334 getMySessionsHistory** — base `("/api/v1/portal")`, `@GetMapping("/me/sessions-history")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:268](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:268)
  - Query inputs (one scenario family per field): `scope`, `timezone`
- [ ] **API-0335 getMySessionStats** — base `("/api/v1/portal")`, `@GetMapping("/me/sessions-stats")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:292](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:292)
  - Query inputs (one scenario family per field): `timezone`
- [ ] **API-0336 rateSession** — base `("/api/v1/portal")`, `@PostMapping("/me/sessions/{sessionId}/rating")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:311](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:311)
- [ ] **API-0337 uploadAvatar** — base `("/api/v1/portal")`, `@PostMapping(value = "/me/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:326](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:326)
  - Query inputs (one scenario family per field): `file`
- [ ] **API-0338 getTimezone** — base `("/api/v1/portal")`, `@GetMapping("/me/timezone")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:342](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:342)
- [ ] **API-0339 updateTimezone** — base `("/api/v1/portal")`, `@PutMapping("/me/timezone")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:361](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:361)
- [ ] **API-0340 logout** — base `("/api/v1/portal")`, `@PostMapping("/logout")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:442](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:442)
- [ ] **API-0341 validateActivationToken** — base `("/api/v1/portal")`, `@GetMapping("/activate/validate")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:489](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:489)
  - Query inputs (one scenario family per field): `token`
- [ ] **API-0342 activate** — base `("/api/v1/portal")`, `@PostMapping("/activate")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:498](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:498)
- [ ] **API-0343 forgotPassword** — base `("/api/v1/portal")`, `@PostMapping("/forgot-password")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:510](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:510)
- [ ] **API-0344 resetPassword** — base `("/api/v1/portal")`, `@PostMapping("/reset-password")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:566](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:566)
- [ ] **API-0345 getAppointments** — base `("/api/v1/portal")`, `@GetMapping("/appointments")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:622](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:622)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `status`
- [ ] **API-0346 getNotifications** — base `("/api/v1/portal")`, `@GetMapping("/notifications/legacy")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:654](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:654)
- [ ] **API-0347 getServices** — base `("/api/v1/portal")`, `@GetMapping("/services")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:670](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:670)
- [ ] **API-0348 getAvailableSlots** — base `("/api/v1/portal")`, `@GetMapping("/available-slots")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:684](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:684)
  - Query inputs (one scenario family per field): `startDate`, `endDate`, `sessionType`, `serviceId`
- [ ] **API-0349 bookAppointment** — base `("/api/v1/portal")`, `@PostMapping("/book-appointment")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:730](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:730)
- [ ] **API-0350 getAppointment** — base `("/api/v1/portal")`, `@GetMapping("/appointments/{id}")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:796](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:796)
- [ ] **API-0351 cancelAppointment** — base `("/api/v1/portal")`, `@PostMapping("/appointments/{id}/cancel")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:812](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:812)
- [ ] **API-0352 rescheduleAppointment** — base `("/api/v1/portal")`, `@PutMapping("/appointments/{id}/reschedule")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:828](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:828)
- [ ] **API-0353 getInvoiceStats** — base `("/api/v1/portal")`, `@GetMapping("/invoices/stats")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:847](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:847)
- [ ] **API-0354 getInvoices** — base `("/api/v1/portal")`, `@GetMapping("/invoices")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:859](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:859)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `paymentStatus`, `insuranceCovered`, `startDate`, `endDate`, `search`
- [ ] **API-0355 downloadInvoiceReceipt** — base `("/api/v1/portal")`, `@GetMapping("/invoices/{invoiceId}/receipt")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:894](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:894)
- [ ] **API-0356 downloadInvoiceReceiptHtml** — base `("/api/v1/portal")`, `@GetMapping("/invoices/{invoiceId}/receipt-html")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:917](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:917)
- [ ] **API-0357 payInvoice** — base `("/api/v1/portal")`, `@PostMapping("/invoices/{invoiceId}/pay")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:940](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:940)
- [ ] **API-0358 getDocuments** — base `("/api/v1/portal")`, `@GetMapping("/documents")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:958](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:958)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `documentType`, `category`, `search`
- [ ] **API-0359 uploadDocument** — base `("/api/v1/portal")`, `@PostMapping(value = "/upload-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:991](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:991)
  - Query inputs (one scenario family per field): `file`, `documentType`
- [ ] **API-0360 viewDocument** — base `("/api/v1/portal")`, `@GetMapping("/documents/{id}/view")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1010](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1010)
- [ ] **API-0361 downloadDocument** — base `("/api/v1/portal")`, `@GetMapping("/documents/{id}/download")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1035](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1035)
- [ ] **API-0362 deleteDocument** — base `("/api/v1/portal")`, `@DeleteMapping("/documents/{id}")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1060](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1060)
- [ ] **API-0363 getFormAssignments** — base `("/api/v1/portal")`, `@GetMapping("/forms/assignments")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1081](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1081)
- [ ] **API-0364 getFormAssignment** — base `("/api/v1/portal")`, `@GetMapping("/forms/assignments/{id}")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1095](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1095)
- [ ] **API-0365 getFormResponses** — base `("/api/v1/portal")`, `@GetMapping("/forms/responses/{assignmentId}")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1110](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1110)
- [ ] **API-0366 saveFormResponse** — base `("/api/v1/portal")`, `@PostMapping("/forms/responses")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1124](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1124)
- [ ] **API-0367 getFormSignature** — base `("/api/v1/portal")`, `@GetMapping("/forms/signature/{assignmentId}")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1169](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1169)
- [ ] **API-0368 getFormSignatureImage** — base `("/api/v1/portal")`, `@GetMapping("/forms/signature/{assignmentId}/image")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1183](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1183)
- [ ] **API-0369 saveFormSignature** — base `("/api/v1/portal")`, `@PostMapping("/forms/signature")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1202](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1202)
- [ ] **API-0370 submitForm** — base `("/api/v1/portal")`, `@PostMapping("/forms/submit/{assignmentId}")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1229](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1229)
- [ ] **API-0371 getConsents** — base `("/api/v1/portal")`, `@GetMapping("/consents")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1246](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1246)
- [ ] **API-0372 grantConsent** — base `("/api/v1/portal")`, `@PostMapping("/consents")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1258](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1258)
- [ ] **API-0373 withdrawConsent** — base `("/api/v1/portal")`, `@PostMapping("/consents/withdraw")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1313](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1313)
- [ ] **API-0374 toggleConsent** — base `("/api/v1/portal")`, `@PutMapping("/consents/toggle")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1364](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1364)
- [ ] **API-0375 getAssessments** — base `("/api/v1/portal")`, `@GetMapping("/assessments")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1434](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1434)
- [ ] **API-0376 getAssessment** — base `("/api/v1/portal")`, `@GetMapping("/assessments/{assignmentId}")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1449](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1449)
- [ ] **API-0377 submitAssessmentResponse** — base `("/api/v1/portal")`, `@PostMapping("/assessments/{assignmentId}/responses")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1465](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1465)
- [ ] **API-0378 getRoomsAvailability** — base `("/api/v1/portal")`, `@GetMapping("/rooms/availability")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1491](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1491)
  - Query inputs (one scenario family per field): `date`
- [ ] **API-0379 getAvailableRoomsForSlot** — base `("/api/v1/portal")`, `@GetMapping("/rooms/available-slot")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1515](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1515)
  - Query inputs (one scenario family per field): `therapistId`, `sessionDate`, `sessionType`, `serviceId`, `duration`
- [ ] **API-0380 getTherapistAvailability** — base `("/api/v1/portal")`, `@GetMapping("/therapists/{therapistId}/availability")` — [src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1537](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/controller/ClientPortalController.java:1537)
  - Query inputs (one scenario family per field): `date`, `serviceId`, `timezone`, `sessionType`
- [ ] **API-0381 getLandingPage** — base `("/api/v1/public/cms")`, `@GetMapping("/landing-page")` — [src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:28](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:28)
- [ ] **API-0382 getGlobalSettings** — base `("/api/v1/public/cms")`, `@GetMapping("/global-settings")` — [src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:34](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:34)
- [ ] **API-0383 listLearningHubs** — base `("/api/v1/public/cms")`, `@GetMapping("/learning-hubs")` — [src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:40](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:40)
  - Query inputs (one scenario family per field): `featured`
- [ ] **API-0384 getLearningHubBySlug** — base `("/api/v1/public/cms")`, `@GetMapping("/learning-hubs/slug/{slug}")` — [src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:48](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:48)
- [ ] **API-0385 getMedia** — base `("/api/v1/public/cms")`, `@GetMapping("/media/{id}")` — [src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/PublicCmsController.java:54)
- [ ] **API-0386 getLandingPage** — base `("/api/v1/super-admin/cms")`, `@GetMapping("/landing-page")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:38](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:38)
- [ ] **API-0387 saveLandingPage** — base `("/api/v1/super-admin/cms")`, `@PutMapping("/landing-page")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:45](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:45)
- [ ] **API-0388 publishLandingPage** — base `("/api/v1/super-admin/cms")`, `@PostMapping("/landing-page/publish")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:59](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:59)
- [ ] **API-0389 getGlobalSettings** — base `("/api/v1/super-admin/cms")`, `@GetMapping("/global-settings")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:72](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:72)
- [ ] **API-0390 saveGlobalSettings** — base `("/api/v1/super-admin/cms")`, `@PutMapping("/global-settings")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:79](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:79)
- [ ] **API-0391 uploadMedia** — base `("/api/v1/super-admin/cms")`, `@PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:93](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:93)
- [ ] **API-0392 listLearningHubs** — base `("/api/v1/super-admin/cms")`, `@GetMapping("/learning-hubs")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:108](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:108)
- [ ] **API-0393 getLearningHub** — base `("/api/v1/super-admin/cms")`, `@GetMapping("/learning-hubs/{id}")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:115](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:115)
- [ ] **API-0394 createLearningHub** — base `("/api/v1/super-admin/cms")`, `@PostMapping("/learning-hubs")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:122](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:122)
- [ ] **API-0395 updateLearningHub** — base `("/api/v1/super-admin/cms")`, `@PutMapping("/learning-hubs/{id}")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:137](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:137)
- [ ] **API-0396 publishLearningHub** — base `("/api/v1/super-admin/cms")`, `@PostMapping("/learning-hubs/{id}/publish")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:153](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:153)
- [ ] **API-0397 deleteLearningHub** — base `("/api/v1/super-admin/cms")`, `@DeleteMapping("/learning-hubs/{id}")` — [src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:167](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/controller/SuperAdminCmsController.java:167)
- [ ] **API-0398 customFeature** — base `("/api/v1/examples/permissions")`, `@GetMapping("/custom-feature")` — [src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:38](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:38)
- [ ] **API-0399 dynamicPermission** — base `("/api/v1/examples/permissions")`, `@GetMapping("/dynamic/{permissionName}")` — [src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:48](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:48)
- [ ] **API-0400 getClientData** — base `("/api/v1/examples/permissions")`, `@GetMapping("/client-data")` — [src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:57](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:57)
- [ ] **API-0401 sensitiveOperation** — base `("/api/v1/examples/permissions")`, `@PostMapping("/sensitive-operation")` — [src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:66](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/common/security/DynamicPermissionExamples.java:66)
- [ ] **API-0402 listPublicServices** — base `("/api/v1/public/orgs/{orgSlug}")`, `@GetMapping("/public-services")` — [src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:29](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:29)
- [ ] **API-0403 listTherapists** — base `("/api/v1/public/orgs/{orgSlug}")`, `@GetMapping("/therapists")` — [src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:35](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:35)
- [ ] **API-0404 availability** — base `("/api/v1/public/orgs/{orgSlug}")`, `@GetMapping("/therapists/{therapistId}/availability")` — [src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:41](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:41)
  - Query inputs (one scenario family per field): `date`, `serviceCode`, `sessionType`, `publicServiceId`
- [ ] **API-0405 book** — base `("/api/v1/public/orgs/{orgSlug}")`, `@PostMapping("/consultations")` — [src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/consultation/controller/PublicConsultationController.java:54)
- [ ] **API-0406 getClientDocuments** — base `("/api/v1/clients/{clientId}/documents")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:51](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:51)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `documentType`, `category`, `reviewStatus`, `shareWithClient`, `search`
- [ ] **API-0407 uploadDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:125](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:125)
  - Query inputs (one scenario family per field): `file`, `documentType`, `category`, `description`, `needsReview`, `shareWithClient`
- [ ] **API-0408 getDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:185](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:185)
- [ ] **API-0409 getDocumentFile** — base `("/api/v1/clients/{clientId}/documents")`, `@GetMapping("/{id}/file")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:224](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:224)
- [ ] **API-0410 deleteDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:254](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:254)
- [ ] **API-0411 shareDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@PatchMapping("/{id}/share")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:266](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:266)
- [ ] **API-0412 previewDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@GetMapping("/{id}/preview")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:323](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:323)
- [ ] **API-0413 reviewDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@PatchMapping("/{id}/review")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:356](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:356)
- [ ] **API-0414 viewDocumentInViewer** — base `("/api/v1/clients/{clientId}/documents")`, `@GetMapping("/{id}/viewer")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:419](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:419)
- [ ] **API-0415 viewDocxDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@GetMapping("/{id}/docx-viewer")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:457](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:457)
- [ ] **API-0416 downloadDocument** — base `("/api/v1/clients/{clientId}/documents")`, `@GetMapping("/{id}/download")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:496](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentController.java:496)
- [ ] **API-0417 getReviewQueue** — base `("/api/v1/documents")`, `@GetMapping("/reviews")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentReviewController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentReviewController.java:33)
  - Query inputs (one scenario family per field): `status`, `overdueOnly`, `overdueHours`, `clientId`, `page`, `pageSize`
- [ ] **API-0418 getReviewSummary** — base `("/api/v1/documents")`, `@GetMapping("/reviews/summary")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentReviewController.java:71](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentReviewController.java:71)
  - Query inputs (one scenario family per field): `overdueHours`, `clientId`
- [ ] **API-0419 getReviewDashboard** — base `("/api/v1/documents")`, `@GetMapping("/reviews/dashboard")` — [src/main/java/com/smart/therapy/flow/document/controller/DocumentReviewController.java:97](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/DocumentReviewController.java:97)
  - Query inputs (one scenario family per field): `status`, `overdueOnly`, `overdueHours`, `clientId`, `page`, `pageSize`
- [ ] **API-0420 getTemplates** — base `("/api/v1/forms")`, `@GetMapping("/templates")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:32](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:32)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `search`, `category`, `categoryType`
- [ ] **API-0421 getTemplate** — base `("/api/v1/forms")`, `@GetMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:63](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:63)
- [ ] **API-0422 createTemplate** — base `("/api/v1/forms")`, `@PostMapping("/templates")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:90](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:90)
- [ ] **API-0423 updateTemplate** — base `("/api/v1/forms")`, `@PatchMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:181](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:181)
- [ ] **API-0424 deleteTemplate** — base `("/api/v1/forms")`, `@DeleteMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:213](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:213)
- [ ] **API-0425 getTemplateFields** — base `("/api/v1/forms")`, `@GetMapping("/templates/{id}/fields")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:236](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:236)
- [ ] **API-0426 getTemplateVersions** — base `("/api/v1/forms")`, `@GetMapping("/templates/{templateId}/versions")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:252](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:252)
- [ ] **API-0427 getTemplateVersion** — base `("/api/v1/forms")`, `@GetMapping("/templates/{templateId}/versions/{versionNumber}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:266](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:266)
- [ ] **API-0428 activateVersion** — base `("/api/v1/forms")`, `@PostMapping("/templates/{templateId}/versions/{versionNumber}/activate")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:282](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:282)
- [ ] **API-0429 archiveVersion** — base `("/api/v1/forms")`, `@PostMapping("/templates/{templateId}/versions/{versionNumber}/archive")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:301](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:301)
- [ ] **API-0430 createSection** — base `("/api/v1/forms")`, `@PostMapping("/template-versions/{templateVersionId}/sections")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:322](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:322)
- [ ] **API-0431 updateSection** — base `("/api/v1/forms")`, `@PatchMapping("/sections/{sectionId}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:346](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:346)
- [ ] **API-0432 deleteSection** — base `("/api/v1/forms")`, `@DeleteMapping("/sections/{sectionId}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:365](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:365)
- [ ] **API-0433 createField** — base `("/api/v1/forms")`, `@PostMapping("/fields")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:383](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:383)
- [ ] **API-0434 createFieldForTemplate** — base `("/api/v1/forms")`, `@PostMapping("/templates/{id}/fields")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:413](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:413)
- [ ] **API-0435 getField** — base `("/api/v1/forms")`, `@GetMapping("/fields/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:431](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:431)
- [ ] **API-0436 updateField** — base `("/api/v1/forms")`, `@PatchMapping("/fields/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:444](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:444)
- [ ] **API-0437 deleteField** — base `("/api/v1/forms")`, `@DeleteMapping("/fields/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:476](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:476)
- [ ] **API-0438 reorderFields** — base `("/api/v1/forms")`, `@PostMapping("/fields/reorder")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:499](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:499)
- [ ] **API-0439 getClientAssignments** — base `("/api/v1/forms")`, `@GetMapping("/assignments/client/{clientId}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:516](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:516)
- [ ] **API-0440 listAssignments** — base `("/api/v1/forms")`, `@GetMapping("/assignments")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:543](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:543)
  - Query inputs (one scenario family per field): `clientId`, `templateId`
- [ ] **API-0441 getClientForms** — base `("/api/v1/forms")`, `@GetMapping("/clients/{clientId}/forms")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:561](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:561)
- [ ] **API-0442 getAssignmentsWithFilters** — base `("/api/v1/forms")`, `@PostMapping("/assignments/filter")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:575](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:575)
- [ ] **API-0443 getAssignment** — base `("/api/v1/forms")`, `@GetMapping("/assignments/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:617](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:617)
- [ ] **API-0444 createAssignment** — base `("/api/v1/forms")`, `@PostMapping("/assignments")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:646](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:646)
- [ ] **API-0445 bulkAssignForms** — base `("/api/v1/forms")`, `@PostMapping("/assignments/bulk")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:676](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:676)
- [ ] **API-0446 assignMultipleTemplatesToClient** — base `("/api/v1/forms")`, `@PostMapping("/assignments/multi-template")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:721](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:721)
- [ ] **API-0447 deleteAssignment** — base `("/api/v1/forms")`, `@DeleteMapping("/assignments/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:754](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:754)
- [ ] **API-0448 updateAssignment** — base `("/api/v1/forms")`, `@PutMapping("/assignments/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:777](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:777)
- [ ] **API-0449 updateAssignmentStatus** — base `("/api/v1/forms")`, `@PatchMapping("/assignments/{id}/status")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:795](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:795)
- [ ] **API-0450 submitResponses** — base `("/api/v1/forms")`, `@PostMapping("/assignments/{id}/responses")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:819](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:819)
- [ ] **API-0451 getAssignmentResponses** — base `("/api/v1/forms")`, `@GetMapping("/assignments/{id}/responses")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:851](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:851)
- [ ] **API-0452 updateResponse** — base `("/api/v1/forms")`, `@PutMapping("/responses/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:868](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:868)
- [ ] **API-0453 submitSignature** — base `("/api/v1/forms")`, `@PostMapping("/assignments/{id}/signature")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:888](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:888)
- [ ] **API-0454 submitSignatureAlias** — base `("/api/v1/forms")`, `@PostMapping("/assignments/{id}/signatures")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:926](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:926)
- [ ] **API-0455 getSignatures** — base `("/api/v1/forms")`, `@GetMapping("/assignments/{id}/signatures")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:942](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:942)
- [ ] **API-0456 reviewAssignment** — base `("/api/v1/forms")`, `@PatchMapping("/assignments/{id}/review")` — [src/main/java/com/smart/therapy/flow/document/controller/FormController.java:958](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/FormController.java:958)
- [ ] **API-0457 getCategories** — base `("/api/v1/library")`, `@GetMapping("/categories")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:41](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:41)
- [ ] **API-0458 getCategory** — base `("/api/v1/library")`, `@GetMapping("/categories/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:58](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:58)
- [ ] **API-0459 createCategory** — base `("/api/v1/library")`, `@PostMapping("/categories")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:80](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:80)
- [ ] **API-0460 updateCategory** — base `("/api/v1/library")`, `@PutMapping("/categories/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:104](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:104)
- [ ] **API-0461 deleteCategory** — base `("/api/v1/library")`, `@DeleteMapping("/categories/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:133](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:133)
- [ ] **API-0462 getEntries** — base `("/api/v1/library")`, `@GetMapping("/entries")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:160](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:160)
  - Query inputs (one scenario family per field): `categoryId`
- [ ] **API-0463 getEntriesWithConnections** — base `("/api/v1/library")`, `@GetMapping("/entries/with-connections")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:181](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:181)
  - Query inputs (one scenario family per field): `categoryId`
- [ ] **API-0464 getEntry** — base `("/api/v1/library")`, `@GetMapping("/entries/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:206](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:206)
- [ ] **API-0465 createEntry** — base `("/api/v1/library")`, `@PostMapping("/entries")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:228](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:228)
- [ ] **API-0466 bulkCreateEntries** — base `("/api/v1/library")`, `@PostMapping({ "/entries/bulk", "/bulk-entries" })` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:252](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:252)
- [ ] **API-0467 getSessionNoteFieldEntries** — base `("/api/v1/library")`, `@GetMapping("/entries/session-note/{field}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:282](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:282)
- [ ] **API-0468 getConnections** — base `("/api/v1/library")`, `@GetMapping("/connections")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:316](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:316)
  - Query inputs (one scenario family per field): `entryId`
- [ ] **API-0469 getConnectedEntries** — base `("/api/v1/library")`, `@GetMapping("/entries/{id}/connected")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:337](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:337)
- [ ] **API-0470 getConnectedEntriesBulk** — base `("/api/v1/library")`, `@PostMapping("/entries/connected-bulk")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:359](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:359)
- [ ] **API-0471 createConnection** — base `("/api/v1/library")`, `@PostMapping("/connections")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:385](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:385)
- [ ] **API-0472 createConnectionsBatch** — base `("/api/v1/library")`, `@PostMapping("/connections/batch")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:413](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:413)
- [ ] **API-0473 updateConnection** — base `("/api/v1/library")`, `@PutMapping("/connections/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:443](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:443)
- [ ] **API-0474 deleteConnection** — base `("/api/v1/library")`, `@DeleteMapping("/connections/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:471](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:471)
- [ ] **API-0475 deleteConnectionsForEntry** — base `("/api/v1/library")`, `@DeleteMapping("/entries/{entryId}/connections")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:494](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:494)
- [ ] **API-0476 updateEntry** — base `("/api/v1/library")`, `@PutMapping("/entries/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:519](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:519)
- [ ] **API-0477 deleteEntry** — base `("/api/v1/library")`, `@DeleteMapping("/entries/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:548](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:548)
- [ ] **API-0478 bulkDeleteEntries** — base `("/api/v1/library")`, `@DeleteMapping("/entries/bulk")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:573](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:573)
- [ ] **API-0479 searchEntries** — base `("/api/v1/library")`, `@GetMapping("/search")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:602](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:602)
  - Query inputs (one scenario family per field): `query`, `categoryId`
- [ ] **API-0480 incrementUsage** — base `("/api/v1/library")`, `@PostMapping("/entries/{id}/increment-usage")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:626](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:626)
- [ ] **API-0481 getAllTags** — base `("/api/v1/library")`, `@GetMapping("/tags")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:655](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:655)
- [ ] **API-0482 getTag** — base `("/api/v1/library")`, `@GetMapping("/tags/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:678](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:678)
- [ ] **API-0483 createTag** — base `("/api/v1/library")`, `@PostMapping("/tags")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:700](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:700)
- [ ] **API-0484 deleteTag** — base `("/api/v1/library")`, `@DeleteMapping("/tags/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:726](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:726)
- [ ] **API-0485 bulkTagOperation** — base `("/api/v1/library")`, `@PostMapping("/entries/tags/bulk")` — [src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:756](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/LibraryController.java:756)
- [ ] **API-0486 getClientNotes** — base `("/api/v1/notes")`, `@GetMapping("/clients/{clientId}/notes")` — [src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:28](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:28)
  - Query inputs (one scenario family per field): `noteType`, `startDate`, `endDate`
- [ ] **API-0487 getNote** — base `("/api/v1/notes")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:41](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:41)
- [ ] **API-0488 createNote** — base `("/api/v1/notes")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:51](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:51)
- [ ] **API-0489 updateNote** — base `("/api/v1/notes")`, `@PatchMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:115](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:115)
- [ ] **API-0490 deleteNote** — base `("/api/v1/notes")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:127](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/controller/NoteController.java:127)
- [ ] **API-0491 getNotifications** — base `("/api/v1/portal/notifications")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:43](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:43)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `unreadOnly`
- [ ] **API-0492 getUnreadCount** — base `("/api/v1/portal/notifications")`, `@GetMapping("/unread/count")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:91](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:91)
- [ ] **API-0493 getUnreadCountAlt** — base `("/api/v1/portal/notifications")`, `@GetMapping("/unread-count")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:115](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:115)
- [ ] **API-0494 markAsRead** — base `("/api/v1/portal/notifications")`, `@PatchMapping("/{id}/read")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:139](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:139)
- [ ] **API-0495 markAllAsRead** — base `("/api/v1/portal/notifications")`, `@PatchMapping("/read-all")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:174](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:174)
- [ ] **API-0496 markAllAsReadAlt** — base `("/api/v1/portal/notifications")`, `@PutMapping("/mark-all-read")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:200](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:200)
- [ ] **API-0497 deleteNotification** — base `("/api/v1/portal/notifications")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:224](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:224)
- [ ] **API-0498 getUserPreferences** — base `("/api/v1/portal/notifications")`, `@GetMapping("/preferences")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:260](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:260)
- [ ] **API-0499 setUserPreference** — base `("/api/v1/portal/notifications")`, `@PutMapping("/preferences/{notificationType}")` — [src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:294](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/ClientPortalNotificationController.java:294)
- [ ] **API-0500 getNotifications** — base `("/api/v1/notifications")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:37)
  - Query inputs (one scenario family per field): `unreadOnly`
- [ ] **API-0501 getTenantNotifications** — base `("/api/v1/notifications")`, `@GetMapping("/all")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:56](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:56)
  - Query inputs (one scenario family per field): `userId`, `unreadOnly`
- [ ] **API-0502 getUnreadCount** — base `("/api/v1/notifications")`, `@GetMapping("/unread/count")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:69](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:69)
- [ ] **API-0503 getUnreadCountAlt** — base `("/api/v1/notifications")`, `@GetMapping("/unread-count")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:76](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:76)
- [ ] **API-0504 markAsRead** — base `("/api/v1/notifications")`, `@PatchMapping("/{id}/read")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:83](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:83)
- [ ] **API-0505 markAllAsRead** — base `("/api/v1/notifications")`, `@PatchMapping("/read-all")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:92](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:92)
- [ ] **API-0506 markAllAsReadForUser** — base `("/api/v1/notifications")`, `@PatchMapping("/users/{userId}/read-all")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:99](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:99)
- [ ] **API-0507 markAsReadForUser** — base `("/api/v1/notifications")`, `@PatchMapping("/users/{userId}/{id}/read")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:109](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:109)
- [ ] **API-0508 markAllAsReadAlt** — base `("/api/v1/notifications")`, `@PutMapping("/mark-all-read")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:119](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:119)
- [ ] **API-0509 deleteNotification** — base `("/api/v1/notifications")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:126](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:126)
- [ ] **API-0510 createNotification** — base `("/api/v1/notifications")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:135](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:135)
- [ ] **API-0511 createBroadcastNotification** — base `("/api/v1/notifications")`, `@PostMapping("/broadcast")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:153](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:153)
- [ ] **API-0512 getUserPreferences** — base `("/api/v1/notifications")`, `@GetMapping("/preferences")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:171](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:171)
- [ ] **API-0513 setUserPreference** — base `("/api/v1/notifications")`, `@PutMapping("/preferences/{triggerType}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:179](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:179)
- [ ] **API-0514 getStats** — base `("/api/v1/notifications")`, `@GetMapping("/stats")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:202](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:202)
- [ ] **API-0515 getSetupHealth** — base `("/api/v1/notifications")`, `@GetMapping("/setup-health")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:208](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:208)
- [ ] **API-0516 getCoverageHealth** — base `("/api/v1/notifications")`, `@GetMapping("/setup/coverage")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:217](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:217)
- [ ] **API-0517 getEventCatalog** — base `("/api/v1/notifications")`, `@GetMapping("/setup/events")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:226](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:226)
- [ ] **API-0518 getActionMetadata** — base `("/api/v1/notifications")`, `@GetMapping("/setup/action-metadata")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:235](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:235)
- [ ] **API-0519 upsertActionMetadata** — base `("/api/v1/notifications")`, `@PutMapping("/setup/action-metadata/{relatedEntityType}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:244](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:244)
- [ ] **API-0520 deleteActionMetadata** — base `("/api/v1/notifications")`, `@DeleteMapping("/setup/action-metadata/{relatedEntityType}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:255](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:255)
- [ ] **API-0521 seedActionMetadata** — base `("/api/v1/notifications")`, `@PostMapping("/setup/action-metadata/seed")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:265](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:265)
  - Query inputs (one scenario family per field): `overwrite`
- [ ] **API-0522 syncNotificationDefaults** — base `("/api/v1/notifications")`, `@PostMapping("/setup/sync")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:275](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:275)
- [ ] **API-0523 cleanupExpiredNotifications** — base `("/api/v1/notifications")`, `@PostMapping("/cleanup")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:285](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:285)
- [ ] **API-0524 getNotificationTriggers** — base `("/api/v1/notifications")`, `@GetMapping("/triggers")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:292](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:292)
- [ ] **API-0525 createNotificationTrigger** — base `("/api/v1/notifications")`, `@PostMapping("/triggers")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:298](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:298)
- [ ] **API-0526 updateNotificationTrigger** — base `("/api/v1/notifications")`, `@PutMapping("/triggers/{id}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:305](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:305)
- [ ] **API-0527 deleteNotificationTrigger** — base `("/api/v1/notifications")`, `@DeleteMapping("/triggers/{id}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:313](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:313)
- [ ] **API-0528 getNotificationTemplates** — base `("/api/v1/notifications")`, `@GetMapping("/templates")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:320](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:320)
  - Query inputs (one scenario family per field): `type`
- [ ] **API-0529 createNotificationTemplate** — base `("/api/v1/notifications")`, `@PostMapping("/templates")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:334](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:334)
- [ ] **API-0530 updateNotificationTemplate** — base `("/api/v1/notifications")`, `@PutMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:341](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:341)
- [ ] **API-0531 deleteNotificationTemplate** — base `("/api/v1/notifications")`, `@DeleteMapping("/templates/{id}")` — [src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:349](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/NotificationController.java:349)
- [ ] **API-0532 handleInbound** — base `("/api/sms")`, `@PostMapping(value = "/inbound", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)` — [src/main/java/com/smart/therapy/flow/notification/controller/TwilioInboundSmsController.java:27](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/controller/TwilioInboundSmsController.java:27)
  - Query inputs (one scenario family per field): `params`
- [ ] **API-0533 initiatePayment** — base `("/api/v1/stripe")`, `@PostMapping("/invoices/{invoiceId}/pay")` — [src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:40](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:40)
- [ ] **API-0534 handlePlatformWebhook** — base `("/api/v1/stripe")`, `@PostMapping("/webhook/platform")` — [src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:50](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:50)
- [ ] **API-0535 handleConnectWebhook** — base `("/api/v1/stripe")`, `@PostMapping("/webhook/connect")` — [src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:59](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:59)
- [ ] **API-0536 handleTenantWebhook** — base `("/api/v1/stripe")`, `@PostMapping("/webhook/tenant/{tenantKey}")` — [src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:68](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/payment/controller/StripeController.java:68)
- [ ] **API-0537 list** — base `("/api/v1/public-site/services")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:27](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:27)
- [ ] **API-0538 create** — base `("/api/v1/public-site/services")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:34](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:34)
- [ ] **API-0539 update** — base `("/api/v1/public-site/services")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:43](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:43)
- [ ] **API-0540 delete** — base `("/api/v1/public-site/services")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:52](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/publicsite/controller/PublicSiteServiceController.java:52)
- [ ] **API-0541 listReports** — base `(no class mapping)`, `@GetMapping("/api/v1/clients/{clientId}/reports")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:35](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:35)
- [ ] **API-0542 generateReport** — base `(no class mapping)`, `@PostMapping("/api/v1/clients/{clientId}/reports/generate")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:43](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:43)
- [ ] **API-0543 listSupportingFiles** — base `(no class mapping)`, `@GetMapping("/api/v1/clients/{clientId}/supporting-files")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:57](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:57)
- [ ] **API-0544 uploadSupportingFile** — base `(no class mapping)`, `@PostMapping("/api/v1/clients/{clientId}/supporting-files")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:65](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:65)
- [ ] **API-0545 getReport** — base `(no class mapping)`, `@GetMapping("/api/v1/reports/{reportId}")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:79](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:79)
- [ ] **API-0546 updateDraft** — base `(no class mapping)`, `@PutMapping("/api/v1/reports/{reportId}")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:87](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:87)
- [ ] **API-0547 finalizeReport** — base `(no class mapping)`, `@PostMapping("/api/v1/reports/{reportId}/finalize")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:100](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:100)
- [ ] **API-0548 unfinalizeReport** — base `(no class mapping)`, `@PostMapping("/api/v1/reports/{reportId}/unfinalize")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:112](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:112)
- [ ] **API-0549 deleteReport** — base `(no class mapping)`, `@DeleteMapping("/api/v1/reports/{reportId}")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:124](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:124)
- [ ] **API-0550 downloadPdf** — base `(no class mapping)`, `@GetMapping("/api/v1/reports/{reportId}/download/pdf")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:136](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:136)
- [ ] **API-0551 downloadDocx** — base `(no class mapping)`, `@GetMapping("/api/v1/reports/{reportId}/download/docx")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:156](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:156)
- [ ] **API-0552 downloadSupportingFile** — base `(no class mapping)`, `@GetMapping("/api/v1/supporting-files/{fileId}/download")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:174](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:174)
- [ ] **API-0553 deleteSupportingFile** — base `(no class mapping)`, `@DeleteMapping("/api/v1/supporting-files/{fileId}")` — [src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:187](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ClientReportController.java:187)
- [ ] **API-0554 listTemplates** — base `("/api/v1/report-templates")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:33)
  - Query inputs (one scenario family per field): `includeInactive`
- [ ] **API-0555 getTemplate** — base `("/api/v1/report-templates")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:49](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:49)
- [ ] **API-0556 createTemplate** — base `("/api/v1/report-templates")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:55](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:55)
- [ ] **API-0557 updateTemplate** — base `("/api/v1/report-templates")`, `@PatchMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:68](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:68)
- [ ] **API-0558 deleteTemplate** — base `("/api/v1/report-templates")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:81](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/controller/ReportTemplateController.java:81)
- [ ] **API-0559 getRooms** — base `("/api/v1/rooms")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:33)
  - Query inputs (one scenario family per field): `activeOnly`
- [ ] **API-0560 getRoom** — base `("/api/v1/rooms")`, `@GetMapping("/{roomId}")` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:51](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:51)
- [ ] **API-0561 createRoom** — base `("/api/v1/rooms")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:65](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:65)
- [ ] **API-0562 updateRoom** — base `("/api/v1/rooms")`, `@PutMapping("/{roomId}")` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:88](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:88)
- [ ] **API-0563 deleteRoom** — base `("/api/v1/rooms")`, `@DeleteMapping("/{roomId}")` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:107](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:107)
- [ ] **API-0564 checkRoomAvailability** — base `("/api/v1/rooms")`, `@GetMapping("/{roomId}/availability")` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:123](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:123)
  - Query inputs (one scenario family per field): `startTime`, `endTime`
- [ ] **API-0565 checkAvailability** — base `("/api/v1/rooms")`, `@GetMapping("/check-availability")` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:144](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:144)
  - Query inputs (one scenario family per field): `roomId`, `therapistId`, `clientId`, `startTime`, `endTime`
- [ ] **API-0566 getAvailableRoomsForSlot** — base `("/api/v1/rooms")`, `@GetMapping("/available")` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:173](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:173)
  - Query inputs (one scenario family per field): `therapistId`, `sessionDate`, `sessionType`, `serviceId`, `duration`, `excludeSessionId`
- [ ] **API-0567 getRoomSlotStatus** — base `("/api/v1/rooms")`, `@GetMapping("/{roomId}/slot-status")` — [src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:201](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/RoomController.java:201)
  - Query inputs (one scenario family per field): `sessionDate`, `sessionType`, `serviceId`, `duration`
- [ ] **API-0568 getSessions** — base `("/api/v1/sessions")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:54)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `startDate`, `endDate`, `therapistId`, `clientId`, `clientSearch`, `status`, `sessionType`, `serviceId`, `serviceCode`, `roomId`, `mySessionsOnly`, `includeHiddenServices`, `view`
- [ ] **API-0569 getSession** — base `("/api/v1/sessions")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:127](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:127)
- [ ] **API-0570 createSession** — base `("/api/v1/sessions")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:137](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:137)
- [ ] **API-0571 previewRecurringSessions** — base `("/api/v1/sessions")`, `@PostMapping("/recurring/preview")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:207](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:207)
- [ ] **API-0572 createRecurringSessions** — base `("/api/v1/sessions")`, `@PostMapping("/recurring")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:220](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:220)
- [ ] **API-0573 updateFutureRecurringSessions** — base `("/api/v1/sessions")`, `@PutMapping("/recurring/{groupId}/future")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:237](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:237)
- [ ] **API-0574 cancelRecurringSeries** — base `("/api/v1/sessions")`, `@DeleteMapping("/recurring/{groupId}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:256](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:256)
- [ ] **API-0575 updateSession** — base `("/api/v1/sessions")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:273](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:273)
- [ ] **API-0576 deleteSession** — base `("/api/v1/sessions")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:325](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:325)
- [ ] **API-0577 checkConflicts** — base `("/api/v1/sessions")`, `@GetMapping("/conflicts/check")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:346](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:346)
  - Query inputs (one scenario family per field): `therapistId`, `roomId`, `sessionDate`, `duration`
- [ ] **API-0578 getAvailability** — base `("/api/v1/sessions")`, `@GetMapping("/availability")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:358](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:358)
  - Query inputs (one scenario family per field): `date`, `therapistId`, `roomId`
- [ ] **API-0579 getSessionOverviewStats** — base `("/api/v1/sessions")`, `@GetMapping("/stats/overview")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:368](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:368)
  - Query inputs (one scenario family per field): `therapistId`, `clientId`, `startDate`, `endDate`, `timezone`
- [ ] **API-0580 getRoleScopedDashboard** — base `("/api/v1/sessions")`, `@GetMapping("/dashboard/role-view")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:403](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:403)
  - Query inputs (one scenario family per field): `timezone`, `recentLimit`, `allLimit`
- [ ] **API-0581 getAllSessionHistory** — base `("/api/v1/sessions")`, `@GetMapping("/history/all")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:427](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:427)
- [ ] **API-0582 getClientSessionHistory** — base `("/api/v1/sessions")`, `@GetMapping("/history/clients/{clientId}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:436](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:436)
- [ ] **API-0583 getSessionsByDay** — base `("/api/v1/sessions")`, `@GetMapping("/history/{year}/{month}/{day}/day")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:448](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:448)
- [ ] **API-0584 getSessionsByWeek** — base `("/api/v1/sessions")`, `@GetMapping("/history/{year}/{week}/week")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:460](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:460)
- [ ] **API-0585 getSessionsByMonth** — base `("/api/v1/sessions")`, `@GetMapping("/history/{year}/{month}/month")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:471](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:471)
- [ ] **API-0586 getRecentSessions** — base `("/api/v1/sessions")`, `@GetMapping("/recent")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:482](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:482)
  - Query inputs (one scenario family per field): `limit`
- [ ] **API-0587 getUpcomingSessions** — base `("/api/v1/sessions")`, `@GetMapping("/upcoming")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:491](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:491)
  - Query inputs (one scenario family per field): `limit`, `startDate`, `endDate`
- [ ] **API-0588 getPreviousSessions** — base `("/api/v1/sessions")`, `@GetMapping("/previous")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:505](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:505)
  - Query inputs (one scenario family per field): `limit`, `startDate`, `endDate`
- [ ] **API-0589 getOverdueSessions** — base `("/api/v1/sessions")`, `@GetMapping("/overdue")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:519](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:519)
  - Query inputs (one scenario family per field): `limit`, `startDate`, `endDate`
- [ ] **API-0590 checkOverdueSessions** — base `("/api/v1/sessions")`, `@PostMapping("/check-overdue")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:533](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:533)
- [ ] **API-0591 bulkUploadSessions** — base `("/api/v1/sessions")`, `@PostMapping("/bulk-upload")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:544](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:544)
- [ ] **API-0592 downloadBulkUploadTemplate** — base `("/api/v1/sessions")`, `@GetMapping(value = "/bulk-upload/template", produces = "text/csv")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:555](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:555)
- [ ] **API-0593 downloadStaticBulkUploadTemplate** — base `("/api/v1/sessions")`, `@GetMapping(value = "/bulk-upload/template/static", produces = "text/csv")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:569](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:569)
- [ ] **API-0594 downloadBulkUploadTemplateXlsx** — base `("/api/v1/sessions")`, `@GetMapping(value = "/bulk-upload/template.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:584](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:584)
- [ ] **API-0595 downloadStaticBulkUploadTemplateXlsx** — base `("/api/v1/sessions")`, `@GetMapping(value = "/bulk-upload/template.xlsx/static", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:599](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:599)
- [ ] **API-0596 bulkUploadSessionsFromCsv** — base `("/api/v1/sessions")`, `@PostMapping(value = "/bulk-upload/import", consumes = "text/csv")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:615](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:615)
- [ ] **API-0597 bulkUploadSessionsFromCsvFile** — base `("/api/v1/sessions")`, `@PostMapping(value = "/bulk-upload/import-file", consumes = "multipart/form-data")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:630](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:630)
  - Query inputs (one scenario family per field): `file`
- [ ] **API-0598 updateSessionStatus** — base `("/api/v1/sessions")`, `@PutMapping("/{id}/status")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:645](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:645)
- [ ] **API-0599 getZoomMeetingDetails** — base `("/api/v1/sessions")`, `@GetMapping("/{id}/zoom/start")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:676](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:676)
- [ ] **API-0600 getSessionBilling** — base `("/api/v1/sessions")`, `@GetMapping("/{id}/billing")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:710](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:710)
- [ ] **API-0601 createSessionBilling** — base `("/api/v1/sessions")`, `@PostMapping("/{id}/billing")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:718](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:718)
- [ ] **API-0602 startTranscriptUpload** — base `("/api/v1/sessions")`, `@PostMapping("/{sessionId}/transcribe-start")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:731](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:731)
- [ ] **API-0603 transcribeTranscriptChunk** — base `("/api/v1/sessions")`, `@PostMapping(value = "/{sessionId}/transcribe-chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:758](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:758)
  - Query inputs (one scenario family per field): `uploadId`, `chunkIndex`, `chunkDurationSeconds`, `audioFile`, `language`
- [ ] **API-0604 finalizeTranscript** — base `("/api/v1/sessions")`, `@PostMapping("/{sessionId}/transcribe-finalize")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:804](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:804)
- [ ] **API-0605 getTranscript** — base `("/api/v1/sessions")`, `@GetMapping("/{sessionId}/transcript")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:824](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:824)
- [ ] **API-0606 downloadTranscript** — base `("/api/v1/sessions")`, `@GetMapping("/{sessionId}/transcript/download")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:833](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:833)
- [ ] **API-0607 deleteTranscript** — base `("/api/v1/sessions")`, `@DeleteMapping("/{sessionId}/transcript")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:847](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:847)
- [ ] **API-0608 smartFillTranscript** — base `("/api/v1/sessions")`, `@PostMapping("/{sessionId}/transcript/smart-fill")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:862](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:862)
- [ ] **API-0609 diarizeTranscript** — base `("/api/v1/sessions")`, `@PostMapping("/{sessionId}/transcript/diarize")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:871](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionController.java:871)
- [ ] **API-0610 listTemplates** — base `("/api/v1/session-note-ai-templates")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:41](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:41)
- [ ] **API-0611 getTemplate** — base `("/api/v1/session-note-ai-templates")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:50](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:50)
- [ ] **API-0612 createTemplate** — base `("/api/v1/session-note-ai-templates")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:58](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:58)
- [ ] **API-0613 updateTemplate** — base `("/api/v1/session-note-ai-templates")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:69](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:69)
- [ ] **API-0614 deleteTemplate** — base `("/api/v1/session-note-ai-templates")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:78](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:78)
- [ ] **API-0615 markLastUsed** — base `("/api/v1/session-note-ai-templates")`, `@PostMapping("/{id}/mark-last-used")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:87](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteAiTemplateController.java:87)
- [ ] **API-0616 getSessionNotes** — base `("/api/v1/session-notes")`, `@GetMapping("/sessions/{sessionId}/notes")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:34](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:34)
- [ ] **API-0617 getClientSessionNotes** — base `("/api/v1/session-notes")`, `@GetMapping("/clients/{clientId}/session-notes")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:44](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:44)
- [ ] **API-0618 getSessionNote** — base `("/api/v1/session-notes")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:54)
- [ ] **API-0619 createSessionNote** — base `("/api/v1/session-notes")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:64](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:64)
- [ ] **API-0620 updateSessionNote** — base `("/api/v1/session-notes")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:138](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:138)
- [ ] **API-0621 deleteSessionNote** — base `("/api/v1/session-notes")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:151](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:151)
- [ ] **API-0622 finalizeSessionNote** — base `("/api/v1/session-notes")`, `@PostMapping("/{id}/finalize")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:162](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:162)
- [ ] **API-0623 createAmendment** — base `("/api/v1/session-notes")`, `@PostMapping("/{id}/amendments")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:174](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:174)
- [ ] **API-0624 getAmendments** — base `("/api/v1/session-notes")`, `@GetMapping("/{id}/amendments")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:187](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:187)
- [ ] **API-0625 transcribeAudio** — base `("/api/v1/session-notes")`, `@PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:197](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:197)
  - Query inputs (one scenario family per field): `sessionNoteId`, `audioFile`
- [ ] **API-0626 reprocessAudio** — base `("/api/v1/session-notes")`, `@PostMapping("/{id}/reprocess-audio")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:235](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:235)
- [ ] **API-0627 getSessionNotePdf** — base `("/api/v1/session-notes")`, `@GetMapping("/{id}/pdf")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:257](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionNoteController.java:257)
- [ ] **API-0628 getTranscriptStatuses** — base `("/api/v1/session-transcripts")`, `@GetMapping("/status")` — [src/main/java/com/smart/therapy/flow/session/controller/SessionTranscriptController.java:28](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/controller/SessionTranscriptController.java:28)
- [ ] **API-0629 listAddons** — base `({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:39](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:39)
- [ ] **API-0630 getAddon** — base `({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})`, `@GetMapping("/{code}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:54)
- [ ] **API-0631 createAddon** — base `({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:67](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:67)
- [ ] **API-0632 updateAddon** — base `({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})`, `@PutMapping("/{code}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:92](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:92)
- [ ] **API-0633 patchAddon** — base `({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})`, `@PatchMapping("/{code}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:118](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:118)
- [ ] **API-0634 deleteAddon** — base `({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})`, `@DeleteMapping("/{code}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:144](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:144)
- [ ] **API-0635 activateAddon** — base `({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})`, `@PostMapping("/{code}/activate")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:162](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminAddonCatalogController.java:162)
- [ ] **API-0636 updateEntitlements** — base `("/api/super-admin/plans")`, `@PutMapping("/{plan}/entitlements")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:35](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:35)
- [ ] **API-0637 getEntitlements** — base `("/api/super-admin/plans")`, `@GetMapping("/{plan}/entitlements")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:55](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:55)
- [ ] **API-0638 exportEntitlements** — base `("/api/super-admin/plans")`, `@GetMapping(value = "/{plan}/entitlements/export", produces = "text/csv")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:67](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:67)
- [ ] **API-0639 importEntitlements** — base `("/api/super-admin/plans")`, `@PostMapping(value = "/{plan}/entitlements/import", consumes = "text/csv")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:82](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:82)
- [ ] **API-0640 replacePricingTiers** — base `("/api/super-admin/plans")`, `@PostMapping("/{plan}/pricing-tiers")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:100](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:100)
- [ ] **API-0641 getPricingTiers** — base `("/api/super-admin/plans")`, `@GetMapping("/{plan}/pricing-tiers")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:119](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:119)
- [ ] **API-0642 applyDefaults** — base `("/api/super-admin/plans")`, `@PostMapping("/{plan}/entitlements/apply-defaults")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:133](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/AdminPlanEntitlementsController.java:133)
- [ ] **API-0643 getSettings** — base `("/api/v1/super-admin/settings")`, `@GetMapping("/tenant-routing")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/PlatformTenantRoutingController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/PlatformTenantRoutingController.java:33)
- [ ] **API-0644 upsert** — base `("/api/v1/super-admin/settings")`, `@PutMapping("/tenant-routing")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/PlatformTenantRoutingController.java:44](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/PlatformTenantRoutingController.java:44)
- [ ] **API-0645 listTemplates** — base `("/api/v1/super-admin/email-templates")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminEmailTemplateController.java:36](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminEmailTemplateController.java:36)
- [ ] **API-0646 getTemplateByKey** — base `("/api/v1/super-admin/email-templates")`, `@GetMapping("/{templateKey}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminEmailTemplateController.java:43](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminEmailTemplateController.java:43)
- [ ] **API-0647 upsertTemplate** — base `("/api/v1/super-admin/email-templates")`, `@PutMapping("/{templateKey}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminEmailTemplateController.java:50](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminEmailTemplateController.java:50)
- [ ] **API-0648 listCatalog** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:33)
  - Query inputs (one scenario family per field): `includeDeprecated`
- [ ] **API-0649 getCatalogItem** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@GetMapping("/{key}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:40](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:40)
- [ ] **API-0650 exportCatalog** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@GetMapping(value = "/export", produces = "text/csv")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:47](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:47)
- [ ] **API-0651 createFeature** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:58](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:58)
- [ ] **API-0652 importCatalog** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@PostMapping(value = "/import", consumes = "text/csv")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:69](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:69)
  - Query inputs (one scenario family per field): `upsert`
- [ ] **API-0653 upsertCatalogBulk** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@PutMapping("/bulk")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:80](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:80)
- [ ] **API-0654 updateFeature** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@PutMapping("/{key}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:92](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:92)
- [ ] **API-0655 patchFeature** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@PatchMapping("/{key}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:105](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:105)
- [ ] **API-0656 featureCatalogAuditLogs** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@GetMapping("/audit-logs")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:118](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:118)
  - Query inputs (one scenario family per field): `page`, `size`
- [ ] **API-0657 featureCatalogHistoryByKey** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@GetMapping("/{key}/history")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:128](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:128)
  - Query inputs (one scenario family per field): `page`, `size`
- [ ] **API-0658 deleteFeature** — base `({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})`, `@DeleteMapping("/{key}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:139](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureCatalogController.java:139)
- [ ] **API-0659 toggleFeature** — base `("/api/v1/super-admin/features")`, `@PatchMapping("/flags/{key}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:33)
- [ ] **API-0660 previewToggleImpact** — base `("/api/v1/super-admin/features")`, `@GetMapping("/flags/{key}/impact")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:48](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:48)
- [ ] **API-0661 disableAll** — base `("/api/v1/super-admin/features")`, `@PatchMapping("/disable-all")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:55](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:55)
- [ ] **API-0662 previewDisableAllImpact** — base `("/api/v1/super-admin/features")`, `@GetMapping("/disable-all/impact")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:68](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminFeatureFlagController.java:68)
- [ ] **API-0663 listGlobalRollouts** — base `("/api/v1/super-admin/rollouts")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:44](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:44)
- [ ] **API-0664 getGlobalRollout** — base `("/api/v1/super-admin/rollouts")`, `@GetMapping("/{ruleId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:54)
- [ ] **API-0665 getGlobalRolloutHistory** — base `("/api/v1/super-admin/rollouts")`, `@GetMapping("/{ruleId}/history")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:65](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:65)
  - Query inputs (one scenario family per field): `page`, `size`
- [ ] **API-0666 listGlobalRolloutHistory** — base `("/api/v1/super-admin/rollouts")`, `@GetMapping("/history")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:79](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:79)
  - Query inputs (one scenario family per field): `page`, `size`, `featureKey`
- [ ] **API-0667 upsertGlobalRollouts** — base `("/api/v1/super-admin/rollouts")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:93](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:93)
- [ ] **API-0668 updateGlobalRollout** — base `("/api/v1/super-admin/rollouts")`, `@PatchMapping("/{ruleId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:121](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:121)
- [ ] **API-0669 deleteGlobalRollout** — base `("/api/v1/super-admin/rollouts")`, `@DeleteMapping("/{ruleId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:147](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:147)
- [ ] **API-0670 bulkDisableGlobalRollouts** — base `("/api/v1/super-admin/rollouts")`, `@PostMapping("/bulk-disable")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:159](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:159)
- [ ] **API-0671 bulkRemoveGlobalRollouts** — base `("/api/v1/super-admin/rollouts")`, `@DeleteMapping("/by-feature")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:182](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminGlobalRolloutController.java:182)
- [ ] **API-0672 policy** — base `("/api/v1/super-admin/impersonation")`, `@GetMapping("/policy")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:44](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:44)
- [ ] **API-0673 upsertPolicy** — base `("/api/v1/super-admin/impersonation")`, `@PutMapping("/policy")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:51](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:51)
- [ ] **API-0674 start** — base `("/api/v1/super-admin/impersonation")`, `@PostMapping("/start")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:72](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:72)
- [ ] **API-0675 exchange** — base `("/api/v1/super-admin/impersonation")`, `@PostMapping("/exchange")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:101](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:101)
- [ ] **API-0676 sessions** — base `("/api/v1/super-admin/impersonation")`, `@GetMapping("/sessions")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:144](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:144)
- [ ] **API-0677 end** — base `("/api/v1/super-admin/impersonation")`, `@PostMapping("/sessions/{id}/end")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:154](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminImpersonationController.java:154)
- [ ] **API-0678 listIntegrations** — base `("/api/v1/super-admin")`, `@GetMapping("/integrations")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:43](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:43)
- [ ] **API-0679 getIntegration** — base `("/api/v1/super-admin")`, `@GetMapping("/integrations/{integrationKey}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:50](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:50)
- [ ] **API-0680 upsertIntegration** — base `("/api/v1/super-admin")`, `@PutMapping("/integrations/{integrationKey}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:57](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:57)
- [ ] **API-0681 testIntegration** — base `("/api/v1/super-admin")`, `@PostMapping("/integrations/{integrationKey}/test")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:87](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:87)
- [ ] **API-0682 listApiKeys** — base `("/api/v1/super-admin")`, `@GetMapping("/api-keys")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:95](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:95)
- [ ] **API-0683 createApiKey** — base `("/api/v1/super-admin")`, `@PostMapping("/api-keys")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:115](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:115)
- [ ] **API-0684 rotateApiKey** — base `("/api/v1/super-admin")`, `@PutMapping("/api-keys/{keyId}/rotate")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:145](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:145)
- [ ] **API-0685 revokeApiKey** — base `("/api/v1/super-admin")`, `@DeleteMapping("/api-keys/{keyId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:173](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminIntegrationController.java:173)
- [ ] **API-0686 listJobs** — base `("/api/v1/super-admin/jobs")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:37)
- [ ] **API-0687 runJob** — base `("/api/v1/super-admin/jobs")`, `@PostMapping("/{jobKey}/run")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:44](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:44)
- [ ] **API-0688 runAllJobs** — base `("/api/v1/super-admin/jobs")`, `@PostMapping("/run-all")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:54)
- [ ] **API-0689 auditLogs** — base `("/api/v1/super-admin/jobs")`, `@GetMapping("/audit-logs")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:63](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:63)
  - Query inputs (one scenario family per field): `jobKey`, `page`, `size`
- [ ] **API-0690 toggleJob** — base `("/api/v1/super-admin/jobs")`, `@PutMapping("/{jobKey}/enabled")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:74](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminJobAdminController.java:74)
- [ ] **API-0691 getKpis** — base `("/api/v1/super-admin")`, `@GetMapping("/metrics/kpis")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:46](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:46)
  - Query inputs (one scenario family per field): `period`, `orgId`, `timezone`
- [ ] **API-0692 getSummary** — base `("/api/v1/super-admin")`, `@GetMapping("/metrics/summary")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:60](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:60)
  - Query inputs (one scenario family per field): `period`, `orgId`, `timezone`
- [ ] **API-0693 getDashboard** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:74](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:74)
  - Query inputs (one scenario family per field): `period`, `orgId`, `timezone`
- [ ] **API-0694 getOrganisationDashboard** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/organisations")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:88](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:88)
  - Query inputs (one scenario family per field): `period`, `timezone`
- [ ] **API-0695 getRevenueOverview** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/revenue-overview")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:101](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:101)
  - Query inputs (one scenario family per field): `period`, `timezone`
- [ ] **API-0696 getDashboardKpis** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/kpis")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:114](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:114)
  - Query inputs (one scenario family per field): `timezone`
- [ ] **API-0697 getTenantGrowth** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/tenant-growth")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:123](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:123)
  - Query inputs (one scenario family per field): `range`, `timezone`
- [ ] **API-0698 getPlanDistribution** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/plan-distribution")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:133](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:133)
- [ ] **API-0699 getMrrBreakdown** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/mrr-breakdown")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:140](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:140)
- [ ] **API-0700 getSystemHealthPanel** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/system-health")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:147](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:147)
- [ ] **API-0701 getTierAliases** — base `("/api/v1/super-admin")`, `@GetMapping("/dashboard/config/tier-aliases")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:154](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:154)
- [ ] **API-0702 replaceTierAliases** — base `("/api/v1/super-admin")`, `@PutMapping("/dashboard/config/tier-aliases")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:161](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminMetricsController.java:161)
- [ ] **API-0703 createBroadcast** — base `("/api/v1/super-admin/notifications")`, `@PostMapping("/broadcast")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:45](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:45)
- [ ] **API-0704 createTargeted** — base `("/api/v1/super-admin/notifications")`, `@PostMapping("/targeted")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:55](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:55)
- [ ] **API-0705 createScheduled** — base `("/api/v1/super-admin/notifications")`, `@PostMapping("/scheduled")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:71](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:71)
- [ ] **API-0706 history** — base `("/api/v1/super-admin/notifications")`, `@GetMapping("/history")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:81](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:81)
  - Query inputs (one scenario family per field): `orgId`
- [ ] **API-0707 unreadCount** — base `("/api/v1/super-admin/notifications")`, `@GetMapping("/unread-count")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:92](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:92)
  - Query inputs (one scenario family per field): `orgId`
- [ ] **API-0708 markAsRead** — base `("/api/v1/super-admin/notifications")`, `@PatchMapping("/{id}/read")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:104](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:104)
- [ ] **API-0709 markAllAsRead** — base `("/api/v1/super-admin/notifications")`, `@PatchMapping("/read-all")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:115](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:115)
  - Query inputs (one scenario family per field): `orgId`
- [ ] **API-0710 templates** — base `("/api/v1/super-admin/notifications")`, `@GetMapping("/templates")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:127](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:127)
- [ ] **API-0711 triggers** — base `("/api/v1/super-admin/notifications")`, `@GetMapping("/triggers")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:134](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:134)
- [ ] **API-0712 triggerMetadata** — base `("/api/v1/super-admin/notifications")`, `@GetMapping("/triggers/metadata")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:141](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:141)
- [ ] **API-0713 createTrigger** — base `("/api/v1/super-admin/notifications")`, `@PostMapping("/triggers")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:148](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:148)
- [ ] **API-0714 updateTrigger** — base `("/api/v1/super-admin/notifications")`, `@PutMapping("/triggers/{id}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:157](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:157)
- [ ] **API-0715 deleteTrigger** — base `("/api/v1/super-admin/notifications")`, `@DeleteMapping("/triggers/{id}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:167](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:167)
- [ ] **API-0716 upsertTemplate** — base `("/api/v1/super-admin/notifications")`, `@PutMapping("/templates/{templateKey}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:175](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:175)
- [ ] **API-0717 deleteTemplate** — base `("/api/v1/super-admin/notifications")`, `@DeleteMapping("/templates/{templateKey}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:200](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminNotificationController.java:200)
- [ ] **API-0718 suspend** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/suspend")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:42](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:42)
- [ ] **API-0719 reactivate** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/reactivate")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:55](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:55)
- [ ] **API-0720 terminate** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/terminate")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:71](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:71)
- [ ] **API-0721 bulk** — base `("/api/v1/super-admin")`, `@PostMapping("/bulk/actions")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:92](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:92)
- [ ] **API-0722 forcePasswordReset** — base `("/api/v1/super-admin")`, `@PostMapping("/organisations/{id}/force-password-reset")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:108](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:108)
- [ ] **API-0723 backups** — base `("/api/v1/super-admin")`, `@GetMapping("/organisations/{id}/backups")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:132](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:132)
- [ ] **API-0724 jobs** — base `("/api/v1/super-admin")`, `@GetMapping("/jobs/scheduled")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:139](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminOperationsController.java:139)
- [ ] **API-0725 upsert** — base `("/api/v1/super-admin/partners")`, `@PutMapping("/{partnerId}/access")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPartnerController.java:40](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPartnerController.java:40)
- [ ] **API-0726 list** — base `("/api/v1/super-admin/partners")`, `@GetMapping("/{partnerId}/access")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPartnerController.java:62](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPartnerController.java:62)
- [ ] **API-0727 listPermissions** — base `("/api/v1/super-admin/permissions")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPermissionController.java:33](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPermissionController.java:33)
- [ ] **API-0728 listPermissionsCatalog** — base `("/api/v1/super-admin/permissions")`, `@GetMapping("/catalog")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPermissionController.java:50](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminPermissionController.java:50)
- [ ] **API-0729 me** — base `("/api/v1/super-admin/me")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:62](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:62)
- [ ] **API-0730 updateMe** — base `("/api/v1/super-admin/me")`, `@PutMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:72](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:72)
- [ ] **API-0731 getProfile** — base `("/api/v1/super-admin/me")`, `@GetMapping("/profile")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:93](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:93)
- [ ] **API-0732 updateProfile** — base `("/api/v1/super-admin/me")`, `@PutMapping("/profile")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:104](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:104)
- [ ] **API-0733 uploadProfilePicture** — base `("/api/v1/super-admin/me")`, `@PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:125](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:125)
  - Query inputs (one scenario family per field): `file`
- [ ] **API-0734 changePassword** — base `("/api/v1/super-admin/me")`, `@PostMapping("/change-password")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:142](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:142)
- [ ] **API-0735 getTwoFactorStatus** — base `("/api/v1/super-admin/me")`, `@GetMapping("/2fa")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:161](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:161)
- [ ] **API-0736 enableTwoFactor** — base `("/api/v1/super-admin/me")`, `@PostMapping("/2fa/enable")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:169](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:169)
- [ ] **API-0737 confirmTwoFactor** — base `("/api/v1/super-admin/me")`, `@PostMapping("/2fa/confirm")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:180](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:180)
- [ ] **API-0738 disableTwoFactor** — base `("/api/v1/super-admin/me")`, `@PostMapping("/2fa/disable")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:191](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminProfileController.java:191)
- [ ] **API-0739 createRole** — base `("/api/v1/super-admin/roles")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:37)
- [ ] **API-0740 listRoles** — base `("/api/v1/super-admin/roles")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:52](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:52)
  - Query inputs (one scenario family per field): `organisationId`
- [ ] **API-0741 getRole** — base `("/api/v1/super-admin/roles")`, `@GetMapping("/{roleId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:63](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:63)
- [ ] **API-0742 updateRole** — base `("/api/v1/super-admin/roles")`, `@PutMapping("/{roleId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:74](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:74)
- [ ] **API-0743 deleteRole** — base `("/api/v1/super-admin/roles")`, `@DeleteMapping("/{roleId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:90](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:90)
- [ ] **API-0744 reseedOrganisationRoles** — base `("/api/v1/super-admin/roles")`, `@PostMapping("/reseed/{organisationId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:105](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:105)
- [ ] **API-0745 reseedAllOrganisationRoles** — base `("/api/v1/super-admin/roles")`, `@PostMapping("/reseed-all")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:117](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminRoleController.java:117)
- [ ] **API-0746 getSecuritySettings** — base `("/api/v1/super-admin/security")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSecurityController.java:45](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSecurityController.java:45)
- [ ] **API-0747 updateSecuritySettings** — base `("/api/v1/super-admin/security")`, `@PutMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSecurityController.java:52](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSecurityController.java:52)
- [ ] **API-0748 requestBreakGlassAccess** — base `("/api/v1/super-admin/security")`, `@PostMapping("/break-glass")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSecurityController.java:74](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSecurityController.java:74)
- [ ] **API-0749 getSubscription** — base `("/api/v1/super-admin/organisations")`, `@GetMapping("/{id}/subscription")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:37)
- [ ] **API-0750 getSubscriptionStatus** — base `("/api/v1/super-admin/organisations")`, `@GetMapping("/{id}/subscription/status")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:45](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:45)
- [ ] **API-0751 updateSubscription** — base `("/api/v1/super-admin/organisations")`, `@PutMapping("/{id}/subscription")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:53](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:53)
- [ ] **API-0752 provisionStripeSubscription** — base `("/api/v1/super-admin/organisations")`, `@PostMapping("/{id}/subscription/provision-stripe")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:85](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:85)
- [ ] **API-0753 createManualRenewalInvoice** — base `("/api/v1/super-admin/organisations")`, `@PostMapping("/{id}/subscription/invoices")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:97](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:97)
- [ ] **API-0754 backfillStripeSubscription** — base `("/api/v1/super-admin/organisations")`, `@PostMapping("/{id}/subscription/backfill-stripe")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:118](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSubscriptionController.java:118)
- [ ] **API-0755 health** — base `("/api/v1/super-admin/system")`, `@GetMapping("/health")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:47](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:47)
- [ ] **API-0756 uptime** — base `("/api/v1/super-admin/system")`, `@GetMapping("/uptime")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:53](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:53)
- [ ] **API-0757 incidents** — base `("/api/v1/super-admin/system")`, `@GetMapping("/incidents")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:60](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:60)
  - Query inputs (one scenario family per field): `status`
- [ ] **API-0758 createIncident** — base `("/api/v1/super-admin/system")`, `@PostMapping("/incidents")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:66](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:66)
- [ ] **API-0759 updateIncident** — base `("/api/v1/super-admin/system")`, `@PatchMapping("/incidents/{id}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:89](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:89)
- [ ] **API-0760 auditLogs** — base `("/api/v1/super-admin/system")`, `@GetMapping("/audit-logs")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:113](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:113)
  - Query inputs (one scenario family per field): `page`, `size`, `authId`, `action`, `resourceType`, `resourceId`, `logLevel`, `q`, `createdFrom`, `createdTo`, `sort`, `order`
- [ ] **API-0761 globalAuditHealth** — base `("/api/v1/super-admin/system")`, `@GetMapping("/global-audit-health")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:145](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminSystemController.java:145)
  - Query inputs (one scenario family per field): `hours`, `topN`
- [ ] **API-0762 getUsage** — base `("/api/v1/super-admin")`, `@GetMapping("/usage")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUsageController.java:35](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUsageController.java:35)
  - Query inputs (one scenario family per field): `organisationId`, `period`, `targetKey`
- [ ] **API-0763 getUsageTargets** — base `("/api/v1/super-admin")`, `@GetMapping("/usage/targets")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUsageController.java:46](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUsageController.java:46)
  - Query inputs (one scenario family per field): `organisationId`, `period`
- [ ] **API-0764 exportUsageTargetsCsv** — base `("/api/v1/super-admin")`, `@GetMapping(value = "/usage/targets/export", produces = "text/csv")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUsageController.java:56](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUsageController.java:56)
  - Query inputs (one scenario family per field): `organisationId`, `period`
- [ ] **API-0765 list** — base `("/api/v1/super-admin/users")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:42](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:42)
  - Query inputs (one scenario family per field): `search`, `role`, `identityType`, `status`, `organisationId`, `active`, `lastLoginFrom`, `lastLoginTo`, `page`, `size`
- [ ] **API-0766 disable** — base `("/api/v1/super-admin/users")`, `@PostMapping("/{authId}/disable")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:86](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:86)
- [ ] **API-0767 enable** — base `("/api/v1/super-admin/users")`, `@PostMapping("/{authId}/enable")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:106](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:106)
- [ ] **API-0768 linkOrganisation** — base `("/api/v1/super-admin/users")`, `@PutMapping("/{authId}/organisations/{organisationId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:121](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:121)
- [ ] **API-0769 unlinkOrganisation** — base `("/api/v1/super-admin/users")`, `@DeleteMapping("/{authId}/organisations/{organisationId}")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:138](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:138)
- [ ] **API-0770 disableForOrganisation** — base `("/api/v1/super-admin/users")`, `@PostMapping("/{authId}/organisations/{organisationId}/disable")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:155](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:155)
- [ ] **API-0771 enableForOrganisation** — base `("/api/v1/super-admin/users")`, `@PostMapping("/{authId}/organisations/{organisationId}/enable")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:177](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:177)
- [ ] **API-0772 getUserRolesPermissions** — base `("/api/v1/super-admin/users")`, `@GetMapping("/{authId}/roles-permissions")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:197](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:197)
  - Query inputs (one scenario family per field): `module`, `permissionGroup`
- [ ] **API-0773 getRolesPermissionsMatrix** — base `("/api/v1/super-admin/users")`, `@GetMapping("/roles-permissions-matrix")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:212](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:212)
  - Query inputs (one scenario family per field): `module`, `permissionGroup`
- [ ] **API-0774 updateRolesPermissionsMatrix** — base `("/api/v1/super-admin/users")`, `@PutMapping("/roles-permissions-matrix")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:226](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:226)
- [ ] **API-0775 toggleRolePermissionCell** — base `("/api/v1/super-admin/users")`, `@PatchMapping("/roles-permissions-matrix/toggle")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:245](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:245)
- [ ] **API-0776 exportRolesPermissionsMatrix** — base `("/api/v1/super-admin/users")`, `@GetMapping(value = "/roles-permissions-matrix/export", produces = "text/csv")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:266](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/SuperAdminUserController.java:266)
  - Query inputs (one scenario family per field): `module`, `permissionGroup`
- [ ] **API-0777 resolve** — base `("/api/v1/super-admin/tenants")`, `@PostMapping("/resolve")` — [src/main/java/com/smart/therapy/flow/superadmin/controller/TenantRoutingController.java:35](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/controller/TenantRoutingController.java:35)
- [ ] **API-0778 getPracticeConfiguration** — base `("/api/v1/practice-configuration")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/system/controller/PracticeConfigurationController.java:30](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/PracticeConfigurationController.java:30)
- [ ] **API-0779 updatePracticeConfiguration** — base `("/api/v1/practice-configuration")`, `@PutMapping` — [src/main/java/com/smart/therapy/flow/system/controller/PracticeConfigurationController.java:48](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/PracticeConfigurationController.java:48)
- [ ] **API-0780 getCategories** — base `("/api/v1/system-options")`, `@GetMapping("/categories")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:28](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:28)
- [ ] **API-0781 getCategory** — base `("/api/v1/system-options")`, `@GetMapping("/categories/{id}")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:40](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:40)
- [ ] **API-0782 getCategoryUsage** — base `("/api/v1/system-options")`, `@GetMapping("/categories/{id}/usage")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:47](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:47)
- [ ] **API-0783 createCategory** — base `("/api/v1/system-options")`, `@PostMapping("/categories")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:54](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:54)
- [ ] **API-0784 updateCategory** — base `("/api/v1/system-options")`, `@PutMapping("/categories/{id}")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:76](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:76)
- [ ] **API-0785 deleteCategory** — base `("/api/v1/system-options")`, `@DeleteMapping("/categories/{id}")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:87](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:87)
- [ ] **API-0786 reorderCategoryOptions** — base `("/api/v1/system-options")`, `@PutMapping("/categories/{id}/options/order")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:97](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:97)
- [ ] **API-0787 getOptions** — base `("/api/v1/system-options")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:110](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:110)
  - Query inputs (one scenario family per field): `categoryId`
- [ ] **API-0788 getOptionsByCategoryKey** — base `("/api/v1/system-options")`, `@GetMapping("/by-category/{categoryKey}")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:132](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:132)
- [ ] **API-0789 getOption** — base `("/api/v1/system-options")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:141](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:141)
- [ ] **API-0790 getOptionUsage** — base `("/api/v1/system-options")`, `@GetMapping("/{id}/usage")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:148](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:148)
- [ ] **API-0791 createOption** — base `("/api/v1/system-options")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:155](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:155)
- [ ] **API-0792 updateOption** — base `("/api/v1/system-options")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:177](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:177)
- [ ] **API-0793 deleteOption** — base `("/api/v1/system-options")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:188](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/controller/SystemOptionController.java:188)
- [ ] **API-0794 getTemplates** — base `("/api/v1/checklists")`, `@GetMapping("/checklist-templates")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:38](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:38)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `search`, `category`, `categoryType`
- [ ] **API-0795 getTemplate** — base `("/api/v1/checklists")`, `@GetMapping("/checklist-templates/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:52](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:52)
- [ ] **API-0796 createTemplate** — base `("/api/v1/checklists")`, `@PostMapping("/checklist-templates")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:59](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:59)
- [ ] **API-0797 updateTemplate** — base `("/api/v1/checklists")`, `@PatchMapping("/checklist-templates/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:82](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:82)
- [ ] **API-0798 deleteTemplate** — base `("/api/v1/checklists")`, `@DeleteMapping("/checklist-templates/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:120](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:120)
- [ ] **API-0799 getTemplateItems** — base `("/api/v1/checklists")`, `@GetMapping("/checklist-templates/{templateId}/items")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:153](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:153)
- [ ] **API-0800 getTemplateItem** — base `("/api/v1/checklists")`, `@GetMapping("/checklist-templates/{templateId}/items/{itemId}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:191](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:191)
- [ ] **API-0801 addTemplateItem** — base `("/api/v1/checklists")`, `@PostMapping("/checklist-templates/{templateId}/items")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:229](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:229)
- [ ] **API-0802 updateTemplateItem** — base `("/api/v1/checklists")`, `@PatchMapping("/checklist-templates/{templateId}/items/{itemId}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:287](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:287)
- [ ] **API-0803 deleteTemplateItem** — base `("/api/v1/checklists")`, `@DeleteMapping("/checklist-templates/{templateId}/items/{itemId}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:347](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:347)
- [ ] **API-0804 getClientChecklists** — base `("/api/v1/checklists")`, `@GetMapping("/clients/{clientId}/checklists")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:369](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:369)
  - Query inputs (one scenario family per field): `templateId`, `category`, `isCompleted`
- [ ] **API-0805 getChecklistsWithFilters** — base `("/api/v1/checklists")`, `@PostMapping("/checklists/filter")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:398](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:398)
- [ ] **API-0806 getClientChecklist** — base `("/api/v1/checklists")`, `@GetMapping("/client-checklists/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:437](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:437)
- [ ] **API-0807 assignChecklist** — base `("/api/v1/checklists")`, `@PostMapping("/clients/{clientId}/checklists")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:444](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:444)
- [ ] **API-0808 bulkAssignChecklist** — base `("/api/v1/checklists")`, `@PostMapping("/bulk-assign")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:472](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:472)
- [ ] **API-0809 getClientChecklistItems** — base `("/api/v1/checklists")`, `@GetMapping("/client-checklist-items/{clientChecklistId}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:490](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:490)
- [ ] **API-0810 updateClientChecklistItem** — base `("/api/v1/checklists")`, `@PutMapping("/client-checklist-items/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:497](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:497)
- [ ] **API-0811 getComplianceReport** — base `("/api/v1/checklists")`, `@GetMapping("/compliance-report/{templateId}")` — [src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:511](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/ChecklistController.java:511)
- [ ] **API-0812 getTasks** — base `("/api/v1/tasks")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:31](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:31)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `status`, `priority`, `assignedToId`, `clientId`, `search`, `dateFilter`, `fromDate`, `toDate`, `dateField`, `sortBy`, `sortOrder`
- [ ] **API-0813 getTaskHistory** — base `("/api/v1/tasks")`, `@GetMapping("/history")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:66](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:66)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `status`, `priority`, `assignedToId`, `clientId`, `search`, `dateFilter`, `fromDate`, `toDate`, `dateField`, `sortBy`, `sortOrder`
- [ ] **API-0814 getTaskStats** — base `("/api/v1/tasks")`, `@GetMapping("/stats")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:102](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:102)
  - Query inputs (one scenario family per field): `assignedToId`, `fromDate`, `toDate`, `dateField`
- [ ] **API-0815 getTask** — base `("/api/v1/tasks")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:114](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:114)
- [ ] **API-0816 createTask** — base `("/api/v1/tasks")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:124](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:124)
- [ ] **API-0817 updateTask** — base `("/api/v1/tasks")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:190](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:190)
- [ ] **API-0818 deleteTask** — base `("/api/v1/tasks")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:202](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:202)
- [ ] **API-0819 checkOverdueTasks** — base `("/api/v1/tasks")`, `@PostMapping("/check-overdue")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:213](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:213)
- [ ] **API-0820 getTaskComments** — base `("/api/v1/tasks")`, `@GetMapping("/{taskId}/comments")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:222](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:222)
- [ ] **API-0821 createTaskComment** — base `("/api/v1/tasks")`, `@PostMapping("/{taskId}/comments")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:232](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:232)
- [ ] **API-0822 updateTaskComment** — base `("/api/v1/tasks")`, `@PutMapping("/{taskId}/comments/{commentId}")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:244](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:244)
- [ ] **API-0823 deleteTaskComment** — base `("/api/v1/tasks")`, `@DeleteMapping("/{taskId}/comments/{commentId}")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:256](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:256)
- [ ] **API-0824 getRecentTasks** — base `("/api/v1/tasks")`, `@GetMapping("/recent")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:267](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:267)
  - Query inputs (one scenario family per field): `limit`
- [ ] **API-0825 getUpcomingTasks** — base `("/api/v1/tasks")`, `@GetMapping("/upcoming")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:277](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:277)
  - Query inputs (one scenario family per field): `limit`
- [ ] **API-0826 getPendingTasksCount** — base `("/api/v1/tasks")`, `@GetMapping("/pending/count")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:287](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:287)
- [ ] **API-0827 getClientTasks** — base `("/api/v1/tasks")`, `@GetMapping("/clients/{clientId}/tasks")` — [src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:298](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/controller/TaskController.java:298)
- [ ] **API-0828 getTherapistBlockedTimes** — base `("/api/v1/therapist-availability")`, `@GetMapping("/therapist-blocked-times")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:41](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:41)
  - Query inputs (one scenario family per field): `therapistId`
- [ ] **API-0829 getBlockedTime** — base `("/api/v1/therapist-availability")`, `@GetMapping("/therapist-blocked-times/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:63](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:63)
- [ ] **API-0830 createBlockedTime** — base `("/api/v1/therapist-availability")`, `@PostMapping("/therapist-blocked-times")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:70](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:70)
- [ ] **API-0831 updateBlockedTime** — base `("/api/v1/therapist-availability")`, `@PatchMapping("/therapist-blocked-times/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:92](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:92)
- [ ] **API-0832 deleteBlockedTime** — base `("/api/v1/therapist-availability")`, `@DeleteMapping("/therapist-blocked-times/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:103](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:103)
- [ ] **API-0833 getAvailableSlots** — base `("/api/v1/therapist-availability")`, `@GetMapping("/availability/slots")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:113](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:113)
  - Query inputs (one scenario family per field): `therapistId`, `date`, `serviceId`, `sessionType`
- [ ] **API-0834 getTherapistSchedule** — base `("/api/v1/therapist-availability")`, `@GetMapping(value = "/api/v1/therapists/{therapistId}/schedule/{date}")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:147](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:147)
- [ ] **API-0835 getBookingCount** — base `("/api/v1/therapist-availability")`, `@GetMapping(value = "/api/v1/therapists/{therapistId}/bookings/count")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:177](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:177)
  - Query inputs (one scenario family per field): `startDate`, `endDate`
- [ ] **API-0836 getSessionStats** — base `("/api/v1/therapist-availability")`, `@GetMapping(value = "/api/v1/therapists/{therapistId}/session-stats")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:209](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistAvailabilityController.java:209)
  - Query inputs (one scenario family per field): `startDate`, `endDate`
- [ ] **API-0837 getDashboardSummary** — base `("/api/v1/therapists/dashboard")`, `@GetMapping("/summary")` — [src/main/java/com/smart/therapy/flow/user/controller/TherapistDashboardController.java:27](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/TherapistDashboardController.java:27)
- [ ] **API-0838 getUsers** — base `("/api/v1/users")`, `@GetMapping` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:39](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:39)
  - Query inputs (one scenario family per field): `page`, `pageSize`, `search`, `role`, `active`
- [ ] **API-0839 getUser** — base `("/api/v1/users")`, `@GetMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:62](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:62)
- [ ] **API-0840 createUser** — base `("/api/v1/users")`, `@PostMapping` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:71](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:71)
- [ ] **API-0841 updateUser** — base `("/api/v1/users")`, `@PutMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:138](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:138)
- [ ] **API-0842 deleteUser** — base `("/api/v1/users")`, `@DeleteMapping("/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:184](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:184)
- [ ] **API-0843 getCurrentUser** — base `("/api/v1/users")`, `@GetMapping("/me")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:202](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:202)
- [ ] **API-0844 updateCurrentUser** — base `("/api/v1/users")`, `@PutMapping("/me")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:207](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:207)
- [ ] **API-0845 uploadMyProfilePicture** — base `("/api/v1/users")`, `@PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:217](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:217)
  - Query inputs (one scenario family per field): `file`
- [ ] **API-0846 getProfile** — base `("/api/v1/users")`, `@GetMapping("/me/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:233](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:233)
- [ ] **API-0847 createProfile** — base `("/api/v1/users")`, `@PostMapping("/me/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:244](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:244)
- [ ] **API-0848 updateProfile** — base `("/api/v1/users")`, `@PutMapping("/me/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:292](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:292)
- [ ] **API-0849 patchProfile** — base `("/api/v1/users")`, `@PatchMapping("/me/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:336](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:336)
- [ ] **API-0850 listAvailableTimezones** — base `("/api/v1/users")`, `@GetMapping("/timezones")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:351](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:351)
- [ ] **API-0851 getTimezone** — base `("/api/v1/users")`, `@GetMapping("/me/timezone")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:365](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:365)
- [ ] **API-0852 updateTimezone** — base `("/api/v1/users")`, `@PutMapping("/me/timezone")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:386](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:386)
- [ ] **API-0853 changePassword** — base `("/api/v1/users")`, `@PostMapping("/me/change-password")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:467](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:467)
- [ ] **API-0854 updateZoomCredentials** — base `("/api/v1/users")`, `@PutMapping("/me/zoom-credentials")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:477](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:477)
- [ ] **API-0855 deleteZoomCredentials** — base `("/api/v1/users")`, `@DeleteMapping("/me/zoom-credentials")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:486](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:486)
- [ ] **API-0856 getZoomStatus** — base `("/api/v1/users")`, `@GetMapping("/me/zoom-credentials/status")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:495](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:495)
- [ ] **API-0857 testZoomCredentials** — base `("/api/v1/users")`, `@PostMapping("/me/zoom-credentials/test")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:502](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:502)
- [ ] **API-0858 logUserActivity** — base `("/api/v1/users")`, `@PostMapping("/{userId}/activity")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:509](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:509)
- [ ] **API-0859 getUserActivity** — base `("/api/v1/users")`, `@GetMapping("/{userId}/activity")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:522](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:522)
  - Query inputs (one scenario family per field): `limit`
- [ ] **API-0860 getUserProfile** — base `("/api/v1/users")`, `@GetMapping("/{userId}/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:533](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:533)
- [ ] **API-0861 createUserProfile** — base `("/api/v1/users")`, `@PostMapping("/{userId}/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:543](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:543)
- [ ] **API-0862 updateUserProfile** — base `("/api/v1/users")`, `@PutMapping("/{userId}/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:556](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:556)
- [ ] **API-0863 patchUserProfile** — base `("/api/v1/users")`, `@PatchMapping("/{userId}/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:569](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:569)
- [ ] **API-0864 deleteUserProfile** — base `("/api/v1/users")`, `@DeleteMapping("/{userId}/profile")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:582](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:582)
- [ ] **API-0865 getZoomCredentialsStatus** — base `("/api/v1/users")`, `@GetMapping("/{userId}/zoom-credentials/status")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:593](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:593)
- [ ] **API-0866 createSupervisorAssignment** — base `("/api/v1/users")`, `@PostMapping("/supervisor-assignments")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:607](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:607)
- [ ] **API-0867 getSupervisorAssignments** — base `("/api/v1/users")`, `@GetMapping("/supervisor-assignments")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:653](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:653)
  - Query inputs (one scenario family per field): `supervisorId`, `therapistId`, `active`, `search`, `requiredMeetingFrequency`
- [ ] **API-0868 getSupervisorAssignment** — base `("/api/v1/users")`, `@GetMapping("/supervisor-assignments/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:699](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:699)
- [ ] **API-0869 updateSupervisorAssignment** — base `("/api/v1/users")`, `@PutMapping("/supervisor-assignments/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:736](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:736)
- [ ] **API-0870 deleteSupervisorAssignment** — base `("/api/v1/users")`, `@DeleteMapping("/supervisor-assignments/{id}")` — [src/main/java/com/smart/therapy/flow/user/controller/UserController.java:781](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/controller/UserController.java:781)

## Backend request/filter fields


### TestEmailRequest.java

[src/main/java/com/smart/therapy/flow/admin/dto/TestEmailRequest.java:6](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/admin/dto/TestEmailRequest.java:6)

- [ ] `toEmail` (String)

### AssistantChatRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/AssistantChatRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/AssistantChatRequest.java:13)

- [ ] `userMessage` (String)
- [ ] `conversationHistory` (List<ChatMessageDto>)
- [ ] `userRole` (String)

### ConnectedSuggestionsRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/ConnectedSuggestionsRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/ConnectedSuggestionsRequest.java:15)

- [ ] `templateId` (String)
- [ ] `sourceField` (String)
- [ ] `sourceValue` (String)
- [ ] `clientId` (Long)

### GenerateClinicalReportRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/GenerateClinicalReportRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/GenerateClinicalReportRequest.java:15)

- [ ] `clientId` (Long)
- [ ] `sessionNoteId` (Long)
- [ ] `clientName` (String)
- [ ] `sessionType` (String)
- [ ] `sessionDate` (String)
- [ ] `sessionNoteData` (Map<String, String>)

### GenerateFromTemplateRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/GenerateFromTemplateRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/GenerateFromTemplateRequest.java:15)

- [ ] `templateId` (String)
- [ ] `field` (String)
- [ ] `context` (String)

### GenerateSuggestionsRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/GenerateSuggestionsRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/GenerateSuggestionsRequest.java:15)

- [ ] `field` (String)
- [ ] `context` (String)
- [ ] `clientId` (Long)

### GenerateTemplateRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/GenerateTemplateRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/GenerateTemplateRequest.java:17)

- [ ] `clientId` (Long)
- [ ] `sessionId` (Long)
- [ ] `templateId` (Long)
- [ ] `formData` (Map<String, String>)
- [ ] `customInstructions` (String)

### RegenerateContentRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/RegenerateContentRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/RegenerateContentRequest.java:13)

- [ ] `customPrompt` (String)

### SessionNoteTemplateRequest.java

[src/main/java/com/smart/therapy/flow/ai/dto/SessionNoteTemplateRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/ai/dto/SessionNoteTemplateRequest.java:14)

- [ ] `client` (ClientInfo)
- [ ] `session` (SessionInfo)
- [ ] `formData` (Map<String, String>)
- [ ] `customInstructions` (String)
- [ ] `fullName` (String)
- [ ] `dateOfBirth` (Instant)
- [ ] `gender` (String)
- [ ] `stage` (String)
- [ ] `sessionType` (String)
- [ ] `sessionDate` (Instant)
- [ ] `duration` (Integer)

### AssessmentOptionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentOptionRequest.java:8](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentOptionRequest.java:8)

- [ ] `optionText` (String)
- [ ] `optionValue` (Double)
- [ ] `sortOrder` (Integer)

### AssessmentQuestionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentQuestionRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentQuestionRequest.java:10)

- [ ] `questionText` (String)
- [ ] `questionType` (String)
- [ ] `isRequired` (Boolean)
- [ ] `sortOrder` (Integer)
- [ ] `ratingMin` (Double)
- [ ] `ratingMax` (Double)
- [ ] `ratingLabels` (String)
- [ ] `contributesToScore` (Boolean)
- [ ] `options` (List<AssessmentOptionRequest>)

### AssessmentReminderRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentReminderRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentReminderRequest.java:16)

- [ ] `assignmentId` (Long)
- [ ] `reminderDaysBefore` (Integer)
- [ ] `reminderDate` (LocalDate)
- [ ] `sendEmail` (Boolean)
- [ ] `sendInApp` (Boolean)

### AssessmentSectionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentSectionRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/AssessmentSectionRequest.java:10)

- [ ] `title` (String)
- [ ] `description` (String)
- [ ] `accessLevel` (String)
- [ ] `isScoring` (Boolean)
- [ ] `sortOrder` (Integer)
- [ ] `reportMapping` (String)
- [ ] `aiReportPrompt` (String)
- [ ] `questions` (List<AssessmentQuestionRequest>)

### BulkAssessmentQuestionOptionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/BulkAssessmentQuestionOptionRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/BulkAssessmentQuestionOptionRequest.java:12)

- [ ] `questionId` (Long)
- [ ] `options` (List<CreateAssessmentQuestionOptionRequest>)

### BulkDeleteRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/BulkDeleteRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/BulkDeleteRequest.java:15)

- [ ] `ids` (List<Long>)

### BulkQuestionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/BulkQuestionRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/BulkQuestionRequest.java:17)

- [ ] `questions` (List<AssessmentQuestionRequest>)

### BulkSectionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/BulkSectionRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/BulkSectionRequest.java:17)

- [ ] `sections` (List<AssessmentSectionRequest>)

### CreateAssessmentAssignmentRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/CreateAssessmentAssignmentRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/CreateAssessmentAssignmentRequest.java:15)

- [ ] `templateId` (Long)
- [ ] `clientId` (Long)
- [ ] `dueDate` (Instant)
- [ ] `notes` (String)

### CreateAssessmentQuestionOptionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/CreateAssessmentQuestionOptionRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/CreateAssessmentQuestionOptionRequest.java:12)

- [ ] `questionId` (Long)
- [ ] `optionKey` (String)
- [ ] `optionText` (String)
- [ ] `optionValue` (String)
- [ ] `scoreValue` (BigDecimal)
- [ ] `sortOrder` (Integer)
- [ ] `isDefault` (Boolean)

### CreateAssessmentTemplateRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/CreateAssessmentTemplateRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/CreateAssessmentTemplateRequest.java:15)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `category` (String)
- [ ] `isStandardized` (Boolean)
- [ ] `version` (Integer)
- [ ] `sections` (List<AssessmentSectionRequest>)

### QuestionResponseRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/QuestionResponseRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/QuestionResponseRequest.java:12)

- [ ] `questionId` (Long)
- [ ] `responseText` (String)
- [ ] `scoreValue` (Double)
- [ ] `selectedOptionId` (Integer)
- [ ] `selectedOptionIds` (List<Integer>)
- [ ] `ratingValue` (Integer)

### SubmitAssessmentResponseRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/SubmitAssessmentResponseRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/SubmitAssessmentResponseRequest.java:12)

- [ ] `assignmentId` (Long)
- [ ] `responses` (List<QuestionResponseRequest>)

### UpdateAssessmentQuestionOptionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentQuestionOptionRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentQuestionOptionRequest.java:16)

- [ ] `optionKey` (String)
- [ ] `optionText` (String)
- [ ] `optionValue` (String)
- [ ] `scoreValue` (BigDecimal)
- [ ] `sortOrder` (Integer)
- [ ] `isDefault` (Boolean)

### UpdateAssessmentQuestionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentQuestionRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentQuestionRequest.java:19)

- [ ] `questionText` (String)
- [ ] `questionType` (String)
- [ ] `isRequired` (Boolean)
- [ ] `sortOrder` (Integer)
- [ ] `ratingMin` (Double)
- [ ] `ratingMax` (Double)
- [ ] `ratingLabels` (String)
- [ ] `contributesToScore` (Boolean)
- [ ] `options` (List<AssessmentOptionRequest>)

### UpdateAssessmentSectionRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentSectionRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentSectionRequest.java:19)

- [ ] `title` (String)
- [ ] `description` (String)
- [ ] `accessLevel` (String)
- [ ] `isScoring` (Boolean)
- [ ] `sortOrder` (Integer)
- [ ] `reportMapping` (String)
- [ ] `aiReportPrompt` (String)
- [ ] `questions` (List<AssessmentQuestionRequest>)

### UpdateAssessmentStatusRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentStatusRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentStatusRequest.java:14)

- [ ] `status` (AssessmentStatus)
- [ ] `notes` (String)

### UpdateAssessmentTemplateRequest.java

[src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentTemplateRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/assessment/dto/UpdateAssessmentTemplateRequest.java:19)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `category` (String)
- [ ] `isStandardized` (Boolean)
- [ ] `version` (String)
- [ ] `isActive` (Boolean)
- [ ] `sections` (List<AssessmentSectionRequest>)

### AuditLogFilterRequest.java

[src/main/java/com/smart/therapy/flow/audit/dto/AuditLogFilterRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/audit/dto/AuditLogFilterRequest.java:18)

- [ ] `startDate` (LocalDate)
- [ ] `endDate` (LocalDate)
- [ ] `riskLevel` (String)
- [ ] `hipaaOnly` (Boolean)
- [ ] `action` (String)
- [ ] `username` (String)
- [ ] `clientId` (Long)
- [ ] `resourceType` (String)
- [ ] `logLevel` (String)

### ChangePasswordRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/ChangePasswordRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/ChangePasswordRequest.java:14)

- [ ] `changePasswordToken` (String)
- [ ] `currentPassword` (String)
- [ ] `newPassword` (String)

### CreatePermissionRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/CreatePermissionRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/CreatePermissionRequest.java:9)

- [ ] `name` (String)
- [ ] `displayName` (String)
- [ ] `description` (String)
- [ ] `category` (String)
- [ ] `isActive` (Boolean)

### CreateRoleRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/CreateRoleRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/CreateRoleRequest.java:16)

- [ ] `name` (String)
- [ ] `displayName` (String)
- [ ] `description` (String)
- [ ] `isSystem` (Boolean)
- [ ] `isActive` (Boolean)
- [ ] `permissions` (List<Long>)

### LoginRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/LoginRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/LoginRequest.java:19)

- [ ] `username` (String)
- [ ] `password` (String)
- [ ] `orgIdentifier` (String)
- [ ] `orgValue` (String)
- [ ] `orgId` (String)
- [ ] `orgSlug` (String)
- [ ] `deviceTrustToken` (String)
- [ ] `trustDevice` (Boolean)
- [ ] `staySignedIn` (Boolean)

### LogoutRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/LogoutRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/LogoutRequest.java:16)

- [ ] `refreshToken` (String)

### RefreshTokenRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/RefreshTokenRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/RefreshTokenRequest.java:17)

- [ ] `refreshToken` (String)

### StaffForgotPasswordRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/StaffForgotPasswordRequest.java:20](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/StaffForgotPasswordRequest.java:20)

- [ ] `email` (String)
- [ ] `orgIdentifier` (String)
- [ ] `orgValue` (String)
- [ ] `orgId` (String)
- [ ] `orgSlug` (String)

### StaffResetPasswordRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/StaffResetPasswordRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/StaffResetPasswordRequest.java:17)

- [ ] `token` (String)
- [ ] `newPassword` (String)

### TenantResolveRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/TenantResolveRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/TenantResolveRequest.java:11)

- [ ] `email` (String)

### UpdateRolePermissionsRequest.java

[src/main/java/com/smart/therapy/flow/auth/dto/UpdateRolePermissionsRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/auth/dto/UpdateRolePermissionsRequest.java:11)

- [ ] `permissionIds` (List<Long>)

### ApplyDiscountRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/ApplyDiscountRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/ApplyDiscountRequest.java:18)

- [ ] `discountType` (String)
- [ ] `discountValue` (BigDecimal)
- [ ] `discountAmount` (BigDecimal)

### ChangeBillingStatusRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/ChangeBillingStatusRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/ChangeBillingStatusRequest.java:18)

- [ ] `billingStatus` (String)
- [ ] `notes` (String)

### CreateServiceRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/CreateServiceRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/CreateServiceRequest.java:16)

- [ ] `serviceCode` (String)
- [ ] `serviceName` (String)
- [ ] `description` (String)
- [ ] `durationInMinutes` (Integer)
- [ ] `baseRate` (BigDecimal)
- [ ] `isActive` (Boolean)
- [ ] `therapistVisible` (Boolean)
- [ ] `clientPortalVisible` (Boolean)
- [ ] `publicSiteEnabled` (Boolean)

### CreateSessionBillingRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/CreateSessionBillingRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/CreateSessionBillingRequest.java:13)

- [ ] `sessionId` (Long)
- [ ] `serviceId` (Long)
- [ ] `serviceCode` (String)
- [ ] `unitRate` (BigDecimal)
- [ ] `units` (Integer)
- [ ] `insuranceCovered` (Boolean)
- [ ] `billingDate` (Instant)
- [ ] `copayAmount` (BigDecimal)
- [ ] `discountType` (String)
- [ ] `discountValue` (BigDecimal)
- [ ] `discountAmount` (BigDecimal)

### EditPaymentRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/EditPaymentRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/EditPaymentRequest.java:13)

- [ ] `amount` (BigDecimal)
- [ ] `paymentMethod` (PaymentMethod)
- [ ] `paymentDate` (Instant)
- [ ] `reference` (String)
- [ ] `notes` (String)

### InvoicePolicyRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/InvoicePolicyRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/InvoicePolicyRequest.java:19)

- [ ] `clientTypeKey` (String)
- [ ] `clientTypeLabel` (String)
- [ ] `appointmentStatusKey` (String)
- [ ] `appointmentStatusLabel` (String)
- [ ] `enabled` (Boolean)
- [ ] `priceType` (InvoicePolicyPriceType)
- [ ] `invoicePrice` (BigDecimal)
- [ ] `policyName` (String)
- [ ] `serviceId` (Long)
- [ ] `serviceScopeKey` (String)
- [ ] `effectiveFrom` (java.time.LocalDate)
- [ ] `effectiveTo` (java.time.LocalDate)
- [ ] `priority` (Integer)

### RecordPaymentRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/RecordPaymentRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/RecordPaymentRequest.java:18)

- [ ] `clientId` (Long)
- [ ] `paymentAmount` (BigDecimal)
- [ ] `paymentMethod` (PaymentMethod)
- [ ] `referenceNumber` (String)
- [ ] `notes` (String)
- [ ] `paymentDate` (Instant)
- [ ] `paymentSide` (String)
- [ ] `expectedPreviousForSource` (BigDecimal)
- [ ] `allowZeroBillOverpayment` (Boolean)
- [ ] `overrideReason` (String)

### RecordSplitPaymentRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/RecordSplitPaymentRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/RecordSplitPaymentRequest.java:10)

- [ ] `clientLeg` (SplitPaymentLegRequest)
- [ ] `insuranceLeg` (SplitPaymentLegRequest)
- [ ] `notes` (String)
- [ ] `allowZeroBillOverpayment` (Boolean)
- [ ] `overrideReason` (String)

### RefundPaymentRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/RefundPaymentRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/RefundPaymentRequest.java:18)

- [ ] `refundAmount` (BigDecimal)
- [ ] `referenceNumber` (String)
- [ ] `paymentMethod` (PaymentMethod)
- [ ] `notes` (String)

### SplitPaymentLegRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/SplitPaymentLegRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/SplitPaymentLegRequest.java:14)

- [ ] `amount` (BigDecimal)
- [ ] `paymentMethod` (PaymentMethod)
- [ ] `paymentDate` (Instant)
- [ ] `referenceNumber` (String)

### UpdateServiceRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/UpdateServiceRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/UpdateServiceRequest.java:13)

- [ ] `serviceCode` (String)
- [ ] `serviceName` (String)
- [ ] `description` (String)
- [ ] `durationInMinutes` (Integer)
- [ ] `baseRate` (BigDecimal)
- [ ] `isActive` (Boolean)
- [ ] `therapistVisible` (Boolean)
- [ ] `clientPortalVisible` (Boolean)
- [ ] `publicSiteEnabled` (Boolean)

### VoidPaymentTransactionRequest.java

[src/main/java/com/smart/therapy/flow/billing/dto/VoidPaymentTransactionRequest.java:21](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/billing/dto/VoidPaymentTransactionRequest.java:21)

- [ ] `voidReason` (String)

### BookingRequestDto.java

[src/main/java/com/smart/therapy/flow/booking/dto/BookingRequestDto.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/booking/dto/BookingRequestDto.java:16)

- [ ] `id` (Long)
- [ ] `firstName` (String)
- [ ] `lastName` (String)
- [ ] `email` (String)
- [ ] `phone` (String)
- [ ] `practiceName` (String)
- [ ] `servicesOffered` (String)
- [ ] `practiceSize` (String)
- [ ] `country` (String)
- [ ] `address` (String)
- [ ] `status` (BookingRequestStatus)
- [ ] `statusMessage` (String)
- [ ] `createdAt` (Instant)

### CreateBookingRequestDto.java

[src/main/java/com/smart/therapy/flow/booking/dto/CreateBookingRequestDto.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/booking/dto/CreateBookingRequestDto.java:14)

- [ ] `firstName` (String)
- [ ] `lastName` (String)
- [ ] `email` (String)
- [ ] `phone` (String)
- [ ] `practiceName` (String)
- [ ] `servicesOffered` (String)
- [ ] `practiceSize` (String)
- [ ] `country` (String)
- [ ] `address` (String)
- [ ] `agreeToTerms` (Boolean)

### BulkPortalAccessRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/BulkPortalAccessRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/BulkPortalAccessRequest.java:12)

- [ ] `clientIds` (List<Long>)
- [ ] `enable` (Boolean)

### BulkReassignTherapistRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/BulkReassignTherapistRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/BulkReassignTherapistRequest.java:11)

- [ ] `clientIds` (List<Long>)
- [ ] `therapistIds` (List<Long>)
- [ ] `distribution` (String)

### BulkUpdateStageRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/BulkUpdateStageRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/BulkUpdateStageRequest.java:12)

- [ ] `clientIds` (List<Long>)
- [ ] `stage` (String)

### BulkUpdateStatusRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/BulkUpdateStatusRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/BulkUpdateStatusRequest.java:12)

- [ ] `clientIds` (List<Long>)
- [ ] `status` (String)

### BulkUploadRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/BulkUploadRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/BulkUploadRequest.java:10)

- [ ] `clients` (List<Map<String, Object>>)

### ClientAddressRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/ClientAddressRequest.java:26](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/ClientAddressRequest.java:26)

- [ ] `addressType` (AddressType)
- [ ] `streetAddress1` (String)
- [ ] `streetAddress2` (String)
- [ ] `city` (String)
- [ ] `stateProvince` (String)
- [ ] `postalCode` (String)
- [ ] `country` (String)
- [ ] `isPrimary` (Boolean)
- [ ] `validFrom` (LocalDate)
- [ ] `validUntil` (LocalDate)

### ClientContactRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/ClientContactRequest.java:23](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/ClientContactRequest.java:23)

- [ ] `contactType` (ContactType)
- [ ] `contactValue` (String)
- [ ] `isPrimary` (Boolean)
- [ ] `isVerified` (Boolean)
- [ ] `contactPersonName` (String)
- [ ] `relationship` (String)

### ClientEmploymentRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/ClientEmploymentRequest.java:22](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/ClientEmploymentRequest.java:22)

- [ ] `employmentStatus` (String)
- [ ] `employerName` (String)
- [ ] `jobTitle` (String)
- [ ] `employmentStartDate` (LocalDate)
- [ ] `employmentEndDate` (LocalDate)
- [ ] `isCurrentlyEmployed` (Boolean)
- [ ] `educationLevel` (String)
- [ ] `fieldOfStudy` (String)
- [ ] `schoolName` (String)
- [ ] `graduationYear` (Integer)
- [ ] `isStudent` (Boolean)
- [ ] `occupationCategory` (String)
- [ ] `workHoursPerWeek` (Integer)
- [ ] `shiftWork` (Boolean)
- [ ] `remoteWork` (Boolean)
- [ ] `annualIncome` (BigDecimal)
- [ ] `householdIncome` (BigDecimal)
- [ ] `dependents` (Integer)
- [ ] `householdSize` (Integer)
- [ ] `financialHardship` (Boolean)
- [ ] `eligibleForSlidingScale` (Boolean)
- [ ] `slidingScalePercentage` (BigDecimal)
- [ ] `disabilityStatus` (String)
- [ ] `veteranStatus` (Boolean)
- [ ] `militaryBranch` (String)
- [ ] `militaryServiceYears` (Integer)
- [ ] `notes` (String)

### ClientFilter.java

[src/main/java/com/smart/therapy/flow/client/dto/ClientFilter.java:21](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/ClientFilter.java:21)

- [ ] `search` (String)
- [ ] `status` (String)
- [ ] `stage` (String)
- [ ] `therapistId` (Long)
- [ ] `clientType` (String)
- [ ] `hasPortalAccess` (Boolean)
- [ ] `hasPendingTasks` (Boolean)
- [ ] `hasNoSessions` (Boolean)
- [ ] `needsFollowUp` (Boolean)
- [ ] `unassigned` (Boolean)
- [ ] `includeUnassigned` (Boolean)
- [ ] `checklistTemplateId` (Long)
- [ ] `reportTemplateId` (Long)

### ClientFiltersBatchResponse.java

[src/main/java/com/smart/therapy/flow/client/dto/ClientFiltersBatchResponse.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/ClientFiltersBatchResponse.java:18)

- [ ] `therapists` (List<UserResponse>)
- [ ] `checklistTemplates` (List<ChecklistTemplateResponse>)
- [ ] `systemOptions` (Map<String, SystemOptionCategory>)
- [ ] `category` (OptionCategoryResponse)
- [ ] `options` (List<SystemOptionResponse>)

### ClientInsuranceRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/ClientInsuranceRequest.java:22](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/ClientInsuranceRequest.java:22)

- [ ] `insuranceProvider` (String)
- [ ] `insuranceType` (String)
- [ ] `policyNumber` (String)
- [ ] `groupNumber` (String)
- [ ] `subscriberName` (String)
- [ ] `subscriberRelationship` (String)
- [ ] `insurancePhone` (String)
- [ ] `insuranceEmail` (String)
- [ ] `copayAmount` (BigDecimal)
- [ ] `deductible` (BigDecimal)
- [ ] `outOfPocketMax` (BigDecimal)
- [ ] `coveragePercentage` (BigDecimal)
- [ ] `effectiveDate` (LocalDate)
- [ ] `expiryDate` (LocalDate)
- [ ] `isActive` (Boolean)
- [ ] `isVerified` (Boolean)
- [ ] `authorizationRequired` (Boolean)
- [ ] `authorizationNumber` (String)
- [ ] `authorizationExpiresAt` (LocalDate)
- [ ] `mentalHealthCoverage` (Boolean)
- [ ] `telehealthCoverage` (Boolean)
- [ ] `sessionsPerYear` (Integer)
- [ ] `notes` (String)

### ClientReferralRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/ClientReferralRequest.java:22](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/ClientReferralRequest.java:22)

- [ ] `referralDate` (LocalDate)
- [ ] `referralSource` (String)
- [ ] `referralType` (String)
- [ ] `referrerName` (String)
- [ ] `referrerTitle` (String)
- [ ] `referrerOrganization` (String)
- [ ] `referrerPhone` (String)
- [ ] `referrerEmail` (String)
- [ ] `referenceNumber` (String)
- [ ] `clientSource` (String)
- [ ] `isCourtOrdered` (Boolean)
- [ ] `courtOrderNumber` (String)
- [ ] `courtJurisdiction` (String)
- [ ] `requiresReporting` (Boolean)
- [ ] `reportingFrequency` (String)
- [ ] `reportingRecipient` (String)
- [ ] `consentToContactReferrer` (Boolean)
- [ ] `marketingCampaign` (String)
- [ ] `promoCode` (String)
- [ ] `referralNotes` (String)
- [ ] `intakeSummary` (String)

### CreateClientRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/CreateClientRequest.java:24](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/CreateClientRequest.java:24)

- [ ] `fullName` (String)
- [ ] `email` (String)
- [ ] `phone` (String)
- [ ] `dateOfBirth` (LocalDate)
- [ ] `gender` (String)
- [ ] `maritalStatus` (String)
- [ ] `preferredLanguage` (String)
- [ ] `pronouns` (String)
- [ ] `timezone` (String)
- [ ] `status` (String)
- [ ] `stage` (String)
- [ ] `clientType` (String)
- [ ] `assignedTherapistId` (Long)
- [ ] `streetAddress1` (String)
- [ ] `streetAddress2` (String)
- [ ] `city` (String)
- [ ] `province` (String)
- [ ] `postalCode` (String)
- [ ] `country` (String)
- [ ] `addressLegacy` (String)
- [ ] `stateLegacy` (String)
- [ ] `zipCodeLegacy` (String)
- [ ] `emergencyContactName` (String)
- [ ] `emergencyContactPhone` (String)
- [ ] `emergencyContactRelationship` (String)
- [ ] `emergencyContactLegacy` (String)
- [ ] `insuranceProvider` (String)
- [ ] `insuranceType` (String)
- [ ] `policyNumber` (String)
- [ ] `groupNumber` (String)
- [ ] `insurancePhone` (String)
- [ ] `copayAmount` (BigDecimal)
- [ ] `deductible` (BigDecimal)
- [ ] `referrerName` (String)
- [ ] `referralDate` (LocalDate)
- [ ] `referenceNumber` (String)
- [ ] `clientSource` (String)
- [ ] `legacyReferral` (String)
- [ ] `referringPersonName` (String)
- [ ] `referralType` (String)
- [ ] `referralNotes` (String)
- [ ] `hasPortalAccess` (Boolean)
- [ ] `portalEmail` (String)
- [ ] `emailNotifications` (Boolean)
- [ ] `notes` (String)
- [ ] `generalNotes` (String)
- [ ] `serviceType` (String)
- [ ] `serviceFrequency` (String)
- [ ] `treatmentModality` (String)
- [ ] `startDate` (LocalDate)
- [ ] `needsFollowUp` (Boolean)
- [ ] `priority` (String)
- [ ] `followUpDate` (LocalDate)
- [ ] `followUpNotes` (String)
- [ ] `employmentStatus` (String)
- [ ] `educationLevel` (String)
- [ ] `dependents` (Integer)
- [ ] `idempotencyKey` (String)

### MarkDuplicateRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/MarkDuplicateRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/MarkDuplicateRequest.java:9)

- [ ] `duplicateOfClientId` (Long)

### PortalAccessRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/PortalAccessRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/PortalAccessRequest.java:11)

- [ ] `enable` (Boolean)
- [ ] `email` (String)

### StaffRecordConsentRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/StaffRecordConsentRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/StaffRecordConsentRequest.java:14)

- [ ] `consentType` (ConsentType)
- [ ] `granted` (Boolean)
- [ ] `consentVersion` (String)
- [ ] `source` (String)
- [ ] `notes` (String)
- [ ] `auditReason` (String)

### UpdateClientRequest.java

[src/main/java/com/smart/therapy/flow/client/dto/UpdateClientRequest.java:28](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/dto/UpdateClientRequest.java:28)

- [ ] `fullName` (String)
- [ ] `email` (String)
- [ ] `phone` (String)
- [ ] `dateOfBirth` (LocalDate)
- [ ] `gender` (String)
- [ ] `maritalStatus` (String)
- [ ] `preferredLanguage` (String)
- [ ] `pronouns` (String)
- [ ] `timezone` (String)
- [ ] `status` (String)
- [ ] `stage` (String)
- [ ] `clientType` (String)
- [ ] `assignedTherapistId` (Long)
- [ ] `streetAddress1` (String)
- [ ] `streetAddress2` (String)
- [ ] `city` (String)
- [ ] `province` (String)
- [ ] `postalCode` (String)
- [ ] `country` (String)
- [ ] `addressLegacy` (String)
- [ ] `stateLegacy` (String)
- [ ] `zipCodeLegacy` (String)
- [ ] `emergencyContactName` (String)
- [ ] `emergencyContactPhone` (String)
- [ ] `emergencyContactRelationship` (String)
- [ ] `emergencyContactLegacy` (String)
- [ ] `insuranceProvider` (String)
- [ ] `insuranceType` (String)
- [ ] `policyNumber` (String)
- [ ] `groupNumber` (String)
- [ ] `insurancePhone` (String)
- [ ] `copayAmount` (BigDecimal)
- [ ] `deductible` (BigDecimal)
- [ ] `referrerName` (String)
- [ ] `referralDate` (LocalDate)
- [ ] `referenceNumber` (String)
- [ ] `clientSource` (String)
- [ ] `legacyReferral` (String)
- [ ] `referringPersonName` (String)
- [ ] `referralType` (String)
- [ ] `referralNotes` (String)
- [ ] `hasPortalAccess` (Boolean)
- [ ] `portalEmail` (String)
- [ ] `emailNotifications` (Boolean)
- [ ] `notes` (String)
- [ ] `generalNotes` (String)
- [ ] `serviceType` (String)
- [ ] `serviceFrequency` (String)
- [ ] `treatmentModality` (String)
- [ ] `startDate` (LocalDate)
- [ ] `needsFollowUp` (Boolean)
- [ ] `priority` (String)
- [ ] `followUpDate` (LocalDate)
- [ ] `followUpNotes` (String)
- [ ] `employmentStatus` (String)
- [ ] `educationLevel` (String)
- [ ] `dependents` (Integer)

### BookAppointmentRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/BookAppointmentRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/BookAppointmentRequest.java:12)

- [ ] `sessionStartUtc` (String)
- [ ] `duration` (Integer)
- [ ] `serviceId` (Long)
- [ ] `sessionType` (String)
- [ ] `location` (String)

### ForgotPasswordRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/ForgotPasswordRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/ForgotPasswordRequest.java:15)

- [ ] `email` (String)
- [ ] `orgIdentifier` (String)
- [ ] `orgValue` (String)
- [ ] `orgId` (String)
- [ ] `orgSlug` (String)

### GrantConsentRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/GrantConsentRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/GrantConsentRequest.java:13)

- [ ] `consentType` (String)
- [ ] `granted` (Boolean)
- [ ] `consentVersion` (String)

### PortalActivateRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/PortalActivateRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/PortalActivateRequest.java:12)

- [ ] `token` (String)
- [ ] `password` (String)

### PortalLoginRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/PortalLoginRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/PortalLoginRequest.java:15)

- [ ] `email` (String)
- [ ] `password` (String)
- [ ] `orgIdentifier` (String)
- [ ] `orgValue` (String)
- [ ] `orgId` (String)
- [ ] `orgSlug` (String)
- [ ] `deviceTrustToken` (String)
- [ ] `trustDevice` (Boolean)
- [ ] `staySignedIn` (Boolean)

### RateSessionRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/RateSessionRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/RateSessionRequest.java:16)

- [ ] `rating` (Integer)
- [ ] `comment` (String)

### RescheduleAppointmentRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/RescheduleAppointmentRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/RescheduleAppointmentRequest.java:14)

- [ ] `newSessionStartUtc` (String)
- [ ] `duration` (Integer)

### ResetPasswordRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/ResetPasswordRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/ResetPasswordRequest.java:12)

- [ ] `token` (String)
- [ ] `password` (String)

### ToggleConsentRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/ToggleConsentRequest.java:25](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/ToggleConsentRequest.java:25)

- [ ] `consentType` (ConsentType)
- [ ] `granted` (Boolean)
- [ ] `consentVersion` (String)

### WithdrawConsentRequest.java

[src/main/java/com/smart/therapy/flow/client/portal/dto/WithdrawConsentRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/portal/dto/WithdrawConsentRequest.java:12)

- [ ] `consentType` (String)

### CmsGlobalSettingsUpdateRequest.java

[src/main/java/com/smart/therapy/flow/cms/dto/CmsGlobalSettingsUpdateRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/dto/CmsGlobalSettingsUpdateRequest.java:16)

- [ ] `content` (JsonNode)

### CmsLandingPageUpdateRequest.java

[src/main/java/com/smart/therapy/flow/cms/dto/CmsLandingPageUpdateRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/dto/CmsLandingPageUpdateRequest.java:16)

- [ ] `draftContent` (JsonNode)

### CmsLearningHubArticleUpsertRequest.java

[src/main/java/com/smart/therapy/flow/cms/dto/CmsLearningHubArticleUpsertRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/cms/dto/CmsLearningHubArticleUpsertRequest.java:17)

- [ ] `title` (String)
- [ ] `slug` (String)
- [ ] `draftContent` (JsonNode)

### PublicBookConsultationRequest.java

[src/main/java/com/smart/therapy/flow/consultation/dto/PublicBookConsultationRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/consultation/dto/PublicBookConsultationRequest.java:14)

- [ ] `therapistId` (Long)
- [ ] `sessionStartUtc` (Instant)
- [ ] `clientFullName` (String)
- [ ] `clientEmail` (String)
- [ ] `clientPhone` (String)
- [ ] `sessionMode` (String)
- [ ] `notes` (String)
- [ ] `clientTimezone` (String)
- [ ] `publicServiceId` (Long)
- [ ] `publicServiceSlug` (String)

### AssignMultipleFormsToClientRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/AssignMultipleFormsToClientRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/AssignMultipleFormsToClientRequest.java:13)

- [ ] `clientId` (Long)
- [ ] `templateIds` (List<Long>)
- [ ] `dueDate` (Instant)
- [ ] `instructions` (String)

### BulkAssignFormRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/BulkAssignFormRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/BulkAssignFormRequest.java:13)

- [ ] `templateId` (Long)
- [ ] `clientIds` (List<Long>)
- [ ] `dueDate` (Instant)
- [ ] `instructions` (String)

### BulkTagRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/BulkTagRequest.java:20](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/BulkTagRequest.java:20)

- [ ] `entryIds` (List<Long>)
- [ ] `tagsToAdd` (List<String>)
- [ ] `tagsToRemove` (List<String>)

### CreateDocumentRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateDocumentRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateDocumentRequest.java:13)

- [ ] `clientId` (Long)
- [ ] `documentType` (String)
- [ ] `category` (String)
- [ ] `description` (String)
- [ ] `needsReview` (Boolean)
- [ ] `shareWithClient` (Boolean)

### CreateFormAssignmentRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateFormAssignmentRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateFormAssignmentRequest.java:11)

- [ ] `templateId` (Long)
- [ ] `clientId` (Long)
- [ ] `dueDate` (Instant)
- [ ] `instructions` (String)

### CreateFormFieldInTemplateRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateFormFieldInTemplateRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateFormFieldInTemplateRequest.java:11)

- [ ] `templateId` (Long)
- [ ] `fieldType` (String)
- [ ] `label` (String)
- [ ] `placeholder` (String)
- [ ] `helpText` (String)
- [ ] `isRequired` (Boolean)
- [ ] `options` (String)
- [ ] `validation` (String)
- [ ] `defaultValue` (String)
- [ ] `autoPopulate` (String)
- [ ] `conditionalDisplay` (String)
- [ ] `sortOrder` (Integer)

### CreateFormFieldRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateFormFieldRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateFormFieldRequest.java:11)

- [ ] `templateId` (Long)
- [ ] `fieldType` (String)
- [ ] `label` (String)
- [ ] `placeholder` (String)
- [ ] `helpText` (String)
- [ ] `isRequired` (Boolean)
- [ ] `options` (String)
- [ ] `validation` (String)
- [ ] `defaultValue` (String)
- [ ] `autoPopulate` (String)
- [ ] `conditionalDisplay` (String)
- [ ] `sortOrder` (Integer)

### CreateFormTemplateRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateFormTemplateRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateFormTemplateRequest.java:15)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `category` (String)
- [ ] `instructions` (String)
- [ ] `requiresSignature` (Boolean)
- [ ] `isActive` (Boolean)
- [ ] `isSystemTemplate` (Boolean)
- [ ] `sortOrder` (Integer)
- [ ] `fields` (List<CreateFormFieldInTemplateRequest>)

### CreateLibraryCategoryRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateLibraryCategoryRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateLibraryCategoryRequest.java:9)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `parentId` (Long)
- [ ] `sortOrder` (Integer)
- [ ] `isActive` (Boolean)

### CreateLibraryEntryRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateLibraryEntryRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateLibraryEntryRequest.java:12)

- [ ] `categoryId` (Long)
- [ ] `title` (String)
- [ ] `content` (String)
- [ ] `tags` (List<String>)
- [ ] `sortOrder` (Integer)
- [ ] `isActive` (Boolean)

### CreateNoteRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateNoteRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateNoteRequest.java:14)

- [ ] `clientId` (Long)
- [ ] `title` (String)
- [ ] `content` (String)
- [ ] `noteType` (String)
- [ ] `eventDate` (Instant)
- [ ] `isPrivate` (Boolean)

### CreateTagRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/CreateTagRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/CreateTagRequest.java:15)

- [ ] `name` (String)

### FormAssignmentFilterRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/FormAssignmentFilterRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/FormAssignmentFilterRequest.java:11)

- [ ] `clientId` (Long)
- [ ] `templateId` (Long)
- [ ] `versionStatus` (FormTemplateVersionStatus)
- [ ] `assignmentStatus` (Status)
- [ ] `assignmentDateFrom` (Instant)
- [ ] `assignmentDateTo` (Instant)
- [ ] `dueDateFrom` (Instant)
- [ ] `dueDateTo` (Instant)

### LibraryConnectedEntriesBulkRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/LibraryConnectedEntriesBulkRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/LibraryConnectedEntriesBulkRequest.java:12)

- [ ] `entryIds` (List<Long>)

### LibraryConnectionRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/LibraryConnectionRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/LibraryConnectionRequest.java:14)

- [ ] `fromEntryId` (Long)
- [ ] `toEntryId` (Long)
- [ ] `connectionType` (ConnectionType)
- [ ] `strength` (Integer)
- [ ] `description` (String)

### LibraryConnectionUpdateRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/LibraryConnectionUpdateRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/LibraryConnectionUpdateRequest.java:11)

- [ ] `fromEntryId` (Long)
- [ ] `toEntryId` (Long)
- [ ] `connectionType` (ConnectionType)
- [ ] `strength` (Integer)
- [ ] `description` (String)
- [ ] `active` (Boolean)

### LibraryEntryBulkRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/LibraryEntryBulkRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/LibraryEntryBulkRequest.java:15)

- [ ] `categoryId` (Long)
- [ ] `domain` (String)
- [ ] `subdomain` (String)
- [ ] `title` (String)
- [ ] `content` (String)
- [ ] `tags` (List<String>)
- [ ] `sortOrder` (Integer)

### ReorderFormFieldsRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/ReorderFormFieldsRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/ReorderFormFieldsRequest.java:16)

- [ ] `fieldIds` (List<Long>)

### ReviewDocumentRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/ReviewDocumentRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/ReviewDocumentRequest.java:17)

- [ ] `reviewStatus` (ReviewStatus)
- [ ] `action` (String)
- [ ] `reviewNotes` (String)

### ReviewFormAssignmentRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/ReviewFormAssignmentRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/ReviewFormAssignmentRequest.java:9)

- [ ] `assignmentId` (Long)
- [ ] `reviewNotes` (String)
- [ ] `approved` (Boolean)

### ShareDocumentRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/ShareDocumentRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/ShareDocumentRequest.java:13)

- [ ] `shareWithClient` (Boolean)

### SubmitFormResponseRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/SubmitFormResponseRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/SubmitFormResponseRequest.java:11)

- [ ] `assignmentId` (Long)
- [ ] `responses` (Map<Long, String>)

### SubmitFormSignatureRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/SubmitFormSignatureRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/SubmitFormSignatureRequest.java:10)

- [ ] `assignmentId` (Long)
- [ ] `signatureData` (String)
- [ ] `signerName` (String)
- [ ] `signerRole` (String)
- [ ] `agreedToTerms` (Boolean)

### UpdateFormAssignmentRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/UpdateFormAssignmentRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/UpdateFormAssignmentRequest.java:16)

- [ ] `dueDate` (Instant)
- [ ] `instructions` (String)
- [ ] `status` (String)

### UpdateFormAssignmentStatusRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/UpdateFormAssignmentStatusRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/UpdateFormAssignmentStatusRequest.java:10)

- [ ] `status` (String)

### UpdateFormFieldRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/UpdateFormFieldRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/UpdateFormFieldRequest.java:14)

- [ ] `fieldType` (String)
- [ ] `label` (String)
- [ ] `placeholder` (String)
- [ ] `helpText` (String)
- [ ] `isRequired` (Boolean)
- [ ] `options` (String)
- [ ] `validation` (String)
- [ ] `defaultValue` (String)
- [ ] `autoPopulate` (String)
- [ ] `conditionalDisplay` (String)
- [ ] `sortOrder` (Integer)

### UpdateFormResponseRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/UpdateFormResponseRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/UpdateFormResponseRequest.java:14)

- [ ] `value` (String)

### UpdateFormSectionRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/UpdateFormSectionRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/UpdateFormSectionRequest.java:14)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `sortOrder` (Integer)

### UpdateFormTemplateRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/UpdateFormTemplateRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/UpdateFormTemplateRequest.java:19)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `category` (String)
- [ ] `instructions` (String)
- [ ] `requiresSignature` (Boolean)
- [ ] `isActive` (Boolean)
- [ ] `isSystemTemplate` (Boolean)
- [ ] `sortOrder` (Integer)
- [ ] `fields` (List<CreateFormFieldInTemplateRequest>)

### UpdateNoteRequest.java

[src/main/java/com/smart/therapy/flow/document/dto/UpdateNoteRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/document/dto/UpdateNoteRequest.java:12)

- [ ] `title` (String)
- [ ] `content` (String)
- [ ] `noteType` (String)
- [ ] `eventDate` (Instant)
- [ ] `isPrivate` (Boolean)

### ZoomMeetingRequest.java

[src/main/java/com/smart/therapy/flow/integration/zoom/dto/ZoomMeetingRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/integration/zoom/dto/ZoomMeetingRequest.java:15)

- [ ] `clientName` (String)
- [ ] `therapistName` (String)
- [ ] `sessionDate` (Instant)
- [ ] `duration` (Integer)
- [ ] `timezone` (String)

### NotificationActionMetadataUpsertRequest.java

[src/main/java/com/smart/therapy/flow/notification/dto/NotificationActionMetadataUpsertRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/dto/NotificationActionMetadataUpsertRequest.java:17)

- [ ] `actionUrlTemplate` (String)
- [ ] `defaultActionLabel` (String)
- [ ] `exampleActionUrl` (String)
- [ ] `sortOrder` (Integer)
- [ ] `isActive` (Boolean)

### NotificationBroadcastRequest.java

[src/main/java/com/smart/therapy/flow/notification/dto/NotificationBroadcastRequest.java:21](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/dto/NotificationBroadcastRequest.java:21)

- [ ] `targetType` (NotificationTargetType)
- [ ] `type` (NotificationType)
- [ ] `category` (NotificationCategory)
- [ ] `title` (String)
- [ ] `message` (String)
- [ ] `data` (String)
- [ ] `priority` (String)
- [ ] `actionUrl` (String)
- [ ] `actionLabel` (String)
- [ ] `relatedEntityType` (String)
- [ ] `relatedEntityId` (Long)
- [ ] `expiresAt` (Instant)

### NotificationPreferenceRequest.java

[src/main/java/com/smart/therapy/flow/notification/dto/NotificationPreferenceRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/dto/NotificationPreferenceRequest.java:13)

- [ ] `notificationType` (com.smart.therapy.flow.notification.enums.NotificationType)
- [ ] `emailEnabled` (Boolean)
- [ ] `smsEnabled` (Boolean)
- [ ] `pushEnabled` (Boolean)
- [ ] `inAppEnabled` (Boolean)
- [ ] `timing` (com.smart.therapy.flow.notification.enums.NotificationTiming)
- [ ] `quietHoursStart` (java.time.Instant)
- [ ] `quietHoursEnd` (java.time.Instant)
- [ ] `weekendsEnabled` (Boolean)

### NotificationRequest.java

[src/main/java/com/smart/therapy/flow/notification/dto/NotificationRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/dto/NotificationRequest.java:19)

- [ ] `userId` (Long)
- [ ] `clientId` (Long)
- [ ] `type` (com.smart.therapy.flow.notification.enums.NotificationType)
- [ ] `category` (com.smart.therapy.flow.notification.enums.NotificationCategory)
- [ ] `title` (String)
- [ ] `message` (String)
- [ ] `data` (String)
- [ ] `priority` (String)
- [ ] `actionUrl` (String)
- [ ] `actionLabel` (String)
- [ ] `relatedEntityType` (String)
- [ ] `relatedEntityId` (Long)
- [ ] `expiresAt` (java.time.Instant)

### NotificationTemplateRequest.java

[src/main/java/com/smart/therapy/flow/notification/dto/NotificationTemplateRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/dto/NotificationTemplateRequest.java:10)

- [ ] `name` (String)
- [ ] `type` (String)
- [ ] `eventType` (String)
- [ ] `subject` (String)
- [ ] `bodyTemplate` (String)
- [ ] `isSystem` (Boolean)
- [ ] `isActive` (Boolean)

### NotificationTriggerRequest.java

[src/main/java/com/smart/therapy/flow/notification/dto/NotificationTriggerRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/dto/NotificationTriggerRequest.java:15)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `eventType` (String)
- [ ] `entityType` (com.smart.therapy.flow.notification.enums.EntityType)
- [ ] `conditionRules` (String)
- [ ] `recipientRules` (String)
- [ ] `priority` (String)
- [ ] `isScheduled` (Boolean)
- [ ] `scheduleOffsetMinutes` (Integer)
- [ ] `batchWindowMinutes` (Integer)
- [ ] `maxBatchSize` (Integer)
- [ ] `isActive` (Boolean)

### TenantStripeConfigUpsertRequest.java

[src/main/java/com/smart/therapy/flow/payment/dto/TenantStripeConfigUpsertRequest.java:7](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/payment/dto/TenantStripeConfigUpsertRequest.java:7)

- [ ] `publishableKey` (String)
- [ ] `secretKey` (String)
- [ ] `webhookEndpointUrl` (String)
- [ ] `webhookSecret` (String)

### CreatePublicSiteServiceRequest.java

[src/main/java/com/smart/therapy/flow/publicsite/dto/CreatePublicSiteServiceRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/publicsite/dto/CreatePublicSiteServiceRequest.java:16)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `enabled` (Boolean)
- [ ] `displayOrder` (Integer)
- [ ] `durationMinutes` (Integer)
- [ ] `baseRate` (BigDecimal)

### UpdatePublicSiteServiceRequest.java

[src/main/java/com/smart/therapy/flow/publicsite/dto/UpdatePublicSiteServiceRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/publicsite/dto/UpdatePublicSiteServiceRequest.java:14)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `enabled` (Boolean)
- [ ] `displayOrder` (Integer)
- [ ] `durationMinutes` (Integer)
- [ ] `baseRate` (BigDecimal)

### CreateReportTemplateRequest.java

[src/main/java/com/smart/therapy/flow/report/dto/CreateReportTemplateRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/dto/CreateReportTemplateRequest.java:12)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `aiInstructions` (String)
- [ ] `fileContent` (String)
- [ ] `originalName` (String)
- [ ] `mimeType` (String)
- [ ] `defaultIncludeProfile` (Boolean)
- [ ] `defaultIncludeNotes` (Boolean)
- [ ] `defaultIncludeAssessments` (Boolean)
- [ ] `supportingFilesGuidance` (String)
- [ ] `supportingFilesExpected` (Boolean)
- [ ] `supportingFileTypes` (List<String>)

### GenerateClientReportRequest.java

[src/main/java/com/smart/therapy/flow/report/dto/GenerateClientReportRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/dto/GenerateClientReportRequest.java:9)

- [ ] `templateId` (Long)
- [ ] `sources` (ReportSources)
- [ ] `supportingFileIds` (List<Long>)
- [ ] `includeProfile` (Boolean)
- [ ] `includeNotes` (Boolean)
- [ ] `includeAssessments` (Boolean)

### UpdateClientReportDraftRequest.java

[src/main/java/com/smart/therapy/flow/report/dto/UpdateClientReportDraftRequest.java:7](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/dto/UpdateClientReportDraftRequest.java:7)

- [ ] `draftContent` (String)

### UpdateReportTemplateRequest.java

[src/main/java/com/smart/therapy/flow/report/dto/UpdateReportTemplateRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/dto/UpdateReportTemplateRequest.java:9)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `aiInstructions` (String)
- [ ] `structureText` (String)
- [ ] `isActive` (Boolean)
- [ ] `defaultIncludeProfile` (Boolean)
- [ ] `defaultIncludeNotes` (Boolean)
- [ ] `defaultIncludeAssessments` (Boolean)
- [ ] `supportingFilesGuidance` (String)
- [ ] `supportingFilesExpected` (Boolean)
- [ ] `supportingFileTypes` (List<String>)

### UploadSupportingFileRequest.java

[src/main/java/com/smart/therapy/flow/report/dto/UploadSupportingFileRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/report/dto/UploadSupportingFileRequest.java:9)

- [ ] `fileContent` (String)
- [ ] `originalName` (String)
- [ ] `mimeType` (String)
- [ ] `documentType` (String)
- [ ] `templateId` (Long)

### BulkUploadSessionsRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/BulkUploadSessionsRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/BulkUploadSessionsRequest.java:12)

- [ ] `sessions` (List<Map<String, Object>>)

### CreateRecurringSessionsRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/CreateRecurringSessionsRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/CreateRecurringSessionsRequest.java:16)

- [ ] `session` (CreateSessionRequest)
- [ ] `occurrences` (Integer)
- [ ] `endDate` (LocalDate)

### CreateSessionNoteAiTemplateRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/CreateSessionNoteAiTemplateRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/CreateSessionNoteAiTemplateRequest.java:12)

- [ ] `name` (String)
- [ ] `instructions` (String)

### CreateSessionNoteAmendmentRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/CreateSessionNoteAmendmentRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/CreateSessionNoteAmendmentRequest.java:10)

- [ ] `amendmentText` (String)
- [ ] `reason` (String)

### CreateSessionNoteRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/CreateSessionNoteRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/CreateSessionNoteRequest.java:16)

- [ ] `sessionId` (Long)
- [ ] `clientId` (Long)
- [ ] `therapistId` (Long)
- [ ] `date` (Instant)
- [ ] `sessionFocus` (String)
- [ ] `symptoms` (String)
- [ ] `shortTermGoals` (String)
- [ ] `intervention` (String)
- [ ] `progress` (String)
- [ ] `remarks` (String)
- [ ] `recommendations` (String)
- [ ] `clientRating` (Integer)
- [ ] `therapistRating` (Integer)
- [ ] `progressTowardGoals` (Integer)
- [ ] `moodBefore` (Integer)
- [ ] `moodAfter` (Integer)
- [ ] `riskSuicidalIdeation` (Integer)
- [ ] `riskSelfHarm` (Integer)
- [ ] `riskHomicidalIdeation` (Integer)
- [ ] `riskPsychosis` (Integer)
- [ ] `riskSubstanceUse` (Integer)
- [ ] `riskImpulsivity` (Integer)
- [ ] `riskAggression` (Integer)
- [ ] `riskTraumaSymptoms` (Integer)
- [ ] `riskNonAdherence` (Integer)
- [ ] `riskSupportSystem` (Integer)
- [ ] `generatedContent` (String)
- [ ] `draftContent` (String)
- [ ] `isDraft` (Boolean)
- [ ] `isFinalized` (Boolean)
- [ ] `aiEnabled` (Boolean)
- [ ] `customAiPrompt` (String)

### CreateSessionRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/CreateSessionRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/CreateSessionRequest.java:18)

- [ ] `clientId` (Long)
- [ ] `therapistId` (Long)
- [ ] `sessionDate` (Instant)
- [ ] `sessionMode` (String)
- [ ] `sessionType` (String)
- [ ] `status` (String)
- [ ] `duration` (Integer)
- [ ] `serviceId` (Long)
- [ ] `roomId` (Long)
- [ ] `notes` (String)
- [ ] `zoomEnabled` (Boolean)
- [ ] `ignoreConflicts` (Boolean)
- [ ] `timezone` (String)

### RecurrenceRuleRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/RecurrenceRuleRequest.java:23](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/RecurrenceRuleRequest.java:23)

- [ ] `clientId` (Long)
- [ ] `therapistId` (Long)
- [ ] `serviceId` (Long)
- [ ] `roomId` (Long)
- [ ] `sessionMode` (String)
- [ ] `sessionType` (String)
- [ ] `notes` (String)
- [ ] `zoomEnabled` (Boolean)
- [ ] `sessionDate` (Instant)
- [ ] `timezone` (String)
- [ ] `recurrenceType` (RecurrenceType)
- [ ] `interval` (Integer)
- [ ] `endMode` (RecurrenceEndMode)
- [ ] `count` (Integer)
- [ ] `untilDate` (LocalDate)

### RoomRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/RoomRequest.java:23](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/RoomRequest.java:23)

- [ ] `roomNumber` (String)
- [ ] `roomName` (String)
- [ ] `capacity` (Integer)
- [ ] `equipment` (String)
- [ ] `isActive` (Boolean)
- [ ] `roomType` (RoomType)

### TranscribeAudioRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/TranscribeAudioRequest.java:7](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/TranscribeAudioRequest.java:7)

- [ ] `sessionNoteId` (Long)

### TranscribeFinalizeRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/TranscribeFinalizeRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/TranscribeFinalizeRequest.java:16)

- [ ] `uploadId` (String)
- [ ] `expectedChunks` (Integer)
- [ ] `totalChunks` (Integer)
- [ ] `silentChunks` (List<SilentChunkDto>)

### TranscribeStartRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/TranscribeStartRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/TranscribeStartRequest.java:18)

- [ ] `expectedChunks` (Integer)
- [ ] `language` (String)
- [ ] `translateToEnglish` (Boolean)
- [ ] `retentionDays` (Integer)

### UpdateRecurringFutureRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/UpdateRecurringFutureRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/UpdateRecurringFutureRequest.java:14)

- [ ] `anchorId` (Long)
- [ ] `sessionDate` (Instant)
- [ ] `roomId` (Long)
- [ ] `notes` (String)
- [ ] `serviceId` (Long)
- [ ] `therapistId` (Long)
- [ ] `sessionType` (String)
- [ ] `sessionMode` (String)
- [ ] `zoomEnabled` (Boolean)
- [ ] `ignoreConflicts` (Boolean)

### UpdateSessionNoteAiTemplateRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionNoteAiTemplateRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionNoteAiTemplateRequest.java:10)

- [ ] `name` (String)
- [ ] `instructions` (String)

### UpdateSessionNoteRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionNoteRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionNoteRequest.java:14)

- [ ] `date` (Instant)
- [ ] `sessionFocus` (String)
- [ ] `symptoms` (String)
- [ ] `shortTermGoals` (String)
- [ ] `intervention` (String)
- [ ] `progress` (String)
- [ ] `remarks` (String)
- [ ] `recommendations` (String)
- [ ] `clientRating` (Integer)
- [ ] `therapistRating` (Integer)
- [ ] `progressTowardGoals` (Integer)
- [ ] `moodBefore` (Integer)
- [ ] `moodAfter` (Integer)
- [ ] `riskSuicidalIdeation` (Integer)
- [ ] `riskSelfHarm` (Integer)
- [ ] `riskHomicidalIdeation` (Integer)
- [ ] `riskPsychosis` (Integer)
- [ ] `riskSubstanceUse` (Integer)
- [ ] `riskImpulsivity` (Integer)
- [ ] `riskAggression` (Integer)
- [ ] `riskTraumaSymptoms` (Integer)
- [ ] `riskNonAdherence` (Integer)
- [ ] `riskSupportSystem` (Integer)
- [ ] `generatedContent` (String)
- [ ] `draftContent` (String)
- [ ] `isDraft` (Boolean)
- [ ] `isFinalized` (Boolean)
- [ ] `aiEnabled` (Boolean)
- [ ] `customAiPrompt` (String)
- [ ] `aiProcessingStatus` (String)

### UpdateSessionRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionRequest.java:15)

- [ ] `clientId` (Long)
- [ ] `therapistId` (Long)
- [ ] `sessionDate` (Instant)
- [ ] `sessionMode` (String)
- [ ] `sessionType` (String)
- [ ] `status` (String)
- [ ] `duration` (Integer)
- [ ] `serviceId` (Long)
- [ ] `roomId` (Long)
- [ ] `notes` (String)
- [ ] `zoomEnabled` (Boolean)
- [ ] `timezone` (String)
- [ ] `ignoreConflicts` (Boolean)

### UpdateSessionStatusRequest.java

[src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionStatusRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/dto/UpdateSessionStatusRequest.java:18)

- [ ] `status` (String)

### AddOnCatalogCreateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/AddOnCatalogCreateRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/AddOnCatalogCreateRequest.java:19)

- [ ] `code` (String)
- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `priceUsd` (BigDecimal)
- [ ] `billingCycle` (AddonBillingCycle)
- [ ] `status` (AddonCatalogStatus)

### AddOnCatalogUpdateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/AddOnCatalogUpdateRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/AddOnCatalogUpdateRequest.java:14)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `priceUsd` (BigDecimal)
- [ ] `billingCycle` (AddonBillingCycle)
- [ ] `status` (AddonCatalogStatus)

### AdminPlanEntitlementsRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/AdminPlanEntitlementsRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/AdminPlanEntitlementsRequest.java:13)

- [ ] `features` (List<PlanFeatureItem>)
- [ ] `key` (String)
- [ ] `enabled` (Boolean)
- [ ] `limit` (Integer)

### BreakGlassAccessRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/BreakGlassAccessRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/BreakGlassAccessRequest.java:12)

- [ ] `reason` (String)
- [ ] `resourceType` (String)
- [ ] `resourceId` (String)

### BulkFeatureKeyRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/BulkFeatureKeyRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/BulkFeatureKeyRequest.java:15)

- [ ] `featureKeys` (List<String>)

### CreateBillingExportJobRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/CreateBillingExportJobRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/CreateBillingExportJobRequest.java:16)

- [ ] `exportType` (BillingExportType)
- [ ] `organisationId` (Long)
- [ ] `status` (String)
- [ ] `months` (Integer)

### CreateOrganisationRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/CreateOrganisationRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/CreateOrganisationRequest.java:14)

- [ ] `name` (String)
- [ ] `slug` (String)
- [ ] `status` (String)
- [ ] `subdomain` (String)
- [ ] `timezone` (String)
- [ ] `region` (String)
- [ ] `dataResidency` (String)
- [ ] `provisionTenant` (Boolean)

### CreatePlanRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/CreatePlanRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/CreatePlanRequest.java:17)

- [ ] `code` (String)
- [ ] `name` (String)
- [ ] `billingCycle` (String)
- [ ] `basePrice` (BigDecimal)
- [ ] `annualPrice` (BigDecimal)
- [ ] `description` (String)
- [ ] `trialDays` (Integer)
- [ ] `status` (String)
- [ ] `providerPriceIdMonthly` (String)
- [ ] `providerPriceIdAnnual` (String)

### FeatureCatalogCreateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureCatalogCreateRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureCatalogCreateRequest.java:14)

- [ ] `key` (String)
- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `scope` (String)
- [ ] `type` (String)
- [ ] `defaultEnabled` (Boolean)

### FeatureCatalogUpdateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureCatalogUpdateRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureCatalogUpdateRequest.java:10)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `scope` (String)
- [ ] `defaultEnabled` (Boolean)
- [ ] `deprecated` (Boolean)

### FeatureFlagDisableAllRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureFlagDisableAllRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureFlagDisableAllRequest.java:9)

- [ ] `confirmation` (String)

### FeatureFlagToggleRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureFlagToggleRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureFlagToggleRequest.java:9)

- [ ] `enabled` (Boolean)

### FeatureValueRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureValueRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/FeatureValueRequest.java:9)

- [ ] `enabled` (Boolean)
- [ ] `usageLimit` (Integer)

### InvoiceAdjustmentRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/InvoiceAdjustmentRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/InvoiceAdjustmentRequest.java:18)

- [ ] `amountUsd` (BigDecimal)
- [ ] `reason` (String)

### OrganisationImpersonationRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/OrganisationImpersonationRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/OrganisationImpersonationRequest.java:11)

- [ ] `targetAuthId` (Long)
- [ ] `reason` (String)
- [ ] `durationMinutes` (Integer)

### PlanPricingTierRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/PlanPricingTierRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/PlanPricingTierRequest.java:18)

- [ ] `tiers` (List<TierItem>)
- [ ] `minTherapists` (Integer)
- [ ] `maxTherapists` (Integer)
- [ ] `pricePerTherapistUsd` (BigDecimal)
- [ ] `includedSupervisors` (Integer)
- [ ] `includedClients` (Integer)

### PlatformTenantRoutingRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/PlatformTenantRoutingRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/PlatformTenantRoutingRequest.java:10)

- [ ] `emailAutoRouting` (Boolean)
- [ ] `pathBasedRouting` (Boolean)
- [ ] `pathPrefix` (String)
- [ ] `orgIdentifier` (String)

### RolePermissionToggleRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/RolePermissionToggleRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/RolePermissionToggleRequest.java:10)

- [ ] `roleName` (String)
- [ ] `permissionName` (String)
- [ ] `granted` (Boolean)

### RolesPermissionsMatrixUpdateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/RolesPermissionsMatrixUpdateRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/RolesPermissionsMatrixUpdateRequest.java:16)

- [ ] `updates` (List<UpdateItem>)
- [ ] `roleName` (String)
- [ ] `permissionName` (String)
- [ ] `granted` (Boolean)

### RolloutRuleRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/RolloutRuleRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/RolloutRuleRequest.java:11)

- [ ] `scope` (String)
- [ ] `targetId` (Long)
- [ ] `targetKey` (String)
- [ ] `featureKey` (String)
- [ ] `enabled` (Boolean)
- [ ] `usageLimit` (Integer)
- [ ] `startAt` (Instant)
- [ ] `endAt` (Instant)

### SuperAdminBulkActionRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminBulkActionRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminBulkActionRequest.java:14)

- [ ] `action` (String)
- [ ] `organisationIds` (List<Long>)
- [ ] `reason` (String)

### SuperAdminCreateApiKeyRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminCreateApiKeyRequest.java:19](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminCreateApiKeyRequest.java:19)

- [ ] `keyName` (String)
- [ ] `scopes` (List<String>)
- [ ] `expiresAt` (Instant)

### SuperAdminCreateIncidentRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminCreateIncidentRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminCreateIncidentRequest.java:11)

- [ ] `title` (String)
- [ ] `description` (String)
- [ ] `serviceName` (String)
- [ ] `severity` (String)

### SuperAdminCreateRoleRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminCreateRoleRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminCreateRoleRequest.java:15)

- [ ] `name` (String)
- [ ] `displayName` (String)
- [ ] `description` (String)
- [ ] `organisationId` (Long)
- [ ] `isActive` (Boolean)
- [ ] `permissions` (List<Long>)

### SuperAdminDashboardTierAliasesReplaceRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminDashboardTierAliasesReplaceRequest.java:20](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminDashboardTierAliasesReplaceRequest.java:20)

- [ ] `aliases` (List<Item>)
- [ ] `tierName` (String)
- [ ] `planCode` (String)

### SuperAdminDisableUserRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminDisableUserRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminDisableUserRequest.java:11)

- [ ] `reason` (String)

### SuperAdminExchangeImpersonationRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminExchangeImpersonationRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminExchangeImpersonationRequest.java:9)

- [ ] `impersonationToken` (String)

### SuperAdminForcePasswordResetRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminForcePasswordResetRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminForcePasswordResetRequest.java:14)

- [ ] `effectiveAt` (Instant)
- [ ] `reason` (String)

### SuperAdminIntegrationUpsertRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminIntegrationUpsertRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminIntegrationUpsertRequest.java:9)

- [ ] `enabled` (Boolean)
- [ ] `clientId` (String)
- [ ] `publishableKey` (String)
- [ ] `secret` (String)
- [ ] `connectClientSecret` (String)
- [ ] `connectWebhookSecret` (String)
- [ ] `platformWebhookSecret` (String)
- [ ] `webhookUrl` (String)

### SuperAdminJobToggleRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminJobToggleRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminJobToggleRequest.java:9)

- [ ] `enabled` (Boolean)

### SuperAdminNotificationRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminNotificationRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminNotificationRequest.java:14)

- [ ] `title` (String)
- [ ] `message` (String)
- [ ] `channel` (String)
- [ ] `scheduledAt` (Instant)
- [ ] `organisationIds` (List<Long>)

### SuperAdminOnboardingCreateOrganisationRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminOnboardingCreateOrganisationRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminOnboardingCreateOrganisationRequest.java:13)

- [ ] `name` (String)
- [ ] `slug` (String)
- [ ] `subdomain` (String)
- [ ] `primaryAdminEmail` (String)
- [ ] `primaryAdminFirstName` (String)
- [ ] `primaryAdminLastName` (String)
- [ ] `plan` (String)
- [ ] `billingCycle` (String)
- [ ] `trialDays` (Integer)
- [ ] `timezone` (String)
- [ ] `region` (String)
- [ ] `dataResidency` (String)
- [ ] `provisionTenant` (Boolean)
- [ ] `provisionStripeSubscription` (Boolean)

### SuperAdminOrganisationListRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminOrganisationListRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminOrganisationListRequest.java:9)

- [ ] `search` (String)
- [ ] `status` (String)
- [ ] `plan` (String)
- [ ] `createdFrom` (LocalDate)
- [ ] `createdTo` (LocalDate)
- [ ] `region` (String)
- [ ] `dataResidency` (String)
- [ ] `page` (Integer)
- [ ] `pageSize` (Integer)
- [ ] `sort` (String)
- [ ] `order` (String)
- [ ] `exportCsv` (Boolean)

### SuperAdminPartnerAccessRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminPartnerAccessRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminPartnerAccessRequest.java:12)

- [ ] `organisationIds` (List<Long>)
- [ ] `featureFlags` (Map<String, Object>)
- [ ] `active` (Boolean)

### SuperAdminResolveTenantRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminResolveTenantRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminResolveTenantRequest.java:11)

- [ ] `email` (String)

### SuperAdminStartImpersonationRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminStartImpersonationRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminStartImpersonationRequest.java:12)

- [ ] `targetAuthId` (Long)
- [ ] `organisationId` (Long)
- [ ] `reason` (String)
- [ ] `durationMinutes` (Integer)

### SuperAdminSubscriptionUpdateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminSubscriptionUpdateRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminSubscriptionUpdateRequest.java:10)

- [ ] `plan` (String)
- [ ] `billingCycle` (String)
- [ ] `trialDays` (Integer)
- [ ] `effectiveDate` (java.time.Instant)
- [ ] `prorate` (Boolean)
- [ ] `createRenewalInvoice` (Boolean)
- [ ] `userLimits` (UserLimitsUpdateRequest)
- [ ] `therapistLimit` (Integer)
- [ ] `supervisorLimit` (Integer)
- [ ] `clientLimit` (Integer)

### SuperAdminSuspendRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminSuspendRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminSuspendRequest.java:9)

- [ ] `reason` (String)

### SuperAdminTargetedNotificationRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminTargetedNotificationRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminTargetedNotificationRequest.java:15)

- [ ] `title` (String)
- [ ] `message` (String)
- [ ] `channel` (String)
- [ ] `scheduledAt` (Instant)
- [ ] `organisationIds` (List<Long>)

### SuperAdminTenantSettingsRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminTenantSettingsRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminTenantSettingsRequest.java:9)

- [ ] `timezone` (String)
- [ ] `region` (String)
- [ ] `dataResidency` (String)
- [ ] `locale` (String)
- [ ] `logoUrl` (String)
- [ ] `brandPrimaryColor` (String)
- [ ] `brandSecondaryColor` (String)
- [ ] `brandAccentColor` (String)
- [ ] `supportEmail` (String)
- [ ] `supportAddress` (String)

### SuperAdminTerminateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminTerminateRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminTerminateRequest.java:11)

- [ ] `reason` (String)
- [ ] `retentionDays` (Integer)

### SuperAdminUpdateIncidentRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminUpdateIncidentRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminUpdateIncidentRequest.java:11)

- [ ] `status` (String)
- [ ] `severity` (String)
- [ ] `description` (String)
- [ ] `resolvedAt` (Instant)

### SuperAdminUpsertImpersonationPolicyRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminUpsertImpersonationPolicyRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminUpsertImpersonationPolicyRequest.java:10)

- [ ] `enabled` (Boolean)
- [ ] `requireReason` (Boolean)
- [ ] `minReasonLength` (Integer)
- [ ] `maxDurationMinutes` (Integer)
- [ ] `allowCrossOrganisation` (Boolean)
- [ ] `allowedRoles` (List<String>)
- [ ] `deniedRoles` (List<String>)
- [ ] `allowedOrgIds` (List<Long>)
- [ ] `deniedOrgIds` (List<Long>)

### SuperAdminUpsertNotificationTemplateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminUpsertNotificationTemplateRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/SuperAdminUpsertNotificationTemplateRequest.java:11)

- [ ] `subjectTemplate` (String)
- [ ] `bodyTemplate` (String)
- [ ] `active` (Boolean)

### UpdatePlanRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpdatePlanRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpdatePlanRequest.java:12)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `basePrice` (BigDecimal)
- [ ] `annualPrice` (BigDecimal)
- [ ] `billingCycle` (String)
- [ ] `trialDays` (Integer)
- [ ] `status` (String)
- [ ] `providerPriceIdMonthly` (String)
- [ ] `providerPriceIdAnnual` (String)

### UpsertAddonCatalogRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertAddonCatalogRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertAddonCatalogRequest.java:17)

- [ ] `pricePerUnit` (BigDecimal)
- [ ] `billingCycle` (AddonBillingCycle)
- [ ] `unitValue` (Integer)
- [ ] `status` (AddonCatalogStatus)

### UpsertBillingContactRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertBillingContactRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertBillingContactRequest.java:9)

- [ ] `fullName` (String)
- [ ] `email` (String)
- [ ] `primary` (Boolean)
- [ ] `active` (Boolean)

### UpsertBillingNotificationTemplateRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertBillingNotificationTemplateRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertBillingNotificationTemplateRequest.java:9)

- [ ] `subjectTemplate` (String)
- [ ] `bodyTemplate` (String)
- [ ] `active` (Boolean)

### UpsertDunningPolicyRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertDunningPolicyRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertDunningPolicyRequest.java:17)

- [ ] `steps` (List<DunningStepRequest>)
- [ ] `gracePeriodDays` (Integer)
- [ ] `trialNoticeDays` (Integer)
- [ ] `day` (Integer)
- [ ] `action` (Action)

### UpsertInvoiceDisputeRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertInvoiceDisputeRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertInvoiceDisputeRequest.java:14)

- [ ] `externalCaseId` (String)
- [ ] `status` (String)
- [ ] `amountUsd` (BigDecimal)
- [ ] `reason` (String)

### UpsertOrgAddonRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertOrgAddonRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertOrgAddonRequest.java:14)

- [ ] `featureCode` (String)
- [ ] `quantity` (Integer)

### UpsertOrganisationFeaturesRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertOrganisationFeaturesRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertOrganisationFeaturesRequest.java:11)

- [ ] `features` (Map<String, FeatureValueRequest>)

### UpsertPlanEntitlementsRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertPlanEntitlementsRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertPlanEntitlementsRequest.java:10)

- [ ] `items` (List<PlanEntitlementItem>)
- [ ] `features` (List<PlanEntitlementItem>)

### UpsertRolloutsRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertRolloutsRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertRolloutsRequest.java:12)

- [ ] `rules` (List<RolloutRuleRequest>)

### UpsertSubscriptionRequest.java

[src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertSubscriptionRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/dto/UpsertSubscriptionRequest.java:9)

- [ ] `planName` (String)
- [ ] `billingCycle` (String)
- [ ] `trialDays` (Integer)
- [ ] `status` (String)
- [ ] `providerCustomerId` (String)
- [ ] `providerSubscriptionId` (String)

### CreateOptionCategoryRequest.java

[src/main/java/com/smart/therapy/flow/system/dto/CreateOptionCategoryRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/dto/CreateOptionCategoryRequest.java:9)

- [ ] `categoryKey` (String)
- [ ] `categoryName` (String)
- [ ] `description` (String)
- [ ] `isSystem` (Boolean)
- [ ] `isActive` (Boolean)

### CreateSystemOptionRequest.java

[src/main/java/com/smart/therapy/flow/system/dto/CreateSystemOptionRequest.java:12](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/dto/CreateSystemOptionRequest.java:12)

- [ ] `categoryId` (Long)
- [ ] `optionKey` (String)
- [ ] `optionLabel` (String)
- [ ] `sortOrder` (Integer)
- [ ] `isDefault` (Boolean)
- [ ] `isSystem` (Boolean)
- [ ] `isActive` (Boolean)
- [ ] `price` (BigDecimal)

### PracticeConfigurationRequest.java

[src/main/java/com/smart/therapy/flow/system/dto/PracticeConfigurationRequest.java:17](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/dto/PracticeConfigurationRequest.java:17)

- [ ] `practiceName` (String)
- [ ] `practiceAddress` (String)
- [ ] `practicePhone` (String)
- [ ] `practiceEmail` (String)
- [ ] `practiceWebsite` (String)
- [ ] `taxId` (String)
- [ ] `licenseNumber` (String)
- [ ] `licenseState` (String)
- [ ] `npiNumber` (String)
- [ ] `description` (String)
- [ ] `subtitle` (String)
- [ ] `timezone` (String)

### ReorderCategoryOptionsRequest.java

[src/main/java/com/smart/therapy/flow/system/dto/ReorderCategoryOptionsRequest.java:21](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/dto/ReorderCategoryOptionsRequest.java:21)

- [ ] `options` (List<OptionOrderItem>)
- [ ] `optionId` (Long)
- [ ] `sortOrder` (Integer)

### UpdateOptionCategoryRequest.java

[src/main/java/com/smart/therapy/flow/system/dto/UpdateOptionCategoryRequest.java:7](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/dto/UpdateOptionCategoryRequest.java:7)

- [ ] `categoryKey` (String)
- [ ] `categoryName` (String)
- [ ] `description` (String)
- [ ] `isActive` (Boolean)

### UpdateSystemOptionRequest.java

[src/main/java/com/smart/therapy/flow/system/dto/UpdateSystemOptionRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/system/dto/UpdateSystemOptionRequest.java:9)

- [ ] `categoryId` (Long)
- [ ] `optionKey` (String)
- [ ] `optionLabel` (String)
- [ ] `sortOrder` (Integer)
- [ ] `isDefault` (Boolean)
- [ ] `isActive` (Boolean)
- [ ] `price` (BigDecimal)

### AssignChecklistRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/AssignChecklistRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/AssignChecklistRequest.java:11)

- [ ] `templateId` (Long)
- [ ] `dueDate` (LocalDate)

### BulkAssignChecklistRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/BulkAssignChecklistRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/BulkAssignChecklistRequest.java:13)

- [ ] `templateId` (Long)
- [ ] `clientIds` (List<Long>)
- [ ] `dueDate` (LocalDate)
- [ ] `description` (String)
- [ ] `notes` (String)

### ClientChecklistFilterRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/ClientChecklistFilterRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/ClientChecklistFilterRequest.java:10)

- [ ] `clientId` (Long)
- [ ] `templateId` (Long)
- [ ] `category` (String)
- [ ] `isCompleted` (Boolean)
- [ ] `completedDateFrom` (LocalDate)
- [ ] `completedDateTo` (LocalDate)
- [ ] `dueDateFrom` (LocalDate)
- [ ] `dueDateTo` (LocalDate)
- [ ] `createdDateFrom` (LocalDate)
- [ ] `createdDateTo` (LocalDate)

### CopyChecklistTemplateRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/CopyChecklistTemplateRequest.java:7](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/CopyChecklistTemplateRequest.java:7)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `isActive` (Boolean)

### CreateChecklistItemRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/CreateChecklistItemRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/CreateChecklistItemRequest.java:9)

- [ ] `title` (String)
- [ ] `description` (String)
- [ ] `category` (String)
- [ ] `isRequired` (Boolean)
- [ ] `itemOrder` (Integer)
- [ ] `daysFromStart` (Integer)
- [ ] `sortOrder` (Integer)

### CreateChecklistTemplateRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/CreateChecklistTemplateRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/CreateChecklistTemplateRequest.java:11)

- [ ] `name` (String)
- [ ] `description` (String)
- [ ] `clientType` (String)
- [ ] `isActive` (Boolean)
- [ ] `sortOrder` (Integer)
- [ ] `items` (List<CreateChecklistItemRequest>)

### CreateTaskCommentRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/CreateTaskCommentRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/CreateTaskCommentRequest.java:10)

- [ ] `content` (String)
- [ ] `isInternal` (Boolean)

### CreateTaskRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/CreateTaskRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/CreateTaskRequest.java:16)

- [ ] `title` (String)
- [ ] `titleKey` (String)
- [ ] `taskType` (String)
- [ ] `description` (String)
- [ ] `priority` (String)
- [ ] `status` (String)
- [ ] `clientId` (Long)
- [ ] `assignedToId` (Long)
- [ ] `dueDate` (Instant)

### ReorderChecklistItemsRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/ReorderChecklistItemsRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/ReorderChecklistItemsRequest.java:11)

- [ ] `itemIds` (List<Long>)

### UpdateChecklistItemRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/UpdateChecklistItemRequest.java:7](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/UpdateChecklistItemRequest.java:7)

- [ ] `isCompleted` (Boolean)
- [ ] `notes` (String)

### UpdateChecklistTemplateItemRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/UpdateChecklistTemplateItemRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/UpdateChecklistTemplateItemRequest.java:14)

- [ ] `title` (String)
- [ ] `description` (String)
- [ ] `category` (String)
- [ ] `isRequired` (Boolean)
- [ ] `itemOrder` (Integer)
- [ ] `daysFromStart` (Integer)
- [ ] `sortOrder` (Integer)

### UpdateTaskCommentRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/UpdateTaskCommentRequest.java:7](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/UpdateTaskCommentRequest.java:7)

- [ ] `content` (String)
- [ ] `isInternal` (Boolean)

### UpdateTaskRequest.java

[src/main/java/com/smart/therapy/flow/task/dto/UpdateTaskRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/task/dto/UpdateTaskRequest.java:16)

- [ ] `title` (String)
- [ ] `titleKey` (String)
- [ ] `taskType` (String)
- [ ] `description` (String)
- [ ] `status` (String)
- [ ] `priority` (String)
- [ ] `clientId` (Long)
- [ ] `assignedToId` (Long)
- [ ] `dueDate` (Instant)

### AssignRoleRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/AssignRoleRequest.java:16](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/AssignRoleRequest.java:16)

- [ ] `roles` (Set<String>)

### ChangePasswordRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/ChangePasswordRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/ChangePasswordRequest.java:11)

- [ ] `currentPassword` (String)
- [ ] `newPassword` (String)

### CreateSupervisorAssignmentRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/CreateSupervisorAssignmentRequest.java:15](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/CreateSupervisorAssignmentRequest.java:15)

- [ ] `supervisorId` (Long)
- [ ] `therapistId` (Long)
- [ ] `assignmentType` (AssignmentType)
- [ ] `startDate` (LocalDate)
- [ ] `endDate` (LocalDate)
- [ ] `requiredMeetingFrequency` (RequiredMeetingFrequency)
- [ ] `notes` (String)

### CreateTherapistBlockedTimeRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/CreateTherapistBlockedTimeRequest.java:11](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/CreateTherapistBlockedTimeRequest.java:11)

- [ ] `therapistId` (Long)
- [ ] `startTime` (Instant)
- [ ] `endTime` (Instant)
- [ ] `allDay` (Boolean)
- [ ] `blockType` (BlockType)
- [ ] `reason` (String)
- [ ] `isRecurring` (Boolean)
- [ ] `recurrencePattern` (String)
- [ ] `isActive` (Boolean)

### CreateUserActivityLogRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/CreateUserActivityLogRequest.java:9](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/CreateUserActivityLogRequest.java:9)

- [ ] `activityType` (String)
- [ ] `description` (String)
- [ ] `ipAddress` (String)
- [ ] `userAgent` (String)

### CreateUserRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/CreateUserRequest.java:21](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/CreateUserRequest.java:21)

- [ ] `username` (String)
- [ ] `fullName` (String)
- [ ] `password` (String)
- [ ] `email` (String)
- [ ] `phone` (String)
- [ ] `roles` (Set<String>)
- [ ] `active` (Boolean)
- [ ] `idempotencyKey` (String)

### UpdateSupervisorAssignmentRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/UpdateSupervisorAssignmentRequest.java:14](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/UpdateSupervisorAssignmentRequest.java:14)

- [ ] `assignmentType` (AssignmentType)
- [ ] `startDate` (LocalDate)
- [ ] `endDate` (LocalDate)
- [ ] `isActive` (Boolean)
- [ ] `requiredMeetingFrequency` (RequiredMeetingFrequency)
- [ ] `notes` (String)
- [ ] `nextMeetingDate` (Instant)
- [ ] `lastMeetingDate` (Instant)

### UpdateUserRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/UpdateUserRequest.java:18](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/UpdateUserRequest.java:18)

- [ ] `username` (String)
- [ ] `fullName` (String)
- [ ] `email` (String)
- [ ] `phone` (String)
- [ ] `active` (Boolean)
- [ ] `roles` (Set<String>)

### UserProfileEducationRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/UserProfileEducationRequest.java:13](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/UserProfileEducationRequest.java:13)

- [ ] `degreeType` (String)
- [ ] `fieldOfStudy` (String)
- [ ] `institution` (String)
- [ ] `graduationYear` (Integer)
- [ ] `graduationDate` (LocalDate)
- [ ] `isAccredited` (Boolean)
- [ ] `accreditationBody` (String)
- [ ] `notes` (String)

### UserProfileRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/UserProfileRequest.java:22](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/UserProfileRequest.java:22)

- [ ] `fullName` (String)
- [ ] `email` (String)
- [ ] `licenseNumber` (String)
- [ ] `licenseType` (String)
- [ ] `licenseState` (String)
- [ ] `licenseExpiry` (LocalDate)
- [ ] `licenseStatus` (LicenseStatus)
- [ ] `specializations` (List<String>)
- [ ] `languages` (List<String>)
- [ ] `yearsOfExperience` (Integer)
- [ ] `clinicalExperience` (String)
- [ ] `researchBackground` (String)
- [ ] `education` (List<UserProfileEducationRequest>)
- [ ] `supervisoryExperience` (String)
- [ ] `careerObjectives` (String)
- [ ] `workingDays` (List<String>)
- [ ] `workingHours` (String)
- [ ] `consultationWorkingHours` (String)
- [ ] `maxClientsPerDay` (Integer)
- [ ] `sessionDuration` (Integer)
- [ ] `availabilityStatus` (AvailabilityStatus)
- [ ] `timezone` (String)
- [ ] `virtualRoomId` (Long)
- [ ] `availablePhysicalRoomIds` (List<Long>)
- [ ] `emergencyContactName` (String)
- [ ] `emergencyContactPhone` (String)
- [ ] `emergencyContactEmail` (String)
- [ ] `emergencyContactRelationship` (String)
- [ ] `zoomAccountId` (String)
- [ ] `zoomClientId` (String)
- [ ] `zoomClientSecret` (String)
- [ ] `currentPassword` (String)
- [ ] `newPassword` (String)
- [ ] `confirmNewPassword` (String)

### ZoomCredentialsRequest.java

[src/main/java/com/smart/therapy/flow/user/dto/ZoomCredentialsRequest.java:10](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/dto/ZoomCredentialsRequest.java:10)

- [ ] `accountId` (String)
- [ ] `clientId` (String)
- [ ] `clientSecret` (String)

## Background entry points

- [ ] TransactionalEventListener — [src/main/java/com/smart/therapy/flow/client/listener/ClientCreatedListener.java:45](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/listener/ClientCreatedListener.java:45)
- [ ] TransactionalEventListener — [src/main/java/com/smart/therapy/flow/client/listener/ClientNotificationListener.java:23](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/listener/ClientNotificationListener.java:23)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/client/service/ClientPurgeScheduler.java:55](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/client/service/ClientPurgeScheduler.java:55)
- [ ] EventListener — [src/main/java/com/smart/therapy/flow/common/config/EmailServiceConfig.java:20](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/common/config/EmailServiceConfig.java:20)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/common/service/PhiBlindIndexBackfillScheduler.java:73](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/common/service/PhiBlindIndexBackfillScheduler.java:73)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/common/service/PhiEncryptionBackfillScheduler.java:185](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/common/service/PhiEncryptionBackfillScheduler.java:185)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/notification/service/NotificationService.java:1215](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/notification/service/NotificationService.java:1215)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/organisation/service/TenantDirectoryService.java:58](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/organisation/service/TenantDirectoryService.java:58)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/organisation/service/TenantMigrationScheduler.java:72](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/organisation/service/TenantMigrationScheduler.java:72)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/organisation/service/TenantSchemaIntegrityChecker.java:50](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/organisation/service/TenantSchemaIntegrityChecker.java:50)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/session/service/AudioCleanupScheduler.java:43](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/service/AudioCleanupScheduler.java:43)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/session/service/AudioCleanupScheduler.java:104](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/service/AudioCleanupScheduler.java:104)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/session/service/SessionReminderService.java:46](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/service/SessionReminderService.java:46)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/session/service/TranscriptRetentionScheduler.java:37](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/session/service/TranscriptRetentionScheduler.java:37)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/subscription/service/RefundRetryProcessor.java:30](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/subscription/service/RefundRetryProcessor.java:30)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:34](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:34)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:45](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:45)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:56](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:56)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:77](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/subscription/service/SubscriptionLifecycleScheduler.java:77)
- [ ] TransactionalEventListener — [src/main/java/com/smart/therapy/flow/superadmin/listener/OrganisationOnboardedListener.java:23](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/listener/OrganisationOnboardedListener.java:23)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminImpersonationService.java:303](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminImpersonationService.java:303)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminNotificationService.java:295](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminNotificationService.java:295)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:178](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:178)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:290](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:290)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:403](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:403)
- [ ] Scheduled — [src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:662](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/superadmin/service/SuperAdminOperationsService.java:662)
- [ ] TransactionalEventListener — [src/main/java/com/smart/therapy/flow/user/listener/UserCreatedListener.java:38](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/listener/UserCreatedListener.java:38)
- [ ] TransactionalEventListener — [src/main/java/com/smart/therapy/flow/user/listener/UserProfileUpdatedListener.java:34](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/listener/UserProfileUpdatedListener.java:34)
- [ ] TransactionalEventListener — [src/main/java/com/smart/therapy/flow/user/listener/UserUpdatedListener.java:34](/Users/apple/IdeaProjects/smarthub/src/main/java/com/smart/therapy/flow/user/listener/UserUpdatedListener.java:34)

## Reconciliation queue

Full UI control candidates, filter/search/sort state declarations, source-file inventory and existing test-file inventory are in `source-inventory.json`. Review dynamic controls, API request bodies, inherited fields, service rules and websocket handlers before calling discovery complete. Existing test files are not mapped or credited as coverage by this scanner.
