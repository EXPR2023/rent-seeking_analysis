package com.rentseeking.analysis.auth.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.auth.dto.CurrentUserResponse;
import com.rentseeking.analysis.auth.dto.LoginRequest;
import com.rentseeking.analysis.auth.dto.LoginResponse;
import com.rentseeking.analysis.auth.service.AuthService;
import com.rentseeking.analysis.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @OperationLog(module = "AUTH", operation = "LOGIN")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    @OperationLog(module = "AUTH", operation = "LOGOUT")
    public ApiResponse<Boolean> logout() {
        return ApiResponse.success(true);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(authService.currentUser());
    }
}
