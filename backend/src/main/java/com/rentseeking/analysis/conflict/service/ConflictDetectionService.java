package com.rentseeking.analysis.conflict.service;

import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.conflict.dto.ConflictItemResponse;
import com.rentseeking.analysis.conflict.dto.ConflictItemReviewRequest;
import com.rentseeking.analysis.conflict.dto.ConflictTaskCreateRequest;
import com.rentseeking.analysis.conflict.dto.ConflictTaskDetailResponse;
import com.rentseeking.analysis.conflict.dto.ConflictTaskResponse;

public interface ConflictDetectionService {

    ConflictTaskDetailResponse createTask(ConflictTaskCreateRequest request);

    PageResponse<ConflictTaskResponse> listTasks(String status, Integer pageNo, Integer pageSize);

    ConflictTaskDetailResponse getTask(Long id);

    ConflictItemResponse confirmItem(Long id, ConflictItemReviewRequest request);

    ConflictItemResponse ignoreItem(Long id, ConflictItemReviewRequest request);
}
