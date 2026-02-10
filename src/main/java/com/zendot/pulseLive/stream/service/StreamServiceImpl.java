package com.zendot.pulseLive.stream.service;

import com.zendot.pulseLive.common.exception.*;
import com.zendot.pulseLive.common.service.StreamTokenService;
import com.zendot.pulseLive.media.util.RelativePathUtils;
import com.zendot.pulseLive.stream.dto.*;
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
    @Value("${stream.callType}")
    private String callType;

    @Value("${stream.callDuration}")
    private Integer callDuration;
    private final UserService userService;
    private final RelativePathUtils utils;
    private static final Logger log = LoggerFactory.getLogger(StreamServiceImpl.class);

    public StreamServiceImpl(Video video, StreamTokenService tokenService,StreamSDKClient streamClient, UserService userService, RelativePathUtils utils) {
        this.video = video;
        this.tokenService = tokenService;
        this.streamClient = streamClient;
        this.userService = userService;
        this.utils = utils;
    }



    @Override
    public StreamStartResponse startStream(StreamGoLiveRequest request)  {

        //  Generate Call ID
        String callId = UUID.randomUUID().toString();

        //  Get Current User
        UserProfileResponse user = userService.getCurrentUserProfile();

        if (user == null || !user.eighteenPlus()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User not allowed to go live");
        }

        String userId = String.valueOf(user.id());

        try {
    /* =======================================================
       UPSERT USER IN STREAM
       ======================================================= */
            streamClient.updateUsers(
                    UpdateUsersRequest.builder()
                            .users(Map.of(
                                    userId,
                                    UserRequest.builder()
                                            .id(userId)
                                            .name(user.username())
                                            .image(utils.buildCdnUrl(user.profileImage().getPath()))

                                            .role("host")
                                            .build()
                            ))
                            .build()
            ).execute();


    /* =======================================================
       PREPARE CALL METADATA (TITLE + CATEGORY)
       ======================================================= */

            CallRequest callData = new CallRequest();

            // Who created stream
            callData.setCreatedBy(
                    UserRequest.builder()
                            .id(userId)
                            .build()
            );


            Map<String, Object> data = new HashMap<>();

            data.put("title", request.title());
            data.put("category", request.category());

            if (user.profileImage() != null) {
                data.put("profileImage",
                        utils.buildCdnUrl(user.profileImage().getPath())
                );
            }
            callData.setCustom(data);

    /* =======================================================
      CREATE CALL
       ======================================================= */

            GetOrCreateCallRequest createCallRequest =
                    GetOrCreateCallRequest.builder()
                            .video(true)
                            .data(callData)
                            .build();

            video.getOrCreateCall(callType, callId, createCallRequest).execute();


    /* =======================================================
       GO LIVE
       ======================================================= */
        video.goLive(callType, callId).execute();


    /* =======================================================
       GENERATE HOST TOKEN
       ======================================================= */

            String token = tokenService.createCallToken(
                    userId,
                    "host",
                    List.of(callId),
                    callDuration
            );


    /* =======================================================
       RETURN RESPONSE
       ======================================================= */

            return new StreamStartResponse(callId, callType, token);
        } catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error(
                    "unable to start stream. callId={}, streamStatus={}, responseData={}",
                    callId,
                    streamStatus,
                    e.getResponseData(),
                    e
            );
            throw new ExternalServiceException(
                    "Unable to go live stream right now. Please try again.",streamStatus,
                    e
            );
        }
    }

    @Override
    public StreamStartResponse joinStream( String callId) {

        // Optional: fetch call details (validate call exists)
        try {
            StreamResponse<GetCallResponse> callResponse = video.getCall(callType, callId).execute();
            if (callResponse.getData() == null) {
                throw new AppException(HttpStatus.NOT_FOUND, "Call not found");
            }
            var call = callResponse.getData().getCall();


            // Already ended
            if (call.getEndedAt() != null) {
                throw new AppException(HttpStatus.GONE, "Stream already ended");
            }

            //  Get Current User
            UserProfileResponse user = userService.getCurrentUserProfile();

            streamClient.updateUsers(
                    UpdateUsersRequest.builder()
                            .users(Map.of(
                                    String.valueOf(user.id()),
                                    UserRequest.builder()
                                            .id(String.valueOf(user.id()))
                                            .name(user.username())
                                            .image(utils.buildCdnUrl(user.profileImage().getPath()))
                                            .role("user")
                                            .build()
                            ))
                            .build()
            ).execute();


            //Generate call token for viewer
            String token = tokenService.createCallToken(
                    String.valueOf(user.id()),
                    "user",
                    List.of(callId),
                    callDuration
            );

            return new StreamStartResponse(callId, callType, token);
        }catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error(
                    "unable to join stream. callId={}, streamStatus={}, responseData={}",
                    callId,
                    streamStatus,
                    e.getResponseData(),
                    e
            );
            throw new ExternalServiceException(
                    "Unable to join stream right now. Please try again.",streamStatus,
                    e
            );
        }
    }

    @Override
    public void endStream(String callId) {

        try {
            // Optional: verify call exists
            StreamResponse<GetCallResponse> callResponse = video.getCall(callType, callId).execute();
            if (callResponse.getData() == null) {
                throw new AppException(HttpStatus.NOT_FOUND, "Call not found");
            }
            var call = callResponse.getData().getCall();

            // Not started
            if (call.getCreatedAt() == null) {
                throw new AppException(HttpStatus.CONFLICT, "Stream not started");
            }

            // Already ended
            if (call.getEndedAt() != null) {
                throw new AppException(HttpStatus.GONE, "Stream already ended");
            }


            // 2️⃣ End Call
            video.endCall(callType, callId).execute();
        }catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error(
                    "Stream endStream failed. callId={}, streamStatus={}, responseData={}",
                    callId,
                    streamStatus,
                    e.getResponseData(),
                    e
            );

            throw new ExternalServiceException(
                    "Unable to end stream right now. Please try again.",streamStatus,
                    e
            );
        }
    }







    private String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }


    @Override
    public LiveFeedResponse<LiveCallResponse> getLiveFeed(
            String cursor,
            Integer limit
    )  {

        try {
            Date cursorDate = null;

            if (cursor != null && !cursor.isBlank()) {
                cursorDate = Date.from(Instant.parse(cursor));
            }

            // -------- FILTER --------
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", callType);

            // Only LIVE calls
            filter.put("ended_at", null);

            if (cursorDate != null) {
                filter.put("created_at", Map.of("$lt", cursorDate));
            }

            // -------- SORT --------
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


            StreamResponse<QueryCallsResponse> response =
                    video.queryCalls(request).execute();

            List<CallStateResponseFields> calls =
                    response.getData().getCalls();


            boolean hasMore = calls.size() > limit;

            if (hasMore) {
                calls = calls.subList(0, limit);
            }

            List<LiveCallResponse> data =
                    calls.stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());

            // -------- NEXT CURSOR --------
            String nextCursor = null;

            if (hasMore && !calls.isEmpty()) {

                CallStateResponseFields lastState = calls.get(calls.size() - 1);

                if (lastState.getCall() != null
                        && lastState.getCall().getCreatedAt() != null) {

                    nextCursor =
                            lastState.getCall().getCreatedAt().toInstant().toString();
                }
            }

            return new LiveFeedResponse<>(data, nextCursor, hasMore);

        }catch (StreamException e) {
            int streamStatus = 502;
            if (e.getResponseData() != null && e.getResponseData().getStatusCode() != null) {
                streamStatus = e.getResponseData().getStatusCode();
            }
            log.error("Stream getLiveFeed failed. cursor={}, limit={}, streamStatus={}, responseData={}",
                    cursor,
                    limit,
                    streamStatus,
                    e.getResponseData(),
                    e
            );
            throw new ExternalServiceException(
                    "Unable to get live feed right now. Please try again.",streamStatus,
                    e
            );
        }
    }
    private LiveCallResponse mapToResponse(CallStateResponseFields state) {

        CallResponse call = state.getCall();

        return new LiveCallResponse(
                extractCallId(call.getCid()),
                call.getType(),
                getString(call.getCustom(), "title"),
                getString(call.getCustom(), "category"),
                getString(call.getCustom(), "profileImage"),
                call.getCreatedBy() != null ? call.getCreatedBy().getId() : null,
                state.getCall().getSession() != null
                        ? state.getCall().getSession().getParticipants().size()
                        : 0,
                call.getCreatedAt() != null
                        ? Date.from(call.getCreatedAt().toInstant())
                        : null,
                call.getEndedAt() == null
        );
    }


    private String extractCallId(String cid) {
        if (cid == null) return null;

        String[] parts = cid.split(":");
        return parts.length > 1 ? parts[1] : cid;
    }


}