package com.zendot.pulseLive.stream.controller;

import com.zendot.pulseLive.stream.dto.LiveFeedResponse;
import com.zendot.pulseLive.stream.dto.LiveCallResponse;
import com.zendot.pulseLive.stream.dto.StreamGoLiveRequest;
import com.zendot.pulseLive.stream.dto.StreamStartResponse;
import com.zendot.pulseLive.stream.service.StreamService;
import io.getstream.exceptions.StreamException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    // Host starts a live stream
    @PostMapping("/go-live")
    public ResponseEntity<StreamStartResponse> goLive(@Valid @RequestBody StreamGoLiveRequest request) throws StreamException {
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

    @GetMapping("/feed")
    public LiveFeedResponse<LiveCallResponse> getLiveFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") Integer limit
    ) throws StreamException {
        return streamService.getLiveFeed(cursor, limit);
    }

}

