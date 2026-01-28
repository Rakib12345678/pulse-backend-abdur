package com.zendot.pulseLive.stream.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinCallResponse {
    private String callId;
    private String token;
}

