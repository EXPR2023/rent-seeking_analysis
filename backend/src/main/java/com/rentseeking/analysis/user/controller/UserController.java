package com.rentseeking.analysis.user.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.user.dto.AssignUserRolesRequest;
import com.rentseeking.analysis.user.dto.CreateUserRequest;
import com.rentseeking.analysis.user.dto.UpdateUserRequest;
import com.rentseeking.analysis.user.dto.UpdateUserStatusRequest;
import com.rentseeking.analysis.user.dto.UserResponse;
import com.rentseeking.analysis.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(userService.listUsers(keyword, status, pageNo, pageSize));
    }

    @PostMapping
    @OperationLog(module = "USER", operation = "CREATE")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "USER", operation = "UPDATE")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    @OperationLog(module = "USER", operation = "UPDATE_STATUS")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ApiResponse.success(userService.updateStatus(id, request));
    }

    @PutMapping("/{id}/roles")
    @OperationLog(module = "USER", operation = "ASSIGN_ROLES")
    public ApiResponse<UserResponse> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignUserRolesRequest request
    ) {
        return ApiResponse.success(userService.assignRoles(id, request));
    }
}
