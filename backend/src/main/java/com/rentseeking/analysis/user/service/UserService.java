package com.rentseeking.analysis.user.service;

import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.user.dto.AssignUserRolesRequest;
import com.rentseeking.analysis.user.dto.CreateUserRequest;
import com.rentseeking.analysis.user.dto.UpdateUserRequest;
import com.rentseeking.analysis.user.dto.UpdateUserStatusRequest;
import com.rentseeking.analysis.user.dto.UserResponse;

public interface UserService {

    PageResponse<UserResponse> listUsers(String keyword, String status, Integer pageNo, Integer pageSize);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    UserResponse updateStatus(Long id, UpdateUserStatusRequest request);

    UserResponse assignRoles(Long id, AssignUserRolesRequest request);
}
