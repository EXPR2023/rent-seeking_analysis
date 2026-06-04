package com.rentseeking.analysis.ai.service;

import com.rentseeking.analysis.ai.dto.WebSourceDiscoveryRequest;
import com.rentseeking.analysis.ai.dto.WebSourceDiscoveryResponse;
import com.rentseeking.analysis.ai.dto.WebSourceImportRequest;
import com.rentseeking.analysis.ai.dto.WebSourceImportResponse;

public interface WebSourceDiscoveryService {

    WebSourceDiscoveryResponse discover(WebSourceDiscoveryRequest request);

    WebSourceImportResponse importSources(WebSourceImportRequest request);
}
