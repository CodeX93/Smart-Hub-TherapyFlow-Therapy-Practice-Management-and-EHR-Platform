package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.OrganisationSsoState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OrganisationSsoStateRepository extends JpaRepository<OrganisationSsoState, Long> {

    Optional<OrganisationSsoState> findByStateToken(String stateToken);

    void deleteByExpiresAtBefore(Instant instant);
}

