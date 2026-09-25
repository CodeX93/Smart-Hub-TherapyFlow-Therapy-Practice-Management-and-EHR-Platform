package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_session_client_date", columnList = "client_id, session_date"),
        @Index(name = "idx_session_therapist_date", columnList = "therapist_id, session_date"),
        @Index(name = "idx_session_status", columnList = "status"),
        @Index(name = "sessions_recurrence_group_id_idx", columnList = "recurrence_group_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Session extends BaseEntity {

    @Column(name = "session_date", nullable = false)
    private Instant sessionDate;

    @Column(name = "session_mode", nullable = false, length = 100)
    private String sessionType;

    @Column(name = "session_type", nullable = false, length = 100)
    private String clinicalSessionType;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String status = "scheduled";

    @Column
    private Integer duration; // Legacy field: session duration in minutes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    @Column(name = "calculated_rate", precision = 10, scale = 2)
    private java.math.BigDecimal calculatedRate; // Auto-calculated from service

    @Column(name = "insurance_applicable", nullable = false)
    @Builder.Default
    private Boolean insuranceApplicable = false;

    @Column(name = "billing_notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String billingNotes;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionNote> sessionNotes = new ArrayList<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL)
    @ToString.Exclude
    private SessionBilling billing;

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL)
    private RoomBooking roomBooking;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionIntegration> integrations = new ArrayList<>();

    @Column(name = "recurrence_group_id", length = 64)
    private String recurrenceGroupId;

    @PrePersist
    @PreUpdate
    private void normalizeSessionFields() {
        if (sessionType == null) {
            sessionType = "in-person";
        }
        if (clinicalSessionType == null || clinicalSessionType.trim().isEmpty()) {
            clinicalSessionType = "General";
        }
    }
}
