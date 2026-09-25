package com.smart.therapy.flow.auth.dto;

import com.smart.therapy.flow.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthMeResponse {

    private Long authId;
    private String username;
    private String email;
    private String identityType;
    private String tenantSchema;
    private Long organisationId;
    private List<String> roles;
    private List<String> permissions;
    private List<String> authorities;
    private Boolean isPlatformAdmin;
    private Boolean isTenantAdmin;
    private Boolean isTherapist;
    private Boolean isSupervisor;
    private Boolean isClient;
    private UserResponse user;
    private ClientSummary client;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientSummary {
        private Long id;
        private String fullName;
        private String email;
        private String portalEmail;
        private String phone;
    }
}
