package com.zendot.pulseLive.otp.controller;

import com.zendot.pulseLive.auth.dto.AuthResult;
import com.zendot.pulseLive.otp.dto.OtpSendRequest;
import com.zendot.pulseLive.otp.dto.OtpVerifyRequest;
import com.zendot.pulseLive.otp.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/otp")
@Tag(name = "OTP", description = "Email OTP Authentication")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Send OTP (email signup / login)
     */
    @Operation(summary = "Send OTP (login/signup)")
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void send(@RequestBody @Valid OtpSendRequest req) {
        otpService.sendOtp(req.email());
    }

    /**
     * Verify OTP + auto login
     */
    @Operation(summary = "Verify OTP and login")
    @PostMapping("/verify")
    public AuthResult verify(@RequestBody @Valid OtpVerifyRequest req) {
        return otpService.verifyOtpAndLogin(req.email(), req.otp());
    }

    @Operation(summary = "Resend OTP")
    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendOtp(@RequestBody OtpSendRequest request) {
        otpService.resendOtp(request.email());
    }

}
