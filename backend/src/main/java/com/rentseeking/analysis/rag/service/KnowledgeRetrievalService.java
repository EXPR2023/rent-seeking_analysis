package com.rentseeking.analysis.rag.service;

import com.rentseeking.analysis.rag.dto.RagEvidence;

import java.util.List;

public interface KnowledgeRetrievalService {

    void indexRegulation(Long regulationId);

    int reindexAllRegulations();

    List<RagEvidence> retrieveForRiskAnalysis(Long regulationId, String queryText);

    List<RagEvidence> retrieveForCaseAnalysis(String queryText);

    List<RagEvidence> retrieveForCaseAnalysis(String queryText, Long regulationSetId);

    List<RagEvidence> retrieveForCaseAnalysisOutsideSet(String queryText, Long excludedRegulationSetId);

    String formatContext(List<RagEvidence> evidences);
}
