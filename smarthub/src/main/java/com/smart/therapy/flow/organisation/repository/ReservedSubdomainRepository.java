package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.ReservedSubdomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservedSubdomainRepository extends JpaRepository<ReservedSubdomain, String> {

    boolean existsBySubdomainIgnoreCase(String subdomain);
}
