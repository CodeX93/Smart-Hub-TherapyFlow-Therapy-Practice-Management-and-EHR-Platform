package com.smart.therapy.flow.common.config;

import com.smart.therapy.flow.common.tenant.TenantScopeInterceptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures Hibernate uses column names from @Column(name = "...") as-is.
 * Registers TenantScopeInterceptor so tenant-scoped entities cannot be used when schema is public.
 */
@Configuration
public class HibernateNamingConfig {

    @Bean
    public PhysicalNamingStrategy physicalNamingStrategy() {
        return PhysicalNamingStrategyStandardImpl.INSTANCE;
    }

    /** Force naming strategies and tenant-scope interceptor at EntityManagerFactory build time. */
    @Bean
    public HibernatePropertiesCustomizer hibernateNamingCustomizer(TenantScopeInterceptor tenantScopeInterceptor) {
        return hibernateProperties -> {
            hibernateProperties.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, PhysicalNamingStrategyStandardImpl.INSTANCE);
            hibernateProperties.put(AvailableSettings.IMPLICIT_NAMING_STRATEGY, ImplicitNamingStrategyJpaCompliantImpl.INSTANCE);
            hibernateProperties.put(AvailableSettings.INTERCEPTOR, tenantScopeInterceptor);
        };
    }
}
