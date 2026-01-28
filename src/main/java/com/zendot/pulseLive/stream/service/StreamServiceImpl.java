package com.zendot.pulseLive.stream.service;

import com.zendot.pulseLive.common.service.StreamTokenService;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import io.getstream.exceptions.StreamException;
import io.getstream.models.*;
import io.getstream.models.framework.StreamResponse;
import io.getstream.services.Video;
import io.getstream.services.framework.StreamSDKClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StreamServiceImpl implements StreamService {
    private final Video video;
    private final StreamTokenService tokenService;
    private final StreamSDKClient streamClient;
    public StreamServiceImpl(Video video, StreamTokenService tokenService, StreamSDKClient streamClient) {
        this.video = video;
        this.tokenService = tokenService;
        this.streamClient = streamClient;
    }
    @Override
    public StreamStartResponse startStream(String hostUserId) throws StreamException {

        String callType = "livestream";
        String callId = UUID.randomUUID().toString();

        /* 1️ UPSERT USER (safe to call every time) */
        streamClient.updateUsers(
                UpdateUsersRequest.builder()
                        .users(Map.of(
                                hostUserId,
                                UserRequest.builder()
                                        .id(hostUserId)
                                        .name("Host User")
                                        .role("host")
                                        .build()
                        ))
                        .build()
        ).execute();

        /*  CREATE CALL (THIS WAS MISSING) */
        CallRequest callData = new CallRequest();
        callData.setCreatedBy(
                UserRequest.builder()
                        .id(hostUserId)
                        .build()
        );

        GetOrCreateCallRequest createCallRequest =
                GetOrCreateCallRequest.builder()
                        .video(true)
                        .data(callData)
                        .build();

        System.out.println(video.getOrCreateCall(callType, callId, createCallRequest).execute());

        /*  GO LIVE (ONLY ONCE) */
        video.goLive(callType, callId).execute();

        /*  GENERATE TOKEN */
        String token = tokenService.createCallToken(
                hostUserId,
                "host",
                List.of(callId),
                60 * 60
        );

        return new StreamStartResponse(callId, callType, token);
    }

    @Override
    public StreamStartResponse joinStream(String userId, String callId) throws StreamException {
        String callType = "livestream";

        // Optional: fetch call details (validate call exists)
        video.getCall(callType, callId).execute();

        streamClient.updateUsers(
                UpdateUsersRequest.builder()
                        .users(Map.of(
                                "rocky",
                                UserRequest.builder()
                                        .id("54321")
                                        .name("rocky")
                                        .role("viewer")
                                        .build()
                        ))
                        .build()
        ).execute();

         //Generate call token for viewer
        String token = tokenService.createCallToken(
                userId,
                "viewer",
                List.of(callId),
                60*60
        );

        return new StreamStartResponse(callId, callType, token);
    }

    @Override
    public void endStream(String hostUserId, String callId) throws StreamException {
        String callType = "livestream";

        // Optional: verify call exists
        video.getCall(callType, callId).execute();


        StreamResponse<EndCallResponse> response = video.endCall(callType, callId).execute();
        System.out.println("END RESPONSE: " + response.getData());

    }

    public void createOrUpdateUser(String userId, String name, String role) throws StreamException {
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