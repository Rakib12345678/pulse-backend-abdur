package com.zendot.pulseLive.stream.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

public record JoinCallResponse(
        String callId,
        String token
) {}

