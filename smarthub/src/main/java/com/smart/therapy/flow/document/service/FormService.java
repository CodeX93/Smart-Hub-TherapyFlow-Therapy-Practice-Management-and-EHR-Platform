package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.dto.PatchUpdates;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.entity.*;
import com.smart.therapy.flow.document.enums.FieldType;
import com.smart.therapy.flow.document.enums.FormTemplateVersionStatus;
import com.smart.therapy.flow.document.enums.FromCategory;
import com.smart.therapy.flow.document.enums.Status;
import com.smart.therapy.flow.document.enums.SignatureType;
import com.smart.therapy.flow.document.repository.*;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FormService {

    private static final String RESOURCE_TYPE_FORM = "form";
    private static final int DEFAULT_TEMPLATE_PAGE = 1;
    private static final int DEFAULT_TEMPLATE_PAGE_SIZE = 20;
    private static final int MAX_TEMPLATE_PAGE_SIZE = 200;

    private final FormTemplateRepository formTemplateRepository;
    private final FormTemplateVersionRepository formTemplateVersionRepository;
    private final FormSectionRepository formSectionRepository;
    private final FormFieldRepository formFieldRepository;
    private final FormFieldOptionRepository formFieldOptionRepository;
    private final FormAssignmentRepository formAssignmentRepository;
    private final FormAssignmentFieldRepository formAssignmentFieldRepository;
    private final FormResponseRepository formResponseRepository;
    private final FormSignatureRepository formSignatureRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final ClientReportAccessService clientReportAccessService;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private com.smart.therapy.flow.notification.service.NotificationService notificationService;

    // ========== TEMPLATE METHODS ==========

    @Transactional(readOnly = true)
    @Cacheable(value = "forms", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('templates')")
    public List<FormTemplateResponse> getTemplates() {
        return getTemplates(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<FormTemplateResponse> getTemplates(Integer page, Integer pageSize, String search, String category) {
        List<FormTemplate> templates = new ArrayList<>(formTemplateRepository.findActiveTemplatesNewestFirst());
        templates.sort(Comparator
                .comparing(FormTemplate::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FormTemplate::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        if (StringUtils.hasText(search)) {
            String term = search.trim().toLowerCase(Locale.ROOT);
            templates = templates.stream()
                    .filter(template -> {
                        String name = template.getName() != null ? template.getName().toLowerCase(Locale.ROOT) : "";
                        String description = template.getDescription() != null
                                ? template.getDescription().toLowerCase(Locale.ROOT)
                                : "";
                        return name.contains(term) || description.contains(term);
                    })
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(category)) {
            String normalizedCategory = normalizeCategoryToken(category);
            templates = templates.stream()
                    .filter(template -> template.getCategory() != null
                            && normalizeCategoryToken(template.getCategory().name()).equals(normalizedCategory))
                    .collect(Collectors.toList());
        }

        int safePage = page != null ? Math.max(DEFAULT_TEMPLATE_PAGE, page) : DEFAULT_TEMPLATE_PAGE;
        int requestedPageSize = pageSize != null ? pageSize : DEFAULT_TEMPLATE_PAGE_SIZE;
        int safePageSize = Math.min(Math.max(requestedPageSize, 1), MAX_TEMPLATE_PAGE_SIZE);
        int fromIndex = (safePage - 1) * safePageSize;

        if (fromIndex >= templates.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + safePageSize, templates.size());
        List<FormTemplate> pageSlice = templates.subList(fromIndex, toIndex);

        return pageSlice.stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "forms", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('template:' + #id)")
    public FormTemplateResponse getTemplate(Long id) {
        FormTemplate template = formTemplateRepository.findByIdWithVersions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));
        return toTemplateResponseWithVersions(template);
    }

    @Transactional
    public FormTemplateResponse createTemplate(CreateFormTemplateRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create form templates");

        // Plan limit: FORM_TEMPLATES
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            Integer limit = subscriptionFeatureService.getEffectiveLimit(orgId, SubscriptionFeatureService.FEATURE_FORM_TEMPLATES, null);
            if (limit != null) {
                long currentCount = formTemplateRepository.countByIsDeletedFalse();
                if (currentCount >= limit) {
                    throw new ForbiddenException("Form template limit reached (" + limit + "). Please upgrade your plan to add more templates.");
                }
            }
            // Track usage for plan reporting
            subscriptionFeatureService.incrementUsage(orgId, SubscriptionFeatureService.FEATURE_FORM_TEMPLATES, 1L);
        }

        User user = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create template
        FormTemplate template = FormTemplate.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(parseFormCategoryOrDefault(request.getCategory(), FromCategory.CUSTOM))
                .instructions(request.getInstructions())
                .requiresSignature(request.getRequiresSignature() != null ? request.getRequiresSignature() : true)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isSystemTemplate(request.getIsSystemTemplate() != null ? request.getIsSystemTemplate() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdByUser(user)
                .build();
        
        template.setIsDeleted(false);
        FormTemplate saved = formTemplateRepository.save(template);

        // Create version 1 (ACTIVE)
        FormTemplateVersion version = FormTemplateVersion.builder()
                .template(saved)
                .versionNumber(1L)
                .name(request.getName().trim())
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .requiresSignature(request.getRequiresSignature() != null ? request.getRequiresSignature() : true)
                .status(FormTemplateVersionStatus.ACTIVE)
                .createdByUser(user)
                .build();
        version.setIsDeleted(false);
        FormTemplateVersion savedVersion = formTemplateVersionRepository.save(version);

        // Create default section
        FormSection defaultSection = FormSection.builder()
                .templateVersion(savedVersion)
                .name("Default Section")
                .description("Default section for form fields")
                .sortOrder(0)
                .build();
        defaultSection.setIsDeleted(false);
        FormSection savedSection = formSectionRepository.save(defaultSection);

        // Create fields if provided
        if (request.getFields() != null && !request.getFields().isEmpty()) {
            List<FormField> fields = new ArrayList<>();
            int sortOrder = 0;
            for (var fieldRequest : request.getFields()) {
                FormField field = createFieldFromTemplateRequest(fieldRequest, savedVersion, savedSection, sortOrder++);
                fields.add(field);
            }
            formFieldRepository.saveAll(fields);
            
            // Create field options if provided
            for (int i = 0; i < fields.size(); i++) {
                FormField field = fields.get(i);
                var fieldRequest = request.getFields().get(i);
                if (fieldRequest.getOptions() != null && !fieldRequest.getOptions().trim().isEmpty()) {
                    createFieldOptions(field, fieldRequest.getOptions());
                }
            }
        }

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_template_created", saved.getId(), ipAddress, false);

        return toTemplateResponseWithVersions(saved);
    }

    @Transactional(readOnly = true)
    public List<FormFieldResponse> getTemplateFields(Long templateId) {
        Objects.requireNonNull(templateId, "Template ID is required");

        FormTemplate template = formTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));

        // Use active version as the default for template-level field queries
        FormTemplateVersion activeVersion = formTemplateVersionRepository
                .findByTemplateIdAndStatusAndIsDeletedFalse(template.getId(), FormTemplateVersionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Template has no active version"));

        List<FormField> fields = formFieldRepository.findByTemplateVersionIdWithOptions(activeVersion.getId());
        return fields.stream()
                .map(this::toFieldResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "forms", allEntries = true)
    public FormTemplateResponse updateTemplate(Long id, UpdateFormTemplateRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(id, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update form templates");

        FormTemplate template = formTemplateRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));

        if (template.getIsDeleted()) {
            throw new BadRequestException("Cannot update deleted template");
        }

        User user = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        applyTemplateMetadataUpdates(template, request);
        FormTemplate updated = formTemplateRepository.save(template);

        if (!isStructuralChange(request)) {
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_template_updated", updated.getId(), ipAddress, false);
            FormTemplate hydrated = formTemplateRepository.findByIdWithVersions(updated.getId()).orElse(updated);
            return toTemplateResponseWithVersions(hydrated);
        }

        FormTemplateVersion currentActiveVersion = formTemplateVersionRepository
                .findByTemplateIdAndStatusAndIsDeletedFalse(template.getId(), FormTemplateVersionStatus.ACTIVE)
                .orElse(null);

        Long maxVersionNumber = formTemplateVersionRepository.findMaxVersionNumberByTemplateId(template.getId());
        Long nextVersionNumber = (maxVersionNumber != null ? maxVersionNumber : 0L) + 1L;

        String effectiveInstructions = resolveEffectiveInstructions(request, currentActiveVersion, updated);
        Boolean effectiveRequiresSignature = resolveEffectiveRequiresSignature(request, currentActiveVersion, updated);

        FormTemplateVersion draftVersion = FormTemplateVersion.builder()
                .template(updated)
                .versionNumber(nextVersionNumber)
                .name(updated.getName())
                .description(updated.getDescription())
                .instructions(effectiveInstructions)
                .requiresSignature(effectiveRequiresSignature)
                .status(FormTemplateVersionStatus.DRAFT)
                .createdByUser(user)
                .build();
        draftVersion.setIsDeleted(false);
        FormTemplateVersion savedVersion = formTemplateVersionRepository.save(draftVersion);

        if (!request.isFieldPresent("fields")) {
            copyVersionStructure(currentActiveVersion, savedVersion);
        } else {
            replaceVersionFieldsFromRequest(savedVersion, request.getFields());
        }

        formTemplateVersionRepository.archiveActiveVersions(
                template.getId(),
                savedVersion.getId(),
                FormTemplateVersionStatus.ACTIVE,
                FormTemplateVersionStatus.ARCHIVED);

        savedVersion.setStatus(FormTemplateVersionStatus.ACTIVE);
        formTemplateVersionRepository.save(savedVersion);

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_template_version_created", savedVersion.getId(), ipAddress, false);

        FormTemplate hydrated = formTemplateRepository.findByIdWithVersions(updated.getId()).orElse(updated);
        return toTemplateResponseWithVersions(hydrated);
    }

    @Transactional
    @CacheEvict(value = "forms", allEntries = true)
    public void deleteTemplate(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete form templates");

        FormTemplate template = formTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));

        // Soft delete
        template.setIsDeleted(true);
        template.setIsActive(false);
        template.setDeletedAt(Instant.now());
        formTemplateRepository.save(template);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_template_deleted", id, ipAddress, false);
    }

    // ========== FIELD METHODS ==========

    @Transactional
    public FormFieldResponse createField(CreateFormFieldRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create form fields");

        if (request.getTemplateId() == null) {
            throw new BadRequestException("Template ID is required when creating a field standalone");
        }

        // Get active version of template
        FormTemplate template = formTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));
        
        FormTemplateVersion activeVersion = formTemplateVersionRepository
                .findByTemplateIdAndStatusAndIsDeletedFalse(template.getId(), FormTemplateVersionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("No active version found for template"));
        
        // Get or create default section
        List<FormSection> sections = formSectionRepository.findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(activeVersion.getId());
        FormSection section = sections.isEmpty() 
                ? createDefaultSection(activeVersion)
                : sections.get(0);

        FormField field = createFieldFromRequest(request, activeVersion, section);
        FormField saved = formFieldRepository.save(field);

        // Audit log
        recordAuditEvent(resolveActorUserId(requester), "form_field_created", saved.getId(), ipAddress, false);

        return toFieldResponse(saved);
    }

    @Transactional
    public FormFieldResponse createFieldForTemplate(Long templateId, CreateFormFieldRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Resolve active version and a default section
        FormTemplate template = formTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));

        FormTemplateVersion activeVersion = formTemplateVersionRepository
                .findByTemplateIdAndStatusAndIsDeletedFalse(template.getId(), FormTemplateVersionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Template has no active version"));

        List<FormSection> sections = formSectionRepository
                .findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(activeVersion.getId());
        if (sections.isEmpty()) {
            throw new BadRequestException("Active template version has no sections to attach fields to");
        }

        // Fallback: if template-level creation is requested but our DTO does not
        // support version/section targeting, delegate to existing createField logic
        // using the original request as-is. This preserves current behavior without
        // relying on non-existent DTO properties.
        return createField(request, requester, ipAddress);
    }

    @Transactional(readOnly = true)
    public FormFieldResponse getField(Long id) {
        Objects.requireNonNull(id, "Field ID is required");
        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form field not found"));
        if (Boolean.TRUE.equals(field.getIsDeleted())) {
            throw new ResourceNotFoundException("Form field not found");
        }
        return toFieldResponse(field);
    }

    @Transactional
    public FormFieldResponse updateField(Long id, UpdateFormFieldRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(id, "Field ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update form fields");

        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form field not found"));

        if (request.isFieldPresent("label")) {
            if (!StringUtils.hasText(request.getLabel())) {
                throw new BadRequestException("label cannot be cleared");
            }
            field.setFieldLabel(request.getLabel());
        }
        if (request.isFieldPresent("fieldType")) {
            if (!StringUtils.hasText(request.getFieldType())) {
                throw new BadRequestException("fieldType cannot be cleared");
            }
            field.setFieldType(parseFieldType(request.getFieldType()));
            if (field.getFieldType() == FieldType.HEADING || field.getFieldType() == FieldType.INFO_TEXT) {
                field.setIsRequired(false);
            }
        }
        if (request.isFieldPresent("placeholder")) {
            field.setPlaceholder(request.getPlaceholder());
        }
        if (request.isFieldPresent("helpText")) {
            field.setHelpText(request.getHelpText());
        }
        if (request.isFieldPresent("isRequired")) {
            if (field.getFieldType() != FieldType.HEADING && field.getFieldType() != FieldType.INFO_TEXT) {
                field.setIsRequired(request.getIsRequired());
            }
        }
        if (request.isFieldPresent("validation")) {
            field.setValidationRules(request.getValidation());
        }
        if (request.isFieldPresent("defaultValue")) {
            field.setDefaultValue(request.getDefaultValue());
        }
        PatchUpdates.apply(request, "sortOrder", request.getSortOrder(), field::setSortOrder);

        FormField updated = formFieldRepository.save(field);

        if (request.isFieldPresent("options")) {
            if (!StringUtils.hasText(request.getOptions())) {
                formFieldOptionRepository.deleteByFieldId(updated.getId());
            } else {
                formFieldOptionRepository.deleteByFieldId(updated.getId());
                createFieldOptions(updated, request.getOptions());
            }
        }

        recordAuditEvent(resolveActorUserId(requester), "form_field_updated", updated.getId(), ipAddress, false);

        return toFieldResponse(updated);
    }

    @Transactional
    public FormFieldResponse updateField(Long id, CreateFormFieldRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(id, "Field ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update form fields");

        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form field not found"));

        // Update fields
        if (StringUtils.hasText(request.getLabel())) {
            field.setFieldLabel(request.getLabel());
        }
        if (StringUtils.hasText(request.getFieldType())) {
            field.setFieldType(parseFieldType(request.getFieldType()));
            // Enforce required=false for heading/info_text
            if (field.getFieldType() == FieldType.HEADING || field.getFieldType() == FieldType.INFO_TEXT) {
                field.setIsRequired(false);
            }
        }
        if (request.getPlaceholder() != null) {
            field.setPlaceholder(request.getPlaceholder());
        }
        if (request.getHelpText() != null) {
            field.setHelpText(request.getHelpText());
        }
        if (request.getIsRequired() != null) {
            // Don't allow required=true for heading/info_text
            if (field.getFieldType() != FieldType.HEADING && field.getFieldType() != FieldType.INFO_TEXT) {
                field.setIsRequired(request.getIsRequired());
            }
        }
        if (request.getValidation() != null) {
            field.setValidationRules(request.getValidation());
        }
        if (request.getDefaultValue() != null) {
            field.setDefaultValue(request.getDefaultValue());
        }
        if (request.getSortOrder() != null) {
            field.setSortOrder(request.getSortOrder());
        }

        FormField updated = formFieldRepository.save(field);

        // Options are now handled separately via FormFieldOption entities
        if (request.getOptions() != null && !request.getOptions().trim().isEmpty()) {
            // Delete existing options and create new ones
            formFieldOptionRepository.deleteByFieldId(updated.getId());
            createFieldOptions(updated, request.getOptions());
        }

        // Audit log
        recordAuditEvent(resolveActorUserId(requester), "form_field_updated", updated.getId(), ipAddress, false);

        return toFieldResponse(updated);
    }

    @Transactional
    public void deleteField(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Field ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete form fields");

        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form field not found"));

        // Check if field is used in any assignment fields (snapshots)
        // Load assignment fields to check references
        List<FormAssignmentField> assignmentFields = field.getAssignmentFields();
        if (assignmentFields != null && !assignmentFields.isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete field that is used in form assignments. Field is referenced in historical data.");
        }

        // Soft delete field and its options
        field.setIsDeleted(true);
        field.setDeletedAt(Instant.now());
        formFieldRepository.save(field);

        // Soft delete field options
        List<FormFieldOption> options = formFieldOptionRepository.findByFieldIdAndIsDeletedFalseOrderBySortOrderAsc(id);
        for (FormFieldOption option : options) {
            option.setIsDeleted(true);
            option.setDeletedAt(Instant.now());
        }
        formFieldOptionRepository.saveAll(options);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_field_deleted", id, ipAddress, false);
    }

    @Transactional
    public void reorderFields(ReorderFormFieldsRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        List<Long> fieldIds = request.getFieldIds();
        if (fieldIds == null || fieldIds.isEmpty()) {
            throw new BadRequestException("Field IDs are required for reordering");
        }

        List<FormField> fields = formFieldRepository.findAllById(fieldIds);
        if (fields.size() != fieldIds.size()) {
            throw new BadRequestException("One or more field IDs are invalid");
        }

        // Assign sortOrder based on position in the list
        Map<Long, Integer> orderById = new HashMap<>();
        for (int i = 0; i < fieldIds.size(); i++) {
            orderById.put(fieldIds.get(i), i);
        }

        for (FormField field : fields) {
            Integer order = orderById.get(field.getId());
            if (order != null) {
                field.setSortOrder(order);
            }
        }

        formFieldRepository.saveAll(fields);

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_fields_reordered", null, ipAddress, false);
    }

    // ========== ASSIGNMENT METHODS ==========

    @Transactional(readOnly = true)
    public List<FormAssignmentResponse> getClientAssignments(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        clientReportAccessService.requireClientAccess(clientId, requester);

        FormAssignmentFilterRequest filter = new FormAssignmentFilterRequest();
        filter.setClientId(clientId);

        List<FormAssignment> assignments = formAssignmentRepository.findAll(
                buildAssignmentFilterSpecification(filter),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return assignments.stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormAssignmentResponse> getAssignmentsWithFilters(FormAssignmentFilterRequest filter,
            AuthPrincipal requester) {
        Objects.requireNonNull(filter, "Filter request is required");
        Objects.requireNonNull(requester, "Requester is required");
        if (filter.getClientId() != null) {
            clientReportAccessService.requireClientAccess(filter.getClientId(), requester);
        }

        List<FormAssignment> assignments = formAssignmentRepository.findAll(
                buildAssignmentFilterSpecification(filter),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return assignments.stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormAssignmentResponse getAssignment(Long id) {
        return getAssignment(id, null, null);
    }

    @Transactional(readOnly = true)
    public FormAssignmentResponse getAssignment(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Assignment ID is required");

        FormAssignment assignment = formAssignmentRepository.findByIdWithTemplateDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (requester != null) {
            if (assignment.getClient() != null) {
                clientReportAccessService.validateClientAccess(assignment.getClient(), requester);
            }
            Long clientId = assignment.getClient() != null ? assignment.getClient().getId() : null;
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                    "form_assignment_viewed", id, ipAddress, true, clientId);
        }

        return toAssignmentResponse(assignment);
    }

    private Specification<FormAssignment> buildAssignmentFilterSpecification(FormAssignmentFilterRequest filter) {
        return (root, query, cb) -> {
            // Avoid N+1 for template metadata while keeping collection loading lazy to
            // prevent multi-bag fetch exceptions.
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("templateVersion", JoinType.LEFT).fetch("template", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            Join<FormAssignment, FormTemplateVersion> versionJoin = null;
            if (filter.getTemplateId() != null || filter.getVersionStatus() != null) {
                versionJoin = root.join("templateVersion", JoinType.INNER);
            }

            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("client").get("id"), filter.getClientId()));
            }
            if (filter.getTemplateId() != null && versionJoin != null) {
                predicates.add(cb.equal(versionJoin.get("template").get("id"), filter.getTemplateId()));
            }
            if (filter.getVersionStatus() != null && versionJoin != null) {
                predicates.add(cb.equal(versionJoin.get("status"), filter.getVersionStatus()));
            }
            if (filter.getAssignmentStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getAssignmentStatus()));
            }
            if (filter.getAssignmentDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getAssignmentDateFrom()));
            }
            if (filter.getAssignmentDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getAssignmentDateTo()));
            }
            if (filter.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueDateFrom()));
            }
            if (filter.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filter.getDueDateTo()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public FormAssignmentResponse createAssignment(CreateFormAssignmentRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only therapists, supervisors, and admins can assign forms
        if (!permissionChecker.hasPermission(requester, "FORM_VIEW") && 
                !permissionChecker.hasPermission(requester, "FORM_FILL") &&
                !permissionChecker.hasPermission(requester, "FORM_TEMPLATE_MANAGE")) {
            throw new ForbiddenException("Insufficient permissions to assign forms");
        }

        // Get active version of template
        FormTemplate template = formTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));

        if (template.getIsDeleted() || !template.getIsActive()) {
            throw new BadRequestException("Cannot assign inactive or deleted template");
        }

        // Get active version
        FormTemplateVersion activeVersion = formTemplateVersionRepository
                .findByTemplateIdAndStatusAndIsDeletedFalse(template.getId(), FormTemplateVersionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("No active version found for template"));

        Client client = clientReportAccessService.requireClientAccess(request.getClientId(), requester);

        User assignedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create assignment with template version; inherit version/template instructions when assigner omits them
        String instructions = StringUtils.hasText(request.getInstructions())
                ? request.getInstructions().trim()
                : null;
        if (!StringUtils.hasText(instructions) && StringUtils.hasText(activeVersion.getInstructions())) {
            instructions = activeVersion.getInstructions();
        }
        if (!StringUtils.hasText(instructions) && StringUtils.hasText(template.getInstructions())) {
            instructions = template.getInstructions();
        }

        FormAssignment assignment = FormAssignment.builder()
                .templateVersion(activeVersion)
                .client(client)
                .assignedBy(assignedBy)
                .status(Status.ASSIGNED)
                .dueDate(request.getDueDate())
                .instructions(instructions)
                .remindersSent(0)
                .build();

        assignment.setIsDeleted(false);
        FormAssignment saved = formAssignmentRepository.save(assignment);

        // CRITICAL: Create assignment field snapshots for historical data integrity
        List<FormField> fields = formFieldRepository.findByTemplateVersionIdWithOptions(activeVersion.getId());
        List<FormAssignmentField> assignmentFields = new ArrayList<>();
        
        for (FormField field : fields) {
            // Get options as JSONB - convert FormFieldOption list to JSON array
            String optionsJson = "[]";
            if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                try {
                    // Convert options to JSON array using ObjectMapper for proper JSON formatting
                    List<Map<String, Object>> optionsList = field.getOptions().stream()
                            .filter(opt -> !Boolean.TRUE.equals(opt.getIsDeleted()))
                            .map(opt -> {
                                Map<String, Object> optionMap = new HashMap<>();
                                optionMap.put("label", opt.getLabel());
                                optionMap.put("value", opt.getValue());
                                if (opt.getScore() != null) {
                                    optionMap.put("score", opt.getScore());
                                }
                                optionMap.put("sortOrder", opt.getSortOrder());
                                return optionMap;
                            })
                            .collect(Collectors.toList());
                    optionsJson = objectMapper.writeValueAsString(optionsList);
                } catch (Exception e) {
                    log.error("Failed to convert field options to JSON for field {}: {}", field.getId(), e.getMessage());
                    optionsJson = "[]";
                }
            }
            
            FormAssignmentField assignmentField = FormAssignmentField.builder()
                    .assignment(saved)
                    .field(field)
                    .fieldLabel(field.getFieldLabel())
                    .fieldType(field.getFieldType() != null ? field.getFieldType().name() : null)
                    .options(optionsJson)
                    .sortOrder(field.getSortOrder())
                    .isRequired(field.getIsRequired())
                    .autoPopulate(field.getAutoPopulate())
                    .conditionalDisplay(field.getConditionalDisplay())
                    .build();
            assignmentField.setIsDeleted(false);
            assignmentFields.add(assignmentField);
        }
        
        formAssignmentFieldRepository.saveAll(assignmentFields);
        saved.setAssignmentFields(assignmentFields);

        // Trigger notification
        if (notificationService != null) {
            try {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("id", saved.getId());
                eventData.put("clientId", client.getId());
                NotificationPayloadFactory.putClientIdentity(eventData, client);
                eventData.put("therapistId", client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null);
                eventData.put("therapistName", client.getAssignedTherapist() != null
                        ? client.getAssignedTherapist().getFullName()
                        : "Unassigned");
                eventData.put("templateId", template.getId());
                eventData.put("templateName", template.getName());
                eventData.put("assignedById", currentUserService.requireCurrentUser(requester).getId());
                notificationService.processEvent(NotificationEventCatalog.FORM_ASSIGNED, eventData);
                if (isClinicalTemplate(template)) {
                    notificationService.processEvent(NotificationEventCatalog.CLINICAL_FORM_ASSIGNED, eventData);
                }
            } catch (Exception e) {
                log.error("Failed to trigger form_assigned notification", e);
            }
        }

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_assignment_created", saved.getId(), ipAddress, true, client.getId());

        return toAssignmentResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "forms", allEntries = true)
    public List<FormAssignmentResponse> bulkAssignForms(BulkAssignFormRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only therapists, supervisors, and admins can assign forms
        if (!permissionChecker.hasPermission(requester, "FORM_VIEW") && 
                !permissionChecker.hasPermission(requester, "FORM_FILL") &&
                !permissionChecker.hasPermission(requester, "FORM_TEMPLATE_MANAGE")) {
            throw new ForbiddenException("Insufficient permissions to assign forms");
        }

        FormTemplate template = formTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Form template not found"));

        if (template.getIsDeleted() || !Boolean.TRUE.equals(template.getIsActive())) {
            throw new BadRequestException("Cannot assign inactive or deleted template");
        }

        // Get active version
        FormTemplateVersion activeVersion = formTemplateVersionRepository
                .findByTemplateIdAndStatusAndIsDeletedFalse(template.getId(), FormTemplateVersionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Template has no active version"));

        User assignedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<FormAssignmentResponse> responses = new ArrayList<>();
        List<Long> failedClientIds = new ArrayList<>();

        for (Long clientId : request.getClientIds()) {
            try {
                Client client = clientRepository.findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                if (client.getIsDeleted()) {
                    failedClientIds.add(clientId);
                    continue;
                }

                // Create assignment
                CreateFormAssignmentRequest assignRequest = new CreateFormAssignmentRequest();
                assignRequest.setTemplateId(request.getTemplateId());
                assignRequest.setClientId(clientId);
                assignRequest.setDueDate(request.getDueDate());
                assignRequest.setInstructions(request.getInstructions());

                FormAssignmentResponse response = createAssignment(assignRequest, requester, ipAddress);
                responses.add(response);
            } catch (Exception e) {
                log.error("Failed to assign form to client {}: {}", clientId, e.getMessage());
                failedClientIds.add(clientId);
            }
        }

        if (!failedClientIds.isEmpty()) {
            log.warn("Failed to assign form to {} client(s): {}", failedClientIds.size(), failedClientIds);
        }

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_bulk_assigned", request.getTemplateId(), ipAddress, false);
        return responses;
    }

    @Transactional
    @CacheEvict(value = "forms", allEntries = true)
    public List<FormAssignmentResponse> assignMultipleFormsToClient(
            AssignMultipleFormsToClientRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientReportAccessService.requireClientAccess(request.getClientId(), requester);

        if (Boolean.TRUE.equals(client.getIsDeleted())) {
            throw new BadRequestException("Cannot assign forms to a deleted client");
        }

        Set<Long> uniqueTemplateIds = new LinkedHashSet<>(request.getTemplateIds());
        List<FormAssignmentResponse> responses = new ArrayList<>();
        List<Long> failedTemplateIds = new ArrayList<>();

        for (Long templateId : uniqueTemplateIds) {
            try {
                CreateFormAssignmentRequest assignRequest = new CreateFormAssignmentRequest();
                assignRequest.setTemplateId(templateId);
                assignRequest.setClientId(request.getClientId());
                assignRequest.setDueDate(request.getDueDate());
                assignRequest.setInstructions(request.getInstructions());

                responses.add(createAssignment(assignRequest, requester, ipAddress));
            } catch (Exception e) {
                log.error("Failed to assign template {} to client {}: {}", templateId, request.getClientId(), e.getMessage());
                failedTemplateIds.add(templateId);
            }
        }

        if (!failedTemplateIds.isEmpty()) {
            log.warn("Failed to assign {} template(s) to client {}: {}", failedTemplateIds.size(), request.getClientId(), failedTemplateIds);
        }

        recordAuditEvent(
                currentUserService.requireCurrentUser(requester).getId(),
                "form_multi_template_assigned",
                request.getClientId(),
                ipAddress,
                false,
                request.getClientId()
        );
        return responses;
    }

    @Transactional
    public FormAssignmentResponse updateAssignment(Long id, UpdateFormAssignmentRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Assignment ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        if (!permissionChecker.hasPermission(requester, "FORM_VIEW") && 
                !permissionChecker.hasPermission(requester, "FORM_FILL") &&
                !permissionChecker.hasPermission(requester, "FORM_TEMPLATE_MANAGE")) {
            throw new ForbiddenException("Insufficient permissions to update form assignments");
        }

        FormAssignment assignment = formAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        if (request.isFieldPresent("dueDate")) {
            assignment.setDueDate(request.getDueDate());
        }
        if (request.isFieldPresent("instructions")) {
            assignment.setInstructions(request.getInstructions());
        }
        if (request.isFieldPresent("status")) {
            if (!StringUtils.hasText(request.getStatus())) {
                throw new BadRequestException("status cannot be cleared");
            }
            Status targetStatus = parseAssignmentStatus(request.getStatus());
            if (targetStatus == Status.SUBMITTED || targetStatus == Status.COMPLETED || targetStatus == Status.REVIEWED) {
                FormSubmissionValidator.validateSubmission(assignment,
                        formAssignmentFieldRepository.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(id),
                        formResponseRepository.findByAssignmentId(id), formSignatureRepository.findByAssignmentId(id));
            }
            assignment.setStatus(targetStatus);
        }

        FormAssignment saved = formAssignmentRepository.save(assignment);

        Long clientId = assignment.getClient() != null ? assignment.getClient().getId() : null;
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_assignment_updated", saved.getId(), ipAddress, true, clientId);

        return toAssignmentResponse(saved);
    }

    @Transactional
    public FormAssignmentResponse updateAssignmentStatus(Long id, String status,
            AuthPrincipal requester, String ipAddress) {
        if (!StringUtils.hasText(status)) {
            throw new BadRequestException("Status is required");
        }

        UpdateFormAssignmentRequest request = new UpdateFormAssignmentRequest();
        request.setStatus(status);
        request.markFieldPresent("status");
        return updateAssignment(id, request, requester, ipAddress);
    }

    @Transactional
    public void deleteAssignment(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only therapists, supervisors, and admins can delete assignments
        if (!permissionChecker.hasPermission(requester, "FORM_VIEW") && 
                !permissionChecker.hasPermission(requester, "FORM_FILL") &&
                !permissionChecker.hasPermission(requester, "FORM_TEMPLATE_MANAGE")) {
            throw new ForbiddenException("Insufficient permissions to delete form assignments");
        }

        FormAssignment assignment = formAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        Long clientId = assignment.getClient() != null ? assignment.getClient().getId() : null;

        // Delete related data
        formResponseRepository.deleteByAssignmentId(id);
        formSignatureRepository.deleteByAssignmentId(id);
        formAssignmentRepository.delete(assignment);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_assignment_deleted", id, ipAddress, true, clientId);
    }

    // ========== RESPONSE METHODS ==========

    @Transactional
    public List<FormResponseDto> submitResponses(Long assignmentId, SubmitFormResponseRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        // Get assignment fields (snapshots)
        List<FormAssignmentField> assignmentFields = formAssignmentFieldRepository
                .findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId);

        // Delete existing responses
        formResponseRepository.deleteByAssignmentId(assignmentId);

        // Create new responses using assignment fields (snapshots)
        List<FormResponse> responses = new ArrayList<>();
        for (Map.Entry<Long, String> entry : request.getResponses().entrySet()) {
            // entry.getKey() is now assignmentFieldId, not fieldId
            FormAssignmentField assignmentField = assignmentFields.stream()
                    .filter(af -> af.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Form assignment field not found: " + entry.getKey()));

            FormResponse response = FormResponse.builder()
                    .assignment(assignment)
                    .assignmentField(assignmentField)
                    .responseValue(entry.getValue())
                    .build();
            response.setIsDeleted(false);
            responses.add(response);
        }

        List<FormResponse> saved = formResponseRepository.saveAll(responses);

        // Update assignment status
        assignment.setStatus(Status.IN_PROGRESS);
        formAssignmentRepository.save(assignment);

        // Audit log
        Long clientId = assignment.getClient() != null ? assignment.getClient().getId() : null;
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_responses_submitted", assignmentId, ipAddress, true, clientId);

        return saved.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormResponseDto> getAssignmentResponses(Long assignmentId) {
        return getAssignmentResponses(assignmentId, null, null);
    }

    @Transactional(readOnly = true)
    public List<FormResponseDto> getAssignmentResponses(Long assignmentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        List<FormResponse> responses = formResponseRepository.findByAssignmentId(assignmentId);

        if (requester != null) {
            FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));
            if (assignment.getClient() != null) {
                clientReportAccessService.validateClientAccess(assignment.getClient(), requester);
            }
            Long clientId = assignment.getClient() != null ? assignment.getClient().getId() : null;
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                    "form_responses_viewed", assignmentId, ipAddress, true, clientId);
        }

        return responses.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public FormResponseDto updateResponse(Long responseId, UpdateFormResponseRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(responseId, "Response ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        FormResponse response = formResponseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("Form response not found"));

        if (request.isFieldPresent("value")) {
            response.setResponseValue(request.getValue());
        }
        FormResponse saved = formResponseRepository.save(response);

        Long assignmentId = saved.getAssignment() != null ? saved.getAssignment().getId() : null;
        Long clientId = saved.getAssignment() != null && saved.getAssignment().getClient() != null
                ? saved.getAssignment().getClient().getId()
                : null;

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_response_updated", assignmentId, ipAddress, true, clientId);

        return toResponseDto(saved);
    }

    // ========== SIGNATURE METHODS ==========

    @Transactional
    public FormSignatureResponse submitSignature(Long assignmentId, SubmitFormSignatureRequest request,
            AuthPrincipal requester, String ipAddress, String userAgent) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");
        Objects.requireNonNull(request, "Request is required");

        FormAssignment assignment = formAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        FormTemplateVersion assignmentVersion = assignment.getTemplateVersion();
        FormTemplate templateForSignature = assignmentVersion != null ? assignmentVersion.getTemplate() : null;
        Boolean requiresSignature = assignmentVersion != null ? assignmentVersion.getRequiresSignature() : null;
        if (requiresSignature == null && templateForSignature != null) {
            requiresSignature = templateForSignature.getRequiresSignature();
        }
        if (!Boolean.TRUE.equals(requiresSignature)) {
            throw new BadRequestException("This form does not require a signature");
        }

        FormSubmissionValidator.requireAcceptedTerms(request.getAgreedToTerms());
        FormSubmissionValidator.validateAnswers(
                formAssignmentFieldRepository.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId),
                formResponseRepository.findByAssignmentId(assignmentId), StringUtils.hasText(request.getSignatureData()));

        FormSignature signature = FormSignature.builder()
                .assignment(assignment)
                .signatureData(request.getSignatureData())
                .signerName(request.getSignerName())
                .signerRole(request.getSignerRole())
                .signatureType(SignatureType.DRAWN)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .signedAt(Instant.now())
                .agreedToTerms(request.getAgreedToTerms() != null ? request.getAgreedToTerms() : false)
                .build();

        FormSignature saved = formSignatureRepository.save(signature);

        // Mark assignment as completed
        assignment.setStatus(Status.COMPLETED);
        assignment.setCompletedAt(Instant.now());
        assignment.setSubmittedAt(Instant.now());
        formAssignmentRepository.save(assignment);

        // Trigger notification
        if (notificationService != null) {
            try {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("id", assignment.getId());
                eventData.put("clientId", assignment.getClient() != null ? assignment.getClient().getId() : null);
                NotificationPayloadFactory.putClientIdentity(eventData, assignment.getClient());
                eventData.put("therapistId", assignment.getClient() != null && assignment.getClient().getAssignedTherapist() != null
                        ? assignment.getClient().getAssignedTherapist().getId()
                        : null);
                eventData.put("therapistName", assignment.getClient() != null && assignment.getClient().getAssignedTherapist() != null
                        ? assignment.getClient().getAssignedTherapist().getFullName()
                        : "Unassigned");
                FormTemplate templateForEvent = assignment.getTemplateVersion() != null 
                        ? assignment.getTemplateVersion().getTemplate() 
                        : null;
                eventData.put("templateId", templateForEvent != null ? templateForEvent.getId() : null);
                eventData.put("templateName", templateForEvent != null ? templateForEvent.getName() : null);
                notificationService.processEvent(NotificationEventCatalog.FORM_COMPLETED, eventData);
                if (isClinicalTemplate(templateForEvent)) {
                    notificationService.processEvent(NotificationEventCatalog.CLINICAL_FORM_COMPLETED, eventData);
                }
            } catch (Exception e) {
                log.error("Failed to trigger form_completed notification", e);
            }
        }

        // Audit log
        Long clientId = assignment.getClient() != null ? assignment.getClient().getId() : null;
        recordAuditEvent(requester != null ? currentUserService.requireCurrentUser(requester).getId() : null, "form_signature_submitted", assignmentId,
                ipAddress, true, clientId);

        return toSignatureResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FormSignatureResponse> getSignatures(Long assignmentId) {
        Objects.requireNonNull(assignmentId, "Assignment ID is required");

        List<FormSignature> signatures = formSignatureRepository.findByAssignmentId(assignmentId);
        return signatures.stream()
                .map(this::toSignatureResponse)
                .collect(Collectors.toList());
    }

    // ========== REVIEW METHODS ==========

    @Transactional
    public FormAssignmentResponse reviewAssignment(Long id, ReviewFormAssignmentRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Assignment ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only therapists, supervisors, and admins can review forms
        if (!permissionChecker.hasPermission(requester, "FORM_VIEW") && 
                !permissionChecker.hasPermission(requester, "FORM_FILL") &&
                !permissionChecker.hasPermission(requester, "FORM_TEMPLATE_MANAGE")) {
            throw new ForbiddenException("Insufficient permissions to review forms");
        }

        FormAssignment assignment = formAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form assignment not found"));

        User reviewedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        assignment.setReviewedAt(Instant.now());
        assignment.setReviewedBy(reviewedBy);
        assignment.setReviewNotes(request.getReviewNotes());

        FormAssignment updated = formAssignmentRepository.save(assignment);

        // Audit log
        Long clientId = assignment.getClient() != null ? assignment.getClient().getId() : null;
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_assignment_reviewed", id, ipAddress, true, clientId);

        return toAssignmentResponse(updated);
    }

    // ========== VERSION MANAGEMENT METHODS ==========

    @Transactional(readOnly = true)
    public List<FormTemplateVersionResponse> getTemplateVersions(Long templateId) {
        Objects.requireNonNull(templateId, "Template ID is required");
        
        List<FormTemplateVersion> versions = formTemplateVersionRepository
                .findByTemplateIdAndIsDeletedFalseOrderByVersionNumberDesc(templateId);
        
        return versions.stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormTemplateVersionResponse getTemplateVersion(Long templateId, Long versionNumber) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(versionNumber, "Version number is required");
        
        FormTemplateVersion version = formTemplateVersionRepository
                .findByTemplateIdAndVersionNumberAndIsDeletedFalse(templateId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template version not found: templateId=" + templateId + ", version=" + versionNumber));
        
        // Load with sections and fields
        FormTemplateVersion versionWithDetails = formTemplateVersionRepository
                .findByIdWithSectionsAndFields(version.getId())
                .orElse(version);
        
        return toVersionResponse(versionWithDetails);
    }

    @Transactional
    @CacheEvict(value = "forms", allEntries = true)
    public FormTemplateVersionResponse activateVersion(Long templateId, Long versionNumber, 
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(versionNumber, "Version number is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can activate template versions");

        // Archive current active version
        FormTemplateVersion currentActive = formTemplateVersionRepository
                .findByTemplateIdAndStatusAndIsDeletedFalse(templateId, FormTemplateVersionStatus.ACTIVE)
                .orElse(null);
        
        if (currentActive != null) {
            currentActive.setStatus(FormTemplateVersionStatus.ARCHIVED);
            formTemplateVersionRepository.save(currentActive);
        }

        // Activate requested version
        FormTemplateVersion version = formTemplateVersionRepository
                .findByTemplateIdAndVersionNumberAndIsDeletedFalse(templateId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template version not found: templateId=" + templateId + ", version=" + versionNumber));

        version.setStatus(FormTemplateVersionStatus.ACTIVE);
        FormTemplateVersion saved = formTemplateVersionRepository.save(version);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_template_version_activated", saved.getId(), ipAddress, false);

        return toVersionResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "forms", allEntries = true)
    public FormTemplateVersionResponse archiveVersion(Long templateId, Long versionNumber,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(versionNumber, "Version number is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can archive template versions");

        FormTemplateVersion version = formTemplateVersionRepository
                .findByTemplateIdAndVersionNumberAndIsDeletedFalse(templateId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template version not found: templateId=" + templateId + ", version=" + versionNumber));

        if (version.getStatus() == FormTemplateVersionStatus.ACTIVE) {
            throw new BadRequestException("Cannot archive active version. Please activate another version first.");
        }

        version.setStatus(FormTemplateVersionStatus.ARCHIVED);
        FormTemplateVersion saved = formTemplateVersionRepository.save(version);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_template_version_archived", saved.getId(), ipAddress, false);

        return toVersionResponse(saved);
    }

    // ========== SECTION MANAGEMENT METHODS ==========

    @Transactional
    public FormSectionResponse createSection(Long templateVersionId, String name, String description,
            Integer sortOrder, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(templateVersionId, "Template version ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create sections");

        FormTemplateVersion version = formTemplateVersionRepository.findById(templateVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Template version not found"));

        FormSection section = FormSection.builder()
                .templateVersion(version)
                .name(name != null ? name.trim() : "New Section")
                .description(description)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();
        section.setIsDeleted(false);
        FormSection saved = formSectionRepository.save(section);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_section_created", saved.getId(), ipAddress, false);

        return toSectionResponse(saved);
    }

    @Transactional
    public FormSectionResponse updateSection(Long sectionId, UpdateFormSectionRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(sectionId, "Section ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update sections");

        FormSection section = formSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (request.isFieldPresent("name")) {
            if (!StringUtils.hasText(request.getName())) {
                throw new BadRequestException("name cannot be cleared");
            }
            section.setName(request.getName().trim());
        }
        if (request.isFieldPresent("description")) {
            section.setDescription(request.getDescription());
        }
        PatchUpdates.apply(request, "sortOrder", request.getSortOrder(), section::setSortOrder);

        FormSection updated = formSectionRepository.save(section);

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_section_updated", updated.getId(), ipAddress, false);

        return toSectionResponse(updated);
    }

    @Transactional
    public void deleteSection(Long sectionId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(sectionId, "Section ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete sections");

        FormSection section = formSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        // Check if section has fields
        if (section.getFields() != null && !section.getFields().isEmpty()) {
            throw new BadRequestException("Cannot delete section with fields. Please remove or move fields first.");
        }

        // Soft delete
        section.setIsDeleted(true);
        section.setDeletedAt(Instant.now());
        formSectionRepository.save(section);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "form_section_deleted", sectionId, ipAddress, false);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private boolean isStructuralChange(UpdateFormTemplateRequest request) {
        return request.isFieldPresent("fields")
                || request.isFieldPresent("instructions")
                || request.isFieldPresent("requiresSignature");
    }

    private void applyTemplateMetadataUpdates(FormTemplate template, UpdateFormTemplateRequest request) {
        if (request.isFieldPresent("name")) {
            if (StringUtils.hasText(request.getName())) {
                template.setName(request.getName().trim());
            } else {
                throw new BadRequestException("name cannot be cleared");
            }
        }
        if (request.isFieldPresent("description")) {
            template.setDescription(request.getDescription());
        }
        if (request.isFieldPresent("category")) {
            template.setCategory(StringUtils.hasText(request.getCategory())
                    ? parseFormCategoryOrDefault(request.getCategory(), template.getCategory())
                    : null);
        }
        PatchUpdates.apply(request, "isActive", request.getIsActive(), template::setIsActive);
        PatchUpdates.apply(request, "isSystemTemplate", request.getIsSystemTemplate(), template::setIsSystemTemplate);
        PatchUpdates.apply(request, "sortOrder", request.getSortOrder(), template::setSortOrder);
        PatchUpdates.apply(request, "requiresSignature", request.getRequiresSignature(), template::setRequiresSignature);
    }

    private String resolveEffectiveInstructions(UpdateFormTemplateRequest request,
                                                FormTemplateVersion previousVersion,
                                                FormTemplate template) {
        if (request.isFieldPresent("instructions")) {
            return request.getInstructions();
        }
        if (previousVersion != null && previousVersion.getInstructions() != null) {
            return previousVersion.getInstructions();
        }
        return template.getInstructions();
    }

    /**
     * Prefer assignment-specific instructions; otherwise fall back to the assigned
     * template version (then template) so staff/portal still see version text.
     */
    private String resolveAssignmentInstructions(FormAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        if (StringUtils.hasText(assignment.getInstructions())) {
            return assignment.getInstructions();
        }
        FormTemplateVersion version = assignment.getTemplateVersion();
        if (version != null && StringUtils.hasText(version.getInstructions())) {
            return version.getInstructions();
        }
        FormTemplate template = version != null ? version.getTemplate() : null;
        return template != null ? template.getInstructions() : null;
    }

    private Boolean resolveEffectiveRequiresSignature(UpdateFormTemplateRequest request,
                                                      FormTemplateVersion previousVersion,
                                                      FormTemplate template) {
        if (request.isFieldPresent("requiresSignature")) {
            return request.getRequiresSignature();
        }
        if (previousVersion != null && previousVersion.getRequiresSignature() != null) {
            return previousVersion.getRequiresSignature();
        }
        return template.getRequiresSignature();
    }

    private void copyVersionStructure(FormTemplateVersion previousVersion, FormTemplateVersion targetVersion) {
        if (previousVersion == null) {
            createDefaultSection(targetVersion);
            return;
        }

        List<FormSection> oldSections = formSectionRepository
                .findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(previousVersion.getId());
        if (oldSections.isEmpty()) {
            createDefaultSection(targetVersion);
            return;
        }

        for (FormSection oldSection : oldSections) {
            FormSection newSection = FormSection.builder()
                    .templateVersion(targetVersion)
                    .name(oldSection.getName())
                    .description(oldSection.getDescription())
                    .sortOrder(oldSection.getSortOrder())
                    .build();
            newSection.setIsDeleted(false);
            FormSection savedSection = formSectionRepository.save(newSection);

            List<FormField> oldFields = formFieldRepository
                    .findBySectionIdAndIsDeletedFalseOrderBySortOrderAsc(oldSection.getId());

            for (FormField oldField : oldFields) {
                FormField newField = FormField.builder()
                        .templateVersion(targetVersion)
                        .section(savedSection)
                        .fieldName(oldField.getFieldName())
                        .fieldLabel(oldField.getFieldLabel())
                        .fieldType(oldField.getFieldType())
                        .placeholder(oldField.getPlaceholder())
                        .helpText(oldField.getHelpText())
                        .isRequired(oldField.getIsRequired())
                        .validationRules(oldField.getValidationRules())
                        .defaultValue(oldField.getDefaultValue())
                        .isRepeatable(oldField.getIsRepeatable())
                        .scoringFormula(oldField.getScoringFormula())
                        .maxScore(oldField.getMaxScore())
                        .autoPopulate(oldField.getAutoPopulate())
                        .conditionalDisplay(oldField.getConditionalDisplay())
                        .sortOrder(oldField.getSortOrder())
                        .build();
                newField.setIsDeleted(false);
                FormField savedField = formFieldRepository.save(newField);

                List<FormFieldOption> oldOptions = formFieldOptionRepository
                        .findByFieldIdAndIsDeletedFalseOrderBySortOrderAsc(oldField.getId());
                for (FormFieldOption oldOption : oldOptions) {
                    FormFieldOption newOption = FormFieldOption.builder()
                            .field(savedField)
                            .label(oldOption.getLabel())
                            .value(oldOption.getValue())
                            .score(oldOption.getScore())
                            .sortOrder(oldOption.getSortOrder())
                            .build();
                    newOption.setIsDeleted(false);
                    formFieldOptionRepository.save(newOption);
                }
            }
        }
    }

    private void replaceVersionFieldsFromRequest(FormTemplateVersion targetVersion,
                                                 List<CreateFormFieldInTemplateRequest> fieldRequests) {
        if (fieldRequests == null || fieldRequests.isEmpty()) {
            return;
        }

        FormSection section = createDefaultSection(targetVersion);
        List<FormField> fields = new ArrayList<>();
        int sortOrder = 0;
        for (CreateFormFieldInTemplateRequest fieldRequest : fieldRequests) {
            FormField field = createFieldFromTemplateRequest(fieldRequest, targetVersion, section, sortOrder++);
            fields.add(field);
        }
        formFieldRepository.saveAll(fields);

        for (int i = 0; i < fields.size(); i++) {
            FormField field = fields.get(i);
            CreateFormFieldInTemplateRequest fieldRequest = fieldRequests.get(i);
            if (fieldRequest.getOptions() != null && !fieldRequest.getOptions().trim().isEmpty()) {
                createFieldOptions(field, fieldRequest.getOptions());
            }
        }
    }

    private FormField createFieldFromRequest(CreateFormFieldRequest request, FormTemplateVersion templateVersion, FormSection section) {
        FormField field = FormField.builder()
                .templateVersion(templateVersion)
                .section(section)
                .fieldName(generateFieldName(request.getLabel()))
                .fieldType(parseFieldType(request.getFieldType()))
                .fieldLabel(request.getLabel())
                .placeholder(request.getPlaceholder())
                .helpText(request.getHelpText())
                .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                .validationRules(normalizeJsonValue(request.getValidation()))
                .defaultValue(request.getDefaultValue())
                .autoPopulate(request.getAutoPopulate())
                .conditionalDisplay(request.getConditionalDisplay())
                .isRepeatable(false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        // Enforce required=false for heading/info_text
        if (field.getFieldType() == FieldType.HEADING || field.getFieldType() == FieldType.INFO_TEXT) {
            field.setIsRequired(false);
        }

        field.setIsDeleted(false);
        return field;
    }

    private FormField createFieldFromTemplateRequest(
            com.smart.therapy.flow.document.dto.CreateFormFieldInTemplateRequest request, 
            FormTemplateVersion templateVersion, 
            FormSection section,
            int sortOrder) {
        FormField field = FormField.builder()
                .templateVersion(templateVersion)
                .section(section)
                .fieldName(generateFieldName(request.getLabel()))
                .fieldType(parseFieldType(request.getFieldType()))
                .fieldLabel(request.getLabel())
                .placeholder(request.getPlaceholder())
                .helpText(request.getHelpText())
                .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                .validationRules(normalizeJsonValue(request.getValidation()))
                .defaultValue(request.getDefaultValue())
                .autoPopulate(request.getAutoPopulate())
                .conditionalDisplay(request.getConditionalDisplay())
                .isRepeatable(false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : sortOrder)
                .build();

        // Enforce required=false for heading/info_text
        if (field.getFieldType() == FieldType.HEADING || field.getFieldType() == FieldType.INFO_TEXT) {
            field.setIsRequired(false);
        }

        field.setIsDeleted(false);
        return field;
    }

    private FieldType parseFieldType(String rawFieldType) {
        if (!StringUtils.hasText(rawFieldType)) {
            throw new BadRequestException("Field type is required");
        }

        String normalized = normalizeToken(rawFieldType);
        return switch (normalized) {
            case "heading", "heading_read_only", "heading_readonly" -> FieldType.HEADING;
            case "info_text", "info", "info_text_read_only", "information_text", "information" -> FieldType.INFO_TEXT;
            case "checkbox_group" -> FieldType.MULTI_SELECT;
            default -> {
                try {
                    yield FieldType.valueOf(normalized.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException(
                            "Invalid field type: " + rawFieldType
                                    + ". Allowed values: text, textarea, number, date, select, multi_select, checkbox, signature, radio, file, heading, info_text.",
                            ex);
                }
            }
        };
    }

    private boolean isClinicalTemplate(FormTemplate template) {
        if (template == null) {
            return false;
        }
        String name = template.getName() != null ? template.getName().toLowerCase(Locale.ROOT) : "";
        String description = template.getDescription() != null ? template.getDescription().toLowerCase(Locale.ROOT) : "";
        return name.contains("clinical")
                || description.contains("clinical")
                || name.contains("assessment")
                || description.contains("assessment")
                || template.getCategory() == FromCategory.SAFETY;
    }

    private FromCategory parseFormCategoryOrDefault(String rawCategory, FromCategory defaultValue) {
        if (!StringUtils.hasText(rawCategory)) {
            return defaultValue;
        }

        String normalizedInput = normalizeCategoryToken(rawCategory);
        return Arrays.stream(FromCategory.values())
                .filter(category -> normalizeCategoryToken(category.name()).equals(normalizedInput))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid category value: " + rawCategory
                        + ". Allowed values: consent, intake, release, agreement, safety, discharge, custom."));
    }

    private String normalizeCategoryToken(String value) {
        return normalizeToken(value).replace("_", "");
    }

    private String normalizeToken(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String normalizeJsonValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String raw = value.trim();
        if (!raw.startsWith("{") && !raw.startsWith("[")) {
            throw new BadRequestException("Validation must be a valid JSON object or array");
        }

        String candidate = raw;
        try {
            objectMapper.readTree(candidate);
            return candidate;
        } catch (JsonProcessingException ignored) {
            // Common client mistake: using single quotes for JSON.
            candidate = candidate.replace('\'', '"');
            try {
                objectMapper.readTree(candidate);
                return candidate;
            } catch (JsonProcessingException ex) {
                throw new BadRequestException("Validation must be valid JSON. Example: {\"minLength\":2,\"maxLength\":100}");
            }
        }
    }

    private Long resolveActorUserId(AuthPrincipal requester) {
        return currentUserService.getCurrentUser(requester)
                .map(User::getId)
                .orElse(null);
    }
    
    private String generateFieldName(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "field_" + System.currentTimeMillis();
        }
        // Convert label to field name: "Full Name" -> "full_name"
        return label.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
    
    private FormSection createDefaultSection(FormTemplateVersion version) {
        FormSection section = FormSection.builder()
                .templateVersion(version)
                .name("Default Section")
                .description("Default section for form fields")
                .sortOrder(0)
                .build();
        section.setIsDeleted(false);
        return formSectionRepository.save(section);
    }
    
    private void createFieldOptions(FormField field, String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return;
        }
        
        try {
            // Parse JSON array
            String trimmed = optionsJson.trim();
            if (!trimmed.startsWith("[")) {
                // Convert comma-separated to JSON array
                String[] parts = trimmed.split(",");
                List<FormFieldOption> optionList = new ArrayList<>();
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim();
                    if (!part.isEmpty()) {
                        FormFieldOption option = FormFieldOption.builder()
                                .field(field)
                                .label(part)
                                .value(part.toLowerCase().replaceAll("[^a-z0-9]+", "_"))
                                .sortOrder(i)
                                .build();
                        option.setIsDeleted(false);
                        optionList.add(option);
                    }
                }
                formFieldOptionRepository.saveAll(optionList);
            } else {
                // Parse as JSON array (simplified - in production use proper JSON parser)
                // For now, handle simple JSON arrays like ["Option1", "Option2"]
                String content = trimmed.substring(1, trimmed.length() - 1);
                String[] parts = content.split(",");
                List<FormFieldOption> optionList = new ArrayList<>();
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim().replaceAll("^\"|\"$", "");
                    if (!part.isEmpty()) {
                        FormFieldOption option = FormFieldOption.builder()
                                .field(field)
                                .label(part)
                                .value(part.toLowerCase().replaceAll("[^a-z0-9]+", "_"))
                                .sortOrder(i)
                                .build();
                        option.setIsDeleted(false);
                        optionList.add(option);
                    }
                }
                formFieldOptionRepository.saveAll(optionList);
            }
        } catch (Exception e) {
            log.error("Failed to create field options for field {}: {}", field.getId(), e.getMessage());
        }
    }

    private String convertOptionsToJson(String options) {
        if (options == null || options.trim().isEmpty()) {
            return null;
        }

        String trimmed = options.trim();
        // If already JSON array format, return as-is
        if (trimmed.startsWith("[")) {
            return trimmed;
        }

        // Convert comma-separated to JSON array
        String[] parts = trimmed.split(",");
        List<String> optionsList = Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Simple JSON array conversion
        return "[" + optionsList.stream()
                .map(opt -> "\"" + opt.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private FormTemplateResponse toTemplateResponse(FormTemplate template) {
        FormTemplateResponse.FormTemplateResponseBuilder builder = FormTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory() != null ? template.getCategory().name() : null)
                .instructions(template.getInstructions())
                .requiresSignature(template.getRequiresSignature())
                .isActive(template.getIsActive())
                .isDeleted(template.getIsDeleted())
                .isSystemTemplate(template.getIsSystemTemplate())
                .sortOrder(template.getSortOrder())
                .createdById(template.getCreatedByUser() != null ? template.getCreatedByUser().getId() : null)
                .createdByName(template.getCreatedByUser() != null ? template.getCreatedByUser().getFullName() : null)
                .deletedAt(template.getDeletedAt())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt());
        
        // Add versions if loaded
        if (template.getVersions() != null && !template.getVersions().isEmpty()) {
            List<FormTemplateVersionResponse> versions = template.getVersions().stream()
                    .map(this::toVersionResponse)
                    .collect(Collectors.toList());
            builder.versions(versions);
            
            // Set active version
            template.getVersions().stream()
                    .filter(v -> v.getStatus() == FormTemplateVersionStatus.ACTIVE && !v.getIsDeleted())
                    .findFirst()
                    .ifPresent(activeVersion -> builder.activeVersion(toVersionResponse(activeVersion)));
        }
        
        return builder.build();
    }

    private FormTemplateResponse toTemplateResponseWithVersions(FormTemplate template) {
        return toTemplateResponse(template);
    }
    
    private FormTemplateVersionResponse toVersionResponse(FormTemplateVersion version) {
        FormTemplateVersionResponse.FormTemplateVersionResponseBuilder builder = FormTemplateVersionResponse.builder()
                .id(version.getId())
                .templateId(version.getTemplate() != null ? version.getTemplate().getId() : null)
                .versionNumber(version.getVersionNumber())
                .name(version.getName())
                .description(version.getDescription())
                .instructions(version.getInstructions())
                .status(version.getStatus() != null ? version.getStatus().name() : null)
                .createdById(version.getCreatedByUser() != null ? version.getCreatedByUser().getId() : null)
                .createdByName(version.getCreatedByUser() != null ? version.getCreatedByUser().getFullName() : null)
                .createdAt(version.getCreatedAt());
        
        // Add sections if loaded
        if (version.getSections() != null && !version.getSections().isEmpty()) {
            List<FormSectionResponse> sections = version.getSections().stream()
                    .filter(section -> !Boolean.TRUE.equals(section.getIsDeleted()))
                    .map(this::toSectionResponse)
                    .collect(Collectors.toList());
            builder.sections(sections);
        }
        
        // Add fields (flattened) if loaded
        if (version.getFields() != null && !version.getFields().isEmpty()) {
            List<FormFieldResponse> fields = version.getFields().stream()
                    .filter(field -> !Boolean.TRUE.equals(field.getIsDeleted()))
                    .map(this::toFieldResponse)
                    .collect(Collectors.toList());
            builder.fields(fields);
        }
        
        return builder.build();
    }
    
    private FormSectionResponse toSectionResponse(FormSection section) {
        FormSectionResponse.FormSectionResponseBuilder builder = FormSectionResponse.builder()
                .id(section.getId())
                .templateVersionId(section.getTemplateVersion() != null ? section.getTemplateVersion().getId() : null)
                .name(section.getName())
                .description(section.getDescription())
                .sortOrder(section.getSortOrder())
                .createdAt(section.getCreatedAt());
        
        // Add fields if loaded
        if (section.getFields() != null && !section.getFields().isEmpty()) {
            List<FormFieldResponse> fields = section.getFields().stream()
                    .filter(field -> !Boolean.TRUE.equals(field.getIsDeleted()))
                    .map(this::toFieldResponse)
                    .collect(Collectors.toList());
            builder.fields(fields);
        }
        
        return builder.build();
    }

    private FormFieldResponse toFieldResponse(FormField field) {
        FormFieldResponse.FormFieldResponseBuilder builder = FormFieldResponse.builder()
                .id(field.getId())
                .templateVersionId(field.getTemplateVersion() != null ? field.getTemplateVersion().getId() : null)
                .sectionId(field.getSection() != null ? field.getSection().getId() : null)
                .sectionName(field.getSection() != null ? field.getSection().getName() : null)
                .fieldType(field.getFieldType() != null ? field.getFieldType().name() : null)
                .label(field.getFieldLabel())
                .placeholder(field.getPlaceholder())
                .helpText(field.getHelpText())
                .isRequired(field.getIsRequired())
                .validation(field.getValidationRules())
                .defaultValue(field.getDefaultValue())
                .isRepeatable(field.getIsRepeatable())
                .scoringFormula(field.getScoringFormula())
                .maxScore(field.getMaxScore())
                .autoPopulate(field.getAutoPopulate())
                .conditionalDisplay(field.getConditionalDisplay())
                .sortOrder(field.getSortOrder())
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt());
        
        // Add options if loaded (filter out deleted)
        if (field.getOptions() != null && !field.getOptions().isEmpty()) {
            List<FormFieldOptionResponse> optionsList = field.getOptions().stream()
                    .filter(opt -> !Boolean.TRUE.equals(opt.getIsDeleted()))
                    .map(opt -> FormFieldOptionResponse.builder()
                            .id(opt.getId())
                            .fieldId(opt.getField() != null ? opt.getField().getId() : null)
                            .label(opt.getLabel())
                            .value(opt.getValue())
                            .score(opt.getScore())
                            .sortOrder(opt.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            builder.options(optionsList);
        }
        
        return builder.build();
    }

    private FormAssignmentResponse toAssignmentResponse(FormAssignment assignment) {
        FormTemplateVersion version = assignment.getTemplateVersion();
        FormTemplate template = version != null ? version.getTemplate() : null;
        
        FormAssignmentResponse.FormAssignmentResponseBuilder builder = FormAssignmentResponse.builder()
                .id(assignment.getId())
                .templateId(template != null ? template.getId() : null)
                .templateVersionId(version != null ? version.getId() : null)
                .versionNumber(version != null ? version.getVersionNumber() : null)
                .templateName(template != null ? template.getName() : null)
                .templateCategory(template != null && template.getCategory() != null
                        ? template.getCategory().name()
                        : null)
                .clientId(assignment.getClient() != null ? assignment.getClient().getId() : null)
                .clientName(assignment.getClient() != null ? assignment.getClient().getFullName() : null)
                .assignedById(assignment.getAssignedBy() != null ? assignment.getAssignedBy().getId() : null)
                .assignedByName(assignment.getAssignedBy() != null ? assignment.getAssignedBy().getFullName() : null)
                .status(assignment.getStatus() != null ? assignment.getStatus().name() : null)
                .dueDate(assignment.getDueDate())
                .instructions(resolveAssignmentInstructions(assignment))
                .completedAt(assignment.getCompletedAt())
                .submittedAt(assignment.getSubmittedAt())
                .reviewedAt(assignment.getReviewedAt())
                .reviewedById(assignment.getReviewedBy() != null ? assignment.getReviewedBy().getId() : null)
                .reviewedByName(assignment.getReviewedBy() != null ? assignment.getReviewedBy().getFullName() : null)
                .reviewNotes(assignment.getReviewNotes())
                .remindersSent(assignment.getRemindersSent())
                .lastReminderAt(assignment.getLastReminderAt())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt());

        // Add responses
        if (assignment.getResponses() != null) {
            List<FormResponseDto> responses = assignment.getResponses().stream()
                    .map(this::toResponseDto)
                    .collect(Collectors.toList());
            builder.responses(responses);
        }

        // Add signatures
        if (assignment.getSignatures() != null) {
            List<FormSignatureResponse> signatures = assignment.getSignatures().stream()
                    .map(this::toSignatureResponse)
                    .collect(Collectors.toList());
            builder.signatures(signatures);
        }

        return builder.build();
    }

    private FormResponseDto toResponseDto(FormResponse response) {
        FormAssignmentField assignmentField = response.getAssignmentField();
        FormField field = assignmentField != null ? assignmentField.getField() : null;
        
        return FormResponseDto.builder()
                .id(response.getId())
                .assignmentId(response.getAssignment() != null ? response.getAssignment().getId() : null)
                .assignmentFieldId(assignmentField != null ? assignmentField.getId() : null)
                .fieldId(field != null ? field.getId() : null)
                .fieldLabel(assignmentField != null ? assignmentField.getFieldLabel() : null)
                .fieldType(assignmentField != null ? assignmentField.getFieldType() : null)
                .value(response.getResponseValue())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    private FormSignatureResponse toSignatureResponse(FormSignature signature) {
        return FormSignatureResponse.builder()
                .id(signature.getId())
                .assignmentId(signature.getAssignment() != null ? signature.getAssignment().getId() : null)
                .signatureData(signature.getSignatureData())
                .signerName(signature.getSignerName())
                .signerRole(signature.getSignerRole())
                .ipAddress(signature.getIpAddress())
                .userAgent(signature.getUserAgent())
                .signedAt(signature.getSignedAt())
                .agreedToTerms(signature.getAgreedToTerms())
                .createdAt(signature.getCreatedAt())
                .build();
    }

    private Status parseAssignmentStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BadRequestException("Status is required");
        }

        String normalized = normalizeToken(status).toUpperCase(Locale.ROOT);
        try {
            return Status.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid status value: " + status
                            + ". Allowed values: assigned, in_progress, submitted, completed, reviewed, cancelled.",
                    ex);
        }
    }

    private void recordAuditEvent(Long userId, String action, Long resourceId, String ipAddress,
            boolean hipaaRelevant) {
        recordAuditEvent(userId, action, resourceId, ipAddress, hipaaRelevant, null);
    }

    private void recordAuditEvent(Long userId, String action, Long resourceId, String ipAddress, boolean hipaaRelevant,
            Long clientId) {
        try {
            auditLogService.recordStaffEvent(userId, action, RESOURCE_TYPE_FORM, resourceId, clientId, ipAddress,
                    hipaaRelevant);
        } catch (Exception e) {
            log.error("Failed to record audit event for form: {}", resourceId, e);
        }
    }

    /**
     * Check if user has USER_MANAGE permission (PBAC).
     * This replaces the old role-based admin check.
     */
    private void assertAdmin(AuthPrincipal principal, String message) {
        permissionChecker.requireConsentAdminModuleAccess(principal, message);
    }

}


