package com.rentseeking.analysis.role.controller;

import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.role.dto.RoleResponse;
import com.rentseeking.analysis.role.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> listRoles(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(roleService.listRoles(enabled));
    }
}
