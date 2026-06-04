package com.rentseeking.analysis.regulation.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.regulation.dto.RegulationContentRequest;
import com.rentseeking.analysis.regulation.dto.RegulationDetailResponse;
import com.rentseeking.analysis.regulation.dto.RegulationResponse;
import com.rentseeking.analysis.regulation.dto.RegulationSaveRequest;
import com.rentseeking.analysis.regulation.service.RegulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulations")
public class RegulationController {

    private final RegulationService regulationService;

    public RegulationController(RegulationService regulationService) {
        this.regulationService = regulationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RegulationResponse>> listRegulations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long regulationSetId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(regulationService.listRegulations(keyword, regulationSetId, typeCode, status, pageNo, pageSize));
    }

    @PostMapping
    @OperationLog(module = "REGULATION", operation = "CREATE")
    public ApiResponse<RegulationDetailResponse> createRegulation(@Valid @RequestBody RegulationSaveRequest request) {
        return ApiResponse.success(regulationService.createRegulation(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RegulationDetailResponse> getRegulation(@PathVariable Long id) {
        return ApiResponse.success(regulationService.getRegulation(id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "REGULATION", operation = "UPDATE")
    public ApiResponse<RegulationDetailResponse> updateRegulation(
            @PathVariable Long id,
            @Valid @RequestBody RegulationSaveRequest request
    ) {
        return ApiResponse.success(regulationService.updateRegulation(id, request));
    }

    @PutMapping("/{id}/content")
    @OperationLog(module = "REGULATION", operation = "SAVE_CONTENT")
    public ApiResponse<RegulationDetailResponse> saveContent(
            @PathVariable Long id,
            @Valid @RequestBody RegulationContentRequest request
    ) {
        return ApiResponse.success(regulationService.saveContent(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "REGULATION", operation = "DELETE")
    public ApiResponse<Boolean> deleteRegulation(@PathVariable Long id) {
        return ApiResponse.success(regulationService.deleteRegulation(id));
    }
}
