package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.SessionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {
    List<SessionNote> findBySessionId(Long sessionId);
    List<SessionNote> findBySession_Client_Id(Long clientId);
    List<SessionNote> findByTherapist_Id(Long therapistId);
    List<SessionNote> findBySession_Client_IdAndSession_Id(Long clientId, Long sessionId);
    
    @Query("SELECT sn FROM SessionNote sn LEFT JOIN FETCH sn.session LEFT JOIN FETCH sn.session.client LEFT JOIN FETCH sn.session.therapist WHERE sn.session.id = :sessionId")
    List<SessionNote> findBySessionIdWithRelations(@Param("sessionId") Long sessionId);
    
    @Query("SELECT sn FROM SessionNote sn LEFT JOIN FETCH sn.session LEFT JOIN FETCH sn.session.client LEFT JOIN FETCH sn.session.therapist WHERE sn.session.client.id = :clientId")
    List<SessionNote> findByClientIdWithRelations(@Param("clientId") Long clientId);
    
    @Query("SELECT sn FROM SessionNote sn LEFT JOIN FETCH sn.session LEFT JOIN FETCH sn.session.client LEFT JOIN FETCH sn.session.therapist WHERE sn.id = :id")
    java.util.Optional<SessionNote> findByIdWithRelations(@Param("id") Long id);
}





