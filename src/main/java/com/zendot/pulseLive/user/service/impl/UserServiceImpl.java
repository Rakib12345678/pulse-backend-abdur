package com.zendot.pulseLive.user.service.impl;

import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.security.SecurityUtils;
import com.zendot.pulseLive.user.dto.EighteenPlusConsentRequest;
import com.zendot.pulseLive.user.dto.UpdateUserProfileRequest;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.mapper.UserDtoMapper;
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
        return UserDtoMapper.toProfile(getCurrentUser());
    }

    @Override
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        User user = getCurrentUser();

        if (user.getUsername() != null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Username already set");
        }

        String normalizedUsername = normalizeUsername(request.getUsername());

        if (userRepo.existsByUsername(normalizedUsername)) {
            throw new AppException(HttpStatus.CONFLICT, "Username already taken");
        }

        user.setUsername(normalizedUsername);
        user.setFullName(request.getFullName());
        user.setBio(request.getBio());
        user.setLocation(request.getLocation());
        user.setProfileImagePath(
                request.getProfileImage() != null
                        ? request.getProfileImage().getPath()
                        : null
        );

        userRepo.save(user);
        return UserDtoMapper.toProfile(user);
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

        return UserDtoMapper.toProfile(user);
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        String normalized = normalizeUsername(username);
        return !userRepo.existsByUsername(normalized);
    }

    /* ---------------- helpers ---------------- */

    private User getCurrentUser() {
        Long userId = SecurityUtils.requireUserId();
        return userRepo.findById(userId)
                .orElseThrow(() ->
                        new AppException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}
