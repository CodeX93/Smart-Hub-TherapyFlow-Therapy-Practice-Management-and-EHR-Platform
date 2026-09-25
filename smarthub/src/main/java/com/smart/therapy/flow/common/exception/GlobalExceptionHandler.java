package com.smart.therapy.flow.common.exception;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.logging.SensitiveDataMasker;
import com.smart.therapy.flow.common.metrics.AuthAbuseMetrics;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.smart.therapy.flow.client.enums.EducationLevel;
import com.smart.therapy.flow.client.enums.EmploymentStatus;
import com.smart.therapy.flow.common.util.EnumParsing;
import org.slf4j.MDC;
import org.hibernate.LazyInitializationException;
import org.hibernate.HibernateException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Pattern RAW_DB_ERROR_PATTERN = Pattern.compile(
            "(?i)key \\([^)]*\\)\\s*=\\s*\\([^)]*\\)|detail:|violates (unique|foreign key|not-null|check) constraint|duplicate key value");

    private final AuditLogService auditLogService;
    private final SensitiveDataMasker sensitiveDataMasker;
    private final AuthAbuseMetrics authAbuseMetrics;

    private String getTraceId() {
        String traceId = MDC.get("correlationId");
        return traceId != null ? traceId : "unknown";
    }
    
    private String getPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "unknown";
    }

    /**
     * Fail-soft HIPAA audit for 403 responses raised via thrown exceptions (as opposed to Spring
     * Security's AccessDeniedHandler, which covers authorization failures at the filter chain level).
     */
    private void auditForbidden(HttpServletRequest request, String message) {
        try {
            authAbuseMetrics.incrementForbidden();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = null;
            if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal authPrincipal) {
                username = authPrincipal.getLoginIdentifier();
            }
            String path = getPath(request);
            String ipAddress = request != null ? HttpRequestUtil.getClientIp(request) : null;
            String userAgent = request != null ? HttpRequestUtil.getUserAgent(request) : null;

            Map<String, Object> details = new HashMap<>();
            details.put("method", request != null ? request.getMethod() : null);
            details.put("message", message);
            details.put("traceId", getTraceId());

            auditLogService.logUnauthorizedAccess(null, username, "endpoint", path, ipAddress, userAgent, details);
        } catch (Exception e) {
            log.warn("Failed to record unauthorized_access audit event: {}", e.getMessage());
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error("Unsupported Media Type")
                .message("Content type is not supported for this endpoint")
                .code(ErrorCode.BAD_REQUEST.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .code(ErrorCode.BAD_REQUEST.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .code(ErrorCode.AUTH_UNAUTHORIZED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PortalAccessDisabledException.class)
    public ResponseEntity<ErrorResponse> handlePortalAccessDisabledException(
            PortalAccessDisabledException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .code(ErrorCode.CLIENT_PORTAL_DISABLED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PortalActivationPendingException.class)
    public ResponseEntity<ErrorResponse> handlePortalActivationPendingException(
            PortalActivationPendingException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .code(ErrorCode.CLIENT_PORTAL_ACTIVATION_PENDING.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex, HttpServletRequest request) {
        auditForbidden(request, ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .code(ErrorCode.AUTH_FORBIDDEN.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AiConsentRequiredException.class)
    public ResponseEntity<ErrorResponse> handleAiConsentRequiredException(
            AiConsentRequiredException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .code(ErrorCode.AUTH_FORBIDDEN.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(ex.getDetails())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        // SECURITY: Don't expose detailed authentication error messages to clients
        // But log more details for debugging (exception class and message if available)
        String errorMsg = ex.getMessage() != null ? ex.getMessage() : "No error message";
        String exceptionType = ex.getClass().getSimpleName();
        log.warn("Authentication failed: traceId={}, path={}, exceptionType={}, error={}", 
                getTraceId(), getPath(request), exceptionType, errorMsg);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Authentication failed")
                .code(ErrorCode.AUTH_UNAUTHORIZED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Invalid credentials")
                .code(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        String path = getPath(request);
        String message = AccessDeniedMessageResolver.resolve(request);
        
        log.warn("Access denied: traceId={}, path={}, message={}", getTraceId(), path, message);
        auditForbidden(request, message);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(message)
                .code(ErrorCode.AUTH_FORBIDDEN.getCode())
                .path(path)
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex, HttpServletRequest request) {
        Map<String, Object> details = null;
        if (ex.getConflictDetails() instanceof Map<?, ?> conflictMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) conflictMap;
            details = typed;
        } else if (ex.getConflictDetails() != null) {
            details = Map.of("conflict", ex.getConflictDetails());
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .code(ErrorCode.SESSION_CONFLICT_DETECTED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(details)
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        authAbuseMetrics.incrementRateLimited();
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message(ex.getMessage())
                .code(ErrorCode.BAD_REQUEST.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    @ExceptionHandler(TranscriptionServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTranscriptionServiceUnavailableException(
            TranscriptionServiceUnavailableException ex, HttpServletRequest request) {
        log.error("Transcription service unavailable: traceId={}, path={}, category={}, message={}",
                getTraceId(), getPath(request), ex.getCategory(), ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(buildTranscriptionErrorDetails(ex.getCategory(), ex.isRetryable()))
                .build();
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(TranscriptionFailedException.class)
    public ResponseEntity<ErrorResponse> handleTranscriptionFailedException(
            TranscriptionFailedException ex, HttpServletRequest request) {
        log.error("Transcription failed: traceId={}, path={}, category={}, message={}",
                getTraceId(), getPath(request), ex.getCategory(), ex.getMessage(), ex);
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_GATEWAY;
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(buildTranscriptionErrorDetails(ex.getCategory(), ex.isRetryable()))
                .build();
        return new ResponseEntity<>(error, status);
    }

    private static Map<String, Object> buildTranscriptionErrorDetails(String category, boolean retryable) {
        Map<String, Object> details = new HashMap<>();
        details.put("provider", "openai");
        details.put("retryable", retryable);
        if (category != null) {
            details.put("category", category);
        }
        return details;
    }

    /**
     * Falls back to a generic message when a transaction-failure cause looks like a raw DB
     * error (e.g. "Key (email)=(phi@x.com) already exists", "Detail: ..."), which may embed PHI
     * from unique/foreign-key/check constraint values. Recognized DataIntegrityViolationException
     * causes are handled earlier via {@link DataIntegrityMessageResolver}; this is the fallback
     * for other cause types that could still carry a raw driver message.
     */
    private static String safeTransactionCauseMessage(Throwable cause) {
        String fallback = "The request could not be completed. Please check your input and try again.";
        if (cause == null || cause.getMessage() == null) {
            return fallback;
        }
        String message = cause.getMessage();
        return RAW_DB_ERROR_PATTERN.matcher(message).find() ? fallback : message;
    }

    private static String resolveRootCauseMessage(Throwable ex) {
        Throwable current = ex;
        Throwable root = ex;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
            root = current;
        }
        return root != null && root.getMessage() != null ? root.getMessage() : null;
    }

    @ExceptionHandler(TenantUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTenantUnavailable(
            TenantUnavailableException ex, HttpServletRequest request) {
        String code = "TENANT_002".equals(ex.getCode()) ? ErrorCode.TENANT_FORCE_DISABLED.getCode() : ErrorCode.TENANT_MAINTENANCE.getCode();
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Tenant Unavailable")
                .message(ex.getMessage())
                .code(code)
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String primaryMessage = errors.isEmpty()
                ? "Invalid input"
                : errors.values().iterator().next();

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(primaryMessage)
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(Map.of("errors", errors))
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "unknown";
            errors.put(field, violation.getMessage());
        }

        String primaryMessage = errors.isEmpty()
                ? "Invalid input"
                : errors.values().iterator().next();

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(primaryMessage)
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(Map.of("errors", errors))
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
            if ("com.smart.therapy.flow.system.dto.UpdateSystemOptionRequest"
                    .equals(unrecognizedPropertyException.getReferringClass().getName())
                    && "isSystem".equals(unrecognizedPropertyException.getPropertyName())) {
                ErrorResponse specificError = ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message("Field 'isSystem' cannot be updated. Remove 'isSystem' from the request body and retry.")
                        .code(ErrorCode.BAD_REQUEST.getCode())
                        .path(getPath(request))
                        .traceId(getTraceId())
                        .build();
                return new ResponseEntity<>(specificError, HttpStatus.BAD_REQUEST);
            }
        }

        Map<String, Object> details = null;
        String message = "Malformed JSON request body. Please send valid JSON.";

        ResolvedJsonReadError resolvedJsonError = resolveJsonReadError(ex);
        if (resolvedJsonError != null) {
            message = resolvedJsonError.message();
            details = resolvedJsonError.details();
        } else if (cause instanceof InvalidFormatException invalidFormatException) {
            String fieldPath = buildFieldPath(invalidFormatException.getPath());
            Class<?> targetType = invalidFormatException.getTargetType();
            Object rejectedValue = invalidFormatException.getValue();
            message = buildInvalidFormatMessage(fieldPath, targetType, rejectedValue);
            details = buildInvalidFormatDetails(fieldPath, targetType, rejectedValue);
        } else if (cause instanceof JsonMappingException jsonMappingException) {
            String fieldPath = buildFieldPath(jsonMappingException.getPath());
            message = buildJsonMappingMessage(fieldPath, jsonMappingException);
            details = buildJsonMappingDetails(fieldPath, jsonMappingException);
        } else if (cause instanceof MismatchedInputException mismatchedInputException) {
            String fieldPath = buildFieldPath(mismatchedInputException.getPath());
            message = String.format("Invalid structure for field '%s'.", fieldPath);
            details = Map.of("field", fieldPath);
        } else if (cause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
            String property = unrecognizedPropertyException.getPropertyName();
            message = String.format("Unknown field '%s' in request body.", property);
            details = Map.of("field", property);
        } else if (cause instanceof JsonParseException jsonParseException) {
            var location = jsonParseException.getLocation();
            if (location != null) {
                details = Map.of(
                        "line", location.getLineNr(),
                        "column", location.getColumnNr());
            }
        }

        log.warn("Malformed JSON request: traceId={}, path={}, error={}",
                getTraceId(), getPath(request), ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        String errorCode = details != null && details.containsKey("field")
                ? ErrorCode.VALIDATION_FAILED.getCode()
                : ErrorCode.BAD_REQUEST.getCode();
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(details != null && details.containsKey("errors") ? "Validation Failed" : "Bad Request")
                .message(message)
                .code(errorCode)
                .path(getPath(request))
                .traceId(getTraceId())
                .details(details)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private record ResolvedJsonReadError(String message, Map<String, Object> details) {
    }

    private ResolvedJsonReadError resolveJsonReadError(HttpMessageNotReadableException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                String fieldPath = buildFieldPath(invalidFormatException.getPath());
                Object rejectedValue = invalidFormatException.getValue();
                Class<?> targetType = invalidFormatException.getTargetType();
                return new ResolvedJsonReadError(
                        buildInvalidFormatMessage(fieldPath, targetType, rejectedValue),
                        buildInvalidFormatDetails(fieldPath, targetType, rejectedValue));
            }
            if (current instanceof JsonMappingException jsonMappingException) {
                String fieldPath = buildFieldPath(jsonMappingException.getPath());
                return new ResolvedJsonReadError(
                        buildJsonMappingMessage(fieldPath, jsonMappingException),
                        buildJsonMappingDetails(fieldPath, jsonMappingException));
            }
            if (current instanceof ValueInstantiationException valueInstantiationException) {
                String fieldPath = buildFieldPath(valueInstantiationException.getPath());
                Throwable root = valueInstantiationException.getCause();
                if (root instanceof IllegalArgumentException illegalArgumentException
                        && illegalArgumentException.getMessage() != null) {
                    return new ResolvedJsonReadError(
                            illegalArgumentException.getMessage(),
                            Map.of("field", fieldPath));
                }
            }
            if (current instanceof IllegalArgumentException illegalArgumentException
                    && illegalArgumentException.getMessage() != null
                    && illegalArgumentException.getMessage().startsWith("Invalid ")) {
                return new ResolvedJsonReadError(
                        illegalArgumentException.getMessage(),
                        Map.of("message", illegalArgumentException.getMessage()));
            }
            current = current.getCause();
        }
        return null;
    }

    private String buildInvalidFormatMessage(String fieldPath, Class<?> targetType, Object rejectedValue) {
        Object safeValue = sensitiveDataMasker.maskFieldValue(fieldPath, rejectedValue);
        if (targetType != null && targetType.isEnum()) {
            String allowed = enumAllowedValues(targetType);
            return String.format(
                    "Invalid value '%s' for field '%s'. Allowed values: %s.",
                    safeValue,
                    fieldPath,
                    allowed);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return buildIntegerFieldMessage(fieldPath);
        }
        return String.format(
                "Invalid value '%s' for field '%s'. Expected %s.",
                safeValue,
                fieldPath,
                targetType != null ? targetType.getSimpleName() : "a different type");
    }

    private String buildJsonMappingMessage(String fieldPath, JsonMappingException ex) {
        String raw = ex.getOriginalMessage() != null ? ex.getOriginalMessage() : ex.getMessage();
        if (raw != null) {
            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.contains("out of range") || lower.contains("not a valid integer")
                    || lower.contains("cannot deserialize value of type `java.lang.integer`")) {
                return buildIntegerFieldMessage(fieldPath);
            }
            if (lower.contains("cannot deserialize value of type `java.lang.string`")
                    && lower.contains("from object value")) {
                return String.format(
                        "Invalid value for field '%s'. Expected a text value, not an object.",
                        fieldPath);
            }
            if (lower.contains("cannot deserialize value of type `java.time.localdate`")) {
                return String.format(
                        "Invalid value for field '%s'. Expected a date in yyyy-MM-dd format.",
                        fieldPath);
            }
            if (lower.contains("cannot deserialize")) {
                return String.format("Invalid value for field '%s'. %s", fieldPath, raw);
            }
        }
        return String.format("Invalid value for field '%s'. Please check your input and try again.", fieldPath);
    }

    private Map<String, Object> buildJsonMappingDetails(String fieldPath, JsonMappingException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("field", fieldPath);
        details.put("errors", Map.of(fieldPath, buildJsonMappingMessage(fieldPath, ex)));
        return details;
    }

    private String buildIntegerFieldMessage(String fieldPath) {
        if ("capacity".equals(fieldPath)) {
            return "Capacity must be a whole number between 1 and 1000.";
        }
        return String.format(
                "Invalid number for field '%s'. Please enter a valid whole number.",
                fieldPath);
    }

    private Map<String, Object> buildInvalidFormatDetails(String fieldPath, Class<?> targetType, Object rejectedValue) {
        Map<String, Object> details = new HashMap<>();
        String message = buildInvalidFormatMessage(fieldPath, targetType, rejectedValue);
        details.put("field", fieldPath);
        details.put("rejectedValue", sensitiveDataMasker.maskFieldValue(fieldPath, rejectedValue));
        details.put("errors", Map.of(fieldPath, message));
        if (targetType != null) {
            details.put("expectedType", targetType.getSimpleName());
            if (targetType.isEnum()) {
                details.put("allowedValues", enumAllowedValues(targetType));
            }
        }
        return details;
    }

    private String enumAllowedValues(Class<?> enumType) {
        if (enumType == EmploymentStatus.class) {
            return EnumParsing.allowedValues(EmploymentStatus.class, EmploymentStatus::getDisplayName);
        }
        if (enumType == EducationLevel.class) {
            return EnumParsing.allowedValues(EducationLevel.class, EducationLevel::getDisplayName);
        }
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return "see API documentation";
        }
        return Arrays.stream(constants)
                .map(constant -> ((Enum<?>) constant).name())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String buildFieldPath(java.util.List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "requestBody";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonMappingException.Reference reference : path) {
            if (reference.getFieldName() != null) {
                if (builder.length() > 0) {
                    builder.append('.');
                }
                builder.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                builder.append('[').append(reference.getIndex()).append(']');
            }
        }
        return builder.length() > 0 ? builder.toString() : "requestBody";
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseSchemaException(
            InvalidDataAccessResourceUsageException ex, HttpServletRequest request) {
        String errorMessage = ex.getMessage();
        String userMessage = "A database schema error occurred. This usually indicates that the database needs to be updated with the latest migrations. Please contact the system administrator.";
        
        // Extract column/table name from error message if possible
        if (errorMessage != null) {
            if (errorMessage.contains("does not exist")) {
                if (errorMessage.contains("column")) {
                    userMessage = "Database schema is out of date. A required column is missing. Please ensure all database migrations have been applied. Contact the system administrator if this issue persists.";
                } else if (errorMessage.contains("relation") || errorMessage.contains("table")) {
                    userMessage = "Database schema is out of date. A required table is missing. Please ensure all database migrations have been applied. Contact the system administrator if this issue persists.";
                }
            }
        }
        
        log.error("Database schema error: traceId={}, path={}, error={}", getTraceId(), getPath(request), errorMessage, ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Database Schema Error")
                .message(userMessage)
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("The requested endpoint or resource was not found.")
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnexpectedRollbackException.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedRollback(
            UnexpectedRollbackException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof BadRequestException badRequestException) {
            return handleBadRequestException(badRequestException, request);
        }
        if (cause instanceof ConstraintViolationException constraintViolationException) {
            return handleConstraintViolationException(constraintViolationException, request);
        }
        if (cause instanceof DataIntegrityViolationException dataIntegrityViolationException) {
            return handleDataIntegrity(dataIntegrityViolationException, request);
        }

        log.warn("Transaction rolled back: traceId={}, path={}, cause={}",
                getTraceId(), getPath(request), cause != null ? cause.getMessage() : ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(safeTransactionCauseMessage(cause))
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystemException(
            TransactionSystemException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof ConstraintViolationException constraintViolationException) {
            return handleConstraintViolationException(constraintViolationException, request);
        }
        if (cause instanceof DataIntegrityViolationException dataIntegrityViolationException) {
            return handleDataIntegrity(dataIntegrityViolationException, request);
        }

        log.error("Transaction failed: traceId={}, path={}", getTraceId(), getPath(request), ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(safeTransactionCauseMessage(cause))
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: traceId={}, path={}", getTraceId(), getPath(request), ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later or contact support if the problem persists.")
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        log.error("Entity not found: {}, traceId={}", ex.getMessage(), getTraceId());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Entity Not Found")
                .message(ex.getMessage())
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, HttpServletRequest request) {
        log.error("Validation error: {}, traceId={}", ex.getMessage(), getTraceId());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogic(
            BusinessLogicException ex, HttpServletRequest request) {
        log.error("Business logic error: {}, traceId={}", ex.getMessage(), getTraceId());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Business Logic Error")
                .message(ex.getMessage())
                .code(ErrorCode.BUSINESS_LOGIC_ERROR.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(StoryApiException.class)
    public ResponseEntity<ErrorResponse> handleStoryApiException(
            StoryApiException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .code(ex.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(ex.getDetails())
                .build();
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus());
        if (ex.getRetryAfterSeconds() != null && ex.getRetryAfterSeconds() > 0) {
            response.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        }
        return response.body(error);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(
            DatabaseException ex, HttpServletRequest request) {
        log.error("Database error: {}, traceId={}", ex.getMessage(), getTraceId(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Database Error")
                .message("A database error occurred")
                .code(ErrorCode.DB_CONNECTION_ERROR.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        DataIntegrityMessageResolver.ResolvedViolation resolved =
                DataIntegrityMessageResolver.resolve(ex);

        if (resolved.status() == HttpStatus.BAD_REQUEST
                && resolved.message().contains("Invalid JSON format")) {
            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message(resolved.message() + " Example: {\"minLength\":2,\"maxLength\":100}.")
                    .code(ErrorCode.BAD_REQUEST.getCode())
                    .path(getPath(request))
                    .traceId(getTraceId())
                    .details(Map.of(
                            "hint",
                            "Fields like 'validation' and 'conditionalDisplay' must be valid JSON strings; single quotes are not allowed."))
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        log.warn("Data integrity violation: {}, resolvedMessage={}, traceId={}",
                ex.getMessage(), resolved.message(), getTraceId());

        Map<String, Object> details = new HashMap<>();
        if (resolved.field() != null) {
            details.put("field", resolved.field());
            details.put("errors", Map.of(resolved.field(), resolved.message()));
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(resolved.status().value())
                .error(resolved.status() == HttpStatus.CONFLICT ? "Conflict" : "Bad Request")
                .message(resolved.message())
                .code(resolved.code())
                .path(getPath(request))
                .traceId(getTraceId())
                .details(details.isEmpty() ? null : details)
                .build();
        return ResponseEntity.status(resolved.status()).body(error);
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<ErrorResponse> handleLazyInitializationException(
            LazyInitializationException ex, HttpServletRequest request) {
        log.error("LazyInitializationException: traceId={}, path={}, error={}", getTraceId(), getPath(request), ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Data Loading Error")
                .message("An error occurred while loading related data. Please try again or contact support if the problem persists.")
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(HibernateException.class)
    public ResponseEntity<ErrorResponse> handleHibernateException(
            HibernateException ex, HttpServletRequest request) {
        log.error("HibernateException: traceId={}, path={}, error={}", getTraceId(), getPath(request), ex.getMessage(), ex);
        
        // Check if it's a LazyInitializationException (already handled above, but as fallback)
        if (ex instanceof LazyInitializationException) {
            return handleLazyInitializationException((LazyInitializationException) ex, request);
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Database Error")
                .message("A database error occurred while processing your request. Please try again or contact support if the problem persists.")
                .code(ErrorCode.DB_CONNECTION_ERROR.getCode())
                .path(getPath(request))
                .traceId(getTraceId())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
