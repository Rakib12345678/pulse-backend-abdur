package com.zendot.pulseLive.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank String idToken,
        Boolean eighteenPlus
) {}
