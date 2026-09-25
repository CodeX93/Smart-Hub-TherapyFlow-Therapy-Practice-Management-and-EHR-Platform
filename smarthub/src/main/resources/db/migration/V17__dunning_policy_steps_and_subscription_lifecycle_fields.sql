ALTER TABLE public.dunning_policies
    ADD COLUMN IF NOT EXISTS steps_json jsonb NOT NULL DEFAULT '[]'::jsonb;

UPDATE public.dunning_policies
SET steps_json = jsonb_build_array(
        jsonb_build_object('day', 1, 'action', 'email_reminder'),
        jsonb_build_object('day', 3, 'action', 'auto_suspend'),
        jsonb_build_object('day', 7, 'action', 'cancel')
    )
WHERE steps_json = '[]'::jsonb OR steps_json IS NULL;

ALTER TABLE public.org_subscriptions
    ADD COLUMN IF NOT EXISTS last_dunning_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS notified_trial boolean NOT NULL DEFAULT false;

ALTER TABLE public.org_subscriptions
    ALTER COLUMN dunning_attempt_count SET DEFAULT 1;

UPDATE public.org_subscriptions
SET dunning_attempt_count = 1
WHERE dunning_attempt_count IS NULL OR dunning_attempt_count < 1;
