package com.zendot.pulseLive.auth.dto;

public record AuthResult(
        String accessToken,
        String refreshToken,
        AuthUserDto user,
        boolean newUser
) {}
