package com.rentseeking.analysis.system.controller;

import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.system.dto.DictResponse;
import com.rentseeking.analysis.system.service.DictService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping
    public ApiResponse<List<DictResponse>> list(@RequestParam(required = false) String dictType) {
        return ApiResponse.success(dictService.list(dictType));
    }
}
