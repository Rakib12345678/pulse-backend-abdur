package com.zendot.pulseLive.media.model;

import java.time.Instant;

public record PresignedUploadResponse(
        String uploadUrl,
        String key,
        String fullUrl,
        Instant expiresAt
) {}

