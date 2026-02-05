package com.zendot.pulseLive.stream.controller;

import com.zendot.pulseLive.security.SecurityUtils;
import com.zendot.pulseLive.stream.dto.StreamGoLiveRequest;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import com.zendot.pulseLive.stream.service.StreamService;
import io.getstream.exceptions.StreamException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    // Host starts a live stream
    @PostMapping("/go-live")
    public ResponseEntity<StreamStartResponse> goLive(StreamGoLiveRequest request) throws StreamException {

        return ResponseEntity.ok(streamService.startStream(request));
    }

    //  Viewer joins an existing live stream
    @PostMapping("/join")
    public ResponseEntity<StreamStartResponse> joinLive(
                                                        @RequestParam String callId) throws StreamException {
        return ResponseEntity.ok(streamService.joinStream(callId));
    }

    @PostMapping("/end")
    public ResponseEntity<Void> endStream(
            @RequestParam String callId
    ) throws StreamException {
            streamService.endStream(callId);
        return ResponseEntity.ok().build();
    }
}

