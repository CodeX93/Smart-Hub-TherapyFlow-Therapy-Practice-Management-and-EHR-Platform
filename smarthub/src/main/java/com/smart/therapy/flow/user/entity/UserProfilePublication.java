package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Research publications, books, and articles authored by therapist.
 */
@Entity
@Table(name = "user_profile_publications", indexes = {
        @Index(name = "idx_publication_profile", columnList = "user_profile_id"),
        @Index(name = "idx_publication_year", columnList = "publication_year"),
        @Index(name = "idx_publication_type", columnList = "publication_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfilePublication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "publication_type", length = 50)
    private String publicationType; // "Journal Article", "Book", "Book Chapter", "Conference Paper"

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(length = 255)
    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String coauthors;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url; // Link to publication (DOI, publisher URL, etc.)

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
