package com.zendot.pulseLive.auth.controller;

import com.zendot.pulseLive.auth.dto.*;
import com.zendot.pulseLive.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "Authentication APIs")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login with Google")
    @PostMapping("/google")
    public AuthResult google(@RequestBody @Valid GoogleLoginRequest req) {
        return authService.loginGoogle(req.idToken(), req.iseighteenPlus());
    }

    @PostMapping("/refresh")
    public AuthResult refresh(@RequestParam String refreshToken) {
        return authService.refresh(refreshToken);
    }
}
