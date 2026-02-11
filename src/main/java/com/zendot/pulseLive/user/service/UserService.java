package com.zendot.pulseLive.user.service;

import com.zendot.pulseLive.user.dto.*;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request);

    UserProfileResponse declareEighteenPlusConsent(EighteenPlusConsentRequest request);

    boolean isUsernameAvailable(String username);

    public void followUnfollow(FollowRequest request);
    UserPublicProfileResponse getUserProfileByUsername(String username);
}
