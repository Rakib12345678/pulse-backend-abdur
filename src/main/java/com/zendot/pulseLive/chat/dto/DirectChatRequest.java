package com.zendot.pulseLive.chat.dto;

import jakarta.validation.constraints.NotNull;

public record DirectChatRequest(
        @NotNull Long targetUserId
) {}
