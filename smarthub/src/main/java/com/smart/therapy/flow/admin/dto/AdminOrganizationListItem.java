package com.smart.therapy.flow.admin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AdminOrganizationListItem {
    private String id;
    private String name;
    private String status;
    private String plan;
    private Long usersCount;
    private Instant createdAt;
    private String timezone;
    private String region;
    private String dataResidency;
}
