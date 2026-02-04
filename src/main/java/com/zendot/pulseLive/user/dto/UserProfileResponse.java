package com.zendot.pulseLive.user.dto;

import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.user.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String username,
        String fullName,
        String bio,
        String location,
        @Schema(
                description = "Profile image CDN URL",
                example = "https://cdn.example.com/media/image/profile-image/123/avatar.jpg"
        )
        RelativePath profileImage,

        UserStatus status,
        boolean emailVerified,
        boolean eighteenPlus,
        Instant createdAt
) {}
