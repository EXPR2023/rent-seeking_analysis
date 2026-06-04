package com.rentseeking.analysis.rag.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.rag.service.KnowledgeRetrievalService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public RagController(KnowledgeRetrievalService knowledgeRetrievalService) {
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @PostMapping("/api/rag/reindex")
    @OperationLog(module = "RAG", operation = "REINDEX")
    public ApiResponse<Integer> reindex() {
        return ApiResponse.success(knowledgeRetrievalService.reindexAllRegulations());
    }
}
