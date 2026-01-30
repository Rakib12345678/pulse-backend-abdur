package com.zendot.pulseLive.user.controller;

import com.zendot.pulseLive.user.dto.EighteenPlusConsentRequest;
import com.zendot.pulseLive.user.dto.UserProfileResponse;
import com.zendot.pulseLive.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile APIs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user's profile")
    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.getCurrentUserProfile();
    }

    @Operation(summary = "Declare 18+ consent (can be done later)")
    @PutMapping("/me/18plus-consent")
    public UserProfileResponse set18PlusConsent(
            @RequestBody @Valid EighteenPlusConsentRequest request
    ) {
        return userService.declareEighteenPlusConsent(request);
    }
}
