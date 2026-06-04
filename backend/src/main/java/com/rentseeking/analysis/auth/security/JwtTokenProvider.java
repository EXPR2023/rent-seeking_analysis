package com.rentseeking.analysis.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentseeking.analysis.common.config.SecurityProperties;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public JwtTokenProvider(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    public String generateToken(Long userId, String username, List<String> roleCodes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(securityProperties.getTokenExpireMinutes(), ChronoUnit.MINUTES);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("roleCodes", roleCodes);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signature = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signature;
    }

    public JwtClaims parseToken(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Token 格式不正确");
        }
        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Token 签名无效");
        }
        Map<String, Object> payload = decodeJson(parts[1]);
        long expiresAt = numberAsLong(payload.get("exp"));
        if (Instant.ofEpochSecond(expiresAt).isBefore(Instant.now())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Token 已过期");
        }

        JwtClaims claims = new JwtClaims();
        claims.setUserId(numberAsLong(payload.get("userId")));
        claims.setUsername((String) payload.get("username"));
        claims.setRoleCodes(objectMapper.convertValue(payload.get("roleCodes"), new TypeReference<List<String>>() {
        }));
        claims.setExpiresAt(Instant.ofEpochSecond(expiresAt));
        return claims;
    }

    public long expiresInSeconds() {
        return securityProperties.getTokenExpireMinutes() * 60;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return encoder.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "Token 生成失败");
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] json = decoder.decode(value);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Token 解析失败");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(
                    securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(key);
            return encoder.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "Token 签名失败");
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    private long numberAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
