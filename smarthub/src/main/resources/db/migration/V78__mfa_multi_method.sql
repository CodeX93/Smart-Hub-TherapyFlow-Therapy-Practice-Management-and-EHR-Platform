-- Multi-method MFA: a user may enable TOTP, SMS, and/or EMAIL together.
-- mfa_method remains the preferred default for login UX.

ALTER TABLE auth_mfa_credentials
    ADD COLUMN IF NOT EXISTS totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE auth_mfa_credentials
SET totp_enabled = CASE
        WHEN COALESCE(enabled, FALSE) AND COALESCE(mfa_method, 'TOTP') = 'TOTP' THEN TRUE
        ELSE totp_enabled
    END,
    sms_enabled = CASE
        WHEN COALESCE(enabled, FALSE) AND mfa_method = 'SMS' THEN TRUE
        ELSE sms_enabled
    END,
    email_enabled = CASE
        WHEN COALESCE(enabled, FALSE) AND mfa_method = 'EMAIL' THEN TRUE
        ELSE email_enabled
    END
WHERE COALESCE(enabled, FALSE) = TRUE
  AND NOT (totp_enabled OR sms_enabled OR email_enabled);
