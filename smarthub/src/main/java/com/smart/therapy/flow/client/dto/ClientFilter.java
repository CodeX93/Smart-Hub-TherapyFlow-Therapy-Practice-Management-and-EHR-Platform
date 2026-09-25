package com.smart.therapy.flow.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for client queries.
 * All enum conversions happen at the controller boundary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter criteria for client queries")
public class ClientFilter {
    
    @Schema(description = "Search by full name, first/last name, email, phone, or MRN (exact match)", example = "John")
    private String search;
    
    @Schema(description = "Filter by client status option key or label", example = "active")
    private String status;
    
    @Schema(description = "Filter by client stage option key or label", example = "intake")
    private String stage;
    
    @Schema(description = "Filter by assigned therapist ID", example = "1")
    private Long therapistId;
    
    @Schema(description = "Filter by client type option key or label", example = "individual")
    private String clientType;
    
    @Schema(description = "Filter by portal access status", example = "true")
    private Boolean hasPortalAccess;
    
    @Schema(description = "Filter by pending tasks", example = "true")
    private Boolean hasPendingTasks;
    
    @Schema(description = "Filter clients with no sessions", example = "true")
    private Boolean hasNoSessions;
    
    @Schema(description = "Filter clients needing follow-up", example = "true")
    private Boolean needsFollowUp;
    
    @Schema(description = "Filter unassigned clients", example = "true")
    private Boolean unassigned;

    @Schema(
            description = "When true with therapistId (or for OWN caseload), include unassigned clients as well as that therapist's clients",
            example = "true")
    private Boolean includeUnassigned;

    @Schema(description = "Filter clients that have this checklist template assigned", example = "3")
    private Long checklistTemplateId;

    @Schema(description = "Filter clients that have a report generated with this report template", example = "2")
    private Long reportTemplateId;
}

