package com.zendot.pulseLive.stream.service.impl;

import com.zendot.pulseLive.chat.service.StreamChatService;
import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.common.exception.ExternalServiceException;
import com.zendot.pulseLive.common.service.StreamTokenService;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.stream.dto.*;
import com.zendot.pulseLive.stream.service.StreamService;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
import io.getstream.exceptions.StreamException;
import io.getstream.models.*;
import io.getstream.models.framework.StreamResponse;
import io.getstream.services.Video;
import io.getstream.services.framework.StreamSDKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
    private final UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(StreamServiceImpl.class);

    public StreamServiceImpl(Video video,
                             StreamTokenService tokenService,
                             StreamSDKClient streamClient,
                             StreamChatService streamChatService,
                             UserService userService,
                             RelativePathUtils utils, UserRepository userRepository) {
        this.video = video;
        this.tokenService = tokenService;
        this.streamClient = streamClient;
        this.streamChatService = streamChatService;
        this.userService = userService;
        this.utils = utils;
        this.userRepository = userRepository;
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
            // Upsert Stream Video user
            streamClient.updateUsers(
                    UpdateUsersRequest.builder()
                            .users(Map.of(
                                    userId,
                                    UserRequest.builder()
                                            .id(userId)
                                            .name(user.username())
                                            .image(utils.buildCdnUrl(user.profileImage().getPath()))
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
            if (user.profileImage() != null) {
                data.put("profileImage", utils.buildCdnUrl(user.profileImage().getPath()));
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
            if (call.getEndedAt() != null) {
                throw new AppException(HttpStatus.GONE, "Stream already ended");
            }

            UserProfileResponse user = userService.getCurrentUserProfile();
            String userId = String.valueOf(user.id());

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

            // Chat: add member + token
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

        try {
            Date cursorDate = null;

            if (cursor != null && !cursor.isBlank()) {
                cursorDate = Date.from(Instant.parse(cursor));
            }

            Map<String, Object> filter = new HashMap<>();
            filter.put("type", callType);
            filter.put("ended_at", null);

            if (cursorDate != null) {
                filter.put("created_at", Map.of("$lt", cursorDate));
            }

            List<SortParamRequest> sort = List.of(
                    SortParamRequest.builder()
                            .field("created_at")
                            .direction(-1)
                            .build()
            );

            QueryCallsRequest request = QueryCallsRequest.builder()
                    .filterConditions(filter)
                    .sort(sort)
                    .limit(limit + 1)
                    .build();

            StreamResponse<QueryCallsResponse> response = video.queryCalls(request).execute();
            List<CallStateResponseFields> calls = response.getData().getCalls();

            boolean hasMore = calls.size() > limit;
            if (hasMore) calls = calls.subList(0, limit);

            List<LiveCallResponse> data = calls.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            String nextCursor = null;
            if (hasMore && !calls.isEmpty()) {
                CallStateResponseFields lastState = calls.get(calls.size() - 1);
                if (lastState.getCall() != null && lastState.getCall().getCreatedAt() != null) {
                    nextCursor = lastState.getCall().getCreatedAt().toInstant().toString();
                }
            }

            return new LiveFeedResponse<>(data, nextCursor, hasMore);

        } catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error("Stream getLiveFeed failed. cursor={}, limit={}, streamStatus={}, responseData={}",
                    cursor, limit, streamStatus, e.getResponseData(), e);

            throw new ExternalServiceException(
                    "Unable to get live feed right now. Please try again.",
                    streamStatus,
                    e
            );
        }
    }

    private LiveCallResponse mapToResponse(CallStateResponseFields state) {
        CallResponse call = state.getCall();

        return new LiveCallResponse(
                getString(call.getCustom(), "username"),
                extractCallId(call.getCid()),
                call.getType(),
                getString(call.getCustom(), "title"),
                getString(call.getCustom(), "category"),
                getString(call.getCustom(), "profileImage"),
                call.getCreatedBy() != null ? call.getCreatedBy().getId() : null,
                state.getCall().getSession() != null
                        ? state.getCall().getSession().getParticipants().size()
                        : 0,
                userRepository.findByUsername(getString(call.getCustom(),"username")).get().getFollowerCount(),
                call.getCreatedAt() != null ? Date.from(call.getCreatedAt().toInstant()) : null,
                call.getEndedAt() == null
        );
    }

    private String extractCallId(String cid) {
        if (cid == null) return null;
        String[] parts = cid.split(":");
        return parts.length > 1 ? parts[1] : cid;
    }

    @Override
    public UserLiveStatus getUserLiveStatusByUsername(String username) {

        try {
            Map<String, Object> filter =new HashMap<>();
            filter.put("type",callType);
            filter.put("ended_at",null);
            filter.put("custom.username",username);


            QueryCallsRequest request = QueryCallsRequest.builder()
                    .filterConditions(filter)
                    .limit(1)
                    .build();

            StreamResponse<QueryCallsResponse> response =
                    video.queryCalls(request).execute();

            List<CallStateResponseFields> calls =
                    response.getData().getCalls();

            if (calls == null || calls.isEmpty()) {
                return new UserLiveStatus(false, null, username);
            }

            var call = calls.get(0).getCall();

            String callId = extractCallId(call.getCid());

            return new UserLiveStatus(true, callId, username);

        } catch (StreamException e) {

            int streamStatus = 502;

            if (e.getResponseData() != null &&
                    e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }

            log.error("getUserLiveStatusByUsername failed. username={}, streamStatus={}, responseData={}",
                    username, streamStatus, e.getResponseData(), e);

            throw new ExternalServiceException(
                    "Unable to check live status right now. Please try again.",
                    streamStatus,
                    e
            );
        }
    }

}
