package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.document.dto.TagResponse;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import com.smart.therapy.flow.document.entity.LibraryEntryTag;
import com.smart.therapy.flow.document.entity.LibraryTag;
import com.smart.therapy.flow.document.repository.LibraryEntryRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryTagRepository;
import com.smart.therapy.flow.document.repository.LibraryTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing library tags and their associations with library entries
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryTagService {

    private final LibraryTagRepository tagRepository;
    private final LibraryEntryTagRepository entryTagRepository;
    private final LibraryEntryRepository entryRepository;

    /**
     * Find an existing tag by name or create a new one if it doesn't exist
     * Uses case-insensitive lookup to avoid duplicate tags with different casing
     * 
     * @param tagName the name of the tag
     * @return the found or newly created LibraryTag
     */
    @Transactional
    public LibraryTag findOrCreateTag(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }

        String trimmedName = tagName.trim();
        
        // Try to find existing tag (case-insensitive)
        return tagRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> {
                    // Create new tag if not found
                    LibraryTag newTag = LibraryTag.builder()
                            .name(trimmedName)
                            .build();
                    LibraryTag saved = tagRepository.save(newTag);
                    log.debug("Created new library tag: {}", trimmedName);
                    return saved;
                });
    }

    /**
     * Find or create multiple tags in bulk
     * 
     * @param tagNames list of tag names
     * @return list of LibraryTag entities
     */
    @Transactional
    public List<LibraryTag> findOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        return tagNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(this::findOrCreateTag)
                .collect(Collectors.toList());
    }

    /**
     * Assign tags to a library entry by creating junction table records
     * Does NOT delete existing tags - use updateEntryTags for full replacement
     * 
     * @param entry the library entry
     * @param tagNames list of tag names to assign
     */
    @Transactional
    public void assignTagsToEntry(LibraryEntry entry, List<String> tagNames) {
        if (entry == null) {
            throw new IllegalArgumentException("Library entry cannot be null");
        }

        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        // Find or create all tags
        List<LibraryTag> tags = findOrCreateTags(tagNames);

        // Create junction records
        List<LibraryEntryTag> entryTags = tags.stream()
                .map(tag -> LibraryEntryTag.builder()
                        .libraryEntry(entry)
                        .tag(tag)
                        .build())
                .collect(Collectors.toList());

        entryTagRepository.saveAll(entryTags);
        log.debug("Assigned {} tags to library entry {}", entryTags.size(), entry.getId());
    }

    /**
     * Update entry tags by removing all existing tags and assigning new ones
     * 
     * @param entry the library entry
     * @param tagNames list of new tag names (replaces all existing)
     */
    @Transactional
    public void updateEntryTags(LibraryEntry entry, List<String> tagNames) {
        if (entry == null) {
            throw new IllegalArgumentException("Library entry cannot be null");
        }

        // Delete all existing tag associations
        entryTagRepository.deleteByLibraryEntryId(entry.getId());
        log.debug("Cleared existing tags for library entry {}", entry.getId());

        // Assign new tags
        if (tagNames != null && !tagNames.isEmpty()) {
            assignTagsToEntry(entry, tagNames);
        }
    }

    /**
     * Get all tag names for a library entry
     * Converts LibraryEntryTag entities to simple string list for DTOs
     * 
     * @param entryId the library entry ID
     * @return list of tag names
     */
    @Transactional(readOnly = true)
    public List<String> getTagNamesForEntry(Long entryId) {
        if (entryId == null) {
            return new ArrayList<>();
        }

        return entryTagRepository.findByLibraryEntryId(entryId).stream()
                .map(LibraryEntryTag::getTag)
                .filter(tag -> tag != null)
                .map(LibraryTag::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get all available tags in the system, ordered alphabetically
     * 
     * @return list of all tags
     */
    @Transactional(readOnly = true)
    public List<LibraryTag> getAllTags() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get all tag names as strings
     * 
     * @return list of all tag names
     */
    @Transactional(readOnly = true)
    public List<String> getAllTagNames() {
        return getAllTags().stream()
                .map(LibraryTag::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get a single tag with usage statistics
     * 
     * @param tagId the tag ID
     * @return TagResponse with usage count
     */
    @Transactional(readOnly = true)
    public TagResponse getTagWithStats(Long tagId) {
        LibraryTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new com.smart.therapy.flow.common.exception.ResourceNotFoundException(
                        "Tag not found with ID: " + tagId));
        
        long usageCount = entryTagRepository.countByTagId(tagId);
        
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .usageCount(usageCount)
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }

    /**
     * Get all tags with usage statistics, sorted by name
     * 
     * @return list of TagResponse objects with usage counts
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTagsWithStats() {
        return getAllTags().stream()
                .map(tag -> TagResponse.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .usageCount(entryTagRepository.countByTagId(tag.getId()))
                        .createdAt(tag.getCreatedAt())
                        .updatedAt(tag.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Add tags to multiple library entries in bulk
     * 
     * @param entryIds list of library entry IDs
     * @param tagNames list of tag names to add
     */
    @Transactional
    public void addTagsToEntries(List<Long> entryIds, List<String> tagNames) {
        if (entryIds == null || entryIds.isEmpty() || tagNames == null || tagNames.isEmpty()) {
            return;
        }

        // Find or create all tags once
        List<LibraryTag> tags = findOrCreateTags(tagNames);

        // Process each entry
        for (Long entryId : entryIds) {
            LibraryEntry entry = entryRepository.findById(entryId).orElse(null);
            if (entry != null) {
                // Get existing tags for this entry
                List<Long> existingTagIds = entryTagRepository.findByLibraryEntryId(entryId).stream()
                        .map(et -> et.getTag().getId())
                        .collect(Collectors.toList());

                // Only add tags that don't already exist
                List<LibraryEntryTag> newEntryTags = tags.stream()
                        .filter(tag -> !existingTagIds.contains(tag.getId()))
                        .map(tag -> LibraryEntryTag.builder()
                                .libraryEntry(entry)
                                .tag(tag)
                                .build())
                        .collect(Collectors.toList());

                if (!newEntryTags.isEmpty()) {
                    entryTagRepository.saveAll(newEntryTags);
                    log.debug("Added {} tags to library entry {}", newEntryTags.size(), entryId);
                }
            }
        }
    }

    /**
     * Remove tags from multiple library entries in bulk
     * 
     * @param entryIds list of library entry IDs
     * @param tagNames list of tag names to remove
     */
    @Transactional
    public void removeTagsFromEntries(List<Long> entryIds, List<String> tagNames) {
        if (entryIds == null || entryIds.isEmpty() || tagNames == null || tagNames.isEmpty()) {
            return;
        }

        // Find tag IDs for the given names
        List<Long> tagIdsToRemove = tagNames.stream()
                .map(name -> tagRepository.findByNameIgnoreCase(name))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get().getId())
                .collect(Collectors.toList());

        if (tagIdsToRemove.isEmpty()) {
            return;
        }

        // Process each entry
        for (Long entryId : entryIds) {
            List<LibraryEntryTag> entryTags = entryTagRepository.findByLibraryEntryId(entryId);
            List<LibraryEntryTag> tagsToDelete = entryTags.stream()
                    .filter(et -> et.getTag() != null && tagIdsToRemove.contains(et.getTag().getId()))
                    .collect(Collectors.toList());

            if (!tagsToDelete.isEmpty()) {
                entryTagRepository.deleteAll(tagsToDelete);
                log.debug("Removed {} tags from library entry {}", tagsToDelete.size(), entryId);
            }
        }
    }

    /**
     * Delete a tag if it's not being used by any library entries
     * 
     * @param tagId the tag ID to delete
     * @return true if deleted, false if tag is in use
     */
    @Transactional
    public boolean deleteUnusedTag(Long tagId) {
        long usageCount = entryTagRepository.countByTagId(tagId);
        
        if (usageCount == 0) {
            tagRepository.deleteById(tagId);
            log.info("Deleted unused library tag: {}", tagId);
            return true;
        }
        
        log.warn("Cannot delete library tag {} - still in use by {} entries", tagId, usageCount);
        return false;
    }
}
