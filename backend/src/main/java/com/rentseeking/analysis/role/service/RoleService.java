package com.rentseeking.analysis.role.service;

import com.rentseeking.analysis.role.dto.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> listRoles(Boolean enabled);
}
