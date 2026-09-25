package com.smart.therapy.flow.common;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.config.EncryptionKeyConfig;
import com.smart.therapy.flow.common.config.HibernateNamingConfig;
import com.smart.therapy.flow.common.converter.EncryptedLocalDateConverter;
import com.smart.therapy.flow.common.converter.EncryptedSearchableStringConverter;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.common.tenant.CurrentTenantIdentifierResolverImpl;
import com.smart.therapy.flow.common.tenant.SchemaMultiTenantConnectionProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantScopeInterceptor;
import com.smart.therapy.flow.common.tenant.TenantScopedGuardAspect;
import com.smart.therapy.flow.organisation.entity.Organisation;
import org.assertj.core.api.Assertions;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

/** Repository fixtures live in a real tenant schema; public identities retain their explicit schema. */
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.default_schema=tenant_repository_test",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "app.encryption.provider=config",
        "app.encryption.master-key=repository-fixture-key-not-for-production"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@EnableAspectJAutoProxy
@Import({HibernateNamingConfig.class, TenantScopeInterceptor.class, TenantScopedGuardAspect.class,
        CurrentTenantIdentifierResolverImpl.class, SchemaMultiTenantConnectionProvider.class,
        EncryptionKeyConfig.class,
        EncryptionService.class,
        BlindIndexService.class,
        EncryptedStringConverter.class,
        EncryptedLocalDateConverter.class,
        EncryptedSearchableStringConverter.class,
        BaseTenantRepositoryTest.CryptoConfiguration.class})
public abstract class BaseTenantRepositoryTest {
    @DynamicPropertySource
    static void isolateFixtureDatabase(DynamicPropertyRegistry registry) {
        TenantTestDatabase.register(registry, "repository");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CryptoConfiguration {
        @Bean
        StringEncryptor stringEncryptor() {
            StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
            encryptor.setPassword("repository-legacy-fixture-only");
            return encryptor;
        }
    }

    @Test
    void shouldUsePhysicalTenantSchema() {
        Assertions.assertThat(fixtureEntityManager.getEntityManager()
                .createNativeQuery("select current_schema()").getSingleResult()).isEqualTo(SCHEMA);
    }

    @Test
    void shouldRejectEntityLoadWithoutTenantContext() {
        Long userId = fixtureEntityManager.getEntityManager()
                .createQuery("select u.id from User u", Long.class).getSingleResult();
        Long organisationId = TenantContext.getOrganisationId();
        fixtureEntityManager.clear();
        TenantContext.clear();
        try {
            Assertions.assertThatThrownBy(() -> fixtureEntityManager.find(User.class, userId))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Tenant-scoped entity");
        } finally {
            TenantContext.setSchemaName(SCHEMA);
            TenantContext.setOrganisationId(organisationId);
        }
    }

    protected static final String SCHEMA = "tenant_repository_test";

    @Autowired
    private TestEntityManager fixtureEntityManager;

    @BeforeTransaction
    void enterTenantBeforeTransaction() {
        TenantContext.clear();
        TenantContext.setSchemaName(SCHEMA);
    }

    @AfterTransaction
    void clearTenantAfterTransaction() {
        TenantContext.clear();
    }

    protected User persistTherapist() {
        Organisation organisation = TestDataFactory.createTestOrganisation();
        organisation.setId(null);
        organisation.setVersion(null);
        organisation.setSchemaName(SCHEMA);
        fixtureEntityManager.persistAndFlush(organisation);
        TenantContext.setOrganisationId(organisation.getId());

        User therapist = TestDataFactory.createTestTherapist();
        therapist.getAuthIdentity().setId(null);
        therapist.getAuthIdentity().setVersion(null);
        therapist.getAuthIdentity().setOrganisation(organisation);
        fixtureEntityManager.persistAndFlush(therapist.getAuthIdentity());
        return fixtureEntityManager.persistAndFlush(therapist);
    }
}
