package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AudioFile entity.
 * Provides queries for retention policy management and cleanup.
 */
@Repository
@TenantScoped
public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {

    /**
     * Find audio file by session note ID
     */
    Optional<AudioFile> findBySessionNoteId(Long sessionNoteId);

    /**
     * Find all active audio files for a client
     */
    @Query("SELECT a FROM AudioFile a WHERE a.client.id = :clientId AND a.status = 'ACTIVE' AND a.isDeleted = false")
    List<AudioFile> findActiveByClientId(@Param("clientId") Long clientId);

    /**
     * Find all expired audio files (for cleanup job)
     */
    @Query("SELECT a FROM AudioFile a WHERE a.expiresAt < :now AND a.status = 'ACTIVE' AND a.isDeleted = false")
    List<AudioFile> findExpiredFiles(@Param("now") Instant now);

    /**
     * Find audio files expiring soon (for notification)
     */
    @Query("SELECT a FROM AudioFile a WHERE a.expiresAt BETWEEN :now AND :futureDate AND a.status = 'ACTIVE' AND a.isDeleted = false")
    List<AudioFile> findExpiringSoon(@Param("now") Instant now, @Param("futureDate") Instant futureDate);

    /**
     * Count active audio files for a client
     */
    @Query("SELECT COUNT(a) FROM AudioFile a WHERE a.client.id = :clientId AND a.status = 'ACTIVE' AND a.isDeleted = false")
    Long countActiveByClientId(@Param("clientId") Long clientId);

    /**
     * Soft delete expired files (for cleanup job)
     */
    @Modifying
    @Query("UPDATE AudioFile a SET a.status = 'EXPIRED', a.deletedAt = :now, a.isDeleted = true WHERE a.expiresAt < :now AND a.status = 'ACTIVE' AND a.isDeleted = false")
    int markExpiredFiles(@Param("now") Instant now);
}




