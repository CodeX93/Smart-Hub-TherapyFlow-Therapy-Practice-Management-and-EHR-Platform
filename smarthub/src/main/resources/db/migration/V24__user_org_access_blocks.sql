CREATE TABLE IF NOT EXISTS public.user_organisation_access_blocks (
    id BIGSERIAL PRIMARY KEY,
    auth_id BIGINT NOT NULL,
    organisation_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    blocked_by BIGINT,
    blocked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user_org_blocks_auth FOREIGN KEY (auth_id) REFERENCES public.auth_identities(id),
    CONSTRAINT fk_user_org_blocks_org FOREIGN KEY (organisation_id) REFERENCES public.organisations(id),
    CONSTRAINT uq_user_org_blocks_auth_org UNIQUE (auth_id, organisation_id)
);

CREATE INDEX IF NOT EXISTS idx_user_org_blocks_auth ON public.user_organisation_access_blocks (auth_id);
CREATE INDEX IF NOT EXISTS idx_user_org_blocks_org ON public.user_organisation_access_blocks (organisation_id);
