package com.zendot.pulseLive.chat.service.impl;

import com.zendot.pulseLive.chat.service.StreamChatService;
import com.zendot.pulseLive.common.exception.ExternalServiceException;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import io.getstream.chat.java.exceptions.StreamException;
import io.getstream.chat.java.models.Channel;
import io.getstream.chat.java.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class StreamChatServiceImpl implements StreamChatService {

    private static final Logger log = LoggerFactory.getLogger(StreamChatServiceImpl.class);

    @Value("${stream.chat.channelType:livestream}")
    private String channelType;

    @Value("${stream.chat.directChannelType:messaging}")
    private String directChannelType;

    @Override
    public void upsertUser(UserProfileResponse user) {
        if (user == null) return;

        try {
            User.UserRequestObject userReq = buildChatUser(user);

            User.upsert()
                    .user(userReq)
                    .request();

        } catch (StreamException e) {
            log.error("StreamChat upsertUser failed. userId={}", user.id(), e);
            throw new ExternalServiceException(
                    "Unable to sync chat user",
                    HttpStatus.BAD_GATEWAY.value(),
                    e
            );
        }
    }

    @Override
    public void ensureLivestreamChannel(
            String callId,
            UserProfileResponse host,
            String title,
            String category,
            String hostImageUrl
    ) {
        try {
            Map<String, Object> createdByFields = new HashMap<>();
            if (hostImageUrl != null) {
                createdByFields.put("image", hostImageUrl);
            }

            User.UserRequestObject createdBy =
                    User.UserRequestObject.builder()
                            .id(String.valueOf(host.id()))
                            .name(host.username())
                            .additionalFields(createdByFields)
                            .build();

            Channel.ChannelRequestObject data =
                    Channel.ChannelRequestObject.builder()
                            .createdBy(createdBy)
                            .build();

            // Idempotent create
            Channel.getOrCreate(channelType, callId)
                    .data(data)
                    .request();

            Map<String, Object> channelFields = new HashMap<>();
            channelFields.put("callId", callId);
            if (title != null) channelFields.put("title", title);
            if (category != null) channelFields.put("category", category);
            if (hostImageUrl != null) channelFields.put("hostImageUrl", hostImageUrl);

            Channel.ChannelRequestObject updateData =
                    Channel.ChannelRequestObject.builder()
                            .additionalFields(channelFields) // ✅ CORRECT
                            .build();

            updateData.setFrozen(false);

            Channel.update(channelType, callId)
                    .data(updateData)
                    .request();

            // Ensure host is member
            Channel.update(channelType, callId)
                    .addMember(String.valueOf(host.id()))
                    .request();

        } catch (StreamException e) {
            log.error("StreamChat ensureLivestreamChannel failed. callId={}", callId, e);
            throw new ExternalServiceException(
                    "Unable to create or update chat channel",
                    HttpStatus.BAD_GATEWAY.value(),
                    e
            );
        }
    }

    @Override
    public void addMemberToLivestream(String callId, String userId) {
        try {
            Channel.update(channelType, callId)
                    .addMember(userId)
                    .hideHistory(false)
                    .request();

        } catch (StreamException e) {
            log.error("StreamChat addMember failed. callId={}, userId={}", callId, userId, e);
            throw new ExternalServiceException(
                    "Unable to join chat",
                    HttpStatus.BAD_GATEWAY.value(),
                    e
            );
        }
    }

    @Override
    public String createChatUserToken(String userId) {
        try {
            return User.createToken(userId, null, null);
        } catch (Exception e) {
            log.error("StreamChat createToken failed. userId={}", userId, e);
            throw new ExternalServiceException(
                    "Unable to create chat token",
                    HttpStatus.BAD_GATEWAY.value(),
                    e
            );
        }
    }

    @Override
    public String ensureDirectChannel(UserProfileResponse userA, UserProfileResponse userB) {
        if (userA == null || userB == null) {
            throw new ExternalServiceException(
                    "Unable to create chat channel",
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        String userAId = String.valueOf(userA.id());
        String userBId = String.valueOf(userB.id());
        String channelId = buildDirectChannelId(userAId, userBId);

        try {
            User.UserRequestObject createdBy = buildChatUser(userA);

            Channel.ChannelRequestObject data =
                    Channel.ChannelRequestObject.builder()
                            .createdBy(createdBy)
                            .build();

            Channel.getOrCreate(directChannelType, channelId)
                    .data(data)
                    .request();

            Channel.update(directChannelType, channelId)
                    .addMember(userAId)
                    .request();

            Channel.update(directChannelType, channelId)
                    .addMember(userBId)
                    .request();

            return channelId;

        } catch (StreamException e) {
            log.error("StreamChat ensureDirectChannel failed. userAId={}, userBId={}", userAId, userBId, e);
            throw new ExternalServiceException(
                    "Unable to create or update chat channel",
                    HttpStatus.BAD_GATEWAY.value(),
                    e
            );
        }
    }

    @Override
    public void freezeLivestreamChannel(String callId) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("stream_ended", true);
            fields.put("stream_ended_at", Instant.now().toString());

            Channel.ChannelRequestObject updateData =
                    Channel.ChannelRequestObject.builder()
                            .additionalFields(fields)
                            .build();

            updateData.setFrozen(true);

            Channel.update(channelType, callId)
                    .data(updateData)
                    .request();

        } catch (StreamException e) {
            log.error("StreamChat freeze channel failed. callId={}", callId, e);
            throw new ExternalServiceException(
                    "Unable to freeze chat channel",
                    HttpStatus.BAD_GATEWAY.value(),
                    e
            );
        }
    }

    private User.UserRequestObject buildChatUser(UserProfileResponse user) {
        Map<String, Object> additionalFields = new HashMap<>();
        if (user.profileImage() != null) {
            additionalFields.put("image", user.profileImage().getPath());
        }

        return User.UserRequestObject.builder()
                .id(String.valueOf(user.id()))
                .name(user.username())
                .additionalFields(additionalFields)
                .build();
    }

    private String buildDirectChannelId(String userAId, String userBId) {
        String first = userAId;
        String second = userBId;
        if (first.compareTo(second) > 0) {
            first = userBId;
            second = userAId;
        }
        return "dm-" + first + "-" + second;
    }
}
