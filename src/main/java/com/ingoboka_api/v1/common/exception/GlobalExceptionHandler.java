package com.ingoboka_api.v1.common.exception;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.enums.AuditOutcome;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;
import com.ingoboka_api.v1.platform.services.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final PlatformSettingsService platformSettingsService;
    private final AuditComplianceService auditComplianceService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        if (ex instanceof DisabledException || ex instanceof LockedException) {
            return handleAccountState(ex);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccountState(RuntimeException ex) {
        if (ex instanceof DisabledException) {
            var config = platformSettingsService.getEffectiveConfig();
            String message = String.format(
                    "Your account has been deactivated. Contact support at %s or %s for assistance.",
                    config.getSupportEmail(), config.getSupportPhone());
            auditComplianceService.logSystem(
                    AuditOutcome.FAILED,
                    "USER_LOGIN_FAILED",
                    "USER",
                    null,
                    "Account deactivated",
                    "unknown");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(message, "ACCOUNT_DISABLED"));
        }
        auditComplianceService.logSystem(
                AuditOutcome.FAILED,
                "USER_LOGIN_FAILED",
                "USER",
                null,
                ex.getMessage() != null ? ex.getMessage() : "Account locked",
                "unknown");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), "ACCOUNT_LOCKED"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Access denied";
        auditComplianceService.logSystem(
                AuditOutcome.FAILED,
                "ACCESS_DENIED",
                "REQUEST",
                null,
                message,
                safeActorEmail());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message, "ACCESS_DENIED"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String message = "Request conflicts with existing data";
        String detail = ex.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("idx_consents_active_user_type")) {
            message = "An active consent of this type already exists";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    /**
     * Client closed the connection (Swagger assets, slow networks). Not an app bug —
     * do not try to write a JSON body onto a response that already has another Content-Type.
     */
    @ExceptionHandler({
        ClientAbortException.class,
        AsyncRequestNotUsableException.class
    })
    public ResponseEntity<Void> handleClientGone(Exception ex) {
        log.debug("Client aborted request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Void> handleIoException(IOException ex, HttpServletRequest request) {
        if (isClientAbort(ex)) {
            log.debug("Client aborted request {}: {}", request.getRequestURI(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
        }
        log.error("Unhandled IOException on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        if (isClientAbort(ex)) {
            log.debug("Client aborted request: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("An unexpected error occurred"));
    }

    private static boolean isClientAbort(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ClientAbortException
                    || current instanceof AsyncRequestNotUsableException) {
                return true;
            }
            String name = current.getClass().getName();
            if (name.contains("ClientAbortException")
                    || name.contains("AsyncRequestNotUsableException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String safeActorEmail() {
        try {
            return SecurityUtils.currentUser().getEmail();
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
