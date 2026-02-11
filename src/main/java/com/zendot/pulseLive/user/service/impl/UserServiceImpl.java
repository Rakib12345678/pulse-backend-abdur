package com.zendot.pulseLive.user.service.impl;

import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.security.SecurityUtils;
import com.zendot.pulseLive.user.dto.*;
import com.zendot.pulseLive.user.mapper.UserDtoMapper;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserFollow;
import com.zendot.pulseLive.user.repo.UserFollowRepository;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final UserFollowRepository followRepo;

    public UserServiceImpl(UserRepository userRepo, UserFollowRepository followRepo) {
        this.userRepo = userRepo;
        this.followRepo = followRepo;
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

    @Override
    @Transactional
    public void followUnfollow(FollowRequest request) {

        Long currentUserId = SecurityUtils.requireUserId();
        Long targetUserId = request.targetUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot follow itself or unfollow itself");
        }

        boolean exists = followRepo.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);

        if (request.follow()) {

            if (!exists) {

                followRepo.save(
                        UserFollow.builder()
                                .followerId(currentUserId)
                                .followingId(targetUserId)
                                .build()
                );

                userRepo.incrementFollowing(currentUserId);
                userRepo.incrementFollowers(targetUserId);
            }

        } else {

            if (exists) {

                followRepo.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);

                userRepo.decrementFollowing(currentUserId);
                userRepo.decrementFollowers(targetUserId);
            }
        }
    }


    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileResponse getUserProfileByUsername(String username) {

        Long currentUserId = SecurityUtils.requireUserId();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        Long targetUserId = user.getId();

        boolean followedByMe = false;

        if (!currentUserId.equals(targetUserId)) {
            followedByMe = followRepo.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        }

        return new UserPublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getBio(),
                user.getLocation(),
                user.getProfileImagePath() == null ? null : new RelativePath(user.getProfileImagePath()),
                user.getStatus(),
                user.isEighteenPlus(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount(),
                user.getCreatedAt(),
                followedByMe
        );
    }


}
