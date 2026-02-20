package com.zendot.pulseLive.stream.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zendot.pulseLive.chat.service.StreamChatService;
import com.zendot.pulseLive.common.service.StreamTokenService;
import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.stream.dto.StreamGoLiveRequest;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import com.zendot.pulseLive.stream.service.StreamService;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
import io.getstream.models.QueryCallsResponse;
import io.getstream.models.framework.StreamResponse;
import io.getstream.services.Video;
import io.getstream.services.framework.StreamRequest;
import io.getstream.services.framework.StreamSDKClient;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "io.getstream.apiKey=stream-key",
        "io.getstream.chat.apiKey=chat-key",
        "stream.chat.channelType=livestream",
        "stream.callType=default",
        "stream.callDuration=3600",
        "cloudfront.url=https://cdn.example.com"
})
class StreamStartIntegrationTest {

    @Autowired
    private StreamService streamService;

    @MockitoBean
    private Video video;
    @MockitoBean
    private StreamTokenService tokenService;
    @MockitoBean
    private StreamSDKClient streamClient;
    @MockitoBean
    private StreamChatService streamChatService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private RelativePathUtils utils;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void startStream_returnsTokensWithSpringWiring() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));

        QueryCallsResponse data = new QueryCallsResponse();
        data.setCalls(new ArrayList<>());
        when(video.queryCalls(any())).thenReturn(streamRequestWithData(data));

        when(streamClient.updateUsers(any())).thenReturn(streamRequestWithData(null));
        when(video.getOrCreateCall(anyString(), anyString(), any())).thenReturn(streamRequestWithData(null));
        when(video.goLive(anyString(), anyString())).thenReturn(streamRequestWithData(null));
        when(tokenService.createCallToken(anyString(), anyString(), anyList(), anyInt())).thenReturn("video-token");
        when(utils.buildCdnUrl(anyString())).thenReturn("https://cdn.example.com/profile.png");
        when(streamChatService.createChatUserToken(anyString())).thenReturn("chat-token");

        StreamStartResponse response = streamService.startStream(new StreamGoLiveRequest("Title", "Category"));

        assertThat(response.token()).isEqualTo("video-token");
        assertThat(response.chatApiKey()).isEqualTo("chat-key");
        assertThat(response.chatChannelType()).isEqualTo("livestream");
    }

    private UserProfileResponse userProfile(boolean eighteenPlus) {
        return new UserProfileResponse(
                1L,
                "user@example.com",
                "alice",
                "Alice",
                null,
                null,
                new RelativePath("profile.png"),
                UserStatus.ACTIVE,
                true,
                eighteenPlus,
                0L,
                0L,
                0L,
                Instant.now()
        );
    }

    private <T> StreamRequest<T> streamRequestWithData(T data) throws Exception {
        StreamResponse<T> response = new StreamResponse<>();
        response.setData(data);
        return new StubStreamRequest<>(response);
    }

    private static class StubStreamRequest<T> extends StreamRequest<T> {
        private final StreamResponse<T> response;

        StubStreamRequest(StreamResponse<T> response) throws io.getstream.exceptions.StreamException {
            super(
                    new OkHttpClient(),
                    new ObjectMapper(),
                    "http://localhost",
                    "test-key",
                    "test-secret",
                    null,
                    Map.of(),
                    new TypeReference<>() {}
            );
            this.response = response;
        }

        @Override
        public StreamResponse<T> execute() {
            return response;
        }
    }
}
