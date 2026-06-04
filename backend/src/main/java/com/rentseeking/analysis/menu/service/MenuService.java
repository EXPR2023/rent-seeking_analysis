package com.rentseeking.analysis.menu.service;

import com.rentseeking.analysis.menu.dto.MenuResponse;

import java.util.List;

public interface MenuService {

    List<MenuResponse> currentMenus();
}
