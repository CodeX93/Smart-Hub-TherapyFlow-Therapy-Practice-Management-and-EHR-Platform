# Super Admin Screen Baseline

- Captured at: 2026-04-16 13:59:07 PKT
- Branch: `authentication/integeration`
- Commit: `7186960`

## Route Inventory (Current Branch)

- `/super-admin/login`
- `/super-admin/dashboard`
- `/super-admin/organisations`
- `/super-admin/organisations/new`
- `/super-admin/organisations/:slug`
- `/super-admin/organisations/:slug/impersonate`
- `/super-admin/organisations/:slug/feature-toggles`
- `/super-admin/roles-and-permissions`
- `/super-admin/roles-and-permissions/create`
- `/super-admin/feature-flags`
- `/super-admin/feature-flags/rollout`
- `/super-admin/feature-flags/create`
- `/super-admin/billings-and-plans`
- `/super-admin/billings-and-plans/create-plan`
- `/super-admin/billings-and-plans/invoices/:invoiceId`
- `/super-admin/my-profile`
- `/super-admin/system-settings`
- `/super-admin/audit-logs`

## Current Gaps/Notes

- `audit-logs` route currently renders `SuperAdminDashboard` (placeholder), not a dedicated audit logs screen.
- No dedicated top-level "entitlements" screen route found in current branch.
- No dedicated separate screen route for plan entitlements found; entitlement language appears inside feature-flag and plan-related UI copy.

## Super Admin Page Tree Present

- `dashboard`
- `organisations` (list, create, details, feature-toggles, impersonate)
- `roles-and-permissions` (list, create)
- `feature-flags` (catalog, rollout, create)
- `billings-and-plans` (main, create-plan, invoice-details)
- `system-settings`
- `my-profile`

## Baseline Comparison Checklist (for next branch)

- Check if dedicated `audit-logs` page exists (route + page + components).
- Check if dedicated `entitlements` page exists (route + page + components).
- Check if billings/plans screens have additional tabs or panels not in this branch.
- Check if any super-admin route in the other branch has different component mapping.
- Check if super-admin auth/login page UI differs from this branch.
- Check if shared components differ for super-admin header, crumb bar, tables, cards.

## Copy Plan After Branch Switch

- Bring missing super-admin screens/components into this branch style without changing existing design language.
- Preserve this branch routing structure unless the new screens require explicit additional routes.
- Prefer file-level copy of missing screens/components, then wire routes/imports minimally.
