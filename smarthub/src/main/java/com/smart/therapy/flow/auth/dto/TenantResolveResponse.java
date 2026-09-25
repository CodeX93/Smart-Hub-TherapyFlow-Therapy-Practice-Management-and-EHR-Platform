package com.smart.therapy.flow.auth.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TenantResolveResponse {
    String email;
    List<TenantResolveItem> organisations;
    int count;

    @Value
    @Builder
    public static class TenantResolveItem {
        Long organisationId;
        String name;
        String slug;
        String subdomain;
        String status;
    }
}

