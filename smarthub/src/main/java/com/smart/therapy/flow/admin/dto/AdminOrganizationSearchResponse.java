package com.smart.therapy.flow.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminOrganizationSearchResponse {
    private List<AdminOrganizationListItem> items;
    private Long total;
}
