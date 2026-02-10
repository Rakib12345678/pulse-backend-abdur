package com.zendot.pulseLive.stream.dto;

import java.util.List;

public record LiveFeedResponse<T>(
        List<T> data,
        String nextCursor,
        Boolean hasMore
) {}
