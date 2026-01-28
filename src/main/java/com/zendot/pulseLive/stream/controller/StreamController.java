package com.zendot.pulseLive.stream.controller;

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
    public ResponseEntity<StreamStartResponse> goLive(@RequestParam String userId) throws StreamException {
        return ResponseEntity.ok(streamService.startStream(userId));
    }

    //  Viewer joins an existing live stream
    @PostMapping("/join")
    public ResponseEntity<StreamStartResponse> joinLive(@RequestParam String userId,
                                                        @RequestParam String callId) throws StreamException {
        return ResponseEntity.ok(streamService.joinStream(userId, callId));
    }

    @PostMapping("/end")
    public ResponseEntity<Void> endStream(
            @RequestParam String hostUserId,
            @RequestParam String callId
    ) throws StreamException {
        try {
            System.out.println("END STREAM");
            streamService.endStream(hostUserId, callId);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}

