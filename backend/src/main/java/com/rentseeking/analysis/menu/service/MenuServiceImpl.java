package com.rentseeking.analysis.menu.service;

import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.menu.dto.MenuResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MenuServiceImpl implements MenuService {

    private final JdbcTemplate jdbcTemplate;

    public MenuServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MenuResponse> currentMenus() {
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        if (principal.getRoleCodes().isEmpty()) {
            return List.of();
        }
        List<MenuResponse> menus = jdbcTemplate.query("""
                        SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_code, m.path, m.component, m.sort_order
                        FROM sys_menu m
                        JOIN sys_role_menu rm ON rm.menu_id = m.id
                        JOIN sys_role r ON r.id = rm.role_id
                        WHERE r.role_code IN (%s) AND r.enabled = 1 AND m.visible = 1
                        ORDER BY m.sort_order, m.id
                        """.formatted(placeholders(principal.getRoleCodes().size())),
                (rs, rowNum) -> {
                    MenuResponse menu = new MenuResponse();
                    menu.setId(rs.getLong("id"));
                    menu.setParentId(rs.getLong("parent_id"));
                    menu.setMenuName(rs.getString("menu_name"));
                    menu.setMenuCode(rs.getString("menu_code"));
                    menu.setPath(rs.getString("path"));
                    menu.setComponent(rs.getString("component"));
                    menu.setSortOrder(rs.getInt("sort_order"));
                    return menu;
                },
                principal.getRoleCodes().toArray()
        );
        return tree(menus);
    }

    private List<MenuResponse> tree(List<MenuResponse> menus) {
        Map<Long, MenuResponse> byId = new LinkedHashMap<>();
        List<MenuResponse> roots = new ArrayList<>();
        for (MenuResponse menu : menus) {
            byId.put(menu.getId(), menu);
        }
        for (MenuResponse menu : menus) {
            if (menu.getParentId() == 0 || !byId.containsKey(menu.getParentId())) {
                roots.add(menu);
            } else {
                byId.get(menu.getParentId()).getChildren().add(menu);
            }
        }
        return roots;
    }

    private String placeholders(int size) {
        return String.join(", ", java.util.Collections.nCopies(size, "?"));
    }
}
