package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SuperAdminOrganisationListItem {
    private Long id;
    private String name;
    private String slug;
    private String status;
    private String plan;
    private Long usersCount;
    private Instant createdAt;
    private String region;
    private String dataResidency;
    private String timezone;
}
