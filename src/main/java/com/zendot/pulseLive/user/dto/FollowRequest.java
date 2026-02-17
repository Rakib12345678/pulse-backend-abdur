package com.zendot.pulseLive.user.dto;

public record FollowRequest(
        Long targetUserId,
        boolean follow
) {}
