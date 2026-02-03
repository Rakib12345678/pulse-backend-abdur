package com.zendot.pulseLive.user.dto;

import com.zendot.pulseLive.user.model.UserStatus;
import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String username,
        String fullName,
        String bio,
        String location,
        String profileImageUrl,
        UserStatus status,
        boolean emailVerified,
        boolean eighteenPlus,
        Instant createdAt
) {}
