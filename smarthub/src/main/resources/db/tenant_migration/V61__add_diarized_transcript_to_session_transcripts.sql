-- TENANT
-- Add AI speaker-diarized transcript column (nullable until therapist triggers Identify Speakers).
-- Encrypted at the application layer via EncryptedStringConverter (same as final_transcript).

ALTER TABLE session_transcripts
    ADD COLUMN IF NOT EXISTS diarized_transcript TEXT;
