-- TENANT
-- Skip when transcript tables are not provisioned yet (partial tenant schemas).

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'session_transcripts'
    ) THEN
        ALTER TABLE session_transcripts
            ADD COLUMN IF NOT EXISTS translated_to_english BOOLEAN;

        UPDATE session_transcripts
        SET translated_to_english = FALSE
        WHERE translated_to_english IS NULL;

        ALTER TABLE session_transcripts
            ALTER COLUMN translated_to_english SET DEFAULT FALSE,
            ALTER COLUMN translated_to_english SET NOT NULL;

        ALTER TABLE session_transcripts
            ADD COLUMN IF NOT EXISTS duration_seconds INTEGER;

        ALTER TABLE session_transcripts
            ADD COLUMN IF NOT EXISTS word_count INTEGER;

        ALTER TABLE session_transcripts
            ADD COLUMN IF NOT EXISTS raw_content TEXT;

        ALTER TABLE session_transcripts
            ALTER COLUMN expected_chunks DROP NOT NULL;

        ALTER TABLE session_transcripts
            ALTER COLUMN status SET DEFAULT 'recording';

        UPDATE session_transcripts
        SET status = 'recording'
        WHERE lower(status) IN ('started', 'uploading');

        UPDATE session_transcripts
        SET status = 'processing'
        WHERE lower(status) = 'finalizing';

        UPDATE session_transcripts
        SET status = 'ready'
        WHERE lower(status) = 'completed';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'session_transcript_chunks'
    ) THEN
        ALTER TABLE session_transcript_chunks
            ADD COLUMN IF NOT EXISTS chunk_duration_seconds DOUBLE PRECISION;
    END IF;
END $$;
