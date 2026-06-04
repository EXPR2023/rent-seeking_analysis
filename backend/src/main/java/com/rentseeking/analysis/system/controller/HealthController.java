package com.rentseeking.analysis.system.controller;

import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.system.dto.HealthResponse;
import com.rentseeking.analysis.system.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(healthService.check());
    }
}
