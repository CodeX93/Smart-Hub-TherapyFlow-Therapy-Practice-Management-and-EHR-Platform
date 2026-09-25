package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientContactRepository extends JpaRepository<ClientContact, Long> {

    @Query("SELECT c FROM ClientContact c WHERE c.client.id = :clientId ORDER BY c.displayOrder, c.id")
    List<ClientContact> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT c FROM ClientContact c WHERE c.client.id = :clientId AND c.contactType = :contactType ORDER BY c.isPrimary DESC, c.displayOrder, c.id")
    List<ClientContact> findByClientIdAndContactType(@Param("clientId") Long clientId,
            @Param("contactType") ContactType contactType);

    @Query("SELECT c FROM ClientContact c WHERE c.client.id = :clientId AND c.isPrimary = true AND c.contactType = :contactType")
    Optional<ClientContact> findPrimaryByClientIdAndContactType(@Param("clientId") Long clientId,
            @Param("contactType") ContactType contactType);

    @Query("SELECT c FROM ClientContact c WHERE c.contactValue = :contactValue")
    List<ClientContact> findByContactValue(@Param("contactValue") String contactValue);

    @Query("SELECT c FROM ClientContact c WHERE c.contactBlindIdx = :blindIdx")
    List<ClientContact> findByContactBlindIdx(@Param("blindIdx") byte[] blindIdx);

    @Query("""
            SELECT COUNT(c) > 0 FROM ClientContact c
            WHERE c.client.id = :clientId
              AND c.contactType = :contactType
              AND c.contactBlindIdx = :blindIdx
            """)
    boolean existsByClientIdAndContactTypeAndContactBlindIdx(
            @Param("clientId") Long clientId,
            @Param("contactType") ContactType contactType,
            @Param("blindIdx") byte[] blindIdx);

    @Query("SELECT c FROM ClientContact c WHERE c.client.id = :clientId AND c.contactType = :emailType AND c.isPrimary = true")
    Optional<ClientContact> findPrimaryEmailByClientId(@Param("clientId") Long clientId,
            @Param("emailType") ContactType emailType);

    @Query("SELECT c FROM ClientContact c WHERE c.client.id = :clientId AND c.contactType IN :phoneTypes AND c.isPrimary = true")
    Optional<ClientContact> findPrimaryPhoneByClientId(@Param("clientId") Long clientId,
            @Param("phoneTypes") List<ContactType> phoneTypes);

    @Query("SELECT c FROM ClientContact c WHERE c.client.id = :clientId AND c.contactType = :emergencyType")
    List<ClientContact> findEmergencyContactsByClientId(@Param("clientId") Long clientId,
            @Param("emergencyType") ContactType emergencyType);

    @Query("SELECT COUNT(c) > 0 FROM ClientContact c WHERE c.client.id = :clientId AND c.contactType = :contactType AND c.contactValue = :contactValue")
    boolean existsByClientIdAndContactTypeAndContactValue(
            @Param("clientId") Long clientId,
            @Param("contactType") ContactType contactType,
            @Param("contactValue") String contactValue);

    @Query("SELECT DISTINCT c.client FROM ClientContact c WHERE c.contactValue = :email AND c.contactType = com.smart.therapy.flow.client.enums.ContactType.EMAIL AND (c.client.isDeleted IS NULL OR c.client.isDeleted = false)")
    List<com.smart.therapy.flow.client.entity.Client> findActiveClientsByEmail(@Param("email") String email);

    @Query("SELECT c FROM ClientContact c WHERE c.contactType IN :phoneTypes AND (c.client.isDeleted IS NULL OR c.client.isDeleted = false)")
    List<ClientContact> findActivePhoneContacts(@Param("phoneTypes") List<ContactType> phoneTypes);
}




