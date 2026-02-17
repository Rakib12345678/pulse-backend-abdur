package com.zendot.pulseLive.user.service;

import com.zendot.pulseLive.user.dto.*;
import java.util.List;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request);

    UserProfileResponse declareEighteenPlusConsent(EighteenPlusConsentRequest request);

    boolean isUsernameAvailable(String username);

    void followUnfollow(FollowRequest request);

    UserPublicProfileResponse getUserProfileByUsername(String username);

    void blockUnblock(BlockUserRequest request);

    List<BlockedUserResponse> getBlockedUsers();
}
