package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.enums.ClientEventType;
import com.smart.therapy.flow.client.enums.EventSource;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "client_history", indexes = {
    @Index(name = "idx_client_history_client", columnList = "client_id"),
    @Index(name = "idx_client_history_event", columnList = "event_type"),
    @Index(name = "idx_client_history_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @NotNull(message = "Client is required")
    @JsonIgnore
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    @NotNull(message = "Event type is required")
    private ClientEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_source", length = 30)
    private EventSource eventSource;

    @Column(name = "from_value", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String fromValue;

    @Column(name = "to_value", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String toValue;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String metadata;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private User createdByUser;

    @Column(name = "created_by_name", length = 255)
    @Size(max = 255, message = "Created by name must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String createdByName; // Store name in case user is deleted

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_log_id")
    @JsonIgnore
    private AuditLog auditLog; // Link to HIPAA audit log if applicable

    // Helper methods
    public String getEventDescription() {
        if (this.description != null) {
            return this.description;
        }
        
        StringBuilder desc = new StringBuilder();
        desc.append(this.eventType != null ? this.eventType.getDisplayName() : "Unknown Event");
        
        if (this.fromValue != null && this.toValue != null) {
            desc.append(": Changed from '").append(this.fromValue)
                .append("' to '").append(this.toValue).append("'");
        } else if (this.toValue != null) {
            desc.append(": Set to '").append(this.toValue).append("'");
        }
        
        return desc.toString();
    }

    public boolean hasValueChange() {
        return this.fromValue != null || this.toValue != null;
    }

    public String getCreatedByDisplayName() {
        if (this.createdByUser != null) {
            return this.createdByUser.getFullName();
        }
        return this.createdByName != null ? this.createdByName : "System";
    }

    public String getEventSourceDisplay() {
        return this.eventSource != null ? this.eventSource.getDisplayName() : "Unknown";
    }
}