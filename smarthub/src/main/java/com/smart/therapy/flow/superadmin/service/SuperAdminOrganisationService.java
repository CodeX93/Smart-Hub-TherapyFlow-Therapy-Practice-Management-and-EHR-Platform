package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.auth.service.TenantRbacSeedService;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantProvisioningService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminCreateOrganisationCommand;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUpdateOrganisationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Platform-level organisation management. Operates on public schema only.
 * Use from admin.yourapp.com (no tenant context). SUPER_ADMIN only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminOrganisationService {

    private final OrganisationRepository organisationRepository;
    private final TenantProvisioningService tenantProvisioningService;
    private final TenantRbacSeedService tenantRbacSeedService;

    public Page<Organisation> listOrganisations(Pageable pageable) {
        return organisationRepository.findAll(pageable);
    }

    public Organisation getOrganisation(Long id) {
        return organisationRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Organisation not found: " + id));
    }

    /**
     * Create organisation in public schema and set schema_name = tenant_{id}.
     * Optionally queue tenant schema provisioning (asynchronous migration).
     */
    @Transactional
    public Organisation createOrganisation(SuperAdminCreateOrganisationCommand command) {
        if (organisationRepository.existsBySlug(command.getSlug())) {
            throw new BadRequestException("Organisation slug already exists: " + command.getSlug());
        }
        if (command.getSubdomain() != null && !command.getSubdomain().isBlank()) {
            if (organisationRepository.findBySubdomain(command.getSubdomain().trim()).isPresent()) {
                throw new BadRequestException("Subdomain already in use: " + command.getSubdomain());
            }
        }

        // Temporary unique schema_name so we can persist and get id
        String tempSchema = "tenant_" + UUID.randomUUID().toString().replace("-", "_").substring(0, 12);
        Organisation org = Organisation.builder()
                .name(command.getName())
                .slug(command.getSlug())
                .status(command.getStatus() != null ? command.getStatus() : "ACTIVE")
                .subdomain(command.getSubdomain() != null && !command.getSubdomain().isBlank() ? command.getSubdomain().trim() : null)
                .schemaName(tempSchema)
                .build();
        org = organisationRepository.save(org);

        // Set final schema_name = tenant_{id}
        String schemaName = "tenant_" + org.getId();
        org.setSchemaName(schemaName);
        org = organisationRepository.save(org);
        log.info("Created organisation id={} schema={}", org.getId(), schemaName);
        tenantRbacSeedService.seedDefaultsForOrganisation(org.getId());

        if (Boolean.TRUE.equals(command.getProvisionSchema())) {
            tenantProvisioningService.queueTenantProvisioning(org.getId());
        }
        return organisationRepository.findById(org.getId()).orElse(org);
    }

    @Transactional
    public Organisation updateOrganisation(Long id, SuperAdminUpdateOrganisationCommand command) {
        Organisation org = getOrganisation(id);
        if (command.getName() != null && !command.getName().isBlank()) {
            org.setName(command.getName().trim());
        }
        if (command.getStatus() != null && !command.getStatus().isBlank()) {
            org.setStatus(command.getStatus().trim());
        }
        if (command.getSubdomain() != null) {
            String sub = command.getSubdomain().isBlank() ? null : command.getSubdomain().trim();
            if (sub != null && organisationRepository.findBySubdomain(sub).filter(o -> !o.getId().equals(id)).isPresent()) {
                throw new BadRequestException("Subdomain already in use: " + sub);
            }
            org.setSubdomain(sub);
        }
        return organisationRepository.save(org);
    }

    /**
     * Queue tenant schema provisioning for this organisation.
     */
    @Transactional
    public void provisionTenant(Long organisationId) {
        tenantRbacSeedService.seedDefaultsForOrganisation(organisationId);
        tenantProvisioningService.queueTenantProvisioning(organisationId);
    }

}
