package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_responses", indexes = {
        @Index(name = "idx_assessment_response_assignment", columnList = "assignment_id"),
        @Index(name = "idx_assessment_response_question", columnList = "question_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentResponse extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssessmentAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    // Multi-responder support: CLIENT or USER
    @Column(name = "responder_type", length = 20, nullable = false)
    private String responderType; // CLIENT or USER

    @Column(name = "responder_user_id")
    private Long responderUserId; // Nullable - set when responderType = USER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_user_id", insertable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User responderUser; // User responder (therapist, admin, supervisor) - read-only, excluded from builder

    @Column(name = "responder_client_id")
    private Long responderClientId; // Nullable - set when responderType = CLIENT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_client_id", insertable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private com.smart.therapy.flow.client.entity.Client responderClient; // Client responder - read-only, excluded from builder

    // Manual getters for @ManyToOne fields (excluded from Lombok @Getter to avoid builder conflicts)
    public User getResponderUser() {
        return responderUser;
    }

    public com.smart.therapy.flow.client.entity.Client getResponderClient() {
        return responderClient;
    }

    // Legacy getter for backward compatibility (returns User if responderType = USER)
    public User getResponder() {
        if ("USER".equals(responderType) && responderUser != null) {
            return responderUser;
        }
        return null; // For CLIENT type, return null (use getResponderClient() instead)
    }

    // Legacy setter for backward compatibility
    public void setResponder(User user) {
        if (user != null) {
            this.responderType = "USER";
            this.responderUserId = user.getId();
            this.responderUser = user;
            this.responderClientId = null;
            this.responderClient = null;
        }
    }

    @Column(name = "response_text", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String responseText; // For text responses

    @Column(name = "response_value", length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String responseValue; // Optional single value

    @Column(precision = 10, scale = 2)
    private BigDecimal score; // Total score for this response

    @Column(name = "answered_at")
    private java.time.Instant answeredAt;

    // -------------------
    // Selected options
    // -------------------
    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentResponseOption> selectedOptions = new ArrayList<>();

    // Helper methods for service compatibility
    public BigDecimal getScoreValue() {
        return this.score;
    }

    public void setScoreValue(BigDecimal score) {
        this.score = score;
    }

    public Integer getRatingValue() {
        if (responseValue == null)
            return null;
        try {
            return Integer.parseInt(responseValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setRatingValue(Integer rating) {
        this.responseValue = rating != null ? String.valueOf(rating) : null;
    }
}
