package com.smart.therapy.flow.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Alternative subdomain for an organisation. Primary subdomain remains immutable;
 * aliases allow redirects (e.g. after rebrand) without breaking URLs. Resolved by TenantDirectoryService.
 */
@Entity
@Table(name = "organisation_domain_aliases", schema = "public", uniqueConstraints = {
    @UniqueConstraint(name = "uq_organisation_domain_aliases_alias", columnNames = "alias_subdomain")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationDomainAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "alias_subdomain", nullable = false, unique = true, length = 63)
    private String aliasSubdomain;
}
