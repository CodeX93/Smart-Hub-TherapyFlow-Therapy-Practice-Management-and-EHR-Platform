package com.smart.therapy.flow.common.config;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * Prevents immutable historical seed SQL from ever making a production identity active.
 * Local/test profiles do not register this callback, so their fixtures remain available.
 */
@Component
@RequiredArgsConstructor
public class ProductionSeedIdentityMigrationBlocker implements Callback {
    static final String TRIGGER_NAME = "trg_block_production_seed_identity_activation";
    static final String FUNCTION_NAME = "block_production_seed_identity_activation";

    private static final String INSTALL_SQL = """
            DO $$
            BEGIN
                IF to_regclass('public.auth_identities') IS NOT NULL THEN
                    CREATE OR REPLACE FUNCTION public.block_production_seed_identity_activation()
                    RETURNS trigger
                    LANGUAGE plpgsql
                    AS $function$
                    BEGIN
                        IF lower(COALESCE(NEW.normalised_login_identifier, '')) LIKE '%@therapyflowseed.com'
                           OR lower(COALESCE(NEW.normalised_login_identifier, '')) IN (
                               'superadmin@therapyflow.com',
                               'admin@therapyflow.com',
                               'supervisor@therapyflow.com',
                               'therapist@therapyflow.com',
                               'superadmin@therapyflow.pro',
                               'superadmin2@smarthub.com',
                               'amjad-superadmin@therapyflow.pro',
                               'aqeel-superadmin@therapyflow.pro'
                           ) THEN
                            NEW.is_active := false;
                            NEW.account_locked := true;
                            NEW.email_verified := false;
                            NEW.password_hash := NULL;
                            NEW.is_deleted := true;
                            NEW.deleted_at := NOW();
                            NEW.provider_user_id := NULL;
                        END IF;
                        RETURN NEW;
                    END;
                    $function$;

                    DROP TRIGGER IF EXISTS trg_block_production_seed_identity_activation
                        ON public.auth_identities;
                    CREATE TRIGGER trg_block_production_seed_identity_activation
                        BEFORE INSERT OR UPDATE ON public.auth_identities
                        FOR EACH ROW
                        EXECUTE FUNCTION public.block_production_seed_identity_activation();
                END IF;
            END
            $$;
            """;

    private static final String REMOVE_SQL = """
            DO $$
            BEGIN
                IF to_regclass('public.auth_identities') IS NOT NULL THEN
                    DROP TRIGGER IF EXISTS trg_block_production_seed_identity_activation
                        ON public.auth_identities;
                    DROP FUNCTION IF EXISTS public.block_production_seed_identity_activation();
                END IF;
            END
            $$;
            """;

    private final JdbcTemplate jdbcTemplate;

    public void customize(FluentConfiguration configuration) {
        configuration.callbacks(this);
    }

    public void removeBlocker() {
        jdbcTemplate.execute(REMOVE_SQL);
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_EACH_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        try {
            context.getConnection().createStatement().execute(INSTALL_SQL);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to install production seed identity blocker", e);
        }
    }

    @Override
    public String getCallbackName() {
        return "production-seed-identity-blocker";
    }
}
