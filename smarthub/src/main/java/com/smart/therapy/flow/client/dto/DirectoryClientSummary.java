package com.smart.therapy.flow.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Directory-oriented client summary with contact/demographic fields needed by
 * {@link com.smart.therapy.flow.admin.service.AdminDirectoryService}, without
 * the normalized-entity lookups performed by {@link ClientResponse}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryClientSummary {

    private Long id;
    private String clientId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String status;
    private String stage;
    private String clientType;
    private String preferredLanguage;
    private Long assignedTherapistId;
    private String assignedTherapistName;
    private Boolean hasPortalAccess;
    private Instant createdAt;
    private Instant updatedAt;
}
