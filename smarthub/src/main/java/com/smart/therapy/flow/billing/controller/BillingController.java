package com.smart.therapy.flow.billing.controller;

import com.smart.therapy.flow.billing.dto.*;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.billing.service.TenantSubscriptionInvoiceService;
import com.smart.therapy.flow.billing.service.TenantSubscriptionQueryService;
import com.smart.therapy.flow.payment.service.StripePlatformSubscriptionService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing", description = "🟢 Billing Specialist, Admin, Therapist & Supervisor - Billing operations")
public class BillingController {

    private final BillingService billingService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final TenantSubscriptionQueryService tenantSubscriptionQueryService;
    private final TenantSubscriptionInvoiceService tenantSubscriptionInvoiceService;
    private final StripePlatformSubscriptionService stripePlatformSubscriptionService;

    @Autowired
    private com.smart.therapy.flow.common.config.AppProperties appProperties;

    /** Require BILLING_MODULE plan feature for this organisation. */
    private void requireBillingModule() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && !subscriptionFeatureService.isFeatureEnabled(orgId, SubscriptionFeatureService.FEATURE_BILLING_MODULE, null)) {
            throw new ForbiddenException("Billing is not included in your plan. Please upgrade to access billing and invoicing.");
        }
    }

    @GetMapping("/subscription/me")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get current tenant subscription details",
            description = "Returns current plan, subscription status, and effective feature entitlements with current-period usage.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TenantSubscriptionDetailsResponse> getMySubscriptionDetails() {
        return ResponseEntity.ok(tenantSubscriptionQueryService.getCurrentTenantSubscriptionDetails());
    }

    @GetMapping("/subscription/invoices")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "List tenant subscription invoices",
            description = "Returns subscription invoices for the current organisation with optional status filter.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Page<TenantSubscriptionInvoiceResponse>> getTenantSubscriptionInvoices(
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int size
    ) {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new ForbiddenException("Tenant context required.");
        }
        return ResponseEntity.ok(tenantSubscriptionInvoiceService.listInvoices(orgId, status, page, size));
    }

    @PostMapping("/subscription/invoices/{invoiceId}/pay")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Generate Stripe payment URL for subscription invoice",
            description = "Returns Stripe hosted invoice payment URL for a pending subscription invoice.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TenantSubscriptionInvoicePayResponse> payTenantSubscriptionInvoice(
            @PathVariable("invoiceId") Long invoiceId
    ) {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new ForbiddenException("Tenant context required.");
        }
        return ResponseEntity.ok(tenantSubscriptionInvoiceService.createStripePaymentUrl(orgId, invoiceId));
    }

    @PostMapping("/subscription/checkout")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create Stripe Checkout session for subscription",
            description = "Returns Stripe Checkout URL for first subscribe or re-subscribe after cancel.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TenantSubscriptionCheckoutResponse> createSubscriptionCheckout() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new ForbiddenException("Tenant context required.");
        }
        return ResponseEntity.ok(stripePlatformSubscriptionService.createCheckoutSession(orgId));
    }

    @PostMapping("/subscription/portal")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create Stripe Billing Portal session",
            description = "Returns Stripe Billing Portal URL to manage payment method and view invoices.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TenantSubscriptionPortalResponse> createSubscriptionPortal() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new ForbiddenException("Tenant context required.");
        }
        return ResponseEntity.ok(stripePlatformSubscriptionService.createBillingPortalSession(orgId));
    }

    @GetMapping("/services")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get all billing services",
            description = """
                    Get a list of all billing services with optional filters.
                    
                    **Query Parameters:**
                    - `activeOnly` (optional): If true, returns only active services
                    - `therapistVisible` (optional): If true/false, filters by therapist visibility
                    - `clientPortalVisible` (optional): If true/false, filters by client portal visibility
                    
                    **Returns:** List of services
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, SUPERVISOR, or THERAPIST role.
                    Therapists only see billing for their assigned clients and sessions they conducted.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<ServiceResponse>> getServices(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Boolean therapistVisible,
            @RequestParam(required = false) Boolean clientPortalVisible
    ) {
        requireBillingModule();
        List<ServiceResponse> services = billingService.getServices(activeOnly, therapistVisible, clientPortalVisible);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/services/{id}")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    public ResponseEntity<ServiceResponse> getService(@PathVariable("id") Long id) {
        requireBillingModule();
        ServiceResponse service = billingService.getService(id);
        return ResponseEntity.ok(service);
    }

    @PostMapping("/services")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.BILLING_CREATE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create billing service",
            description = """
                    Create a new billing service.
                    
                    **Required Fields:**
                    - `serviceCode` (REQUIRED): Unique service code/identifier
                    - `serviceName` (REQUIRED): Service name
                    - `baseRate` (REQUIRED): Base rate/price for the service
                    
                    **Optional Fields:**
                    - `description` (optional): Service description
                    - `durationInMinutes` (optional): Duration of service in minutes
                    - `isActive` (optional, default: true): Whether the service is active
                    - `therapistVisible` (optional, default: true): Whether service is visible to therapists
                    - `clientPortalVisible` (optional, default: false): Whether service is visible in client portal
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Service information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateServiceRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Service Creation",
                                    value = """
                                            {
                                              "serviceCode": "PSY-60",
                                              "serviceName": "Psychotherapy Session - 60 minutes",
                                              "description": "Standard 60-minute psychotherapy session",
                                              "durationInMinutes": 60,
                                              "baseRate": 150.00,
                                              "isActive": true,
                                              "therapistVisible": true,
                                              "clientPortalVisible": false
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Service created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ServiceResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody CreateServiceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        ServiceResponse service = billingService.createService(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(service);
    }

    @PutMapping("/services/{id}")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.BILLING_EDIT)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update billing service",
            description = """
                    Update an existing billing service. **All fields are optional** - only include the fields you want to update.
                    
                    **Optional Fields (include only what you want to update):**
                    - `serviceCode`: Change the service code
                    - `serviceName`: Change the service name
                    - `description`: Update the description
                    - `durationInMinutes`: Change the duration
                    - `baseRate`: Update the base rate
                    - `isActive`: Enable/disable the service
                    - `therapistVisible`: Change therapist visibility
                    - `clientPortalVisible`: Change client portal visibility
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Service updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ServiceResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Service not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ServiceResponse> updateService(
            @io.swagger.v3.oas.annotations.Parameter(description = "Service ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @RequestBody UpdateServiceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        ServiceResponse service = billingService.updateService(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(service);
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.BILLING_DELETE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete billing service",
            description = """
                    Delete a billing service by ID.

                    **Important:**
                    - Service cannot be deleted if it is already used by sessions or billing records.

                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Service deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Service is in use and cannot be deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Service not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> deleteService(
            @io.swagger.v3.oas.annotations.Parameter(description = "Service ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        billingService.deleteService(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services/visibility/therapists/show-all")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.BILLING_EDIT)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Show all services to therapists",
            description = """
                    Bulk operation to set `therapistVisible = true` for all services.

                    **Effect:**
                    - All services become visible to therapists when booking or viewing sessions.
                    - Admins are always able to see all services regardless of this setting.

                    **Requires:** ADMIN role with billing edit permission.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> showAllServicesForTherapists(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        billingService.setAllServicesTherapistVisibility(true, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services/visibility/therapists/hide-all")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.BILLING_EDIT)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Hide all services from therapists",
            description = """
                    Bulk operation to set `therapistVisible = false` for all services.

                    **Effect:**
                    - All services become hidden from non-admin therapists when booking or viewing sessions.
                    - Admins can still see and manage all services.

                    **Requires:** ADMIN role with billing edit permission.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> hideAllServicesFromTherapists(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        billingService.setAllServicesTherapistVisibility(false, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/billing")
    @PreAuthorize(PermissionConstants.BILLING_CREATE_ACCESS)
    public ResponseEntity<SessionBillingResponse> createSessionBilling(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody CreateSessionBillingRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        request.setSessionId(sessionId);
        SessionBillingResponse billing = billingService.createSessionBilling(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(billing);
    }

    @GetMapping("/sessions/{sessionId}/billing")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    public ResponseEntity<SessionBillingResponse> getSessionBilling(@PathVariable("sessionId") Long sessionId) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.getSessionBilling(sessionId);
        return ResponseEntity.ok(billing);
    }

    @GetMapping("/billing")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "List billing records with filters",
            description = """
                    Get a paginated list of billing records with optional filters.
                    
                    **Query Parameters:**
                    
                    **Pagination (optional):**
                    - `page`: Page number (default: 0)
                    - `size`: Page size (default: 25, max: 200)
                    - `sort`: Sort field (default: "billingDate") — matches ClientHubAI (billing date only)
                    - `direction`: Sort direction - "asc" or "desc" (default: "desc")
                    
                    **Filters (all optional):**
                    - `clientId`: Filter by client ID
                    - `clientSearch`: Search by client name, MRN/client number, email, or phone (exact match)
                    - `therapistId`: Filter by therapist ID
                    - `status`: Unified billing status filter (pending, billed, paid, denied, refunded, follow_up)
                    - `serviceCode`: Filter by service code
                    - `clientType`: Filter by client type (Individual, Couple, Family, Group)
                    - `sessionType`: Filter by session mode (online, in-person)
                    - `paymentMethod`: Filter by payment method (cash, check, credit_card, debit_card, insurance, bank_transfer, online_payment, credit_balance)
                    - `startDate`: Filter from session date (yyyy-MM-dd), matching ClientHubAI
                    - `endDate`: Filter until session date (yyyy-MM-dd), matching ClientHubAI
                    - `minAmount`: Minimum total amount
                    - `maxAmount`: Maximum total amount
                    
                    **Example Request:**
                    ```
                    GET /api/v1/billing/billing?clientSearch=sarah&status=follow_up&startDate=2024-01-01&endDate=2024-12-31&page=0&size=25&sort=billingDate&direction=desc
                    ```
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, SUPERVISOR, or THERAPIST role.
                    Therapists only see billing for their assigned clients and sessions they conducted.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Billing records retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json"
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Page<SessionBillingResponse>> getBillingRecords(
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by client ID", example = "1")
            @RequestParam(required = false) Long clientId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Search by client name (partial match)", example = "sarah")
            @RequestParam(required = false) String clientSearch,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by therapist ID", example = "2")
            @RequestParam(required = false) Long therapistId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing status filter (pending, partial, paid, denied, refunded)", example = "pending")
            @RequestParam(required = false) String status,
            @io.swagger.v3.oas.annotations.Parameter(description = "Independent payment status: pending, partial, paid, failed, refunded")
            @RequestParam(required = false) String paymentStatus,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by service code", example = "PSY-60")
            @RequestParam(required = false) String serviceCode,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by client type (Individual, Couple, Family, Group)", example = "Individual")
            @RequestParam(required = false) String clientType,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by session mode (online, in-person)", example = "online")
            @RequestParam(required = false) String sessionType,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by payment method (cash, check, credit_card, debit_card, insurance, bank_transfer, online_payment, credit_balance)", example = "credit_card")
            @RequestParam(required = false) String paymentMethod,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter from session date (yyyy-MM-dd), matching ClientHubAI", example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter until session date (yyyy-MM-dd), matching ClientHubAI", example = "2024-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @io.swagger.v3.oas.annotations.Parameter(description = "Minimum total amount", example = "100.00")
            @RequestParam(required = false) BigDecimal minAmount,
            @io.swagger.v3.oas.annotations.Parameter(description = "Maximum total amount", example = "500.00")
            @RequestParam(required = false) BigDecimal maxAmount,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page size", example = "25")
            @RequestParam(required = false) Integer size,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sort field", example = "billingDate")
            @RequestParam(defaultValue = "billingDate") String sort,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sort direction (asc or desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireBillingModule();
        // Use configured defaults and validate page size
        if (size == null) {
            size = appProperties.getPagination().getDefaultPageSize();
        }
        int maxPageSize = appProperties.getPagination().getMaxPageSize();
        int minPageSize = appProperties.getPagination().getMinPageSize();
        int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
        
        if (size > maxPageSize) {
            size = maxPageSize;
        }
        if (size < minPageSize) {
            size = defaultPageSize;
        }

        Pageable pageable = PageRequest.of(page, size, resolveBillingListSort(sort, direction));

        Page<SessionBillingResponse> billingPage = billingService.getBillingRecords(
                clientId, clientSearch, therapistId, status, paymentStatus, serviceCode, clientType, sessionType, paymentMethod,
                startDate, endDate, minAmount, maxAmount, pageable, principal
        );
        return ResponseEntity.ok(billingPage);
    }

    /**
     * Primary DB sort hint for billing list. Real ClientHub same-day order is applied in
     * {@code BillingService} ({@code billing_date} then ClientHub billing id, same direction).
     */
    public static Sort resolveBillingListSort(String sort, String direction) {
        Sort.Direction primaryDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String field = (sort == null || sort.isBlank()) ? "billingDate" : sort.trim();

        if ("sessionDate".equalsIgnoreCase(field)) {
            field = "billingDate";
        }

        if ("billingDate".equalsIgnoreCase(field)) {
            return Sort.by(
                    new Sort.Order(primaryDirection, "billingDate"),
                    // Same direction as primary so Spring never flips newest-first lists to id ASC.
                    new Sort.Order(primaryDirection, "id")
            );
        }

        return Sort.by(new Sort.Order(primaryDirection, field));
    }

    @PatchMapping("/billing/{id}/payment-status")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    public ResponseEntity<SessionBillingResponse> updatePaymentStatus(
            @PathVariable("id") Long id,
            @RequestParam String paymentStatus,
            @RequestParam(required = false) String stripePaymentIntentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.updatePaymentStatus(
                id, paymentStatus, stripePaymentIntentId, principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(billing);
    }

    @PatchMapping("/billing/{id}/discount")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Apply discount to session billing",
            description = """
                    Apply a discount to an existing session billing record. Supports percentage-based and fixed-amount discounts.
                    
                    **Discount Types:**
                    - `percentage`: Discount as a percentage of total amount (0-100%). Example: 15.00 for 15% off
                    - `fixed`: Fixed dollar amount discount. Example: 50.00 for $50 off
                    - `none`: Remove any existing discount
                    
                    **Calculation:**
                    - For percentage: `discountAmount = totalAmount × (discountValue / 100)`
                    - For fixed: `discountAmount = discountValue` or `discountAmount` (whichever is provided)
                    - Final amount due: `amountDue = totalAmount - discountAmount`
                    
                    **Validation:**
                    - Percentage must be between 0 and 100
                    - Discount amount cannot exceed total amount
                    - Discount values must be positive
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    
                    **Audit:** All discount applications are logged for compliance.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Discount applied successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionBillingResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Percentage Discount Applied",
                                    value = """
                                            {
                                              "id": 123,
                                              "sessionId": 456,
                                              "serviceCode": "90834",
                                              "serviceName": "Individual Therapy - 45 min",
                                              "totalAmount": 150.00,
                                              "discountType": "percentage",
                                              "discountValue": 15.00,
                                              "discountAmount": 22.50,
                                              "amountDue": 127.50,
                                              "paymentStatus": "pending",
                                              "insuranceCovered": false
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid discount parameters (e.g., percentage > 100, negative values)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Insufficient permissions - requires BILLING_SPECIALIST, ADMIN, or SUPERVISOR role"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Session billing record not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Discount details to apply",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApplyDiscountRequest.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "15% Discount",
                                    value = """
                                            {
                                              "discountType": "percentage",
                                              "discountValue": 15.00
                                            }
                                            """
                            ),
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "$50 Fixed Discount",
                                    value = """
                                            {
                                              "discountType": "fixed",
                                              "discountAmount": 50.00
                                            }
                                            """
                            ),
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Remove Discount",
                                    value = """
                                            {
                                              "discountType": "none"
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<SessionBillingResponse> applyDiscount(
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "ID of the session billing record",
                    required = true,
                    example = "123"
            )
            @PathVariable("id") Long id,
            @RequestBody ApplyDiscountRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.applyDiscount(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(billing);
    }

    @GetMapping("/statistics")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get billing statistics",
            description = """
                    Get billing statistics including:
                    - Outstanding balance
                    - Total collected
                    - Active clients
                    - Total billing records
                    - Pending/paid/denied/follow-up counts
                    
                    Optional `startDate` / `endDate` (yyyy-MM-dd) limit stats to billing dates in that range.
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, SUPERVISOR, or THERAPIST role.
                    Therapists only see billing for their assigned clients and sessions they conducted.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<BillingStatisticsResponse> getBillingStatistics(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String clientSearch,
            @RequestParam(required = false) Long therapistId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String clientType,
            @RequestParam(required = false) String sessionType,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireBillingModule();
        BillingStatisticsResponse statistics = billingService.getBillingStatistics(principal, clientId, clientSearch, therapistId,
                status, paymentStatus, serviceCode, clientType, sessionType, paymentMethod,
                startDate, endDate, minAmount, maxAmount);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/clients/{clientId}/stats")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get billing stats for a specific client",
            description = """
                    Get client-specific billing statistics:
                    - total invoices
                    - counts by billing status
                    - total billed amount
                    - total paid amount
                    - due amount
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientBillingStatsResponse> getClientBillingStats(
            @PathVariable Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireBillingModule();
        return ResponseEntity.ok(billingService.getClientBillingStats(clientId, principal));
    }

    @GetMapping("/history")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get billing history",
            description = """
                    Get billing history with optional filters.
                    
                    **Query Parameters (all optional):**
                    - `clientId`: Filter by client ID
                    - `therapistId`: Filter by therapist ID
                    - `paymentStatus`: Filter by payment status (pending, paid, partial, failed, refunded)
                    - `billingStatus`: Filter by billing status (pending, billed, paid, denied, follow_up, cancelled)
                    - `startDate`: Filter from billing date (yyyy-MM-dd)
                    - `endDate`: Filter until billing date (yyyy-MM-dd)
                    - `page`: Page number (1-based, default: 1)
                    - `limit`: Page size (default from app config)
                    
                    Results are sorted by billing date descending (newest first), matching ClientHubAI.
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, SUPERVISOR, or THERAPIST role.
                    Therapists only see billing for their assigned clients and sessions they conducted.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PaginatedResponse<BillingHistoryResponse>> getBillingHistory(
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by client ID", example = "1")
            @RequestParam(required = false) Long clientId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by therapist ID", example = "2")
            @RequestParam(required = false) Long therapistId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by payment status", example = "pending")
            @RequestParam(required = false) String paymentStatus,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter by billing status", example = "billed")
            @RequestParam(required = false) String billingStatus,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter from billing date (yyyy-MM-dd)", example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @io.swagger.v3.oas.annotations.Parameter(description = "Filter until billing date (yyyy-MM-dd)", example = "2024-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page size", example = "10")
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireBillingModule();
        int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
        PaginatedResponse<BillingHistoryResponse> history = billingService.getBillingHistory(
                clientId, therapistId, paymentStatus, billingStatus, startDate, endDate,
                page, limit != null ? limit : defaultPageSize, principal
        );
        return ResponseEntity.ok(history);
    }

    @PostMapping("/billing/{id}/record-payment")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Record payment for billing record",
            description = """
                    Record a payment for a billing record.
                    
                    **Required Fields:**
                    - `paymentAmount`: Payment amount (must be positive)
                    - `paymentMethod`: Payment method (CASH, CHECK, CREDIT_CARD, DEBIT_CARD, INSURANCE, BANK_TRANSFER, ONLINE_PAYMENT, CREDIT_BALANCE)
                    
                    **Optional Fields:**
                    - `referenceNumber`: Reference number (transaction ID, check number, etc.)
                    - `notes`: Additional payment notes
                    
                    **Payment Status Logic:**
                    - If cumulative paid amount >= amount due (after discounts): Billing status set to PAID
                    - If cumulative paid amount < amount due: Billing status set to BILLED (partial payment)
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SessionBillingResponse> recordPayment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @Valid @RequestBody RecordPaymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.recordPayment(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(billing);
    }

    @PostMapping("/billing/{id}/record-split-payment")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    public ResponseEntity<SessionBillingResponse> recordSplitPayment(
            @PathVariable("id") Long id,
            @RequestBody RecordSplitPaymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.recordSplitPayment(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(billing);
    }

    @GetMapping("/billing/{id}/payment-guidance")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    public ResponseEntity<PaymentGuidanceResponse> getPaymentGuidance(
            @PathVariable("id") Long id
    ) {
        requireBillingModule();
        return ResponseEntity.ok(billingService.getPaymentGuidance(id));
    }

    @PatchMapping("/billing/{id}/payments/{paymentId}")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    public ResponseEntity<SessionBillingResponse> editPayment(
            @PathVariable("id") Long id,
            @PathVariable Long paymentId,
            @Valid @RequestBody EditPaymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.editPayment(
                id, paymentId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(billing);
    }

    @PostMapping("/billing/{id}/refund-payment")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Refund payment for billing record",
            description = """
                    Refund a payment for a billing record.
                    
                    **Required Fields:**
                    - `refundAmount`: Refund amount (must be positive)
                    
                    **Optional Fields:**
                    - `referenceNumber`: Refund reference number
                    - `paymentMethod`: Refund method (defaults to BANK_TRANSFER)
                    - `notes`: Refund reason/details
                    
                    **Validation Rules:**
                    - Refund amount cannot exceed currently paid balance for this invoice
                    - Invoice paid/outstanding amounts are recalculated immediately
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SessionBillingResponse> refundPayment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @Valid @RequestBody RefundPaymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.refundPayment(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(billing);
    }

    @GetMapping("/billing/{id}/refunds")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get refund history for billing record",
            description = """
                    Returns all refund ledger entries for the billing record in descending payment date order.
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<RefundHistoryResponse> getRefundHistory(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id
    ) {
        requireBillingModule();
        return ResponseEntity.ok(billingService.getRefundsForBilling(id));
    }

    @GetMapping("/billing/{id}/transactions")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get payment transactions for billing record",
            description = """
                    Returns provider transaction records (manual/stripe/etc.) for the billing record
                    in descending creation order.

                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<PaymentTransactionResponse>> getBillingTransactions(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id
    ) {
        requireBillingModule();
        return ResponseEntity.ok(billingService.getTransactionsForBilling(id));
    }

    @PostMapping("/billing/{id}/transactions/{transactionId}/void")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Void a payment transaction",
            description = """
                    Voids a payment transaction and recalculates the billing record totals/status.
                    This action is intended for accidental/duplicate manual entries.

                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SessionBillingResponse> voidBillingTransaction(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @io.swagger.v3.oas.annotations.Parameter(description = "Payment transaction ID", required = true, example = "987")
            @PathVariable("transactionId") Long transactionId,
            @Valid @RequestBody VoidPaymentTransactionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        SessionBillingResponse billing = billingService.voidPaymentTransaction(
                id,
                transactionId,
                request,
                principal,
                HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(billing);
    }

    @PatchMapping("/billing/{id}/status")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Change billing status",
            description = """
                    Change the billing status of a billing record.
                    
                    **Required Fields:**
                    - `billingStatus`: New billing status (pending, billed, paid, denied, follow_up)
                    
                    **Optional Fields:**
                    - `notes`: Notes for status change
                    
                    **Status Values:**
                    - `pending`: Pending - Not yet billed
                    - `billed`: Billed - Invoice sent
                    - `paid`: Paid - Payment received
                    - `denied`: Denied - Payment/claim denied
                    - `follow_up`: Follow Up Required
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SessionBillingResponse> changeBillingStatus(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangeBillingStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SessionBillingResponse billing = billingService.changeBillingStatus(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(billing);
    }

    @PostMapping("/billing/{id}/send-invoice-email")
    @PreAuthorize(PermissionConstants.BILLING_WRITE_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Send invoice email to client",
            description = """
                    Send invoice email to the client associated with this billing record.
                    The invoice will be sent to the client's primary email address.
                    Billing status will be updated to BILLED if currently PENDING.
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> sendInvoiceEmail(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        billingService.sendInvoiceEmail(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/billing/{id}/invoice-preview")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Preview invoice HTML",
            description = """
                    Get HTML preview of the invoice for a billing record.
                    Returns HTML that can be displayed or converted to PDF.
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, SUPERVISOR, or THERAPIST role.
                    Therapists only see billing for their assigned clients and sessions they conducted.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Map<String, String>> getInvoicePreview(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        String html = billingService.getInvoicePreview(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(Map.of("html", html));
    }

    @GetMapping("/billing/{id}/invoice-download")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Download invoice",
            description = """
                    Download invoice for a billing record.
                    Currently returns HTML that can be converted to PDF by frontend.
                    Future: Will return PDF directly.
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, SUPERVISOR, or THERAPIST role.
                    Therapists only see billing for their assigned clients and sessions they conducted.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<String> downloadInvoice(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        String html = billingService.getInvoiceDownload(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"invoice-" + id + ".html\"");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(html);
    }

    @GetMapping("/billing/{id}/invoice")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Download invoice PDF",
            description = """
                    Download invoice as PDF for a billing record.
                    Includes practice configuration details and therapist license information.
                    
                    **Requires:** BILLING_SPECIALIST, ADMIN, SUPERVISOR, or THERAPIST role.
                    Therapists only see billing for their assigned clients and sessions they conducted.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @io.swagger.v3.oas.annotations.Parameter(description = "Billing record ID", required = true, example = "123")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        requireBillingModule();
        byte[] pdf = billingService.getInvoicePdf(id, principal, HttpRequestUtil.getClientIp(httpRequest));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

}
