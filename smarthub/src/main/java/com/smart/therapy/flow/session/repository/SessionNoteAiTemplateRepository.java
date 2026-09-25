package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.session.entity.SessionNoteAiTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionNoteAiTemplateRepository extends JpaRepository<SessionNoteAiTemplate, Long> {

    List<SessionNoteAiTemplate> findByCreatedByAndIsDeletedFalseOrderByLastUsedAtDescUpdatedAtDesc(Long createdBy);

    Optional<SessionNoteAiTemplate> findByIdAndCreatedByAndIsDeletedFalse(Long id, Long createdBy);
}
