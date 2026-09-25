# Portal form draft regression

Run from the SmartHub backend checkout:

```sh
node scripts/test-portal-form-draft.mjs
```

Optional `QA_FRONTEND_ROOT` selects the frontend checkout; `QA_PLAYWRIGHT_CLI` selects the Playwright CLI wrapper. Requires installed frontend dependencies and the Playwright CLI skill wrapper/Chromium. The runner creates its own local Vite server and browser session and closes both on completion.

These checks mount the real ClinicalFormDetail and staff AssignedFormsList with the real Redux store, RTK Query normalization and React Router. Synthetic HTTP responses provide controlled storage and errors. They verify frontend behavior; they do not prove database persistence, authentication or production deployment.

Coverage:

- Assigned date uses `assignedAt`/backend `createdAt`, rather than `dueDate`.
- Partial answers save without required-field or signature completion.
- Draft save never calls signing or final submission endpoints.
- Reload restores answers, including intentionally cleared values.
- Partially failed saves and failed response refetch retain edits and allow retry.
- Changing assignment routes resets local answers.
- Final submission still requires completion, saves a typed signature, and becomes read-only.

`Save Draft` saves answers only. Signatures and terms acceptance remain part of final submission. Saves use the existing per-field backend API; a failure can leave some answers persisted. The UI reports that the draft could not be fully saved and retains local edits for retry. No autosave or browser storage of clinical answers is added.

Evidence appears under `output/playwright/form-draft-<timestamp>/`; screenshots are `output/playwright/form-draft-saved.png` and `form-draft-completed.png`.

Real-backend phase scripts are also available: `real-draft.js`, `real-submit.js`, and `real-staff.js`. Run them through `playwright-cli run-code --filename ...` in an already authenticated, isolated QA session with the synthetic form described in each script. They do not install HTTP mocks. Check persisted draft state before running the submission phase. The September 11 database-backed verification and its exact scope are recorded in `docs/testing/portal-form-draft-e2e-2026-09-11.md`.
