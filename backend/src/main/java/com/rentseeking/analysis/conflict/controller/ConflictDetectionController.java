package com.rentseeking.analysis.conflict.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.conflict.dto.ConflictItemResponse;
import com.rentseeking.analysis.conflict.dto.ConflictItemReviewRequest;
import com.rentseeking.analysis.conflict.dto.ConflictTaskCreateRequest;
import com.rentseeking.analysis.conflict.dto.ConflictTaskDetailResponse;
import com.rentseeking.analysis.conflict.dto.ConflictTaskResponse;
import com.rentseeking.analysis.conflict.service.ConflictDetectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConflictDetectionController {

    private final ConflictDetectionService conflictDetectionService;

    public ConflictDetectionController(ConflictDetectionService conflictDetectionService) {
        this.conflictDetectionService = conflictDetectionService;
    }

    @PostMapping("/api/conflict-tasks")
    @OperationLog(module = "CONFLICT", operation = "CREATE_TASK")
    public ApiResponse<ConflictTaskDetailResponse> createTask(@Valid @RequestBody ConflictTaskCreateRequest request) {
        return ApiResponse.success(conflictDetectionService.createTask(request));
    }

    @GetMapping("/api/conflict-tasks")
    public ApiResponse<PageResponse<ConflictTaskResponse>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(conflictDetectionService.listTasks(status, pageNo, pageSize));
    }

    @GetMapping("/api/conflict-tasks/{id}")
    public ApiResponse<ConflictTaskDetailResponse> getTask(@PathVariable Long id) {
        return ApiResponse.success(conflictDetectionService.getTask(id));
    }

    @PutMapping("/api/conflict-items/{id}/confirm")
    @OperationLog(module = "CONFLICT", operation = "CONFIRM_ITEM")
    public ApiResponse<ConflictItemResponse> confirmItem(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ConflictItemReviewRequest request
    ) {
        return ApiResponse.success(conflictDetectionService.confirmItem(id, request));
    }

    @PutMapping("/api/conflict-items/{id}/ignore")
    @OperationLog(module = "CONFLICT", operation = "IGNORE_ITEM")
    public ApiResponse<ConflictItemResponse> ignoreItem(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ConflictItemReviewRequest request
    ) {
        return ApiResponse.success(conflictDetectionService.ignoreItem(id, request));
    }
}
