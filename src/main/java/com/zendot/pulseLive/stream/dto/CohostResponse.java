package com.zendot.pulseLive.stream.dto;

import com.zendot.pulseLive.stream.model.CohostStatus;

public record CohostResponse(
        String username,
        String profileImage,
        CohostStatus status
) {}
