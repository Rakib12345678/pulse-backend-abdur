package com.zendot.pulseLive.stream.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCohostRequest(
        @NotBlank String callId,
        @NotBlank String username,
        boolean add
) {}
