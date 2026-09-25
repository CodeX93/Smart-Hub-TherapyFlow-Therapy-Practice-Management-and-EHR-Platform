package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.SessionTranscriptChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface SessionTranscriptChunkRepository extends JpaRepository<SessionTranscriptChunk, Long> {

    @Query("SELECT c FROM SessionTranscriptChunk c WHERE c.transcript.id = :transcriptId AND c.isDeleted = false ORDER BY c.chunkIndex ASC")
    List<SessionTranscriptChunk> findByTranscriptIdOrderByChunkIndexAsc(@Param("transcriptId") Long transcriptId);

    @Query("SELECT c FROM SessionTranscriptChunk c WHERE c.transcript.id = :transcriptId AND c.chunkIndex = :chunkIndex AND c.isDeleted = false")
    Optional<SessionTranscriptChunk> findByTranscriptIdAndChunkIndex(
            @Param("transcriptId") Long transcriptId,
            @Param("chunkIndex") Integer chunkIndex
    );

    @Query("SELECT COUNT(c) FROM SessionTranscriptChunk c WHERE c.transcript.id = :transcriptId AND c.isDeleted = false")
    long countByTranscriptId(@Param("transcriptId") Long transcriptId);

    @Modifying
    @Transactional
    @Query("UPDATE SessionTranscriptChunk c SET c.chunkText = null WHERE c.transcript.id = :transcriptId")
    int clearChunkTextByTranscriptId(@Param("transcriptId") Long transcriptId);
}

