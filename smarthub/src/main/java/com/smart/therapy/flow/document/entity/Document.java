package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.DocumentCategory;
import com.smart.therapy.flow.document.enums.DocumentScanStatus;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_client", columnList = "client_id"),
    @Index(name = "idx_document_category", columnList = "category"),
    @Index(name = "idx_document_type", columnList = "document_type"),
    @Index(name = "idx_document_needs_review", columnList = "needs_review")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy; // Nullable for client uploads

    @Column(name = "file_name", nullable = false, columnDefinition = "TEXT")
    private String fileName;

    @Column(name = "original_name", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String originalName;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    // DB column is smallint (tenant baseline); must stay ORDINAL — do not switch to STRING.
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "category", nullable = false)
    private DocumentCategory category; // uploaded, shared, generated, forms, insurance

    @Column(name = "is_shared_in_portal", nullable = false)
    @Builder.Default
    private Boolean isSharedInPortal = false;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    // Review workflow fields
    @Column(name = "needs_review", nullable = false)
    @Builder.Default
    private Boolean needsReview = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING; // pending, therapist_review, supervisor_review, approved, rejected, overdue

    @Column(name = "review_due_at")
    private Instant reviewDueAt; // When the document review is due

    @Column(name = "reviewed_at")
    private Instant reviewedAt; // When the document was reviewed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy; // User who reviewed the document

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String description; // Optional description of the document

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50)
    private DocumentType documentType; // More specific type: 'intake_form', 'consent', 'insurance_card', etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", length = 20)
    private DocumentScanStatus scanStatus;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    @Column(name = "scan_detail", columnDefinition = "TEXT")
    private String scanDetail;

    @Column(name = "content_checksum", length = 64)
    private String contentChecksum;
}
