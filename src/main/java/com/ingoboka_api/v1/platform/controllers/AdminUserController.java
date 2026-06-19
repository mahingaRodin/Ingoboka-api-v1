package com.ingoboka_api.v1.platform.controllers;

import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.requests.CreateManagedUserRequest;
import com.ingoboka_api.v1.common.requests.ResetManagedUserPasswordRequest;
import com.ingoboka_api.v1.common.requests.UpdateManagedUserRequest;
import com.ingoboka_api.v1.common.requests.UpdateManagedUserRolesRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.ManagedUserResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.identity.services.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Platform user management", description = "Platform administrator CRUD for staff and tenant users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List users", description = "Platform admin lists all users or filters by organization/status")
    public ApiResponse<PageResponse<ManagedUserResponse>> listUsers(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Users retrieved", userManagementService.listUsers(organizationId, status, page, size));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get user")
    public ApiResponse<ManagedUserResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.ok("User retrieved", userManagementService.getUser(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create user", description = "Creates staff/tenant user with emailed temporary password")
    public ApiResponse<ManagedUserResponse> createUser(@Valid @RequestBody CreateManagedUserRequest request) {
        return ApiResponse.ok("User created", userManagementService.createUser(request));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update user profile")
    public ApiResponse<ManagedUserResponse> updateUser(
            @PathVariable UUID userId, @Valid @RequestBody UpdateManagedUserRequest request) {
        return ApiResponse.ok("User updated", userManagementService.updateUser(userId, request));
    }

    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update user role")
    public ApiResponse<ManagedUserResponse> updateUserRoles(
            @PathVariable UUID userId, @Valid @RequestBody UpdateManagedUserRolesRequest request) {
        return ApiResponse.ok("User role updated", userManagementService.updateUserRoles(userId, request));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update user status")
    public ApiResponse<ManagedUserResponse> updateUserStatus(
            @PathVariable UUID userId, @Valid @RequestBody UpdateStaffStatusRequest request) {
        return ApiResponse.ok("User status updated", userManagementService.updateUserStatus(userId, request));
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Reset user password", description = "Issues a new temporary password by email and forces change on next login")
    public ApiResponse<ManagedUserResponse> resetPassword(
            @PathVariable UUID userId, @RequestBody(required = false) ResetManagedUserPasswordRequest request) {
        return ApiResponse.ok(
                "Password reset email sent",
                userManagementService.resetUserPassword(
                        userId, request != null ? request : new ResetManagedUserPasswordRequest()));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Disable user", description = "Soft-disables a non-citizen user account")
    public void deleteUser(@PathVariable UUID userId) {
        userManagementService.deleteUser(userId);
    }
}
