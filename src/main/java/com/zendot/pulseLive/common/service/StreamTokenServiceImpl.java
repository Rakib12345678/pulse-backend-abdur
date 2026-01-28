package com.zendot.pulseLive.common.service;


import io.getstream.models.framework.CallTokenClaims;
import io.getstream.services.framework.StreamSDKClient;
import io.getstream.services.framework.TokenBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StreamTokenServiceImpl implements StreamTokenService {

    private final StreamSDKClient streamSDKClient;

    public StreamTokenServiceImpl(StreamSDKClient streamSDKClient) {
        this.streamSDKClient = streamSDKClient;
    }

    @Override
    public String createUserToken(String userId) {
        String apiSecret = streamSDKClient.getHttpClient().getApiSecret();
        TokenBuilder tokenBuilder = new TokenBuilder(apiSecret);
        return tokenBuilder.createToken(userId);
    }

    @Override
    public String createUserToken(String userId, long expirySeconds) {
        String apiSecret = streamSDKClient.getHttpClient().getApiSecret();
        TokenBuilder tokenBuilder = new TokenBuilder(apiSecret);
        return tokenBuilder.createToken(userId, (int) expirySeconds);
    }

    @Override
    public String createCallToken(String userId, String role, List<String> callIds, Integer expiresIn) {
        String apiSecret = streamSDKClient.getHttpClient().getApiSecret();

        CallTokenClaims claims = new CallTokenClaims(
                userId,
                role,
                callIds,expiresIn

        );

        TokenBuilder tokenBuilder = new TokenBuilder(apiSecret);
        return tokenBuilder.createCallToken(claims);
    }
}
