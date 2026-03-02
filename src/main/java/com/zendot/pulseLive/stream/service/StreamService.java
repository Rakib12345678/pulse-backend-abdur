package com.zendot.pulseLive.stream.service;

import com.zendot.pulseLive.stream.dto.*;

/*
 * StreamService is used to manage live-streaming features using Stream.io Video SDK.
 *
 * It contains all core operations related to live calls such as:
 * - starting a live stream as a host
 * - joining a live stream as a viewer
 * - ending an active live stream
 * - fetching live streams feed with cursor based pagination
 *
 * This service acts as a central layer between controllers and Stream.io SDK,
 */
public interface StreamService {
    // Creates a new call and starts live-streaming as host
    StreamStartResponse startStream(StreamGoLiveRequest request) ;

    //  Validates call and generates join token for viewer
    StreamStartResponse joinStream( String callId) ;

    //Ends an active live stream
    void endStream(String callId) ;

    //Returns paginated list of currently live streams (cursor based)
    LiveFeedResponse<LiveCallResponse> getLiveFeed(
            String cursor,
            Integer limit
    ) ;

    UserLiveStatus getUserLiveStatusByUsername(String username);

    LiveCallResponse syncParticipantCount(String callId);

    LiveCallResponse updateCohost(AddCohostRequest request);

}
