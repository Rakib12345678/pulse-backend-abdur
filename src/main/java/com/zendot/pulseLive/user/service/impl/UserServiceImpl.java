package com.zendot.pulseLive.user.service.impl;

import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.security.SecurityUtils;
import com.zendot.pulseLive.user.dto.EighteenPlusConsentRequest;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;

    public UserServiceImpl(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return toProfile(user);
    }

    @Override
    public UserProfileResponse declareEighteenPlusConsent(EighteenPlusConsentRequest request) {
        if (!Boolean.TRUE.equals(request.eighteenPlus())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "eighteenPlus must be true");
        }

        User user = getCurrentUser();

        if (!user.isEighteenPlus()) {
            user.setEighteenPlus(true);
            userRepo.save(user);
        }

        return toProfile(user);
    }

    /* -------------------- helpers -------------------- */

    private User getCurrentUser() {
        Long userId = SecurityUtils.requireUserId();

        return userRepo.findById(userId)
                .orElseThrow(() ->
                        new AppException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private static UserProfileResponse toProfile(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getEmail(),
                u.getStatus(),
                u.isEmailVerified(),
                u.isEighteenPlus(),
                u.getCreatedAt()
        );
    }
}
