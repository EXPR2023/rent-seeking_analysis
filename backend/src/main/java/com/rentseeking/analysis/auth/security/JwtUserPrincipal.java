package com.rentseeking.analysis.auth.security;

import java.util.List;

public class JwtUserPrincipal {

    private final Long userId;
    private final String username;
    private final List<String> roleCodes;

    public JwtUserPrincipal(Long userId, String username, List<String> roleCodes) {
        this.userId = userId;
        this.username = username;
        this.roleCodes = List.copyOf(roleCodes);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }
}
