package com.zendot.pulseLive.common.service;

public interface StreamTokenService {
    String createUserToken(String userId);

    String createUserToken(String userId, long expirySeconds);

    String createCallToken(String userId, String role, java.util.List<String> callIds, Integer expiresAt);

}
