package com.rentseeking.analysis.regulation.service;

import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.regulation.dto.RegulationContentRequest;
import com.rentseeking.analysis.regulation.dto.RegulationDetailResponse;
import com.rentseeking.analysis.regulation.dto.RegulationResponse;
import com.rentseeking.analysis.regulation.dto.RegulationSaveRequest;

public interface RegulationService {

    PageResponse<RegulationResponse> listRegulations(
            String keyword,
            Long regulationSetId,
            String typeCode,
            String status,
            Integer pageNo,
            Integer pageSize
    );

    RegulationDetailResponse createRegulation(RegulationSaveRequest request);

    RegulationDetailResponse getRegulation(Long id);

    RegulationDetailResponse updateRegulation(Long id, RegulationSaveRequest request);

    RegulationDetailResponse saveContent(Long id, RegulationContentRequest request);

    Boolean deleteRegulation(Long id);
}
