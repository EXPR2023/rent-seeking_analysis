package com.rentseeking.analysis.auth.dto;

import java.util.List;

public class CurrentUserResponse {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private List<String> roleCodes;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(Long id, String username, String realName, String email, List<String> roleCodes) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.email = email;
        this.roleCodes = roleCodes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
