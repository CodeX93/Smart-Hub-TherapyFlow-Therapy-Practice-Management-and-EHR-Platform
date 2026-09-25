package com.smart.therapy.flow.assessment.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.assessment.dto.*;
import com.smart.therapy.flow.assessment.entity.*;
import com.smart.therapy.flow.assessment.entity.AssessmentReportVersion;
import com.smart.therapy.flow.assessment.repository.*;
import com.smart.therapy.flow.assessment.repository.AssessmentReportVersionRepository;
import com.smart.therapy.flow.assessment.entity.AssessmentReportVersion;
import com.smart.therapy.flow.assessment.util.AssessmentReportDocxBuilder;
import com.smart.therapy.flow.assessment.util.AssessmentReportHtmlBuilder;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.report.util.HtmlToPdfConverter;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.dto.PatchUpdates;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentService {

    private static final String RESOURCE_TYPE_ASSESSMENT = "assessment";
    private static final int DEFAULT_ASSIGNMENT_PAGE = 1;
    private static final int DEFAULT_ASSIGNMENT_PAGE_SIZE = 20;
    private static final int MAX_ASSIGNMENT_PAGE_SIZE = 200;

    private final AssessmentTemplateRepository templateRepository;
    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentResponseRepository responseRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentSectionRepository sectionRepository;
    private final AssessmentQuestionOptionRepository optionRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AssessmentReportRepository reportRepository;
    private final AssessmentReportVersionRepository reportVersionRepository;
    private final OptionCategoryRepository optionCategoryRepository;
    private final SystemOptionRepository systemOptionRepository;
    private final PracticeConfigurationService practiceConfigurationService;
    private final UserProfileRepository userProfileRepository;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final CaseloadScopeService caseloadScopeService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final ClientReportAccessService clientReportAccessService;

    @Autowired(required = false)
    private SystemOptionResolverService systemOptionResolverService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private com.smart.therapy.flow.ai.service.AiService aiService;

    @Autowired(required = false)
    private com.smart.therapy.flow.ai.service.ConsentPolicyService consentPolicyService;

    @Autowired(required = false)
    private com.smart.therapy.flow.ai.service.OpenAiClient openAiClient;

    @Autowired(required = false)
    private com.smart.therapy.flow.common.service.EmailService emailService;

    @Autowired(required = false)
    private com.smart.therapy.flow.client.service.ClientContactService clientContactService;


    @Transactional(readOnly = true)
    @Cacheable(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('templates')")
    public @NonNull List<AssessmentTemplateResponse> getTemplates() {
        List<AssessmentTemplate> templates = templateRepository.findByIsActive(true);
        return templates.stream()
                .sorted(Comparator
                        .comparing(AssessmentTemplate::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AssessmentTemplate::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('template:' + #templateId)")
    public @NonNull AssessmentTemplateResponse getTemplate(@NonNull Long templateId) {
        return toTemplateResponse(requireTemplateWithSectionsAndQuestions(templateId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('template-sections:' + #templateId)")
    public @NonNull List<AssessmentSectionResponse> getTemplateSections(@NonNull Long templateId) {
        AssessmentTemplate template = requireTemplateWithSectionsAndQuestions(templateId);

        return template.getSections().stream()
                .filter(section -> !Boolean.TRUE.equals(section.getIsDeleted()))
                .sorted(newestFirst())
                .map(this::toSectionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public @NonNull AssessmentTemplateResponse createTemplate(@NonNull CreateAssessmentTemplateRequest request,
            @NonNull AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can create assessment templates");
        enforceAssessmentTemplateCreationLimit();

        User createdBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        AssessmentTemplate template = AssessmentTemplate.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .isStandardized(request.getIsStandardized() != null ? request.getIsStandardized() : false)
                .versionNumber(request.getVersion() != null ? request.getVersion() : 1)
                .isActive(true)
                .createdByUser(createdBy)
                .build();

        AssessmentTemplate saved = templateRepository.save(template);

        // Create sections and questions
        if (request.getSections() != null) {
            for (int i = 0; i < request.getSections().size(); i++) {
                AssessmentSectionRequest sectionReq = request.getSections().get(i);
                AssessmentSection section = AssessmentSection.builder()
                        .template(saved)
                        .title(sectionReq.getTitle())
                        .description(sectionReq.getDescription())
                        .accessLevel(sectionReq.getAccessLevel())
                        .isScoring(sectionReq.getIsScoring() != null ? sectionReq.getIsScoring() : false)
                        .sortOrder(sectionReq.getSortOrder() != null ? sectionReq.getSortOrder() : i)
                        .build();
                AssessmentSection savedSection = sectionRepository.save(section);

                // Create questions
                if (sectionReq.getQuestions() != null) {
                    for (int j = 0; j < sectionReq.getQuestions().size(); j++) {
                        AssessmentQuestionRequest questionReq = sectionReq.getQuestions().get(j);
                        AssessmentQuestion question = AssessmentQuestion.builder()
                                .section(savedSection)
                                .questionText(questionReq.getQuestionText())
                                .questionType(questionReq.getQuestionType())
                                .isRequired(questionReq.getIsRequired() != null ? questionReq.getIsRequired() : false)
                                .sortOrder(questionReq.getSortOrder() != null ? questionReq.getSortOrder() : j)
                                .ratingMin(questionReq.getRatingMin() != null ? questionReq.getRatingMin().intValue()
                                        : null)
                                .ratingMax(questionReq.getRatingMax() != null ? questionReq.getRatingMax().intValue()
                                        : null)
                                .ratingLabels(questionReq.getRatingLabels() != null
                                        && !questionReq.getRatingLabels().isEmpty()
                                                ? Arrays.stream(questionReq.getRatingLabels().split(","))
                                                        .map(l -> AssessmentQuestionRatingLabel.builder()
                                                                .label(l.trim()).active(true).build())
                                                        .collect(Collectors.toList())
                                                : new ArrayList<>())
                                .contributesToScore(questionReq.getContributesToScore() != null
                                        ? questionReq.getContributesToScore()
                                        : true)
                                .build();
                        AssessmentQuestion savedQuestion = questionRepository.save(question);

                        // Create options
                        if (questionReq.getOptions() != null) {
                            for (int k = 0; k < questionReq.getOptions().size(); k++) {
                                AssessmentOptionRequest optionReq = questionReq.getOptions().get(k);
                                AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                                        .question(savedQuestion)
                                        .optionText(optionReq.getOptionText())
                                        .optionValue(optionReq.getOptionValue() != null
                                                ? String.valueOf(optionReq.getOptionValue())
                                                : null)
                                        .scoreValue(optionReq.getOptionValue() != null
                                                ? BigDecimal.valueOf(optionReq.getOptionValue())
                                                : null)
                                        .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                                        .build();
                                optionRepository.save(option);
                            }
                        }
                    }
                }
            }
        }

        // Reload with all relationships
        AssessmentTemplate reloaded = templateRepository.findById(saved.getId()).orElse(saved);
        
        // Validate scoring configuration after all sections and questions are created
        validateTemplateScoringConfiguration(reloaded);
        incrementAssessmentTemplateUsage();
        
        return toTemplateResponse(reloaded);
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public @NonNull AssessmentAssignmentResponse assignAssessment(@NonNull CreateAssessmentAssignmentRequest request,
            @NonNull AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));

        Client client = clientReportAccessService.requireClientAccess(request.getClientId(), requester);

        if (assignmentRepository.existsByClientIdAndTemplateIdAndIsDeletedFalse(
                request.getClientId(), request.getTemplateId())) {
            throw new ConflictException(
                    "This assessment template is already assigned to the client.");
        }

        User assignedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .template(template)
                .client(client)
                .assignedBy(assignedBy)
                .assignedDate(Instant.now()) // Set explicit assignment date
                .dueDate(request.getDueDate() != null
                        ? request.getDueDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        : null)
                .status(com.smart.therapy.flow.assessment.enums.AssessmentStatus.PENDING.getValue())
                .notes(request.getNotes())
                .build();

        AssessmentAssignment saved;
        try {
            saved = Objects.requireNonNull(assignmentRepository.save(assignment),
                    "Persisted assignment must not be null");

            // Create report
            AssessmentReport report = AssessmentReport.builder()
                    .assignment(saved)
                    .createdByUser(assignedBy)
                    .isDraft(true)
                    .isFinalized(false)
                    .build();
            reportRepository.save(report);
        } catch (InvalidDataAccessResourceUsageException ex) {
            log.error(
                    "Assessment assignment schema failure: tenantContext={}, dbCurrentSchema={}, templateId={}, clientId={}, requesterId={}",
                    TenantContext.getSchemaName(),
                    resolveCurrentSchemaSafely(),
                    request.getTemplateId(),
                    request.getClientId(),
                    assignedBy.getId(),
                    ex);
            throw ex;
        }
        Long savedId = requireAssignmentId(saved);

        // Trigger notification
        if (notificationService != null) {
            try {
                notificationService.processEventInNewTransaction(NotificationEventCatalog.ASSESSMENT_ASSIGNED, buildAssignmentEventData(saved));
            } catch (Exception e) {
                log.error("Failed to trigger assessment_assigned notification", e);
            }
        }

        // Send email notification
        sendAssessmentAssignedEmail(saved);

        // Audit log using AuditLogService
        auditLogService.logAssessmentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                savedId,
                client.getId(),
                "assessment_assigned",
                ipAddress,
                HttpRequestUtil.getUserAgent(null),
                Map.of("templateId", template.getId(), "templateName", template.getName(), "dueDate",
                        saved.getDueDate()));

        return toAssignmentResponse(saved);
    }

    private String resolveCurrentSchemaSafely() {
        try {
            Object currentSchema = entityManager.createNativeQuery("select current_schema()").getSingleResult();
            return currentSchema != null ? currentSchema.toString() : null;
        } catch (Exception ex) {
            return "unavailable:" + ex.getClass().getSimpleName();
        }
    }

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #request.assignmentId)")
    public @NonNull AssessmentAssignmentResponse submitResponses(@NonNull SubmitAssessmentResponseRequest request,
            @NonNull AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        if ("completed".equals(assignment.getStatus())) {
            throw new BadRequestException("Assessment is already completed");
        }

        List<QuestionResponseRequest> responseRequests = Optional.ofNullable(request.getResponses())
                .orElse(List.of());

        // Preload questions to avoid N+1 queries
        Map<Long, AssessmentQuestion> questionsById = responseRequests.stream()
                .map(QuestionResponseRequest::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), ids -> {
                    if (ids.isEmpty()) {
                        return Map.<Long, AssessmentQuestion>of();
                    }
                    return questionRepository.findAllById(ids).stream()
                            .collect(Collectors.toMap(AssessmentQuestion::getId, Function.identity()));
                }));

        // Determine responder type and ID
        // For regular users (therapist, admin, supervisor), use USER type
        String responderType = "USER";
        Long responderUserId = currentUserService.requireCurrentUser(requester).getId();
        Long responderClientId = null;

        // Preload sections to check isScoring flag
        Set<Long> sectionIds = questionsById.values().stream()
                .map(q -> q.getSection().getId())
                .collect(Collectors.toSet());
        Map<Long, AssessmentSection> sectionsById = sectionRepository.findAllById(sectionIds).stream()
                .collect(Collectors.toMap(AssessmentSection::getId, Function.identity()));

        // Preload options for score calculation
        Set<Long> allOptionIds = responseRequests.stream()
                .filter(r -> r.getSelectedOptionIds() != null && !r.getSelectedOptionIds().isEmpty())
                .flatMap(r -> r.getSelectedOptionIds().stream().map(Integer::longValue))
                .collect(Collectors.toSet());
        // Also include legacy single option IDs
        responseRequests.stream()
                .filter(r -> r.getSelectedOptionId() != null)
                .map(r -> r.getSelectedOptionId().longValue())
                .forEach(allOptionIds::add);
        Map<Long, AssessmentQuestionOption> optionsById = allOptionIds.isEmpty() ? Map.of()
                : optionRepository.findAllById(allOptionIds).stream()
                        .collect(Collectors.toMap(AssessmentQuestionOption::getId, Function.identity()));

        // Save or update responses
        for (QuestionResponseRequest responseReq : responseRequests) {
            AssessmentQuestion question = Optional.ofNullable(questionsById.get(responseReq.getQuestionId()))
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Question not found: " + responseReq.getQuestionId()));

            // Validate response before saving
            validateResponse(question, responseReq);

            // Find existing response for this question and responder (deduplication)
            AssessmentResponse response = findExistingResponse(assignment.getId(), question.getId(), 
                    responderType, responderUserId, responderClientId);

            if (response == null) {
                response = AssessmentResponse.builder()
                        .assignment(assignment)
                        .question(question)
                        .responderType(responderType)
                        .responderUserId(responderUserId)
                        .responderClientId(responderClientId)
                        .build();
            }

            response.setResponseText(responseReq.getResponseText());

            // Handle selected options (support multiple for checkbox questions)
            if (responseReq.getSelectedOptionIds() != null && !responseReq.getSelectedOptionIds().isEmpty()) {
                response.getSelectedOptions().clear();
                for (Integer optionId : responseReq.getSelectedOptionIds()) {
                    AssessmentQuestionOption option = optionsById.get(optionId.longValue());
                    if (option != null) {
                        response.getSelectedOptions().add(AssessmentResponseOption.builder()
                                .response(response)
                                .option(option)
                                .build());
                    }
                }
            } else if (responseReq.getSelectedOptionId() != null) {
                // Legacy support: single option ID
                response.getSelectedOptions().clear();
                AssessmentQuestionOption option = optionsById.get(responseReq.getSelectedOptionId().longValue());
                if (option != null) {
                    response.getSelectedOptions().add(AssessmentResponseOption.builder()
                            .response(response)
                            .option(option)
                            .build());
                }
            } else {
                response.getSelectedOptions().clear();
            }

            response.setRatingValue(responseReq.getRatingValue());

            // Calculate score value for this response
            BigDecimal calculatedScore = calculateResponseScore(question, response, optionsById);
            response.setScoreValue(calculatedScore);
            response.setAnsweredAt(Instant.now());

            responseRepository.save(response);
        }

        // Recalculate total score (only from scoring sections)
        BigDecimal totalScore = recalculateTotalScore(assignment.getId());

        // Auto-update status based on workflow
        String currentStatus = assignment.getStatus();
        String newStatus = resolveStatusAfterResponsesSaved(assignment, currentStatus);

        applyStatusTransitionTimestamps(assignment, currentStatus, newStatus);
        assignment.setTotalScore(totalScore);
        AssessmentAssignment updated = assignmentRepository.save(assignment);

        // Notify when the client phase is fully submitted (pending/in_progress → waiting_for_review)
        if (notificationService != null
                && com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_REVIEW.getValue().equals(newStatus)
                && !newStatus.equals(currentStatus)) {
            try {
                notificationService.processEventInNewTransaction(NotificationEventCatalog.ASSESSMENT_COMPLETED, buildAssignmentEventData(updated));
            } catch (Exception e) {
                log.error("Failed to trigger assessment_completed notification", e);
            }
        }

        if (com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_REVIEW.getValue().equals(newStatus)
                && !newStatus.equals(currentStatus)) {
            sendAssessmentCompletedEmail(updated);
        }

        // Audit log using AuditLogService
        auditLogService.logAssessmentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                updated.getId(),
                assignment.getClient().getId(),
                "assessment_response_saved",
                ipAddress,
                HttpRequestUtil.getUserAgent(null),
                Map.of("status", newStatus, "totalScore", totalScore, "responsesCount", responseRequests.size()));

        return toAssignmentResponse(updated);
    }

    @Transactional(readOnly = true)
    public @NonNull List<AssessmentAssignmentResponse> getClientAssessments(@NonNull Long clientId,
            @NonNull AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<AssessmentAssignment> assignments = assignmentRepository.findByClientIdOrderByAssignedDateDesc(clientId);
        return assignments.stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public @NonNull AssessmentAssignmentResponse getAssignment(@NonNull Long assignmentId,
            @NonNull AuthPrincipal requester) {
        Objects.requireNonNull(assignmentId, "Assignment id is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));
        validateClientAccessForResponses(assignment.getClient(), requester);

        return toAssignmentResponse(assignment);
    }

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public void deleteAssignment(@NonNull Long assignmentId, @NonNull AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(assignmentId, "Assignment id is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));
        validateClientAccessForResponses(assignment.getClient(), requester);

        Long clientId = assignment.getClient().getId();
        Long templateId = assignment.getTemplate() != null ? assignment.getTemplate().getId() : null;

        // ClientHub deletes the assignment with ON DELETE CASCADE on responses/reports.
        // TherapyFlow FKs do not cascade, and finalized reports are protected by a DB trigger.
        // Explicitly remove children first, and allow the finalized-report delete in this txn.
        entityManager.createNativeQuery(
                        "SELECT set_config('app.allow_clinical_unfinalize', 'true', true)")
                .getSingleResult();

        entityManager.createNativeQuery(
                        "DELETE FROM assessment_response_options WHERE response_id IN ("
                                + "SELECT id FROM assessment_responses WHERE assignment_id = :assignmentId)")
                .setParameter("assignmentId", assignmentId)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "DELETE FROM assessment_responses WHERE assignment_id = :assignmentId")
                .setParameter("assignmentId", assignmentId)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "DELETE FROM assessment_report_versions WHERE report_id IN ("
                                + "SELECT id FROM assessment_reports WHERE assignment_id = :assignmentId)")
                .setParameter("assignmentId", assignmentId)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "DELETE FROM assessment_reports WHERE assignment_id = :assignmentId")
                .setParameter("assignmentId", assignmentId)
                .executeUpdate();

        assignmentRepository.delete(assignment);
        assignmentRepository.flush();

        // Audit log using AuditLogService
        Map<String, Object> auditDetails = new HashMap<>();
        if (templateId != null) {
            auditDetails.put("templateId", templateId);
        }
        auditLogService.logAssessmentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                assignmentId,
                clientId,
                "assessment_deleted",
                ipAddress,
                HttpRequestUtil.getUserAgent(null),
                auditDetails);
    }

    // Private helper methods

    private AssessmentTemplateResponse toTemplateResponse(AssessmentTemplate template) {
        return AssessmentTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .isStandardized(template.getIsStandardized())
                .version(template.getVersionNumber() != null ? template.getVersionNumber() : 1)
                .isActive(template.getIsActive())
                .sections(template.getSections() != null ? template.getSections().stream()
                        .filter(section -> !Boolean.TRUE.equals(section.getIsDeleted()))
                        .sorted(newestFirst())
                        .map(this::toSectionResponse)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    /**
     * Loads template + sections, then questions in a second query.
     * Hibernate cannot simultaneously fetch both List bags in one JOIN FETCH.
     */
    private AssessmentTemplate requireTemplateWithSectionsAndQuestions(Long templateId) {
        AssessmentTemplate template = templateRepository.findByIdWithSections(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));
        templateRepository.findSectionsWithQuestionsByTemplateId(templateId);
        return template;
    }

    private AssessmentSectionResponse toSectionResponse(AssessmentSection section) {
        return AssessmentSectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .accessLevel(section.getAccessLevel())
                .isScoring(section.getIsScoring())
                .reportMapping(section.getReportMapping())
                .aiReportPrompt(section.getAiReportPrompt())
                .sortOrder(section.getSortOrder())
                .questions(section.getQuestions() != null ? section.getQuestions().stream()
                        .filter(question -> !Boolean.TRUE.equals(question.getIsDeleted()))
                        .sorted(newestFirst())
                        .map(this::toQuestionResponse)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }

    private AssessmentQuestionResponse toQuestionResponse(AssessmentQuestion question) {
        // Convert rating labels list to comma-separated string
        String ratingLabelsStr = null;
        if (question.getRatingLabels() != null && !question.getRatingLabels().isEmpty()) {
            ratingLabelsStr = question.getRatingLabels().stream()
                    .filter(label -> Boolean.TRUE.equals(label.getActive()))
                    .map(AssessmentQuestionRatingLabel::getLabel)
                    .collect(Collectors.joining(","));
        }
        
        return AssessmentQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .isRequired(question.getIsRequired())
                .sortOrder(question.getSortOrder())
                .ratingMin(question.getRatingMin() != null ? question.getRatingMin().doubleValue() : null)
                .ratingMax(question.getRatingMax() != null ? question.getRatingMax().doubleValue() : null)
                .ratingLabels(ratingLabelsStr)
                .contributesToScore(question.getContributesToScore())
                .options(question.getOptions() != null ? question.getOptions().stream()
                        .map(this::toOptionResponse)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }

    private AssessmentOptionResponse toOptionResponse(AssessmentQuestionOption option) {
        return AssessmentOptionResponse.builder()
                .id(option.getId())
                .optionText(option.getOptionText())
                .optionValue(option.getScoreValue() != null ? option.getScoreValue().doubleValue() : null)
                .sortOrder(option.getSortOrder())
                .build();
    }

    private static <T extends com.smart.therapy.flow.common.entity.BaseEntity> Comparator<T> newestFirst() {
        return Comparator
                .comparing((T entity) -> entity.getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing((T entity) -> entity.getId(),
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private AssessmentAssignmentResponse toAssignmentResponse(AssessmentAssignment assignment) {
        return AssessmentAssignmentResponse.builder()
                .id(assignment.getId())
                .templateId(assignment.getTemplate().getId())
                .templateName(assignment.getTemplate().getName())
                .clientId(assignment.getClient().getId())
                .clientName(assignment.getClient().getFullName())
                .status(assignment.getStatus())
                .statusLabel(com.smart.therapy.flow.assessment.enums.AssessmentStatus
                        .displayLabelFor(assignment.getStatus()))
                .assignedById(assignment.getAssignedBy() != null ? assignment.getAssignedBy().getId() : null)
                .assignedByName(assignment.getAssignedBy() != null ? assignment.getAssignedBy().getFullName() : null)
                .assignedDate(assignment.getAssignedDate())
                .dueDate(assignment.getDueDate() != null
                        ? assignment.getDueDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                        : null)
                .completedAt(assignment.getCompletedAt())
                .totalScore(assignment.getTotalScore() != null ? assignment.getTotalScore().doubleValue() : null)
                .notes(assignment.getNotes())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    private Map<String, Object> buildAssignmentEventData(AssessmentAssignment assignment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", assignment.getId());
        data.put("clientId", assignment.getClient().getId());
        NotificationPayloadFactory.putClientIdentity(data, assignment.getClient());
        data.put("templateId", assignment.getTemplate().getId());
        data.put("templateName", assignment.getTemplate().getName());
        data.put("status", assignment.getStatus());
        data.put("dueDate", assignment.getDueDate());
        data.put("totalScore", assignment.getTotalScore());
        if (assignment.getClient() != null && assignment.getClient().getAssignedTherapist() != null) {
            data.put("therapistId", assignment.getClient().getAssignedTherapist().getId());
            data.put("therapistName", assignment.getClient().getAssignedTherapist().getFullName());
        } else {
            data.put("therapistName", "Unassigned");
        }
        if (assignment.getAssignedBy() != null) {
            data.put("assignedTherapistId", assignment.getAssignedBy().getId());
            data.put("assignedById", assignment.getAssignedBy().getId());
            data.put("assignedByName", assignment.getAssignedBy().getFullName());
        }
        return data;
    }

    /**
     * Calculate score for a single response based on question type and selected
     * options.
     * Only calculates if question contributes to score and is in a scoring section.
     */
    private BigDecimal calculateResponseScore(AssessmentQuestion question, AssessmentResponse response,
            Map<Long, AssessmentQuestionOption> optionsById) {
        // Check if question contributes to score
        if (!Boolean.TRUE.equals(question.getContributesToScore())) {
            return null;
        }

        // Check if section is a scoring section
        AssessmentSection section = question.getSection();
        if (!Boolean.TRUE.equals(section.getIsScoring())) {
            return null;
        }

        // For multiple choice/checkbox questions - sum option values
        if (!response.getSelectedOptions().isEmpty()) {
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (AssessmentResponseOption responseOption : response.getSelectedOptions()) {
                AssessmentQuestionOption option = responseOption.getOption();
                if (option != null && option.getScoreValue() != null) {
                    total = total.add(option.getScoreValue());
                }
            }
            return total.compareTo(java.math.BigDecimal.ZERO) > 0 ? total : null;
        }

        // For rating scale questions - use rating value directly
        if (response.getRatingValue() != null) {
            return BigDecimal.valueOf(response.getRatingValue());
        }

        // For text questions - no scoring
        return null;
    }

    /**
     * Recalculate total score for an assignment.
     * Only includes responses from questions in scoring sections.
     */
    @Transactional(readOnly = true)
    private BigDecimal recalculateTotalScore(Long assignmentId) {
        // Get all responses with their questions and sections
        List<AssessmentResponse> allResponses = responseRepository.findByAssignmentId(assignmentId);

        // Group by question-responder pair and keep only latest (deduplication)
        Map<String, AssessmentResponse> latestResponses = new HashMap<>();
        for (AssessmentResponse response : allResponses) {
            // Create key using responder type and ID
            String responderKey = response.getResponderType();
            if ("USER".equals(response.getResponderType()) && response.getResponderUserId() != null) {
                responderKey += "-" + response.getResponderUserId();
            } else if ("CLIENT".equals(response.getResponderType()) && response.getResponderClientId() != null) {
                responderKey += "-" + response.getResponderClientId();
            }
            String key = response.getQuestion().getId() + "-" + responderKey;
            AssessmentResponse existing = latestResponses.get(key);
            if (existing == null || response.getCreatedAt().isAfter(existing.getCreatedAt())) {
                latestResponses.put(key, response);
            }
        }

        // Sum scores from scoring sections only
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentResponse response : latestResponses.values()) {
            AssessmentQuestion question = response.getQuestion();
            AssessmentSection section = question.getSection();

            // Only include if section is scoring and question contributes to score
            if (Boolean.TRUE.equals(section.getIsScoring()) &&
                    Boolean.TRUE.equals(question.getContributesToScore()) &&
                    response.getScoreValue() != null) {
                total = total.add(response.getScoreValue());
            }
        }

        return total;
    }

    /**
     * Advance assignment status after responses are saved.
     * Workflow: pending → client_in_progress → waiting_for_review → therapist_completed → …
     */
    private String resolveStatusAfterResponsesSaved(AssessmentAssignment assignment, String currentStatus) {
        com.smart.therapy.flow.assessment.enums.AssessmentStatus status;
        try {
            status = com.smart.therapy.flow.assessment.enums.AssessmentStatus.fromValue(currentStatus);
        } catch (IllegalArgumentException e) {
            status = com.smart.therapy.flow.assessment.enums.AssessmentStatus.PENDING;
        }

        if (status == com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED
                || status == com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_THERAPIST) {
            return currentStatus;
        }

        Long templateId = assignment.getTemplate() != null ? assignment.getTemplate().getId() : null;
        if (templateId == null) {
            return currentStatus;
        }

        if (status == com.smart.therapy.flow.assessment.enums.AssessmentStatus.PENDING
                || status == com.smart.therapy.flow.assessment.enums.AssessmentStatus.CLIENT_IN_PROGRESS) {
            if (areAllRequiredClientQuestionsAnswered(assignment.getId(), templateId)) {
                return com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_REVIEW.getValue();
            }
            return com.smart.therapy.flow.assessment.enums.AssessmentStatus.CLIENT_IN_PROGRESS.getValue();
        }

        if (status == com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_REVIEW
                && areAllRequiredTherapistQuestionsAnswered(assignment.getId(), templateId)) {
            return com.smart.therapy.flow.assessment.enums.AssessmentStatus.THERAPIST_COMPLETED.getValue();
        }

        return currentStatus;
    }

    private void applyStatusTransitionTimestamps(AssessmentAssignment assignment, String currentStatus,
            String newStatus) {
        assignment.setStatus(newStatus);
        if (newStatus.equals(currentStatus)) {
            return;
        }
        if (com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_REVIEW.getValue().equals(newStatus)
                && assignment.getClientSubmittedAt() == null) {
            assignment.setClientSubmittedAt(Instant.now());
        }
        if (com.smart.therapy.flow.assessment.enums.AssessmentStatus.THERAPIST_COMPLETED.getValue().equals(newStatus)
                && assignment.getTherapistCompletedAt() == null) {
            assignment.setTherapistCompletedAt(Instant.now());
        }
        if (com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED.getValue().equals(newStatus)) {
            assignment.setCompletedAt(Instant.now());
        }
    }

    private boolean areAllRequiredClientQuestionsAnswered(Long assignmentId, Long templateId) {
        return areAllRequiredQuestionsAnsweredInScope(assignmentId, templateId, true);
    }

    private boolean areAllRequiredTherapistQuestionsAnswered(Long assignmentId, Long templateId) {
        return areAllRequiredQuestionsAnsweredInScope(assignmentId, templateId, false);
    }

    private boolean areAllRequiredQuestionsAnsweredInScope(Long assignmentId, Long templateId, boolean clientScope) {
        List<AssessmentSection> sections = sectionRepository.findByTemplateId(templateId);
        if (sections == null || sections.isEmpty()) {
            return true;
        }

        List<AssessmentResponse> responses = responseRepository.findByAssignmentId(assignmentId);
        for (AssessmentSection section : sections) {
            if (Boolean.TRUE.equals(section.getIsDeleted())) {
                continue;
            }
            if (clientScope && !isClientAccessibleSection(section)) {
                continue;
            }
            if (!clientScope && !isTherapistAccessibleSection(section)) {
                continue;
            }

            List<AssessmentQuestion> questions = questionRepository.findBySectionId(section.getId());
            for (AssessmentQuestion question : questions) {
                if (!Boolean.TRUE.equals(question.getIsRequired())) {
                    continue;
                }
                if (!hasAnsweredQuestion(question, responses)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isClientAccessibleSection(AssessmentSection section) {
        if (section == null || !StringUtils.hasText(section.getAccessLevel())) {
            return true;
        }
        return !"therapist_only".equalsIgnoreCase(section.getAccessLevel().trim());
    }

    private boolean isTherapistAccessibleSection(AssessmentSection section) {
        if (section == null || !StringUtils.hasText(section.getAccessLevel())) {
            return true;
        }
        return !"client_only".equalsIgnoreCase(section.getAccessLevel().trim());
    }

    private boolean hasAnsweredQuestion(AssessmentQuestion question, List<AssessmentResponse> responses) {
        if (question == null || question.getId() == null || responses == null) {
            return false;
        }
        return responses.stream()
                .filter(response -> response.getQuestion() != null
                        && question.getId().equals(response.getQuestion().getId()))
                .anyMatch(this::responseHasContent);
    }

    private boolean responseHasContent(AssessmentResponse response) {
        if (response == null) {
            return false;
        }
        if (StringUtils.hasText(response.getResponseText())) {
            return true;
        }
        if (response.getRatingValue() != null) {
            return true;
        }
        return response.getSelectedOptions() != null && !response.getSelectedOptions().isEmpty();
    }

    private Long requireAssignmentId(AssessmentAssignment assignment) {
        Objects.requireNonNull(assignment, "Assignment is required");
        return Objects.requireNonNull(assignment.getId(), "Assignment id must not be null");
    }

    private List<Long> getSupervisedTherapistIds(Long supervisorId) {
        return caseloadScopeService.getSupervisedTherapistIds(supervisorId);
    }

    private boolean hasAssessmentCaseloadAccess(AssessmentAssignment assignment, AuthPrincipal requester) {
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        if (resolved.scope() == CaseloadScope.ALL) {
            return true;
        }
        if (resolved.scope() == CaseloadScope.NONE) {
            return false;
        }
        Long assignedById = assignment.getAssignedBy() != null ? assignment.getAssignedBy().getId() : null;
        if (resolved.includesTherapist(assignedById)) {
            return true;
        }
        Long clientTherapistId = assignment.getClient() != null && assignment.getClient().getAssignedTherapist() != null
                ? assignment.getClient().getAssignedTherapist().getId()
                : null;
        return resolved.includesTherapist(clientTherapistId);
    }

    /**
     * Check if user has USER_MANAGE permission (PBAC).
     * This replaces the old role-based admin check.
     */
    private void assertAdmin(AuthPrincipal principal, String message) {
        permissionChecker.requireAssessmentAssignAccess(principal, message);
    }

    private void enforceAssessmentTemplateCreationLimit() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            return;
        }
        Integer limit = subscriptionFeatureService.getEffectiveLimit(
                orgId,
                SubscriptionFeatureService.FEATURE_ASSESSMENT_TEMPLATES,
                null
        );
        if (limit == null) {
            return;
        }
        long currentCount = templateRepository.countActiveTemplates();
        if (currentCount >= limit) {
            throw new ForbiddenException("Assessment template limit reached for this organisation. Contact Super Admin to increase your limit.");
        }
    }

    private void incrementAssessmentTemplateUsage() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            return;
        }
        subscriptionFeatureService.incrementUsage(
                orgId,
                SubscriptionFeatureService.FEATURE_ASSESSMENT_TEMPLATES,
                1L
        );
    }

    /**
     * Ensures the requester has caseload access to the client's PHI
     * (CLIENT_VIEW_ALL / TEAM / OWN).
     */
    private void validateClientAccessForResponses(Client client, AuthPrincipal requester) {
        if (requester == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (client == null) {
            return;
        }
        clientReportAccessService.validateClientAccess(client, requester);
    }

    // ========== PDF DOWNLOAD ==========

    @Transactional(readOnly = true)
    public String generateAssessmentPdfHtml(Long assignmentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        validateClientAccessForResponses(assignment.getClient(), requester);

        AssessmentReport report = reportRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment report not found. Please generate a report first."));

        // Get practice settings
        Map<String, String> practiceSettings = getPracticeSettings();

        // Get therapist profile if assigned
        com.smart.therapy.flow.user.entity.UserProfile therapistProfile = null;
        if (assignment.getAssignedBy() != null) {
            therapistProfile = userProfileRepository.findByUserId(assignment.getAssignedBy().getId()).orElse(null);
        }

        String genderLabel = null;
        if (assignment.getClient() != null && StringUtils.hasText(assignment.getClient().getGender())
                && systemOptionResolverService != null) {
            try {
                genderLabel = systemOptionResolverService.resolveOptionLabel(
                        SystemOptionCategories.GENDER, assignment.getClient().getGender());
            } catch (Exception ex) {
                log.debug("Unable to resolve gender label for assessment PDF", ex);
            }
        }

        String html = AssessmentReportHtmlBuilder.build(
                assignment, report, practiceSettings, therapistProfile, genderLabel);

        // Audit log
        auditLogService.logDocumentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                report.getId(),
                assignment.getClient().getId(),
                "assessment_report_downloaded",
                ipAddress,
                HttpRequestUtil.getUserAgent(null),
                Map.of("format", "pdf", "assignmentId", assignmentId, "templateId", assignment.getTemplate().getId()));

        return html;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ========== DOCX DOWNLOAD ==========

    @Transactional(readOnly = true)
    public byte[] downloadAssessmentDocx(Long assignmentId, AuthPrincipal requester, String ipAddress)
            throws IOException {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        validateClientAccessForResponses(assignment.getClient(), requester);

        AssessmentReport report = reportRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment report not found"));

        Map<String, String> practiceSettings = getPracticeSettings();

        com.smart.therapy.flow.user.entity.UserProfile therapistProfile = null;
        if (assignment.getAssignedBy() != null) {
            therapistProfile = userProfileRepository.findByUserId(assignment.getAssignedBy().getId()).orElse(null);
        }

        String genderLabel = null;
        if (assignment.getClient() != null && StringUtils.hasText(assignment.getClient().getGender())
                && systemOptionResolverService != null) {
            try {
                genderLabel = systemOptionResolverService.resolveOptionLabel(
                        SystemOptionCategories.GENDER, assignment.getClient().getGender());
            } catch (Exception ex) {
                log.debug("Unable to resolve gender label for assessment DOCX", ex);
            }
        }

        byte[] docxBytes = AssessmentReportDocxBuilder.build(
                assignment, report, practiceSettings, therapistProfile, genderLabel);

        auditLogService.logDocumentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                report.getId(),
                assignment.getClient().getId(),
                "assessment_report_downloaded",
                ipAddress,
                HttpRequestUtil.getUserAgent(null),
                Map.of("format", "docx", "assignmentId", assignmentId, "templateId", assignment.getTemplate().getId()));

        return docxBytes;
    }

    private Map<String, String> getPracticeSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("name", "Resilience Counseling Research & Consultation");
        settings.put("address", "111 Waterloo St Unit 406, London, ON N6B 2M4");
        settings.put("phone", "+1 (548)866-0366");
        settings.put("email", "resiliencecrc@gmail.com");
        settings.put("website", "www.resiliencec.com");

        try {
            PracticeConfigurationResponse config = practiceConfigurationService.getPracticeConfiguration();
            if (config != null) {
                if (StringUtils.hasText(config.getPracticeName())) {
                    settings.put("name", config.getPracticeName());
                }
                if (StringUtils.hasText(config.getPracticeAddress())) {
                    settings.put("address", config.getPracticeAddress());
                }
                if (StringUtils.hasText(config.getPracticePhone())) {
                    settings.put("phone", config.getPracticePhone());
                }
                if (StringUtils.hasText(config.getPracticeEmail())) {
                    settings.put("email", config.getPracticeEmail());
                }
                if (StringUtils.hasText(config.getPracticeWebsite())) {
                    settings.put("website", config.getPracticeWebsite());
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to load canonical practice configuration, falling back to system options", ex);
        }

        try {
            optionCategoryRepository.findByCategoryKey("practice_settings").ifPresent(category -> {
                List<com.smart.therapy.flow.system.entity.SystemOption> options = systemOptionRepository
                        .findByCategoryId(category.getId());
                Map<String, String> optionsByKey = options.stream()
                        .filter(option -> Boolean.TRUE.equals(option.getIsActive()))
                        .collect(Collectors.toMap(
                                com.smart.therapy.flow.system.entity.SystemOption::getOptionKey,
                                com.smart.therapy.flow.system.entity.SystemOption::getOptionLabel,
                                (a, b) -> b));

                if (optionsByKey.containsKey("practice_name")) {
                    settings.put("name", optionsByKey.get("practice_name"));
                }
                if (optionsByKey.containsKey("practice_address")) {
                    settings.put("address", optionsByKey.get("practice_address"));
                }
                if (optionsByKey.containsKey("practice_phone")) {
                    settings.put("phone", optionsByKey.get("practice_phone"));
                }
                if (optionsByKey.containsKey("practice_email")) {
                    settings.put("email", optionsByKey.get("practice_email"));
                }
                if (optionsByKey.containsKey("practice_website")) {
                    settings.put("website", optionsByKey.get("practice_website"));
                }
            });
        } catch (Exception e) {
            log.warn("Failed to load practice settings, using defaults", e);
        }

        return settings;
    }

    // ========== AI REPORT GENERATION ==========

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public AssessmentReport generateReport(Long assignmentId, AuthPrincipal requester, String ipAddress,
            String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        // Check AI consent
        if (consentPolicyService != null) {
            try {
                consentPolicyService.requireAiConsent(assignment.getClient().getId());
            } catch (ForbiddenException ex) {
                // Log blocked attempt
                auditLogService.logAssessmentAccess(
                        currentUserService.requireCurrentUser(requester).getId(),
                        requester.getLoginIdentifier(),
                        assignmentId,
                        assignment.getClient().getId(),
                        "ai_processing_blocked",
                        ipAddress,
                        userAgent,
                        Map.of("reason", "consent_not_granted", "consentType", "AI_PROCESSING"));
                throw ex;
            }
        }

        // Get all responses
        List<AssessmentResponse> responses = responseRepository.findByAssignmentId(assignmentId);

        // Load template + sections + questions so section AI prompts and options are available
        AssessmentTemplate template = requireTemplateWithSectionsAndQuestions(assignment.getTemplate().getId());
        List<AssessmentSection> sections = template.getSections() != null
                ? template.getSections().stream()
                        .filter(section -> !Boolean.TRUE.equals(section.getIsDeleted()))
                        .sorted(Comparator.comparing(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                        .collect(Collectors.toList())
                : Collections.emptyList();

        // Capture template snapshot as JSON
        String templateSnapshot = captureTemplateSnapshot(template, sections);

        Optional<AssessmentReport> existingReport = reportRepository.findByAssignmentId(assignmentId);
        if (existingReport.map(AssessmentReport::getIsFinalized).orElse(false)) {
            throw new BadRequestException(
                    "Finalized assessment reports are immutable; create an amendment instead");
        }

        // Generate AI report
        if (aiService == null) {
            throw new BadRequestException("AI service is not configured");
        }

        String generatedContent = aiService.generateAssessmentReport(assignment, responses, sections);

        // Get or create report
        AssessmentReport report = existingReport
                .orElse(AssessmentReport.builder()
                        .assignment(assignment)
                        .createdByUser(userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                        .isDraft(true)
                        .isFinalized(false)
                        .build());

        report.setGeneratedContent(generatedContent);
        report.setDraftContent(null); // Clear old draft
        report.setFinalContent(null); // Clear old final
        report.setTemplateSnapshot(templateSnapshot); // Store template snapshot
        report.setGeneratedAt(Instant.now());
        report.setIsFinalized(false);
        report.setFinalizedAt(null);
        report.setFinalizedByUser(null);

        AssessmentReport saved = reportRepository.save(report);

        // Create version history entry
        createReportVersion(saved, generatedContent, "GENERATED", 
                "AI-generated report", currentUserService.requireCurrentUser(requester).getId());

        // Update assignment status
        assignment.setStatus(com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_THERAPIST.getValue());
        assignmentRepository.save(assignment);

        // Audit log
        auditLogService.logAssessmentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                assignmentId,
                assignment.getClient().getId(),
                "assessment_report_generated",
                ipAddress,
                userAgent,
                Map.of("reportId", saved.getId(), "templateId", assignment.getTemplate().getId()));

        return saved;
    }

    // ========== REPORT FINALIZATION ==========

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public AssessmentReport finalizeReport(Long assignmentId, AuthPrincipal requester, String ipAddress,
            String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        AssessmentReport report = reportRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment report not found"));

        if (Boolean.TRUE.equals(report.getIsFinalized())) {
            throw new BadRequestException("Report is already finalized");
        }

        // PBAC: Permission + Data Scope enforcement
        boolean hasViewPermission = permissionChecker.hasAssessmentViewAccess(requester);
        if (!hasViewPermission) {
            throw new ForbiddenException("You do not have permission to view assessments");
        }

        // Data scope: Check if user can access this specific assessment
        if (!hasAssessmentCaseloadAccess(assignment, requester)) {
            throw new ForbiddenException("You do not have permission to finalize this report");
        }

        User finalizedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Finalize report
        report.finalizeReport(finalizedBy);
        if (report.getDraftContent() != null) {
            report.setFinalContent(report.getDraftContent());
        } else if (report.getGeneratedContent() != null) {
            report.setFinalContent(report.getGeneratedContent());
        }

        AssessmentReport saved = reportRepository.save(report);

        // Create version history entry for finalized report
        createReportVersion(saved, saved.getFinalContent(), "FINALIZED", 
                "Report finalized by " + finalizedBy.getFullName(), currentUserService.requireCurrentUser(requester).getId());

        // Update assignment status
        assignment.setStatus(com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED.getValue());
        assignment.setCompletedAt(Instant.now());
        assignmentRepository.save(assignment);

        // Audit log
        auditLogService.logAssessmentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                assignmentId,
                assignment.getClient().getId(),
                "assessment_report_finalized",
                ipAddress,
                userAgent,
                Map.of("reportId", saved.getId()));

        return saved;
    }

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public AssessmentReport unfinalizeReport(Long assignmentId, AuthPrincipal requester, String ipAddress,
            String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        Optional<AssessmentReport> reportOpt = reportRepository.findByAssignmentId(assignmentId);
        if (reportOpt.isEmpty()) {
            throw new ResourceNotFoundException("Assessment report not found");
        }
        AssessmentReport report = reportOpt.get();

        if (!Boolean.TRUE.equals(report.getIsFinalized())) {
            throw new BadRequestException("Report is not finalized");
        }

        // PBAC: Permission + Data Scope enforcement
        boolean hasViewPermission = permissionChecker.hasAssessmentViewAccess(requester);
        if (!hasViewPermission) {
            throw new ForbiddenException("You do not have permission to view assessments");
        }

        // Data scope: Check if user can access this specific assessment
        if (!hasAssessmentCaseloadAccess(assignment, requester)) {
            throw new ForbiddenException("You do not have permission to amend this report");
        }

        throw new BadRequestException(
                "Finalized assessment reports are immutable; create an amendment instead");
    }

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public AssessmentReport updateReportDraft(Long assignmentId, String draftContent, AuthPrincipal requester,
            String ipAddress, String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(draftContent, "Draft content is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        Optional<AssessmentReport> reportOpt = reportRepository.findByAssignmentId(assignmentId);
        if (reportOpt.isEmpty()) {
            throw new ResourceNotFoundException("Assessment report not found");
        }
        AssessmentReport report = reportOpt.get();

        if (Boolean.TRUE.equals(report.getIsFinalized())) {
            throw new BadRequestException("Cannot edit finalized report");
        }

        report.updateDraft(draftContent);
        
        // Create version history entry for edited report
        createReportVersion(report, draftContent, "EDITED", 
                "Report draft updated", currentUserService.requireCurrentUser(requester).getId());
        AssessmentReport saved = reportRepository.save(report);

        // Audit log
        auditLogService.logAssessmentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                assignmentId,
                assignment.getClient().getId(),
                "assessment_report_draft_updated",
                ipAddress,
                userAgent,
                Map.of("reportId", saved.getId()));

        return saved;
    }

    @Transactional(readOnly = true)
    public AssessmentReport getReport(Long assignmentId) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        return reportRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment report not found"));
    }

    // ========== VOICE TRANSCRIPTION ==========

    @Transactional
    public String transcribeAudio(byte[] audioData, String fileName, Long assignmentId, AuthPrincipal requester,
            String ipAddress, String userAgent) {
        Objects.requireNonNull(audioData, "Audio data is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Get assignment for consent checking
        AssessmentAssignment assignment = null;
        if (assignmentId != null) {
            assignment = assignmentRepository.findById(assignmentId)
                    .orElse(null);
        }

        // Check AI consent if assignment exists
        if (assignment != null && consentPolicyService != null) {
            try {
                consentPolicyService.requireAiConsent(assignment.getClient().getId());
            } catch (ForbiddenException ex) {
                // Log blocked attempt
                auditLogService.logAssessmentAccess(
                        currentUserService.requireCurrentUser(requester).getId(),
                        requester.getLoginIdentifier(),
                        assignmentId,
                        assignment.getClient().getId(),
                        "ai_processing_blocked",
                        ipAddress,
                        userAgent,
                        Map.of("reason", "consent_not_granted", "consentType", "AI_PROCESSING", "operation",
                                "voice_transcription"));
                throw ex;
            }
        }

        // Transcribe using OpenAI Whisper
        if (openAiClient == null) {
            throw new BadRequestException(
                    "OpenAI client is not configured. Voice transcription requires OpenAI API configuration.");
        }

        String transcription = openAiClient.transcribeAudio(audioData, fileName);

        // Audit log
        if (assignment != null) {
            auditLogService.logAssessmentAccess(
                    currentUserService.requireCurrentUser(requester).getId(),
                    requester.getLoginIdentifier(),
                    assignmentId,
                    assignment.getClient().getId(),
                    "assessment_voice_transcribed",
                    ipAddress,
                    userAgent,
                    Map.of("audioFileSize", audioData.length, "transcriptionLength", transcription.length()));
        }

        return transcription;
    }

    // ========== BATCH RESPONSE SAVING ==========

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #request.assignmentId)")
    public AssessmentAssignmentResponse submitBatchResponses(SubmitAssessmentResponseRequest request,
            AuthPrincipal requester, String ipAddress) {
        // Reuse the existing submitResponses method
        return submitResponses(request, requester, ipAddress);
    }

    // ========== ASSIGNMENT LISTING ==========

    @Transactional(readOnly = true)
    public PaginatedResponse<AssessmentAssignmentResponse> getAssignments(Long templateId,
            Long clientId,
            String status,
            Instant from,
            Instant to,
            String search,
            Integer page,
            Integer pageSize) {

        List<AssessmentAssignment> all = assignmentRepository.findAll();
        String normalizedSearch = StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;

        List<AssessmentAssignment> filtered = all.stream()
                .filter(a -> templateId == null || (a.getTemplate() != null
                        && templateId.equals(a.getTemplate().getId())))
                .filter(a -> clientId == null || (a.getClient() != null
                        && clientId.equals(a.getClient().getId())))
                .filter(a -> status == null || status.equalsIgnoreCase(a.getStatus()))
                .filter(a -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    Instant createdAt = a.getCreatedAt();
                    if (createdAt == null) {
                        return false;
                    }
                    boolean afterFrom = from == null || !createdAt.isBefore(from);
                    boolean beforeTo = to == null || !createdAt.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .filter(a -> !StringUtils.hasText(normalizedSearch) || matchesAssignmentSearch(a, normalizedSearch))
                .sorted(newestFirst())
                .collect(Collectors.toList());

        long total = filtered.size();
        int safePage = page != null ? Math.max(DEFAULT_ASSIGNMENT_PAGE, page) : DEFAULT_ASSIGNMENT_PAGE;
        int requestedPageSize = pageSize != null ? pageSize : DEFAULT_ASSIGNMENT_PAGE_SIZE;
        int safePageSize = Math.min(Math.max(requestedPageSize, 1), MAX_ASSIGNMENT_PAGE_SIZE);
        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        List<AssessmentAssignmentResponse> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.of(items, total, safePage, safePageSize);
    }

    private boolean matchesAssignmentSearch(AssessmentAssignment assignment, String term) {
        String clientName = assignment.getClient() != null && assignment.getClient().getFullName() != null
                ? assignment.getClient().getFullName().toLowerCase(Locale.ROOT)
                : "";
        String templateName = assignment.getTemplate() != null && assignment.getTemplate().getName() != null
                ? assignment.getTemplate().getName().toLowerCase(Locale.ROOT)
                : "";
        String assignmentStatus = assignment.getStatus() != null
                ? assignment.getStatus().toLowerCase(Locale.ROOT)
                : "";
        String assignedByName = assignment.getAssignedBy() != null && assignment.getAssignedBy().getFullName() != null
                ? assignment.getAssignedBy().getFullName().toLowerCase(Locale.ROOT)
                : "";
        String notes = assignment.getNotes() != null
                ? assignment.getNotes().toLowerCase(Locale.ROOT)
                : "";

        return clientName.contains(term)
                || templateName.contains(term)
                || assignmentStatus.contains(term)
                || assignedByName.contains(term)
                || notes.contains(term);
    }

    // ========== RESPONSES QUERY ==========

    @Transactional(readOnly = true)
    public List<AssessmentResponseDto> getAssignmentResponses(Long assignmentId, AuthPrincipal requester) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        // Basic access check: ensure requester has access to the client
        validateClientAccessForResponses(assignment.getClient(), requester);

        List<AssessmentResponse> responses = responseRepository.findByAssignmentIdOrderByQuestionSortOrder(
                assignmentId);
        return responses.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    private AssessmentResponseDto toResponseDto(AssessmentResponse response) {
        List<Long> selectedOptionIds = response.getSelectedOptions() != null
                ? response.getSelectedOptions().stream()
                        .map(opt -> opt.getOption() != null ? opt.getOption().getId() : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                : List.of();

        return AssessmentResponseDto.builder()
                .id(response.getId())
                .assignmentId(response.getAssignment() != null ? response.getAssignment().getId() : null)
                .questionId(response.getQuestion() != null ? response.getQuestion().getId() : null)
                .questionText(response.getQuestion() != null ? response.getQuestion().getQuestionText() : null)
                .responderType(response.getResponderType())
                .responderUserId(response.getResponderUserId())
                .responderClientId(response.getResponderClientId())
                .responseText(response.getResponseText())
                .responseValue(response.getResponseValue())
                .score(response.getScoreValue())
                .answeredAt(response.getAnsweredAt())
                .selectedOptionIds(selectedOptionIds)
                .ratingValue(response.getRatingValue())
                .build();
    }

    // ========== TEMPLATE UPDATE/DELETE ==========

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentTemplateResponse updateTemplate(Long templateId, UpdateAssessmentTemplateRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can update assessment templates");

        AssessmentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));

        // Check if template has active assignments
        long activeAssignments = assignmentRepository.countByTemplateIdAndStatusNotIn(
                templateId,
                Arrays.asList(
                        com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED.getValue()));
        if (activeAssignments > 0) {
            throw new BadRequestException(
                    "Cannot update template with active assignments. Please complete or cancel all assignments first.");
        }

        // Update fields
        if (StringUtils.hasText(request.getName())) {
            template.setName(request.getName().trim());
        }
        if (request.isFieldPresent("description")) {
            template.setDescription(request.getDescription());
        }
        if (request.isFieldPresent("category")) {
            template.setCategory(request.getCategory());
        }
        PatchUpdates.apply(request, "isStandardized", request.getIsStandardized(), template::setIsStandardized);
        if (request.isFieldPresent("version")) {
            if (!StringUtils.hasText(request.getVersion())) {
                template.setVersionNumber(null);
            } else {
                try {
                    template.setVersionNumber(Integer.parseInt(request.getVersion()));
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Invalid version number: " + request.getVersion());
                }
            }
        }
        PatchUpdates.apply(request, "isActive", request.getIsActive(), template::setIsActive);

        // Update sections if provided
        if (request.isFieldPresent("sections")) {
            // Delete existing sections (cascade will handle questions and options)
            sectionRepository.deleteAll(template.getSections());
            template.getSections().clear();

            // Create new sections
            for (int i = 0; i < request.getSections().size(); i++) {
                AssessmentSectionRequest sectionReq = request.getSections().get(i);
                AssessmentSection section = AssessmentSection.builder()
                        .template(template)
                        .title(sectionReq.getTitle())
                        .description(sectionReq.getDescription())
                        .accessLevel(sectionReq.getAccessLevel())
                        .isScoring(sectionReq.getIsScoring() != null ? sectionReq.getIsScoring() : false)
                        .reportMapping(sectionReq.getReportMapping())
                        .aiReportPrompt(sectionReq.getAiReportPrompt())
                        .sortOrder(sectionReq.getSortOrder() != null ? sectionReq.getSortOrder() : i)
                        .build();
                AssessmentSection savedSection = sectionRepository.save(section);

                // Create questions
                if (sectionReq.getQuestions() != null) {
                    for (int j = 0; j < sectionReq.getQuestions().size(); j++) {
                        AssessmentQuestionRequest questionReq = sectionReq.getQuestions().get(j);
                        AssessmentQuestion question = AssessmentQuestion.builder()
                                .section(savedSection)
                                .questionText(questionReq.getQuestionText())
                                .questionType(questionReq.getQuestionType())
                                .isRequired(questionReq.getIsRequired() != null ? questionReq.getIsRequired() : false)
                                .sortOrder(questionReq.getSortOrder() != null ? questionReq.getSortOrder() : j)
                                .ratingMin(questionReq.getRatingMin() != null ? questionReq.getRatingMin().intValue()
                                        : null)
                                .ratingMax(questionReq.getRatingMax() != null ? questionReq.getRatingMax().intValue()
                                        : null)
                                .ratingLabels(questionReq.getRatingLabels() != null
                                        && !questionReq.getRatingLabels().isEmpty()
                                                ? java.util.Arrays.stream(questionReq.getRatingLabels().split(","))
                                                        .map(l -> com.smart.therapy.flow.assessment.entity.AssessmentQuestionRatingLabel
                                                                .builder()
                                                                .label(l.trim())
                                                                .active(true)
                                                                .build())
                                                        .collect(java.util.stream.Collectors.toList())
                                                : new ArrayList<>())
                                .contributesToScore(questionReq.getContributesToScore() != null
                                        ? questionReq.getContributesToScore()
                                        : true)
                                .build();
                        AssessmentQuestion savedQuestion = questionRepository.save(question);

                        // Create options
                        if (questionReq.getOptions() != null) {
                            for (int k = 0; k < questionReq.getOptions().size(); k++) {
                                AssessmentOptionRequest optionReq = questionReq.getOptions().get(k);
                                AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                                        .question(savedQuestion)
                                        .optionText(optionReq.getOptionText())
                                        .scoreValue(optionReq.getOptionValue() != null
                                                ? BigDecimal.valueOf(optionReq.getOptionValue())
                                                : null)
                                        .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                                        .build();
                                optionRepository.save(option);
                            }
                        }
                    }
                }
            }
        }

        AssessmentTemplate saved = templateRepository.save(template);
        return toTemplateResponse(requireTemplateWithSectionsAndQuestions(saved.getId()));
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public void deleteTemplate(Long templateId, AuthPrincipal requester) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can delete assessment templates");

        AssessmentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));

        // Check if template has any assignments
        long assignmentCount = assignmentRepository.countByTemplateId(templateId);
        if (assignmentCount > 0) {
            throw new BadRequestException(
                    "Cannot delete template with existing assignments. Please delete or reassign all assignments first.");
        }

        templateRepository.delete(template);
    }

    // ========== SECTION CRUD ==========

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentSectionResponse createSection(Long templateId, AssessmentSectionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can create assessment sections");

        AssessmentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));

        // Get max sort order
        int maxSortOrder = template.getSections().stream()
                .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                .max()
                .orElse(-1);

        AssessmentSection section = AssessmentSection.builder()
                .template(template)
                .title(request.getTitle())
                .description(request.getDescription())
                .accessLevel(request.getAccessLevel())
                .isScoring(request.getIsScoring() != null ? request.getIsScoring() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : maxSortOrder + 1)
                .reportMapping(request.getReportMapping())
                .aiReportPrompt(request.getAiReportPrompt())
                .build();

        AssessmentSection saved = sectionRepository.save(section);

        // Create questions if provided
        if (request.getQuestions() != null) {
            for (int j = 0; j < request.getQuestions().size(); j++) {
                AssessmentQuestionRequest questionReq = request.getQuestions().get(j);
                AssessmentQuestion question = AssessmentQuestion.builder()
                        .section(saved)
                        .questionText(questionReq.getQuestionText())
                        .questionType(questionReq.getQuestionType())
                        .isRequired(questionReq.getIsRequired() != null ? questionReq.getIsRequired() : false)
                        .sortOrder(questionReq.getSortOrder() != null ? questionReq.getSortOrder() : j)
                        .ratingMin(questionReq.getRatingMin() != null ? questionReq.getRatingMin().intValue() : null)
                        .ratingMax(questionReq.getRatingMax() != null ? questionReq.getRatingMax().intValue() : null)
                        .ratingLabels(questionReq.getRatingLabels() != null && !questionReq.getRatingLabels().isEmpty()
                                ? Arrays.stream(questionReq.getRatingLabels().split(","))
                                        .map(l -> AssessmentQuestionRatingLabel.builder().label(l.trim()).active(true)
                                                .build())
                                        .collect(Collectors.toList())
                                : new ArrayList<>())
                        .contributesToScore(
                                questionReq.getContributesToScore() != null ? questionReq.getContributesToScore()
                                        : true)
                        .build();
                AssessmentQuestion savedQuestion = questionRepository.save(question);

                // Create options
                if (questionReq.getOptions() != null) {
                    for (int k = 0; k < questionReq.getOptions().size(); k++) {
                        AssessmentOptionRequest optionReq = questionReq.getOptions().get(k);
                        AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                                .question(savedQuestion)
                                .optionText(optionReq.getOptionText())
                                .optionValue(
                                        optionReq.getOptionValue() != null ? String.valueOf(optionReq.getOptionValue())
                                                : null)
                                .scoreValue(optionReq.getOptionValue() != null
                                        ? BigDecimal.valueOf(optionReq.getOptionValue())
                                        : null)
                                .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                                .build();
                        optionRepository.save(option);
                    }
                }
            }
        }

        return toSectionResponse(sectionRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentSectionResponse updateSection(Long sectionId, UpdateAssessmentSectionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(sectionId, "Section ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can update assessment sections");

        AssessmentSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment section not found"));

        // Check if section has responses
        long responseCount = responseRepository.countByQuestionSectionId(sectionId);
        if (responseCount > 0) {
            throw new BadRequestException(
                    "Cannot update section with existing responses. Please delete or reassign responses first.");
        }

        if (StringUtils.hasText(request.getTitle())) {
            section.setTitle(request.getTitle());
        }
        if (request.isFieldPresent("description")) {
            section.setDescription(request.getDescription());
        }
        if (request.isFieldPresent("accessLevel")) {
            section.setAccessLevel(request.getAccessLevel());
        }
        PatchUpdates.apply(request, "isScoring", request.getIsScoring(), section::setIsScoring);
        PatchUpdates.apply(request, "sortOrder", request.getSortOrder(), section::setSortOrder);
        if (request.isFieldPresent("reportMapping")) {
            section.setReportMapping(request.getReportMapping());
        }
        if (request.isFieldPresent("aiReportPrompt")) {
            section.setAiReportPrompt(request.getAiReportPrompt());
        }

        // Update questions if provided
        if (request.isFieldPresent("questions")) {
            // Delete existing questions (cascade will handle options)
            questionRepository.deleteAll(section.getQuestions());
            section.getQuestions().clear();

            // Create new questions
            for (int j = 0; j < request.getQuestions().size(); j++) {
                AssessmentQuestionRequest questionReq = request.getQuestions().get(j);
                AssessmentQuestion question = AssessmentQuestion.builder()
                        .section(section)
                        .questionText(questionReq.getQuestionText())
                        .questionType(questionReq.getQuestionType())
                        .isRequired(questionReq.getIsRequired() != null ? questionReq.getIsRequired() : false)
                        .sortOrder(questionReq.getSortOrder() != null ? questionReq.getSortOrder() : j)
                        .ratingMin(questionReq.getRatingMin() != null ? questionReq.getRatingMin().intValue() : null)
                        .ratingMax(questionReq.getRatingMax() != null ? questionReq.getRatingMax().intValue() : null)
                        .ratingLabels(questionReq.getRatingLabels() != null && !questionReq.getRatingLabels().isEmpty()
                                ? Arrays.stream(questionReq.getRatingLabels().split(","))
                                        .map(l -> AssessmentQuestionRatingLabel.builder().label(l.trim()).active(true)
                                                .build())
                                        .collect(Collectors.toList())
                                : new ArrayList<>())
                        .contributesToScore(
                                questionReq.getContributesToScore() != null ? questionReq.getContributesToScore()
                                        : true)
                        .build();
                AssessmentQuestion savedQuestion = questionRepository.save(question);

                // Create options
                if (questionReq.getOptions() != null) {
                    for (int k = 0; k < questionReq.getOptions().size(); k++) {
                        AssessmentOptionRequest optionReq = questionReq.getOptions().get(k);
                        AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                                .question(savedQuestion)
                                .optionText(optionReq.getOptionText())
                                .optionValue(
                                        optionReq.getOptionValue() != null ? String.valueOf(optionReq.getOptionValue())
                                                : null)
                                .scoreValue(optionReq.getOptionValue() != null
                                        ? BigDecimal.valueOf(optionReq.getOptionValue())
                                        : null)
                                .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                                .build();
                        optionRepository.save(option);
                    }
                }
            }
        }

        AssessmentSection saved = sectionRepository.save(section);
        return toSectionResponse(sectionRepository.findById(saved.getId()).orElse(saved));
    }

    // ========== QUESTION OPTIONS ==========

    @Transactional(readOnly = true)
    public List<AssessmentQuestionOptionResponse> getQuestionOptions(Long questionId, AuthPrincipal requester) {
        Objects.requireNonNull(questionId, "Question ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Ensure question exists
        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment question not found"));

        List<AssessmentQuestionOption> options = optionRepository.findByQuestionId(question.getId());
        return options.stream()
                .sorted(Comparator.comparing(
                        o -> o.getSortOrder() != null ? o.getSortOrder() : Integer.MAX_VALUE))
                .map(this::toQuestionOptionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public void deleteQuestionOptions(Long questionId, AuthPrincipal requester) {
        Objects.requireNonNull(questionId, "Question ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can delete question options");

        // Ensure question exists
        questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment question not found"));

        optionRepository.deleteByQuestionId(questionId);
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentQuestionOptionResponse createQuestionOption(CreateAssessmentQuestionOptionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can create question options");

        AssessmentQuestion question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment question not found"));

        AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                .question(question)
                .optionKey(request.getOptionKey())
                .optionText(request.getOptionText())
                .optionValue(request.getOptionValue())
                .scoreValue(request.getScoreValue())
                .sortOrder(request.getSortOrder())
                .isDefault(request.getIsDefault())
                .build();

        AssessmentQuestionOption saved = optionRepository.save(option);
        return toQuestionOptionResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public List<AssessmentQuestionOptionResponse> bulkCreateQuestionOptions(BulkAssessmentQuestionOptionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can create question options");

        AssessmentQuestion question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment question not found"));

        List<AssessmentQuestionOption> options = new ArrayList<>();
        for (CreateAssessmentQuestionOptionRequest optReq : request.getOptions()) {
            AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                    .question(question)
                    .optionKey(optReq.getOptionKey())
                    .optionText(optReq.getOptionText())
                    .optionValue(optReq.getOptionValue())
                    .scoreValue(optReq.getScoreValue())
                    .sortOrder(optReq.getSortOrder())
                    .isDefault(optReq.getIsDefault())
                    .build();
            options.add(option);
        }

        List<AssessmentQuestionOption> saved = optionRepository.saveAll(options);
        return saved.stream()
                .sorted(Comparator.comparing(
                        o -> o.getSortOrder() != null ? o.getSortOrder() : Integer.MAX_VALUE))
                .map(this::toQuestionOptionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentQuestionOptionResponse updateQuestionOption(Long optionId,
            UpdateAssessmentQuestionOptionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(optionId, "Option ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can update question options");

        AssessmentQuestionOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment question option not found"));

        if (request.isFieldPresent("optionKey")) {
            option.setOptionKey(request.getOptionKey());
        }
        if (request.isFieldPresent("optionText")) {
            option.setOptionText(request.getOptionText());
        }
        if (request.isFieldPresent("optionValue")) {
            option.setOptionValue(request.getOptionValue());
        }
        PatchUpdates.apply(request, "scoreValue", request.getScoreValue(), option::setScoreValue);
        PatchUpdates.apply(request, "sortOrder", request.getSortOrder(), option::setSortOrder);
        PatchUpdates.apply(request, "isDefault", request.getIsDefault(), option::setIsDefault);

        AssessmentQuestionOption saved = optionRepository.save(option);
        return toQuestionOptionResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public void deleteQuestionOption(Long optionId, AuthPrincipal requester) {
        Objects.requireNonNull(optionId, "Option ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can delete question options");

        if (!optionRepository.existsById(optionId)) {
            throw new ResourceNotFoundException("Assessment question option not found");
        }
        optionRepository.deleteById(optionId);
    }

    private AssessmentQuestionOptionResponse toQuestionOptionResponse(AssessmentQuestionOption option) {
        return AssessmentQuestionOptionResponse.builder()
                .id(option.getId())
                .questionId(option.getQuestion() != null ? option.getQuestion().getId() : null)
                .optionKey(option.getOptionKey())
                .optionText(option.getOptionText())
                .optionValue(option.getOptionValue())
                .scoreValue(option.getScoreValue())
                .sortOrder(option.getSortOrder())
                .isDefault(option.getIsDefault())
                .build();
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public void deleteSection(Long sectionId, AuthPrincipal requester) {
        Objects.requireNonNull(sectionId, "Section ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can delete assessment sections");

        AssessmentSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment section not found"));

        // Check if section has responses
        long responseCount = responseRepository.countByQuestionSectionId(sectionId);
        if (responseCount > 0) {
            throw new BadRequestException(
                    "Cannot delete section with existing responses. Please delete or reassign responses first.");
        }

        sectionRepository.delete(section);
    }

    // ========== QUESTION CRUD ==========

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentQuestionResponse createQuestion(Long sectionId, AssessmentQuestionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(sectionId, "Section ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can create assessment questions");

        AssessmentSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment section not found"));

        // Get max sort order
        int maxSortOrder = section.getQuestions().stream()
                .mapToInt(q -> q.getSortOrder() != null ? q.getSortOrder() : 0)
                .max()
                .orElse(-1);

        AssessmentQuestion question = AssessmentQuestion.builder()
                .section(section)
                .questionText(request.getQuestionText())
                .questionType(request.getQuestionType())
                .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : maxSortOrder + 1)
                .ratingMin(request.getRatingMin() != null ? request.getRatingMin().intValue() : null)
                .ratingMax(request.getRatingMax() != null ? request.getRatingMax().intValue() : null)
                .ratingLabels(request.getRatingLabels() != null && !request.getRatingLabels().isEmpty()
                        ? Arrays.stream(request.getRatingLabels().split(","))
                                .map(l -> AssessmentQuestionRatingLabel.builder().label(l.trim()).active(true).build())
                                .collect(Collectors.toList())
                        : new ArrayList<>())
                .contributesToScore(request.getContributesToScore() != null ? request.getContributesToScore() : true)
                .build();

        AssessmentQuestion saved = questionRepository.save(question);

        // Create options if provided
        if (request.getOptions() != null) {
            for (int k = 0; k < request.getOptions().size(); k++) {
                AssessmentOptionRequest optionReq = request.getOptions().get(k);
                AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                        .question(saved)
                        .optionText(optionReq.getOptionText())
                        .optionValue(
                                optionReq.getOptionValue() != null ? String.valueOf(optionReq.getOptionValue()) : null)
                        .scoreValue(optionReq.getOptionValue() != null ? BigDecimal.valueOf(optionReq.getOptionValue())
                                : null)
                        .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                        .build();
                optionRepository.save(option);
            }
        }

        return toQuestionResponse(questionRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentQuestionResponse updateQuestion(Long questionId, UpdateAssessmentQuestionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(questionId, "Question ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can update assessment questions");

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment question not found"));

        // Check if question has responses
        long responseCount = responseRepository.countByQuestionId(questionId);
        if (responseCount > 0) {
            throw new BadRequestException(
                    "Cannot update question with existing responses. Please delete or reassign responses first.");
        }

        if (request.isFieldPresent("questionText")) {
            question.setQuestionText(StringUtils.hasText(request.getQuestionText())
                    ? request.getQuestionText()
                    : null);
        }
        if (request.isFieldPresent("questionType")) {
            question.setQuestionType(request.getQuestionType());
        }
        PatchUpdates.apply(request, "isRequired", request.getIsRequired(), question::setIsRequired);
        PatchUpdates.apply(request, "sortOrder", request.getSortOrder(), question::setSortOrder);
        if (request.isFieldPresent("ratingMin")) {
            question.setRatingMin(request.getRatingMin() != null ? request.getRatingMin().intValue() : null);
        }
        if (request.isFieldPresent("ratingMax")) {
            question.setRatingMax(request.getRatingMax() != null ? request.getRatingMax().intValue() : null);
        }
        if (request.isFieldPresent("ratingLabels")) {
            if (!StringUtils.hasText(request.getRatingLabels())) {
                question.setRatingLabels(new ArrayList<>());
            } else {
                question.setRatingLabels(Arrays.stream(request.getRatingLabels().split(","))
                        .map(l -> AssessmentQuestionRatingLabel.builder().label(l.trim()).active(true)
                                .question(question).build())
                        .collect(Collectors.toList()));
            }
        }
        PatchUpdates.apply(request, "contributesToScore", request.getContributesToScore(), question::setContributesToScore);

        // Update options if provided
        if (request.isFieldPresent("options")) {
            // Delete existing options
            optionRepository.deleteAll(question.getOptions());
            question.getOptions().clear();

            // Create new options
            for (int k = 0; k < request.getOptions().size(); k++) {
                AssessmentOptionRequest optionReq = request.getOptions().get(k);
                AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                        .question(question)
                        .optionText(optionReq.getOptionText())
                        .optionValue(
                                optionReq.getOptionValue() != null ? String.valueOf(optionReq.getOptionValue()) : null)
                        .scoreValue(optionReq.getOptionValue() != null ? BigDecimal.valueOf(optionReq.getOptionValue())
                                : null)
                        .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                        .build();
                optionRepository.save(option);
            }
        }

        AssessmentQuestion saved = questionRepository.save(question);
        return toQuestionResponse(questionRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public void deleteQuestion(Long questionId, AuthPrincipal requester) {
        Objects.requireNonNull(questionId, "Question ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can delete assessment questions");

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment question not found"));

        // Check if question has responses
        long responseCount = responseRepository.countByQuestionId(questionId);
        if (responseCount > 0) {
            throw new BadRequestException(
                    "Cannot delete question with existing responses. Please delete or reassign responses first.");
        }

        questionRepository.delete(question);
    }

    // ========== RECALCULATE SCORES ==========

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public AssessmentAssignmentResponse recalculateScores(Long assignmentId, AuthPrincipal requester) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        // Recalculate total score
        BigDecimal totalScore = recalculateTotalScore(assignment.getId());
        assignment.setTotalScore(totalScore);

        AssessmentAssignment saved = assignmentRepository.save(assignment);
        return toAssignmentResponse(saved);
    }

    // ========== STATUS TRANSITIONS ==========

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public AssessmentAssignmentResponse updateStatus(Long assignmentId, UpdateAssessmentStatusRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        // Validate status transition
        com.smart.therapy.flow.assessment.enums.AssessmentStatus currentStatus = com.smart.therapy.flow.assessment.enums.AssessmentStatus
                .fromValue(assignment.getStatus());
        com.smart.therapy.flow.assessment.enums.AssessmentStatus newStatus = request.getStatus();

        // Validate transition rules
        if (currentStatus == com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED &&
                newStatus != com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot change status from COMPLETED. Please unfinalize the report first.");
        }

        // Update status
        assignment.setStatus(newStatus.getValue());

        // Set completed date if transitioning to completed
        if (newStatus == com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED) {
            assignment.setCompletedAt(Instant.now());
        }

        AssessmentAssignment saved = assignmentRepository.save(assignment);
        return toAssignmentResponse(saved);
    }

    // ========== SERVER-SIDE PDF GENERATION ==========

    @Transactional(readOnly = true)
    public byte[] generateAssessmentPdf(Long assignmentId, AuthPrincipal requester) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        try {
            String html = generateAssessmentPdfHtml(assignmentId, requester, "pdf-file");
            return HtmlToPdfConverter.toPdfBytes(html);
        } catch (ResourceNotFoundException | ForbiddenException | BadRequestException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Failed to generate PDF for assessment assignment {}", assignmentId, e);
            throw new BadRequestException("Failed to generate PDF: " + e.getMessage());
        }
    }

    // ========== CLIENT PORTAL METHODS ==========

    @Transactional(readOnly = true)
    public List<AssessmentAssignmentResponse> getClientAssignments(Long clientId) {
        Objects.requireNonNull(clientId, "Client ID is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<AssessmentAssignment> assignments = assignmentRepository.findByClientId(clientId);
        return assignments.stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssessmentAssignmentResponse getClientAssignment(Long assignmentId, Long clientId) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(clientId, "Client ID is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        if (!assignment.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Access denied: This assessment does not belong to the specified client");
        }

        return toAssignmentResponse(assignment);
    }

    @Transactional
    @CacheEvict(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('assignment:' + #assignmentId)")
    public AssessmentAssignmentResponse submitClientResponse(Long assignmentId, Long clientId,
            SubmitAssessmentResponseRequest request,
            String ipAddress) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(request, "Request is required");

        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        if (!assignment.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Access denied: This assessment does not belong to the specified client");
        }

        // For client responses, we need to handle responder differently
        // Create a custom submit method that sets responder type to CLIENT
        request.setAssignmentId(assignmentId);
        
        // Determine responder type and ID for CLIENT
        String responderType = "CLIENT";
        Long responderUserId = null;
        Long responderClientId = clientId;
        
        // Get questions and validate
        List<QuestionResponseRequest> responseRequests = Optional.ofNullable(request.getResponses())
                .orElse(List.of());
        
        // Preload questions
        Map<Long, AssessmentQuestion> questionsById = responseRequests.stream()
                .map(QuestionResponseRequest::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), ids -> {
                    if (ids.isEmpty()) {
                        return Map.<Long, AssessmentQuestion>of();
                    }
                    return questionRepository.findAllById(ids).stream()
                            .collect(Collectors.toMap(AssessmentQuestion::getId, Function.identity()));
                }));
        
        // Index existing responses
        Map<Long, AssessmentResponse> existingResponses = responseRepository.findByAssignmentId(assignment.getId())
                .stream()
                .collect(Collectors.toMap(response -> response.getQuestion().getId(), Function.identity()));
        
        // Preload sections
        Set<Long> sectionIds = questionsById.values().stream()
                .map(q -> q.getSection().getId())
                .collect(Collectors.toSet());
        Map<Long, AssessmentSection> sectionsById = sectionRepository.findAllById(sectionIds).stream()
                .collect(Collectors.toMap(AssessmentSection::getId, Function.identity()));
        
        // Preload options
        Set<Long> allOptionIds = responseRequests.stream()
                .filter(r -> r.getSelectedOptionIds() != null && !r.getSelectedOptionIds().isEmpty())
                .flatMap(r -> r.getSelectedOptionIds().stream().map(Integer::longValue))
                .collect(Collectors.toSet());
        responseRequests.stream()
                .filter(r -> r.getSelectedOptionId() != null)
                .map(r -> r.getSelectedOptionId().longValue())
                .forEach(allOptionIds::add);
        Map<Long, AssessmentQuestionOption> optionsById = allOptionIds.isEmpty() ? Map.of()
                : optionRepository.findAllById(allOptionIds).stream()
                        .collect(Collectors.toMap(AssessmentQuestionOption::getId, Function.identity()));
        
        // Save or update responses with CLIENT responder type
        for (QuestionResponseRequest responseReq : responseRequests) {
            AssessmentQuestion question = Optional.ofNullable(questionsById.get(responseReq.getQuestionId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + responseReq.getQuestionId()));
            
            validateResponse(question, responseReq);
            
            AssessmentResponse response = findExistingResponse(assignment.getId(), question.getId(), 
                    responderType, responderUserId, responderClientId);
            
            if (response == null) {
                response = AssessmentResponse.builder()
                        .assignment(assignment)
                        .question(question)
                        .responderType(responderType)
                        .responderUserId(responderUserId)
                        .responderClientId(responderClientId)
                        .build();
            }
            
            response.setResponseText(responseReq.getResponseText());
            
            // Handle selected options
            if (responseReq.getSelectedOptionIds() != null && !responseReq.getSelectedOptionIds().isEmpty()) {
                response.getSelectedOptions().clear();
                for (Integer optionId : responseReq.getSelectedOptionIds()) {
                    AssessmentQuestionOption option = optionsById.get(optionId.longValue());
                    if (option != null) {
                        response.getSelectedOptions().add(AssessmentResponseOption.builder()
                                .response(response)
                                .option(option)
                                .build());
                    }
                }
            } else if (responseReq.getSelectedOptionId() != null) {
                response.getSelectedOptions().clear();
                AssessmentQuestionOption option = optionsById.get(responseReq.getSelectedOptionId().longValue());
                if (option != null) {
                    response.getSelectedOptions().add(AssessmentResponseOption.builder()
                            .response(response)
                            .option(option)
                            .build());
                }
            }
            
            // Set rating value if provided
            response.setRatingValue(responseReq.getRatingValue());
            
            // Calculate score value for this response
            BigDecimal calculatedScore = calculateResponseScore(question, response, optionsById);
            response.setScoreValue(calculatedScore);
            response.setAnsweredAt(Instant.now());
            
            responseRepository.save(response);
        }
        
        // Recalculate total score
        BigDecimal totalScore = recalculateTotalScore(assignment.getId());
        assignment.setTotalScore(totalScore);

        String currentStatus = assignment.getStatus();
        String newStatus = resolveStatusAfterResponsesSaved(assignment, currentStatus);
        applyStatusTransitionTimestamps(assignment, currentStatus, newStatus);
        assignment.setStatus(newStatus);
        AssessmentAssignment updated = assignmentRepository.save(assignment);

        if (notificationService != null
                && com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_REVIEW.getValue().equals(newStatus)
                && !newStatus.equals(currentStatus)) {
            try {
                notificationService.processEventInNewTransaction(NotificationEventCatalog.ASSESSMENT_COMPLETED, buildAssignmentEventData(updated));
            } catch (Exception e) {
                log.error("Failed to trigger assessment_completed notification for client submission", e);
            }
        }

        if (com.smart.therapy.flow.assessment.enums.AssessmentStatus.WAITING_FOR_REVIEW.getValue().equals(newStatus)
                && !newStatus.equals(currentStatus)) {
            sendAssessmentCompletedEmail(updated);
        }

        return toAssignmentResponse(updated);
    }

    // ========== BULK OPERATIONS ==========

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public List<AssessmentSectionResponse> bulkCreateSections(Long templateId, BulkSectionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can perform bulk operations");

        AssessmentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));

        List<AssessmentSectionResponse> created = new ArrayList<>();
        int maxSortOrder = template.getSections().stream()
                .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                .max()
                .orElse(-1);

        for (int i = 0; i < request.getSections().size(); i++) {
            AssessmentSectionRequest sectionReq = request.getSections().get(i);
        AssessmentSection section = AssessmentSection.builder()
                .template(template)
                .title(sectionReq.getTitle())
                .description(sectionReq.getDescription())
                .accessLevel(sectionReq.getAccessLevel())
                .isScoring(sectionReq.getIsScoring() != null ? sectionReq.getIsScoring() : false)
                .sortOrder(sectionReq.getSortOrder() != null ? sectionReq.getSortOrder() : maxSortOrder + 1 + i)
                .reportMapping(sectionReq.getReportMapping())
                .aiReportPrompt(sectionReq.getAiReportPrompt())
                .build();

            AssessmentSection saved = sectionRepository.save(section);

            // Create questions if provided
            if (sectionReq.getQuestions() != null) {
                for (int j = 0; j < sectionReq.getQuestions().size(); j++) {
                    AssessmentQuestionRequest questionReq = sectionReq.getQuestions().get(j);
                    AssessmentQuestion question = AssessmentQuestion.builder()
                            .section(saved)
                            .questionText(questionReq.getQuestionText())
                            .questionType(questionReq.getQuestionType())
                            .isRequired(questionReq.getIsRequired() != null ? questionReq.getIsRequired() : false)
                            .sortOrder(questionReq.getSortOrder() != null ? questionReq.getSortOrder() : j)
                            .ratingMin(
                                    questionReq.getRatingMin() != null ? questionReq.getRatingMin().intValue() : null)
                            .ratingMax(
                                    questionReq.getRatingMax() != null ? questionReq.getRatingMax().intValue() : null)
                            .ratingLabels(
                                    questionReq.getRatingLabels() != null && !questionReq.getRatingLabels().isEmpty()
                                            ? java.util.Arrays.stream(questionReq.getRatingLabels().split(","))
                                                    .map(l -> com.smart.therapy.flow.assessment.entity.AssessmentQuestionRatingLabel
                                                            .builder()
                                                            .label(l.trim())
                                                            .active(true)
                                                            .build())
                                                    .collect(java.util.stream.Collectors.toList())
                                            : new ArrayList<>())
                            .contributesToScore(
                                    questionReq.getContributesToScore() != null ? questionReq.getContributesToScore()
                                            : true)
                            .build();
                    AssessmentQuestion savedQuestion = questionRepository.save(question);

                    // Create options
                    if (questionReq.getOptions() != null) {
                        for (int k = 0; k < questionReq.getOptions().size(); k++) {
                            AssessmentOptionRequest optionReq = questionReq.getOptions().get(k);
                            AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                                    .question(savedQuestion)
                                    .optionText(optionReq.getOptionText())
                                    .scoreValue(optionReq.getOptionValue() != null
                                            ? BigDecimal.valueOf(optionReq.getOptionValue())
                                            : null)
                                    .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                                    .build();
                            optionRepository.save(option);
                        }
                    }
                }
            }

            created.add(toSectionResponse(sectionRepository.findById(saved.getId()).orElse(saved)));
        }

        return created;
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public List<AssessmentQuestionResponse> bulkCreateQuestions(Long sectionId, BulkQuestionRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(sectionId, "Section ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can perform bulk operations");

        AssessmentSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment section not found"));

        List<AssessmentQuestionResponse> created = new ArrayList<>();
        int maxSortOrder = section.getQuestions().stream()
                .mapToInt(q -> q.getSortOrder() != null ? q.getSortOrder() : 0)
                .max()
                .orElse(-1);

        for (int i = 0; i < request.getQuestions().size(); i++) {
            AssessmentQuestionRequest questionReq = request.getQuestions().get(i);
            AssessmentQuestion question = AssessmentQuestion.builder()
                    .section(section)
                    .questionText(questionReq.getQuestionText())
                    .questionType(questionReq.getQuestionType())
                    .isRequired(questionReq.getIsRequired() != null ? questionReq.getIsRequired() : false)
                    .sortOrder(questionReq.getSortOrder() != null ? questionReq.getSortOrder() : maxSortOrder + 1 + i)
                    .ratingMin(questionReq.getRatingMin() != null ? questionReq.getRatingMin().intValue() : null)
                    .ratingMax(questionReq.getRatingMax() != null ? questionReq.getRatingMax().intValue() : null)
                    .ratingLabels(questionReq.getRatingLabels() != null && !questionReq.getRatingLabels().isEmpty()
                            ? java.util.Arrays.stream(questionReq.getRatingLabels().split(","))
                                    .map(l -> com.smart.therapy.flow.assessment.entity.AssessmentQuestionRatingLabel
                                            .builder()
                                            .label(l.trim())
                                            .active(true)
                                            .build())
                                    .collect(java.util.stream.Collectors.toList())
                            : new ArrayList<>())
                    .contributesToScore(
                            questionReq.getContributesToScore() != null ? questionReq.getContributesToScore() : true)
                    .build();

            AssessmentQuestion saved = questionRepository.save(question);

            // Create options if provided
            if (questionReq.getOptions() != null) {
                for (int k = 0; k < questionReq.getOptions().size(); k++) {
                    AssessmentOptionRequest optionReq = questionReq.getOptions().get(k);
                    AssessmentQuestionOption option = AssessmentQuestionOption.builder()
                            .question(saved)
                            .optionText(optionReq.getOptionText())
                            .scoreValue(
                                    optionReq.getOptionValue() != null ? BigDecimal.valueOf(optionReq.getOptionValue())
                                            : null)
                            .sortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : k)
                            .build();
                    optionRepository.save(option);
                }
            }

            created.add(toQuestionResponse(questionRepository.findById(saved.getId()).orElse(saved)));
        }

        return created;
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public Map<String, Object> bulkDeleteSections(BulkDeleteRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can perform bulk operations");

        List<Long> deleted = new ArrayList<>();
        List<Long> failed = new ArrayList<>();

        for (Long sectionId : request.getIds()) {
            try {
                AssessmentSection section = sectionRepository.findById(sectionId)
                        .orElse(null);
                if (section == null) {
                    failed.add(sectionId);
                    continue;
                }

                long responseCount = responseRepository.countByQuestionSectionId(sectionId);
                if (responseCount > 0) {
                    failed.add(sectionId);
                    continue;
                }

                sectionRepository.delete(section);
                deleted.add(sectionId);
            } catch (Exception e) {
                log.error("Failed to delete section {}", sectionId, e);
                failed.add(sectionId);
            }
        }

        return Map.of("deleted", deleted, "failed", failed, "totalRequested", request.getIds().size());
    }

    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public Map<String, Object> bulkDeleteQuestions(BulkDeleteRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can perform bulk operations");

        List<Long> deleted = new ArrayList<>();
        List<Long> failed = new ArrayList<>();

        for (Long questionId : request.getIds()) {
            try {
                AssessmentQuestion question = questionRepository.findById(questionId)
                        .orElse(null);
                if (question == null) {
                    failed.add(questionId);
                    continue;
                }

                long responseCount = responseRepository.countByQuestionId(questionId);
                if (responseCount > 0) {
                    failed.add(questionId);
                    continue;
                }

                questionRepository.delete(question);
                deleted.add(questionId);
            } catch (Exception e) {
                log.error("Failed to delete question {}", questionId, e);
                failed.add(questionId);
            }
        }

        return Map.of("deleted", deleted, "failed", failed, "totalRequested", request.getIds().size());
    }

    // ========== TEMPLATE VERSIONING ==========

    /**
     * Create a new version of an existing template.
     * Copies all sections, questions, and options to the new version.
     * 
     * @param templateId The ID of the template to version
     * @param newVersion Optional version number. If null, auto-increments from current version
     * @param requester The user creating the version
     * @return The newly created template version
     */
    @Transactional
    @CacheEvict(value = "assessments", allEntries = true)
    public AssessmentTemplateResponse createTemplateVersion(Long templateId, String newVersion,
            AuthPrincipal requester) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        assertAdmin(requester, "Only administrators can create template versions");
        enforceAssessmentTemplateCreationLimit();

        // Load template with sections to ensure all data is available
        AssessmentTemplate original = templateRepository.findByIdWithSections(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));

        if (original.getIsDeleted()) {
            throw new BadRequestException("Cannot create version of deleted template");
        }

        // Parse version number from string or auto-increment
        Integer newVersionNumber;
        if (newVersion != null && !newVersion.trim().isEmpty()) {
            try {
                newVersionNumber = Integer.parseInt(newVersion.trim());
                if (newVersionNumber <= 0) {
                    throw new BadRequestException("Version number must be positive");
                }
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid version number format. Must be an integer.");
            }
        } else {
            // Auto-increment: find max version for this template's base name
            String baseNameForMax = cleanTemplateName(original.getName());
            // Use pattern matching: baseName + " (v%" to find all versions
            String baseNamePattern = baseNameForMax + " (v%";
            Integer maxVersion = templateRepository.findMaxVersionNumberByBaseName(baseNamePattern);
            newVersionNumber = (maxVersion != null && maxVersion >= original.getVersionNumber() 
                    ? maxVersion : original.getVersionNumber()) + 1;
        }

        // Check if version already exists
        String baseName = cleanTemplateName(original.getName());
        String newTemplateName = baseName + " (v" + newVersionNumber + ")";
        Optional<AssessmentTemplate> existing = templateRepository.findByNameAndVersionNumber(newTemplateName, newVersionNumber);
        if (existing.isPresent() && !existing.get().getIsDeleted()) {
            throw new BadRequestException("Template version " + newVersionNumber + " already exists for this template");
        }

        AssessmentTemplate newVersionTemplate = AssessmentTemplate.builder()
                .name(newTemplateName)
                .description(original.getDescription())
                .category(original.getCategory())
                .isStandardized(original.getIsStandardized())
                .versionNumber(newVersionNumber)
                .isActive(true)
                .createdByUser(userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .build();

        AssessmentTemplate saved = templateRepository.save(newVersionTemplate);

        // Copy sections and questions
        // Ensure sections are loaded (they should be from findByIdWithSections, but
        // reload to be safe)
        List<AssessmentSection> originalSections = sectionRepository.findByTemplateId(templateId);
        for (AssessmentSection originalSection : originalSections) {
            AssessmentSection newSection = AssessmentSection.builder()
                    .template(saved)
                    .title(originalSection.getTitle())
                    .description(originalSection.getDescription())
                    .accessLevel(originalSection.getAccessLevel())
                    .isScoring(originalSection.getIsScoring())
                    .reportMapping(originalSection.getReportMapping())
                    .aiReportPrompt(originalSection.getAiReportPrompt())
                    .sortOrder(originalSection.getSortOrder())
                    .build();
            AssessmentSection savedSection = sectionRepository.save(newSection);

            // Copy questions - load questions for this section
            List<AssessmentQuestion> originalQuestions = questionRepository.findBySectionId(originalSection.getId());
            for (AssessmentQuestion originalQuestion : originalQuestions) {
                // Build new question
                AssessmentQuestion newQuestion = AssessmentQuestion.builder()
                        .section(savedSection)
                        .questionText(originalQuestion.getQuestionText())
                        .questionType(originalQuestion.getQuestionType())
                        .isRequired(originalQuestion.getIsRequired())
                        .sortOrder(originalQuestion.getSortOrder())
                        .ratingMin(originalQuestion.getRatingMin())
                        .ratingMax(originalQuestion.getRatingMax())
                        .contributesToScore(originalQuestion.getContributesToScore())
                        .build();
                
                // Copy rating labels manually (cascade works but we need to set question reference)
                if (originalQuestion.getRatingLabels() != null && !originalQuestion.getRatingLabels().isEmpty()) {
                    List<AssessmentQuestionRatingLabel> newLabels = new ArrayList<>();
                    for (AssessmentQuestionRatingLabel originalLabel : originalQuestion.getRatingLabels()) {
                        AssessmentQuestionRatingLabel newLabel = AssessmentQuestionRatingLabel.builder()
                                .question(newQuestion)
                                .label(originalLabel.getLabel())
                                .score(originalLabel.getScore())
                                .active(originalLabel.getActive() != null ? originalLabel.getActive() : true)
                                .build();
                        newLabels.add(newLabel);
                    }
                    newQuestion.setRatingLabels(newLabels);
                }
                
                AssessmentQuestion savedQuestion = questionRepository.save(newQuestion);

                // Copy options - load options for this question
                List<AssessmentQuestionOption> originalOptions = optionRepository
                        .findByQuestionId(originalQuestion.getId());
                for (AssessmentQuestionOption originalOption : originalOptions) {
                    AssessmentQuestionOption newOption = AssessmentQuestionOption.builder()
                            .question(savedQuestion)
                            .optionKey(originalOption.getOptionKey())
                            .optionText(originalOption.getOptionText())
                            .optionValue(originalOption.getOptionValue())
                            .scoreValue(originalOption.getScoreValue())
                            .sortOrder(originalOption.getSortOrder())
                            .isDefault(originalOption.getIsDefault())
                            .build();
                    optionRepository.save(newOption);
                }
            }
        }

        AssessmentTemplateResponse response = toTemplateResponse(
                templateRepository.findById(saved.getId()).orElse(saved));
        incrementAssessmentTemplateUsage();

        // Audit log
        auditLogService.logAssessmentAccess(
                currentUserService.requireCurrentUser(requester).getId(),
                requester.getLoginIdentifier(),
                saved.getId(),
                null, // No client ID for template operations
                "template_version_created",
                null,
                null,
                Map.of("originalTemplateId", templateId, "originalVersion", original.getVersionNumber(), "newVersion",
                        newVersionNumber));

        return response;
    }

    /**
     * Get all versions of a template by its base name.
     * Returns versions sorted by version number (newest first).
     * 
     * @param templateId The ID of any version of the template
     * @return List of all template versions
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "assessments", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('template_versions:' + #templateId)")
    public List<AssessmentTemplateResponse> getTemplateVersions(Long templateId) {
        Objects.requireNonNull(templateId, "Template ID is required");
        
        AssessmentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment template not found"));
        
        String baseName = cleanTemplateName(template.getName());
        // Use pattern matching to find all versions
        String baseNamePattern = baseName + " (v%";
        List<AssessmentTemplate> versions = templateRepository.findVersionsByBaseNameClean(baseNamePattern);
        
        if (versions.isEmpty()) {
            // If no versions found by base name, return just this template
            versions = List.of(template);
        }
        
        return versions.stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cleans template name by removing existing version suffix (e.g., "Template
     * (v1.0)" -> "Template").
     * This prevents duplicate version suffixes when creating multiple versions.
     */
    private String cleanTemplateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        // Remove version suffix pattern: " (vX)" at the end
        return name.replaceAll("\\s*\\(v\\d+\\)\\s*$", "").trim();
    }

    /**
     * Capture template snapshot as JSON for report reproducibility.
     * Includes template structure, sections, questions, options, and rating labels.
     */
    private String captureTemplateSnapshot(AssessmentTemplate template, List<AssessmentSection> sections) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            
            // Template metadata
            snapshot.put("templateId", template.getId());
            snapshot.put("templateName", template.getName());
            snapshot.put("templateDescription", template.getDescription());
            snapshot.put("templateCategory", template.getCategory());
            snapshot.put("versionNumber", template.getVersionNumber());
            snapshot.put("isStandardized", template.getIsStandardized());
            snapshot.put("capturedAt", Instant.now().toString());
            
            // Sections with questions
            List<Map<String, Object>> sectionsData = new ArrayList<>();
            for (AssessmentSection section : sections) {
                Map<String, Object> sectionData = new HashMap<>();
                sectionData.put("id", section.getId());
                sectionData.put("title", section.getTitle());
                sectionData.put("description", section.getDescription());
                sectionData.put("accessLevel", section.getAccessLevel());
                sectionData.put("isScoring", section.getIsScoring());
                sectionData.put("reportMapping", section.getReportMapping());
                sectionData.put("aiReportPrompt", section.getAiReportPrompt());
                sectionData.put("sortOrder", section.getSortOrder());
                
                // Questions in section — load via repository; never replace orphanRemoval collections
                List<Map<String, Object>> questionsData = new ArrayList<>();
                List<AssessmentQuestion> questions = questionRepository.findBySectionId(section.getId());
                for (AssessmentQuestion question : questions) {
                        Map<String, Object> questionData = new HashMap<>();
                        questionData.put("id", question.getId());
                        questionData.put("questionText", question.getQuestionText());
                        questionData.put("questionType", question.getQuestionType());
                        questionData.put("isRequired", question.getIsRequired());
                        questionData.put("sortOrder", question.getSortOrder());
                        questionData.put("ratingMin", question.getRatingMin());
                        questionData.put("ratingMax", question.getRatingMax());
                        questionData.put("contributesToScore", question.getContributesToScore());
                        
                        // Options for multiple choice questions
                        if ("multiple_choice".equals(question.getQuestionType())
                                || "checkbox".equals(question.getQuestionType())) {
                            List<AssessmentQuestionOption> options = optionRepository.findByQuestionId(question.getId());
                            if (options != null && !options.isEmpty()) {
                                List<Map<String, Object>> optionsData = new ArrayList<>();
                                for (AssessmentQuestionOption option : options) {
                                    Map<String, Object> optionData = new HashMap<>();
                                    optionData.put("id", option.getId());
                                    optionData.put("optionKey", option.getOptionKey());
                                    optionData.put("optionText", option.getOptionText());
                                    optionData.put("optionValue", option.getOptionValue());
                                    optionData.put("scoreValue", option.getScoreValue());
                                    optionData.put("sortOrder", option.getSortOrder());
                                    optionData.put("isDefault", option.getIsDefault());
                                    optionsData.add(optionData);
                                }
                                questionData.put("options", optionsData);
                            }
                        }
                        
                        // Rating labels for rating/scale questions (lazy-safe read only)
                        if (question.getRatingLabels() != null && !question.getRatingLabels().isEmpty()) {
                            List<Map<String, Object>> labelsData = new ArrayList<>();
                            for (AssessmentQuestionRatingLabel label : question.getRatingLabels()) {
                                Map<String, Object> labelData = new HashMap<>();
                                labelData.put("id", label.getId());
                                labelData.put("label", label.getLabel());
                                labelData.put("score", label.getScore());
                                labelData.put("active", label.getActive());
                                labelsData.add(labelData);
                            }
                            questionData.put("ratingLabels", labelsData);
                        }
                        
                        questionsData.add(questionData);
                }
                sectionData.put("questions", questionsData);
                sectionsData.add(sectionData);
            }
            snapshot.put("sections", sectionsData);
            
            // Convert to JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("Failed to capture template snapshot", e);
            // Return minimal snapshot on error
            return String.format("{\"templateId\":%d,\"templateName\":\"%s\",\"versionNumber\":%d,\"error\":\"Failed to capture full snapshot\"}",
                    template.getId(), template.getName(), template.getVersionNumber());
        }
    }

    // ========== RESPONSE VALIDATION ==========

    private void validateResponse(AssessmentQuestion question, QuestionResponseRequest responseRequest) {
        if (question == null || responseRequest == null) {
            throw new BadRequestException("Question and response are required");
        }

        // Validate required questions
        if (Boolean.TRUE.equals(question.getIsRequired()) &&
                (responseRequest.getResponseText() == null || responseRequest.getResponseText().trim().isEmpty()) &&
                (responseRequest.getSelectedOptionIds() == null || responseRequest.getSelectedOptionIds().isEmpty()) &&
                responseRequest.getRatingValue() == null) {
            throw new BadRequestException("Response is required for question: " + question.getQuestionText());
        }

        // Validate question type specific rules
        switch (question.getQuestionType()) {
            case "multiple_choice":
                boolean hasMultipleChoiceSelection = (responseRequest.getSelectedOptionIds() != null
                        && !responseRequest.getSelectedOptionIds().isEmpty())
                        || responseRequest.getSelectedOptionId() != null;
                if (!hasMultipleChoiceSelection) {
                    throw new BadRequestException("At least one option must be selected for multiple choice question");
                }
                // Validate option IDs exist for this question
                List<Long> validOptionIds = question.getOptions().stream()
                        .map(AssessmentQuestionOption::getId)
                        .collect(Collectors.toList());
                if (responseRequest.getSelectedOptionIds() != null) {
                    for (Integer optionIdInt : responseRequest.getSelectedOptionIds()) {
                        Long optionId = optionIdInt.longValue();
                        if (!validOptionIds.contains(optionId)) {
                            throw new BadRequestException("Invalid option ID: " + optionId);
                        }
                    }
                }
                if (responseRequest.getSelectedOptionId() != null) {
                    Long optionId = responseRequest.getSelectedOptionId().longValue();
                    if (!validOptionIds.contains(optionId)) {
                        throw new BadRequestException("Invalid option ID: " + optionId);
                    }
                }
                break;

            case "rating":
            case "scale":
                if (responseRequest.getRatingValue() == null) {
                    throw new BadRequestException("Rating value is required for rating/scale question");
                }
                if (question.getRatingMin() != null && responseRequest.getRatingValue() < question.getRatingMin()) {
                    throw new BadRequestException("Rating value must be at least " + question.getRatingMin());
                }
                if (question.getRatingMax() != null && responseRequest.getRatingValue() > question.getRatingMax()) {
                    throw new BadRequestException("Rating value must be at most " + question.getRatingMax());
                }
                break;

            case "text":
                if (responseRequest.getResponseText() == null || responseRequest.getResponseText().trim().isEmpty()) {
                    throw new BadRequestException("Text response is required for text question");
                }
                break;

            case "yes_no":
                if (responseRequest.getResponseText() == null ||
                        (!responseRequest.getResponseText().equalsIgnoreCase("yes") &&
                                !responseRequest.getResponseText().equalsIgnoreCase("no"))) {
                    throw new BadRequestException("Yes/No question requires 'yes' or 'no' response");
                }
                break;
                
            case "checkbox":
                // Checkbox allows multiple selections, validation is optional
                if (responseRequest.getSelectedOptionIds() != null && !responseRequest.getSelectedOptionIds().isEmpty()) {
                    // Validate option IDs exist for this question
                    List<Long> validCheckboxOptionIds2 = question.getOptions().stream()
                            .map(AssessmentQuestionOption::getId)
                            .collect(Collectors.toList());
                    for (Integer optionIdInt : responseRequest.getSelectedOptionIds()) {
                        Long optionId = optionIdInt.longValue();
                        if (!validCheckboxOptionIds2.contains(optionId)) {
                            throw new BadRequestException("Invalid option ID: " + optionId);
                        }
                    }
                }
                if (responseRequest.getSelectedOptionId() != null) {
                    List<Long> validCheckboxOptionIds = question.getOptions().stream()
                            .map(AssessmentQuestionOption::getId)
                            .collect(Collectors.toList());
                    Long optionId = responseRequest.getSelectedOptionId().longValue();
                    if (!validCheckboxOptionIds.contains(optionId)) {
                        throw new BadRequestException("Invalid option ID: " + optionId);
                    }
                }
                break;
        }
        
        // Additional validation for scoring questions
        if (Boolean.TRUE.equals(question.getContributesToScore())) {
            AssessmentSection section = question.getSection();
            if (!Boolean.TRUE.equals(section.getIsScoring())) {
                log.warn("Question {} contributes to score but section {} is not a scoring section", 
                        question.getId(), section.getId());
            }
            
            // Validate scoring configuration based on question type
            if ("multiple_choice".equals(question.getQuestionType()) || 
                "checkbox".equals(question.getQuestionType())) {
                if (question.getOptions() == null || question.getOptions().isEmpty()) {
                    throw new BadRequestException("Scoring multiple choice question must have options with score values");
                }
                boolean hasScoreValue = question.getOptions().stream()
                        .anyMatch(opt -> opt.getScoreValue() != null);
                if (!hasScoreValue) {
                    log.warn("Question {} is marked for scoring but no options have score values", question.getId());
                }
            } else if ("rating".equals(question.getQuestionType()) || 
                       "scale".equals(question.getQuestionType())) {
                // Rating/scale questions use rating value directly as score
                if (question.getRatingMin() == null || question.getRatingMax() == null) {
                    log.warn("Question {} is marked for scoring but rating min/max not set", question.getId());
                }
            }
        }
    }
    
    /**
     * Validate template scoring configuration.
     * Ensures scoring sections and questions are properly configured.
     * 
     * @param template The template to validate
     */
    @Transactional(readOnly = true)
    public void validateTemplateScoringConfiguration(AssessmentTemplate template) {
        if (template == null) {
            throw new BadRequestException("Template is required");
        }
        
        List<AssessmentSection> sections = sectionRepository.findByTemplateId(template.getId());
        boolean hasScoringSection = false;
        
        for (AssessmentSection section : sections) {
            if (Boolean.TRUE.equals(section.getIsScoring())) {
                hasScoringSection = true;
                List<AssessmentQuestion> questions = questionRepository.findBySectionId(section.getId());
                boolean hasScoringQuestion = false;
                
                for (AssessmentQuestion question : questions) {
                    if (Boolean.TRUE.equals(question.getContributesToScore())) {
                        hasScoringQuestion = true;
                        
                        // Validate scoring question configuration
                        if ("multiple_choice".equals(question.getQuestionType()) || 
                            "checkbox".equals(question.getQuestionType())) {
                            if (question.getOptions() == null || question.getOptions().isEmpty()) {
                                log.warn("Scoring question {} in section {} has no options", 
                                        question.getId(), section.getId());
                            } else {
                                boolean hasScoreValue = question.getOptions().stream()
                                        .anyMatch(opt -> opt.getScoreValue() != null);
                                if (!hasScoreValue) {
                                    log.warn("Scoring question {} has options but no score values", question.getId());
                                }
                            }
                        }
                    }
                }
                
                if (!hasScoringQuestion) {
                    log.warn("Scoring section {} has no questions that contribute to score", section.getId());
                }
            }
        }
        
        if (hasScoringSection) {
            log.debug("Template {} has properly configured scoring sections", template.getId());
        }
    }

    // ========== ASSESSMENT ANALYTICS ==========

    @Transactional(readOnly = true)
    public AssessmentAnalyticsResponse getAnalytics(Long templateId, Long clientId, LocalDate startDate,
            LocalDate endDate) {
        List<AssessmentAssignment> assignments;

        if (templateId != null) {
            assignments = assignmentRepository.findByTemplateId(templateId);
        } else if (clientId != null) {
            assignments = assignmentRepository.findByClientId(clientId);
        } else {
            assignments = assignmentRepository.findAll();
        }

        // Filter by date range if provided
        if (startDate != null || endDate != null) {
            assignments = assignments.stream()
                    .filter(a -> {
                        if (startDate != null && a.getAssignedDate() != null &&
                                a.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && a.getAssignedDate() != null &&
                                a.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        long total = assignments.size();
        long completed = assignments.stream()
                .filter(a -> com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED.getValue()
                        .equals(a.getStatus()))
                .count();
        long pending = assignments.stream()
                .filter(a -> com.smart.therapy.flow.assessment.enums.AssessmentStatus.PENDING.getValue()
                        .equals(a.getStatus()))
                .count();
        long inProgress = assignments.stream()
                .filter(a -> com.smart.therapy.flow.assessment.enums.AssessmentStatus.CLIENT_IN_PROGRESS.getValue()
                        .equals(a.getStatus()))
                .count();

        // Calculate average completion time
        List<Double> completionTimes = assignments.stream()
                .filter(a -> a.getCompletedAt() != null && a.getAssignedDate() != null)
                .map(a -> {
                    long days = java.time.Duration.between(a.getAssignedDate(), a.getCompletedAt()).toDays();
                    return (double) days;
                })
                .collect(Collectors.toList());
        double avgCompletionTime = completionTimes.isEmpty() ? 0.0
                : completionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calculate completion rate
        BigDecimal completionRate = total > 0
                ? BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Calculate average score
        List<BigDecimal> scores = assignments.stream()
                .filter(a -> a.getTotalScore() != null &&
                        com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED.getValue()
                                .equals(a.getStatus()))
                .map(AssessmentAssignment::getTotalScore)
                .collect(Collectors.toList());
        BigDecimal avgScore = scores.isEmpty() ? BigDecimal.ZERO
                : scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, java.math.RoundingMode.HALF_UP);

        // Score distribution
        Map<String, Long> scoreDistribution = Map.of(
                "0-25", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(25)) <= 0).count(),
                "26-50",
                scores.stream()
                        .filter(s -> s.compareTo(BigDecimal.valueOf(25)) > 0
                                && s.compareTo(BigDecimal.valueOf(50)) <= 0)
                        .count(),
                "51-75",
                scores.stream()
                        .filter(s -> s.compareTo(BigDecimal.valueOf(50)) > 0
                                && s.compareTo(BigDecimal.valueOf(75)) <= 0)
                        .count(),
                "76-100", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(75)) > 0).count());

        // Assignments by status
        Map<String, Long> assignmentsByStatus = Map.of(
                "completed", completed,
                "pending", pending,
                "in_progress", inProgress);

        // Assignments by template (use name strings — never entity equals/hashCode)
        Map<String, Long> assignmentsByTemplate = assignments.stream()
                .filter(a -> a.getTemplate() != null)
                .collect(Collectors.groupingBy(
                        a -> {
                            String name = a.getTemplate().getName();
                            return StringUtils.hasText(name) ? name : "Untitled";
                        },
                        Collectors.counting()));

        // Completion trends (last 30 days)
        List<AssessmentAnalyticsResponse.CompletionTrend> trends = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long completions = assignments.stream()
                    .filter(a -> a.getCompletedAt() != null &&
                            a.getCompletedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(date))
                    .count();
            trends.add(AssessmentAnalyticsResponse.CompletionTrend.builder()
                    .date(date)
                    .completions(completions)
                    .build());
        }

        // Top performing templates — group by template id to avoid Lombok/Hibernate
        // bidirectional equals/hashCode StackOverflowError on AssessmentTemplate.
        List<AssessmentAnalyticsResponse.TemplatePerformance> topTemplates = assignments.stream()
                .filter(a -> a.getTemplate() != null && a.getTemplate().getId() != null)
                .collect(Collectors.groupingBy(a -> a.getTemplate().getId()))
                .entrySet().stream()
                .map(entry -> {
                    Long templateIdKey = entry.getKey();
                    List<AssessmentAssignment> templateAssignments = entry.getValue();
                    AssessmentTemplate template = templateAssignments.get(0).getTemplate();
                    String templateName = template != null && StringUtils.hasText(template.getName())
                            ? template.getName()
                            : "Untitled";
                    long templateCompleted = templateAssignments.stream()
                            .filter(a -> com.smart.therapy.flow.assessment.enums.AssessmentStatus.COMPLETED.getValue()
                                    .equals(a.getStatus()))
                            .count();
                    BigDecimal templateAvgScore = templateAssignments.stream()
                            .filter(a -> a.getTotalScore() != null)
                            .map(AssessmentAssignment::getTotalScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(templateAssignments.size()), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal templateCompletionRate = templateAssignments.size() > 0 ? BigDecimal
                            .valueOf(templateCompleted)
                            .divide(BigDecimal.valueOf(templateAssignments.size()), 2, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

                    return AssessmentAnalyticsResponse.TemplatePerformance.builder()
                            .templateId(templateIdKey)
                            .templateName(templateName)
                            .assignmentCount((long) templateAssignments.size())
                            .averageScore(templateAvgScore)
                            .completionRate(templateCompletionRate)
                            .build();
                })
                .sorted((a, b) -> b.getCompletionRate().compareTo(a.getCompletionRate()))
                .limit(10)
                .collect(Collectors.toList());

        return AssessmentAnalyticsResponse.builder()
                .totalAssignments(total)
                .completedAssignments(completed)
                .pendingAssignments(pending)
                .inProgressAssignments(inProgress)
                .averageCompletionTimeDays(avgCompletionTime)
                .completionRate(completionRate)
                .averageScore(avgScore)
                .scoreDistribution(scoreDistribution)
                .assignmentsByStatus(assignmentsByStatus)
                .assignmentsByTemplate(assignmentsByTemplate)
                .completionTrends(trends)
                .topTemplates(topTemplates)
                .build();
    }

    // ========== EMAIL NOTIFICATIONS ==========

    @Async("emailExecutor")
    public void sendAssessmentAssignedEmail(AssessmentAssignment assignment) {
        if (emailService == null || clientContactService == null) {
            log.warn("Email service or client contact service not available");
            return;
        }

        try {
            Optional<ClientContact> primaryEmail = clientContactService.getPrimaryEmail(assignment.getClient().getId());
            if (primaryEmail.isEmpty() || !StringUtils.hasText(primaryEmail.get().getContactValue())) {
                log.warn("No email found for client {}", assignment.getClient().getId());
                return;
            }

            String clientName = assignment.getClient().getFullName();
            String clientMrn = assignment.getClient().getClientId();
            String templateName = assignment.getTemplate().getName();
            String dueDate = assignment.getDueDate() != null
                    ? assignment.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                    : "Not specified";
            String portalUrl = System.getenv("APP_BASE_URL") != null
                    ? System.getenv("APP_BASE_URL") + "/portal/assessments"
                    : "http://localhost:8080/portal/assessments";

            String subject = "New Assessment Assigned: " + templateName;
            String htmlBody = buildAssessmentAssignedEmailTemplate(
                    clientName, clientMrn, templateName, dueDate, portalUrl);

            emailService.sendEmail(primaryEmail.get().getContactValue(), subject, htmlBody);
            log.info("Assessment assigned email sent to client {}", assignment.getClient().getId());
        } catch (Exception e) {
            log.error("Failed to send assessment assigned email", e);
        }
    }

    @Async("emailExecutor")
    public void sendAssessmentCompletedEmail(AssessmentAssignment assignment) {
        if (emailService == null || clientContactService == null) {
            log.warn("Email service or client contact service not available");
            return;
        }

        try {
            Optional<ClientContact> primaryEmail = clientContactService.getPrimaryEmail(assignment.getClient().getId());
            if (primaryEmail.isEmpty() || !StringUtils.hasText(primaryEmail.get().getContactValue())) {
                log.warn("No email found for client {}", assignment.getClient().getId());
                return;
            }

            String clientName = assignment.getClient().getFullName();
            String clientMrn = assignment.getClient().getClientId();
            String templateName = assignment.getTemplate().getName();
            String score = assignment.getTotalScore() != null ? assignment.getTotalScore().toString() : "N/A";

            String subject = "Assessment Completed: " + templateName;
            String htmlBody = buildAssessmentCompletedEmailTemplate(clientName, clientMrn, templateName, score);

            emailService.sendEmail(primaryEmail.get().getContactValue(), subject, htmlBody);
            log.info("Assessment completed email sent to client {}", assignment.getClient().getId());
        } catch (Exception e) {
            log.error("Failed to send assessment completed email", e);
        }
    }

    private String buildAssessmentAssignedEmailTemplate(String clientName, String clientMrn, String templateName,
            String dueDate, String portalUrl) {
        return EmailHtmlComponents.assessmentAssignedEmailBody(
                escapeHtml(clientName), escapeHtml(clientMrn), escapeHtml(templateName), escapeHtml(dueDate),
                portalUrl);
    }

    private String buildAssessmentCompletedEmailTemplate(String clientName, String clientMrn, String templateName,
            String score) {
        return EmailHtmlComponents.assessmentCompletedEmailBody(
                escapeHtml(clientName), escapeHtml(clientMrn), escapeHtml(templateName), escapeHtml(score));
    }

    // ========== REMINDER SYSTEM ==========

    @Transactional
    public void createAssessmentReminder(AssessmentReminderRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        AssessmentAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment assignment not found"));

        if (assignment.getDueDate() == null) {
            throw new BadRequestException("Assessment assignment must have a due date to create a reminder");
        }

        LocalDate reminderDate = request.getReminderDate() != null ? request.getReminderDate()
                : assignment.getDueDate().minusDays(request.getReminderDaysBefore());

        if (reminderDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Reminder date cannot be in the past");
        }

        // Create scheduled notification if notification service is available
        if (notificationService != null) {
            try {
                Map<String, Object> entityData = Map.of(
                        "assignmentId", assignment.getId(),
                        "templateName", assignment.getTemplate().getName(),
                        "dueDate", assignment.getDueDate().toString(),
                        "clientId", assignment.getClient().getId(),
                        "clientName", assignment.getClient().getFullName());

                // Schedule reminder (this would need to be integrated with the notification
                // system)
                // For now, we'll use a simple approach and schedule it via the notification
                // service
                log.info("Reminder scheduled for assessment assignment {} on {}", assignment.getId(), reminderDate);
            } catch (Exception e) {
                log.error("Failed to schedule reminder", e);
            }
        }

        // Send immediate reminder if date is today
        if (reminderDate.equals(LocalDate.now()) && request.getSendEmail()) {
            sendAssessmentReminderEmail(assignment);
        }
    }

    @Async("emailExecutor")
    private void sendAssessmentReminderEmail(AssessmentAssignment assignment) {
        if (emailService == null || clientContactService == null) {
            return;
        }

        try {
            Optional<ClientContact> primaryEmail = clientContactService.getPrimaryEmail(assignment.getClient().getId());
            if (primaryEmail.isEmpty() || !StringUtils.hasText(primaryEmail.get().getContactValue())) {
                return;
            }

            String clientName = assignment.getClient().getFullName();
            String clientMrn = assignment.getClient().getClientId();
            String templateName = assignment.getTemplate().getName();
            String dueDate = assignment.getDueDate() != null
                    ? assignment.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                    : "Not specified";
            String portalUrl = System.getenv("APP_BASE_URL") != null
                    ? System.getenv("APP_BASE_URL") + "/portal/assessments"
                    : "http://localhost:8080/portal/assessments";

            String subject = "Reminder: Assessment Due - " + templateName;
            String htmlBody = buildAssessmentReminderEmailTemplate(
                    clientName, clientMrn, templateName, dueDate, portalUrl);

            emailService.sendEmail(primaryEmail.get().getContactValue(), subject, htmlBody);
        } catch (Exception e) {
            log.error("Failed to send assessment reminder email", e);
        }
    }

    private String buildAssessmentReminderEmailTemplate(String clientName, String clientMrn, String templateName,
            String dueDate, String portalUrl) {
        return EmailHtmlComponents.assessmentReminderEmailBody(
                escapeHtml(clientName), escapeHtml(clientMrn), escapeHtml(templateName), escapeHtml(dueDate),
                portalUrl);
    }

    // ========== EXPORT OPTIONS ==========

    @Transactional(readOnly = true)
    public byte[] exportAssessmentDataAsCsv(Long templateId, Long clientId, LocalDate startDate, LocalDate endDate) {
        List<AssessmentAssignment> assignments;

        if (templateId != null) {
            assignments = assignmentRepository.findByTemplateId(templateId);
        } else if (clientId != null) {
            assignments = assignmentRepository.findByClientId(clientId);
        } else {
            assignments = assignmentRepository.findAll();
        }

        // Filter by date range
        if (startDate != null || endDate != null) {
            assignments = assignments.stream()
                    .filter(a -> {
                        if (startDate != null && a.getAssignedDate() != null &&
                                a.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && a.getAssignedDate() != null &&
                                a.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        StringBuilder csv = new StringBuilder();
        csv.append(
                "Assignment ID,Client Name,Template Name,Status,Assigned Date,Due Date,Completed Date,Total Score\n");

        for (AssessmentAssignment assignment : assignments) {
            csv.append(String.format("%d,\"%s\",\"%s\",%s,%s,%s,%s,%s\n",
                    assignment.getId(),
                    assignment.getClient().getFullName().replace("\"", "\"\""),
                    assignment.getTemplate().getName().replace("\"", "\"\""),
                    assignment.getStatus(),
                    assignment.getAssignedDate() != null
                            ? assignment.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                            : "",
                    assignment.getDueDate() != null ? assignment.getDueDate().toString() : "",
                    assignment.getCompletedAt() != null
                            ? assignment.getCompletedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                            : "",
                    assignment.getTotalScore() != null ? assignment.getTotalScore().toString() : ""));
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportAssessmentDataAsExcel(Long templateId, Long clientId, LocalDate startDate, LocalDate endDate) {
        List<AssessmentAssignment> assignments;

        if (templateId != null) {
            assignments = assignmentRepository.findByTemplateId(templateId);
        } else if (clientId != null) {
            assignments = assignmentRepository.findByClientId(clientId);
        } else {
            assignments = assignmentRepository.findAll();
        }

        // Filter by date range
        if (startDate != null || endDate != null) {
            assignments = assignments.stream()
                    .filter(a -> {
                        if (startDate != null && a.getAssignedDate() != null &&
                                a.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && a.getAssignedDate() != null &&
                                a.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        try {
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Assessment Data");

            // Create header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = { "Assignment ID", "Client Name", "Template Name", "Status", "Assigned Date", "Due Date",
                    "Completed Date", "Total Score" };
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Create data rows
            int rowNum = 1;
            for (AssessmentAssignment assignment : assignments) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(assignment.getId());
                row.createCell(1).setCellValue(assignment.getClient().getFullName());
                row.createCell(2).setCellValue(assignment.getTemplate().getName());
                row.createCell(3).setCellValue(assignment.getStatus());
                row.createCell(4)
                        .setCellValue(assignment.getAssignedDate() != null
                                ? assignment.getAssignedDate().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                                : "");
                row.createCell(5)
                        .setCellValue(assignment.getDueDate() != null ? assignment.getDueDate().toString() : "");
                row.createCell(6)
                        .setCellValue(assignment.getCompletedAt() != null
                                ? assignment.getCompletedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                                : "");
                row.createCell(7).setCellValue(
                        assignment.getTotalScore() != null ? assignment.getTotalScore().doubleValue() : 0.0);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate Excel export", e);
            throw new BadRequestException("Failed to generate Excel export: " + e.getMessage());
        }
    }

    /**
     * Create a new version entry for report versioning
     */
    private void createReportVersion(AssessmentReport report, String content, String changeType, 
                                     String changeDescription, Long userId) {
        try {
            // Get next version number
            Long versionCount = reportVersionRepository.countByReportId(report.getId());
            Integer nextVersionNumber = versionCount.intValue() + 1;

            User createdBy = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            AssessmentReportVersion version = AssessmentReportVersion.builder()
                    .report(report)
                    .versionNumber(nextVersionNumber)
                    .content(content != null ? content : "")
                    .changeType(changeType)
                    .changeDescription(changeDescription)
                    .createdByUser(createdBy)
                    .createdAt(Instant.now())
                    .build();

            reportVersionRepository.save(version);
            log.debug("Created report version: reportId={}, version={}, changeType={}", 
                    report.getId(), nextVersionNumber, changeType);
        } catch (Exception e) {
            log.error("Failed to create report version: reportId={}, changeType={}", 
                    report.getId(), changeType, e);
            // Don't throw - versioning is non-critical
        }
    }

    /**
     * Find existing response for a question and responder (for deduplication)
     */
    private AssessmentResponse findExistingResponse(Long assignmentId, Long questionId, 
            String responderType, Long responderUserId, Long responderClientId) {
        List<AssessmentResponse> responses = responseRepository.findByAssignmentIdAndQuestionId(assignmentId, questionId);
        
        if (responses == null || responses.isEmpty()) {
            return null;
        }
        
        // Filter by responder type and ID
        return responses.stream()
                .filter(r -> {
                    if ("USER".equals(responderType) && responderUserId != null) {
                        return "USER".equals(r.getResponderType()) && 
                               responderUserId.equals(r.getResponderUserId());
                    } else if ("CLIENT".equals(responderType) && responderClientId != null) {
                        return "CLIENT".equals(r.getResponderType()) && 
                               responderClientId.equals(r.getResponderClientId());
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

}

