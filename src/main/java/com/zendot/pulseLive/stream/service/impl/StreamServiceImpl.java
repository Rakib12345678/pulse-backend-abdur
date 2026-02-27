package com.zendot.pulseLive.stream.service.impl;

import com.zendot.pulseLive.chat.service.StreamChatService;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.common.exception.ExternalServiceException;
import com.zendot.pulseLive.common.service.StreamTokenService;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.stream.dto.*;
import com.zendot.pulseLive.stream.model.LiveStream;
import com.zendot.pulseLive.stream.repo.LiveStreamRepository;
import com.zendot.pulseLive.stream.service.StreamService;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.service.UserService;
import io.getstream.exceptions.StreamException;
import io.getstream.models.*;
import io.getstream.models.framework.StreamResponse;
import io.getstream.services.Video;
import io.getstream.services.framework.StreamSDKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StreamServiceImpl implements StreamService {
    private final Video video;
    private final StreamTokenService tokenService;
    private final StreamSDKClient streamClient;
    private final StreamChatService streamChatService;

    @Value("${io.getstream.apiKey}")
    private String streamApiKey;

    @Value("${io.getstream.chat.apiKey:}")
    private String chatApiKey;

    @Value("${stream.chat.channelType:livestream}")
    private String chatChannelType;

    @Value("${stream.callType}")
    private String callType;

    @Value("${stream.callDuration}")
    private Integer callDuration;

    private final UserService userService;
    private final RelativePathUtils utils;
    private final LiveStreamRepository liveStreamRepository;

    private static final Logger log = LoggerFactory.getLogger(StreamServiceImpl.class);

    public StreamServiceImpl(Video video,
                             StreamTokenService tokenService,
                             StreamSDKClient streamClient,
                             StreamChatService streamChatService,
                             UserService userService,
                             RelativePathUtils utils,
                             LiveStreamRepository liveStreamRepository
    ) {
        this.video = video;
        this.tokenService = tokenService;
        this.streamClient = streamClient;
        this.streamChatService = streamChatService;
        this.userService = userService;
        this.utils = utils;
        this.liveStreamRepository = liveStreamRepository;
    }

    @Override
    public StreamStartResponse startStream(StreamGoLiveRequest request) {

        //  Generate Call ID
        String callId = UUID.randomUUID().toString();

        //  Get Current User
        UserProfileResponse user = userService.getCurrentUserProfile();

        if (user == null || !user.eighteenPlus()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User not allowed to go live");
        }

        String userId = String.valueOf(user.id());

        try {

            // Prevent duplicate streams: check if this user already has an ongoing live stream

            QueryCallsRequest queryCallsRequest = QueryCallsRequest.builder()
                    .filterConditions(Map.of(
                            "created_by_user_id", userId,
                            "ongoing", true
                    ))
                    .limit(1)
                    .build();

            QueryCallsResponse activeCallsResponse = video.queryCalls(queryCallsRequest).execute().getData();

            if (activeCallsResponse != null
                    && activeCallsResponse.getCalls() != null
                    && !activeCallsResponse.getCalls().isEmpty()) {

                CallStateResponseFields activeCall = activeCallsResponse.getCalls().get(0);
                String activeCallId = activeCall.getCall().getId();

                log.warn("User {} already has an active stream. callId={}", userId, activeCallId);
                throw new AppException(
                        HttpStatus.CONFLICT,
                        "You already have an active live stream. Please end it before starting a new one."
                );

            }

            liveStreamRepository.findFirstByCreatedByUserIdAndIsLiveTrueOrderByCreatedAtDesc(userId)
                    .ifPresent(active -> {
                        throw new AppException(
                                HttpStatus.CONFLICT,
                                "You already have an active live stream. Please end it before starting a new one."
                        );
                    });


            // Upsert Stream Video user
            String profileImageUrl = user.profileImage() != null
                    ? utils.buildCdnUrl(user.profileImage().getPath())
                    : null;
            streamClient.updateUsers(
                    UpdateUsersRequest.builder()
                            .users(Map.of(
                                    userId,
                                    UserRequest.builder()
                                            .id(userId)
                                            .name(user.username())
                                            .image(profileImageUrl)
                                            .role("user")
                                            .build()
                            ))
                            .build()
            ).execute();

            // Prepare call metadata
            CallRequest callData = new CallRequest();
            callData.setCreatedBy(UserRequest.builder().id(userId).build());

            Map<String, Object> data = new HashMap<>();
            data.put("title", request.title());
            data.put("category", request.category());
            data.put("username", user.username());
            if (profileImageUrl != null) {
                data.put("profileImage", profileImageUrl);
            }
            callData.setCustom(data);

            // Create call
            GetOrCreateCallRequest createCallRequest =
                    GetOrCreateCallRequest.builder()
                            .video(true)
                            .data(callData)
                            .build();

            video.getOrCreateCall(callType, callId, createCallRequest).execute();

            // Go live
            video.goLive(callType, callId).execute();

            // Video token
            String token = tokenService.createCallToken(
                    userId,
                    "host",
                    List.of(callId),
                    callDuration
            );

            LiveStream liveStream = new LiveStream();
            liveStream.setCallId(callId);
            liveStream.setCallType(callType);
            liveStream.setUsername(user.username());
            liveStream.setTitle(request.title());
            liveStream.setCategory(request.category());
            liveStream.setProfileImage(profileImageUrl);
            liveStream.setCreatedByUserId(userId);
            liveStream.setParticipantCount(1);
            liveStream.setFollowerCount(user.followerCount());
            liveStream.setIsLive(true);
            liveStreamRepository.save(liveStream);

            // Chat: ensure user + channel + token
            String chatToken = null;
            try {
                String hostImageUrl = (user.profileImage() != null)
                        ? utils.buildCdnUrl(user.profileImage().getPath())
                        : null;

                log.info("➡️ Chat step 1: upsertUser");
                streamChatService.upsertUser(user);
                log.info("➡️ Chat step 2: ensureLivestreamChannel");
                streamChatService.ensureLivestreamChannel(callId, user, request.title(), request.category(), hostImageUrl);
                log.info("➡️ Chat step 3: createChatUserToken");
                chatToken = streamChatService.createChatUserToken(userId);

                log.info("✅ Chat token created successfully");


            } catch (Exception e) {
                log.error("🔥 CHAT PROVISIONING FAILED for callId=" + callId, e);
            }


            String effectiveChatApiKey = (chatApiKey != null && !chatApiKey.isBlank()) ? chatApiKey : streamApiKey;

            return new StreamStartResponse(callId, callType, token, effectiveChatApiKey, chatChannelType, callId, chatToken);

        } catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error("unable to start stream. callId={}, streamStatus={}, responseData={}",
                    callId, streamStatus, e.getResponseData(), e);

            throw new ExternalServiceException(
                    "Unable to go live stream right now. Please try again.",
                    streamStatus,
                    e
            );
        }
    }


    @Override
    public StreamStartResponse joinStream(String callId) {

        try {
            StreamResponse<GetCallResponse> callResponse = video.getCall(callType, callId).execute();
            if (callResponse.getData() == null) {
                throw new AppException(HttpStatus.NOT_FOUND, "Call not found");
            }

            var call = callResponse.getData().getCall();

            // ✅ Get viewer (current user)
            UserProfileResponse user = userService.getCurrentUserProfile();
            String userId = String.valueOf(user.id());

            // ✅ Block check viewer vs host (THIS is what prevents chat too)
            String hostId = call.getCreatedBy().getId();
            Long hostUserId = Long.valueOf(hostId);
            Long viewerId = user.id();

            userService.ensureNotBlocked(viewerId, hostUserId);

            if (call.getEndedAt() != null) {
                throw new AppException(HttpStatus.GONE, "Stream already ended");
            }

            // Upsert Stream Video user
            streamClient.updateUsers(
                    UpdateUsersRequest.builder()
                            .users(Map.of(
                                    userId,
                                    UserRequest.builder()
                                            .id(userId)
                                            .name(user.username())
                                            .image(utils.buildCdnUrl(user.profileImage()!=null?user.profileImage().getPath():null))
                                            .role("user")
                                            .build()
                            ))
                            .build()
            ).execute();

            // Video token
            String token = tokenService.createCallToken(
                    userId,
                    "user",
                    List.of(callId),
                    callDuration
            );

            // Chat: add member + token (only after join allowed)
            String chatToken = null;
            try {
                streamChatService.upsertUser(user);
                streamChatService.addMemberToLivestream(callId, userId);
                chatToken = streamChatService.createChatUserToken(userId);
            } catch (Exception e) {
                log.warn("Chat join failed for callId={}, userId={}: {}", callId, userId, e.getMessage());
            }

            String effectiveChatApiKey = (chatApiKey != null && !chatApiKey.isBlank()) ? chatApiKey : streamApiKey;

            return new StreamStartResponse(callId, callType, token, effectiveChatApiKey, chatChannelType, callId, chatToken);

        } catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error("unable to join stream. callId={}, streamStatus={}, responseData={}",
                    callId, streamStatus, e.getResponseData(), e);

            throw new ExternalServiceException(
                    "Unable to join stream right now. Please try again.",
                    streamStatus,
                    e
            );
        }
    }

    @Override
    public void endStream(String callId) {
        try {
            StreamResponse<GetCallResponse> callResponse = video.getCall(callType, callId).execute();
            if (callResponse.getData() == null) {
                throw new AppException(HttpStatus.NOT_FOUND, "Call not found");
            }

            var call = callResponse.getData().getCall();

            if (call.getCreatedAt() == null) {
                throw new AppException(HttpStatus.CONFLICT, "Stream not started");
            }

            if (call.getEndedAt() != null) {
                throw new AppException(HttpStatus.GONE, "Stream already ended");
            }

            // 1) End Stream Video call
            video.endCall(callType, callId).execute();

            // 2) Auto-disable chat (freeze => read-only)
            try {
                streamChatService.freezeLivestreamChannel(callId);
            } catch (Exception e) {
                // Don’t fail end-stream if chat freeze fails
                log.warn("Chat freeze failed for callId={}: {}", callId, e.getMessage());
            }

            liveStreamRepository.findById(callId).ifPresent(stream -> {
                stream.setIsLive(false);
                liveStreamRepository.save(stream);
            });

        } catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error("Stream endStream failed. callId={}, streamStatus={}, responseData={}",
                    callId, streamStatus, e.getResponseData(), e);

            throw new ExternalServiceException(
                    "Unable to end stream right now. Please try again.",
                    streamStatus,
                    e
            );
        }
    }

    // ======== Feed code unchanged ========

    private String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    @Override
    public LiveFeedResponse<LiveCallResponse> getLiveFeed(String cursor, Integer limit) {
        int safeLimit = (limit == null || limit < 1) ? 10 : Math.min(limit, 50);
        Instant cursorInstant = (cursor == null || cursor.isBlank()) ? null : Instant.parse(cursor);

        var pageable = PageRequest.of(0, safeLimit + 1);
        List<LiveStream> streams = (cursorInstant == null)
                ? liveStreamRepository.findByIsLiveTrueOrderByCreatedAtDesc(pageable).getContent()
                : liveStreamRepository.findByIsLiveTrueAndCreatedAtBeforeOrderByCreatedAtDesc(cursorInstant, pageable).getContent();

        boolean hasMore = streams.size() > safeLimit;
        if (hasMore) {
            streams = streams.subList(0, safeLimit);
        }

        List<LiveCallResponse> data = streams.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasMore && !streams.isEmpty()) {
            nextCursor = streams.get(streams.size() - 1).getCreatedAt().toString();
        }

        return new LiveFeedResponse<>(data, nextCursor, hasMore);
    }

    private LiveCallResponse mapToResponse(LiveStream stream) {
        return new LiveCallResponse(
                stream.getUsername(),
                stream.getCallId(),
                stream.getCallType(),
                stream.getTitle(),
                stream.getCategory(),
                stream.getProfileImage(),
                stream.getCreatedByUserId(),
                stream.getParticipantCount(),
                stream.getFollowerCount(),
                stream.getCreatedAt() != null ? Date.from(stream.getCreatedAt()) : null,
                stream.getIsLive(),
                stream.getUpdatedAt() != null ? Date.from(stream.getUpdatedAt()) : null
        );
    }

    private String extractCallId(String cid) {
        if (cid == null) return null;
        String[] parts = cid.split(":");
        return parts.length > 1 ? parts[1] : cid;
    }

    @Override
    public UserLiveStatus getUserLiveStatusByUsername(String username) {
        return liveStreamRepository
                .findFirstByUsernameAndIsLiveTrueOrderByCreatedAtDesc(username)
                .map(stream -> new UserLiveStatus(true, stream.getCallId(), username))
                .orElseGet(() -> new UserLiveStatus(false, null, username));
    }

    @Override
    @Transactional
    public LiveCallResponse syncParticipantCount(String callId) {
        LiveStream stream = liveStreamRepository.findByCallIdAndIsLiveTrue(callId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Live stream not found"));

        UserProfileResponse user = userService.getCurrentUserProfile();
        if (!stream.getCreatedByUserId().equals(String.valueOf(user.id()))) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only host can sync participants");
        }

        try {
            StreamResponse<GetCallResponse> callResponse = video.getCall(callType, callId).execute();
            GetCallResponse data = callResponse.getData();
            if (data == null || data.getCall() == null) {
                throw new AppException(HttpStatus.NOT_FOUND, "Call not found");
            }

            CallResponse call = data.getCall();
            int participantCount = 0;
            if (call.getSession() != null && call.getSession().getParticipants() != null) {
                participantCount = call.getSession().getParticipants().size();
            }

            stream.setParticipantCount(participantCount);
            liveStreamRepository.save(stream);

            return mapToResponse(stream);
        } catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error("syncParticipantCount failed. callId={}, streamStatus={}, responseData={}",
                    callId, streamStatus, e.getResponseData(), e);

            throw new ExternalServiceException(
                    "Unable to sync participants right now. Please try again.",
                    streamStatus,
                    e
            );
        }
    }

    @Transactional
    @Scheduled(fixedDelayString = "${stream.live-stale-check-ms:60000}")
    public void markStaleStreamsOffline() {
        Instant threshold = Instant.now().minusSeconds(60);
        List<LiveStream> staleStreams = liveStreamRepository.findByIsLiveTrueAndUpdatedAtBefore(threshold);
        if (staleStreams.isEmpty()) {
            return;
        }

        staleStreams.forEach(stream -> stream.setIsLive(false));
        liveStreamRepository.saveAll(staleStreams);
        log.info("Marked {} stale live stream(s) as offline", staleStreams.size());
    }

}
