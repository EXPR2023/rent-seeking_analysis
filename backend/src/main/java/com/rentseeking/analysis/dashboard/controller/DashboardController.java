package com.rentseeking.analysis.dashboard.controller;

import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.dashboard.dto.ChartItemResponse;
import com.rentseeking.analysis.dashboard.dto.DashboardSummaryResponse;
import com.rentseeking.analysis.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success(dashboardService.summary());
    }

    @GetMapping("/risk-levels")
    public ApiResponse<List<ChartItemResponse>> riskLevels() {
        return ApiResponse.success(dashboardService.riskLevels());
    }

    @GetMapping("/conflict-status")
    public ApiResponse<List<ChartItemResponse>> conflictStatus() {
        return ApiResponse.success(dashboardService.conflictStatus());
    }
}
