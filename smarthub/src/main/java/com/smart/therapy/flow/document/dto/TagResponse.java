package com.smart.therapy.flow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for library tag with usage statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {
    
    /**
     * Unique tag ID
     */
    private Long id;
    
    /**
     * Tag name
     */
    private String name;
    
    /**
     * Number of library entries using this tag
     */
    private Long usageCount;
    
    /**
     * When the tag was created
     */
    private Instant createdAt;
    
    /**
     * When the tag was last updated
     */
    private Instant updatedAt;
}
