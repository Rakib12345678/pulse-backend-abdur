package com.zendot.pulseLive.common.config;


import io.getstream.services.Video;
import io.getstream.services.framework.StreamSDKClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamConfig {

    @Bean
    public StreamSDKClient streamSDKClient() {
        return new StreamSDKClient(
                "wcvnfqnmkx95",

                "s3xdpmsqm5zejv7qybr8up59gu2bmkkvusjrcnhmntmkntphhsp63cursmdasf9b".trim()
        );
    }

    @Bean
    public Video video(StreamSDKClient client) {
        return client.video(); // returns VideoClient internally
    }
}
