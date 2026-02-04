package com.zendot.pulseLive.user.mapper;

import com.zendot.pulseLive.auth.dto.AuthUserDto;
import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.model.User;

public final class UserDtoMapper {

    private UserDtoMapper() {}

    public static UserProfileResponse toProfile(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                u.getFullName(),
                u.getBio(),
                u.getLocation(),
                u.getProfileImagePath() != null ? new RelativePath(u.getProfileImagePath()) : null,
                u.getStatus(),
                u.isEmailVerified(),
                u.isEighteenPlus(),
                u.getCreatedAt()
        );
    }

    public static AuthUserDto toAuthUser(User u) {
        return new AuthUserDto(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                u.getFullName(),
                u.getBio(),
                u.getLocation(),
                u.getProfileImagePath() != null ? new RelativePath(u.getProfileImagePath()) : null,
                u.getStatus(),
                u.isEmailVerified(),
                u.isEighteenPlus(),
                u.getCreatedAt()
        );
    }
}
