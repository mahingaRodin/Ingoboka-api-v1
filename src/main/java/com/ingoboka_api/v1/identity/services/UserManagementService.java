package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.requests.CreateManagedUserRequest;
import com.ingoboka_api.v1.common.requests.ResetManagedUserPasswordRequest;
import com.ingoboka_api.v1.common.requests.UpdateManagedUserRequest;
import com.ingoboka_api.v1.common.requests.UpdateManagedUserRolesRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.ManagedUserResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import java.util.UUID;

public interface UserManagementService {

    PageResponse<ManagedUserResponse> listUsers(UUID organizationId, UserStatus status, int page, int size);

    ManagedUserResponse getUser(UUID userId);

    ManagedUserResponse createUser(CreateManagedUserRequest request);

    ManagedUserResponse updateUser(UUID userId, UpdateManagedUserRequest request);

    ManagedUserResponse updateUserRoles(UUID userId, UpdateManagedUserRolesRequest request);

    ManagedUserResponse updateUserStatus(UUID userId, UpdateStaffStatusRequest request);

    ManagedUserResponse resetUserPassword(UUID userId, ResetManagedUserPasswordRequest request);

    void deleteUser(UUID userId);
}
