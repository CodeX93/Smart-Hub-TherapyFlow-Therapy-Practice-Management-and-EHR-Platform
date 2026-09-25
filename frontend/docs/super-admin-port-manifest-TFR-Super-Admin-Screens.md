# Super Admin Port Manifest

- Source branch: `TFR-Super-Admin-Screens`
- Source commit: `b2a0ac1`
- Captured at: `2026-04-16 14:00:57 PKT`
- Target branch (planned): `authentication/integeration`

## Findings

- No dedicated top-level Audit Logs screen exists in this source branch.
- `audit-logs` menu/route exists, but route points to dashboard placeholder:
  - `src/routes/index.tsx` maps `"/super-admin/audit-logs"` to `SuperAdminDashboard`.
- No dedicated top-level Entitlements screen route/page exists.
- Entitlement wording appears inside feature-flag/tenant feature-toggle UIs, not a standalone screen.

## Files With UI/Screen Differences vs `authentication/integeration`

- `src/pages/super-admin/billings-and-plans/index.tsx`
- `src/pages/super-admin/billings-and-plans/components/PlansPricingPanel.tsx`
- `src/pages/super-admin/billings-and-plans/create-plan/index.tsx`
- `src/pages/super-admin/billings-and-plans/create-plan/components/CreatePlanForm.tsx`
- `src/pages/super-admin/billings-and-plans/invoice-details/index.tsx`
- `src/pages/super-admin/dashboard/index.tsx`
- `src/pages/super-admin/dashboard/dashboard.types.ts`
- `src/pages/super-admin/dashboard/dashboard.utils.tsx`
- `src/pages/super-admin/dashboard/components/RevenueOverviewCard.tsx`
- `src/pages/super-admin/dashboard/components/RevenuePlanList.tsx`
- `src/pages/super-admin/dashboard/components/RevenueMovements.tsx`
- `src/pages/super-admin/my-profile/index.tsx`
- `src/pages/super-admin/organisations/index.tsx`
- `src/pages/super-admin/organisations/create/index.tsx`
- `src/pages/super-admin/organisations/create/createOrganisation.schema.ts`
- `src/pages/super-admin/organisations/create/components/CreateOrganisationForm.tsx`
- `src/pages/super-admin/organisations/create/components/PlanBillingCard.tsx`
- `src/routes/index.tsx`

## Source-Only Direct API Helpers (Non-RTK)

- `src/pages/super-admin/billings-and-plans/plans.api.ts`
- `src/pages/super-admin/organisations/organisations.api.ts`

## Porting Instruction (when back on target branch)

- Port only screen/UI structure from the files above.
- Do not port direct-fetch API helper style (`plans.api.ts`, `organisations.api.ts`) into target branch.
- Keep target branch RTK Query integration intact.
- Keep target branch dedicated super-admin login route/page intact.
