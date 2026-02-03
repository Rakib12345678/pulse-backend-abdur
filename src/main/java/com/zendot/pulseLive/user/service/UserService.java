package com.zendot.pulseLive.user.service;

import com.zendot.pulseLive.user.dto.EighteenPlusConsentRequest;
import com.zendot.pulseLive.user.dto.UpdateUserProfileRequest;
import com.zendot.pulseLive.user.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request);

    UserProfileResponse declareEighteenPlusConsent(EighteenPlusConsentRequest request);

    boolean isUsernameAvailable(String username);

}
