package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

@Data
public class SuperAdminCreateOrganisationCommand {
    private String name;
    private String slug;
    private String status;
    private String subdomain;
    private Boolean provisionSchema;
}
