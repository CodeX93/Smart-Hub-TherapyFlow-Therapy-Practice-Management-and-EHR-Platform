ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN;

UPDATE public.auth_identities
SET must_change_password = FALSE
WHERE must_change_password IS NULL;

ALTER TABLE public.auth_identities
    ALTER COLUMN must_change_password SET DEFAULT FALSE;

ALTER TABLE public.auth_identities
    ALTER COLUMN must_change_password SET NOT NULL;
