package com.rentseeking.analysis.role.service;

import com.rentseeking.analysis.role.dto.RoleResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final JdbcTemplate jdbcTemplate;

    public RoleServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<RoleResponse> listRoles(Boolean enabled) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, role_code, role_name, description, enabled, created_at
                FROM sys_role
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (enabled != null) {
            sql.append(" AND enabled = ?");
            args.add(enabled ? 1 : 0);
        }
        sql.append(" ORDER BY id");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            RoleResponse role = new RoleResponse();
            role.setId(rs.getLong("id"));
            role.setRoleCode(rs.getString("role_code"));
            role.setRoleName(rs.getString("role_name"));
            role.setDescription(rs.getString("description"));
            role.setEnabled(rs.getInt("enabled") == 1);
            Timestamp createdAt = rs.getTimestamp("created_at");
            role.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return role;
        }, args.toArray());
    }
}
