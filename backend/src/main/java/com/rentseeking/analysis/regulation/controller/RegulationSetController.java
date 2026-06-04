package com.rentseeking.analysis.regulation.controller;

import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.regulation.dto.RegulationSetRequest;
import com.rentseeking.analysis.regulation.dto.RegulationSetResponse;
import com.rentseeking.analysis.regulation.service.RegulationSetService;
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

import java.util.List;

@RestController
@RequestMapping("/api/regulation-sets")
public class RegulationSetController {

    private final RegulationSetService regulationSetService;

    public RegulationSetController(RegulationSetService regulationSetService) {
        this.regulationSetService = regulationSetService;
    }

    @GetMapping
    public ApiResponse<List<RegulationSetResponse>> listSets(
            @RequestParam(required = false, defaultValue = "false") Boolean enabledOnly
    ) {
        return ApiResponse.success(regulationSetService.listSets(enabledOnly));
    }

    @PostMapping
    @OperationLog(module = "REGULATION_SET", operation = "CREATE")
    public ApiResponse<RegulationSetResponse> createSet(@Valid @RequestBody RegulationSetRequest request) {
        return ApiResponse.success(regulationSetService.createSet(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "REGULATION_SET", operation = "UPDATE")
    public ApiResponse<RegulationSetResponse> updateSet(
            @PathVariable Long id,
            @Valid @RequestBody RegulationSetRequest request
    ) {
        return ApiResponse.success(regulationSetService.updateSet(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "REGULATION_SET", operation = "DELETE")
    public ApiResponse<Boolean> deleteSet(@PathVariable Long id) {
        return ApiResponse.success(regulationSetService.deleteSet(id));
    }
}
