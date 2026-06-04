package com.rentseeking.analysis.menu.dto;

import java.util.ArrayList;
import java.util.List;

public class MenuResponse {

    private Long id;
    private Long parentId;
    private String menuName;
    private String menuCode;
    private String path;
    private String component;
    private Integer sortOrder;
    private List<MenuResponse> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<MenuResponse> getChildren() {
        return children;
    }

    public void setChildren(List<MenuResponse> children) {
        this.children = children;
    }
}
