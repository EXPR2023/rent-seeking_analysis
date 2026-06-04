package com.rentseeking.analysis.system.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.system.dto.SystemConfigResponse;
import com.rentseeking.analysis.system.dto.UpdateSystemConfigRequest;
import com.rentseeking.analysis.system.service.SystemConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ApiResponse<List<SystemConfigResponse>> list() {
        return ApiResponse.success(systemConfigService.list());
    }

    @PutMapping("/{configKey}")
    @OperationLog(module = "SYSTEM_CONFIG", operation = "UPDATE")
    public ApiResponse<SystemConfigResponse> update(
            @PathVariable String configKey,
            @Valid @RequestBody UpdateSystemConfigRequest request
    ) {
        return ApiResponse.success(systemConfigService.update(configKey, request));
    }
}
