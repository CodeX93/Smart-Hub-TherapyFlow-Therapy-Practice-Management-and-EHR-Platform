-- TENANT
-- Upload malware/signature scan tracking for client documents.

ALTER TABLE documents ADD COLUMN IF NOT EXISTS scan_status varchar(20);
ALTER TABLE documents ADD COLUMN IF NOT EXISTS scanned_at timestamp(6) with time zone;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS scan_detail TEXT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS content_checksum varchar(64);

CREATE INDEX IF NOT EXISTS idx_document_scan_status ON documents (scan_status);

COMMENT ON COLUMN documents.scan_status IS 'PENDING, CLEAN, QUARANTINED, or FAILED';
