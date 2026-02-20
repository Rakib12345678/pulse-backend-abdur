package com.zendot.pulseLive.stream.service;

import com.zendot.pulseLive.chat.service.StreamChatService;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.common.exception.ExternalServiceException;
import com.zendot.pulseLive.common.service.StreamTokenService;
import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.stream.dto.LiveFeedResponse;
import com.zendot.pulseLive.stream.dto.StreamGoLiveRequest;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import com.zendot.pulseLive.stream.dto.UserLiveStatus;
import com.zendot.pulseLive.stream.service.impl.StreamServiceImpl;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getstream.exceptions.StreamException;
import io.getstream.exceptions.StreamException;
import io.getstream.models.CallParticipantResponse;
import io.getstream.models.CallResponse;
import io.getstream.models.CallSessionResponse;
import io.getstream.models.CallStateResponseFields;
import io.getstream.models.GetCallResponse;
import io.getstream.models.QueryCallsResponse;
import io.getstream.models.UserResponse;
import io.getstream.models.framework.StreamResponse;
import io.getstream.services.Video;
import io.getstream.services.framework.StreamRequest;
import io.getstream.services.framework.StreamSDKClient;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamServiceImplTest {

    @Mock
    private Video video;
    @Mock
    private StreamTokenService tokenService;
    @Mock
    private StreamSDKClient streamClient;
    @Mock
    private StreamChatService streamChatService;
    @Mock
    private UserService userService;
    @Mock
    private RelativePathUtils utils;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StreamServiceImpl streamService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(streamService, "streamApiKey", "stream-key");
        ReflectionTestUtils.setField(streamService, "chatApiKey", "");
        ReflectionTestUtils.setField(streamService, "chatChannelType", "livestream");
        ReflectionTestUtils.setField(streamService, "callType", "default");
        ReflectionTestUtils.setField(streamService, "callDuration", 3600);
    }

    @Test
    void startStream_throwsWhenUserNotAllowed() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(false));

        assertThatThrownBy(() -> streamService.startStream(new StreamGoLiveRequest("Title", "Category")))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(video, never()).getOrCreateCall(anyString(), anyString(), any());
    }

    @Test
    void startStream_throwsConflictWhenActiveStreamExists() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));

        CallResponse activeCall = buildCall("call-1", "default", new Date(), null, "1", Map.of(), 0);
        CallStateResponseFields state = new CallStateResponseFields();
        state.setCall(activeCall);

        QueryCallsResponse data = new QueryCallsResponse();
        data.setCalls(List.of(state));

        when(video.queryCalls(any())).thenReturn(streamRequestWithData(data));

        assertThatThrownBy(() -> streamService.startStream(new StreamGoLiveRequest("Title", "Category")))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(video, never()).getOrCreateCall(anyString(), anyString(), any());
    }

    @Test
    void joinStream_throwsGoneWhenCallEnded() throws Exception {
        CallResponse call = buildCall("call-1", "default", new Date(), new Date(), "99", Map.of(), 0);
        GetCallResponse data = new GetCallResponse();
        data.setCall(call);

        when(video.getCall(anyString(), anyString())).thenReturn(streamRequestWithData(data));
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));

        assertThatThrownBy(() -> streamService.joinStream("call-1"))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.GONE);
    }

    @Test
    void joinStream_returnsTokensAndChecksBlockList() throws Exception {
        CallResponse call = buildCall("call-1", "default", new Date(), null, "99", Map.of(), 0);
        GetCallResponse data = new GetCallResponse();
        data.setCall(call);

        when(video.getCall(anyString(), anyString())).thenReturn(streamRequestWithData(data));
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));
        when(utils.buildCdnUrl(anyString())).thenReturn("https://cdn.example.com/profile.png");
        when(streamClient.updateUsers(any())).thenReturn(streamRequestWithData(null));
        when(tokenService.createCallToken(anyString(), anyString(), anyList(), anyInt())).thenReturn("video-token");
        when(streamChatService.createChatUserToken(anyString())).thenReturn("chat-token");

        StreamStartResponse response = streamService.joinStream("call-1");

        verify(userService).ensureNotBlocked(1L, 99L);
        verify(tokenService).createCallToken(anyString(), anyString(), anyList(), anyInt());
        assertThat(response.token()).isEqualTo("video-token");
        assertThat(response.chatApiKey()).isEqualTo("stream-key");
        assertThat(response.chatToken()).isEqualTo("chat-token");
        assertThat(response.callId()).isEqualTo("call-1");
    }

    @Test
    void joinStream_throwsExternalServiceExceptionOnStreamFailure() throws Exception {
        when(video.getCall(anyString(), anyString()))
                .thenThrow(streamExceptionWithStatus(503));

        assertThatThrownBy(() -> streamService.joinStream("call-1"))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("status")
                .isEqualTo(503);
    }

    @Test
    void endStream_throwsConflictWhenNotStarted() throws Exception {
        CallResponse call = buildCall("call-1", "default", null, null, "1", Map.of(), 0);
        GetCallResponse data = new GetCallResponse();
        data.setCall(call);

        when(video.getCall(anyString(), anyString())).thenReturn(streamRequestWithData(data));

        assertThatThrownBy(() -> streamService.endStream("call-1"))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void endStream_endsCallEvenIfChatFreezeFails() throws Exception {
        CallResponse call = buildCall("call-1", "default", new Date(), null, "1", Map.of(), 0);
        GetCallResponse data = new GetCallResponse();
        data.setCall(call);

        when(video.getCall(anyString(), anyString())).thenReturn(streamRequestWithData(data));
        when(video.endCall(anyString(), anyString())).thenReturn(streamRequestWithData(null));
        doThrow(new RuntimeException("chat error"))
                .when(streamChatService)
                .freezeLivestreamChannel("call-1");

        streamService.endStream("call-1");

        verify(video).endCall("default", "call-1");
    }

    @Test
    void endStream_throwsExternalServiceExceptionOnStreamFailure() throws Exception {
        when(video.getCall(anyString(), anyString()))
                .thenThrow(streamExceptionWithStatus(500));

        assertThatThrownBy(() -> streamService.endStream("call-1"))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("status")
                .isEqualTo(500);
    }

    @Test
    void getLiveFeed_paginatesAndSetsCursor() throws Exception {
        CallResponse first = buildCall("call-1", "default", Date.from(Instant.parse("2024-01-01T10:00:00Z")),
                null, "1",
                Map.of("username", "alice", "title", "T1", "category", "C1", "profileImage", "img1"), 2);
        CallResponse second = buildCall("call-2", "default", Date.from(Instant.parse("2024-01-01T09:00:00Z")),
                null, "2",
                Map.of("username", "bob", "title", "T2", "category", "C2", "profileImage", "img2"), 1);

        QueryCallsResponse data = new QueryCallsResponse();
        data.setCalls(List.of(callState(first), callState(second)));

        when(video.queryCalls(any())).thenReturn(streamRequestWithData(data));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userEntity("alice", 10)));

        LiveFeedResponse<?> response = streamService.getLiveFeed(null, 1);

        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("2024-01-01T10:00:00Z");
        assertThat(response.data()).hasSize(1);
    }

    @Test
    void getLiveFeed_throwsExternalServiceExceptionOnStreamFailure() throws Exception {
        when(video.queryCalls(any())).thenThrow(streamExceptionWithStatus(502));

        assertThatThrownBy(() -> streamService.getLiveFeed(null, 10))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("status")
                .isEqualTo(502);
    }

    @Test
    void getUserLiveStatusByUsername_returnsFalseWhenNoCalls() throws Exception {
        QueryCallsResponse data = new QueryCallsResponse();
        data.setCalls(new ArrayList<>());

        when(video.queryCalls(any())).thenReturn(streamRequestWithData(data));

        UserLiveStatus status = streamService.getUserLiveStatusByUsername("alice");

        assertThat(status.isLive()).isFalse();
        assertThat(status.username()).isEqualTo("alice");
    }

    @Test
    void getUserLiveStatusByUsername_returnsTrueWhenCallActive() throws Exception {
        CallResponse call = buildCall("call-9", "default", new Date(), null, "1", Map.of(), 0);
        CallStateResponseFields state = new CallStateResponseFields();
        state.setCall(call);

        QueryCallsResponse data = new QueryCallsResponse();
        data.setCalls(List.of(state));

        when(video.queryCalls(any())).thenReturn(streamRequestWithData(data));

        UserLiveStatus status = streamService.getUserLiveStatusByUsername("alice");

        assertThat(status.isLive()).isTrue();
        assertThat(status.callId()).isEqualTo("call-9");
    }

    @Test
    void getUserLiveStatusByUsername_throwsExternalServiceExceptionOnStreamFailure() throws Exception {
        when(video.queryCalls(any())).thenThrow(streamExceptionWithStatus(500));

        assertThatThrownBy(() -> streamService.getUserLiveStatusByUsername("alice"))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("status")
                .isEqualTo(500);
    }

    @Test
    void startStream_throwsExternalServiceExceptionOnStreamFailure() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));
        when(video.queryCalls(any())).thenThrow(streamExceptionWithStatus(502));

        assertThatThrownBy(() -> streamService.startStream(new StreamGoLiveRequest("Title", "Category")))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("status")
                .isEqualTo(502);
    }

    @Test
    void startStream_returnsResponseWhenChatFails() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));

        QueryCallsResponse data = new QueryCallsResponse();
        data.setCalls(new ArrayList<>());

        when(video.queryCalls(any())).thenReturn(streamRequestWithData(data));
        when(streamClient.updateUsers(any())).thenReturn(streamRequestWithData(null));
        when(video.getOrCreateCall(anyString(), anyString(), any())).thenReturn(streamRequestWithData(null));
        when(video.goLive(anyString(), anyString())).thenReturn(streamRequestWithData(null));
        when(tokenService.createCallToken(anyString(), anyString(), anyList(), anyInt())).thenReturn("video-token");
        when(streamChatService.createChatUserToken(anyString()))
                .thenThrow(new RuntimeException("chat error"));

        StreamStartResponse response = streamService.startStream(new StreamGoLiveRequest("Title", "Category"));

        assertThat(response.token()).isEqualTo("video-token");
        assertThat(response.chatToken()).isNull();
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

    private User userEntity(String username, long followers) {
        User user = new User();
        user.setUsername(username);
        user.setFollowerCount(followers);
        return user;
    }

    private CallStateResponseFields callState(CallResponse call) {
        CallStateResponseFields state = new CallStateResponseFields();
        state.setCall(call);
        return state;
    }

    private CallResponse buildCall(
            String id,
            String type,
            Date createdAt,
            Date endedAt,
            String createdById,
            Map<String, Object> custom,
            int participants
    ) {
        CallResponse call = new CallResponse();
        call.setId(id);
        call.setType(type);
        call.setCid(type + ":" + id);
        call.setCreatedAt(createdAt);
        call.setEndedAt(endedAt);
        call.setCustom(custom);

        UserResponse createdBy = new UserResponse();
        createdBy.setId(createdById);
        call.setCreatedBy(createdBy);

        CallSessionResponse session = new CallSessionResponse();
        List<CallParticipantResponse> list = new ArrayList<>();
        for (int i = 0; i < participants; i++) {
            list.add(new CallParticipantResponse());
        }
        session.setParticipants(list);
        call.setSession(session);

        return call;
    }

    private <T> StreamRequest<T> streamRequestWithData(T data) throws Exception {
        StreamResponse<T> response = new StreamResponse<>();
        response.setData(data);
        return new StubStreamRequest<>(response);
    }

    private StreamException streamExceptionWithStatus(int status) {
        StreamException.ResponseData responseData = new StreamException.ResponseData();
        responseData.setStatusCode(status);
        return new StreamException("stream failure", responseData);
    }

    private static class StubStreamRequest<T> extends StreamRequest<T> {
        private final StreamResponse<T> response;

        StubStreamRequest(StreamResponse<T> response) throws StreamException {
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
