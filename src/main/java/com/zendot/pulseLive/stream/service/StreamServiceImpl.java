package com.zendot.pulseLive.stream.service;

import com.zendot.pulseLive.common.exception.StreamEndedException;
import com.zendot.pulseLive.common.exception.StreamNotFoundException;
import com.zendot.pulseLive.common.exception.StreamNotStartedException;
import com.zendot.pulseLive.common.service.StreamTokenService;
import com.zendot.pulseLive.stream.dto.StreamGoLiveRequest;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.service.UserService;
import io.getstream.exceptions.StreamException;
import io.getstream.models.*;
import io.getstream.models.framework.StreamResponse;
import io.getstream.services.Video;
import io.getstream.services.framework.StreamSDKClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public StreamServiceImpl(Video video, StreamTokenService tokenService, StreamSDKClient streamClient, UserService userService) {
        this.video = video;
        this.tokenService = tokenService;
        this.streamClient = streamClient;
        this.userService = userService;
    }



    @Override
    public StreamStartResponse startStream(StreamGoLiveRequest request) throws StreamException {

        //  Generate Call ID
        String callId = UUID.randomUUID().toString();

        //  Get Current User
        UserProfileResponse user = userService.getCurrentUserProfile();

        if (user == null || !user.eighteenPlus()) {
            throw new RuntimeException("User not allowed to go live");
        }

        String userId = String.valueOf(user.id());

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

    //    STREAM CUSTOM DATA
        callData.setCustom(Map.of(
                "title", request.title(),
                "category", request.category()
        ));


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
    }

    @Override
    public StreamStartResponse joinStream( String callId) throws StreamException {

        // Optional: fetch call details (validate call exists)
      StreamResponse<GetCallResponse> callResponse=  video.getCall(callType, callId).execute();
      if(callResponse.getData()==null){
          throw new StreamNotFoundException("Call not found");
      }


        //  Get Current User
        UserProfileResponse user = userService.getCurrentUserProfile();

        if (callResponse.getData() == null) {
            throw new StreamNotFoundException("Stream not found");
        }

        if (callResponse.getData().getCall().getStartsAt() == null) {
            throw new StreamNotStartedException("Stream has not started yet");
        }

        if (callResponse.getData().getCall().getEndedAt() != null) {
            throw new StreamEndedException("Stream has already ended");
        }

        streamClient.updateUsers(
                UpdateUsersRequest.builder()
                        .users(Map.of(
                                String.valueOf(user.id()),
                                UserRequest.builder()
                                        .id(String.valueOf(user.id()))
                                        .name(user.username())
                                        .role("viewer")
                                        .build()
                        ))
                        .build()
        ).execute();

         //Generate call token for viewer
        String token = tokenService.createCallToken(
                String.valueOf(user.id()),
                "viewer",
                List.of(callId),
                callDuration
        );

        return new StreamStartResponse(callId, callType, token);
    }

    @Override
    public void endStream(String callId) throws StreamException {


        // Optional: verify call exists
     //   StreamResponse<GetCallResponse> callResponse=  video.getCall(callType, callId).execute();
//        if(callResponse.getData()==null){
//            throw new StreamNotFoundException("Call not found");
//        }
//
//
//        //  Get Current User
//        UserProfileResponse user = userService.getCurrentUserProfile();
//
//        if (callResponse.getData() == null) {
//            throw new StreamNotFoundException("Stream not found");
//        }
//
//        if (callResponse.getData().getCall().getStartsAt() == null) {
//            throw new StreamNotStartedException("Stream has not started yet");
//        }


         video.endCall(callType, callId).execute();

    }

    private void createOrUpdateUser(String userId, String name, String role) throws StreamException {
        UserRequest user = UserRequest.builder()
                .id(userId)
                .name(name)
                .role(role)
                .build();

        UpdateUsersRequest request = new UpdateUsersRequest();
        request.setUsers(Map.of(userId, user));

        streamClient.updateUsers(request).execute();
    }


}