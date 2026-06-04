package com.rentseeking.analysis.ai.controller;

import com.rentseeking.analysis.ai.dto.WebSourceDiscoveryRequest;
import com.rentseeking.analysis.ai.dto.WebSourceDiscoveryResponse;
import com.rentseeking.analysis.ai.dto.WebSourceImportRequest;
import com.rentseeking.analysis.ai.dto.WebSourceImportResponse;
import com.rentseeking.analysis.ai.service.WebSourceDiscoveryService;
import com.rentseeking.analysis.audit.OperationLog;
import com.rentseeking.analysis.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/web-sources")
public class WebSourceDiscoveryController {

    private final WebSourceDiscoveryService webSourceDiscoveryService;

    public WebSourceDiscoveryController(WebSourceDiscoveryService webSourceDiscoveryService) {
        this.webSourceDiscoveryService = webSourceDiscoveryService;
    }

    @PostMapping("/discover")
    @OperationLog(module = "AI", operation = "WEB_SOURCE_DISCOVER")
    public ApiResponse<WebSourceDiscoveryResponse> discover(@Valid @RequestBody WebSourceDiscoveryRequest request) {
        return ApiResponse.success(webSourceDiscoveryService.discover(request));
    }

    @PostMapping("/import")
    @OperationLog(module = "AI", operation = "WEB_SOURCE_IMPORT")
    public ApiResponse<WebSourceImportResponse> importSources(@Valid @RequestBody WebSourceImportRequest request) {
        return ApiResponse.success(webSourceDiscoveryService.importSources(request));
    }
}
