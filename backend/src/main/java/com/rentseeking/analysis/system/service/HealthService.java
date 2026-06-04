package com.rentseeking.analysis.system.service;

import com.rentseeking.analysis.system.dto.HealthResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthService(JdbcTemplate jdbcTemplate, RedisConnectionFactory redisConnectionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    public HealthResponse check() {
        HealthResponse response = new HealthResponse();
        response.setBackend("UP");
        response.setMysql(checkMysql(response));
        response.setRedis(checkRedis(response));
        response.setAi(checkAi(response));
        response.setCheckedAt(LocalDateTime.now());
        return response;
    }

    private String checkMysql(HealthResponse response) {
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            response.getDetails().put("mysql", "SELECT 1 = " + value);
            return "UP";
        } catch (DataAccessException ex) {
            response.getDetails().put("mysql", ex.getMostSpecificCause().getMessage());
            return "DOWN";
        }
    }

    private String checkRedis(HealthResponse response) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            response.getDetails().put("redis", pong);
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (Exception ex) {
            response.getDetails().put("redis", ex.getMessage());
            return "DOWN";
        }
    }

    private String checkAi(HealthResponse response) {
        try {
            String enabled = configValue("ai.enabled");
            String baseUrl = configValue("ai.base-url");
            String model = configValue("ai.model");
            String apiKey = configValue("ai.api-key");
            response.getDetails().put("ai.enabled", String.valueOf(enabled));
            response.getDetails().put("ai.base-url", baseUrl == null || baseUrl.isBlank() ? "未配置" : baseUrl);
            response.getDetails().put("ai.model", model == null || model.isBlank() ? "未配置" : model);
            response.getDetails().put("ai.api-key", apiKey == null || apiKey.isBlank() ? "未配置" : "已配置");
            if (!Boolean.parseBoolean(enabled)) {
                return "DISABLED";
            }
            return isBlank(baseUrl) || isBlank(model) || isBlank(apiKey) ? "PARTIAL" : "CONFIGURED";
        } catch (Exception ex) {
            response.getDetails().put("ai", "配置未读取");
            return "UNKNOWN";
        }
    }

    private String configValue(String key) {
        return jdbcTemplate.query(
                "SELECT config_value FROM sys_config WHERE config_key = ?",
                rs -> rs.next() ? rs.getString("config_value") : null,
                key
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
