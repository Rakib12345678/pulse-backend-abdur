package com.zendot.pulseLive.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleLoginRequest(
        @NotBlank String idToken,
        @NotNull Boolean eighteenPlus
) {}
