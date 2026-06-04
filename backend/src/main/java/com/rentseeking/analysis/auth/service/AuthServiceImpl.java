package com.rentseeking.analysis.auth.service;

import com.rentseeking.analysis.auth.dto.AuthUser;
import com.rentseeking.analysis.auth.dto.CurrentUserResponse;
import com.rentseeking.analysis.auth.dto.LoginRequest;
import com.rentseeking.analysis.auth.dto.LoginResponse;
import com.rentseeking.analysis.auth.dto.UserProfileResponse;
import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.security.JwtTokenProvider;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        AuthUser user = jdbcTemplate.query("""
                        SELECT id, username, password_hash, real_name, email, status
                        FROM sys_user
                        WHERE username = ? AND deleted = 0
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    AuthUser value = new AuthUser();
                    value.setId(rs.getLong("id"));
                    value.setUsername(rs.getString("username"));
                    value.setPasswordHash(rs.getString("password_hash"));
                    value.setRealName(rs.getString("real_name"));
                    value.setEmail(rs.getString("email"));
                    value.setStatus(rs.getString("status"));
                    return value;
                },
                request.getUsername()
        );
        if (user == null || !"ENABLED".equals(user.getStatus())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        List<String> roleCodes = jdbcTemplate.queryForList("""
                        SELECT r.role_code
                        FROM sys_role r
                        JOIN sys_user_role ur ON ur.role_id = r.id
                        WHERE ur.user_id = ? AND r.enabled = 1
                        ORDER BY r.id
                        """,
                String.class,
                user.getId()
        );
        user.setRoleCodes(roleCodes);
        jdbcTemplate.update("UPDATE sys_user SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?", user.getId());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roleCodes));
        response.setExpiresIn(jwtTokenProvider.expiresInSeconds());
        response.setRoleCodes(roleCodes);
        response.setUser(new UserProfileResponse(user.getId(), user.getUsername(), user.getRealName(), user.getEmail()));
        return response;
    }

    @Override
    public CurrentUserResponse currentUser() {
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        return jdbcTemplate.query("""
                        SELECT id, username, real_name, email
                        FROM sys_user
                        WHERE id = ? AND deleted = 0 AND status = 'ENABLED'
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new BizException(ErrorCode.UNAUTHORIZED, "User is disabled or deleted");
                    }
                    return new CurrentUserResponse(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("real_name"),
                            rs.getString("email"),
                            principal.getRoleCodes()
                    );
                },
                principal.getUserId()
        );
    }
}
