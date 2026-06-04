package com.rentseeking.analysis.system.service;

import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.system.dto.SystemConfigResponse;
import com.rentseeking.analysis.system.dto.UpdateSystemConfigRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SystemConfigService {

    private static final Set<String> SENSITIVE_KEYS = Set.of("ai.api-key", "embedding.api-key");

    private final JdbcTemplate jdbcTemplate;

    public SystemConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SystemConfigResponse> list() {
        ensureDefaultConfigs();
        return jdbcTemplate.query("""
                        SELECT config_key, config_value, description, updated_at
                        FROM sys_config
                        ORDER BY config_key
                        """,
                (rs, rowNum) -> {
                    SystemConfigResponse response = new SystemConfigResponse();
                    response.setConfigKey(rs.getString("config_key"));
                    response.setConfigValue(displayValue(rs.getString("config_key"), rs.getString("config_value")));
                    response.setDescription(rs.getString("description"));
                    response.setSensitive(isSensitive(rs.getString("config_key")));
                    response.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return response;
                }
        );
    }

    public SystemConfigResponse update(String configKey, UpdateSystemConfigRequest request) {
        ensureDefaultConfigs();
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        String nextValue = request.getConfigValue();
        if (isSensitive(configKey) && (nextValue == null || nextValue.isBlank())) {
            return find(configKey);
        }
        int updated = jdbcTemplate.update("""
                        UPDATE sys_config
                        SET config_value = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE config_key = ?
                        """,
                nextValue,
                principal.getUserId(),
                configKey
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "System config not found");
        }
        return find(configKey);
    }

    private SystemConfigResponse find(String configKey) {
        return jdbcTemplate.query("""
                        SELECT config_key, config_value, description, updated_at
                        FROM sys_config
                        WHERE config_key = ?
                        """,
                rs -> {
                    rs.next();
                    SystemConfigResponse response = new SystemConfigResponse();
                    response.setConfigKey(rs.getString("config_key"));
                    response.setConfigValue(displayValue(rs.getString("config_key"), rs.getString("config_value")));
                    response.setDescription(rs.getString("description"));
                    response.setSensitive(isSensitive(rs.getString("config_key")));
                    response.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return response;
                },
                configKey
        );
    }

    private void ensureDefaultConfigs() {
        jdbcTemplate.batchUpdate("""
                        INSERT INTO sys_config (config_key, config_value, description, updated_by)
                        VALUES (?, ?, ?, NULL)
                        ON DUPLICATE KEY UPDATE description = VALUES(description)
                        """,
                List.of(
                        new Object[]{"ai.enabled", "true", "是否启用 LLM 寻租分析"},
                        new Object[]{"ai.provider", "openai-compatible", "LLM 服务提供方"},
                        new Object[]{"ai.base-url", "", "OpenAI-compatible Base URL，例如 https://api.openai.com/v1"},
                        new Object[]{"ai.api-key", "", "LLM API Key，页面不明文展示"},
                        new Object[]{"ai.model", "", "LLM 模型名称"},
                        new Object[]{"ai.timeout", "30s", "LLM 调用超时时间"},
                        new Object[]{"embedding.provider", "local", "Embedding 提供方：local 或 openai-compatible"},
                        new Object[]{"embedding.base-url", "", "OpenAI-compatible Embedding Base URL，例如 https://api.openai.com/v1"},
                        new Object[]{"embedding.api-key", "", "Embedding API Key，页面不明文展示"},
                        new Object[]{"embedding.model", "local-bigram-v1", "Embedding 模型名称"},
                        new Object[]{"embedding.dimension", "384", "本地向量维度，外部模型可留作记录"},
                        new Object[]{"embedding.timeout", "30s", "Embedding 调用超时时间"},
                        new Object[]{"rag.enabled", "true", "是否启用 RAG 检索增强"},
                        new Object[]{"rag.top-k", "5", "RAG 检索返回证据数量"},
                        new Object[]{"rag.similarity.threshold", "0.15", "RAG 向量余弦相似度阈值"},
                        new Object[]{"rag.chunk-size", "420", "知识切片目标长度"}
                )
        );
    }

    private boolean isSensitive(String configKey) {
        return SENSITIVE_KEYS.contains(configKey);
    }

    private String displayValue(String configKey, String value) {
        if (!isSensitive(configKey)) {
            return value;
        }
        return value == null || value.isBlank() ? "" : "********";
    }
}
