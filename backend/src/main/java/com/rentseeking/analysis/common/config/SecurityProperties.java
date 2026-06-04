package com.rentseeking.analysis.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String jwtSecret;
    private long tokenExpireMinutes = 120;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpireMinutes() {
        return tokenExpireMinutes;
    }

    public void setTokenExpireMinutes(long tokenExpireMinutes) {
        this.tokenExpireMinutes = tokenExpireMinutes;
    }
}
