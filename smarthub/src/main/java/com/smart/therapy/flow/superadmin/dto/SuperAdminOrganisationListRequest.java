package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SuperAdminOrganisationListRequest {
    private String search;
    private String status;
    private String plan;
    private LocalDate createdFrom;
    private LocalDate createdTo;
    private String region;
    private String dataResidency;
    private Integer page = 1;
    private Integer pageSize = 25;
    private String sort = "createdAt";
    private String order = "desc";
    private Boolean exportCsv = false;
}
