CREATE TABLE IF NOT EXISTS public.platform_tenant_routing_settings (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    email_auto_routing boolean NOT NULL DEFAULT false,
    path_based_routing boolean NOT NULL DEFAULT false,
    path_prefix character varying(100) NOT NULL,
    org_identifier character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone DEFAULT now() NOT NULL,
    updated_by_auth_id bigint,
    created_at timestamp(6) with time zone DEFAULT now() NOT NULL
);

