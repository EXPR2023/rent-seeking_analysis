package com.rentseeking.analysis.auth.util;

import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static JwtUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Not logged in");
        }
        return principal;
    }
}
