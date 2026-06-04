package com.rentseeking.analysis.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AssignUserRolesRequest {

    @NotNull
    private List<Long> roleIds = new ArrayList<>();

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
