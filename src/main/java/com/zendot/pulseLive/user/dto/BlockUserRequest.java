package com.zendot.pulseLive.user.dto;

public record BlockUserRequest(
        Long targetUserId,
        boolean block
) {}
