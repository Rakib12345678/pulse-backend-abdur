package com.zendot.pulseLive.user.service;

import com.zendot.pulseLive.user.dto.EighteenPlusConsentRequest;
import com.zendot.pulseLive.user.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse declareEighteenPlusConsent(EighteenPlusConsentRequest request);
}
