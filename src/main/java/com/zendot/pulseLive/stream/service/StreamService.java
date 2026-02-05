package com.zendot.pulseLive.stream.service;

import com.zendot.pulseLive.stream.dto.StreamGoLiveRequest;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import io.getstream.exceptions.StreamException;

public interface StreamService {
    StreamStartResponse startStream(StreamGoLiveRequest request) throws StreamException;

    StreamStartResponse joinStream( String callId) throws StreamException;
    void endStream(String callId) throws StreamException;
}