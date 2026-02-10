package com.zendot.pulseLive.stream.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

public record GoLiveResponse(
        String callId,
        String status
) {}
