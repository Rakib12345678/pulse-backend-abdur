package com.zendot.pulseLive.stream.dto;

public record StreamStartResponse(
        String callId,
        String callType,
        String token
){}
