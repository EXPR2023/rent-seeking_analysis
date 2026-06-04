package com.rentseeking.analysis.risk.service;

import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalysisDetailResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalysisResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalyzeRequest;
import com.rentseeking.analysis.risk.dto.RiskEvidenceResponse;
import com.rentseeking.analysis.risk.dto.RiskReviewRequest;

import java.util.List;

public interface RiskAnalysisService {

    Long analyze(Long regulationId, RiskAnalyzeRequest request);

    PageResponse<RiskAnalysisResponse> listAnalyses(String riskLevel, String reviewStatus, Integer pageNo, Integer pageSize);

    RiskAnalysisDetailResponse getAnalysis(Long id);

    List<RiskEvidenceResponse> listEvidence(Long id);

    RiskAnalysisDetailResponse reviewAnalysis(Long id, RiskReviewRequest request);
}
