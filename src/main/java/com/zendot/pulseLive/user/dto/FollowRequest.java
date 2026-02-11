package com.zendot.pulseLive.user.dto;

import lombok.Data;

public record FollowRequest(
        Long targetUserId,
        boolean follow
) {}
