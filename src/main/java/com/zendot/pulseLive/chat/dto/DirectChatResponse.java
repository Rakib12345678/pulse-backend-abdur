package com.zendot.pulseLive.chat.dto;

public record DirectChatResponse(
        String chatApiKey,
        String chatChannelType,
        String chatChannelId,
        String chatToken,
        Long targetUserId
) {}
