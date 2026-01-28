package com.zendot.pulseLive.stream.service;

import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import io.getstream.exceptions.StreamException;

public interface StreamService {
    StreamStartResponse startStream(String hostUserId) throws StreamException;

    StreamStartResponse joinStream(String userId, String callId) throws StreamException;
    void endStream(String hostUserId, String callId) throws StreamException;
}