package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.SessionNoteAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface SessionNoteAmendmentRepository extends JpaRepository<SessionNoteAmendment, Long> {

    @Query("""
            SELECT a FROM SessionNoteAmendment a
            WHERE a.sessionNote.id = :sessionNoteId AND a.isDeleted = false
            ORDER BY a.signedAt ASC, a.id ASC
            """)
    List<SessionNoteAmendment> findBySessionNoteId(@Param("sessionNoteId") Long sessionNoteId);
}
