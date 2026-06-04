package com.rentseeking.analysis.system.service;

import com.rentseeking.analysis.system.dto.DictResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DictService {

    private final JdbcTemplate jdbcTemplate;

    public DictService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DictResponse> list(String dictType) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, dict_type, item_code, item_name, sort_order, enabled
                FROM sys_dict
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (dictType != null && !dictType.isBlank()) {
            sql.append(" AND dict_type = ?");
            args.add(dictType.trim());
        }
        sql.append(" ORDER BY dict_type, sort_order, id");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            DictResponse response = new DictResponse();
            response.setId(rs.getLong("id"));
            response.setDictType(rs.getString("dict_type"));
            response.setItemCode(rs.getString("item_code"));
            response.setItemName(rs.getString("item_name"));
            response.setSortOrder(rs.getInt("sort_order"));
            response.setEnabled(rs.getInt("enabled") == 1);
            return response;
        }, args.toArray());
    }
}
