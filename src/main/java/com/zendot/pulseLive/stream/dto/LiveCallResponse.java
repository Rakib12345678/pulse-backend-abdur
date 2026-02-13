package com.zendot.pulseLive.stream.dto;
import java.util.Date;


public record LiveCallResponse(
        String username,
        String callId,
        String callType,
        String title,
        String category,
        String profileImage,
        String createdByUserId,
        Integer participantCount,
        Long followerCount,
        Date createdAt,
        Boolean isLive
) {}