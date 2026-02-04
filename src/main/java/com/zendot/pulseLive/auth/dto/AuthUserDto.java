package com.zendot.pulseLive.auth.dto;

import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.user.model.UserStatus;

import java.time.Instant;

public record AuthUserDto(
        Long id,
        String email,
        String username,
        String fullName,
        String bio,
        String location,
        RelativePath profileImage,
        UserStatus status,
        boolean emailVerified,
        boolean eighteenPlus,
        Instant createdAt
) {}
