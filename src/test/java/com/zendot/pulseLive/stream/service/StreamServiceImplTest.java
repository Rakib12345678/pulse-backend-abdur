package com.zendot.pulseLive.stream.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.zendot.pulseLive.stream.dto.AddCohostRequest;
import com.zendot.pulseLive.stream.model.CohostStatus;
import com.zendot.pulseLive.stream.model.LiveStream;
import com.zendot.pulseLive.stream.model.LiveStreamCohost;
import com.zendot.pulseLive.stream.repo.LiveStreamCohostRepository;
import com.zendot.pulseLive.stream.repo.LiveStreamRepository;
import com.zendot.pulseLive.stream.service.impl.StreamServiceImpl;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserStatus;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private LiveStreamRepository liveStreamRepository;
    @Mock
    private LiveStreamCohostRepository liveStreamCohostRepository;

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
    void startStream_throwsConflictWhenLocalActiveStreamExists() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));
        when(video.queryCalls(any())).thenReturn(streamRequestWithData(new QueryCallsResponse()));

        LiveStream live = new LiveStream();
        live.setCallId("existing");
        when(liveStreamRepository.findFirstByCreatedByUserIdAndIsLiveTrueOrderByCreatedAtDesc("1"))
                .thenReturn(Optional.of(live));

        assertThatThrownBy(() -> streamService.startStream(new StreamGoLiveRequest("Title", "Category")))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void startStream_returnsResponseWhenChatFails() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));
        when(video.queryCalls(any())).thenReturn(streamRequestWithData(new QueryCallsResponse()));
        when(liveStreamRepository.findFirstByCreatedByUserIdAndIsLiveTrueOrderByCreatedAtDesc("1"))
                .thenReturn(Optional.empty());
        when(streamClient.updateUsers(any())).thenReturn(streamRequestWithData(null));
        when(video.getOrCreateCall(anyString(), anyString(), any())).thenReturn(streamRequestWithData(null));
        when(video.goLive(anyString(), anyString())).thenReturn(streamRequestWithData(null));
        when(tokenService.createCallToken(anyString(), anyString(), anyList(), anyInt())).thenReturn("video-token");
        when(liveStreamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(streamChatService.createChatUserToken(anyString())).thenThrow(new RuntimeException("chat error"));

        StreamStartResponse response = streamService.startStream(new StreamGoLiveRequest("Title", "Category"));

        assertThat(response.token()).isEqualTo("video-token");
        assertThat(response.chatToken()).isNull();
    }

    @Test
    void startStream_returnsTokensWithExpectedChatSettings() throws Exception {
        ReflectionTestUtils.setField(streamService, "chatApiKey", "chat-key");
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));
        when(video.queryCalls(any())).thenReturn(streamRequestWithData(new QueryCallsResponse()));
        when(liveStreamRepository.findFirstByCreatedByUserIdAndIsLiveTrueOrderByCreatedAtDesc("1"))
                .thenReturn(Optional.empty());
        when(streamClient.updateUsers(any())).thenReturn(streamRequestWithData(null));
        when(video.getOrCreateCall(anyString(), anyString(), any())).thenReturn(streamRequestWithData(null));
        when(video.goLive(anyString(), anyString())).thenReturn(streamRequestWithData(null));
        when(tokenService.createCallToken(anyString(), anyString(), anyList(), anyInt())).thenReturn("video-token");
        when(liveStreamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(streamChatService.createChatUserToken(anyString())).thenReturn("chat-token");

        StreamStartResponse response = streamService.startStream(new StreamGoLiveRequest("Title", "Category"));

        assertThat(response.token()).isEqualTo("video-token");
        assertThat(response.chatApiKey()).isEqualTo("chat-key");
        assertThat(response.chatChannelType()).isEqualTo("livestream");
    }

    @Test
    void joinStream_throwsGoneWhenCallEnded() throws Exception {
        CallResponse call = buildCall("call-1", "default", new Date(), new Date(), "99", Map.of());
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
    void getLiveFeed_readsFromLocalDb() {
        LiveStream first = live("call-1", Instant.parse("2024-01-01T10:00:00Z"));
        LiveStream second = live("call-2", Instant.parse("2024-01-01T09:00:00Z"));
        when(liveStreamRepository.findByIsLiveTrueOrderByCreatedAtDesc(PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(List.of(first, second)));
        when(liveStreamCohostRepository.findByCallIdIn(List.of("call-1"))).thenReturn(List.of());

        LiveFeedResponse<?> response = streamService.getLiveFeed(null, 1);

        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("2024-01-01T10:00:00Z");
        assertThat(response.data()).hasSize(1);
    }

    @Test
    void getUserLiveStatusByUsername_readsFromLocalDb() {
        when(liveStreamRepository.findFirstByUsernameAndIsLiveTrueOrderByCreatedAtDesc("alice"))
                .thenReturn(Optional.of(live("call-9", Instant.now())));

        UserLiveStatus status = streamService.getUserLiveStatusByUsername("alice");

        assertThat(status.isLive()).isTrue();
        assertThat(status.callId()).isEqualTo("call-9");
    }

    @Test
    void getUserLiveStatusByUsername_returnsFalseWhenNoLocalRow() {
        when(liveStreamRepository.findFirstByUsernameAndIsLiveTrueOrderByCreatedAtDesc("alice"))
                .thenReturn(Optional.empty());

        UserLiveStatus status = streamService.getUserLiveStatusByUsername("alice");

        assertThat(status.isLive()).isFalse();
        assertThat(status.callId()).isNull();
    }

    @Test
    void syncParticipantCount_updatesCountWhenHostRequests() throws Exception {
        LiveStream stream = live("call-1", Instant.now());
        when(liveStreamRepository.findByCallIdAndIsLiveTrue("call-1"))
                .thenReturn(Optional.of(stream));
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));

        CallResponse call = buildCall("call-1", "default", new Date(), null, "1", Map.of());
        CallSessionResponse session = new CallSessionResponse();
        session.setParticipants(List.of(
                new CallParticipantResponse(),
                new CallParticipantResponse(),
                new CallParticipantResponse()
        ));
        call.setSession(session);
        GetCallResponse data = new GetCallResponse();
        data.setCall(call);
        when(video.getCall(anyString(), eq("call-1"))).thenReturn(streamRequestWithData(data));
        when(liveStreamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(liveStreamCohostRepository.findByCallIdOrderByIdAsc("call-1")).thenReturn(List.of());

        var response = streamService.syncParticipantCount("call-1");

        assertThat(response.participantCount()).isEqualTo(3);
        assertThat(response.isLive()).isTrue();
        verify(liveStreamRepository).save(stream);
    }

    @Test
    void syncParticipantCount_throwsForbiddenWhenNonHostRequests() {
        LiveStream stream = live("call-1", Instant.now());
        stream.setCreatedByUserId("99");
        when(liveStreamRepository.findByCallIdAndIsLiveTrue("call-1"))
                .thenReturn(Optional.of(stream));
        when(userService.getCurrentUserProfile()).thenReturn(userProfile(true));

        assertThatThrownBy(() -> streamService.syncParticipantCount("call-1"))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void markStaleStreamsOffline_marksOldRowsNotLive() {
        LiveStream stale = live("call-1", Instant.now().minusSeconds(100));
        stale.setUpdatedAt(Instant.now().minusSeconds(100));
        stale.setIsLive(true);
        when(liveStreamRepository.findByIsLiveTrueAndUpdatedAtBefore(any()))
                .thenReturn(List.of(stale));

        streamService.markStaleStreamsOffline();

        assertThat(stale.getIsLive()).isFalse();
        verify(liveStreamRepository).saveAll(any());
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
    void updateCohost_addsWhenUnderLimit() {
        LiveStream stream = live("call-1", Instant.now());
        User user = new User();
        user.setUsername("bob");
        user.setProfileImagePath("users/bob.png");
        LiveStreamCohost saved = new LiveStreamCohost();
        saved.setCallId("call-1");
        saved.setUsername("bob");
        saved.setProfileImage("users/bob.png");
        saved.setStatus(CohostStatus.ACTIVE);

        when(liveStreamRepository.findByCallIdAndIsLiveTrue("call-1")).thenReturn(Optional.of(stream));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(liveStreamCohostRepository.findByCallIdAndUsername("call-1", "bob")).thenReturn(Optional.empty());
        when(liveStreamCohostRepository.countByCallIdAndStatus("call-1", CohostStatus.ACTIVE)).thenReturn(2L);
        when(liveStreamCohostRepository.findByCallIdOrderByIdAsc("call-1")).thenReturn(List.of(saved));
        when(utils.buildCdnUrl("users/bob.png")).thenReturn("https://cdn/users/bob.png");

        var response = streamService.updateCohost(new AddCohostRequest("call-1", "bob", true));

        assertThat(response.cohosts()).hasSize(1);
        assertThat(response.cohosts().get(0).username()).isEqualTo("bob");
        assertThat(response.cohosts().get(0).status()).isEqualTo(CohostStatus.ACTIVE);
    }

    @Test
    void updateCohost_leaveMarksInactive() {
        LiveStream stream = live("call-1", Instant.now());
        User user = new User();
        user.setUsername("bob");
        user.setProfileImagePath("users/bob.png");
        LiveStreamCohost existing = new LiveStreamCohost();
        existing.setCallId("call-1");
        existing.setUsername("bob");
        existing.setStatus(CohostStatus.ACTIVE);
        LiveStreamCohost inactive = new LiveStreamCohost();
        inactive.setCallId("call-1");
        inactive.setUsername("bob");
        inactive.setProfileImage("users/bob.png");
        inactive.setStatus(CohostStatus.INACTIVE);

        when(liveStreamRepository.findByCallIdAndIsLiveTrue("call-1")).thenReturn(Optional.of(stream));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(liveStreamCohostRepository.findByCallIdAndUsername("call-1", "bob")).thenReturn(Optional.of(existing));
        when(liveStreamCohostRepository.findByCallIdOrderByIdAsc("call-1")).thenReturn(List.of(inactive));
        when(utils.buildCdnUrl("users/bob.png")).thenReturn("https://cdn/users/bob.png");

        var response = streamService.updateCohost(new AddCohostRequest("call-1", "bob", false));

        assertThat(response.cohosts()).hasSize(1);
        assertThat(response.cohosts().get(0).status()).isEqualTo(CohostStatus.INACTIVE);
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
                10L,
                0L,
                0L,
                Instant.now()
        );
    }

    private LiveStream live(String callId, Instant createdAt) {
        LiveStream stream = new LiveStream();
        stream.setCallId(callId);
        stream.setCallType("default");
        stream.setUsername("alice");
        stream.setTitle("Title");
        stream.setCategory("Category");
        stream.setCreatedByUserId("1");
        stream.setParticipantCount(1);
        stream.setFollowerCount(10L);
        stream.setIsLive(true);
        stream.setCreatedAt(createdAt);
        stream.setUpdatedAt(createdAt);
        return stream;
    }

    private CallResponse buildCall(
            String id,
            String type,
            Date createdAt,
            Date endedAt,
            String createdById,
            Map<String, Object> custom
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
