package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface PatientConsentRepository extends JpaRepository<PatientConsent, Long> {
    
    List<PatientConsent> findByClientIdOrderByCreatedAtDesc(Long clientId);
    
    @Query("SELECT pc FROM PatientConsent pc WHERE pc.client.id = :clientId AND pc.consentType = :consentType ORDER BY pc.createdAt DESC, pc.id DESC")
    List<PatientConsent> findByClientIdAndConsentType(@Param("clientId") Long clientId, @Param("consentType") ConsentType consentType);

    default Optional<PatientConsent> findLatestByClientIdAndConsentType(Long clientId, ConsentType consentType) {
        List<PatientConsent> consents = findByClientIdAndConsentType(clientId, consentType);
        return consents.isEmpty() ? Optional.empty() : Optional.of(consents.get(0));
    }

    /** @deprecated Use {@link #findLatestByClientIdAndConsentType(Long, ConsentType)} */
    default Optional<PatientConsent> findFirstByClientIdAndConsentTypeOrderByCreatedAtDesc(Long clientId, ConsentType consentType) {
        return findLatestByClientIdAndConsentType(clientId, consentType);
    }
}





