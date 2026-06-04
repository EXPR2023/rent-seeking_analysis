package com.rentseeking.analysis.auth.service;

import com.rentseeking.analysis.auth.dto.LoginRequest;
import com.rentseeking.analysis.auth.dto.LoginResponse;
import com.rentseeking.analysis.auth.dto.CurrentUserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    CurrentUserResponse currentUser();
}
