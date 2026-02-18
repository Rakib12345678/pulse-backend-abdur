package com.zendot.pulseLive.user.service.impl;

import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.media.model.RelativePath;
import com.zendot.pulseLive.security.SecurityUtils;
import com.zendot.pulseLive.user.dto.*;
import com.zendot.pulseLive.user.mapper.UserDtoMapper;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserBlock;
import com.zendot.pulseLive.user.model.UserFollow;
import com.zendot.pulseLive.user.repo.UserBlockRepository;
import com.zendot.pulseLive.user.repo.UserFollowRepository;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final UserFollowRepository followRepo;
    private final UserBlockRepository blockRepo;

    public UserServiceImpl(UserRepository userRepo,
                           UserFollowRepository followRepo,
                           UserBlockRepository blockRepo) {
        this.userRepo = userRepo;
        this.followRepo = followRepo;
        this.blockRepo = blockRepo;
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        return UserDtoMapper.toProfile(getCurrentUser());
    }

    @Override
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        User user = getCurrentUser();

        String requestedUsername = request.getUsername();

        if (requestedUsername != null) {

            String normalizedUsername = normalizeUsername(requestedUsername);

            if (normalizedUsername.isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Username cannot be blank");
            }

            if (user.getUsername() != null) {
                if (!user.getUsername().equals(normalizedUsername)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Username cannot be changed once set");
                }
            } else {
                if (userRepo.existsByUsername(normalizedUsername)) {
                    throw new AppException(HttpStatus.CONFLICT, "Username already taken");
                }
                user.setUsername(normalizedUsername);
            }
        }

        Optional.ofNullable(request.getFullName())
                .ifPresent(user::setFullName);

        Optional.ofNullable(request.getBio())
                .ifPresent(user::setBio);

        Optional.ofNullable(request.getLocation())
                .ifPresent(user::setLocation);

        Optional.ofNullable(request.getProfileImage())
                .map(RelativePath::getPath)
                .ifPresent(user::setProfileImagePath);

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

    /* ---------------- follow ---------------- */

    @Override
    @Transactional
    public void followUnfollow(FollowRequest request) {

        Long currentUserId = SecurityUtils.requireUserId();
        Long targetUserId = request.targetUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot follow itself or unfollow itself");
        }

        if (isBlockedBetween(currentUserId, targetUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "User is blocked");
        }

        boolean exists = isBlockedByMe(currentUserId, targetUserId);

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

    /* ---------------- public profile ---------------- */

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileResponse getUserProfileByUsername(String username) {

        Long currentUserId = SecurityUtils.requireUserId();

        User user = userRepo.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        Long targetUserId = user.getId();

        // ✅ hide profile if either side blocked (Insta/Twitter style)
        ensureNotBlocked(currentUserId, targetUserId);

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

    /* ---------------- block ---------------- */

    @Override
    @Transactional
    public void blockUnblock(BlockUserRequest request) {

        Long currentUserId = SecurityUtils.requireUserId();
        Long targetUserId = request.targetUserId();

        if (targetUserId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "targetUserId is required");
        }

        if (currentUserId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot block itself");
        }

        // ensure target exists
        userRepo.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        boolean exists = blockRepo.existsByBlockerIdAndBlockedId(currentUserId, targetUserId);

        if (request.block()) {

            if (!exists) {
                blockRepo.save(
                        UserBlock.builder()
                                .blockerId(currentUserId)
                                .blockedId(targetUserId)
                                .build()
                );
            }

            // remove follow both directions + fix counters
            if (followRepo.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
                followRepo.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
                userRepo.decrementFollowing(currentUserId);
                userRepo.decrementFollowers(targetUserId);
            }

            if (followRepo.existsByFollowerIdAndFollowingId(targetUserId, currentUserId)) {
                followRepo.deleteByFollowerIdAndFollowingId(targetUserId, currentUserId);
                userRepo.decrementFollowing(targetUserId);
                userRepo.decrementFollowers(currentUserId);
            }

        } else {

            blockRepo.deleteByBlockerIdAndBlockedId(currentUserId, targetUserId);
            blockRepo.flush(); // ensure immediate DB update
        }
    }

    /* ---------------- blocked list ---------------- */

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers() {

        Long currentUserId = SecurityUtils.requireUserId();

        return blockRepo.findAllByBlockerId(currentUserId)
                .stream()
                .map(b -> userRepo.findById(b.getBlockedId()).orElse(null))
                .filter(u -> u != null)
                .map(u -> new BlockedUserResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getProfileImagePath() == null ? null : new RelativePath(u.getProfileImagePath())
                ))
                .toList();
    }

    /* ---------------- Helpers  ---------------- */
    @Override
    public boolean isBlockedBetween(Long userA, Long userB) {
        return blockRepo.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                userA, userB,
                userB, userA
        );
    }

    @Override
    public void ensureNotBlocked(Long userA, Long userB) {
        if (isBlockedBetween(userA, userB)) {
            throw new AppException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    @Override
    public boolean isBlockedByMe(Long blockerId, Long blockedId) {
        return blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

}
