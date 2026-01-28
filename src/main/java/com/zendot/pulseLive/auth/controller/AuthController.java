package com.zendot.pulseLive.auth.controller;

import com.zendot.pulseLive.auth.dto.*;
import com.zendot.pulseLive.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public AuthResult google(@RequestBody @Valid GoogleLoginRequest req) {
        return authService.loginGoogle(req.idToken(), req.is18Plus());
    }

    @PostMapping("/refresh")
    public AuthResult refresh(@RequestParam String refreshToken) {
        return authService.refresh(refreshToken);
    }
}
