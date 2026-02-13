package com.zendot.pulseLive.stream.dto;

public record UserLiveStatus(
        boolean isLive,
        String callId,
        String username
) {}

