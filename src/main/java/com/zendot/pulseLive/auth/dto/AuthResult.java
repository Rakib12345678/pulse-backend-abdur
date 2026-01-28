package com.zendot.pulseLive.auth.dto;

import com.zendot.pulseLive.user.model.User;

public record AuthResult(
        String accessToken,
        String refreshToken,
        User user,
        boolean newUser
) {}
