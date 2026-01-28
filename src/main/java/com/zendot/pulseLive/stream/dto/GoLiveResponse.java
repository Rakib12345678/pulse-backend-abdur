package com.zendot.pulseLive.stream.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoLiveResponse {
    private String callId;
    private String status;
}
