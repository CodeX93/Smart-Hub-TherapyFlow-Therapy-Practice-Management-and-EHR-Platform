package com.smart.therapy.flow.document.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.ConnectionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "library_entry_connections", indexes = {
    @Index(name = "idx_library_connection_from", columnList = "from_entry_id"),
    @Index(name = "idx_library_connection_to", columnList = "to_entry_id"),
    @Index(name = "idx_library_connection_unique", columnList = "from_entry_id, to_entry_id, connection_type", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = { "fromEntry", "toEntry", "createdByUser" })
public class LibraryEntryConnection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_entry_id", nullable = false)
    @NotNull(message = "From entry is required")
    @JsonIgnore
    private LibraryEntry fromEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_entry_id", nullable = false)
    @NotNull(message = "To entry is required")
    @JsonIgnore
    private LibraryEntry toEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 50)
    @NotNull(message = "Connection type is required")
    private ConnectionType connectionType;

    @Column(columnDefinition = "TEXT")
    private String description; // Optional description of the connection

    @Column
    @Builder.Default
    @Min(value = 1, message = "Strength must be between 1 and 5")
    @Max(value = 5, message = "Strength must be between 1 and 5")
    private Integer strength = 1; // 1-5 scale for connection strength

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @NotNull(message = "Created by user is required")
    @JsonIgnore
    private User createdByUser;

    // Helper methods
    public boolean isBidirectional() {
        return ConnectionType.RELATED.equals(this.connectionType);
    }

    public void deactivate() {
        this.isActive = false;
    }

    // For backward compatibility with String-based connection types
    public String getConnectionTypeString() {
        return connectionType != null ? connectionType.name() : null;
    }
}
