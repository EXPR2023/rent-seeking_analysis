package com.rentseeking.analysis.audit;

import com.rentseeking.analysis.audit.dto.OperationLogResponse;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(OperationLog annotation, HttpServletRequest request, String resultCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username = null;
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            userId = principal.getUserId();
            username = principal.getUsername();
        }
        if (username == null && request.getParameter("username") != null) {
            username = request.getParameter("username");
        }
        jdbcTemplate.update("""
                        INSERT INTO operation_log (
                          user_id, username, module_name, operation_type, request_method,
                          request_uri, result_code, ip_address, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                userId,
                username,
                annotation.module(),
                annotation.operation(),
                request.getMethod(),
                request.getRequestURI(),
                resultCode,
                request.getRemoteAddr()
        );
    }

    public PageResponse<OperationLogResponse> list(String moduleName, String username, Integer pageNo, Integer pageSize) {
        int currentPage = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        StringBuilder condition = new StringBuilder(" WHERE 1 = 1");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (moduleName != null && !moduleName.isBlank()) {
            condition.append(" AND module_name = ?");
            args.add(moduleName.trim());
        }
        if (username != null && !username.isBlank()) {
            condition.append(" AND username LIKE ?");
            args.add("%" + username.trim() + "%");
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation_log" + condition,
                Long.class,
                args.toArray()
        );
        java.util.List<Object> pageArgs = new java.util.ArrayList<>(args);
        pageArgs.add((currentPage - 1) * size);
        pageArgs.add(size);
        java.util.List<OperationLogResponse> records = jdbcTemplate.query("""
                        SELECT id, user_id, username, module_name, operation_type, business_id,
                               request_method, request_uri, result_code, ip_address, created_at
                        FROM operation_log
                        %s
                        ORDER BY created_at DESC, id DESC
                        LIMIT ?, ?
                        """.formatted(condition),
                (rs, rowNum) -> {
                    OperationLogResponse response = new OperationLogResponse();
                    response.setId(rs.getLong("id"));
                    long userId = rs.getLong("user_id");
                    response.setUserId(rs.wasNull() ? null : userId);
                    response.setUsername(rs.getString("username"));
                    response.setModuleName(rs.getString("module_name"));
                    response.setOperationType(rs.getString("operation_type"));
                    long businessId = rs.getLong("business_id");
                    response.setBusinessId(rs.wasNull() ? null : businessId);
                    response.setRequestMethod(rs.getString("request_method"));
                    response.setRequestUri(rs.getString("request_uri"));
                    response.setResultCode(rs.getString("result_code"));
                    response.setIpAddress(rs.getString("ip_address"));
                    response.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return response;
                },
                pageArgs.toArray()
        );
        return new PageResponse<>(records, currentPage, size, total == null ? 0 : total);
    }
}
