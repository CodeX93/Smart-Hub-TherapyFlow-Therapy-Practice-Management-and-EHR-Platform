-- TENANT
-- Allow controlled unfinalize / assignment-delete of clinical records when the
-- application sets LOCAL app.allow_clinical_unfinalize = 'true' inside a transaction.
-- Default behavior remains immutable for all other mutations/deletes.

CREATE OR REPLACE FUNCTION reject_finalized_clinical_record_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    allow_unfinalize text;
BEGIN
    allow_unfinalize := NULLIF(current_setting('app.allow_clinical_unfinalize', true), '');

    IF OLD.is_finalized IS TRUE THEN
        -- Permit finalized -> draft/unfinalized when explicitly allowed.
        IF TG_OP = 'UPDATE'
           AND allow_unfinalize = 'true'
           AND NEW.is_finalized IS FALSE THEN
            RETURN NEW;
        END IF;

        -- Permit hard-delete of finalized records only when the app flag is set
        -- (used when deleting an entire assessment assignment, matching ClientHub).
        IF TG_OP = 'DELETE' AND allow_unfinalize = 'true' THEN
            RETURN OLD;
        END IF;

        RAISE EXCEPTION 'finalized clinical records are immutable; create an amendment instead'
            USING ERRCODE = '55000';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END
$$;

COMMENT ON FUNCTION reject_finalized_clinical_record_mutation() IS
    'Database guard preventing finalized clinical records from being reopened, changed, or deleted, unless app.allow_clinical_unfinalize=true for an unfinalize transition or assignment delete.';