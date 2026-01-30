package com.zendot.pulseLive.user.dto;

import jakarta.validation.constraints.NotNull;

public record EighteenPlusConsentRequest(
        @NotNull Boolean eighteenPlus
) {}
