package com.rentseeking.analysis.auth.dto;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String realName;
    private String email;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String username, String realName, String email) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.email = email;
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
}
