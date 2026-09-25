package com.smart.therapy.flow.report.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "report_supporting_files", indexes = {
        @Index(name = "idx_report_supporting_files_client", columnList = "client_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReportSupportingFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @Column(name = "original_name", nullable = false, length = 500)
    @Convert(converter = EncryptedStringConverter.class)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 150)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "file_blob_name", length = 1000)
    private String fileBlobName;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "document_type", length = 150)
    private String documentType;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String extractedText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @JsonIgnore
    private User createdByUser;
}
