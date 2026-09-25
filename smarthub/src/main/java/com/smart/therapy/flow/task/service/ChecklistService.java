package com.smart.therapy.flow.task.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.dto.PatchUpdates;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.task.entity.*;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.task.dto.*;
import com.smart.therapy.flow.task.dto.ClientChecklistFilterRequest;
import com.smart.therapy.flow.task.dto.AssignChecklistRequest;
import com.smart.therapy.flow.task.enums.CheckListCategory;
import com.smart.therapy.flow.task.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChecklistService {

    private static final String RESOURCE_TYPE_CHECKLIST = "checklist";
    private static final int DEFAULT_TEMPLATE_PAGE = 1;
    private static final int DEFAULT_TEMPLATE_PAGE_SIZE = 20;
    private static final int MAX_TEMPLATE_PAGE_SIZE = 200;

    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ClientChecklistRepository clientChecklistRepository;
    private final ClientChecklistItemRepository clientChecklistItemRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private com.smart.therapy.flow.notification.service.NotificationService notificationService;

    // ========== TEMPLATE METHODS ==========

    @Transactional(readOnly = true)
    @Cacheable(value = "checklists", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('templates')")
    public List<ChecklistTemplateResponse> getTemplates() {
        return getTemplates(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ChecklistTemplateResponse> getTemplates(Integer page, Integer pageSize, String search, String category) {
        List<ChecklistTemplate> templates = checklistTemplateRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc();

        if (StringUtils.hasText(search)) {
            String term = search.trim().toLowerCase(Locale.ROOT);
            templates = templates.stream()
                    .filter(template -> {
                        String name = template.getName() != null ? template.getName().toLowerCase(Locale.ROOT) : "";
                        String description = template.getDescription() != null
                                ? template.getDescription().toLowerCase(Locale.ROOT)
                                : "";
                        String clientType = template.getClientType() != null
                                ? template.getClientType().toLowerCase(Locale.ROOT)
                                : "";
                        return name.contains(term) || description.contains(term) || clientType.contains(term);
                    })
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(category)) {
            CheckListCategory categoryFilter = parseChecklistCategory(category);
            String normalizedCategory = normalizeToken(category);

            templates = templates.stream()
                    .filter(template -> {
                        boolean matchesTemplateCategory = normalizeToken(template.getCategory()).equals(normalizedCategory)
                                || normalizeToken(template.getClientType()).equals(normalizedCategory);
                        boolean matchesItemCategory = categoryFilter != null
                                && template.getId() != null
                                && checklistItemRepository.existsByTemplateIdAndCategory(template.getId(), categoryFilter);
                        return matchesTemplateCategory || matchesItemCategory;
                    })
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
        List<ChecklistTemplate> pageSlice = templates.subList(fromIndex, toIndex);

        List<Long> templateIds = pageSlice.stream()
                .map(ChecklistTemplate::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Integer> itemCountsByTemplateId = loadItemCountsByTemplateIds(templateIds);

        return pageSlice.stream()
                .map(template -> toTemplateResponse(
                        template,
                        itemCountsByTemplateId.getOrDefault(template.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "checklists", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('template:' + #id)")
    public ChecklistTemplateResponse getTemplate(Long id) {
        ChecklistTemplate template = checklistTemplateRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));
        return toTemplateResponseWithItems(template);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getTemplateItems(Long templateId) {
        Objects.requireNonNull(templateId, "Template ID is required");
        checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        return checklistItemRepository.findByTemplateIdOrderBySortOrderAsc(templateId).stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChecklistItemResponse getTemplateItem(Long templateId, Long itemId) {
        ChecklistItem item = findTemplateItemOrThrow(templateId, itemId);
        return toItemResponse(item);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistTemplateResponse createTemplate(CreateChecklistTemplateRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create checklist templates");

        ChecklistTemplate template = ChecklistTemplate.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .clientType(request.getClientType())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        // Create items if provided
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ChecklistItem> items = request.getItems().stream()
                    .map(itemRequest -> createItemFromRequest(itemRequest, saved))
                    .collect(Collectors.toList());
            checklistItemRepository.saveAll(items);
            saved.setItems(items);
        }

        // Audit log
        recordAuditEvent(resolveActorUserId(requester), "checklist_template_created", saved.getId(), ipAddress, false);

        return toTemplateResponseWithItems(saved);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistTemplateResponse updateTemplate(Long id, CreateChecklistTemplateRequest request, 
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update checklist templates");

        ChecklistTemplate template = checklistTemplateRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (template.getIsDeleted()) {
            throw new BadRequestException("Cannot update deleted template");
        }

        // Update fields
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            template.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getClientType() != null) {
            template.setClientType(request.getClientType());
        }
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            template.setSortOrder(request.getSortOrder());
        }

        // Update items if provided
        if (request.getItems() != null) {
            List<ChecklistItem> managedItems = template.getItems();
            if (managedItems == null) {
                managedItems = new ArrayList<>();
                template.setItems(managedItems);
            }

            // Do not physically remove existing template items because historical
            // client_checklist_items can still reference them.
            for (ChecklistItem existingItem : managedItems) {
                if (!Boolean.TRUE.equals(existingItem.getIsDeleted())) {
                    existingItem.setIsDeleted(true);
                    existingItem.setDeletedAt(Instant.now());
                }
            }

            if (!request.getItems().isEmpty()) {
                List<ChecklistItem> items = request.getItems().stream()
                        .map(itemRequest -> createItemFromRequest(itemRequest, template))
                        .collect(Collectors.toList());
                managedItems.addAll(items);
            }
        }

        ChecklistTemplate updated = checklistTemplateRepository.save(template);

        // Audit log
        recordAuditEvent(resolveActorUserId(requester), "checklist_template_updated", updated.getId(), ipAddress, false);

        return toTemplateResponseWithItems(updated);
    }

    @Transactional
    public void deleteTemplate(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete checklist templates");

        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (template.getIsDeleted()) {
            throw new BadRequestException("Template is already deleted");
        }

        // Check if template has active client checklists
        long activeChecklistCount = clientChecklistRepository.countByTemplateIdAndNotDeleted(id);
        if (activeChecklistCount > 0) {
            throw new BadRequestException(
                    "Cannot delete template that is assigned to " + activeChecklistCount + 
                    " client(s). Please remove assignments first or deactivate the template instead.");
        }

        // Soft delete template
        template.setIsDeleted(true);
        template.setDeletedAt(Instant.now());
        checklistTemplateRepository.save(template);

        // Audit log
        recordAuditEvent(resolveActorUserId(requester), "checklist_template_deleted", id, ipAddress, false);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistTemplateResponse activateTemplate(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can activate/deactivate checklist templates");

        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (template.getIsDeleted()) {
            throw new BadRequestException("Cannot activate deleted template");
        }

        template.setIsActive(true);
        ChecklistTemplate updated = checklistTemplateRepository.save(template);

        recordAuditEvent(resolveActorUserId(requester), "checklist_template_activated", id, ipAddress, false);
        return toTemplateResponseWithItems(updated);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistTemplateResponse deactivateTemplate(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can activate/deactivate checklist templates");

        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (template.getIsDeleted()) {
            throw new BadRequestException("Cannot deactivate deleted template");
        }

        template.setIsActive(false);
        ChecklistTemplate updated = checklistTemplateRepository.save(template);

        recordAuditEvent(resolveActorUserId(requester), "checklist_template_deactivated", id, ipAddress, false);
        return toTemplateResponseWithItems(updated);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistItemResponse addItemToTemplate(Long templateId, CreateChecklistItemRequest request, 
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can add items to templates");

        ChecklistTemplate template = checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (template.getIsDeleted()) {
            throw new BadRequestException("Cannot add items to deleted template");
        }

        ChecklistItem item = createItemFromRequest(request, template);
        ChecklistItem saved = checklistItemRepository.save(item);

        recordAuditEvent(resolveActorUserId(requester), "checklist_item_added", saved.getId(), ipAddress, false);
        return toItemResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistItemResponse updateTemplateItem(Long itemId, UpdateChecklistTemplateItemRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(itemId, "Item ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update template items");

        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found"));

        if (item.getIsDeleted()) {
            throw new BadRequestException("Cannot update deleted item");
        }

        // Update fields
        if (request.isAnyFieldPresent("title", "titleKey")) {
            if (request.isFieldPresent("title")) {
                if (StringUtils.hasText(request.getTitle())) {
                    item.setTitle(request.getTitle());
                    item.setItemText(request.getTitle());
                } else {
                    item.setTitle(null);
                    item.setItemText(null);
                }
            }
        }
        PatchUpdates.apply(request, "description", request.getDescription(), item::setDescription);
        if (request.isFieldPresent("category")) {
            item.setCategory(StringUtils.hasText(request.getCategory())
                    ? parseChecklistCategory(request.getCategory())
                    : null);
        }
        PatchUpdates.apply(request, "isRequired", request.getIsRequired(), item::setIsRequired);
        PatchUpdates.apply(request, "itemOrder", request.getItemOrder(), item::setItemOrder);
        PatchUpdates.apply(request, "daysFromStart", request.getDaysFromStart(), item::setDaysFromStart);
        PatchUpdates.apply(request, "sortOrder", request.getSortOrder(), item::setSortOrder);

        ChecklistItem updated = checklistItemRepository.save(item);

        recordAuditEvent(resolveActorUserId(requester), "checklist_template_item_updated", itemId, ipAddress, false);
        return toItemResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistItemResponse updateTemplateItem(Long templateId, Long itemId, UpdateChecklistTemplateItemRequest request,
            AuthPrincipal requester, String ipAddress) {
        findTemplateItemOrThrow(templateId, itemId);
        return updateTemplateItem(itemId, request, requester, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public void deleteTemplateItem(Long itemId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(itemId, "Item ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete template items");

        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found"));

        if (item.getIsDeleted()) {
            throw new BadRequestException("Item is already deleted");
        }

        // Check if item is used in any client checklists
        // Note: We don't prevent deletion if used, but we could add this check if needed
        // For now, we allow deletion as items are snapshotted when assigned

        item.setIsDeleted(true);
        item.setDeletedAt(Instant.now());
        checklistItemRepository.save(item);

        recordAuditEvent(resolveActorUserId(requester), "checklist_template_item_deleted", itemId, ipAddress, false);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public void deleteTemplateItem(Long templateId, Long itemId, AuthPrincipal requester, String ipAddress) {
        findTemplateItemOrThrow(templateId, itemId);
        deleteTemplateItem(itemId, requester, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistTemplateResponse reorderItems(Long templateId, ReorderChecklistItemsRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can reorder items");

        ChecklistTemplate template = checklistTemplateRepository.findByIdWithItems(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (template.getIsDeleted()) {
            throw new BadRequestException("Cannot reorder items in deleted template");
        }

        List<ChecklistItem> items = template.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Template has no items to reorder");
        }

        // Validate that all provided IDs exist in the template
        Set<Long> templateItemIds = items.stream()
                .map(ChecklistItem::getId)
                .collect(Collectors.toSet());
        
        if (!templateItemIds.containsAll(request.getItemIds())) {
            throw new BadRequestException("Some item IDs do not belong to this template");
        }

        if (request.getItemIds().size() != templateItemIds.size()) {
            throw new BadRequestException("Item IDs count must match template items count");
        }

        // Create a map for quick lookup
        Map<Long, ChecklistItem> itemMap = items.stream()
                .collect(Collectors.toMap(ChecklistItem::getId, item -> item));

        // Reorder items based on request
        List<ChecklistItem> reorderedItems = request.getItemIds().stream()
                .map(itemMap::get)
                .collect(Collectors.toList());

        // Update sort orders
        for (int i = 0; i < reorderedItems.size(); i++) {
            reorderedItems.get(i).setSortOrder(i);
        }

        checklistItemRepository.saveAll(reorderedItems);
        
        // Refresh template to get updated items
        ChecklistTemplate refreshed = checklistTemplateRepository.findByIdWithItems(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        recordAuditEvent(resolveActorUserId(requester), "checklist_items_reordered", templateId, ipAddress, false);
        return toTemplateResponseWithItems(refreshed);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public ChecklistTemplateResponse copyTemplate(Long templateId, CopyChecklistTemplateRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can copy checklist templates");

        ChecklistTemplate original = checklistTemplateRepository.findByIdWithItems(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (original.getIsDeleted()) {
            throw new BadRequestException("Cannot copy deleted template");
        }

        // Create new template
        ChecklistTemplate copy = ChecklistTemplate.builder()
                .name(request.getName() != null ? request.getName() : (original.getName() + " (Copy)"))
                .description(request.getDescription() != null ? request.getDescription() : original.getDescription())
                .clientType(original.getClientType())
                .isActive(request.getIsActive() != null ? request.getIsActive() : false)
                .sortOrder(original.getSortOrder())
                .isSystem(false) // Copies are never system templates
                .build();

        ChecklistTemplate saved = checklistTemplateRepository.save(copy);

        // Copy items
        if (original.getItems() != null && !original.getItems().isEmpty()) {
            List<ChecklistItem> copiedItems = original.getItems().stream()
                    .map(originalItem -> ChecklistItem.builder()
                            .template(saved)
                            .itemText(originalItem.getItemText())
                            .title(originalItem.getTitle())
                            .description(originalItem.getDescription())
                            .category(originalItem.getCategory())
                            .isRequired(originalItem.getIsRequired())
                            .itemOrder(originalItem.getItemOrder())
                            .daysFromStart(originalItem.getDaysFromStart())
                            .sortOrder(originalItem.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
            checklistItemRepository.saveAll(copiedItems);
            saved.setItems(copiedItems);
        }

        recordAuditEvent(resolveActorUserId(requester), "checklist_template_copied", saved.getId(), ipAddress, false);
        return toTemplateResponseWithItems(saved);
    }

    @Transactional
    @CacheEvict(value = "checklists", allEntries = true)
    public List<ClientChecklistResponse> bulkAssignChecklist(BulkAssignChecklistRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        ChecklistTemplate template = checklistTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (template.getIsDeleted() || !Boolean.TRUE.equals(template.getIsActive())) {
            throw new BadRequestException("Cannot assign inactive or deleted template");
        }

        List<ClientChecklistResponse> responses = new ArrayList<>();
        List<Long> failedClientIds = new ArrayList<>();

        for (Long clientId : request.getClientIds()) {
            try {
                Client client = clientRepository.findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

                if (client.getIsDeleted()) {
                    failedClientIds.add(clientId);
                    continue;
                }

                // Check if already assigned
                if (clientChecklistRepository.existsByClientIdAndTemplateId(clientId, request.getTemplateId())) {
                    failedClientIds.add(clientId);
                    continue;
                }

                AssignChecklistRequest assignRequest = new AssignChecklistRequest();
                assignRequest.setTemplateId(request.getTemplateId());
                assignRequest.setDueDate(request.getDueDate());
                // Note: AssignChecklistRequest doesn't have description/notes fields

                ClientChecklistResponse response = assignChecklistToClient(clientId, assignRequest, requester, ipAddress);
                responses.add(response);
            } catch (Exception e) {
                log.error("Failed to assign checklist to client {}: {}", clientId, e.getMessage());
                failedClientIds.add(clientId);
            }
        }

        if (!failedClientIds.isEmpty()) {
            log.warn("Failed to assign checklist to {} client(s): {}", failedClientIds.size(), failedClientIds);
        }

        recordAuditEvent(resolveActorUserId(requester), "checklist_bulk_assigned", request.getTemplateId(), ipAddress, false);
        return responses;
    }

    @Transactional(readOnly = true)
    public ComplianceReportResponse getComplianceReport(Long templateId, AuthPrincipal requester) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        ChecklistTemplate template = checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        // Get all client checklists for this template
        List<ClientChecklist> checklists = clientChecklistRepository.findByTemplateIdAndNotDeleted(templateId);

        long totalAssignments = checklists.size();
        long completedAssignments = checklists.stream()
                .filter(cc -> Boolean.TRUE.equals(cc.getIsCompleted()))
                .count();
        long pendingAssignments = totalAssignments - completedAssignments;

        // Calculate overdue (not completed and due date passed)
        long overdueAssignments = checklists.stream()
                .filter(cc -> !Boolean.TRUE.equals(cc.getIsCompleted()))
                .filter(cc -> cc.getDueDate() != null && cc.getDueDate().isBefore(java.time.LocalDate.now()))
                .count();

        double completionRate = totalAssignments > 0 ? (completedAssignments * 100.0 / totalAssignments) : 0.0;

        // Build client statuses
        List<ComplianceReportResponse.ClientComplianceStatus> clientStatuses = checklists.stream()
                .map(checklist -> {
                    long completedItems = checklist.getItems() != null ?
                            checklist.getItems().stream()
                                    .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                                    .count() : 0;
                    long totalItems = checklist.getItems() != null ? checklist.getItems().size() : 0;

                    return ComplianceReportResponse.ClientComplianceStatus.builder()
                            .clientId(checklist.getClient() != null ? checklist.getClient().getId() : null)
                            .clientName(checklist.getClient() != null ? checklist.getClient().getFullName() : null)
                            .checklistId(checklist.getId())
                            .isCompleted(checklist.getIsCompleted())
                            .completedAt(checklist.getCompletedAt())
                            .dueDate(checklist.getDueDate())
                            .isOverdue(!Boolean.TRUE.equals(checklist.getIsCompleted()) && 
                                    checklist.getDueDate() != null && 
                                    checklist.getDueDate().isBefore(java.time.LocalDate.now()))
                            .completedItemsCount(completedItems)
                            .totalItemsCount(totalItems)
                            .build();
                })
                .collect(Collectors.toList());

        return ComplianceReportResponse.builder()
                .templateId(template.getId())
                .templateName(template.getName())
                .category(template.getClientType())
                .totalAssignments(totalAssignments)
                .completedAssignments(completedAssignments)
                .pendingAssignments(pendingAssignments)
                .overdueAssignments(overdueAssignments)
                .completionRate(completionRate)
                .clientStatuses(clientStatuses)
                .reportGeneratedAt(Instant.now())
                .build();
    }

    // ========== CLIENT CHECKLIST METHODS ==========

    @Transactional(readOnly = true)
    public List<ClientChecklistResponse> getClientChecklists(Long clientId) {
        Objects.requireNonNull(clientId, "Client ID is required");

        List<ClientChecklist> checklists = clientChecklistRepository.findByClientIdWithDetails(clientId);
        return checklists.stream()
                .map(this::toClientChecklistResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClientChecklistResponse> getClientChecklistsWithFilters(ClientChecklistFilterRequest filter) {
        Objects.requireNonNull(filter, "Filter request is required");
        CheckListCategory category = parseChecklistCategoryOrNull(filter.getCategory());

        List<ClientChecklist> checklists = clientChecklistRepository.findWithFilters(
                filter.getClientId(),
                filter.getTemplateId(),
                category,
                filter.getIsCompleted(),
                filter.getCompletedDateFrom(),
                filter.getCompletedDateTo(),
                filter.getDueDateFrom(),
                filter.getDueDateTo(),
                filter.getCreatedDateFrom(),
                filter.getCreatedDateTo()
        );

        return checklists.stream()
                .map(this::toClientChecklistResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientChecklistResponse getClientChecklist(Long id) {
        Objects.requireNonNull(id, "Checklist ID is required");

        ClientChecklist checklist = clientChecklistRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client checklist not found"));

        return toClientChecklistResponse(checklist);
    }

    @Transactional
    public ClientChecklistResponse assignChecklistToClient(Long clientId, AssignChecklistRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // PBAC: Check permission to assign checklists
        // Checklist assignment requires ability to view clients and create tasks
        if (!permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN") && 
            !permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM") &&
            !permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL")) {
            throw new ForbiddenException("Insufficient permissions to assign checklists");
        }

        // Get client
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ChecklistTemplate template = checklistTemplateRepository.findByIdWithItems(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        if (!template.getIsActive()) {
            throw new BadRequestException("Cannot assign inactive template");
        }

        // Check if client already has this checklist template assigned
        if (clientChecklistRepository.existsByClientIdAndTemplateId(clientId, request.getTemplateId())) {
            throw new ConflictException("This checklist template is already assigned to this client");
        }

        // Create client checklist
        ClientChecklist clientChecklist = ClientChecklist.builder()
                .client(client)
                .template(template)
                .isCompleted(false)
                .dueDate(request.getDueDate())
                .build();

        ClientChecklist saved = clientChecklistRepository.save(clientChecklist);

        // Create checklist items from template
        if (template.getItems() != null && !template.getItems().isEmpty()) {
            List<ClientChecklistItem> items = template.getItems().stream()
                    .map(templateItem -> ClientChecklistItem.builder()
                            .clientChecklist(saved)
                            .checklistItem(templateItem)
                            .isCompleted(false)
                            .build())
                    .collect(Collectors.toList());
            clientChecklistItemRepository.saveAll(items);
            saved.setItems(items);
        }

        // Trigger notification
        if (notificationService != null) {
            try {
                Map<String, Object> eventData = buildChecklistNotificationPayload(saved, null);
                notificationService.processEvent(NotificationEventCatalog.CHECKLIST_ASSIGNED, eventData);
            } catch (Exception e) {
                log.error("Failed to trigger checklist_assigned notification", e);
            }
        }

        // Audit log
        recordAuditEvent(resolveActorUserId(requester), "checklist_assigned", saved.getId(), ipAddress, true, clientId);

        return toClientChecklistResponse(saved);
    }

    // ========== CLIENT CHECKLIST ITEM METHODS ==========

    @Transactional(readOnly = true)
    public List<ClientChecklistItemResponse> getClientChecklistItems(Long clientChecklistId) {
        Objects.requireNonNull(clientChecklistId, "Client checklist ID is required");

        List<ClientChecklistItem> items = clientChecklistItemRepository
                .findByClientChecklistIdOrderByChecklistItemSortOrderAsc(clientChecklistId);
        return items.stream()
                .map(this::toClientChecklistItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientChecklistItemResponse updateClientChecklistItem(Long id, UpdateChecklistItemRequest request,
            AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Item ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        ClientChecklistItem item = clientChecklistItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client checklist item not found"));

        // Update fields
        if (request.getIsCompleted() != null) {
            item.setIsCompleted(request.getIsCompleted());
            if (request.getIsCompleted()) {
                item.setCompletedAt(Instant.now());
                User completedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                item.setCompletedBy(completedBy);
            } else {
                item.setCompletedAt(null);
                item.setCompletedBy(null);
            }
        }
        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }

        ClientChecklistItem updated = clientChecklistItemRepository.saveAndFlush(item);

        // Check if all items are completed and update parent checklist
        ClientChecklist checklist = item.getClientChecklist();
        if (checklist != null) {
            Long checklistId = checklist.getId();
            List<ClientChecklistItem> allItems = clientChecklistItemRepository
                    .findByClientChecklistIdOrderByChecklistItemSortOrderAsc(checklistId);
            boolean allCompleted = allItems.stream()
                    .allMatch(entry -> Boolean.TRUE.equals(entry.getIsCompleted()));

            // Re-load parent to avoid stale @Version when many item updates run in parallel
            ClientChecklist freshChecklist = clientChecklistRepository.findById(checklistId)
                    .orElse(checklist);

            if (allCompleted && !Boolean.TRUE.equals(freshChecklist.getIsCompleted())) {
                freshChecklist.setIsCompleted(true);
                freshChecklist.setCompletedAt(Instant.now());
                User completedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                freshChecklist.setCompletedBy(completedBy);
                try {
                    clientChecklistRepository.saveAndFlush(freshChecklist);
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                    log.warn("Optimistic lock while completing checklist {}: {}", checklistId, ex.getMessage());
                    ClientChecklist retry = clientChecklistRepository.findById(checklistId).orElse(null);
                    if (retry != null && !Boolean.TRUE.equals(retry.getIsCompleted())) {
                        retry.setIsCompleted(true);
                        retry.setCompletedAt(Instant.now());
                        retry.setCompletedBy(completedBy);
                        clientChecklistRepository.saveAndFlush(retry);
                    }
                }

                // Trigger notification
                if (notificationService != null) {
                    try {
                        Map<String, Object> eventData = buildChecklistNotificationPayload(freshChecklist, null);
                        notificationService.processEvent(NotificationEventCatalog.CHECKLIST_COMPLETED, eventData);
                    } catch (Exception e) {
                        log.error("Failed to trigger checklist_completed notification", e);
                    }
                }
            } else if (Boolean.TRUE.equals(item.getIsCompleted()) && notificationService != null) {
                try {
                    Map<String, Object> eventData = buildChecklistNotificationPayload(freshChecklist, item);
                    notificationService.processEvent(NotificationEventCatalog.CHECKLIST_ITEM_COMPLETED, eventData);
                } catch (Exception e) {
                    log.error("Failed to trigger checklist_item_completed notification", e);
                }
            } else if (!allCompleted && Boolean.TRUE.equals(freshChecklist.getIsCompleted())) {
                freshChecklist.setIsCompleted(false);
                freshChecklist.setCompletedAt(null);
                freshChecklist.setCompletedBy(null);
                try {
                    clientChecklistRepository.saveAndFlush(freshChecklist);
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                    log.warn("Optimistic lock while reopening checklist {}: {}", checklistId, ex.getMessage());
                    ClientChecklist retry = clientChecklistRepository.findById(checklistId).orElse(null);
                    if (retry != null && Boolean.TRUE.equals(retry.getIsCompleted())) {
                        retry.setIsCompleted(false);
                        retry.setCompletedAt(null);
                        retry.setCompletedBy(null);
                        clientChecklistRepository.saveAndFlush(retry);
                    }
                }
            }

            checklist = freshChecklist;
        }

        // Audit log
        Long clientId = checklist != null && checklist.getClient() != null ? checklist.getClient().getId() : null;
        recordAuditEvent(resolveActorUserId(requester), "checklist_item_updated", id, ipAddress, true, clientId);

        return toClientChecklistItemResponse(updated);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * HIPAA-safe checklist notification payload: MRN + checklist/template labels, never client name.
     */
    private Map<String, Object> buildChecklistNotificationPayload(
            ClientChecklist checklist,
            ClientChecklistItem item) {
        Map<String, Object> eventData = new HashMap<>();
        if (checklist == null) {
            return eventData;
        }
        eventData.put("id", checklist.getId());
        Client client = checklist.getClient();
        if (client != null) {
            eventData.put("clientId", client.getId());
            NotificationPayloadFactory.putClientIdentity(eventData, client);
            if (client.getAssignedTherapist() != null) {
                eventData.put("therapistId", client.getAssignedTherapist().getId());
                eventData.put("therapistName", client.getAssignedTherapist().getFullName());
            } else {
                eventData.put("therapistName", "Unassigned");
            }
        }
        if (checklist.getTemplate() != null) {
            eventData.put("templateId", checklist.getTemplate().getId());
            eventData.put("templateName", checklist.getTemplate().getName());
            eventData.put("checklistName", checklist.getTemplate().getName());
        }
        if (item != null) {
            eventData.put("itemId", item.getId());
            String itemTitle = item.getChecklistItem() != null
                    ? (StringUtils.hasText(item.getChecklistItem().getTitle())
                            ? item.getChecklistItem().getTitle()
                            : item.getChecklistItem().getItemText())
                    : null;
            if (StringUtils.hasText(itemTitle)) {
                eventData.put("itemTitle", itemTitle);
            }
        }
        return eventData;
    }

    private ChecklistItem createItemFromRequest(CreateChecklistItemRequest request, ChecklistTemplate template) {
        // itemText is required - use title if available, otherwise use description or empty string
        String itemText = request.getTitle() != null && !request.getTitle().trim().isEmpty() 
                ? request.getTitle() 
                : (request.getDescription() != null && !request.getDescription().trim().isEmpty()
                        ? request.getDescription()
                        : "");
        
        if (itemText.isEmpty()) {
            throw new BadRequestException("Item text is required. Provide either title or description.");
        }
        
        return ChecklistItem.builder()
                .template(template)
                .itemText(itemText)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(parseChecklistCategoryOrNull(request.getCategory()))
                .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                .itemOrder(request.getItemOrder())
                .daysFromStart(request.getDaysFromStart())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
    }

    private ChecklistItem findTemplateItemOrThrow(Long templateId, Long itemId) {
        Objects.requireNonNull(templateId, "Template ID is required");
        Objects.requireNonNull(itemId, "Item ID is required");

        checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template not found"));

        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found"));

        if (item.getTemplate() == null || !templateId.equals(item.getTemplate().getId())) {
            throw new BadRequestException("Checklist item does not belong to the provided template");
        }
        if (Boolean.TRUE.equals(item.getIsDeleted())) {
            throw new ResourceNotFoundException("Checklist item not found");
        }
        return item;
    }

    private ChecklistTemplateResponse toTemplateResponse(ChecklistTemplate template, int itemCount) {
        return ChecklistTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .clientType(template.getClientType())
                .isActive(template.getIsActive())
                .sortOrder(template.getSortOrder())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .itemCount(itemCount)
                .build();
    }

    private Map<Long, Integer> loadItemCountsByTemplateIds(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Map.of();
        }
        return checklistItemRepository.countActiveItemsByTemplateIds(templateIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    private ChecklistTemplateResponse toTemplateResponseWithItems(ChecklistTemplate template) {
        ChecklistTemplateResponse response = toTemplateResponse(template, 0);

        if (template.getItems() != null) {
            List<ChecklistItemResponse> items = template.getItems().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
            response.setItems(items);
            response.setItemCount(items.size());
        }

        return response;
    }

    private ChecklistItemResponse toItemResponse(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .templateId(item.getTemplate() != null ? item.getTemplate().getId() : null)
                .title(item.getTitle())
                .description(item.getDescription())
                .category(item.getCategory() != null ? item.getCategory().name() : null)
                .isRequired(item.getIsRequired())
                .itemOrder(item.getItemOrder())
                .daysFromStart(item.getDaysFromStart())
                .sortOrder(item.getSortOrder())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private ClientChecklistResponse toClientChecklistResponse(ClientChecklist checklist) {
        ClientChecklistResponse.ClientChecklistResponseBuilder builder = ClientChecklistResponse.builder()
                .id(checklist.getId())
                .clientId(checklist.getClient() != null ? checklist.getClient().getId() : null)
                .clientName(checklist.getClient() != null ? checklist.getClient().getFullName() : null)
                .templateId(checklist.getTemplate() != null ? checklist.getTemplate().getId() : null)
                .templateName(checklist.getTemplate() != null ? checklist.getTemplate().getName() : null)
                .isCompleted(checklist.getIsCompleted())
                .completedAt(checklist.getCompletedAt())
                .completedById(checklist.getCompletedBy() != null ? checklist.getCompletedBy().getId() : null)
                .completedByName(checklist.getCompletedBy() != null ? checklist.getCompletedBy().getFullName() : null)
                .notes(checklist.getNotes())
                .description(checklist.getDescription())
                .dueDate(checklist.getDueDate())
                .createdAt(checklist.getCreatedAt())
                .updatedAt(checklist.getUpdatedAt());

        // Add items
        if (checklist.getItems() != null) {
            List<ClientChecklistItemResponse> items = checklist.getItems().stream()
                    .map(this::toClientChecklistItemResponse)
                    .collect(Collectors.toList());
            builder.items(items);
        }

        return builder.build();
    }

    private ClientChecklistItemResponse toClientChecklistItemResponse(ClientChecklistItem item) {
        return ClientChecklistItemResponse.builder()
                .id(item.getId())
                .clientChecklistId(item.getClientChecklist() != null ? item.getClientChecklist().getId() : null)
                .checklistItemId(item.getChecklistItem() != null ? item.getChecklistItem().getId() : null)
                .checklistItemTitle(item.getChecklistItem() != null ? item.getChecklistItem().getTitle() : null)
                .checklistItemDescription(
                        item.getChecklistItem() != null ? item.getChecklistItem().getDescription() : null)
                .checklistItemCategory(item.getChecklistItem() != null && item.getChecklistItem().getCategory() != null
                        ? item.getChecklistItem().getCategory().name()
                        : null)
                .isCompleted(item.getIsCompleted())
                .completedAt(item.getCompletedAt())
                .completedById(item.getCompletedBy() != null ? item.getCompletedBy().getId() : null)
                .completedByName(item.getCompletedBy() != null ? item.getCompletedBy().getFullName() : null)
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private void recordAuditEvent(Long userId, String action, Long resourceId, String ipAddress,
            boolean hipaaRelevant) {
        recordAuditEvent(userId, action, resourceId, ipAddress, hipaaRelevant, null);
    }

    private void recordAuditEvent(Long userId, String action, Long resourceId, String ipAddress, boolean hipaaRelevant,
            Long clientId) {
        try {
            auditLogService.recordStaffEvent(userId, action, RESOURCE_TYPE_CHECKLIST, resourceId, clientId, ipAddress,
                    hipaaRelevant);
        } catch (Exception e) {
            log.error("Failed to record audit event for checklist: {}", resourceId, e);
        }
    }

    private CheckListCategory parseChecklistCategoryOrNull(String rawCategory) {
        if (!StringUtils.hasText(rawCategory)) {
            return null;
        }
        return parseChecklistCategory(rawCategory);
    }

    private CheckListCategory parseChecklistCategory(String rawCategory) {
        String normalized = normalizeToken(rawCategory);
        try {
            return CheckListCategory.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid category value: " + rawCategory
                    + ". Allowed values: intake, assessment, ongoing, discharge.", ex);
        }
    }

    private String normalizeToken(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Check if user has USER_MANAGE permission (PBAC).
     * This replaces the old role-based admin check.
     */
    private void assertAdmin(AuthPrincipal principal, String message) {
        permissionChecker.requireConsentAdminModuleAccess(principal, message);
    }

    private Long resolveActorUserId(AuthPrincipal requester) {
        return currentUserService.getCurrentUser(requester)
                .map(User::getId)
                .orElse(null);
    }
}


