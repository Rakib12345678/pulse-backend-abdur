package com.zendot.pulseLive.user.controller;

import com.zendot.pulseLive.user.dto.*;
import com.zendot.pulseLive.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Current user profile APIs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.getCurrentUserProfile();
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/me")
    public UserProfileResponse updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return userService.updateCurrentUserProfile(request);
    }

    @Operation(summary = "Declare 18+ consent")
    @PutMapping("/me/18plus-consent")
    public UserProfileResponse set18PlusConsent(
            @Valid @RequestBody EighteenPlusConsentRequest request
    ) {
        return userService.declareEighteenPlusConsent(request);
    }

    @GetMapping("/username-available")
    public boolean isUsernameAvailable(@RequestParam String username) {
        return userService.isUsernameAvailable(username);
    }

    @PostMapping({"/follow", "/unfollow"})
    public ResponseEntity<?> followUnfollow(@RequestBody FollowRequest request) {
        userService.followUnfollow(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get user profile by username")
    @GetMapping("/{username}")
    public UserPublicProfileResponse getByUsername(@PathVariable String username) {
        return userService.getUserProfileByUsername(username);
    }


}
