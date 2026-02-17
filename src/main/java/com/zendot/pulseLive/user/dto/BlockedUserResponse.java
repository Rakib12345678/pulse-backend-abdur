package com.zendot.pulseLive.user.dto;

import com.zendot.pulseLive.media.model.RelativePath;

public record BlockedUserResponse(
        Long id,
        String username,
        String fullName,
        RelativePath profileImage
) {}
