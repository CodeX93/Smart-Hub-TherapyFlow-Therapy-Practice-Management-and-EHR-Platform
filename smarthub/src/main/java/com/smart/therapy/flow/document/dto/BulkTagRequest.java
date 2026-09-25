package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for bulk tag operations on multiple library entries
 */
@Data
public class BulkTagRequest {
    
    /**
     * List of library entry IDs to apply tags to
     */
    @NotNull(message = "Entry IDs are required")
    @NotEmpty(message = "At least one entry ID must be provided")
    private List<Long> entryIds;
    
    /**
     * List of tag names to add to the entries
     */
    private List<String> tagsToAdd;
    
    /**
     * List of tag names to remove from the entries
     */
    private List<String> tagsToRemove;
}
