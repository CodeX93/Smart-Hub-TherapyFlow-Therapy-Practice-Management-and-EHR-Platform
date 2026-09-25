package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.SessionTranscript;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface SessionTranscriptRepository extends JpaRepository<SessionTranscript, Long> {

    @Query("SELECT st FROM SessionTranscript st WHERE st.uploadId = :uploadId AND st.isDeleted = false")
    Optional<SessionTranscript> findByUploadId(@Param("uploadId") String uploadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM SessionTranscript st WHERE st.uploadId = :uploadId AND st.isDeleted = false")
    Optional<SessionTranscript> findByUploadIdForUpdate(@Param("uploadId") String uploadId);

    @Query("SELECT st FROM SessionTranscript st WHERE st.session.id = :sessionId AND st.isDeleted = false ORDER BY st.createdAt DESC")
    List<SessionTranscript> findBySessionIdOrderByCreatedAtDesc(@Param("sessionId") Long sessionId);

    @Query("SELECT st FROM SessionTranscript st WHERE st.client.id = :clientId AND st.isDeleted = false ORDER BY st.updatedAt DESC")
    List<SessionTranscript> findByClientIdOrderByUpdatedAtDesc(@Param("clientId") Long clientId);

    @Query("SELECT st FROM SessionTranscript st WHERE st.isDeleted = false ORDER BY st.updatedAt DESC")
    List<SessionTranscript> findAllActiveOrderByUpdatedAtDesc();

    @Query("SELECT st FROM SessionTranscript st WHERE st.session.id = :sessionId AND st.status IN :statuses AND st.isDeleted = false")
    List<SessionTranscript> findBySessionIdAndStatusIn(
            @Param("sessionId") Long sessionId,
            @Param("statuses") Collection<SessionTranscriptStatus> statuses
    );
    @Query("SELECT st.session.id FROM SessionTranscript st WHERE st.session.id IN :sessionIds AND st.status != 'EXPIRED' AND st.isDeleted = false")
    List<Long> findSessionIdsWithTranscripts(@Param("sessionIds") Collection<Long> sessionIds);
}

