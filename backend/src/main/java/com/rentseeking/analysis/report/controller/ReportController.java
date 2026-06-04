package com.rentseeking.analysis.report.controller;

import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.report.dto.RegulationReportResponse;
import com.rentseeking.analysis.report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/regulation/{id}")
    public ApiResponse<RegulationReportResponse> regulationReport(@PathVariable Long id) {
        return ApiResponse.success(reportService.regulationReport(id));
    }
}
