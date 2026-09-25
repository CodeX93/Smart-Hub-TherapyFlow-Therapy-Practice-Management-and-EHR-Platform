package com.smart.therapy.flow.billing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

/** Read-only projection of V91's migration metadata; Hibernate does not own this table. */
@Entity
@Immutable
@Getter
@Subselect("select id, organisation_id, entity_name, source_id, target_schema, target_table, target_id from public.clienthub_legacy_id_mappings")
public class ClientHubBillingOrderMapping {
    @Id private Long id;
    @Column(name = "organisation_id")
    private Long organisationId;
    @Column(name = "entity_name")
    private String entityName;
    @Column(name = "source_id")
    private String sourceId;
    @Column(name = "target_schema")
    private String targetSchema;
    @Column(name = "target_table")
    private String targetTable;
    @Column(name = "target_id")
    private Long targetId;
}
