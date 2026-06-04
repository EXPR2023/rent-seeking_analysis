package com.rentseeking.analysis.ai.controller;

import com.rentseeking.analysis.ai.dto.AiRecordResponse;
import com.rentseeking.analysis.ai.dto.CaseAnalysisRequest;
import com.rentseeking.analysis.ai.dto.CaseAnalysisResponse;
import com.rentseeking.analysis.ai.dto.RegulationQaRequest;
import com.rentseeking.analysis.ai.dto.RegulationQaResponse;
import com.rentseeking.analysis.ai.service.AiService;
import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/records")
    public ApiResponse<PageResponse<AiRecordResponse>> listRecords(
            @RequestParam(required = false) String sceneCode,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(aiService.listRecords(sceneCode, businessType, status, pageNo, pageSize));
    }

    @PostMapping("/regulations/{id}/chat")
    @OperationLog(module = "AI", operation = "REGULATION_QA")
    public ApiResponse<RegulationQaResponse> answerRegulationQuestion(
            @PathVariable Long id,
            @Valid @RequestBody RegulationQaRequest request
    ) {
        return ApiResponse.success(aiService.answerRegulationQuestion(id, request));
    }

    @PostMapping("/cases/analyze")
    @OperationLog(module = "AI", operation = "CASE_EXTEND_ANALYSIS")
    public ApiResponse<CaseAnalysisResponse> analyzeCase(@Valid @RequestBody CaseAnalysisRequest request) {
        return ApiResponse.success(aiService.analyzeCase(request));
    }
}
