package com.smart.therapy.flow.document.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * LibraryEntryTag - junction table between library entries and tags
 * Uses composite primary key
 */
@Entity
@Table(name = "library_entry_tags", indexes = {
    @Index(name = "idx_library_entry_tag_tag", columnList = "tag_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(LibraryEntryTag.LibraryEntryTagId.class)
@ToString(exclude = { "libraryEntry", "tag" })
public class LibraryEntryTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_entry_id", nullable = false)
    private LibraryEntry libraryEntry;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private LibraryTag tag;

    /**
     * Composite key class for LibraryEntryTag
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LibraryEntryTagId implements Serializable {
        private Long libraryEntry; // Refers to LibraryEntry.id
        private Long tag; // Refers to LibraryTag.id
    }
}
