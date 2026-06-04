package com.rentseeking.analysis.user.service;

import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.user.dto.AssignUserRolesRequest;
import com.rentseeking.analysis.user.dto.CreateUserRequest;
import com.rentseeking.analysis.user.dto.UpdateUserRequest;
import com.rentseeking.analysis.user.dto.UpdateUserStatusRequest;
import com.rentseeking.analysis.user.dto.UserResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResponse<UserResponse> listUsers(String keyword, String status, Integer pageNo, Integer pageSize) {
        int currentPage = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        StringBuilder condition = new StringBuilder(" WHERE u.deleted = 0");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            condition.append(" AND (u.username LIKE ? OR u.real_name LIKE ? OR u.email LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (status != null && !status.isBlank()) {
            condition.append(" AND u.status = ?");
            args.add(status.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user u" + condition,
                Long.class,
                args.toArray()
        );
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((currentPage - 1) * size);
        pageArgs.add(size);
        List<UserResponse> records = jdbcTemplate.query("""
                        SELECT u.id, u.username, u.real_name, u.email, u.phone, u.status, u.last_login_at, u.created_at
                        FROM sys_user u
                        %s
                        ORDER BY u.id
                        LIMIT ?, ?
                        """.formatted(condition),
                (rs, rowNum) -> mapUser(rs),
                pageArgs.toArray()
        );
        attachRoles(records);
        return new PageResponse<>(records, currentPage, size, total == null ? 0 : total);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        validateRoles(request.getRoleIds());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                                INSERT INTO sys_user (username, password_hash, real_name, email, phone, status)
                                VALUES (?, ?, ?, ?, ?, 'ENABLED')
                                """,
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, request.getUsername());
                ps.setString(2, passwordEncoder.encode(request.getPassword()));
                ps.setString(3, request.getRealName());
                ps.setString(4, blankToNull(request.getEmail()));
                ps.setString(5, blankToNull(request.getPhone()));
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "Username already exists");
        }
        Long userId = keyHolder.getKey().longValue();
        replaceRoles(userId, request.getRoleIds());
        return getUser(userId);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        ensureUserExists(id);
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            jdbcTemplate.update("""
                            UPDATE sys_user
                            SET real_name = ?, email = ?, phone = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE id = ? AND deleted = 0
                            """,
                    request.getRealName(),
                    blankToNull(request.getEmail()),
                    blankToNull(request.getPhone()),
                    id
            );
        } else {
            jdbcTemplate.update("""
                            UPDATE sys_user
                            SET real_name = ?, email = ?, phone = ?, password_hash = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE id = ? AND deleted = 0
                            """,
                    request.getRealName(),
                    blankToNull(request.getEmail()),
                    blankToNull(request.getPhone()),
                    passwordEncoder.encode(request.getPassword()),
                    id
            );
        }
        return getUser(id);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        ensureUserExists(id);
        String status = request.getStatus().trim();
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Status must be ENABLED or DISABLED");
        }
        jdbcTemplate.update("""
                        UPDATE sys_user
                        SET status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = 0
                        """,
                status,
                id
        );
        return getUser(id);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, AssignUserRolesRequest request) {
        ensureUserExists(id);
        validateRoles(request.getRoleIds());
        replaceRoles(id, request.getRoleIds());
        return getUser(id);
    }

    private UserResponse getUser(Long id) {
        UserResponse user = jdbcTemplate.query("""
                        SELECT id, username, real_name, email, phone, status, last_login_at, created_at
                        FROM sys_user
                        WHERE id = ? AND deleted = 0
                        """,
                rs -> rs.next() ? mapUser(rs) : null,
                id
        );
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "User not found");
        }
        attachRoles(List.of(user));
        return user;
    }

    private void ensureUserExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id = ? AND deleted = 0",
                Integer.class,
                id
        );
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "User not found");
        }
    }

    private void validateRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(roleIds.size(), "?"));
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE enabled = 1 AND id IN (" + placeholders + ")",
                Long.class,
                roleIds.toArray()
        );
        if (count == null || count != roleIds.stream().distinct().count()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Role does not exist or is disabled");
        }
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Object[]> args = roleIds.stream()
                .distinct()
                .map(roleId -> new Object[]{userId, roleId})
                .toList();
        jdbcTemplate.batchUpdate("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", args);
    }

    private void attachRoles(List<UserResponse> users) {
        if (users.isEmpty()) {
            return;
        }
        for (UserResponse user : users) {
            user.setRoleCodes(jdbcTemplate.queryForList("""
                            SELECT r.role_code
                            FROM sys_role r
                            JOIN sys_user_role ur ON ur.role_id = r.id
                            WHERE ur.user_id = ?
                            ORDER BY r.id
                            """,
                    String.class,
                    user.getId()
            ));
        }
    }

    private UserResponse mapUser(java.sql.ResultSet rs) throws java.sql.SQLException {
        UserResponse user = new UserResponse();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setRealName(rs.getString("real_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setStatus(rs.getString("status"));
        Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
        user.setLastLoginAt(lastLoginAt == null ? null : lastLoginAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
