package com.zendot.pulseLive.user.serviceImpl;

import com.zendot.pulseLive.common.exception.AppException;
import com.zendot.pulseLive.security.SecurityUtils;
import com.zendot.pulseLive.user.dto.BlockUserRequest;
import com.zendot.pulseLive.user.dto.BlockedUserResponse;
import com.zendot.pulseLive.user.dto.EighteenPlusConsentRequest;
import com.zendot.pulseLive.user.dto.FollowRequest;
import com.zendot.pulseLive.user.dto.UpdateUserProfileRequest;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.dto.UserPublicProfileResponse;
import com.zendot.pulseLive.user.model.User;
import com.zendot.pulseLive.user.model.UserBlock;
import com.zendot.pulseLive.user.model.UserFollow;
import com.zendot.pulseLive.user.repo.UserBlockRepository;
import com.zendot.pulseLive.user.repo.UserFollowRepository;
import com.zendot.pulseLive.user.repo.UserRepository;
import com.zendot.pulseLive.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private UserFollowRepository followRepo;

    @Mock
    private UserBlockRepository blockRepo;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEighteenPlus(false);
    }

    @Test
    void getCurrentUserProfile_success() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(userRepo.findById(1L)).thenReturn(Optional.of(user));

            UserProfileResponse res = userService.getCurrentUserProfile();

            assertNotNull(res);
            assertEquals("testuser", res.username());
            verify(userRepo).findById(1L);
        }
    }

    @Test
    void updateProfile_success_basicFields() {
        UpdateUserProfileRequest req =
                new UpdateUserProfileRequest(null, "New Name", "Bio", "India", null);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(userRepo.findById(1L)).thenReturn(Optional.of(user));

            userService.updateCurrentUserProfile(req);

            verify(userRepo).save(user);
            assertEquals("New Name", user.getFullName());
            assertEquals("Bio", user.getBio());
            assertEquals("India", user.getLocation());
        }
    }

    @Test
    void updateProfile_usernameTaken_shouldThrow() {
        user.setUsername(null);
        UpdateUserProfileRequest req =
                new UpdateUserProfileRequest("taken", null, null, null, null);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(userRepo.findById(1L)).thenReturn(Optional.of(user));
            when(userRepo.existsByUsername("taken")).thenReturn(true);

            assertThrows(AppException.class, () -> userService.updateCurrentUserProfile(req));
        }
    }

    @Test
    void declareEighteenPlusConsent_success() {
        EighteenPlusConsentRequest req = new EighteenPlusConsentRequest(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(userRepo.findById(1L)).thenReturn(Optional.of(user));

            userService.declareEighteenPlusConsent(req);

            verify(userRepo).save(user);
            assertTrue(user.isEighteenPlus());
        }
    }

    @Test
    void declareEighteenPlusConsent_false_shouldThrow() {
        EighteenPlusConsentRequest req = new EighteenPlusConsentRequest(false);
        assertThrows(AppException.class, () -> userService.declareEighteenPlusConsent(req));
    }

    @Test
    void isUsernameAvailable_true() {
        when(userRepo.existsByUsername("abc")).thenReturn(false);
        assertTrue(userService.isUsernameAvailable(" ABC "));
    }

    @Test
    void follow_success() {
        FollowRequest req = new FollowRequest(2L, true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(blockRepo.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                    1L, 2L, 2L, 1L)).thenReturn(false);
            when(followRepo.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);

            userService.followUnfollow(req);

            verify(followRepo).save(any(UserFollow.class));
            verify(userRepo).incrementFollowing(1L);
            verify(userRepo).incrementFollowers(2L);
        }
    }

    @Test
    void follow_self_shouldThrow() {
        FollowRequest req = new FollowRequest(1L, true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            assertThrows(AppException.class, () -> userService.followUnfollow(req));
        }
    }

    @Test
    void follow_blocked_shouldThrow() {
        FollowRequest req = new FollowRequest(2L, true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(blockRepo.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                    1L, 2L, 2L, 1L)).thenReturn(true);

            assertThrows(AppException.class, () -> userService.followUnfollow(req));
        }
    }

    @Test
    void getUserProfileByUsername_success() {
        User target = new User();
        target.setId(2L);
        target.setUsername("target");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(userRepo.findByUsername("target")).thenReturn(Optional.of(target));
            when(blockRepo.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                    1L, 2L, 2L, 1L)).thenReturn(false);
            when(followRepo.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

            UserPublicProfileResponse res = userService.getUserProfileByUsername("target");

            assertNotNull(res);
            assertEquals("target", res.username());
            assertTrue(res.followedByMe());
        }
    }

    @Test
    void block_success() {
        BlockUserRequest req = new BlockUserRequest(2L, true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(userRepo.findById(2L)).thenReturn(Optional.of(new User()));
            when(blockRepo.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
            when(followRepo.existsByFollowerIdAndFollowingId(anyLong(), anyLong())).thenReturn(false);

            userService.blockUnblock(req);

            verify(blockRepo).save(any(UserBlock.class));
        }
    }

    @Test
    void unblock_success() {
        BlockUserRequest req = new BlockUserRequest(2L, false);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(userRepo.findById(2L)).thenReturn(Optional.of(new User()));
            when(blockRepo.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            userService.blockUnblock(req);

            verify(blockRepo).deleteByBlockerIdAndBlockedId(1L, 2L);
            verify(blockRepo).flush();
        }
    }

    @Test
    void getBlockedUsers_success() {
        UserBlock block = UserBlock.builder().blockerId(1L).blockedId(2L).build();
        User blockedUser = new User();
        blockedUser.setId(2L);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::requireUserId).thenReturn(1L);
            when(blockRepo.findAllByBlockerId(1L)).thenReturn(List.of(block));
            when(userRepo.findById(2L)).thenReturn(Optional.of(blockedUser));

            List<BlockedUserResponse> res = userService.getBlockedUsers();

            assertEquals(1, res.size());
        }
    }

    @Test
    void ensureNotBlocked_shouldThrow() {
        when(blockRepo.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                1L, 2L, 2L, 1L)).thenReturn(true);

        assertThrows(AppException.class, () -> userService.ensureNotBlocked(1L, 2L));
    }

    @Test
    void ensureNotBlocked_success() {
        when(blockRepo.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                1L, 2L, 2L, 1L)).thenReturn(false);

        assertDoesNotThrow(() -> userService.ensureNotBlocked(1L, 2L));
    }
}
