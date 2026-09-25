CREATE OR REPLACE FUNCTION reject_finalized_clinical_record_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.is_finalized IS TRUE THEN
        RAISE EXCEPTION 'finalized clinical records are immutable; create an amendment instead'
            USING ERRCODE = '55000';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS trg_session_notes_finalized_immutable ON session_notes;
CREATE TRIGGER trg_session_notes_finalized_immutable
    BEFORE UPDATE OR DELETE ON session_notes
    FOR EACH ROW
    EXECUTE FUNCTION reject_finalized_clinical_record_mutation();

DROP TRIGGER IF EXISTS trg_client_reports_finalized_immutable ON client_reports;
CREATE TRIGGER trg_client_reports_finalized_immutable
    BEFORE UPDATE OR DELETE ON client_reports
    FOR EACH ROW
    EXECUTE FUNCTION reject_finalized_clinical_record_mutation();

DROP TRIGGER IF EXISTS trg_assessment_reports_finalized_immutable ON assessment_reports;
CREATE TRIGGER trg_assessment_reports_finalized_immutable
    BEFORE UPDATE OR DELETE ON assessment_reports
    FOR EACH ROW
    EXECUTE FUNCTION reject_finalized_clinical_record_mutation();

COMMENT ON FUNCTION reject_finalized_clinical_record_mutation() IS
    'Database guard preventing finalized clinical records from being reopened, changed, or deleted.';
