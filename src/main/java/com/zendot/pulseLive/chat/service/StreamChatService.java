package com.zendot.pulseLive.chat.service;

import com.zendot.pulseLive.user.dto.UserProfileResponse;

public interface StreamChatService {

    /** Ensure the given app user exists in Stream Chat (upsert). */
    void upsertUser(UserProfileResponse user);

    /**
     * Create (if missing) and configure a chat channel for a livestream call.
     * Channel ID = callId.
     */
    void ensureLivestreamChannel(String callId, UserProfileResponse host, String title, String category, String hostImageUrl);

    /** Add a user to the livestream channel for the given callId (idempotent). */
    void addMemberToLivestream(String callId, String userId);

    /** Create a Stream Chat user token for a given user id. */
    String createChatUserToken(String userId);

    /**
     * Create (if missing) and configure a direct (1:1) chat channel.
     * Channel ID should be deterministic for the same pair.
     */
    String ensureDirectChannel(UserProfileResponse userA, UserProfileResponse userB);

    /**
     * Auto-disable chat when livestream ends:
     * Freeze channel => read-only (no new messages).
     */
    void freezeLivestreamChannel(String callId);
}
