package com.zendot.pulseLive.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamChatConfig {

    public StreamChatConfig(
            @Value("${io.getstream.chat.apiKey}") String apiKey,
            @Value("${io.getstream.chat.apiSecret}") String apiSecret
    ) {
        System.setProperty("io.getstream.chat.apiKey", apiKey);
        System.setProperty("io.getstream.chat.apiSecret", apiSecret);
    }
}
