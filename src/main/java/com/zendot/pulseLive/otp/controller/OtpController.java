package com.zendot.pulseLive.otp.controller;

import com.zendot.pulseLive.auth.dto.AuthResult;
import com.zendot.pulseLive.otp.dto.OtpSendRequest;
import com.zendot.pulseLive.otp.dto.OtpVerifyRequest;
import com.zendot.pulseLive.otp.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Send OTP (email signup / login)
     */
    @PostMapping("/send")
    public void send(@RequestBody @Valid OtpSendRequest req) {
        otpService.sendOtp(req.email());
    }

    /**
     * Verify OTP + auto login
     */
    @PostMapping("/verify")
    public AuthResult verify(@RequestBody @Valid OtpVerifyRequest req) {
        return otpService.verifyOtpAndLogin(req.email(), req.otp());
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendOtp(@RequestBody OtpSendRequest request) {
        otpService.resendOtp(request.email());
    }

}
