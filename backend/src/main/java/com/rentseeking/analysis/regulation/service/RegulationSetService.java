package com.rentseeking.analysis.regulation.service;

import com.rentseeking.analysis.regulation.dto.RegulationSetRequest;
import com.rentseeking.analysis.regulation.dto.RegulationSetResponse;

import java.util.List;

public interface RegulationSetService {

    List<RegulationSetResponse> listSets(Boolean enabledOnly);

    RegulationSetResponse createSet(RegulationSetRequest request);

    RegulationSetResponse updateSet(Long id, RegulationSetRequest request);

    Boolean deleteSet(Long id);
}
