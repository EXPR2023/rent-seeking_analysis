package com.rentseeking.analysis.risk.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalysisDetailResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalysisResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalyzeRequest;
import com.rentseeking.analysis.risk.dto.RiskEvidenceResponse;
import com.rentseeking.analysis.risk.dto.RiskReviewRequest;
import com.rentseeking.analysis.risk.service.RiskAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    public RiskAnalysisController(RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }

    @PostMapping("/api/regulations/{id}/analyze")
    @OperationLog(module = "RISK", operation = "ANALYZE")
    public ApiResponse<Long> analyze(@PathVariable Long id, @RequestBody(required = false) RiskAnalyzeRequest request) {
        return ApiResponse.success(riskAnalysisService.analyze(id, request == null ? new RiskAnalyzeRequest() : request));
    }

    @GetMapping("/api/risk-analyses")
    public ApiResponse<PageResponse<RiskAnalysisResponse>> listAnalyses(
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(riskAnalysisService.listAnalyses(riskLevel, reviewStatus, pageNo, pageSize));
    }

    @GetMapping("/api/risk-analyses/{id}")
    public ApiResponse<RiskAnalysisDetailResponse> getAnalysis(@PathVariable Long id) {
        return ApiResponse.success(riskAnalysisService.getAnalysis(id));
    }

    @GetMapping("/api/risk-analyses/{id}/evidence")
    public ApiResponse<List<RiskEvidenceResponse>> listEvidence(@PathVariable Long id) {
        return ApiResponse.success(riskAnalysisService.listEvidence(id));
    }

    @PutMapping("/api/risk-analyses/{id}/review")
    @OperationLog(module = "RISK", operation = "REVIEW")
    public ApiResponse<RiskAnalysisDetailResponse> reviewAnalysis(
            @PathVariable Long id,
            @Valid @RequestBody RiskReviewRequest request
    ) {
        return ApiResponse.success(riskAnalysisService.reviewAnalysis(id, request));
    }
}
