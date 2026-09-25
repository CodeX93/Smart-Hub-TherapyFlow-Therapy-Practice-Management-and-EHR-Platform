package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SuperAdminOrganisationListResult {
    private List<SuperAdminOrganisationListItem> items;
    private Long total;
    private Integer page;
    private Integer pageSize;
}
