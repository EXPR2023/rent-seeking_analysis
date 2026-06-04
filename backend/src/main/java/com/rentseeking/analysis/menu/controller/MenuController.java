package com.rentseeking.analysis.menu.controller;

import com.rentseeking.analysis.common.response.ApiResponse;
import com.rentseeking.analysis.menu.dto.MenuResponse;
import com.rentseeking.analysis.menu.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/current")
    public ApiResponse<List<MenuResponse>> currentMenus() {
        return ApiResponse.success(menuService.currentMenus());
    }
}
