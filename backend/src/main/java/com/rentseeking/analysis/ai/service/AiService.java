package com.rentseeking.analysis.ai.service;

import com.rentseeking.analysis.ai.dto.AiRecordResponse;
import com.rentseeking.analysis.ai.dto.CaseAnalysisRequest;
import com.rentseeking.analysis.ai.dto.CaseAnalysisResponse;
import com.rentseeking.analysis.ai.dto.RegulationQaRequest;
import com.rentseeking.analysis.ai.dto.RegulationQaResponse;
import com.rentseeking.analysis.ai.dto.RentSeekingRiskResult;
import com.rentseeking.analysis.common.response.PageResponse;

public interface AiService {

    PageResponse<AiRecordResponse> listRecords(
            String sceneCode,
            String businessType,
            String status,
            Integer pageNo,
            Integer pageSize
    );

    RegulationQaResponse answerRegulationQuestion(Long regulationId, RegulationQaRequest request);

    CaseAnalysisResponse analyzeCase(CaseAnalysisRequest request);

    String explainConflict(Long businessId, String conflictType, String mainClause, String compareClause);

    RentSeekingRiskResult analyzeRentSeekingRisk(
            Long regulationId,
            String title,
            String publishDepartment,
            String applicableScope,
            String content,
            String ruleContext,
            String ragContext
    );

    Long record(
            String sceneCode,
            String businessType,
            Long businessId,
            String prompt,
            String responseText,
            String parsedJson,
            String status,
            String errorMessage
    );
}
