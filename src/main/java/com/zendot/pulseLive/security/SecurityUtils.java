package com.zendot.pulseLive.security;

import com.zendot.pulseLive.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal requirePrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (auth.getPrincipal() instanceof UserPrincipal p) {
            return p;
        }

        // In case something else got set as principal
        throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid auth principal");
    }

    public static Long requireUserId() {
        return requirePrincipal().userId();
    }

    public static String requireEmail() {
        return requirePrincipal().email();
    }
}
