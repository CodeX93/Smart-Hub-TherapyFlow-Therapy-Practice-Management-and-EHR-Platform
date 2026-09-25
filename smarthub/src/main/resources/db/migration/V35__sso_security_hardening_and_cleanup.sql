CREATE TABLE IF NOT EXISTS public.organisation_sso_states (
    id BIGSERIAL PRIMARY KEY,
    state_token VARCHAR(255) NOT NULL UNIQUE,
    organisation_id BIGINT NOT NULL REFERENCES public.organisations(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    nonce VARCHAR(255) NOT NULL,
    pkce_verifier VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_org_sso_states_org_provider
    ON public.organisation_sso_states(organisation_id, provider);

CREATE INDEX IF NOT EXISTS idx_org_sso_states_expires
    ON public.organisation_sso_states(expires_at);

CREATE TABLE IF NOT EXISTS public.organisation_sso_allowed_domains (
    id BIGSERIAL PRIMARY KEY,
    organisation_id BIGINT NOT NULL REFERENCES public.organisations(id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_org_sso_allowed_domains_org_domain UNIQUE (organisation_id, domain)
);

-- Remove SSO from feature catalog/entitlement path; SSO is controlled by org SSO config + domain allowlist.
DELETE FROM public.feature_rollout_rules WHERE feature_key = 'SSO_ENABLED';
DELETE FROM public.org_feature_overrides WHERE feature_key = 'SSO_ENABLED';
DELETE FROM public.plan_feature_versions
WHERE feature_id IN (SELECT id FROM public.app_features WHERE code = 'SSO_ENABLED');
DELETE FROM public.app_features WHERE code = 'SSO_ENABLED';

-- Cleanup legacy tenant feature switch for SSO.
DELETE FROM public.tenant_features WHERE feature_key = 'SSO_ENABLED';

