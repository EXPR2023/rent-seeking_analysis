package com.rentseeking.analysis.audit;

import com.rentseeking.analysis.audit.dto.OperationLogResponse;
import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.common.response.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/logs")
public class AuditLogController {

    private final OperationLogService operationLogService;

    public AuditLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OperationLogResponse>> list(
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(operationLogService.list(moduleName, username, pageNo, pageSize));
    }
}
