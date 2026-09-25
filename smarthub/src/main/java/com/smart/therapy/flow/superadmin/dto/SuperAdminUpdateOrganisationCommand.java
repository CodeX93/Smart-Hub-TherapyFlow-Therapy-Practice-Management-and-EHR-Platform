package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

@Data
public class SuperAdminUpdateOrganisationCommand {
    private String name;
    private String status;
    private String subdomain;
}
