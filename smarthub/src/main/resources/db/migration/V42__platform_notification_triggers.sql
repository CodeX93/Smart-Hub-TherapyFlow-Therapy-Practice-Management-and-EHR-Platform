CREATE TABLE IF NOT EXISTS public.platform_notification_triggers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NULL,
    condition_rules TEXT NULL,
    recipient_rules TEXT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'medium',
    delay_minutes INTEGER NOT NULL DEFAULT 0,
    batch_window_minutes INTEGER NOT NULL DEFAULT 5,
    max_batch_size INTEGER NOT NULL DEFAULT 10,
    is_scheduled BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_platform_notification_triggers_event_type
    ON public.platform_notification_triggers(event_type);

CREATE INDEX IF NOT EXISTS idx_platform_notification_triggers_is_active
    ON public.platform_notification_triggers(is_active);
