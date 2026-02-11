package com.zendot.pulseLive.user.dto;

import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.user.model.UserStatus;

import java.time.Instant;

public record UserPublicProfileResponse(
        Long id,
        String username,
        String fullName,
        String bio,
        String location,
        RelativePath profileImage,
        UserStatus status,
        boolean eighteenPlus,
        Long followerCount,
        Long followingCount,
        Long postCount,
        Instant createdAt,
        boolean followedByMe
) {}

